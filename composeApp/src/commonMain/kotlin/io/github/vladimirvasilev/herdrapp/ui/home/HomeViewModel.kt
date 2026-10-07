package io.github.vladimirvasilev.herdrapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vladimirvasilev.herdrapp.domain.AgentOrganizer
import io.github.vladimirvasilev.herdrapp.domain.CommandResult
import io.github.vladimirvasilev.herdrapp.domain.ConnectionState
import io.github.vladimirvasilev.herdrapp.domain.HostRepository
import io.github.vladimirvasilev.herdrapp.domain.Pane
import io.github.vladimirvasilev.herdrapp.domain.SavedHost
import io.github.vladimirvasilev.herdrapp.domain.Session
import io.github.vladimirvasilev.herdrapp.domain.SessionRepository
import io.github.vladimirvasilev.herdrapp.domain.WorkspaceGroup
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** A question to the user before the layout on the PC is changed. */
sealed interface HomeDialog {
    data class RenamePane(val paneId: String, val current: String) : HomeDialog
    data class ClosePane(val paneId: String, val title: String) : HomeDialog
    data class NewTab(val workspaceId: String) : HomeDialog
    data class RenameTab(val tabId: String, val current: String) : HomeDialog
    data class CloseTab(val tabId: String, val label: String) : HomeDialog
}

data class HomeUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val hostName: String? = null,
    val herdrAvailable: Boolean = true,
    val hasHosts: Boolean = false,
    /** Agents waiting for the user, repeated above the workspace groups. */
    val needsYou: List<Pane> = emptyList(),
    val groups: List<WorkspaceGroup> = emptyList(),
    val dialog: HomeDialog? = null,
    /** Why the last change to the layout did not happen; shown for a moment. */
    val changeFailed: String? = null,
)

private const val FAILURE_MILLIS = 4_000L

class HomeViewModel(
    private val session: SessionRepository,
    private val hosts: HostRepository,
) : ViewModel() {
    private data class Local(val dialog: HomeDialog? = null, val changeFailed: String? = null)

    private val local = MutableStateFlow(Local())
    private var failureJob: Job? = null

    val uiState: StateFlow<HomeUiState> = combine(
        session.connectionState,
        session.currentHost,
        session.herdrAvailable,
        session.session,
        hosts.hosts,
        ::toUiState,
    ).combine(local) { state, own ->
        state.copy(dialog = own.dialog, changeFailed = own.changeFailed)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = toUiState(
            session.connectionState.value,
            session.currentHost.value,
            session.herdrAvailable.value,
            session.session.value,
            hosts.hosts.value,
        ),
    )

    init {
        // Connect to the first saved host as soon as there is one and nothing is connected yet.
        viewModelScope.launch {
            hosts.hosts.collect { saved ->
                if (session.currentHost.value == null && saved.isNotEmpty()) session.connect(saved.first())
            }
        }
    }

    fun refresh() {
        viewModelScope.launch { session.refresh() }
    }

    fun showDialog(dialog: HomeDialog) = local.update { it.copy(dialog = dialog) }

    fun dismissDialog() = local.update { it.copy(dialog = null) }

    /**
     * The user confirmed the open dialog; [name] is what they typed, for the dialogs that ask for
     * one. A rename needs a name. A new tab may go without and is then named by herdr.
     */
    fun confirmDialog(name: String = "") {
        val dialog = local.value.dialog ?: return
        val trimmed = name.trim()
        if ((dialog is HomeDialog.RenamePane || dialog is HomeDialog.RenameTab) && trimmed.isEmpty()) return
        dismissDialog()
        viewModelScope.launch {
            val result = when (dialog) {
                is HomeDialog.RenamePane -> session.renamePane(dialog.paneId, trimmed)
                is HomeDialog.ClosePane -> session.closePane(dialog.paneId)
                is HomeDialog.NewTab -> session.createTab(dialog.workspaceId, trimmed.ifEmpty { null })
                is HomeDialog.RenameTab -> session.renameTab(dialog.tabId, trimmed)
                is HomeDialog.CloseTab -> session.closeTab(dialog.tabId)
            }
            if (result is CommandResult.Failure) reportFailure(result.message)
        }
    }

    private fun reportFailure(message: String) {
        local.update { it.copy(changeFailed = message) }
        failureJob?.cancel()
        failureJob = viewModelScope.launch {
            delay(FAILURE_MILLIS)
            local.update { it.copy(changeFailed = null) }
        }
    }

    private fun toUiState(
        connection: ConnectionState,
        host: SavedHost?,
        herdrAvailable: Boolean,
        current: Session,
        saved: List<SavedHost>,
    ) = HomeUiState(
        connection = connection,
        hostName = host?.name,
        herdrAvailable = herdrAvailable,
        hasHosts = saved.isNotEmpty(),
        needsYou = AgentOrganizer.needsYou(current.agents),
        groups = AgentOrganizer.groups(current),
    )
}
