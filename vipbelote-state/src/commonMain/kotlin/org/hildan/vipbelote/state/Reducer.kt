package org.hildan.vipbelote.state

import kotlinx.coroutines.flow.*
import org.hildan.vipbelote.protocol.messages.*
import kotlin.contracts.*

fun Flow<GameMessage>.states(selfPlayerId: String = HildanPlayerId): Flow<GameState> =
    scan<GameMessage, GameState>(GameState.Initial(selfPlayerId)) { s, msg -> s.updatedWith(msg) }

fun GameState.updatedWith(msg: GameMessage) = when (msg) {
    // expected to be first
    is GameStarted -> {
        ensureIs<GameState.Initial>()
        startedGameState(msg, selfPlayerId)
    }
    is ResumeGame -> resumedGameState(msg, selfPlayerId)

    is CardsDealt -> ensureIs<GameState.Bidding>().withSelfCards(msg.cards.toSet())
    is BidOptions -> this // bid options events don't bring useful info
    is PlayerBid -> ensureIs<GameState.WithPlayers>().withBid(msg)

    is PlayOptions -> this // ignore
    is CardPlayed -> ensureIs<GameState.Playable>().updateWithPlay(msg)

    is PlayerDeclared -> this // TODO update "certain" cards when BELOTE is declared

    is EndOfTrick -> ensureIs<GameState.Playing>().nextTrick()
    is EndOfRound -> this // ignore
    is NewRound -> ensureIs<GameState.WithPlayers>().newRound(msg.startPlayerId)


    is AvailableDeclarations -> this // ignore "available options" events, we know what we have

    is TopCardShown -> this // doesn't change anything

    is BidAction,
    is PlayCardAction -> this // ignore self actions that trigger a similar generic event anyway

    is ComeBack,
    is ReactionMessage -> this // ignore side player behaviours

    is JustAPlayer16,
    is JustAPlayer18,
    is JustAPlayer19,
    is UnknownGameMessageIn,
    is UnknownGameMessageOut -> this // ignore unknown
}

@OptIn(ExperimentalContracts::class)
private inline fun <reified T : GameState> GameState.ensureIs(): T {
    contract {
        returns() implies (this@ensureIs is T)
    }
    if (this !is T) {
        throw IllegalStateException("Game state should be of type ${T::class.simpleName} at this point, got ${this::class.simpleName}")
    }
    return this
}

private fun startedGameState(gameStartEvent: GameStarted, selfPlayerId: String): GameState.BeforeBid {
    val playerIds = gameStartEvent.playerPositions.keys.sortedBy { gameStartEvent.playerPositions[it] }
    val players = playerIds.associateWith {
        Player(id = it, team = gameStartEvent.playerTeams.getValue(it), hand = Hand.Empty)
    }
    return GameState.BeforeBid(
        selfPlayerId = selfPlayerId,
        players = players,
        startPlayerId = gameStartEvent.startPlayerId,
    )
}

private fun resumedGameState(resumeGame: ResumeGame, selfPlayerId: String): GameState {
    val gameSaveData = resumeGame.gameSaveData
    val players = gameSaveData.gameViewModel.players.associate {
        it.id to Player(
            id = it.id,
            team = it.teamId,
            hand = Hand.Empty.withCertainCards(it.cards.filterNotNullTo(mutableSetOf()))
        )
    }

    val announceData = gameSaveData.gameViewModel.announce.announceData
    val bidType = announceData.announceType
    val bid = if (bidType == 1000) null else Bid(announceData.points, Suit.entries.first { it.serializedCode == bidType })
    return when(gameSaveData.fromStateName) {
        "Announce" -> when {
            bid == null -> GameState.BeforeBid(
                selfPlayerId = selfPlayerId,
                players = players,
                startPlayerId = gameSaveData.startPlayerId,
            )
            else -> GameState.WithBid(
                selfPlayerId = selfPlayerId,
                players = players,
                currentBid = bid,
            )
        }
        "CardPlay" -> GameState.Playing(
            selfPlayerId = selfPlayerId,
            players = players,
            currentPrimeSuit = bid?.suit ?: error("no bid available in CardPlay state: $gameSaveData"),
            pastTricks = listOfNotNull(gameSaveData.gameViewModel.lastTrick?.toTrick(players)),
            currentTrick = gameSaveData.gameViewModel.table.toTrick(players),
        )
        else -> error("Unknown state '${gameSaveData.fromStateName}'")
    }
}

