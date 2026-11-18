package com.example

import android.Manifest
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.data.auth.authStateFlow
import com.example.data.repository.JanzRepository
import com.example.notifications.JanzNotificationHelper
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.CreatorDashboardScreen
import com.example.ui.screens.ExploreSearchScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.NotificationsScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.SettingsPrivacyScreen
import com.example.ui.screens.ShortsScreen
import com.example.ui.screens.SplashScreen
import com.example.ui.screens.UploadScreen
import com.example.ui.screens.VideoPlayerScreen
import com.example.ui.state.JanzViewModel
import com.example.ui.state.UiState
import com.example.ui.theme.ElectricViolet
import com.example.ui.theme.FlareOrange
import com.example.ui.theme.JanzFlareTheme
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

object JanzRoutes {
    const val HOME = "home"
    const val EXPLORE = "explore"
    const val CREATE = "create"
    const val SHORTS = "shorts"
    const val PROFILE = "profile"
    const val WATCH = "watch/{videoId}"
    const val CREATOR = "creator/{creatorId}"
    const val DASHBOARD = "dashboard?tab={tab}"
    const val NOTIFICATIONS = "notifications"
    const val SETTINGS = "settings"
    const val ADMIN = "admin"

    fun watch(videoId: String) = "watch/$videoId"
    fun creator(creatorId: String) = "creator/$creatorId"
    fun dashboard(tab: Int = 0) = "dashboard?tab=$tab"
}

class MainActivity : ComponentActivity() {
    private var pendingNotifVideoId by mutableStateOf<String?>(null)
    private var pendingOpenNotifications by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleNotificationIntent(intent)
        enableEdgeToEdge()
        setContent {
            val isFirebaseReady = remember { FirebaseApp.getApps(this).isNotEmpty() }
            if (!isFirebaseReady) {
                JanzFlareTheme(themeMode = "dark") {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "JanzFlare Setup Required",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Firebase configuration (google-services.json) was not included in this build. Add GOOGLE_SERVICES_JSON to GitHub Secrets or place app/google-services.json in the project and rebuild.",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
                return@setContent
            }

            var splashDone by remember { mutableStateOf(false) }
            val currentUser by Firebase.auth.authStateFlow()
                .collectAsStateWithLifecycle(initialValue = Firebase.auth.currentUser)

            if (!splashDone) {
                JanzFlareTheme(themeMode = "dark") {
                    SplashScreen(onSplashComplete = { splashDone = true })
                }
            } else if (currentUser == null) {
                JanzFlareTheme(themeMode = "dark") {
                    AuthScreen(onAuthSuccess = { })
                }
            } else {
                val user = currentUser!!
                key(user.uid) {
                    AuthenticatedJanzApp(
                        user = user,
                        pendingVideoId = pendingNotifVideoId,
                        pendingOpenNotifications = pendingOpenNotifications,
                        onConsumedPendingDestination = {
                            pendingNotifVideoId = null
                            pendingOpenNotifications = false
                        }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleNotificationIntent(intent)
    }

    private fun handleNotificationIntent(intent: Intent?) {
        val vid = intent?.getStringExtra(JanzNotificationHelper.EXTRA_VIDEO_ID)
        if (!vid.isNullOrBlank()) {
            pendingNotifVideoId = vid
            pendingOpenNotifications = false
        } else if (intent?.getBooleanExtra(JanzNotificationHelper.EXTRA_OPEN_NOTIFICATIONS, false) == true) {
            pendingOpenNotifications = true
        }
    }
}

@Composable
private fun AuthenticatedJanzApp(
    user: FirebaseUser,
    pendingVideoId: String?,
    pendingOpenNotifications: Boolean,
    onConsumedPendingDestination: () -> Unit
) {
    val context = LocalContext.current
    val viewModel: JanzViewModel = viewModel(
        key = user.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY])
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                JanzViewModel(
                    appContext = app.applicationContext,
                    repository = JanzRepository(db, app),
                    currentUserId = user.uid,
                    currentFirebaseUser = user
                )
            }
        }
    )

