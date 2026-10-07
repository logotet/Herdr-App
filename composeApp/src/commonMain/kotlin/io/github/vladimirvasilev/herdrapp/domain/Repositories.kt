package io.github.vladimirvasilev.herdrapp.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/** The connection to a bridge and the herdr session it reports. */
interface SessionRepository {
    val connectionState: StateFlow<ConnectionState>
    val currentHost: StateFlow<SavedHost?>
    /** False while the bridge is connected but herdr itself is not running on the PC. */
    val herdrAvailable: StateFlow<Boolean>
    val session: StateFlow<Session>

    fun connect(host: SavedHost)
    suspend fun refresh()
}

/** Live terminal streams and the input that can be sent to a pane. */
interface TerminalRepository {
    /** Panes with an open stream. A pane that is absent has no stream. */
    val streamModes: StateFlow<Map<String, StreamMode>>

    /** Frames for [paneId] in order, none dropped. Collect from one place at a time. */
    fun frames(paneId: String): Flow<TerminalFrame>

    suspend fun open(paneId: String, cols: Int, rows: Int)
    /** Returns at once; the stream is closed in the background so it outlives the caller. */
    fun close(paneId: String)
    suspend fun resize(paneId: String, cols: Int, rows: Int)
    suspend fun takeControl(paneId: String, cols: Int, rows: Int)
    suspend fun releaseControl(paneId: String)

    /** Raw keyboard bytes; the bridge accepts them only while this client has control. */
    suspend fun sendInput(paneId: String, bytes: ByteArray)
    suspend fun sendKeys(paneId: String, keys: List<String>)
    suspend fun sendText(paneId: String, text: String)
    /** Types [text] and presses Enter; works without control. */
    suspend fun submitPrompt(paneId: String, text: String): CommandResult
    suspend fun readHistory(paneId: String): HistoryResult
}
