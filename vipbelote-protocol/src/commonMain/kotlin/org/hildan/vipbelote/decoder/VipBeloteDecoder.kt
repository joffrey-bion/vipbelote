package org.hildan.vipbelote.decoder

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.hildan.socketio.*
import org.hildan.vipbelote.model.*

/**
 * A decoder for messages in the VIP Belote game (http://vipbelote.fr).
 */
class VipBeloteDecoder {

    private data class Key(val namespace: String, val id: Int)

    private val json = Json
    private val messagesById = mutableMapOf<Key, VipBeloteMessage>()
    private val commandsById = mutableMapOf<Key, Command>()

    /**
     * Decodes the given Socket.IO message into a [VipBeloteMessage].
     *
     * This operation is stateful. Commands and events that require responses are stored so that future references
     * can find them.
     */
    fun decode(sioMessage: SocketIOPacket.Message): VipBeloteMessage = try {
        when(sioMessage) {
            is SocketIOPacket.Event -> decodeEvent(sioMessage).also { msg ->
                sioMessage.ackId?.let { id ->
                    messagesById[Key(sioMessage.namespace, id)] = msg
                }
            }
            is SocketIOPacket.Ack -> decodeAck(
                packet = sioMessage,
                requestMessage = messagesById[Key(sioMessage.namespace, sioMessage.ackId)]
                    ?: error("No past event corresponds to ackId=${sioMessage.ackId} in namespace ${sioMessage.namespace}"),
            )
        }
    } catch (e: SerializationException) {
        throw InvalidVipBeloteMessageException(sioMessage, e)
    }

    private fun decodeEvent(packet: SocketIOPacket.Event): VipBeloteMessage {
        val (eventType, eventData) = packet.payload.toVipBeloteEvent()
        return when (eventType) {
            "bonusesUpdated" -> json.decodeFromJsonElement<BonusesUpdated>(eventData)
            "cashbackBonusUpdate" -> json.decodeFromJsonElement<CashbackBonusUpdate>(eventData)
            "chat.pub.msg" -> json.decodeFromJsonElement<ChatPubMessage>(eventData)
            "chat.pub.conv.del" -> json.decodeFromJsonElement(DeleteConversationSerializer, eventData)
            "challengeProgressUpdatedEvent" -> json.decodeFromJsonElement<ChallengeProgressUpdated>(eventData)
            "chiching" -> json.decodeFromJsonElement(ChichingSerializer, eventData)
            "clmsg.in",
            "clmsg.out" -> json.decodeFromJsonElement<ReactionMessage>(eventData)
            "connect.ok" -> ConnectOK
            "getst" -> GetStRequest
            "gmsg.in" -> json.decodeFromJsonElement<RawGameMessageIn>(eventData).toGameMessage()
            "gmsg.out" -> json.decodeFromJsonElement<RawGameMessageOut>(eventData).toGameMessage()
            "mygupd" -> json.decodeFromJsonElement<TableState>(eventData)
            "notification",
            "notification-v2" -> json.decodeFromJsonElement<Notification>(eventData)
            "premium.gift.notification" -> json.decodeFromJsonElement<PremiumGiftNotification>(eventData)
            "pactiveall" -> when (eventData) {
                is JsonNull -> NbActiveUsersRequest
                else -> json.decodeFromJsonElement(NbActiveUsersUpdateSerializer, eventData)
            }
            "rconn.ok" -> json.decodeFromJsonElement<RoomConnectionOk>(eventData)
            "rcancel" -> json.decodeFromJsonElement<RoomSearchCancelRequest>(eventData)
            "rdst" -> json.decodeFromJsonElement<RoomDestroyed>(eventData)
            "rematch" -> json.decodeFromJsonElement<RematchCommand>(eventData).also {
                commandsById[Key(packet.namespace, it.cid)] = it
            }
            "rfound" -> json.decodeFromJsonElement<RoomFound>(eventData)
            "rjoin" -> json.decodeFromJsonElement<RoomJoinCommand>(eventData).also {
                commandsById[Key(packet.namespace, it.cid)] = it
            }
            "rleav" -> json.decodeFromJsonElement<RoomLeaveCommand>(eventData).also {
                commandsById[Key(packet.namespace, it.cid)] = it
            }
            "rrdy" -> json.decodeFromJsonElement<RoomReadyCommand>(eventData).also {
                commandsById[Key(packet.namespace, it.cid)] = it
            }
            "rsrch" -> json.decodeFromJsonElement<RoomSearchRequest>(eventData)
            "rupd" -> json.decodeFromJsonElement<RoomUpdated>(eventData)
            "sscmd.touch" -> SSCommandTouch
            "supd" -> json.decodeFromJsonElement<SearchUpdate>(eventData)
            "walletChipsUpdate" -> json.decodeFromJsonElement<WalletChipsUpdate>(eventData)
            "xpUpdated" -> json.decodeFromJsonElement<XPUpdated>(eventData)
            else -> {
                val commandId = eventType.toIntOrNull() ?: return UnknownMessage(eventType, eventData)
                decodeCommandResponse(Key(packet.namespace, commandId), eventData)
            }
        }
    }

