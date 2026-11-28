package com.example.olahragakuy.pages.posts

import com.example.olahragakuy.ui.components.LocalKuyRefresh
import com.example.olahragakuy.ui.components.addRefreshableSnapshotListener

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.components.HomePostCard
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.components.KuyBottomNavBar
import com.example.olahragakuy.ui.components.KuyPostSearchBar
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

private val SuggestionCardColor = Color(0xFF0E2238)

private val PreviewSearchPosts = listOf(
    PostUiModel(
        id = "preview-1",
        authorName = "SayurOlahraga",
        time = "3 jam lalu",
        caption = "Ayo mulai konsumsi sayur supaya harimu makin sehat",
        category = "Makanan Sehat",
        searchKeywords = listOf("makanan sehat", "sayur", "sehat"),
        likesCount = 57L,
        commentsCount = 4L,
        sharesCount = 8L,
    ),
    PostUiModel(
        id = "preview-2",
        authorName = "ManusiaBiasa",
        time = "4 jam lalu",
        caption = "Hari pertama minum susu bulking , semoga konsisten",
        category = "Makanan Sehat, Bulking",
        searchKeywords = listOf("makanan sehat", "bulking", "susu"),
        likesCount = 24L,
        commentsCount = 2L,
        sharesCount = 1L,
    ),
)

/**
 * Halaman Pencarian Postingan:
 * - Saat mengetik / sebelum submit (`showResults == false`): menampilkan kotak saran (`PencarianPostingan`).
 * - Setelah memilih saran atau menekan tombol cari (`showResults == true`): menampilkan hasil postingan
 *   beserta navbar bawah (`PencarianPostingan2`).
 */
@Composable
fun SearchPostScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialQuery: String = "",
    initialShowResults: Boolean = false,
    onUserProfileClick: (userId: String, displayName: String) -> Unit = { _, _ -> },
    onTabSelected: (HomeNavTab) -> Unit = {},
) {
    val refreshRegistry = LocalKuyRefresh.current
    val isPreview = LocalInspectionMode.current
    val focusManager = LocalFocusManager.current
    var query by rememberSaveable { mutableStateOf(initialQuery) }
    var showResults by rememberSaveable { mutableStateOf(initialShowResults) }
    var posts by remember { mutableStateOf(if (isPreview) PreviewSearchPosts else emptyList()) }
    var loading by remember { mutableStateOf(!isPreview) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }

    DisposableEffect(isPreview, retry) {
        var listener: ListenerRegistration? = null
        if (!isPreview) {
            loading = true
            loadError = null
            try {
                listener = FirebaseFirestore.getInstance()
                    .collection("posts")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(50)
                    .addRefreshableSnapshotListener(refreshRegistry) { snapshot, error ->
                        loading = false
                        if (error != null) {
                            loadError = error.localizedMessage ?: "Gagal memuat postingan."
                        } else {
                            posts = snapshot?.documents?.map { it.toPostUiModel() }.orEmpty()
                            loadError = null
                        }
                    }
            } catch (e: Exception) {
                loading = false
                loadError = e.localizedMessage ?: "Gagal menghubungkan Firebase."
            }
        }
        onDispose { listener?.remove() }
    }

    val currentUser = remember { currentUserOrNull() }

    val effectivePosts = posts.map { it.withLocalState() }
    val suggestions = remember(effectivePosts, query) {
        suggestPostQueries(effectivePosts, query)
    }
    val filteredPosts = remember(effectivePosts, query) {
        effectivePosts.filter { matchesPostQuery(it, query) }
    }

    val runSearch: (String) -> Unit = { selectedQuery ->
        val trimmed = selectedQuery.trim()
        query = trimmed
        showResults = true
        focusManager.clearFocus()
    }

    Scaffold(
        modifier = modifier,
        containerColor = HomeNavy,
        contentColor = HomeText,
        topBar = {
            Column(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Box(Modifier.fillMaxWidth()) {
                    KuyBackButton(onClick = onBack)
                }
                KuyPostSearchBar(
                    query = query,
                    onQueryChange = {
                        query = it
                        showResults = false
                    },
                    onSearch = { runSearch(query) },
                )
            }
        },
        bottomBar = {
            if (showResults) {
                KuyBottomNavBar(
                    selectedTab = HomeNavTab.KOMUNITAS,
                    onTabSelected = onTabSelected,
                )
            }
        },
    ) { insets ->
        if (!showResults) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(insets)
                    .padding(horizontal = 20.dp),
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp),
                    shape = RoundedCornerShape(18.dp),
                    color = SuggestionCardColor,
                    contentColor = HomeText,
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                    ) {
                        suggestions.forEach { item ->
                            Text(
                                text = item,
                                color = HomeText,
                                fontSize = 12.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { runSearch(item) }
                                    .padding(horizontal = 18.dp, vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                when {
                    loading -> item {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = HomeOrange)
                        }
                    }
                    loadError != null -> item {
                        Column(Modifier.fillMaxWidth().padding(20.dp)) {
                            Text("Gagal memuat postingan: $loadError", color = HomeText)
                            TextButton(onClick = { retry++ }) {
                                Text("Coba lagi", color = HomeOrange)
                            }
                        }
                    }
                    filteredPosts.isEmpty() -> item {
                        Text(
                            text = if (query.isBlank()) {
                                "Belum ada postingan."
                            } else {
                                "Tidak ada postingan untuk \"$query\"."
                            },
                            modifier = Modifier.padding(20.dp),
                            color = HomeText,
                        )
                    }
                    else -> items(filteredPosts, key = { "search_post:${it.id}" }) { post ->
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
                            onAuthorClick = { onUserProfileClick(post.authorId, post.authorName) },
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
private fun PencarianPostinganPreview() {
    OlahragaKuyTheme {
        SearchPostScreen(onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PencarianPostingan2Preview() {
    OlahragaKuyTheme {
        SearchPostScreen(
            onBack = {},
            initialQuery = "Makanan Sehat",
            initialShowResults = true,
        )
    }
}
