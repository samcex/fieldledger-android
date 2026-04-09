package com.indie.shiftledger.ui.theme

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

private val AmoledDarkColors = darkColorScheme(
    primary = MeadowGlow,
    onPrimary = AmoledBlack,
    primaryContainer = Color(0xFF132019),
    onPrimaryContainer = MidnightText,
    secondary = EmberGlow,
    onSecondary = AmoledBlack,
    secondaryContainer = Color(0xFF3C241A),
    onSecondaryContainer = MidnightText,
    tertiary = SandLight,
    onTertiary = AmoledBlack,
    tertiaryContainer = Color(0xFF403624),
    onTertiaryContainer = MidnightText,
    background = AmoledBlack,
    onBackground = MidnightText,
    surface = AmoledSurface,
    onSurface = MidnightText,
    surfaceVariant = AmoledVariant,
    onSurfaceVariant = MidnightMuted,
    outline = AmoledOutline,
    surfaceTint = MeadowGlow,
)

@Composable
fun FieldLedgerTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) AmoledDarkColors else LightColors,
        typography = FieldLedgerTypography,
        content = content,
    )
}
