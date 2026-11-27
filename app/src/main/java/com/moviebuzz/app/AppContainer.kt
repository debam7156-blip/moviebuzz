package com.moviebuzz.app

import android.content.Context
import com.moviebuzz.app.data.db.MovieBuzzDatabase
import com.moviebuzz.app.data.settings.SettingsStore
import com.moviebuzz.app.data.sync.BackupManager
import com.moviebuzz.app.data.tmdb.ApiKeyProvider
import com.moviebuzz.app.data.tmdb.TmdbRepository

/**
 * Simple dependency holder created once by MovieBuzzApp. ViewModels receive their
 * dependencies through MovieBuzzViewModelFactory — no global mutable singletons.
 */
class AppContainer(context: Context) {

    val appContext: Context = context.applicationContext

    val database: MovieBuzzDatabase = MovieBuzzDatabase.get(appContext)

    val settingsStore: SettingsStore = SettingsStore(appContext)

    val apiKeyProvider: ApiKeyProvider = ApiKeyProvider(appContext, settingsStore)

    val tmdbRepository: TmdbRepository = TmdbRepository(database.movieDao(), apiKeyProvider)

    val backupManager: BackupManager = BackupManager(
        movieDao = database.movieDao(),
        watchEventDao = database.watchEventDao(),
        customListDao = database.customListDao(),
        tagDao = database.tagDao()
    )
}
