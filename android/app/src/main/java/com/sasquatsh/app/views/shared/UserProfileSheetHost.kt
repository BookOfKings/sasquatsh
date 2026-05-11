package com.sasquatsh.app.views.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.sasquatsh.app.models.CollectionGame
import com.sasquatsh.app.viewmodels.UserProfileSheetViewModel

/**
 * Host composable that manages showing/hiding the UserProfileSheet.
 * Includes "View all games" support via a nested fullscreen dialog.
 */
@Composable
fun UserProfileSheetHost(
    userId: String?,
    onDismiss: () -> Unit,
    viewModel: UserProfileSheetViewModel = hiltViewModel()
) {
    var viewAllGames by remember { mutableStateOf<List<CollectionGame>?>(null) }
    var viewAllName by remember { mutableStateOf("") }

    if (userId != null) {
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            UserProfileSheet(
                userId = userId,
                profileService = viewModel.profileService,
                badgesService = viewModel.badgesService,
                collectionsService = viewModel.collectionsService,
                onDismiss = onDismiss,
                onViewAllGames = { games, name ->
                    viewAllGames = games
                    viewAllName = name
                }
            )
        }
    }

    // Full collection viewer
    viewAllGames?.let { games ->
        Dialog(
            onDismissRequest = { viewAllGames = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            com.sasquatsh.app.views.collections.UserCollectionListView(
                games = games,
                userName = viewAllName,
                onDismiss = { viewAllGames = null }
            )
        }
    }
}
