package io.github.vladimirvasilev.herdrapp.protocol

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class BridgeHello(
    val type: String = "hello",
    val protocol: Int,
    @SerialName("bridge_version") val bridgeVersion: String,
    val name: String,
    val herdr: HerdrInfo
)

@Serializable
data class HerdrInfo(val version: String? = null, val protocol: Int? = null, val available: Boolean = false)

@Serializable
data class HerdrStatusMessage(
    val type: String = "herdr_status",
    val version: String? = null,
    val protocol: Int? = null,
    val available: Boolean = false
) {
    fun toInfo(): HerdrInfo = HerdrInfo(version = version, protocol = protocol, available = available)
}

@Serializable
data class BridgeSnapshot(
    val type: String = "snapshot",
    val workspaces: List<WorkspaceInfo> = emptyList(),
    val tabs: List<TabInfo> = emptyList(),
    val panes: List<PaneInfo> = emptyList(),
    val agents: List<AgentInfo> = emptyList(),
    @SerialName("focused_workspace_id") val focusedWorkspaceId: String? = null,
    @SerialName("focused_tab_id") val focusedTabId: String? = null,
    @SerialName("focused_pane_id") val focusedPaneId: String? = null,
    val previews: Map<String, String> = emptyMap()
)

@Serializable
data class WorkspaceInfo(
    @SerialName("workspace_id") val workspaceId: String,
    val number: Int = 0,
    val label: String = "",
    @SerialName("agent_status") val agentStatus: AgentStatus = AgentStatus.UNKNOWN,
    val focused: Boolean = false,
    @SerialName("pane_count") val paneCount: Int = 0,
    @SerialName("tab_count") val tabCount: Int = 0,
    @SerialName("active_tab_id") val activeTabId: String? = null
)

@Serializable
data class TabInfo(
    @SerialName("tab_id") val tabId: String,
    @SerialName("workspace_id") val workspaceId: String,
    val number: Int = 0,
    val label: String = "",
    @SerialName("agent_status") val agentStatus: AgentStatus = AgentStatus.UNKNOWN,
    val focused: Boolean = false,
    @SerialName("pane_count") val paneCount: Int = 0
)

@Serializable
data class PaneInfo(
    @SerialName("pane_id") val paneId: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("tab_id") val tabId: String? = null,
    @SerialName("terminal_id") val terminalId: String? = null,
    val cwd: String? = null,
    val agent: String? = null,
    @SerialName("agent_status") val agentStatus: AgentStatus = AgentStatus.UNKNOWN,
    val focused: Boolean = false,
    val label: String? = null,
    @SerialName("terminal_title") val terminalTitle: String? = null,
    @SerialName("terminal_title_stripped") val terminalTitleStripped: String? = null,
    val scroll: PaneScroll? = null,
    val revision: Long? = null
)

@Serializable
data class AgentInfo(
    @SerialName("pane_id") val paneId: String,
    @SerialName("workspace_id") val workspaceId: String,
    @SerialName("tab_id") val tabId: String? = null,
    @SerialName("terminal_id") val terminalId: String? = null,
    val cwd: String? = null,
    val agent: String? = null,
    @SerialName("agent_status") val agentStatus: AgentStatus = AgentStatus.UNKNOWN,
    val focused: Boolean = false,
    val label: String? = null,
    @SerialName("terminal_title") val terminalTitle: String? = null,
    @SerialName("terminal_title_stripped") val terminalTitleStripped: String? = null,
    val scroll: PaneScroll? = null,
    val revision: Long? = null,
    @SerialName("state_change_seq") val stateChangeSeq: Long = 0
) {
    fun title(): String = terminalTitleStripped?.takeIf { it.isNotBlank() } ?: label?.takeIf { it.isNotBlank() } ?: agent ?: paneId
}

@Serializable
data class PaneScroll(
    @SerialName("offset_from_bottom") val offsetFromBottom: Int = 0,
    @SerialName("max_offset_from_bottom") val maxOffsetFromBottom: Int = 0,
    @SerialName("viewport_rows") val viewportRows: Int = 0
)

