package io.github.vladimirvasilev.herdrapp.domain

import io.github.vladimirvasilev.herdrapp.data.bridge.dto.AgentInfo
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.AgentStatus
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.PaneInfo
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.WorkspaceInfo

data class WorkspaceGroup(val workspace: WorkspaceInfo, val agents: List<AgentInfo>, val otherPanes: List<PaneInfo> = emptyList())

object AgentOrganizer {
    fun orderedAgents(agents: List<AgentInfo>, workspaces: List<WorkspaceInfo>): List<AgentInfo> {
        val workspaceOrder = workspaces.withIndex().associate { it.value.workspaceId to it.index }
        return agents.sortedWith(
            compareByDescending<AgentInfo> { it.agentStatus == AgentStatus.BLOCKED }
                .thenBy { workspaceOrder[it.workspaceId] ?: Int.MAX_VALUE }
                .thenBy { it.tabId ?: "" }
                .thenBy { it.paneId }
        )
    }

    fun needsYou(agents: List<AgentInfo>): List<AgentInfo> = agents.filter { it.agentStatus == AgentStatus.BLOCKED }

    fun groups(workspaces: List<WorkspaceInfo>, agents: List<AgentInfo>, panes: List<PaneInfo>): List<WorkspaceGroup> {
        val agentPaneIds = agents.map { it.paneId }.toSet()
        return workspaces.sortedBy { it.number }.map { ws ->
            WorkspaceGroup(
                workspace = ws,
                agents = orderedAgents(agents.filter { it.workspaceId == ws.workspaceId }, listOf(ws)),
                otherPanes = panes.filter { it.workspaceId == ws.workspaceId && it.paneId !in agentPaneIds }
            )
        }
    }
}
