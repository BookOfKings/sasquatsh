package com.sasquatsh.app.views.billing

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sasquatsh.app.config.AppConfig
import com.sasquatsh.app.models.SubscriptionTier
import com.sasquatsh.app.viewmodels.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingView(
    authViewModel: AuthViewModel,
    onBack: () -> Unit
) {
    val authState by authViewModel.uiState.collectAsState()
    val currentTier = authState.user?.effectiveTier ?: SubscriptionTier.FREE
    val context = LocalContext.current
    var showSuccess by rememberSaveable { mutableStateOf(false) }
    var purchasedTierName by rememberSaveable { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pricing") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Text(
                text = "Simple, Transparent Pricing",
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Choose the plan that fits your game nights",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            // Free Tier
            TierCard(
                tierName = "Free",
                price = "$0",
                priceSubtitle = "forever",
                features = listOf(
                    "Host 1 game per event",
                    "Create 1 group",
                    "Basic event management",
                    "Join unlimited events"
                ),
                isPopular = false,
                isCurrent = currentTier == SubscriptionTier.FREE,
                buttonTitle = if (currentTier == SubscriptionTier.FREE) "Current Plan" else "Downgrade",
                isDisabled = currentTier == SubscriptionTier.FREE,
                onTap = {}
            )

            // Basic Tier
            TierCard(
                tierName = "Basic",
                price = "$4.99",
                priceSubtitle = "/month",
                features = listOf(
                    "Up to 5 games per event",
                    "Create up to 5 groups",
                    "1 recurring game per group",
                    "Table/room/hall locations",
                    "Game night planning",
                    "Event chat",
                    "No ads"
                ),
                isPopular = true,
                isCurrent = currentTier == SubscriptionTier.BASIC,
                buttonTitle = buttonTitle(currentTier, SubscriptionTier.BASIC),
                isDisabled = currentTier == SubscriptionTier.BASIC,
                onTap = {
                    // Open Stripe checkout in browser
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("${AppConfig.PRICING_URL}?tier=basic")
                    )
                    context.startActivity(intent)
                }
            )

            // Pro Tier
            TierCard(
                tierName = "Pro",
                price = "$7.99",
                priceSubtitle = "/month",
                features = listOf(
                    "Up to 10 games per event",
                    "Create up to 10 groups",
                    "Unlimited recurring games",
                    "Table/room/hall locations",
                    "Game night planning",
                    "Items to bring lists",
                    "Event chat",
                    "No ads"
                ),
                isPopular = false,
                isCurrent = currentTier == SubscriptionTier.PRO,
                buttonTitle = buttonTitle(currentTier, SubscriptionTier.PRO),
                isDisabled = currentTier == SubscriptionTier.PRO,
                onTap = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("${AppConfig.PRICING_URL}?tier=pro")
                    )
                    context.startActivity(intent)
                }
            )

            // Legal links
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://sasquatsh.com/terms")
                    )
                    context.startActivity(intent)
                }) {
                    Text(
                        text = "Terms of Service",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                TextButton(onClick = {
                    val intent = Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://sasquatsh.com/privacy")
                    )
                    context.startActivity(intent)
                }) {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = {
                showSuccess = false
                onBack()
            },
            title = { Text("Welcome to $purchasedTierName!") },
            text = { Text("Your subscription is now active. Enjoy your upgraded features!") },
            confirmButton = {
                TextButton(onClick = {
                    showSuccess = false
                    onBack()
                }) {
                    Text("Let's Go")
                }
            }
        )
    }
}

@Composable
private fun TierCard(
    tierName: String,
    price: String,
    priceSubtitle: String,
    features: List<String>,
    isPopular: Boolean,
    isCurrent: Boolean,
    buttonTitle: String,
    isDisabled: Boolean,
    onTap: () -> Unit
) {
    val borderColor = when {
        isCurrent -> MaterialTheme.colorScheme.primary
        isPopular -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.outlineVariant
    }
    val borderWidth = if (isCurrent || isPopular) 2.dp else 1.dp

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .border(borderWidth, borderColor, RoundedCornerShape(16.dp))
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = tierName,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (isPopular) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            text = "Most Popular",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
                if (isCurrent) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.tertiaryContainer
                    ) {
                        Text(
                            text = "Current",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // Price
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = price,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = priceSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }

            HorizontalDivider()

            // Features
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                features.forEach { feature ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // CTA Button
            Button(
                onClick = onTap,
                enabled = !isDisabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDisabled)
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    else
                        MaterialTheme.colorScheme.primary,
                    contentColor = if (isDisabled)
                        MaterialTheme.colorScheme.onSurfaceVariant
                    else
                        MaterialTheme.colorScheme.onPrimary,
                    disabledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) {
                Text(
                    text = buttonTitle,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

private fun buttonTitle(currentTier: SubscriptionTier, targetTier: SubscriptionTier): String {
    if (currentTier == targetTier) return "Current Plan"
    return if (currentTier.rank < targetTier.rank) "Upgrade" else "Downgrade"
}
