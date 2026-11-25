package com.moviebuzz.app.data.tmdb

import com.moviebuzz.app.BuildConfig
import com.moviebuzz.app.data.db.MovieDao
import com.moviebuzz.app.data.db.MovieEntity
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import java.util.concurrent.TimeUnit

/**
 * TMDB repository with local caching:
 * - API key injected by interceptor from BuildConfig (never in code or Git).
 * - Movie details are persisted into the Room movies table, so detail pages
 *   and the library work fully offline after first load.
 */
class TmdbRepository(
    private val movieDao: MovieDao,
    baseUrl: String = "https://api.themoviedb.org/3/"
) {

    private val json = Json { ignoreUnknownKeys = true; coerceInputValues = true }

    private val authInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
            .url(
                chain.request().url.newBuilder()
                    .addQueryParameter("api_key", BuildConfig.TMDB_API_KEY)
                    .build()
            )
            .build()
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

    /** Fetch full details and cache them locally. Falls back to cache when offline. */
    suspend fun movieDetailsCached(tmdbId: Long): MovieEntity? = withContext(Dispatchers.IO) {
        try {
            val remote = api.movieDetails(tmdbId)
            val entity = remote.toEntity()
            val existing = movieDao.byTmdbId(tmdbId)
            if (existing == null) {
                movieDao.insertIgnore(entity)
            } else {
                movieDao.update(
                    entity.copy(
                        id = existing.id,
                        watched = existing.watched,
                        personalRating = existing.personalRating,
                        ratingScale = existing.ratingScale,
                        review = existing.review,
                        notes = existing.notes,
                        isFavorite = existing.isFavorite,
                        watchlist = existing.watchlist,
                        addedAt = existing.addedAt,
                        updatedAt = System.currentTimeMillis()
                    )
                )
            }
            movieDao.byTmdbId(tmdbId)
        } catch (e: Exception) {
            movieDao.byTmdbId(tmdbId)
        }
    }

    suspend fun searchRemote(query: String, year: Int? = null): List<MovieEntity> =
        withContext(Dispatchers.IO) {
            try {
                api.search(query, year).results.map { it.toEntity() }
            } catch (e: Exception) {
                emptyList()
            }
        }

    suspend fun trending(page: Int = 1) = fetchPage { api.trending(page) }
    suspend fun popular(page: Int = 1) = fetchPage { api.popular(page) }
    suspend fun upcoming(page: Int = 1) = fetchPage { api.upcoming(page) }
    suspend fun topRated(page: Int = 1) = fetchPage { api.topRated(page) }

    private suspend fun fetchPage(call: suspend () -> TmdbPage): List<MovieEntity> =
        withContext(Dispatchers.IO) {
            try {
                call().results.map { it.toEntity() }
            } catch (e: Exception) {
                emptyList()
            }
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
    posterPath = poster_path,
    backdropPath = backdrop_path,
    runtimeMinutes = runtime,
    director = director,
    genres = genres.map { it.name },
    countries = production_countries.map { it.name },
    languages = spoken_languages.map { it.english_name },
    productionCompanies = production_companies.map { it.name },
    cast = castNames,
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
    tmdbVoteAverage = vote_average,
    tmdbVoteCount = vote_count,
    addedAt = System.currentTimeMillis(),
    updatedAt = System.currentTimeMillis()
)
