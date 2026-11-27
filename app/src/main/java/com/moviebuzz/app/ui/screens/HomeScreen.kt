package com.moviebuzz.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
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
import com.moviebuzz.app.data.tmdb.TmdbImages
import com.moviebuzz.app.ui.MovieBuzzViewModelFactory
import com.moviebuzz.app.ui.components.EmptyState
import com.moviebuzz.app.ui.components.ErrorState
import com.moviebuzz.app.ui.components.MovieCard
import com.moviebuzz.app.ui.components.PosterRowSkeleton
import com.moviebuzz.app.ui.nav.Routes

@Composable
fun HomeScreen(navController: NavController, vm: HomeViewModel = viewModel(factory = MovieBuzzViewModelFactory)) {
    val ui by vm.ui.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("MOVIEBUZZ") }) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Column(Modifier.padding(horizontal = 16.dp)) {
                    Text("Your personal movie universe.", style = MaterialTheme.typography.headlineMedium)
                    if (ui.recommended.isEmpty() && ui.error != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            ui.error,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (ui.loading) {
                item { SectionLabel("Trending this week") }
                item { PosterRowSkeleton() }
                item { SectionLabel("Popular right now") }
                item { PosterRowSkeleton() }
            } else {
                FeedSection("Trending this week", ui.trending, ui.loading, navController)
                FeedSection("Popular right now", ui.popular, ui.loading, navController)
                FeedSection("Recently released", ui.recentlyReleased, ui.loading, navController)
                FeedSection("Upcoming soon", ui.upcoming, ui.loading, navController)
                FeedSection("Recommended for you", ui.recommended, ui.loading, navController)
                if (ui.error != null) {
                    item {
                        ErrorState(
                            message = ui.error,
                            onRetry = { vm.refresh() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun FeedSection(
    title: String,
    movies: List<com.moviebuzz.app.data.db.MovieEntity>,
    loading: Boolean,
    navController: NavController
) {
    if (movies.isEmpty()) return
    item { SectionLabel(title) }
    item {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(movies, key = { it.id }) { movie ->
                MovieCard(
                    title = movie.title,
                    year = movie.year?.toString() ?: "",
                    posterUrl = TmdbImages.posterUrl(movie.posterPath),
                    rating = movie.tmdbVoteAverage?.let { "%.1f".format(it) },
                    onClick = { navController.navigate(Routes.movieDetail(movie.id)) }
                )
            }
        }
    }
}
