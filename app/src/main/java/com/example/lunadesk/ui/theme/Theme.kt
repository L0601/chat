package com.example.lunadesk.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Forest,
    secondary = Clay,
    tertiary = Moss,
    background = Paper,
    surface = Paper
)

private val DarkColors = darkColorScheme(
    primary = Sand,
    secondary = Moss,
    tertiary = Clay
)

@Composable
fun LunaDeskTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        content = content
    )
}

