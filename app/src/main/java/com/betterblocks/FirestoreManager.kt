// ====================================================================
// REQUIRED FIRESTORE INDEXES (create these in Firebase Console)
//
// 1) For tier leaderboard (score DESC):
//
// Collection: leaderboards
// Fields:
//   trophyTier = Ascending
//   score      = Descending
//
// 2) For unranked leaderboard (score DESC):
//
// Collection: leaderboards
// Fields:
//   trophyTier = Ascending
//   score      = Descending
//
// If Firestore shows "needs index" error, click the console link
// ====================================================================

package com.betterblocks

import com.betterblocks.model.TrophyTier
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

object FirestoreManager {
    private val db = Firebase.firestore

    data class LeaderboardEntry(
        val userId: String = "",
        val score: Int = 0,
        val trophyTier: String = "",
        val updatedAt: com.google.firebase.Timestamp? = null
    )

    // ---------------------------
    // WRITE / UPDATE SCORE
    // ---------------------------
    fun updateLeaderboard(userId: String, score: Int, tier: TrophyTier) {
        try {
            val data = hashMapOf(
                "userId" to userId,
                "score" to score,
                "trophyTier" to tier.name,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("leaderboards")
                .document(userId)
                .set(data, SetOptions.merge())
        } catch (_: Exception) {
            // Swallow exceptions to ensure game never crashes from Firestore issues
        }
    }

    // ---------------------------
    // QUERY: Get leaderboard for a trophy tier
    // Ordered by score descending
    // Limit 100 players
    // ---------------------------
    fun getLeaderboardForTier(
        tier: TrophyTier,
        onResult: (List<LeaderboardEntry>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            db.collection("leaderboards")
                .whereEqualTo("trophyTier", tier.name)
                .orderBy("score", Query.Direction.DESCENDING)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        onResult(emptyList())
                        return@addSnapshotListener
                    }

                    val list = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(LeaderboardEntry::class.java)
                    }
                    onResult(list)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }

    // ---------------------------
    // QUERY: Get UNRANKED users
    // ---------------------------
    fun getUnranked(
        onResult: (List<LeaderboardEntry>) -> Unit,
        onError: (Exception) -> Unit
    ) {
        try {
            db.collection("leaderboards")
                .whereEqualTo("trophyTier", "UNRANKED")
                .orderBy("score", Query.Direction.DESCENDING)
                .orderBy("updatedAt", Query.Direction.DESCENDING)
                .limit(100)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        onError(error)
                        return@addSnapshotListener
                    }

                    if (snapshot == null) {
                        onResult(emptyList())
                        return@addSnapshotListener
                    }

                    val list = snapshot.documents.mapNotNull { it.toObject(LeaderboardEntry::class.java) }
                    onResult(list)
                }
        } catch (e: Exception) {
            onError(e)
        }
    }
}
