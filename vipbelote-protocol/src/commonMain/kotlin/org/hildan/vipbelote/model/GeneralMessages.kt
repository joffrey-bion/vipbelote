package org.hildan.vipbelote.model

import kotlinx.serialization.Serializable

@Serializable
sealed interface GeneralMessage : VipBeloteMessage

@Serializable
data object ConnectOK : GeneralMessage

@Serializable
data object SSCommandTouch : GeneralMessage

@Serializable
data class Chiching(val amount: Int) : GeneralMessage

@Serializable
data object NbActiveUsersRequest : GeneralMessage

@Serializable
data class NbActiveUsersUpdate(val activeUsersByGameType: Map<String, Int>) : GeneralMessage

@Serializable
data object GetStRequest : GeneralMessage

@Serializable
data class GetStResponse(
    override val status: String,
    override val data: Data,
) : GeneralMessage, Response<GetStResponse.Data> {
    @Serializable
    data class Data(
        val id: String,
        val spectateGameId: String? = null,
        val currentRoomId: String? = null,
        val currentRoomType: String? = null,
        val gameName: String? = null,
        val lastGameStartedAt: Long,
        val hostId: String? = null,
        val knockoutTournamentState: KnockoutTournamentState,
    ) {
        @Serializable
        data class KnockoutTournamentState(
            val sessionId: String,
            val currentGame: Long,
            val gameName: String,
            val settings: TournamentSettings,
            val expiry: Long,
        ) {
            @Serializable
            data class TournamentSettings(
                val buyIn: BuyIn,
            ) {
                @Serializable
                data class BuyIn(
                    val bet: Long,
                    val reward: Long,
                    val streak: Long,
                )
            }
        }
    }
}