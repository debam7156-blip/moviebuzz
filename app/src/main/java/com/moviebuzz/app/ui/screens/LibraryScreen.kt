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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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
import com.moviebuzz.app.ui.components.MovieCard
import com.moviebuzz.app.ui.nav.Routes

@Composable
fun LibraryScreen(navController: NavController, vm: LibraryViewModel = viewModel(factory = MovieBuzzViewModelFactory)) {
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Library") },
                actions = {
                    Text(
                        "${state.totalInLibrary} films",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            OutlinedTextField(
                value = state.filters.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search title, director, genre…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            androidx.compose.foundation.layout.FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = state.filters.onlyWatched,
                    onClick = vm::toggleWatchedFilter,
                    label = { Text("Watched") }
                )
                FilterChip(
                    selected = state.filters.onlyWatchlist,
                    onClick = vm::toggleWatchlistFilter,
                    label = { Text("Watchlist") }
                )
                FilterChip(
                    selected = state.filters.onlyFavorites,
                    onClick = vm::toggleFavoritesFilter,
                    label = { Text("Favourites") }
                )
                FilterChip(
                    selected = false,
                    onClick = {
                        vm.setSort(
                            when (state.filters.sort) {
                                LibraryViewModel.Sort.RECENT -> LibraryViewModel.Sort.TITLE
                                LibraryViewModel.Sort.TITLE -> LibraryViewModel.Sort.YEAR
                                LibraryViewModel.Sort.YEAR -> LibraryViewModel.Sort.RATING
                                LibraryViewModel.Sort.RATING -> LibraryViewModel.Sort.RECENT
                            }
                        )
                    },
                    label = { Text("Sort: ${state.filters.sort.label}") }
                )
            }
            Spacer(Modifier.height(8.dp))

            if (state.movies.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Movie,
                    title = if (state.filters.query.isBlank()) "Your library is empty" else "No matches",
                    message = if (state.filters.query.isBlank())
                        "Watch and rate films, or add movies to your watchlist — they appear here."
                    else
                        "Try a different search or clear the filters."
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(124.dp),
                    state = rememberLazyGridState(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(state.movies, key = { it.id }) { movie ->
                        MovieCard(
                            title = movie.title,
                            year = movie.year?.toString() ?: "",
                            posterUrl = TmdbImages.posterUrl(movie.posterPath),
                            rating = movie.personalRating?.let { "%.1f".format(it) },
                            onClick = { navController.navigate(Routes.movieDetail(movie.id)) }
                        )
                    }
                }
            }
        }
    }
}
