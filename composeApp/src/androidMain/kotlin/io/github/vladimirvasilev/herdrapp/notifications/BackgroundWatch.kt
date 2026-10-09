package io.github.vladimirvasilev.herdrapp.notifications

import android.content.Context
import io.github.vladimirvasilev.herdrapp.domain.ConnectionState
import io.github.vladimirvasilev.herdrapp.domain.SessionRepository
import io.github.vladimirvasilev.herdrapp.domain.SettingsRepository
import io.github.vladimirvasilev.herdrapp.domain.attention
import io.github.vladimirvasilev.herdrapp.domain.attentionEvents
import io.github.vladimirvasilev.herdrapp.domain.keepWatching
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

/**
 * Runs [BridgeService] for as long as it is worth staying connected off the screen, and turns
 * what changes in the session into notifications.
 */
internal class BackgroundWatch(
    private val context: Context,
    private val session: SessionRepository,
    private val settings: SettingsRepository,
    private val visibility: AppVisibility,
    private val notifier: AgentNotifier,
    /** How long the connection is kept after the last reason to keep it went away. */
    private val grace: Duration = 10.minutes,
) {
    /** The bridge the service is running for; null while it is stopped. */
    private var runningFor: String? = null

    fun start(scope: CoroutineScope) {
        scope.launch {
            notifier.createChannels()
            launch { runService() }
            launch { notifyAboutAgents() }
        }
    }

    private suspend fun runService() {
        combine(
            settings.backgroundAlerts,
            visibility.visible,
            session.connectionState,
            session.session,
        ) { enabled, visible, connection, now ->
            val bridgeName = (connection as? ConnectionState.Connected)?.bridgeName
            bridgeName.takeIf { keepWatching(enabled, visible, connected = it != null, session = now) }
        }.distinctUntilChanged().collectLatest { bridgeName ->
            if (bridgeName != null) {
                // Android only lets the service start while the app is on screen. It is therefore
                // started as soon as it may be needed, and not again while it runs.
                if (runningFor != bridgeName && BridgeService.start(context, notifier.connection(bridgeName))) {
                    runningFor = bridgeName
                }
            } else if (runningFor != null) {
                // A short gap, such as a reconnect, must not end it: it could not be started again.
                if (settings.backgroundAlerts.value) delay(grace)
                BridgeService.stop(context)
                runningFor = null
            }
        }
    }

    private suspend fun notifyAboutAgents() {
        var previous = session.session.value
        combine(session.session, settings.backgroundAlerts, visibility.visible, ::Triple).collect { (now, enabled, visible) ->
            val events = attentionEvents(previous, now)
            previous = now
            // On screen the app shows the agents itself, so its notifications are cleared.
            notifier.keepOnly(if (enabled && !visible) now.attention().keys else emptySet())
            if (enabled && !visible) events.forEach { notifier.show(it) }
        }
    }
}
