package org.hildan.vipbelote.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

@Serializable
sealed interface GameMessage : VipBeloteMessage

@Serializable
sealed interface RawGameMessage {
    val type: Int
    val data: JsonObject
}

@Serializable
data class GameMessageIn(
    override val type: Int,
    override val data: JsonObject,
) : RawGameMessage

@Serializable
data class GameMessageOut(
    val msgId: Int,
    override val type: Int,
    override val data: JsonObject,
) : RawGameMessage

fun RawGameMessage.toGameMessage() = when (this) {
    is GameMessageOut -> when (type) {
        0 -> Json.decodeFromJsonElement<GameStarted>(data)
        1 -> Json.decodeFromJsonElement<NewRound>(data)
        2 -> Json.decodeFromJsonElement<CardPlayed>(data)
        3 -> Json.decodeFromJsonElement<PlayOptions>(data)
        4 -> Json.decodeFromJsonElement<PlayerAnnounced>(data)
        5 -> Json.decodeFromJsonElement<AnnounceOptions>(data)
        6 -> Json.decodeFromJsonElement<PlayerAnnounced>(data)
        7 -> Json.decodeFromJsonElement<AnnounceOptions>(data)
        8 -> Json.decodeFromJsonElement<CardsDealt>(data)
        9 -> Json.decodeFromJsonElement<TopCardShown>(data)
        10 -> Json.decodeFromJsonElement<AvailableDeclarations>(data)
        11 -> Json.decodeFromJsonElement<EndOfTrick>(data)
        12 -> Json.decodeFromJsonElement<EndOfRound>(data)
        14 -> Json.decodeFromJsonElement<PlayerDeclared>(data)
        15 -> Json.decodeFromJsonElement<ComeBack>(data)
        18 -> Json.decodeFromJsonElement<JustAPlayer>(data)
        19 -> Json.decodeFromJsonElement<JustAPlayer>(data)
        else -> error("Unknown 'out' message type $type. Data: $data")
    }
    is GameMessageIn -> when (type) {
        0 -> Json.decodeFromJsonElement<PlayCard>(data)
        1 -> Json.decodeFromJsonElement<MakeAnnounce>(data)
        2 -> Json.decodeFromJsonElement<Declare>(data)
        4 -> Json.decodeFromJsonElement<MakeAnnounce>(data)
        else -> error("Unknown 'in' message type $type. Data: $data")
    }
}

@Serializable
data class ReactionMessage(
    val type: String,
    val id: String,
    val payload: JsonElement,
    val userId: String
) : GameMessage

@Serializable
data class GameStarted(
    val startPlayerId: String,
    val playerPositions: Map<String, Long>,
    val playerTeams: Map<String, String>,
) : GameMessage

@Serializable
data class CardsDealt(
    val announcePlayerId: String?,
    val cards: List<Card>,
) : GameMessage

// Classic belote only
@Serializable
data class TopCardShown(val card: Card) : GameMessage

@Serializable
data class AnnounceOptions(
    val playerIdToAnnounce: String,
    val minPoints: Int? = null,
    val availableAnnounceTypes: List<Int>?,
    val availableAnnounceVariationTypes: List<Int>?,
) : GameMessage

@Serializable
data class MakeAnnounce(
    val points: Int? = null,
    val announceType: Int, // 1000=PASS?
    val announceVariationType: Int?,
) : GameMessage

@Serializable
data class PlayerAnnounced(
    val playerId: String,
    val points: Int? = null,
    val announceType: Int, // 1000=PASS?
    val announceVariationType: Int?,
) : GameMessage

@Serializable
data class PlayOptions(
    val playerId: String,
    val playableCards: List<Card>?,
    val isLastCard: Boolean,
) : GameMessage

@Serializable
data class CardPlayed(
    val playerId: String,
    val card: Card,
    /** Index of the current round (pli). */
    val trickIndex: Int,
) : GameMessage

@Serializable
data class PlayCard(
    val card: Card,
) : GameMessage

@Serializable
data class EndOfTrick(
    /**
     * The current trick, on the table.
     */
    val table: Trick,
    val trickIndex: Long,
    val currentScoresByTeam: Score,
    val trick: Trick,
) : GameMessage {
    @Serializable
    data class Score(
        val team1: Long,
        val team2: Long,
    )
}

@Serializable
data class Trick(
    val cards: List<Card>,
    val majorCard: Card,
    val majorCardOwner: Player,
    val primeCard: Card,
    val primeCardOwner: Player,
    val playerOptions: Player,
) {
    @Serializable
    data class Player(
        val id: String,
        val teamId: String,
    )
}

@Serializable
data class Card(val rankEnum: Int, val suitEnum: Int)

@Serializable
data class EndOfRound(
    val scoreData: Scores,
) : GameMessage {
    @Serializable
    data class Scores(
        val team1: TeamScore,
        val team2: TeamScore,
    )

    @Serializable
    data class TeamScore(
        val teamId: String,
        val vutre: Boolean,
        val valat: Boolean,
        val hanging: Boolean,
        val cardsPoints: Long,
        val totalPoints: Long,
        val totalPointsRounded: Long,
        val declarationModels: List<Declaration>,
    )
}

@Serializable
data class NewRound(val startPlayerId: String) : GameMessage

@Serializable
data class ComeBack(
    val playerId: String,
    val comebackRemainingMS: Int?,
) : GameMessage

@Serializable
data class AvailableDeclarations(
    val playerId: String,
    val availableDeclarations: List<Declaration>,
) : GameMessage

@Serializable
data class PlayerDeclared(
    val playerId: String,
    val declarations: List<Declaration>,
) : GameMessage

@Serializable
data class Declare(
    val declarations: List<Declaration>,
) : GameMessage

@Serializable
data class Declaration(
    /**
     * The ID of this declaration. For instance: "BELOTE:CLUBS"
     */
    val id: String,
    val typeEnum: Int,
    val rankEnum: Int? = null,
    val suitEnum: Int,
    val length: Int? = null,
    val valid: Boolean,
    val score: Int,
    val groupId: Int,
)

@Serializable
data class JustAPlayer(val playerId: String) : GameMessage // TODO understand this one
