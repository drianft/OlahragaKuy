package com.example.olahragakuy.pages.posts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.olahragakuy.R
import com.example.olahragakuy.ui.components.KuySuccessPopupOverlay
import com.example.olahragakuy.ui.components.KuyUserAvatar
import com.example.olahragakuy.ui.components.PostActionCard
import com.example.olahragakuy.ui.components.TaggedFriendsRow
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

const val MAX_POST_CAPTION_LENGTH = 2000

private val EditPostSuccessIllustration = R.drawable.amico_home

/** Data postingan yang sedang diedit pada halaman [EditPostScreen]. */
data class EditPostTarget(
    val postId: String,
    val authorId: String = "",
    val authorName: String,
    val caption: String,
    val imageUrl: String? = null,
    val category: String = "",
    val taggedFriends: List<FollowingFriend> = emptyList(),
    val taggedNames: List<String> = taggedFriends.map { it.displayName },
)

/** Bridge ringan untuk mengirim target postingan ke rute navigasi `edit_post`. */
object EditPostBridge {
    @Volatile
    private var pendingTarget: EditPostTarget? = null

    fun setPendingTarget(target: EditPostTarget?) {
        pendingTarget = target
    }

    fun peekTarget(): EditPostTarget? = pendingTarget

    fun clear() {
        pendingTarget = null
    }
}

/** Memeriksa apakah caption hasil edit valid sesuai batas Firestore Rules (1..2000 karakter). */
fun isValidEditedCaption(caption: String): Boolean =
    caption.trim().length in 1..MAX_POST_CAPTION_LENGTH

/** Memeriksa apakah caption berubah dibandingkan caption awal setelah trim. */
fun hasCaptionChanged(originalCaption: String, editedCaption: String): Boolean =
    editedCaption.trim() != originalCaption.trim()

/** Memeriksa apakah daftar teman yang ditandai berubah dibandingkan kondisi awal. */
fun hasTaggedFriendsChanged(
    original: List<FollowingFriend>,
    edited: List<FollowingFriend>,
): Boolean = original.map { it.uid }.toSet() != edited.map { it.uid }.toSet()

/** Mengembalikan teman yang baru ditandai pada sesi edit ini (untuk dikirimkan notifikasi tag). */
fun newlyAddedTaggedFriends(
    original: List<FollowingFriend>,
    edited: List<FollowingFriend>,
): List<FollowingFriend> {
    val existingIds = original.map { it.uid }.toSet()
    return edited.distinctBy { it.uid }.filter { it.uid.isNotBlank() && it.uid !in existingIds }
}

/** Menentukan apakah tombol Simpan pada halaman edit dapat ditekan. */
fun canSaveEditedPost(
    originalCaption: String,
    editedCaption: String,
    originalTaggedFriends: List<FollowingFriend> = emptyList(),
    editedTaggedFriends: List<FollowingFriend> = originalTaggedFriends,
    isSaving: Boolean,
): Boolean = !isSaving &&
    isValidEditedCaption(editedCaption) &&
    (
        hasCaptionChanged(originalCaption, editedCaption) ||
            hasTaggedFriendsChanged(originalTaggedFriends, editedTaggedFriends)
        )

/** Kompatibilitas pemeriksaan simpan khusus perubahan caption. */
fun canSaveEditedCaption(
    originalCaption: String,
    editedCaption: String,
    isSaving: Boolean,
): Boolean = canSaveEditedPost(
    originalCaption = originalCaption,
    editedCaption = editedCaption,
    isSaving = isSaving,
)

/**
 * Membentuk payload update Firestore untuk edit postingan.
 * Field `taggedFriendIds` dan `taggedFriends` hanya disertakan bila daftar teman berubah,
 * sedangkan `updatedAt` ditambahkan saat dikirim ke Firestore.
 */
fun buildEditPostUpdatePayload(
    caption: String,
    category: String,
    authorName: String,
    taggedFriends: List<FollowingFriend> = emptyList(),
    includeTaggedFriends: Boolean = false,
    fallbackTaggedNames: List<String> = emptyList(),
    hasPhoto: Boolean = false,
): Map<String, Any>? {
    val trimmed = caption.trim()
    if (trimmed.length !in 1..MAX_POST_CAPTION_LENGTH) return null
    val normalizedFriends = taggedFriends.distinctBy { it.uid }.take(50)
    val friendsForKeywords = if (normalizedFriends.isNotEmpty() || includeTaggedFriends) {
        normalizedFriends
    } else {
        fallbackTaggedNames.map { FollowingFriend(uid = "", displayName = it) }
    }
    val keywords = buildSearchKeywords(
        caption = trimmed,
        category = category,
        authorName = authorName,
        taggedFriends = friendsForKeywords,
        hasPhoto = hasPhoto,
    ).take(100)
    val payload = linkedMapOf<String, Any>(
        "caption" to trimmed,
        "searchKeywords" to keywords,
    )
    if (includeTaggedFriends) {
        payload["taggedFriendIds"] = normalizedFriends.map { it.uid }
        payload["taggedFriends"] = normalizedFriends.map {
            mapOf("uid" to it.uid, "displayName" to it.displayName)
        }
    }
    return payload
}

