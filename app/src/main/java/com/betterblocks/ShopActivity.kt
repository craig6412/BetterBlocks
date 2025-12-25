package com.betterblocks

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
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
import java.util.Locale
import androidx.core.graphics.toColorInt
import com.android.billingclient.api.ProductDetails



class ShopActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager
    private lateinit var shopRepo: ShopRepository
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

        // 1. Set System Bar Colors to match Game Theme (use KTX toColorInt)
        window.statusBarColor = "#1E214A".toColorInt() // DarkBackground
        window.navigationBarColor = "#1E214A".toColorInt()

        // 2. Initialize Billing Manager
        billingManager = BillingManager(
            this,
            lifecycleScope,
            onCoinsPurchased = { coinsPurchased ->
                // This callback runs when a purchase is SUCCESSFUL
                // Centralize updates through ShopRepository
                shopRepo.addCoins(coinsPurchased)
                purchasedCoins = coinsPurchased
                showPurchaseSuccessDialog = true
            }
        )
        billingManager.startConnection()

        // repository singleton
        shopRepo = ShopRepository.get(applicationContext)

        setContent {
            BetterBlocksTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    val productDetailsMap by billingManager.productDetailsMap.collectAsState()

                    ShopScreen(
                        productDetailsMap = productDetailsMap,
                        onBack = { finish() },
                        onPurchaseClick = { productId ->
                            billingManager.launchPurchaseFlow(this, productId)
                        },
                        onPowerUpPurchase = { id, cost -> handlePowerUpPurchase(id, cost) },
                        onTrophyPurchase = { tier, cost -> handleTrophyPurchase(tier, cost) }
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
        // Use central repo to update coins so all UIs stay in sync
        shopRepo.addCoins(amount)
    }

    /**
     * Handle purchasing power-ups (Rainbow Wipe, Color Wipe)
     */
    private fun handlePowerUpPurchase(itemType: String, cost: Int) {
        // Use repository to atomically check and apply purchase
        val success = shopRepo.useCoins(cost)
        if (success) {
            when (itemType) {
                "rainbow_wipe" -> {
                    shopRepo.addRainbowWipe(1)
                    purchasedItemName = "Rainbow Wipe"
                }
                "color_wipe" -> {
                    shopRepo.addColorWipe(1)
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
        val currentCoins = shopRepo.coins.value
        val lifetimeCoins = shopRepo.lifetimeCoins.value
        val currentTierOrdinal = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getInt(KEY_HIGHEST_TIER_UNLOCKED, TrophyTier.UNRANKED.ordinal)
        val currentTier = TrophyTier.fromOrdinalSafe(currentTierOrdinal)

        // Check if already owns this tier or higher
        if (currentTier.ordinal >= tier.ordinal) {
            // Already have this tier or better
            return
        }

        // COIN PURCHASES CAN SKIP TIERS - No sequential requirement!
        // You can buy Elite tier directly if you have 250,000 coins

        if (shopRepo.useCoins(cost)) {
            // update lifetime coins and unlock tier through repo + prefs
            shopRepo.recordLifetimeCoins(cost)
            shopRepo.unlockTier(tier.ordinal)
            shopRepo.addPremiumTier(tier.name)

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
    val imageResId: Int,
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
    productDetailsMap: Map<String, ProductDetails>,
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
        ShopItem("coins_small", "Stack of Coins", 3_000, R.drawable.shop_coins_small),
        ShopItem("coins_medium", "Sack of Coins", 25_000, R.drawable.shop_coins_medium, "POPULAR"),
        ShopItem("coins_large", "Chest of Coins", 100_000, R.drawable.shop_coins_large, "BEST VALUE"),
        ShopItem("coins_mega", "Mega Coin Hoard", 400_000, R.drawable.shop_coins_mega, "ELITE MODE")
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

    // Define Trophy Tier Items - show all tiers so user can purchase any
    val trophyItems = listOf(
        TrophyShopItem(
            tier = TrophyTier.PLATINUM,
            cost = 15625,
            description = "Unlock Platinum Trophy Tier"
        ),
        TrophyShopItem(
            tier = TrophyTier.DIAMOND,
            cost = 62500,
            description = "Unlock Diamond Trophy Tier"
        ),
        TrophyShopItem(
            tier = TrophyTier.ELITE,
            cost = 250000,
            description = "Unlock Elite Trophy Tier"
        )
    )

    // COMPACT SINGLE-PAGE DESIGN - NO SCROLLING
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(DarkBackground, DeepBlue)))
            .padding(WindowInsets.systemBars.asPaddingValues())
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        val config = LocalConfiguration.current
        val isTabletOrFold = config.screenWidthDp > 600 || config.screenHeightDp > 900

        val coinCardHeight = if (isTabletOrFold) 140.dp else 120.dp
        // make power-up boxes slightly taller (add 5.dp)
        val powerUpCardHeight = if (isTabletOrFold) 115.dp else 95.dp
        val sectionSpacing = if (isTabletOrFold) 10.dp else 8.dp
        val sectionTitlePaddingTop = if (isTabletOrFold) 8.dp else 4.dp

        Column(
            modifier = Modifier.fillMaxSize(),
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
                            String.format(Locale.getDefault(), "%,d", currentCoins),
                            color = GoldCoin,
                            fontFamily = Oswald,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(4.dp))

            // Make the remaining shop content scrollable when it doesn't fit on small screens.
            val shopScroll = rememberScrollState()
            Column(modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(shopScroll)
            ) {

                // SECTION 1: COIN PACKS (2x2 Grid) - Ultra Compact
                Spacer(Modifier.height(sectionTitlePaddingTop))
                Text("💎 COIN PACKS", color = LightText, fontFamily = Oswald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompactCoinCard(shopItems[0], onPurchaseClick, productDetailsMap, Modifier.weight(1f).height(coinCardHeight))
                        CompactCoinCard(shopItems[1], onPurchaseClick, productDetailsMap, Modifier.weight(1f).height(coinCardHeight))
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        CompactCoinCard(shopItems[2], onPurchaseClick, productDetailsMap, Modifier.weight(1f).height(coinCardHeight))
                        CompactCoinCard(shopItems[3], onPurchaseClick, productDetailsMap, Modifier.weight(1f).height(coinCardHeight))
                    }
                }

                Spacer(Modifier.height(sectionSpacing))

                // SECTION 2: POWER-UPS (1x2 Row) - Compact
                Text("⚡ POWER-UPS", color = LightText, fontFamily = Oswald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    CompactPowerUpCard(powerUpItems[0], currentCoins, onPowerUpPurchase, Modifier.weight(1f).height(powerUpCardHeight))
                    CompactPowerUpCard(powerUpItems[1], currentCoins, onPowerUpPurchase, Modifier.weight(1f).height(powerUpCardHeight))
                }

                Spacer(Modifier.height(sectionSpacing))

                // SECTION 3: TROPHY TIERS - Compact
                if (trophyItems.isNotEmpty()) {
                    Text("🏆 TROPHY TIERS", color = LightText, fontFamily = Oswald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        trophyItems.forEach { item ->
                            CompactTrophyCard(item, currentCoins, currentTier, onTrophyPurchase)
                        }
                    }
                }

            } // end scrollable Column
        }
    }
}

