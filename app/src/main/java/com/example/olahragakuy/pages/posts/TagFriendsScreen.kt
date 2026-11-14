package com.example.olahragakuy.pages.posts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.olahragakuy.pages.HomeBlue
import com.example.olahragakuy.pages.HomeNavy
import com.example.olahragakuy.pages.HomeOrange
import com.example.olahragakuy.pages.HomeText
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

/** Akun yang diikuti pengguna dan dapat ditandai pada postingan. */
data class FollowingFriend(
    val uid: String,
    val displayName: String,
    val photoUrl: String? = null,
)

/** Membaca dokumen `users/{uid}/following/{friendId}` secara toleran terhadap nama field. */
fun followingFriendFrom(documentId: String, data: Map<String, Any?>): FollowingFriend? {
    val uid = (data["uid"] as? String)?.takeIf { it.isNotBlank() } ?: documentId.takeIf { it.isNotBlank() }
    uid ?: return null
    val name = listOf("displayName", "name", "username")
        .firstNotNullOfOrNull { key -> (data[key] as? String)?.trim()?.takeIf { it.isNotEmpty() } }
        ?: "Teman"
    val photo = listOf("photoUrl", "avatarUrl")
        .firstNotNullOfOrNull { key -> (data[key] as? String)?.takeIf { it.isNotBlank() } }
    return FollowingFriend(uid = uid, displayName = name, photoUrl = photo)
}

/** Mengurutkan teman berdasarkan nama dan membuang uid ganda. */
fun sortFollowingFriends(friends: List<FollowingFriend>): List<FollowingFriend> =
    friends.distinctBy { it.uid }.sortedBy { it.displayName.lowercase() }

/** Menambah atau menghapus teman dari daftar tanda. */
fun toggleTaggedFriend(selected: List<FollowingFriend>, friend: FollowingFriend): List<FollowingFriend> =
    if (selected.any { it.uid == friend.uid }) selected.filterNot { it.uid == friend.uid }
    else selected + friend

/** Field Firestore untuk postingan; kosong bila tidak ada teman yang ditandai. */
fun taggedFriendsFields(selected: List<FollowingFriend>): Map<String, Any> {
    if (selected.isEmpty()) return emptyMap()
    return mapOf(
        "taggedFriendIds" to selected.map { it.uid },
        "taggedFriends" to selected.map { mapOf("uid" to it.uid, "displayName" to it.displayName) },
    )
}

/** Membaca nama teman yang ditandai dari dokumen postingan (kompatibel dengan post lama). */
fun taggedFriendNamesFrom(raw: Any?): List<String> =
    (raw as? List<*>).orEmpty().mapNotNull { item ->
        ((item as? Map<*, *>)?.get("displayName") as? String)?.trim()?.takeIf { it.isNotEmpty() }
    }

val TaggedFriendsSaver: Saver<List<FollowingFriend>, Any> = listSaver(
    save = { friends -> friends.flatMap { listOf(it.uid, it.displayName, it.photoUrl.orEmpty()) } },
    restore = { values ->
        values.chunked(3).mapNotNull { chunk ->
            if (chunk.size < 3) null
            else FollowingFriend(chunk[0], chunk[1], chunk[2].takeIf { it.isNotEmpty() })
        }
    },
)

private val SampleFriends = listOf(
    FollowingFriend("sample-1", "Gurita bola"),
    FollowingFriend("sample-2", "Monster basket"),
    FollowingFriend("sample-3", "Kucing padel"),
    FollowingFriend("sample-4", "Kenanganindah"),
)

private val AvatarColors = listOf(
    Color(0xFF5B7CFA), Color(0xFF4FC3A1), Color(0xFFF2B84B), Color(0xFF7FA7E8),
)

/**
 * Layar "Tandai Teman": menampilkan akun yang diikuti pengguna dari
 * Firestore `users/{uid}/following`. Tombol kembali menyimpan pilihan.
 */
