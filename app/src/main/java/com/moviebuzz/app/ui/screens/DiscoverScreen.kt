package com.moviebuzz.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.moviebuzz.app.ui.components.PosterRowSkeleton
import com.moviebuzz.app.ui.nav.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val tmdbRepository: TmdbRepository,
    private val movieDao: MovieDao
) : ViewModel() {

    data class DiscoverUi(
        val loading: Boolean = true,
        val trending: List<MovieEntity> = emptyList(),
        val popular: List<MovieEntity> = emptyList(),
        val upcoming: List<MovieEntity> = emptyList(),
        val topRated: List<MovieEntity> = emptyList(),
        val recentlyReleased: List<MovieEntity> = emptyList(),
        val error: String? = null
    )

    private val _ui = MutableStateFlow(DiscoverUi())
    val ui: StateFlow<DiscoverUi> = _ui

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            _ui.value = _ui.value.copy(loading = true, error = null)
            try {
                val results = listOf(
                    tmdbRepository.trending(1),
                    tmdbRepository.popular(1),
                    tmdbRepository.upcoming(1),
                    tmdbRepository.topRated(1),
                    tmdbRepository.nowPlaying(1)
                )
                _ui.value = DiscoverUi(
                    loading = false,
                    trending = results[0],
                    popular = results[1],
                    upcoming = results[2],
                    topRated = results[3],
                    recentlyReleased = results[4]
                )
            } catch (e: Exception) {
                if (e is kotlinx.coroutines.CancellationException) throw e
                _ui.value = _ui.value.copy(loading = false, error = friendlyTmdbError(e))
            }
        }
    }
}

@Composable
fun DiscoverScreen(navController: NavController, vm: DiscoverViewModel = viewModel(factory = MovieBuzzViewModelFactory)) {
    val ui by vm.ui.collectAsState()
    var selected by remember { mutableIntStateOf(0) }
    val feeds = listOf("Trending", "Popular", "Upcoming", "Top rated", "In theaters")
    val lists = listOf(ui.trending, ui.popular, ui.upcoming, ui.topRated, ui.recentlyReleased)

    Scaffold(topBar = { TopAppBar(title = { Text("Discover") }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selected) {
                feeds.forEachIndexed { i, label ->
                    Tab(text = { Text(label) }, selected = selected == i, onClick = { selected = i })
                }
            }
            if (ui.loading) {
                LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(3) { PosterRowSkeleton() }
                }
            } else {
                val list = lists[selected]
                if (list.isEmpty()) {
                    if (ui.error != null) {
                        ErrorState(message = ui.error, onRetry = { vm.loadAll() })
                    } else {
                        EmptyState(
                            icon = androidx.compose.material.icons.Icons.Filled.Search,
                            title = "Nothing here yet",
                            message = "This feed returned no movies. Try again in a moment."
                        )
                    }
                } else {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        item { Text(feeds[selected], style = MaterialTheme.typography.headlineMedium) }
                        item {
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(list, key = { it.id }) { movie ->
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
    }
}
