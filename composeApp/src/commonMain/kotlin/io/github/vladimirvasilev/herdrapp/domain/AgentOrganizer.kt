package io.github.vladimirvasilev.herdrapp.domain

data class WorkspaceGroup(val workspace: Workspace, val agents: List<Pane>, val otherPanes: List<Pane> = emptyList())

object AgentOrganizer {
    /** Blocked agents first, then by the position of their workspace in [workspaceIds]. */
    fun orderedAgents(agents: List<Pane>, workspaceIds: List<String>): List<Pane> {
        val workspaceOrder = workspaceIds.withIndex().associate { it.value to it.index }
        return agents.sortedWith(
            compareByDescending<Pane> { it.agent?.status == AgentStatus.BLOCKED }
                .thenBy { workspaceOrder[it.workspaceId] ?: Int.MAX_VALUE }
                .thenBy { it.tabId ?: "" }
                .thenBy { it.paneId }
        )
    }

    fun needsYou(agents: List<Pane>): List<Pane> = agents.filter { it.agent?.status == AgentStatus.BLOCKED }

    fun groups(session: Session): List<WorkspaceGroup> =
        session.workspaces.sortedBy { it.number }.map { ws ->
            WorkspaceGroup(
                workspace = ws,
                agents = orderedAgents(session.agents.filter { it.workspaceId == ws.id }, listOf(ws.id)),
                otherPanes = session.panes.filter { it.workspaceId == ws.id && it.agent == null },
            )
        }
}
