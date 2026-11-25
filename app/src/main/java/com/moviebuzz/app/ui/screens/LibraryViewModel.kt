package com.moviebuzz.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviebuzz.app.data.db.MovieDao
import com.moviebuzz.app.data.db.MovieEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Library screen state. All data comes from Room via Flow, so the library is
 * live-updated (import, watch, rate) with no manual refresh anywhere.
 */
class LibraryViewModel(private val movieDao: MovieDao) : ViewModel() {

    enum class Sort(val label: String) {
        TITLE("Title"), YEAR("Year"), RATING("My rating"), RECENT("Recently added")
    }

    data class Filters(
        val query: String = "",
        val sort: Sort = Sort.RECENT,
        val onlyWatched: Boolean = false,
        val onlyFavorites: Boolean = false,
        val onlyWatchlist: Boolean = false
    )

    data class UiState(
        val filters: Filters = Filters(),
        val movies: List<MovieEntity> = emptyList(),
        val totalInLibrary: Int = 0
    )

    private val filters = MutableStateFlow(Filters())

    val state: StateFlow<UiState> =
        combine(movieDao.observeAll(), filters) { all, f ->
            UiState(
                filters = f,
                movies = applyFilters(all, f),
                totalInLibrary = all.size
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UiState())

    fun setQuery(query: String) { filters.value = filters.value.copy(query = query) }
    fun setSort(sort: Sort) { filters.value = filters.value.copy(sort = sort) }
    fun toggleWatchedFilter() { filters.value = filters.value.copy(onlyWatched = !filters.value.onlyWatched) }
    fun toggleFavoritesFilter() { filters.value = filters.value.copy(onlyFavorites = !filters.value.onlyFavorites) }
    fun toggleWatchlistFilter() { filters.value = filters.value.copy(onlyWatchlist = !filters.value.onlyWatchlist) }

    fun markWatched(movie: MovieEntity) = viewModelScope.launch {
        movieDao.markWatched(movie.id, System.currentTimeMillis())
    }

    fun toggleFavorite(movie: MovieEntity) = viewModelScope.launch {
        movieDao.setFavorite(movie.id, !movie.isFavorite, System.currentTimeMillis())
    }

    fun toggleWatchlist(movie: MovieEntity) = viewModelScope.launch {
        movieDao.setWatchlist(movie.id, !movie.watchlist, System.currentTimeMillis())
    }

    private fun applyFilters(all: List<MovieEntity>, f: Filters): List<MovieEntity> {
        var list = all
        if (f.onlyWatched) list = list.filter { it.watched }
        if (f.onlyWatchlist) list = list.filter { it.watchlist }
        if (f.onlyFavorites) list = list.filter { it.isFavorite }
        if (f.query.isNotBlank()) {
            val q = f.query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                    (it.director?.lowercase()?.contains(q) == true) ||
                    it.genres.any { g -> g.lowercase().contains(q) }
            }
        }
        return when (f.sort) {
            Sort.TITLE -> list.sortedBy { it.title.lowercase() }
            Sort.YEAR -> list.sortedByDescending { it.year ?: 0 }
            Sort.RATING -> list.sortedByDescending { it.personalRating ?: -1.0 }
            Sort.RECENT -> list.sortedByDescending { it.addedAt }
        }
    }
}