private fun org.hildan.vipbelote.protocol.messages.Trick.toTrick(players: Map<String, Player>): Trick {
    return Trick(
        cards = cards,
        majorCard = majorCard?.let { PlayedCard(it, players.getValue(majorCardOwner?.id!!)) },
        primeCard = primeCard?.let { PlayedCard(it, players.getValue(primeCardOwner?.id!!)) },
    )
}

private fun GameState.WithPlayers.newRound(startPlayerId: String): GameState.BeforeBid = GameState.BeforeBid(
    startPlayerId = startPlayerId,
    selfPlayerId = selfPlayerId,
    players = players.mapValues { (_, p) -> p.copy(hand = Hand.Empty) },
)

private fun GameState.WithPlayers.withBid(playerBid: PlayerBid): GameState.WithPlayers {
    val bidPoints = playerBid.points
    // 1000 always means PASS
    // In classic belote, points=null but announceType gives the suit
    if (playerBid.announceType == 1000) {
        return this // pass
    }
    val bid = Bid(bidPoints, Suit.entries.first { it.serializedCode == playerBid.announceType })
    return GameState.WithBid(selfPlayerId, players, bid)
}

private fun GameState.WithBid.startPlaying(): GameState.Playing = GameState.Playing(
    selfPlayerId = selfPlayerId,
    players = players,
    currentPrimeSuit = currentBid.suit,
    pastTricks = emptyList(),
    currentTrick = Trick.Empty,
)

private fun GameState.Playable.updateWithPlay(msg: CardPlayed): GameState.Playing {
    val playingState = when (this) {
        is GameState.WithBid -> startPlaying()
        is GameState.Playing -> this
    }
    return playingState.updateWithPlay(msg)
}

private fun GameState.Playing.updateWithPlay(msg: CardPlayed): GameState.Playing {
    val currentPrime = currentPrimeSuit
    val player = players.getValue(msg.playerId)
    val newCard = msg.card

    return copy(
        currentTrick = currentTrick.withNewCard(PlayedCard(newCard, player), currentPrime),
        players = players.mapValues { (id, p) ->
            if (id == player.id) p.afterPlaying(newCard, currentTrick, currentPrime) else p.withoutCandidate(newCard)
        }.refresh(),
    )
}

private fun Map<String, Player>.refresh(): Map<String, Player> {
    return mapValues { (id, p) ->
        val otherPlayers = values.filter { it.id != id }
        val otherCertain = otherPlayers.flatMapTo(mutableSetOf()) { it.hand.certainCards }
        val otherCandidates = otherPlayers.flatMapTo(mutableSetOf()) { it.hand.canditateCards }
        p.copy(hand = p.hand.refresh(otherCertain, otherCandidates))
    }
}

private fun Hand.refresh(otherCertain: Set<Card>, otherCandidates: Set<Card>): Hand {
    // if nobody else has a card in their candidates, we can be sure that this player has it
    val nowCertain = canditateCards - otherCandidates
    return copy(
        certainCards = certainCards + nowCertain,
        canditateCards = canditateCards - otherCertain - nowCertain,
    )
}

