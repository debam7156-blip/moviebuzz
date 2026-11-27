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
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.moviebuzz.app.ui.MovieBuzzViewModelFactory

@Composable
fun StatsScreen(
    navController: NavController,
    vm: StatsViewModel = viewModel(factory = MovieBuzzViewModelFactory)
) {
    val stats by vm.stats.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Statistics") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Films watched", "${stats.totalMovies}", Modifier.weight(1f))
                    StatCard("Viewing sessions", "${stats.totalSessions}", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Watch hours", "%.1f".format(stats.totalHours), Modifier.weight(1f))
                    StatCard("Avg rating", stats.averageRating?.let { "%.1f".format(it) } ?: "—", Modifier.weight(1f))
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    StatCard("Current streak", "${stats.currentStreak} d", Modifier.weight(1f))
                    StatCard("Best streak", "${stats.longestStreak} d", Modifier.weight(1f))
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Genres you watch most", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        if (stats.genres.isEmpty()) {
                            Text("No data yet — watch and rate some films.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else stats.genres.forEach { g ->
                            Column(Modifier.padding(vertical = 2.dp)) {
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(g.genre); Text("${g.count}")
                                }
                                LinearProgressIndicator(
                                    progress = { g.share.toFloat() },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Rating distribution", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        if (stats.ratingBuckets.values.sum() == 0) {
                            Text("No personal ratings yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            val max = stats.ratingBuckets.values.max()
                            (5 downTo 1).forEach { star ->
                                val count = stats.ratingBuckets[star] ?: 0
                                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                                    Text("$star★", Modifier.padding(end = 8.dp))
                                    LinearProgressIndicator(
                                        progress = { if (max > 0) count.toFloat() / max else 0f },
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text("$count", Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
            if (stats.rewatched.isNotEmpty()) item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Most rewatched", style = MaterialTheme.typography.titleMedium)
                        stats.rewatched.forEach { (movie, count) ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(movie.title); Text("×$count")
                            }
                        }
                    }
                }
            }
            if (stats.topDirectors.isNotEmpty()) item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Top directors", style = MaterialTheme.typography.titleMedium)
                        stats.topDirectors.forEach { d ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(d.name); Text("${d.count} films")
                            }
                        }
                    }
                }
            }
            if (stats.topActors.isNotEmpty()) item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Top actors", style = MaterialTheme.typography.titleMedium)
                        stats.topActors.forEach { a ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(a.name); Text("${a.count} films")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier) {
        Column(Modifier.padding(16.dp)) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
