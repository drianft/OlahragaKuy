package com.example.olahragakuy.pages.profile.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.tooling.preview.Preview
import com.example.olahragakuy.pages.profile.user.ProfileScreen
import com.example.olahragakuy.pages.profile.user.UserPost
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme

/**
 * Halaman Profil khusus pengguna dengan role Admin.
 * Menggunakan struktur yang sama dengan [ProfileScreen], dengan tambahan tombol
 * Dashboard Laporan di kanan atas yang mengarah ke [AdminDashboardReportScreen].
 *
 * Jika [onOpenDashboardReport] tidak diberikan (seperti pada saat Preview atau testing),
 * halaman ini secara otomatis mengelola perpindahan ke [AdminDashboardReportScreen] dan
 * kembali lagi saat tombol "Kembali" ditekan.
 */
@Composable
fun AdminProfileScreen(
    initialUsername: String = "BibiOlahraga",
    initialDisplayName: String = "Bibi Olahraga",
    followers: Int = 4,
    following: Int = 5,
    initialBio: String = "You Can Do it",
    initialTab: Int = 0,
    feedPosts: List<UserPost> = listOf(
        UserPost(1, "Always on Proces", "2 jam lalu"),
    ),
    gridPosts: List<UserPost> = listOf(
        UserPost(1, "Always on Proces", "2 jam lalu"),
    ),
    taggedPosts: List<UserPost> = emptyList(),
    onBack: () -> Unit = {},
    onLogout: () -> Unit = {},
    onPostClick: (UserPost) -> Unit = {},
    onHomeClick: () -> Unit = {},
    onTabSelected: (HomeNavTab) -> Unit = {},
    onProfileSaved: (username: String, bio: String) -> Unit = { _, _ -> },
    onDeletePost: ((UserPost) -> Unit)? = null,
    onEditPost: ((UserPost, String) -> Unit)? = null,
    onOpenDashboardReport: (() -> Unit)? = null,
) {
    var showDashboardReport by rememberSaveable { mutableStateOf(false) }

    if (showDashboardReport) {
        AdminDashboardReportScreen(
            onBack = { showDashboardReport = false },
        )
    } else {
        ProfileScreen(
            initialUsername = initialUsername,
            initialDisplayName = initialDisplayName,
            followers = followers,
            following = following,
            initialBio = initialBio,
            initialTab = initialTab,
            feedPosts = feedPosts,
            gridPosts = gridPosts,
            taggedPosts = taggedPosts,
            onBack = onBack,
            onLogout = onLogout,
            onPostClick = onPostClick,
            onHomeClick = onHomeClick,
            onTabSelected = onTabSelected,
            onProfileSaved = onProfileSaved,
            onDeletePost = onDeletePost,
            onEditPost = onEditPost,
            showAdminDashboard = true,
            onAdminDashboardClick = {
                if (onOpenDashboardReport != null) {
                    onOpenDashboardReport()
                } else {
                    showDashboardReport = true
                }
            },
        )
    }
}

@Preview(name = "Admin Profile - Feed", showBackground = true, showSystemUi = true)
@Composable
private fun AdminProfileFeedPreview() {
    OlahragaKuyTheme {
        AdminProfileScreen(initialTab = 0)
    }
}

@Preview(name = "Admin Profile - Grid", showBackground = true, showSystemUi = true)
@Composable
private fun AdminProfileGridPreview() {
    OlahragaKuyTheme {
        AdminProfileScreen(initialTab = 1)
    }
}
