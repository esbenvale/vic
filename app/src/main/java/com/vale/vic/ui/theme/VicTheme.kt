
package com.vale.vic.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Public VALE colour palette used across all VIC screens. */
object VicColors {
    val Black = Color(0xFF080A0C)
    val Charcoal = Color(0xFF0F1318)
    val Panel = Color(0xFF171C23)
    val Cyan = Color(0xFF00E5FF)
    val Blue = Color(0xFF2F7BFF)
    val TextPrimary = Color(0xFFE6EDF3)
    val TextMuted = Color(0xFF8B98A5)
}

private val VicDarkScheme = darkColorScheme(
    primary = VicColors.Cyan,
    secondary = VicColors.Blue,
    background = VicColors.Charcoal,
    surface = VicColors.Panel,
    onPrimary = VicColors.Black,
    onBackground = VicColors.TextPrimary,
    onSurface = VicColors.TextPrimary
)

@Composable
fun VicTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = VicDarkScheme, content = content)
}
