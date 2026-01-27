package com.betterblocks.coupon

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.betterblocks.PREFS_NAME
import com.betterblocks.ShopRepository

/**
 * Thread-safe, persistent ledger for coupon redemptions.
 *
 * - Redeem codes only once per device.
 * - Idempotent: repeated redemption attempts must not double grant.
 */
class CouponLedger private constructor(private val context: Context) {

    sealed class Result {
        data object Success : Result()
        data object Invalid : Result()
        data object AlreadyRedeemed : Result()
    }

    companion object {
        private const val TAG = "CouponLedger"

        // Stored as a StringSet of normalized codes.
        private const val KEY_REDEEMED_CODES_V1 = "redeemed_coupon_codes_v1"

        @Volatile private var INSTANCE: CouponLedger? = null

        fun get(context: Context): CouponLedger =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: CouponLedger(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val lock = Any()

    fun isRedeemed(code: String): Boolean {
        val normalized = CouponConfig.normalize(code)
        if (normalized.isBlank()) return false
        val set = prefs.getStringSet(KEY_REDEEMED_CODES_V1, emptySet()) ?: emptySet()
        return set.contains(normalized)
    }

    fun markRedeemed(code: String) {
        val normalized = CouponConfig.normalize(code)
        if (normalized.isBlank()) return
        synchronized(lock) {
            val existing = prefs.getStringSet(KEY_REDEEMED_CODES_V1, emptySet()) ?: emptySet()
            if (existing.contains(normalized)) return
            val next = existing.toMutableSet().apply { add(normalized) }
            prefs.edit().putStringSet(KEY_REDEEMED_CODES_V1, next).commit()
        }
    }

    /**
     * Redeems a coupon code.
     *
     * Order is important for stability:
     * - Check validity
     * - Under lock: check already redeemed -> grant coins (sync) -> mark redeemed (sync)
     */
    fun redeem(rawCode: String): Result {
        val code = CouponConfig.normalize(rawCode)
        if (code.isBlank()) return Result.Invalid

        if (!CouponConfig.VALID_COUPON_CODES.contains(code)) {
            Log.d(TAG, "redeem: invalid code='$code'")
            return Result.Invalid
        }

        synchronized(lock) {
            val redeemed = prefs.getStringSet(KEY_REDEEMED_CODES_V1, emptySet()) ?: emptySet()
            if (redeemed.contains(code)) {
                Log.d(TAG, "redeem: already redeemed code='$code'")
                return Result.AlreadyRedeemed
            }

            // Grant coins immediately using existing persistence.
            val repo = ShopRepository.get(context)
            repo.addCoins(CouponConfig.COUPON_REWARD_COINS)
            repo.recordLifetimeCoins(CouponConfig.COUPON_REWARD_COINS)

            // Persist redeemed state synchronously so double taps/recomposition can't re-grant.
            val next = redeemed.toMutableSet().apply { add(code) }
            val ok = prefs.edit().putStringSet(KEY_REDEEMED_CODES_V1, next).commit()

            Log.i(TAG, "redeem: success code='$code' coins=${CouponConfig.COUPON_REWARD_COINS} markOk=$ok")
            return Result.Success
        }
    }
}

