package com.moviebuzz.app.data.import

import com.moviebuzz.app.data.db.CustomListDao
import com.moviebuzz.app.data.db.CustomListEntity
import com.moviebuzz.app.data.db.CustomListEntryEntity
import com.moviebuzz.app.data.db.ImportUnresolvedEntity
import com.moviebuzz.app.data.db.MovieDao
import com.moviebuzz.app.data.db.MovieEntity
import com.moviebuzz.app.data.db.WatchEventDao
import com.moviebuzz.app.data.db.WatchEventEntity
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * The import orchestrator. Takes parsed records from any adapter, matches each against
 * the local library and TMDB, applies duplicates policy, records rewatches,
 * creates custom lists, and produces an honest report. Nothing silently dropped.
 */
class ImportEngine(
    private val movieDao: MovieDao,
    private val watchEventDao: WatchEventDao,
    private val customListDao: CustomListDao,
    private val unresolvedDao: com.moviebuzz.app.data.db.ImportUnresolvedDao,
    private val tmdbSearch: (suspend (title: String, year: Int?) -> List<MovieEntity>)? = null
) {

    suspend fun import(records: List<ImportRecord>): ImportReport {
        var imported = 0
        var duplicates = 0
        var failed = 0
        val unresolvedRows = mutableListOf<ImportUnresolvedEntity>()
        val importedLists = mutableListOf<String>()
        val missingNotes = mutableListOf<String>()

        val allMovies = movieDao.observeAll().firstOrNull() ?: emptyList()

        for (record in records) {
            try {
                val matchResult = matcher.resolve(record, MovieMatcher.Lookup(allMovies, tmdbSearch))
                when (matchResult) {
                    is MovieMatcher.MatchResult.Confident -> {
                        // Existing movie: merge personal data (never overwrite richer local data with blank)
                        val existing = matchResult.movie
                        val updated = existing.copy(
                            personalRating = existing.personalRating ?: record.rating,
                            review = existing.review ?: record.review,
                            notes = existing.notes ?: record.notes,
                            isFavorite = existing.isFavorite || record.isFavorite,
                            watchlist = existing.watchlist || record.watchlist,
                            updatedAt = System.currentTimeMillis()
                        )
                        movieDao.update(updated)
                        if (record.watchedOn != null) {
                            watchEventDao.insert(WatchEventEntity(movieId = existing.id, watchedOn = record.watchedOn))
                        }
                        if (record.listName != null) {
                            addToList(existing.id, record.listName, importedLists)
                        }
                        if (updated.id == 0L) {
                            // merged into existing; count as duplicate if nothing changed
                            if (updated == existing) duplicates++
                        } else {
                            imported++
                        }
                        if (matchResult.confidence < 0.9) {
                            missingNotes.add("${record.title} (${record.year ?: "?"}) — matched with ${matchResult.confidence} confidence via ${matchResult.basis}")
                        }
                    }
                    is MovieMatcher.MatchResult.NeedsReview -> {
                        unresolvedRows.add(
                            ImportUnresolvedEntity(
                                source = record.source,
                                recordTitle = record.title,
                                year = record.year,
                                fileLabel = record.fileLabel,
                                rowNumber = record.rowNumber,
                                rawJson = record.rawFields.toString()
                            )
                        )
                        unresolvedCount++
                    }
                    is MovieMatcher.MatchResult.NotFound -> {
                        failed++
                    }
        }
            } catch (e: Exception) {
                failed++
            }
        }

        return ImportReport(
            source = records.firstOrNull()?.source ?: "unknown",
            totalRecords = records.size,
            imported = imported,
            duplicates = duplicates,
            unresolved = unresolvedRows.size,
            failed = failed,
            importedLists = importedLists.distinct(),
            missingInfoNotes = missingNotes.distinct(),
            unresolvedRows = unresolvedRows
        )
    }

    private val matcher = MovieMatcher()
    private var unresolvedCount = 0
    private suspend fun addToList(movieId: Long, listName: String, importedLists: MutableList<String>) {
        val list = customListDao.listByName(listName) ?: CustomListEntity(name = listName).also {
            customListDao.insertList(it)
        }
        customListDao.insertEntry(CustomListEntryEntity(listId = list.id, movieId = movieId))
        importedLists.add(listName)
    }
}
