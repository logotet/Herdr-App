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
import io.github.vladimirvasilev.herdrapp.domain.SettingsRepository
import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeConnection
import io.github.vladimirvasilev.herdrapp.state.HerdrStore
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
    store: HerdrStore,
    connection: BridgeConnection,
    hosts: HostRepository,
    settings: SettingsRepository,
) {
    HerdrTheme {
        val scope = rememberCoroutineScope()
        var screen by remember { mutableStateOf<Screen>(Screen.Home) }
        val hostList by hosts.hosts.collectAsState()
        LaunchedEffect(hostList) {
            val current = connection.currentHost.value
            if (current == null && hostList.isNotEmpty()) connection.connect(hostList.first())
        }
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // Edge-to-edge: keep content clear of the status/navigation bars and the keyboard,
            // so the extra-keys bar sits above the IME and the terminal shrinks to fit.
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                when (val s = screen) {
                    Screen.Home -> HomeScreen(
                        store = store,
                        connection = connection,
                        hostList = hostList,
                        onOpen = { paneId ->
                            val idx = store.agents.value.indexOfFirst { it.paneId == paneId }.coerceAtLeast(0)
                            screen = Screen.Terminal(idx)
                        },
                        onHosts = { screen = Screen.Hosts },
                        onRefresh = { scope.launch { connection.refresh() } },
                    )
                    Screen.Hosts -> HostsScreen(
                        hostList = hostList,
                        onBack = { screen = Screen.Home },
                        onSave = { host ->
                            scope.launch {
                                hosts.upsert(host)
                                connection.connect(host)
                                screen = Screen.Home
                            }
                        },
                        onSelect = { host ->
                            connection.connect(host)
                            screen = Screen.Home
                        },
                        onDelete = { host -> scope.launch { hosts.delete(host.id) } },
                    )
                    is Screen.Terminal -> TerminalPagerScreen(
                        initialPage = s.initialPage,
                        store = store,
                        connection = connection,
                        settings = settings,
                        onBack = { screen = Screen.Home },
                    )
                }
            }
        }
    }
}
