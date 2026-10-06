package io.github.vladimirvasilev.herdrapp.protocol

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonPrimitive

object BridgeJson {
    val json = Json {
        ignoreUnknownKeys = true
        explicitNulls = false
        encodeDefaults = true
    }

    fun parse(text: String): ServerMessage {
        val element = json.parseToJsonElement(text)
        val obj = element as? JsonObject ?: return ServerMessage.Unknown("", element)
        return when (val type = obj["type"]?.jsonPrimitive?.content ?: "") {
            "hello" -> ServerMessage.Hello(json.decodeFromJsonElement(element))
            "snapshot" -> ServerMessage.Snapshot(json.decodeFromJsonElement(element))
            "agent_status" -> ServerMessage.AgentStatus(json.decodeFromJsonElement(element))
            "frame" -> ServerMessage.Frame(json.decodeFromJsonElement(element))
            "stream" -> ServerMessage.Stream(json.decodeFromJsonElement(element))
            "herdr_status" -> ServerMessage.HerdrStatus(json.decodeFromJsonElement(element))
            "result" -> ServerMessage.Result(json.decodeFromJsonElement(element))
            "pong" -> ServerMessage.Pong(json.decodeFromJsonElement(element))
            else -> ServerMessage.Unknown(type, element)
        }
    }

    inline fun <reified T> encode(value: T): String = json.encodeToString(value)
}
