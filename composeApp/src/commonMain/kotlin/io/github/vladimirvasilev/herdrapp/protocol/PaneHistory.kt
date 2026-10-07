package io.github.vladimirvasilev.herdrapp.protocol

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

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
