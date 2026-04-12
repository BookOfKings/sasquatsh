package com.sasquatsh.app.ui.billing

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillingScreen(
    viewModel: BillingViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Open checkout or portal URLs in Custom Tab
    LaunchedEffect(uiState.checkoutUrl) {
        uiState.checkoutUrl?.let {
            openCustomTab(context, it)
            viewModel.clearUrls()
        }
    }
    LaunchedEffect(uiState.portalUrl) {
        uiState.portalUrl?.let {
            openCustomTab(context, it)
            viewModel.clearUrls()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Billing") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
        ) {
            if (uiState.isLoading) {
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }

            // Current plan
            val tier = uiState.billingInfo?.subscriptionOverrideTier
                ?: uiState.billingInfo?.subscriptionTier
                ?: "free"

            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Current Plan", style = MaterialTheme.typography.titleMedium)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        tier.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (uiState.billingInfo?.cancelAtPeriodEnd == true) {
                        Text(
                            "Cancels at end of period",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Plan options
            Text("Available Plans", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(12.dp))

            PlanCard(
                name = "Basic",
                price = "$4.99/month",
                features = listOf("Unlimited events", "Group creation", "Planning sessions"),
                isCurrent = tier == "basic",
                onSelect = { viewModel.subscribe("price_1TGNutEcd2kvlye5IYI5yOMQ") },
            )

            Spacer(modifier = Modifier.height(8.dp))

            PlanCard(
                name = "Pro",
                price = "$7.99/month",
                features = listOf("Everything in Basic", "MTG deck builder", "Recurring games", "Priority support"),
                isCurrent = tier == "pro",
                onSelect = { viewModel.subscribe("price_1T791dEcd2kvlye5pFBFvlyu") },
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Manage billing
            if (uiState.billingInfo?.stripeCustomerId != null) {
                OutlinedButton(
                    onClick = { viewModel.openBillingPortal() },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Manage Billing")
                }
            }

            if (uiState.error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun PlanCard(
    name: String,
    price: String,
    features: List<String>,
    isCurrent: Boolean,
    onSelect: () -> Unit,
) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Text(price, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(8.dp))
            features.forEach { feature ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(feature, style = MaterialTheme.typography.bodySmall)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (isCurrent) {
                Text("Current plan", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            } else {
                Button(onClick = onSelect, modifier = Modifier.fillMaxWidth()) {
                    Text("Subscribe")
                }
            }
        }
    }
}

private fun openCustomTab(context: Context, url: String) {
    val intent = CustomTabsIntent.Builder().build()
    intent.launchUrl(context, Uri.parse(url))
}
