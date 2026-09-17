package com.tanakhpoc.learner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Forest,
    onPrimary = Sand,
    primaryContainer = ChipHebrew,
    onPrimaryContainer = ForestDark,
    secondary = Accent,
    onSecondary = Color.White,
    secondaryContainer = ChipPhonetic,
    onSecondaryContainer = Ink,
    background = Sand,
    onBackground = Ink,
    surface = Color.White,
    onSurface = Ink,
    surfaceVariant = SandDark,
    onSurfaceVariant = InkMuted
)

private val Dark = darkColorScheme(
    primary = Color(0xFF9BC4B0),
    onPrimary = ForestDark,
    primaryContainer = Forest,
    onPrimaryContainer = Sand,
    secondary = Color(0xFFE0C56E),
    onSecondary = ForestDark,
    background = Color(0xFF121814),
    onBackground = Sand,
    surface = Color(0xFF1B221E),
    onSurface = Sand
)

@Composable
fun TanakhTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, content = content)
}
