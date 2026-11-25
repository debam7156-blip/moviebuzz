package com.moviebuzz.app.domain

/**
 * MOVIEBUZZ AI — on-device hybrid recommendation engine.
 * Every recommendation carries human-readable reasons derived from the user's
 * real history. Watched movies are always excluded. Sparse data yields empty
 * sections, never guesses.
 */
class RecommendationEngine(private val stats: StatsEngine = StatsEngine()) {

    data class Candidate(
        val id: Long,
        val title: String,
        val year: Int?,
        val genres: List<String>,
        val director: String?,
        val cast: List<String>,
        val countries: List<String>,
        val languages: List<String>,
        val tmdbVoteAverage: Double?,
        val tmdbVoteCount: Int?,
        val overview: String?
    )

    data class Recommendation(
        val candidate: Candidate,
        val score: Double,
        val reasons: List<String>
    ) {
        val explanation: String get() = reasons.joinToString("; ")
    }

    data class TasteProfile(
        val favouriteGenres: List<GenreStat>,
        val dislikedGenres: List<GenreStat>,
        val favouriteDirectors: List<PersonStat>,
        val favouriteActors: List<PersonStat>,
        val favouriteCountries: List<String>,
        val favouriteLanguages: List<String>,
        val highlyRated: List<MovieForStats>,
        val lowRated: List<MovieForStats>,
        val averageRating: Double?,
        val watchedIds: Set<Long>,
        val watchlistIds: Set<Long>
    )

