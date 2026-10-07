package io.github.vladimirvasilev.herdrapp.domain

enum class StreamMode { OBSERVE, CONTROL }

/** VT bytes for one pane, to be fed to an emulator sized [cols] x [rows]. */
class TerminalFrame(val bytes: ByteArray, val cols: Int, val rows: Int)

data class PaneHistory(val text: String, val truncated: Boolean) {
    val lineCount: Int get() = if (text.isEmpty()) 0 else text.count { it == '\n' } + 1
}

sealed interface CommandResult {
    data object Success : CommandResult
    data class Failure(val message: String) : CommandResult
}

sealed interface HistoryResult {
    data class Loaded(val history: PaneHistory) : HistoryResult
    /** The bridge answered, but with nothing that can be shown. */
    data object Empty : HistoryResult
    data class Failed(val message: String) : HistoryResult
}
