package com.example.olahragakuy.pages

import com.example.olahragakuy.ui.components.LocalKuyRefresh
import com.example.olahragakuy.ui.components.addRefreshableSnapshotListener

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import com.google.firebase.firestore.Source
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.pages.posts.CommentOverlay
import com.example.olahragakuy.pages.posts.EditPostTarget
import com.example.olahragakuy.pages.posts.LocalReportedPostIds
import com.example.olahragakuy.pages.posts.PostUiModel
import com.example.olahragakuy.pages.posts.currentUserOrNull
import com.example.olahragakuy.pages.posts.isOwnPost
import com.example.olahragakuy.pages.posts.resolveDisplayName
import com.example.olahragakuy.pages.posts.submitPostReport
import com.example.olahragakuy.pages.posts.syncCurrentUserToFirestore
import com.example.olahragakuy.pages.posts.toPostUiModel
import com.example.olahragakuy.pages.posts.togglePostLike
import com.example.olahragakuy.pages.posts.withLocalState
import com.example.olahragakuy.pages.profile.user.deleteUserPostFromFirestore
import com.example.olahragakuy.ui.components.CreatePostPromptBar
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.components.HomePostCard
import com.example.olahragakuy.ui.components.KuyBottomNavBar
import com.example.olahragakuy.ui.components.KuyHomeTopBar
import com.example.olahragakuy.ui.components.ReportPostDialog
import com.example.olahragakuy.ui.components.StreakBanner
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    streakDaysCount: Int = 0,
    unreadNotificationCount: Int = 0,
    unreadChatCount: Int = 0,
    onStreakClick: () -> Unit = {},
    onCreatePost: () -> Unit = {},
    onEditPost: (EditPostTarget) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
    onTabSelected: (HomeNavTab) -> Unit = {},
    onUserProfileClick: (userId: String, displayName: String) -> Unit = { _, _ -> },
) {
    val context = LocalContext.current
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    var postPendingReport by remember { mutableStateOf<PostUiModel?>(null) }
    var reportSubmitting by remember { mutableStateOf(false) }
    var reportError by remember { mutableStateOf<String?>(null) }
    var postPendingDelete by remember { mutableStateOf<PostUiModel?>(null) }
    var posts by remember { mutableStateOf<List<PostUiModel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var feedError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var imageRetry by remember { mutableIntStateOf(0) }
    val refreshRegistry = LocalKuyRefresh.current
    val isPreview = LocalInspectionMode.current

    com.example.olahragakuy.ui.components.RegisterKuyRefresh { imageRetry++ }

    DisposableEffect(isPreview, retry) {
        var listener: ListenerRegistration? = null
        if (isPreview) {
            loading = false
            // Refresh progress is managed by KuyRefreshHost.
        } else {
            syncCurrentUserToFirestore(onError = { notice = it })
            if (posts.isEmpty()) {
                loading = true
            }
            feedError = null
            try {
                listener = FirebaseFirestore.getInstance()
                    .collection("posts")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(20)
                    .addRefreshableSnapshotListener(refreshRegistry) { snapshot, error ->
                        loading = false
                        // Refresh progress is managed by KuyRefreshHost.
                        if (error != null) {
                            feedError = error.localizedMessage ?: "Gagal mengambil postingan."
                        } else {
                            posts = snapshot?.documents?.map { it.toPostUiModel() }.orEmpty()
                            feedError = null
                        }
                    }
            } catch (error: Exception) {
                loading = false
                // Refresh progress is managed by KuyRefreshHost.
                feedError = error.localizedMessage ?: "Gagal menghubungkan Firebase."
            }
        }
        onDispose { listener?.remove() }
    }

    val currentUser = remember { currentUserOrNull() }
    val userDisplayName = remember(currentUser) { currentUser.resolveDisplayName() }

    Scaffold(
        containerColor = HomeNavy,
        contentColor = HomeText,
        topBar = {
            KuyHomeTopBar(
                onSearchClick = onSearchClick,
                onNotificationClick = onNotificationClick,
                onChatClick = onChatClick,
                unreadNotificationCount = unreadNotificationCount,
                unreadChatCount = unreadChatCount,
            )
        },
        bottomBar = {
            KuyBottomNavBar(
                selectedTab = HomeNavTab.KOMUNITAS,
                onTabSelected = onTabSelected,
            )
        },
    ) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets)) {
            val widthPx = with(LocalDensity.current) {
                (maxWidth - 40.dp).roundToPx().coerceAtLeast(1)
            }
            val images = rememberPreparedFeedImages(posts, widthPx, imageRetry)
            val visiblePosts = posts.filter { it.imageUrl == null || it.id in images.painters }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    StreakBanner(
                        streakDaysCount = streakDaysCount,
                        onClick = onStreakClick,
                    )
                }
                item {
                    CreatePostPromptBar(
                        userDisplayName = userDisplayName,
                        onClick = onCreatePost,
                    )
                }
                if (images.preparing) item {
                    Text("Menyiapkan foto postingan…", Modifier.padding(20.dp), color = HomeText)
                }
                if (images.failed) item {
                    Column(Modifier.padding(20.dp)) {
                        Text("Beberapa foto belum berhasil dimuat", color = HomeText)
                        TextButton(onClick = { imageRetry++ }) { Text("Coba lagi") }
                    }
                }
                when {
                    loading -> item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = HomeOrange)
                        }
                    }
                    feedError != null -> item {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("Gagal memuat postingan: $feedError", color = HomeText)
                            TextButton(onClick = { retry++ }) {
                                Text("Coba lagi", color = HomeOrange)
                            }
                        }
                    }
                    posts.isEmpty() -> item {
                        Text("Belum ada postingan.", Modifier.padding(20.dp), color = HomeText)
                    }
                    else -> items(visiblePosts, key = { "post:${it.id}" }) { rawPost ->
                        val post = rawPost.withLocalState()
                        val ownPost = isOwnPost(post, currentUser?.uid)
                        val alreadyReported = post.id in LocalReportedPostIds
                        HomePostCard(
                            post = post,
                            preparedImage = images.painters[post.id],
                            isLiked = currentUser?.uid != null && currentUser.uid in post.likedByIds,
                            isOwnPost = ownPost,
                            isReported = alreadyReported,
                            onLikeClick = {
                                togglePostLike(
                                    post = post,
                                    onAuthRequired = { notice = "Silakan masuk untuk menyukai postingan." },
                                    onError = { notice = it },
                                )
                            },
                            onEditClick = {
                                onEditPost(
                                    EditPostTarget(
                                        postId = post.id,
                                        authorId = post.authorId,
                                        authorName = post.authorName,
                                        caption = post.caption,
                                        imageUrl = post.imageUrl,
                                        category = post.category,
                                        taggedFriends = post.taggedFriends,
                                        taggedNames = post.taggedNames,
                                    ),
                                )
                            },
                            onCopyClick = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                clipboard?.setPrimaryClip(ClipData.newPlainText("Caption", post.caption))
                                Toast.makeText(context, "Caption disalin", Toast.LENGTH_SHORT).show()
                            },
                            onShareClick = {
                                val shareText = buildString {
                                    append("${post.authorName}: ${post.caption}")
                                    if (!post.imageUrl.isNullOrBlank()) {
                                        append("\n${post.imageUrl}")
                                    }
                                }
                                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, shareText)
                                }
                                context.startActivity(
                                    Intent.createChooser(shareIntent, "Bagikan postingan"),
                                )
                            },
                            onDeleteClick = {
                                postPendingDelete = post
                            },
                            onReportClick = {
                                when {
                                    currentUser == null -> {
                                        notice = "Silakan masuk untuk melaporkan postingan."
                                    }
                                    alreadyReported -> {
                                        notice = "Kamu sudah melaporkan postingan ini sebelumnya."
                                    }
                                    else -> {
                                        reportError = null
                                        postPendingReport = post
                                    }
                                }
                            },
                            onAction = { notice = it },
                            onCommentClick = { activeCommentPostId = post.id },
                            onAuthorClick = { onUserProfileClick(post.authorId, post.authorName) },
                        )
                    }
                }
            }
        }
    }

    postPendingDelete?.let { target ->
        val deleteRed = Color(0xFFE53935)
        AlertDialog(
            onDismissRequest = { postPendingDelete = null },
            containerColor = Color(0xFF16324F),
            titleContentColor = HomeText,
            textContentColor = HomeText,
            title = {
                Text(
                    text = "Hapus Postingan?",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                )
            },
            text = {
                Text(
                    text = "Postingan ini akan dihapus secara permanen dan tidak dapat dikembalikan.",
                    fontFamily = Poppins,
                    color = HomeText.copy(alpha = 0.85f),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val user = currentUser
                        postPendingDelete = null
                        if (user == null) {
                            notice = "Silakan masuk untuk menghapus postingan."
                            return@TextButton
                        }
                        val previousPosts = posts
                        posts = posts.filterNot { it.id == target.id }
                        deleteUserPostFromFirestore(
                            user = user,
                            documentId = target.id,
                            onError = { err ->
                                posts = previousPosts
                                notice = err
                            },
                        )
                    },
                ) {
                    Text(
                        text = "Hapus",
                        color = deleteRed,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { postPendingDelete = null }) {
                    Text("Batal", color = HomeText.copy(alpha = 0.8f), fontFamily = Poppins)
                }
            },
        )
    }

    postPendingReport?.let { targetPost ->
        ReportPostDialog(
            authorName = targetPost.authorName,
            submitting = reportSubmitting,
            errorMessage = reportError,
            onDismiss = {
                if (!reportSubmitting) {
                    postPendingReport = null
                    reportError = null
                }
            },
            onSubmit = { reason, detail ->
                if (reportSubmitting) return@ReportPostDialog
                reportSubmitting = true
                reportError = null
                submitPostReport(
                    post = targetPost,
                    reason = reason,
                    detail = detail,
                    onSuccess = { message ->
                        reportSubmitting = false
                        postPendingReport = null
                        reportError = null
                        notice = message
                    },
                    onError = { err ->
                        reportSubmitting = false
                        reportError = err
                    },
                )
            },
        )
    }

    notice?.let { message ->
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text("Informasi") },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { notice = null }) { Text("Mengerti") } },
        )
    }

    CommentOverlay(
        visible = activeCommentPostId != null,
        postId = activeCommentPostId,
        onDismiss = { activeCommentPostId = null },
    )
}

