package io.github.vladimirvasilev.herdrapp.data.bridge

import io.github.vladimirvasilev.herdrapp.data.bridge.dto.BridgeError
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.CallRequest
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.CloseStreamRequest
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.InputRequest
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.OpenStreamRequest
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.RefreshRequest
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.ReleaseControlRequest
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.ResizeRequest
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.ScrollRequest
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.ServerMessage
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.TakeControlRequest
import io.github.vladimirvasilev.herdrapp.domain.SavedHost
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.min

sealed interface SocketStatus {
    data object Connecting : SocketStatus
    data object Open : SocketStatus
    data class Lost(val message: String) : SocketStatus
}

/**
 * Receives everything the bridge pushes. Called on the connection's dispatcher, from the loop that
 * reads the socket, so an implementation must not wait for a request's result here.
 */
interface BridgeListener {
    suspend fun onStatus(status: SocketStatus) {}
    suspend fun onMessage(message: ServerMessage) {}
}

/**
 * The WebSocket to one bridge: keeps it connected, sends requests and matches their results.
 *
 * [dispatcher] must be single-threaded. All mutable state here is touched only on it, which also
 * keeps JSON parsing off the main thread.
 */
class BridgeConnection(
    private val sockets: BridgeSocketFactory,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) {
    private val tracker = RequestTracker()
    private val listeners = mutableListOf<BridgeListener>()
    private var socket: BridgeSocket? = null
    private var job: Job? = null
    private var requestSeq = 0
    private val _currentHost = MutableStateFlow<SavedHost?>(null)
    val currentHost: StateFlow<SavedHost?> = _currentHost

    /** Register listeners while wiring the app, before the first [connect]. */
    fun addListener(listener: BridgeListener) {
        listeners += listener
    }

    fun connect(host: SavedHost) {
        _currentHost.value = host
        scope.launch(dispatcher) {
            val previous = job
            job = currentCoroutineContext()[Job]
            // Wait for the old socket to close so switching hosts never leaves two connections.
            previous?.cancelAndJoin()
            loop(host)
        }
    }

    private suspend fun loop(host: SavedHost) {
        var delayMs = INITIAL_BACKOFF_MS
        while (currentCoroutineContext().isActive) {
            notify(SocketStatus.Connecting)
            val lost = try {
                sockets.connect(host) { opened ->
                    // The socket library may run this block on its own threads.
                    withContext(dispatcher) {
                        socket = opened
                        delayMs = INITIAL_BACKOFF_MS
                        notify(SocketStatus.Open)
                        while (true) {
                            val text = opened.receive() ?: break
                            handleIncomingText(text)
                        }
                    }
                }
                "Connection closed"
            } catch (ce: CancellationException) {
                throw ce
            } catch (t: Throwable) {
                t.message ?: t::class.simpleName ?: "Connection error"
            } finally {
                socket = null
                tracker.failAll(BridgeError("connection_lost", "Connection lost"))
            }
            notify(SocketStatus.Lost(lost))
            // A clean close by the server backs off like an error instead of reconnecting in a tight loop.
            delay(delayMs)
            delayMs = min(delayMs * 2, MAX_BACKOFF_MS)
        }
    }

    private suspend fun notify(status: SocketStatus) {
        listeners.forEach { it.onStatus(status) }
    }

    private suspend fun handleIncomingText(text: String) {
        // One malformed message must not tear down the connection.
        val message = try {
            BridgeJson.parse(text)
        } catch (_: IllegalArgumentException) {
            return
        }
        if (message is ServerMessage.Result) {
            tracker.complete(message.value)
        } else {
            listeners.forEach { it.onMessage(message) }
        }
    }

    suspend fun refresh(): RequestOutcome = request { id -> BridgeJson.encode(RefreshRequest(id = id)) }

    suspend fun openStream(paneId: String, cols: Int, rows: Int): RequestOutcome =
        request { id -> BridgeJson.encode(OpenStreamRequest(id = id, paneId = paneId, cols = cols, rows = rows)) }

    /**
     * Opens a stream without waiting for the result. For listeners, which run inside the read loop
     * and so cannot wait for one.
     */
    suspend fun openStreamWithoutReply(paneId: String, cols: Int, rows: Int) {
        socket?.send(BridgeJson.encode(OpenStreamRequest(paneId = paneId, cols = cols, rows = rows)))
    }

    suspend fun closeStream(paneId: String): RequestOutcome =
        request { id -> BridgeJson.encode(CloseStreamRequest(id = id, paneId = paneId)) }

    suspend fun takeControl(paneId: String, cols: Int, rows: Int, takeover: Boolean = false): RequestOutcome =
        request { id ->
            BridgeJson.encode(
                TakeControlRequest(id = id, paneId = paneId, cols = cols, rows = rows, takeover = takeover.takeIf { it })
            )
        }

    suspend fun releaseControl(paneId: String): RequestOutcome =
        request { id -> BridgeJson.encode(ReleaseControlRequest(id = id, paneId = paneId)) }

    @OptIn(ExperimentalEncodingApi::class)
    suspend fun inputBytes(paneId: String, bytes: ByteArray): RequestOutcome = request { id ->
        BridgeJson.encode(InputRequest(id = id, paneId = paneId, bytes = Base64.Default.encode(bytes)))
    }

    suspend fun resize(paneId: String, cols: Int, rows: Int): RequestOutcome =
        request { id -> BridgeJson.encode(ResizeRequest(id = id, paneId = paneId, cols = cols, rows = rows)) }

    /** Scrolls the pane's view on the PC towards the newest output. The bridge needs control for it. */
    suspend fun scrollDown(paneId: String, lines: Int): RequestOutcome =
        request { id -> BridgeJson.encode(ScrollRequest(id = id, paneId = paneId, direction = "down", lines = lines)) }

    suspend fun sendKeys(paneId: String, keys: List<String>): RequestOutcome {
        val params = buildJsonObject {
            put("pane_id", paneId)
            put("keys", JsonArray(keys.map { JsonPrimitive(it) }))
        }
        return call("pane.send_keys", params)
    }

    suspend fun sendText(paneId: String, text: String): RequestOutcome {
        val params = buildJsonObject {
            put("pane_id", paneId)
            put("text", text)
        }
        return call("pane.send_text", params)
    }

    suspend fun renamePane(paneId: String, label: String): RequestOutcome =
        call("pane.rename", buildJsonObject { put("pane_id", paneId); put("label", label) })

    /** Closes the pane and stops the program running in it. */
    suspend fun closePane(paneId: String): RequestOutcome =
        call("pane.close", buildJsonObject { put("pane_id", paneId) })

    /** Adds a tab with one shell pane to the workspace, without moving the focus on the PC. */
    suspend fun createTab(workspaceId: String, label: String?): RequestOutcome {
        val params = buildJsonObject {
            put("workspace_id", workspaceId)
            if (label != null) put("label", label)
        }
        return call("tab.create", params)
    }

    suspend fun renameTab(tabId: String, label: String): RequestOutcome =
        call("tab.rename", buildJsonObject { put("tab_id", tabId); put("label", label) })

    /** Closes the tab with every pane in it. */
    suspend fun closeTab(tabId: String): RequestOutcome =
        call("tab.close", buildJsonObject { put("tab_id", tabId) })

    /** Types [text] into the pane and presses Enter in one herdr call; works without control. */
    suspend fun submitPrompt(paneId: String, text: String): RequestOutcome {
        val params = buildJsonObject {
            put("pane_id", paneId)
            if (text.isNotEmpty()) put("text", text)
            put("keys", JsonArray(listOf(JsonPrimitive("enter"))))
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

    suspend fun call(method: String, params: JsonElement): RequestOutcome =
        request { id -> BridgeJson.encode(CallRequest(id = id, method = method, params = params)) }

    private suspend fun request(
        timeoutMillis: Long = REQUEST_TIMEOUT_MS,
        body: (String) -> String,
    ): RequestOutcome = withContext(dispatcher) {
        val open = socket ?: return@withContext RequestOutcome.Failure(BridgeError("disconnected", "Not connected"))
        val id = "m${++requestSeq}"
        tracker.register(id)
        try {
            open.send(body(id))
        } catch (ce: CancellationException) {
            tracker.discard(id)
            throw ce
        } catch (_: Throwable) {
            tracker.discard(id)
            return@withContext RequestOutcome.Failure(BridgeError("connection_lost", "Connection lost"))
        }
        tracker.await(id, timeoutMillis)
    }

    internal val pendingRequests: Int get() = tracker.pendingCount

    private companion object {
        const val INITIAL_BACKOFF_MS = 1_000L
        const val MAX_BACKOFF_MS = 30_000L
        const val REQUEST_TIMEOUT_MS = 10_000L
    }
}
