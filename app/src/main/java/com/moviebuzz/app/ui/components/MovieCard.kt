package com.moviebuzz.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/**
 * The core movie card used across feeds, grids and detail rows.
 * Poster uses the true 2:3 aspect ratio so rows feel like a real collection;
 * rating chip sits on a scrim; typography stays at two lines max.
 */
@Composable
fun MovieCard(
    title: String,
    year: String,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rating: String? = null
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Box {
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = "$title poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                )
            } else {
                PosterPlaceholder(Modifier.fillMaxWidth().aspectRatio(2f / 3f), title)
            }
            if (rating != null) {
                RatingChip(rating, Modifier.align(Alignment.BottomStart))
            }
        }
        Spacer(Modifier.height(10.dp))
        Column(Modifier.padding(horizontal = 2.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (year.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = year,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Gold star chip on a dark scrim, bottom-left over the poster. */
@Composable
fun RatingChip(rating: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .padding(8.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xCC0A0A0E))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Icon(
            imageVector = Icons.Filled.Star,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.height(12.dp)
        )
        Text(
            text = rating,
            style = MaterialTheme.typography.labelSmall,
            color = Color(0xFFF4F1EA)
        )
    }
}

/**
 * Poster fallback when TMDB has no image: a film-slate look with the title's
 * initials rather than a flat grey block.
 */
@Composable
fun PosterPlaceholder(modifier: Modifier = Modifier, title: String) {
    Box(
        modifier = modifier
            .background(
                Brush.linearGradient(
                    listOf(
                        MaterialTheme.colorScheme.surfaceContainerHighest,
                        MaterialTheme.colorScheme.surfaceContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title.trim()
                .split(" ")
                .take(2)
                .mapNotNull { it.firstOrNull()?.uppercaseChar() }
                .joinToString(""),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
