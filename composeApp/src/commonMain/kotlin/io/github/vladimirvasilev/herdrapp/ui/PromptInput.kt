package io.github.vladimirvasilev.herdrapp.ui

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

/**
 * True when [new] is [old] with exactly one line feed typed into it, i.e. the user pressed Enter
 * on the keyboard. Pasted multi-line text changes more than one character and is kept as is.
 */
fun isEnterPress(old: String, new: String): Boolean {
    if (new.length != old.length + 1) return false
    var i = 0
    while (i < old.length && old[i] == new[i]) i++
    return new[i] == '\n' && new.regionMatches(i + 1, old, i, old.length - i)
}

/** Normalizes compose-box text before it is sent to an agent. */
fun normalizePrompt(text: String): String = text.replace("\r\n", "\n").replace('\r', '\n').trimEnd('\n')

data class PaneHistory(val text: String, val truncated: Boolean) {
    val lineCount: Int get() = if (text.isEmpty()) 0 else text.count { it == '\n' } + 1
}

/** Parses the bridge `call` result of herdr `pane.read` (`{"type":"pane_read","read":{...}}`). */
fun parsePaneRead(data: JsonElement?): PaneHistory? {
    val read = (data as? JsonObject)?.get("read") as? JsonObject ?: return null
    val text = (read["text"] as? JsonPrimitive)?.contentOrNull ?: return null
    val truncated = (read["truncated"] as? JsonPrimitive)?.booleanOrNull ?: false
    return PaneHistory(text, truncated)
}
