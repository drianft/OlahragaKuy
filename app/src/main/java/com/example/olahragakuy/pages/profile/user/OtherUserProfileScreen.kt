package com.example.olahragakuy.pages.profile.user

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.olahragakuy.pages.profile.admin.ReportPostDialog
import com.example.olahragakuy.pages.profile.admin.submitUserPostReport
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.components.KuyBottomNavBar
import com.example.olahragakuy.ui.theme.KuyNavy
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

@Composable
fun OtherUserProfileScreen(
    targetUserId: String,
    initialDisplayName: String,
    onBack: () -> Unit,
    onMessageClick: (targetUid: String, targetName: String) -> Unit,
    onTabSelected: (HomeNavTab) -> Unit = {},
    onHomeClick: () -> Unit = {},
) {
    val auth = remember { FirebaseAuth.getInstance() }
    val currentUser = remember { auth.currentUser }
    val cleanTargetUid = targetUserId.trim()
    val fallbackName = initialDisplayName.trim().ifEmpty { "Pengguna" }

    var profile by remember(cleanTargetUid, fallbackName) {
        mutableStateOf(
            UserProfileSummary(
                username = fallbackName,
                displayName = fallbackName,
                bio = "",
            ),
        )
    }
    var userPosts by remember(cleanTargetUid, fallbackName) { mutableStateOf<List<UserPost>>(emptyList()) }
    var taggedPosts by remember(cleanTargetUid, fallbackName) { mutableStateOf<List<UserPost>>(emptyList()) }
    var followersSubCount by remember(cleanTargetUid) { mutableIntStateOf(0) }
    var isFollowing by remember(cleanTargetUid) { mutableStateOf(false) }
    var initialFollowingLoaded by remember(cleanTargetUid) { mutableStateOf<Boolean?>(null) }
    var preFollowFollowersCount by remember(cleanTargetUid) { mutableStateOf<Int?>(null) }
    var followBusy by remember(cleanTargetUid) { mutableStateOf(false) }
    var loading by remember(cleanTargetUid, fallbackName) { mutableStateOf(true) }
    var loadError by remember(cleanTargetUid) { mutableStateOf(false) }
    var actionError by remember(cleanTargetUid) { mutableStateOf<String?>(null) }
    var postPendingReport by remember(cleanTargetUid) { mutableStateOf<UserPost?>(null) }
    var reportSubmitting by remember(cleanTargetUid) { mutableStateOf(false) }
    var retry by remember(cleanTargetUid) { mutableIntStateOf(0) }

    DisposableEffect(cleanTargetUid, fallbackName, currentUser?.uid, retry) {
        val db = FirebaseFirestore.getInstance()
        val registrations = mutableListOf<ListenerRegistration>()
        loading = true

        if (cleanTargetUid.isNotEmpty()) {
            registrations += db.collection("users")
                .document(cleanTargetUid)
                .addSnapshotListener { document, error ->
                    if (error != null) {
                        loadError = shouldShowProfileLoadError(error.code.name)
                    } else {
                        profile = parseUserProfileSnapshot(
                            data = document?.data.orEmpty(),
                            fallbackName = fallbackName,
                        )
                        loadError = false
                    }
                    loading = false
                }

            registrations += db.collection("users")
                .document(cleanTargetUid)
                .collection("followers")
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        followersSubCount = snapshot.size()
                    }
                }

            registrations += db.collection("posts")
                .whereEqualTo("authorId", cleanTargetUid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        val byId = snapshot.documents.toSortedUserPosts()
                        if (byId.isNotEmpty()) {
                            userPosts = byId
                        } else {
                            db.collection("posts")
                                .whereEqualTo("authorName", fallbackName)
                                .get()
                                .addOnSuccessListener { fallbackSnap ->
                                    if (userPosts.isEmpty() && fallbackSnap != null) {
                                        userPosts = fallbackSnap.documents.toSortedUserPosts()
                                    }
                                }
                        }
                    }
                }

            registrations += db.collection("posts")
                .whereArrayContains("taggedFriendIds", cleanTargetUid)
                .addSnapshotListener { snapshot, _ ->
                    if (snapshot != null) {
                        taggedPosts = snapshot.documents.toSortedUserPosts()
                    }
                }

            if (currentUser != null && currentUser.uid != cleanTargetUid) {
                registrations += db.collection("users")
                    .document(currentUser.uid)
                    .collection("following")
                    .document(cleanTargetUid)
                    .addSnapshotListener { doc, _ ->
                        val exists = doc?.exists() == true
                        if (!followBusy) {
                            initialFollowingLoaded = exists
                            isFollowing = exists
                        }
                    }
            }
        } else {
            registrations += db.collection("users")
                .whereEqualTo("username", fallbackName)
                .limit(1)
                .addSnapshotListener { userSnap, _ ->
                    val doc = userSnap?.documents?.firstOrNull()
                    if (doc != null) {
                        profile = parseUserProfileSnapshot(
                            data = doc.data.orEmpty(),
                            fallbackName = fallbackName,
                        )
                    }
                }

            registrations += db.collection("posts")
                .whereEqualTo("authorName", fallbackName)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        loadError = shouldShowProfileLoadError(error.code.name)
                    } else if (snapshot != null) {
                        userPosts = snapshot.documents.toSortedUserPosts()
                        loadError = false
                    }
                    loading = false
                }
        }

        onDispose {
            registrations.forEach { it.remove() }
        }
    }

    val liveBaseFollowers = maxOf(profile.followersCount, followersSubCount)
    val displayedFollowers = if (followBusy && preFollowFollowersCount == liveBaseFollowers) {
        calculateFollowerDisplayCount(
            baseFollowers = liveBaseFollowers,
            wasInitiallyFollowing = initialFollowingLoaded ?: !isFollowing,
            isCurrentlyFollowing = isFollowing,
        )
    } else {
        liveBaseFollowers
    }

    if (loading) {
        Scaffold(containerColor = KuyNavy) { padding ->
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(color = OrangeBright)
            }
        }
    } else {
        OtherUserProfileContent(
            username = profile.username,
            displayName = profile.displayName,
            bio = profile.bio,
            photoUrl = profile.photoUrl,
            postsCount = maxOf(userPosts.size, profile.postsCount),
            followers = displayedFollowers,
            following = profile.followingCount,
            isFollowing = isFollowing,
            followEnabled = !followBusy && currentUser != null && cleanTargetUid.isNotEmpty() && currentUser.uid != cleanTargetUid,
            feedPosts = userPosts,
            gridPosts = userPosts,
            taggedPosts = taggedPosts,
            onBack = onBack,
            onToggleFollow = {
                val user = currentUser
                if (user == null) {
                    actionError = "Silakan masuk untuk mengikuti akun ini."
                } else if (!followBusy) {
                    followBusy = true
                    preFollowFollowersCount = liveBaseFollowers
                    val targetState = !isFollowing
                    isFollowing = targetState
                    toggleFollowUser(
                        currentUser = user,
                        targetUid = cleanTargetUid,
                        targetUsername = profile.username,
                        targetDisplayName = profile.displayName,
                        targetPhotoUrl = profile.photoUrl,
                        currentlyFollowing = !targetState,
                        onSuccess = { nowFollowing ->
                            initialFollowingLoaded = nowFollowing
                            isFollowing = nowFollowing
                            preFollowFollowersCount = null
                            followBusy = false
                        },
                        onError = { err ->
                            isFollowing = !targetState
                            preFollowFollowersCount = null
                            followBusy = false
                            actionError = err
                        },
                    )
                }
            },
            onMessageClick = {
                val resolvedUid = cleanTargetUid.ifEmpty {
                    "user_${profile.username.lowercase().replace(Regex("[^a-z0-9]+"), "_")}"
                }
                val resolvedName = profile.displayName.ifBlank { profile.username }
                onMessageClick(resolvedUid, resolvedName)
            },
            onReportPost = { post -> postPendingReport = post },
            onTabSelected = onTabSelected,
            onHomeClick = onHomeClick,
        )
    }

    postPendingReport?.let { targetPost ->
        val reportedName = profile.username.ifBlank { profile.displayName }
        ReportPostDialog(
            reportedUsername = reportedName,
            submitting = reportSubmitting,
            onDismiss = { postPendingReport = null },
            onSubmit = { category, description ->
                reportSubmitting = true
                submitUserPostReport(
                    post = targetPost,
                    reportedUserId = cleanTargetUid,
                    reportedUsername = reportedName,
                    reportCategory = category,
                    reportDescription = description,
                    onSuccess = {
                        reportSubmitting = false
                        postPendingReport = null
                        actionError = "Laporan untuk postingan @$reportedName berhasil dikirim ke Admin."
                    },
                    onError = { err ->
                        reportSubmitting = false
                        postPendingReport = null
                        actionError = err
                    },
                )
            },
        )
    }

    if (loadError) {
        AlertDialog(
            onDismissRequest = { loadError = false },
            title = { Text("Profil belum dapat dimuat") },
            text = { Text("Data profil pengguna belum tersedia. Periksa koneksi internet lalu coba lagi.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        loadError = false
                        retry++
                    },
                ) { Text("Coba lagi") }
            },
            dismissButton = {
                TextButton(onClick = { loadError = false }) { Text("Tutup") }
            },
        )
    }

    actionError?.let { msg ->
        AlertDialog(
            onDismissRequest = { actionError = null },
            title = { Text("Informasi") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { actionError = null }) { Text("Mengerti") }
            },
        )
    }
}

