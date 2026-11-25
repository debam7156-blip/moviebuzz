package com.moviebuzz.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.moviebuzz.app.data.tmdb.TmdbImages

@Composable
fun MovieDetailScreen(
    navController: NavController,
    movieId: Long,
    vm: MovieDetailViewModel = viewModel()
) {
    val movie by vm.movie.collectAsState()
    val watchHistory by vm.watchHistory.collectAsState()
    LaunchedEffect(movieId) { vm.load(movieId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(movie?.title ?: "") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val m = movie
        if (m == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Loading…", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            AsyncImage(
                model = TmdbImages.backdropUrl(m.backdropPath ?: m.posterPath),
                contentDescription = "${m.title} backdrop",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            )
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.Top) {
                    AsyncImage(
                        model = TmdbImages.posterUrl(m.posterPath),
                        contentDescription = "${m.title} poster",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .width(110.dp)
                            .height(165.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(Modifier.width(16.dp))
                    Column {
                        Text(m.title, style = MaterialTheme.typography.headlineMedium)
                        Text(
                            listOfNotNull(m.year?.toString(), m.runtimeMinutes?.let { "$it min" })
                                .joinToString(" · "),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (m.genres.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                m.genres.take(3).forEach { genre ->
                                    AssistChip(onClick = {}, label = { Text(genre) })
                                }
                            }
                        }
                        if (m.director != null) {
                            Spacer(Modifier.height(8.dp))
                            Text("Directed by ${m.director}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (m.tmdbVoteAverage != null) {
                            Text(
                                "TMDB ★ %.1f (${m.tmdbVoteCount ?: 0} votes)".format(m.tmdbVoteAverage),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (m.watched) {
                        OutlinedButton(onClick = { vm.toggleWatchlist() }) {
                            Text(if (m.watchlist) "In watchlist ✓" else "Add to watchlist")
                        }
                    } else {
                        Button(onClick = { vm.markWatchedToday() }) {
                            Text("Mark watched today")
                        }
                    }
                    IconButton(onClick = { vm.toggleFavorite() }) {
                        Icon(
                            imageVector = if (m.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Toggle favourite",
                            tint = if (m.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text("Your rating", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Row {
                    (1..10).forEach { step ->
                        val filled = (m.personalRating ?: 0.0) >= step - 0.5
                        IconButton(
                            onClick = { vm.setRating(step.toDouble()) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                contentDescription = "Rate $step",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                var review by remember(m.id) { mutableStateOf(m.review ?: "") }
                OutlinedTextField(
                    value = review,
                    onValueChange = { review = it },
                    placeholder = { Text("Write your review…") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { vm.saveReview(review) }) {
                    Text("Save review")
                }

                Spacer(Modifier.height(16.dp))
                Text("Overview", style = MaterialTheme.typography.titleMedium)
                Text(
                    m.overview ?: "No overview available yet. Connect to the internet once to fetch details.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                if (m.cast.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Cast", style = MaterialTheme.typography.titleMedium)
                    Text(
                        m.cast.joinToString(", "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (watchHistory.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text("Your watch history", style = MaterialTheme.typography.titleMedium)
                    watchHistory.forEach { event ->
                        Text(
                            "• ${event.watchedOn}" + (event.ratingAtWatch?.let { " — rated ${it}" } ?: ""),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(32.dp))
            }
        }
    }
}
