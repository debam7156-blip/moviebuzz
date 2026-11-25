package com.moviebuzz.app.domain

/**
 * Pure-Kotlin statistics engine. All computations run over a snapshot of the
 * user's real data — never fabricated, never estimated. Missing values are
 * excluded from the metrics they don't apply to, never counted as zero.
 */

data class LibrarySnapshot(
    val movies: List<MovieForStats>,
    val watchEvents: List<WatchEventForStats>
)

data class MovieForStats(
    val id: Long,
    val title: String,
    val year: Int?,
    val runtimeMinutes: Int?,
    val director: String?,
    val genres: List<String>,
    val countries: List<String>,
    val languages: List<String>,
    val cast: List<String>,
    val personalRating: Double?,
    val tmdbVoteAverage: Double?,
    val isFavorite: Boolean,
    val watched: Boolean
)

data class WatchEventForStats(
    val movieId: Long,
    val watchedOn: String,
    val runtimeMinutes: Int?
)

data class GenreStat(val genre: String, val count: Int, val share: Double, val averageRating: Double?)
data class PersonStat(val name: String, val count: Int, val averageRating: Double?)
data class YearStat(val year: Int, val movies: Int, val sessions: Int, val averageRating: Double?)
data class MonthStat(val year: Int, val month: Int, val sessions: Int)

class StatsEngine {

    private fun watchedMovies(s: LibrarySnapshot): List<MovieForStats> = s.movies.filter { it.watched }

    // ---------- GENERAL ----------

    fun totalMoviesWatched(s: LibrarySnapshot): Int = watchedMovies(s).size

    fun totalViewingSessions(s: LibrarySnapshot): Int = s.watchEvents.size

    fun totalWatchHours(s: LibrarySnapshot): Double {
        val runtimeById = s.movies.associate { it.id to it.runtimeMinutes }
        var minutes = 0L
        for (event in s.watchEvents) {
            val runtime = event.runtimeMinutes ?: runtimeById[event.movieId]
            if (runtime != null) minutes += runtime
        }
        return minutes / 60.0
    }

    fun averagePersonalRating(s: LibrarySnapshot): Double? {
        val ratings = watchedMovies(s).mapNotNull { it.personalRating }
        if (ratings.isEmpty()) return null
        return ratings.sum() / ratings.size
    }

    fun favouriteMovies(s: LibrarySnapshot): List<MovieForStats> = watchedMovies(s).filter { it.isFavorite }

    fun mostRewatched(s: LibrarySnapshot, limit: Int = 10): List<Pair<MovieForStats, Int>> =
        s.watchEvents.groupBy { it.movieId }
            .map { (movieId, events) -> movieId to events.size }
            .filter { it.second >= 2 }
            .sortedByDescending { it.second }
            .take(limit)
            .mapNotNull { (id, count) -> watchedMovies(s).firstOrNull { it.id == id }?.let { it to count } }

    // ---------- GENRES ----------

    fun genreStats(s: LibrarySnapshot): List<GenreStat> {
        val watched = watchedMovies(s)
        if (watched.isEmpty()) return emptyList()
        val counts = watched.flatMap { it.genres }.grouping().eachCount()
        val n = watched.size
        return counts.entries
            .map { (genre, count) ->
                val rated = watched.filter { genre in it.genres }.mapNotNull { it.personalRating }
                GenreStat(
                    genre = genre,
                    count = count,
                    share = count.toDouble() / n,
                    averageRating = if (rated.isEmpty()) null else rated.sum() / rated.size
                )
            }
            .sortedByDescending { it.count }
    }

