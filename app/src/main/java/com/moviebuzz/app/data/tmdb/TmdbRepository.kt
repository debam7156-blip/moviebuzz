package com.moviebuzz.app.data.tmdb

import android.util.Log
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.moviebuzz.app.data.db.MovieDao
import com.moviebuzz.app.data.db.MovieEntity
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/** Thrown before any network call when no TMDB key is available. */
class TmdbKeyMissingException : IllegalStateException("No TMDB API key configured")

/** Human-readable messages for every TMDB failure mode — shown in UI error states. */
fun friendlyTmdbError(e: Throwable): String = when {
    e is TmdbKeyMissingException ->
        "No TMDB API key configured yet — open Settings to add one (free key from themoviedb.org)."
    e is HttpException && e.code() == 401 ->
        "TMDB rejected the API key. Check the key in Settings."
    e is HttpException && e.code() == 429 ->
        "TMDB rate limit reached. Wait a moment and try again."
    e is HttpException ->
        "TMDB returned an error (HTTP ${e.code()}). Try again shortly."
    e is IOException ->
        "You appear to be offline. Your library, watchlist and history still work."
    else -> "Unexpected error: ${e.message ?: "unknown"}"
}

/** Full detail payload for a movie, plus its related feeds (already persisted locally). */
data class DetailBundle(
    val movie: MovieEntity,
    val fromCache: Boolean,
    val similar: List<MovieEntity>,
    val recommended: List<MovieEntity>
)

/**
 * TMDB repository with local caching:
 * - API key injected by interceptor from ApiKeyProvider (never in code or Git).
 * - Every list result is persisted into the Room movies table so navigation ids
 *   are real database rows and detail pages work fully offline after first load.
 * - Feed results are kept in a 10-minute in-memory TTL cache; on network failure
 *   the stale cache is served instead of an error whenever possible.
 */
