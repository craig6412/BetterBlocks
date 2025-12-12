package com.betterblocks

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import com.betterblocks.model.TrophyTier
import com.betterblocks.ui.*
import com.betterblocks.ui.theme.BetterBlocksTheme
import java.util.concurrent.TimeUnit




class ShopActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager
    private var showPurchaseSuccessDialog by mutableStateOf(false)
    private var purchasedCoins by mutableStateOf(0)

    // Power-up purchase dialogs
    private var showItemSuccessDialog by mutableStateOf(false)
    private var showNotEnoughCoinsDialog by mutableStateOf(false)
    private var purchasedItemName by mutableStateOf("")

    // Trophy purchase dialogs
    private var showTrophySuccessDialog by mutableStateOf(false)
    private var purchasedTrophyTier by mutableStateOf<TrophyTier?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Set System Bar Colors to match Game Theme
        window.statusBarColor = android.graphics.Color.parseColor("#1E214A") // DarkBackground
        window.navigationBarColor = android.graphics.Color.parseColor("#1E214A")

        // 2. Initialize Billing Manager
        billingManager = BillingManager(this, lifecycleScope) { coinsPurchased ->
            // This callback runs when a purchase is SUCCESSFUL
            addCoinsToUserAccount(coinsPurchased)
            purchasedCoins = coinsPurchased
            showPurchaseSuccessDialog = true
        }
        billingManager.startConnection()

        setContent {
            BetterBlocksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    ShopScreen(
                        onBack = { finish() },
                        onPurchaseClick = { productId ->
                            billingManager.launchPurchaseFlow(this, productId)
                        },
                        onPowerUpPurchase = { itemType, cost ->
                            handlePowerUpPurchase(itemType, cost)
                        },
                        onTrophyPurchase = { tier, cost ->
                            handleTrophyPurchase(tier, cost)
                        }
                    )

                    // IAP Purchase Success Dialog
                    if (!LocalInspectionMode.current && showPurchaseSuccessDialog) {
                        PurchaseSuccessDialog(
                            coinsAwarded = purchasedCoins,
                            onDismiss = {
                                showPurchaseSuccessDialog = false
                                purchasedCoins = 0
                            }
                        )
                    }

                    // Power-up Purchase Success Dialog
                    if (!LocalInspectionMode.current && showItemSuccessDialog) {
                        ItemPurchaseSuccessDialog(
                            itemName = purchasedItemName,
                            onDismiss = {
                                showItemSuccessDialog = false
                                purchasedItemName = ""
                            }
                        )
                    }

                    // Not Enough Coins Dialog
                    if (!LocalInspectionMode.current && showNotEnoughCoinsDialog) {
                        NotEnoughCoinsDialog(
                            onDismiss = { showNotEnoughCoinsDialog = false },
                            onGoToShop = {
                                showNotEnoughCoinsDialog = false
                                // Already in shop, do nothing
                            }
                        )
                    }

                    // Trophy Purchase Success Dialog
                    if (!LocalInspectionMode.current && showTrophySuccessDialog && purchasedTrophyTier != null) {
                        TrophyPurchaseSuccessDialog(
                            tier = purchasedTrophyTier!!,
                            onDismiss = {
                                showTrophySuccessDialog = false
                                purchasedTrophyTier = null
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::billingManager.isInitialized) {
            billingManager.endConnection()
        }
    }

    /**
     * Helper to manually save coins to SharedPreferences so they appear
     * when the user returns to the Game Activity.
     */
    private fun addCoinsToUserAccount(amount: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCoins = prefs.getInt(KEY_COINS, 100)
        val newBalance = currentCoins + amount
        prefs.edit().putInt(KEY_COINS, newBalance).apply()
    }

    /**
     * Handle purchasing power-ups (Rainbow Wipe, Color Wipe)
     */
    private fun handlePowerUpPurchase(itemType: String, cost: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCoins = prefs.getInt(KEY_COINS, 0)

        if (currentCoins >= cost) {
            // Deduct coins
            prefs.edit().putInt(KEY_COINS, currentCoins - cost).apply()

            // Add item to inventory
            when (itemType) {
                "rainbow_wipe" -> {
                    val currentCount = prefs.getInt(KEY_RAINBOW_COUNT, 0)
                    prefs.edit().putInt(KEY_RAINBOW_COUNT, currentCount + 1).apply()
                    purchasedItemName = "Rainbow Wipe"
                }
                "color_wipe" -> {
                    val currentCount = prefs.getInt(KEY_COLOR_WIPE_COUNT, 0)
                    prefs.edit().putInt(KEY_COLOR_WIPE_COUNT, currentCount + 1).apply()
                    purchasedItemName = "Color Wipe"
                }
            }

            showItemSuccessDialog = true
        } else {
            showNotEnoughCoinsDialog = true
        }
    }

    /**
     * Handle purchasing trophy tiers (Platinum, Diamond, Elite)
     * NOTE: Coin purchases can SKIP tiers (don't need sequential unlock)
     * Sequential unlock only applies to score-based progression
     */
    private fun handleTrophyPurchase(tier: TrophyTier, cost: Int) {
        val prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentCoins = prefs.getInt(KEY_COINS, 0)
        val lifetimeCoins = prefs.getInt(KEY_LIFETIME_COINS, 0)
        val currentTierOrdinal = prefs.getInt(KEY_HIGHEST_TIER_UNLOCKED, TrophyTier.UNRANKED.ordinal)
        val currentTier = TrophyTier.fromOrdinalSafe(currentTierOrdinal)

        // Check if already owns this tier or higher
        if (currentTier.ordinal >= tier.ordinal) {
            // Already have this tier or better
            return
        }

        // COIN PURCHASES CAN SKIP TIERS - No sequential requirement!
        // You can buy Elite tier directly if you have 250,000 coins

        if (currentCoins >= cost) {
            // Deduct coins
            prefs.edit().putInt(KEY_COINS, currentCoins - cost).apply()

            // Update lifetime coins
            val newLifetimeCoins = lifetimeCoins + cost
            prefs.edit().putInt(KEY_LIFETIME_COINS, newLifetimeCoins).apply()

            // Unlock tier (jump directly to purchased tier)
            prefs.edit().putInt(KEY_HIGHEST_TIER_UNLOCKED, tier.ordinal).apply()

            // Save to premium tiers list
            val premiumTiers = prefs.getString(KEY_PREMIUM_TIERS, "") ?: ""
            val tiersList = premiumTiers.split(",").filter { it.isNotBlank() }.toMutableSet()
            tiersList.add(tier.name)
            prefs.edit().putString(KEY_PREMIUM_TIERS, tiersList.joinToString(",")).apply()

            purchasedTrophyTier = tier
            showTrophySuccessDialog = true
        } else {
            showNotEnoughCoinsDialog = true
        }
    }
}

// --- SHOP DATA MODELS ---
data class ShopItem(
    val id: String,
    val title: String,
    val coinAmount: Int,
    val price: String,
    val imageResId: Int, // Uses your PNGs
    val badge: String? = null
)

data class PowerUpItem(
    val id: String,
    val title: String,
    val description: String,
    val cost: Int,
    val icon: String
)

data class TrophyShopItem(
    val tier: TrophyTier,
    val cost: Int,
    val description: String
)

// --- SHOP UI ---

@Composable
fun ShopScreen(
    onBack: () -> Unit,
    onPurchaseClick: (String) -> Unit,
    onPowerUpPurchase: (String, Int) -> Unit,
    onTrophyPurchase: (TrophyTier, Int) -> Unit
) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val currentCoins = prefs.getInt(KEY_COINS, 0)
    val currentTierOrdinal = prefs.getInt(KEY_HIGHEST_TIER_UNLOCKED, TrophyTier.UNRANKED.ordinal)
    val currentTier = TrophyTier.fromOrdinalSafe(currentTierOrdinal)


    // Define Shop Options
    val shopItems = listOf(
        ShopItem(
            id = "coins_small",
            title = "Stack of Coins",
            coinAmount = 3000,
            price = "$10.00",
            imageResId = R.drawable.shop_coins_small
        ),
        ShopItem(
            id = "coins_medium",
            title = "Sack of Coins",
            coinAmount = 25000,
            price = "$50.00",
            imageResId = R.drawable.shop_coins_medium,
            badge = "POPULAR"
        ),
        ShopItem(
            id = "coins_large",
            title = "Chest of Coins",
            coinAmount = 100000,
            price = "$100.00",
            imageResId = R.drawable.shop_coins_large,
            badge = "BEST VALUE"
        ),
        ShopItem(
            id = "coins_mega",
            title = "Mega Coin Hoard",
            coinAmount = 400000,
            price = "$400.00",
            imageResId = R.drawable.shop_coins_mega,
            badge = "Elite MODE"
        )
    )

    // Define Power-Up Items
    val powerUpItems = listOf(
        PowerUpItem(
            id = "rainbow_wipe",
            title = "Rainbow Wipe",
            description = "Clear the entire board",
            cost = 1000,
            icon = "🌈"
        ),
        PowerUpItem(
            id = "color_wipe",
            title = "Color Wipe",
            description = "Remove all blocks of one color",
            cost = 75,
            icon = "🎨"
        )
    )

    // Define Trophy Tier Items - ALWAYS SHOW ALL THREE
    val trophyItems = mutableListOf<TrophyShopItem>()

    if (currentTier.ordinal < TrophyTier.PLATINUM.ordinal) {
        trophyItems.add(TrophyShopItem(
            tier = TrophyTier.PLATINUM,
            cost = 15625,
            description = "Unlock Platinum Trophy Tier"
        ))
    }

    if (currentTier.ordinal < TrophyTier.DIAMOND.ordinal) {
        trophyItems.add(TrophyShopItem(
            tier = TrophyTier.DIAMOND,
            cost = 62500,
            description = "Unlock Diamond Trophy Tier"
        ))
    }

    if (currentTier.ordinal < TrophyTier.ELITE.ordinal) {
        trophyItems.add(TrophyShopItem(
            tier = TrophyTier.ELITE,
            cost = 250000,
            description = "Unlock Elite Trophy Tier"
        ))
    }

    // COMPACT SINGLE-PAGE DESIGN - NO SCROLLING
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBackground, DeepBlue)))
            .padding(WindowInsets.systemBars.asPaddingValues())
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // HEADER ROW - Back Button + Title + Coin Counter
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = LightText, modifier = Modifier.size(20.dp))
            }

            Text("SHOP", color = LightText, fontFamily = Oswald, fontSize = 20.sp, fontWeight = FontWeight.Bold)

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DeepBlue.copy(0.7f),
                modifier = Modifier.padding(end = 4.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("💰", fontSize = 14.sp)
                    Spacer(Modifier.width(4.dp))
                    Text(
                        String.format("%,d", currentCoins),
                        color = GoldCoin,
                        fontFamily = Oswald,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        // SECTION 1: COIN PACKS (2x2 Grid) - Ultra Compact
        Text("💎 COIN PACKS", color = LightText, fontFamily = Oswald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))

        Column(modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactCoinCard(shopItems[0], onPurchaseClick, Modifier.weight(1f))
                CompactCoinCard(shopItems[1], onPurchaseClick, Modifier.weight(1f))
            }
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactCoinCard(shopItems[2], onPurchaseClick, Modifier.weight(1f))
                CompactCoinCard(shopItems[3], onPurchaseClick, Modifier.weight(1f))
            }
        }

        Spacer(Modifier.height(8.dp))

        // SECTION 2: POWER-UPS (1x2 Row) - Compact
        Text("⚡ POWER-UPS", color = LightText, fontFamily = Oswald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            CompactPowerUpCard(powerUpItems[0], currentCoins, onPowerUpPurchase, Modifier.weight(1f))
            CompactPowerUpCard(powerUpItems[1], currentCoins, onPowerUpPurchase, Modifier.weight(1f))
        }

        Spacer(Modifier.height(8.dp))

        // SECTION 3: TROPHY TIERS - Compact
        if (trophyItems.isNotEmpty()) {
            Text("🏆 TROPHY TIERS", color = LightText, fontFamily = Oswald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                trophyItems.forEach { item ->
                    CompactTrophyCard(item, currentCoins, onTrophyPurchase)
                }
            }
        }
    }
}

