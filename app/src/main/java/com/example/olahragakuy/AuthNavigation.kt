package com.example.olahragakuy

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import com.example.olahragakuy.ui.theme.KuyNavy
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.olahragakuy.pages.HomeScreen
import com.example.olahragakuy.pages.posts.CreatePostScreen
import com.example.olahragakuy.pages.LoginScreen
import com.example.olahragakuy.pages.RegisterScreen

@Composable
fun AuthNavigation(modifier: Modifier = Modifier, onExit: () -> Unit = {}) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = "login",
        modifier = modifier.fillMaxSize().background(KuyNavy),
        // Keep both screens opaque so the window never shows through a crossfade.
        enterTransition = {
            slideInHorizontally(
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                initialOffsetX = { it },
            )
        },
        exitTransition = {
            slideOutHorizontally(
                animationSpec = tween(280, easing = FastOutSlowInEasing),
                targetOffsetX = { -it },
            )
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
        composable("login") {
            LoginScreen(
                onBack = onExit,
                onRegistered = { navController.navigate("register") { launchSingleTop = true } },
                onLoggedIn = {navController.navigate("home"){popUpTo("login") { inclusive = true } } }
            )
        }
        composable("register") {
            RegisterScreen(
                onBack = { navController.popBackStack() },
                onRegistered = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                }
            )
        }
        composable("home") {
            HomeScreen(
                onCreatePost = {
                    navController.navigate("create_post") { launchSingleTop = true }
                },
            )
        }
        composable("create_post") {
            CreatePostScreen(
                onBack = { navController.popBackStack() },
                onPosted = { navController.popBackStack() },
            )
        }
    }

}



