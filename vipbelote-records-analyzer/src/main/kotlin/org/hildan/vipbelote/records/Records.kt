package org.hildan.vipbelote.records

import kotlinx.datetime.*
import org.hildan.har.*
import org.hildan.vipbelote.protocol.decoder.*
import org.hildan.vipbelote.protocol.messages.*

data class Record(
    val direction: WebSocketDirection,
    val timestamp: Instant,
    val namespace: String,
    val message: VipBeloteMessage,
)

fun Har.recordsSequence(): Sequence<Record> {
    val decoder = VipBeloteDecoder()
    return allWebSocketMessages()
        .asSequence()
        .map { it as? HarWebSocketMessage.Text ?: error("Only text messages are supported in VIP Belote") }
        .mapNotNull { decoder.decode(it) }
}

private fun Har.allWebSocketMessages() = log.entries.flatMap { it.webSocketMessages ?: emptyList() }

private fun VipBeloteDecoder.decode(wsMessage: HarWebSocketMessage.Text): Record? {
    val packet = decode(wsMessage.data) as? VipBelotePacket.Message ?: return null
    return Record(wsMessage.direction, wsMessage.time, packet.namespace, packet.message)
}
