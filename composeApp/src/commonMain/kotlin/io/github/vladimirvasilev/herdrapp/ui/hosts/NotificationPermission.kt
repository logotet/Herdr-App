package io.github.vladimirvasilev.herdrapp.ui.hosts

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Asks the user for permission to show notifications, the way the platform does it. */
interface NotificationPermission {
    /** Returns a function that asks. [onResult] gets true when notifications may be shown. */
    @Composable
    fun rememberRequest(onResult: (Boolean) -> Unit): () -> Unit
}

val LocalNotificationPermission = staticCompositionLocalOf<NotificationPermission> {
    object : NotificationPermission {
        @Composable
        override fun rememberRequest(onResult: (Boolean) -> Unit): () -> Unit = { onResult(true) }
    }
}
