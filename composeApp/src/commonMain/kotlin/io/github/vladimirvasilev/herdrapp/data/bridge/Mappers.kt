package io.github.vladimirvasilev.herdrapp.data.bridge

import io.github.vladimirvasilev.herdrapp.data.bridge.dto.AgentInfo
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.BridgeSnapshot
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.PaneInfo
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.WorkspaceInfo
import io.github.vladimirvasilev.herdrapp.domain.Agent
import io.github.vladimirvasilev.herdrapp.domain.AgentOrganizer
import io.github.vladimirvasilev.herdrapp.domain.AgentStatus
import io.github.vladimirvasilev.herdrapp.domain.Pane
import io.github.vladimirvasilev.herdrapp.domain.PaneHistory
import io.github.vladimirvasilev.herdrapp.domain.Session
import io.github.vladimirvasilev.herdrapp.domain.Workspace
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.AgentStatus as AgentStatusDto

internal fun BridgeSnapshot.toSession(): Session {
    val mapped = agents.map { it.toAgent(previews[it.paneId]) }
    return Session(
        workspaces = workspaces.map { it.toWorkspace() }.sortedBy { it.number },
        agents = AgentOrganizer.orderedAgents(mapped, workspaces.map { it.workspaceId }),
        panes = panes.map { it.toPane() },
    )
}

private fun WorkspaceInfo.toWorkspace() = Workspace(id = workspaceId, number = number, label = label, paneCount = paneCount)

private fun AgentInfo.toAgent(preview: String?) = Agent(
    paneId = paneId,
    workspaceId = workspaceId,
    tabId = tabId,
    kind = agent,
    title = terminalTitleStripped?.takeIf { it.isNotBlank() } ?: label?.takeIf { it.isNotBlank() } ?: agent ?: paneId,
    status = agentStatus.toDomain(),
    preview = preview,
)

private fun PaneInfo.toPane() = Pane(
    paneId = paneId,
    workspaceId = workspaceId,
    title = terminalTitleStripped ?: label ?: paneId,
)

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
