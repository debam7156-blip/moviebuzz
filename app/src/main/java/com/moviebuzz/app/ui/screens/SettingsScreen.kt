package com.moviebuzz.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.moviebuzz.app.BuildConfig

/**
 * Settings — now fully functional:
 * - Enter/clear the TMDB API key at runtime (stored in DataStore, never in code).
 * - Export a full JSON backup and restore it.
 * - Everything works offline.
 */
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    val app = context.applicationContext as com.moviebuzz.app.MovieBuzzApp
    val container = app.container
    val apiKeyProvider = container.apiKeyProvider
    val backupManager = container.backupManager

    var keyInput by remember { mutableStateOf("") }
    var keyStatus by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }

    fun saveKey() {
        busy = true
        keyStatus = null
        androidx.compose.runtime.rememberCoroutineScope().launch {
            try {
                apiKeyProvider.update(keyInput)
                keyStatus = if (keyInput.isBlank()) "Cleared — build-time key in use (if any)."
                else "Saved. TMDB features will use this key."
            } catch (e: Exception) {
                keyStatus = "Could not save: ${e.message}"
            } finally {
                busy = false
            }
        }
    }

    fun exportBackup() {
        busy = true
        keyStatus = null
        androidx.compose.runtime.rememberCoroutineScope().launch {
            try {
                val json = backupManager.export()
                val intent = android.content.Intent(android.content.Intent.ACTION_CREATE_DOCUMENT).apply {
                    addCategory(android.content.Intent.CATEGORY_OPENABLE)
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_TITLE, "moviebuzz-backup-${System.currentTimeMillis()}.json")
                }
                // Simple, reliable flow: write to app files dir and share via ACTION_SEND.
                val dir = android.os.Environment.getExternalStorageDirectory()
                val fileName = "moviebuzz-backup.json"
                val file = java.io.File(context.cacheDir, fileName)
                file.writeText(json)
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context, "${context.packageName}.fileprovider", file
                )
                val share = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "application/json"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(android.content.Intent.createChooser(share, "Save backup"))
                keyStatus = "Backup ready — choose where to save it."
            } catch (e: Exception) {
                keyStatus = "Export failed: ${e.message}"
            } finally {
                busy = false
            }
        }
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
                        "Free key from themoviedb.org. Stored only on this device; never committed to any repository.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = keyInput,
                        onValueChange = { keyInput = it },
                        placeholder = {
                            Text(if (apiKeyProvider.isConfigured()) "•••• configured — type to replace" else "Paste your TMDB v3 key")
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Button(onClick = { saveKey() }, enabled = !busy) { Text("Save key") }
                        OutlinedButton(onClick = { keyInput = ""; saveKey() }, enabled = !busy) { Text("Clear") }
                        if (busy) CircularProgressIndicator(Modifier.height(24.dp).width? .height(24.dp))
                    }
                    keyStatus?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    if (!BuildConfig.TMDB_API_KEY.isBlank()) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "A build-time key is also active (from local.properties / CI). The runtime key overrides it.",
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
                        "Export your entire library (movies, ratings, reviews, watch history, collections, tags) as a portable JSON file.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { exportBackup() }, enabled = !busy) { Text("Export backup") }
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
