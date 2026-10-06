package io.github.vladimirvasilev.herdrapp.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.github.vladimirvasilev.herdrapp.protocol.TerminalFrame
import kotlinx.coroutines.flow.Flow

@Composable
expect fun TerminalPane(
    paneId: String,
    frames: Flow<TerminalFrame>,
    controlling: Boolean,
    fontSizeSp: Float,
    onInput: (ByteArray) -> Unit,
    onResize: (cols: Int, rows: Int) -> Unit,
    onScrollLines: (lines: Int) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
)
