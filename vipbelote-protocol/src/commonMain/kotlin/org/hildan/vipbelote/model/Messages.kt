package org.hildan.vipbelote.model

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed interface VipBeloteMessage

@Serializable
data class Response<T>(
    val status: String,
    val data: T,
) : VipBeloteMessage

@Serializable
data class UnknownMessage(val type: String, val data: JsonElement?) : VipBeloteMessage

@Serializable
data object ConnectOK : VipBeloteMessage

@Serializable
data object SSCommandTouch : VipBeloteMessage

@Serializable
data class Chiching(val amount: Int) : VipBeloteMessage

@Serializable
data object GetStRequest : VipBeloteMessage

@Serializable
data object NbActiveUsersRequest : VipBeloteMessage

@Serializable
data class NbActiveUsersUpdate(val activeUsersByGameType: Map<String, Int>) : VipBeloteMessage

@Serializable
data class ClMessage(
    val type: String,
    val id: String,
    val payload: JsonElement,
    val userId: String
) : VipBeloteMessage
