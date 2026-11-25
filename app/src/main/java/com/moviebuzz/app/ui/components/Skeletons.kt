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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

@Composable
fun shimmerBrush(): androidx.compose.ui.graphics.Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val translate = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerTranslate"
    )
    val colors = listOf(
        androidx.compose.ui.graphics.Color(0xFF1C1C1E),
        androidx.compose.ui.graphics.Color(0xFF2A2A2E),
        androidx.compose.ui.graphics.Color( 0xFF1C1C1E)
    )
    return androidx.compose.ui.graphics.Brush.linearGradient(
        colors = colors,
        start = androidx.compose.ui.geometry.Offset(translate.value - 200, 0f),
        end = androidx.compose.ui.geometry.Offset(translate.value, 200f)
    )
}

@Composable
fun PosterSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .height(160.dp)
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .background(shimmerBrush())
    )
}

@Composable
fun RowSkeleton(modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(20.dp).clip(CircleShape).background(shimmerBrush()))
        Box(Modifier.fillMaxWidth().height(14.dp).clip(CircleShape).background(shimmerBrush()))
        Box(Modifier.fillMaxWidth(0.6f).height(14.dp).clip(CircleShape).background(shimmerBrush()))
    }
}

@Composable
fun PosterRowSkeleton(count: Int = 6) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        repeat(count) { PosterSkeleton(Modifier.weight(1f)) }
    }
}
