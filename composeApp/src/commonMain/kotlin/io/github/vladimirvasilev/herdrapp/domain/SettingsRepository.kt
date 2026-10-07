package io.github.vladimirvasilev.herdrapp.domain

import kotlinx.coroutines.flow.StateFlow

/** What the home screen lists. */
enum class HomeView {
    /** Every pane, by workspace and tab. */
    WORKSPACES,
    /** Only the panes that run an agent, the ones waiting for the user first. */
    AGENTS,
}

interface SettingsRepository {
    val terminalFontSize: StateFlow<Float>
    suspend fun setTerminalFontSize(sizeSp: Float)

    val homeView: StateFlow<HomeView>
    suspend fun setHomeView(view: HomeView)
}
