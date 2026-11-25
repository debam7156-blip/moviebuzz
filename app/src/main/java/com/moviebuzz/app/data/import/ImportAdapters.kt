package com.moviebuzz.app.data.import

import java.io.InputStream

/**
 * Import adapters. Each adapter converts a real export format into normalized
 * [ImportRecord]s. Nothing is dropped: rows that fail to parse become
 * UnparsedRow results which the engine records as failures with row context.
 */
object ImportAdapters {

    sealed interface AdapterResult
    data class Parsed(val record: ImportRecord) : AdapterResult
    data class UnparsedRow(val rowNumber: Int, val reason: String, val raw: Map<String, String>) : AdapterResult
    data class FileError(val fileName: String, val reason: String) : AdapterResult

    /**
     * Letterboxd official ZIP export. Real files (as documented by Letterboxd):
     * - watched.csv: Name, Year, Letterboxd URI, Created At
     * - ratings.csv: Name, Year, Letterboxd URI, Rating, Created At
     * - diary.csv: Date, Name, Year, Letterboxd URI, Rating, Rewatch, Tags, Watched Date, Created At
     * - watchlist.csv: Name, Year, Letterboxd URI, Created At
     * - reviews.csv: Name, Year, Letterboxd URI, Rating, Review, Tags, Watched Date, Created At
     * - lists.csv: Description, ListName, Tags, Created At, Updated At, ListComments
     * - list-details-*.csv: Position, Name, Year, Letterboxd URI
     * The Letterboxd URI contains the TMDB id (e.g. .../tmdb/12345) when present.
     */
    object Letterboxd {

        private val TmdbUriRegex = Regex("""/tmdb/(\d+)""")
        private val YearRegex = Regex("""^(\d{4})""")

        fun fromZip(stream: InputStream): List<AdapterResult> {
            val results = mutableListOf<AdapterResult>()
            val entries = try {
                ZipExtractor.extractCsvEntries(stream)
            } catch (e: Exception) {
                return listOf(FileError("zip", "Could not read archive: ${e.message}"))
            }
            if (entries.isEmpty()) return listOf(FileError("zip", "No CSV files found in archive"))

            // Resolve list names from lists.csv so list-details files carry real names.
            val listsCsv = entries.firstOrNull { it.entryName.substringAfterLast('/').equals("lists.csv", true) }
            val slugToName = mutableMapOf<String, String>()
            if (listsCsv != null) {
                try {
                    val table = CsvParser.parse(listsCsv.bytes.inputStream())
                    for (row in table.rows) {
                        val name = row["name"] ?: row["listname"] ?: continue
                        val slug = slugify(name)
                        if (slug.isNotBlank()) slugToName[slug] = name
                    }
                } catch (_: Exception) {
                    // lists.csv unreadable: fall back to slug-derived names
                }
            }

            for (entry in entries) {
                val label = entry.entryName.substringAfterLast('/')
                when {
                    label.equals("watched.csv", true) -> results += parseSimple(entry.bytes, label, watched = true)
                    label.equals("ratings.csv", true) -> results += parseSimple(entry.bytes, label, watched = true)
                    label.equals("watchlist.csv", true) -> results += parseSimple(entry.bytes, label, watchlist = true)
                    label.startsWith("diary", true) -> results += parseDiary(entry.bytes, label)
                    label.startsWith("reviews", true) -> results += parseReviews(entry.bytes, label)
                    label.startsWith("list-details", true) -> {
                        val slug = label.removePrefix("list-details-").removeSuffix(".csv")
                        val listName = slugToName[slug]
                            ?: slug.replace('-', ' ').replaceFirstChar { it.uppercase() }
                        results += parseListDetails(entry.bytes, label, listName)
                    }
                    // lists.csv is metadata-only; names handled above.
                    else -> {}
                }
            }
            return results
        }

        private fun parseSimple(bytes: ByteArray, label: String, watched: Boolean = false, watchlist: Boolean = false): List<AdapterResult> =
            try {
                CsvParser.parse(bytes.inputStream()).rows.mapIndexed { i, row ->
                    rowToRecord(row, i + 2, label, watched = watched, watchlist = watchlist)
                }
            } catch (e: Exception) {
                listOf(FileError(label, "Could not parse: ${e.message}"))
            }

