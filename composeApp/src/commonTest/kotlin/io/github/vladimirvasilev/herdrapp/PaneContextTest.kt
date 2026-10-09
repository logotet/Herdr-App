package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.data.bridge.folderName
import io.github.vladimirvasilev.herdrapp.domain.AgentOrganizer
import io.github.vladimirvasilev.herdrapp.domain.AgentState
import io.github.vladimirvasilev.herdrapp.domain.AgentStatus
import io.github.vladimirvasilev.herdrapp.domain.Pane
import io.github.vladimirvasilev.herdrapp.domain.PaneContext
import io.github.vladimirvasilev.herdrapp.domain.Session
import io.github.vladimirvasilev.herdrapp.domain.Tab
import io.github.vladimirvasilev.herdrapp.domain.Workspace
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class PaneContextTest {
    private fun agent(paneId: String, tabId: String?, folder: String?) = Pane(
        paneId = paneId,
        workspaceId = "w1",
        tabId = tabId,
        title = "Fix tests",
        agent = AgentState("claude", AgentStatus.IDLE, null),
        folder = folder,
    )

    private fun session(vararg agents: Pane) = Session(
        workspaces = listOf(Workspace("w1", 1, "Mobile", agents.size)),
        // A tab's number name need not be its position: herdr keeps it when earlier tabs close.
        tabs = listOf(Tab("w1:t1", "w1", 1, "release"), Tab("w1:t2", "w1", 2, "3")),
        agents = agents.toList(),
        panes = agents.toList(),
    )

    @Test
    fun anAgentIsPlacedByItsWorkspaceTabAndFolder() {
        val contexts = AgentOrganizer.contexts(session(agent("w1:p1", "w1:t1", "herdr-app")))

        assertEquals(PaneContext("Mobile", "release", "herdr-app"), contexts["w1:p1"])
    }

    @Test
    fun aTabThatOnlyHasANumberForANameIsLeftOut() {
        val contexts = AgentOrganizer.contexts(session(agent("w1:p1", "w1:t2", "herdr-app")))

        assertEquals(PaneContext("Mobile", null, "herdr-app"), contexts["w1:p1"])
    }

    @Test
    fun aFolderNamedLikeTheWorkspaceIsLeftOut() {
        val contexts = AgentOrganizer.contexts(session(agent("w1:p1", "w1:t1", "mobile")))

        assertEquals(PaneContext("Mobile", "release", null), contexts["w1:p1"])
    }

    @Test
    fun anAgentWithoutATabOrAFolderKeepsItsWorkspace() {
        val contexts = AgentOrganizer.contexts(session(agent("w1:p1", null, null)))

        assertEquals(PaneContext("Mobile", null, null), contexts["w1:p1"])
    }

    @Test
    fun theFolderIsTheLastPartOfTheWorkingDirectory() {
        assertEquals("mobile", folderName("C:\\fake\\mobile"))
        assertEquals("mobile", folderName("C:\\fake\\mobile\\"))
        assertEquals("bridge", folderName("/home/me/bridge"))
        assertEquals("bridge", folderName("/home/me/bridge/"))
        assertNull(folderName(null))
        assertNull(folderName(""))
        assertNull(folderName("/"))
    }

    @Test
    fun aHomeFolderIsShownAsATilde() {
        assertEquals("~", folderName("C:\\Users\\someone"))
        assertEquals("~", folderName("/home/someone/"))
        assertEquals("projects", folderName("C:\\Users\\someone\\projects"))
    }
}