    fun buildTasteProfile(snapshot: LibrarySnapshot, watchlistIds: Set<Long>): TasteProfile {
        val watched = snapshot.movies.filter { it.watched }
        val rated = watched.filter { it.personalRating != null }
        val avg = rated.map { it.personalRating!! }.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size }
        val highBar = (avg ?: 7.0) + 0.5
        val lowBar = (avg ?: 7.0) - 1.5
        val genreStats = stats.genreStats(snapshot)
        return TasteProfile(
            favouriteGenres = genreStats.filter { (it.averageRating ?: avg ?: 0.0) >= (avg ?: 0.0) }
                .sortedByDescending { it.count },
            dislikedGenres = genreStats.filter { it.averageRating != null && it.averageRating!! < lowBar }
                .sortedBy { it.averageRating },
            favouriteDirectors = stats.topDirectors(snapshot, 15),
            favouriteActors = stats.topActors(snapshot, 20),
            favouriteCountries = watched.flatMap { it.countries }.grouping().eachCount()
                .entries.sortedByDescending { it.value }.take(5).map { it.key },
            favouriteLanguages = watched.flatMap { it.languages }.grouping().eachCount()
                .entries.sortedByDescending { it.value }.take(5).map { it.key },
            highlyRated = rated.filter { it.personalRating!! >= highBar }.sortedByDescending { it.personalRating },
            lowRated = rated.filter { it.personalRating!! <= lowBar }.sortedBy { it.personalRating },
            averageRating = avg,
            watchedIds = watched.map { it.id }.toSet(),
            watchlistIds = watchlistIds
        )
    }

    fun madeForYou(profile: TasteProfile, candidates: List<Candidate>, limit: Int = 20): List<Recommendation> =
        rank(candidates, profile).filter { it.score > 0 }.take(limit)

    fun hiddenGems(profile: TasteProfile, candidates: List<Candidate>, limit: Int = 10): List<Recommendation> =
        rank(candidates, profile)
            .filter { (it.candidate.tmdbVoteCount ?: 0) in 100..2000 && (it.candidate.tmdbVoteAverage ?: 0.0) >= 6.5 && it.score > 0 }
            .take(limit)
            .map { it.copy(reasons = it.reasons + "Hidden gem: well-rated but underseen (${it.candidate.tmdbVoteCount} votes)") }

    fun similarToFavourites(profile: TasteProfile, candidates: List<Candidate>, limit: Int = 10): List<Recommendation> {
        val favGenreSet = profile.highlyRated.flatMap { it.genres }.toSet()
        val favDirectorSet = profile.highlyRated.mapNotNull { it.director }.filter { it.isNotBlank() }.toSet()
        val favActorSet = profile.highlyRated.flatMap { it.cast }.toSet()
        return candidates
            .map { c ->
                val genreOverlap = c.genres.count { it in favGenreSet }
                val directorHit = c.director != null && c.director in favDirectorSet
                val actorHits = c.cast.count { it in favActorSet }
                val score = genreOverlap * 1.0 + (if (directorHit) 2.0 else 0.0) + actorHits * 0.3
                val reasons = buildList {
                    if (genreOverlap >= 2) add("Shares $genreOverlap genres with your highest-rated films")
                    if (directorHit) add("Directed by ${c.director}, who made one of your favourites")
                    if (actorHits > 0) add("Features $actorHits actors you rated highly")
                }
                Recommendation(c, score, reasons)
            }
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .take(limit)
    }

    fun directorsToExplore(profile: TasteProfile, candidates: List<Candidate>, limit: Int = 10): List<Recommendation> {
        val knownDirectors = profile.favouriteDirectors.map { it.name }.toSet()
        return candidates
            .filter { !it.director.isNullOrBlank() && it.director !in knownDirectors }
            .groupBy { it.director!! }
            .map { (director, films) ->
                val best = films.maxByOrNull { it.tmdbVoteAverage ?: 0.0 }!!
                Recommendation(best, best.tmdbVoteAverage ?: 0.0, listOf("New director to explore: $director — start with ${best.title}"))
            }
            .sortedByDescending { it.score }
            .take(limit)
    }

    fun underrated(profile: TasteProfile, candidates: List<Candidate>, limit: Int = 10): List<Recommendation> =
        candidates
            .filter { (it.tmdbVoteAverage ?: 0.0) >= 6.8 && (it.tmdbVoteAverage ?: 0.0) <= 7.5 && (it.tmdbVoteCount ?: 0) < 500 }
            .map { Recommendation(it, it.tmdbVoteAverage ?: 0.0, listOf("Underrated: strong quality (${it.tmdbVoteAverage}) with few votes (${it.tmdbVoteCount})")) }
            .sortedByDescending { it.score }
            .take(limit)

    fun outsideComfortZone(profile: TasteProfile, candidates: List<Candidate>, limit: Int = 10): List<Recommendation> {
        val comfortable = profile.favouriteGenres.map { it.genre }.toSet()
        val disliked = profile.dislikedGenres.map { it.genre }.toSet()
        return candidates
            .filter { c -> c.genres.none { it in comfortable } && c.genres.none { it in disliked } }
            .filter { (it.tmdbVoteAverage ?: 0.0) >= 7.0 }
            .map { Recommendation(it, it.tmdbVoteAverage ?: 0.0, listOf("Outside your usual genres but critically strong (${it.tmdbVoteAverage})")) }
            .sortedByDescending { it.score }
            .take(limit)
    }

    fun nextWatch(profile: TasteProfile, candidates: List<Candidate>): Recommendation? =
        rank(candidates, profile).firstOrNull()

    private fun rank(candidates: List<Candidate>, profile: TasteProfile): List<Recommendation> {
        val excluded = profile.watchedIds
        val watchlist = profile.watchlistIds
        val favGenres = profile.favouriteGenres.associate { it.genre to it.count }
        val dislikedGenres = profile.dislikedGenres.map { it.genre }.toSet()
        val favDirectors = profile.favouriteDirectors.associate { it.name to it.count }
        val favActors = profile.favouriteActors.associate { it.name to it.count }
        val favCountries = profile.favouriteCountries.toSet()
        val favLanguages = profile.favouriteLanguages.toSet()
        val maxGenreCount = (favGenres.values.maxOrNull() ?: 1).toDouble()

        return candidates.asSequence()
            .filter { it.id !in excluded }
            .map { c ->
                val reasons = mutableListOf<String>()
                var score = 0.0
                val genreHits = c.genres.filter { it in favGenres }
                if (genreHits.isNotEmpty()) {
                    score += 3.0 * genreHits.sumOf { favGenres.getValue(it) } / maxGenreCount
                    reasons.add("Matches your favourite genres: ${genreHits.joinToString()}")
                }
                if (c.genres.any { it in dislikedGenres }) {
                    score -= 2.0
                    reasons.add("Note: contains a genre you tend to rate low")
                }
                if (c.director != null && c.director in favDirectors) {
                    score += 2.0 + favDirectors.getValue(c.director) * 0.1
                    reasons.add("Directed by ${c.director}, one of your most-watched directors")
                }
                val actorHits = c.cast.filter { it in favActors }
                if (actorHits.isNotEmpty()) {
                    score += actorHits.size * 0.4
                    reasons.add("Stars ${actorHits.take(2).joinToString()}, familiar favourites")
                }
                if (c.countries.any { it in favCountries }) {
                    score += 0.5
                    reasons.add("From a film industry you watch often")
                }
                if (c.languages.any { it in favLanguages }) score += 0.5
                val votes = c.tmdbVoteCount ?: 0
                val public = c.tmdbVoteAverage ?: 0.0
                if (votes >= 100) score += (public - 6.0).coerceAtLeast(0.0) * 0.3
                if (c.id in watchlist) {
                    score += 1.5
                    reasons.add("Already on your watchlist — high time!")
                }
                Recommendation(c, score, reasons)
            }
            .filter { it.score > 0 }
            .sortedByDescending { it.score }
            .toList()
    }

    fun itemItemSimilarity(a: Candidate, b: Candidate): Double {
        var s = 0.0
        if (a.genres.isNotEmpty()) s += 2.0 * a.genres.count { it in b.genres } / a.genres.size
        if (a.director != null && a.director == b.director) s += 1.0
        s += 0.5 * a.cast.count { it in b.cast } / 10.0
        return s
    }
}
