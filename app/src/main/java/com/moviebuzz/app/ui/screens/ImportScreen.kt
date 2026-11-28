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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.moviebuzz.app.ui.MovieBuzzViewModelFactory
import com.moviebuzz.app.ui.nav.Routes

@Composable
fun ImportScreen(
    navController: NavController,
    vm: ImportViewModel = viewModel(factory = MovieBuzzViewModelFactory)
) {
    val state by vm.state.collectAsState()

    val picker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) vm.importFile(uri)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate(Routes.IMPORT_ISSUES) }) {
                        Icon(
                            androidx.compose.material.icons.Icons.Outlined.Inbox,
                            contentDescription = "Review unmatched import rows"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("Bring your movie history", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Supported: Letterboxd ZIP export, and CSV exports with common headers " +
                    "(Moviebase, I Like Movies, and generic formats). Nothing is discarded — " +
                    "anything that cannot be matched is kept for you to resolve.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                picker.launch(
                    arrayOf("application/zip", "text/csv", "text/comma-separated-values", "application/octet-stream")
                )
            }) {
                Text("Choose ZIP or CSV file")
            }
            Spacer(Modifier.height(16.dp))

            when (val s = state) {
                is ImportViewModel.UiState.Idle -> {}
                is ImportViewModel.UiState.Running -> {
                    Text(s.label)
                    Spacer(Modifier.height(8.dp))
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
                is ImportViewModel.UiState.Failed -> {
                    Text("Import failed: ${s.reason}", color = MaterialTheme.colorScheme.error)
                }
                is ImportViewModel.UiState.Done -> {
                    val report = s.report
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Import report — ${report.source}", style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(8.dp))
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Total records"); Text("${report.totalRecords}")
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Imported"); Text("${report.imported}")
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Merged into existing"); Text("${report.duplicates}")
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Needs your review"); Text("${report.unresolved}")
                            }
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Failed"); Text("${report.failed}")
                            }
                            if (report.importedLists.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text("Lists created: ${report.importedLists.joinToString(", ")}")
                            }
                            if (report.unresolved > 0) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Unmatched rows were saved for you to review.",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(8.dp))
                                OutlinedButton(onClick = { navController.navigate(Routes.IMPORT_ISSUES) }) {
                                    Text("Review unmatched rows (${report.unresolved})")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
