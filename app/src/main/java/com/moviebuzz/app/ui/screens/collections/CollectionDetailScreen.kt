package com.moviebuzz.app.ui.screens.collections

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.moviebuzz.app.ui.components.MovieCard
import com.moviebuzz.app.ui.nav.Routes

@Composable
fun CollectionDetailScreen(
    navController: NavController,
    listId: Long,
    vm: CollectionDetailViewModel = viewModel(factory = MovieBuzzViewModelFactory)
) {
    val list by vm.list.collectAsState()
    val movies by vm.movies.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(list?.name ?: "Collection") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        // Scaffold content is a Box-like layout: siblings would stack on top of
        // each other. Everything goes inside one Column so the description text
        // and the movie grid are laid out vertically, never overlapping.
        Column(Modifier.fillMaxSize().padding(padding)) {
            list?.description?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            if (movies.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.Delete,
                    title = "This collection is empty",
                    message = "Open any movie and add it to this collection from its detail page.",
                    actionLabel = "Browse Discover",
                    onAction = { navController.navigate(Routes.DISCOVER) }
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(124.dp),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(movies, key = { it.id }) { movie ->
                        MovieCard(
                            title = movie.title,
                            year = movie.year?.toString() ?: "",
                            posterUrl = TmdbImages.posterUrl(movie.posterPath),
                            onClick = { navController.navigate(Routes.movieDetail(movie.id)) }
                        )
                    }
                }
            }
        }
    }
}
