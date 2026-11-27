package com.moviebuzz.app.data.tmdb

import android.content.Context
import com.moviebuzz.app.BuildConfig
import com.moviebuzz.app.data.settings.SettingsStore
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Single source of truth for the TMDB API key.
 * Priority: runtime key saved in Settings (DataStore) -> build-time key
 * (local.properties / CI secret). The key is never hardcoded and never committed.
 *
 * [ensureLoaded] guarantees the stored runtime key is visible to the network
 * layer before the first TMDB call, even when the app-scope init coroutine is
 * still in flight (fixes a startup race that falsely reported a missing key).
 */
class ApiKeyProvider(
    context: Context,
    private val store: SettingsStore
) {
    private val buildKey: String = BuildConfig.TMDB_API_KEY.trim()

    @Volatile
    private var key: String = buildKey

    @Volatile
    private var loaded: Boolean = false

    private val loadMutex = Mutex()

    fun current(): String = key

    fun isConfigured(): Boolean = key.isNotBlank()

    /** Called once at app start; loads a previously saved runtime key if present. */
    suspend fun initialize() {
        val stored = store.storedKey()
        key = stored.ifBlank { buildKey }
        loaded = true
    }

    /** Idempotent: blocks until any pending [initialize] has completed. */
    suspend fun ensureLoaded() {
        if (loaded) return
        loadMutex.withLock {
            if (!loaded) initialize()
        }
    }

    /** Save or clear the runtime key (a blank value restores the build-time key). */
    suspend fun update(newKey: String) {
        val trimmed = newKey.trim()
        store.setTmdbKey(trimmed)
        key = trimmed.ifBlank { buildKey }
        loaded = true
    }
}
