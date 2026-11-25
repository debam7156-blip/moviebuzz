package com.moviebuzz.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviebuzz.app.data.db.MovieDao
import com.moviebuzz.app.data.db.MovieEntity
import com.moviebuzz.app.data.db.WatchEventDao
import com.moviebuzz.app.data.db.WatchEventEntity
import com.moviebuzz.app.data.tmdb.TmdbRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.time.LocalDate

class MovieDetailViewModel(
    private val movieDao: MovieDao,
    private val watchEventDao: WatchEventDao,
    private val tmdbRepository: TmdbRepository?
) : ViewModel() {

    private val _movie = MutableStateFlow<MovieEntity?>(null)
    val movie: StateFlow<MovieEntity?> = _movie

    private val _watchHistory = MutableStateFlow<List<WatchEventEntity>>(emptyList())
    val watchHistory: StateFlow<List<WatchEventEntity>> = _watchHistory

    fun load(movieId: Long) {
        viewModelScope.launch {
            val local = movieDao.byId(movieId)
            _movie.value = local
            _watchHistory.value = watchEventDao.forMovie(movieId)
            val tmdbId = local?.tmdbId
            if (local != null && tmdbId != null && tmdbRepository != null) {
                tmdbRepository.movieDetailsCached(tmdbId)?.let { enriched ->
                    _movie.value = enriched
                }
            }
        }
    }

    fun markWatchedToday() {
        val m = _movie.value ?: return
        viewModelScope.launch {
            val today = LocalDate.now().toString()
            movieDao.markWatched(m.id, System.currentTimeMillis())
            if (watchEventDao.forMovie(m.id).none { it.watchedOn == today }) {
                watchEventDao.insert(
                    WatchEventEntity(movieId = m.id, watchedOn = today, ratingAtWatch = m.personalRating)
                )
            }
            _movie.value = movieDao.byId(m.id)
            _watchHistory.value = watchEventDao.forMovie(m.id)
        }
    }

    fun toggleFavorite() {
        val m = _movie.value ?: return
        viewModelScope.launch {
            movieDao.setFavorite(m.id, !m.isFavorite, System.currentTimeMillis())
            _movie.value = movieDao.byId(m.id)
        }
    }

    fun toggleWatchlist() {
        val m = _movie.value ?: return
        viewModelScope.launch {
            movieDao.setWatchlist(m.id, !m.watchlist, System.currentTimeMillis())
            _movie.value = movieDao.byId(m.id)
        }
    }

    fun setRating(rating: Double) {
        val m = _movie.value ?: return
        viewModelScope.launch {
            movieDao.setRating(m.id, rating, 10, System.currentTimeMillis())
            _movie.value = movieDao.byId(m.id)
        }
    }

    fun saveReview(review: String) {
        val m = _movie.value ?: return
        viewModelScope.launch {
            movieDao.setReview(m.id, review.ifBlank { null }, System.currentTimeMillis())
            _movie.value = movieDao.byId(m.id)
        }
    }
}
