package com.sasquatsh.app.ui.navigation

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.sasquatsh.app.ui.auth.AuthViewModel
import com.sasquatsh.app.ui.events.EventDetailScreen
import com.sasquatsh.app.ui.events.EventListScreen
import com.sasquatsh.app.ui.auth.ForgotPasswordScreen
import com.sasquatsh.app.ui.auth.LoginScreen
import com.sasquatsh.app.ui.auth.SignupScreen
import com.sasquatsh.app.ui.dashboard.DashboardScreen
import com.sasquatsh.app.ui.groups.GroupDetailScreen
import com.sasquatsh.app.ui.groups.GroupListScreen
import com.sasquatsh.app.ui.profile.ProfileScreen

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val authState by authViewModel.uiState.collectAsState()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val showBottomBar = isAuthenticated && currentRoute in bottomNavItems.map { it.route }

    // Navigate to dashboard when authenticated, or login when not
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated && currentRoute in listOf(Routes.Login.route, Routes.Signup.route, Routes.ForgotPassword.route, null)) {
            navController.navigate(Routes.Dashboard.route) {
                popUpTo(0) { inclusive = true }
            }
        } else if (!isAuthenticated && currentRoute !in listOf(Routes.Login.route, Routes.Signup.route, Routes.ForgotPassword.route)) {
            navController.navigate(Routes.Login.route) {
                popUpTo(0) { inclusive = true }
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                BottomNavBar(
                    currentRoute = currentRoute,
                    onNavigate = { route ->
                        navController.navigate(route) {
                            popUpTo(Routes.Dashboard.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = if (isAuthenticated) Routes.Dashboard.route else Routes.Login.route,
            modifier = Modifier.padding(padding),
        ) {
            // Auth
            composable(Routes.Login.route) {
                val activity = LocalContext.current as Activity

                val googleSignInLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.StartActivityForResult()
                ) { result ->
                    authViewModel.handleGoogleSignInResult(result)
                }

                LoginScreen(
                    uiState = authState,
                    onLogin = { email, password -> authViewModel.loginWithEmail(email, password) },
                    onGoogleSignIn = {
                        val client = authViewModel.getGoogleSignInClient(activity)
                        googleSignInLauncher.launch(client.signInIntent)
                    },
                    onNavigateToSignup = { navController.navigate(Routes.Signup.route) },
                    onNavigateToForgotPassword = { navController.navigate(Routes.ForgotPassword.route) },
                    onClearError = { authViewModel.clearError() },
                )
            }

            composable(Routes.Signup.route) {
                SignupScreen(
                    uiState = authState,
                    onSignup = { email, password, name -> authViewModel.signupWithEmail(email, password, name) },
                    onNavigateBack = { navController.popBackStack() },
                    onClearError = { authViewModel.clearError() },
                )
            }

            composable(Routes.ForgotPassword.route) {
                ForgotPasswordScreen(
                    uiState = authState,
                    onSendReset = { email -> authViewModel.sendPasswordReset(email) },
                    onNavigateBack = { navController.popBackStack() },
                    onClearError = { authViewModel.clearError() },
                )
            }

            // Main tabs
            composable(Routes.Dashboard.route) {
                DashboardScreen(
                    onNavigateToEvents = { navController.navigate(Routes.Events.route) },
                    onNavigateToGroups = { navController.navigate(Routes.Groups.route) },
                    onNavigateToEvent = { id -> navController.navigate(Routes.EventDetail.withId(id)) },
                    onNavigateToGroup = { slug -> navController.navigate(Routes.GroupDetail.withSlug(slug)) },
                    onNavigateToCreateEvent = { navController.navigate(Routes.CreateEvent.route) },
                    onNavigateToProfile = { navController.navigate(Routes.Profile.route) },
                )
            }

            composable(Routes.Events.route) {
                EventListScreen(
                    onNavigateToEvent = { id -> navController.navigate(Routes.EventDetail.withId(id)) },
                    onNavigateToCreateEvent = { navController.navigate(Routes.CreateEvent.route) },
                )
            }

            composable(Routes.Groups.route) {
                GroupListScreen(
                    onNavigateToGroup = { slug ->
                        navController.navigate(Routes.GroupDetail.withSlug(slug))
                    },
                    onNavigateToCreateGroup = {
                        navController.navigate(Routes.CreateGroup.route)
                    },
                )
            }

            composable(Routes.GroupDetail.route) {
                GroupDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            composable(Routes.More.route) {
                MoreScreen(
                    onNavigateToProfile = { navController.navigate(Routes.Profile.route) },
                    onNavigateToMtgDecks = { navController.navigate(Routes.MtgDecks.route) },
                    onNavigateToLfp = { navController.navigate(Routes.LookingForPlayers.route) },
                    onNavigateToBilling = { navController.navigate(Routes.Billing.route) },
                    onSignOut = { authViewModel.logout() },
                )
            }

            composable(Routes.Profile.route) {
                ProfileScreen(
                    onNavigateBack = { navController.popBackStack() },
                )
            }

            // Event routes
            composable(Routes.EventDetail.route) {
                EventDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(Routes.EditEvent.withId(id)) },
                )
            }
            composable(Routes.CreateEvent.route) {
                PlaceholderScreen("Create Event")
            }
            composable(Routes.EditEvent.route) {
                PlaceholderScreen("Edit Event")
            }

            // Planning routes
            composable(Routes.PlanGameNight.route) {
                PlaceholderScreen("Plan Game Night")
            }
            composable(Routes.PlanningSession.route) {
                PlaceholderScreen("Planning Session")
            }

            // Invitation routes
            composable(Routes.GroupInvite.route) {
                PlaceholderScreen("Group Invite")
            }
            composable(Routes.EventInvite.route) {
                PlaceholderScreen("Event Invite")
            }

            // Other routes
            composable(Routes.CreateGroup.route) {
                PlaceholderScreen("Create Group")
            }
            composable(Routes.MtgDecks.route) {
                PlaceholderScreen("MTG Decks")
            }
            composable(Routes.MtgDeckBuilder.route) {
                PlaceholderScreen("Deck Builder")
            }
            composable(Routes.LookingForPlayers.route) {
                PlaceholderScreen("Looking for Players")
            }
            composable(Routes.Billing.route) {
                PlaceholderScreen("Billing")
            }
        }
    }
}

