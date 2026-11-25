package com.moviebuzz.app.ui.screens

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.moviebuzz.app.data.db.MovieBuzzDatabase
import com.moviebuzz.app.data.import.ImportAdapters
import com.moviebuzz.app.data.import.ImportEngine
import com.moviebuzz.app.data.import.ImportReport
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportViewModel(
    private val db: MovieBuzzDatabase,
    private val contentResolver: android.content.ContentResolver
) : ViewModel() {

    sealed interface UiState {
        data object Idle : UiState
        data class Running(val label: String) : UiState
        data class Done(val report: ImportReport) : UiState
        data class Failed(val reason: String) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Idle)
    val state: StateFlow<UiState> = _state

    fun import(uri: Uri) {
        _state.value = UiState.Running("Reading file…")
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                        ?: return@withContext UiState.Failed("Could not read the selected file")

                    // ZIP (Letterboxd export) vs plain CSV — sniff magic bytes first.
                    val isZip = bytes.size >= 4 &&
                        bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte()
                    val adapterResults = if (isZip) {
                        ImportAdapters.Letterboxd.fromZip(bytes.inputStream())
                    } else {
                        ImportAdapters.FlexibleCsv.fromCsv(bytes, uri.lastPathSegment ?: "import.csv")
                    }

                    val engine = ImportEngine(
                        movieDao = db.movieDao(),
                        watchEventDao = db.watchEventDao(),
                        customListDao = db.customListDao(),
                        unresolvedDao = db.importUnresolvedDao()
                    )
                    UiState.Done(engine.import(adapterResults))
                } catch (e: Exception) {
                    UiState.Failed(e.message ?: "Import failed")
                }
            }
            _state.value = result
        }
    }
}
