package io.github.vladimirvasilev.herdrapp.ui

import androidx.compose.runtime.staticCompositionLocalOf

interface QrScannerService {
    fun scan(onResult: (String?) -> Unit)
}

val LocalQrScannerService = staticCompositionLocalOf<QrScannerService> {
    object : QrScannerService { override fun scan(onResult: (String?) -> Unit) = onResult(null) }
}
