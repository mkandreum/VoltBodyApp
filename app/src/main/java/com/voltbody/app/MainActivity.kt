package com.voltbody.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.voltbody.app.domain.model.AppTab
import com.voltbody.app.service.VoltBodyNotificationService
import com.voltbody.app.ui.components.ToastOverlay
import com.voltbody.app.ui.components.VoltBodyBottomNav
import com.voltbody.app.ui.navigation.Screen
import com.voltbody.app.ui.navigation.VoltBodyNavHost
import com.voltbody.app.ui.theme.*
import com.voltbody.app.ui.viewmodel.AppViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var notificationService: VoltBodyNotificationService

    override fun onCreate(savedInstanceState: Bundle?) {
        // Splash screen must be installed BEFORE super.onCreate so the
        // splash theme is applied before the window is fully created.
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // enableEdgeToEdge() must be called AFTER super.onCreate and BEFORE
        // setContent. On Android 15+ (API 35) edge-to-edge is enforced by the
        // platform; calling it explicitly ensures correct behaviour on 14 and
        // below as well, and sets up the WindowInsetsController so our Compose
        // content can draw behind system bars.
        enableEdgeToEdge()
        enable120fps()
        notificationService.createChannels()

        setContent {
            val appViewModel: AppViewModel = hiltViewModel()
            val hasHydrated by appViewModel.hasHydrated.collectAsState()
            val theme by appViewModel.theme.collectAsState()

            splashScreen.setKeepOnScreenCondition { !hasHydrated }

            VoltBodyTheme(appTheme = theme) {
                VoltBodyRoot(appViewModel = appViewModel)
            }
        }
    }
}

@Composable
private fun VoltBodyRoot(appViewModel: AppViewModel) {
    val navController = rememberNavController()
    val isAuthenticated by appViewModel.isAuthenticated.collectAsState()
    val isOnboarded by appViewModel.isOnboarded.collectAsState()
    val currentTab by appViewModel.currentTab.collectAsState()
    val toasts by appViewModel.toasts.collectAsState()

    val showBottomNav = isAuthenticated && isOnboarded

    // Sync tab with nav controller
    LaunchedEffect(currentTab) {
        if (!showBottomNav) return@LaunchedEffect
        val route = currentTab.toRoute()
        val current = navController.currentDestination?.route
        if (route != current) {
            navController.navigate(route) {
                popUpTo(Screen.Home.route) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            // Transparent container so our custom dark background (set in
            // VoltBodyTheme) shows through without any M3 surface tinting.
            containerColor = androidx.compose.ui.graphics.Color.Transparent,
            contentColor = LocalVoltBodyColors.current.textPrimary,
            // Scaffold must NOT consume insets itself — we handle them
            // per-screen so each screen can position content relative to
            // system bars exactly as needed (e.g. chat input above IME).
            contentWindowInsets = WindowInsets(0)
        ) { innerPadding ->
            // Pass innerPadding to NavHost so Scaffold-owned padding
            // (e.g. bottom bar height when it IS inside Scaffold) is
            // respected. Since our bottom nav is outside Scaffold, the
            // padding here is effectively zero, but it is correct to
            // propagate it for future-proofing.
            VoltBodyNavHost(
                navController = navController,
                appViewModel = appViewModel,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            )
        }

        // ── Floating bottom nav ───────────────────────────────────────────
        // Lives outside Scaffold so it truly floats over content and the
        // screens below it can draw edge-to-edge. Screens are responsible
        // for adding their own bottom padding to avoid content hiding
        // behind this bar (use WindowInsets.navigationBars + ~80.dp pill).
        AnimatedVisibility(
            visible = showBottomNav,
            enter = slideInVertically { it } + fadeIn(),
            exit = slideOutVertically { it } + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            VoltBodyBottomNav(
                currentTab = currentTab,
                onTabSelected = { tab ->
                    appViewModel.setTab(tab)
                    navController.navigate(tab.toRoute()) {
                        popUpTo(Screen.Home.route) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }

        // ── Toast overlay ─────────────────────────────────────────────────
        ToastOverlay(
            toasts = toasts,
            onDismiss = appViewModel::dismissToast,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )
    }
}

private fun AppTab.toRoute() = when (this) {
    AppTab.HOME -> Screen.Home.route
    AppTab.WORKOUT -> Screen.Workout.route
    AppTab.DIET -> Screen.Diet.route
    AppTab.CALENDAR -> Screen.Calendar.route
    AppTab.PROFILE -> Screen.Profile.route
}

/** Request the highest available refresh rate (up to 120 Hz) for smooth animations. */
private fun ComponentActivity.enable120fps() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val displayManager = getSystemService(android.hardware.display.DisplayManager::class.java)
        val display = displayManager?.getDisplay(android.view.Display.DEFAULT_DISPLAY) ?: return
        val bestMode = display.supportedModes.maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.also { lp ->
            lp.preferredDisplayModeId = bestMode.modeId
        }
    } else {
        @Suppress("DEPRECATION")
        window.attributes = window.attributes.also { lp ->
            lp.preferredRefreshRate = 120f
        }
    }
}