class TmdbRepository(
    private val movieDao: MovieDao,
    private val apiKeyProvider: ApiKeyProvider,
    baseUrl: String = "https://api.themoviedb.org/3/"
) {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val authInterceptor = Interceptor { chain ->
        val key = apiKeyProvider.current()
        val request = if (key.isNotBlank()) {
            chain.request().newBuilder()
                .url(
                    chain.request().url.newBuilder()
                        .addQueryParameter("api_key", key)
                        .build()
                )
                .build()
        } else {
            chain.request()
        }
        chain.proceed(request)
    }

    private val client = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private val api: TmdbService = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(TmdbService::class.java)

    private class CacheEntry(val savedAt: Long, val data: List<MovieEntity>)

    private val feedCache = ConcurrentHashMap<String, CacheEntry>()
    private val cacheTtlMs: Long = 10 * 60 * 1000L

    private fun requireKey() {
        if (!apiKeyProvider.isConfigured()) throw TmdbKeyMissingException()
    }

    suspend fun trending(page: Int = 1): List<MovieEntity> =
        feed("trending", page) { api.trending(page) }

    suspend fun popular(page: Int = 1): List<MovieEntity> =
        feed("popular", page) { api.popular(page) }

    suspend fun upcoming(page: Int = 1): List<MovieEntity> =
        feed("upcoming", page) { api.upcoming(page) }

    suspend fun topRated(page: Int = 1): List<MovieEntity> =
        feed("top_rated", page) { api.topRated(page) }

    suspend fun nowPlaying(page: Int = 1): List<MovieEntity> =
        feed("now_playing", page) { api.nowPlaying(page) }

    suspend fun recommendedFor(tmdbId: Long, page: Int = 1): List<MovieEntity> =
        feed("rec-$tmdbId", page) { api.recommendations(tmdbId, page) }

    suspend fun similarTo(tmdbId: Long, page: Int = 1): List<MovieEntity> =
        feed("similar-$tmdbId", page) { api.similar(tmdbId, page) }

    private suspend fun feed(name: String, page: Int, call: suspend () -> TmdbPage):
            List<MovieEntity> = withContext(Dispatchers.IO) {
        requireKey()
        val cacheKey = "$name-$page"
        val cached = feedCache[cacheKey]
        if (cached != null && System.currentTimeMillis() - cached.savedAt < cacheTtlMs) {
            return@withContext cached.data
        }
        try {
            val movies = ensureMovies(call().results.map { it.toEntity() })
            feedCache[cacheKey] = CacheEntry(System.currentTimeMillis(), movies)
            movies
        } catch (e: IOException) {
            // Network failed: serve the stale cache when we have one, else propagate.
            cached?.data ?: throw e
        }
    }

    /** Search is never TTL-cached (dynamic), but results are persisted to Room. */
    suspend fun searchRemote(query: String, year: Int? = null): List<MovieEntity> =
        withContext(Dispatchers.IO) {
            requireKey()
            val q = query.trim()
            if (q.isEmpty()) emptyList()
            else ensureMovies(api.search(q, year).results.map { it.toEntity() })
        }

    /**
     * Fetch full details and cache them locally. Falls back to the saved row when
     * offline or key-less; returns null only when nothing is known about the movie.
     */
    suspend fun movieDetailsCached(tmdbId: Long): DetailBundle? = withContext(Dispatchers.IO) {
        val local = movieDao.byTmdbId(tmdbId)
        val remote: TmdbMovie? = try {
            requireKey()
            api.movieDetails(tmdbId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w("TmdbRepository", "Detail fetch failed for tmdbId=$tmdbId, using local cache", e)
            null
        }

        if (remote == null) {
            return@withContext local?.let {
                DetailBundle(it, fromCache = true, emptyList(), emptyList())
            }
        }

        val entity = remote!!.toEntity()
        if (local == null) {
            movieDao.insertIgnore(entity)
        } else {
            movieDao.update(
                entity.copy(
                    id = local.id,
                    watched = local.watched,
                    personalRating = local.personalRating,
                    ratingScale = local.ratingScale,
                    review = local.review,
                    notes = local.notes,
                    isFavorite = local.isFavorite,
                    watchlist = local.watchlist,
                    addedAt = local.addedAt,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        val stored = movieDao.byTmdbId(tmdbId)
            ?: return@withContext DetailBundle(entity, fromCache = false, emptyList(), emptyList())
        val similar = ensureMovies((remote.similar?.results ?: emptyList()).map { it.toEntity() })
        val recommended = ensureMovies((remote.recommendations?.results ?: emptyList()).map { it.toEntity() })
        DetailBundle(stored, fromCache = false, similar, recommended)
    }

    /**
     * Persist a list of lightweight movie rows and return the full database rows
n     * (real ids, stable keys for lazy lists, offline-capable navigation targets).
     */
    private suspend fun ensureMovies(list: List<MovieEntity>): List<MovieEntity> {
        val unique = list.filter { it.tmdbId != null }.distinctBy { it.tmdbId!! }
        if (unique.isEmpty()) return emptyList()
        movieDao.insertIgnoreAll(unique)
        val byId = movieDao.byTmdbIds(unique.map { it.tmdbId!! }).associateBy { it.tmdbId }
        return unique.mapNotNull { byId[it.tmdbId] }
    }
}

/** Map a TMDB detail payload to the local entity (no personal fields set). */
internal fun TmdbMovie.toEntity(): MovieEntity = MovieEntity(
    tmdbId = id,
    imdbId = external_ids?.imdb_id,
    title = title,
    originalTitle = original_title,
    year = year,
    releaseDate = release_date,
    overview = overview,
    tagline = tagline,
    status = status,
    trailerKey = trailerKey,
    posterPath = poster_path,
    backdropPath = backdrop_path,
    runtimeMinutes = runtime,
    director = director,
    genres = genres.map { it.name },
    countries = production_countries.map { it.name },
    languages = spoken_languages.map { it.english_name },
    productionCompanies = production_companies.map { it.name },
    cast = castNames,
    castCharacters = castCharacters,
    castProfiles = castProfiles,
    tmdbVoteAverage = vote_average,
    tmdbVoteCount = vote_count,
    addedAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)

/** Map a lightweight search result to the local entity. */
internal fun TmdbSearchResult.toEntity(): MovieEntity = MovieEntity(
    tmdbId = id,
    title = title,
    year = year,
    releaseDate = release_date,
    overview = overview,
    posterPath = poster_path,
    backdropPath = backdrop_path,
    tmdbVoteAverage = vote_average,
    tmdbVoteCount = vote_count,
    addedAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)
