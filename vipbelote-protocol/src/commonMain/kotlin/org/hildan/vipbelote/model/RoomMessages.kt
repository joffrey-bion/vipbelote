package org.hildan.vipbelote.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

@Serializable
sealed interface RoomMessage : VipBeloteMessage

@Serializable
data class RoomSearchRequest(val presetId: String) : RoomMessage

@Serializable
data class RoomSearchResponse(
    override val status: String,
    override val data: SearchUpdate,
) : RoomMessage, Response<SearchUpdate>

@Serializable
data class SearchUpdate(
    val avgAttempts: Double,
    val avgSessionLifetime: Double,
    val currentSize: Int,
    val maxAttempts: Int,
    val maxAttemptsMmr: Int,
    val maxSessionLifetime: Int,
    val totalMatches: Int,
) : RoomMessage

@Serializable
data class RoomSearchCancelRequest(val presetId: String) : RoomMessage

@Serializable
data class RoomSearchCancelResponse(override val status: String) : RoomMessage, EmptyResponse

@Serializable
data class RoomFound(
    override val id: String,
    override val settings: RoomSettings,
    override val state: String,
    override val tables: List<TableState>,
    override val poolId: String,
    override val blocklist: List<JsonElement>,
    override val createdAt: Long,
    val stateData: StateData,
    val invitationCode: String,
    val declinedInvitations: List<JsonElement>,
) : RoomMessage, Room

@Serializable
data class RoomConnectionOk(
    override val id: String,
    override val settings: RoomSettings,
    override val state: String,
    override val tables: List<TableState>,
    override val poolId: String,
    override val blocklist: List<JsonElement>,
    override val createdAt: Long,
) : RoomMessage, Room

@Serializable
data class RoomJoinCommand(
    override val cid: Int,
    val tableId: String,
    val seatId: Int,
) : RoomMessage, Command

@Serializable
data class RoomJoinResponse(
    override val cmd: RoomJoinCommand,
    override val status: String,
    override val data: Data,
) : RoomMessage, CommandResponse<RoomJoinCommand, RoomJoinResponse.Data> {
    @Serializable
    data class Data(
        val id: String,
        val state: String,
        val stateData: StateData,
        val tables: List<TableState>,
    )
}

@Serializable
data class RoomLeaveCommand(
    override val cid: Int,
    val currentClientTableStateName: String,
    val currentClientRoomStateName: String,
    val onlyLeaveTable: Boolean? = null,
) : RoomMessage, Command

@Serializable
data class RoomLeaveResponse(
    override val cmd: RoomLeaveCommand,
    override val status: String,
) : RoomMessage, EmptyCommandResponse<RoomLeaveCommand>

@Serializable
data class RoomReadyCommand(override val cid: Int) : RoomMessage, Command

@Serializable
data class RoomReadyResponse(
    override val cmd: RoomReadyCommand,
    override val status: String,
) : RoomMessage, EmptyCommandResponse<RoomReadyCommand>

@Serializable
data class RematchCommand(override val cid: Int) : RoomMessage, Command

@Serializable
data class RematchResponse(
    override val cmd: RematchCommand,
    override val status: String,
) : RoomMessage, EmptyCommandResponse<RematchCommand>

@Serializable
data class RoomDestroyed(val reason: String? = null) : RoomMessage

@Serializable
data class RoomUpdated(
    val id: String,
    val state: String,
    val poolId: String? = null,
    val tables: List<TableState>,
) : RoomMessage

