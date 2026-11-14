package com.example.olahragakuy.pages

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.platform.LocalInspectionMode
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import java.text.DateFormat
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import coil.compose.SubcomposeAsyncImage
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.R
import com.example.olahragakuy.pages.posts.CommentOverlay
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

val HomeNavy = Color(0xFF0A1A2C)
val HomeBlue = Color(0xFF203F60)
val HomeOrange = Color(0xFFFF6B00)
val HomeText = Color(0xFFE9EDF5)

@Composable
fun HomeScreen(onCreatePost: () -> Unit = {}) {
    var notice by rememberSaveable { mutableStateOf<String?>(null) }
    var activeCommentPostId by remember { mutableStateOf<String?>(null) }
    var posts by remember { mutableStateOf<List<DocumentSnapshot>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var feedError by remember { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    val isPreview = LocalInspectionMode.current


    DisposableEffect(isPreview, retry) {
        var listener: ListenerRegistration? = null
        if (isPreview) {
            loading = false
        } else {
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
                            posts = snapshot?.documents.orEmpty()
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

    val currentUser = remember {
        try { FirebaseAuth.getInstance().currentUser }
        catch (_: Exception) { null }
    }
    val userDisplayName = currentUser?.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser?.email?.substringBefore("@")
        ?: "Teman"

    Scaffold(
        containerColor = HomeNavy,
        contentColor = HomeText,
        topBar = {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Image(
                    painterResource(R.drawable.login_wordmark), "OlahragaKUY",
                    Modifier.weight(1f).height(28.dp),
                    alignment = Alignment.CenterStart,
                )
                HomeLogoButton("Cari") { notice = "Fitur pencarian belum tersedia." }
                HomeLogoButton("Notifikasi") { notice = "Belum ada notifikasi." }
                HomeLogoButton("Chat") { notice = "Fitur chat belum tersedia." }
            }
        },
        bottomBar = {
            Surface(color = HomeNavy, shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)) {
                Row(
                    Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    listOf("Komunitas", "Lokasi", "Latihan", "Profil").forEachIndexed { index, label ->
                        TextButton(
                            onClick = {
                                if (index == 3 && currentUser != null) {
                                    notice = "Masuk sebagai ${currentUser.email ?: userDisplayName}"
                                } else if (index != 0) {
                                    notice = "Halaman $label belum tersedia."
                                }
                            },
                            modifier = Modifier.sizeIn(minWidth = 64.dp, minHeight = 64.dp)
                                .background(if (index == 0) Color(0xFFBB4D00) else Color.Transparent, CircleShape),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = if (index == 0) HomeText else HomeOrange,
                            ),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Isi logo", fontSize = 10.sp)
                                Text(label, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
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
                Row(
                    modifier = Modifier.fillMaxWidth().background(HomeBlue)
                        .clickable(role = Role.Button, onClickLabel = "Buat postingan", onClick = onCreatePost)
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    HomePlaceholder("Isi avatar\ndi sini", Modifier.size(40.dp), CircleShape)
                    Text(
                        "Apa yang anda pikirkan, $userDisplayName?",
                        modifier = Modifier.weight(1f)
                            .border(1.dp, HomeOrange, RoundedCornerShape(16.dp))
                            .padding(16.dp),
                        color = HomeText.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                    )
                    Text("Foto +", color = HomeOrange, fontSize = 12.sp)
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
                else -> items(posts, key = { "post:${it.id}" }) { post ->
                    val createdAt = (post.get("createdAt") as? com.google.firebase.Timestamp)?.toDate()
                    HomePost(
                        name = (post.get("authorName") as? String)?.takeIf { it.isNotBlank() } ?: "Pengguna",
                        time = createdAt?.let {
                            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(it)
                        }.orEmpty(),
                        caption = post.get("caption") as? String ?: "(Tanpa caption)",
                        imageUrl = (post.get("imageUrl") as? String)?.takeIf { it.isNotBlank() },
                        initialLikes = (post.get("likes") as? Number)?.toLong()?.coerceAtLeast(0) ?: 0L,
                        initialComment = (post.get("comment") as? Number)?.toLong()?.coerceAtLeast(0) ?: 0L,
                        initialShare = (post.get("Share") as? Number)?.toLong()?.coerceAtLeast(0) ?: 0L,
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




@Composable
private fun StreakBanner(onClick: () -> Unit) {
    Row(
        Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
            .fillMaxWidth().clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFA183), HomeOrange)))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Pertahankan Streak anda!",
                color = Color(0xFF713000), fontWeight = FontWeight.Bold, fontSize = 16.sp,
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("🔥", fontSize = 30.sp)
                Text("30", color = HomeText, fontSize = 36.sp, fontWeight = FontWeight.Bold)
            }
            OutlinedButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 0.dp),
                border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFF8C3C00)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFE9E9E9), contentColor = Color(0xFF8C3C00),
                ),
            ) { Text("Pergi") }
        }
        Image(
            painterResource(R.drawable.login_wordmark), "Ilustrasi olahraga bersama",
            Modifier.width(100.dp).height(120.dp),
        )
    }
}

@Composable
private fun HomePost(
    name: String,
    time: String,
    caption: String,
    imageUrl: String?,
    initialLikes: Long,
    initialComment: Long,
    initialShare: Long,
    onAction: (String) -> Unit,
    onCommentClick: () -> Unit = {},
) {
    Column(
        Modifier.fillMaxWidth().background(HomeBlue).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            HomePlaceholder("Isi avatar\ndi sini", Modifier.size(54.dp), CircleShape)
            Column(Modifier.weight(1f)) {
                Text(name, color = HomeText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(time, color = HomeOrange, fontSize = 12.sp)
            }
            TextButton(onClick = { onAction("Menu postingan belum tersedia.") }) {
                Text("⋮", color = HomeText, fontSize = 28.sp)
            }
        }
        Text(caption, color = HomeText, fontSize = 14.sp)
        if (imageUrl != null) {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = "Foto postingan oleh $name",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.28f)
                    .clip(RoundedCornerShape(24.dp)),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = HomeOrange)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Foto gagal dimuat", color = HomeText)
                    }
                },
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextButton(onClick = { onAction("Fitur suka belum tersedia.") }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_like),
                        contentDescription = "Suka",
                        tint = HomeText,
                        modifier = Modifier.size(20.dp),
                    )
                    Text("$initialLikes", color = HomeText, fontSize = 14.sp)
                }
            }
            TextButton(onClick = onCommentClick) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_comment),
                        contentDescription = "Komentar",
                        tint = HomeText,
                        modifier = Modifier.size(20.dp),
                    )
                    Text("$initialComment", color = HomeText, fontSize = 12.sp)
                }
            }
            TextButton(onClick = { onAction("Fitur bagikan belum tersedia.") }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_share),
                        contentDescription = "Bagikan",
                        tint = HomeText,
                        modifier = Modifier.size(20.dp),
                    )
                    Text("$initialShare", color = HomeText, fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun HomeLogoButton(label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        contentPadding = PaddingValues(2.dp),
        modifier = Modifier.width(48.dp).heightIn(min = 48.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Isi logo", color = Color(0xFF5094D5), fontSize = 9.sp)
            Text(label, color = Color(0xFF5094D5), fontSize = 9.sp)
        }
    }
}

@Composable
fun HomePlaceholder(
    label: String,
    modifier: Modifier = Modifier,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(12.dp),
) {
    Box(
        modifier.clip(shape).background(Color(0xFF315575))
            .border(1.dp, Color(0xFF6587A4), shape).padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = HomeText.copy(alpha = 0.8f), fontSize = 10.sp, textAlign = TextAlign.Center)
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeScreenPreview() {
    OlahragaKuyTheme { HomeScreen() }
}
