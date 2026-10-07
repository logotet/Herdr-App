package io.github.vladimirvasilev.herdrapp.ui.terminal

import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import io.github.vladimirvasilev.herdrapp.domain.Agent
import io.github.vladimirvasilev.herdrapp.domain.CommandResult
import io.github.vladimirvasilev.herdrapp.domain.HistoryResult
import io.github.vladimirvasilev.herdrapp.domain.Session
import io.github.vladimirvasilev.herdrapp.domain.SessionRepository
import io.github.vladimirvasilev.herdrapp.domain.SettingsRepository
import io.github.vladimirvasilev.herdrapp.domain.StreamMode
import io.github.vladimirvasilev.herdrapp.domain.TerminalFrame
import io.github.vladimirvasilev.herdrapp.domain.TerminalRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeMark
import kotlin.time.TimeSource

/** A short message shown over a pane for a moment. */
sealed interface PaneNotice {
    data object NoScrollback : PaneNotice
    data class SendFailed(val reason: String) : PaneNotice
    data class HistoryFailed(val reason: String) : PaneNotice
    data class JumpFailed(val reason: String) : PaneNotice
}

data class PaneUiState(
    val controlling: Boolean = false,
    val draft: TextFieldValue = TextFieldValue(""),
    /** Non-null while the local scrollback is shown instead of the live stream. */
    val history: String? = null,
    val historyLoading: Boolean = false,
    val notice: PaneNotice? = null,
    val sending: Boolean = false,
    /** True while the pane on the PC is being scrolled to its newest output. */
    val jumping: Boolean = false,
)

data class TerminalUiState(
    val agents: List<Agent> = emptyList(),
    val herdrAvailable: Boolean = true,
    val fontSizeSp: Float = 14f,
    private val panes: Map<String, PaneUiState> = emptyMap(),
) {
    fun pane(paneId: String): PaneUiState = panes[paneId] ?: PaneUiState()
}

private const val NOTICE_MILLIS = 2_500L
private const val RESIZE_SETTLE_MILLIS = 300L
private val NO_SCROLLBACK_COOLDOWN = 5.seconds
private val DEFAULT_GRID = 80 to 24

