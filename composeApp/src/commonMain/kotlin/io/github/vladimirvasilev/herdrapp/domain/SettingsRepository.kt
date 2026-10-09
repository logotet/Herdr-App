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

    /** Stay connected while the app is off the screen and notify about agents. Off by default. */
    val backgroundAlerts: StateFlow<Boolean>
    suspend fun setBackgroundAlerts(enabled: Boolean)
}
