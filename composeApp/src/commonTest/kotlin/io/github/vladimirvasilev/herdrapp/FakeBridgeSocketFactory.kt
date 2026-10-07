package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeSocket
import io.github.vladimirvasilev.herdrapp.data.bridge.BridgeSocketFactory
import io.github.vladimirvasilev.herdrapp.domain.SavedHost
import kotlinx.coroutines.channels.Channel

/** A bridge the test drives by hand: push messages to the app, read what the app sent. */
class FakeBridgeSocketFactory : BridgeSocketFactory {
    val sent = mutableListOf<String>()
    var attempts = 0
        private set
    var refuse = false
    private var incoming = Channel<String>(Channel.UNLIMITED)

    override suspend fun connect(host: SavedHost, block: suspend (BridgeSocket) -> Unit) {
        attempts++
        if (refuse) throw IllegalStateException("refused")
        val channel = Channel<String>(Channel.UNLIMITED)
        incoming = channel
        block(object : BridgeSocket {
            override suspend fun receive(): String? = channel.receiveCatching().getOrNull()
            override suspend fun send(text: String) {
                sent += text
            }
        })
    }

    fun push(text: String) {
        incoming.trySend(text)
    }

    fun closeFromServer() {
        incoming.close()
    }
}

val TEST_HOST = SavedHost(id = "h1", name = "Desk", host = "pc.local", port = 8787, token = "secret")
