package com.moviebuzz.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.moviebuzz.app.data.db.CustomListEntity
import com.moviebuzz.app.data.db.MovieEntity
import com.moviebuzz.app.data.tmdb.TmdbImages
import com.moviebuzz.app.ui.MovieBuzzViewModelFactory
import com.moviebuzz.app.ui.components.EmptyState
import com.moviebuzz.app.ui.components.MovieCard
import com.moviebuzz.app.ui.nav.Routes

private val RatingStars = 1..10

@Composable
fun MovieDetailScreen(
    navController: NavController,
    movieId: Long,
    vm: MovieDetailViewModel = viewModel(factory = MovieBuzzViewModelFactory)
) {
    val ui by vm.ui.collectAsState()
    val watchHistory by vm.watchHistory.collectAsState()
    val memberListIds by vm.memberListIds.collectAsState()
    val collections by vm.collections.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(movieId) { vm.load(movieId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(ui.movie?.title ?: "", maxLines = 1)
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        val m = ui.movie
        if (ui.loading) {
            Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (ui.notFound || m == null) {
            Box(Modifier.fillMaxSize().padding(padding)) {
                EmptyState(
                    icon = Icons.Outlined.Search,
                    title = "Movie not found",
                    message = "This movie is not in your library yet and could not be fetched. " +
                        "Check your connection, or find it via Discover or Search."
                )
            }
        } else if (m != null) {
            LazyColumn(modifier = Modifier.fillMaxSize().padding(padding)) {

                // ---- Hero ----
                item {
                    HeroSection(m, onPlayTrailer = {
                        val key = m.trailerKey
                        if (!key.isNullOrBlank()) {
                            context.startActivity(
                                Intent(
                                    Intent.ACTION_VIEW,
                                    Uri.parse("https://www.youtube.com/watch?v=$key")
                                )
                            )
                        }
                    })
                }

                // ---- Offline notice + actions ----
                item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        ui.offlineNotice?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { vm.toggleWatched() },
                                colors = if (m.watched) ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ) else ButtonDefaults.buttonColors()
                            ) {
                                Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(if (m.watched) "Watched" else "Mark watched")
                            }
                            OutlinedButton(onClick = { vm.toggleWatchlist() }) {
                                Text(if (m.watchlist) "✓ In watchlist" else "Add to watchlist")
                            }
                            IconButton(onClick = { vm.toggleFavorite() }) {
                                Icon(
                                    imageVector = if (m.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                    contentDescription = "Toggle favourite",
                                    tint = if (m.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // ---- Metadata ----
                item {
                    Column(Modifier.padding(16.dp)) {
                        if (m.genres.isNotEmpty()) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                m.genres.take(4).forEach { genre ->
                                    AssistChip(onClick = {}, label = { Text(genre) })
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                        }
                        m.director?.let {
                            Text("Directed by $it", style = MaterialTheme.typography.bodyMedium)
                            Spacer(Modifier.height(4.dp))
                        }
                        m.tmdbVoteAverage?.let { average ->
                            Text(
                                "TMDB ★ %.1f (${m.tmdbVoteCount ?: 0} votes)".format(average),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                        }
                        m.overview?.let {
                            Spacer(Modifier.height(8.dp))
                            Text("Overview", style = MaterialTheme.typography.titleMedium)
                            Text(
                                it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ---- Cast ----
                if (m.cast.isNotEmpty()) item {
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Cast", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(m.cast.take(20)) { index, name ->
                                val character = m.castCharacters.getOrNull(index)
                                val profile = m.castProfiles.getOrNull(index)?.let { TmdbImages.profileUrl(it) }
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.width(84.dp)
                                ) {
                                    if (profile != null) {
                                        AsyncImage(
                                            model = profile,
                                            contentDescription = "$name headshot",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(84.dp)
                                                .clip(RoundedCornerShape(42.dp))
                                        )
                                    } else {
                                        Surface(
                                            shape = RoundedCornerShape(42.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            modifier = Modifier.size(84.dp)
                                        ) {}
                                    }
                                    Spacer(Modifier.height(6.dp))
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                    character?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ---- Rating ----
                item {
                    Column(Modifier.padding(16.dp)) {
                        Text("Your rating", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Row {
                            RatingStars.forEach { step ->
                                val current = m.personalRating ?: 0.0
                                val filled = current >= step - 0.5
                                IconButton(
                                    onClick = {
                                        val newRating = if (m.personalRating == step.toDouble()) null else step.toDouble()
                                        vm.setRating(newRating)
                                    },
                                    modifier = Modifier.size(30.dp)
                                ) {
                                    Icon(
                                        imageVector = if (filled) Icons.Filled.Star else Icons.Outlined.StarBorder,
                                        contentDescription = "Rate $step",
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                        if (m.personalRating != null) {
                            Text(
                                "${m.personalRating}/10 — tap the same star to clear",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // ---- Review ----
                item {
                    var review by remember(m.id) { mutableStateOf(m.review ?: "") }
                    Column(Modifier.padding(horizontal = 16.dp)) {
                        Text("Your review", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        OutlinedTextField(
                            value = review,
                            onValueChange = { review = it },
                            placeholder = { Text("Write your thoughts…") },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 3
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { vm.saveReview(review) }) {
                            Text("Save review")
                        }
                    }
                }

                // ---- Collections membership ----
                item {
                    CollectionMembershipSection(
                        collections = collections,
                        memberListIds = memberListIds,
                        onToggle = { vm.toggleCollection(it) },
                        onCreate = { name, desc -> vm.createCollectionAndAdd(name, desc) }
                    )
                }

                // ---- Similar ----
                if (ui.similar.isNotEmpty()) item {
                    SectionHeader("Similar movies")
                    PosterRow(ui.similar) { navController.navigate(Routes.movieDetail(it.id)) }
                }

                // ---- Recommended ----
                if (ui.recommended.isNotEmpty()) item {
                    SectionHeader("Recommended for you")
                    PosterRow(ui.recommended) { navController.navigate(Routes.movieDetail(it.id)) }
                }

                // ---- History ----
                if (watchHistory.isNotEmpty()) item {
                    Column(Modifier.padding(16.dp)) {
                        Text("Your watch history", style = MaterialTheme.typography.titleMedium)
                        watchHistory.forEach { event ->
                            Text(
                                "• ${event.watchedOn}" + (event.ratingAtWatch?.let { " — rated $it" } ?: ""),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}

@Composable
private fun HeroSection(m: MovieEntity, onPlayTrailer: () -> Unit) {
    Box(Modifier.fillMaxWidth()) {
        val backdropUrl = TmdbImages.backdropUrl(m.backdropPath ?: m.posterPath)
        if (backdropUrl != null) {
            AsyncImage(
                model = backdropUrl,
                contentDescription = "${m.title} backdrop",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(240.dp)
            )
        } else {
            Box(Modifier.fillMaxWidth().height(240.dp).background(MaterialTheme.colorScheme.surfaceVariant))
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0x99000000), Color(0xFF0B0B0F))
                    )
                )
        )
        if (!m.trailerKey.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = Color(0x99000000),
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp)
            ) {
                IconButton(onClick = onPlayTrailer) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = "Play trailer")
                }
            }
        }
        Column(Modifier.padding(16.dp)) {
            Spacer(Modifier.height(128.dp))
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
                    m.tagline?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        listOfNotNull(
                            m.year?.toString(),
                            m.runtimeMinutes?.let { "$it min" }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        title,
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun PosterRow(movies: List<MovieEntity>, onMovieClick: (MovieEntity) -> Unit) {
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
                onClick = { onMovieClick(movie) }
            )
        }
    }
}

@Composable
private fun CollectionMembershipSection(
    collections: List<CustomListEntity>,
    memberListIds: List<Long>,
    onToggle: (Long) -> Unit,
    onCreate: (String, String?) -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    Column(Modifier.padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Collections", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(8.dp))
            TextButton(onClick = { showCreate = true }) { Text("+ New") }
        }
        Spacer(Modifier.height(4.dp))
        if (collections.isEmpty()) {
            Text(
                "No collections yet — create one to organize this movie.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            collections.forEach { list ->
                val isMember = list.id in memberListIds
                AssistChip(
                    onClick = { onToggle(list.id) },
                    label = { Text(if (isMember) "✓ ${list.name}" else list.name) }
                )
                Spacer(Modifier.height(4.dp))
            }
        }
    }
    if (showCreate) {
        var name by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreate = false },
            title = { Text("New collection") },
            text = {
                Column {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description (optional)") },
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (name.isNotBlank()) {
                            onCreate(name, description)
                            showCreate = false
                        }
                    }
                ) { Text("Create & add") }
            },
            dismissButton = { TextButton(onClick = { showCreate = false }) { Text("Cancel") } }
        )
    }
}
