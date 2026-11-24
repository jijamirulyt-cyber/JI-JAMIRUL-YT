package com.example

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.local.FeelkarDatabase
import com.example.data.local.UserEntity
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.FeelkarRepository
import com.example.ui.screens.*
import com.example.ui.theme.FeelkarBrandGradient
import com.example.ui.theme.FeelkarTheme
import com.example.ui.viewmodel.FeelkarSessionState
import com.example.ui.viewmodel.FeelkarViewModel

object FeelkarRoutes {
    const val HOME = "home"
    const val REELS = "reels"
    const val CREATE = "create"
    const val MESSAGES = "messages"
    const val PROFILE = "profile"
    const val CHAT_DETAIL = "chat_detail"
    const val LIVE_STREAM = "live_stream"
    const val EXPLORE = "explore"
    const val NOTIFICATIONS = "notifications"
    const val ADMIN = "admin"
}

private data class BottomNavTab(
    val route: String,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val isCreate: Boolean = false
)

private val mainTabs = listOf(
    BottomNavTab(FeelkarRoutes.HOME, "Home", Icons.Filled.Home, Icons.Outlined.Home),
    BottomNavTab(FeelkarRoutes.REELS, "Reels", Icons.Filled.Movie, Icons.Outlined.Movie),
    BottomNavTab(FeelkarRoutes.CREATE, "Create", Icons.Filled.Add, Icons.Filled.Add, isCreate = true),
    BottomNavTab(FeelkarRoutes.MESSAGES, "Messages", Icons.Filled.ChatBubble, Icons.Outlined.ChatBubbleOutline),
    BottomNavTab(FeelkarRoutes.PROFILE, "Profile", Icons.Filled.Person, Icons.Outlined.Person)
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            FeelkarRootApp()
        }
    }
}

@Composable
fun FeelkarRootApp() {
    val context = LocalContext.current.applicationContext
    val repository = remember(context) {
        val db = FeelkarDatabase.getInstance(context)
        val prefs = UserPreferencesRepository(context)
        FeelkarRepository(db.feelkarDao(), prefs)
    }
    val viewModel: FeelkarViewModel = viewModel(factory = FeelkarViewModel.Factory(repository))
    val sessionState by viewModel.sessionState.collectAsStateWithLifecycle()
    val authError by viewModel.authError.collectAsStateWithLifecycle()
    val isAuthLoading by viewModel.isAuthLoading.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.statusMessage.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    FeelkarTheme(themePreference = sessionState.themePreference) {
        val currentUser = sessionState.currentUser
        if (!sessionState.isInitialized) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (currentUser == null) {
            AuthScreen(
                isLoading = isAuthLoading,
                errorMessage = authError,
                onLogin = { id, pass -> viewModel.login(id, pass) },
                onRegister = { user, name, email, pass, isBiz ->
                    viewModel.register(user, name, email, pass, isBiz)
                },
                onQuickDemoLogin = { uid -> viewModel.quickDemoLogin(uid) },
                onClearError = { viewModel.clearAuthError() }
            )
        } else {
            key(currentUser.id) {
                FeelkarMainShell(
                    viewModel = viewModel,
                    currentUser = currentUser,
                    sessionState = sessionState,
                    snackbarHostState = snackbarHostState
                )
            }
        }
    }
}