// --- COMPACT CARD COMPONENTS (NO SCROLLING DESIGN) ---

@Composable
fun CompactCoinCard(item: ShopItem, onPurchase: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = { onPurchase(item.id) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(0.7f)),
        border = BorderStroke(2.dp, Pink_Jackie.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = modifier.height(115.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // NO BADGE TEXT - removed "POPULAR", "BEST VALUE", etc.

            // Coin Image - larger and more prominent
            Image(
                painterResource(item.imageResId),
                null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(42.dp) // Increased from 36dp
            )

            // Coin Amount - bigger and bolder
            Text(
                String.format("%,d", item.coinAmount),
                color = GoldCoin,
                fontSize = 16.sp, // Increased from 15sp
                fontWeight = FontWeight.ExtraBold,
                fontFamily = Oswald
            )

            // Price - clean and clear
            Text(
                item.price,
                color = SuccessGreen,
                fontSize = 15.sp, // Increased from 14sp
                fontWeight = FontWeight.ExtraBold,
                fontFamily = Oswald
            )
        }
    }
}

@Composable
fun CompactPowerUpCard(
    item: PowerUpItem,
    currentCoins: Int,
    onPurchase: (String, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val canAfford = currentCoins >= item.cost
    Card(
        onClick = { if (canAfford) onPurchase(item.id, item.cost) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(0.7f)),
        border = BorderStroke(2.dp, Pink_Jackie.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = modifier.height(110.dp)
    ) {
        Column(
            Modifier.fillMaxSize().padding(12.dp), // More padding
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Icon - scaled down by 30% (from 38sp to 27sp)
            Text(item.icon, fontSize = 27.sp) // Reduced from 38sp (30% smaller)

            // Title - slightly larger
            Text(
                item.title,
                color = LightText,
                fontSize = 14.sp, // Increased from 13sp
                fontWeight = FontWeight.ExtraBold, // Bolder
                fontFamily = Oswald,
                textAlign = TextAlign.Center
            )

            // Price - more prominent
            Text(
                "${String.format("%,d", item.cost)} 💰",
                color = if (canAfford) Pink_Jackie else Color.Gray,
                fontSize = 14.sp, // Increased from 13sp
                fontWeight = FontWeight.ExtraBold,
                fontFamily = Oswald
            )
        }
    }
}

@Composable
fun CompactTrophyCard(
    item: TrophyShopItem,
    currentCoins: Int,
    onPurchase: (TrophyTier, Int) -> Unit
) {
    val canAfford = currentCoins >= item.cost
    val trophyColor = trophyColorForTier(item.tier)

    Card(
        onClick = { if (canAfford) onPurchase(item.tier, item.cost) },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(0.8f)),
        border = BorderStroke(2.dp, Pink_Jackie.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth().height(55.dp) // Slightly taller
    ) {
        Row(
            Modifier.fillMaxSize().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.EmojiEvents,
                    null,
                    tint = trophyColor,
                    modifier = Modifier.size(32.dp) // Slightly larger
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    item.tier.name,
                    color = trophyColor,
                    fontSize = 15.sp, // Increased from 13sp
                    fontWeight = FontWeight.ExtraBold, // Bolder
                    fontFamily = Oswald
                )
            }

            // Price with better contrast
            Text(
                "${String.format("%,d", item.cost)} 💰",
                color = if (canAfford) trophyColor else Color.Gray,
                fontSize = 13.sp, // Increased from 10sp
                fontWeight = FontWeight.ExtraBold,
                fontFamily = Oswald
            )
        }
    }
}

