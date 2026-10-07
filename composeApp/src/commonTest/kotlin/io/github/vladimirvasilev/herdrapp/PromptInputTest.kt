package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.ui.isEnterPress
import io.github.vladimirvasilev.herdrapp.ui.normalizePrompt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
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
}
