package com.moviebuzz.app

import android.content.Context
import com.moviebuzz.app.data.db.MovieBuzzDatabase
import com.moviebuzz.app.data.tmdb.TmdbRepository
import com.moviebuzz.app.ui.screens.AiViewModel
import com.moviebuzz.app.ui.screens.DiscoverViewModel
import com.moviebuzz.app.ui.screens.ImportViewModel
import com.moviebuzz.app.ui.screens.LibraryViewModel
import com.moviebuzz.app.ui.screens.MovieDetailViewModel
import com.moviebuzz.app.ui.screens.StatsViewModel

/**
 * Simple dependency holder created once by MovieBuzzApp. ViewModels receive
 * their dependencies through factories; no global singletons beyond the
 * database and repository (both lifecycle-safe).
 */
class AppContainer(context: Context) {

    val database: MovieBuzzDatabase = MovieBuzzDatabase.get(context)

    val tmdbRepository: TmdbRepository = TmdbRepository(database.movieDao())

    val libraryViewModelFactory = { LibraryViewModel(database.movieDao()) }

    val discoverViewModelFactory = { DiscoverViewModel(tmdbRepository, database.movieDao()) }

    val movieDetailViewModelFactory = { MovieDetailViewModel(database.movieDao(), database.watchEventDao(), tmdbRepository) }

    val statsViewModelFactory = { StatsViewModel(database) }

    val aiViewModelFactory = { AiViewModel(database) }

    val importViewModelFactory = { ImportViewModel(database, context.contentResolver) }
}
