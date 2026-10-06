package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.ui.isEnterPress
import io.github.vladimirvasilev.herdrapp.ui.normalizePrompt
import io.github.vladimirvasilev.herdrapp.ui.parsePaneRead
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PromptInputTest {
    @Test
    fun enterAtEndOrMiddleIsDetected() {
        assertTrue(isEnterPress("fix it", "fix it\n"))
        assertTrue(isEnterPress("fix it", "fix\n it"))
        assertTrue(isEnterPress("", "\n"))
    }

    @Test
    fun typingAndPastingAreNotEnter() {
        assertFalse(isEnterPress("fix", "fix!"))
        assertFalse(isEnterPress("fix", "fix\nline two"))
        assertFalse(isEnterPress("a\nb", "a\nb"))
        assertFalse(isEnterPress("abc", "ab"))
    }

    @Test
    fun normalizeKeepsInnerNewlines() {
        assertEquals("one\ntwo", normalizePrompt("one\r\ntwo\n\n"))
        assertEquals("", normalizePrompt("\n"))
    }

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
