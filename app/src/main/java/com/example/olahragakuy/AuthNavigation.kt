package com.example.olahragakuy

import android.Manifest
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavType
import com.example.olahragakuy.ui.components.RefreshRegistry
import com.example.olahragakuy.ui.components.KuyRefreshHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.olahragakuy.pages.HomeScreen
import com.example.olahragakuy.pages.LoginScreen
import com.example.olahragakuy.pages.OpeningScreen
import com.example.olahragakuy.pages.RegisterScreen
import com.example.olahragakuy.pages.clearGoogleCredentialState
import com.example.olahragakuy.pages.posts.ChatScreen
import com.example.olahragakuy.pages.posts.CreatePostScreen
import com.example.olahragakuy.pages.posts.NotificationScreen
import com.example.olahragakuy.pages.posts.RealtimeBadgePrefs
import com.example.olahragakuy.pages.posts.RealtimeBadgeStore
import com.example.olahragakuy.pages.posts.EditPostBridge
import com.example.olahragakuy.pages.posts.EditPostScreen
import com.example.olahragakuy.pages.posts.EditPostTarget
import com.example.olahragakuy.pages.posts.RealtimeSystemNotifier
import com.example.olahragakuy.pages.posts.SearchPostScreen
import com.example.olahragakuy.pages.posts.currentUserOrNull
import com.example.olahragakuy.pages.posts.ensureDirectChatRoom
import com.example.olahragakuy.pages.posts.observeRealtimeBadges
import com.example.olahragakuy.pages.posts.resolveDisplayName
import com.example.olahragakuy.pages.drinkreminder.DrinkReminderScreen
import com.example.olahragakuy.pages.profile.user.LoggedInProfileScreen
import com.example.olahragakuy.pages.profile.user.OtherUserProfileScreen
import com.example.olahragakuy.pages.steptracker.StepTrackerScreen
import com.example.olahragakuy.pages.training.CreateCustomWorkoutScreen
import com.example.olahragakuy.pages.training.CustomWorkoutDetailScreen
import com.example.olahragakuy.pages.training.CustomWorkoutListScreen
import com.example.olahragakuy.pages.training.CustomWorkoutStore
import com.example.olahragakuy.pages.training.ExerciseDetailScreen
import com.example.olahragakuy.pages.training.GuidedWorkoutDaysScreen
import com.example.olahragakuy.pages.training.GuidedWorkoutDetailScreen
import com.example.olahragakuy.pages.training.GuidedWorkoutProgressStore
import com.example.olahragakuy.pages.training.HomeWorkoutScreen
import com.example.olahragakuy.pages.training.TrainingHomeScreen
import com.example.olahragakuy.pages.training.WorkoutFirestoreStore
import com.example.olahragakuy.pages.training.WorkoutSessionScreen
import com.example.olahragakuy.pages.training.completeGuidedWorkoutDayForUser
import com.example.olahragakuy.pages.training.deleteCustomWorkoutFromFirestore
import com.example.olahragakuy.pages.training.observeUserCustomWorkouts
import com.example.olahragakuy.pages.training.observeUserGuidedProgress
import com.example.olahragakuy.pages.training.observeWorkoutFirestoreData
import com.example.olahragakuy.pages.training.saveCustomWorkoutToFirestore
import com.example.olahragakuy.pages.training.streak.TrainingStreakStore
import com.example.olahragakuy.pages.training.streak.buildStreakSubtitle
import com.example.olahragakuy.pages.training.streak.observeUserStreak
import com.example.olahragakuy.pages.training.streak.recordWorkoutCompletedForStreak
import com.example.olahragakuy.pages.venuefinder.VenueChatBridge
import com.example.olahragakuy.pages.venuefinder.VenueChatTarget
import com.example.olahragakuy.pages.venuefinder.VenueFinderScreen
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.theme.KuyNavy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

// Figma: Login -> Home uses Dissolve, Ease Out, 300ms.
private const val HOME_DISSOLVE_MS = 300

