package org.hildan.vipbelote.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface ChatMessage : VipBeloteMessage

@Serializable
data class ChatPubMessage(
    val id: String,
    val senderId: String,
    val type: String,
    val conversationId: String,
    val isDeleted: Boolean,
    val createdAt: Long,
    val data: Data,
) : ChatMessage {

    @Serializable
    data class Data(val body: String)
}
