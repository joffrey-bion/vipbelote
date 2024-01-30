package org.hildan.vipbelote.state

import org.hildan.vipbelote.protocol.messages.*

const val HildanPlayerId = "21f53f8d-fa04-4a92-bf03-f0a2030391b6"

sealed interface GameState {
    val selfPlayerId: String

    data class Initial(
        override val selfPlayerId: String,
    ): GameState {
        override fun toString(): String = "Not started"
    }

    sealed interface WithPlayers : GameState {
        val players: Map<String, Player>
    }

    sealed interface Bidding : WithPlayers

    data class BeforeBid(
        override val selfPlayerId: String,
        override val players: Map<String, Player>,
        val startPlayerId: String,
    ): Bidding {
        override fun toString(): String = "Initial: players = ${players.keys}"
    }

    sealed interface Playable : WithPlayers

    data class WithBid(
        override val selfPlayerId: String,
        override val players: Map<String, Player>,
        val currentBid: Bid,
    ): Bidding, Playable {
        override fun toString(): String = "Bidding: current bid = ${currentBid.points} ${currentBid.suit}"
    }

    data class Playing(
        override val selfPlayerId: String,
        override val players: Map<String, Player>,
        val currentPrimeSuit: Suit,
        val pastTricks: List<Trick>,
        val currentTrick: Trick,
    ): WithPlayers, Playable {
        override fun toString(): String {
            val playerStates = players.values.joinToString("\n") {
                "Player ${it.id}:\t${it.hand.certainCards}, maybe ${it.hand.canditateCards}"
            }
            val pastTricksState = pastTricks.joinToString("\n") { " - ${it.cards}" }
            return "Table: ${currentTrick.cards}\n$playerStates\nPast tricks:\n$pastTricksState"
        }
    }
}

data class Bid(val points: Int?, val suit: Suit)

data class Player(
    val id: String,
    val team: String,
    val hand: Hand,
)

data class Hand(
    val certainCards: Set<Card>,
    val canditateCards: Set<Card>,
) {
    override fun toString(): String = "$certainCards, and maybe $canditateCards"

    companion object {
        val Empty = Hand(
            certainCards = emptySet(),
            canditateCards = Suit.entries.flatMapTo(mutableSetOf()) { s ->
                Rank.entries.map { r -> Card(r, s) }
            }
        )
    }
}

data class Trick(
    val cards: List<Card>,
    val majorCard: PlayedCard? = null,
    val primeCard: PlayedCard? = null,
) {
    companion object {
        val Empty = Trick(emptyList())
    }
}

data class PlayedCard(val card: Card, val player: Player)
