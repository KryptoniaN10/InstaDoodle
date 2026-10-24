package com.instadoodle.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

object Brand {
    val Purple = Color(0xFF833AB4)
    val Pink = Color(0xFFE1306C)
    val Orange = Color(0xFFF77737)
    val Yellow = Color(0xFFFCAF45)
    val gradient = Brush.linearGradient(listOf(Purple, Pink, Orange))
    val splashGradient = Brush.verticalGradient(listOf(Purple, Pink, Orange, Yellow))
}

private val LightColors = lightColorScheme(
    primary = Color(0xFFD62F6B),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD9E3),
    onPrimaryContainer = Color(0xFF3E0020),
    secondary = Brand.Purple,
    secondaryContainer = Color(0xFFF1DBFF),
    onSecondaryContainer = Color(0xFF2E004E),
    tertiary = Brand.Orange,
    background = Color(0xFFFFF8FA),
    surface = Color(0xFFFFF8FA),
    surfaceVariant = Color(0xFFF6E9EF),
    surfaceContainerLow = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFFBEEF3),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF7BA6),
    onPrimary = Color(0xFF5E0031),
    primaryContainer = Color(0xFF85114A),
    onPrimaryContainer = Color(0xFFFFD9E3),
    secondary = Color(0xFFD7A8FF),
    secondaryContainer = Color(0xFF5B2A82),
    onSecondaryContainer = Color(0xFFF1DBFF),
    tertiary = Color(0xFFFFB68D),
    background = Color(0xFF141116),
    surface = Color(0xFF141116),
    surfaceVariant = Color(0xFF2A242D),
    surfaceContainerLow = Color(0xFF1D191F),
    surfaceContainer = Color(0xFF221D24),
)

@Composable
fun InstaDoodleTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        shapes = Shapes(
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(20.dp),
            large = RoundedCornerShape(28.dp),
        ),
        content = content,
    )
}
