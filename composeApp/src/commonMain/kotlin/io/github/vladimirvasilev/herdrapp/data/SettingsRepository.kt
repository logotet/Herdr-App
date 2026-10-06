package io.github.vladimirvasilev.herdrapp.data

import kotlinx.coroutines.flow.StateFlow

interface SettingsRepository {
    val terminalFontSize: StateFlow<Float>
    suspend fun setTerminalFontSize(sizeSp: Float)
}
