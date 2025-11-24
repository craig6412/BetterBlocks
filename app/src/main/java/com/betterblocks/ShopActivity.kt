package com.betterblocks

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.betterblocks.ui.CoinGold
import com.betterblocks.ui.DarkBackground
import com.betterblocks.ui.DeepBlue
import com.betterblocks.ui.LightText
import com.betterblocks.ui.Oswald
import com.betterblocks.ui.SuccessGreen
import com.betterblocks.ui.theme.BetterBlocksTheme

class ShopActivity : ComponentActivity() {

    private lateinit var billingManager: BillingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Set System Bar Colors to match Game Theme
        window.statusBarColor = android.graphics.Color.parseColor("#1E214A") // DarkBackground
        window.navigationBarColor = android.graphics.Color.parseColor("#1E214A")

        // 2. Initialize Billing Manager
        billingManager = BillingManager(this, lifecycleScope) { coinsPurchased ->
            // This callback runs when a purchase is SUCCESSFUL
            addCoinsToUserAccount(coinsPurchased)
            Toast.makeText(this, "Success! Added $coinsPurchased coins.", Toast.LENGTH_LONG).show()
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
                        }
                    )
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
}

// --- SHOP DATA MODEL ---
data class ShopItem(
    val id: String,
    val title: String,
    val coinAmount: Int,
    val price: String,
    val imageResId: Int, // Uses your PNGs
    val badge: String? = null
)

// --- SHOP UI ---

@Composable
fun ShopScreen(
    onBack: () -> Unit,
    onPurchaseClick: (String) -> Unit
) {
    // Define Shop Options (Updated Pricing)
    val shopItems = listOf(
        ShopItem(
            id = "coins_small",
            title = "Stack of Coins",
            coinAmount = 1000,
            price = "$10.00",
            imageResId = R.drawable.shop_coins_small
        ),
        ShopItem(
            id = "coins_medium",
            title = "Sack of Coins",
            coinAmount = 7500,
            price = "$50.00",
            imageResId = R.drawable.shop_coins_medium,
            badge = "POPULAR"
        ),
        ShopItem(
            id = "coins_large",
            title = "Chest of Coins",
            coinAmount = 15000,
            price = "$100.00",
            imageResId = R.drawable.shop_coins_large,
            badge = "BEST VALUE"
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- Header ---
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp)
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = LightText,
                    modifier = Modifier.size(32.dp)
                )
            }

            Text(
                text = "SHOP",
                color = LightText,
                fontFamily = Oswald,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        Text(
            text = "Restock your coins to keep the game going!",
            color = LightText.copy(alpha = 0.7f),
            fontFamily = Oswald,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // --- Shop Options List ---
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            shopItems.forEach { item ->
                ShopItemCard(item) {
                    onPurchaseClick(item.id)
                }
            }
        }
    }
}

@Composable
fun ShopItemCard(item: ShopItem, onPurchase: () -> Unit) {
    val isSpecial = item.badge != null
    val border = if (isSpecial) BorderStroke(2.dp, CoinGold) else BorderStroke(1.dp, DeepBlue)
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
                color = if (isSpecial) CoinGold else SuccessGreen,
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
@Preview(showBackground = true)
@Composable
fun ShopScreenPreview() {
    BetterBlocksTheme {
        Surface(color = DarkBackground) {
            // Empty lambda for preview actions
            ShopScreen(onBack = {}, onPurchaseClick = {})
        }
    }
}