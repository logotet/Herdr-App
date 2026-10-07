package io.github.vladimirvasilev.herdrapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import io.github.vladimirvasilev.herdrapp.domain.HostRepository
import io.github.vladimirvasilev.herdrapp.domain.SessionRepository
import io.github.vladimirvasilev.herdrapp.domain.SettingsRepository
import io.github.vladimirvasilev.herdrapp.domain.TerminalRepository
import io.github.vladimirvasilev.herdrapp.ui.home.HomeScreen
import io.github.vladimirvasilev.herdrapp.ui.hosts.HostsScreen
import io.github.vladimirvasilev.herdrapp.ui.terminal.TerminalPagerScreen
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme
import kotlinx.coroutines.launch

private sealed interface Screen {
    data object Home : Screen
    data object Hosts : Screen
    data class Terminal(val initialPage: Int) : Screen
}

@Composable
fun HerdrApp(
    session: SessionRepository,
    terminal: TerminalRepository,
    hosts: HostRepository,
    settings: SettingsRepository,
) {
    HerdrTheme {
        val scope = rememberCoroutineScope()
        var screen by remember { mutableStateOf<Screen>(Screen.Home) }
        val hostList by hosts.hosts.collectAsState()
        LaunchedEffect(hostList) {
            val current = session.currentHost.value
            if (current == null && hostList.isNotEmpty()) session.connect(hostList.first())
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // Edge-to-edge: keep content clear of the status/navigation bars and the keyboard,
            // so the extra-keys bar sits above the IME and the terminal shrinks to fit.
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                when (val s = screen) {
                    Screen.Home -> HomeScreen(
                        session = session,
                        hostList = hostList,
                        onOpen = { paneId ->
                            val agents = session.session.value.agents
                            screen = Screen.Terminal(agents.indexOfFirst { it.paneId == paneId }.coerceAtLeast(0))
                        },
                        onHosts = { screen = Screen.Hosts },
                        onRefresh = { scope.launch { session.refresh() } },
                    )
                    Screen.Hosts -> HostsScreen(
                        hostList = hostList,
                        onBack = { screen = Screen.Home },
                        onSave = { host ->
                            scope.launch {
                                hosts.upsert(host)
                                session.connect(host)
                                screen = Screen.Home
                            }
                        },
                        onSelect = { host ->
                            session.connect(host)
                            screen = Screen.Home
                        },
                        onDelete = { host -> scope.launch { hosts.delete(host.id) } },
                    )
                    is Screen.Terminal -> TerminalPagerScreen(
                        initialPage = s.initialPage,
                        session = session,
                        terminal = terminal,
                        settings = settings,
                        onBack = { screen = Screen.Home },
                    )
                }
            }
        }
    }
}
