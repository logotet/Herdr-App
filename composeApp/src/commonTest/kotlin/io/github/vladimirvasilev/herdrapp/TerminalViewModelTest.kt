package io.github.vladimirvasilev.herdrapp

import androidx.compose.ui.text.input.TextFieldValue
import io.github.vladimirvasilev.herdrapp.domain.AgentState
import io.github.vladimirvasilev.herdrapp.domain.AgentStatus
import io.github.vladimirvasilev.herdrapp.domain.CommandResult
import io.github.vladimirvasilev.herdrapp.domain.GridSize
import io.github.vladimirvasilev.herdrapp.domain.HistoryResult
import io.github.vladimirvasilev.herdrapp.domain.Pane
import io.github.vladimirvasilev.herdrapp.domain.PaneHistory
import io.github.vladimirvasilev.herdrapp.domain.Session
import io.github.vladimirvasilev.herdrapp.domain.StreamMode
import io.github.vladimirvasilev.herdrapp.domain.Workspace
import io.github.vladimirvasilev.herdrapp.ui.terminal.KeySpec
import io.github.vladimirvasilev.herdrapp.ui.terminal.PaneNotice
import io.github.vladimirvasilev.herdrapp.ui.terminal.PaneUiState
import io.github.vladimirvasilev.herdrapp.ui.terminal.TerminalViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.testTimeSource
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val terminal = FakeTerminalRepository()
    private val session = FakeSessionRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(opened: String = PANE): TerminalViewModel {
        val viewModel = TerminalViewModel(session, terminal, FakeSettingsRepository(), opened, testTimeSource)
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun TestScope.pane(viewModel: TerminalViewModel): PaneUiState {
        runCurrent()
        return viewModel.uiState.value.pane(PANE)
    }

    @Test
    fun theFirstGridOpensTheStreamAndLaterOnesResizeIt() = runTest(dispatcher) {
        val viewModel = viewModel()

        viewModel.onGridMeasured(PANE, 45, 30)
        viewModel.onGridMeasured(PANE, 45, 20)
        advanceTimeBy(301)

        assertEquals(listOf("open $PANE 45x30", "resize $PANE 45x20"), terminal.calls)
    }

    @Test
    fun aBurstOfGridChangesSendsOneResizeOnceItSettles() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onGridMeasured(PANE, 77, 53)
        runCurrent()
        terminal.calls.clear()

        // The keyboard sliding in: one report per animation step.
        listOf(50, 31, 24, 23, 22).forEach { rows ->
            viewModel.onGridMeasured(PANE, 77, rows)
            advanceTimeBy(50)
        }
        assertTrue(terminal.calls.isEmpty())

        advanceTimeBy(301)
        assertEquals(listOf("resize $PANE 77x22"), terminal.calls)
    }

    @Test
    fun aGridThatReturnsToTheSizeAlreadySentSendsNothing() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onGridMeasured(PANE, 77, 53)
        runCurrent()
        terminal.calls.clear()

        listOf(30, 22, 30, 53).forEach { rows ->
            viewModel.onGridMeasured(PANE, 77, rows)
            advanceTimeBy(50)
        }
        advanceTimeBy(301)

        assertTrue(terminal.calls.isEmpty())
    }

    @Test
    fun aPaneThatLeavesIsClosedAndKeepsOnlyItsDraft() = runTest(dispatcher) {
        val viewModel = viewModel()
        terminal.historyResult = HistoryResult.Loaded(PaneHistory(LONG_HISTORY, truncated = false))
        viewModel.onGridMeasured(PANE, 45, 30)
        viewModel.onDraftChange(PANE, TextFieldValue("half typed"))
        viewModel.loadHistory(PANE)
        runCurrent()

        viewModel.onPaneGone(PANE)

        assertEquals("close $PANE", terminal.calls.last())
        assertEquals(PaneUiState(draft = TextFieldValue("half typed")), pane(viewModel))
    }

    @Test
    fun submitSendsTheNormalizedDraftAndClearsIt() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onDraftChange(PANE, TextFieldValue("run tests\r\n"))

        viewModel.submit(PANE)

        assertEquals(PaneUiState(), pane(viewModel))
        assertEquals(listOf("submit $PANE run tests"), terminal.calls)
    }

    @Test
    fun aFailedSubmitKeepsTheDraftAndShowsANoticeForAMoment() = runTest(dispatcher) {
        val viewModel = viewModel()
        terminal.submitResult = CommandResult.Failure("pane is gone")
        viewModel.onDraftChange(PANE, TextFieldValue("hello"))

        viewModel.submit(PANE)

        assertEquals(PaneUiState(draft = TextFieldValue("hello"), notice = PaneNotice.SendFailed("pane is gone")), pane(viewModel))
        advanceTimeBy(2_501)
        assertNull(pane(viewModel).notice)
    }

    @Test
    fun historyLongerThanTheScreenIsShownUntilExited() = runTest(dispatcher) {
        val viewModel = viewModel()
        terminal.historyResult = HistoryResult.Loaded(PaneHistory(LONG_HISTORY, truncated = false))
        viewModel.onGridMeasured(PANE, 45, 30)

        viewModel.loadHistory(PANE)
        assertEquals(LONG_HISTORY, pane(viewModel).history)

        viewModel.exitHistory(PANE)
        assertNull(pane(viewModel).history)
    }

    @Test
    fun historyThatFitsTheScreenIsNotShownAndNotAskedForAgainRightAway() = runTest(dispatcher) {
        val viewModel = viewModel()
        terminal.historyResult = HistoryResult.Loaded(PaneHistory("one\ntwo", truncated = false))
        viewModel.onGridMeasured(PANE, 45, 30)
        runCurrent()
        terminal.calls.clear()

        viewModel.loadHistory(PANE)
        assertNull(pane(viewModel).history)
        viewModel.loadHistory(PANE)
        runCurrent()
        assertEquals(listOf("history $PANE"), terminal.calls)

        advanceTimeBy(5_001)
        viewModel.loadHistory(PANE)
        runCurrent()
        assertEquals(listOf("history $PANE", "history $PANE"), terminal.calls)
    }

    @Test
    fun plainKeysAreTypedAsTextUnlessThisClientHasControl() = runTest(dispatcher) {
        val viewModel = viewModel()
        val key = KeySpec.Bytes("y".encodeToByteArray(), "y")

        viewModel.onKey(PANE, key)
        viewModel.onInput(PANE, "ignored".encodeToByteArray())
        runCurrent()
        terminal.streamModes.value = mapOf(PANE to StreamMode.CONTROL)
        viewModel.onKey(PANE, key)
        viewModel.onInput(PANE, "raw".encodeToByteArray())
        viewModel.onKey(PANE, KeySpec.HerdrKey("esc"))
        runCurrent()

        assertEquals(listOf("text $PANE y", "input $PANE y", "input $PANE raw", "keys $PANE esc"), terminal.calls)
    }

    @Test
    fun takingControlUsesTheMeasuredGrid() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onGridMeasured(PANE, 45, 30)
        viewModel.onGridMeasured(PANE, 45, 28)

        viewModel.takeControl(PANE)
        advanceTimeBy(301)

        assertEquals(listOf("open $PANE 45x30", "takeControl $PANE 45x28"), terminal.calls)
    }

    @Test
    fun aControlledPaneThatLeavesIsTakenBackWhenItReturns() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onGridMeasured(PANE, 45, 30)
        terminal.streamModes.value = mapOf(PANE to StreamMode.CONTROL)
        runCurrent()
        terminal.calls.clear()

        viewModel.onPaneGone(PANE)
        terminal.streamModes.value = emptyMap()
        viewModel.onGridMeasured(PANE, 45, 28)
        runCurrent()

        assertEquals(listOf("close $PANE", "open $PANE 45x28", "takeControl $PANE 45x28"), terminal.calls)
    }

    @Test
    fun anObservedPaneThatLeavesComesBackObserved() = runTest(dispatcher) {
        val viewModel = viewModel()
        viewModel.onGridMeasured(PANE, 45, 30)
        runCurrent()
        terminal.calls.clear()

        viewModel.onPaneGone(PANE)
        viewModel.onGridMeasured(PANE, 45, 30)
        runCurrent()

        assertEquals(listOf("close $PANE", "open $PANE 45x30"), terminal.calls)
    }

    private fun scrolledBack(lines: Int, pcGrid: GridSize? = GridSize(144, 39)): Session {
        val pane = Pane(PANE, "w1", null, "Fix tests", AgentState("claude", AgentStatus.IDLE, null), lines, pcGrid)
        return Session(agents = listOf(pane), panes = listOf(pane))
    }

    private fun agent(paneId: String, workspaceId: String) =
        Pane(paneId, workspaceId, null, "agent", AgentState("claude", AgentStatus.IDLE, null))

    private fun shell(paneId: String, workspaceId: String) = Pane(paneId, workspaceId, null, "shell")

    private fun sessionOf(vararg panes: Pane) = Session(
        workspaces = panes.map { it.workspaceId }.distinct().mapIndexed { index, id -> Workspace(id, index + 1, id, 0) },
        agents = panes.filter { it.agent != null },
        panes = panes.toList(),
    )

    @Test
    fun openedOnAnAgentItPagesThroughEveryAgent() = runTest(dispatcher) {
        val viewModel = viewModel(opened = "w1:p1")
        session.session.value = sessionOf(agent("w1:p1", "w1"), shell("w1:p2", "w1"), agent("w2:p1", "w2"))
        runCurrent()

        assertEquals(listOf("w1:p1", "w2:p1"), viewModel.uiState.value.panes.map { it.paneId })
    }

    @Test
    fun openedOnAnotherPaneItPagesThroughThatWorkspace() = runTest(dispatcher) {
        val viewModel = viewModel(opened = "w1:p2")
        session.session.value = sessionOf(agent("w1:p1", "w1"), shell("w1:p2", "w1"), agent("w2:p1", "w2"))
        runCurrent()

        assertEquals(listOf("w1:p1", "w1:p2"), viewModel.uiState.value.panes.map { it.paneId })
    }

    @Test
    fun thePagesStayTheSameWhenThePaneGainsAnAgent() = runTest(dispatcher) {
        val viewModel = viewModel(opened = "w1:p2")
        session.session.value = sessionOf(agent("w1:p1", "w1"), shell("w1:p2", "w1"), agent("w2:p1", "w2"))
        runCurrent()

        session.session.value = sessionOf(agent("w1:p1", "w1"), agent("w1:p2", "w1"), agent("w2:p1", "w2"))
        runCurrent()

        assertEquals(listOf("w1:p1", "w1:p2"), viewModel.uiState.value.panes.map { it.paneId })
    }

    @Test
    fun jumpToLatestScrollsThePcPaneAndAsksForAFreshSnapshot() = runTest(dispatcher) {
        val viewModel = viewModel()
        session.session.value = scrolledBack(499)

        viewModel.jumpToLatest(PANE)
        viewModel.jumpToLatest(PANE)
        runCurrent()

        assertEquals(listOf("scrollToLatest $PANE 144x39 499"), terminal.calls)
        assertEquals(1, session.refreshes)
        assertEquals(PaneUiState(), pane(viewModel))
    }

    @Test
    fun jumpToLatestDoesNothingWhenThePaneIsLiveOrItsPcSizeIsUnknown() = runTest(dispatcher) {
        val viewModel = viewModel()

        session.session.value = scrolledBack(0)
        viewModel.jumpToLatest(PANE)
        session.session.value = scrolledBack(499, pcGrid = null)
        viewModel.jumpToLatest(PANE)
        runCurrent()

        assertTrue(terminal.calls.isEmpty())
    }

    @Test
    fun aFailedJumpIsReported() = runTest(dispatcher) {
        val viewModel = viewModel()
        session.session.value = scrolledBack(499)
        terminal.scrollResult = CommandResult.Failure("pane is controlled elsewhere")

        viewModel.jumpToLatest(PANE)

        assertEquals(PaneNotice.JumpFailed("pane is controlled elsewhere"), pane(viewModel).notice)
        assertEquals(0, session.refreshes)
    }

    private companion object {
        const val PANE = "w1:p1"
        val LONG_HISTORY = (1..40).joinToString("\n") { "line $it" }
    }
}
