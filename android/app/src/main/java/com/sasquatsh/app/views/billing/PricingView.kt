package com.sasquatsh.app.views.billing

import android.app.Activity
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sasquatsh.app.models.SubscriptionTier
import com.sasquatsh.app.viewmodels.AuthViewModel
import com.sasquatsh.app.viewmodels.BillingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PricingView(
    authViewModel: AuthViewModel,
    onBack: () -> Unit,
    billingViewModel: BillingViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    val billingState by billingViewModel.uiState.collectAsState()
    val currentTier = authState.user?.effectiveTier ?: SubscriptionTier.FREE
    val context = LocalContext.current
    val activity = context as? Activity
    var isAnnual by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        billingViewModel.loadGoogleProducts()
    }

    // Handle success message
    val showSuccess = billingState.successMessage != null
    val successTier = billingState.successMessage

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
        Box(modifier = Modifier.fillMaxSize()) {
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

                // Monthly / Annual toggle
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Monthly",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (!isAnnual) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Switch(
                        checked = isAnnual,
                        onCheckedChange = { isAnnual = it }
                    )
                    Text(
                        text = "Annual",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isAnnual) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (isAnnual) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Text(
                                text = "Save ~17%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

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
                    isLoading = false,
                    onTap = {}
                )

                // Basic Tier
                val basicProduct = billingViewModel.getProductForTier("basic", isAnnual)
                val basicPrice = basicProduct?.let { billingViewModel.getFormattedPrice(it) }
                    ?: if (isAnnual) "$49.99" else "$4.99"
                val basicSubtitle = if (isAnnual) "/year" else "/month"

                TierCard(
                    tierName = "Basic",
                    price = basicPrice,
                    priceSubtitle = basicSubtitle,
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
                    isDisabled = currentTier == SubscriptionTier.BASIC || billingState.purchaseInProgress,
                    isLoading = billingState.purchaseInProgress,
                    onTap = {
                        if (activity != null && basicProduct != null) {
                            val offerToken = basicProduct.subscriptionOfferDetails
                                ?.firstOrNull()?.offerToken ?: return@TierCard
                            billingViewModel.purchaseSubscription(activity, basicProduct, offerToken)
                        }
                    }
                )

                // Pro Tier
                val proProduct = billingViewModel.getProductForTier("pro", isAnnual)
                val proPrice = proProduct?.let { billingViewModel.getFormattedPrice(it) }
                    ?: if (isAnnual) "$79.99" else "$7.99"
                val proSubtitle = if (isAnnual) "/year" else "/month"

                TierCard(
                    tierName = "Pro",
                    price = proPrice,
                    priceSubtitle = proSubtitle,
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
                    isDisabled = currentTier == SubscriptionTier.PRO || billingState.purchaseInProgress,
                    isLoading = billingState.purchaseInProgress,
                    onTap = {
                        if (activity != null && proProduct != null) {
                            val offerToken = proProduct.subscriptionOfferDetails
                                ?.firstOrNull()?.offerToken ?: return@TierCard
                            billingViewModel.purchaseSubscription(activity, proProduct, offerToken)
                        }
                    }
                )

                // Restore purchases
                TextButton(
                    onClick = { billingViewModel.restorePurchases() },
                    enabled = !billingState.actionLoading
                ) {
                    Text(
                        text = "Restore Purchases",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Legal links
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Subscriptions automatically renew unless cancelled at least 24 hours before the end of the current period. Manage subscriptions in Google Play Store settings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        TextButton(onClick = {
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://sasquatsh.com/terms")
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
                            val intent = android.content.Intent(
                                android.content.Intent.ACTION_VIEW,
                                android.net.Uri.parse("https://sasquatsh.com/privacy")
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
                }

                Spacer(modifier = Modifier.height(20.dp))
            }

            // Error snackbar
            billingState.error?.let { error ->
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { billingViewModel.clearMessages() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(error)
                }
            }

            // Loading overlay
            if (billingState.purchaseInProgress) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.scrim.copy(alpha = 0.3f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Text(
                                text = "Processing purchase...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSuccess) {
        AlertDialog(
            onDismissRequest = {
                billingViewModel.clearMessages()
                onBack()
            },
            title = { Text("Subscription Active!") },
            text = { Text(successTier ?: "Your subscription is now active. Enjoy your upgraded features!") },
            confirmButton = {
                TextButton(onClick = {
                    billingViewModel.clearMessages()
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
    isLoading: Boolean,
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
