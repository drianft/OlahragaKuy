package com.example.olahragakuy.pages.posts

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
    val name: String,
    val time: String,
    val text: String,
    val avatarColor: Color = Color(0xFF6587A4),
    val avatarText: String = "",
)

private val SampleComments = listOf(
    CommentData(
        id = "sample1",
        name = "BibiOlahraga",
        time = "2 jam lalu",
        text = "jogging itu benar benar mengubah hidupku menjadi lebih baik",
        avatarColor = Color(0xFFFFD54F),
        avatarText = "👩",
    ),
    CommentData(
        id = "sample2",
        name = "Monster basket",
        time = "3 jam lalu",
        text = "Jogging itu harus di barengi dengan banyak minum air putih",
        avatarColor = Color(0xFF81C784),
        avatarText = "👾",
    ),
    CommentData(
        id = "sample3",
        name = "Kucing padel",
        time = "3 jam lalu",
        text = "Lebih seru bermain padel daripada jogging",
        avatarColor = Color(0xFF80CBC4),
        avatarText = "🐱",
    ),
    CommentData(
        id = "sample4",
        name = "KenanganIndah",
        time = "4 jam lalu",
        text = "Ketika kalian jogging jangan lupa untuk abadikan moment saat kalian bersama yaa , setiap moment itu sangat berharga",
        avatarColor = Color(0xFF64B5F6),
        avatarText = "📷",
    ),
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

    var newCommentText by remember { mutableStateOf("") }
    var firestoreComments by remember { mutableStateOf<List<CommentData>>(emptyList()) }
    val currentUser = remember { FirebaseAuth.getInstance().currentUser }
    val currentUserName = remember {
        currentUser?.displayName?.takeIf { it.isNotBlank() }
            ?: currentUser?.email?.substringBefore("@")
            ?: "Pengguna"
    }

    // Listen to Firestore comments for this post
    DisposableEffect(postId) {
        val listener = FirebaseFirestore.getInstance()
            .collection("posts")
            .document(postId)
            .collection("comments")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    val list = snapshot.documents.mapIndexed { index, doc ->
                        val author = doc.getString("authorName") ?: "Pengguna"
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
                            name = author,
                            time = timeStr,
                            text = text,
                            avatarColor = AvatarColors[index % AvatarColors.size],
                            avatarText = author.take(1).uppercase(),
                        )
                    }
                    firestoreComments = list
                }
            }
        onDispose { listener.remove() }
    }

    val displayComments = remember(firestoreComments) {
        if (firestoreComments.isNotEmpty()) firestoreComments else SampleComments
    }

    val handleSendComment: (String) -> Unit = { commentText ->
        if (commentText.isNotBlank()) {
            val db = FirebaseFirestore.getInstance()
            val commentMap = hashMapOf<String, Any>(
                "authorName" to currentUserName,
                "text" to commentText,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            db.collection("posts").document(postId).collection("comments").add(commentMap)
            db.collection("posts").document(postId).update("comment", FieldValue.increment(1))
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
                        itemsIndexed(displayComments, key = { _, item -> item.id }) { _, comment ->
                            CommentItem(comment = comment)
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
                            onValueChange = { newCommentText = it },
                            modifier = Modifier
                                .weight(1f)
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
                                    newCommentText = ""
                                }
                            },
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
private fun CommentItem(comment: CommentData) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(comment.avatarColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = comment.avatarText,
                color = Color.Black,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = comment.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
                Text(
                    text = comment.time,
                    color = Color(0xFFE87A44),
                    fontSize = 11.sp,
                )
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF4274A4))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                Text(
                    text = comment.text,
                    color = Color.White,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                )
            }
        }
    }
}
