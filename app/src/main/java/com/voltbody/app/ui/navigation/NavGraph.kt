package com.voltbody.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import com.voltbody.app.domain.model.AppTab
import com.voltbody.app.ui.screens.aicoach.AiCoachScreen
import com.voltbody.app.ui.screens.calendar.CalendarScreen
import com.voltbody.app.ui.screens.diet.DietScreen
import com.voltbody.app.ui.screens.home.HomeScreen
import com.voltbody.app.ui.screens.login.LoginScreen
import com.voltbody.app.ui.screens.onboarding.OnboardingScreen
import com.voltbody.app.ui.screens.profile.ProfileScreen
import com.voltbody.app.ui.screens.workout.WorkoutScreen
import com.voltbody.app.ui.viewmodel.AppViewModel

sealed class Screen(val route: String) {
    object Login : Screen("login")
    object Onboarding : Screen("onboarding")
    object Home : Screen("home")
    object Workout : Screen("workout")
    object Diet : Screen("diet")
    object Calendar : Screen("calendar")
    object Profile : Screen("profile")
    object AiCoach : Screen("ai_coach")
}

@Composable
fun VoltBodyNavHost(
    navController: NavHostController,
    appViewModel: AppViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val isAuthenticated by appViewModel.isAuthenticated.collectAsState()
    val isOnboarded by appViewModel.isOnboarded.collectAsState()
    val hasHydrated by appViewModel.hasHydrated.collectAsState()

    val startDestination = when {
        !hasHydrated -> Screen.Home.route
        !isAuthenticated -> Screen.Login.route
        !isOnboarded -> Screen.Onboarding.route
        else -> Screen.Home.route
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(tween(180)) + slideInVertically(tween(180)) { it / 20 }
        },
        exitTransition = {
            fadeOut(tween(120)) + slideOutVertically(tween(120)) { -it / 20 }
        },
        popEnterTransition = {
            fadeIn(tween(180)) + slideInVertically(tween(180)) { -it / 20 }
        },
        popExitTransition = {
            fadeOut(tween(120)) + slideOutVertically(tween(120)) { it / 20 }
        }
    ) {
        composable(Screen.Login.route) {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onNeedsOnboarding = {
                    navController.navigate(Screen.Onboarding.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Onboarding.route) {
            OnboardingScreen(
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.Home.route) { 
            HomeScreen(
                onNavigateToWorkout = { day ->
                    navController.navigate(Screen.Workout.route)
                },
                onNavigateToDiet = {
                    navController.navigate(Screen.Diet.route)
                },
                onNavigateToProfile = {
                    navController.navigate(Screen.Profile.route)
                }
            )
        }

        composable(Screen.Workout.route) { WorkoutScreen() }
        composable(Screen.Diet.route) { DietScreen() }
        composable(Screen.Calendar.route) { CalendarScreen() }
        composable(Screen.AiCoach.route) { AiCoachScreen() }
        
        composable(Screen.Profile.route) {
            ProfileScreen(
                onLogout = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }
    }
}
