package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.domain.Agent
import io.github.vladimirvasilev.herdrapp.domain.AgentStatus
import io.github.vladimirvasilev.herdrapp.domain.Session
import io.github.vladimirvasilev.herdrapp.domain.Workspace
import io.github.vladimirvasilev.herdrapp.ui.home.HomeViewModel
import io.github.vladimirvasilev.herdrapp.ui.hosts.HostForm
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

    @BeforeTest
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun homeConnectsToTheFirstSavedHost() = runTest(dispatcher) {
        HomeViewModel(session, FakeHostRepository(listOf(TEST_HOST)))
        runCurrent()

        assertEquals(listOf(TEST_HOST), session.connected)
    }

    @Test
    fun homeLeavesAnExistingConnectionAlone() = runTest(dispatcher) {
        session.currentHost.value = TEST_HOST
        HomeViewModel(session, FakeHostRepository(listOf(TEST_HOST.copy(id = "other"))))
        runCurrent()

        assertTrue(session.connected.isEmpty())
    }

    @Test
    fun homeShowsBlockedAgentsAndWorkspaceGroups() = runTest(dispatcher) {
        val viewModel = HomeViewModel(session, FakeHostRepository(listOf(TEST_HOST)))
        backgroundScope.launch { viewModel.uiState.collect {} }
        val blocked = Agent("w1:p1", "w1", null, "claude", "Fix tests", AgentStatus.BLOCKED, null)

        session.session.value = Session(workspaces = listOf(Workspace("w1", 1, "Mobile", 1)), agents = listOf(blocked))
        runCurrent()

        val state = viewModel.uiState.value
        assertTrue(state.hasHosts)
        assertEquals("Desk", state.hostName)
        assertEquals(listOf(blocked), state.needsYou)
        assertEquals(listOf(blocked), state.groups.single().agents)
    }

    @Test
    fun hostsRejectsAnIncompleteForm() = runTest(dispatcher) {
        val hosts = FakeHostRepository()
        val viewModel = HostsViewModel(hosts, session)
        backgroundScope.launch { viewModel.uiState.collect {} }
        viewModel.onHostChange("pc.local")

        assertFalse(viewModel.saveForm())
        runCurrent()

        assertEquals("Host and token are required", viewModel.uiState.value.form.error)
        assertTrue(hosts.hosts.value.isEmpty())
    }

    @Test
    fun hostsSavesTheFormConnectsAndClearsIt() = runTest(dispatcher) {
        val hosts = FakeHostRepository()
        val viewModel = HostsViewModel(hosts, session)
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
        val viewModel = HostsViewModel(hosts, session)
        backgroundScope.launch { viewModel.uiState.collect {} }

        assertFalse(viewModel.onScanned(null))
        assertFalse(viewModel.onScanned("https://example.com"))
        runCurrent()
        assertEquals("Invalid QR", viewModel.uiState.value.form.error)

        assertTrue(viewModel.onScanned("herdr-bridge://pair?host=pc.local&token=abc&name=Desk"))
        runCurrent()
        assertEquals("Desk", hosts.hosts.value.single().name)
    }

    @Test
    fun hostsDeletesASavedHost() = runTest(dispatcher) {
        val hosts = FakeHostRepository(listOf(TEST_HOST))
        HostsViewModel(hosts, session).delete(TEST_HOST)
        runCurrent()

        assertTrue(hosts.hosts.value.isEmpty())
    }
}
