package com.moviebuzz.app.data.tmdb

import android.content.Context
import com.moviebuzz.app.BuildConfig
import com.moviebuzz.app.data.settings.SettingsStore

/**
 * Single source of truth for the TMDB API key.
 * Priority: runtime key saved in Settings (DataStore) -> build-time key
 * (local.properties / CI secret). The key is never hardcoded and never committed.
 */
class ApiKeyProvider(context: Context) {

    private val store = SettingsStore(context)
    private val buildKey: String = BuildConfig.TMDB_API_KEY.trim()

    @Volatile
    private var key: String = buildKey

    fun current(): String = key

    fun isConfigured(): Boolean = key.isNotBlank()

    /** Called once at app start; loads a previously saved runtime key if present. */
    suspend fun initialize() {
        val stored = store.storedKey()
        key = stored.ifBlank { buildKey }
    }

    /** Save or clear the runtime key (a blank value restores the build-time key). */
    suspend fun update(newKey: String) {
        val trimmed = newKey.trim()
        store.setTmdbKey(trimmed)
        key = trimmed.ifBlank { buildKey }
    }
}
