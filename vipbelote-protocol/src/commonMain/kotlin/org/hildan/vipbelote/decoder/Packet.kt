package org.hildan.vipbelote.decoder

import org.hildan.socketio.*
import org.hildan.vipbelote.model.*

sealed interface Packet {

    sealed interface Namespaced : Packet {
        val namespace: String
    }

    sealed interface Transport : Packet {
        data class EIO(val engineIOPacket: EngineIOPacket<*>) : Transport

        data class SIO(val socketIOPacket: SocketIOPacket) : Transport, Namespaced {
            override val namespace: String
                get() = socketIOPacket.namespace
        }
    }

    data class Message(override val namespace: String, val message: VipBeloteMessage) : Namespaced
}

fun VipBeloteDecoder.decode(webSocketPayload: String): Packet =
    when (val engineIOPacket = EngineIO.decodeSocketIO(webSocketPayload)) {
        is EngineIOPacket.Open,
        is EngineIOPacket.Upgrade,
        is EngineIOPacket.Ping,
        is EngineIOPacket.Pong,
        is EngineIOPacket.Close,
        is EngineIOPacket.Noop -> Packet.Transport.EIO(engineIOPacket)
        is EngineIOPacket.Message -> decode(engineIOPacket)
    }

private fun VipBeloteDecoder.decode(engineIOPacket: EngineIOPacket.Message<SocketIOPacket>): Packet =
    when (val socketIOPacket = engineIOPacket.payload) {
        is SocketIOPacket.Connect,
        is SocketIOPacket.ConnectError,
        is SocketIOPacket.Disconnect -> Packet.Transport.SIO(socketIOPacket)
        is SocketIOPacket.Message -> Packet.Message(socketIOPacket.namespace, decode(socketIOPacket))
    }
