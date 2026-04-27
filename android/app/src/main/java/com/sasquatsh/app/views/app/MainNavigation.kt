package com.sasquatsh.app.views.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonSearch
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sasquatsh.app.R
import com.sasquatsh.app.viewmodels.AuthViewModel
import com.sasquatsh.app.views.auth.ForgotPasswordView
import com.sasquatsh.app.views.auth.LoginView
import com.sasquatsh.app.views.auth.SignupView
import com.sasquatsh.app.views.dashboard.DashboardView
import kotlinx.coroutines.delay

// Route constants
object Routes {
    // Auth
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val FORGOT_PASSWORD = "forgot_password"

    // Main tabs
    const val DASHBOARD = "dashboard"
    const val GAMES = "games"
    const val GROUPS = "groups"
    const val NEED_PLAYERS = "need_players"
    const val PROFILE = "profile"

    // Detail screens
    const val EVENT_DETAIL = "event/{eventId}"
    const val GROUP_DETAIL = "group/{groupId}"
    const val PLANNING_DETAIL = "planning/{sessionId}"
    const val CREATE_EVENT = "create_event"
    const val CREATE_GROUP = "create_group"
    const val PRICING = "pricing"
    const val RAFFLE_DETAIL = "raffle_detail"
    const val PLAYER_REQUEST_DETAIL = "player_request/{requestId}"
    const val TOOLBOX = "toolbox"

    fun eventDetail(eventId: String) = "event/$eventId"
    fun groupDetail(groupId: String) = "group/$groupId"
    fun planningDetail(sessionId: String) = "planning/$sessionId"
    fun playerRequestDetail(requestId: String) = "player_request/$requestId"
}

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem(Routes.DASHBOARD, "Dashboard", Icons.Default.Dashboard),
    BottomNavItem(Routes.GAMES, "Games", Icons.Default.Casino),
    BottomNavItem(Routes.GROUPS, "Groups", Icons.Default.Groups),
    BottomNavItem(Routes.NEED_PLAYERS, "Need Players", Icons.Default.PersonSearch),
    BottomNavItem(Routes.PROFILE, "Profile", Icons.Default.Person)
)

@Composable
fun AppNavigation(
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val authState by authViewModel.uiState.collectAsState()
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2000)
        showSplash = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when {
            showSplash || !authState.isInitialized -> {
                SplashScreen()
            }
            authState.isAuthenticated -> {
                MainScaffold(authViewModel = authViewModel)
            }
            else -> {
                AuthNavigation(authViewModel = authViewModel)
            }
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo_white),
                contentDescription = "Sasquatsh logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Sasquatsh",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.height(24.dp))
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = MaterialTheme.colorScheme.onPrimary,
                strokeWidth = 3.dp
            )
        }
    }
}

@Composable
private fun AuthNavigation(
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Routes.LOGIN
    ) {
        composable(Routes.LOGIN) {
            LoginView(
                authViewModel = authViewModel,
                onNavigateToSignup = { navController.navigate(Routes.SIGNUP) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }
        composable(Routes.SIGNUP) {
            SignupView(
                authViewModel = authViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordView(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}

@Composable
private fun MainScaffold(
    authViewModel: AuthViewModel
) {
    val navController = rememberNavController()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    Scaffold(
        topBar = {
            MainTopBar()
        },
        bottomBar = {
            MainBottomBar(
                selectedTab = selectedTab,
                onTabSelected = { index ->
                    selectedTab = index
                    val route = bottomNavItems[index].route
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding)
        ) {
            // Tab destinations
            composable(Routes.DASHBOARD) {
                DashboardView(
                    authViewModel = authViewModel,
                    navController = navController,
                    onSwitchTab = { tabIndex ->
                        selectedTab = tabIndex
                        val route = bottomNavItems[tabIndex].route
                        navController.navigate(route) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
            composable(Routes.GAMES) {
                // TODO: EventListView
                PlaceholderScreen("Games")
            }
            composable(Routes.GROUPS) {
                // TODO: GroupListView
                PlaceholderScreen("Groups")
            }
            composable(Routes.NEED_PLAYERS) {
                // TODO: PlayerRequestListView
                PlaceholderScreen("Need Players")
            }
            composable(Routes.PROFILE) {
                // TODO: ProfileView
                PlaceholderScreen("Profile")
            }

            // Detail destinations
            composable(
                route = Routes.EVENT_DETAIL,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                // TODO: EventDetailView(eventId)
                PlaceholderScreen("Event: $eventId")
            }
            composable(
                route = Routes.GROUP_DETAIL,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                // TODO: GroupDetailView(groupId)
                PlaceholderScreen("Group: $groupId")
            }
            composable(
                route = Routes.PLANNING_DETAIL,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                // TODO: PlanningSessionDetailView(sessionId)
                PlaceholderScreen("Planning: $sessionId")
            }
            composable(Routes.CREATE_EVENT) {
                // TODO: CreateEventView
                PlaceholderScreen("Create Event")
            }
            composable(Routes.CREATE_GROUP) {
                // TODO: CreateGroupView
                PlaceholderScreen("Create Group")
            }
            composable(Routes.PRICING) {
                // TODO: PricingView
                PlaceholderScreen("Pricing")
            }
            composable(Routes.RAFFLE_DETAIL) {
                // TODO: RaffleDetailView
                PlaceholderScreen("Raffle")
            }
            composable(Routes.TOOLBOX) {
                // TODO: ToolboxView
                PlaceholderScreen("Gamer Toolbox")
            }
            composable(
                route = Routes.PLAYER_REQUEST_DETAIL,
                arguments = listOf(navArgument("requestId") { type = NavType.StringType })
            ) { backStackEntry ->
                val requestId = backStackEntry.arguments?.getString("requestId") ?: return@composable
                // TODO: PlayerRequestDetailView(requestId)
                PlaceholderScreen("Player Request: $requestId")
            }
        }
    }
}

@Composable
private fun MainTopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Sasquatsh logo",
            modifier = Modifier
                .size(22.dp)
                .clip(RoundedCornerShape(4.dp))
        )
        Spacer(modifier = Modifier.width(8.dp))
        Row(
            modifier = Modifier.clickable { /* TODO: Navigate to toolbox */ },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Gamer Toolbox",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MainBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        bottomNavItems.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1,
                        fontSize = 11.sp
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer
                )
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerLow),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