/** Menyimpan perubahan caption dan tag teman pada `posts/{postId}` serta mengirim notifikasi ke teman baru. */
fun submitEditedPost(
    db: FirebaseFirestore,
    auth: FirebaseAuth,
    target: EditPostTarget,
    newCaption: String,
    editedTaggedFriends: List<FollowingFriend>,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
) {
    val cleanId = target.postId.trim()
    if (cleanId.isEmpty()) {
        onError("ID postingan tidak valid.")
        return
    }
    val trimmedCaption = newCaption.trim()
    val tagsChanged = hasTaggedFriendsChanged(target.taggedFriends, editedTaggedFriends)
    val basePayload = buildEditPostUpdatePayload(
        caption = trimmedCaption,
        category = target.category,
        authorName = target.authorName,
        taggedFriends = editedTaggedFriends,
        includeTaggedFriends = tagsChanged,
        fallbackTaggedNames = target.taggedNames,
        hasPhoto = !target.imageUrl.isNullOrBlank(),
    )
    if (basePayload == null) {
        onError("Caption harus berisi 1 sampai $MAX_POST_CAPTION_LENGTH karakter.")
        return
    }
    val postRef = db.collection("posts").document(cleanId)
    val payload = basePayload + ("updatedAt" to FieldValue.serverTimestamp())

    fun notifyNewTagsAndFinish() {
        val actorId = target.authorId.ifBlank { auth.currentUser?.uid.orEmpty() }
        if (actorId.isNotBlank() && tagsChanged) {
            newlyAddedTaggedFriends(target.taggedFriends, editedTaggedFriends).forEach { friend ->
                sendPostNotification(
                    db = db,
                    recipientUid = friend.uid,
                    actorId = actorId,
                    actorName = target.authorName,
                    postId = cleanId,
                    type = NotificationType.TAG,
                    deterministicId = "tag_${cleanId}_${friend.uid}",
                )
            }
        }
        onSuccess()
    }

    postRef.update(payload)
        .addOnSuccessListener { notifyNewTagsAndFinish() }
        .addOnFailureListener { err ->
            val isPermissionDenied = err.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
            if (isPermissionDenied && !tagsChanged) {
                // Fallback jika Firestore Rules di server hanya mengizinkan field caption.
                postRef.update(mapOf("caption" to trimmedCaption))
                    .addOnSuccessListener { onSuccess() }
                    .addOnFailureListener {
                        onError(
                            "Edit postingan ditolak oleh Firestore Rules. Publikasikan firestore.rules terbaru di Firebase Console.",
                        )
                    }
            } else {
                onError(
                    if (isPermissionDenied) {
                        "Simpan perubahan tag teman ditolak oleh Firestore Rules. Publikasikan firestore.rules terbaru di Firebase Console agar update taggedFriendIds & taggedFriends diizinkan."
                    } else {
                        err.localizedMessage ?: "Gagal memperbarui postingan."
                    },
                )
            }
        }
}

