package com.moviebuzz.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.SearchOff
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.moviebuzz.app.data.db.MovieDao
import com.moviebuzz.app.data.db.MovieEntity
import com.moviebuzz.app.data.tmdb.TmdbImages
import com.moviebuzz.app.data.tmdb.TmdbRepository
import com.moviebuzz.app.data.tmdb.friendlyTmdbError
import com.moviebuzz.app.ui.MovieBuzzViewModelFactory
import com.moviebuzz.app.ui.components.EmptyState
import com.moviebuzz.app.ui.components.ErrorState
import com.moviebuzz.app.ui.components.MovieCard
import com.moviebuzz.app.ui.components.PosterSkeleton
import com.moviebuzz.app.ui.nav.Routes
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class)
class SearchViewModel(
    private val tmdbRepository: TmdbRepository,
    private val movieDao: MovieDao
) : ViewModel() {

    data class SearchUi(
        val query: String = "",
        val loading: Boolean = false,
        val results: List<MovieEntity> = emptyList(),
        val error: String? = null,
        val searched: Boolean = false
    )

    private val _ui = MutableStateFlow(SearchUi())
    val ui: StateFlow<SearchUi> = _ui

    private var searchJob: Job? = null

    fun setQuery(query: String) {
        _ui.value = _ui.value.copy(query = query)
        searchJob?.cancel()
        if (query.isBlank()) {
            _ui.value = _ui.value.copy(results = emptyList(), loading = false, error = null, searched = false)
            return
        }
        searchJob = viewModelScope.launch {
            delay(350) // debounce
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                val results = tmdbRepository.searchRemote(query)
                _ui.value = _ui.value.copy(loading = false, results = results, searched = true)
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                // Offline fallback: search the user's local library instead.
                val local = movieDao.search(query)
                _ui.value = _ui.value.copy(
                    loading = false,
                    results = local,
                    searched = true,
                    error = if (local.isEmpty()) friendlyTmdbError(e) else null
                )
            }
        }
    }

    fun clear() {
        searchJob?.cancel()
        _ui.value = SearchUi()
    }
}

@Composable
fun SearchScreen(
    navController: NavController,
    vm: SearchViewModel = viewModel(factory = MovieBuzzViewModelFactory)
) {
    val ui by vm.ui.collectAsState()

    Scaffold(topBar = { TopAppBar(title = { Text("Search") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = ui.query,
                onValueChange = vm::setQuery,
                placeholder = { Text("Search movies by title…") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (ui.query.isNotEmpty()) {
                        IconButton(onClick = vm::clear) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )

            when {
                ui.query.isBlank() -> EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = "Find any movie",
                    message = "Type a title to search TMDB. Offline, your library is searched instead."
                )
                ui.loading -> LazyVerticalGrid(
                    columns = GridCells.Adaptive(124.dp),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(9) { PosterSkeleton() }
                }
                ui.results.isEmpty() -> {
                    // Capture into a local: 'ui' is a delegated property and Kotlin
                    // never smart-casts delegated properties.
                    val error = ui.error
                    if (error != null) {
                        ErrorState(message = error, onRetry = { vm.setQuery(ui.query) })
                    } else {
                        EmptyState(
                            icon = Icons.Outlined.SearchOff,
                            title = "No results for \"${ui.query}\"",
                            message = "Try a different title or check the spelling."
                        )
                    }
                }
                else -> {
                    ui.error?.let { Text(it, Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.labelMedium) }
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(124.dp),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(ui.results, key = { it.id }) { movie ->
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
        }
    }
}
