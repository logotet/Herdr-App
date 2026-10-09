package io.github.vladimirvasilev.herdrapp

import android.Manifest
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import io.github.vladimirvasilev.herdrapp.notifications.AgentNotifier
import io.github.vladimirvasilev.herdrapp.ui.HerdrApp
import io.github.vladimirvasilev.herdrapp.ui.hosts.LocalNotificationPermission
import io.github.vladimirvasilev.herdrapp.ui.hosts.LocalQrScanner
import io.github.vladimirvasilev.herdrapp.ui.hosts.NotificationPermission
import io.github.vladimirvasilev.herdrapp.ui.hosts.QrScanner
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    /** The pane a tapped notification asks for, until the navigation has gone there. */
    private val paneToOpen = MutableStateFlow<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // A recreated activity gets the same intent again; its pane was opened the first time.
        if (savedInstanceState == null) paneToOpen.value = intent.paneId()
        val container = (application as HerdrApplication).container
        setContent {
            CompositionLocalProvider(
                LocalQrScanner provides CameraQrScanner,
                LocalNotificationPermission provides SystemNotificationPermission,
            ) {
                HerdrApp(container, paneToOpen, onPaneOpened = { paneToOpen.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        paneToOpen.value = intent.paneId()
    }

    private fun Intent.paneId(): String? = getStringExtra(AgentNotifier.EXTRA_PANE_ID)
}

private object CameraQrScanner : QrScanner {
    @Composable
    override fun rememberLauncher(onResult: (String?) -> Unit): () -> Unit {
        val currentOnResult by rememberUpdatedState(onResult)
        val launcher = rememberLauncherForActivityResult(ScanPairingQr()) { currentOnResult(it) }
        return { launcher.launch(Unit) }
    }
}

private object SystemNotificationPermission : NotificationPermission {
    @Composable
    override fun rememberRequest(onResult: (Boolean) -> Unit): () -> Unit {
        val currentOnResult by rememberUpdatedState(onResult)
        val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            currentOnResult(it)
        }
        return { launcher.launch(Manifest.permission.POST_NOTIFICATIONS) }
    }
}
