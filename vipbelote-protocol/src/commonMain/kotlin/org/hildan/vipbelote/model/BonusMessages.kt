package org.hildan.vipbelote.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
sealed interface BonusMessage : VipBeloteMessage

@Serializable
data class WalletChipsUpdate(val amount: Int) : BonusMessage

@Serializable
data class CashbackBonusUpdate(val custom: Custom) : BonusMessage {

    @Serializable
    data class Custom(val accumulatedAmount: Int)
}

@Serializable
data class BonusesUpdated(val cashback: Cashback) : BonusMessage {

    @Serializable
    data class Cashback(
        val isReady: Boolean,
        val isActive: Boolean,
        val reward: Reward,
        val requirements: Requirements,
        val custom: Custom,
    ) {
        @Serializable
        data class Reward(
            val current: CurrencyAmounts,
            val all: List<CurrencyAmounts>,
        ) {
            @Serializable
            data class CurrencyAmounts(
                val chips: Long,
                val gems: Long,
                val items: List<JsonElement>, // TODO
            )
        }

        @Serializable
        data class Requirements(val timeToNextBonus: Long)

        @Serializable
        data class Custom(
            val accumulatedAmount: Long,
            val tier: String,
            val multiplier: Double,
            val lastCollected: Long,
            val currentBonusLevel: Long,
            val nextBonusLevel: Long,
            val maxAccumulatedAmountReachedAt: Long,
            val tierLevelChangeAt: Long,
            val nextBonusAt: Long,
            val timers: Timers,
            val cashbackUnlockers: CashbackUnlockers,
        ) {
            @Serializable
            data class Timers(
                val tierLevelChangeInterval: Long,
                val resetInterval: Long,
                val decayNotificationAt: Long,
                val purchaseCooldownStartsAt: Long,
            )

            @Serializable
            data class CashbackUnlockers(
                val defaultUnlockerItemId: String,
                val decayUnlockerItemId: String,
            )
        }
    }
}

@Serializable
data class ChallengeProgressUpdated(val challenges: List<Challenge>) : BonusMessage {

    @Serializable
    data class Challenge (
        val challengeViewModel: ViewModel,
        val currentProgress: Update,
        val levelsCompleted: Update
    ) {
        @Serializable
        data class ViewModel (
            val id: String,
            val categoryId: String,
            val games: List<Game>,
            val type: String,
            val progressType: String,
            val breakpoints: List<Long>,
            val rewards: List<Reward>,
            val levelsCompleted: Long,
            val takenRewards: Long,
            val currentProgress: Long,
            val completedAt: Long,
            val expiresAt: Long,
            val lockStatus: String,
            val rerollStatus: String
        ) {
            @Serializable
            data class Game(
                val game: String
            )

            @Serializable
            data class Reward(
                val cp: Long,
                val items: List<Item>
            ) {
                @Serializable
                data class Item (
                    val id: String,
                    val type: String,
                    val durability: String,
                    val data: Data,
                    val amount: Long
                ) {
                    @Serializable
                    data class Data(
                        val clientPreviewUrl: String
                    )
                }
            }
        }

        @Serializable
        data class Update (
            val previousValue: Long,
            val currentValue: Long
        )
    }
}

@Serializable
data class XPUpdated(
    val level: Long,
    val previousLevelAt: Long,
    val nextLevelAt: Long,
    val xp: Long,
    val previousXp: Long,
) : BonusMessage
