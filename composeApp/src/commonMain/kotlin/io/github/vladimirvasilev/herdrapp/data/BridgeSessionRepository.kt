package io.github.vladimirvasilev.herdrapp.data

import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeConnection
import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeListener
import io.github.vladimirvasilev.herdrapp.data.bridge.RequestOutcome
import io.github.vladimirvasilev.herdrapp.data.bridge.SocketStatus
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.ServerMessage
import io.github.vladimirvasilev.herdrapp.data.bridge.toCommandResult
import io.github.vladimirvasilev.herdrapp.data.bridge.toSession
import io.github.vladimirvasilev.herdrapp.domain.CommandResult
import io.github.vladimirvasilev.herdrapp.domain.ConnectionState
import io.github.vladimirvasilev.herdrapp.domain.SavedHost
import io.github.vladimirvasilev.herdrapp.domain.Session
import io.github.vladimirvasilev.herdrapp.domain.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class BridgeSessionRepository(private val connection: BridgeConnection) : SessionRepository, BridgeListener {
    private val _connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    override val currentHost: StateFlow<SavedHost?> get() = connection.currentHost
    private val _herdrAvailable = MutableStateFlow(true)
    override val herdrAvailable: StateFlow<Boolean> = _herdrAvailable.asStateFlow()
    private val _session = MutableStateFlow(Session())
    override val session: StateFlow<Session> = _session.asStateFlow()

    override fun connect(host: SavedHost) = connection.connect(host)

    override suspend fun refresh() {
        connection.refresh()
    }

    override suspend fun renamePane(paneId: String, label: String) = changed(connection.renamePane(paneId, label))

    override suspend fun closePane(paneId: String) = changed(connection.closePane(paneId))

    override suspend fun createTab(workspaceId: String, label: String?) =
        changed(connection.createTab(workspaceId, label))

    override suspend fun renameTab(tabId: String, label: String) = changed(connection.renameTab(tabId, label))

    override suspend fun closeTab(tabId: String) = changed(connection.closeTab(tabId))

    /** After a change to the layout, asks for a snapshot instead of waiting for the bridge to notice. */
    private suspend fun changed(outcome: RequestOutcome): CommandResult {
        if (outcome is RequestOutcome.Success) connection.refresh()
        return outcome.toCommandResult()
    }

    override suspend fun onStatus(status: SocketStatus) {
        when (status) {
            SocketStatus.Connecting -> _connectionState.value = ConnectionState.Connecting
            // Connected is reported once the bridge says hello.
            SocketStatus.Open -> Unit
            is SocketStatus.Lost -> _connectionState.value = ConnectionState.Error(status.message)
        }
    }

    override suspend fun onMessage(message: ServerMessage) {
        when (message) {
            is ServerMessage.Hello -> {
                _connectionState.value = ConnectionState.Connected(message.value.name)
                _herdrAvailable.value = message.value.herdr.available
            }
            is ServerMessage.Snapshot -> _session.value = message.value.toSession()
            is ServerMessage.HerdrStatus -> _herdrAvailable.value = message.value.available
            else -> Unit
        }
    }
}