@Serializable
enum class AgentStatus {
    @SerialName("idle") IDLE,
    @SerialName("working") WORKING,
    @SerialName("blocked") BLOCKED,
    @SerialName("done") DONE,
    @SerialName("unknown") UNKNOWN
}

@Serializable
data class AgentStatusTransition(
    val type: String = "agent_status",
    @SerialName("pane_id") val paneId: String,
    @SerialName("workspace_id") val workspaceId: String,
    val agent: String? = null,
    val from: AgentStatus = AgentStatus.UNKNOWN,
    val to: AgentStatus = AgentStatus.UNKNOWN,
    val title: String? = null,
    @SerialName("workspace_label") val workspaceLabel: String? = null
)

@Serializable
data class TerminalFrame(
    val type: String = "frame",
    @SerialName("pane_id") val paneId: String,
    val seq: Long,
    val full: Boolean = false,
    val width: Int,
    val height: Int,
    val bytes: String
)

@Serializable
data class StreamState(
    val type: String = "stream",
    @SerialName("pane_id") val paneId: String,
    val mode: StreamMode,
    val reason: String? = null
)

@Serializable
enum class StreamMode {
    @SerialName("observe") OBSERVE,
    @SerialName("control") CONTROL,
    @SerialName("closed") CLOSED
}

@Serializable
data class BridgeResultMessage(
    val type: String = "result",
    val id: String,
    val ok: Boolean,
    val data: JsonElement? = null,
    val error: BridgeError? = null
)

@Serializable
data class BridgeError(val code: String, val message: String? = null)

@Serializable
data class PongMessage(val type: String = "pong", val id: String? = null)

@Serializable
data class PaneReadResult(
    val type: String = "pane_read",
    val read: PaneReadPayload
)

@Serializable
data class PaneReadPayload(
    val text: String,
    @SerialName("pane_id") val paneId: String,
    val truncated: Boolean = false
)

@Serializable
data class PingRequest(val type: String = "ping", val id: String? = null)
@Serializable
data class RefreshRequest(val type: String = "refresh", val id: String? = null)
@Serializable
data class OpenStreamRequest(val type: String = "open_stream", val id: String? = null, @SerialName("pane_id") val paneId: String, val cols: Int, val rows: Int)
@Serializable
data class CloseStreamRequest(val type: String = "close_stream", val id: String? = null, @SerialName("pane_id") val paneId: String)
@Serializable
data class TakeControlRequest(val type: String = "take_control", val id: String? = null, @SerialName("pane_id") val paneId: String, val cols: Int, val rows: Int, val takeover: Boolean? = null)
@Serializable
data class ReleaseControlRequest(val type: String = "release_control", val id: String? = null, @SerialName("pane_id") val paneId: String)
@Serializable
data class InputRequest(val type: String = "input", val id: String? = null, @SerialName("pane_id") val paneId: String, val text: String? = null, val bytes: String? = null)
@Serializable
data class ResizeRequest(val type: String = "resize", val id: String? = null, @SerialName("pane_id") val paneId: String, val cols: Int, val rows: Int)
@Serializable
data class ScrollRequest(val type: String = "scroll", val id: String? = null, @SerialName("pane_id") val paneId: String, val direction: ScrollDirection, val lines: Int)
@Serializable
data class CallRequest(val type: String = "call", val id: String? = null, val method: String, val params: JsonElement)
@Serializable
data class DiffRequest(val type: String = "diff", val id: String? = null, @SerialName("pane_id") val paneId: String, val staged: Boolean? = null, val path: String? = null)

@Serializable
enum class ScrollDirection { @SerialName("up") UP, @SerialName("down") DOWN }

sealed interface ServerMessage {
    data class Hello(val value: BridgeHello) : ServerMessage
    data class Snapshot(val value: BridgeSnapshot) : ServerMessage
    data class AgentStatus(val value: AgentStatusTransition) : ServerMessage
    data class Frame(val value: TerminalFrame) : ServerMessage
    data class Stream(val value: StreamState) : ServerMessage
    data class HerdrStatus(val value: HerdrStatusMessage) : ServerMessage
    data class Result(val value: BridgeResultMessage) : ServerMessage
    data class Pong(val value: PongMessage) : ServerMessage
    data class Unknown(val type: String, val raw: JsonElement) : ServerMessage
}
