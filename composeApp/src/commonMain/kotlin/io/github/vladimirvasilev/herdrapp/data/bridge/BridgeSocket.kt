package io.github.vladimirvasilev.herdrapp.data.bridge

import io.github.vladimirvasilev.herdrapp.domain.SavedHost
import io.ktor.client.HttpClient
import io.ktor.client.plugins.websocket.webSocket
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.websocket.Frame
import io.ktor.websocket.readText

/** One open WebSocket to a bridge, reduced to the text messages the protocol uses. */
interface BridgeSocket {
    /** The next text message, or null once the server has closed the socket cleanly. */
    suspend fun receive(): String?
    suspend fun send(text: String)
}

fun interface BridgeSocketFactory {
    /** Opens a socket to [host], runs [block] with it and returns when the socket is closed. */
    suspend fun connect(host: SavedHost, block: suspend (BridgeSocket) -> Unit)
}

class KtorBridgeSocketFactory(private val client: HttpClient) : BridgeSocketFactory {
    override suspend fun connect(host: SavedHost, block: suspend (BridgeSocket) -> Unit) {
        client.webSocket(
            urlString = "ws://${host.host}:${host.port}/ws",
            request = { header(HttpHeaders.Authorization, "Bearer ${host.token}") },
        ) {
            val session = this
            block(object : BridgeSocket {
                override suspend fun receive(): String? {
                    while (true) {
                        val result = session.incoming.receiveCatching()
                        if (result.isClosed) {
                            result.exceptionOrNull()?.let { throw it }
                            return null
                        }
                        val frame = result.getOrNull()
                        if (frame is Frame.Text) return frame.readText()
                    }
                }

                override suspend fun send(text: String) = session.send(Frame.Text(text))
            })
        }
    }
}
