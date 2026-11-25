package com.moviebuzz.app.data.import

/**
 * Legacy standalone Letterboxd adapter.
 * Superseded by ImportAdapters.Letterboxd, which handles the ZIP archive
 * directly (watched/ratings/diary/watchlist/reviews/custom lists).
 * Kept only as a thin pointer to avoid duplicate symbol errors.
 */
@Deprecated("Use ImportAdapters.Letterboxd instead")
object LetterboxdAdapter {
    fun detect(tables: Map<String, CsvParser.CsvTable>): Boolean =
        tables.isNotEmpty()
}
