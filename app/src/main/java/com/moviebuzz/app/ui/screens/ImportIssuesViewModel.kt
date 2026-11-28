package com.moviebuzz.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviebuzz.app.data.db.ImportUnresolvedDao
import com.moviebuzz.app.data.db.ImportUnresolvedEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Import issues review queue: rows from an import that could not be
 * automatically matched to a movie. The user can dismiss rows they do not
 * want, or retry a manual TMDB match later (search from the Search screen).
 */
class ImportIssuesViewModel(
    private val unresolvedDao: ImportUnresolvedDao
) : ViewModel() {

    val rows: StateFlow<List<ImportUnresolvedEntity>> =
        unresolvedDao.observeAll()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    /** Permanently remove one unresolved row from the review queue. */
    fun dismiss(id: Long) {
        viewModelScope.launch { unresolvedDao.delete(id) }
    }

    /** Clear the whole queue. */
    fun dismissAll() {
        viewModelScope.launch { unresolvedDao.deleteAll() }
    }
}
