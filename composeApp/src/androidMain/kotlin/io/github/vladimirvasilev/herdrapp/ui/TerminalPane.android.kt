package io.github.vladimirvasilev.herdrapp.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.InputMethodManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalEmulator
import com.termux.terminal.TerminalSession
import com.termux.terminal.TerminalSessionClient
import com.termux.view.TerminalView
import com.termux.view.TerminalViewClient
import io.github.vladimirvasilev.herdrapp.protocol.TerminalFrame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.roundToInt

private const val TAG = "HerdrTerminal"
private const val TRANSCRIPT_ROWS = 2000
private const val MIN_FONT_SP = 8f
private const val MAX_FONT_SP = 28f

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
    val currentOnInput = rememberUpdatedState(onInput)
    val currentOnResize = rememberUpdatedState(onResize)
    val currentOnScroll = rememberUpdatedState(onScrollLines)
    val currentOnFont = rememberUpdatedState(onFontSizeChanged)
    val currentFont = rememberUpdatedState(fontSizeSp)
    val currentControlling = rememberUpdatedState(controlling)

    val bridge = remember(paneId) {
        RemoteTerminalBridge(currentOnInput, currentOnResize, currentOnFont, currentFont, currentControlling)
    }

    LaunchedEffect(paneId, frames) {
        frames.filter { it.paneId == paneId }.collect { frame ->
            bridge.session.appendRemote(Base64.Default.decode(frame.bytes), frame.width, frame.height)
        }
    }

    // A pager page can be reused for another pane; give each pane its own view and session.
    key(paneId) { AndroidView(
        modifier = modifier,
        factory = { context ->
            TerminalView(context, null).apply {
                isFocusable = true
                isFocusableInTouchMode = true
                keepScreenOn = true
                bridge.view = this
                setTerminalViewClient(bridge)
                bridge.applyFontSize(this, fontSizeSp)
                attachSession(bridge.session)
                setRemoteScrollListener { rowsDown -> currentOnScroll.value(-rowsDown) }
            }
        },
        update = { view ->
            bridge.view = view
            bridge.applyFontSize(view, fontSizeSp)
            bridge.setControlling(view, controlling)
        },
        onRelease = { view ->
            bridge.hideKeyboard(view)
            if (bridge.view === view) bridge.view = null
        },
    ) }
}

/**
 * Glue between the vendored Termux view/emulator and the herdr-bridge stream: implements both
 * client interfaces, forwards keystrokes/resizes to the bridge and owns the remote session.
 */
