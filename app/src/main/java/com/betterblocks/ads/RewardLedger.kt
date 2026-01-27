package com.betterblocks.ads

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.betterblocks.PREFS_NAME
import com.betterblocks.ShopRepository

/**
 * Persistent, idempotent ledger for rewarded-ad coin grants.
 *
 * Goals:
 * - Only grant inside onUserEarnedReward.
 * - Prevent double-grants (idempotent per transactionId).
 * - Persist coins immediately before marking the transaction as granted.
 */
class RewardLedger private constructor(private val context: Context) {

    companion object {
        private const val TAG = "RewardLedger"

        private const val KEY_GRANTED_TX_IDS = "rewarded_granted_tx_ids_v1" // string set

        @Volatile private var INSTANCE: RewardLedger? = null

        fun get(context: Context): RewardLedger =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: RewardLedger(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val prefs: SharedPreferences =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // a single lock to ensure check+write+mark is atomic across threads
    private val lock = Any()

    /**
     * Attempts to grant coins for a given rewarded-ad transaction.
     *
     * Idempotent: if the transactionId has already been granted, returns false and does nothing.
     * Otherwise, it will:
     *  1) persist coins immediately
     *  2) then mark the transactionId as granted
     *
     * @return true if coins were granted now; false if already granted.
     */
    fun grantRewardOnce(
        transactionId: String,
        coinsToGrant: Int,
        adUnitType: String
    ): Boolean {
        if (transactionId.isBlank() || coinsToGrant <= 0) {
            Log.w(TAG, "grantRewardOnce: invalid args txId='$transactionId' coins=$coinsToGrant type=$adUnitType")
            return false
        }

        synchronized(lock) {
            val granted = prefs.getStringSet(KEY_GRANTED_TX_IDS, emptySet()) ?: emptySet()
            if (granted.contains(transactionId)) {
                Log.i(TAG, "grantRewardOnce: ALREADY_GRANTED txId=$transactionId type=$adUnitType")
                return false
            }

            // 1) persist coins immediately (synchronous commit)
            val repo = ShopRepository.get(context)
            repo.addCoins(coinsToGrant)
            repo.recordLifetimeCoins(coinsToGrant)

            // 2) mark transaction granted (synchronous commit)
            val newSet = granted.toMutableSet()
            newSet.add(transactionId)
            val ok = prefs.edit().putStringSet(KEY_GRANTED_TX_IDS, newSet).commit()

            Log.i(
                TAG,
                "grantRewardOnce: GRANTED txId=$transactionId coins=$coinsToGrant type=$adUnitType markOk=$ok"
            )

            return true
        }
    }

    fun wasGranted(transactionId: String): Boolean {
        if (transactionId.isBlank()) return false
        val granted = prefs.getStringSet(KEY_GRANTED_TX_IDS, emptySet()) ?: emptySet()
        return granted.contains(transactionId)
    }
}