@Composable
fun CompactCoinCard(
    item: ShopItem,
    onPurchaseClick: (String) -> Unit,
    productDetailsMap: Map<String, ProductDetails>,
    modifier: Modifier = Modifier
) {
    val price = productDetailsMap[item.id]
        ?.oneTimePurchaseOfferDetails
        ?.formattedPrice
        ?: "—"

    Surface(
        modifier = modifier
            .clickable(onClick = { onPurchaseClick(item.id) }, role = Role.Button),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        color = DeepBlue.copy(alpha = 0.6f)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Image(
                painter = painterResource(item.imageResId),
                contentDescription = item.title,
                modifier = Modifier.size(42.dp),
                contentScale = ContentScale.Fit
            )

            Text(
                price,
                fontFamily = Oswald,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = LightText,
                maxLines = 1
            )

            Text(
                String.format(Locale.getDefault(), "%,d", item.coinAmount),
                fontFamily = Oswald,
                fontSize = 13.sp,
                color = GoldCoin,
                fontWeight = FontWeight.Bold
            )

            // removed badge and internal purchase Button so the full card is clickable
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

    Box(modifier = modifier) {
        val config = LocalConfiguration.current
        val tightVertical = config.screenHeightDp < 700

        // Adjust slightly if vertical space is tight so cost text always visible
        val internalPadding = if (tightVertical) 6.dp else 8.dp
        val titleFont = if (tightVertical) 11.sp else 12.sp
        val buttonHeight = if (tightVertical) 24.dp else 26.dp
        val buttonFont = if (tightVertical) 10.sp else 11.sp
        val priceFont = if (tightVertical) 11.sp else 12.sp

        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
            color = DeepBlue.copy(alpha = 0.6f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(internalPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(item.icon, fontSize = 24.sp)

                Text(
                    item.title,
                    fontFamily = Oswald,
                    fontSize = titleFont,
                    fontWeight = FontWeight.Bold,
                    color = LightText
                )

                // Explicit visible price line above the button so it's always readable
                Text(
                    text = String.format(Locale.getDefault(), "%,d", item.cost),
                    color = GoldCoin,
                    fontFamily = Oswald,
                    fontWeight = FontWeight.Bold,
                    fontSize = priceFont
                )

                Button(
                    onClick = { onPurchase(item.id, item.cost) },
                    enabled = canAfford,
                    modifier = Modifier.height(buttonHeight),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(String.format(Locale.getDefault(), "%,d", item.cost), fontSize = buttonFont)
                }
            }
        }
    }
}

@Composable
fun CompactTrophyCard(
    item: TrophyShopItem,
    currentCoins: Int,
    currentTier: TrophyTier,
    onPurchase: (TrophyTier, Int) -> Unit
) {
    val canAfford = currentCoins >= item.cost
    val alreadyOwned = currentTier.ordinal >= item.tier.ordinal
    val tierColor = trophyColorForTier(item.tier)

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, tierColor.copy(alpha = 0.5f)),
        color = DeepBlue.copy(alpha = 0.6f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    item.tier.name,
                    fontFamily = Oswald,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = tierColor
                )
                Text(
                    item.description,
                    fontSize = 10.sp,
                    color = LightText.copy(alpha = 0.8f)
                )
            }

            // Make price visible to the left of the purchase button
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = String.format(Locale.getDefault(), "%,d", item.cost),
                    color = GoldCoin,
                    fontFamily = Oswald,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(end = 8.dp)
                )

                Button(
                    onClick = { onPurchase(item.tier, item.cost) },
                    enabled = canAfford && !alreadyOwned,
                    // slightly taller to avoid text clipping on some devices
                    modifier = Modifier.height(34.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = tierColor)
                ) {
                    Text(
                        text = "UNLOCK",
                        fontSize = 11.sp,
                        color = Color.Black
                    )
                }
            }
        }
    }
}

