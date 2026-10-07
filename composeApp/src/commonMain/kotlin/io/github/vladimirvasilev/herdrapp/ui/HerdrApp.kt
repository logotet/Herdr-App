package io.github.vladimirvasilev.herdrapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.vladimirvasilev.herdrapp.AppContainer
import io.github.vladimirvasilev.herdrapp.ui.home.HomeRoute
import io.github.vladimirvasilev.herdrapp.ui.hosts.HostsRoute
import io.github.vladimirvasilev.herdrapp.ui.terminal.TerminalRoute
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme

private sealed interface Screen {
    data object Home : Screen
    data object Hosts : Screen
    data class Terminal(val paneId: String) : Screen
}

@Composable
fun HerdrApp(container: AppContainer) {
    HerdrTheme {
        var screen by remember { mutableStateOf<Screen>(Screen.Home) }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // Edge-to-edge: keep content clear of the status/navigation bars and the keyboard,
            // so the extra-keys bar sits above the IME and the terminal shrinks to fit.
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                when (val current = screen) {
                    Screen.Home -> HomeRoute(
                        container = container,
                        onOpen = { paneId -> screen = Screen.Terminal(paneId) },
                        onHosts = { screen = Screen.Hosts },
                    )
                    Screen.Hosts -> HostsRoute(container, onDone = { screen = Screen.Home })
                    is Screen.Terminal -> TerminalRoute(container, current.paneId, onBack = { screen = Screen.Home })
                }
            }
        }
    }
}
