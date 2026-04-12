package com.sasquatsh.app.ui.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.ContactMail
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Policy
import androidx.compose.material.icons.filled.PriceCheck
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.sasquatsh.app.BuildConfig

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToMtgDecks: () -> Unit,
    onNavigateToLfp: () -> Unit,
    onNavigateToBilling: () -> Unit,
    onSignOut: () -> Unit,
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("More") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState()),
        ) {
            // Account section
            MoreMenuItem(
                icon = Icons.Filled.Person,
                title = "Profile",
                subtitle = "View and edit your profile",
                onClick = onNavigateToProfile,
            )
            HorizontalDivider()

            MoreMenuItem(
                icon = Icons.Filled.Style,
                title = "MTG Decks",
                subtitle = "Manage your Magic decks",
                onClick = onNavigateToMtgDecks,
            )
            HorizontalDivider()

            MoreMenuItem(
                icon = Icons.Filled.Search,
                title = "Looking for Players",
                subtitle = "Find players near you",
                onClick = onNavigateToLfp,
            )
            HorizontalDivider()

            MoreMenuItem(
                icon = Icons.Filled.AttachMoney,
                title = "Billing",
                subtitle = "Manage your subscription",
                onClick = onNavigateToBilling,
            )
            HorizontalDivider()

            MoreMenuItem(
                icon = Icons.Filled.PriceCheck,
                title = "Pricing",
                subtitle = "View plans and pricing",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sasquatsh.com/pricing"))
                    context.startActivity(intent)
                },
            )
            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // Legal section
            MoreMenuItem(
                icon = Icons.Filled.Gavel,
                title = "Terms of Service",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sasquatsh.com/terms"))
                    context.startActivity(intent)
                },
            )
            HorizontalDivider()

            MoreMenuItem(
                icon = Icons.Filled.Policy,
                title = "Privacy Policy",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sasquatsh.com/privacy"))
                    context.startActivity(intent)
                },
            )
            HorizontalDivider()

            MoreMenuItem(
                icon = Icons.Filled.ContactMail,
                title = "Contact Us",
                onClick = {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://sasquatsh.com/contact"))
                    context.startActivity(intent)
                },
            )
            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // App info (not clickable)
            ListItem(
                headlineContent = { Text("App Version") },
                supportingContent = {
                    Text(
                        text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingContent = {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
            )
            HorizontalDivider()

            Spacer(modifier = Modifier.height(16.dp))

            // Sign out
            MoreMenuItem(
                icon = Icons.AutoMirrored.Filled.Logout,
                title = "Sign Out",
                tintColor = MaterialTheme.colorScheme.error,
                onClick = onSignOut,
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MoreMenuItem(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    tintColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                color = if (tintColor != MaterialTheme.colorScheme.onSurfaceVariant) {
                    tintColor
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
        },
        supportingContent = subtitle?.let {
            {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        leadingContent = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tintColor,
            )
        },
        modifier = Modifier.clickable(onClick = onClick),
    )
}
