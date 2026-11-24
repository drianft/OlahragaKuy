package com.example.olahragakuy.pages.profile.user

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.components.KuyBottomNavBar
import androidx.compose.runtime.saveable.rememberSaveable

@Composable
fun ProfileScreen(
    initialUsername: String = "BibiOlahraga",
    initialDisplayName: String = "Bibi Olahraga",
    followers: Int = 4,
    following: Int = 5,
    initialBio: String = "You Can Do it",
    initialTab: Int = 0, // 0 = Feed, 1 = Grid, 2 = Ditandai
    feedPosts: List<UserPost> = listOf(
        UserPost(1, "Always on Proces", "2 jam lalu")
    ),
    gridPosts: List<UserPost> = listOf(
        UserPost(1, "Always on Proces", "2 jam lalu")
    ),
    taggedPosts: List<UserPost> = emptyList(),
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onPostClick: (UserPost) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onTabSelected: (HomeNavTab) -> Unit = {},
    onProfileSaved: (username: String, bio: String) -> Unit = { _, _ -> },
    showAdminDashboard: Boolean = false,
    onAdminDashboardClick: () -> Unit = {},
) {
    var state by rememberSaveable(stateSaver = ProfileUiState.Saver) {
        mutableStateOf(
            ProfileUiState(initialUsername, initialBio, ProfileContentTab.fromIndex(initialTab))
        )
    }
    // Incoming profile changes replace only the fields whose source values changed.
    val currentState = state.syncInputs(initialUsername, initialBio)
    if (currentState != state) state = currentState

    when (currentState.subScreen) {
        ProfileSubScreen.EDIT_PROFILE -> EditProfileScreen(
            currentUsername = currentState.username,
            currentBio = currentState.bio,
            onBack = { state = state.backToProfile() },
            onSave = { username, bio ->
                val trimmedUsername = username.trim().ifEmpty { currentState.username }
                val trimmedBio = bio.trim()
                state = state.saveProfile(trimmedUsername, trimmedBio)
                onProfileSaved(trimmedUsername, trimmedBio)
            }
        )
        ProfileSubScreen.ACCOUNT_SETTINGS -> AccountSettingsScreen(
            onBack = { state = state.backToProfile() },
            onChangeEmail = { /* Aksi ganti email */ },
            onChangePassword = { /* Aksi ganti password */ },
            onLogout = {
                state = state.backToProfile()
                onLogout()
            }
        )
        ProfileSubScreen.PROFILE -> Scaffold(
            containerColor = NavyDark,
            bottomBar = {
                KuyBottomNavBar(
                    selectedTab = HomeNavTab.PROFIL,
                    onTabSelected = { tab ->
                        onTabSelected(tab)
                        if (tab == HomeNavTab.KOMUNITAS) onHomeClick()
                    }
                )
            }
        ) { padding ->
            val header: @Composable () -> Unit = {
                ProfileHeaderSection(
                    username = currentState.username,
                    displayName = initialDisplayName,
                    postsCount = maxOf(feedPosts.size, gridPosts.size),
                    followers = followers,
                    following = following,
                    bio = currentState.bio,
                    selectedTab = currentState.selectedTab,
                    onTabSelected = { state = state.selectTab(it) },
                    onBack = onBack,
                    onMenu = { state = state.openSettings() },
                    onEditProfile = { state = state.openEdit() },
                    onShareProfile = {},
                    showAdminDashboard = showAdminDashboard,
                    onAdminDashboardClick = onAdminDashboardClick,
                )
            }
            val contentModifier = Modifier.fillMaxSize().padding(padding)
            when (currentState.selectedTab) {
                ProfileContentTab.FEED -> ProfileFeedContent(
                    username = currentState.username,
                    posts = feedPosts,
                    onPostClick = onPostClick,
                    modifier = contentModifier,
                    header = header
                )
                ProfileContentTab.GRID -> ProfileGridContent(
                    posts = gridPosts,
                    tagged = false,
                    onPostClick = { post ->
                        onPostClick(post)
                        state = state.selectTab(ProfileContentTab.FEED)
                    },
                    modifier = contentModifier,
                    header = header
                )
                ProfileContentTab.TAGGED -> ProfileGridContent(
                    posts = taggedPosts,
                    tagged = true,
                    onPostClick = onPostClick,
                    modifier = contentModifier,
                    header = header
                )
            }
        }
    }
}

@Preview(name = "Feed", showBackground = true, showSystemUi = true)
@Composable
private fun ProfileFeedPreview() {
    ProfileScreen(initialTab = 0)
}

@Preview(name = "Grid", showBackground = true, showSystemUi = true)
@Composable
private fun ProfileGridPreview() {
    ProfileScreen(initialTab = 1)
}

@Preview(name = "Ditandai kosong", showBackground = true)
@Composable
private fun ProfileTaggedEmptyPreview() {
    ProfileScreen(initialTab = 2)
}

@Preview(name = "Ditandai berisi", showBackground = true)
@Composable
private fun ProfileTaggedPreview() {
    ProfileScreen(initialTab = 2, taggedPosts = listOf(UserPost(2, "Latihan bersama", "1 jam lalu")))
}
