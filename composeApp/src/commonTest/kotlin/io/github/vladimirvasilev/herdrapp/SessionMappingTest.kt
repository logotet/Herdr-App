package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeJson
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.ServerMessage
import io.github.vladimirvasilev.herdrapp.data.bridge.parsePaneRead
import io.github.vladimirvasilev.herdrapp.data.bridge.toSession
import io.github.vladimirvasilev.herdrapp.domain.AgentOrganizer
import io.github.vladimirvasilev.herdrapp.domain.AgentState
import io.github.vladimirvasilev.herdrapp.domain.AgentStatus
import io.github.vladimirvasilev.herdrapp.domain.GridSize
import io.github.vladimirvasilev.herdrapp.domain.Pane
import io.github.vladimirvasilev.herdrapp.domain.Session
import io.github.vladimirvasilev.herdrapp.domain.Tab
import io.github.vladimirvasilev.herdrapp.domain.Workspace
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SessionMappingTest {
    private fun fixture(): Session =
        assertIs<ServerMessage.Snapshot>(BridgeJson.parse(readResource("snapshot_fixture.json"))).value.toSession()

    @Test
    fun blockedAgentsComeFirst() {
        val first = fixture().agents.first()
        assertEquals("w1:p1", first.paneId)
        assertEquals(AgentStatus.BLOCKED, first.agent?.status)
    }

    @Test
    fun agentCarriesItsKindTitleAndPreview() {
        val pane = fixture().agents.first { it.paneId == "w2:p1" }
        assertEquals("claude", pane.agent?.kind)
        assertEquals("Implement websocket", pane.title)
        assertEquals("Running tests", pane.agent?.preview)
    }

    @Test
    fun groupsHoldEveryPaneOfAWorkspaceByTab() {
        val groups = AgentOrganizer.groups(fixture())
        assertEquals(listOf("Mobile", "Bridge"), groups.map { it.workspace.label })

        val mobile = groups.first()
        assertEquals(listOf("copilot", "shell"), mobile.tabs.map { it.tab?.label })
        assertEquals(listOf("Fix tests", "PowerShell"), mobile.panes.map { it.title })
        assertNull(mobile.panes.last().agent)
    }

    @Test
    fun aWorkspaceShowsItsMostUrgentAgentStatus() {
        val groups = AgentOrganizer.groups(fixture())
        assertEquals(listOf(AgentStatus.BLOCKED, AgentStatus.WORKING), groups.map { it.status })

        val noAgents = Session(
            workspaces = listOf(Workspace("w1", 1, "Tools", 1)),
            panes = listOf(Pane("w1:p1", "w1", null, "lazygit")),
        )
        assertNull(AgentOrganizer.groups(noAgents).single().status)
    }

    @Test
    fun inATabAgentsComeBeforeOtherPanesAndBlockedOnesFirst() {
        fun agent(id: String, status: AgentStatus) = Pane(id, "w1", "w1:t1", id, AgentState("claude", status, null))
        val session = Session(
            workspaces = listOf(Workspace("w1", 1, "Mobile", 3)),
            tabs = listOf(Tab("w1:t1", "w1", 1, "main")),
            panes = listOf(Pane("nvim", "w1", "w1:t1", "nvim"), agent("idle", AgentStatus.IDLE), agent("blocked", AgentStatus.BLOCKED)),
        )

        assertEquals(listOf("blocked", "idle", "nvim"), AgentOrganizer.groups(session).single().panes.map { it.paneId })
    }

    @Test
    fun agentCarriesHowFarThePcViewIsScrolledBackAndThePcSize() {
        val snapshot = """{"type":"snapshot","workspaces":[{"workspace_id":"w1","number":1,"label":"A"}],
            "agents":[{"pane_id":"w1:p1","workspace_id":"w1"},{"pane_id":"w1:p2","workspace_id":"w1"}],
            "panes":[{"pane_id":"w1:p1","workspace_id":"w1","scroll":{"offset_from_bottom":499,"max_offset_from_bottom":618,"viewport_rows":39}},
                     {"pane_id":"w1:p2","workspace_id":"w1","scroll":{"offset_from_bottom":0,"max_offset_from_bottom":12,"viewport_rows":39}}],
            "pane_sizes":{"w1:p1":[144,39]}}"""
        val agents = assertIs<ServerMessage.Snapshot>(BridgeJson.parse(snapshot)).value.toSession().agents

        assertEquals(499, agents[0].scrolledBackLines)
        assertEquals(GridSize(144, 39), agents[0].pcGrid)
        assertEquals(0, agents[1].scrolledBackLines)
        assertNull(agents[1].pcGrid)
    }

    @Test
    fun parsesPaneReadResult() {
        val data = Json.parseToJsonElement("""{"type":"pane_read","read":{"pane_id":"w1:p1","text":"a\r\nb","truncated":true}}""")
        val history = assertNotNull(parsePaneRead(data))
        assertEquals("a\r\nb", history.text)
        assertTrue(history.truncated)
        assertEquals(2, history.lineCount)
        assertNull(parsePaneRead(Json.parseToJsonElement("""{"type":"ok"}""")))
    }
}
