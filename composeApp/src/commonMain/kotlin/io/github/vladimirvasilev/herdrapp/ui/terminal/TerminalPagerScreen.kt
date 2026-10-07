package io.github.vladimirvasilev.herdrapp.ui.terminal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import herdrapp.composeapp.generated.resources.*
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.vladimirvasilev.herdrapp.AppContainer

@Composable
internal fun TerminalRoute(container: AppContainer, initialPaneId: String, onBack: () -> Unit) {
    val viewModel = viewModel { TerminalViewModel(container.session, container.terminal, container.settings) }
    TerminalPagerScreen(viewModel, initialPaneId, onBack)
}

@Composable
private fun TerminalPagerScreen(viewModel: TerminalViewModel, initialPaneId: String, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val agents = state.agents
    if (agents.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextButton(onClick = onBack) { Text(stringResource(Res.string.terminal_no_agents)) }
        }
        return
    }
    // Created only once there are agents, so the first page is the pane that was asked for even
    // when the list arrives after the screen does (for example after the process was restored).
    val pagerState = rememberPagerState(
        initialPage = agents.indexOfFirst { it.paneId == initialPaneId }.coerceAtLeast(0),
        pageCount = { agents.size },
    )
    val currentPaneId = agents.getOrNull(pagerState.currentPage)?.paneId
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        // History is a reading mode: paging is off while the current pane shows it (leave via ↓ Live).
        userScrollEnabled = currentPaneId == null || state.pane(currentPaneId).history == null,
    ) { page ->
        val pane = agents[page.coerceAtMost(agents.lastIndex)]
        val paneId = pane.paneId
        val frames = remember(paneId) { viewModel.frames(paneId) }
        DisposableEffect(paneId) { onDispose { viewModel.onPaneGone(paneId) } }
        TerminalScreen(
            pane = pane,
            state = state.pane(paneId),
            herdrAvailable = state.herdrAvailable,
            fontSizeSp = state.fontSizeSp,
            frames = frames,
            actions = remember(paneId) { viewModel.actionsFor(paneId, onBack) },
        )
    }
}

private fun TerminalViewModel.actionsFor(paneId: String, onBack: () -> Unit) = PaneActions(
    onBack = onBack,
    onGridMeasured = { cols, rows -> onGridMeasured(paneId, cols, rows) },
    onInput = { bytes -> onInput(paneId, bytes) },
    onKey = { key -> onKey(paneId, key) },
    onDraftChange = { draft -> onDraftChange(paneId, draft) },
    onSubmit = { submit(paneId) },
    onLoadHistory = { loadHistory(paneId) },
    onExitHistory = { exitHistory(paneId) },
    onTakeControl = { takeControl(paneId) },
    onReleaseControl = { releaseControl(paneId) },
    onFontSizeChanged = ::setFontSize,
    onJumpToLatest = { jumpToLatest(paneId) },
)