@Composable
fun TagFriendsScreen(
    selected: List<FollowingFriend>,
    onSelectedChange: (List<FollowingFriend>) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inPreview = LocalInspectionMode.current
    var friends by remember { mutableStateOf(if (inPreview) SampleFriends else emptyList()) }
    var loading by remember { mutableStateOf(!inPreview) }
    var error by remember { mutableStateOf<String?>(null) }
    var reloadKey by remember { mutableIntStateOf(0) }

    if (!inPreview) {
        LaunchedEffect(reloadKey) {
            val uid = FirebaseAuth.getInstance().currentUser?.uid
            if (uid == null) {
                friends = emptyList()
                loading = false
                return@LaunchedEffect
            }
            loading = true
            error = null
            FirebaseFirestore.getInstance()
                .collection("users").document(uid).collection("following")
                .get()
                .addOnSuccessListener { snapshot ->
                    friends = sortFollowingFriends(
                        snapshot.documents.mapNotNull { followingFriendFrom(it.id, it.data.orEmpty()) }
                    )
                    loading = false
                }
                .addOnFailureListener {
                    error = "Gagal memuat daftar teman."
                    loading = false
                }
        }
    }

    BackHandler(onBack = onBack)

    TagFriendsContent(
        friends = friends,
        selectedIds = selected.map { it.uid }.toSet(),
        loading = loading,
        error = error,
        onToggle = { onSelectedChange(toggleTaggedFriend(selected, it)) },
        onBack = onBack,
        onRetry = { reloadKey++ },
        modifier = modifier,
    )
}

@Composable
private fun TagFriendsContent(
    friends: List<FollowingFriend>,
    selectedIds: Set<String>,
    loading: Boolean,
    error: String?,
    onToggle: (FollowingFriend) -> Unit,
    onBack: () -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize(), color = HomeBlue, contentColor = HomeText) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onBack) {
                    Text("↶", color = HomeText, fontSize = 20.sp)
                    Text(
                        "  Tandai Teman",
                        color = HomeText,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.weight(1f))
                if (selectedIds.isNotEmpty()) {
                    Text(
                        "${selectedIds.size} dipilih",
                        color = HomeOrange,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
            }
            when {
                loading -> CenterMessage {
                    CircularProgressIndicator(color = HomeOrange)
                }
                error != null -> CenterMessage {
                    Text(error, color = HomeText, textAlign = TextAlign.Center)
                    TextButton(onClick = onRetry) { Text("Coba lagi", color = HomeOrange) }
                }
                friends.isEmpty() -> CenterMessage {
                    Text(
                        "Kamu belum mengikuti siapa pun.\nIkuti teman agar bisa menandai mereka.",
                        color = HomeText,
                        textAlign = TextAlign.Center,
                        fontSize = 13.sp,
                    )
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    itemsIndexed(friends, key = { _, friend -> friend.uid }) { index, friend ->
                        FriendRow(
                            friend = friend,
                            index = index,
                            checked = friend.uid in selectedIds,
                            onToggle = { onToggle(friend) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CenterMessage(content: @Composable () -> Unit) {
    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) { content() }
}

@Composable
private fun FriendRow(
    friend: FollowingFriend,
    index: Int,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FriendAvatar(friend, AvatarColors[index.mod(AvatarColors.size)])
        Text(
            friend.displayName,
            modifier = Modifier.weight(1f),
            color = HomeText,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        TagCheckbox(checked)
    }
}

@Composable
private fun FriendAvatar(friend: FollowingFriend, fallbackColor: Color) {
    Box(
        Modifier.size(36.dp).clip(CircleShape).background(fallbackColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            friend.displayName.take(1).uppercase(),
            color = HomeNavy,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
        )
        if (friend.photoUrl != null) {
            AsyncImage(
                model = friend.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TagCheckbox(checked: Boolean) {
    Surface(
        modifier = Modifier.size(20.dp),
        shape = RoundedCornerShape(6.dp),
        color = if (checked) HomeOrange else Color.White,
        border = BorderStroke(1.5.dp, HomeOrange),
    ) {
        if (checked) {
            Box(contentAlignment = Alignment.Center) {
                Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun TagFriendsScreenPreview() {
    OlahragaKuyTheme {
        TagFriendsScreen(
            selected = listOf(SampleFriends[1]),
            onSelectedChange = {},
            onBack = {},
        )
    }
}
