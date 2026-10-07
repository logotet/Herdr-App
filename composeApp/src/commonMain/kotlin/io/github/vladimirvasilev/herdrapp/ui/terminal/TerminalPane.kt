package io.github.vladimirvasilev.herdrapp.ui.terminal

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.TerminalFrame
import kotlinx.coroutines.flow.Flow

@Composable
expect fun TerminalPane(
    paneId: String,
    frames: Flow<TerminalFrame>,
    controlling: Boolean,
    fontSizeSp: Float,
    history: String?,
    onInput: (ByteArray) -> Unit,
    onResize: (cols: Int, rows: Int) -> Unit,
    onScrollBack: () -> Unit,
    onExitHistory: () -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
)
