package com.moviebuzz.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviebuzz.app.data.db.MovieDao
import com.moviebuzz.app.data.db.MovieEntity
import com.moviebuzz.app.data.tmdb.TmdbRepository
import com.moviebuzz.app.data.tmdb.friendlyTmdbError
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(
    private val tmdbRepository: TmdbRepository,
    private val movieDao: MovieDao
) : ViewModel() {

    data class HomeUi(
        val loading: Boolean = true,
        val trending: List<MovieEntity> = emptyList(),
        val popular: List<MovieEntity> = emptyList(),
        val upcoming: List<MovieEntity> = emptyList(),
        val recentlyReleased: List<MovieEntity> = emptyList(),
        val recommended: List<MovieEntity> = emptyList(),
        val error: String? = null
    )

    private val _ui = MutableStateFlow(HomeUi())
    val ui: StateFlow<HomeUi> = _ui

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                val deferred = listOf(
                    async { tmdbRepository.trending(1) },
                    async { tmdbRepository.popular(1) },
                    async { tmdbRepository.upcoming(1) },
                    async { tmdbRepository.nowPlaying(1) },
                    async { recommendations() }
                )
                val results = deferred.awaitAll()
                _ui.value = HomeUi(
                    loading = false,
                    trending = results[0],
                    popular = results[1],
                    upcoming = results[2],
                    recentlyReleased = results[3],
                    recommended = results[4]
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(
                    loading = false,
                    error = friendlyTmdbError(e)
                )
            }
        }
    }

    /** Recommendations based on the user's best-rated watched movie; falls back to trending. */
    private suspend fun recommendations(): List<MovieEntity> =
        try {
            val seed = movieDao.bestWatchedWithTmdbId()
            if (seed == null) {
                tmdbRepository.trending(2)
            } else {
                val recs = tmdbRepository.recommendedFor(seed.tmdbId!!)
                if (recs.isEmpty()) tmdbRepository.trending(2) else recs
            }
        } catch (e: Exception) {
            if (e is kotlinx.coroutines.CancellationException) throw e
            emptyList()
        }
}
