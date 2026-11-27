package com.moviebuzz.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviebuzz.app.data.db.CustomListDao
import com.moviebuzz.app.data.db.CustomListEntity
import com.moviebuzz.app.data.db.CustomListEntryEntity
import com.moviebuzz.app.data.db.MovieDao
import com.moviebuzz.app.data.db.MovieEntity
import com.moviebuzz.app.data.db.WatchEventDao
import com.moviebuzz.app.data.db.WatchEventEntity
import com.moviebuzz.app.data.tmdb.TmdbRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class MovieDetailViewModel(
    private val movieDao: MovieDao,
    private val watchEventDao: WatchEventDao,
    private val customListDao: CustomListDao,
    private val tmdbRepository: TmdbRepository
) : ViewModel() {

    data class DetailUi(
        val movie: MovieEntity? = null,
        val loading: Boolean = true,
        val notFound: Boolean = false,
        val offlineNotice: String? = null,
        val similar: List<MovieEntity> = emptyList(),
        val recommended: List<MovieEntity> = emptyList()
    )

    private val _ui = MutableStateFlow(DetailUi())
    val ui: StateFlow<DetailUi> = _ui

    private val _watchHistory = MutableStateFlow<List<WatchEventEntity>>(emptyList())
    val watchHistory: StateFlow<List<WatchEventEntity>> = _watchHistory

    private val _memberListIds = MutableStateFlow<List<Long>>(emptyList())
    val memberListIds: StateFlow<List<Long>> = _memberListIds

    val collections: StateFlow<List<CustomListEntity>> = customListDao.observeLists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private var movieId: Long = -1
    private var membershipJob: Job? = null

    fun load(id: Long) {
        movieId = id
        viewModelScope.launch {
            _ui.value = DetailUi(loading = true)
            val local = movieDao.byId(id)
            if (local == null) {
                _ui.value = DetailUi(loading = false, notFound = true)
                return@launch
            }
            _ui.value = DetailUi(movie = local, loading = false)
            _watchHistory.value = watchEventDao.forMovie(id)

            membershipJob?.cancel()
            membershipJob = viewModelScope.launch {
                customListDao.observeListIdsForMovie(id).collect { ids ->
                    _memberListIds.value = ids
                }
            }

            val tmdbId = local.tmdbId ?: return@launch
            val bundle = tmdbRepository.movieDetailsCached(tmdbId)
            if (bundle == null) {
                _ui.value = _ui.value.copy(
                    movie = local,
                    offlineNotice = "Offline — showing saved details. Connect once to refresh posters, cast and trailers."
                )
            } else {
                _ui.value = _ui.value.copy(
                    movie = bundle.movie,
                    offlineNotice = if (bundle.fromCache) "Offline — showing saved details." else null,
                    similar = bundle.similar,
                    recommended = bundle.recommended
                )
                _watchHistory.value = watchEventDao.forMovie(id)
            }
        }
    }

    private fun reloadLocal() {
        val id = movieId
        viewModelScope.launch {
            _ui.value = _ui.value.copy(movie = movieDao.byId(id))
            _watchHistory.value = watchEventDao.forMovie(id)
        }
    }

    fun toggleWatchlist() {
        val m = _ui.value.movie ?: return
        viewModelScope.launch {
            movieDao.setWatchlist(m.id, !m.watchlist, System.currentTimeMillis())
            reloadLocal()
        }
    }

    fun toggleWatched() {
        val m = _ui.value.movie ?: return
        viewModelScope.launch {
            if (!m.watched) {
                movieDao.setWatched(m.id, true, System.currentTimeMillis())
                val today = LocalDate.now().toString()
                if (watchEventDao.forMovie(m.id).none { it.watchedOn == today }) {
                    watchEventDao.insert(
                        WatchEventEntity(
                            movieId = m.id,
                            watchedOn = today,
                            ratingAtWatch = m.personalRating
                        )
                    )
                }
            } else {
                movieDao.setWatched(m.id, false, System.currentTimeMillis())
            }
            reloadLocal()
        }
    }

    fun toggleFavorite() {
        val m = _ui.value.movie ?: return
        viewModelScope.launch {
            movieDao.setFavorite(m.id, !m.isFavorite, System.currentTimeMillis())
            reloadLocal()
        }
    }

    /** Tap a star to rate; tap the same star again to clear the rating. */
    fun setRating(rating: Double?) {
        val m = _ui.value.movie ?: return
        viewModelScope.launch {
            movieDao.setRating(m.id, rating, 10, System.currentTimeMillis())
            reloadLocal()
        }
    }

    fun saveReview(review: String) {
        val m = _ui.value.movie ?: return
        viewModelScope.launch {
            movieDao.setReview(m.id, review.ifBlank { null }, System.currentTimeMillis())
            reloadLocal()
        }
    }

    fun toggleCollection(listId: Long) {
        val m = _ui.value.movie ?: return
        viewModelScope.launch {
            if (customListDao.entryCount(listId, m.id) > 0) {
                customListDao.deleteEntry(listId, m.id)
            } else {
                customListDao.insertEntry(CustomListEntryEntity(listId = listId, movieId = m.id))
            }
            // memberListIds updates automatically via the observe flow.
        }
    }

    fun createCollectionAndAdd(name: String, description: String?) {
        val m = _ui.value.movie ?: return
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val existing = customListDao.listByName(trimmed)
            val listId = existing?.id ?: customListDao.insertList(
                CustomListEntity(name = trimmed, description = description?.trim()?.ifBlank { null })
            )
            if (listId != -1L) {
                customListDao.insertEntry(CustomListEntryEntity(listId = listId, movieId = m.id))
            }
        }
    }
}