@Composable
fun EditPostScreen(
    target: EditPostTarget?,
    onBack: () -> Unit,
    onSaved: () -> Unit,
) {
    if (target == null) {
        LaunchedEffect(Unit) { onBack() }
        Box(Modifier.fillMaxSize().background(HomeNavy))
        return
    }

    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    var draft by rememberSaveable(target.postId) { mutableStateOf(target.caption) }
    var taggedFriends by rememberSaveable(target.postId, stateSaver = TaggedFriendsSaver) {
        mutableStateOf(target.taggedFriends)
    }
    var showTagFriends by rememberSaveable(target.postId) { mutableStateOf(false) }
    var isSaving by remember { mutableStateOf(false) }
    var notice by rememberSaveable(target.postId) { mutableStateOf<String?>(null) }
    var showSuccessPopup by rememberSaveable(target.postId) { mutableStateOf(false) }

    BackHandler(enabled = isSaving) { }

    Box(Modifier.fillMaxSize()) {
        EditPostContent(
            target = target,
            draft = draft,
            taggedFriends = taggedFriends,
            isSaving = isSaving,
            notice = notice,
            showSuccessPopup = showSuccessPopup,
            onDraftChange = {
                draft = it
                if (notice != null) notice = null
            },
            onOpenTagFriends = { showTagFriends = true },
            onRemoveTaggedFriend = { friend ->
                taggedFriends = toggleTaggedFriend(taggedFriends, friend)
                if (notice != null) notice = null
            },
            onBack = onBack,
            onSaveClick = {
                isSaving = true
                notice = null
                submitEditedPost(
                    db = db,
                    auth = auth,
                    target = target,
                    newCaption = draft,
                    editedTaggedFriends = taggedFriends,
                    onSuccess = {
                        isSaving = false
                        showSuccessPopup = true
                    },
                    onError = { message ->
                        isSaving = false
                        notice = message
                    },
                )
            },
            onContinueAfterSuccess = {
                showSuccessPopup = false
                onSaved()
            },
        )

        if (showTagFriends) {
            TagFriendsScreen(
                selected = taggedFriends,
                onSelectedChange = {
                    taggedFriends = it
                    if (notice != null) notice = null
                },
                onBack = { showTagFriends = false },
            )
        }
    }
}

@Composable
internal fun EditPostContent(
    target: EditPostTarget,
    draft: String,
    taggedFriends: List<FollowingFriend>,
    isSaving: Boolean,
    notice: String?,
    showSuccessPopup: Boolean,
    onDraftChange: (String) -> Unit,
    onOpenTagFriends: () -> Unit,
    onRemoveTaggedFriend: (FollowingFriend) -> Unit,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
    onContinueAfterSuccess: () -> Unit,
) {
    val trimmedLength = draft.trim().length
    val isTooLong = trimmedLength > MAX_POST_CAPTION_LENGTH
    val canSave = canSaveEditedPost(
        originalCaption = target.caption,
        editedCaption = draft,
        originalTaggedFriends = target.taggedFriends,
        editedTaggedFriends = taggedFriends,
        isSaving = isSaving,
    )
    val categoryTags = remember(target.category) {
        target.category.split(",", ";")
            .map { it.trim() }
            .filter { it.isNotEmpty() }
    }

    Box(Modifier.fillMaxSize()) {
        Scaffold(
            containerColor = HomeNavy,
            contentColor = HomeText,
            topBar = {
                EditPostTopBar(
                    isSaving = isSaving,
                    canSave = canSave,
                    onBack = onBack,
                    onSaveClick = onSaveClick,
                )
            },
        ) { insets ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(insets)
                    .imePadding()
                    .verticalScroll(rememberScrollState()),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    notice?.let { message ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFF4A1C24),
                            contentColor = Color(0xFFFFD6D6),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(
                                text = message,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            )
                        }
                    }

                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        KuyUserAvatar(name = target.authorName)
                        Text(
                            text = target.authorName,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = HomeText,
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        OutlinedTextField(
                            value = draft,
                            onValueChange = onDraftChange,
                            enabled = !isSaving,
                            placeholder = {
                                Text("Apa yang anda pikirkan", fontSize = 11.sp)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(260.dp),
                            shape = RoundedCornerShape(14.dp),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(color = HomeText),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = if (isTooLong) Color(0xFFE53935) else HomeOrange,
                                unfocusedBorderColor = if (isTooLong) Color(0xFFE53935) else HomeOrange,
                                focusedTextColor = HomeText,
                                unfocusedTextColor = HomeText,
                                focusedPlaceholderColor = HomeText.copy(alpha = 0.75f),
                                unfocusedPlaceholderColor = HomeText.copy(alpha = 0.75f),
                                cursorColor = HomeOrange,
                            ),
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = when {
                                    trimmedLength == 0 -> "Caption tidak boleh kosong"
                                    isTooLong -> "Melebihi batas maksimal karakter"
                                    else -> ""
                                },
                                color = Color(0xFFFF8A80),
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Text(
                                text = "$trimmedLength/$MAX_POST_CAPTION_LENGTH",
                                color = if (isTooLong) Color(0xFFFF8A80) else HomeText.copy(alpha = 0.65f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }

                    if (!target.imageUrl.isNullOrBlank()) {
                        LockedPostPhotoSection(imageUrl = target.imageUrl)
                    }

                    PostActionCard(
                        label = "Tandai Teman",
                        photo = false,
                        enabled = !isSaving,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onOpenTagFriends,
                    )

                    if (taggedFriends.isNotEmpty()) {
                        TaggedFriendsRow(
                            friends = taggedFriends,
                            enabled = !isSaving,
                            onRemove = onRemoveTaggedFriend,
                        )
                    }

                    if (categoryTags.isNotEmpty()) {
                        ReadOnlyChipSection(
                            title = "Topik / Kata Kunci",
                            items = categoryTags,
                        )
                    }
                }
            }
        }

        KuySuccessPopupOverlay(
            visible = showSuccessPopup,
            illustrationRes = EditPostSuccessIllustration,
            subtitle = "Postingan anda berhasil diperbarui",
            onContinue = onContinueAfterSuccess,
        )
    }
}