private fun Player.afterPlaying(newCard: Card, previousTrickState: Trick, prime: Suit): Player {
    val pWithoutPlayedCard = withoutPlayedCard(newCard)
    val firstPlay = previousTrickState.cards.firstOrNull()
        ?: return pWithoutPlayedCard // this is the first played card, we can't deduce anything certainly

    val requestedSuit = firstPlay.suit
    if (newCard.suit == requestedSuit) {
        // when the player provided the requested suit, there is nothing more we can say
        return pWithoutPlayedCard
    }

    val pWithoutRequestedSuit = pWithoutPlayedCard.withoutCandidateIf { it.suit == requestedSuit }
    val currentTopPrimeCard = previousTrickState.primeCard

    val majorCard = previousTrickState.majorCard
        ?: error("a card has been played, so there should be an existing major card")
    val winningCard = currentTopPrimeCard ?: majorCard
    val winningTeam = winningCard.player.team
    val isPartnerWinning = winningTeam == team

    return when {
        isPartnerWinning -> pWithoutRequestedSuit // we can't know more
        newCard.suit == prime -> when {
            currentTopPrimeCard == null -> pWithoutRequestedSuit
            newCard.rank.primeRank > currentTopPrimeCard.card.rank.primeRank -> pWithoutRequestedSuit
            else -> pWithoutRequestedSuit.withoutCandidateIf { it.suit == prime && it.rank.primeRank >= currentTopPrimeCard.card.rank.primeRank }
        }
        else -> pWithoutPlayedCard.withoutCandidateIf { it.suit == prime } // no more primes
    }
}

private fun GameState.Playing.nextTrick() = copy(pastTricks = pastTricks + currentTrick, currentTrick = Trick(emptyList()))

private fun Trick.withNewCard(newCard: PlayedCard, currentPrime: Suit): Trick {
    val newMajor = majorOf(majorCard, newCard)
    val newPrime = primeOf(primeCard, newCard, primeSuit = currentPrime)
    return copy(
        cards = cards + newCard.card,
        majorCard = newMajor,
        primeCard = newPrime,
    )
}

private fun majorOf(currentMajor: PlayedCard?, newCard: PlayedCard): PlayedCard = when {
    currentMajor == null -> newCard
    currentMajor.card.suit == newCard.card.suit && currentMajor.card.rank < newCard.card.rank -> newCard
    else -> currentMajor
}

private fun primeOf(currentPrime: PlayedCard?, newCard: PlayedCard, primeSuit: Suit): PlayedCard? = when {
    newCard.card.suit != primeSuit -> currentPrime
    currentPrime == null -> newCard
    currentPrime.card.rank < newCard.card.rank -> newCard
    else -> currentPrime
}

private fun GameState.Bidding.withSelfCards(cards: Set<Card>): GameState.Bidding {
    val newPlayers = players.mapValues { (id, p) ->
        if (id == selfPlayerId) {
            p.withCertainCards(cards)
        } else {
            p.withoutCandidates(cards)
        }
    }
    return when (this) {
        is GameState.BeforeBid -> copy(players = newPlayers)
        is GameState.WithBid -> copy(players = newPlayers)
    }
}

private fun Player.withCertainCards(cards: Set<Card>): Player = copy(hand = hand.withCertainCards(cards))

private fun Hand.withCertainCards(cards: Set<Card>): Hand {
    val newCertainCards = certainCards + cards
    return copy(
        certainCards = newCertainCards,
        canditateCards = if (newCertainCards.size == 8) emptySet() else canditateCards - cards,
    )
}

private fun Player.withoutPlayedCard(card: Card): Player = copy(hand = hand.withoutPlayedCard(card))

private fun Hand.withoutPlayedCard(card: Card): Hand {
    if (card !in certainCards && card !in canditateCards) {
        error("Playing $card but it was not in the candidates nor the certain cards from this hand")
    }
    return copy(
        certainCards = certainCards - card,
        canditateCards = canditateCards - card,
    )
}

private fun Player.withoutCandidate(card: Card): Player = withoutCandidates(setOf(card))

private fun Player.withoutCandidates(cards: Set<Card>): Player = copy(hand = hand.withoutCandidateIf { it in cards })

private fun Player.withoutCandidateIf(predicate: (Card) -> Boolean): Player = copy(hand = hand.withoutCandidateIf(predicate))

private fun Hand.withoutCandidateIf(predicate: (Card) -> Boolean): Hand {
    if (certainCards.any(predicate)) {
        error("we thought ${certainCards.first(predicate)} was certain, but we want to rule it out now")
    }
    return copy(
        certainCards = certainCards,
        canditateCards = canditateCards.filterNotTo(mutableSetOf(), predicate),
    )
}
