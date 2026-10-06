package io.github.vladimirvasilev.herdrapp.ui

import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.view.RemoteTerminalSession
import com.termux.view.TerminalView
import io.github.vladimirvasilev.herdrapp.protocol.TerminalFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt

@OptIn(ExperimentalEncodingApi::class)
@Composable
actual fun TerminalPane(
    paneId: String,
    frames: Flow<TerminalFrame>,
    controlling: Boolean,
    fontSizeSp: Float,
    onInput: (ByteArray) -> Unit,
    onResize: (cols: Int, rows: Int) -> Unit,
    onScrollLines: (lines: Int) -> Unit,
    onFontSizeChanged: (Float) -> Unit,
    modifier: Modifier,
) {
    val session = remember(paneId) { RemoteTerminalSession(80, 24) { bytes -> onInput(bytes) } }
    var lastSize by remember(paneId) { mutableStateOf(80 to 24) }
    LaunchedEffect(paneId, frames) {
        frames.filter { it.paneId == paneId }.collect { frame ->
            if (frame.full) session.emulator.clear()
            if (frame.width != session.emulator.columns || frame.height != session.emulator.rows) session.resize(frame.width, frame.height)
            session.appendRemote(Base64.Default.decode(frame.bytes))
        }
    }
    AndroidView(
        modifier = modifier
            .pointerInput(controlling) { detectVerticalDragGestures { change, dragAmount -> change.consume(); if (kotlin.math.abs(dragAmount) > 14f) onScrollLines(if (dragAmount > 0) 3 else -3) } }
            .pointerInput(fontSizeSp) { detectTransformGestures { _, _, zoom, _ -> if (zoom > 1.03f || zoom < 0.97f) onFontSizeChanged((fontSizeSp * zoom).coerceIn(8f, 28f)) } },
        factory = { context ->
            TerminalView(context).apply {
                setRemoteSession(session)
                setInputEnabled(controlling)
                setFontSize(fontSizeSp)
            }
        },
        update = { view ->
            view.setRemoteSession(session)
            view.setInputEnabled(controlling)
            view.setFontSize(fontSizeSp)
            view.post {
                val next = view.estimateColumns() to view.estimateRows()
                if (next != lastSize) {
                    lastSize = next
                    session.resize(next.first, next.second)
                    onResize(next.first, next.second)
                }
            }
        }
    )
}
