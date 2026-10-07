package io.github.vladimirvasilev.herdrapp

import android.app.Application
import io.github.vladimirvasilev.herdrapp.data.AndroidHostRepository
import io.github.vladimirvasilev.herdrapp.data.AndroidSettingsRepository
import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeConnection
import io.github.vladimirvasilev.herdrapp.data.bridge.KtorBridgeSocketFactory
import io.github.vladimirvasilev.herdrapp.state.HerdrStore
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
    val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // One thread for everything that touches the socket, so its state needs no locks and JSON
    // parsing stays off the main thread.
    private val bridgeDispatcher = Dispatchers.Default.limitedParallelism(1)

    val store by lazy { HerdrStore() }
    val connection by lazy {
        val client = HttpClient(OkHttp) {
            install(WebSockets)
        }
        BridgeConnection(KtorBridgeSocketFactory(client), appScope, bridgeDispatcher).apply { addListener(store) }
    }
    val hosts by lazy { AndroidHostRepository(this, appScope) }
    val settings by lazy { AndroidSettingsRepository(this, appScope) }
}
