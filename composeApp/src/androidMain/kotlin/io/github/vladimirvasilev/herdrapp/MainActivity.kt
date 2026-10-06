package io.github.vladimirvasilev.herdrapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import io.github.vladimirvasilev.herdrapp.network.BridgeConnection
import io.github.vladimirvasilev.herdrapp.protocol.BridgeJson
import io.github.vladimirvasilev.herdrapp.state.HerdrStore
import io.github.vladimirvasilev.herdrapp.ui.HerdrApp
import io.github.vladimirvasilev.herdrapp.ui.LocalQrScannerService
import io.github.vladimirvasilev.herdrapp.ui.QrScannerService
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val store = HerdrStore()
        val client = HttpClient(OkHttp) {
            install(WebSockets)
            install(ContentNegotiation) { json(BridgeJson.json) }
        }
        val connection = BridgeConnection(client, store, appScope)
        val hostRepository = AndroidHostRepository(this, appScope)
        val settingsRepository = AndroidSettingsRepository(this, appScope)
        setContent {
            val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                QrScanResultDispatcher.consume(result.resultCode == Activity.RESULT_OK, result.data?.getStringExtra(QrScannerActivity.EXTRA_RESULT))
            }
            val scanner = remember {
                object : QrScannerService {
                    override fun scan(onResult: (String?) -> Unit) {
                        QrScanResultDispatcher.callback = onResult
                        launcher.launch(Intent(this@MainActivity, QrScannerActivity::class.java))
                    }
                }
            }
            CompositionLocalProvider(LocalQrScannerService provides scanner) {
                HerdrApp(store, connection, hostRepository, settingsRepository)
            }
        }
    }
}

private object QrScanResultDispatcher {
    var callback: ((String?) -> Unit)? = null
    fun consume(ok: Boolean, value: String?) { callback?.invoke(if (ok) value else null); callback = null }
}
