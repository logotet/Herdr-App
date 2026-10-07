package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeConnection
import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeListener
import io.github.vladimirvasilev.herdrapp.data.bridge.RequestOutcome
import io.github.vladimirvasilev.herdrapp.data.bridge.SocketStatus
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.ServerMessage
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class BridgeConnectionTest {
    private class Recorder : BridgeListener {
        val statuses = mutableListOf<SocketStatus>()
        val messages = mutableListOf<ServerMessage>()
        override suspend fun onStatus(status: SocketStatus) {
            statuses += status
        }

        override suspend fun onMessage(message: ServerMessage) {
            messages += message
        }
    }

    private fun TestScope.connection(bridge: FakeBridgeSocketFactory, recorder: Recorder = Recorder()) =
        BridgeConnection(bridge, backgroundScope, StandardTestDispatcher(testScheduler)).apply { addListener(recorder) }

    @Test
    fun resultWithTheSameIdAnswersTheRequest() = runTest {
        val bridge = FakeBridgeSocketFactory()
        val connection = connection(bridge)
        connection.connect(TEST_HOST)
        runCurrent()

        val outcome = async { connection.refresh() }
        runCurrent()
        assertTrue(bridge.sent.single().contains(""""id":"m1""""))
        bridge.push("""{"type":"result","id":"m1","ok":true}""")

        assertIs<RequestOutcome.Success>(outcome.await())
    }

    @Test
    fun requestWithoutAConnectionFailsAndLeavesNothingPending() = runTest {
        val connection = connection(FakeBridgeSocketFactory())

        val failure = assertIs<RequestOutcome.Failure>(connection.refresh())

        assertEquals("disconnected", failure.error.code)
        assertEquals(0, connection.pendingRequests)
    }

    @Test
    fun pendingRequestFailsWhenTheConnectionDrops() = runTest {
        val bridge = FakeBridgeSocketFactory()
        val connection = connection(bridge)
        connection.connect(TEST_HOST)
        runCurrent()

        val outcome = async { connection.refresh() }
        runCurrent()
        bridge.closeFromServer()

        assertEquals("connection_lost", assertIs<RequestOutcome.Failure>(outcome.await()).error.code)
    }

    @Test
    fun cleanCloseBacksOffBeforeReconnecting() = runTest {
        val bridge = FakeBridgeSocketFactory()
        val recorder = Recorder()
        connection(bridge, recorder).connect(TEST_HOST)
        runCurrent()
        assertEquals(1, bridge.attempts)

        bridge.closeFromServer()
        runCurrent()
        assertEquals(SocketStatus.Lost("Connection closed"), recorder.statuses.last())
        advanceTimeBy(999)
        assertEquals(1, bridge.attempts)

        advanceTimeBy(2)
        assertEquals(2, bridge.attempts)
    }

    @Test
    fun backoffDoublesWhileTheBridgeRefuses() = runTest {
        val bridge = FakeBridgeSocketFactory().apply { refuse = true }
        connection(bridge).connect(TEST_HOST)
        runCurrent()
        assertEquals(1, bridge.attempts)

        advanceTimeBy(1_001)
        assertEquals(2, bridge.attempts)
        advanceTimeBy(1_999)
        assertEquals(2, bridge.attempts)
        advanceTimeBy(2)
        assertEquals(3, bridge.attempts)
    }

    @Test
    fun malformedMessageIsSkippedAndTheSocketStaysOpen() = runTest {
        val bridge = FakeBridgeSocketFactory()
        val recorder = Recorder()
        connection(bridge, recorder).connect(TEST_HOST)
        runCurrent()

        bridge.push("{not json")
        bridge.push("""{"type":"hello","protocol":1,"bridge_version":"0.1.0","name":"WORK-PC","herdr":{"available":true}}""")
        runCurrent()

        assertIs<ServerMessage.Hello>(recorder.messages.single())
        assertEquals(1, bridge.attempts)
    }

    @Test
    fun openStreamsAreReopenedAfterAReconnect() = runTest {
        val bridge = FakeBridgeSocketFactory()
        val connection = connection(bridge)
        connection.connect(TEST_HOST)
        runCurrent()
        val opened = async { connection.openStream("w1:p1", 94, 39) }
        runCurrent()
        bridge.push("""{"type":"result","id":"m1","ok":true}""")
        opened.await()
        bridge.sent.clear()

        bridge.closeFromServer()
        advanceTimeBy(1_001)

        val reopen = bridge.sent.single()
        assertTrue(reopen.contains(""""type":"open_stream"""") && reopen.contains(""""pane_id":"w1:p1""""))
    }
}
