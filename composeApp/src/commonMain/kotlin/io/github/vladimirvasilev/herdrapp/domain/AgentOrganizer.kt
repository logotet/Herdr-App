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

/**
 * Where an agent pane is, for a line under its title. A part is null when it would say nothing:
 * a tab that only has a number for a name, a folder named like the workspace.
 */
data class PaneContext(val workspace: String?, val tab: String?, val folder: String?)

object AgentOrganizer {
    /** The [PaneContext] of every agent pane, by pane id. */
    fun contexts(session: Session): Map<String, PaneContext> {
        val workspaces = session.workspaces.associateBy { it.id }
        val tabs = session.tabs.associateBy { it.id }
        return session.agents.associate { pane ->
            val workspace = workspaces[pane.workspaceId]?.label?.takeIf { it.isNotBlank() }
            val tab = tabs[pane.tabId]?.takeIf { it.label.isNotBlank() && !it.label.all(Char::isDigit) }
            val folder = pane.folder?.takeIf { !it.equals(workspace, ignoreCase = true) }
            pane.paneId to PaneContext(workspace, tab?.label, folder)
        }
    }

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
