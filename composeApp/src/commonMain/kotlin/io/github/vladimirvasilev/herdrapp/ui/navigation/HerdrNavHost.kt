package io.github.vladimirvasilev.herdrapp.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import io.github.vladimirvasilev.herdrapp.AppContainer
import io.github.vladimirvasilev.herdrapp.ui.home.HomeRoute
import io.github.vladimirvasilev.herdrapp.ui.hosts.HostsRoute
import io.github.vladimirvasilev.herdrapp.ui.terminal.TerminalRoute
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

@Serializable
private data object Home

@Serializable
private data object Hosts

@Serializable
private data class Terminal(val paneId: String)

@Composable
internal fun HerdrNavHost(container: AppContainer, paneToOpen: StateFlow<String?>, onPaneOpened: () -> Unit) {
    val navController = rememberNavController()
    val requestedPane by paneToOpen.collectAsStateWithLifecycle()
    LaunchedEffect(requestedPane) {
        val paneId = requestedPane ?: return@LaunchedEffect
        // Back to Home first: a pane that is already open is replaced, not stacked under this one.
        navController.navigate(Terminal(paneId)) { popUpTo<Home>() }
        onPaneOpened()
    }
    // Home is the start destination, so going back from any screen means returning to it.
    val backToHome: () -> Unit = { navController.popBackStack<Home>(inclusive = false) }
    NavHost(navController, startDestination = Home) {
        composable<Home> {
            HomeRoute(
                container = container,
                onOpen = { paneId -> navController.navigate(Terminal(paneId)) { launchSingleTop = true } },
                onHosts = { navController.navigate(Hosts) { launchSingleTop = true } },
            )
        }
        composable<Hosts> {
            HostsRoute(container, onDone = backToHome)
        }
        composable<Terminal> { entry ->
            TerminalRoute(container, entry.toRoute<Terminal>().paneId, onBack = backToHome)
        }
    }
}