@Composable
internal fun OtherUserProfileContent(
    username: String,
    displayName: String,
    bio: String,
    photoUrl: String? = null,
    postsCount: Int = 0,
    followers: Int = 0,
    following: Int = 0,
    isFollowing: Boolean = false,
    followEnabled: Boolean = true,
    initialTab: ProfileContentTab = ProfileContentTab.FEED,
    feedPosts: List<UserPost> = emptyList(),
    gridPosts: List<UserPost> = emptyList(),
    taggedPosts: List<UserPost> = emptyList(),
    onBack: () -> Unit = {},
    onToggleFollow: () -> Unit = {},
    onMessageClick: () -> Unit = {},
    onPostClick: (UserPost) -> Unit = {},
    onReportPost: ((UserPost) -> Unit)? = null,
    onTabSelected: (HomeNavTab) -> Unit = {},
    onHomeClick: () -> Unit = {},
) {
    var selectedTab by rememberSaveable { mutableStateOf(initialTab) }

    Scaffold(
        containerColor = NavyDark,
        bottomBar = {
            KuyBottomNavBar(
                selectedTab = HomeNavTab.KOMUNITAS,
                onTabSelected = { tab ->
                    onTabSelected(tab)
                    if (tab == HomeNavTab.KOMUNITAS) onHomeClick()
                },
            )
        },
    ) { padding ->
        val header: @Composable () -> Unit = {
            ProfileHeaderSection(
                username = username,
                displayName = displayName,
                postsCount = maxOf(postsCount, maxOf(feedPosts.size, gridPosts.size)),
                followers = followers,
                following = following,
                bio = bio,
                photoUrl = photoUrl,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                onBack = onBack,
                onMenu = {},
                onEditProfile = onToggleFollow,
                onShareProfile = onMessageClick,
                showMenu = false,
                primaryActionLabel = if (isFollowing) "Mengikuti" else "Ikuti",
                secondaryActionLabel = "Pesan",
                primaryActionSelected = isFollowing,
                primaryActionEnabled = followEnabled,
            )
        }
        val contentModifier = Modifier.fillMaxSize().padding(padding)
        when (selectedTab) {
            ProfileContentTab.FEED -> ProfileFeedContent(
                username = displayName,
                posts = feedPosts,
                onPostClick = onPostClick,
                onReportPost = onReportPost,
                modifier = contentModifier,
                header = header,
            )
            ProfileContentTab.GRID -> ProfileGridContent(
                posts = gridPosts,
                tagged = false,
                onPostClick = { post ->
                    onPostClick(post)
                    selectedTab = ProfileContentTab.FEED
                },
                modifier = contentModifier,
                header = header,
            )
            ProfileContentTab.TAGGED -> ProfileGridContent(
                posts = taggedPosts,
                tagged = true,
                onPostClick = onPostClick,
                modifier = contentModifier,
                header = header,
            )
        }
    }
}

