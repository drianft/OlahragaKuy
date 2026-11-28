package com.example.olahragakuy.pages.profile.admin

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import com.example.olahragakuy.pages.profile.user.ProfileScreen
import com.example.olahragakuy.pages.profile.user.UserPost
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme

private enum class AdminProfileSubScreen {
    PROFILE,
    DASHBOARD_REPORT,
    PROFILE_EDIT,
    HISTORY_ACTION,
}

/**
 * Halaman Profil khusus pengguna dengan role Admin.
 * Menggunakan struktur yang sama dengan [ProfileScreen], dengan tambahan:
 * - Tombol Dashboard Laporan di kanan atas yang mengarah ke [AdminDashboardReportScreen].
 * - Tombol Menu (kanan atas paling ujung) yang mengarah ke [AdminProfileEditScreen] ->
 *   [AdminHistoryActionScreen] -> [AdminHistoryActionDetailScreen].
 *
 * Jika [onOpenDashboardReport] tidak diberikan (seperti pada saat Preview atau testing),
 * halaman ini secara otomatis mengelola perpindahan ke [AdminDashboardReportScreen] dan
 * menyinkronkan aksi preview ke riwayat aksi lokal.
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
    val isPreview = LocalInspectionMode.current
    var activeSubScreen by rememberSaveable { mutableStateOf(AdminProfileSubScreen.PROFILE) }
    var deletedPostKeys by rememberSaveable { mutableStateOf(listOf<String>()) }
    val previewHistoryItems = remember {
        mutableStateListOf<AdminActionHistoryItem>().apply {
            addAll(SampleAdminHistoryItems)
        }
    }

    val recordPreviewHistory: (AdminReportItem, String, String, String) -> Unit =
        { report, status, warningTitle, warningDescription ->
            if (isPreview) {
                val now = System.currentTimeMillis()
                previewHistoryItems.removeAll { it.id == report.id }
                previewHistoryItems.add(
                    0,
                    AdminActionHistoryItem(
                        id = report.id,
                        report = report,
                        actionStatus = status,
                        actionLabel = formatAdminActionLabel(status, report.type),
                        actionDateShort = formatReportDateShort(now),
                        actionDateLong = formatReportDateIndonesian(now),
                        actionTimestampMillis = now,
                        resolvedByName = initialUsername,
                        warningTitle = warningTitle,
                        warningDescription = warningDescription,
                    ),
                )
            }
        }

    val visibleFeedPosts = feedPosts.filterNot { post ->
        post.documentId in deletedPostKeys || post.caption.lowercase() in deletedPostKeys
    }
    val visibleGridPosts = gridPosts.filterNot { post ->
        post.documentId in deletedPostKeys || post.caption.lowercase() in deletedPostKeys
    }

    when (activeSubScreen) {
        AdminProfileSubScreen.DASHBOARD_REPORT -> {
            AdminDashboardReportScreen(
                onBack = { activeSubScreen = AdminProfileSubScreen.PROFILE },
                onIgnoreReport = { report ->
                    recordPreviewHistory(report, REPORT_STATUS_IGNORED, "", "")
                },
                onDeleteReportedPost = { report ->
                    recordPreviewHistory(report, REPORT_STATUS_RESOLVED_DELETED, "", "")
                    val keysToAdd = buildList {
                        report.postDocumentId?.takeIf { it.isNotBlank() }?.let { add(it) }
                        if (report.reportedUsername.equals(initialUsername, ignoreCase = true) &&
                            report.contentPreview.isNotBlank()
                        ) {
                            add(report.contentPreview.lowercase())
                        }
                    }
                    if (keysToAdd.isNotEmpty()) {
                        deletedPostKeys = deletedPostKeys + keysToAdd
                    }
                    val matchingPost = visibleFeedPosts.firstOrNull { post ->
                        (report.postDocumentId != null && post.documentId == report.postDocumentId) ||
                            (report.reportedUsername.equals(initialUsername, ignoreCase = true) &&
                                post.caption.equals(report.contentPreview, ignoreCase = true))
                    }
                    if (matchingPost != null) {
                        onDeletePost?.invoke(matchingPost)
                    }
                },
                onSendWarning = { report, title, description ->
                    recordPreviewHistory(report, REPORT_STATUS_RESOLVED_WARNED, title, description)
                },
            )
        }

        AdminProfileSubScreen.PROFILE_EDIT -> {
            AdminProfileEditScreen(
                onBack = { activeSubScreen = AdminProfileSubScreen.PROFILE },
                onChangeEmail = { /* Aksi ganti email */ },
                onChangePassword = { /* Aksi ganti password */ },
                onHistoryAction = { activeSubScreen = AdminProfileSubScreen.HISTORY_ACTION },
                onLogout = {
                    activeSubScreen = AdminProfileSubScreen.PROFILE
                    onLogout()
                },
            )
        }

        AdminProfileSubScreen.HISTORY_ACTION -> {
            AdminHistoryActionScreen(
                onBack = { activeSubScreen = AdminProfileSubScreen.PROFILE_EDIT },
                historyItems = if (isPreview) previewHistoryItems.toList() else null,
                onPermanentDelete = { item ->
                    if (isPreview) {
                        previewHistoryItems.removeAll { it.id == item.id }
                    }
                },
                onRestore = { item ->
                    if (isPreview) {
                        previewHistoryItems.removeAll { it.id == item.id }
                    }
                },
            )
        }

        AdminProfileSubScreen.PROFILE -> {
            ProfileScreen(
                initialUsername = initialUsername,
                initialDisplayName = initialDisplayName,
                followers = followers,
                following = following,
                initialBio = initialBio,
                initialTab = initialTab,
                feedPosts = visibleFeedPosts,
                gridPosts = visibleGridPosts,
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
                        activeSubScreen = AdminProfileSubScreen.DASHBOARD_REPORT
                    }
                },
                onMenuClick = {
                    activeSubScreen = AdminProfileSubScreen.PROFILE_EDIT
                },
            )
        }
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
