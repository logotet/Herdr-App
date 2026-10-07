package io.github.vladimirvasilev.herdrapp.ui.hosts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Scans a pairing QR code with whatever the platform offers. */
interface QrScanner {
    /**
     * Returns a function that starts a scan. [onResult] gets the scanned text, or null when the
     * scan was cancelled; it is still delivered if the screen was recreated in the meantime.
     */
    @Composable
    fun rememberLauncher(onResult: (String?) -> Unit): () -> Unit
}

val LocalQrScanner = staticCompositionLocalOf<QrScanner> {
    object : QrScanner {
        @Composable
        override fun rememberLauncher(onResult: (String?) -> Unit): () -> Unit = { onResult(null) }
    }
}
