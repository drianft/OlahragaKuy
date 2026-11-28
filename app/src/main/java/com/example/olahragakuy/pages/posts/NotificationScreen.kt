package com.example.olahragakuy.pages.posts

import com.example.olahragakuy.ui.components.LocalKuyRefresh
import com.example.olahragakuy.ui.components.addRefreshableSnapshotListener

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.components.HomePostCard
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.components.KuyBottomNavBar
import com.example.olahragakuy.ui.components.KuyUserAvatar
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

private const val DAY_MS = 24L * 60 * 60 * 1000
private const val PREVIEW_NOW_MS = 1_760_000_000_000L

private val PreviewNotifications = listOf(
    NotificationUiModel(
        id = "n1",
        actorId = "u1",
        actorName = "Monster Basket",
        type = NotificationType.LIKE,
        postId = "post-liked",
        createdAtMillis = PREVIEW_NOW_MS - 1 * DAY_MS,
    ),
    NotificationUiModel(
        id = "n2",
        actorId = "u2",
        actorName = "Nama Pengguna",
        type = NotificationType.LIKE,
        postId = "post-liked",
        createdAtMillis = PREVIEW_NOW_MS - 2 * DAY_MS,
    ),
    NotificationUiModel(
        id = "n3",
        actorId = "u3",
        actorName = "KenanganTerindah",
        type = NotificationType.TAG,
        postId = "post-tagged",
        createdAtMillis = PREVIEW_NOW_MS - 3 * DAY_MS,
    ),
    NotificationUiModel(
        id = "n4",
        actorId = "u2",
        actorName = "Nama Pengguna",
        type = NotificationType.LIKE,
        postId = "post-liked",
        createdAtMillis = PREVIEW_NOW_MS - 10 * DAY_MS,
    ),
    NotificationUiModel(
        id = "n5",
        actorId = "u2",
        actorName = "Nama Pengguna",
        type = NotificationType.LIKE,
        postId = "post-liked",
        createdAtMillis = PREVIEW_NOW_MS - 12 * DAY_MS,
    ),
    NotificationUiModel(
        id = "n6",
        actorId = "u3",
        actorName = "Nama Pengguna",
        type = NotificationType.TAG,
        postId = "post-tagged",
        createdAtMillis = PREVIEW_NOW_MS - 15 * DAY_MS,
    ),
    NotificationUiModel(
        id = "n7",
        actorId = "u2",
        actorName = "Nama Pengguna",
        type = NotificationType.COMMENT,
        postId = "post-liked",
        createdAtMillis = PREVIEW_NOW_MS - 18 * DAY_MS,
    ),
)

private val PreviewNotificationPosts = mapOf(
    "post-tagged" to PostUiModel(
        id = "post-tagged",
        authorName = "KenanganTerindah",
        time = "3 jam lalu",
        caption = "Jogging santuy lagi guys with my bestie",
        likesCount = 57L,
        commentsCount = 4L,
        sharesCount = 8L,
    ),
    "post-liked" to PostUiModel(
        id = "post-liked",
        authorName = "BibiOlahraga",
        time = "2 jam lalu",
        caption = "Always on Proces",
        likesCount = 57L,
        likedByNames = listOf("NamaPengguna", "Monster Basket"),
        commentsCount = 4L,
        sharesCount = 8L,
    ),
)

private val NotificationAvatarColors = listOf(
    Color(0xFF4FC3A1),
    Color(0xFFF2B84B),
    Color(0xFF5B7CFA),
    Color(0xFF7FA7E8),
)

/**
 * Halaman Notifikasi (`Notifikasi`, `Postinganditandai`, & `Postingandisukai`):
 * - Menampilkan daftar aktivitas (suka, komentar, dan tandai teman) yang dikelompokkan
 *   menjadi "7 hari terakhir" dan "30 hari terakhir".
 * - Mengetuk salah satu notifikasi akan menampilkan detail postingan terkait.
 */
