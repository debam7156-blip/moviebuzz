package com.moviebuzz.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * MOVIEBUZZ cinematic palette.
 * Deep blacks and charcoal surfaces, warm gold accent — the look of a
 * darkened theater. Every color below is tuned for contrast on OLED.
 */
object MovieBuzzColors {
    // Surfaces
    val Black = Color(0xFF0A0A0E)
    val Charcoal = Color(0xFF131319)
    val CharcoalElevated = Color(0xFF1C1C25)
    val CharcoalOverlay = Color(0xFF24242F)
    val Outline = Color(0xFF2C2C38)

    // Accents
    val Gold = Color(0xFFE8C25A)
    val GoldBright = Color(0xFFFFD970)
    val GoldDim = Color(0xFF8A7433)

    // Text
    val TextPrimary = Color(0xFFF4F1EA)
    val TextSecondary = Color(0xFFA9A9B4)
    val Error = Color(0xFFFF6B6B)

    // Semantic
    val Success = Color(0xFF6BCB77)
    val BackdropScrim = Color(0xB3000000)
}

private val DarkColorScheme = darkColorScheme(
    primary = MovieBuzzColors.Gold,
    onPrimary = Color(0xFF1F1B08),
    primaryContainer = MovieBuzzColors.CharcoalOverlay,
    onPrimaryContainer = MovieBuzzColors.GoldBright,
    secondary = MovieBuzzColors.GoldDim,
    onSecondary = Color(0xFF1F1B08),
    tertiary = MovieBuzzColors.GoldBright,
    background = MovieBuzzColors.Black,
    onBackground = MovieBuzzColors.TextPrimary,
    surface = MovieBuzzColors.Charcoal,
    onSurface = MovieBuzzColors.TextPrimary,
    surfaceVariant = MovieBuzzColors.CharcoalElevated,
    onSurfaceVariant = MovieBuzzColors.TextSecondary,
    surfaceContainer = MovieBuzzColors.CharcoalElevated,
    surfaceContainerHigh = MovieBuzzColors.CharcoalOverlay,
    surfaceContainerHighest = MovieBuzzColors.CharcoalOverlay,
    outline = MovieBuzzColors.Outline,
    outlineVariant = MovieBuzzColors.Outline,
    error = MovieBuzzColors.Error,
    onError = MovieBuzzColors.Black,
    scrim = Color(0xFF000000)
)

@Composable
fun MovieBuzzTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = MovieBuzzTypography,
        content = content
    )
}
