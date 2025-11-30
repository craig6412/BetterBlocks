package com.betterblocks

import android.content.Context
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.tasks.await

object AuthManager {
    private const val KEY_USER_ID = "firebase_user_id"

    suspend fun getOrCreateUserId(context: Context): String {

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        val cached = prefs.getString(KEY_USER_ID, null)
        if (cached != null) return cached

        val auth = FirebaseAuth.getInstance()

        val user = auth.currentUser ?: auth.signInAnonymously().await().user
        val uid = user?.uid ?: "UNKNOWN_USER"

        prefs.edit().putString(KEY_USER_ID, uid).apply()

        return uid
    }
}

