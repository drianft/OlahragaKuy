package com.example.olahragakuy.pages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
    val isPreview = LocalInspectionMode.current

    DisposableEffect(isPreview, retry) {
        var listener: ListenerRegistration? = null
        if (isPreview) {
            loading = false
        } else {
            syncCurrentUserToFirestore()
            loading = true
            feedError = null
            try {
                listener = FirebaseFirestore.getInstance()
                    .collection("posts")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(20)
                    .addSnapshotListener { snapshot, error ->
                        loading = false
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
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(insets),
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
                else -> items(posts, key = { "post:${it.id}" }) { rawPost ->
                    val post = rawPost.withLocalState()
                    HomePostCard(
                        post = post,
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
