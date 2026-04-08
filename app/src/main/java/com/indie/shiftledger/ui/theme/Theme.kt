package com.indie.shiftledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Cobalt,
    onPrimary = Color.White,
    primaryContainer = MistBlue,
    onPrimaryContainer = InkDark,
    secondary = Lagoon,
    onSecondary = Color.White,
    secondaryContainer = MintTint,
    onSecondaryContainer = InkDark,
    tertiary = Tangerine,
    onTertiary = Color.White,
    tertiaryContainer = PeachTint,
    onTertiaryContainer = InkDark,
    background = Canvas,
    onBackground = InkDark,
    surface = Paper,
    onSurface = InkDark,
    surfaceVariant = Clouded,
    onSurfaceVariant = Slate,
    outline = Outline,
)

private val DarkColors = darkColorScheme(
    primary = SkyLine,
    onPrimary = Night,
    primaryContainer = Color(0xFF17325F),
    onPrimaryContainer = NightText,
    secondary = MintGlow,
    onSecondary = Night,
    secondaryContainer = Color(0xFF103E39),
    onSecondaryContainer = NightText,
    tertiary = Ember,
    onTertiary = Night,
    tertiaryContainer = Color(0xFF4D2D19),
    onTertiaryContainer = NightText,
    background = Night,
    onBackground = NightText,
    surface = NightSurface,
    onSurface = NightText,
    surfaceVariant = NightVariant,
    onSurfaceVariant = NightMuted,
    outline = Color(0xFF425270),
)

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
