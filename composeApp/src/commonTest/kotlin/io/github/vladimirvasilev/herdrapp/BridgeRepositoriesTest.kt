package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.data.BridgeSessionRepository
import io.github.vladimirvasilev.herdrapp.data.BridgeTerminalRepository
import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeConnection
import io.github.vladimirvasilev.herdrapp.domain.CommandResult
import io.github.vladimirvasilev.herdrapp.domain.ConnectionState
import io.github.vladimirvasilev.herdrapp.domain.GridSize
import io.github.vladimirvasilev.herdrapp.domain.StreamMode
import io.github.vladimirvasilev.herdrapp.domain.TerminalFrame
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class, ExperimentalEncodingApi::class)
class BridgeRepositoriesTest {
    private class Rig(scope: TestScope) {
        val bridge = FakeBridgeSocketFactory()
        private val dispatcher = StandardTestDispatcher(scope.testScheduler)
        private val connection = BridgeConnection(bridge, scope.backgroundScope, dispatcher)
        val session = BridgeSessionRepository(connection).also(connection::addListener)
        val terminal = BridgeTerminalRepository(connection, scope.backgroundScope, dispatcher).also(connection::addListener)
        val received = mutableListOf<String>()

        /** Connects, opens a stream for [PANE] and starts collecting its frames as text. */
        fun TestScope.openPane() {
            session.connect(TEST_HOST)
            backgroundScope.launch { terminal.frames(PANE).collect { received += it.text() } }
            backgroundScope.launch { terminal.open(PANE, 94, 39) }
            runCurrent()
            bridge.sent.clear()
        }

        fun frame(seq: Long, text: String, full: Boolean = false) = bridge.push(
            """{"type":"frame","pane_id":"$PANE","seq":$seq,"full":$full,"width":94,"height":39,"bytes":"${Base64.Default.encode(text.encodeToByteArray())}"}"""
        )

        /** Answers every request the app sends with success, until it stops sending. */
        fun TestScope.answerRequests(ok: Boolean = true) {
            var answered = 0
            while (true) {
                runCurrent()
                val pending = bridge.sent.drop(answered)
                if (pending.isEmpty()) return
                pending.forEach { text ->
                    val id = REQUEST_ID.find(text)?.groupValues?.get(1) ?: return@forEach
                    val error = if (ok) "" else ""","error":{"code":"stream_failed","message":"pane is controlled elsewhere"}"""
                    bridge.push("""{"type":"result","id":"$id","ok":$ok$error}""")
                }
                answered += pending.size
            }
        }

        fun sentTypes() = bridge.sent.map { REQUEST_TYPE.find(it)?.groupValues?.get(1) }

        private fun TerminalFrame.text() = bytes.decodeToString()
    }

    @Test
    fun helloMarksTheSessionConnectedAndReportsHerdr() = runTest {
        val rig = Rig(this)
        rig.session.connect(TEST_HOST)
        runCurrent()
        assertEquals(ConnectionState.Connecting, rig.session.connectionState.value)

        rig.bridge.push("""{"type":"hello","protocol":1,"bridge_version":"0.1.0","name":"WORK-PC","herdr":{"available":false}}""")
        runCurrent()

        assertEquals(ConnectionState.Connected("WORK-PC"), rig.session.connectionState.value)
        assertEquals(false, rig.session.herdrAvailable.value)
    }

    @Test
    fun snapshotReplacesTheSessionAndALostSocketIsAnError() = runTest {
        val rig = Rig(this)
        rig.session.connect(TEST_HOST)
        runCurrent()

        rig.bridge.push(readResource("snapshot_fixture.json"))
        runCurrent()
        assertEquals(2, rig.session.session.value.agents.size)

        rig.bridge.closeFromServer()
        runCurrent()
        assertEquals(ConnectionState.Error("Connection closed"), rig.session.connectionState.value)
    }

    @Test
    fun framesArriveDecodedAndInOrder() = runTest {
        val rig = Rig(this)
        with(rig) { openPane() }

        rig.frame(1, "screen", full = true)
        rig.frame(2, "+a")
        rig.frame(3, "+b")
        runCurrent()

        assertEquals(listOf("screen", "+a", "+b"), rig.received)
    }

