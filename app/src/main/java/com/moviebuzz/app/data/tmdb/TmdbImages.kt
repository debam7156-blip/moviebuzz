package com.moviebuzz.app.data.tmdb

/**
 * TMDB image URL builder. Poster/backdrop/profile paths in TMDB payloads are
 * partial paths (e.g. "/abc123.jpg"); these helpers turn them into full URLs.
 * Returns null when the path is missing so Coil can show a placeholder instead.
 */
object TmdbImages {

    private const val IMAGE_BASE = "https://image.tmdb.org/t/p/"

    fun posterUrl(path: String?, size: String = "w342"): String? =
        path?.takeIf { it.isNotBlank() }?.let { IMAGE_BASE + size + it }

    fun backdropUrl(path: String?, size: String = "w780"): String? =
        path?.takeIf { it.isNotBlank() }?.let { IMAGE_BASE + size + it }

    fun profileUrl(path: String?, size: String = "w185"): String? =
        path?.takeIf { it.isNotBlank() }?.let { IMAGE_BASE + size + it }
}
