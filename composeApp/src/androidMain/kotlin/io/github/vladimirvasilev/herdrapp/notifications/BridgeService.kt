package io.github.vladimirvasilev.herdrapp.notifications

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat

/**
 * Keeps the process in the foreground so the bridge connection, which HerdrApplication owns,
 * survives while the app is off the screen. It holds no state of its own: [BackgroundWatch]
 * decides when it runs.
 */
class BridgeService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = intent?.getParcelableExtra(EXTRA_NOTIFICATION, Notification::class.java)
        if (notification == null) {
            stopSelf()
            return START_NOT_STICKY
        }
        ServiceCompat.startForeground(
            this,
            AgentNotifier.CONNECTION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        // Not restarted after the system kills the process: nothing would reconnect it.
        return START_NOT_STICKY
    }

    companion object {
        private const val EXTRA_NOTIFICATION = "notification"

        /** False when Android refuses, which it does when the app is not on screen. */
        fun start(context: Context, notification: Notification): Boolean = try {
            val intent = Intent(context, BridgeService::class.java).putExtra(EXTRA_NOTIFICATION, notification)
            ContextCompat.startForegroundService(context, intent)
            true
        } catch (_: IllegalStateException) {
            false
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, BridgeService::class.java))
        }
    }
}
