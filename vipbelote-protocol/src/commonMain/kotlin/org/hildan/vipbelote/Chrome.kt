package org.hildan.vipbelote

import kotlinx.coroutines.flow.*
import org.hildan.chrome.devtools.protocol.*
import org.hildan.chrome.devtools.sessions.*

private suspend fun printChromeMessages() {
    ChromeDPClient().webSocket().use { browserSession ->
        val vipBeloteTab = browserSession.target.getTargets().targetInfos.find { "vipbelote.fr" in it.url }
            ?: error("No VIP Belote game running in the current Chrome instance")

        browserSession.attachToTarget(vipBeloteTab.targetId).asPageSession().use { page ->
            val sent = page.network.webSocketFrameReceivedEvents().map { it.response }
            val received = page.network.webSocketFrameSentEvents().map { it.response }
            merge(sent, received).collect {
                println("WS ${it.payloadData}")
            }
        }
    }
}