    fun genrePreferencesByYear(s: LibrarySnapshot): Map<Int, List<GenreStat>> =
        watchedMovies(s)
            .filter { it.year != null }
            .groupBy { it.year!! }
            .mapValues { (_, moviesInYear) ->
                val counts = moviesInYear.flatMap { it.genres }.grouping().eachCount()
                counts.entries
                    .map { (genre, count) ->
                        val rated = moviesInYear.filter { genre in it.genres }.mapNotNull { it.personalRating }
                        GenreStat(genre, count, count.toDouble() / moviesInYear.size, rated.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size })
                    }
                    .sortedByDescending { it.count }
            }

    // ---------- TIME ----------

    private fun parseYear(date: String): Int? = date.takeIf { it.length >= 4 }?.take(4)?.toIntOrNull()

    fun monthlyStats(s: LibrarySnapshot): List<MonthStat> =
        s.watchEvents
            .filter { it.watchedOn.length >= 7 }
            .groupBy { Pair(it.watchedOn.take(4).toIntOrNull() ?: 0, it.watchedOn.substring(5, 7).toIntOrNull() ?: 0) }
            .map { (key, events) -> MonthStat(key.first, key.second, events.size) }
            .sortedWith(compareBy({ it.year }, { it.month }))

    fun yearlyStats(s: LibrarySnapshot): List<YearStat> {
        val watched = watchedMovies(s)
        return watched.groupBy { it.year }
            .filterKeys { it != null }
            .map { (year, moviesInYear) ->
                val sessions = s.watchEvents.count { parseYear(it.watchedOn) == year }
                val rated = moviesInYear.mapNotNull { it.personalRating }
                YearStat(
                    year = year!!,
                    movies = moviesInYear.size,
                    sessions = sessions,
                    averageRating = if (rated.isEmpty()) null else rated.sum() / rated.size
                )
            }
            .sortedBy { it.year }
    }

    /** Activity calendar: date -> number of viewing sessions that day. */
    fun activityCalendar(s: LibrarySnapshot): Map<String, Int> =
        s.watchEvents.groupBy { it.watchedOn }.mapValues { it.value.size }

    /** Longest run of consecutive days with at least one viewing. */
    fun longestStreak(s: LibrarySnapshot): Int {
        val dates = s.watchEvents.mapNotNull { it.watchedOn.takeIf { d -> d.length == 10 } }
            .map { java.time.LocalDate.parse(it) }
            .distinct()
            .sorted()
        if (dates.isEmpty()) return 0
        var best = 1
        var current = 1
        for (i in 1 until dates.size) {
            current = if (dates[i - 1].plusDays(1) == dates[i]) current + 1 else 1
            if (current > best) best = current
        }
        return best
    }

    /** Consecutive-day streak ending today or yesterday (0 if none). */
    fun currentStreak(s: LibrarySnapshot, today: java.time.LocalDate = java.time.LocalDate.now()): Int {
        val dates = s.watchEvents.mapNotNull { it.watchedOn.takeIf { d -> d.length == 10 } }
            .map { java.time.LocalDate.parse(it) }
            .distinct()
            .toSet()
        if (dates.isEmpty()) return 0
        val yesterday = today.minusDays(1)
        if (today !in dates && yesterday !in dates) return 0
        var cursor = if (today in dates) today else yesterday
        var streak = 0
        while (cursor in dates) {
            streak++
            cursor = cursor.minusDays(1)
        }
        return streak
    }

    // ---------- RATINGS ----------

    /**
     * Distribution of personal ratings bucketed to whole stars on a 5-star scale.
     * Ratings are stored on a 0-10 scale, so a star bucket = round(rating / 2).
     */
    fun ratingDistribution(s: LibrarySnapshot): Map<Int, Int> {
        val buckets = watchedMovies(s)
            .mapNotNull { it.personalRating }
            .grouping()
            .eachCount()
            .mapKeys { bucketToStar(it.key) }
        return (0..5).associateWith { star -> buckets[star] ?: 0 }
    }

    private fun bucketToStar(rating: Double): Int =
        kotlin.math.round(rating / 2.0).toInt().coerceIn(0, 5)

    fun averageRatingByYear(s: LibrarySnapshot): List<YearStat> = yearlyStats(s)

    fun personalVsPublicRatings(s: LibrarySnapshot): List<Pair<MovieForStats, Double>> =
        watchedMovies(s)
            .filter { it.personalRating != null && it.tmdbVoteAverage != null }
            .map { it to (it.personalRating!! - it.tmdbVoteAverage!!) }

    // ---------- PEOPLE ----------

    fun topDirectors(s: LibrarySnapshot, limit: Int = 10): List<PersonStat> {
        val watched = watchedMovies(s).filter { !it.director.isNullOrBlank() }
        val byPerson = LinkedHashMap<String, MutableList<Double?>>()
        for (movie in watched) {
            for (director in movie.director!!.split(',', ';').map { it.trim() }.filter { it.isNotBlank() }) {
                byPerson.getOrPut(director) { mutableListOf() }.add(movie.personalRating)
            }
        }
        return byPerson.entries
            .map { (name, ratings) ->
                val present = ratings.filterNotNull()
                PersonStat(
                    name = name,
                    count = ratings.size,
                    averageRating = present.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size }
                )
            }
            .sortedWith(compareByDescending<PersonStat> { it.count }.thenByDescending { it.averageRating ?: -1.0 })
            .take(limit)
    }

    fun topActors(s: LibrarySnapshot, limit: Int = 10): List<PersonStat> {
        val watched = watchedMovies(s)
        val byPerson = LinkedHashMap<String, MutableList<Double?>>()
        for (movie in watched) {
            for (actor in movie.cast.take(10)) {
                byPerson.getOrPut(actor.trim()) { mutableListOf() }.add(movie.personalRating)
            }
        }
        return byPerson.entries
            .map { (name, ratings) ->
                val present = ratings.filterNotNull()
                PersonStat(
                    name = name,
                    count = ratings.size,
                    averageRating = present.takeIf { it.isNotEmpty() }?.let { it.sum() / it.size }
                )
            }
            .sortedWith(compareByDescending<PersonStat> { it.count }.thenByDescending { it.averageRating ?: -1.0 })
            .take(limit)
    }

    fun favouriteIndustries(s: LibrarySnapshot, limit: Int = 10): List<Pair<String, Int>> =
        watchedMovies(s).flatMap { it.countries }.grouping().eachCount()
            .entries.sortedByDescending { it.value }.take(limit).map { it.key to it.value }
}
