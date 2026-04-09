package com.indie.shiftledger.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Pine,
    onPrimary = Color.White,
    primaryContainer = SageWash,
    onPrimaryContainer = Ink,
    secondary = Terracotta,
    onSecondary = Color.White,
    secondaryContainer = ClayWash,
    onSecondaryContainer = Ink,
    tertiary = Olive,
    onTertiary = Color.White,
    tertiaryContainer = BrassWash,
    onTertiaryContainer = Ink,
    background = Parchment,
    onBackground = Ink,
    surface = Bone,
    onSurface = Ink,
    surfaceVariant = Fog,
    onSurfaceVariant = Bark,
    outline = BorderSand,
    surfaceTint = Pine,
)

private val DarkColors = darkColorScheme(
    primary = MeadowGlow,
    onPrimary = Midnight,
    primaryContainer = Color(0xFF20372E),
    onPrimaryContainer = MidnightText,
    secondary = EmberGlow,
    onSecondary = Midnight,
    secondaryContainer = Color(0xFF4B3023),
    onSecondaryContainer = MidnightText,
    tertiary = SandLight,
    onTertiary = Midnight,
    tertiaryContainer = Color(0xFF4C412C),
    onTertiaryContainer = MidnightText,
    background = Midnight,
    onBackground = MidnightText,
    surface = MidnightSurface,
    onSurface = MidnightText,
    surfaceVariant = MidnightVariant,
    onSurfaceVariant = MidnightMuted,
    outline = Color(0xFF5A4D42),
    surfaceTint = MeadowGlow,
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
