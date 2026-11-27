package com.moviebuzz.app.data.settings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.settingsDataStore by preferencesDataStore(name = "moviebuzz_settings")

/**
 * Small typed wrapper around the DataStore that holds runtime settings.
 * Currently stores only the optional TMDB API key entered in Settings.
 */
class SettingsStore(private val context: Context) {

    suspend fun storedKey(): String =
        context.settingsDataStore.data.first()[TMDB_KEY]?.trim() ?: ""

    suspend fun setTmdbKey(value: String) {
        context.settingsDataStore.edit { prefs ->
            if (value.isBlank()) prefs.remove(TMDB_KEY) else prefs[TMDB_KEY] = value
        }
    }

    companion object {
        private val TMDB_KEY = stringPreferencesKey("tmdb_api_key")
    }
}
