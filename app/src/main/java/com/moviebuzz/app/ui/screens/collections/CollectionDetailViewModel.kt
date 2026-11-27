package com.moviebuzz.app.ui.screens.collections

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviebuzz.app.data.db.CustomListDao
import com.moviebuzz.app.data.db.CustomListEntity
import com.moviebuzz.app.data.db.MovieEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/** Observes a single collection and its movies (live Room flow). */
class CollectionDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val customListDao: CustomListDao
) : ViewModel() {

    val listId: Long = savedStateHandle.get<Long>("listId") ?: 0L

    val list: StateFlow<CustomListEntity?> =
        customListDao.observeListById(listId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val movies: StateFlow<List<MovieEntity>> =
        customListDao.observeMoviesInList(listId)
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
