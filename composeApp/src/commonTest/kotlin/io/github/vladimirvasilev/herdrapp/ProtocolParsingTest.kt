package io.github.vladimirvasilev.herdrapp

import io.github.vladimirvasilev.herdrapp.data.bridge.*
import io.github.vladimirvasilev.herdrapp.data.bridge.dto.*
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

class ProtocolParsingTest {
    @Test fun parsesHelloSnapshotFrameResultAndStream() {
        val hello = assertIs<ServerMessage.Hello>(BridgeJson.parse("""{"type":"hello","protocol":1,"bridge_version":"0.1.0","name":"WORK-PC","herdr":{"version":"0.8","protocol":19,"available":true}}"""))
        assertEquals("WORK-PC", hello.value.name)

        val snapshotText = readResource("snapshot_fixture.json")
        val snapshot = assertIs<ServerMessage.Snapshot>(BridgeJson.parse(snapshotText)).value
        assertEquals(2, snapshot.workspaces.size)
        assertEquals("C:\\fake\\mobile", snapshot.panes.first { it.paneId == "w1:p1" }.cwd)
        assertEquals(AgentStatus.BLOCKED, snapshot.agents.first { it.paneId == "w1:p1" }.agentStatus)

        @OptIn(ExperimentalEncodingApi::class)
        val frame = assertIs<ServerMessage.Frame>(BridgeJson.parse("""{"type":"frame","pane_id":"w1:p1","seq":12,"full":false,"width":80,"height":40,"bytes":"${Base64.Default.encode("hello".encodeToByteArray())}"}""")).value
        assertEquals(12, frame.seq)

        val result = assertIs<ServerMessage.Result>(BridgeJson.parse("""{"type":"result","id":"r1","ok":false,"error":{"code":"pane_not_found","message":"missing"}}""")).value
        assertEquals("pane_not_found", result.error?.code)

        val stream = assertIs<ServerMessage.Stream>(BridgeJson.parse("""{"type":"stream","pane_id":"w1:p1","mode":"observe","reason":"detached"}""")).value
        assertEquals(StreamMode.OBSERVE, stream.mode)

        val herdrStatus = assertIs<ServerMessage.HerdrStatus>(BridgeJson.parse("""{"type":"herdr_status","version":"0.8","protocol":19,"available":false,"future":true}""")).value
        assertEquals(false, herdrStatus.available)

        val unknown = assertIs<ServerMessage.Unknown>(BridgeJson.parse("""{"type":"future_message","payload":42}"""))
        assertEquals("future_message", unknown.type)
    }
}

fun readResource(name: String): String = object {}.javaClass.classLoader!!.getResource(name)!!.readText()
