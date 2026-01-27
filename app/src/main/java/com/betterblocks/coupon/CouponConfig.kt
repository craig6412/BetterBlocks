package com.betterblocks.coupon

/**
 * Centralized coupon definitions.
 *
 * Codes are case-insensitive and should be normalized via: trim() + uppercase().
 */
object CouponConfig {
    val VALID_COUPON_CODES: Set<String> = setOf(
        "CRAIGUNGARO",
        "JOEATCHLEY",
        "TANYAUNGARO",
        "RANDYPETTERSON",
        "WAYNEGREEN",
        "KATHERINESMITH",
        "JACKIEUNGARO",
        "JACKIEREBARDI",
        "NANA"
    )

    const val COUPON_REWARD_COINS: Int = 100_000

    fun normalize(raw: String): String = raw.trim().uppercase()
}

