package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.network.RequestOutcome
import io.github.vladimirvasilev.herdrapp.network.RequestTracker
import io.github.vladimirvasilev.herdrapp.protocol.BridgeError
import io.github.vladimirvasilev.herdrapp.protocol.BridgeResultMessage
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RequestTrackerTest {
    @Test fun correlatesResultById() = runTest {
        val tracker = RequestTracker()
        tracker.register("r1")
        val wait = async { tracker.await("r1", 1_000) }
        tracker.complete(BridgeResultMessage(id = "r1", ok = true))
        assertIs<RequestOutcome.Success>(wait.await())
    }

    @Test fun reportsBridgeErrors() = runTest {
        val tracker = RequestTracker()
        tracker.register("r2")
        val wait = async { tracker.await("r2", 1_000) }
        tracker.complete(BridgeResultMessage(id = "r2", ok = false, error = BridgeError("not_controlling", "no")))
        val failure = assertIs<RequestOutcome.Failure>(wait.await())
        assertEquals("not_controlling", failure.error.code)
    }
}