        private fun parseDiary(bytes: ByteArray, label: String): List<AdapterResult> =
            try {
                CsvParser.parse(bytes.inputStream()).rows.mapIndexed { i, row ->
                    rowToRecord(
                        row, i + 2, label,
                        watched = true,
                        watchedOn = row["date"] ?: row["watched date"],
                        rewatch = row["rewatch"]?.trim()?.equals("yes", true) == true,
                        rating = parseStarRating(row["rating"]),
                        notes = row["tags"]?.takeIf { it.isNotBlank() }?.let { "Imported tags: $it" }
                    )
                }
            } catch (e: Exception) {
                listOf(FileError(label, "Could not parse: ${e.message}"))
            }

        private fun parseReviews(bytes: ByteArray, label: String): List<AdapterResult> =
            try {
                CsvParser.parse(bytes.inputStream()).rows.mapIndexed { i, row ->
                    rowToRecord(
                        row, i + 2, label,
                        watched = true,
                        review = row["review"],
                        watchedOn = row["watched date"],
                        rating = parseStarRating(row["rating"])
                    )
                }
            } catch (e: Exception) {
                listOf(FileError(label, "Could not parse: ${e.message}"))
            }

        private fun parseListDetails(bytes: ByteArray, label: String, listName: String): List<AdapterResult> =
            try {
                CsvParser.parse(bytes.inputStream()).rows.mapIndexed { i, row ->
                    rowToRecord(row, i + 2, label, listName = listName)
                }
            } catch (e: Exception) {
                listOf(FileError(label, "Could not parse: ${e.message}"))
            }

        private fun rowToRecord(
            row: Map<String, String>,
            rowNumber: Int,
            label: String,
            watched: Boolean = false,
            watchlist: Boolean = false,
            watchedOn: String? = null,
            rewatch: Boolean = false,
            review: String? = null,
            rating: Double? = null,
            notes: String? = null,
            listName: String? = null
        ): AdapterResult {
            val title = (row["name"] ?: row["title"]).orBlankTrim()
            if (title.isBlank()) return UnparsedRow(rowNumber, "Missing title", row)
            val year = row["year"]?.trim()?.let { YearRegex.find(it)?.groupValues?.get(1)?.toIntOrNull() }
            val uri = row["letterboxd uri"] ?: row["uri"] ?: ""
            val tmdbId = TmdbUriRegex.find(uri)?.groupValues?.get(1)?.toLongOrNull()
            return Parsed(
                ImportRecord(
                    title = title,
                    year = year,
                    tmdbId = tmdbId,
                    rating = rating ?: parseStarRating(row["rating"]),
                    ratingScale = 10,
                    watchedOn = watchedOn,
                    review = review,
                    notes = notes,
                    watchlist = watchlist,
                    listName = listName,
                    rewatch = rewatch,
                    source = "letterboxd",
                    fileLabel = label,
                    rowNumber = rowNumber,
                    rawFields = row
                )
            )
        }

        /** Letterboxd ratings are 5-star in half-star steps; returns 0-10 scale. */
        private fun parseStarRating(raw: String?): Double? =
            raw?.trim()?.toDoubleOrNull()?.let { it * 2.0 }

        private fun slugify(name: String): String =
            name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    }

    /**
     * Flexible CSV adapter for Moviebase, "I Like Movies" and generic exports.
     * Maps common header spellings to normalized records.
     * Rating scale is auto-detected (5-point, 10-point, percentage, x/y).
     */
    object FlexibleCsv {

        private val TITLE_HEADERS = listOf("title", "name", "movie", "movie title", "movie name")
        private val YEAR_HEADERS = listOf("year", "release year", "release date", "released", "releasedate", "year released")
        private val RATING_HEADERS = listOf("rating", "my rating", "user rating", "personal rating", "your rating", "score", "stars")
        private val DATE_HEADERS = listOf("watched date", "watch date", "date watched", "watched on", "viewed", "last watched", "date", "watched")
        private val REVIEW_HEADERS = listOf("review", "comment", "comments", "notes", "my review")
        private val FAVOURITE_HEADERS = listOf("favorite", "favourite", "favorites", "favourites", "liked")
        private val WATCHLIST_HEADERS = listOf("watchlist", "in watchlist", "want to watch", "planned", "to watch")
        private val TMDB_HEADERS = listOf("tmdb", "tmdbid", "tmdb id", "tmdb_id", "themoviedb", "themoviedb id", "moviedb id")
        private val IMDB_HEADERS = listOf("imdb", "imdbid", "imdb id", "imdb_id", "imdb code")
        private val LIST_HEADERS = listOf("list", "list name", "lists", "custom list", "collection")
        private val REWATCH_HEADERS = listOf("rewatch", "rewatched", "re-watch", "times watched", "watch count")
        private val DIRECTOR_HEADERS = listOf("director", "directors", "directed by")

