package com.moviebuzz.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Modern loading skeletons. The shimmer sweep reads the theme's elevated
 * surface colors so it stays visually consistent in light and dark themes.
 */
@Composable
fun shimmerBrush(): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1200f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val base = MaterialTheme.colorScheme.surfaceContainerHighest
    val highlight = MaterialTheme.colorScheme.surfaceContainer
    val colors = listOf(base, highlight, base)
    return Brush.linearGradient(
        colors = colors,
        start = Offset(translate - 400f, 0f),
        end = Offset(translate, 400f)
    )
}

/** A single poster-shaped skeleton with the 2:3 poster aspect ratio. */
@Composable
fun PosterSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(2f / 3f)
            .clip(RoundedCornerShape(12.dp))
            .background(shimmerBrush())
    )
}

/** One row of poster skeletons — mirrors MovieCard's width in feed rows. */
@Composable
fun PosterRowSkeleton(count: Int = 6, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(count) {
            PosterSkeleton(Modifier.weight(1f))
        }
    }
}

/** A text-line skeleton with configurable width fraction. */
@Composable
fun TextSkeleton(widthFraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(14.dp)
            .clip(CircleShape)
            .background(shimmerBrush())
    )
}

/** Poster skeleton plus two text lines underneath — a MovieCard skeleton. */
@Composable
fun MovieCardSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        PosterSkeleton()
        Spacer(Modifier.height(8.dp))
        TextSkeleton(0.9f)
        Spacer(Modifier.height(6.dp))
        TextSkeleton(0.5f)
    }
}

/** A grid of card skeletons for full-screen loading states. */
@Composable
fun PosterGridSkeleton(count: Int = 9, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(count / 3) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(3) { MovieCardSkeleton(Modifier.weight(1f)) }
            }
        }
    }
}
