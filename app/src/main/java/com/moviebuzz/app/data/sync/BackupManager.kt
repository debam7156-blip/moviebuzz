package com.moviebuzz.app.data.sync

import com.moviebuzz.app.data.db.CustomListDao
import com.moviebuzz.app.data.db.CustomListEntity
import com.moviebuzz.app.data.db.CustomListEntryEntity
import com.moviebuzz.app.data.db.MovieDao
import com.moviebuzz.app.data.db.MovieEntity
import com.moviebuzz.app.data.db.TagDao
import com.moviebuzz.app.data.db.TagEntity
import com.moviebuzz.app.data.db.MovieTagEntity
import com.moviebuzz.app.data.db.WatchEventDao
import com.moviebuzz.app.data.db.WatchEventEntity
import kotlinx.serialization.json.Json

/**
 * Export the user's entire library to a portable JSON file, and restore it.
 *
 * Restore is merge-aware:
 * - Existing movies are enriched, never blanked or duplicated.
 * - Watch events are deduplicated by (movie, date): restoring the same backup
 *   twice never duplicates history.
 * - Lists and tags are re-created only when missing.
 */
class BackupManager(
    private val movieDao: MovieDao,
    private val watchEventDao: WatchEventDao,
    private val customListDao: CustomListDao,
    private val tagDao: TagDao
) {

    private val json = Json { ignoreUnknownKeys = true; prettyPrint = true }

    suspend fun export(): String {
        // snapshotAll() has no row cap — search("") was limited to 200 rows and
        // silently truncated large libraries in the exported backup.
        val movies = movieDao.snapshotAll().sortedBy { it.id }
        val events = watchEventDao.observeAllSnapshot()
        val lists = customListDao.snapshotLists()
        val entries = customListDao.snapshotEntries()
        val tags = tagDao.snapshotTags()
        val movieTags = tagDao.snapshotMovieTags()

        val payload = BackupPayload(
            exportedAt = System.currentTimeMillis(),
            movies = movies.map { it.toBackupMovie() },
            watchEvents = events.map { ev ->
                BackupWatchEvent(
                    movieTmdbId = movies.firstOrNull { it.id == ev.movieId }?.tmdbId,
                    watchedOn = ev.watchedOn,
                    ratingAtWatch = ev.ratingAtWatch,
                    note = ev.note,
                    createdAt = ev.createdAt
                )
            },
            lists = lists.map { BackupList(it.name, it.description, it.createdAt) },
            listEntries = entries.map { BackupListEntry(it.listName, it.movieTmdbId) },
            tags = tags,
            movieTags = movieTags.map { BackupMovieTag(it.movieTmdbId, it.tagName) }
        )
        return json.encodeToString(BackupPayload.serializer(), payload)
    }

    suspend fun restore(payloadJson: String): RestoreResult {
        val payload = try {
            json.decodeFromString(BackupPayload.serializer(), payloadJson)
        } catch (e: Exception) {
            return RestoreResult(failedReason = "Unreadable backup file: ${e.message}")
        }
        if (!payload.format.equals("moviebuzz-backup", ignoreCase = true)) {
            return RestoreResult(failedReason = "This is not a MOVIEBUZZ backup file")
        }

        val idByTmdb = mutableMapOf<Long, Long>()
        var restoredMovies = 0
        var mergedMovies = 0
        var restoredEvents = 0

        // 1. Movies: insert new, merge into existing.
        for (backup in payload.movies) {
            val existing = backup.tmdbId?.let { movieDao.byTmdbId(it) }
            if (existing == null) {
                val id = movieDao.insertIgnore(backup.toEntity())
                if (id != -1L) {
                    backup.tmdbId?.let { idByTmdb[it] = id }
                    restoredMovies++
                }
            } else {
                movieDao.update(mergeMovie(existing, backup))
                backup.tmdbId?.let { idByTmdb[it] = existing.id }
                mergedMovies++
            }
        }

        // 2. Watch events: deduplicate by (movie, date).
        for (event in payload.watchEvents) {
            val movieId = event.movieTmdbId?.let { idByTmdb[it] } ?: continue
            val already = watchEventDao.forMovie(movieId).any { it.watchedOn == event.watchedOn }
            if (!already) {
                watchEventDao.insert(
                    WatchEventEntity(
                        movieId = movieId,
                        watchedOn = event.watchedOn,
                        ratingAtWatch = event.ratingAtWatch,
                        note = event.note,
                        createdAt = event.createdAt
                    )
                )
                restoredEvents++
            }
        }

        // 3. Custom lists and entries.
        val listIdByName = mutableMapOf<String, Long>()
        for (list in payload.lists) {
            val existing = customListDao.listByName(list.name)
            listIdByName[list.name] = existing?.id
                ?: customListDao.insertList(CustomListEntity(name = list.name, description = list.description))
        }
        for (entry in payload.listEntries) {
            val listId = listIdByName[entry.listName] ?: continue
            val movieId = entry.movieTmdbId?.let { idByTmdb[it] } ?: continue
            customListDao.insertEntry(CustomListEntryEntity(listId = listId, movieId = movieId))
        }

        // 4. Tags and movie-tag links.
        val tagIdByName = mutableMapOf<String, Long>()
        for (tag in payload.tags) {
            val existing = tagDao.tagByName(tag)
            tagIdByName[tag] = existing?.id ?: tagDao.insertTag(TagEntity(name = tag))
        }
        for (movieTag in payload.movieTags) {
            val tagId = tagIdByName[movieTag.tagName] ?: continue
            val movieId = movieTag.movieTmdbId?.let { idByTmdb[it] } ?: continue
            tagDao.insertMovieTag(MovieTagEntity(movieId = movieId, tagId = tagId))
        }

        return RestoreResult(
            restoredMovies = restoredMovies,
            mergedMovies = mergedMovies,
            restoredWatchEvents = restoredEvents
        )
    }

    /** Merge backup fields onto an existing movie; user data is never overwritten with blanks. */
    private fun mergeMovie(existing: MovieEntity, backup: BackupMovie): MovieEntity {
        var m = existing
        if (m.tmdbId == null && backup.tmdbId != null) m = m.copy(tmdbId = backup.tmdbId)
        if (m.imdbId == null && backup.imdbId != null) m = m.copy(imdbId = backup.imdbId)
        if (m.year == null && backup.year != null) m = m.copy(year = backup.year)
        if (m.personalRating == null && backup.personalRating != null) {
            m = m.copy(personalRating = backup.personalRating, ratingScale = backup.ratingScale)
        }
        if (m.review.isNullOrBlank() && !backup.review.isNullOrBlank()) m = m.copy(review = backup.review)
        if (m.notes.isNullOrBlank() && !backup.notes.isNullOrBlank()) m = m.copy(notes = backup.notes)
        if (backup.isFavorite) m = m.copy(isFavorite = true)
        if (backup.watched) m = m.copy(watched = true)
        if (backup.watchlist) m = m.copy(watchlist = true)
        return m.copy(updatedAt = System.currentTimeMillis())
    }
}

