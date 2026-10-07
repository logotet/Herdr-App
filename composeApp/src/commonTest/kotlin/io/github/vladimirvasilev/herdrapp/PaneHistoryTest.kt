package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.data.bridge.parsePaneRead
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PaneHistoryTest {
    @Test
    fun parsesPaneReadResult() {
        val data = Json.parseToJsonElement("""{"type":"pane_read","read":{"pane_id":"w1:p1","text":"a\r\nb","truncated":true}}""")
        val history = assertNotNull(parsePaneRead(data))
        assertEquals("a\r\nb", history.text)
        assertTrue(history.truncated)
        assertEquals(2, history.lineCount)
        assertNull(parsePaneRead(Json.parseToJsonElement("""{"type":"ok"}""")))
    }
}
