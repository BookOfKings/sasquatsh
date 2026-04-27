package com.sasquatsh.app.views.shared

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@Composable
fun UserAvatarView(
    url: String?,
    name: String?,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
    userId: String? = null,
    isAdmin: Boolean = false,
    isFoundingMember: Boolean = false,
    onProfileClick: ((String) -> Unit)? = null
) {
    val density = LocalDensity.current
    val badgeSize = size * 0.3f
    val iconSize = size * 0.15f
    val fontSize = with(density) { (size * 0.4f).toSp() }

    Box(
        modifier = modifier
            .size(size)
            .then(
                if (userId != null && onProfileClick != null) {
                    Modifier.clickable { onProfileClick(userId) }
                } else {
                    Modifier
                }
            )
    ) {
        // Avatar content
        if (url != null) {
            AsyncImage(
                model = url,
                contentDescription = name ?: "User avatar",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer)
            )
        } else {
            // Initials placeholder
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = getInitials(name),
                    fontSize = fontSize,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        // Badge overlay
        if (isAdmin || isFoundingMember) {
            val badgeColor = if (isAdmin) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.tertiary
            }

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(badgeSize)
                    .clip(CircleShape)
                    .background(badgeColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Star,
                    contentDescription = if (isAdmin) "Admin" else "Founding Member",
                    tint = Color.White,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    }
}

private fun getInitials(name: String?): String {
    if (name.isNullOrBlank()) return "?"
    val parts = name.trim().split(" ")
    return if (parts.size >= 2) {
        "${parts[0].first()}${parts[1].first()}".uppercase()
    } else {
        name.take(2).uppercase()
    }
}
