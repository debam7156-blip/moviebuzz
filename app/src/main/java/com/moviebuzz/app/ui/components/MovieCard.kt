package com.moviebuzz.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieCard(
    title: String,
    year: String,
    posterUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    rating: String? = null,
    onLongClick: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .width(124.dp)
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = { onLongClick?.invoke() }
            )
    ) {
        Box {
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = "$title poster",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            } else {
                PosterPlaceholder(height = 180.dp, title = title)
            }
            if (rating != null) {
                Text(
                    text = "★ $rating",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xCC000000))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        if (year.isNotBlank()) {
            Text(
                text = year,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

@Composable
fun PosterPlaceholder(height: Dp, title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title.take(3).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
