package io.github.vladimirvasilev.herdrapp

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import io.github.vladimirvasilev.herdrapp.ui.HerdrApp
import io.github.vladimirvasilev.herdrapp.ui.hosts.LocalNotificationPermission
import io.github.vladimirvasilev.herdrapp.ui.hosts.LocalQrScanner
import io.github.vladimirvasilev.herdrapp.ui.hosts.NotificationPermission
import io.github.vladimirvasilev.herdrapp.ui.hosts.QrScanner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as HerdrApplication).container
        setContent {
            CompositionLocalProvider(
                LocalQrScanner provides CameraQrScanner,
                LocalNotificationPermission provides SystemNotificationPermission,
            ) {
                HerdrApp(container)
            }
        }
    }
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
