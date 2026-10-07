package io.github.vladimirvasilev.herdrapp.domain

enum class AgentStatus { IDLE, WORKING, BLOCKED, DONE, UNKNOWN }

data class Workspace(
    val id: String,
    val number: Int,
    val label: String,
    val paneCount: Int,
)

/** The coding agent herdr detected in a pane. */
data class AgentState(
    /** The agent program, for example "claude"; null when herdr could not tell. */
    val kind: String?,
    val status: AgentStatus,
    /** The last few lines of the pane as plain text, when the bridge sent them. */
    val preview: String?,
)

data class Pane(
    val paneId: String,
    val workspaceId: String,
    val tabId: String?,
    val title: String,
    /** Null for a pane that runs no detected coding agent, for example a shell or an editor. */
    val agent: AgentState? = null,
    /**
     * How far the pane is scrolled back on the PC. While this is above zero the stream shows old
     * output and nothing new, because herdr only streams the part of a pane that is visible.
     */
    val scrolledBackLines: Int = 0,
    /** The pane's size on the PC, when herdr knows it (it may not for panes in hidden tabs). */
    val pcGrid: GridSize? = null,
)

data class GridSize(val cols: Int, val rows: Int)

/**
 * Everything herdr currently has open. [panes] holds every pane; [agents] are the ones that run
 * an agent, in display order.
 */
data class Session(
    val workspaces: List<Workspace> = emptyList(),
    val agents: List<Pane> = emptyList(),
    val panes: List<Pane> = emptyList(),
)
