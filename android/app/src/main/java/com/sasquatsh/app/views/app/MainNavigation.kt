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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.sasquatsh.app.R
import com.sasquatsh.app.viewmodels.AuthViewModel
import com.sasquatsh.app.views.auth.ForgotPasswordView
import com.sasquatsh.app.views.auth.LoginView
import com.sasquatsh.app.views.auth.SignupView
import com.sasquatsh.app.views.badges.BadgesView
import com.sasquatsh.app.views.billing.BillingView
import com.sasquatsh.app.views.billing.PricingView
import com.sasquatsh.app.views.collections.MyCollectionView
import com.sasquatsh.app.views.dashboard.DashboardView
import com.sasquatsh.app.views.events.CreateEventView
import com.sasquatsh.app.views.events.EventDetailView
import com.sasquatsh.app.views.events.EventListView
import com.sasquatsh.app.views.groups.CreateGroupView
import com.sasquatsh.app.views.groups.GroupDetailView
import com.sasquatsh.app.views.groups.GroupListView
import com.sasquatsh.app.views.lfp.PlayerRequestListView
import com.sasquatsh.app.views.mtg.MyDecksView
import com.sasquatsh.app.views.planning.PlanningSessionDetailView
import com.sasquatsh.app.views.profile.BlockedUsersView
import com.sasquatsh.app.views.profile.ProfileView
import com.sasquatsh.app.views.raffle.RaffleDetailView
import com.sasquatsh.app.views.toolbox.CardDrawPickerView
import com.sasquatsh.app.views.toolbox.FirstPlayerPickerView
import com.sasquatsh.app.views.toolbox.FirstPlayerPickersView
import com.sasquatsh.app.views.toolbox.RandomGamePickerView
import com.sasquatsh.app.views.toolbox.RoundCounterView
import com.sasquatsh.app.views.toolbox.ScoreKeeperView
import com.sasquatsh.app.views.toolbox.SpinWheelPickerView
import com.sasquatsh.app.views.toolbox.StatementPickerView
import com.sasquatsh.app.views.toolbox.ToolboxView
import com.sasquatsh.app.views.toolbox.TurnTrackerView
import kotlinx.coroutines.delay

// Route constants
object Routes {
    const val LOGIN = "login"
    const val SIGNUP = "signup"
    const val FORGOT_PASSWORD = "forgot_password"

    const val DASHBOARD = "dashboard"
    const val GAMES = "games"
    const val GROUPS = "groups"
    const val NEED_PLAYERS = "need_players"
    const val PROFILE = "profile"

    const val EVENT_DETAIL = "event/{eventId}"
    const val EDIT_EVENT = "edit_event/{eventId}"
    const val GROUP_DETAIL = "group/{groupId}"
    const val PLANNING_DETAIL = "planning/{sessionId}"
    const val CREATE_EVENT = "create_event"
    const val CREATE_GROUP = "create_group"
    const val PRICING = "pricing"
    const val BILLING = "billing"
    const val RAFFLE_DETAIL = "raffle_detail"
    const val TOOLBOX = "toolbox"
    const val BLOCKED_USERS = "blocked_users"
    const val BADGES = "badges"
    const val COLLECTION = "collection"
    const val MTG_DECKS = "mtg_decks"
    const val FIRST_PLAYER = "first_player"
    const val FINGER_PICKER = "finger_picker"
    const val SPIN_WHEEL = "spin_wheel"
    const val CARD_DRAW = "card_draw"
    const val STATEMENT_PICKER = "statement_picker"
    const val TURN_TRACKER = "turn_tracker"
    const val ROUND_COUNTER = "round_counter"
    const val SCORE_KEEPER = "score_keeper"
    const val RANDOM_GAME = "random_game"

    fun eventDetail(eventId: String) = "event/$eventId"
    fun editEvent(eventId: String) = "edit_event/$eventId"
    fun groupDetail(groupId: String) = "group/$groupId"
    fun planningDetail(sessionId: String) = "planning/$sessionId"
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
            showSplash || !authState.isInitialized -> SplashScreen()
            authState.isAuthenticated -> MainScaffold(authViewModel = authViewModel)
            else -> AuthNavigation(authViewModel = authViewModel)
        }
    }
}

@Composable
private fun SplashScreen() {
    Box(
        modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.primary),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(id = R.drawable.logo_meeple_white),
                contentDescription = "Sasquatsh logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("Sasquatsh", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
            Spacer(modifier = Modifier.height(24.dp))
            com.sasquatsh.app.views.shared.D20SpinnerView(
                size = 48.dp,
                modifier = Modifier.size(48.dp),
                color = Color(0xFF6366F1),
                numberColor = Color.White
            )
        }
    }
}