@Serializable
data class TableState(
    val id: String,
    val state: String,
    val seats: List<Seat>,
    val tournamentRound: Long? = null,
    val settings: Game? = null,
    val gameInfo: GameInfo? = null,
    val gameResult: GameResult? = null,
    val spectatorsCount: Long? = null,
    val spectators: List<JsonElement>? = null, // TODO
    val lastModified: Long? = null,
) : RoomMessage {
    @Serializable
    data class Seat(
        val id: Long,
        val teamId: String,
        val player: Player? = null,
        val reserved: Player? = null,
    ) {
        @Serializable
        data class Player(
            val id: String,
            val state: String,
            val isBot: Boolean,
            val isSpectator: Boolean,
            val profile: Profile,
        )

        @Serializable
        data class Profile(
            val id: String,
            val avatar: String,
            val avatarUrl: String,
            val device: String,
            val isVip: Boolean,
            val level: Long,
            val inventory: List<JsonElement>, // TODO
            val username: String,
            val membershipLevel: Long,
        )
    }

    @Serializable
    data class GameInfo(
        val gameId: String,
        val roomId: String,
        val bet: Long,
        val reward: Long,
        val configuration: Game,
        val roomType: String,
        val teams: List<List<String>>,
    )

    @Serializable
    data class GameResult(
        val winners: List<JsonElement>, // TODO
        val losers: List<JsonElement>, // TODO
        val gameSummary: GameSummary,
        /** Not present in RoomFound event */
        val gameStats: JsonElement? = null,
    ) {
        @Serializable
        data class GameSummary(
            val playerStats: JsonObject,
            val deltaXp: JsonObject,
            val deltaLevel: JsonObject,
            val oldLevel: JsonObject,
            val playersBalance: JsonObject,
            /** Not present in [RoomFound] event, present in [RoomUpdated] */
            val rematchDestroyTimestamp: Long? = null,
        )
    }
}

interface Room {
    val id: String
    val settings: RoomSettings
    val state: String
    val tables: List<TableState>
    val poolId: String
    val blocklist: List<JsonElement>
    val createdAt: Long
}

@Serializable
data class RoomSettings(
    val presetId: String,
    val debug: Boolean,
    val type: String,
    val ranked: Boolean,
    val locked: Boolean,
    val spectators: Boolean,
    val rematch: Boolean,
    val findNewRoomEnabled: Boolean,
    val transactions: Transactions,
    val trackingId: String,
    val players: Players,
    val leaderboard: Leaderboard,
    val requirements: Requirements,
    val timers: Timers,
    val game: Game,
    val extends: JsonElement, // TODO
) {

    @Serializable
    data class Players(
        val count: Long,
        val shuffle: Boolean,
        val qualifyingSpots: Long,
    )

    @Serializable
    data class Requirements(
        val requiresVip: Boolean,
        val minLevel: Long,
        val maxLevel: Long,
    )

    @Serializable
    data class Timers(
        val gameSummaryTimeout: Long,
        val nextTableWaitingTimeout: Long,
        val waitingTimeout: Long,
        val waitingStateEndTimer: Long,
        val readyStateStartTimer: Long,
    )
}

@Serializable
data class Game(
    val name: String, // contree
    val mode: String, // contree
    val enablePredefinedChat: Boolean,
    val enableEmoji: Boolean,
    val enableGifts: Boolean,
    val enableVoiceMessaging: Boolean,
    val timers: Timers,
    val players: Players,
    val xpTable: XPTable,
    val declarations: Boolean,
    val allNoTrumps: Boolean,
    val moveDirection: String,
    val enableSlots: Boolean,
    val inventory: List<JsonElement>, // TODO
    val maxScore: Int? = null,
) {

    @Serializable
    data class Players(
        val count: Long,
        val perTeam: Long,
    )

    @Serializable
    data class Timers(
        val move: Long,
        val endround: Long,
        val gameOverLeaveCountdown: Long,
        val gameLoadTimeout: Long,
    )

    @Serializable
    data class XPTable(
        val gameStarted: Long,
        val takeTrick: Long,
        val cardPlay: Long,
        val gameWon: Long,
        val gameLost: Long,
    )
}

@Serializable
data class Leaderboard(
    val enabled: Boolean,
    val id: String,
    val secondaryId: String,
    val pointsPerWin: Long,
    val pointsPerLoss: Long,
)

@Serializable
data class Transactions(
    val bet: Long,
    val placementRewards: Map<String, Rewards>,
) {
    @Serializable
    data class Rewards(
        val amount: Long,
        val currencyType: String,
        val place: Long,
    )
}

@Serializable
data class StateData(
    val stateEndTimestamp: Long,
    val stateTransitionTimestamp: Long? = null,
)
