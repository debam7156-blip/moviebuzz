package com.moviebuzz.app.ui.screens.collections

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviebuzz.app.data.db.CustomListDao
import com.moviebuzz.app.data.db.CustomListEntity
import com.moviebuzz.app.data.db.CustomListEntryEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel shared by the Collections list screen and the collection detail screen.
 * Backed entirely by Room flows — every mutation is reflected instantly and
 * works fully offline.
 */
class CollectionsViewModel(private val customListDao: CustomListDao) : ViewModel() {

    val collections: StateFlow<List<CustomListDao.CustomListWithCount>> =
        customListDao.observeListsWithCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createList(name: String, description: String?, onDone: () -> Unit = {}) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            if (customListDao.listByName(trimmed) == null) {
                customListDao.insertList(
                    CustomListEntity(name = trimmed, description = description?.trim()?.ifBlank { null })
                )
            }
            onDone()
        }
    }

    fun renameList(listId: Long, newName: String, newDescription: String?) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            customListDao.updateList(listId, trimmed, newDescription?.trim()?.ifBlank { null })
        }
    }

    fun deleteList(listId: Long) {
        viewModelScope.launch { customListDao.deleteList(listId) }
    }

    fun addMovie(listId: Long, movieId: Long) {
        viewModelScope.launch {
            customListDao.insertEntry(CustomListEntryEntity(listId = listId, movieId = movieId))
        }
    }

    fun removeMovie(listId: Long, movieId: Long) {
        viewModelScope.launch { customListDao.deleteEntry(listId, movieId) }
    }
}
