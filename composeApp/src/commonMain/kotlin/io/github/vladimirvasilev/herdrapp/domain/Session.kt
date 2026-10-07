package io.github.vladimirvasilev.herdrapp.domain

enum class AgentStatus { IDLE, WORKING, BLOCKED, DONE, UNKNOWN }

data class Workspace(
    val id: String,
    val number: Int,
    val label: String,
    val paneCount: Int,
)

/** A pane that runs a detected coding agent. */
data class Agent(
    val paneId: String,
    val workspaceId: String,
    val tabId: String?,
    /** The agent program, for example "claude"; null when herdr could not tell. */
    val kind: String?,
    val title: String,
    val status: AgentStatus,
    /** The last few lines of the pane as plain text, when the bridge sent them. */
    val preview: String?,
)

data class Pane(
    val paneId: String,
    val workspaceId: String,
    val title: String,
)

/** Everything herdr currently has open. [agents] are in display order. */
data class Session(
    val workspaces: List<Workspace> = emptyList(),
    val agents: List<Agent> = emptyList(),
    val panes: List<Pane> = emptyList(),
)
