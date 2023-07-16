package org.hildan.vipbelote.model

import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed interface VipBeloteMessage

@Serializable
sealed interface Response<out D> {
    val status: String
    val data: D
}

sealed interface Command {
    /** Command ID, reused in responses. */
    val cid: Int
}

sealed interface CommandResponse<out C : Command, out D> : Response<D> {
    val cmd: C
}

sealed interface EmptyCommandResponse<out C : Command> : CommandResponse<C, Nothing> {
    override val data: Nothing
        get() = error("No data in empty response")
}

@Serializable
data class UnknownMessage(val type: String, val data: JsonElement?) : VipBeloteMessage
