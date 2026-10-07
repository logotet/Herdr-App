package io.github.vladimirvasilev.herdrapp.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vladimirvasilev.herdrapp.domain.Agent
import io.github.vladimirvasilev.herdrapp.domain.AgentOrganizer
import io.github.vladimirvasilev.herdrapp.domain.ConnectionState
import io.github.vladimirvasilev.herdrapp.domain.HostRepository
import io.github.vladimirvasilev.herdrapp.domain.SavedHost
import io.github.vladimirvasilev.herdrapp.domain.Session
import io.github.vladimirvasilev.herdrapp.domain.SessionRepository
import io.github.vladimirvasilev.herdrapp.domain.WorkspaceGroup
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val connection: ConnectionState = ConnectionState.Disconnected,
    val hostName: String? = null,
    val herdrAvailable: Boolean = true,
    val hasHosts: Boolean = false,
    /** Agents waiting for the user, repeated above the workspace groups. */
    val needsYou: List<Agent> = emptyList(),
    val groups: List<WorkspaceGroup> = emptyList(),
)

class HomeViewModel(
    private val session: SessionRepository,
    private val hosts: HostRepository,
) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        session.connectionState,
        session.currentHost,
        session.herdrAvailable,
        session.session,
        hosts.hosts,
        ::toUiState,
    ).stateIn(
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
