package com.moviebuzz.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.moviebuzz.app.MovieBuzzApp
import com.moviebuzz.app.ui.screens.AiViewModel
import com.moviebuzz.app.ui.screens.DiscoverViewModel
import com.moviebuzz.app.ui.screens.ImportViewModel
import com.moviebuzz.app.ui.screens.LibraryViewModel
import com.moviebuzz.app.ui.screens.MovieDetailViewModel
import com.moviebuzz.app.ui.screens.StatsViewModel

/**
 * The single ViewModel factory for the app. ViewModels are constructed with
 * their real dependencies from the AppContainer — this is what was missing
 * before and caused InstantiationException crashes on every screen.
 */
object MovieBuzzViewModelFactory : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as? MovieBuzzApp
            ?: throw IllegalStateException("MovieBuzzApp is not available in ViewModel extras")
        val db = app.container.database
        val repo = app.container.tmdbRepository
        val vm: ViewModel = when (modelClass) {
            MovieDetailViewModel::class.java ->
                MovieDetailViewModel(db.movieDao(), db.watchEventDao(), db.customListDao(), repo)
            LibraryViewModel::class.java -> LibraryViewModel(db.movieDao())
            DiscoverViewModel::class.java -> DiscoverViewModel(repo, db.movieDao())
            StatsViewModel::class.java -> StatsViewModel(db)
            AiViewModel::class.java -> AiViewModel(db)
            ImportViewModel::class.java ->
                ImportViewModel(db, app.container.appContext.contentResolver)
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
        return vm as T
    }
}