data class RestoreResult(
    val restoredMovies: Int = 0,
    val mergedMovies: Int = 0,
    val restoredWatchEvents: Int = 0,
    val failedReason: String? = null
)

private fun MovieEntity.toBackupMovie(): BackupMovie = BackupMovie(
    tmdbId = tmdbId,
    imdbId = imdbId,
    title = title,
    year = year,
    releaseDate = releaseDate,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    runtimeMinutes = runtimeMinutes,
    director = director,
    genres = genres,
    countries = countries,
    languages = languages,
    productionCompanies = productionCompanies,
    cast = cast,
    tmdbVoteAverage = tmdbVoteAverage,
    tmdbVoteCount = tmdbVoteCount,
    watched = watched,
    personalRating = personalRating,
    ratingScale = ratingScale,
    review = review,
    notes = notes,
    isFavorite = isFavorite,
    watchlist = watchlist,
    addedAt = addedAt,
    updatedAt = updatedAt
)

private fun BackupMovie.toEntity(): MovieEntity = MovieEntity(
    tmdbId = tmdbId,
    imdbId = imdbId,
    title = title,
    year = year,
    releaseDate = releaseDate,
    overview = overview,
    posterPath = posterPath,
    backdropPath = backdropPath,
    runtimeMinutes = runtimeMinutes,
    director = director,
    genres = genres,
    countries = countries,
    languages = languages,
    productionCompanies = productionCompanies,
    cast = cast,
    tmdbVoteAverage = tmdbVoteAverage,
    tmdbVoteCount = tmdbVoteCount,
    watched = watched,
    personalRating = personalRating,
    ratingScale = ratingScale,
    review = review,
    notes = notes,
    isFavorite = isFavorite,
    watchlist = watchlist,
    addedAt = addedAt,
    updatedAt = updatedAt
)
