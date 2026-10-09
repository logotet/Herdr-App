package io.github.vladimirvasilev.herdrapp

import android.app.Application
import io.github.vladimirvasilev.herdrapp.data.AndroidHostRepository
import io.github.vladimirvasilev.herdrapp.data.AndroidSettingsRepository
import io.github.vladimirvasilev.herdrapp.data.BridgeSessionRepository
import io.github.vladimirvasilev.herdrapp.data.BridgeTerminalRepository
import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeConnection
import io.github.vladimirvasilev.herdrapp.data.bridge.KtorBridgeSocketFactory
import io.github.vladimirvasilev.herdrapp.notifications.AgentNotifier
import io.github.vladimirvasilev.herdrapp.notifications.AppVisibility
import io.github.vladimirvasilev.herdrapp.notifications.BackgroundWatch
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.websocket.WebSockets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Process-wide objects. They live here rather than in MainActivity so an activity recreation
 * (rotation, process-kept relaunch) reuses the one bridge connection instead of leaking another
 * reconnect loop.
 */
class HerdrApplication : Application() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // One thread for everything that touches the socket, so its state needs no locks and JSON
    // parsing stays off the main thread.
    private val bridgeDispatcher = Dispatchers.Default.limitedParallelism(1)

    private val connection = BridgeConnection(
        sockets = KtorBridgeSocketFactory(HttpClient(OkHttp) { install(WebSockets) }),
        scope = appScope,
        dispatcher = bridgeDispatcher,
    )

    // Both repositories listen to the connection, so they exist before anything can connect.
    private val sessionRepository = BridgeSessionRepository(connection).also(connection::addListener)
    private val terminalRepository =
        BridgeTerminalRepository(connection, appScope, bridgeDispatcher).also(connection::addListener)

    // Lazy because the storage repositories need the application context.
    val container: AppContainer by lazy {
        AppContainer(
            session = sessionRepository,
            terminal = terminalRepository,
            hosts = AndroidHostRepository(this, appScope),
            settings = AndroidSettingsRepository(this, appScope),
        )
    }

    override fun onCreate() {
        super.onCreate()
        // Created here so it sees the first activity start.
        val visibility = AppVisibility(this)
        BackgroundWatch(this, container.session, container.settings, visibility, AgentNotifier(this)).start(appScope)
    }
}
