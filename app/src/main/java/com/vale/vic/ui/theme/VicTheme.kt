package com.vale.vic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VicColors = darkColorScheme(
    primary = Color(0xFF36A9FF),
    onPrimary = Color(0xFF001D32),
    secondary = Color(0xFF83CFFF),
    background = Color(0xFF080B12),
    onBackground = Color(0xFFF4F7FB),
    surface = Color(0xFF111827),
    onSurface = Color(0xFFF4F7FB)
)

@Composable
fun VicTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = VicColors,
        content = content
    )
}