@Composable
private fun FeelkarMainShell(
    viewModel: FeelkarViewModel,
    currentUser: UserEntity,
    sessionState: FeelkarSessionState,
    snackbarHostState: SnackbarHostState
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val currentRoute = currentDestination?.route.orEmpty()

    val showChrome = currentRoute == FeelkarRoutes.HOME ||
        currentRoute == FeelkarRoutes.REELS ||
        currentRoute.startsWith(FeelkarRoutes.CREATE) ||
        currentRoute == FeelkarRoutes.MESSAGES ||
        currentRoute == FeelkarRoutes.PROFILE

    val conversations by viewModel.conversations.collectAsStateWithLifecycle()
    val unreadMessagesCount = remember(conversations) {
        conversations.filter { !it.blocked }.sumOf { it.unreadCount }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (showChrome && !useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        mainTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route?.startsWith(tab.route) == true
                            } == true

                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    if (tab.isCreate) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(14.dp))
                                                .background(FeelkarBrandGradient),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.Add,
                                                contentDescription = "Create",
                                                tint = Color.White
                                            )
                                        }
                                    } else if (tab.route == FeelkarRoutes.MESSAGES && unreadMessagesCount > 0) {
                                        BadgedBox(badge = { Badge { Text(unreadMessagesCount.toString()) } }) {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = {
                                    if (!tab.isCreate) {
                                        Text(tab.label, maxLines = 1)
                                    }
                                },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                if (showChrome && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))
                        mainTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any {
                                it.route?.startsWith(tab.route) == true
                            } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = tab.label
                                    )
                                },
                                label = { Text(tab.label) }
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = FeelkarRoutes.HOME,
                    modifier = Modifier.weight(1f)
                ) {
                    composable(FeelkarRoutes.HOME) {
                        HomeScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            onOpenExplore = { query ->
                                val encoded = Uri.encode(query.orEmpty())
                                navController.navigate("${FeelkarRoutes.EXPLORE}?query=$encoded")
                            },
                            onOpenNotifications = {
                                navController.navigate(FeelkarRoutes.NOTIFICATIONS)
                            },
                            onOpenLiveStream = { streamId ->
                                navController.navigate("${FeelkarRoutes.LIVE_STREAM}/$streamId")
                            },
                            onOpenCreateStory = {
                                navController.navigate("${FeelkarRoutes.CREATE}?mode=${CreateMode.STORY.name}")
                            },
                            onOpenChat = { convId ->
                                navController.navigate("${FeelkarRoutes.CHAT_DETAIL}/$convId")
                            }
                        )
                    }

                    composable(FeelkarRoutes.REELS) {
                        ReelsScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            onOpenChat = { convId ->
                                navController.navigate("${FeelkarRoutes.CHAT_DETAIL}/$convId")
                            }
                        )
                    }

                    composable(
                        route = "${FeelkarRoutes.CREATE}?mode={mode}",
                        arguments = listOf(navArgument("mode") {
                            type = NavType.StringType
                            defaultValue = CreateMode.POST.name
                        })
                    ) { backStackEntry ->
                        val modeName = backStackEntry.arguments?.getString("mode") ?: CreateMode.POST.name
                        val mode = CreateMode.entries.firstOrNull { it.name == modeName } ?: CreateMode.POST
                        CreateScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            initialMode = mode,
                            onDoneNavigateHome = { navigateToTab(FeelkarRoutes.HOME) },
                            onDoneNavigateReels = { navigateToTab(FeelkarRoutes.REELS) },
                            onLiveStarted = { streamId ->
                                navController.navigate("${FeelkarRoutes.LIVE_STREAM}/$streamId")
                            }
                        )
                    }

                    composable(FeelkarRoutes.MESSAGES) {
                        MessagesScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            onOpenConversation = { convId ->
                                navController.navigate("${FeelkarRoutes.CHAT_DETAIL}/$convId")
                            }
                        )
                    }

                    composable(FeelkarRoutes.PROFILE) {
                        ProfileScreen(
                            viewModel = viewModel,
                            currentUser = currentUser,
                            sessionState = sessionState,
                            onOpenAdminDashboard = {
                                navController.navigate(FeelkarRoutes.ADMIN)
                            },
                            onOpenCreateProduct = {
                                navController.navigate("${FeelkarRoutes.CREATE}?mode=${CreateMode.PROMOTE.name}")
                            }
                        )
                    }

                    composable(
                        route = "${FeelkarRoutes.CHAT_DETAIL}/{conversationId}",
                        arguments = listOf(navArgument("conversationId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val convId = backStackEntry.arguments?.getString("conversationId").orEmpty()
                        ChatDetailScreen(
                            conversationId = convId,
                            viewModel = viewModel,
                            currentUser = currentUser,
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(
                        route = "${FeelkarRoutes.LIVE_STREAM}/{streamId}",
                        arguments = listOf(navArgument("streamId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val streamId = backStackEntry.arguments?.getString("streamId").orEmpty()
                        LiveStreamScreen(
                            streamId = streamId,
                            viewModel = viewModel,
                            currentUser = currentUser,
                            onBack = { navController.popBackStack() },
                            onOpenChat = { convId ->
                                navController.navigate("${FeelkarRoutes.CHAT_DETAIL}/$convId")
                            }
                        )
                    }

                    composable(
                        route = "${FeelkarRoutes.EXPLORE}?query={query}",
                        arguments = listOf(navArgument("query") {
                            type = NavType.StringType
                            defaultValue = ""
                        })
                    ) { backStackEntry ->
                        val query = backStackEntry.arguments?.getString("query").orEmpty()
                        ExploreScreen(
                            initialQuery = query.ifBlank { null },
                            viewModel = viewModel,
                            currentUser = currentUser,
                            onBack = { navController.popBackStack() },
                            onOpenChat = { convId ->
                                navController.navigate("${FeelkarRoutes.CHAT_DETAIL}/$convId")
                            }
                        )
                    }

                    composable(FeelkarRoutes.NOTIFICATIONS) {
                        NotificationsScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() },
                            onOpenLiveStream = { streamId ->
                                navController.navigate("${FeelkarRoutes.LIVE_STREAM}/$streamId")
                            },
                            onOpenChat = { convId ->
                                navController.navigate("${FeelkarRoutes.CHAT_DETAIL}/$convId")
                            }
                        )
                    }

                    composable(FeelkarRoutes.ADMIN) {
                        AdminDashboardScreen(
                            viewModel = viewModel,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}
