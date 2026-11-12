package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.auth.authStateFlow
import com.example.auth.signOutUser
import com.example.data.ai.AiStudyService
import com.example.data.firebase.configureAppCheck
import com.example.data.model.ThemeMode
import com.example.data.repository.HscFirestoreRepository
import com.example.ui.components.HscTopNavbar
import com.example.ui.screens.AdminPanelScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.BookmarksScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.QuickExamScreen
import com.example.ui.screens.QuizScreen
import com.example.ui.screens.SubjectDetailScreen
import com.example.ui.screens.SubjectsHomeScreen
import com.example.ui.theme.HscEdgeTheme
import com.example.ui.viewmodel.HscViewModel
import com.example.ui.viewmodel.RegistrationDraft
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

private object AppRoutes {
    const val HOME = "home"
    const val EXAM = "exam"
    const val BOOKMARKS = "bookmarks"
    const val PROFILE = "profile"
    const val ADMIN = "admin"
    const val SUBJECT_DETAIL = "subject_detail/{subjectId}"
    const val QUIZ = "quiz/{subjectId}?chapterId={chapterId}"

    fun subjectDetail(subjectId: String) = "subject_detail/$subjectId"
    fun quiz(subjectId: String, chapterId: String?) =
        if (chapterId.isNullOrBlank()) "quiz/$subjectId" else "quiz/$subjectId?chapterId=$chapterId"
}

private enum class TopLevelTab(
    val route: String,
    val label: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME(AppRoutes.HOME, "Home", "HSC Edge", Icons.Rounded.Home, Icons.Outlined.Home),
    EXAM(AppRoutes.EXAM, "Exam", "Quick Exam", Icons.Rounded.Timer, Icons.Outlined.Timer),
    BOOKMARKS(AppRoutes.BOOKMARKS, "Bookmarks", "Bookmarks", Icons.Rounded.Bookmark, Icons.Outlined.BookmarkBorder),
    PROFILE(AppRoutes.PROFILE, "Profile", "Profile", Icons.Rounded.Person, Icons.Outlined.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Mandatory Step 1: Configure App Check with launch intent before UI or ViewModels
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            AppNavigation()
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(
        initialValue = auth.currentUser
    )
    var pendingRegistration by remember { mutableStateOf<RegistrationDraft?>(null) }
    var showAuthScreen by remember { mutableStateOf(false) }
    val user = currentUser

    LaunchedEffect(user?.uid) {
        if (user != null) {
            showAuthScreen = false
        }
    }

    // Users can access the bundled HSC curriculum immediately without registration;
    // Firestore cloud sync & Admin Panel remain strictly gated behind Google Sign-In.
    if (user == null && showAuthScreen) {
        HscEdgeTheme(themeMode = ThemeMode.SYSTEM) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                AuthScreen(
                    onPrepareRegistration = { draft -> pendingRegistration = draft },
                    onAuthSuccess = { showAuthScreen = false },
                    onContinueAsGuest = { showAuthScreen = false }
                )
            }
        }
    } else {
        key(user?.uid ?: "guest") {
            MainAppShell(
                user = user,
                pendingRegistration = pendingRegistration,
                onRegistrationConsumed = { pendingRegistration = null },
                onRequestSignIn = { showAuthScreen = true }
            )
        }
    }
}

