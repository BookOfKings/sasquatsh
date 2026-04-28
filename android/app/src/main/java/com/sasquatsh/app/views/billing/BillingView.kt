package com.sasquatsh.app.views.billing

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sasquatsh.app.config.AppConfig
import com.sasquatsh.app.models.InvoiceStatus
import com.sasquatsh.app.models.SubscriptionTier
import com.sasquatsh.app.viewmodels.AuthViewModel
import com.sasquatsh.app.viewmodels.BillingViewModel
import com.sasquatsh.app.views.profile.SubscriptionBadge
import com.sasquatsh.app.views.shared.BadgeView
import com.sasquatsh.app.views.shared.D20SpinnerView
import com.sasquatsh.app.views.shared.ErrorBannerView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingView(
    authViewModel: AuthViewModel,
    onNavigateToPricing: () -> Unit,
    onBack: () -> Unit,
    billingViewModel: BillingViewModel = hiltViewModel()
) {
    val uiState by billingViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showCancelConfirm by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        billingViewModel.loadBillingInfo()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Billing") },
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
        if (uiState.isLoading && uiState.subscriptionInfo == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                D20SpinnerView(size = 40.dp, modifier = Modifier.size(40.dp))
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Error banner
                uiState.error?.let { error ->
                    ErrorBannerView(
                        message = error,
                        onDismiss = { billingViewModel.clearMessages() }
                    )
                }

                // Success message
                uiState.successMessage?.let { msg ->
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Current Plan Card
                CurrentPlanCard(
                    billingViewModel = billingViewModel,
                    authViewModel = authViewModel,
                    onNavigateToPricing = onNavigateToPricing,
                    onCancelClick = { showCancelConfirm = true }
                )

                // View All Plans
                Card(
                    onClick = onNavigateToPricing,
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "View All Plans",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Payment Method Card (Stripe only)
                if (uiState.isStripeSubscription || (!uiState.isGoogleSubscription)) {
                    PaymentMethodCard(billingViewModel = billingViewModel)
                    InvoiceHistoryCard(billingViewModel = billingViewModel)
                }
            }
        }
    }

    // Cancel confirmation
    if (showCancelConfirm) {
        AlertDialog(
            onDismissRequest = { showCancelConfirm = false },
            title = { Text("Cancel Subscription") },
            text = {
                Text("Your subscription will remain active until the end of the current billing period.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        billingViewModel.cancelSubscription(context)
                        showCancelConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancel Subscription")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelConfirm = false }) {
                    Text("Keep Subscription")
                }
            }
        )
    }
}

@Composable
private fun CurrentPlanCard(
    billingViewModel: BillingViewModel,
    authViewModel: AuthViewModel,
    onNavigateToPricing: () -> Unit,
    onCancelClick: () -> Unit
) {
    val uiState by billingViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val effectiveTier = authState.user?.effectiveTier ?: uiState.currentTier
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Current Plan",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                SubscriptionBadge(tier = effectiveTier)
            }

            // Price
            Text(
                text = effectiveTier.priceLabel,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.primary
            )

            // Source
            if (uiState.isGoogleSubscription) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Managed by Google Play",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (uiState.isStripeSubscription) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Managed by Stripe",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Override indicator
            if (uiState.hasOverride) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Text(
                        text = "Admin override active",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Features checklist
            val features = tierFeatures(effectiveTier)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                features.forEach { feature ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = feature,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            HorizontalDivider()

            // Action buttons
            if (uiState.isGoogleSubscription) {
                OutlinedButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            uiState.manageGoogleSubscriptionUri
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ShoppingCart,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Manage in Google Play")
                }
            } else if (effectiveTier == SubscriptionTier.FREE && !uiState.hasOverride) {
                Button(
                    onClick = onNavigateToPricing,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Upgrade Plan")
                }
            } else if (uiState.isCancelled) {
                Button(
                    onClick = { billingViewModel.reactivateSubscription() },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    enabled = !uiState.actionLoading
                ) {
                    Text("Reactivate Subscription")
                }

                uiState.subscriptionInfo?.subscription?.expiresAt?.let { expiresAt ->
                    Text(
                        text = "Access until ${billingViewModel.formattedDate(expiresAt)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (uiState.hasActiveSubscription && uiState.isStripeSubscription) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onNavigateToPricing,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Change Plan")
                    }
                    TextButton(
                        onClick = onCancelClick,
                        enabled = !uiState.actionLoading,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Cancel")
                    }
                }
            }

            // Past due warning
            if (uiState.isPastDue) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Payment past due -- please update your payment method",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun PaymentMethodCard(billingViewModel: BillingViewModel) {
    val uiState by billingViewModel.uiState.collectAsState()
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            val pm = uiState.subscriptionInfo?.paymentMethod
            if (pm != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CreditCard,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Column {
                        Text(
                            text = "${billingViewModel.formatCardBrand(pm.brand)} ****${pm.last4}",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Text(
                            text = "Expires ${pm.expMonth}/${pm.expYear}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                OutlinedButton(
                    onClick = {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(AppConfig.BILLING_URL)
                        )
                        context.startActivity(intent)
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Manage")
                }
            } else {
                Text(
                    text = "No payment method on file",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InvoiceHistoryCard(billingViewModel: BillingViewModel) {
    val uiState by billingViewModel.uiState.collectAsState()

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Invoice History",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (uiState.invoices.isEmpty()) {
                Text(
                    text = "No invoices yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else {
                uiState.invoices.forEach { invoice ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = billingViewModel.formattedDate(invoice.invoiceDate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = billingViewModel.formattedAmount(invoice.amountCents),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        InvoiceStatusBadge(status = invoice.status)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                if (uiState.hasMoreInvoices) {
                    TextButton(
                        onClick = { billingViewModel.loadMoreInvoices() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Load More",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InvoiceStatusBadge(status: InvoiceStatus) {
    val (text, color) = when (status) {
        InvoiceStatus.PAID -> "Paid" to MaterialTheme.colorScheme.primaryContainer
        InvoiceStatus.OPEN -> "Open" to MaterialTheme.colorScheme.tertiaryContainer
        InvoiceStatus.DRAFT -> "Draft" to MaterialTheme.colorScheme.surfaceContainerHigh
        InvoiceStatus.VOID -> "Void" to MaterialTheme.colorScheme.errorContainer
        InvoiceStatus.UNCOLLECTIBLE -> "Uncollectible" to MaterialTheme.colorScheme.errorContainer
    }
    BadgeView(text = text, color = color)
}

private fun tierFeatures(tier: SubscriptionTier): List<String> {
    return when (tier) {
        SubscriptionTier.FREE -> listOf(
            "Host 1 game per event",
            "Create 1 group",
            "Basic event management",
            "Join unlimited events"
        )
        SubscriptionTier.BASIC -> listOf(
            "Up to 5 games per event",
            "Create up to 5 groups",
            "1 recurring game per group",
            "Table/room/hall locations",
            "Game night planning",
            "Event chat",
            "No ads"
        )
        SubscriptionTier.PRO -> listOf(
            "Up to 10 games per event",
            "Create up to 10 groups",
            "Unlimited recurring games",
            "Table/room/hall locations",
            "Game night planning",
            "Items to bring lists",
            "Event chat",
            "No ads"
        )
        SubscriptionTier.PREMIUM -> listOf(
            "All Pro features",
            "Priority support",
            "Custom branding"
        )
    }
}
