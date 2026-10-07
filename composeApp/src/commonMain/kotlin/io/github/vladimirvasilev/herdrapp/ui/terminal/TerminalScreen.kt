package io.github.vladimirvasilev.herdrapp.ui.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import io.github.vladimirvasilev.herdrapp.domain.Agent
import io.github.vladimirvasilev.herdrapp.domain.CommandResult
import io.github.vladimirvasilev.herdrapp.domain.HistoryResult
import io.github.vladimirvasilev.herdrapp.domain.SettingsRepository
import io.github.vladimirvasilev.herdrapp.domain.StreamMode
import io.github.vladimirvasilev.herdrapp.domain.TerminalRepository
import io.github.vladimirvasilev.herdrapp.ui.components.HerdrUnavailableBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

private const val NO_SCROLLBACK = "No scrollback here (full-screen app). Try PgUp/PgDn."

@Composable
internal fun TerminalScreen(
    agent: Agent,
    herdrAvailable: Boolean,
    terminal: TerminalRepository,
    settings: SettingsRepository,
    onBack: () -> Unit,
    draft: TextFieldValue,
    onDraftChange: (TextFieldValue) -> Unit,
    onHistoryOpenChange: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val streamModes by terminal.streamModes.collectAsState()
    val fontSize by settings.terminalFontSize.collectAsState()
    val controlling = streamModes[agent.paneId] == StreamMode.CONTROL
    val frames = remember(agent.paneId) { terminal.frames(agent.paneId) }
    var colsRows by remember(agent.paneId) { mutableStateOf(80 to 24) }
    // The stream opens once the view has measured its grid; later size changes only resize it,
    // because re-opening would restart observe mode and drop control.
    var streamOpened by remember(agent.paneId) { mutableStateOf(false) }
    var confirmTakeover by remember { mutableStateOf(false) }
    // Non-null while the local scrollback view is shown instead of the live stream.
    var history by remember(agent.paneId) { mutableStateOf<String?>(null) }
    var historyLoading by remember(agent.paneId) { mutableStateOf(false) }
    var noScrollbackAt by remember(agent.paneId) { mutableStateOf<TimeSource.Monotonic.ValueTimeMark?>(null) }
    var notice by remember(agent.paneId) { mutableStateOf<String?>(null) }
    var sending by remember(agent.paneId) { mutableStateOf(false) }

    LaunchedEffect(notice) {
        if (notice != null) {
            delay(2_500)
            notice = null
        }
    }
    LaunchedEffect(history != null) { onHistoryOpenChange(history != null) }

    fun noScrollback() {
        noScrollbackAt = TimeSource.Monotonic.markNow()
        notice = NO_SCROLLBACK
    }

    fun loadHistory() {
        if (history != null || historyLoading) return
        if (noScrollbackAt?.let { it.elapsedNow() < 5.seconds } == true) return
        historyLoading = true
        scope.launch {
            when (val result = terminal.readHistory(agent.paneId)) {
                is HistoryResult.Loaded ->
                    if (result.history.lineCount <= colsRows.second) noScrollback() else history = result.history.text
                HistoryResult.Empty -> noScrollback()
                is HistoryResult.Failed -> notice = "History: ${result.message}"
            }
            historyLoading = false
        }
    }

    fun submit() {
        if (sending) return
        val text = normalizePrompt(draft.text)
        sending = true
        scope.launch {
            when (val result = terminal.submitPrompt(agent.paneId, text)) {
                CommandResult.Success -> {
                    onDraftChange(TextFieldValue(""))
                    history = null
                }
                is CommandResult.Failure -> notice = "Send failed: ${result.message}"
            }
            sending = false
        }
    }

    DisposableEffect(agent.paneId) {
        onDispose {
            onHistoryOpenChange(false)
            terminal.close(agent.paneId)
        }
    }

    Column(Modifier.fillMaxSize()) {
        TerminalTopBar(
            agent = agent,
            controlling = controlling,
            onBack = onBack,
            onRelease = { scope.launch { terminal.releaseControl(agent.paneId) } },
            onTakeControl = { confirmTakeover = true },
        )
        HerdrUnavailableBanner(herdrAvailable)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            TerminalPane(
                paneId = agent.paneId,
                frames = frames,
                controlling = controlling,
                fontSizeSp = fontSize,
                history = history,
                onInput = { bytes -> if (controlling) scope.launch { terminal.sendInput(agent.paneId, bytes) } },
                onResize = { c, r ->
                    colsRows = c to r
                    if (!streamOpened) {
                        streamOpened = true
                        scope.launch { terminal.open(agent.paneId, c, r) }
                    } else {
                        scope.launch { terminal.resize(agent.paneId, c, r) }
                    }
                },
                onScrollBack = { loadHistory() },
                onExitHistory = { history = null },
                onFontSizeChanged = { scope.launch { settings.setTerminalFontSize(it) } },
                modifier = Modifier.fillMaxSize(),
            )
            if (history != null) HistoryOverlay(onLive = { history = null })
            if (historyLoading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            notice?.let { Notice(it) }
        }
        PromptBar(value = draft, onValueChange = onDraftChange, sending = sending, onSend = { submit() })
        ExtraKeysBar(
            onKey = { spec ->
                scope.launch {
                    when (spec) {
                        is KeySpec.Bytes ->
                            if (controlling) {
                                terminal.sendInput(agent.paneId, spec.bytes)
                            } else {
                                terminal.sendText(agent.paneId, spec.textFallback)
                            }
                        is KeySpec.HerdrKey -> terminal.sendKeys(agent.paneId, listOf(spec.key))
                    }
                }
            },
        )
    }
    if (confirmTakeover) {
        TakeControlDialog(
            onConfirm = {
                confirmTakeover = false
                scope.launch { terminal.takeControl(agent.paneId, colsRows.first, colsRows.second) }
            },
            onDismiss = { confirmTakeover = false },
        )
    }
}
