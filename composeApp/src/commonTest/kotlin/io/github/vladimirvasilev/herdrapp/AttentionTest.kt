package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.domain.AgentState
import io.github.vladimirvasilev.herdrapp.domain.AgentStatus
import io.github.vladimirvasilev.herdrapp.domain.AttentionEvent
import io.github.vladimirvasilev.herdrapp.domain.AttentionKind
import io.github.vladimirvasilev.herdrapp.domain.Pane
import io.github.vladimirvasilev.herdrapp.domain.Session
import io.github.vladimirvasilev.herdrapp.domain.Workspace
import io.github.vladimirvasilev.herdrapp.domain.attention
import io.github.vladimirvasilev.herdrapp.domain.attentionEvents
import io.github.vladimirvasilev.herdrapp.domain.keepWatching
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AttentionTest {
    private fun agent(paneId: String, status: AgentStatus) =
        Pane(paneId, "w1", "w1:t1", "Fix tests", AgentState("claude", status, "last line"))

    private fun session(vararg agents: Pane) = Session(
        workspaces = listOf(Workspace("w1", 1, "api", agents.size)),
        agents = agents.toList(),
        panes = agents.toList(),
    )

    @Test
    fun anAgentThatStartsWaitingIsAnEvent() {
        val events = attentionEvents(
            before = session(agent("w1:p1", AgentStatus.WORKING)),
            after = session(agent("w1:p1", AgentStatus.BLOCKED)),
        )

        assertEquals(listOf(AttentionEvent("w1:p1", AttentionKind.BLOCKED, "Fix tests", "api", "last line")), events)
    }

    @Test
    fun anAgentThatFinishesIsAnEventAndSoIsOneThatFinishesAfterWaiting() {
        val working = session(agent("w1:p1", AgentStatus.WORKING))
        val blocked = session(agent("w1:p1", AgentStatus.BLOCKED))
        val done = session(agent("w1:p1", AgentStatus.DONE))

        assertEquals(listOf(AttentionKind.DONE), attentionEvents(working, done).map { it.kind })
        assertEquals(listOf(AttentionKind.DONE), attentionEvents(blocked, done).map { it.kind })
    }

    @Test
    fun anAgentThatKeepsWaitingOrGoesBackToWorkIsNotAnEvent() {
        val blocked = session(agent("w1:p1", AgentStatus.BLOCKED))

        assertTrue(attentionEvents(blocked, blocked).isEmpty())
        assertTrue(attentionEvents(blocked, session(agent("w1:p1", AgentStatus.WORKING))).isEmpty())
        assertTrue(attentionEvents(blocked, session()).isEmpty())
    }

    @Test
    fun anAgentThatAppearsAlreadyWaitingIsAnEvent() {
        val events = attentionEvents(session(), session(agent("w1:p2", AgentStatus.BLOCKED)))

        assertEquals(listOf("w1:p2"), events.map { it.paneId })
    }

    @Test
    fun whatIsWaitingBeforeTheFirstSnapshotIsNotAnEvent() {
        assertTrue(attentionEvents(Session(), session(agent("w1:p1", AgentStatus.BLOCKED))).isEmpty())
    }

    @Test
    fun attentionListsTheWaitingAndTheFinishedAgents() {
        val now = session(
            agent("w1:p1", AgentStatus.BLOCKED),
            agent("w1:p2", AgentStatus.WORKING),
            agent("w1:p3", AgentStatus.DONE),
            agent("w1:p4", AgentStatus.IDLE),
        )

        assertEquals(mapOf("w1:p1" to AttentionKind.BLOCKED, "w1:p3" to AttentionKind.DONE), now.attention())
    }

    @Test
    fun theAppKeepsWatchingOnlyWhileSomethingCanStillHappen() {
        val working = session(agent("w1:p1", AgentStatus.WORKING))
        val blocked = session(agent("w1:p1", AgentStatus.BLOCKED))
        val quiet = session(agent("w1:p1", AgentStatus.DONE), agent("w1:p2", AgentStatus.IDLE))

        assertTrue(keepWatching(enabled = true, appVisible = false, connected = true, session = working))
        assertTrue(keepWatching(enabled = true, appVisible = false, connected = true, session = blocked))
        assertTrue(keepWatching(enabled = true, appVisible = true, connected = true, session = quiet))
        assertFalse(keepWatching(enabled = true, appVisible = false, connected = true, session = quiet))
        assertFalse(keepWatching(enabled = true, appVisible = false, connected = false, session = working))
        assertFalse(keepWatching(enabled = false, appVisible = true, connected = true, session = working))
    }
}
