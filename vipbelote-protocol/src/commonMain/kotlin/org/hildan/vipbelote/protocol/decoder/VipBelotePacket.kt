package org.hildan.vipbelote.protocol.decoder

import org.hildan.socketio.*
import org.hildan.vipbelote.protocol.messages.*

sealed interface VipBelotePacket {

    sealed interface Namespaced : VipBelotePacket {
        val namespace: String
    }

    sealed interface Transport : VipBelotePacket {
        data class EIO(val engineIOPacket: EngineIOPacket<*>) : Transport

        data class SIO(val socketIOPacket: SocketIOPacket) : Transport, Namespaced {
            override val namespace: String
                get() = socketIOPacket.namespace
        }
    }

    data class Message(override val namespace: String, val message: VipBeloteMessage) : Namespaced
}

fun VipBeloteDecoder.decode(webSocketPayload: String): VipBelotePacket =
    when (val engineIOPacket = EngineIO.decodeSocketIO(webSocketPayload)) {
        is EngineIOPacket.Open,
        is EngineIOPacket.Upgrade,
        is EngineIOPacket.Ping,
        is EngineIOPacket.Pong,
        is EngineIOPacket.Close,
        is EngineIOPacket.Noop -> VipBelotePacket.Transport.EIO(engineIOPacket)
        is EngineIOPacket.Message -> decode(engineIOPacket)
        is EngineIOPacket.BinaryData -> error("Unexpected binary data in WebSocket payload")
    }

private fun VipBeloteDecoder.decode(engineIOPacket: EngineIOPacket.Message<SocketIOPacket>): VipBelotePacket =
    when (val socketIOPacket = engineIOPacket.payload) {
        is SocketIOPacket.Connect,
        is SocketIOPacket.ConnectError,
        is SocketIOPacket.Disconnect -> VipBelotePacket.Transport.SIO(socketIOPacket)
        is SocketIOPacket.Message -> VipBelotePacket.Message(socketIOPacket.namespace, decode(socketIOPacket))
    }