private class RemoteTerminalBridge(
    private val onInput: State<(ByteArray) -> Unit>,
    private val onResize: State<(Int, Int) -> Unit>,
    private val onFontSizeChanged: State<(Float) -> Unit>,
    private val fontSizeSp: State<Float>,
    private val controlling: State<Boolean>,
) : TerminalSessionClient, TerminalViewClient {

    var view: TerminalView? = null
    private var appliedFontSp = -1f
    private var lastSentSize: Pair<Int, Int>? = null
    private var inputEnabled = false

    val session = TerminalSession(TRANSCRIPT_ROWS, this, object : TerminalSession.RemoteIO {
        override fun onWrite(data: ByteArray) = onInput.value(data)

        override fun onResize(cols: Int, rows: Int) {
            val size = cols to rows
            if (size == lastSentSize) return
            lastSentSize = size
            onResize.value(cols, rows)
        }
    })

    fun applyFontSize(view: TerminalView, sp: Float) {
        if (sp == appliedFontSp) return
        appliedFontSp = sp
        val px = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, view.resources.displayMetrics)
        view.setTextSize(px.roundToInt().coerceAtLeast(1))
    }

    fun setControlling(view: TerminalView, enabled: Boolean) {
        if (enabled == inputEnabled) return
        inputEnabled = enabled
        session.setInputEnabled(enabled)
        if (!enabled) hideKeyboard(view)
        imm(view.context)?.restartInput(view)
    }

    fun hideKeyboard(view: TerminalView) {
        imm(view.context)?.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun showKeyboard(view: TerminalView) {
        view.requestFocus()
        imm(view.context)?.showSoftInput(view, InputMethodManager.SHOW_IMPLICIT)
    }

    private fun imm(context: Context) = context.getSystemService(InputMethodManager::class.java)

    // --- TerminalSessionClient ---

    override fun onTextChanged(changedSession: TerminalSession) {
        view?.onScreenUpdated()
    }

    override fun onTitleChanged(changedSession: TerminalSession) = Unit
    override fun onSessionFinished(finishedSession: TerminalSession) = Unit

    override fun onCopyTextToClipboard(session: TerminalSession, text: String?) {
        val ctx = view?.context ?: return
        if (text.isNullOrEmpty()) return
        ctx.getSystemService(ClipboardManager::class.java)
            ?.setPrimaryClip(ClipData.newPlainText("herdr", text))
    }

    override fun onPasteTextFromClipboard(session: TerminalSession?) {
        val ctx = view?.context ?: return
        if (!controlling.value) return
        val clip = ctx.getSystemService(ClipboardManager::class.java)?.primaryClip ?: return
        if (clip.itemCount == 0) return
        val text = clip.getItemAt(0).coerceToText(ctx)?.toString().orEmpty()
        if (text.isNotEmpty()) this.session.emulator?.paste(text)
    }

    override fun onBell(session: TerminalSession) = Unit
    override fun onColorsChanged(session: TerminalSession) {
        view?.invalidate()
    }

    override fun onTerminalCursorStateChange(state: Boolean) = Unit
    override fun setTerminalShellPid(session: TerminalSession, pid: Int) = Unit
    override fun getTerminalCursorStyle(): Int = TerminalEmulator.DEFAULT_TERMINAL_CURSOR_STYLE

    // --- TerminalViewClient ---

    override fun onScale(scale: Float): Float {
        if (scale < 0.9f || scale > 1.1f) {
            val next = (fontSizeSp.value * scale).coerceIn(MIN_FONT_SP, MAX_FONT_SP)
            if (next != fontSizeSp.value) onFontSizeChanged.value(next)
            return 1f
        }
        return scale
    }

    override fun onSingleTapUp(e: MotionEvent) {
        val v = view ?: return
        if (controlling.value) showKeyboard(v)
    }

    override fun shouldBackButtonBeMappedToEscape() = false
    override fun shouldEnforceCharBasedInput() = true
    override fun shouldUseCtrlSpaceWorkaround() = false
    override fun isTerminalViewSelected() = true
    override fun copyModeChanged(copyMode: Boolean) = Unit
    override fun onKeyDown(keyCode: Int, e: KeyEvent, session: TerminalSession) = false
    override fun onKeyUp(keyCode: Int, e: KeyEvent) = false
    override fun onLongPress(event: MotionEvent) = false
    override fun readControlKey() = false
    override fun readAltKey() = false
    override fun readShiftKey() = false
    override fun readFnKey() = false
    override fun onCodePoint(codePoint: Int, ctrlDown: Boolean, session: TerminalSession) = false
    override fun onEmulatorSet() {
        view?.invalidate()
    }

    // --- Logging (shared by both interfaces) ---

    override fun logError(tag: String?, message: String?) { Log.e(tag ?: TAG, message.orEmpty()) }
    override fun logWarn(tag: String?, message: String?) { Log.w(tag ?: TAG, message.orEmpty()) }
    override fun logInfo(tag: String?, message: String?) { Log.i(tag ?: TAG, message.orEmpty()) }
    override fun logDebug(tag: String?, message: String?) { Log.d(tag ?: TAG, message.orEmpty()) }
    override fun logVerbose(tag: String?, message: String?) { Log.v(tag ?: TAG, message.orEmpty()) }
    override fun logStackTraceWithMessage(tag: String?, message: String?, e: Exception?) {
        Log.e(tag ?: TAG, message.orEmpty(), e)
    }
    override fun logStackTrace(tag: String?, e: Exception?) { Log.e(tag ?: TAG, "error", e) }
}
