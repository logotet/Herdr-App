package io.github.vladimirvasilev.herdrapp

import android.app.Application
import io.github.vladimirvasilev.herdrapp.data.AndroidHostRepository
import io.github.vladimirvasilev.herdrapp.data.AndroidSettingsRepository
import io.github.vladimirvasilev.herdrapp.data.BridgeSessionRepository
import io.github.vladimirvasilev.herdrapp.data.BridgeTerminalRepository
import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeConnection
import io.github.vladimirvasilev.herdrapp.data.bridge.KtorBridgeSocketFactory
import io.github.vladimirvasilev.herdrapp.domain.HostRepository
import io.github.vladimirvasilev.herdrapp.domain.SessionRepository
import io.github.vladimirvasilev.herdrapp.domain.SettingsRepository
import io.github.vladimirvasilev.herdrapp.domain.TerminalRepository
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
    val sessionRepository: SessionRepository = BridgeSessionRepository(connection).also(connection::addListener)
    val terminalRepository: TerminalRepository =
        BridgeTerminalRepository(connection, appScope, bridgeDispatcher).also(connection::addListener)

    val hosts: HostRepository by lazy { AndroidHostRepository(this, appScope) }
    val settings: SettingsRepository by lazy { AndroidSettingsRepository(this, appScope) }
}
