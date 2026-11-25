package com.moviebuzz.app.data.import

/**
 * A single normalized record from any import source, before matching.
 * Unknown fields stay null. The engine never discards a record because a field is missing.
 */
data class ImportRecord(
    val title: String,
    val year: Int? = null,
    val tmdbId: Long? = null,
    val imdbId: String? = null,
    val rating: Double? = null,
    val ratingScale: Int = 10,
    val watchedOn: String? = null,
    val review: String? = null,
    val notes: String? = null,
    val isFavorite: Boolean = false,
    val watchlist: Boolean = false,
    val listName: String? = null,
    val rewatch: Boolean = false,
    val source: String,
    val fileLabel: String,
    val rowNumber: Int,
    val rawFields: Map<String, String> = emptyMap()
)
