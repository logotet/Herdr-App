package io.github.vladimirvasilev.herdrapp.data.bridge

import io.github.vladimirvasilev.herdrapp.data.bridge.dto.AgentInfo
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.BridgeSnapshot
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.PaneInfo
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.WorkspaceInfo
import io.github.vladimirvasilev.herdrapp.domain.AgentOrganizer
import io.github.vladimirvasilev.herdrapp.domain.AgentState
import io.github.vladimirvasilev.herdrapp.domain.AgentStatus
import io.github.vladimirvasilev.herdrapp.domain.GridSize
import io.github.vladimirvasilev.herdrapp.domain.Pane
import io.github.vladimirvasilev.herdrapp.domain.PaneHistory
import io.github.vladimirvasilev.herdrapp.domain.Session
import io.github.vladimirvasilev.herdrapp.domain.Tab
import io.github.vladimirvasilev.herdrapp.domain.Workspace
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.AgentStatus as AgentStatusDto

internal fun BridgeSnapshot.toSession(): Session {
    // herdr reports the scroll position on the pane entry only, not on the agent entry.
    val scrollByPane = panes.associate { it.paneId to (it.scroll?.offsetFromBottom ?: 0) }
    val agentPanes = agents.map { it.toPane(previews[it.paneId], scrollByPane[it.paneId] ?: 0, paneSizes[it.paneId]) }
    val agentByPane = agentPanes.associateBy { it.paneId }
    return Session(
        workspaces = workspaces.map { it.toWorkspace() }.sortedBy { it.number },
        tabs = tabs.map { Tab(id = it.tabId, workspaceId = it.workspaceId, number = it.number, label = it.label) },
        agents = AgentOrganizer.orderedAgents(agentPanes, workspaces.map { it.workspaceId }),
        panes = panes.map { agentByPane[it.paneId] ?: it.toPane(paneSizes[it.paneId]) },
    )
}

private fun WorkspaceInfo.toWorkspace() = Workspace(id = workspaceId, number = number, label = label, paneCount = paneCount)

private fun AgentInfo.toPane(preview: String?, scrolledBack: Int, pcSize: List<Int>?) = Pane(
    paneId = paneId,
    workspaceId = workspaceId,
    tabId = tabId,
    title = terminalTitleStripped?.takeIf { it.isNotBlank() } ?: label?.takeIf { it.isNotBlank() } ?: agent ?: paneId,
    agent = AgentState(kind = agent, status = agentStatus.toDomain(), preview = preview),
    scrolledBackLines = scrolledBack.coerceAtLeast(0),
    pcGrid = pcSize.toGridSize(),
)

private fun PaneInfo.toPane(pcSize: List<Int>?) = Pane(
    paneId = paneId,
    workspaceId = workspaceId,
    tabId = tabId,
    title = terminalTitleStripped ?: label ?: paneId,
    scrolledBackLines = (scroll?.offsetFromBottom ?: 0).coerceAtLeast(0),
    pcGrid = pcSize.toGridSize(),
)

private fun List<Int>?.toGridSize(): GridSize? =
    this?.takeIf { it.size == 2 && it[0] > 0 && it[1] > 0 }?.let { GridSize(cols = it[0], rows = it[1]) }

private fun AgentStatusDto.toDomain() = when (this) {
    AgentStatusDto.IDLE -> AgentStatus.IDLE
    AgentStatusDto.WORKING -> AgentStatus.WORKING
    AgentStatusDto.BLOCKED -> AgentStatus.BLOCKED
    AgentStatusDto.DONE -> AgentStatus.DONE
    AgentStatusDto.UNKNOWN -> AgentStatus.UNKNOWN
}

/** Parses the bridge `call` result of herdr `pane.read` (`{"type":"pane_read","read":{...}}`). */
internal fun parsePaneRead(data: JsonElement?): PaneHistory? {
    val read = (data as? JsonObject)?.get("read") as? JsonObject ?: return null
    val text = (read["text"] as? JsonPrimitive)?.contentOrNull ?: return null
    val truncated = (read["truncated"] as? JsonPrimitive)?.booleanOrNull ?: false
    return PaneHistory(text, truncated)
}
