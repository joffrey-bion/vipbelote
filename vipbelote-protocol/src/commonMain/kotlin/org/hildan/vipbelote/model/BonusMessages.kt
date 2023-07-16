package org.hildan.vipbelote.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*

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
    data class Challenge(
        val challengeViewModel: ViewModel,
        val currentProgress: Update,
        val levelsCompleted: Update,
    ) {
        @Serializable
        data class ViewModel(
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
            val rerollStatus: String,
        ) {
            @Serializable
            data class Game(
                val game: String,
            )

            @Serializable
            data class Reward(
                val cp: Long,
                val items: List<Item>,
            )
        }

        @Serializable
        data class Update(
            val previousValue: Long,
            val currentValue: Long,
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

@Serializable
data class PremiumGiftNotification(
    val giftReceiverId: String,
    val giftSenderId: String,
    val item: PremiumGiftItem,
    val senderProfile: Profile,
    val receiverProfile: Profile,
) : BonusMessage {
    @Serializable
    data class PremiumGiftItem(
        val item: Item,
        val vCurrency: Boolean,
        val group: String,
        val prices: Prices,
    ) {
        @Serializable
        data class Prices(
            val gems: Long,
        )
    }
}

@Serializable
data class Profile(
    val username: Username,
    val level: Int,
    val vip: VipSubscription,
    val location: Location,
    val inventory: Inventory,
    val id: String,
    val avatar: String,
    val status: String?,
    val age: Int?,
    val phone: String?,
    val sex: String,
    val isIdentityVerified: Boolean,
) {
    @Serializable
    data class Username(
        val currentUsername: String,
        val usernamesUsed: List<String>,
    )

    @Serializable
    data class VipSubscription(
        val id: String? = null,
        val expiresAt: Long? = null,
        val purchasedAt: Long? = null,
        val args: JsonObject? = null,
        val custom: JsonElement? = null,
    )

    @Serializable
    data class Location(
        val city: String? = null,
        val country: String? = null,
    )

    @Serializable
    data class Inventory(
        val items: List<InventoryItem>,
        val dirty: Boolean,
    ) {
        @Serializable
        data class InventoryItem(
            val item: Item,
            val equipped: Boolean? = null,
            val expiresAt: Long,
            val args: JsonObject? = null,
        )
    }
}

@Serializable
data class Item(
    val id: String,
    val type: String,
    val durability: String,
    val data: JsonObject,
    val lifeTime: Long? = null,
    val amount: Int? = null,
)
