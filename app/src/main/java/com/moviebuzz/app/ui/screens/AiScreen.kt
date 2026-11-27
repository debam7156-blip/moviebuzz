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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ThumbDown
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.moviebuzz.app.ui.MovieBuzzViewModelFactory

@Composable
fun AiScreen(
    navController: NavController,
    vm: AiViewModel = viewModel(factory = MovieBuzzViewModelFactory)
) {
    val ui by vm.ui.collectAsState()
    var question by remember { mutableStateOf("") }

    Scaffold(topBar = { TopAppBar(title = { Text("MOVIEBUZZ AI") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            if (ui.needsMoreData) {
                Column(Modifier.padding(24.dp)) {
                    Text("Your AI needs a little more history", style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Watch and rate at least 5 films (or import your history) and your personalised sections will appear here.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(ui.sections) { section ->
                        Card(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp)) {
                                Text(section.title, style = MaterialTheme.typography.titleLarge)
                                Spacer(Modifier.height(8.dp))
                                section.recs.take(5).forEach { rec ->
                                    Column(Modifier.padding(vertical = 6.dp)) {
                                        Row(
                                            Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(Modifier.weight(1f)) {
                                                Text(
                                                    rec.candidate.title + (rec.candidate.year?.let { " (${it})" } ?: ""),
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    rec.explanation,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                            Row {
                                                IconButton(onClick = { vm.like(rec.candidate.id) }) {
                                                    Icon(Icons.Filled.ThumbUp, "Helpful", tint = MaterialTheme.colorScheme.primary)
                                                }
                                                IconButton(onClick = { vm.dislike(rec.candidate.id) }) {
                                                    Icon(Icons.Filled.ThumbDown, "Not for me", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    items(ui.conversation) { line ->
                        Text(line, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = question,
                    onValueChange = { question = it },
                    placeholder = { Text("Ask your movie AI…") },
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = { if (question.isNotBlank()) { vm.ask(question); question = "" } }) {
                    Icon(Icons.AutoMirrored.Filled.Send, "Send", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}