@Composable
private fun MainAppShell(
    user: FirebaseUser?,
    pendingRegistration: RegistrationDraft?,
    onRegistrationConsumed: () -> Unit,
    onRequestSignIn: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val aiService = remember { AiStudyService() }

    val hscViewModel: HscViewModel = viewModel(
        key = "hsc_${user?.uid ?: "guest"}",
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val repo = if (user != null) {
                    val databaseId = app.getString(R.string.firestore_database_id)
                    HscFirestoreRepository(FirebaseFirestore.getInstance(databaseId))
                } else {
                    null
                }
                HscViewModel(
                    repository = repo,
                    aiService = aiService,
                    currentUserId = user?.uid ?: "guest",
                    userDisplayName = user?.displayName ?: "HSC Scholar",
                    userEmail = user?.email,
                    isEmailVerified = user?.isEmailVerified == true,
                    pendingRegistration = pendingRegistration,
                    onRegistrationConsumed = onRegistrationConsumed
                )
            }
        }
    )

    val userSettings by hscViewModel.userSettings.collectAsStateWithLifecycle()
    val userProfile by hscViewModel.userProfile.collectAsStateWithLifecycle()
    val currentRole = hscViewModel.currentRole
    val isAdmin = hscViewModel.isAdmin
    val isGuest = hscViewModel.isGuest
    val themeMode = ThemeMode.fromKey(userSettings.themeMode)

    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    var pendingAdminSubjectId by remember { mutableStateOf<String?>(null) }

    val activeTab = TopLevelTab.entries.firstOrNull { it.route == currentRoute }
    val showTopLevelChrome = activeTab != null
    val boardName = userProfile?.board?.takeIf { it.isNotBlank() } ?: "Dhaka"

    val navigateTab: (String) -> Unit = { route ->
        navController.navigate(route) {
            popUpTo(AppRoutes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    val openAdminIfAuthorized: () -> Unit = {
        if (isAdmin) {
            navController.navigate(AppRoutes.ADMIN) { launchSingleTop = true }
        }
    }

    val doSignOut: () -> Unit = {
        signOutUser(credentialManager = credentialManager, scope = scope)
    }

    HscEdgeTheme(themeMode = themeMode) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp

                Scaffold(
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        if (showTopLevelChrome) {
                            HscTopNavbar(
                                title = activeTab?.title ?: "HSC Edge",
                                subtitle = "$boardName Board • Bangladesh HSC",
                                role = currentRole,
                                themeMode = themeMode,
                                isGuest = isGuest,
                                onToggleTheme = {
                                    val nextTheme = if (themeMode == ThemeMode.DARK) ThemeMode.LIGHT else ThemeMode.DARK
                                    hscViewModel.updateSettings(themeMode = nextTheme)
                                },
                                onOpenProfile = { navigateTab(AppRoutes.PROFILE) },
                                onOpenAdminPanel = if (isAdmin) openAdminIfAuthorized else null,
                                onOpenSignIn = if (isGuest) onRequestSignIn else null,
                                onSignOut = doSignOut
                            )
                        }
                    },
                    bottomBar = {
                        if (showTopLevelChrome && !useNavigationRail) {
                            NavigationBar {
                                TopLevelTab.entries.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationBarItem(
                                        selected = selected,
                                        onClick = { navigateTab(tab.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label, maxLines = 1) },
                                        modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }
                    }
                ) { innerPadding ->
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        if (showTopLevelChrome && useNavigationRail) {
                            NavigationRail {
                                TopLevelTab.entries.forEach { tab ->
                                    val selected = currentRoute == tab.route
                                    NavigationRailItem(
                                        selected = selected,
                                        onClick = { navigateTab(tab.route) },
                                        icon = {
                                            Icon(
                                                imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        },
                                        label = { Text(tab.label) },
                                        modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                                    )
                                }
                            }
                        }

                        NavHost(
                            navController = navController,
                            startDestination = AppRoutes.HOME,
                            modifier = Modifier.weight(1f)
                        ) {
                            composable(AppRoutes.HOME) {
                                SubjectsHomeScreen(
                                    viewModel = hscViewModel,
                                    userName = userProfile?.displayName?.takeIf { it.isNotBlank() }
                                        ?: user?.displayName ?: "Scholar",
                                    onSubjectClick = { subjectId ->
                                        navController.navigate(AppRoutes.subjectDetail(subjectId))
                                    },
                                    onStartQuickExam = { navigateTab(AppRoutes.EXAM) }
                                )
                            }

                            composable(AppRoutes.EXAM) {
                                QuickExamScreen(viewModel = hscViewModel)
                            }

                            composable(AppRoutes.BOOKMARKS) {
                                BookmarksScreen(viewModel = hscViewModel)
                            }

                            composable(AppRoutes.PROFILE) {
                                ProfileScreen(
                                    viewModel = hscViewModel,
                                    userEmail = user?.email,
                                    onOpenAdminPanel = openAdminIfAuthorized,
                                    onOpenSignIn = onRequestSignIn,
                                    onSignOut = doSignOut
                                )
                            }

                            composable(
                                route = AppRoutes.SUBJECT_DETAIL,
                                arguments = listOf(navArgument("subjectId") { type = NavType.StringType })
                            ) { backStack ->
                                val subjectId = backStack.arguments?.getString("subjectId").orEmpty()
                                SubjectDetailScreen(
                                    subjectId = subjectId,
                                    viewModel = hscViewModel,
                                    onBack = { navController.popBackStack() },
                                    onStartQuiz = { subId, chId ->
                                        navController.navigate(AppRoutes.quiz(subId, chId))
                                    },
                                    onOpenAdminForSubject = {
                                        if (isAdmin) {
                                            pendingAdminSubjectId = subjectId
                                            openAdminIfAuthorized()
                                        }
                                    }
                                )
                            }

                            composable(
                                route = AppRoutes.QUIZ,
                                arguments = listOf(
                                    navArgument("subjectId") { type = NavType.StringType },
                                    navArgument("chapterId") {
                                        type = NavType.StringType
                                        nullable = true
                                        defaultValue = null
                                    }
                                )
                            ) { backStack ->
                                val subjectId = backStack.arguments?.getString("subjectId").orEmpty()
                                val chapterId = backStack.arguments?.getString("chapterId")
                                QuizScreen(
                                    subjectId = subjectId,
                                    chapterId = chapterId,
                                    viewModel = hscViewModel,
                                    onBack = { navController.popBackStack() }
                                )
                            }

                            // Admin route is strictly guarded; non-admin users are immediately redirected Home
                            composable(AppRoutes.ADMIN) {
                                if (isAdmin) {
                                    AdminPanelScreen(
                                        viewModel = hscViewModel,
                                        initialSubjectId = pendingAdminSubjectId,
                                        onBack = { navController.popBackStack() },
                                        onUnauthorized = {
                                            navController.navigate(AppRoutes.HOME) {
                                                popUpTo(AppRoutes.HOME) { inclusive = false }
                                                launchSingleTop = true
                                            }
                                        }
                                    )
                                } else {
                                    LaunchedEffect(Unit) {
                                        navController.navigate(AppRoutes.HOME) {
                                            popUpTo(AppRoutes.HOME) { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
