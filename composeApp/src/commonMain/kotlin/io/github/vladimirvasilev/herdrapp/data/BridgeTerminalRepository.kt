package io.github.vladimirvasilev.herdrapp.data

import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeConnection
import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeListener
import io.github.vladimirvasilev.herdrapp.data.bridge.RequestOutcome
import io.github.vladimirvasilev.herdrapp.data.bridge.SocketStatus
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.ServerMessage
import io.github.vladimirvasilev.herdrapp.data.bridge.parsePaneRead
import io.github.vladimirvasilev.herdrapp.domain.CommandResult
import io.github.vladimirvasilev.herdrapp.domain.HistoryResult
import io.github.vladimirvasilev.herdrapp.domain.StreamMode
import io.github.vladimirvasilev.herdrapp.domain.TerminalFrame
import io.github.vladimirvasilev.herdrapp.domain.TerminalRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.StreamMode as StreamModeDto
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.TerminalFrame as FrameDto

/**
 * Terminal streams over the bridge. [dispatcher] is the connection's single-threaded dispatcher;
 * the per-pane state here is only touched on it.
 */
class BridgeTerminalRepository(
    private val connection: BridgeConnection,
    private val scope: CoroutineScope,
    private val dispatcher: CoroutineDispatcher,
) : TerminalRepository, BridgeListener {

    private class PaneStream {
        // Unbounded so a frame is never dropped; a delta that goes missing corrupts the screen.
        val frames = Channel<TerminalFrame>(Channel.UNLIMITED)
        /** The grid size last asked for; null until the stream has been opened. */
        var size: Pair<Int, Int>? = null
        var lastSeq: Long? = null
        /** Set after a gap: deltas are useless until the next full repaint arrives. */
        var awaitingFull = false
    }

    private val panes = mutableMapOf<String, PaneStream>()
    private val _streamModes = MutableStateFlow<Map<String, StreamMode>>(emptyMap())
    override val streamModes: StateFlow<Map<String, StreamMode>> = _streamModes.asStateFlow()

    override fun frames(paneId: String): Flow<TerminalFrame> = flow {
        val channel = withContext(dispatcher) { panes.getOrPut(paneId) { PaneStream() }.frames }
        for (frame in channel) emit(frame)
    }

    override suspend fun open(paneId: String, cols: Int, rows: Int) = withContext(dispatcher) {
        val pane = panes.getOrPut(paneId) { PaneStream() }
        pane.size = cols to rows
        connection.openStream(paneId, cols, rows)
        Unit
    }

    override fun close(paneId: String) {
        scope.launch(dispatcher) {
            panes.remove(paneId)?.frames?.close()
            _streamModes.value -= paneId
            connection.closeStream(paneId)
        }
    }

    override suspend fun resize(paneId: String, cols: Int, rows: Int) = withContext(dispatcher) {
        panes[paneId]?.size = cols to rows
        connection.resize(paneId, cols, rows)
        Unit
    }

    override suspend fun takeControl(paneId: String, cols: Int, rows: Int) {
        connection.takeControl(paneId, cols, rows, takeover = true)
    }

    override suspend fun releaseControl(paneId: String) {
        connection.releaseControl(paneId)
    }

    override suspend fun sendInput(paneId: String, bytes: ByteArray) {
        connection.inputBytes(paneId, bytes)
    }

    override suspend fun sendKeys(paneId: String, keys: List<String>) {
        connection.sendKeys(paneId, keys)
    }

    override suspend fun sendText(paneId: String, text: String) {
        connection.sendText(paneId, text)
    }

    override suspend fun submitPrompt(paneId: String, text: String): CommandResult =
        when (val outcome = connection.submitPrompt(paneId, text)) {
            is RequestOutcome.Success -> CommandResult.Success
            is RequestOutcome.Failure -> CommandResult.Failure(outcome.error.message ?: outcome.error.code)
        }

    override suspend fun readHistory(paneId: String): HistoryResult =
        when (val outcome = connection.readHistory(paneId)) {
            is RequestOutcome.Success -> parsePaneRead(outcome.data)?.let { HistoryResult.Loaded(it) } ?: HistoryResult.Empty
            is RequestOutcome.Failure -> HistoryResult.Failed(outcome.error.message ?: outcome.error.code)
        }

    override suspend fun onStatus(status: SocketStatus) {
        when (status) {
            SocketStatus.Connecting -> Unit
            // Streams belong to one socket, so a new socket needs them opened again.
            SocketStatus.Open -> panes.forEach { (paneId, pane) ->
                val (cols, rows) = pane.size ?: return@forEach
                connection.openStreamWithoutReply(paneId, cols, rows)
            }
            is SocketStatus.Lost -> _streamModes.value = emptyMap()
        }
    }

    override suspend fun onMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.Frame -> onFrame(message.value)
            is ServerMessage.Stream -> {
                val paneId = message.value.paneId
                when (message.value.mode) {
                    StreamModeDto.OBSERVE -> _streamModes.value += paneId to StreamMode.OBSERVE
                    StreamModeDto.CONTROL -> _streamModes.value += paneId to StreamMode.CONTROL
                    StreamModeDto.CLOSED -> _streamModes.value -= paneId
                }
            }
            else -> Unit
        }
    }

    @OptIn(ExperimentalEncodingApi::class)
    private fun onFrame(frame: FrameDto) {
        val pane = panes[frame.paneId] ?: return
        if (!inSequence(pane, frame)) return
        val bytes = try {
            Base64.Default.decode(frame.bytes)
        } catch (_: IllegalArgumentException) {
            resync(frame.paneId, pane)
            return
        }
        pane.frames.trySend(TerminalFrame(bytes, frame.width ?: 0, frame.height ?: 0))
    }

    /**
     * False when [frame] must not be drawn because it follows a gap, so the screen it patches is
     * unknown. herdr numbers the frames of a stream 1, 2, 3 and starts every stream, and every
     * resize, with a full frame.
     */
    private fun inSequence(pane: PaneStream, frame: FrameDto): Boolean {
        if (frame.full) {
            pane.awaitingFull = false
            pane.lastSeq = frame.seq
            return true
        }
        if (pane.awaitingFull) return false
        val seq = frame.seq ?: return true
        val last = pane.lastSeq
        if (last != null && seq != last + 1) {
            resync(frame.paneId, pane)
            return false
        }
        pane.lastSeq = seq
        return true
    }

    /** Restarts the stream in its current mode; a restarted stream begins with a full frame. */
    private fun resync(paneId: String, pane: PaneStream) {
        pane.awaitingFull = true
        val (cols, rows) = pane.size ?: return
        val controlling = _streamModes.value[paneId] == StreamMode.CONTROL
        scope.launch(dispatcher) {
            if (controlling) {
                connection.takeControl(paneId, cols, rows, takeover = true)
            } else {
                connection.openStream(paneId, cols, rows)
            }
        }
    }
}
