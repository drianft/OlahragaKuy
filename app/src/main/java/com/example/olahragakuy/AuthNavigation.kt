package com.example.olahragakuy

import android.net.Uri
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import com.example.olahragakuy.pages.posts.ChatScreen
import com.example.olahragakuy.pages.posts.CreatePostScreen
import com.example.olahragakuy.pages.posts.NotificationScreen
import com.example.olahragakuy.pages.posts.SearchPostScreen
import com.example.olahragakuy.pages.posts.currentUserOrNull
import com.example.olahragakuy.pages.posts.ensureDirectChatRoom
import com.example.olahragakuy.pages.posts.resolveDisplayName
import com.example.olahragakuy.pages.drinkreminder.DrinkReminderScreen
import com.example.olahragakuy.pages.profile.user.LoggedInProfileScreen
import com.example.olahragakuy.pages.profile.user.OtherUserProfileScreen
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
import com.example.olahragakuy.pages.venuefinder.VenueChatBridge
import com.example.olahragakuy.pages.venuefinder.VenueChatTarget
import com.example.olahragakuy.pages.venuefinder.VenueFinderScreen
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.theme.KuyNavy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

// Figma: Login -> Home uses Dissolve, Ease Out, 300ms.
private const val HOME_DISSOLVE_MS = 300

@Composable
fun AuthNavigation(
    modifier: Modifier = Modifier,
    onExit: () -> Unit = {},
    openDrinkReminder: Boolean = false,
    onConsumeOpenDrinkReminder: () -> Unit = {},
) {
    val navController = rememberNavController()
    val refreshRegistry = remember { RefreshRegistry() }
    val currentEntry by navController.currentBackStackEntryAsState()
    var currentUserRole by rememberSaveable { mutableStateOf("member") }
    var prefilledEmail by rememberSaveable { mutableStateOf("") }
    val auth = remember { FirebaseAuth.getInstance() }
    var currentUserUid by remember { mutableStateOf(auth.currentUser?.uid) }
    val startDestination = remember {
        if (auth.currentUser != null) "home" else "opening"
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
        onDispose {
            catalogRegistration?.remove()
            guidedProgressRegistration?.remove()
            customWorkoutRegistration?.remove()
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
            TrainingHomeScreen(
                onTabSelected = navigateToTab,
                onHomeWorkoutClick = {
                    navController.navigate("workout_rumahan") {
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
                onWorkoutCompleted = {},
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
        composable("profile") {
            LoggedInProfileScreen(
                onBack = {
                    if (!navController.popBackStack()) {
                        navigateToTab(HomeNavTab.KOMUNITAS)
                    }
                },
                onHomeClick = { navigateToTab(HomeNavTab.KOMUNITAS) },
                onTabSelected = navigateToTab,
                onLogout = {
                    GuidedWorkoutProgressStore.reset()
                    CustomWorkoutStore.clear()
                    FirebaseAuth.getInstance().signOut()
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
        composable("search_posts") {
            SearchPostScreen(
                onBack = { navController.popBackStack() },
                onUserProfileClick = navigateToUserProfile,
            )
        }
        composable("notifications") {
            NotificationScreen(
                onBack = { navController.popBackStack() },
                onUserProfileClick = navigateToUserProfile,
            )
        }
        composable(
            route = "chat?returnDirectlyOnBack={returnDirectlyOnBack}",
            arguments = listOf(
                navArgument("returnDirectlyOnBack") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            val returnDirectlyOnBack = entry.arguments?.getBoolean("returnDirectlyOnBack") ?: false
            ChatScreen(
                onBack = { navController.popBackStack() },
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



