package com.moviebuzz.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

object MovieBuzzColors {
    val Black = androidx.compose.ui.graphics.Color(0xFF0B0B0F)
    val Charcoal = androidx.compose.ui.graphics.Color(0xFF14141B)
    val CharcoalElevated = androidx.compose.ui.graphics.Color(0xFF1E1E27)
    val Gold = androidx.compose.ui.graphics.Color(0xFFFFD54A)
    val GoldDim = androidx.compose.ui.graphics.Color(0xFFB29600)
    val TextPrimary = androidx.compose.ui.graphics.Color(0xFFF5F5F5)
    val TextSecondary = androidx.compose.ui.graphics.Color(0xFFAAAAAA)
    val Error = androidx.compose.ui.graphics.Color(0xFFFF5252)
}

private val DarkColorScheme = androidx.compose.material3.darkColorScheme(
    primary = MovieBuzzColors.Gold,
    onPrimary = androidx.compose.ui.graphics.Color(0xFF1A1A00),
    secondary = MovieBuzzColors.GoldDim,
    onSecondary = androidx.compose.ui.graphics.Color(0xFF1A1A00),
    tertiary = MovieBuzzColors.Gold,
    background = MovieBuzzColors.Black,
    onBackground = MovieBuzzColors.TextPrimary,
    surface = MovieBuzzColors.Charcoal,
    onSurface = MovieBuzzColors.TextPrimary,
    surfaceVariant = MovieBuzzColors.CharcoalElevated,
    onSurfaceVariant = MovieBuzzColors.TextSecondary,
    error = MovieBuzzColors.Error,
    onError = MovieBuzzColors.Black
)

@Composable
fun MovieBuzzTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MovieBuzzTypography,
        content = content
    )
}
