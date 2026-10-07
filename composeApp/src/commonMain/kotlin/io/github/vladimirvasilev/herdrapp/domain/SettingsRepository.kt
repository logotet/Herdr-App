package io.github.vladimirvasilev.herdrapp.domain

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val terminalFontSize: StateFlow<Float>
    suspend fun setTerminalFontSize(sizeSp: Float)
}
