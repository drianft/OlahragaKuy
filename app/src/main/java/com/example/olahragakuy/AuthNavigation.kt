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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.olahragakuy.pages.HomeScreen
import com.example.olahragakuy.pages.posts.ChatScreen
import com.example.olahragakuy.pages.posts.CreatePostScreen
import com.example.olahragakuy.pages.posts.NotificationScreen
import com.example.olahragakuy.pages.posts.SearchPostScreen
import com.example.olahragakuy.pages.LoginScreen
import com.example.olahragakuy.pages.OpeningScreen
import com.example.olahragakuy.pages.RegisterScreen
import com.example.olahragakuy.ui.theme.KuyNavy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

// Figma: Login -> Home uses Dissolve, Ease Out, 300ms.
private const val HOME_DISSOLVE_MS = 300

@Composable
fun AuthNavigation(modifier: Modifier = Modifier, onExit: () -> Unit = {}) {
    val navController = rememberNavController()
    var currentUserRole by rememberSaveable { mutableStateOf("member") }
    var prefilledEmail by rememberSaveable { mutableStateOf("") }
    val startDestination = remember {
        if (FirebaseAuth.getInstance().currentUser != null) "home" else "opening"
    }

    LaunchedEffect(Unit) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            currentUser.reload().addOnCompleteListener { reloadTask ->
                if (!reloadTask.isSuccessful && reloadTask.exception is FirebaseAuthInvalidUserException) {
                    FirebaseAuth.getInstance().signOut()
                    navController.navigate("opening") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                } else if (reloadTask.isSuccessful) {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(currentUser.uid)
                        .get()
                        .addOnSuccessListener { doc ->
                            val role = doc.getString("role") ?: "member"
                            if (role != "admin" && !currentUser.isEmailVerified) {
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("opening") {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            } else {
                                currentUserRole = role
                            }
                        }
                        .addOnFailureListener {
                            if (!currentUser.isEmailVerified) {
                                FirebaseAuth.getInstance().signOut()
                                navController.navigate("opening") {
                                    popUpTo(0) { inclusive = true }
                                    launchSingleTop = true
                                }
                            }
                        }
                }
            }
        }
    }

    val navigateToLogin: () -> Unit = {
        navController.navigate("login") {
            popUpTo("register") { inclusive = true }
            launchSingleTop = true
        }
    }

    NavHost(
        navController = navController,
        startDestination = startDestination,
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
                onLoginClick = {
                    if (FirebaseAuth.getInstance().currentUser != null) {
                        navController.navigate("home") {
                            popUpTo("opening") { inclusive = true }
                            launchSingleTop = true
                        }
                    } else {
                        navController.navigate("login") { launchSingleTop = true }
                    }
                },
                onRegisterClick = { navController.navigate("register") { launchSingleTop = true } },
            )
        }
        composable("login") {
            LoginScreen(
                initialEmail = prefilledEmail,
                onBack = { if (!navController.popBackStack()) onExit() },
                onRegistered = { navController.navigate("register") { launchSingleTop = true } },
                onLoggedIn = { role ->
                    currentUserRole = role
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
                onRegisteredWithEmail = { prefilledEmail = it },
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
            HomeScreen(
                onCreatePost = {
                    navController.navigate("create_post") { launchSingleTop = true }
                },
                onSearchClick = {
                    navController.navigate("search_posts") { launchSingleTop = true }
                },
                onNotificationClick = {
                    navController.navigate("notifications") { launchSingleTop = true }
                },
                onChatClick = {
                    navController.navigate("chat") { launchSingleTop = true }
                },
            )
        }
        composable("create_post") {
            CreatePostScreen(
                onBack = { navController.popBackStack() },
                onPosted = { navController.popBackStack() },
            )
        }
        composable("search_posts") {
            SearchPostScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable("notifications") {
            NotificationScreen(
                onBack = { navController.popBackStack() },
            )
        }
        composable("chat") {
            ChatScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }


}