    private fun decodeCommandResponse(commandKey: Key, eventData: JsonElement): VipBeloteMessage =
        when (commandsById[commandKey] ?: error("Cannot find command with id=${commandKey.id} in namespace ${commandKey.namespace}")) {
            is RoomJoinCommand -> json.decodeFromJsonElement<RoomJoinResponse>(eventData)
            is RoomLeaveCommand -> json.decodeFromJsonElement<RoomLeaveResponse>(eventData)
            is RoomReadyCommand -> json.decodeFromJsonElement<RoomReadyResponse>(eventData)
            is RematchCommand -> json.decodeFromJsonElement<RematchResponse>(eventData)
        }

    private fun decodeAck(packet: SocketIOPacket.Ack, requestMessage: VipBeloteMessage): VipBeloteMessage =
        when(requestMessage) {
            is GetStRequest -> json.decodeFromJsonElement<GetStResponse>(packet.payload[0])
            is RoomSearchRequest -> json.decodeFromJsonElement<RoomSearchResponse>(packet.payload[0])
            is RoomSearchCancelRequest -> json.decodeFromJsonElement<RoomSearchCancelResponse>(packet.payload[0])
            else -> error("Message type ${requestMessage::class.simpleName} not supported as initial request, got ACK for it: $packet")
        }
}

/**
 * An exception thrown when a Socket.IO packet doesn't represent a valid message from VIP Belote.
 */
@Suppress("MemberVisibilityCanBePrivate", "CanBeParameter")
class InvalidVipBeloteMessageException(
    val socketIOPacket: SocketIOPacket.Message,
    cause: Throwable,
) : Exception("Invalid VIP Belote message: $socketIOPacket", cause)

private fun JsonArray.toVipBeloteEvent(): VipBeloteEvent {
    val eventData = when (size) {
        1 -> JsonNull
        2 -> this[1]
        3 -> this[2].also {
            require(this[1].isString && this[1].string == "clmsg.out") {
                "Only 'clmsg.in'+'clmsg.out' event type is allowed to have 3 elements"
            }
        }
        else -> error("VIP Belote events must have between 1 and 3 elements, got $size: $this")
    }
    val eventTypeElement = this[0]
    require(eventTypeElement.isString) {
        "VIP Belote events must have a string event type as first element, got $eventTypeElement"
    }
    return VipBeloteEvent(eventTypeElement.string, eventData)
}

private data class VipBeloteEvent(val type: String, val data: JsonElement)

private val JsonElement.isString: Boolean
    get() = this is JsonPrimitive && isString

private val JsonElement.string: String
    get() = if (this is JsonPrimitive) string else error("$this does not represent a string")

private val JsonPrimitive.string: String
    get() = if (isString) content else error("$this does not represent a string")


private val ChichingSerializer = ObjectWrapperSerializer("amount", Chiching.serializer())
private val DeleteConversationSerializer = ObjectWrapperSerializer("id", DeleteConversation.serializer())
private val NbActiveUsersUpdateSerializer = ObjectWrapperSerializer("activeUsersByGameType", NbActiveUsersUpdate.serializer())

private class ObjectWrapperSerializer<T : Any>(
    private val propertyName: String,
    delegate: KSerializer<T>,
) : JsonTransformingSerializer<T>(delegate) {
    override fun transformSerialize(element: JsonElement): JsonElement =
        element.jsonObject.getValue(propertyName)

    override fun transformDeserialize(element: JsonElement): JsonElement =
        JsonObject(content = mapOf(propertyName to element))
}