// --- OLD PAGE COMPOSABLES (UNUSED) ---

@Composable
fun CoinPacksPage(
    shopItems: List<ShopItem>,
    onPurchaseClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 2x2 Grid with larger cards
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            shopItems.take(2).forEach { item ->
                LargeCoinPackCard(
                    item = item,
                    onPurchase = { onPurchaseClick(item.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            shopItems.drop(2).forEach { item ->
                LargeCoinPackCard(
                    item = item,
                    onPurchase = { onPurchaseClick(item.id) },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun PowerUpsPage(
    powerUpItems: List<PowerUpItem>,
    currentCoins: Int,
    onPurchase: (String, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        powerUpItems.forEach { item ->
            LargePowerUpCard(
                item = item,
                currentCoins = currentCoins,
                onPurchase = { onPurchase(item.id, item.cost) }
            )

            if (item != powerUpItems.last()) {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
fun TrophyTiersPage(
    trophyItems: List<TrophyShopItem>,
    currentCoins: Int,
    onPurchase: (TrophyTier, Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (trophyItems.isEmpty()) {
            // Show message if all trophies unlocked
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🏆",
                    fontSize = 64.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "All Trophies Unlocked!",
                    color = Pink_Jackie,
                    fontFamily = Oswald,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "You've reached the highest tier",
                    color = LightText.copy(alpha = 0.7f),
                    fontFamily = Oswald,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            trophyItems.forEach { item ->
                LargeTrophyCard(
                    item = item,
                    currentCoins = currentCoins,
                    onPurchase = { onPurchase(item.tier, item.cost) }
                )

                if (item != trophyItems.last()) {
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

// --- LARGE CARD COMPONENTS FOR PAGER PAGES ---

@Composable
fun LargeCoinPackCard(
    item: ShopItem,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasBadge = item.badge != null

    Card(
        onClick = onPurchase,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasBadge) DeepBlue.copy(alpha = 0.9f) else DeepBlue.copy(alpha = 0.6f)
        ),
        border = BorderStroke(2.dp, Pink_Jackie.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = modifier.height(180.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Badge
            if (hasBadge) {
                Text(
                    text = item.badge!!,
                    color = SuccessGreen,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald
                )
            } else {
                Spacer(modifier = Modifier.height(13.sp.value.dp))
            }

            // Coin Image
            Image(
                painter = painterResource(id = item.imageResId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(64.dp)
            )

            // Coin Amount
            Text(
                text = String.format("%,d", item.coinAmount),
                color = Pink_Jackie,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Oswald
            )

            // Price Button
            Surface(
                color = SuccessGreen,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = item.price,
                    color = LightText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun LargePowerUpCard(
    item: PowerUpItem,
    currentCoins: Int,
    onPurchase: () -> Unit
) {
    val canAfford = currentCoins >= item.cost

    Card(
        onClick = { if (canAfford) onPurchase() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.8f)),
        border = BorderStroke(2.dp, Pink_Jackie.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon and description
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.icon,
                    fontSize = 56.sp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = item.title,
                        color = LightText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = item.description,
                        color = LightText.copy(alpha = 0.7f),
                        fontSize = 14.sp,
                        fontFamily = Oswald
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Price
            Surface(
                color = if (canAfford) Pink_Jackie else Color.Gray,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "${String.format("%,d", item.cost)} 💰",
                    color = if (canAfford) DeepBlue else LightText.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
        }
    }
}

@Composable
fun LargeTrophyCard(
    item: TrophyShopItem,
    currentCoins: Int,
    onPurchase: () -> Unit
) {
    val canAfford = currentCoins >= item.cost
    val trophyColor = trophyColorForTier(item.tier)

    Card(
        onClick = { if (canAfford) onPurchase() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.85f)),
        border = BorderStroke(2.dp, Pink_Jackie.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Trophy Icon & Info
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = trophyColor,
                    modifier = Modifier
                        .size(56.dp)
                        .padding(end = 12.dp)
                )

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = item.tier.name,
                        color = trophyColor,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                    Text(
                        text = "Unlock ${item.tier.name} Tier",
                        color = LightText.copy(alpha = 0.7f),
                        fontSize = 13.sp,
                        fontFamily = Oswald
                    )
                }
            }

            // Price
            Surface(
                color = if (canAfford) trophyColor else Color.Gray,
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = "${String.format("%,d", item.cost)} 💰",
                    color = if (item.tier == TrophyTier.PLATINUM || item.tier == TrophyTier.DIAMOND)
                        DarkBackground else LightText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }
        }
    }
}

// Compact secondary components

@Composable
fun CompactCoinPackCard(
    item: ShopItem,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hasBadge = item.badge != null

    Card(
        onClick = onPurchase,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (hasBadge) DeepBlue.copy(alpha = 0.9f) else DeepBlue.copy(alpha = 0.6f)
        ),
        border = BorderStroke(1.5.dp, Pink_Jackie.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = modifier.height(100.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            if (hasBadge) {
                Text(
                    text = item.badge!!,
                    color = SuccessGreen,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald
                )
            } else {
                Spacer(modifier = Modifier.height(11.sp.value.dp))
            }

            Image(
                painter = painterResource(id = item.imageResId),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(32.dp)
            )

            Text(
                text = String.format("%,d", item.coinAmount),
                color = Pink_Jackie,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Oswald
            )

            Surface(
                color = SuccessGreen,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = item.price,
                    color = LightText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun CompactPowerUpCard(
    item: PowerUpItem,
    currentCoins: Int,
    onPurchase: () -> Unit,
    modifier: Modifier = Modifier
) {
    val canAfford = currentCoins >= item.cost

    Card(
        onClick = { if (canAfford) onPurchase() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.7f)),
        border = BorderStroke(1.5.dp, Pink_Jackie.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(4.dp),
        modifier = modifier.height(110.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(10.dp), // Increased from 8dp
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = item.icon,
                fontSize = 30.sp // Slightly smaller icon to make room
            )

            Text(
                text = item.title,
                color = LightText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = Oswald,
                textAlign = TextAlign.Center,
                maxLines = 1
            )

            Surface(
                color = if (canAfford) Pink_Jackie else Color.Gray,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${String.format("%,d", item.cost)} 💰",
                    color = if (canAfford) DeepBlue else LightText.copy(alpha = 0.5f),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), // Increased vertical padding
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun CompactTrophyCard(
    item: TrophyShopItem,
    currentCoins: Int,
    onPurchase: () -> Unit
) {
    val canAfford = currentCoins >= item.cost
    val trophyColor = trophyColorForTier(item.tier)

    Card(
        onClick = { if (canAfford) onPurchase() },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.8f)),
        border = BorderStroke(1.5.dp, Pink_Jackie.copy(alpha = 0.9f)),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth().height(70.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = trophyColor,
                    modifier = Modifier.size(40.dp)
                )

                Spacer(modifier = Modifier.width(10.dp))

                Column {
                    Text(
                        text = item.tier.name,
                        color = trophyColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                    Text(
                        text = "Unlock ${item.tier.name} Tier",
                        color = LightText.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontFamily = Oswald
                    )
                }
            }

            Surface(
                color = if (canAfford) trophyColor else Color.Gray,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "${String.format("%,d", item.cost)} 💰",
                    color = if (item.tier == TrophyTier.PLATINUM || item.tier == TrophyTier.DIAMOND)
                        DarkBackground else LightText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
    }
}

// --- LEGACY FULL-SIZE CARDS ---

@Composable
fun PowerUpCard(
    item: PowerUpItem,
    currentCoins: Int,
    onPurchase: () -> Unit
) {
    val canAfford = currentCoins >= item.cost
    val scale = remember { Animatable(1f) }

    Card(
        onClick = { if (canAfford) onPurchase() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, Pink_Jackie.copy(alpha = 0.9f)),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.6f)),
        elevation = CardDefaults.cardElevation(6.dp),
        modifier = Modifier.fillMaxWidth().height(90.dp).scale(scale.value)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon & Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = item.icon,
                    fontSize = 40.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = item.title,
                        color = LightText,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                    Text(
                        text = item.description,
                        color = LightText.copy(alpha = 0.6f),
                        fontSize = 12.sp,
                        fontFamily = Oswald
                    )
                }
            }

            // Price Button
            Surface(
                color = if (canAfford) Pink_Jackie else Color.Gray,
                shape = RoundedCornerShape(50),
                modifier = Modifier.wrapContentSize()
            ) {
                Text(
                    text = "${String.format("%,d", item.cost)} 💰",
                    color = if (canAfford) DeepBlue else LightText.copy(alpha = 0.5f),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun TrophyPurchaseCard(
    item: TrophyShopItem,
    currentCoins: Int,
    onPurchase: () -> Unit
) {
    val canAfford = currentCoins >= item.cost
    val trophyColor = trophyColorForTier(item.tier)

    Card(
        onClick = { if (canAfford) onPurchase() },
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(2.dp, Pink_Jackie.copy(alpha = 0.9f)),
        colors = CardDefaults.cardColors(containerColor = DeepBlue.copy(alpha = 0.8f)),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth().height(100.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Trophy Icon & Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = trophyColor,
                    modifier = Modifier
                        .size(56.dp)
                        .padding(end = 12.dp)
                )

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "${item.tier.name} TIER",
                        color = trophyColor,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                    Text(
                        text = item.description,
                        color = LightText.copy(alpha = 0.7f),
                        fontSize = 12.sp,
                        fontFamily = Oswald
                    )
                }
            }

            // Price Button
            Surface(
                color = if (canAfford) trophyColor else Color.Gray,
                shape = RoundedCornerShape(50),
                modifier = Modifier.wrapContentSize()
            ) {
                Text(
                    text = "${String.format("%,d", item.cost)} 💰",
                    color = if (item.tier == TrophyTier.PLATINUM || item.tier == TrophyTier.DIAMOND)
                        DarkBackground else LightText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
fun SeasonTimer() {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // Get or create season start date
    val seasonStartDate = prefs.getLong("season_start_date", 0L)
    val currentTime = System.currentTimeMillis()

    if (seasonStartDate == 0L) {
        // First time - set season start date
        prefs.edit().putLong("season_start_date", currentTime).apply()
    }

    val actualStartDate = if (seasonStartDate == 0L) currentTime else seasonStartDate

    // Calculate season end (1 year from start)
    val oneYearInMillis = 365L * 24 * 60 * 60 * 1000
    val seasonEndDate = actualStartDate + oneYearInMillis
    val timeRemaining = seasonEndDate - currentTime

    val daysRemaining = TimeUnit.MILLISECONDS.toDays(timeRemaining)
    val hoursRemaining = TimeUnit.MILLISECONDS.toHours(timeRemaining) % 24

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DeepBlue.copy(alpha = 0.4f)
        ),
        border = BorderStroke(1.dp, SpecialPurple.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "⏰ Season End Countdown",
                color = SpecialPurple,
                fontFamily = Oswald,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (daysRemaining > 0) {
                    "$daysRemaining days, $hoursRemaining hours remaining"
                } else {
                    "Season Ended - New Season Starting!"
                },
                color = LightText.copy(alpha = 0.8f),
                fontFamily = Oswald,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun ShopItemCard(item: ShopItem, onPurchase: () -> Unit) {
    val isSpecial = item.badge != null
    val border = BorderStroke(2.dp, Pink_Jackie.copy(alpha = 0.9f))
    val background = if (isSpecial) DeepBlue.copy(alpha = 0.8f) else DeepBlue.copy(alpha = 0.4f)

    Card(
        onClick = onPurchase,
        shape = RoundedCornerShape(16.dp),
        border = border,
        colors = CardDefaults.cardColors(containerColor = background),
        elevation = CardDefaults.cardElevation(8.dp),
        modifier = Modifier.fillMaxWidth().height(100.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Icon & Details
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Display the PNG Image (ensure these exist in drawable)
                // If crashing, replace 'item.imageResId' with a placeholder like R.drawable.ic_launcher_foreground
                Image(
                    painter = painterResource(id = item.imageResId),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(64.dp)
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(verticalArrangement = Arrangement.Center) {
                    Text(
                        text = "${String.format("%,d", item.coinAmount)} Coins",
                        color = LightText,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = Oswald
                    )
                    if (item.badge != null) {
                        Text(
                            text = item.badge,
                            color = SuccessGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = Oswald
                        )
                    } else {
                        Text(
                            text = item.title,
                            color = LightText.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            fontFamily = Oswald
                        )
                    }
                }
            }

            // Price Button
            Surface(
                color = if (isSpecial) Pink_Jackie else SuccessGreen,
                shape = RoundedCornerShape(50),
                modifier = Modifier.wrapContentSize()
            ) {
                Text(
                    text = item.price,
                    color = if (isSpecial) DeepBlue else LightText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = Oswald,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

// --- PREVIEW (For Android Studio) ---
@Preview(
    name = "Tablet – Portrait",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=800dp,height=1280dp,dpi=480"
)
@Composable
fun ShopScreenPreview() {
    val vm = PreviewShopViewModel()
    BetterBlocksTheme {
        Surface(color = DarkBackground) {
            ShopScreen(
                onBack = {},
                onPurchaseClick = { _ -> vm.onPurchase("") },
                onPowerUpPurchase = { _, _ -> },
                onTrophyPurchase = { _, _ -> }
            )
        }
    }
}

// --- DIALOG COMPONENTS ---

@Composable
fun ItemPurchaseSuccessDialog(
    itemName: String,
    onDismiss: () -> Unit
) {
    var scaleState by remember { mutableStateOf(0.8f) }

    LaunchedEffect(Unit) {
        scaleState = 1f
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .scale(scaleState),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = DeepBlue
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DarkBackground, DeepBlue)
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Success Icon
                    Text(
                        text = "✨",
                        fontSize = 64.sp
                    )

                    Text(
                        text = "Purchase Successful!",
                        fontFamily = Oswald,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = SuccessGreen,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "You purchased 1x $itemName",
                        fontFamily = Oswald,
                        fontSize = 18.sp,
                        color = LightText.copy(alpha = 0.9f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SuccessGreen
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Awesome!",
                            fontFamily = Oswald,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NotEnoughCoinsDialog(
    onDismiss: () -> Unit,
    onGoToShop: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = DeepBlue
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DarkBackground, DeepBlue)
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Warning Icon
                    Text(
                        text = "⚠️",
                        fontSize = 64.sp
                    )

                    Text(
                        text = "Not Enough Coins",
                        fontFamily = Oswald,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B6B),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "You don't have enough coins for this purchase. Buy more coin packs below!",
                        fontFamily = Oswald,
                        fontSize = 16.sp,
                        color = LightText.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = DeepBlue
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Got It",
                            fontFamily = Oswald,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = LightText,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TrophyPurchaseSuccessDialog(
    tier: TrophyTier,
    onDismiss: () -> Unit
) {
    val scale = remember { Animatable(0.5f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .scale(scale.value),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = DeepBlue
            ),
            elevation = CardDefaults.cardElevation(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(DarkBackground, DeepBlue)
                        )
                    )
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Glowing Trophy
                    GlowingTrophy(tier)

                    Text(
                        text = "Trophy Unlocked!",
                        fontFamily = Oswald,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = LightText,
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = tier.name,
                        fontFamily = Oswald,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = trophyColorForTier(tier),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "You've been promoted to ${tier.name} tier!",
                        fontFamily = Oswald,
                        fontSize = 16.sp,
                        color = LightText.copy(alpha = 0.8f),
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = trophyColorForTier(tier)
                        ),
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Amazing!",
                            fontFamily = Oswald,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (tier == TrophyTier.PLATINUM || tier == TrophyTier.DIAMOND)
                                DarkBackground else LightText,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

