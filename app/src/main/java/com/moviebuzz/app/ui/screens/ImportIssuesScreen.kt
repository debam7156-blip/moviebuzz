package com.moviebuzz.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.moviebuzz.app.data.db.ImportUnresolvedEntity
import com.moviebuzz.app.ui.MovieBuzzViewModelFactory
import com.moviebuzz.app.ui.components.EmptyState
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/**
 * Review queue for import rows that could not be automatically matched.
 * Previously the Import screen told users these rows were "saved for review
 * in Settings > Import issues" but no such screen existed - they were saved
 * to the database and could never be seen or cleared. This screen closes
 * that dead end.
 */
@Composable
fun ImportIssuesScreen(
    navController: NavController,
    vm: ImportIssuesViewModel = viewModel(factory = MovieBuzzViewModelFactory)
) {
    val rows by vm.rows.collectAsState()
    var confirmClear by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import issues") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (rows.isNotEmpty()) {
                        TextButton(onClick = { confirmClear = true }) { Text("Clear all") }
                    }
                }
            )
        }
    ) { padding ->
        if (rows.isEmpty()) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
            ) {
                EmptyState(
                    icon = Icons.Outlined.Inbox,
                    title = "Nothing to review",
                    message = "When an import cannot automatically match a movie, the row appears here so you can search for it manually and nothing is ever lost."
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Text(
                        "Rows below could not be matched automatically. Search the title in the Search screen to add the film, then dismiss the row.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                items(rows, key = { it.id }) { row ->
                    ImportIssueCard(
                        row = row,
                        onSearch = {
                            navController.navigate(RoutesSearchProxy.SEARCH)
                        },
                        onDismiss = { vm.dismiss(row.id) }
                    )
                }
                item {
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Clear all import issues?") },
            text = { Text("These unmatched rows will be removed. Your imported movies, ratings and history are not affected.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.dismissAll()
                    confirmClear = false
                }) { Text("Clear", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Cancel") } }
        )
    }
}

/** Route constant for the search screen (avoids importing the whole Routes object here). */
private object RoutesSearchProxy {
    const val SEARCH = "search"
}

private val issueJson = Json { ignoreUnknownKeys = true }
private val issueMapSerializer = MapSerializer(String.serializer(), String.serializer())

@Composable
private fun ImportIssueCard(
    row: ImportUnresolvedEntity,
    onSearch: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(
                row.title,
                style = MaterialTheme.typography.titleMedium
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    listOfNotNull(
                        row.year?.toString(),
                        row.fileLabel
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "row ${row.rowNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(8.dp))
            val raw = remember(row.id) {
                runCatching {
                    issueJson.decodeFromString(issueMapSerializer, row.rawJson)
                }.getOrNull() ?: emptyMap()
            }
            if (raw.isNotEmpty()) {
                Text(
                    "Raw fields:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                raw.entries.take(6).forEach { (key, value) ->
                    Text(
                        "$key: $value",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onSearch) { Text("Search title") }
                OutlinedButton(onClick = onDismiss) { Text("Dismiss") }
            }
        }
    }
}
