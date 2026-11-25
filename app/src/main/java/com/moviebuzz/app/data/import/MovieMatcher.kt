package com.moviebuzz.app.data.import

import com.moviebuzz.app.data.db.MovieEntity

/**
 * Matches an import record to an existing movie or TMDB candidate.
 * Confidence-based; never fuzzy-guesses. If confident match not found,
 * the record goes to the manual resolution queue.
 */
class MovieMatcher {

    sealed interface MatchResult {
        data class Confident(
            val movie: MovieEntity,
            val confidence: Double,
            val basis: String
        ) : MatchResult

        data class NeedsReview(
            val candidates: List<MovieEntity>,
            val reason: String
        ) : MatchResult

        data class NotFound(val reason: String) : MatchResult
    }

    data class Lookup(
        val allMovies: List<MovieEntity>,
        val tmdbSearch: (suspend (title: String, year: Int?) -> List<MovieEntity>)? = null
    )

    suspend fun resolve(record: ImportRecord, lookup: Lookup): MatchResult {
        // 1. Strong identifiers first
        record.tmdbId?.let { id ->
            lookup.allMovies.firstOrNull { it.tmdbId == id }?.let {
                return MatchResult.Confident(it, 1.0, "TMDB ID match")
            }
        }
        record.imdbId?.let { id ->
            lookup.allMovies.firstOrNull { it.imdbId == id }?.let {
                return MatchResult.Confident(it, 1.0, "IMDb ID match")
            }
        }
        // 2. Exact title + year
        if (record.year != null) {
            val exact = lookup.allMovies.firstOrNull {
                it.title.equals(record.title, ignoreCase = true) && it.year == record.year
            }
            if (exact != null) return MatchResult.Confident(exact, 0.95, "Exact title and year")
        }
        // 3. TMDB remote search (if online)
        val remote = lookup.tmdbSearch?.invoke(record.title, record.year)
        if (!remote.isNullOrEmpty()) {
            val hit = remote.firstOrNull { it.year == record.year && it.title.equals(record.title, ignoreCase = true) }
            if (hit != null) return MatchResult.Confident(hit, 0.9, "TMDB search exact hit")
            if (remote.size == 1) return MatchResult.Confident(remote.first(), 0.75, "TMDB search single candidate")
        }
        // 4. Nothing confident: never guess on similar titles
        return MatchResult.NeedsReview(
            candidates = remote?.filter { candidatesCloseness(it, record) } ?: emptyList(),
            reason = "No ID match and no exact title+year match. Needs your review."
        )
    }

    private fun candidatesCloseness(movie: MovieEntity, record: ImportRecord): Boolean {
        val sameTitle = movie.title.equals(record.title, ignoreCase = true)
        val yearClose = record.year == null || movie.year == null ||
            kotlin.math.abs((movie.year ?: 0) - (record.year ?: 0)) <= 1
        return sameTitle && yearClose
    }
}
