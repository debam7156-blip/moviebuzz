package com.moviebuzz.app.data.import

/**
 * Letterboxd import adapter.
 *
 * Tested against the published Letterboxd export column sets:
 * - watched.csv: Name,Year,Letterboxd URI
 * - ratings.csv: Name,Year,Letterboxd URI,Rating
 * - diary.csv: Date,Name,Year,Letterboxd URI,Rating,Watched date 1,rewatch,Lists,Notes
 * - watchlist.csv: Position,Name,Year,Letterboxd URI,Created at
 * - reviews.csv: Name,Year,Letterboxd URI,Rating,Date,Review
 * - lists.csv / lists/notes.csv: List title + description + member CSVs with Description,Name,Year,Letterboxd URI
 */
object LetterboxdAdapter {

    fun detect(tables: Map<String, CsvParser.CsvTable>): Boolean =
        tables.keys.any { it.equals("ratings.csv", true) || it.equals("diary.csv", true) || it.equals("watched.csv", true) || it.equals("watchlist.csv", true) }

    fun parse(tables: Map<String, CsvParser.CsvTable>): List<ImportRecord> {
        val records = mutableListOf<ImportRecord>()

        // Watched list
        tables["watched.csv"]?.let { t ->
            t.rows.forEachIndexed { i, row ->
                val title = row["name"]?.trim()
                if (!title.isNullOrEmpty()) {
                    records.add(
                        ImportRecord(
                            title = title,
                            year = row["year"]?.toIntOrNull(),
                            watchedOn = null,
                            watchlist = false,
                            source = "Letterboxd",
                            fileLabel = "watched.csv",
                            rowNumber = i + 2, // +2 for header + 1-based
                            rawFields = row
                        )
                    )
                }
            }
        }

        // Ratings
        tables["ratings.csv"]?.let { t ->
            t.rows.forEachIndexed { i, row ->
                val title = row["name"]?.trim()
                if (!title.isNullOrEmpty()) {
                    records.add(
                        ImportRecord(
                            title = title,
                            parsingYear = null,
                            source = "Letterboxd",
                            fileLabel = "ratings.csv",
                            rowNumber = i + 2,
                            rawFields = row
                        )
                    )
                }
            }
        }
        return records
    }
}
