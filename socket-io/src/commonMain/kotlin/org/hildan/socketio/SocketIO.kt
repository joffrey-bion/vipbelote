package org.hildan.socketio

import kotlinx.serialization.json.*

/**
 * The Socket.IO decoder, following the [Socket.IO protocol](https://socket.io/docs/v4/socket-io-protocol).
 */
object SocketIO {

    private val packetFormatRegex = Regex("""(?<packetType>\d)(?<nBinaryAttachments>\d-)?((?<namespace>/[^,]+),)?(?<ackId>\d+)?(?<payload>.*)?""")

    /**
     * Decodes the given [encodedData] into a [SocketIOPacket].
     *
     * Binary packet types are not supported. Binary attachments are passed in subsequent web socket frames
     * (1 frame per attachment), and thus cannot be handled in this single-frame decoding function.
     */
    fun decode(encodedData: String): SocketIOPacket {
        val match = packetFormatRegex.matchEntire(encodedData) ?: throw InvalidSocketIOPacketException(encodedData)
        val packetType = match.groups["packetType"]?.value?.toInt()
            ?: error("Internal error: Socket.IO format regex was matched but the packetType group is missing")

        val nBinaryAttachments = match.groups["nBinaryAttachments"]?.value?.toInt() ?: 0
        check(nBinaryAttachments == 0) { "Socket.IO packets with binary attachments are not supported" }

        val namespace = match.groups["namespace"]?.value ?: "/"
        val ackId = match.groups["ackId"]?.value?.toInt()
        val payload = match.groups["payload"]?.value?.takeIf { it.isNotBlank() }?.let { Json.parseToJsonElement(it) }
        return when (packetType) {
            0 -> SocketIOPacket.Connect(namespace, payload = ensureJsonObjectOrNull(payload))
            1 -> SocketIOPacket.Disconnect(namespace)
            2 -> SocketIOPacket.Event(namespace, ackId, payload = ensureNonEmptyArray(payload))
            3 -> SocketIOPacket.Ack(
                namespace = namespace,
                ackId = ackId ?: error("ACK packet must have an Ack ID"),
                payload = ensureNonEmptyArray(payload)
            )
            4 -> SocketIOPacket.ConnectError(namespace, errorData = payload)
            5, 6 -> error("Binary Socket.IO packets are not supported")
            else -> error("Unknown Socket.IO packet type $packetType")
        }
    }

    private fun ensureJsonObjectOrNull(payload: JsonElement?): JsonObject? {
        if (payload == null) {
            return null
        }
        check(payload is JsonObject) { "The payload must be a JSON object" }
        return payload
    }

    private fun ensureNonEmptyArray(payload: JsonElement?): JsonArray {
        check(payload != null) { "The payload is mandatory" }
        check(payload is JsonArray) { "The payload must be a JSON array" }
        check(payload.isNotEmpty()) { "The payload must be a non-empty JSON array" }
        return payload
    }
}

/**
 * An exception thrown when some encoded data doesn't represent a valid Socket.IO packet as defined by the
 * [Socket.IO protocol](https://socket.io/docs/v4/socket-io-protocol).
 */
@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class InvalidSocketIOPacketException(val encodedData: String) : Exception("Invalid Socket.IO packet: $encodedData")

