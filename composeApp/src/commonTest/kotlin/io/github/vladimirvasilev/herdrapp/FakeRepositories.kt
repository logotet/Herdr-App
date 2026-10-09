package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.domain.CommandResult
import io.github.vladimirvasilev.herdrapp.domain.ConnectionState
import io.github.vladimirvasilev.herdrapp.domain.GridSize
import io.github.vladimirvasilev.herdrapp.domain.HistoryResult
import io.github.vladimirvasilev.herdrapp.domain.HomeView
import io.github.vladimirvasilev.herdrapp.domain.HostRepository
import io.github.vladimirvasilev.herdrapp.domain.SavedHost
import io.github.vladimirvasilev.herdrapp.domain.Session
import io.github.vladimirvasilev.herdrapp.domain.SessionRepository
import io.github.vladimirvasilev.herdrapp.domain.SettingsRepository
import io.github.vladimirvasilev.herdrapp.domain.StreamMode
import io.github.vladimirvasilev.herdrapp.domain.TerminalFrame
import io.github.vladimirvasilev.herdrapp.domain.TerminalRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

class FakeSessionRepository : SessionRepository {
    override val connectionState = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected)
    override val currentHost = MutableStateFlow<SavedHost?>(null)
    override val herdrAvailable = MutableStateFlow(true)
    override val session = MutableStateFlow(Session())
    val connected = mutableListOf<SavedHost>()
    var refreshes = 0

    override fun connect(host: SavedHost) {
        connected += host
        currentHost.value = host
    }

    override suspend fun refresh() {
        refreshes++
    }

    /** Layout changes as one line each, for example "renamePane w1:p1 tests". */
    val changes = mutableListOf<String>()
    var changeResult: CommandResult = CommandResult.Success

    override suspend fun renamePane(paneId: String, label: String) = change("renamePane $paneId $label")

    override suspend fun closePane(paneId: String) = change("closePane $paneId")

    override suspend fun createTab(workspaceId: String, label: String?) = change("createTab $workspaceId $label")

    override suspend fun renameTab(tabId: String, label: String) = change("renameTab $tabId $label")

    override suspend fun closeTab(tabId: String) = change("closeTab $tabId")

    override suspend fun createWorkspace(label: String?) = change("createWorkspace $label")

    override suspend fun renameWorkspace(workspaceId: String, label: String) =
        change("renameWorkspace $workspaceId $label")

    override suspend fun closeWorkspace(workspaceId: String) = change("closeWorkspace $workspaceId")

    private fun change(line: String): CommandResult {
        changes += line
        return changeResult
    }
}

class FakeHostRepository(initial: List<SavedHost> = emptyList()) : HostRepository {
    override val hosts = MutableStateFlow(initial)

    override suspend fun upsert(host: SavedHost) {
        hosts.value = hosts.value.filterNot { it.id == host.id } + host
    }

    override suspend fun delete(id: String) {
        hosts.value = hosts.value.filterNot { it.id == id }
    }
}

class FakeSettingsRepository : SettingsRepository {
    override val terminalFontSize = MutableStateFlow(14f)

    override suspend fun setTerminalFontSize(sizeSp: Float) {
        terminalFontSize.value = sizeSp
    }

    override val homeView = MutableStateFlow(HomeView.WORKSPACES)

    override suspend fun setHomeView(view: HomeView) {
        homeView.value = view
    }

    override val backgroundAlerts = MutableStateFlow(false)

    override suspend fun setBackgroundAlerts(enabled: Boolean) {
        backgroundAlerts.value = enabled
    }
}

/** Records every call as one line, for example "open w1:p1 94x39". */
class FakeTerminalRepository : TerminalRepository {
    override val streamModes = MutableStateFlow<Map<String, StreamMode>>(emptyMap())
    val calls = mutableListOf<String>()
    var submitResult: CommandResult = CommandResult.Success
    var keysResult: CommandResult = CommandResult.Success
    var historyResult: HistoryResult = HistoryResult.Empty
    var scrollResult: CommandResult = CommandResult.Success

    override fun frames(paneId: String): Flow<TerminalFrame> = emptyFlow()

    override suspend fun open(paneId: String, cols: Int, rows: Int) {
        calls += "open $paneId ${cols}x$rows"
    }

    override fun close(paneId: String) {
        calls += "close $paneId"
    }

    override suspend fun resize(paneId: String, cols: Int, rows: Int) {
        calls += "resize $paneId ${cols}x$rows"
    }

    override suspend fun takeControl(paneId: String, cols: Int, rows: Int) {
        calls += "takeControl $paneId ${cols}x$rows"
    }

    override suspend fun releaseControl(paneId: String) {
        calls += "releaseControl $paneId"
    }

    override suspend fun sendInput(paneId: String, bytes: ByteArray) {
        calls += "input $paneId ${bytes.decodeToString()}"
    }

    override suspend fun sendKeys(paneId: String, keys: List<String>): CommandResult {
        calls += "keys $paneId ${keys.joinToString(",")}"
        return keysResult
    }

    override suspend fun sendText(paneId: String, text: String) {
        calls += "text $paneId $text"
    }

    override suspend fun submitPrompt(paneId: String, text: String): CommandResult {
        calls += "submit $paneId $text"
        return submitResult
    }

    override suspend fun readHistory(paneId: String): HistoryResult {
        calls += "history $paneId"
        return historyResult
    }

    override suspend fun scrollToLatest(paneId: String, pcGrid: GridSize, lines: Int): CommandResult {
        calls += "scrollToLatest $paneId ${pcGrid.cols}x${pcGrid.rows} $lines"
        return scrollResult
    }
}
