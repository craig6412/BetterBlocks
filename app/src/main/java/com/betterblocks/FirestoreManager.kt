package com.betterblocks

import android.content.Context
import android.util.Log
import com.betterblocks.model.TrophyTier
import com.betterblocks.model.getPlayerTier
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import android.content.SharedPreferences
import com.google.firebase.ktx.app

object FirestoreManager {

    private val db = Firebase.firestore
    private const val COLLECTION = "leaderboards"
    private lateinit var prefs: SharedPreferences

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun currentPlayerName(): String {
        return prefs.getString(KEY_PLAYER_NAME, "")?.takeIf { it.isNotBlank() } ?: ""
    }

    data class LeaderboardEntry(
        val userId: String = "",
        val score: Int = 0,
        val trophyTier: String = "",
        val updatedAt: Timestamp? = null,
        val updatedAt_fallback: Long = 0L,
        val playerName: String = ""
    )

    // ----------------------------------------------------
    // WRITE / UPDATE SCORE
    // ----------------------------------------------------
    fun updateLeaderboard(userId: String, score: Int, tier: TrophyTier, playerNameOverride: String? = null) {
        try {
            val now = System.currentTimeMillis()
            val playerName = playerNameOverride?.takeIf { it.isNotBlank() } ?: currentPlayerName()

            val data = hashMapOf(
                "userId" to userId,
                "score" to score,
                "trophyTier" to tier.name,
                "playerName" to playerName,
                "updatedAt" to FieldValue.serverTimestamp(),
                "updatedAt_fallback" to now
            )

            db.collection(COLLECTION)
                .document(userId)
                .set(data, SetOptions.merge())
                .addOnFailureListener {
                    Log.e("FirestoreManager", "Failed to update leaderboard", it)
                }

        } catch (e: Exception) {
            Log.e("FirestoreManager", "Exception in updateLeaderboard()", e)
        }
    }

    fun updateLeaderboardForCurrentPlayer(score: Int) {
        val userId = prefs.getString(KEY_FIREBASE_USER_ID, null) ?: return
        val lifetimeCoins = prefs.getInt(KEY_LIFETIME_COINS, 0)
        val tier = getPlayerTier(score, lifetimeCoins, prefs)
        updateLeaderboard(userId, score, tier)
    }

    // ----------------------------------------------------
    // GET LEADERBOARD BY TIER (One-Time Load)
    // ----------------------------------------------------
    fun getLeaderboardForTier(
        tier: TrophyTier,
        onResult: (List<LeaderboardEntry>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            db.collection(COLLECTION)
                .whereEqualTo("trophyTier", tier.name)
                .orderBy("score", Query.Direction.DESCENDING)
                .orderBy("updatedAt_fallback", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener { snapshot ->
                    val list = snapshot.documents.mapNotNull { it.toObject(LeaderboardEntry::class.java) }
                    onResult(list)
                }
                .addOnFailureListener { error ->
                    Log.e("FirestoreManager", "Tier leaderboard load failed", error)
                    onError(error)
                }

        } catch (e: Exception) {
            Log.e("FirestoreManager", "Exception in getLeaderboardForTier()", e)
            onError(e)
        }
    }

    // ----------------------------------------------------
    // GET UNRANKED PLAYERS (One-Time Load)
    // ----------------------------------------------------
    fun getUnranked(
        onResult: (List<LeaderboardEntry>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            db.collection(COLLECTION)
                .whereEqualTo("trophyTier", "UNRANKED")
                .orderBy("score", Query.Direction.DESCENDING)
                .orderBy("updatedAt_fallback", Query.Direction.DESCENDING)
                .limit(100)
                .get()
                .addOnSuccessListener { snapshot ->
                    val list = snapshot.documents.mapNotNull { it.toObject(LeaderboardEntry::class.java) }
                    onResult(list)
                }
                .addOnFailureListener { error ->
                    Log.e("FirestoreManager", "Unranked leaderboard load failed", error)
                    onError(error)
                }

        } catch (e: Exception) {
            Log.e("FirestoreManager", "Exception in getUnranked()", e)
            onError(e)
        }
    }
}
