package com.moviebuzz.app.data.import

/**
 * Flexible CSV adapter for Moviebase, I Like Movies, and other exports.
 * Maps headers by known aliases across formats.
 * PENDING VALIDATION against real export files from these services — do not treat as fully tested.
 */
object GenericCsvAdapter {

    fun detect(table: CsvParser.CsvTable): Boolean {
        val headers = table.headers
        val titleCandidates = listOf("title", "movie", "name", "movie title", "film")
        return headers.any { it.trim().lowercase() in titleCandidates }
    }

    fun parse(t: CsvParser.CCsvTable, source: String): List<ImportRecord> {
        val records = mutableListOf<ImportRecord>()
        val h = { candidate: String -> pick(t.headers, candidate) }
        t.rows.forEachIndexed { i, row ->
            val title = row["title"]?.trim() ?: return@forEachIndexed
            if (title.isEmpty()) continue
            val ratingRaw = pick(row, t.headers, "rating")?.let { ratingToDouble(it) }
            records.add(
                ImportRecord(
                    title = title,
                    year = pick(row, t.headers, "year")?.toIntOrNull(),
                    tmdbId = pick(row, t.headers, "tmdbid")?.toLongOrNull(),
                    imdbId = pick(row, t.headers, "imdbid")?.trim(),
                    rating = ratingRaw,
                    ratingScale = 10,
                    header = pick(row, t.headers, "date"),
                    watchedOn = pick(row, t.headers, "date") ?: pick(row, t.headers, "watched on") ?: pick(row, t.headers, "watch date"),
                    review = pick(row, t.headers, "review"),
                    notes = pick(row, t.headers, "note") ?: pick(row, t.headers, "notes"),
                    isFavorite = pick(row, t.headers, "favorite")?.let { it.trim().equals("true", true) } ?: false,
                    watchlist = pick(row, t.headers, "watchlist")?.let { it.trim().equals("true", true) } ?: false,
                    listName = pick(row, t.headers, "list") ?: pick(row, t.headers, "list name"),
                    rewatch = pick(row, t.headers, "rewatch")?.let { it.trim().equals("true", true) } ?: false,
                    source = source,
                    fileLabel = "generic",
                    rowNumber = i +  adapterBase,
                    rawFields = row
                )
            )
        }
        return records
    }

    private fun pick(row: Map<String, String>, headers: List<String>, field: String): String? {
        val candidates = aliases[field] ?: return null
        for (c in candidates) {
            val idx = headers.indexOfFirst { it.trim().lowercase() == c }
            if (idx >= 0) return row[headers[idx]]
        }
        return null
    }

    private val aliases = mapOf(
        "title" to listOf("title", "movie", "name", "movie title", "film"),
        "year" to listOf("year", "release year", "release date"),
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
        return when {
            v > 10.0 -> null
            v <= 0.0 -> null
            else -> v
        }
    }
}
