package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.domain.PairUriParser
import io.github.vladimirvasilev.herdrapp.domain.withHost
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

    @Test
    fun decodesEscapedUtf8() {
        // "ПК 🙂": two-byte Cyrillic letters and a four-byte emoji.
        val host = PairUriParser.parse("herdr-bridge://pair?host=pc.local&token=t&name=%D0%9F%D0%9A+%F0%9F%99%82")

        assertEquals("ПК 🙂", host?.name)
    }

    @Test
    fun keepsAPercentSignThatIsNotAnEscape() {
        assertEquals("100%", PairUriParser.parse("herdr-bridge://pair?host=h&token=t&name=100%")?.name)
        assertEquals("a%zzb", PairUriParser.parse("herdr-bridge://pair?host=h&token=t&name=a%zzb")?.name)
    }

    @Test
    fun theSameBridgeAlwaysGetsTheSameId() {
        val uri = "herdr-bridge://pair?host=pc.local&port=9000&token=abc&name=Desk"

        assertEquals(PairUriParser.parse(uri)?.id, PairUriParser.parse(uri)?.id)
        assertEquals("pc.local:9000", PairUriParser.parse(uri)?.id)
    }

    @Test
    fun pairingABridgeAgainReplacesItsEntry() {
        val first = assertNotNull(PairUriParser.parse("herdr-bridge://pair?host=pc.local&token=old&name=Desk"))
        val again = assertNotNull(PairUriParser.parse("herdr-bridge://pair?host=pc.local&token=new&name=Desk"))
        val other = TEST_HOST.copy(id = "legacy-id", name = "Attic", host = "attic.local")

        val hosts = listOf(first, other).withHost(again)

        assertEquals(listOf("Attic", "Desk"), hosts.map { it.name })
        assertEquals("new", hosts.last().token)
    }

    @Test
    fun anEntrySavedUnderAnOlderIdSchemeIsReplacedByAddress() {
        val legacy = TEST_HOST.copy(id = "manual-12345-8787")
        val paired = TEST_HOST.copy(id = "pc.local:8787", token = "new")

        assertEquals(listOf(paired), listOf(legacy).withHost(paired))
    }
}