@Composable
fun NotificationScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialSelectedPostId: String? = null,
    onUserProfileClick: (userId: String, displayName: String) -> Unit = { _, _ -> },
    onTabSelected: (HomeNavTab) -> Unit = {},
) {
    val refreshRegistry = LocalKuyRefresh.current
    val isPreview = LocalInspectionMode.current
    var remoteNotifications by remember {
        mutableStateOf(if (isPreview) PreviewNotifications else emptyList())
    }
    var derivedNotifications by remember {
        mutableStateOf<List<NotificationUiModel>>(emptyList())
    }
    var selectedPostId by rememberSaveable { mutableStateOf(initialSelectedPostId) }
    var selectedPost by remember(selectedPostId, isPreview) {
        mutableStateOf(if (isPreview) PreviewNotificationPosts[selectedPostId] else null)
    }
    var loading by remember { mutableStateOf(!isPreview) }
    var detailLoading by remember { mutableStateOf(false) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedWarning by remember { mutableStateOf<NotificationUiModel?>(null) }
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val currentUser = remember { currentUserOrNull() }
    val userDisplayName = remember(currentUser) { currentUser.resolveDisplayName() }

    if (!isPreview) {
        DisposableEffect(currentUser?.uid) {
            RealtimeBadgeStore.setNotificationScreenOpen(
                isOpen = true,
                userId = currentUser?.uid,
                context = context,
            )
            onDispose {
                RealtimeBadgeStore.setNotificationScreenOpen(
                    isOpen = false,
                    userId = currentUser?.uid,
                    context = context,
                )
            }
        }
    }

    DisposableEffect(isPreview, currentUser?.uid, retry) {
        var notifListener: ListenerRegistration? = null
        var postsListener: ListenerRegistration? = null
        val commentListeners = mutableListOf<ListenerRegistration>()

        if (!isPreview) {
            val uid = currentUser?.uid
            val db = FirebaseFirestore.getInstance()
            loading = true
            loadError = null

            // 1. Dengarkan koleksi posts & comments (selalu diizinkan oleh aturan Firestore yang ada)
            try {
                postsListener = db.collection("posts")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(30)
                    .addRefreshableSnapshotListener(refreshRegistry) { postSnap, postErr ->
                        loading = false
                        if (postErr != null && remoteNotifications.isEmpty()) {
                            val isDenied = postErr.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
                            if (!isDenied) {
                                loadError = postErr.localizedMessage ?: "Gagal memuat notifikasi."
                            }
                            return@addRefreshableSnapshotListener
                        }
                        val docs = postSnap?.documents.orEmpty()
                        val fromPosts = mutableListOf<NotificationUiModel>()

                        commentListeners.forEach { it.remove() }
                        commentListeners.clear()

                        docs.forEach { doc ->
                            val data = doc.data.orEmpty()
                            val postModel = parsePostUiModel(doc.id, data).withLocalState()
                            val postMillis = doc.getTimestamp("createdAt")?.toDate()?.time
                                ?: System.currentTimeMillis()
                            val isMyPost = (uid != null && postModel.authorId == uid) ||
                                postModel.authorName.equals(userDisplayName, ignoreCase = true) ||
                                postModel.authorId.isBlank()

                            // Notifikasi Suka dari field likedByNames pada postingan milik pengguna
                            if (isMyPost) {
                                postModel.likedByNames.forEachIndexed { idx, likerName ->
                                    fromPosts.add(
                                        NotificationUiModel(
                                            id = "derived_like_${doc.id}_$idx",
                                            actorId = "",
                                            actorName = likerName,
                                            type = NotificationType.LIKE,
                                            postId = doc.id,
                                            createdAtMillis = postMillis,
                                        ),
                                    )
                                }
                            }

                            // Notifikasi Tandai Teman bila pengguna ditandai atau postingan memiliki tag
                            val taggedIds = (data["taggedFriendIds"] as? List<*>)
                                .orEmpty()
                                .mapNotNull { it as? String }
                            val isTagged = (uid != null && uid in taggedIds) ||
                                postModel.taggedNames.any { it.equals(userDisplayName, ignoreCase = true) } ||
                                (isMyPost && postModel.taggedNames.isNotEmpty())
                            if (isTagged) {
                                fromPosts.add(
                                    NotificationUiModel(
                                        id = "derived_tag_${doc.id}",
                                        actorId = postModel.authorId,
                                        actorName = postModel.authorName,
                                        type = NotificationType.TAG,
                                        postId = doc.id,
                                        createdAtMillis = postMillis,
                                    ),
                                )
                            }

                            // Notifikasi Komentar dari sub-koleksi comments pada postingan milik pengguna
                            if (isMyPost) {
                                val cReg = doc.reference.collection("comments")
                                    .orderBy("createdAt", Query.Direction.DESCENDING)
                                    .limit(10)
                                    .addRefreshableSnapshotListener(refreshRegistry) { cSnap, _ ->
                                        val commentNotifs = cSnap?.documents?.map { cDoc ->
                                            val cAuthor = cDoc.getString("authorName")?.takeIf { it.isNotBlank() }
                                                ?: "Pengguna"
                                            val cMillis = cDoc.getTimestamp("createdAt")?.toDate()?.time
                                                ?: postMillis
                                            NotificationUiModel(
                                                id = "derived_comment_${doc.id}_${cDoc.id}",
                                                actorId = cDoc.getString("authorId").orEmpty(),
                                                actorName = cAuthor,
                                                type = NotificationType.COMMENT,
                                                postId = doc.id,
                                                createdAtMillis = cMillis,
                                            )
                                        }.orEmpty()
                                        derivedNotifications = (
                                            derivedNotifications.filterNot {
                                                it.id.startsWith("derived_comment_${doc.id}_")
                                            } + commentNotifs
                                        )
                                    }
                                commentListeners.add(cReg)
                            }
                        }

                        val existingComments = derivedNotifications.filter {
                            it.id.startsWith("derived_comment_")
                        }
                        derivedNotifications = fromPosts + existingComments
                    }
            } catch (_: Exception) {
                loading = false
            }

            // 2. Dengarkan sub-koleksi users/{uid}/notifications bila sudah diizinkan di Firestore Rules
            if (uid != null) {
                try {
                    notifListener = db.collection("users")
                        .document(uid)
                        .collection("notifications")
                        .orderBy("createdAt", Query.Direction.DESCENDING)
                        .limit(50)
                        .addRefreshableSnapshotListener(refreshRegistry) { snapshot, error ->
                            loading = false
                            if (error == null) {
                                remoteNotifications = snapshot?.documents?.mapNotNull { doc ->
                                    parseNotificationUiModel(doc.id, doc.data.orEmpty())
                                }.orEmpty()
                                loadError = null
                            }
                            // Jika PERMISSION_DENIED pada sub-koleksi notifications, abaikan error
                            // dan gunakan derivedNotifications + LocalNotifications.
                        }
                } catch (_: Exception) {
                    loading = false
                }
            }
        }
        onDispose {
            notifListener?.remove()
            postsListener?.remove()
            commentListeners.forEach { it.remove() }
        }
    }

    DisposableEffect(isPreview, selectedPostId) {
        var postListener: ListenerRegistration? = null
        val postId = selectedPostId
        if (!isPreview && postId != null) {
            detailLoading = true
            try {
                postListener = FirebaseFirestore.getInstance()
                    .collection("posts")
                    .document(postId)
                    .addRefreshableSnapshotListener(refreshRegistry) { snapshot, _ ->
                        detailLoading = false
                        selectedPost = snapshot?.takeIf { it.exists() }?.toPostUiModel()
                    }
            } catch (_: Exception) {
                detailLoading = false
            }
        }
        onDispose { postListener?.remove() }
    }

    BackHandler(enabled = selectedPostId != null) {
        selectedPostId = null
    }

    val localSnapshot = LocalNotifications.filter { item ->
        item.type != NotificationType.REPORT_SENT ||
            currentUser?.uid == null ||
            item.actorId == currentUser.uid
    }
    val allNotifications = if (isPreview) {
        PreviewNotifications
    } else {
        (localSnapshot + remoteNotifications + derivedNotifications)
            .distinctBy { item ->
                when (item.type) {
                    NotificationType.WARNING,
                    NotificationType.REPORT_SENT,
                    -> "${item.type.code}:${item.id}"
                    else -> "${item.type.code}:${item.postId}:${item.actorName.lowercase()}"
                }
            }
    }

    if (!isPreview) {
        val latestNotifMillis = allNotifications.maxOfOrNull { it.createdAtMillis } ?: 0L
        LaunchedEffect(currentUser?.uid, latestNotifMillis) {
            RealtimeBadgeStore.markNotificationsSeen(
                userId = currentUser?.uid,
                latestTimestampMillis = maxOf(System.currentTimeMillis(), latestNotifMillis),
                context = context,
            )
        }
    }

    val groupedNotifications = remember(allNotifications, isPreview) {
        groupNotificationsByPeriod(
            notifications = allNotifications,
            nowMillis = if (isPreview) PREVIEW_NOW_MS else System.currentTimeMillis(),
        )
    }

    Scaffold(
        modifier = modifier,
        containerColor = HomeNavy,
        contentColor = HomeText,
        topBar = {
            if (selectedPostId == null) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    KuyBackButton(
                        text = "Notifikasi",
                        onClick = onBack,
                    )
                }
            } else {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                ) {
                    KuyBackButton(
                        text = "Notifikasi",
                        onClick = { selectedPostId = null },
                    )
                }
            }
        },
        bottomBar = {
            KuyBottomNavBar(
                selectedTab = HomeNavTab.KOMUNITAS,
                onTabSelected = onTabSelected,
            )
        },
    ) { insets ->
        if (selectedPostId != null) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(insets),
            ) {
                when {
                    detailLoading -> Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(color = HomeOrange)
                    }
                    selectedPost == null -> Text(
                        "Postingan tidak ditemukan atau telah dihapus.",
                        modifier = Modifier.padding(20.dp),
                        color = HomeText,
                    )
                    else -> {
                        val post = selectedPost!!.withLocalState()
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
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets),
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
                            Text("Gagal memuat notifikasi: $loadError", color = HomeText)
                            TextButton(onClick = { retry++ }) {
                                Text("Coba lagi", color = HomeOrange)
                            }
                        }
                    }
                    groupedNotifications.isEmpty() -> item {
                        Text(
                            "Belum ada notifikasi aktivitas pada postinganmu.",
                            modifier = Modifier.padding(20.dp),
                            color = HomeText,
                        )
                    }
                    else -> {
                        groupedNotifications.forEach { (sectionTitle, sectionItems) ->
                            item(key = "header:$sectionTitle") {
                                Text(
                                    text = sectionTitle,
                                    color = HomeText,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    modifier = Modifier.padding(
                                        start = 20.dp,
                                        end = 20.dp,
                                        top = 12.dp,
                                        bottom = 8.dp,
                                    ),
                                )
                            }
                            itemsIndexed(
                                items = sectionItems,
                                key = { _, item -> "notif:${item.id}" },
                            ) { index, item ->
                                val isSystemNotice = item.type == NotificationType.WARNING ||
                                    item.type == NotificationType.REPORT_SENT
                                NotificationItemRow(
                                    item = item,
                                    avatarColor = when (item.type) {
                                        NotificationType.WARNING -> HomeOrange
                                        NotificationType.REPORT_SENT -> Color(0xFF4CAF50)
                                        else -> NotificationAvatarColors[
                                            index % NotificationAvatarColors.size
                                        ]
                                    },
                                    onAvatarClick = {
                                        if (isSystemNotice) {
                                            selectedWarning = item
                                        } else {
                                            onUserProfileClick(item.actorId, item.actorName)
                                        }
                                    },
                                    onClick = {
                                        if (isSystemNotice) {
                                            selectedWarning = item
                                        } else if (item.postId.isNotBlank()) {
                                            selectedPostId = item.postId
                                        } else {
                                            onUserProfileClick(item.actorId, item.actorName)
                                        }
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    selectedWarning?.let { infoNotif ->
        val defaultTitle = if (infoNotif.type == NotificationType.REPORT_SENT) {
            "Laporan Berhasil Dikirim"
        } else {
            "Notifikasi Warning"
        }
        val defaultDesc = if (infoNotif.type == NotificationType.REPORT_SENT) {
            "Laporanmu telah diterima dan akan segera ditinjau serta ditindaklanjuti oleh Admin."
        } else {
            "Admin mengirimkan peringatan terkait aktivitas akun atau postingan Anda."
        }
        AlertDialog(
            onDismissRequest = { selectedWarning = null },
            title = {
                Text(infoNotif.title.ifBlank { defaultTitle })
            },
            text = {
                Text(infoNotif.description.ifBlank { defaultDesc })
            },
            confirmButton = {
                TextButton(onClick = { selectedWarning = null }) { Text("Mengerti") }
            },
            dismissButton = if (infoNotif.postId.isNotBlank()) {
                {
                    TextButton(
                        onClick = {
                            val targetPostId = infoNotif.postId
                            selectedWarning = null
                            selectedPostId = targetPostId
                        },
                    ) { Text("Lihat Postingan") }
                }
            } else {
                null
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
private fun NotificationItemRow(
    item: NotificationUiModel,
    avatarColor: Color,
    onAvatarClick: () -> Unit,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(HomeBlue)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier.clickable(
                role = Role.Button,
                onClickLabel = "Buka profil ${item.actorName}",
                onClick = onAvatarClick,
            ),
        ) {
            KuyUserAvatar(
                name = item.actorName,
                size = 38.dp,
                fontSize = 15.sp,
                backgroundColor = avatarColor,
            )
        }
        Text(
            text = buildAnnotatedString {
                when (item.type) {
                    NotificationType.WARNING -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = HomeOrange)) {
                            append(item.title.ifBlank { "Notifikasi Warning" })
                        }
                        if (item.description.isNotBlank()) {
                            append(": ")
                            append(item.description)
                        }
                    }
                    NotificationType.REPORT_SENT -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = Color(0xFF81C784))) {
                            append(item.title.ifBlank { "Laporan Berhasil Dikirim" })
                        }
                        if (item.description.isNotBlank()) {
                            append(": ")
                            append(item.description)
                        }
                    }
                    else -> {
                        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(item.actorName)
                        }
                        append(" ")
                        append(item.type.message)
                    }
                }
            },
            color = HomeText,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            modifier = Modifier.weight(1f),
        )
        NotificationPostIcon()
    }
}

@Composable
private fun NotificationPostIcon() {
    Canvas(Modifier.size(22.dp)) {
        drawRoundRect(
            color = Color(0xFF426386),
            topLeft = Offset(0f, 0f),
            size = size,
            cornerRadius = CornerRadius(4.dp.toPx()),
        )
        drawRoundRect(
            color = Color(0xFFD5E3F2),
            topLeft = Offset(size.width * 0.16f, size.height * 0.14f),
            size = Size(size.width * 0.45f, size.height * 0.1f),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
        drawRoundRect(
            color = Color(0xFFD5E3F2),
            topLeft = Offset(size.width * 0.16f, size.height * 0.34f),
            size = Size(size.width * 0.68f, size.height * 0.5f),
            cornerRadius = CornerRadius(2.dp.toPx()),
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun NotifikasiPreview() {
    OlahragaKuyTheme {
        NotificationScreen(onBack = {})
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PostinganDitandaiPreview() {
    OlahragaKuyTheme {
        NotificationScreen(
            onBack = {},
            initialSelectedPostId = "post-tagged",
        )
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun PostinganDisukaiPreview() {
    OlahragaKuyTheme {
        NotificationScreen(
            onBack = {},
            initialSelectedPostId = "post-liked",
        )
    }
}
