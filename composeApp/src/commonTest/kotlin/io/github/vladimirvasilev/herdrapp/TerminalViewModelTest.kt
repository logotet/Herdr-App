package io.github.vladimirvasilev.herdrapp

import androidx.compose.ui.text.input.TextFieldValue
import io.github.vladimirvasilev.herdrapp.domain.CommandResult
import io.github.vladimirvasilev.herdrapp.domain.HistoryResult
import io.github.vladimirvasilev.herdrapp.domain.PaneHistory
import io.github.vladimirvasilev.herdrapp.domain.StreamMode
import io.github.vladimirvasilev.herdrapp.ui.terminal.KeySpec
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

@OptIn(ExperimentalCoroutinesApi::class)
class TerminalViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val terminal = FakeTerminalRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    private fun TestScope.viewModel(): TerminalViewModel {
        val viewModel = TerminalViewModel(FakeSessionRepository(), terminal, FakeSettingsRepository(), testTimeSource)
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
        runCurrent()

        assertEquals(listOf("open $PANE 45x30", "resize $PANE 45x20"), terminal.calls)
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

        assertEquals(PaneUiState(draft = TextFieldValue("hello"), notice = "Send failed: pane is gone"), pane(viewModel))
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

        viewModel.takeControl(PANE)
        runCurrent()

        assertEquals("takeControl $PANE 45x30", terminal.calls.last())
    }

    private companion object {
        const val PANE = "w1:p1"
        val LONG_HISTORY = (1..40).joinToString("\n") { "line $it" }
    }
}
