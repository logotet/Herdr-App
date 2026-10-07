package io.github.vladimirvasilev.herdrapp.network

import io.github.vladimirvasilev.herdrapp.data.SavedHost
import io.github.vladimirvasilev.herdrapp.protocol.*
import io.github.vladimirvasilev.herdrapp.state.ConnectionState
import io.github.vladimirvasilev.herdrapp.state.HerdrStore
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.DefaultClientWebSocketSession
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.readText
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min

class BridgeConnection(
    private val client: HttpClient,
    private val store: HerdrStore,
    private val scope: CoroutineScope
) {
    private val tracker = RequestTracker()
    private val activeStreams = mutableMapOf<String, Pair<Int, Int>>()
    private var session: DefaultClientWebSocketSession? = null
    private var job: Job? = null
    private var requestSeq = 0
    private val _currentHost = MutableStateFlow<SavedHost?>(null)
    val currentHost: StateFlow<SavedHost?> = _currentHost

    fun connect(host: SavedHost) {
        _currentHost.value = host
        val previous = job
        job = scope.launch {
            // Wait for the old socket to close so switching hosts never leaves two connections.
            previous?.cancelAndJoin()
            loop(host)
        }
    }

    fun disconnect() {
        job?.cancel()
        session = null
        tracker.failAll(BridgeError("disconnected", "Disconnected"))
        store.setConnectionState(ConnectionState.Disconnected)
    }

    private suspend fun loop(host: SavedHost) {
        var delayMs = 1_000L
        while (currentCoroutineContext().isActive) {
            store.setConnectionState(ConnectionState.Connecting)
            try {
                client.webSocket(host.wsUrl, request = { header(HttpHeaders.Authorization, "Bearer ${host.token}") }) {
                    session = this
                    delayMs = 1_000L
                    activeStreams.toMap().forEach { (paneId, size) -> sendRaw(BridgeJson.encode(OpenStreamRequest(paneId = paneId, cols = size.first, rows = size.second))) }
                    for (frame in incoming) {
                        if (frame is Frame.Text) handleIncomingText(frame.readText())
                    }
                }
                store.setConnectionState(ConnectionState.Error("Connection closed"))
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                store.setConnectionState(ConnectionState.Error(t.message ?: t::class.simpleName ?: "Connection error"))
            }
            // A clean close by the server backs off like an error instead of reconnecting in a tight loop.
            tracker.failAll(BridgeError("connection_lost", "Connection lost"))
            session = null
            delay(delayMs)
            delayMs = min(delayMs * 2, 30_000L)
        }
    }

    fun handleIncomingText(text: String) {
        when (val message = BridgeJson.parse(text)) {
            is ServerMessage.Hello -> {
                store.setConnectionState(ConnectionState.Connected(message.value.name))
                store.setHerdrStatus(message.value.herdr)
            }
            is ServerMessage.Snapshot -> store.replace(message.value)
            is ServerMessage.AgentStatus -> store.onAgentStatus(message.value)
            is ServerMessage.Frame -> store.onFrame(message.value)
            is ServerMessage.Stream -> store.onStream(message.value)
            is ServerMessage.HerdrStatus -> store.setHerdrStatus(message.value.toInfo())
            is ServerMessage.Result -> tracker.complete(message.value)
            is ServerMessage.Pong -> tracker.complete(message.value)
            is ServerMessage.Unknown -> Unit
        }
    }

    suspend fun refresh(): RequestOutcome = request { id -> BridgeJson.encode(RefreshRequest(id = id)) }
    suspend fun ping(): RequestOutcome = request { id -> BridgeJson.encode(PingRequest(id = id)) }

    suspend fun openStream(paneId: String, cols: Int, rows: Int): RequestOutcome {
        activeStreams[paneId] = cols to rows
        return request { id -> BridgeJson.encode(OpenStreamRequest(id = id, paneId = paneId, cols = cols, rows = rows)) }
    }

    suspend fun closeStream(paneId: String): RequestOutcome {
        activeStreams.remove(paneId)
        return request { id -> BridgeJson.encode(CloseStreamRequest(id = id, paneId = paneId)) }
    }

    suspend fun takeControl(paneId: String, cols: Int, rows: Int, takeover: Boolean = false): RequestOutcome =
        request { id -> BridgeJson.encode(TakeControlRequest(id = id, paneId = paneId, cols = cols, rows = rows, takeover = takeover.takeIf { it })) }

    suspend fun releaseControl(paneId: String): RequestOutcome = request { id -> BridgeJson.encode(ReleaseControlRequest(id = id, paneId = paneId)) }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun inputBytes(paneId: String, bytes: ByteArray): RequestOutcome = request { id ->
        BridgeJson.encode(InputRequest(id = id, paneId = paneId, bytes = Base64.Default.encode(bytes)))
    }

    suspend fun inputText(paneId: String, text: String): RequestOutcome = request { id -> BridgeJson.encode(InputRequest(id = id, paneId = paneId, text = text)) }

    suspend fun resize(paneId: String, cols: Int, rows: Int): RequestOutcome {
        activeStreams[paneId] = cols to rows
        return request { id -> BridgeJson.encode(ResizeRequest(id = id, paneId = paneId, cols = cols, rows = rows)) }
    }

    suspend fun scroll(paneId: String, direction: ScrollDirection, lines: Int): RequestOutcome =
        request { id -> BridgeJson.encode(ScrollRequest(id = id, paneId = paneId, direction = direction, lines = lines)) }

    suspend fun sendKeys(paneId: String, keys: List<String>): RequestOutcome {
        val params = buildJsonObject {
            put("pane_id", paneId)
            put("keys", kotlinx.serialization.json.JsonArray(keys.map { kotlinx.serialization.json.JsonPrimitive(it) }))
        }
        return call("pane.send_keys", params)
    }

    suspend fun sendText(paneId: String, text: String): RequestOutcome {
        val params = buildJsonObject { put("pane_id", paneId); put("text", text) }
        return call("pane.send_text", params)
    }

    /** Types [text] into the pane and presses Enter in one herdr call; works without control. */
    suspend fun submitPrompt(paneId: String, text: String): RequestOutcome {
        val params = buildJsonObject {
            put("pane_id", paneId)
            if (text.isNotEmpty()) put("text", text)
            put("keys", kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive("enter"))))
        }
        return call("pane.send_input", params)
    }

    /** The pane's recent output (scrollback + screen) as ANSI text with soft-wrapped lines joined. */
    suspend fun readHistory(paneId: String, lines: Int = 1000): RequestOutcome {
        val params = buildJsonObject {
            put("pane_id", paneId)
            put("source", "recent_unwrapped")
            put("format", "ansi")
            put("strip_ansi", false)
            put("lines", lines)
        }
        return call("pane.read", params)
    }

    suspend fun call(method: String, params: JsonElement): RequestOutcome = request { id -> BridgeJson.encode(CallRequest(id = id, method = method, params = params)) }

    private suspend fun request(timeoutMillis: Long = 10_000L, body: (String) -> String): RequestOutcome {
        val id = "m${++requestSeq}"
        tracker.register(id)
        val ws = session ?: return RequestOutcome.Failure(BridgeError("disconnected", "Not connected"))
        sendRaw(body(id))
        return tracker.await(id, timeoutMillis)
    }

    private suspend fun sendRaw(text: String) { session?.send(Frame.Text(text)) }
}
