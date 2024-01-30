package org.hildan.vipbelote.chrome.inspect

import kotlinx.coroutines.flow.*
import org.hildan.chrome.devtools.domains.target.*
import org.hildan.chrome.devtools.protocol.*
import org.hildan.chrome.devtools.sessions.*
import org.hildan.vipbelote.protocol.decoder.*
import org.hildan.vipbelote.protocol.messages.*
import org.hildan.vipbelote.state.*

suspend fun main() {
    ChromeDPClient().webSocket().use { browserSession ->
        val vipBeloteTab = browserSession.findPageMatching { "vipbelote.fr" in it.url }
            ?: error("No VIP Belote tab in the current Chrome instance")
        println("Found VIP belote tab: $vipBeloteTab")

        print("Connecting to the VIP Belote tab... ")
        val page = browserSession.attachToTarget(vipBeloteTab.targetId).asPageSession()
        println("Success!")

        print("Setting up eavesdropping...")
        val wsTrafficEvents = page.wsTrafficEvents()
        println("Success!")

        val decoder = VipBeloteDecoder()
        wsTrafficEvents
            .map { decoder.decode(it.data) }
            .filterIsInstance<VipBelotePacket.Message>() // ignore transport stuff
            .map { it.message }
            .filterIsInstance<GameMessage>()
            .states()
            .distinctUntilChanged()
            .collect {
                println("___________________________")
                println(it)
            }
    }
}

private suspend fun BrowserSession.findPageMatching(predicate: (TargetInfo) -> Boolean) =
    target.getTargets().targetInfos.find { it.type == "page" && predicate(it) }

enum class WebSocketDirection { Send, Receive }

data class WebSocketFrame(val direction: WebSocketDirection, val data: String)

private suspend fun PageSession.wsTrafficEvents(): Flow<WebSocketFrame> {
    network.enable()
    val sent = network.webSocketFrameReceivedEvents().map { WebSocketFrame(WebSocketDirection.Send, it.response.payloadData) }
    val received = network.webSocketFrameSentEvents().map { WebSocketFrame(WebSocketDirection.Receive, it.response.payloadData) }
    return merge(sent, received)
}

private fun printDecodedEvent(decoder: VipBeloteDecoder, wsFrame: WebSocketFrame) {
    val packet = decoder.decode(wsFrame.data)
    if (packet !is VipBelotePacket.Message) {
        return
    }
    val message = packet.message
    when (message) {
        is GeneralMessage,
        is BonusMessage,
        is ChatMessage,
        is NotificationMessage -> Unit // ignore

        is GameMessage,
        is RoomMessage -> printMessage(wsFrame, packet)

        is UnknownMessage -> {
            println("*".repeat(50))
            printMessage(wsFrame, packet)
            println("*".repeat(50))
        }
    }
}

private fun printMessage(wsFrame: WebSocketFrame, packet: VipBelotePacket.Message) {
    println("${wsFrame.direction.toString().padEnd(7)}\t${packet.namespace}\t${packet.message}")
}
