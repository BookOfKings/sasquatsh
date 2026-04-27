package com.sasquatsh.app.views.shared

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Backpack
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sasquatsh.app.config.AppConfig
import com.sasquatsh.app.models.SubscriptionTier

enum class LimitType(
    val title: String,
    val message: String,
    val icon: ImageVector
) {
    GAMES(
        title = "Game Limit Reached",
        message = "You've reached the maximum number of games for your current plan.",
        icon = Icons.Outlined.SportsEsports
    ),
    GROUPS(
        title = "Group Limit Reached",
        message = "You've reached the maximum number of groups for your current plan.",
        icon = Icons.Outlined.Groups
    ),
    ITEMS(
        title = "Items to Bring",
        message = "Track items to bring to game night with a Pro plan or higher.",
        icon = Icons.Outlined.Backpack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpgradePromptView(
    limitType: LimitType,
    currentTier: SubscriptionTier,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {},
    onViewPlans: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val recommendedTier = recommendedUpgrade(currentTier)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Upgrade") },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // Icon
            Icon(
                imageVector = limitType.icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title & Message
            Text(
                text = limitType.title,
                style = MaterialTheme.typography.headlineMedium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = limitType.message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Recommended tier card
            if (recommendedTier != null) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SubscriptionBadgeView(tier = recommendedTier)
                            Text(
                                text = recommendedTier.priceLabel,
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            tierFeatures(recommendedTier).forEach { feature ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Text(
                                        text = feature,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Buttons
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        if (onViewPlans != null) {
                            onViewPlans()
                        } else {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(AppConfig.PRICING_URL))
                            context.startActivity(intent)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("View Plans")
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Maybe Later",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

private fun recommendedUpgrade(currentTier: SubscriptionTier): SubscriptionTier? {
    return when (currentTier) {
        SubscriptionTier.FREE -> SubscriptionTier.BASIC
        SubscriptionTier.BASIC -> SubscriptionTier.PRO
        SubscriptionTier.PRO -> SubscriptionTier.PREMIUM
        SubscriptionTier.PREMIUM -> null
    }
}

private fun tierFeatures(tier: SubscriptionTier): List<String> {
    return when (tier) {
        SubscriptionTier.BASIC -> listOf(
            "Host up to 5 events per month",
            "Join up to 3 groups",
            "Ad-free experience"
        )
        SubscriptionTier.PRO -> listOf(
            "Host unlimited events",
            "Join up to 10 groups",
            "Items to bring tracking",
            "Priority support"
        )
        SubscriptionTier.PREMIUM -> listOf(
            "Everything in Pro",
            "Unlimited groups",
            "Custom branding",
            "Dedicated support"
        )
        SubscriptionTier.FREE -> listOf(
            "Host up to 2 events per month",
            "Join 1 group",
            "Basic features"
        )
    }
}
