package org.hildan.vipbelote.chrome.inspect

import kotlinx.coroutines.flow.*
import org.hildan.chrome.devtools.domains.target.*
import org.hildan.chrome.devtools.protocol.*
import org.hildan.chrome.devtools.sessions.*
import org.hildan.vipbelote.protocol.decoder.*
import org.hildan.vipbelote.protocol.messages.*
import org.hildan.vipbelote.state.*
import java.net.ConnectException
import kotlin.system.*

suspend fun main() {
    connectToChrome().use { browserSession ->
        val vipBeloteTab = browserSession.findPageMatching { "vipbelote.fr" in it.url }
            ?: error("No VIP Belote tab in the current Chrome instance")
        println("Found VIP belote tab: ${vipBeloteTab.url}")

        print("Connecting to the VIP Belote tab... ")
        val page = browserSession.attachToTarget(vipBeloteTab.targetId).asPageSession()
        println("Success!")

        print("Setting up eavesdropping...")
        val wsFrames = page.wsFramePayloads()
        println("Success!")

        println("Printing game states...")
        val decoder = VipBeloteDecoder()
        wsFrames
            .map { decoder.decode(it) }
            .filterIsInstance<VipBelotePacket.Message>() // ignore transport stuff
            .map { it.message }
            .onEach {
                if (it is UnknownMessage) {
                    println("*".repeat(50))
                    println(it)
                    println("*".repeat(50))
                }
            }
            .filterIsInstance<GameMessage>()
            .states()
            .distinctUntilChanged()
            .collect {
                println("──────────────────────────────────")
                println(it)
            }
    }
}

private suspend fun connectToChrome() = try {
    ChromeDPClient().webSocket()
} catch (e: ConnectException) {
    System.err.println("Couldn't connect to Chrome, is the debugger enabled?")
    exitProcess(1)
}

private suspend fun BrowserSession.findPageMatching(predicate: (TargetInfo) -> Boolean) =
    target.getTargets().targetInfos.find { it.type == "page" && predicate(it) }

private suspend fun PageSession.wsFramePayloads(): Flow<String> {
    network.enable()
    val sent = network.webSocketFrameReceivedEvents().map { it.response.payloadData }
    val received = network.webSocketFrameSentEvents().map { it.response.payloadData }
    return merge(sent, received)
}
