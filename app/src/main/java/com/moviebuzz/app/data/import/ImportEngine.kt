package com.moviebuzz.app.data.import

import com.moviebuzz.app.data.db.CustomListDao
import com.moviebuzz.app.data.db.CustomListEntity
import com.moviebuzz.app.data.db.CustomListEntryEntity
import com.moviebuzz.app.data.db.ImportUnresolvedDao
import com.moviebuzz.app.data.db.ImportUnresolvedEntity
import com.moviebuzz.app.data.db.MovieDao
import com.moviebuzz.app.data.db.MovieEntity
import com.moviebuzz.app.data.db.WatchEventDao
import com.moviebuzz.app.data.db.WatchEventEntity
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * The import orchestrator. Guarantees:
 * - Every source row is accounted for: imported, duplicate, unresolved, or failed.
 * - No silent discards: unmatched rows are persisted for the manual resolution screen.
 * - Rewatches are preserved as separate watch events on distinct dates.
 * - Existing movies are enriched (never overwritten with blanks).
 */
class ImportEngine(
    private val movieDao: MovieDao,
    private val watchEventDao: WatchEventDao,
    private val customListDao: CustomListDao,
    private val unresolvedDao: ImportUnresolvedDao,
    private val tmdbSearch: (suspend (title: String, year: Int?) -> List<MovieEntity>)? = null
) {

    private val matcher = MovieMatcher()
    private val json = Json { ignoreUnknownKeys = true }
    private val rawMapSerializer = MapSerializer(String.serializer(), String.serializer())

    /** Files whose rows represent viewings (vs. watchlist or list-membership intents). */
    private val viewedFileLabels = setOf("watched.csv", "ratings.csv", "diary.csv", "reviews.csv")

    suspend fun import(results: List<ImportAdapters.AdapterResult>): ImportReport {
        var imported = 0
        var duplicates = 0
        var failed = 0
        val unresolvedRows = mutableListOf<ImportUnresolvedEntity>()
        val importedLists = sortedSetOf<String>()
        val missingNotes = sortedSetOf<String>()

        // Snapshot of the library; later rows in this run resolve against earlier ones.
        val mirror = movieDao.search("").toMutableList()

        fun unresolvedEntity(
            source: String,
            fileLabel: String,
            rowNumber: Int,
            raw: Map<String, String>,
            title: String,
            year: Int?
        ): ImportUnresolvedEntity = ImportUnresolvedEntity(
            source = source,
            fileLabel = fileLabel,
            rowNumber = rowNumber,
            rawJson = json.encodeToString(rawMapSerializer, raw),
            title = title,
            year = year
        )

        for (result in results) {
            when (result) {
                is ImportAdapters.FileError -> {
                    failed++
                    missingNotes.add("${result.fileName}: ${result.reason}")
                }
                is ImportAdapters.UnparsedRow -> {
                    failed++
                    unresolvedRows.add(
                        unresolvedEntity(
                            source = "unknown",
                            fileLabel = "parse",
                            rowNumber = result.rowNumber,
                            raw = result.raw,
                            title = result.raw.values.firstOrNull { it.isNotBlank() } ?: "unknown",
                            year = null
                        )
                    )
                }
                is ImportAdapters.Parsed -> {
                    val record = result.record
                    val match = matcher.resolve(record, MovieMatcher.Lookup(mirror.toList(), tmdbSearch))
                    when (match) {
                        is MovieMatcher.MatchResult.Confident -> {
                            val existing = match.movie
                            movieDao.update(merge(existing, record))
                            applyWatch(record, existing.id)
                            applyList(record, existing.id, importedLists)
                            duplicates++
                            if (match.confidence < 0.9) {
                                missingNotes.add(
                                    "${record.title} (${record.year ?: "?"}) matched with ${match.confidence} confidence via ${match.basis}"
                                )
                            }
                        }
                        is MovieMatcher.MatchResult.NeedsReview -> {
                            unresolvedRows.add(
                                unresolvedEntity(
                                    source = record.source,
                                    fileLabel = record.fileLabel,
                                    rowNumber = record.rowNumber,
                                    raw = record.rawFields,
                                    title = record.title,
                                    year = record.year
                                )
                            )
                        }
                        is MovieMatcher.MatchResult.NotFound -> {
                            val entity = record.toEntity(inferWatched(record))
                            val newId = movieDao.insertIgnore(entity)
                            if (newId != -1L) {
                                movieDao.byId(newId)?.let { mirror.add(it) }
                                applyWatch(record, newId)
                                applyList(record, newId, importedLists)
                                imported++
                                if (record.year == null) missingNotes.add("${record.title}: missing release year")
                            } else {
                                // Unique-index collision (same TMDB id already stored): merge instead of dropping.
                                val existing = if (record.tmdbId != null) {
                                    movieDao.byTmdbId(record.tmdbId)
                                } else {
                                    null
                                } ?: movieDao.byTitleYear(record.title, record.year ?: 0)
                                if (existing != null) {
                                    movieDao.update(merge(existing, record))
                                    applyWatch(record, existing.id)
                                    applyList(record, existing.id, importedLists)
                                    duplicates++
                                } else {
                                    failed++
                                    missingNotes.add("${record.title}: could not insert (unknown collision)")
                                }
                            }
                        }
                    }
                }
            }
        }

        if (unresolvedRows.isNotEmpty()) unresolvedDao.insertAll(unresolvedRows)

        return ImportReport(
            source = (results.filterIsInstance<ImportAdapters.Parsed>().firstOrNull()?.record?.source) ?: "unknown",
            totalRecords = results.size,
            imported = imported,
            duplicates = duplicates,
            unresolved = unresolvedRows.size,
            failed = failed,
            importedLists = importedLists.toList(),
            missingInfoNotes = missingNotes.toList(),
            unresolvedRows = unresolvedRows
        )
    }

    /** Merge imported fields onto an existing movie. Existing user data is never overwritten with blanks. */
    private fun merge(existing: MovieEntity, record: ImportRecord): MovieEntity {
        var m = existing
        if (m.tmdbId == null && record.tmdbId != null) m = m.copy(tmdbId = record.tmdbId)
        if (m.imdbId == null && record.imdbId != null) m = m.copy(imdbId = record.imdbId)
        if (m.year == null && record.year != null) m = m.copy(year = record.year)
        if (m.personalRating == null && record.rating != null) {
            m = m.copy(personalRating = record.rating, ratingScale = record.ratingScale)
        }
        if (m.review.isNullOrBlank() && !record.review.isNullOrBlank()) m = m.copy(review = record.review)
        if (m.notes.isNullOrBlank() && !record.notes.isNullOrBlank()) m = m.copy(notes = record.notes)
        if (record.isFavorite) m = m.copy(isFavorite = true)
        if (record.watchlist) m = m.copy(watchlist = true)
        if (inferWatched(record)) m = m.copy(watched = true)
        return m.copy(updatedAt = System.currentTimeMillis())
    }

    /**
     * A record represents a viewing when it carries a date, comes from a viewed-file
     * (watched/ratings/diary/reviews), or is a generic non-watchlist row.
     */
    private fun inferWatched(record: ImportRecord): Boolean {
        if (record.watchlist) return false
        if (record.watchedOn != null) return true
        if (record.fileLabel.lowercase() in viewedFileLabels) return true
        if (record.fileLabel.lowercase().startsWith("list-details")) return false
        return record.source == "generic" || record.source == "moviebase" || record.source == "ilikemovies"
    }

    /** Insert a viewing session unless one already exists for the same movie on the same date. */
    private suspend fun applyWatch(record: ImportRecord, movieId: Long) {
        val date = record.watchedOn?.takeIf { it.matches(Regex("\\d{4}-\\d{2}-\\d{2}")) } ?: return
        val already = watchEventDao.forMovie(movieId).any { it.watchedOn == date }
        if (already) return
        watchEventDao.insert(
            WatchEventEntity(
                movieId = movieId,
                watchedOn = date,
                ratingAtWatch = record.rating,
                note = if (record.rewatch) "Rewatch (imported)" else null
            )
        )
    }

    private suspend fun applyList(record: ImportRecord, movieId: Long, importedLists: MutableSet<String>) {
        val name = record.listName?.trim()?.takeIf { it.isNotBlank() } ?: return
        val list = customListDao.listByName(name)
        val listId = if (list != null) list.id else customListDao.insertList(CustomListEntity(name = name))
        customListDao.insertEntry(CustomListEntryEntity(listId = listId, movieId = movieId))
        importedLists.add(name)
    }

    private fun ImportRecord.toEntity(watched: Boolean): MovieEntity = MovieEntity(
        tmdbId = tmdbId,
        imdbId = imdbId,
        title = title,
        year = year,
        personalRating = rating,
        ratingScale = ratingScale,
        review = review,
        notes = notes,
        isFavorite = isFavorite,
        watchlist = watchlist,
        watched = watched,
        addedAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
}
