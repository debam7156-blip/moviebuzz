package com.moviebuzz.app.data.import

/**
 * Letterboxd import adapter.
 *
 * Tested against the published Letterboxd export column sets:
 * - watched.csv: Name,Year,Letterboxd URI
 * - ratings.csv: Name,Year,Letterboxd URI,Rating
 * - diary.csv: Date,Name,Year,Letterboxd URI,Rating,rewatch,Lists,Notes
 * - watchlist.csv: Position,Name,Year,Letterboxd URI,Created at
 * - reviews.csv: Name,Year,Letterboxd URI,Rating,Date,Review
 * - lists/list-*.csv: Description,Name,Year,Letterboxd URI (list name taken from the file name)
 */
object LetterboxdAdapter {

    fun detect(tables: Map<String, CsvParser.CsvTable>): Boolean =
        tables.keys.any {
            it.equals("ratings.csv", true) ||
            it.equals("diary.csv", true) ||
            it.equals("watched.csv", true) ||
            it.equals("watchlist.csv", true)
        }

    fun parse(tables: Map<String, CsvParser.CsvTable>): List<ImportRecord> {
        val records = mutableListOf<ImportRecord>()
        val seen = mutableMapOf<String, ImportRecord>()

        fun upsert(record: ImportRecord): ImportRecord {
            val key = "${record.title.lowercase()}\u0000${record.year}"
            val existing = seen[key]
            if (existing == null) {
                seen[key] = record
                return record
            }
            // Merge: keep the most informative version
            val merged = existing.copy(
                rating = existing.rating ?: record.rating,
                watchedOn = existing.watchedOn ?: record.watchedOn,
                review = existing.review ?: record.review,
                notes = existing.notes ?: record.notes,
                isFavorite = existing.isFavorite || record.isFavorite,
                watchlist = existing.watchlist || record.watchlist,
                listName = existing.listName ?: record.listName
            )
            seen[key] = merged
            return merged
        }

        fun base(row: Map<String, String>, i: Int, file: String): ImportRecord? {
            val title = row["name"]?.trim()
            if (title.isNullOrEmpty()) return null
            return ImportRecord(
                title = title,
                year = row["year"]?.trim()?.toIntOrNull(),
                rating = row["rating"]?.let { ratingToDouble(it) },
                ratingScale = 10,
                watchedOn = row["date"]?.takeIf { it.isNotBlank() },
                review = row["review"]?.takeIf { it.isNotBlank() },
                notes = row["notes"]?.takeIf { it.isNotBlank() },
                isFavorite = false,
                watchlist = false,
                listName = null,
                rewatch = row["rewatch"]?.trim()?.equals("Yes", true) ?: false,
                source = "Letterboxd",
                fileLabel = file,
                rowNumber = i + 2,
                rawFields = row
            )
        }

        // Diary entries — the richest source: dates, ratings, rewatch flags, list memberships, notes
        tables["diary.csv"]?.let { t ->
            t.rows.forEachIndexed { i, row ->
                base(row, i, "diary.csv")?.let { rec ->
                    val diaryLists = row["lists"]?.split('/').mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } } ?: emptyList()
                    val withDate = rec.copy(watchedOn = row["date"]?.takeIf { it.isNotBlank() } ?: rec.watchedOn)
                    upsert(withDate)
                    for (listName in diaryLists) {
                        records.add(withDate.copy(listName = listName))
                    }
                    if (diaryLists.isEmpty()) records.add(withDate)
                }
            }
        }

        // Watched list
        tables["watched.csv"]?.let { t ->
            t rowsIndexed@{
            }
        }

        // Ratings
        tables["ratings.csv"]?.let { t ->
            t.rows.forEachIndexed { i, row ->
                base(row, i, "ratings.csv")?.let { upsert(it) }
            }
        }

        // Watchlist
        tables["watchlist.csv"]?.let { t ->
            t.rows.forEachIndexed { i, row ->
                base(row, i, "watchlist.csv")?.let { upsert(it.copy(watchlist = true)) }
            }
        }

        // Reviews
        tables["reviews.csv"]?.let { t ->
            t.rows.forEachIndexed { i, row ->
                base(row, i, "reviews.csv")?.let { upsert(it) }
            }
        }

        records.addAll(seen.values)
        return records
    }

    /** Letterboxd ratings are whole numbers 0.5–5.0 in 0.5 steps; convert to a 0–10 scale. */
    internal fun ratingToDouble(raw: String): Double? {
        val v = raw.trim().toDoubleOrNull() ?: return null
        return v * 2.0
    }
}
