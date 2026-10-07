package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.data.PairUriParser
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PairUriParserTest {
    @Test
    fun parsesPairUriWithDefaultsAndEscaping() {
        val host = PairUriParser.parse("herdr-bridge://pair?host=100.64.0.2&token=secret&name=Work+PC")
        assertNotNull(host)
        assertEquals("Work PC", host.name)
        assertEquals(8787, host.port)
        val withPort = PairUriParser.parse("herdr-bridge://pair?host=pc.local&port=9000&token=abc%20123&name=Desk")
        assertEquals(9000, withPort?.port)
        assertEquals("abc 123", withPort?.token)
        assertNull(PairUriParser.parse("https://example.com"))
    }
}
