package org.hildan.vipbelote.protocol.messages

import kotlinx.serialization.*
import kotlinx.serialization.json.*
import org.hildan.vipbelote.protocol.serialization.EnumAsCodeSerializer

@Serializable
sealed interface GameMessage : VipBeloteMessage

@Serializable
sealed interface RawGameMessage {
    val type: Int
    val data: JsonObject
}

@Serializable
data class RawGameMessageIn(
    override val type: Int,
    override val data: JsonObject,
) : RawGameMessage

@Serializable
data class RawGameMessageOut(
    val msgId: Int,
    override val type: Int,
    override val data: JsonObject,
) : RawGameMessage

fun RawGameMessage.toGameMessage() = when (this) {
    is RawGameMessageIn -> when (type) {
        0 -> Json.decodeFromJsonElement<PlayCardAction>(data)
        1 -> Json.decodeFromJsonElement<BidAction>(data)
        2 -> Json.decodeFromJsonElement<SelfDeclared>(data)
        4 -> Json.decodeFromJsonElement<BidAction>(data)
        else -> UnknownGameMessageIn(type, data)
    }
    is RawGameMessageOut -> when (type) {
        0 -> Json.decodeFromJsonElement<GameStarted>(data)
        1 -> Json.decodeFromJsonElement<NewRound>(data)
        2 -> Json.decodeFromJsonElement<CardPlayed>(data)
        3 -> Json.decodeFromJsonElement<PlayOptions>(data)
        4 -> Json.decodeFromJsonElement<PlayerBid>(data)
        5 -> Json.decodeFromJsonElement<BidOptions>(data)
        6 -> Json.decodeFromJsonElement<PlayerBid>(data)
        7 -> Json.decodeFromJsonElement<BidOptions>(data)
        8 -> Json.decodeFromJsonElement<CardsDealt>(data)
        9 -> Json.decodeFromJsonElement<TopCardShown>(data)
        10 -> Json.decodeFromJsonElement<AvailableDeclarations>(data)
        11 -> Json.decodeFromJsonElement<EndOfTrick>(data)
        12 -> Json.decodeFromJsonElement<EndOfRound>(data)
        14 -> Json.decodeFromJsonElement<OtherDeclared>(data)
        15 -> Json.decodeFromJsonElement<ComeBack>(data)
        16 -> Json.decodeFromJsonElement<JustAPlayer16>(data)
        17 -> Json.decodeFromJsonElement<ResumeGame>(data)
        18 -> Json.decodeFromJsonElement<JustAPlayer18>(data)
        19 -> Json.decodeFromJsonElement<JustAPlayer19>(data)
        else -> UnknownGameMessageOut(type, msgId, data)
    }
}

@Serializable
data class UnknownGameMessageIn(
    val type: Int,
    val data: JsonObject,
) : GameMessage

@Serializable
data class UnknownGameMessageOut(
    val type: Int,
    val msgId: Int,
    val data: JsonObject,
) : GameMessage

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
data class BidOptions(
    val playerIdToAnnounce: String,
    val minPoints: Int? = null,
    val availableAnnounceTypes: List<Int>?,
    val availableAnnounceVariationTypes: List<Int>?,
) : GameMessage

@Serializable
data class BidAction(
    val points: Int? = null,
    /**
     * Suit code, or 1000 for PASS
     */
    val announceType: Int,
    val announceVariationType: Int?,
) : GameMessage

@Serializable
data class PlayerBid(
    val playerId: String,
    val points: Int? = null,
    /**
     * Suit code, or 1000 for PASS
     */
    val announceType: Int,
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
    /** ID of the player who just played */
    val playerId: String,
    /** The card that was just played. */
    val card: Card,
    /** Index of the current trick (pli). */
    val trickIndex: Int,
) : GameMessage

@Serializable
data class PlayCardAction(
    val card: Card,
) : GameMessage

@Serializable
data class EndOfTrick(
    /**
     * The current trick, on the table.
     */
    val table: Trick,
    val trickIndex: Long,
    val currentScoresByTeam: ScoresByTeam,
    val trick: Trick,
) : GameMessage

@Serializable
data class Trick(
    val cards: List<Card>,
    val majorCard: Card? = null, // null in some saved games' table
    val majorCardOwner: Player? = null,
    val primeCard: Card? = null,
    val primeCardOwner: Player? = null,
    val playerOptions: Player? = null,
)

@Serializable
data class Player(
    val id: String,
    val teamId: String,
)

