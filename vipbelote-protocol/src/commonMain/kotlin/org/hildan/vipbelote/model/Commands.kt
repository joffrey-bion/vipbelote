package org.hildan.vipbelote.model

import kotlinx.serialization.Serializable

sealed interface Command {
    /** Command ID, reused in responses. */
    val cid: Int
}

@Serializable
data class SimpleCommandResponse<C : Command>(
    val cmd: C,
    val status: String,
) : VipBeloteMessage

@Serializable
data class CommandResponse<C : Command, T>(
    val cmd: C,
    val status: String,
    val data: T,
) : VipBeloteMessage
