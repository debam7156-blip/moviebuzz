package com.moviebuzz.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviebuzz.app.data.db.MovieBuzzDatabase
import com.moviebuzz.app.domain.GenreStat
import com.moviebuzz.app.domain.LibrarySnapshot
import com.moviebuzz.app.domain.MovieForStats
import com.moviebuzz.app.domain.PersonStat
import com.moviebuzz.app.domain.StatsEngine
import com.moviebuzz.app.domain.WatchEventForStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class StatsViewModel(private val db: MovieBuzzDatabase) : ViewModel() {

    data class StatsUi(
        val totalMovies: Int = 0,
        val totalSessions: Int = 0,
        val totalHours: Double = 0.0,
        val averageRating: Double? = null,
        val longestStreak: Int = 0,
        val currentStreak: Int = 0,
        val favouriteCount: Int = 0,
        val genres: List<GenreStat> = emptyList(),
        val topDirectors: List<PersonStat> = emptyList(),
        val topActors: List<PersonStat> = emptyList(),
        val rewatched: List<Pair<MovieForStats, Int>> = emptyList(),
        val ratingBuckets: Map<Int, Int> = emptyMap()
    )

    private val engine = StatsEngine()
    private val _stats = MutableStateFlow(StatsUi())
    val stats: StateFlow<StatsUi> = _stats

    init {
        viewModelScope.launch {
            db.movieDao().observeAll().collect { movies ->
                val events = db.watchEventDao().observeAllSnapshot()
                val snapshot = LibrarySnapshot(
                    movies = movies.map {
                        MovieForStats(
                            id = it.id, title = it.title, year = it.year,
                            runtimeMinutes = it.runtimeMinutes, director = it.director,
                            genres = it.genres, countries = it.countries,
                            languages = it.languages, cast = it.cast,
                            personalRating = it.personalRating,
                            tmdbVoteAverage = it.tmdbVoteAverage,
                            isFavorite = it.isFavorite, watched = it.watched
                        )
                    },
                    watchEvents = events.map { WatchEventForStats(it.movieId, it.watchedOn, null) }
                )
                _stats.value = StatsUi(
                    totalMovies = engine.totalMoviesWatched(snapshot),
                    totalSessions = engine.totalViewingSessions(snapshot),
                    totalHours = engine.totalWatchHours(snapshot),
                    averageRating = engine.averagePersonalRating(snapshot),
                    longestStreak = engine.longestStreak(snapshot),
                    currentStreak = engine.currentStreak(snapshot),
                    favouriteCount = engine.favouriteMovies(snapshot).size,
                    genres = engine.genreStats(snapshot).take(8),
                    topDirectors = engine.topDirectors(snapshot, 5),
                    topActors = engine.topActors(snapshot, 5),
                    rewatched = engine.mostRewatched(snapshot, 5),
                    ratingBuckets = engine.ratingDistribution(snapshot)
                )
            }
        }
    }
}
