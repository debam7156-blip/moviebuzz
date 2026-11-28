package com.moviebuzz.app.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.moviebuzz.app.BuildConfig
import com.moviebuzz.app.data.sync.BackupManager
import com.moviebuzz.app.data.tmdb.ApiKeyProvider
import com.moviebuzz.app.ui.MovieBuzzViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val apiKeyProvider: ApiKeyProvider,
    private val backupManager: BackupManager
) : ViewModel() {

    data class SettingsUi(
        val busy: Boolean = false,
        val keyConfigured: Boolean = false,
        val message: String? = null
    )

    private val _ui = MutableStateFlow(SettingsUi())
    val ui: StateFlow<SettingsUi> = _ui

    init {
        // Reflect the already-configured key (runtime or build-time) on first show.
        viewModelScope.launch {
            apiKeyProvider.ensureLoaded()
            _ui.value = _ui.value.copy(keyConfigured = apiKeyProvider.isConfigured())
        }
    }

    /**
     * Save or clear the runtime key. Goes through ApiKeyProvider so both the
     * DataStore copy AND the live in-memory key used by the network layer
     * update immediately — no app restart needed.
     */
    fun saveKey(newKey: String) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, message = null)
            try {
                apiKeyProvider.update(newKey)
                _ui.value = _ui.value.copy(
                    busy = false,
                    keyConfigured = apiKeyProvider.isConfigured(),
                    message = if (newKey.isBlank())
                        "Cleared — the build-time key will be used (if one was provided)."
                    else "Saved. Online TMDB features will use this key immediately."
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(busy = false, message = "Could not save: ${e.message}")
            }
        }
    }

    fun exportBackup(uri: android.net.Uri, resolver: android.content.ContentResolver) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, message = null)
            try {
                val json = backupManager.export()
                resolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
                    ?: throw IllegalStateException("Could not open destination file")
                _ui.value = _ui.value.copy(busy = false, message = "Backup exported successfully.")
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(busy = false, message = "Export failed: ${e.message}")
            }
        }
    }

    fun restoreBackup(uri: android.net.Uri, resolver: android.content.ContentResolver) {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(busy = true, message = null)
            try {
                val json = resolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                    ?: throw IllegalStateException("Could not read the selected file")
                val result = backupManager.restore(json)
                _ui.value = _ui.value.copy(
                    busy = false,
                    message = result.failedReason
                        ?: "Restored ${result.restoredMovies} new movies, merged ${result.mergedMovies}, " +
                            "restored ${result.restoredWatchEvents} watch events" +
                            (if (result.restoredImportIssues > 0) " and ${result.restoredImportIssues} import review rows" else "") +
                            "."
                )
            } catch (e: Exception) {
                _ui.value = _ui.value.copy(busy = false, message = "Restore failed: ${e.message}")
            }
        }
    }
}

@Composable
fun SettingsScreen(
    navController: NavController,
    vm: SettingsViewModel = viewModel(factory = MovieBuzzViewModelFactory)
) {
    val ui by vm.ui.collectAsState()
    val context = LocalContext.current

    var keyInput by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) vm.exportBackup(uri, context.contentResolver)
    }

    val restoreLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.restoreBackup(uri, context.contentResolver)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("TMDB API key", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Free key from themoviedb.org. Stored only on this device — never committed to the repository.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = {
                            Text(if (ui.keyConfigured) "Configured — type a new key to replace" else "Paste your TMDB v3 key")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Button(onClick = { vm.saveKey(keyInput) }, enabled = !ui.busy) {
                            Text("Save key")
                        }
                        OutlinedButton(
                            onClick = {
                                keyInput = ""
                                vm.saveKey("")
                            },
                            enabled = !ui.busy
                        ) { Text("Clear") }
                        if (ui.busy) CircularProgressIndicator(Modifier.size(24.dp))
                    }
                    ui.message?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (BuildConfig.TMDB_API_KEY.isNotBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "A build-time key is also active (from local.properties / CI). A key saved here overrides it.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Backup & restore", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Export your entire library (movies, ratings, reviews, watch history, collections, tags, and unresolved import rows) as a portable JSON file, or restore one.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { exportLauncher.launch("moviebuzz-backup.json") },
                            enabled = !ui.busy
                        ) { Text("Export") }
                        OutlinedButton(
                            onClick = { restoreLauncher.launch(arrayOf("application/json")) },
                            enabled = !ui.busy
                        ) { Text("Restore") }
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("About", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "MOVIEBUZZ v1.1.0 — offline-first personal movie tracking. " +
                            "Watchlists, ratings, history, collections and statistics always work without internet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