    @Test
    fun framesSentBeforeAnyoneCollectsAreKept() = runTest {
        val rig = Rig(this)
        rig.session.connect(TEST_HOST)
        backgroundScope.launch { rig.terminal.open(PANE, 94, 39) }
        runCurrent()
        rig.frame(1, "screen", full = true)
        rig.frame(2, "+a")
        runCurrent()

        val received = mutableListOf<String>()
        backgroundScope.launch { rig.terminal.frames(PANE).collect { received += it.bytes.decodeToString() } }
        runCurrent()

        assertEquals(listOf("screen", "+a"), received)
    }

    @Test
    fun aGapRestartsTheStreamAndSkipsDeltasUntilTheNextFullFrame() = runTest {
        val rig = Rig(this)
        with(rig) { openPane() }
        rig.frame(1, "screen", full = true)
        rig.frame(2, "+a")

        rig.frame(4, "+lost-its-base")
        rig.frame(5, "+still-useless")
        runCurrent()
        assertTrue(rig.bridge.sent.single().contains(""""type":"open_stream""""))

        rig.frame(1, "repaint", full = true)
        rig.frame(2, "+c")
        runCurrent()
        assertEquals(listOf("screen", "+a", "repaint", "+c"), rig.received)
    }

    @Test
    fun aResizeRepaintInTheMiddleOfAStreamIsNotAGap() = runTest {
        val rig = Rig(this)
        with(rig) { openPane() }

        rig.frame(1, "screen", full = true)
        rig.frame(2, "resized", full = true)
        rig.frame(3, "+a")
        runCurrent()

        assertEquals(listOf("screen", "resized", "+a"), rig.received)
        assertTrue(rig.bridge.sent.isEmpty())
    }

    @Test
    fun streamMessagesSetTheModeAndALostSocketClearsIt() = runTest {
        val rig = Rig(this)
        with(rig) { openPane() }

        rig.bridge.push("""{"type":"stream","pane_id":"$PANE","mode":"control"}""")
        runCurrent()
        assertEquals(mapOf(PANE to StreamMode.CONTROL), rig.terminal.streamModes.value)

        rig.bridge.closeFromServer()
        runCurrent()
        assertEquals(emptyMap(), rig.terminal.streamModes.value)
    }

    @Test
    fun openStreamsAreReopenedAfterAReconnect() = runTest {
        val rig = Rig(this)
        with(rig) { openPane() }

        rig.bridge.closeFromServer()
        advanceTimeBy(1_001)

        val reopen = rig.bridge.sent.single()
        assertTrue(reopen.contains(""""type":"open_stream"""") && reopen.contains(""""pane_id":"$PANE""""))
    }

    @Test
    fun closeStopsTheStreamAndIgnoresLaterFrames() = runTest {
        val rig = Rig(this)
        with(rig) { openPane() }

        rig.terminal.close(PANE)
        runCurrent()
        assertTrue(rig.bridge.sent.single().contains(""""type":"close_stream""""))

        rig.frame(1, "late", full = true)
        runCurrent()
        assertTrue(rig.received.isEmpty())
    }

    @Test
    fun scrollToLatestTakesControlAtThePcSizeScrollsAndReleases() = runTest {
        val rig = Rig(this)
        with(rig) { openPane() }

        val result = async { rig.terminal.scrollToLatest(PANE, GridSize(144, 39), lines = 1_500) }
        with(rig) { answerRequests() }

        assertEquals(CommandResult.Success, result.await())
        assertEquals(listOf("take_control", "scroll", "scroll", "release_control"), rig.sentTypes())
        val takeControl = rig.bridge.sent[0]
        assertTrue(takeControl.contains(""""cols":144,"rows":39""") && !takeControl.contains("takeover"))
        assertTrue(rig.bridge.sent[1].contains(""""direction":"down","lines":1000"""))
        assertTrue(rig.bridge.sent[2].contains(""""lines":500"""))
    }

    @Test
    fun scrollToLatestGivesUpWhenControlIsRefused() = runTest {
        val rig = Rig(this)
        with(rig) { openPane() }

        val result = async { rig.terminal.scrollToLatest(PANE, GridSize(144, 39), lines = 10) }
        with(rig) { answerRequests(ok = false) }

        assertEquals(CommandResult.Failure("pane is controlled elsewhere"), result.await())
        assertEquals(listOf("take_control"), rig.sentTypes())
    }

    private companion object {
        const val PANE = "w1:p1"
        val REQUEST_ID = Regex(""""id":"(m\d+)"""")
        val REQUEST_TYPE = Regex(""""type":"(\w+)"""")
    }
}
