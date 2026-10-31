package com.omnichat.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8B7CFF),
    secondary = Color(0xFF4FD1C5),
    background = Color(0xFF0E0F14),
    surface = Color(0xFF16181F),
    surfaceVariant = Color(0xFF20232C),
    primaryContainer = Color(0xFF3A3370),
)

@Composable
fun OmniTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