@Composable
fun InProgressTabScreen(
    selectedTab: HomeNavTab,
    onTabSelected: (HomeNavTab) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
) {
    Scaffold(
        containerColor = HomeNavy,
        contentColor = HomeText,
        topBar = {
            KuyHomeTopBar(
                onSearchClick = onSearchClick,
                onNotificationClick = onNotificationClick,
                onChatClick = onChatClick,
            )
        },
        bottomBar = {
            KuyBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = onTabSelected,
            )
        },
    ) { insets ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = HomeBlue,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(KuyButton.copy(alpha = 0.25f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(selectedTab.iconRes),
                            contentDescription = selectedTab.label,
                            tint = HomeOrange,
                            modifier = Modifier.size(38.dp),
                        )
                    }
                    Spacer(Modifier.height(16.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = KuyButton,
                    ) {
                        Text(
                            text = "Masih Dalam Proses",
                            color = HomeText,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(
                        text = "Halaman ${selectedTab.label}",
                        style = MaterialTheme.typography.titleLarge,
                        color = HomeText,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Fitur ${selectedTab.label} saat ini masih dalam tahap pengembangan dan akan segera hadir.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = HomeText.copy(alpha = 0.78f),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeScreenPreview() {
    OlahragaKuyTheme { HomeScreen() }
}
