package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.data.PairUriParser
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.AgentStatus
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.ServerMessage
import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeJson
import io.github.vladimirvasilev.herdrapp.domain.AgentOrganizer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class OrganizerAndPairingTest {
    @Test fun blockedAgentsFloatWhilePreservingWorkspaceOrder() {
        val snapshot = assertIs<ServerMessage.Snapshot>(BridgeJson.parse(readResource("snapshot_fixture.json"))).value
        val ordered = AgentOrganizer.orderedAgents(snapshot.agents, snapshot.workspaces)
        assertEquals("w1:p1", ordered.first().paneId)
        assertEquals(AgentStatus.BLOCKED, ordered.first().agentStatus)
        val groups = AgentOrganizer.groups(snapshot.workspaces, ordered, snapshot.panes)
        assertEquals(1, groups.first { it.workspace.workspaceId == "w1" }.otherPanes.size)
    }

    @Test fun parsesPairUriWithDefaultsAndEscaping() {
        val host = PairUriParser.parse("herdr-bridge://pair?host=100.64.0.2&token=secret&name=Work+PC")
        assertNotNull(host)
        assertEquals("Work PC", host.name)
        assertEquals(8787, host.port)
        val withPort = PairUriParser.parse("herdr-bridge://pair?host=pc.local&port=9000&token=abc%20123&name=Desk")
        assertEquals(9000, withPort?.port)
        assertEquals("abc 123", withPort?.token)
        assertNull(PairUriParser.parse("https://example.com"))
    }
}