// --- REMOVED LEGACY SHOP PAGES & LARGE CARDS ---
// The following legacy composables were intentionally removed to keep the
// Shop focused on the compact single-screen layout and to avoid unused
// top-level billing/price logic. This includes:
// - CoinPacksPage, PowerUpsPage, TrophyTiersPage
// - LargeCoinPackCard, LargePowerUpCard, LargeTrophyCard
// - Duplicate/legacy CompactCoinPackCard, CompactPowerUpCard, CompactTrophyCard
// - PowerUpCard, TrophyPurchaseCard, SeasonTimer, ShopItemCard
// If you need them restored, retrieve from version control.

// --- PREVIEW (For Android Studio) ---
@Preview(
    name = "Tablet – Portrait",
    showBackground = true,
    showSystemUi = true,
    device = "spec:width=800dp,height=1280dp,dpi=480"
)
@Composable
fun ShopScreenPreview() {
    // Use empty productDetailsMap for preview to avoid real billing calls
    BetterBlocksTheme {
        Surface(color = DarkBackground) {
            ShopScreen(
                productDetailsMap = emptyMap(),
                onBack = {},
                onPurchaseClick = { _ -> },
                onPowerUpPurchase = { _, _ -> /* preview no-op */ },
                onTrophyPurchase = { _, _ -> /* preview no-op */ }
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
