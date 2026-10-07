package io.github.vladimirvasilev.herdrapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import io.github.vladimirvasilev.herdrapp.ui.HerdrApp
import io.github.vladimirvasilev.herdrapp.ui.hosts.LocalQrScanner
import io.github.vladimirvasilev.herdrapp.ui.hosts.QrScanner

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val container = (application as HerdrApplication).container
        setContent {
            CompositionLocalProvider(LocalQrScanner provides CameraQrScanner) {
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
