package com.example.olahragakuy.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import com.google.firebase.firestore.Source
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.olahragakuy.pages.posts.CommentOverlay
import com.example.olahragakuy.pages.posts.PostUiModel
import com.example.olahragakuy.pages.posts.currentUserOrNull
import com.example.olahragakuy.pages.posts.resolveDisplayName
import com.example.olahragakuy.pages.posts.seedInitialFirestoreCollectionsIfNeeded
import com.example.olahragakuy.pages.posts.syncCurrentUserToFirestore
import com.example.olahragakuy.pages.posts.toPostUiModel
import com.example.olahragakuy.pages.posts.togglePostLike
import com.example.olahragakuy.pages.posts.withLocalState
import com.example.olahragakuy.ui.components.CreatePostPromptBar
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.components.HomePostCard
import com.example.olahragakuy.ui.components.KuyBottomNavBar
import com.example.olahragakuy.ui.components.KuyHomeTopBar
import com.example.olahragakuy.ui.components.StreakBanner
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCreatePost: () -> Unit = {},
    onSearchClick: () -> Unit = {},
    onNotificationClick: () -> Unit = {},
    onChatClick: () -> Unit = {},
) {
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    var posts by remember { mutableStateOf<List<PostUiModel>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var feedError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var refreshRequest by remember { mutableIntStateOf(0) }
    var imageRetry by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    val revisions = remember { FeedRevision() }
    val isPreview = LocalInspectionMode.current
    val feedQuery = remember(isPreview) {
        if (isPreview) null else FirebaseFirestore.getInstance()
            .collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(20)
    }
    val refresh: () -> Unit = {
        if (!refreshing && !isPreview) {
            refreshing = true
            refreshRequest++
            imageRetry++
        }
    }

    LaunchedEffect(refreshRequest) {
        if (refreshRequest == 0 || feedQuery == null) return@LaunchedEffect
        val token = revisions.capture()
        try {
            val snapshot = withTimeoutOrNull(30_000) {
                suspendCancellableCoroutine { continuation ->
                    feedQuery.get(Source.SERVER)
                        .addOnSuccessListener { if (continuation.isActive) continuation.resume(it) }
                        .addOnFailureListener { if (continuation.isActive) continuation.resumeWithException(it) }
                }
            }
            if (snapshot == null) {
                feedError = "Koneksi terlalu lama. Tarik ke bawah untuk mencoba lagi."
            } else if (revisions.accepts(token)) {
                posts = snapshot.documents.map { it.toPostUiModel() }
                feedError = null
                loading = false
            }
        } catch (cancelled: kotlinx.coroutines.CancellationException) {
            throw cancelled
        } catch (error: Exception) {
            feedError = error.localizedMessage ?: "Gagal memperbarui postingan."
        } finally {
            refreshing = false
        }
    }

    DisposableEffect(isPreview, retry) {
        var listener: ListenerRegistration? = null
        if (isPreview) {
            loading = false
        } else {
            syncCurrentUserToFirestore()
            seedInitialFirestoreCollectionsIfNeeded()
            loading = posts.isEmpty()
            feedError = null
            try {
                listener = feedQuery?.addSnapshotListener { snapshot, error ->
                        loading = false
                        revisions.changed()
                        if (error != null) {
                            feedError = error.localizedMessage ?: "Gagal mengambil postingan."
                        } else {
                            posts = snapshot?.documents?.map { it.toPostUiModel() }.orEmpty()
                            feedError = null
                        }
                    }
            } catch (error: Exception) {
                loading = false
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
            )
        },
        bottomBar = {
            KuyBottomNavBar(
                selectedTab = HomeNavTab.KOMUNITAS,
                onTabSelected = { tab ->
                    if (tab == HomeNavTab.PROFIL && currentUser != null) {
                        notice = "Masuk sebagai ${currentUser.email ?: userDisplayName}"
                    } else if (tab != HomeNavTab.KOMUNITAS) {
                        notice = "Halaman ${tab.label} belum tersedia."
                    }
                },
            )
        },
    ) { insets ->
        BoxWithConstraints(Modifier.fillMaxSize().padding(insets)) {
            val widthPx = with(LocalDensity.current) {
                (maxWidth - 40.dp).roundToPx().coerceAtLeast(1)
            }
            val images = rememberPreparedFeedImages(posts, widthPx, imageRetry)
            val visiblePosts = posts.filter { it.imageUrl == null || it.id in images.painters }
            PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = refresh,
                modifier = Modifier.fillMaxSize(),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item {
                        StreakBanner(onClick = { notice = "Fitur streak belum tersedia." })
                    }
                    item {
                        CreatePostPromptBar(
                            userDisplayName = userDisplayName,
                            onClick = onCreatePost,
                        )
                    }
                    if (loading && posts.isEmpty()) item {
                        Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = HomeOrange)
                        }
                    }
                    if (feedError != null) item {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("Gagal memuat postingan: $feedError", color = HomeText)
                            TextButton(onClick = { retry++; refresh() }) {
                                Text("Coba lagi", color = HomeOrange)
                            }
                        }
                    }
                    if (images.preparing) item {
                        Text("Menyiapkan foto postingan…", Modifier.padding(20.dp), color = HomeText)
                    }
                    if (images.failed) item {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("Beberapa foto belum berhasil dimuat", color = HomeText)
                            TextButton(onClick = { imageRetry++ }) {
                                Text("Coba lagi", color = HomeOrange)
                            }
                        }
                    }
                    if (!loading && feedError == null && posts.isEmpty()) item {
                        Text("Belum ada postingan.", Modifier.padding(20.dp), color = HomeText)
                    }
                    items(visiblePosts, key = { "post:${it.id}" }) { rawPost ->
                        val post = rawPost.withLocalState()
                        HomePostCard(
                            post = post,
                            preparedImage = images.painters[post.id],
                            isLiked = currentUser?.uid != null && currentUser.uid in post.likedByIds,
                            onLikeClick = {
                                togglePostLike(
                                    post = post,
                                    onAuthRequired = { notice = "Silakan masuk untuk menyukai postingan." },
                                    onError = { notice = it },
                                )
                            },
                            onAction = { notice = it },
                            onCommentClick = { activeCommentPostId = post.id },
                        )
                    }
                }
            }
        }
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

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeScreenPreview() {
    OlahragaKuyTheme { HomeScreen() }
}
