package io.github.vladimirvasilev.herdrapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vladimirvasilev.herdrapp.AppContainer
import io.github.vladimirvasilev.herdrapp.ui.navigation.HerdrNavHost
import io.github.vladimirvasilev.herdrapp.ui.theme.HerdrTheme
import kotlinx.coroutines.flow.StateFlow

/**
 * [paneToOpen] is a pane the platform wants shown, for example from a tapped notification;
 * [onPaneOpened] is called once the app has gone there.
 */
@Composable
fun HerdrApp(container: AppContainer, paneToOpen: StateFlow<String?>, onPaneOpened: () -> Unit) {
    HerdrTheme {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            // Edge-to-edge: keep content clear of the status/navigation bars and the keyboard,
            // so the extra-keys bar sits above the IME and the terminal shrinks to fit.
            Box(Modifier.fillMaxSize().safeDrawingPadding()) {
                HerdrNavHost(container, paneToOpen, onPaneOpened)
            }
        }
    }
}
