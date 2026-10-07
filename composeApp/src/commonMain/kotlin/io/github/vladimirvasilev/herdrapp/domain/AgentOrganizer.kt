package io.github.vladimirvasilev.herdrapp.domain

/** The panes of one tab; [tab] is null for panes whose tab herdr did not report. */
data class TabGroup(val tab: Tab?, val panes: List<Pane>)

data class WorkspaceGroup(
    val workspace: Workspace,
    /** The most urgent status among the workspace's agents; null when it has none. */
    val status: AgentStatus?,
    val tabs: List<TabGroup>,
) {
    val panes: List<Pane> get() = tabs.flatMap { it.panes }
}

object AgentOrganizer {
    // Most urgent first.
    private val urgency = listOf(
        AgentStatus.BLOCKED,
        AgentStatus.WORKING,
        AgentStatus.DONE,
        AgentStatus.IDLE,
        AgentStatus.UNKNOWN,
    )

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

    /** Every workspace with its panes by tab. In a tab the agents come first, blocked ones on top. */
    fun groups(session: Session): List<WorkspaceGroup> {
        val tabsById = session.tabs.associateBy { it.id }
        return session.workspaces.sortedBy { it.number }.map { ws ->
            val panes = session.panes.filter { it.workspaceId == ws.id }
            val tabs = panes
                .groupBy { tabsById[it.tabId] }
                .map { (tab, inTab) -> TabGroup(tab, inTab.sortedBy { paneRank(it) }) }
                .sortedBy { it.tab?.number ?: Int.MAX_VALUE }
            WorkspaceGroup(
                workspace = ws,
                status = panes.mapNotNull { it.agent?.status }.minByOrNull { urgency.indexOf(it) },
                tabs = tabs,
            )
        }
    }

    private fun paneRank(pane: Pane): Int = when (pane.agent?.status) {
        null -> 2
        AgentStatus.BLOCKED -> 0
        else -> 1
    }
}
