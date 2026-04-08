package com.indie.shiftledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = DeepForest,
    secondary = BurnishedGold,
    tertiary = Atlantic,
    background = ClayBackground,
    surface = SoftSurface,
    surfaceVariant = Mist,
    onPrimary = androidx.compose.ui.graphics.Color.White,
    onSecondary = androidx.compose.ui.graphics.Color.White,
    onTertiary = androidx.compose.ui.graphics.Color.White,
    onBackground = Ink,
    onSurface = Ink,
)

private val DarkColors = darkColorScheme(
    primary = ColorPaletteDark.primary,
    secondary = ColorPaletteDark.secondary,
    tertiary = ColorPaletteDark.tertiary,
    background = ColorPaletteDark.background,
    surface = ColorPaletteDark.surface,
    onPrimary = ColorPaletteDark.onPrimary,
    onSecondary = ColorPaletteDark.onSecondary,
    onTertiary = ColorPaletteDark.onTertiary,
    onBackground = ColorPaletteDark.onBackground,
    onSurface = ColorPaletteDark.onSurface,
)

private object ColorPaletteDark {
    val primary = DeepForest
    val secondary = BurnishedGold
    val tertiary = Atlantic
    val background = Ink
    val surface = androidx.compose.ui.graphics.Color(0xFF28231F)
    val onPrimary = androidx.compose.ui.graphics.Color.White
    val onSecondary = androidx.compose.ui.graphics.Color.White
    val onTertiary = androidx.compose.ui.graphics.Color.White
    val onBackground = androidx.compose.ui.graphics.Color(0xFFF8F2E9)
    val onSurface = androidx.compose.ui.graphics.Color(0xFFF8F2E9)
}

@Composable
fun FieldLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = FieldLedgerTypography,
        content = content,
    )
}
