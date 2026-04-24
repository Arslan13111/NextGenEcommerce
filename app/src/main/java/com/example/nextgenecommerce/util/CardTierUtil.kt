package com.example.nextgenecommerce.util

object CardTierUtil {

    enum class CardTier(
        val label: String,
        val cashbackPercent: Double,
        val minSpend: Double,
        val displayMinSpend: String
    ) {
        SILVER("Silver", 5.0, 0.0, "Rs 0"),
        GOLD("Gold", 8.0, 25_000.0, "Rs 25,000"),
        PLATINUM("Platinum", 12.0, 75_000.0, "Rs 75,000")
    }

    fun getTierForSpend(totalSpent: Double): CardTier = when {
        totalSpent >= CardTier.PLATINUM.minSpend -> CardTier.PLATINUM
        totalSpent >= CardTier.GOLD.minSpend -> CardTier.GOLD
        else -> CardTier.SILVER
    }

    fun computeDiscount(subtotal: Double, tier: CardTier): Double =
        subtotal * (tier.cashbackPercent / 100.0)

    fun nextTier(tier: CardTier): CardTier? = when (tier) {
        CardTier.SILVER -> CardTier.GOLD
        CardTier.GOLD -> CardTier.PLATINUM
        CardTier.PLATINUM -> null
    }

    fun progressToNext(totalSpent: Double, tier: CardTier): Float {
        val next = nextTier(tier) ?: return 1f
        val base = tier.minSpend
        return ((totalSpent - base) / (next.minSpend - base)).toFloat().coerceIn(0f, 1f)
    }

    fun amountToNext(totalSpent: Double, tier: CardTier): Double? {
        val next = nextTier(tier) ?: return null
        return (next.minSpend - totalSpent).coerceAtLeast(0.0)
    }

    fun formatAmount(amount: Double): String {
        val intPart = amount.toLong()
        return if (intPart >= 1000) {
            val thousands = intPart / 1000
            val remainder = intPart % 1000
            if (remainder == 0L) "Rs $thousands,000" else "Rs $intPart"
        } else {
            "Rs $intPart"
        }
    }
}
