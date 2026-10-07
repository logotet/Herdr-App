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
import io.github.vladimirvasilev.herdrapp.domain.SettingsRepository
import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeConnection
import io.github.vladimirvasilev.herdrapp.data.bridge.RequestOutcome
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.AgentInfo
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.StreamMode
import io.github.vladimirvasilev.herdrapp.data.bridge.parsePaneRead
import io.github.vladimirvasilev.herdrapp.state.HerdrStore
import io.github.vladimirvasilev.herdrapp.ui.components.HerdrUnavailableBanner
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

@Composable
internal fun TerminalScreen(
    agent: AgentInfo,
    store: HerdrStore,
    connection: BridgeConnection,
    settings: SettingsRepository,
    onBack: () -> Unit,
    draft: TextFieldValue,
    onDraftChange: (TextFieldValue) -> Unit,
    onHistoryOpenChange: (Boolean) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val streams by store.streams.collectAsState()
    val herdrStatus by store.herdrStatus.collectAsState()
    val fontSize by settings.terminalFontSize.collectAsState()
    val stream = streams[agent.paneId]
    val controlling = stream?.mode == StreamMode.CONTROL
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

    fun loadHistory() {
        if (history != null || historyLoading) return
        if (noScrollbackAt?.let { it.elapsedNow() < 5.seconds } == true) return
        historyLoading = true
        scope.launch {
            when (val outcome = connection.readHistory(agent.paneId)) {
                is RequestOutcome.Success -> {
                    val h = parsePaneRead(outcome.data)
                    if (h == null || h.lineCount <= colsRows.second) {
                        noScrollbackAt = TimeSource.Monotonic.markNow()
                        notice = "No scrollback here (full-screen app). Try PgUp/PgDn."
                    } else {
                        history = h.text
                    }
                }
                is RequestOutcome.Failure -> notice = "History: ${outcome.error.message}"
            }
            historyLoading = false
        }
    }

    fun submit() {
        if (sending) return
        val text = normalizePrompt(draft.text)
        sending = true
        scope.launch {
            when (val outcome = connection.submitPrompt(agent.paneId, text)) {
                is RequestOutcome.Success -> {
                    onDraftChange(TextFieldValue(""))
                    history = null
                }
                is RequestOutcome.Failure -> notice = "Send failed: ${outcome.error.message}"
            }
            sending = false
        }
    }

    DisposableEffect(agent.paneId) {
        onDispose {
            onHistoryOpenChange(false)
            scope.launch { connection.closeStream(agent.paneId) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        TerminalTopBar(
            agent = agent,
            controlling = controlling,
            onBack = onBack,
            onRelease = { scope.launch { connection.releaseControl(agent.paneId) } },
            onTakeControl = { confirmTakeover = true },
        )
        HerdrUnavailableBanner(herdrStatus)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            TerminalPane(
                paneId = agent.paneId,
                frames = store.frames,
                controlling = controlling,
                fontSizeSp = fontSize,
                history = history,
                onInput = { bytes -> if (controlling) scope.launch { connection.inputBytes(agent.paneId, bytes) } },
                onResize = { c, r ->
                    colsRows = c to r
                    if (!streamOpened) {
                        streamOpened = true
                        scope.launch { connection.openStream(agent.paneId, c, r) }
                    } else {
                        scope.launch { connection.resize(agent.paneId, c, r) }
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
                                connection.inputBytes(agent.paneId, spec.bytes)
                            } else {
                                connection.sendText(agent.paneId, spec.textFallback)
                            }
                        is KeySpec.HerdrKey -> connection.sendKeys(agent.paneId, listOf(spec.key))
                    }
                }
            },
        )
    }
    if (confirmTakeover) {
        TakeControlDialog(
            onConfirm = {
                confirmTakeover = false
                scope.launch {
                    connection.takeControl(agent.paneId, colsRows.first, colsRows.second, takeover = true)
                }
            },
            onDismiss = { confirmTakeover = false },
        )
    }
}