/** State and actions for the pager of agent panes. Every pane is addressed by its pane id. */
internal class TerminalViewModel(
    private val session: SessionRepository,
    private val terminal: TerminalRepository,
    private val settings: SettingsRepository,
    private val timeSource: TimeSource = TimeSource.Monotonic,
) : ViewModel() {
    // Everything below is only touched on the main thread, where viewModelScope runs.
    private val panes = MutableStateFlow<Map<String, PaneUiState>>(emptyMap())
    private val grids = mutableMapOf<String, Pair<Int, Int>>()
    /** The grid each pane's stream was last opened or resized to. */
    private val sentGrids = mutableMapOf<String, Pair<Int, Int>>()
    private val resizeJobs = mutableMapOf<String, Job>()
    private val openStreams = mutableSetOf<String>()
    /** Panes that were under control when they left the screen; control is taken back on return. */
    private val resumeControl = mutableSetOf<String>()
    private val noScrollbackAt = mutableMapOf<String, TimeMark>()
    private val noticeJobs = mutableMapOf<String, Job>()

    val uiState: StateFlow<TerminalUiState> = combine(
        session.session,
        session.herdrAvailable,
        settings.terminalFontSize,
        terminal.streamModes,
        panes,
        ::toUiState,
    ).stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = toUiState(
            session.session.value,
            session.herdrAvailable.value,
            settings.terminalFontSize.value,
            terminal.streamModes.value,
            panes.value,
        ),
    )

    fun frames(paneId: String): Flow<TerminalFrame> = terminal.frames(paneId)

    /**
     * The pane's view reports its grid. The stream opens on the first report; later ones only
     * resize it, because re-opening would restart observe mode and drop control.
     *
     * A resize is sent once the grid has stopped changing. The keyboard sliding in or out reports
     * a new grid for every step of its animation, and in control mode each one would resize the
     * real pane on the PC, where every resize makes the program in it redraw.
     */
    fun onGridMeasured(paneId: String, cols: Int, rows: Int) {
        val grid = cols to rows
        grids[paneId] = grid
        if (openStreams.add(paneId)) {
            sentGrids[paneId] = grid
            val resume = resumeControl.remove(paneId)
            viewModelScope.launch {
                terminal.open(paneId, cols, rows)
                if (resume) terminal.takeControl(paneId, cols, rows)
            }
            return
        }
        resizeJobs.remove(paneId)?.cancel()
        resizeJobs[paneId] = viewModelScope.launch {
            delay(RESIZE_SETTLE_MILLIS)
            // Back at the size the PC already has, for example the keyboard opened and closed again.
            if (sentGrids[paneId] == grid) return@launch
            sentGrids[paneId] = grid
            terminal.resize(paneId, cols, rows)
        }
    }

    /**
     * The pane left the screen: stop its stream and forget everything except the draft. Closing
     * the stream releases control, so a pane that was controlled is remembered for its return.
     */
    fun onPaneGone(paneId: String) {
        if (controlling(paneId)) resumeControl += paneId else resumeControl -= paneId
        terminal.close(paneId)
        openStreams -= paneId
        grids -= paneId
        sentGrids -= paneId
        resizeJobs.remove(paneId)?.cancel()
        noScrollbackAt -= paneId
        noticeJobs.remove(paneId)?.cancel()
        update(paneId) { PaneUiState(draft = it.draft) }
    }

    fun onDraftChange(paneId: String, draft: TextFieldValue) = update(paneId) { it.copy(draft = draft) }

    fun submit(paneId: String) {
        val pane = panes.value[paneId] ?: PaneUiState()
        if (pane.sending) return
        update(paneId) { it.copy(sending = true) }
        viewModelScope.launch {
            when (val result = terminal.submitPrompt(paneId, normalizePrompt(pane.draft.text))) {
                CommandResult.Success -> update(paneId) { it.copy(draft = TextFieldValue(""), history = null) }
                is CommandResult.Failure -> notify(paneId, PaneNotice.SendFailed(result.message))
            }
            update(paneId) { it.copy(sending = false) }
        }
    }

    fun loadHistory(paneId: String) {
        val pane = panes.value[paneId] ?: PaneUiState()
        if (pane.history != null || pane.historyLoading) return
        if (noScrollbackAt[paneId]?.let { it.elapsedNow() < NO_SCROLLBACK_COOLDOWN } == true) return
        update(paneId) { it.copy(historyLoading = true) }
        viewModelScope.launch {
            val rows = (grids[paneId] ?: DEFAULT_GRID).second
            when (val result = terminal.readHistory(paneId)) {
                is HistoryResult.Loaded ->
                    if (result.history.lineCount <= rows) {
                        noScrollback(paneId)
                    } else {
                        update(paneId) { it.copy(history = result.history.text) }
                    }
                HistoryResult.Empty -> noScrollback(paneId)
                is HistoryResult.Failed -> notify(paneId, PaneNotice.HistoryFailed(result.message))
            }
            update(paneId) { it.copy(historyLoading = false) }
        }
    }

    fun exitHistory(paneId: String) = update(paneId) { it.copy(history = null) }

    /** Scrolls a pane that is scrolled back on the PC to its newest output, so it is live again. */
    fun jumpToLatest(paneId: String) {
        val agent = session.session.value.agents.firstOrNull { it.paneId == paneId } ?: return
        val pcGrid = agent.pcGrid ?: return
        if (agent.scrolledBackLines <= 0 || (panes.value[paneId] ?: PaneUiState()).jumping) return
        update(paneId) { it.copy(jumping = true) }
        viewModelScope.launch {
            when (val result = terminal.scrollToLatest(paneId, pcGrid, agent.scrolledBackLines)) {
                // Ask for a fresh snapshot, so the scrolled-back state clears without waiting for a poll.
                CommandResult.Success -> session.refresh()
                is CommandResult.Failure -> notify(paneId, PaneNotice.JumpFailed(result.message))
            }
            update(paneId) { it.copy(jumping = false) }
        }
    }

    /** Raw keyboard bytes from the terminal view; dropped unless this client has control. */
    fun onInput(paneId: String, bytes: ByteArray) {
        if (!controlling(paneId)) return
        viewModelScope.launch { terminal.sendInput(paneId, bytes) }
    }

    fun onKey(paneId: String, key: KeySpec) {
        viewModelScope.launch {
            when (key) {
                is KeySpec.Bytes ->
                    if (controlling(paneId)) {
                        terminal.sendInput(paneId, key.bytes)
                    } else {
                        terminal.sendText(paneId, key.textFallback)
                    }
                is KeySpec.HerdrKey -> terminal.sendKeys(paneId, listOf(key.key))
            }
        }
    }

    fun takeControl(paneId: String) {
        val grid = grids[paneId] ?: DEFAULT_GRID
        // Control starts at the current grid, so a resize still waiting to be sent is not needed.
        resizeJobs.remove(paneId)?.cancel()
        sentGrids[paneId] = grid
        viewModelScope.launch { terminal.takeControl(paneId, grid.first, grid.second) }
    }

    fun releaseControl(paneId: String) {
        resumeControl -= paneId
        viewModelScope.launch { terminal.releaseControl(paneId) }
    }

    fun setFontSize(sizeSp: Float) {
        viewModelScope.launch { settings.setTerminalFontSize(sizeSp) }
    }

    override fun onCleared() {
        openStreams.forEach(terminal::close)
    }

    private fun controlling(paneId: String) = terminal.streamModes.value[paneId] == StreamMode.CONTROL

    private fun noScrollback(paneId: String) {
        noScrollbackAt[paneId] = timeSource.markNow()
        notify(paneId, PaneNotice.NoScrollback)
    }

    /** Shows [notice] over the pane for a moment. */
    private fun notify(paneId: String, notice: PaneNotice) {
        update(paneId) { it.copy(notice = notice) }
        noticeJobs.remove(paneId)?.cancel()
        noticeJobs[paneId] = viewModelScope.launch {
            delay(NOTICE_MILLIS)
            update(paneId) { it.copy(notice = null) }
        }
    }

    private fun update(paneId: String, change: (PaneUiState) -> PaneUiState) {
        panes.update { it + (paneId to change(it[paneId] ?: PaneUiState())) }
    }

    private fun toUiState(
        current: Session,
        herdrAvailable: Boolean,
        fontSizeSp: Float,
        modes: Map<String, StreamMode>,
        local: Map<String, PaneUiState>,
    ) = TerminalUiState(
        agents = current.agents,
        herdrAvailable = herdrAvailable,
        fontSizeSp = fontSizeSp,
        panes = (local.keys + modes.keys).associateWith { paneId ->
            (local[paneId] ?: PaneUiState()).copy(controlling = modes[paneId] == StreamMode.CONTROL)
        },
    )
}
