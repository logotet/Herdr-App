package io.github.vladimirvasilev.herdrapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF89B4FA),
    secondary = Color(0xFFA6E3A1),
    background = Color(0xFF0C0C0F),
    surface = Color(0xFF15151B),
    error = Color(0xFFF38BA8)
)

@Composable
fun HerdrTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = DarkColors, content = content)
}
