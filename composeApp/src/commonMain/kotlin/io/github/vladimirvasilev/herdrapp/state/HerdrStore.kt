package io.github.vladimirvasilev.herdrapp.state

import io.github.vladimirvasilev.herdrapp.data.bridge.*
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.*
import io.github.vladimirvasilev.herdrapp.domain.AgentOrganizer
import io.github.vladimirvasilev.herdrapp.domain.ConnectionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class HerdrStore {
    private val _workspaces = MutableStateFlow<List<WorkspaceInfo>>(emptyList())
    val workspaces: StateFlow<List<WorkspaceInfo>> = _workspaces.asStateFlow()
    private val _panes = MutableStateFlow<List<PaneInfo>>(emptyList())
    val panes: StateFlow<List<PaneInfo>> = _panes.asStateFlow()
    private val _agents = MutableStateFlow<List<AgentInfo>>(emptyList())
    val agents: StateFlow<List<AgentInfo>> = _agents.asStateFlow()
    private val _previews = MutableStateFlow<Map<String, String>>(emptyMap())
    val previews: StateFlow<Map<String, String>> = _previews.asStateFlow()
    private val _frames = MutableSharedFlow<TerminalFrame>(extraBufferCapacity = 2048)
    val frames: SharedFlow<TerminalFrame> = _frames.asSharedFlow()
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    private val _herdrStatus = MutableStateFlow<HerdrInfo?>(null)
    val herdrStatus: StateFlow<HerdrInfo?> = _herdrStatus.asStateFlow()
    private val _streams = MutableStateFlow<Map<String, StreamState>>(emptyMap())
    val streams: StateFlow<Map<String, StreamState>> = _streams.asStateFlow()

    fun setConnectionState(state: ConnectionState) { _connectionState.value = state }
    fun setHerdrStatus(status: HerdrInfo?) { _herdrStatus.value = status }

    fun replace(snapshot: BridgeSnapshot) {
        _workspaces.value = snapshot.workspaces.sortedBy { it.number }
        _panes.value = snapshot.panes
        _agents.value = AgentOrganizer.orderedAgents(snapshot.agents, snapshot.workspaces)
        _previews.value = snapshot.previews
    }

    fun onFrame(frame: TerminalFrame) { _frames.tryEmit(frame) }
    fun onStream(stream: StreamState) {
        _streams.value = if (stream.mode == StreamMode.CLOSED) _streams.value - stream.paneId else _streams.value + (stream.paneId to stream)
    }
}
