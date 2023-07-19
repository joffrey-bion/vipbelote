package org.hildan.vipbelote.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface NotificationMessage : VipBeloteMessage

@Serializable
data class Notification(
    val id: String,
    val notificationType: String,
    val read: Boolean,
    val createdAt: Long,
    val senderId: String? = null,
    val params: JsonObject,
    val senderProfile: JsonElement? = null,
    val notificationCategory: String,
    val customBodyText: JsonObject,
    val customTitleText: JsonObject,
) : NotificationMessage
