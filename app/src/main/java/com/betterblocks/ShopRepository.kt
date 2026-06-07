package com.betterblocks

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Centralized repository for coins, rainbow wipes, and color wipe counts.
 * Writes to the same SharedPreferences used elsewhere so existing code that
 * listens to SharedPreferences (e.g., GameViewModel) will remain in sync.
 */
class ShopRepository private constructor(context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: ShopRepository? = null

        private const val STARTER_COINS = 100
        private const val STARTER_RAINBOW_WIPES = 5
        private const val STARTER_COLOR_WIPES = 10

        fun get(context: Context): ShopRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: ShopRepository(context.applicationContext).also { INSTANCE = it }
            }
    }

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // simple lock for synchronizing multi-step read/validate/write operations
    private val lock = Any()

    init {
        ensureFreshInstallStarterInventory()
    }

    private val _coins = MutableStateFlow(prefs.getInt(KEY_COINS, STARTER_COINS))
    val coins: StateFlow<Int> = _coins.asStateFlow()

    private val _rainbow = MutableStateFlow(prefs.getInt(KEY_RAINBOW_COUNT, STARTER_RAINBOW_WIPES))
    val rainbowWipes: StateFlow<Int> = _rainbow.asStateFlow()

    private val _color = MutableStateFlow(prefs.getInt(KEY_COLOR_WIPE_COUNT, STARTER_COLOR_WIPES))
    val colorWipes: StateFlow<Int> = _color.asStateFlow()

    private val _lifetime = MutableStateFlow(prefs.getInt(KEY_LIFETIME_COINS, prefs.getInt(KEY_COINS, STARTER_COINS)))
    val lifetimeCoins: StateFlow<Int> = _lifetime.asStateFlow()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        when (key) {
            KEY_COINS -> _coins.value = prefs.getInt(KEY_COINS, STARTER_COINS)
            KEY_RAINBOW_COUNT -> _rainbow.value = prefs.getInt(KEY_RAINBOW_COUNT, STARTER_RAINBOW_WIPES)
            KEY_COLOR_WIPE_COUNT -> _color.value = prefs.getInt(KEY_COLOR_WIPE_COUNT, STARTER_COLOR_WIPES)
            KEY_LIFETIME_COINS -> _lifetime.value = prefs.getInt(KEY_LIFETIME_COINS, prefs.getInt(KEY_COINS, STARTER_COINS))
        }
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener(listener)
    }

    /**
     * Guarantees the intended starter inventory for true fresh installs in both
     * debug and release builds:
     *
     * - 100 coins
     * - 5 rainbow wipes
     * - 10 color wipes / color wheels
     *
     * This also repairs the bad fresh-install state where all three inventory values
     * already exist as 0 after first launch. Existing players with progress are left
     * untouched.
     */
    private fun ensureFreshInstallStarterInventory() {
        synchronized(lock) {
            val hasCoins = prefs.contains(KEY_COINS)
            val hasRainbow = prefs.contains(KEY_RAINBOW_COUNT)
            val hasColorWipe = prefs.contains(KEY_COLOR_WIPE_COUNT)

            val allStarterKeysMissing = !hasCoins && !hasRainbow && !hasColorWipe

            val allStarterValuesZero =
                prefs.getInt(KEY_COINS, 0) == 0 &&
                        prefs.getInt(KEY_RAINBOW_COUNT, 0) == 0 &&
                        prefs.getInt(KEY_COLOR_WIPE_COUNT, 0) == 0

            val hasPlayerProgress =
                prefs.getInt(KEY_HIGH_SCORE, 0) > 0 ||
                        prefs.getInt(KEY_SAVED_SCORE, 0) > 0 ||
                        prefs.getInt(KEY_LIFETIME_COINS, 0) > 0 ||
                        prefs.contains(KEY_SAVED_BOARD)

            val looksLikeBadFreshInstall = allStarterValuesZero && !hasPlayerProgress

            if (allStarterKeysMissing || looksLikeBadFreshInstall) {
                prefs.edit()
                    .putInt(KEY_COINS, STARTER_COINS)
                    .putInt(KEY_RAINBOW_COUNT, STARTER_RAINBOW_WIPES)
                    .putInt(KEY_COLOR_WIPE_COUNT, STARTER_COLOR_WIPES)
                    .putInt(KEY_LIFETIME_COINS, STARTER_COINS)
                    // Fresh installs should not receive the old one-time update gift on top.
                    .putBoolean(KEY_UPDATE_GIFTS_APPLIED, true)
                    .commit()
            }
        }
    }

    fun addCoins(amount: Int) {
        synchronized(lock) {
            val new = prefs.getInt(KEY_COINS, STARTER_COINS) + amount
            prefs.edit().putInt(KEY_COINS, new).commit()
            _coins.value = new
        }
    }

    /**
     * Try to spend coins. Returns true if there were enough coins and write succeeded.
     */
    fun useCoins(amount: Int): Boolean {
        synchronized(lock) {
            val current = prefs.getInt(KEY_COINS, STARTER_COINS)
            if (current < amount) return false
            val new = current - amount
            val ok = prefs.edit().putInt(KEY_COINS, new).commit()
            if (ok) _coins.value = new
            return ok
        }
    }

    /** Set absolute coin balance (synchronous) */
    fun setCoins(new: Int) {
        synchronized(lock) {
            prefs.edit().putInt(KEY_COINS, new).commit()
            _coins.value = new
        }
    }

    fun addRainbowWipe(count: Int = 1) {
        synchronized(lock) {
            val new = prefs.getInt(KEY_RAINBOW_COUNT, STARTER_RAINBOW_WIPES) + count
            prefs.edit().putInt(KEY_RAINBOW_COUNT, new).commit()
            _rainbow.value = new
        }
    }

    fun addColorWipe(count: Int = 1) {
        synchronized(lock) {
            val new = prefs.getInt(KEY_COLOR_WIPE_COUNT, STARTER_COLOR_WIPES) + count
            prefs.edit().putInt(KEY_COLOR_WIPE_COUNT, new).commit()
            _color.value = new
        }
    }

    fun setRainbowCount(count: Int) {
        synchronized(lock) {
            prefs.edit().putInt(KEY_RAINBOW_COUNT, count).commit()
            _rainbow.value = count
        }
    }

    fun setColorWipeCount(count: Int) {
        synchronized(lock) {
            prefs.edit().putInt(KEY_COLOR_WIPE_COUNT, count).commit()
            _color.value = count
        }
    }

    fun recordLifetimeCoins(amount: Int) {
        synchronized(lock) {
            val new = prefs.getInt(KEY_LIFETIME_COINS, prefs.getInt(KEY_COINS, STARTER_COINS)) + amount
            prefs.edit().putInt(KEY_LIFETIME_COINS, new).commit()
            _lifetime.value = new
        }
    }

    fun setLifetimeIfHigher(totalCoins: Int) {
        synchronized(lock) {
            val previous = prefs.getInt(KEY_LIFETIME_COINS, prefs.getInt(KEY_COINS, STARTER_COINS))
            if (totalCoins > previous) {
                prefs.edit().putInt(KEY_LIFETIME_COINS, totalCoins).commit()
                _lifetime.value = totalCoins
            }
        }
    }

    fun unlockTier(tierOrdinal: Int) {
        synchronized(lock) {
            prefs.edit().putInt(KEY_HIGHEST_TIER_UNLOCKED, tierOrdinal).commit()
        }
    }

    fun addPremiumTier(tierName: String) {
        synchronized(lock) {
            val current = prefs.getString(KEY_PREMIUM_TIERS, "") ?: ""
            val set = current.split(",").filter { it.isNotBlank() }.toMutableSet()
            set.add(tierName)
            prefs.edit().putString(KEY_PREMIUM_TIERS, set.joinToString(",")).commit()
        }
    }

    fun close() {
        try { prefs.unregisterOnSharedPreferenceChangeListener(listener) } catch (_: Throwable) {}
    }
}
