package com.skillbridge.sa.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType
import com.skillbridge.sa.data.model.UserProfile
import com.skillbridge.sa.ui.components.SbBottomNavBar
import com.skillbridge.sa.ui.screens.admin.AdminUsersScreen
import com.skillbridge.sa.ui.screens.auth.LoginScreen
import com.skillbridge.sa.ui.screens.auth.RegisterScreen
import com.skillbridge.sa.ui.screens.dashboard.HomeDashboardScreen
import com.skillbridge.sa.ui.screens.explore.ExploreScreen
import com.skillbridge.sa.ui.screens.learning.CareerPathwaysScreen
import com.skillbridge.sa.ui.screens.learning.LearningCentreScreen
import com.skillbridge.sa.ui.screens.mentorship.MentorshipScreen
import com.skillbridge.sa.ui.screens.messages.ConversationScreen
import com.skillbridge.sa.ui.screens.messages.MessagesListScreen
import com.skillbridge.sa.ui.screens.notifications.NotificationsScreen
import com.skillbridge.sa.ui.screens.opportunity.ApplicationsScreen
import com.skillbridge.sa.ui.screens.opportunity.OpportunityDetailScreen
import com.skillbridge.sa.ui.screens.profile.ProfileScreen
import com.skillbridge.sa.ui.screens.profile.ProfileViewScreen
import com.skillbridge.sa.ui.screens.settings.SettingsScreen
import com.skillbridge.sa.ui.screens.splash.OnboardingScreen
import com.skillbridge.sa.ui.screens.splash.SplashScreen
import com.skillbridge.sa.viewmodel.AppViewModel

private val bottomNavRoutes = setOf(
    Routes.HOME, Routes.EXPLORE, Routes.MESSAGES, Routes.NOTIFICATIONS, Routes.PROFILE
)

