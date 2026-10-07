package io.github.vladimirvasilev.herdrapp

import android.app.Application
import io.github.vladimirvasilev.herdrapp.network.BridgeConnection
import io.github.vladimirvasilev.herdrapp.protocol.BridgeJson
import io.github.vladimirvasilev.herdrapp.state.HerdrStore
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
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
    val store by lazy { HerdrStore() }
    val connection by lazy {
        val client = HttpClient(OkHttp) {
            install(WebSockets)
            install(ContentNegotiation) { json(BridgeJson.json) }
        }
        BridgeConnection(client, store, appScope)
    }
    val hosts by lazy { AndroidHostRepository(this, appScope) }
    val settings by lazy { AndroidSettingsRepository(this, appScope) }
}
