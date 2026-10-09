package io.github.vladimirvasilev.herdrapp.domain

/** Why an agent wants the user's attention. */
enum class AttentionKind {
    /** It is waiting for an answer. */
    BLOCKED,
    /** It finished, and nobody has looked at the result yet. */
    DONE,
}

/** An agent that started waiting, or finished, between two snapshots. */
data class AttentionEvent(
    val paneId: String,
    val kind: AttentionKind,
    val title: String,
    val workspaceLabel: String?,
    val preview: String?,
)

private fun Pane.attentionKind(): AttentionKind? = when (agent?.status) {
    AgentStatus.BLOCKED -> AttentionKind.BLOCKED
    AgentStatus.DONE -> AttentionKind.DONE
    else -> null
}

/** The agents that want attention right now, by pane id. */
fun Session.attention(): Map<String, AttentionKind> =
    agents.mapNotNull { pane -> pane.attentionKind()?.let { pane.paneId to it } }.toMap()

/**
 * The agents that want attention in [after] and did not, or not for the same reason, in [before].
 *
 * A [before] without workspaces is the state before the first snapshot. Nothing is reported then:
 * what was already waiting when the app connected is not news.
 */
fun attentionEvents(before: Session, after: Session): List<AttentionEvent> {
    if (before.workspaces.isEmpty()) return emptyList()
    val was = before.attention()
    val workspaceLabels = after.workspaces.associate { it.id to it.label }
    return after.agents.mapNotNull { pane ->
        val kind = pane.attentionKind() ?: return@mapNotNull null
        if (was[pane.paneId] == kind) return@mapNotNull null
        AttentionEvent(pane.paneId, kind, pane.title, workspaceLabels[pane.workspaceId], pane.agent?.preview)
    }
}

/**
 * Whether the app should stay connected while it is off the screen, so it can tell the user about
 * an [attentionEvents]. It should only while something can still happen: an agent is working, or
 * one is waiting and the answer may be given on the PC. With every agent idle or done it lets go,
 * and so it does when the bridge cannot be reached.
 */
fun keepWatching(enabled: Boolean, appVisible: Boolean, connected: Boolean, session: Session): Boolean {
    if (!enabled || !connected) return false
    return appVisible || session.agents.any {
        it.agent?.status == AgentStatus.WORKING || it.agent?.status == AgentStatus.BLOCKED
    }
}
