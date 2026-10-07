package io.github.vladimirvasilev.herdrapp.ui.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import io.github.vladimirvasilev.herdrapp.domain.Agent
import io.github.vladimirvasilev.herdrapp.domain.TerminalFrame
import io.github.vladimirvasilev.herdrapp.ui.components.HerdrUnavailableBanner
import kotlinx.coroutines.flow.Flow

/** What one pane's screen can ask for. */
internal class PaneActions(
    val onBack: () -> Unit,
    val onGridMeasured: (cols: Int, rows: Int) -> Unit,
    val onInput: (ByteArray) -> Unit,
    val onKey: (KeySpec) -> Unit,
    val onDraftChange: (TextFieldValue) -> Unit,
    val onSubmit: () -> Unit,
    val onLoadHistory: () -> Unit,
    val onExitHistory: () -> Unit,
    val onTakeControl: () -> Unit,
    val onReleaseControl: () -> Unit,
    val onFontSizeChanged: (Float) -> Unit,
)

@Composable
internal fun TerminalScreen(
    agent: Agent,
    pane: PaneUiState,
    herdrAvailable: Boolean,
    fontSizeSp: Float,
    frames: Flow<TerminalFrame>,
    actions: PaneActions,
) {
    var confirmTakeover by rememberSaveable(agent.paneId) { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        TerminalTopBar(
            agent = agent,
            controlling = pane.controlling,
            onBack = actions.onBack,
            onRelease = actions.onReleaseControl,
            onTakeControl = { confirmTakeover = true },
        )
        HerdrUnavailableBanner(herdrAvailable)
        Box(Modifier.fillMaxWidth().weight(1f)) {
            TerminalPane(
                paneId = agent.paneId,
                frames = frames,
                controlling = pane.controlling,
                fontSizeSp = fontSizeSp,
                history = pane.history,
                onInput = actions.onInput,
                onResize = actions.onGridMeasured,
                onScrollBack = actions.onLoadHistory,
                onExitHistory = actions.onExitHistory,
                onFontSizeChanged = actions.onFontSizeChanged,
                modifier = Modifier.fillMaxSize(),
            )
            if (pane.history != null) HistoryOverlay(onLive = actions.onExitHistory)
            if (pane.historyLoading) LinearProgressIndicator(Modifier.fillMaxWidth().align(Alignment.TopCenter))
            pane.notice?.let { Notice(it) }
        }
        PromptBar(
            value = pane.draft,
            onValueChange = actions.onDraftChange,
            sending = pane.sending,
            onSend = actions.onSubmit,
        )
        ExtraKeysBar(onKey = actions.onKey)
    }
    if (confirmTakeover) {
        TakeControlDialog(
            onConfirm = {
                confirmTakeover = false
                actions.onTakeControl()
            },
            onDismiss = { confirmTakeover = false },
        )
    }
}
