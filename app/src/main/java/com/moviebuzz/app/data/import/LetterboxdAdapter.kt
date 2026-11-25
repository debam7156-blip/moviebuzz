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
            it.equals("watched.csv", true) ||
            it.equals("watchlist.csv", true)
        }

    fun parse(tables: Map<String, CsvParser.CsvTable>): List<ImportRecord> {
        val records = mutableListOf<ImportRecord>()
        val merged = mutableMapOf<String, ImportRecord>()
        val order = mutableListOf<String>()

        fun upsert(record: ImportRecord): ImportRecord {
            val key = "${record.title.lowercase()}|${record.year}"
            val existing = merged[key]
            if (existing == null) {
                merged[key] = record
                order.add(key)
                return record
            }
            val m = existing.copy(
                rating = existing.rating ?: record.rating,
                watchedOn = existing.watchedOn ?: record.watchedOn,
                review = existing.review ?: record.review,
                notes = existing.notes ?: record.notes,
                isFavorite = existing.isFavorite || record.isFavorite,
                watchlist = existing.watchlist || record.watchlist,
                rewatch = existing.rewatch || record.rewatch
            )
            merged[key] = m
            return m
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
               boxd watchlist = false,
                listName = null,
                rewatch = row["rewatch"]?.trim()?.equals("Yes", true) ?: false,
                source = "Letterboxd",
                fileLabel = file,
                rowNumber = i + 2,
                rawFields = row
            )
        }

        // Diary — richest source
        tables["diary.csv"]?.let { t ->
            t.rows.forEachIndexed { i, row ->
                val rec = base(row, i, "diary.csv") ?: return@forEachIndexed
                val diaryLists = row["lists"]
                    ?.split('/')
                    ?.mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } }
                    ?: emptyList()
                upsert(rec)
                for (listName in diaryLists) {
                    records.add(rec.copy(listName = listName))
                }
            }
        }

        // Watched
        tables["watched.csv"]?.let { t ->
            t.rows.forEachIndexed { i, row ->
                base(row, i, "watched.csv")?.let { upsert(it) }
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

        // Custom lists (list-*.csv in the lists/ folder)
        tables.filterKeys { it.contains("list-") }.forEach { (file, t) ->
            val listName = file.substringAfterLast('/').removePrefix("list-").removeSuffix(".csv")
                .replace('-', ' ').trim()
            t.rows.forEachIndexed { i, row ->
                base(row, i, file)?.let { upsert(it.copy(listName = listName)) }
                records.add(base(row, i, file)?.copy(listName = listName) ?: return@forEachIndexed)
            }
        }

        records.addAll(order.map { merged[it]!! })
        return records
    }

    /** Letterboxd ratings are 0.5–5.0 in half-star steps; convert to 0–10. */
    internal fun ratingToDouble(raw: String): Double? {
        val v = raw.trim().toDoubleOrNull() ?: return null
        if (v <= 0.0 || v > 5.0) return null
        return v * 2.0
    }
}
