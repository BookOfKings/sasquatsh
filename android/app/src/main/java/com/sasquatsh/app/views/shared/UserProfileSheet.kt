package com.sasquatsh.app.views.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.sasquatsh.app.models.Badge
import com.sasquatsh.app.models.CollectionGame
import com.sasquatsh.app.models.PublicProfile
import com.sasquatsh.app.models.UserBadge
import com.sasquatsh.app.services.BadgesService
import com.sasquatsh.app.services.CollectionsService
import com.sasquatsh.app.services.ProfileService
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Reusable bottom sheet / full-screen composable showing another user's profile.
 * Port of the iOS UserProfileSheet.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun UserProfileSheet(
    userId: String,
    profileService: ProfileService,
    badgesService: BadgesService,
    collectionsService: CollectionsService,
    onDismiss: () -> Unit,
    onViewAllGames: ((List<CollectionGame>, String) -> Unit)? = null
) {
    var profile by remember { mutableStateOf<PublicProfile?>(null) }
    var badges by remember { mutableStateOf<List<UserBadge>>(emptyList()) }
    var collection by remember { mutableStateOf<List<CollectionGame>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(userId) {
        isLoading = true
        try {
            coroutineScope {
                val profileDeferred = async {
                    try {
                        profileService.getPublicProfile(userId)
                    } catch (_: Exception) {
                        null
                    }
                }
                val badgesDeferred = async {
                    try {
                        badgesService.getUserBadges(userId)
                    } catch (_: Exception) {
                        emptyList<UserBadge>()
                    }
                }
                val collectionDeferred = async {
                    try {
                        collectionsService.getUserCollection(userId)
                    } catch (_: Exception) {
                        emptyList()
                    }
                }

                profile = profileDeferred.await()
                badges = badgesDeferred.await()
                collection = collectionDeferred.await()
            }
        } catch (_: Exception) {
            // Non-fatal
        }
        isLoading = false
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text("Done")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { innerPadding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                D20ProgressView(size = 32.dp)
            }
        } else if (profile != null) {
            val prof = profile!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(top = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar + name
                UserAvatarView(
                    url = prof.avatarUrl,
                    name = prof.displayName,
                    size = 72.dp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = prof.displayName ?: "Unknown",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Text(
                    text = "@${prof.username}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Location
                if (prof.homeCity != null && prof.homeState != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${prof.homeCity}, ${prof.homeState}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Bio
                if (!prof.bio.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = prof.bio,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Pinned badges
                val pinned = badges.filter { it.isPinned }
                if (pinned.isNotEmpty()) {
                    SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Pinned Badges",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            pinned.forEach { ub ->
                                Column(
                                    modifier = Modifier.width(70.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    BadgeIcon(badge = ub.badge, size = 36)
                                    Text(
                                        text = ub.badge.name,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Badges summary
                if (badges.isNotEmpty()) {
                    SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Filled.EmojiEvents,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${badges.size} Badges Earned",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        // Show first 8 badges in a grid
                        val displayBadges = badges.take(8)
                        // Use a simple flow row instead of LazyVerticalGrid (inside scrollable)
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly,
                            maxItemsInEachRow = 4
                        ) {
                            displayBadges.forEach { ub ->
                                Column(
                                    modifier = Modifier
                                        .padding(4.dp)
                                        .width(70.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    BadgeIcon(badge = ub.badge, size = 36)
                                    Text(
                                        text = ub.badge.name,
                                        fontSize = 9.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (badges.size > 8) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "+${badges.size - 8} more",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Game collection
                if (collection.isNotEmpty()) {
                    SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "\uD83C\uDFB2", // dice emoji
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Game Collection (${collection.size})",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        collection.take(5).forEach { game ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (game.thumbnailUrl != null) {
                                    AsyncImage(
                                        model = game.thumbnailUrl,
                                        contentDescription = game.gameName,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                }

                                Text(
                                    text = game.gameName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )

                                val min = game.minPlayers
                                val max = game.maxPlayers
                                if (min != null && max != null) {
                                    Text(
                                        text = if (min == max) "${min}p" else "${min}-${max}p",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        if (collection.size > 5 && onViewAllGames != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = {
                                    onViewAllGames(collection, prof.displayName ?: "User")
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "View all ${collection.size} games \u2192",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Favorite games
                val favorites = prof.favoriteGames
                if (!favorites.isNullOrEmpty()) {
                    SectionCard(modifier = Modifier.padding(horizontal = 16.dp)) {
                        Text(
                            text = "Favorite Games",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            favorites.forEach { game ->
                                Text(
                                    text = game,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                                        )
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

/**
 * Reusable section card matching the iOS surface + rounded rect pattern.
 */
@Composable
private fun SectionCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        content = content
    )
}

/**
 * Badge icon with tier-colored circle background and category icon.
 */
@Composable
private fun BadgeIcon(badge: Badge, size: Int = 36) {
    val tColor = tierColor(badge.tier)
    Box(
        modifier = Modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(tColor.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = categoryIconEmoji(badge.category),
            fontSize = (size / 2).sp
        )
    }
}

private fun tierColor(tier: String): Color {
    return when (tier) {
        "bronze" -> Color(0xFFCC8033)
        "silver" -> Color(0xFFA0A0A0)
        "gold" -> Color(0xFFFFD700)
        "platinum" -> Color(0xFF6366F1)
        else -> Color(0xFF9E9E9E)
    }
}

private fun categoryIconEmoji(category: String): String {
    return when (category) {
        "hosting" -> "\uD83C\uDFE0"        // house
        "attendance" -> "\u2714\uFE0F"      // check
        "planning" -> "\uD83D\uDCC5"        // calendar
        "social" -> "\uD83D\uDC65"          // people
        "collection" -> "\uD83C\uDFB2"      // dice
        "game_system" -> "\uD83C\uDFAE"     // game controller
        "items" -> "\uD83D\uDC5C"           // bag
        "special" -> "\u2B50"               // star
        else -> "\u2753"                    // question mark
    }
}
