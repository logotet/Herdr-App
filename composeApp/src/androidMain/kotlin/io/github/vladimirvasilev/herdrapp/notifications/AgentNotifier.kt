package io.github.vladimirvasilev.herdrapp.notifications

import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import herdrapp.composeapp.generated.resources.Res
import herdrapp.composeapp.generated.resources.notify_channel_connection
import herdrapp.composeapp.generated.resources.notify_channel_finished
import herdrapp.composeapp.generated.resources.notify_channel_waiting
import herdrapp.composeapp.generated.resources.notify_connected_body
import herdrapp.composeapp.generated.resources.notify_connected_title
import herdrapp.composeapp.generated.resources.notify_finished
import herdrapp.composeapp.generated.resources.notify_in_workspace
import herdrapp.composeapp.generated.resources.notify_waiting
import io.github.vladimirvasilev.herdrapp.MainActivity
import io.github.vladimirvasilev.herdrapp.R
import io.github.vladimirvasilev.herdrapp.domain.AttentionEvent
import io.github.vladimirvasilev.herdrapp.domain.AttentionKind
import org.jetbrains.compose.resources.getString

/** Builds and shows the app's notifications: one per agent that wants attention. */
internal class AgentNotifier(private val context: Context) {
    private val manager = NotificationManagerCompat.from(context)
    private val shown = mutableSetOf<String>()

    /** Waiting and finished are separate channels so each can be silenced in the system settings. */
    suspend fun createChannels() {
        manager.createNotificationChannelsCompat(
            listOf(
                channel(CONNECTION, NotificationManagerCompat.IMPORTANCE_LOW, getString(Res.string.notify_channel_connection)),
                channel(WAITING, NotificationManagerCompat.IMPORTANCE_HIGH, getString(Res.string.notify_channel_waiting)),
                channel(FINISHED, NotificationManagerCompat.IMPORTANCE_DEFAULT, getString(Res.string.notify_channel_finished)),
            )
        )
    }

    private fun channel(id: String, importance: Int, name: String) =
        NotificationChannelCompat.Builder(id, importance).setName(name).build()

    /** The permanent notification Android requires while the connection is kept in the background. */
    suspend fun connection(bridgeName: String): Notification =
        NotificationCompat.Builder(context, CONNECTION)
            .setSmallIcon(R.drawable.ic_stat_agent)
            .setContentTitle(getString(Res.string.notify_connected_title, bridgeName))
            .setContentText(getString(Res.string.notify_connected_body))
            .setContentIntent(open(paneId = null))
            .setOngoing(true)
            .build()

    suspend fun show(event: AttentionEvent) {
        if (!manager.areNotificationsEnabled()) return
        val what = getString(
            when (event.kind) {
                AttentionKind.BLOCKED -> Res.string.notify_waiting
                AttentionKind.DONE -> Res.string.notify_finished
            }
        )
        val text = event.workspaceLabel?.let { getString(Res.string.notify_in_workspace, what, it) } ?: what
        val channel = if (event.kind == AttentionKind.BLOCKED) WAITING else FINISHED
        // The lock screen shows this version: no terminal content.
        val public = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_agent)
            .setContentTitle(event.title)
            .setContentText(text)
        val notification = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_agent)
            .setContentTitle(event.title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(listOfNotNull(text, event.preview).joinToString("\n")))
            .setContentIntent(open(event.paneId))
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setPublicVersion(public.build())
            .build()
        shown += event.paneId
        @Suppress("MissingPermission") // checked above with areNotificationsEnabled
        manager.notify(event.paneId, AGENT_ID, notification)
    }

    /** Removes the notification of every agent that is not in [paneIds] any more. */
    fun keepOnly(paneIds: Set<String>) {
        (shown - paneIds).forEach { paneId ->
            manager.cancel(paneId, AGENT_ID)
            shown -= paneId
        }
    }

    private fun open(paneId: String?): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
            // The action makes the intents of two panes different, so each keeps its own pane id.
            .setAction("open:$paneId")
            .putExtra(EXTRA_PANE_ID, paneId)
            .addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    companion object {
        /** The pane a notification was about, on the intent that opens the app. */
        const val EXTRA_PANE_ID = "pane_id"
        const val CONNECTION_ID = 1
        private const val AGENT_ID = 2
        private const val CONNECTION = "connection"
        private const val WAITING = "waiting"
        private const val FINISHED = "finished"
    }
}
