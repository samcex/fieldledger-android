package com.indie.shiftledger.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = LedgerNavy,
    onPrimary = LedgerIvory,
    primaryContainer = LedgerBlueMist,
    onPrimaryContainer = LedgerInk,
    secondary = LedgerOxblood,
    onSecondary = LedgerIvory,
    secondaryContainer = LedgerRoseMist,
    onSecondaryContainer = LedgerInk,
    tertiary = LedgerBrass,
    onTertiary = LedgerInk,
    tertiaryContainer = LedgerSand,
    onTertiaryContainer = LedgerInk,
    background = LedgerParchment,
    onBackground = LedgerInk,
    surface = LedgerIvory,
    onSurface = LedgerInk,
    surfaceVariant = LedgerStone,
    onSurfaceVariant = LedgerSlate,
    outline = LedgerBorder,
    error = LedgerOxblood,
    onError = LedgerIvory,
    surfaceTint = LedgerBrass,
    scrim = Color.Black.copy(alpha = 0.45f),
)

private val DarkColors = darkColorScheme(
    primary = LedgerDarkText,
    onPrimary = LedgerDarkBackground,
    primaryContainer = LedgerDarkRaised,
    onPrimaryContainer = LedgerDarkText,
    secondary = LedgerDarkText,
    onSecondary = LedgerDarkBackground,
    secondaryContainer = LedgerDarkWine,
    onSecondaryContainer = LedgerDarkText,
    tertiary = LedgerDarkText,
    onTertiary = LedgerDarkBackground,
    tertiaryContainer = LedgerDarkOlive,
    onTertiaryContainer = LedgerDarkText,
    background = LedgerDarkBackground,
    onBackground = LedgerDarkText,
    surface = LedgerDarkSurface,
    onSurface = LedgerDarkText,
    surfaceVariant = LedgerDarkRaised,
    onSurfaceVariant = LedgerDarkMuted,
    outline = LedgerDarkOutline,
    error = Color(0xFFE2A192),
    onError = LedgerDarkBackground,
    surfaceTint = LedgerBrass,
    scrim = Color.Black.copy(alpha = 0.65f),
)

@Composable
fun FieldLedgerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FieldLedgerTypography,
        shapes = FieldLedgerShapes,
        content = content,
    )
}
