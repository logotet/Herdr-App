package io.github.vladimirvasilev.herdrapp.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary = Color(0xFF89B4FA),
    secondary = Color(0xFFA6E3A1),
    background = Color(0xFF0C0C0F),
    surface = Color(0xFF15151B),
    error = Color(0xFFF38BA8)
)

/** The app's colors that have no slot in the Material scheme. Read them with [HerdrTheme.colors]. */
@Immutable
data class HerdrColors(
    val bannerConnected: Color = Color(0xFF23452C),
    val bannerError: Color = Color(0xFF4A2027),
    val bannerNeutral: Color = Color(0xFF303044),
    val bannerWarning: Color = Color(0xFF5A3418),
    val onBannerWarning: Color = Color(0xFFFFD8A8),
    val statusBlocked: Color = Color(0xFFF38BA8),
    val statusWorking: Color = Color(0xFF89B4FA),
    val statusDone: Color = Color(0xFFA6E3A1),
    val statusIdle: Color = Color(0xFF9399B2),
    val onStatus: Color = Color.Black,
    /** Secondary text and outlines. */
    val muted: Color = Color.Gray,
    val workspaceLabel: Color = Color(0xFFBAC2DE),
    /** Cards and the bars around the terminal. */
    val panel: Color = Color(0xFF181825),
    val panelInset: Color = Color(0xFF11111B),
    val terminalText: Color = Color(0xFFCDD6F4),
    val historyBadge: Color = Color(0xFFF9E2AF),
    val onHistoryBadge: Color = Color.Black,
    val notice: Color = Color(0xE6313244),
    val onNotice: Color = Color.White,
    /** Content drawn on a primary-colored control. */
    val onAction: Color = Color.Black,
)

private val LocalHerdrColors = staticCompositionLocalOf { HerdrColors() }

object HerdrTheme {
    val colors: HerdrColors
        @Composable
        @ReadOnlyComposable
        get() = LocalHerdrColors.current
}

@Composable
fun HerdrTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalHerdrColors provides HerdrColors()) {
        MaterialTheme(colorScheme = DarkColors, content = content)
    }
}
