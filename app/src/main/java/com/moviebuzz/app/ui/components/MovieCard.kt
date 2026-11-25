package com.moviebuzz.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.android3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun MovieCard(
    title: String,
    year: String,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(120.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Box {
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = "$title poster",
                    contentScale = ContentScale.Crop,
                    imported = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                PosterPlaceholder(height = 180.dp, title = title)
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MovieCardTitle(),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
    }
}

@Composable
private fun MovieCardTitle(): androidx.compose.material3.Typography {
    return MaterialTheme.typography
}

@Composable
fun PosterPlaceholder(height: androidx.compose.ui.unit.Dp, title: String) {
    Box(
        modifier = Modifier
            . fillerMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title.take(3).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