@Composable
private fun EditPostTopBar(
    isSaving: Boolean,
    canSave: Boolean,
    onBack: () -> Unit,
    onSaveClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Button(
            onClick = onBack,
            enabled = !isSaving,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF488ACC),
                contentColor = HomeText,
            ),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
            shape = RoundedCornerShape(24.dp),
        ) {
            Text("↶", fontSize = 20.sp)
            Text(" Edit Postingan", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.weight(1f))
        Button(
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF54CC0C),
                contentColor = HomeText,
                disabledContainerColor = Color(0xFF54CC0C).copy(alpha = 0.45f),
                disabledContentColor = HomeText.copy(alpha = 0.65f),
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 0.dp),
            shape = RoundedCornerShape(24.dp),
            enabled = canSave,
            onClick = onSaveClick,
        ) {
            Text(if (isSaving) "Menyimpan..." else "Simpan")
        }
    }
}

@Composable
private fun LockedPostPhotoSection(
    imageUrl: String,
    modifier: Modifier = Modifier,
) {
    val isInspection = LocalInspectionMode.current
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = "Foto Postingan",
            color = HomeText,
            style = MaterialTheme.typography.titleSmall,
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(HomeBlue),
            contentAlignment = Alignment.Center,
        ) {
            if (isInspection) {
                Text(
                    text = "Preview foto postingan (tidak dapat diubah)",
                    color = HomeText.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = "Foto postingan (tidak dapat diubah)",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                color = HomeOrange,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                    },
                    error = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = "Foto tidak dapat dimuat",
                                color = HomeText.copy(alpha = 0.7f),
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun ReadOnlyChipSection(
    title: String,
    items: List<String>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .alpha(0.75f),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(title, color = HomeText, style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items.forEach { label ->
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = HomeBlue,
                    contentColor = HomeText,
                ) {
                    Text(
                        text = label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Preview(name = "Edit Postingan Dengan Foto", showBackground = true, widthDp = 360, heightDp = 760)
@Composable
private fun EditPostWithPhotoPreview() {
    val initialFriends = listOf(FollowingFriend("u1", "Raka"))
    OlahragaKuyTheme {
        EditPostContent(
            target = EditPostTarget(
                postId = "post-1",
                authorId = "me",
                authorName = "BibiOlahraga",
                caption = "Lari pagi 3 km santai bareng teman!",
                imageUrl = "https://example.com/run.jpg",
                category = "Jogging, Lari Pagi",
                taggedFriends = initialFriends,
            ),
            draft = "Lari pagi 3 km santai bareng teman! Semangat sehat hari ini.",
            taggedFriends = initialFriends + FollowingFriend("u2", "Dina"),
            isSaving = false,
            notice = null,
            showSuccessPopup = false,
            onDraftChange = {},
            onOpenTagFriends = {},
            onRemoveTaggedFriend = {},
            onBack = {},
            onSaveClick = {},
            onContinueAfterSuccess = {},
        )
    }
}

@Preview(name = "Edit Postingan Tanpa Foto", showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun EditPostTextOnlyPreview() {
    OlahragaKuyTheme {
        EditPostContent(
            target = EditPostTarget(
                postId = "post-2",
                authorId = "me",
                authorName = "BibiOlahraga",
                caption = "Stretching 10 menit sebelum tidur.",
                imageUrl = null,
            ),
            draft = "Stretching 10 menit sebelum tidur.",
            taggedFriends = emptyList(),
            isSaving = false,
            notice = null,
            showSuccessPopup = false,
            onDraftChange = {},
            onOpenTagFriends = {},
            onRemoveTaggedFriend = {},
            onBack = {},
            onSaveClick = {},
            onContinueAfterSuccess = {},
        )
    }
}