    val notifPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    LaunchedEffect(user.uid) {
        JanzNotificationHelper.ensureChannel(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !JanzNotificationHelper.hasNotificationPermission(context) &&
            !JanzNotificationHelper.wasPermissionPromptShown(context)
        ) {
            JanzNotificationHelper.markPermissionPromptShown(context)
            notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    val userAccountState by viewModel.userAccountState.collectAsStateWithLifecycle()
    val themeMode = (userAccountState as? UiState.Success)?.data?.themeMode ?: "dark"
    val snackbarMessage by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    JanzFlareTheme(themeMode = themeMode) {
        val navController = rememberNavController()

        LaunchedEffect(pendingVideoId, pendingOpenNotifications) {
            when {
                !pendingVideoId.isNullOrBlank() -> {
                    navController.navigate(JanzRoutes.watch(pendingVideoId))
                    onConsumedPendingDestination()
                }
                pendingOpenNotifications -> {
                    navController.navigate(JanzRoutes.NOTIFICATIONS)
                    onConsumedPendingDestination()
                }
            }
        }
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        val bottomTabRoutes = setOf(
            JanzRoutes.HOME,
            JanzRoutes.EXPLORE,
            JanzRoutes.CREATE,
            JanzRoutes.SHORTS,
            JanzRoutes.PROFILE
        )
        val showChrome = currentRoute in bottomTabRoutes

        fun navigateTopLevel(route: String) {
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

            Row(modifier = Modifier.fillMaxSize()) {
                if (showChrome && useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(Modifier.height(12.dp))
                        NavigationRailItem(
                            selected = currentRoute == JanzRoutes.HOME,
                            onClick = { navigateTopLevel(JanzRoutes.HOME) },
                            icon = { Icon(if (currentRoute == JanzRoutes.HOME) Icons.Filled.Home else Icons.Outlined.Home, "Home") },
                            label = { Text("Home") }
                        )
                        NavigationRailItem(
                            selected = currentRoute == JanzRoutes.EXPLORE,
                            onClick = { navigateTopLevel(JanzRoutes.EXPLORE) },
                            icon = { Icon(if (currentRoute == JanzRoutes.EXPLORE) Icons.Filled.Explore else Icons.Outlined.Explore, "Explore") },
                            label = { Text("Explore") }
                        )
                        NavigationRailItem(
                            selected = currentRoute == JanzRoutes.CREATE,
                            onClick = { navigateTopLevel(JanzRoutes.CREATE) },
                            icon = { Icon(Icons.Filled.Add, "Create") },
                            label = { Text("Create") }
                        )
                        NavigationRailItem(
                            selected = currentRoute == JanzRoutes.SHORTS,
                            onClick = { navigateTopLevel(JanzRoutes.SHORTS) },
                            icon = { Icon(if (currentRoute == JanzRoutes.SHORTS) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle, "Shorts") },
                            label = { Text("Shorts") }
                        )
                        NavigationRailItem(
                            selected = currentRoute == JanzRoutes.PROFILE,
                            onClick = { navigateTopLevel(JanzRoutes.PROFILE) },
                            icon = { Icon(if (currentRoute == JanzRoutes.PROFILE) Icons.Filled.Person else Icons.Outlined.Person, "Profile") },
                            label = { Text("Profile") }
                        )
                    }
                }

                Scaffold(
                    contentWindowInsets = WindowInsets(0, 0, 0, 0),
                    snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
                    bottomBar = {
                        if (showChrome && !useRail) {
                            JanzBottomBar(
                                currentRoute = currentRoute,
                                onNavigate = ::navigateTopLevel
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        val contentModifier = if (currentRoute == JanzRoutes.SHORTS || currentRoute?.startsWith("watch") == true) {
                            Modifier.fillMaxSize()
                        } else {
                            Modifier
                                .fillMaxSize()
                                .widthIn(max = 840.dp)
                        }

                        NavHost(
                            navController = navController,
                            startDestination = JanzRoutes.HOME,
                            modifier = contentModifier
                        ) {
                            composable(JanzRoutes.HOME) {
                                HomeScreen(
                                    viewModel = viewModel,
                                    onOpenSearch = { navigateTopLevel(JanzRoutes.EXPLORE) },
                                    onOpenNotifications = { navController.navigate(JanzRoutes.NOTIFICATIONS) },
                                    onOpenOwnProfile = { navigateTopLevel(JanzRoutes.PROFILE) },
                                    onOpenCreatorProfile = { cid -> navController.navigate(JanzRoutes.creator(cid)) },
                                    onOpenVideo = { vid -> navController.navigate(JanzRoutes.watch(vid)) },
                                    onOpenShorts = { navigateTopLevel(JanzRoutes.SHORTS) },
                                    onOpenUpload = { navigateTopLevel(JanzRoutes.CREATE) },
                                    onOpenAdmin = { navController.navigate(JanzRoutes.ADMIN) }
                                )
                            }

                            composable(JanzRoutes.EXPLORE) {
                                ExploreSearchScreen(
                                    viewModel = viewModel,
                                    onOpenVideo = { vid -> navController.navigate(JanzRoutes.watch(vid)) },
                                    onOpenCreatorProfile = { cid -> navController.navigate(JanzRoutes.creator(cid)) },
                                    onOpenShorts = { navigateTopLevel(JanzRoutes.SHORTS) }
                                )
                            }

                            composable(JanzRoutes.CREATE) {
                                UploadScreen(
                                    viewModel = viewModel,
                                    onPublished = { newVideoId ->
                                        navController.navigate(JanzRoutes.watch(newVideoId)) {
                                            popUpTo(JanzRoutes.HOME)
                                        }
                                    }
                                )
                            }

                            composable(JanzRoutes.SHORTS) {
                                ShortsScreen(
                                    viewModel = viewModel,
                                    onOpenCreatorProfile = { cid -> navController.navigate(JanzRoutes.creator(cid)) },
                                    onOpenUpload = { navigateTopLevel(JanzRoutes.CREATE) }
                                )
                            }

                            composable(JanzRoutes.PROFILE) {
                                ProfileScreen(
                                    creatorId = user.uid,
                                    isBottomTab = true,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenVideo = { vid -> navController.navigate(JanzRoutes.watch(vid)) },
                                    onOpenShorts = { navigateTopLevel(JanzRoutes.SHORTS) },
                                    onOpenDashboard = { navController.navigate(JanzRoutes.dashboard(0)) },
                                    onOpenMonetize = { navController.navigate(JanzRoutes.dashboard(1)) },
                                    onOpenSettings = { navController.navigate(JanzRoutes.SETTINGS) },
                                    onOpenUpload = { navigateTopLevel(JanzRoutes.CREATE) }
                                )
                            }

                            composable(
                                route = JanzRoutes.WATCH,
                                arguments = listOf(navArgument("videoId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val vid = backStackEntry.arguments?.getString("videoId").orEmpty()
                                VideoPlayerScreen(
                                    videoId = vid,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onSelectVideo = { nextId ->
                                        navController.navigate(JanzRoutes.watch(nextId)) {
                                            popUpTo(JanzRoutes.WATCH) { inclusive = true }
                                        }
                                    },
                                    onOpenCreatorProfile = { cid -> navController.navigate(JanzRoutes.creator(cid)) }
                                )
                            }

                            composable(
                                route = JanzRoutes.CREATOR,
                                arguments = listOf(navArgument("creatorId") { type = NavType.StringType })
                            ) { backStackEntry ->
                                val cid = backStackEntry.arguments?.getString("creatorId").orEmpty()
                                ProfileScreen(
                                    creatorId = cid,
                                    isBottomTab = false,
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenVideo = { vid -> navController.navigate(JanzRoutes.watch(vid)) },
                                    onOpenShorts = { navigateTopLevel(JanzRoutes.SHORTS) },
                                    onOpenDashboard = { navController.navigate(JanzRoutes.dashboard(0)) },
                                    onOpenMonetize = { navController.navigate(JanzRoutes.dashboard(1)) },
                                    onOpenSettings = { navController.navigate(JanzRoutes.SETTINGS) },
                                    onOpenUpload = { navigateTopLevel(JanzRoutes.CREATE) }
                                )
                            }

                            composable(
                                route = JanzRoutes.DASHBOARD,
                                arguments = listOf(
                                    navArgument("tab") {
                                        type = NavType.IntType
                                        defaultValue = 0
                                    }
                                )
                            ) { backStackEntry ->
                                val initialTab = backStackEntry.arguments?.getInt("tab") ?: 0
                                CreatorDashboardScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenUpload = { navigateTopLevel(JanzRoutes.CREATE) },
                                    onOpenVideo = { vid -> navController.navigate(JanzRoutes.watch(vid)) },
                                    initialTab = initialTab
                                )
                            }

                            composable(JanzRoutes.NOTIFICATIONS) {
                                NotificationsScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenVideo = { vid -> navController.navigate(JanzRoutes.watch(vid)) }
                                )
                            }

                            composable(JanzRoutes.SETTINGS) {
                                SettingsPrivacyScreen(
                                    viewModel = viewModel,
                                    onBack = { navController.popBackStack() },
                                    onOpenAdmin = { navController.navigate(JanzRoutes.ADMIN) }
                                )
                            }

                            composable(JanzRoutes.ADMIN) {
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
    }
}

@Composable
private fun JanzBottomBar(
    currentRoute: String?,
    onNavigate: (String) -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier.navigationBarsPadding()
        ) {
            NavigationBarItem(
                selected = currentRoute == JanzRoutes.HOME,
                onClick = { onNavigate(JanzRoutes.HOME) },
                icon = {
                    Icon(
                        if (currentRoute == JanzRoutes.HOME) Icons.Filled.Home else Icons.Outlined.Home,
                        contentDescription = "Home"
                    )
                },
                label = { Text("Home", maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("nav_home")
            )
            NavigationBarItem(
                selected = currentRoute == JanzRoutes.EXPLORE,
                onClick = { onNavigate(JanzRoutes.EXPLORE) },
                icon = {
                    Icon(
                        if (currentRoute == JanzRoutes.EXPLORE) Icons.Filled.Explore else Icons.Outlined.Explore,
                        contentDescription = "Explore"
                    )
                },
                label = { Text("Explore", maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("nav_explore")
            )

            // Prominent Signature Create (+) Button
            Box(
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 6.dp)
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Brush.linearGradient(listOf(FlareOrange, ElectricViolet)))
                    .clickable { onNavigate(JanzRoutes.CREATE) }
                    .testTag("nav_create"),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = "Create Video",
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }

            NavigationBarItem(
                selected = currentRoute == JanzRoutes.SHORTS,
                onClick = { onNavigate(JanzRoutes.SHORTS) },
                icon = {
                    Icon(
                        if (currentRoute == JanzRoutes.SHORTS) Icons.Filled.PlayCircle else Icons.Outlined.PlayCircle,
                        contentDescription = "Shorts"
                    )
                },
                label = { Text("Shorts", maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("nav_shorts")
            )
            NavigationBarItem(
                selected = currentRoute == JanzRoutes.PROFILE,
                onClick = { onNavigate(JanzRoutes.PROFILE) },
                icon = {
                    Icon(
                        if (currentRoute == JanzRoutes.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                        contentDescription = "Profile"
                    )
                },
                label = { Text("Profile", maxLines = 1) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.testTag("nav_profile")
            )
        }
    }
}
