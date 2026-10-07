package io.github.vladimirvasilev.herdrapp.ui.terminal

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

internal sealed interface KeySpec {
    data class Bytes(val bytes: ByteArray, val textFallback: String) : KeySpec
    data class HerdrKey(val key: String) : KeySpec
}

private val EXTRA_KEYS = listOf(
    "Esc", "Tab", "Ctrl", "Alt", "Up", "Down", "Left", "Right", "Enter", "Ctrl+C", "PgUp", "PgDn",
    "Home", "End", "/", "|", "-", "1", "2", "y", "n", "Shift+Tab",
)

@Composable
internal fun ExtraKeysBar(onKey: (KeySpec) -> Unit) {
    var ctrl by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .background(Color(0xFF11111B))
            .horizontalScroll(rememberScrollState())
            .padding(6.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        EXTRA_KEYS.forEach { label ->
            AssistChip(
                onClick = {
                    if (label == "Ctrl") {
                        ctrl = !ctrl
                        return@AssistChip
                    }
                    onKey(mapKey(label, ctrl))
                    ctrl = false
                },
                label = { Text(if (label == "Ctrl" && ctrl) "CTRL*" else label) },
            )
        }
    }
}

private fun mapKey(label: String, ctrl: Boolean): KeySpec {
    if (ctrl && label.length == 1) return KeySpec.HerdrKey("ctrl+${label.lowercase()}")
    return when (label) {
        "Esc" -> KeySpec.HerdrKey("esc")
        "Tab" -> KeySpec.HerdrKey("tab")
        "Alt" -> KeySpec.HerdrKey("alt+x")
        "Up" -> KeySpec.HerdrKey("up")
        "Down" -> KeySpec.HerdrKey("down")
        "Left" -> KeySpec.HerdrKey("left")
        "Right" -> KeySpec.HerdrKey("right")
        "Enter" -> KeySpec.HerdrKey("enter")
        "Ctrl+C" -> KeySpec.HerdrKey("ctrl+c")
        "PgUp" -> KeySpec.HerdrKey("pageup")
        "PgDn" -> KeySpec.HerdrKey("pagedown")
        "Home" -> KeySpec.HerdrKey("home")
        "End" -> KeySpec.HerdrKey("end")
        "Shift+Tab" -> KeySpec.HerdrKey("shift+tab")
        else -> KeySpec.Bytes(label.encodeToByteArray(), label)
    }
}
