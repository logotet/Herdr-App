package io.github.vladimirvasilev.herdrapp.network

import io.github.vladimirvasilev.herdrapp.protocol.BridgeError
import io.github.vladimirvasilev.herdrapp.protocol.BridgeResultMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.JsonElement

sealed interface RequestOutcome {
    data class Success(val data: JsonElement?) : RequestOutcome
    data class Failure(val error: BridgeError) : RequestOutcome
}

class RequestTracker {
    private val pending = mutableMapOf<String, CompletableDeferred<RequestOutcome>>()

    fun register(id: String): CompletableDeferred<RequestOutcome> = synchronized(pending) {
        CompletableDeferred<RequestOutcome>().also { pending[id] = it }
    }

    suspend fun await(id: String, timeoutMillis: Long): RequestOutcome {
        val deferred = synchronized(pending) { pending[id] } ?: return RequestOutcome.Failure(BridgeError("missing_request", "No pending request"))
        return try {
            withTimeout(timeoutMillis) { deferred.await() }
        } catch (_: TimeoutCancellationException) {
            RequestOutcome.Failure(BridgeError("timeout", "Request timed out"))
        } finally {
            synchronized(pending) { pending.remove(id) }
        }
    }

    fun complete(result: BridgeResultMessage) {
        val deferred = synchronized(pending) { pending[result.id] } ?: return
        if (result.ok) deferred.complete(RequestOutcome.Success(result.data))
        else deferred.complete(RequestOutcome.Failure(result.error ?: BridgeError("unknown", null)))
    }

    fun failAll(error: BridgeError) {
        val all = synchronized(pending) { pending.values.toList().also { pending.clear() } }
        all.forEach { it.complete(RequestOutcome.Failure(error)) }
    }
}