@Serializable
data class Card(
    @SerialName("rankEnum")
    val rank: Rank,
    @SerialName("suitEnum")
    val suit: Suit,
) {
    // The appended U+FE0F code turns the char into its emoji version
    // See: https://en.wikipedia.org/wiki/Playing_cards_in_Unicode#Emoji
    override fun toString(): String = "${rank.displayName}${suit.filledChar}\uFE0F"
}

@Serializable(with = RankEnumSerializer::class)
enum class Rank(val serializedCode: Int, val displayName: String, val primeRank: Int) {
    C_7(serializedCode = 55, displayName = "7", primeRank = 1),
    C_8(serializedCode = 56, displayName = "8", primeRank = 2),
    C_9(serializedCode = 57, displayName = "9", primeRank = 7),
    Jack(serializedCode = 106, displayName = "J", primeRank = 8),
    Queen(serializedCode = 113, displayName = "Q", primeRank = 3),
    King(serializedCode = 107, displayName = "K", primeRank = 4),
    C_10(serializedCode = 116, displayName = "10", primeRank = 5),
    Ace(serializedCode = 97, displayName = "A", primeRank = 6),
}

private class RankEnumSerializer : EnumAsCodeSerializer<Rank>(Rank.entries, { serializedCode })

@Serializable(with = SuitEnumSerializer::class)
enum class Suit(val serializedCode: Int, val filledChar: String, val frameChar: String) {
    Clubs(serializedCode = 99, filledChar = "♣", frameChar = "♧"),
    Diamonds(serializedCode = 100, filledChar = "♦", frameChar = "♢"),
    Hearts(serializedCode = 104, filledChar = "♥", frameChar = "♡"),
    Spades(serializedCode = 115, filledChar = "♠", frameChar = "♤"),
}

private class SuitEnumSerializer : EnumAsCodeSerializer<Suit>(Suit.entries, { serializedCode })

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

sealed interface PlayerDeclared {
    val declarations: List<Declaration>
}

@Serializable
data class OtherDeclared(
    val playerId: String,
    override val declarations: List<Declaration>,
) : GameMessage, PlayerDeclared

@Serializable
data class SelfDeclared(
    override val declarations: List<Declaration>,
) : GameMessage, PlayerDeclared

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
data class JustAPlayer16(val playerId: String) : GameMessage // TODO understand this one

@Serializable
data class JustAPlayer18(val playerId: String) : GameMessage // TODO understand this one

@Serializable
data class JustAPlayer19(val playerId: String) : GameMessage // TODO understand this one

@Serializable
data class ResumeGame(val gameSaveData: GameSaveData) : GameMessage

@Serializable
data class GameSaveData(
    val gameViewModel: GameViewModel,
    val currentPlayerId: String,
    val startPlayerId: String,
    val myPlayerId: String,
    val playerPositions: Map<String, Int>,
    val playerTeams: Map<String, String>,
    val scoresByTeam: ScoresByTeam,
    val fromStateName: String, // e.g. "CardPlay", "Announce"
    val lastPlayableCards: List<Card>? = null,
    val coincheMinPoints: Int? = null,
    val availableAnnounces: List<Int>? = null,
    val availableAnnounceVariations: List<Int>? = null,
    val elapsedTime: Long,
    val replacedUsers: Map<String, Boolean>,
    val comebackRemainingMS: Long? = null,
    val winnerTeamID: String? = null,
)

@Serializable
data class GameViewModel(
    val players: List<PlayerWithCards>,
    val table: Trick,
    val announce: Announce,
    val currentPlayerIndex: Long,
    val startPlayerId: String,
    val shownCard: Card? = null,
    val currentScoresByTeam: ScoresByTeam,
    val lastTable: Trick? = null,
    val lastTrick: Trick? = null,
) {
    @Serializable
    data class PlayerWithCards(
        val id: String,
        val teamId: String,
        val cards: List<Card?>,
    )

    @Serializable
    data class Announce(
        val passAnnCounter: Int,
        val topAnnouncerPlayer: Player?,
        val announceData: AnnounceData,
        val hasContract: Boolean,
        val playerAnnounces: Map<String, AnnounceData>,
    ) {
        @Serializable
        data class AnnounceData(
            val announceType: Int,
            val announceVariationType: Int? = null,
            val points: Int? = null,
        )
    }
}

@Serializable
data class ScoresByTeam(
    val team1: Long,
    val team2: Long,
)
