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
    /**
     * How far the pane is scrolled back on the PC. While this is above zero the stream shows old
     * output and nothing new, because herdr only streams the part of a pane that is visible.
     */
    val scrolledBackLines: Int = 0,
    /** The pane's size on the PC, when herdr knows it (it may not for panes in hidden tabs). */
    val pcGrid: GridSize? = null,
)

data class GridSize(val cols: Int, val rows: Int)

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
