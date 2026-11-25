package com.moviebuzz.app.data.import

/**
 * Flexible CSV adapter for Moviebase, I Like Movies, and other exports.
 * Maps headers by known aliases across formats.
 * PENDING VALIDATION against real export files from these services — do not treat as fully tested.
 */
object GenericCsvAdapter {

    fun detect(table: CsvParser.CsvTable): Boolean {
        val titleCandidates = listOf("title", "movie", "name", "movie title", "film")
        return table.headers.any { it.trim().lowercase() in titleCandidates }
    }

    fun parse(t: CsvParser.CsvTable, source: String): List<ImportRecord> {
        val records = mutableListOf<ImportRecord>()
        t.rows.forEachIndexed { i, row ->
            val title = pick(row, t.headers, "title")?.trim()
            if (title.isNullOrEmpty()) return@forEachIndexed
            records.add(
                ImportRecord(
                    title = title,
                    year = pick(row, t.headers, "year")?.trim()?.toIntOrNull(),
                    tmdbId = pick(row, t.headers, "tmdbid")?.trim()?.toLongOrNull(),
                    imdbId = pick(row, t.headers, "imdbid")?.trim(),
                    rating = pick(row, t.headers, "rating")?.let { ratingToDouble(it) },
                    ratingScale = 10,
                    watchedOn = pick(row, t.headers, "date")?.takeIf { it.isNotBlank() },
                    review = pick(row, t.headers, "review")?.takeIf { it.isNotBlank() },
                    notes = pick(row, t.headers, "note")?.takeIf { it.isNotBlank() },
                    isFavorite = pick(row, t.headers, "favorite")?.let { it.trim().equals("true", true) } ?: false,
                    watchlist = pick(row, t.headers, "watchlist")?.let { it.trim().equals("true", true) } ?: false,
                    listName = pick(row, t.headers, "list")?.takeIf { it.isNotBlank() },
                    rewatch = pick(row, t.headers, "rewatch")?.let { it.trim().equals("true", true) } ?: false,
                    source = source,
                    fileLabel = "generic",
                    rowNumber = i + 2,
                    rawFields = row
                )
            )
        }
        return records
    }

    private fun pick(row: Map<String, String>, headers: List<String>, field: String): String? {
        val candidates = aliases[field] ?: return null
        for (c in candidates) {
            val key = headers.firstOrNull { it.trim().lowercase() == c }
            if (key != null) return row[key]
        }
        return null
    }

    private val aliases = mapOf(
        "title" to listOf("title", "movie", "name", "movie title", "film"),
        "year" to listOf("year", "release year"),
        "tmdbid" to listOf("tmdbid", "tmdb id", "tmdb_id"),
        "imdbid" to listOf("imdbid", "imdb id", "imdb_id"),
        "rating" to listOf("rating", "my rating", "personal rating", "score", "user rating"),
        "date" to listOf("date", "watched on", "watch date", "watched date", "viewing date", "watched at"),
        "review" to listOf("review", "my review", "comment", "comments"),
        "note" to listOf("note", "notes", "my notes", "personal note"),
        "favorite" to listOf("favorite", "favourite", "is favorite", "is favourite", "liked"),
        "watchlist" to listOf("watchlist", "in watchlist", "on watchlist"),
        "list" to listOf("list", "list name", "lists"),
        "rewatch" to listOf("rewatch", "is rewatch", "repeat viewing")
    )

    private fun ratingToDouble(raw: String): Double? {
        val v = raw.trim().toDoubleOrNull() ?: return null
        return if (v in 0.5..10.0) v else null
    }
}
