package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KioskDarkColorScheme = darkColorScheme(
    primary = NeonRose,
    secondary = NeonCyan,
    tertiary = ElectricViolet,
    background = DarkObsidian,
    surface = DarkObsidian,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = KioskDarkColorScheme,
        typography = Typography,
        content = content
    )
}

