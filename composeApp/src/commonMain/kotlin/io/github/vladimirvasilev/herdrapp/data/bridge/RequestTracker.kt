package io.github.vladimirvasilev.herdrapp.data.bridge

import io.github.vladimirvasilev.herdrapp.data.bridge.dto.BridgeError
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.BridgeResultMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement

sealed interface RequestOutcome {
    data class Success(val data: JsonElement?) : RequestOutcome
    data class Failure(val error: BridgeError) : RequestOutcome
}

/** Matches results to requests by id. Not thread-safe: the owner confines it to one dispatcher. */
class RequestTracker {
    private val pending = mutableMapOf<String, CompletableDeferred<RequestOutcome>>()

    val pendingCount: Int get() = pending.size

    fun register(id: String) {
        pending[id] = CompletableDeferred()
    }

    fun discard(id: String) {
        pending.remove(id)
    }

    suspend fun await(id: String, timeoutMillis: Long): RequestOutcome {
        val deferred = pending[id] ?: return RequestOutcome.Failure(BridgeError("missing_request", "No pending request"))
        return try {
            withTimeout(timeoutMillis) { deferred.await() }
        } catch (_: TimeoutCancellationException) {
            RequestOutcome.Failure(BridgeError("timeout", "Request timed out"))
        } finally {
            pending.remove(id)
        }
    }

    fun complete(result: BridgeResultMessage) {
        val deferred = pending[result.id] ?: return
        if (result.ok) {
            deferred.complete(RequestOutcome.Success(result.data))
        } else {
            deferred.complete(RequestOutcome.Failure(result.error ?: BridgeError("unknown", null)))
        }
    }

    fun failAll(error: BridgeError) {
        val all = pending.values.toList()
        pending.clear()
        all.forEach { it.complete(RequestOutcome.Failure(error)) }
    }
}
