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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.moviebuzz.app.data.db.MovieDao
import com.moviebuzz.app.data.db.MovieEntity
import com.moviebuzz.app.data.tmdb.TmdbRepository
import com.moviebuzz.app.ui.components.MovieCard
import com.moviebuzz.app.ui.components.PosterRowSkeleton
import com.moviebuzz.app.ui.nav.Routes
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class DiscoverViewModel(
    private val tmdbRepository: TmdbRepository?,
    private val movieDao: MovieDao
) : ViewModel() {

    data class DiscoverUi(
        val loading: Boolean = true,
        val trending: List<MovieEntity> = emptyList(),
        val popular: List<MovieEntity> = emptyList(),
        val upcoming: List<MovieEntity> = emptyList(),
        val topRated: List<MovieEntity> = emptyList()
    )

    private val _ui = MutableStateFlow(DiscoverUi())
    val ui: StateFlow<DiscoverUi> = _ui

    init { loadAll() }

    fun loadAll() {
        viewModelScope.launch {
            if (tmdbRepository == null) {
                _ui.value = DiscoverUi(loading = false)
                return@launch
            }
            val t = tmdbRepository.trending(1)
            val p = tmdbRepository.popular(1)
            val u = tmdbRepository.upcoming(1)
            val r = tmdbRepository.topRated(1)
            _ui.value = DiscoverUi(loading = false, trending = t, popular = p, upcoming = u, topRated = r)
        }
    }
}

@Composable
fun DiscoverScreen(navController: NavController, vm: DiscoverViewModel = viewModel()) {
    val ui by vm.ui.collectAsState()
    var selected by remember { mutableIntStateOf(0) }
    val feeds = listOf("Trending", "Popular", "Upcoming", "Top rated")
    val lists = listOf(ui.trending, ui.popular, ui.upcoming, ui.topRated)

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
                    Column(Modifier.padding(24.dp)) {
                        Text("No data", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "TMDB is unreachable right now (no key configured or offline). Your library still works offline.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                                        posterUrl = com.moviebuzz.app.data.tmdb.TmdbImages.posterUrl(movie.posterPath),
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
