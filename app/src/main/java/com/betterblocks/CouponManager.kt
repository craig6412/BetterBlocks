package com.betterblocks

import android.content.Context
import android.util.Log

/**
 * Handles one-time-use coupon codes.
 *
 * Storage: best-effort per-device persistence via SharedPreferences.
 */
object CouponManager {

    const val COUPON_REWARD_COINS = 100_000

    private const val TAG = "COUPON"
    private const val PREFS_NAME = "coupon_prefs"
    private const val USED_COUPON_PREFIX = "used_coupon_"

    private val VALID_COUPONS = setOf(
        "CraigUngaro",
        "Nana",
        "JoeAtchley",
        "TanyaUngaro",
        "RandyPeterson",
        "WayneGreen",
        "TonjaGreen",
        "JackieRebardi"
    )

    sealed class CouponResult {
        data class Success(val coinsGranted: Int) : CouponResult()
        data object Invalid : CouponResult()
        data object AlreadyUsed : CouponResult()
    }

    /**
     * Applies [code] if valid and unused.
     *
     * - Whitespace is trimmed.
     * - Match is case-sensitive (exact).
     * - One-time use is enforced per device via SharedPreferences.
     */
    fun applyCoupon(context: Context, code: String, repo: ShopRepository): CouponResult {
        val trimmed = code.trim()
        Log.d(TAG, "applyCoupon pressed: code='$trimmed'")

        if (trimmed.isBlank() || trimmed !in VALID_COUPONS) {
            Log.d(TAG, "applyCoupon result: Invalid")
            return CouponResult.Invalid
        }

        val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val usedKey = "$USED_COUPON_PREFIX$trimmed"

        // best-effort atomicity: lock in-process and commit synchronously
        synchronized(this) {
            if (prefs.getBoolean(usedKey, false)) {
                Log.d(TAG, "applyCoupon result: AlreadyUsed")
                return CouponResult.AlreadyUsed
            }

            val wrote = prefs.edit().putBoolean(usedKey, true).commit()
            if (!wrote) {
                // If we can't persist, don't grant (avoid allowing repeats / inconsistency).
                Log.w(TAG, "Failed to persist coupon usage; not granting coins")
                return CouponResult.AlreadyUsed
            }

            repo.addCoins(COUPON_REWARD_COINS)
            Log.d(TAG, "applyCoupon result: Success (+$COUPON_REWARD_COINS)")
            return CouponResult.Success(COUPON_REWARD_COINS)
        }
    }
}

