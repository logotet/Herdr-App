package io.github.vladimirvasilev.herdrapp.ui.hosts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vladimirvasilev.herdrapp.domain.HostRepository
import io.github.vladimirvasilev.herdrapp.domain.PairUriParser
import io.github.vladimirvasilev.herdrapp.domain.SavedHost
import io.github.vladimirvasilev.herdrapp.domain.SessionRepository
import io.github.vladimirvasilev.herdrapp.domain.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HostForm(
    val name: String = "",
    val host: String = "",
    val port: String = DEFAULT_PORT.toString(),
    val token: String = "",
    val error: HostFormError? = null,
)

enum class HostFormError { MISSING_FIELDS, INVALID_QR }

data class HostsUiState(
    val hosts: List<SavedHost> = emptyList(),
    val form: HostForm = HostForm(),
    val backgroundAlerts: Boolean = false,
    /** The user asked for background alerts but the system does not let the app notify. */
    val alertsDenied: Boolean = false,
)

private const val DEFAULT_PORT = 8787

class HostsViewModel(
    private val hosts: HostRepository,
    private val session: SessionRepository,
    private val settings: SettingsRepository,
) : ViewModel() {
    private val form = MutableStateFlow(HostForm())
    private val alertsDenied = MutableStateFlow(false)

    val uiState: StateFlow<HostsUiState> = combine(
        hosts.hosts,
        form,
        settings.backgroundAlerts,
        alertsDenied,
        ::HostsUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HostsUiState(hosts.hosts.value, form.value, settings.backgroundAlerts.value),
    )

    /**
     * Switches background alerts on or off. Switching on only takes effect when the system lets
     * the app notify ([mayNotify]); otherwise the screen says why nothing happened.
     */
    fun setBackgroundAlerts(enabled: Boolean, mayNotify: Boolean = true) {
        alertsDenied.value = enabled && !mayNotify
        viewModelScope.launch { settings.setBackgroundAlerts(enabled && mayNotify) }
    }

    fun onNameChange(value: String) = form.update { it.copy(name = value) }
    fun onHostChange(value: String) = form.update { it.copy(host = value) }
    fun onPortChange(value: String) = form.update { it.copy(port = value) }
    fun onTokenChange(value: String) = form.update { it.copy(token = value) }

    fun select(host: SavedHost) = session.connect(host)

    fun delete(host: SavedHost) {
        viewModelScope.launch { hosts.delete(host.id) }
    }

    /** Saves the typed host and connects to it. False when the form is incomplete. */
    fun saveForm(): Boolean {
        val current = form.value
        if (current.host.isBlank() || current.token.isBlank()) {
            form.update { it.copy(error = HostFormError.MISSING_FIELDS) }
            return false
        }
        val port = current.port.toIntOrNull() ?: DEFAULT_PORT
        save(
            SavedHost(
                id = SavedHost.idFor(current.host, port),
                name = current.name.ifBlank { current.host },
                host = current.host,
                port = port,
                token = current.token,
            )
        )
        return true
    }

    /** Saves the host from a scanned pairing code. False when the scan was cancelled or invalid. */
    fun onScanned(raw: String?): Boolean {
        if (raw == null) return false
        val host = PairUriParser.parse(raw)
        if (host == null) {
            form.update { it.copy(error = HostFormError.INVALID_QR) }
            return false
        }
        save(host)
        return true
    }

    private fun save(host: SavedHost) {
        form.value = HostForm()
        viewModelScope.launch {
            hosts.upsert(host)
            session.connect(host)
        }
    }
}