        fun detectSource(headers: List<String>): String {
            val h = headers.map { it.trim().lowercase() }
            return when {
                h.any { it.contains("letterboxd") } -> "letterboxd"
                h.any { it.contains("moviebase") } -> "moviebase"
                h.any { it.contains("i like movies") || it == "ilm" } -> "ilikemovies"
                else -> "generic"
            }
        }

        fun fromCsv(bytes: ByteArray, fileName: String): List<AdapterResult> =
            try {
                val table = CsvParser.parse(bytes.inputStream())
                val headers = table.headers.map { it.trim().lowercase() }
                val source = detectSource(headers)
                if (findColumn(headers, TITLE_HEADERS) == null) {
                    return listOf(FileError(fileName, "No recognizable title column. Found: ${headers.joinToString()}"))
                }
                table.rows.mapIndexed { i, row -> rowToRecord(row, headers, i + 2, source, fileName) }
            } catch (e: Exception) {
                listOf(FileError(fileName, "Could not parse: ${e.message}"))
            }

        private fun rowToRecord(row: Map<String, String>, headers: List<String>, rowNumber: Int, source: String, fileName: String): AdapterResult {
            val title = findValue(row, headers, TITLE_HEADERS).orBlankTrim()
            if (title.isBlank()) return UnparsedRow(rowNumber, "Missing title", row)
            val (rating, scale) = normalizeRating(findValue(row, headers, RATING_HEADERS))
            val yearStr = findValue(row, headers, YEAR_HEADERS)
            val year = Regex("\\d{4}").find(yearStr ?: "")?.value?.toIntOrNull()
            val timesWatched = findValue(row, headers, REWATCH_HEADERS)?.trim()?.toIntOrNull() ?: 1
            return Parsed(
                ImportRecord(
                    title = title,
                    year = year,
                    tmdbId = findValue(row, headers, TMDB_HEADERS)?.extractLong(),
                    imdbId = findValue(row, headers, IMDB_HEADERS)?.trim()?.ifBlank { null },
                    rating = rating,
                    ratingScale = scale,
                    watchedOn = findValue(row, headers, DATE_HEADERS)?.trim()?.ifBlank { null },
                    review = findValue(row, headers, REVIEW_HEADERS)?.trim()?.ifBlank { null },
                    isFavorite = parseBoolean(findValue(row, headers, FAVOURITE_HEADERS)),
                    watchlist = parseBoolean(findValue(row, headers, WATCHLIST_HEADERS)),
                    listName = findValue(row, headers, LIST_HEADERS)?.trim()?.ifBlank { null },
                    rewatch = timesWatched > 1 || parseBoolean(findValue(row, headers, listOf("rewatch", "rewatched", "re-watch"))),
                    source = source,
                    fileLabel = fileName,
                    rowNumber = rowNumber,
                    rawFields = row
                )
            )
        }

        private fun findColumn(headers: List<String>, candidates: List<String>): String? =
            headers.firstOrNull { it in candidates } ?: headers.firstOrNull { h -> candidates.any { h.contains(it) } }

        private fun findValue(row: Map<String, String>, headers: List<String>, candidates: List<String>): String? =
            findColumn(headers, candidates)?.let { row[it] }

        /** Returns rating normalized to a 0-10 scale. */
        fun normalizeRating(raw: String?): Pair<Double?, Int> {
            val s = raw?.trim() ?: return null to 10
            if (s.isBlank()) return null to 10
            if (s.endsWith("%")) {
                val v = s.removeSuffix("%").trim().toDoubleOrNull()
                if (v != null && v in 0.0..100.0) return (v / 10.0) to 10
            }
            val slash = Regex("^(\\d+(?:\\.\\d+)?)\\s*/\\s*(\\d+)$").find(s)
            if (slash != null) {
                val value = slash.groupValues[1].toDoubleOrNull()
                val max = slash.groupValues[2].toIntOrNull() ?: 10
                if (value != null && max > 0) return (value / max * 10.0) to 10
            }
            val v = s.toDoubleOrNull() ?: return null to 10
            return when {
                v > 10.0 && v <= 100.0 -> (v / 10.0) to 10
                v in 0.0..10.0 -> v to 10
                else -> null to 10
            }
        }

        private fun parseBoolean(raw: String?): Boolean =
            raw?.trim()?.lowercase() in setOf("yes", "true", "1", "y", "watched", "favorite", "favourite")

        private fun String.extractLong(): Long? = Regex("\\d+").find(this)?.value?.toLongOrNull()
    }

    private fun String?.orBlankTrim(): String = this?.trim() ?: ""
}
