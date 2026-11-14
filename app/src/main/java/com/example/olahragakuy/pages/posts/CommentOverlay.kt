package com.example.olahragakuy.pages.posts

import android.content.Intent
import android.content.ActivityNotFoundException
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.TextButton
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

data class CommentData(
    val id: String = "",
    val authorId: String = "",
    val replyToCommentId: String? = null,
    val replyToAuthorName: String? = null,
    val name: String,
    val time: String,
    val text: String,
    val avatarColor: Color = Color(0xFF6587A4),
    val avatarText: String = "",
)

private val AvatarColors = listOf(
    Color(0xFFFFD54F),
    Color(0xFF81C784),
    Color(0xFF80CBC4),
    Color(0xFF64B5F6),
    Color(0xFFFF8A65),
    Color(0xFFBA68C8),
)

@Composable
fun CommentOverlay(
    visible: Boolean,
    postId: String?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible || postId == null) return

    BackHandler(enabled = visible) { onDismiss() }

    val context = LocalContext.current
    val inputFocus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    var replyTarget by remember(postId) { mutableStateOf<CommentData?>(null) }
    var newCommentText by remember(postId) { mutableStateOf("") }
    var firestoreComments by remember(postId) { mutableStateOf<List<CommentData>>(emptyList()) }
    var sending by remember(postId) { mutableStateOf(false) }
    var loading by remember(postId) { mutableStateOf(true) }
    var loadError by remember(postId) { mutableStateOf<String?>(null) }
    var sendError by remember(postId) { mutableStateOf<String?>(null) }
    var retry by remember(postId) { mutableIntStateOf(0) }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val currentUserName = remember {
        currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser?.email?.substringBefore("@")
            ?: "Pengguna"
    }

    // Listen to Firestore comments for this post
    DisposableEffect(postId, retry) {
        loading = true
        loadError = null
        val listener = FirebaseFirestore.getInstance()
            .collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                loading = false
                loadError = if (error != null) "Komentar gagal dimuat. Coba lagi." else null
                if (snapshot != null && error == null) {
                    val list = snapshot.documents.map { doc ->
                        val author = doc.getString("authorName") ?: "Pengguna"
                        // Prefer stable account identity; support older comments without authorId.
                        val avatarKey = doc.getString("authorId")?.takeIf { it.isNotBlank() }
                            ?: author
                        val avatarIndex = Math.floorMod(avatarKey.hashCode(), AvatarColors.size)
                        val text = doc.getString("text") ?: ""
                        val timestamp = doc.getTimestamp("createdAt")?.toDate()
                        val timeStr = if (timestamp != null) {
                            val diffMinutes = (System.currentTimeMillis() - timestamp.time) / 60000
                            when {
                                diffMinutes < 1 -> "Baru saja"
                                diffMinutes < 60 -> "$diffMinutes menit lalu"
                                else -> "${diffMinutes / 60} jam lalu"
                            }
                        } else "Baru saja"

                        CommentData(
                            id = doc.id,
                            authorId = doc.getString("authorId").orEmpty(),
                            replyToCommentId = doc.getString("replyToCommentId"),
                            replyToAuthorName = doc.getString("replyToAuthorName"),
                            name = author,
                            time = timeStr,
                            text = text,
                            avatarColor = AvatarColors[avatarIndex],
                            avatarText = author.take(1).uppercase(),
                        )
                    }
                    firestoreComments = list
                }
            }
        onDispose { listener.remove() }
    }

    var expandedThreads by rememberSaveable(postId) { mutableStateOf(emptyList<String>()) }
    val threads = remember(firestoreComments) { groupCommentThreads(firestoreComments) }
    val rootByComment = remember(threads) {
        buildMap {
            threads.forEach { (root, replies) ->
                put(root.id, root.id)
                replies.forEach { put(it.id, root.id) }
            }
        }
    }
    val replyCounts = remember(threads) { threads.associate { it.first.id to it.second.size } }
    val displayComments = remember(threads, expandedThreads) {
        threads.flatMap { (root, replies) ->
            val expanded = root.id in expandedThreads
            val visible = if (expanded) listOf(root) + replies else listOf(root)
            visible.mapIndexed { index, comment ->
                Triple(comment, index > 0, root.id.takeIf { replies.isNotEmpty() && index == visible.lastIndex })
            }
        }
    }

    val handleSendComment: (String) -> Unit = send@{ commentText ->
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            sendError = "Silakan masuk kembali untuk mengirim komentar."
            return@send
        }
        if (commentText.length > 2000) {
            sendError = "Komentar maksimal 2000 karakter."
            return@send
        }
        if (commentText.isNotBlank() && !sending) {
            val db = FirebaseFirestore.getInstance()
            sending = true
            sendError = null
            val commentMap = hashMapOf<String, Any>(
                "authorId" to user.uid,
                "authorName" to currentUserName.take(100),
                "text" to commentText,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            replyTarget?.let { target ->
                commentMap["replyToCommentId"] = target.id
                commentMap["replyToAuthorId"] = target.authorId
                commentMap["replyToAuthorName"] = target.name
            }
            val postRef = db.collection("posts").document(postId)
            val commentRef = postRef.collection("comments").document()
            db.batch()
                .set(commentRef, commentMap)
                .update(postRef, mapOf(
                    "comment" to FieldValue.increment(1),
                    "lastCommentId" to commentRef.id,
                ))
                .commit()
                .addOnSuccessListener {
                    newCommentText = ""
                    replyTarget = null
                    sending = false
                }
                .addOnFailureListener {
                    sending = false
                    sendError = "Komentar gagal dikirim. Coba lagi."
                }
        }
    }

    val visibilityState = remember {
        MutableTransitionState(false).apply { targetState = true }
    }

    AnimatedVisibility(
        visibleState = visibilityState,
        enter = fadeIn(tween(250)),
        exit = fadeOut(tween(200)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                ),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier.animateEnterExit(
                    enter = slideInVertically(
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                        initialOffsetY = { fullHeight -> fullHeight },
                    ) + fadeIn(tween(200)),
                    exit = slideOutVertically(
                        animationSpec = tween(280, easing = FastOutSlowInEasing),
                        targetOffsetY = { fullHeight -> fullHeight },
                    ) + fadeOut(tween(150)),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight(0.88f)
                        .padding(horizontal = 12.dp)
                        .padding(bottom = 12.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Color(0xFF1B3B5F))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = {}, // Prevent clicking card from dismissing overlay
                        ),
                ) {
                    // Header Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(24.dp),
                        ) {
                            Image(
                                painter = painterResource(R.drawable.login_back),
                                modifier = Modifier.size(20.dp),
                                contentDescription = "Tutup Komentar",
                            )
                        }
                        Text(
                            text = "Komentar",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                        )
                    }

                    // Comment List
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        when {
                            loading -> item { Text("Memuat komentar...", color = Color.White) }
                            loadError != null -> item {
                                androidx.compose.material3.TextButton(onClick = { retry++ }) {
                                    Text("$loadError Ketuk untuk mencoba lagi.", color = Color.White)
                                }
                            }
                            displayComments.isEmpty() -> item {
                                Text("Belum ada komentar. Jadilah yang pertama!", color = Color.White)
                            }
                        }
                        itemsIndexed(displayComments, key = { _, item -> item.first.id }) { _, row ->
                            val comment = row.first
                            CommentItem(
                                comment = comment,
                                isReply = row.second,
                                postId = postId,
                                canReply = !sending,
                                onReply = {
                                    replyTarget = comment
                                    val rootId = rootByComment[comment.id] ?: comment.id
                                    expandedThreads = (expandedThreads + rootId).distinct()
                                    inputFocus.requestFocus()
                                    keyboard?.show()
                                },
                                onShare = {
                                    val mention = comment.replyToAuthorName?.let { "@$it " }.orEmpty()
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_TEXT, "${comment.name}: $mention${comment.text}\n\nDibagikan dari OlahragaKuy")
                                    }
                                    try {
                                        context.startActivity(Intent.createChooser(shareIntent, "Bagikan komentar"))
                                    } catch (_: ActivityNotFoundException) {
                                        sendError = "Tidak ada aplikasi untuk membagikan komentar."
                                    }
                                },
                            )
                            row.third?.let { rootId ->
                                TextButton(
                                    modifier = Modifier.padding(start = 48.dp),
                                    onClick = {
                                        expandedThreads = if (rootId in expandedThreads)
                                            expandedThreads - rootId else expandedThreads + rootId
                                    },
                                ) {
                                    Box(Modifier.width(24.dp).height(1.dp).background(Color(0xFF45484D)))
                                    Spacer(Modifier.width(12.dp))
                                    Text(
                                        if (rootId in expandedThreads) "Sembunyikan balasan"
                                        else "Lihat ${replyCounts[rootId]} balasan",
                                        color = Color(0xFFA3A8B1),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                }
                            }
                        }
                    }

                    sendError?.let { Text(it, color = Color.White, modifier = Modifier.padding(16.dp)) }
                    if (sending) {
                        Text("Mengirim komentar...", color = Color.White, modifier = Modifier.padding(horizontal = 16.dp))
                    }

                    replyTarget?.let { target ->
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Membalas @${target.name}", color = Color(0xFFFFA183), modifier = Modifier.weight(1f))
                            TextButton(enabled = !sending, onClick = { replyTarget = null }) {
                                Text("Batal", color = Color.White)
                            }
                        }
                    }

                    // Bottom Input Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                            .background(Color(0xFF0F2236))
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        BasicTextField(
                            value = newCommentText,
                            enabled = !sending,
                            onValueChange = { newCommentText = it },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(inputFocus)
                                .height(44.dp)
                                .border(1.dp, Color(0xFFFF6B00), RoundedCornerShape(16.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            textStyle = TextStyle(color = Color.White, fontSize = 12.sp),
                            singleLine = true,
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (newCommentText.isEmpty()) {
                                        Text(
                                            "Apa yang anda pikirkan",
                                            color = Color.White.copy(alpha = 0.6f),
                                            fontSize = 12.sp,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )

                        IconButton(
                            onClick = {
                                if (newCommentText.isNotBlank()) {
                                    handleSendComment(newCommentText.trim())
                                }
                            },
                            enabled = !sending && newCommentText.isNotBlank(),
                            modifier = Modifier.size(36.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_send),
                                contentDescription = "Kirim Komentar",
                                tint = Color.Unspecified,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: CommentData,
    isReply: Boolean,
    postId: String,
    canReply: Boolean,
    onReply: () -> Unit,
    onShare: () -> Unit,
) {
    val userId = FirebaseAuth.getInstance().currentUser?.uid
    var liked by remember(postId, comment.id, userId) { mutableStateOf(false) }
    var likeCount by remember(postId, comment.id) { mutableIntStateOf(0) }
    var likesReady by remember(postId, comment.id, userId) { mutableStateOf(false) }
    var liking by remember(postId, comment.id, userId) { mutableStateOf(false) }
    var likeError by remember(postId, comment.id) { mutableStateOf<String?>(null) }
    var retryLikes by remember(postId, comment.id) { mutableIntStateOf(0) }
    val likesRef = remember(postId, comment.id) {
        FirebaseFirestore.getInstance().collection("posts").document(postId)
            .collection("comments").document(comment.id).collection("likes")
    }
    DisposableEffect(likesRef, userId, retryLikes) {
        likesReady = false
        likeError = null
        val listener = likesRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                likesReady = false
                likeError = "Suka gagal dimuat. Ketuk untuk mencoba lagi."
            } else if (snapshot != null) {
                likeCount = snapshot.size()
                liked = snapshot.documents.any { it.id == userId }
                likesReady = true
            }
        }
        onDispose { listener.remove() }
    }
    val toggleLike: () -> Unit = {
        if (userId != null && likesReady && !liking) {
            liking = true
            likeError = null
            val task = if (liked) likesRef.document(userId).delete()
                else likesRef.document(userId).set(mapOf("createdAt" to FieldValue.serverTimestamp()))
            task.addOnSuccessListener { liking = false }
                .addOnFailureListener {
                    liking = false
                    likeError = "Gagal mengubah suka. Ketuk untuk mencoba lagi."
                }
        }
    }
    CommentContent(
        comment = comment,
        isReply = isReply,
        liked = liked,
        likeCount = likeCount,
        likeEnabled = userId != null && likesReady && !liking,
        liking = liking,
        canReply = canReply,
        onLike = toggleLike,
        onReply = onReply,
        onShare = onShare,
        likeError = likeError,
        onRetryLikes = { retryLikes++ },
    )
}

@Composable
private fun CommentContent(
    comment: CommentData,
    isReply: Boolean = false,
    liked: Boolean = false,
    likeCount: Int = 0,
    likeEnabled: Boolean = true,
    liking: Boolean = false,
    canReply: Boolean = true,
    onLike: () -> Unit = {},
    onReply: () -> Unit = {},
    onShare: () -> Unit = {},
    likeError: String? = null,
    onRetryLikes: () -> Unit = {},
) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .padding(start = if (isReply) 48.dp else 0.dp, top = 10.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(if (isReply) 30.dp else 38.dp)
                .clip(CircleShape).background(comment.avatarColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(comment.avatarText, color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Column(Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                Text(
                    comment.name, color = Color(0xFFF2F2F2),
                    fontWeight = FontWeight.SemiBold, fontSize = 13.sp,
                    maxLines = 1, overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                Text(comment.time, color = Color(0xFFE87A44), fontSize = 11.sp)
            }
            Spacer(Modifier.height(4.dp))
            Box(
                Modifier.fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 0.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp))
                    .background(Color(0xFF4888C8))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = buildAnnotatedString {
                        comment.replyToAuthorName?.let {
                            withStyle(SpanStyle(color = Color(0xFFD3DEFF), fontWeight = FontWeight.SemiBold)) {
                                append("@$it ")
                            }
                        }
                        append(comment.text)
                    },
                    color = Color.White, fontSize = 13.sp, lineHeight = 18.sp,
                )
            }
            Row {
                TextButton(
                    enabled = canReply && comment.authorId.isNotBlank(),
                    onClick = onReply, contentPadding = PaddingValues(end = 12.dp),
                ) { Text("Balas", color = Color(0xFFA3A8B1), fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                TextButton(onClick = onShare, contentPadding = PaddingValues(horizontal = 4.dp)) {
                    Text("Bagikan", color = Color(0xFFA3A8B1), fontSize = 12.sp)
                }
            }
            likeError?.let {
                TextButton(onClick = onRetryLikes) { Text(it, color = Color(0xFFFFA183), fontSize = 11.sp) }
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(enabled = likeEnabled, onClick = onLike, modifier = Modifier.size(48.dp)) {
                androidx.compose.foundation.Canvas(Modifier.size(20.dp).semantics {
                    contentDescription = if (liked) "Batal suka" else "Suka komentar"
                }) {
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(size.width * .5f, size.height * .88f)
                        cubicTo(-size.width * .2f, size.height * .4f, size.width * .05f, -size.height * .15f, size.width * .5f, size.height * .22f)
                        cubicTo(size.width * .95f, -size.height * .15f, size.width * 1.2f, size.height * .4f, size.width * .5f, size.height * .88f)
                        close()
                    }
                    drawPath(path, if (liked) Color(0xFFFF647C) else Color(0xFFA3A8B1),
                        style = if (liked) androidx.compose.ui.graphics.drawscope.Fill
                        else androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()))
                }
            }
            if (liking || likeCount > 0) Text(
                if (liking) "…" else likeCount.toString(),
                color = Color(0xFFA3A8B1), fontSize = 11.sp,
            )
        }
    }
}

// Resolve reply-to-reply chains into a single visual thread without changing stored targets.
internal fun groupCommentThreads(comments: List<CommentData>): List<Pair<CommentData, List<CommentData>>> {
    val byId = comments.associateBy { it.id }
    fun rootId(comment: CommentData): String {
        var current = comment
        val visited = mutableSetOf<String>()
        while (visited.add(current.id)) {
            val parent = byId[current.replyToCommentId] ?: return current.id
            current = parent
        }
        return visited.minOrNull() ?: comment.id
    }
    return comments.groupBy { rootId(it) }.map { (rootId, members) ->
        val root = byId.getValue(rootId)
        root to members.filter { it.id != rootId }
    }
}

@androidx.compose.ui.tooling.preview.Preview(showBackground = true, backgroundColor = 0xFF1B3B5F, widthDp = 393)
@Composable
private fun CommentThreadPreview() {
    Column(Modifier.background(Color(0xFF1B3B5F)).padding(16.dp)) {
        CommentContent(CommentData(id = "1", authorId = "user", name = "rarokkaa", time = "1 hari", text = "Aku yang punya barang, beli yh guys", avatarText = "R"))
        CommentContent(CommentData(id = "2", authorId = "user", replyToAuthorName = "rarokkaa", name = "rarokkaa", time = "1 hari", text = "free tas laptop dan mouse bluetooth ya", avatarText = "R"), isReply = true)
        Text("—  Sembunyikan balasan", color = Color(0xFFA3A8B1), fontSize = 12.sp, modifier = Modifier.padding(start = 48.dp, top = 4.dp, bottom = 12.dp))
    }
}