@Composable
fun SkillBridgeNavGraph(
    viewModel: AppViewModel,
    onGoogleSignInClick: () -> Unit
) {
    val navController = rememberNavController()
    val profile by viewModel.currentProfile.collectAsState()
    val sessionCheckDone by viewModel.isSessionCheckDone.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val showBottomBar = currentRoute in bottomNavRoutes && profile != null

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                SbBottomNavBar(currentRoute = currentRoute) { route ->
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Routes.SPLASH,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.SPLASH) {
                SplashScreen(onFinished = {
                    viewModel.tryRestoreSession()
                })

                androidx.compose.runtime.LaunchedEffect(sessionCheckDone) {
                    if (sessionCheckDone) {
                        val dest = if (profile != null) Routes.HOME else Routes.ONBOARDING
                        navController.navigate(dest) { popUpTo(Routes.SPLASH) { inclusive = true } }
                    }
                }
            }
            composable(Routes.ONBOARDING) {
                OnboardingScreen(onDone = {
                    navController.navigate(Routes.LOGIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                })
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                    onGoToRegister = { navController.navigate(Routes.REGISTER) },
                    onGoogleSignInClick = onGoogleSignInClick
                )
            }
            composable(Routes.REGISTER) {
                RegisterScreen(
                    viewModel = viewModel,
                    onRegisterSuccess = {
                        navController.navigate(Routes.HOME) { popUpTo(Routes.LOGIN) { inclusive = true } }
                    },
                    onGoToLogin = { navController.popBackStack() }
                )
            }
            composable(Routes.HOME) {
                val p = profile
                if (p != null) {
                    HomeDashboardScreen(
                        profile = p,
                        repo = viewModel.repo(),
                        onOpenExplore = { navController.navigate(Routes.EXPLORE) },
                        onOpenLearning = { navController.navigate(Routes.LEARNING_CENTRE) },
                        onOpenMentorship = { navController.navigate(Routes.MENTORSHIP) },
                        onOpenCareerPathways = { navController.navigate(Routes.CAREER_PATHWAYS) },
                        onOpenApplications = { navController.navigate("applications") },
                        onOpenAdminUsers = { navController.navigate(Routes.ADMIN_USERS) },
                        onOpenMessages = { navController.navigate(Routes.MESSAGES) }
                    )
                } else {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.padding(padding)
                    )
                }
            }
            composable(Routes.EXPLORE) {
                val p = profile ?: return@composable
                ExploreScreen(
                    repo = viewModel.repo(),
                    currentUser = p,
                    onOpenOpportunity = { id -> navController.navigate(Routes.opportunityDetailRoute(id)) },
                    onOpenProfile = { userId -> navController.navigate(Routes.profileViewRoute(userId)) }
                )
            }
            composable(
                Routes.OPPORTUNITY_DETAIL,
                arguments = listOf(navArgument("opportunityId") { type = NavType.StringType })
            ) { backStack ->
                val p = profile ?: return@composable
                val id = backStack.arguments?.getString("opportunityId") ?: return@composable
                OpportunityDetailScreen(
                    opportunityId = id,
                    profile = p,
                    repo = viewModel.repo(),
                    onOpenConversation = { convId, otherUserId ->
                        navController.navigate(Routes.conversationRoute(convId, otherUserId))
                    },
                    onOpenProfile = { userId ->
                        navController.navigate(Routes.profileViewRoute(userId))
                    }
                )
            }
            composable("applications") {
                val p = profile ?: return@composable
                ApplicationsScreen(
                    profile = p,
                    repo = viewModel.repo(),
                    onOpenProfile = { userId ->
                        navController.navigate(Routes.profileViewRoute(userId))
                    }
                )
            }
            composable(Routes.MESSAGES) {
                val p = profile ?: return@composable
                MessagesListScreen(
                    profile = p,
                    repo = viewModel.repo(),
                    onOpenConversation = { convId, otherUserId ->
                        navController.navigate(Routes.conversationRoute(convId, otherUserId))
                    }
                )
            }
            composable(
                Routes.CONVERSATION,
                arguments = listOf(
                    navArgument("conversationId") { type = NavType.StringType },
                    navArgument("otherUserId") { type = NavType.StringType }
                )
            ) { backStack ->
                val p = profile ?: return@composable
                val convId = backStack.arguments?.getString("conversationId") ?: return@composable
                val otherUserId = backStack.arguments?.getString("otherUserId") ?: return@composable
                ConversationScreen(
                    conversationId = convId,
                    otherUserId = otherUserId,
                    profile = p,
                    repo = viewModel.repo(),
                    onBack = { navController.popBackStack() }
                )
            }
            composable(Routes.NOTIFICATIONS) {
                val p = profile ?: return@composable
                NotificationsScreen(profile = p, repo = viewModel.repo())
            }
            composable(Routes.PROFILE) {
                val p = profile ?: return@composable
                ProfileScreen(
                    profile = p,
                    onSave = { viewModel.updateProfile(it) },
                    onLogout = {
                        viewModel.signOut()
                        navController.navigate(Routes.LOGIN) { popUpTo(0) { inclusive = true } }
                    }
                )
            }
            composable(
                Routes.PROFILE_VIEW,
                arguments = listOf(navArgument("userId") { type = NavType.StringType })
            ) { backStack ->
                val p = profile ?: return@composable
                val userId = backStack.arguments?.getString("userId") ?: return@composable
                ProfileViewScreen(
                    userId = userId,
                    currentUser = p,
                    repo = viewModel.repo(),
                    onBack = { navController.popBackStack() },
                    onOpenConversation = { convId, otherId ->
                        navController.navigate(Routes.conversationRoute(convId, otherId))
                    }
                )
            }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.LEARNING_CENTRE) { LearningCentreScreen(repo = viewModel.repo()) }
            composable(Routes.MENTORSHIP) {
                val p = profile ?: return@composable
                MentorshipScreen(
                    profile = p,
                    repo = viewModel.repo(),
                    onOpenConversation = { convId, otherUserId ->
                        navController.navigate(Routes.conversationRoute(convId, otherUserId))
                    },
                    onOpenProfile = { userId ->
                        navController.navigate(Routes.profileViewRoute(userId))
                    }
                )
            }
            composable(Routes.CAREER_PATHWAYS) {
                val p = profile ?: return@composable
                CareerPathwaysScreen(profile = p)
            }
            composable(Routes.ADMIN_USERS) {
                if (profile?.role == com.skillbridge.sa.data.model.UserRole.ADMIN) {
                    AdminUsersScreen(repo = viewModel.repo())
                } else {
                    // Redirect or show error. For now, just show Dashboard
                    val p = profile ?: return@composable
                    HomeDashboardScreen(
                        profile = p,
                        repo = viewModel.repo(),
                        onOpenExplore = { navController.navigate(Routes.EXPLORE) },
                        onOpenLearning = { navController.navigate(Routes.LEARNING_CENTRE) },
                        onOpenMentorship = { navController.navigate(Routes.MENTORSHIP) },
                        onOpenCareerPathways = { navController.navigate(Routes.CAREER_PATHWAYS) },
                        onOpenApplications = { navController.navigate("applications") },
                        onOpenAdminUsers = { navController.navigate(Routes.ADMIN_USERS) },
                        onOpenMessages = { navController.navigate(Routes.MESSAGES) }
                    )
                }
            }
        }
    }
}
