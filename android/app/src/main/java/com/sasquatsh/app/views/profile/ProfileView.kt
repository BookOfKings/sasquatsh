package com.sasquatsh.app.views.profile

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.sasquatsh.app.models.AppTimezone
import com.sasquatsh.app.models.GameCategory
import com.sasquatsh.app.models.SubscriptionTier
import com.sasquatsh.app.viewmodels.AuthViewModel
import com.sasquatsh.app.viewmodels.ProfileViewModel
import com.sasquatsh.app.views.shared.BadgeView
import com.sasquatsh.app.views.shared.ErrorBannerView
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileView(
    authViewModel: AuthViewModel,
    onNavigateToBilling: () -> Unit,
    onNavigateToBlockedUsers: () -> Unit,
    profileViewModel: ProfileViewModel = hiltViewModel()
) {
    val uiState by profileViewModel.uiState.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var showEditSheet by rememberSaveable { mutableStateOf(false) }
    var showDeleteAvatarConfirm by rememberSaveable { mutableStateOf(false) }
    var showDeleteAccount by rememberSaveable { mutableStateOf(false) }
    var deleteConfirmText by rememberSaveable { mutableStateOf("") }
    var isDeleting by remember { mutableStateOf(false) }
    var accountError by remember { mutableStateOf<String?>(null) }

    // Photo picker launcher
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(it)
                inputStream?.let { stream ->
                    val bytes = stream.readBytes()
                    stream.close()
                    profileViewModel.uploadAvatar(bytes)
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    LaunchedEffect(Unit) {
        profileViewModel.loadProfile()
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .verticalScroll(scrollState)
    ) {
        if (uiState.isLoading && uiState.profile == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            val profile = uiState.profile ?: return@Column

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Avatar & Name
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { photoPickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profile.avatarUrl != null) {
                                AsyncImage(
                                    model = profile.avatarUrl,
                                    contentDescription = "Avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Text(
                                    text = (profile.displayName ?: profile.username)
                                        .take(1).uppercase(),
                                    style = MaterialTheme.typography.headlineLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (uiState.isUploadingAvatar) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(
                                            MaterialTheme.colorScheme.scrim.copy(alpha = 0.4f)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .size(28.dp)
                                .offset(x = 2.dp, y = 2.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = "Change photo",
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier
                                    .padding(4.dp)
                                    .size(16.dp)
                            )
                        }
                    }

                    if (profile.avatarUrl != null) {
                        TextButton(
                            onClick = { showDeleteAvatarConfirm = true },
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Remove Photo",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    Text(
                        text = profile.displayName ?: profile.username,
                        style = MaterialTheme.typography.headlineMedium
                    )

                    Text(
                        text = "@${profile.username}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Stats
                profile.stats?.let { stats ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(value = stats.hostedCount, label = "Hosted")
                            StatItem(value = stats.attendedCount, label = "Attended")
                            StatItem(value = stats.groupCount, label = "Groups")
                        }
                    }
                }

                // Subscription
                Card(
                    onClick = onNavigateToBilling,
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
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Subscription",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            val tier = authState.user?.effectiveTier ?: SubscriptionTier.FREE
                            Text(
                                text = tier.priceLabel,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        SubscriptionBadge(
                            tier = authState.user?.effectiveTier ?: SubscriptionTier.FREE
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Bio
                val bio = profile.bio
                if (!bio.isNullOrEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "About",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = bio,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Home Location
                val homeCity = profile.homeCity
                val homeState = profile.homeState
                if (homeCity != null && homeState != null) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Text(
                                text = "$homeCity, $homeState",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Active Location
                val activeCity = profile.activeCity
                val activeState = profile.activeState
                if (!activeCity.isNullOrEmpty() && !activeState.isNullOrEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MyLocation,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Column {
                                Text(
                                    text = "$activeCity, $activeState",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                profile.activeLocationExpiresAt?.let { expires ->
                                    Text(
                                        text = "Until ${formattedExpiration(expires)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Timezone
                profile.timezone?.let { tz ->
                    val appTz = AppTimezone.fromValue(tz)
                    if (appTz != null) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    text = appTz.displayName,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Favorite Games
                val favoriteGames = profile.favoriteGames
                if (!favoriteGames.isNullOrEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Favorite Games",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                favoriteGames.forEach { game ->
                                    BadgeView(
                                        text = game,
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Preferred Game Types
                val gameTypes = profile.preferredGameTypes
                if (!gameTypes.isNullOrEmpty()) {
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Preferred Game Types",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                gameTypes.forEach { type ->
                                    val displayName = GameCategory.fromValue(type)?.displayName
                                        ?: type.replaceFirstChar { it.uppercase() }
                                    BadgeView(
                                        text = displayName,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    )
                                }
                            }
                        }
                    }
                }

                // Actions
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Edit Profile
                    OutlinedButton(
                        onClick = { showEditSheet = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Profile")
                    }

                    // Blocked Users
                    TextButton(
                        onClick = onNavigateToBlockedUsers,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Block,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Blocked Users",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Change Password (only for non-Google users)
                    val firebaseUser = com.google.firebase.auth.FirebaseAuth
                        .getInstance().currentUser
                    val isGoogleUser = firebaseUser?.providerData
                        ?.any { it.providerId == "google.com" } ?: false
                    if (!isGoogleUser) {
                        TextButton(
                            onClick = { /* TODO: Change password */ },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Change Password",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Sign Out
                    TextButton(
                        onClick = { authViewModel.logout() },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Sign Out",
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    // Delete Account
                    TextButton(
                        onClick = { showDeleteAccount = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Account",
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Edit Profile Sheet
    if (showEditSheet && uiState.profile != null) {
        ProfileEditSheet(
            profile = uiState.profile!!,
            profileViewModel = profileViewModel,
            onDismiss = {
                showEditSheet = false
                profileViewModel.loadProfile()
                authViewModel.refreshUser()
            }
        )
    }

    // Delete Avatar Confirmation
    if (showDeleteAvatarConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteAvatarConfirm = false },
            title = { Text("Remove Avatar") },
            text = { Text("Are you sure you want to remove your profile photo?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        profileViewModel.deleteAvatar()
                        showDeleteAvatarConfirm = false
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAvatarConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Delete Account Confirmation
    if (showDeleteAccount) {
        AlertDialog(
            onDismissRequest = {
                showDeleteAccount = false
                deleteConfirmText = ""
            },
            title = { Text("Delete Account") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "This will permanently delete your account, all your events, groups, and data. This cannot be undone."
                    )
                    OutlinedTextField(
                        value = deleteConfirmText,
                        onValueChange = { deleteConfirmText = it },
                        label = { Text("Type DELETE to confirm") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    accountError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (deleteConfirmText != "DELETE") {
                            accountError = "You must type DELETE to confirm"
                            return@TextButton
                        }
                        isDeleting = true
                        // Delete account via profile service is handled by VM
                        authViewModel.logout()
                        showDeleteAccount = false
                        deleteConfirmText = ""
                        isDeleting = false
                    },
                    enabled = deleteConfirmText == "DELETE" && !isDeleting,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete Forever")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showDeleteAccount = false
                    deleteConfirmText = ""
                    accountError = null
                }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatItem(value: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun SubscriptionBadge(tier: SubscriptionTier) {
    val color = when (tier) {
        SubscriptionTier.FREE -> MaterialTheme.colorScheme.surfaceContainerHigh
        SubscriptionTier.BASIC -> MaterialTheme.colorScheme.primaryContainer
        SubscriptionTier.PRO -> MaterialTheme.colorScheme.tertiaryContainer
        SubscriptionTier.PREMIUM -> MaterialTheme.colorScheme.secondaryContainer
    }
    BadgeView(text = tier.displayName, color = color)
}

private fun formattedExpiration(dateString: String): String {
    return try {
        val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }
        val date = try {
            isoFormat.parse(dateString)
        } catch (_: Exception) {
            val isoNoFrac = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            isoNoFrac.parse(dateString)
        }
        if (date != null) {
            SimpleDateFormat("MMM d, yyyy", Locale.US).format(date)
        } else {
            dateString
        }
    } catch (_: Exception) {
        dateString
    }
}