@Composable
fun AuthNavigation(
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {},
    openDrinkReminder: Boolean = false,
    onConsumeOpenDrinkReminder: () -> Unit = {},
    openStepTracker: Boolean = false,
    onConsumeOpenStepTracker: () -> Unit = {},
    openChatFromNotification: Boolean = false,
    openChatRoomId: String? = null,
    onConsumeOpenChatFromNotification: () -> Unit = {},
    openNotificationsFromNotification: Boolean = false,
    openNotificationPostId: String? = null,
    onConsumeOpenNotificationsFromNotification: () -> Unit = {},
) {
    val navController = rememberNavController()
    val refreshRegistry = remember { RefreshRegistry() }
    val currentEntry by navController.currentBackStackEntryAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var currentUserRole by rememberSaveable { mutableStateOf("member") }
    var prefilledEmail by rememberSaveable { mutableStateOf("") }
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUserUid by remember { mutableStateOf(auth.currentUser?.uid) }
    val startDestination = remember {
        if (auth.currentUser != null) "home" else "opening"
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    LaunchedEffect(currentUserUid, currentEntry?.destination?.route) {
        val route = currentEntry?.destination?.route
        val isAuthRoute = route == null || route == "opening" || route == "login" || route == "register"
        if (currentUserUid != null &&
            !isAuthRoute &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !RealtimeSystemNotifier.hasNotificationPermission(context) &&
            !RealtimeBadgePrefs.hasRequestedNotificationPermission(context)
        ) {
            RealtimeBadgePrefs.markNotificationPermissionRequested(context)
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    DisposableEffect(auth) {
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            currentUserUid = firebaseAuth.currentUser?.uid
        }
        auth.addAuthStateListener(listener)
        onDispose { auth.removeAuthStateListener(listener) }
    }

    DisposableEffect(currentUserUid) {
        val uid = currentUserUid
        val catalogRegistration = if (uid != null) {
            observeWorkoutFirestoreData(autoSeedIfEmpty = true, refreshRegistry = refreshRegistry)
        } else {
            WorkoutFirestoreStore.clear()
            null
        }
        val guidedProgressRegistration = if (uid != null) {
            observeUserGuidedProgress(userId = uid, refreshRegistry = refreshRegistry)
        } else {
            GuidedWorkoutProgressStore.reset()
            null
        }
        val customWorkoutRegistration = if (uid != null) {
            observeUserCustomWorkouts(userId = uid, refreshRegistry = refreshRegistry)
        } else {
            CustomWorkoutStore.clear()
            null
        }
        val streakRegistration = if (uid != null) {
            observeUserStreak(userId = uid, refreshRegistry = refreshRegistry)
        } else {
            TrainingStreakStore.reset()
            null
        }
        val badgeRegistration = if (uid != null) {
            val displayName = auth.currentUser.resolveDisplayName("")
            observeRealtimeBadges(
                context = context,
                userId = uid,
                userDisplayName = displayName,
                refreshRegistry = refreshRegistry,
            )
        } else {
            RealtimeBadgeStore.reset()
            null
        }
        onDispose {
            catalogRegistration?.remove()
            guidedProgressRegistration?.remove()
            customWorkoutRegistration?.remove()
            streakRegistration?.remove()
            badgeRegistration?.remove()
        }
    }

    LaunchedEffect(openDrinkReminder, currentUserRole) {
        if (!openDrinkReminder) return@LaunchedEffect
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentRoute = navController.currentDestination?.route
        val isOnAuthScreen = currentRoute == "opening" || currentRoute == "login" || currentRoute == "register"
        if (currentUser == null || isOnAuthScreen) {
            onConsumeOpenDrinkReminder()
            return@LaunchedEffect
        }
        val isVerifiedOrAdmin = currentUser.isEmailVerified || currentUserRole == "admin"
        if (isVerifiedOrAdmin) {
            onConsumeOpenDrinkReminder()
            if (currentRoute != "drink_reminder") {
                navController.navigate("drink_reminder") {
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(openStepTracker, currentUserRole) {
        if (!openStepTracker) return@LaunchedEffect
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentRoute = navController.currentDestination?.route
        val isOnAuthScreen = currentRoute == "opening" || currentRoute == "login" || currentRoute == "register"
        if (currentUser == null || isOnAuthScreen) {
            onConsumeOpenStepTracker()
            return@LaunchedEffect
        }
        val isVerifiedOrAdmin = currentUser.isEmailVerified || currentUserRole == "admin"
        if (isVerifiedOrAdmin) {
            onConsumeOpenStepTracker()
            if (currentRoute != "step_tracker") {
                navController.navigate("step_tracker") {
                    launchSingleTop = true
                }
            }
        }
    }

    LaunchedEffect(openChatFromNotification, openChatRoomId, currentUserRole) {
        if (!openChatFromNotification) return@LaunchedEffect
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentRoute = navController.currentDestination?.route
        val isOnAuthScreen = currentRoute == "opening" || currentRoute == "login" || currentRoute == "register"
        if (currentUser == null || isOnAuthScreen) {
            onConsumeOpenChatFromNotification()
            return@LaunchedEffect
        }
        val isVerifiedOrAdmin = currentUser.isEmailVerified || currentUserRole == "admin"
        if (isVerifiedOrAdmin) {
            val targetRoomId = openChatRoomId?.trim().orEmpty()
            onConsumeOpenChatFromNotification()
            VenueChatBridge.setPendingTarget(null)
            val route = if (targetRoomId.isNotEmpty()) {
                "chat?returnDirectlyOnBack=false&roomId=${Uri.encode(targetRoomId)}"
            } else {
                "chat?returnDirectlyOnBack=false"
            }
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
    }

    LaunchedEffect(openNotificationsFromNotification, openNotificationPostId, currentUserRole) {
        if (!openNotificationsFromNotification) return@LaunchedEffect
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentRoute = navController.currentDestination?.route
        val isOnAuthScreen = currentRoute == "opening" || currentRoute == "login" || currentRoute == "register"
        if (currentUser == null || isOnAuthScreen) {
            onConsumeOpenNotificationsFromNotification()
            return@LaunchedEffect
        }
        val isVerifiedOrAdmin = currentUser.isEmailVerified || currentUserRole == "admin"
        if (isVerifiedOrAdmin) {
            val targetPostId = openNotificationPostId?.trim().orEmpty()
            onConsumeOpenNotificationsFromNotification()
            val route = if (targetPostId.isNotEmpty()) {
                "notifications?postId=${Uri.encode(targetPostId)}"
            } else {
                "notifications"
            }
            navController.navigate(route) {
                launchSingleTop = true
            }
        }
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

    val navigateToTab: (HomeNavTab) -> Unit = { tab ->
        val targetRoute = when (tab) {
            HomeNavTab.KOMUNITAS -> "home"
            HomeNavTab.LOKASI -> "lokasi"
            HomeNavTab.LATIHAN -> "latihan"
            HomeNavTab.PROFIL -> "profile"
        }
        if (navController.currentDestination?.route != targetRoute) {
            navController.navigate(targetRoute) {
                popUpTo("home") {
                    inclusive = false
                }
                launchSingleTop = true
            }
        }
    }

    val navigateToUserProfile: (String, String) -> Unit = { userId, displayName ->
        val cleanUid = userId.trim()
        val currentUser = FirebaseAuth.getInstance().currentUser
        val currentUid = currentUser?.uid
        val currentDisplayName = currentUser.resolveDisplayName("").trim()
        val isOwnProfile = (cleanUid.isNotEmpty() && cleanUid == currentUid) ||
            (cleanUid.isEmpty() && currentDisplayName.isNotEmpty() && displayName.trim().equals(currentDisplayName, ignoreCase = true))
        if (isOwnProfile) {
            navigateToTab(HomeNavTab.PROFIL)
        } else {
            val encodedUid = Uri.encode(cleanUid)
            val encodedName = Uri.encode(displayName.trim().ifEmpty { "Pengguna" })
            navController.navigate("user_profile?userId=$encodedUid&name=$encodedName") {
                launchSingleTop = false
            }
        }
    }

    val navigateToDirectChat: (String, String) -> Unit = { targetUid, targetName ->
        val encodedUid = Uri.encode(targetUid.trim())
        val encodedName = Uri.encode(targetName.trim().ifEmpty { "Teman" })
        navController.navigate("chat_direct?userId=$encodedUid&name=$encodedName") {
            launchSingleTop = true
        }
    }

    val navigateToVenueFinder: (String) -> Unit = { venueName ->
        val encodedVenueName = Uri.encode(venueName.trim())
        navController.navigate("venue_finder?venueName=$encodedVenueName") {
            launchSingleTop = true
        }
    }

    val navigateToVenueInviteChat: (VenueChatTarget) -> Unit = { target ->
        VenueChatBridge.setPendingTarget(target)
        navController.navigate("chat?returnDirectlyOnBack=true") {
            launchSingleTop = true
        }
    }

    val navigateToEditPost: (EditPostTarget) -> Unit = { target ->
        EditPostBridge.setPendingTarget(target)
        navController.navigate("edit_post") {
            launchSingleTop = true
        }
    }

    val mainTabRoutes = remember { setOf("home", "lokasi", "latihan", "profile") }

    KuyRefreshHost(
        registry = refreshRegistry,
        pageKey = currentEntry?.id,
        mapPage = currentEntry?.destination?.route?.let {
            it == "lokasi" || it.startsWith("venue_finder")
        } == true,
        modifier = modifier,
    ) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = Modifier.fillMaxSize().background(KuyNavy),
        // Keep both screens opaque so the window never shows through a crossfade.
        enterTransition = {
            if (initialState.destination.route in mainTabRoutes && targetState.destination.route in mainTabRoutes) {
                fadeIn(animationSpec = tween(HOME_DISSOLVE_MS, easing = FastOutSlowInEasing))
            } else {
                slideInHorizontally(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    initialOffsetX = { it },
                )
            }
        },
        exitTransition = {
            if (targetState.destination.route in mainTabRoutes) {
                fadeOut(animationSpec = tween(HOME_DISSOLVE_MS, easing = FastOutSlowInEasing))
            } else {
                slideOutHorizontally(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    targetOffsetX = { -it },
                )
            }
        },
        popEnterTransition = {
            if (initialState.destination.route in mainTabRoutes && targetState.destination.route in mainTabRoutes) {
                fadeIn(animationSpec = tween(HOME_DISSOLVE_MS, easing = FastOutSlowInEasing))
            } else {
                slideInHorizontally(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    initialOffsetX = { -it },
                )
            }
        },
        popExitTransition = {
            if (initialState.destination.route in mainTabRoutes && targetState.destination.route in mainTabRoutes) {
                fadeOut(animationSpec = tween(HOME_DISSOLVE_MS, easing = FastOutSlowInEasing))
            } else {
                slideOutHorizontally(
                    animationSpec = tween(280, easing = FastOutSlowInEasing),
                    targetOffsetX = { it },
                )
            }
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
                onGoogleSignedIn = { role ->
                    currentUserRole = role
                    navController.navigate("home") {
                        popUpTo("opening") { inclusive = true }
                    }
                },
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
            val activeUid = currentUserUid ?: auth.currentUser?.uid
            val streakSummary = TrainingStreakStore.summaryFor(activeUid)
            val unreadNotifCount = RealtimeBadgeStore.unreadNotificationTotalFor(activeUid)
            val unreadChatCount = RealtimeBadgeStore.unreadChatTotalFor(activeUid)
            HomeScreen(
                streakDaysCount = streakSummary.currentStreakDays,
                unreadNotificationCount = unreadNotifCount,
                unreadChatCount = unreadChatCount,
                onStreakClick = { navigateToTab(HomeNavTab.LATIHAN) },
                onCreatePost = {
                    navController.navigate("create_post") { launchSingleTop = true }
                },
                onEditPost = navigateToEditPost,
                onSearchClick = {
                    navController.navigate("search_posts") { launchSingleTop = true }
                },
                onNotificationClick = {
                    navController.navigate("notifications") { launchSingleTop = true }
                },
                onChatClick = {
                    VenueChatBridge.setPendingTarget(null)
                    navController.navigate("chat?returnDirectlyOnBack=false") { launchSingleTop = true }
                },
                onTabSelected = navigateToTab,
                onUserProfileClick = navigateToUserProfile,
            )
        }
        composable("lokasi") {
            VenueFinderScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navigateToTab(HomeNavTab.KOMUNITAS)
                    }
                },
                onTabSelected = navigateToTab,
                onNavigateToChat = navigateToVenueInviteChat,
            )
        }
        composable(
            route = "venue_finder?venueName={venueName}",
            arguments = listOf(
                navArgument("venueName") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val venueName = entry.arguments?.getString("venueName").orEmpty().ifBlank { null }
            VenueFinderScreen(
                onBack = { navController.popBackStack() },
                initialFocusedVenueName = venueName,
                onTabSelected = navigateToTab,
                onNavigateToChat = navigateToVenueInviteChat,
            )
        }
        composable("latihan") {
            val activeUid = currentUserUid ?: auth.currentUser?.uid
            val streakSummary = TrainingStreakStore.summaryFor(activeUid)
            TrainingHomeScreen(
                streakDaysCount = streakSummary.currentStreakDays,
                streakDays = streakSummary.weekDays,
                streakSubtitle = buildStreakSubtitle(streakSummary),
                onTabSelected = navigateToTab,
                onHomeWorkoutClick = {
                    navController.navigate("workout_rumahan") {
                        launchSingleTop = true
                    }
                },
                onStepTrackerClick = {
                    navController.navigate("step_tracker") {
                        launchSingleTop = true
                    }
                },
                onWaterReminderClick = {
                    navController.navigate("drink_reminder") {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable("workout_rumahan") {
            HomeWorkoutScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navigateToTab(HomeNavTab.LATIHAN)
                    }
                },
                onTabSelected = navigateToTab,
                onGuidedWorkoutClick = {
                    navController.navigate("guided_workout_days") {
                        launchSingleTop = true
                    }
                },
                onCustomWorkoutClick = {
                    navController.navigate("custom_workout_list") {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable("custom_workout_list") {
            CustomWorkoutListScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navigateToTab(HomeNavTab.LATIHAN)
                    }
                },
                onTabSelected = navigateToTab,
                onWorkoutClick = { workoutId ->
                    navController.navigate("custom_workout_detail/${Uri.encode(workoutId)}") {
                        launchSingleTop = true
                    }
                },
                onAddWorkoutClick = {
                    navController.navigate("custom_workout_create") {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable("custom_workout_create") {
            CreateCustomWorkoutScreen(
                onBack = { navController.popBackStack() },
                onSaveWorkout = { name, selectedExercises, onResult ->
                    val uid = currentUserUid ?: auth.currentUser?.uid.orEmpty()
                    saveCustomWorkoutToFirestore(
                        userId = uid,
                        name = name,
                        selectedExercises = selectedExercises,
                        onResult = onResult,
                    )
                },
                onCreatedContinue = {
                    if (!navController.popBackStack("custom_workout_list", inclusive = false)) {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable(
            route = "custom_workout_detail/{workoutId}",
            arguments = listOf(
                navArgument("workoutId") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val workoutId = entry.arguments?.getString("workoutId").orEmpty()
            CustomWorkoutDetailScreen(
                workoutId = workoutId,
                onBack = { navController.popBackStack() },
                onExerciseClick = { exerciseKey ->
                    navController.navigate("exercise_detail/${Uri.encode(exerciseKey)}") {
                        launchSingleTop = true
                    }
                },
                onStartWorkoutClick = {
                    navController.navigate("custom_workout_session/${Uri.encode(workoutId)}") {
                        launchSingleTop = true
                    }
                },
                onDeleteWorkoutClick = { targetId ->
                    val uid = currentUserUid ?: auth.currentUser?.uid.orEmpty()
                    deleteCustomWorkoutFromFirestore(
                        userId = uid,
                        workoutId = targetId,
                        onResult = { success, _ ->
                            if (success) {
                                navController.popBackStack()
                            }
                        },
                    )
                },
            )
        }
        composable(
            route = "custom_workout_session/{workoutId}",
            arguments = listOf(
                navArgument("workoutId") { type = NavType.StringType; defaultValue = "" },
            ),
        ) { entry ->
            val workoutId = entry.arguments?.getString("workoutId").orEmpty()
            val customPlan = CustomWorkoutStore.workoutById(workoutId)
            WorkoutSessionScreen(
                exercises = customPlan?.exercises.orEmpty(),
                onQuitConfirmed = { navController.popBackStack() },
                onWorkoutCompleted = {
                    val uid = currentUserUid ?: auth.currentUser?.uid.orEmpty()
                    recordWorkoutCompletedForStreak(userId = uid)
                },
                onCompletedContinue = {
                    if (!navController.popBackStack("custom_workout_list", inclusive = false)) {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable("guided_workout_days") {
            val activeUid = currentUserUid ?: auth.currentUser?.uid
            val syncedForCurrentUser = activeUid != null && GuidedWorkoutProgressStore.activeUserId == activeUid
            GuidedWorkoutDaysScreen(
                currentUserId = activeUid,
                isLoadingProgress = !syncedForCurrentUser || GuidedWorkoutProgressStore.isLoading,
                highestUnlockedDay = if (syncedForCurrentUser) GuidedWorkoutProgressStore.highestUnlockedDay else 1,
                completedDays = if (syncedForCurrentUser) GuidedWorkoutProgressStore.completedDays else emptySet(),
                onBack = {
                    if (!navController.popBackStack()) {
                        navigateToTab(HomeNavTab.LATIHAN)
                    }
                },
                onTabSelected = navigateToTab,
                onDayClick = { dayNumber ->
                    navController.navigate("guided_workout_detail/$dayNumber") {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            route = "guided_workout_detail/{dayNumber}",
            arguments = listOf(
                navArgument("dayNumber") { type = NavType.IntType; defaultValue = 1 },
            ),
        ) { entry ->
            val dayNumber = entry.arguments?.getInt("dayNumber") ?: 1
            GuidedWorkoutDetailScreen(
                dayNumber = dayNumber,
                onBack = { navController.popBackStack() },
                onExerciseClick = { exerciseKey ->
                    navController.navigate("exercise_detail/${Uri.encode(exerciseKey)}") {
                        launchSingleTop = true
                    }
                },
                onStartWorkoutClick = {
                    navController.navigate("guided_workout_session/$dayNumber") {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(
            route = "guided_workout_session/{dayNumber}",
            arguments = listOf(
                navArgument("dayNumber") { type = NavType.IntType; defaultValue = 1 },
            ),
        ) { entry ->
            val dayNumber = entry.arguments?.getInt("dayNumber") ?: 1
            val dayPlan = WorkoutFirestoreStore.dayPlanOrNull(dayNumber)
            WorkoutSessionScreen(
                exercises = dayPlan?.exercises.orEmpty(),
                onQuitConfirmed = { navController.popBackStack() },
                onWorkoutCompleted = {
                    val uid = currentUserUid ?: auth.currentUser?.uid.orEmpty()
                    completeGuidedWorkoutDayForUser(
                        userId = uid,
                        dayNumber = dayNumber,
                    )
                    recordWorkoutCompletedForStreak(userId = uid)
                },
                onCompletedContinue = {
                    if (!navController.popBackStack("guided_workout_days", inclusive = false)) {
                        navController.popBackStack()
                    }
                },
            )
        }
        composable(
            route = "exercise_detail/{exerciseKey}",
            arguments = listOf(
                navArgument("exerciseKey") { type = NavType.StringType; defaultValue = "SIT_UP" },
            ),
        ) { entry ->
            val exerciseKey = entry.arguments?.getString("exerciseKey").orEmpty().ifBlank { "SIT_UP" }
            ExerciseDetailScreen(
                exerciseKey = exerciseKey,
                onBack = { navController.popBackStack() },
            )
        }
        composable("drink_reminder") {
            DrinkReminderScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navigateToTab(HomeNavTab.LATIHAN)
                    }
                },
            )
        }
        composable("step_tracker") {
            StepTrackerScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navigateToTab(HomeNavTab.LATIHAN)
                    }
                },
            )
        }
        composable("profile") {
            LoggedInProfileScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navigateToTab(HomeNavTab.KOMUNITAS)
                    }
                },
                onHomeClick = { navigateToTab(HomeNavTab.KOMUNITAS) },
                onTabSelected = navigateToTab,
                onEditPostRequest = navigateToEditPost,
                onLogout = {
                    GuidedWorkoutProgressStore.reset()
                    CustomWorkoutStore.clear()
                    TrainingStreakStore.reset()
                    RealtimeBadgeStore.reset()
                    FirebaseAuth.getInstance().signOut()
                    coroutineScope.launch { clearGoogleCredentialState(context) }
                    navController.navigate("opening") {
                        popUpTo(0) { inclusive = true }
                        launchSingleTop = true
                    }
                },
            )
        }
        composable("create_post") {
            CreatePostScreen(
                onBack = { navController.popBackStack() },
                onPosted = { navController.popBackStack() },
            )
        }
        composable("edit_post") { entry ->
            val target = remember(entry.id) { EditPostBridge.peekTarget() }
            val closeEditPost: () -> Unit = {
                if (navController.currentBackStackEntry?.id == entry.id) {
                    navController.popBackStack()
                }
            }
            DisposableEffect(entry.id) {
                onDispose {
                    if (EditPostBridge.peekTarget() == target) {
                        EditPostBridge.clear()
                    }
                }
            }
            EditPostScreen(
                target = target,
                onBack = closeEditPost,
                onSaved = closeEditPost,
            )
        }
        composable("search_posts") {
            SearchPostScreen(
                onBack = { navController.popBackStack() },
                onUserProfileClick = navigateToUserProfile,
            )
        }
        composable(
            route = "notifications?postId={postId}",
            arguments = listOf(
                navArgument("postId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val postId = entry.arguments?.getString("postId").orEmpty().ifBlank { null }
            NotificationScreen(
                onBack = { navController.popBackStack() },
                initialSelectedPostId = postId,
                onUserProfileClick = navigateToUserProfile,
            )
        }
        composable(
            route = "chat?returnDirectlyOnBack={returnDirectlyOnBack}&roomId={roomId}",
            arguments = listOf(
                navArgument("returnDirectlyOnBack") {
                    type = NavType.BoolType
                    defaultValue = false
                },
                navArgument("roomId") {
                    type = NavType.StringType
                    defaultValue = ""
                },
            ),
        ) { entry ->
            val returnDirectlyOnBack = entry.arguments?.getBoolean("returnDirectlyOnBack") ?: false
            val roomId = entry.arguments?.getString("roomId").orEmpty().ifBlank { null }
            ChatScreen(
                onBack = { navController.popBackStack() },
                initialSelectedRoomId = roomId,
                returnDirectlyOnBack = returnDirectlyOnBack,
                onOpenVenueFinder = navigateToVenueFinder,
            )
        }
        composable(
            route = "user_profile?userId={userId}&name={name}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType; defaultValue = "" },
                navArgument("name") { type = NavType.StringType; defaultValue = "Pengguna" },
            ),
        ) { entry ->
            val targetUserId = entry.arguments?.getString("userId").orEmpty()
            val targetName = entry.arguments?.getString("name").orEmpty().ifBlank { "Pengguna" }
            OtherUserProfileScreen(
                targetUserId = targetUserId,
                initialDisplayName = targetName,
                onBack = { navController.popBackStack() },
                onMessageClick = navigateToDirectChat,
                onTabSelected = navigateToTab,
                onHomeClick = { navigateToTab(HomeNavTab.KOMUNITAS) },
            )
        }
        composable(
            route = "chat_direct?userId={userId}&name={name}",
            arguments = listOf(
                navArgument("userId") { type = NavType.StringType; defaultValue = "" },
                navArgument("name") { type = NavType.StringType; defaultValue = "Teman" },
            ),
        ) { entry ->
            val targetUserId = entry.arguments?.getString("userId").orEmpty()
            val targetName = entry.arguments?.getString("name").orEmpty().ifBlank { "Teman" }
            val currentUser = remember { currentUserOrNull() }
            val currentUserId = currentUser?.uid ?: "me"
            val currentUserName = remember(currentUser) { currentUser.resolveDisplayName("Anda") }
            val directRoom = remember(currentUserId, currentUserName, targetUserId, targetName) {
                ensureDirectChatRoom(
                    currentUserId = currentUserId,
                    currentUserName = currentUserName,
                    targetUid = targetUserId,
                    targetName = targetName,
                )
            }
            ChatScreen(
                onBack = { navController.popBackStack() },
                initialSelectedRoomId = directRoom.id,
                initialDirectRoom = directRoom,
                exitOnDetailBack = true,
                onOpenVenueFinder = navigateToVenueFinder,
            )
        }
    }
    }
}



