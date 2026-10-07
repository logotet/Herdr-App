package io.github.vladimirvasilev.herdrapp.domain

data class WorkspaceGroup(val workspace: Workspace, val agents: List<Agent>, val otherPanes: List<Pane> = emptyList())

object AgentOrganizer {
    /** Blocked agents first, then by the position of their workspace in [workspaceIds]. */
    fun orderedAgents(agents: List<Agent>, workspaceIds: List<String>): List<Agent> {
        val workspaceOrder = workspaceIds.withIndex().associate { it.value to it.index }
        return agents.sortedWith(
            compareByDescending<Agent> { it.status == AgentStatus.BLOCKED }
                .thenBy { workspaceOrder[it.workspaceId] ?: Int.MAX_VALUE }
                .thenBy { it.tabId ?: "" }
                .thenBy { it.paneId }
        )
    }

    fun needsYou(agents: List<Agent>): List<Agent> = agents.filter { it.status == AgentStatus.BLOCKED }

    fun groups(session: Session): List<WorkspaceGroup> {
        val agentPaneIds = session.agents.map { it.paneId }.toSet()
        return session.workspaces.sortedBy { it.number }.map { ws ->
            WorkspaceGroup(
                workspace = ws,
                agents = orderedAgents(session.agents.filter { it.workspaceId == ws.id }, listOf(ws.id)),
                otherPanes = session.panes.filter { it.workspaceId == ws.id && it.paneId !in agentPaneIds },
            )
        }
    }
}
