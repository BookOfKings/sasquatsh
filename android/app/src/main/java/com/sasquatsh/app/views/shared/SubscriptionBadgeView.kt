package com.sasquatsh.app.views.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.sasquatsh.app.models.SubscriptionTier

@Composable
fun SubscriptionBadgeView(
    tier: SubscriptionTier,
    modifier: Modifier = Modifier
) {
    val badgeColor = when (tier) {
        SubscriptionTier.FREE -> MaterialTheme.colorScheme.surfaceContainerHigh
        SubscriptionTier.BASIC -> MaterialTheme.colorScheme.primaryContainer
        SubscriptionTier.PRO -> MaterialTheme.colorScheme.tertiaryContainer
        SubscriptionTier.PREMIUM -> MaterialTheme.colorScheme.secondaryContainer
    }

    BadgeView(
        text = tier.displayName,
        color = badgeColor,
        modifier = modifier
    )
}