@Composable
private fun AuthNavigation(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginView(
                authViewModel = authViewModel,
                onNavigateToSignup = { navController.navigate(Routes.SIGNUP) },
                onNavigateToForgotPassword = { navController.navigate(Routes.FORGOT_PASSWORD) }
            )
        }
        composable(Routes.SIGNUP) {
            SignupView(authViewModel = authViewModel, onNavigateBack = { navController.popBackStack() })
        }
        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordView(onNavigateBack = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainScaffold(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Hide bottom bar on toolbox and detail routes
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val mainTabRoutes = setOf(Routes.DASHBOARD, Routes.GAMES, Routes.GROUPS, Routes.NEED_PLAYERS, Routes.PROFILE)
    val showBottomBar = currentRoute in mainTabRoutes

    Scaffold(
        // No top bar — toolbox link is inside DashboardView
        bottomBar = {
            if (showBottomBar) {
                MainBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { index ->
                        selectedTab = index
                        navController.navigate(bottomNavItems[index].route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Routes.DASHBOARD,
            modifier = Modifier.padding(innerPadding)
        ) {
            // ===== TAB DESTINATIONS =====
            composable(Routes.DASHBOARD) {
                DashboardView(
                    authViewModel = authViewModel,
                    navController = navController,
                    onSwitchTab = { tabIndex ->
                        selectedTab = tabIndex
                        navController.navigate(bottomNavItems[tabIndex].route) {
                            popUpTo(navController.graph.startDestinationId) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }

            composable(Routes.GAMES) {
                EventListView(
                    onNavigateToDetail = { eventId -> navController.navigate(Routes.eventDetail(eventId)) },
                    onNavigateToCreate = { navController.navigate(Routes.CREATE_EVENT) }
                )
            }

            composable(Routes.GROUPS) {
                GroupListView(
                    onNavigateToDetail = { groupId -> navController.navigate(Routes.groupDetail(groupId)) },
                    onNavigateToCreate = { navController.navigate(Routes.CREATE_GROUP) }
                )
            }

            composable(Routes.NEED_PLAYERS) {
                PlayerRequestListView(
                    onNavigateToEventDetail = { eventId -> navController.navigate(Routes.eventDetail(eventId)) }
                )
            }

            composable(Routes.PROFILE) {
                ProfileView(
                    authViewModel = authViewModel,
                    onNavigateToBilling = { navController.navigate(Routes.BILLING) },
                    onNavigateToBlockedUsers = { navController.navigate(Routes.BLOCKED_USERS) },
                    onNavigateToBadges = { navController.navigate(Routes.BADGES) },
                    onNavigateToCollection = { navController.navigate(Routes.COLLECTION) },
                    onNavigateToMtgDecks = { navController.navigate(Routes.MTG_DECKS) }
                )
            }

            // ===== EVENT ROUTES =====
            composable(
                route = Routes.EVENT_DETAIL,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                EventDetailView(
                    eventId = eventId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEdit = { id -> navController.navigate(Routes.editEvent(id)) }
                )
            }

            composable(Routes.CREATE_EVENT) {
                CreateEventView(onDismiss = { navController.popBackStack() })
            }

            composable(
                route = Routes.EDIT_EVENT,
                arguments = listOf(navArgument("eventId") { type = NavType.StringType })
            ) { backStackEntry ->
                val eventId = backStackEntry.arguments?.getString("eventId") ?: return@composable
                // EditEventView needs the eventId to load — use placeholder for now
                // since EditEventView expects a pre-loaded event
                PlaceholderScreen("Edit Event: $eventId")
            }

            // ===== GROUP ROUTES =====
            composable(
                route = Routes.GROUP_DETAIL,
                arguments = listOf(navArgument("groupId") { type = NavType.StringType })
            ) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString("groupId") ?: return@composable
                GroupDetailView(
                    groupId = groupId,
                    onNavigateBack = { navController.popBackStack() },
                    onNavigateToEvent = { eventId -> navController.navigate(Routes.eventDetail(eventId)) },
                    onNavigateToPlanning = { sessionId -> navController.navigate(Routes.planningDetail(sessionId)) },
                    onNavigateToCreatePlanning = { /* TODO: CreatePlanningView needs members list */ },
                    onNavigateToEditGroup = { /* TODO: EditGroupView needs group object */ }
                )
            }

            composable(Routes.CREATE_GROUP) {
                CreateGroupView(onDismiss = { navController.popBackStack() })
            }

            // ===== PLANNING ROUTES =====
            composable(
                route = Routes.PLANNING_DETAIL,
                arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
            ) { backStackEntry ->
                val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
                PlanningSessionDetailView(
                    sessionId = sessionId,
                    onNavigateToEvent = { eventId -> navController.navigate(Routes.eventDetail(eventId)) },
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // ===== BILLING / PRICING =====
            composable(Routes.BILLING) {
                BillingView(
                    authViewModel = authViewModel,
                    onNavigateToPricing = { navController.navigate(Routes.PRICING) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.PRICING) {
                PricingView(
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() }
                )
            }

            // ===== PROFILE SUB-SCREENS =====
            composable(Routes.BLOCKED_USERS) {
                BlockedUsersView(onBack = { navController.popBackStack() })
            }

            composable(Routes.BADGES) {
                BadgesView()
            }

            composable(Routes.COLLECTION) {
                MyCollectionView()
            }

            composable(Routes.MTG_DECKS) {
                MyDecksView(
                    onNavigateToDeck = { /* TODO: DeckBuilderView navigation */ }
                )
            }

            // ===== RAFFLE =====
            composable(Routes.RAFFLE_DETAIL) {
                RaffleDetailView()
            }

            // ===== TOOLBOX =====
            composable(Routes.TOOLBOX) {
                ToolboxView(
                    onBack = { navController.popBackStack() },
                    onNavigateToFirstPlayer = { navController.navigate(Routes.FIRST_PLAYER) },
                    onNavigateToRoundCounter = { navController.navigate(Routes.ROUND_COUNTER) },
                    onNavigateToTurnTracker = { navController.navigate(Routes.TURN_TRACKER) },
                    onNavigateToScoreKeeper = { navController.navigate(Routes.SCORE_KEEPER) },
                    onNavigateToRandomGame = { navController.navigate(Routes.RANDOM_GAME) }
                )
            }

            composable(Routes.FIRST_PLAYER) {
                FirstPlayerPickersView(
                    onBack = { navController.popBackStack() },
                    onNavigateToFingerPicker = { navController.navigate(Routes.FINGER_PICKER) },
                    onNavigateToSpinWheel = { navController.navigate(Routes.SPIN_WHEEL) },
                    onNavigateToCardDraw = { navController.navigate(Routes.CARD_DRAW) },
                    onNavigateToStatement = { navController.navigate(Routes.STATEMENT_PICKER) }
                )
            }

            composable(Routes.FINGER_PICKER) {
                FirstPlayerPickerView(onBack = { navController.popBackStack() })
            }

            composable(Routes.SPIN_WHEEL) {
                SpinWheelPickerView(onBack = { navController.popBackStack() })
            }

            composable(Routes.CARD_DRAW) {
                CardDrawPickerView(onBack = { navController.popBackStack() })
            }

            composable(Routes.STATEMENT_PICKER) {
                StatementPickerView(onBack = { navController.popBackStack() })
            }

            composable(Routes.TURN_TRACKER) {
                TurnTrackerView(onBack = { navController.popBackStack() })
            }

            composable(Routes.ROUND_COUNTER) {
                RoundCounterView(onBack = { navController.popBackStack() })
            }

            composable(Routes.SCORE_KEEPER) {
                ScoreKeeperView(onBack = { navController.popBackStack() })
            }

            composable(Routes.RANDOM_GAME) {
                RandomGamePickerView(onBack = { navController.popBackStack() })
            }
        }
    }
}

@Composable
private fun MainTopBar(onToolboxClick: () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface).padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(painter = painterResource(id = R.drawable.logo_full), contentDescription = "Sasquatsh logo", modifier = Modifier.size(22.dp).clip(RoundedCornerShape(4.dp)))
        Spacer(modifier = Modifier.width(8.dp))
        Row(modifier = Modifier.clickable { onToolboxClick() }, verticalAlignment = Alignment.CenterVertically) {
            Text("Gamer Toolbox", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(4.dp))
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MainBottomBar(selectedTab: Int, onTabSelected: (Int) -> Unit) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        bottomNavItems.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = selectedTab == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.label, modifier = Modifier.size(24.dp)) },
                label = { Text(text = item.label, style = MaterialTheme.typography.labelSmall, fontWeight = if (selectedTab == index) FontWeight.SemiBold else FontWeight.Medium, maxLines = 1, fontSize = 11.sp) },
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
    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceContainerLow), contentAlignment = Alignment.Center) {
        Text(text = title, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
