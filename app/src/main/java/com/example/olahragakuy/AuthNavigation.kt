package com.example.olahragakuy

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.olahragakuy.pages.HomeScreen
import com.example.olahragakuy.pages.LoginScreen
import com.example.olahragakuy.pages.OpeningScreen
import com.example.olahragakuy.pages.RegisterScreen
import com.example.olahragakuy.ui.theme.KuyNavy

// Figma: Login -> Home uses Dissolve, Ease Out, 300ms.
private const val HOME_DISSOLVE_MS = 300

@Composable
fun AuthNavigation(modifier: Modifier = Modifier, onExit: () -> Unit = {}) {
    val navController = rememberNavController()

    val navigateToLogin: () -> Unit = {
        navController.navigate("login") {
            popUpTo("register") { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = "opening",
        modifier = modifier.fillMaxSize().background(KuyNavy),
        // Keep both screens opaque so the window never shows through a crossfade.
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                initialOffsetX = { it },
            )
        },
        exitTransition = {
            if (targetState.destination.route == "home") {
                fadeOut(animationSpec = tween(HOME_DISSOLVE_MS, easing = FastOutSlowInEasing))
            } else {
                slideOutHorizontally(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    targetOffsetX = { -it },
                )
            }
        },
        popEnterTransition = {
            slideInHorizontally(
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                initialOffsetX = { -it },
            )
        },
        popExitTransition = {
            slideOutHorizontally(
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                targetOffsetX = { it },
            )
        },
    ) {
        composable("opening") {
            // Opening stays in the back stack so Back from Login/Register returns to onboarding.
            OpeningScreen(
                onLoginClick = { navController.navigate("login") { launchSingleTop = true } },
                onRegisterClick = { navController.navigate("register") { launchSingleTop = true } },
            )
        }
        composable("login") {
            LoginScreen(
                onBack = { if (!navController.popBackStack()) onExit() },
                onRegistered = { navController.navigate("register") { launchSingleTop = true } },
                onLoggedIn = {
                    navController.navigate("home") {
                        popUpTo("opening") { inclusive = true }
                    }
                },
            )
        }
        composable("register") {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegistered = navigateToLogin,
                onLogin = navigateToLogin,
            )
        }
        composable(
            route = "home",
            enterTransition = {
                fadeIn(animationSpec = tween(HOME_DISSOLVE_MS, easing = FastOutSlowInEasing))
            },
            exitTransition = {
                fadeOut(animationSpec = tween(HOME_DISSOLVE_MS, easing = FastOutSlowInEasing))
            },
        ) {
            HomeScreen()
        }
    }
}
