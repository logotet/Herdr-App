package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.domain.AgentState
import io.github.vladimirvasilev.herdrapp.domain.AgentStatus
import io.github.vladimirvasilev.herdrapp.domain.Pane
import io.github.vladimirvasilev.herdrapp.domain.Session
import io.github.vladimirvasilev.herdrapp.domain.Workspace
import io.github.vladimirvasilev.herdrapp.domain.CommandResult
import io.github.vladimirvasilev.herdrapp.domain.HomeView
import io.github.vladimirvasilev.herdrapp.ui.home.HomeDialog
import io.github.vladimirvasilev.herdrapp.ui.home.HomeViewModel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlin.test.assertNull
import io.github.vladimirvasilev.herdrapp.ui.hosts.HostForm
import io.github.vladimirvasilev.herdrapp.ui.hosts.HostFormError
import io.github.vladimirvasilev.herdrapp.ui.hosts.HostsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HomeAndHostsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val session = FakeSessionRepository()
    private val settings = FakeSettingsRepository()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun homeConnectsToTheFirstSavedHost() = runTest(dispatcher) {
        HomeViewModel(session, FakeHostRepository(listOf(TEST_HOST)), settings)
        runCurrent()

        assertEquals(listOf(TEST_HOST), session.connected)
    }

    @Test
    fun homeLeavesAnExistingConnectionAlone() = runTest(dispatcher) {
        session.currentHost.value = TEST_HOST
        HomeViewModel(session, FakeHostRepository(listOf(TEST_HOST.copy(id = "other"))), settings)
        runCurrent()

        assertTrue(session.connected.isEmpty())
    }

    @Test
    fun homeShowsBlockedAgentsAndWorkspaceGroups() = runTest(dispatcher) {
        val viewModel = HomeViewModel(session, FakeHostRepository(listOf(TEST_HOST)), settings)
        backgroundScope.launch { viewModel.uiState.collect {} }
        val blocked = Pane("w1:p1", "w1", null, "Fix tests", AgentState("claude", AgentStatus.BLOCKED, null))

        session.session.value = Session(
            workspaces = listOf(Workspace("w1", 1, "Mobile", 1)),
            agents = listOf(blocked),
            panes = listOf(blocked),
        )
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.hasHosts)
        assertEquals("Desk", state.hostName)
        assertEquals(listOf(blocked), state.needsYou)
        assertEquals(listOf(blocked), state.groups.single().panes)
    }

    private fun TestScope.home(): HomeViewModel {
        val viewModel = HomeViewModel(session, FakeHostRepository(listOf(TEST_HOST)), settings)
        backgroundScope.launch { viewModel.uiState.collect {} }
        return viewModel
    }

    private fun TestScope.confirm(viewModel: HomeViewModel, dialog: HomeDialog, name: String = "") {
        viewModel.showDialog(dialog)
        runCurrent()
        assertEquals(dialog, viewModel.uiState.value.dialog)
        viewModel.confirmDialog(name)
        runCurrent()
    }

    @Test
    fun confirmingADialogChangesTheLayoutAndClosesIt() = runTest(dispatcher) {
        val viewModel = home()

        confirm(viewModel, HomeDialog.RenamePane("w1:p1", "old"), " tests ")
        confirm(viewModel, HomeDialog.ClosePane("w1:p1", "tests"))
        confirm(viewModel, HomeDialog.NewTab("w1", "Mobile"), "scratch")
        confirm(viewModel, HomeDialog.NewTab("w1", "Mobile"), "  ")
        confirm(viewModel, HomeDialog.RenameTab("w1:t1", "old"), "git")
        confirm(viewModel, HomeDialog.CloseTab("w1:t1", "git"))

        assertEquals(
            listOf(
                "renamePane w1:p1 tests",
                "closePane w1:p1",
                "createTab w1 scratch",
                "createTab w1 null",
                "renameTab w1:t1 git",
                "closeTab w1:t1",
            ),
            session.changes,
        )
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun theViewComesFromTheSettingAndChoosingOneStoresIt() = runTest(dispatcher) {
        settings.homeView.value = HomeView.AGENTS
        val viewModel = home()
        assertEquals(HomeView.AGENTS, viewModel.uiState.value.view)

        viewModel.setView(HomeView.WORKSPACES)
        runCurrent()

        assertEquals(HomeView.WORKSPACES, settings.homeView.value)
        assertEquals(HomeView.WORKSPACES, viewModel.uiState.value.view)
    }

    @Test
    fun theAgentsViewListsAgentsOnlyInTheSessionOrder() = runTest(dispatcher) {
        val viewModel = home()
        val blocked = Pane("w2:p1", "w2", null, "Fix tests", AgentState("claude", AgentStatus.BLOCKED, null))
        val idle = Pane("w1:p1", "w1", null, "Pull tasks", AgentState("claude", AgentStatus.IDLE, null))

        session.session.value = Session(
            agents = listOf(blocked, idle),
            panes = listOf(idle, Pane("w1:p2", "w1", null, "nvim"), blocked),
        )
        runCurrent()

        assertEquals(listOf(blocked, idle), viewModel.uiState.value.agents)
    }

    @Test
    fun workspacesAreCreatedRenamedAndClosed() = runTest(dispatcher) {
        val viewModel = home()

        confirm(viewModel, HomeDialog.NewWorkspace, "scratch")
        confirm(viewModel, HomeDialog.NewWorkspace)
        confirm(viewModel, HomeDialog.RenameWorkspace("w1", "old"), "mobile")
        confirm(viewModel, HomeDialog.RenameWorkspace("w1", "old"), " ")
        viewModel.dismissDialog()
        confirm(viewModel, HomeDialog.CloseWorkspace("w1", "mobile", paneCount = 4))

        assertEquals(
            listOf("createWorkspace scratch", "createWorkspace null", "renameWorkspace w1 mobile", "closeWorkspace w1"),
            session.changes,
        )
    }

    @Test
    fun aRenameWithoutANameIsNotSent() = runTest(dispatcher) {
        val viewModel = home()
        val dialog = HomeDialog.RenamePane("w1:p1", "old")

        confirm(viewModel, dialog, "   ")

        assertTrue(session.changes.isEmpty())
        assertEquals(dialog, viewModel.uiState.value.dialog)
    }

    @Test
    fun dismissingADialogChangesNothing() = runTest(dispatcher) {
        val viewModel = home()
        viewModel.showDialog(HomeDialog.ClosePane("w1:p1", "tests"))

        viewModel.dismissDialog()
        runCurrent()

        assertTrue(session.changes.isEmpty())
        assertNull(viewModel.uiState.value.dialog)
    }

    @Test
    fun aRefusedChangeIsShownForAMoment() = runTest(dispatcher) {
        val viewModel = home()
        session.changeResult = CommandResult.Failure("pane not found")

        confirm(viewModel, HomeDialog.ClosePane("w1:p1", "tests"))
        assertEquals("pane not found", viewModel.uiState.value.changeFailed)

        advanceTimeBy(4_001)
        runCurrent()
        assertNull(viewModel.uiState.value.changeFailed)
    }

    @Test
    fun hostsRejectsAnIncompleteForm() = runTest(dispatcher) {
        val hosts = FakeHostRepository()
        val viewModel = HostsViewModel(hosts, session, settings)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onHostChange("pc.local")

        assertFalse(viewModel.saveForm())
        runCurrent()

        assertEquals(HostFormError.MISSING_FIELDS, viewModel.uiState.value.form.error)
        assertTrue(hosts.hosts.value.isEmpty())
    }

    @Test
    fun hostsSavesTheFormConnectsAndClearsIt() = runTest(dispatcher) {
        val hosts = FakeHostRepository()
        val viewModel = HostsViewModel(hosts, session, settings)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onHostChange("pc.local")
        viewModel.onPortChange("9000")
        viewModel.onTokenChange("secret")

        assertTrue(viewModel.saveForm())
        runCurrent()

        val saved = hosts.hosts.value.single()
        assertEquals("pc.local", saved.name)
        assertEquals(9000, saved.port)
        assertEquals(listOf(saved), session.connected)
        assertEquals(HostForm(), viewModel.uiState.value.form)
    }

    @Test
    fun hostsHandlesScanResults() = runTest(dispatcher) {
        val hosts = FakeHostRepository()
        val viewModel = HostsViewModel(hosts, session, settings)
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertFalse(viewModel.onScanned(null))
        assertFalse(viewModel.onScanned("https://example.com"))
        runCurrent()
        assertEquals(HostFormError.INVALID_QR, viewModel.uiState.value.form.error)

        assertTrue(viewModel.onScanned("herdr-bridge://pair?host=pc.local&token=abc&name=Desk"))
        runCurrent()
        assertEquals("Desk", hosts.hosts.value.single().name)
    }

    @Test
    fun backgroundAlertsAreSwitchedOnOnlyWhenTheAppMayNotify() = runTest(dispatcher) {
        val viewModel = HostsViewModel(FakeHostRepository(), session, settings)
        backgroundScope.launch { viewModel.uiState.collect {} }

        viewModel.setBackgroundAlerts(enabled = true, mayNotify = false)
        runCurrent()
        assertFalse(settings.backgroundAlerts.value)
        assertTrue(viewModel.uiState.value.alertsDenied)

        viewModel.setBackgroundAlerts(enabled = true, mayNotify = true)
        runCurrent()
        assertTrue(viewModel.uiState.value.backgroundAlerts)
        assertFalse(viewModel.uiState.value.alertsDenied)

        viewModel.setBackgroundAlerts(enabled = false)
        runCurrent()
        assertFalse(settings.backgroundAlerts.value)
    }

    @Test
    fun hostsDeletesASavedHost() = runTest(dispatcher) {
        val hosts = FakeHostRepository(listOf(TEST_HOST))
        HostsViewModel(hosts, session, settings).delete(TEST_HOST)
        runCurrent()

        assertTrue(hosts.hosts.value.isEmpty())
    }
}
