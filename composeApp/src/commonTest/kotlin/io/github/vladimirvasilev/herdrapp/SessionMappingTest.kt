package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeJson
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.ServerMessage
import io.github.vladimirvasilev.herdrapp.data.bridge.parsePaneRead
import io.github.vladimirvasilev.herdrapp.data.bridge.toSession
import io.github.vladimirvasilev.herdrapp.domain.AgentOrganizer
import io.github.vladimirvasilev.herdrapp.domain.AgentStatus
import io.github.vladimirvasilev.herdrapp.domain.Session
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
        assertEquals(AgentStatus.BLOCKED, first.status)
    }

    @Test
    fun agentCarriesItsKindTitleAndPreview() {
        val agent = fixture().agents.first { it.paneId == "w2:p1" }
        assertEquals("claude", agent.kind)
        assertEquals("Implement websocket", agent.title)
        assertEquals("Running tests", agent.preview)
    }

    @Test
    fun groupsListPanesWithoutAnAgentSeparately() {
        val groups = AgentOrganizer.groups(fixture())
        assertEquals(listOf("Mobile", "Bridge"), groups.map { it.workspace.label })
        assertEquals(listOf("PowerShell"), groups.first().otherPanes.map { it.title })
        assertTrue(groups.last().otherPanes.isEmpty())
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