@Preview(name = "Profil Orang - Feed", showBackground = true, showSystemUi = true)
@Composable
private fun OtherUserProfileFeedPreview() {
    OtherUserProfileContent(
        username = "PecintaJogging",
        displayName = "Pecinta Jogging",
        bio = "Be yourself and Never Surrender",
        postsCount = 3,
        followers = 24,
        following = 42,
        feedPosts = listOf(UserPost(1, "Ayoo jogging guysss", "2 hari lalu")),
        gridPosts = listOf(UserPost(1, "Ayoo jogging guysss", "2 hari lalu")),
        initialTab = ProfileContentTab.FEED,
    )
}

@Preview(name = "Profil Orang - Grid", showBackground = true, showSystemUi = true)
@Composable
private fun OtherUserProfileGridPreview() {
    OtherUserProfileContent(
        username = "PecintaJogging",
        displayName = "Pecinta Jogging",
        bio = "Be yourself and Never Surrender",
        postsCount = 10,
        followers = 24,
        following = 42,
        feedPosts = List(3) { UserPost(it + 1, "Jogging ${it + 1}", "2 hari lalu") },
        gridPosts = List(3) { UserPost(it + 1, "Jogging ${it + 1}", "2 hari lalu") },
        initialTab = ProfileContentTab.GRID,
    )
}

@Preview(name = "Profil Orang - Ditandai Kosong", showBackground = true, showSystemUi = true)
@Composable
private fun OtherUserProfileTaggedEmptyPreview() {
    OtherUserProfileContent(
        username = "PecintaJogging",
        displayName = "Pecinta Jogging",
        bio = "Be yourself and Never Surrender",
        postsCount = 10,
        followers = 24,
        following = 42,
        initialTab = ProfileContentTab.TAGGED,
    )
}
