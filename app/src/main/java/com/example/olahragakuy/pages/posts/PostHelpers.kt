package com.example.olahragakuy.pages.posts

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import java.text.DateFormat

/** Pilihan topik cepat saat membuat atau mencari postingan. */
val DefaultPostCategories = listOf(
    "Makanan Sehat",
    "Foto Orang",
    "Olahraga Pagi",
    "Bulking",
    "Kardio",
)

/** Jenis notifikasi aktivitas postingan. */
enum class NotificationType(val code: String, val message: String) {
    LIKE("LIKE", "menyukai postingan anda"),
    COMMENT("COMMENT", "mengomentari postingan anda"),
    TAG("TAG", "menandai anda di sebuah postingan");

    companion object {
        fun fromCode(raw: String?): NotificationType =
            entries.firstOrNull { it.code.equals(raw, ignoreCase = true) } ?: LIKE
    }
}

/** Representasi UI satu item notifikasi. */
data class NotificationUiModel(
    val id: String,
    val actorId: String,
    val actorName: String,
    val type: NotificationType,
    val postId: String,
    val createdAtMillis: Long = System.currentTimeMillis(),
)

/** Representasi UI satu postingan di beranda, pencarian, dan detail notifikasi. */
data class PostUiModel(
    val id: String,
    val authorId: String = "",
    val authorName: String,
    val time: String,
    val caption: String,
    val imageUrl: String? = null,
    val category: String = "",
    val searchKeywords: List<String> = emptyList(),
    val taggedNames: List<String> = emptyList(),
    val likesCount: Long = 0L,
    val likedByIds: List<String> = emptyList(),
    val likedByNames: List<String> = emptyList(),
    val commentsCount: Long = 0L,
    val sharesCount: Long = 0L,
)

/** Membentuk daftar kata kunci pencarian dari caption, kategori/topik, penulis, teman, dan status foto. */
fun buildSearchKeywords(
    caption: String,
    category: String,
    authorName: String,
    taggedFriends: List<FollowingFriend> = emptyList(),
    hasPhoto: Boolean = false,
): List<String> {
    val phrases = mutableSetOf<String>()
    category.split(",", ";")
        .map { it.trim().lowercase() }
        .filter { it.isNotEmpty() }
        .forEach { phrases.add(it) }

    val rawText = buildString {
        append(caption).append(' ')
        append(category).append(' ')
        append(authorName).append(' ')
        taggedFriends.forEach { append(it.displayName).append(' ') }
    }
    rawText.lowercase()
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.length >= 2 }
        .forEach { phrases.add(it) }

    if (hasPhoto) {
        phrases.add("foto")
        phrases.add("gambar")
    }
    return phrases.toList()
}

/** Mengubah map dokumen Firestore menjadi [PostUiModel] secara aman. */
fun parsePostUiModel(
    id: String,
    data: Map<String, Any?>,
    formattedTime: String = "",
): PostUiModel {
    val authorId = (data["authorId"] as? String)?.trim().orEmpty()
    val authorName = (data["authorName"] as? String)?.takeIf { it.isNotBlank() } ?: "Pengguna"
    val caption = (data["caption"] as? String) ?: "(Tanpa caption)"
    val imageUrl = (data["imageUrl"] as? String)?.takeIf { it.isNotBlank() }
    val category = (data["category"] as? String)?.trim().orEmpty()
    val storedKeywords = (data["searchKeywords"] as? List<*>)
        .orEmpty()
        .mapNotNull { (it as? String)?.trim()?.lowercase()?.takeIf { s -> s.isNotEmpty() } }
    val likedByIds = (data["likedByIds"] as? List<*>)
        .orEmpty()
        .mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
    val likedByNames = (data["likedByNames"] as? List<*>)
        .orEmpty()
        .mapNotNull { (it as? String)?.trim()?.takeIf { s -> s.isNotEmpty() } }
    val rawLikes = (data["likes"] as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L

    return PostUiModel(
        id = id,
        authorId = authorId,
        authorName = authorName,
        time = formattedTime,
        caption = caption,
        imageUrl = imageUrl,
        category = category,
        searchKeywords = storedKeywords,
        taggedNames = taggedFriendNamesFrom(data["taggedFriends"]),
        likesCount = maxOf(rawLikes, likedByIds.size.toLong()),
        likedByIds = likedByIds,
        likedByNames = likedByNames,
        commentsCount = (data["comment"] as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L,
        sharesCount = (data["Share"] as? Number)?.toLong()?.coerceAtLeast(0L) ?: 0L,
    )
}

/** Mengubah map dokumen `users/{uid}/notifications/{id}` menjadi [NotificationUiModel]. */
fun parseNotificationUiModel(
    id: String,
    data: Map<String, Any?>,
    fallbackMillis: Long = 0L,
): NotificationUiModel? {
    val postId = (data["postId"] as? String)?.trim().orEmpty()
    val actorName = (data["actorName"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: "Nama Pengguna"
    val actorId = (data["actorId"] as? String)?.trim().orEmpty()
    val type = NotificationType.fromCode(data["type"] as? String)
    val timestampMillis = (data["createdAt"] as? Timestamp)?.toDate()?.time
        ?: (data["createdAtMillis"] as? Number)?.toLong()
        ?: fallbackMillis
    if (id.isBlank()) return null
    return NotificationUiModel(
        id = id,
        actorId = actorId,
        actorName = actorName,
        type = type,
        postId = postId,
        createdAtMillis = timestampMillis,
    )
}

/** Mengelompokkan notifikasi ke "7 hari terakhir" dan "30 hari terakhir" sesuai desain Figma. */
fun groupNotificationsByPeriod(
    notifications: List<NotificationUiModel>,
    nowMillis: Long = System.currentTimeMillis(),
): List<Pair<String, List<NotificationUiModel>>> {
    val sevenDaysMs = 7L * 24 * 60 * 60 * 1000
    val sorted = notifications.sortedByDescending { it.createdAtMillis }
    val recent7 = sorted.filter { nowMillis - it.createdAtMillis <= sevenDaysMs }
    val older30 = sorted.filter { nowMillis - it.createdAtMillis > sevenDaysMs }

    return buildList {
        if (recent7.isNotEmpty()) add("7 hari terakhir" to recent7)
        if (older30.isNotEmpty()) add("30 hari terakhir" to older30)
    }
}

/** Mengecek apakah postingan cocok dengan kata kunci pencarian (makanan sehat, foto orang, caption, dll.). */
fun matchesPostQuery(post: PostUiModel, rawQuery: String): Boolean {
    val normalizedQuery = rawQuery.trim().lowercase()
    if (normalizedQuery.isEmpty()) return true

    val searchableBlob = buildString {
        append(post.caption.lowercase()).append(' ')
        append(post.category.lowercase()).append(' ')
        append(post.authorName.lowercase()).append(' ')
        post.taggedNames.forEach { append(it.lowercase()).append(' ') }
        post.searchKeywords.forEach { append(it.lowercase()).append(' ') }
        if (post.imageUrl != null) append("foto gambar ")
    }

    if (searchableBlob.contains(normalizedQuery)) return true

    val tokens = normalizedQuery
        .split(Regex("[^\\p{L}\\p{N}]+"))
        .filter { it.isNotEmpty() }
    return tokens.isNotEmpty() && tokens.all { token -> searchableBlob.contains(token) }
}

/** Menghasilkan daftar saran pencarian berdasarkan daftar postingan dan input pengguna. */
fun suggestPostQueries(posts: List<PostUiModel>, rawQuery: String): List<String> {
    val fromPosts = posts
        .flatMap { post ->
            post.category.split(",", ";").map { it.trim() }
        }
        .filter { it.isNotEmpty() }

    val candidates = (DefaultPostCategories + fromPosts)
        .distinctBy { it.lowercase() }

    val q = rawQuery.trim().lowercase()
    if (q.isEmpty()) return candidates
    val filtered = candidates.filter { it.lowercase().contains(q) }
    return if (filtered.isNotEmpty()) filtered else listOf(rawQuery.trim())
}

/** Menambah atau menghapus satu kategori pada string kategori yang dipisahkan koma. */
fun toggleCategoryTag(currentCategory: String, tag: String): String {
    val items = currentCategory.split(",")
        .map { it.trim() }
        .filter { it.isNotEmpty() }
    val exists = items.any { it.equals(tag, ignoreCase = true) }
    val updated = if (exists) {
        items.filterNot { it.equals(tag, ignoreCase = true) }
    } else {
        items + tag
    }
    return updated.joinToString(", ")
}

/** Ekstensi pembacaan [DocumentSnapshot] menjadi [PostUiModel]. */
fun DocumentSnapshot.toPostUiModel(): PostUiModel {
    val createdAt = (get("createdAt") as? Timestamp)?.toDate()
    val formattedTime = createdAt?.let {
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(it)
    }.orEmpty()
    return parsePostUiModel(id = id, data = data.orEmpty(), formattedTime = formattedTime)
}

/** Mengambil pengguna Firebase aktif dengan aman (misal pada mode Preview). */
fun currentUserOrNull(): FirebaseUser? =
    try {
        FirebaseAuth.getInstance().currentUser
    } catch (_: Exception) {
        null
    }

/** Mengambil nama tampilan pengguna dari displayName atau prefix email. */
fun FirebaseUser?.resolveDisplayName(fallback: String = "Teman"): String =
    this?.displayName?.takeIf { it.isNotBlank() }
        ?: this?.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        ?: fallback

/** State lokal sesi aktif agar suka/notifikasi/kategori langsung tampil walau Firestore Rules belum diperbarui. */
val LocalNotifications = mutableStateListOf<NotificationUiModel>()
val LocalLikedByIds = mutableStateMapOf<String, List<String>>()
val LocalLikedByNames = mutableStateMapOf<String, List<String>>()
val LocalPostCategories = mutableStateMapOf<String, String>()

/** Menggabungkan state Firestore dengan state sesi lokal (optimistic update / fallback). */
fun PostUiModel.withLocalState(): PostUiModel {
    val mergedLikedIds = LocalLikedByIds[id] ?: likedByIds
    val mergedLikedNames = LocalLikedByNames[id] ?: likedByNames
    val mergedCategory = category.ifBlank { LocalPostCategories[id].orEmpty() }
    val deltaLikes = mergedLikedIds.size - likedByIds.size
    val mergedLikesCount = (likesCount + deltaLikes).coerceAtLeast(mergedLikedIds.size.toLong())
    return copy(
        category = mergedCategory,
        likesCount = mergedLikesCount,
        likedByIds = mergedLikedIds,
        likedByNames = mergedLikedNames,
    )
}

/** Mencatat dokumen notifikasi pada state lokal dan sub-koleksi `users/{recipientUid}/notifications`. */
fun sendPostNotification(
    db: FirebaseFirestore,
    recipientUid: String,
    actorId: String,
    actorName: String,
    postId: String,
    type: NotificationType,
    deterministicId: String? = null,
) {
    if (recipientUid.isBlank() || postId.isBlank()) return
    val localId = deterministicId ?: "${type.code.lowercase()}_${postId}_${System.currentTimeMillis()}"
    LocalNotifications.removeAll { it.id == localId }
    LocalNotifications.add(
        0,
        NotificationUiModel(
            id = localId,
            actorId = actorId,
            actorName = actorName,
            type = type,
            postId = postId,
            createdAtMillis = System.currentTimeMillis(),
        ),
    )

    val notifCollection = db.collection("users").document(recipientUid).collection("notifications")
    val docRef = if (deterministicId != null) {
        notifCollection.document(deterministicId)
    } else {
        notifCollection.document()
    }
    val payload = mapOf(
        "actorId" to actorId,
        "actorName" to actorName,
        "postId" to postId,
        "type" to type.code,
        "createdAt" to FieldValue.serverTimestamp(),
    )
    docRef.set(payload, SetOptions.merge())
}

/** Menyukai atau batal menyukai postingan, sekaligus mengirim notifikasi ke pemilik postingan. */
fun togglePostLike(
    post: PostUiModel,
    onAuthRequired: () -> Unit = {},
    onError: (String) -> Unit = {},
) {
    val user = currentUserOrNull()
    if (user == null) {
        onAuthRequired()
        return
    }
    val effectivePost = post.withLocalState()
    val db = FirebaseFirestore.getInstance()
    val actorName = user.resolveDisplayName("Pengguna")
    val postRef = db.collection("posts").document(effectivePost.id)
    val alreadyLiked = user.uid in effectivePost.likedByIds
    val likeDocId = "like_${effectivePost.id}_${user.uid}"

    // Optimistic update agar UI dan halaman Notifikasi langsung ter-update
    if (alreadyLiked) {
        LocalLikedByIds[effectivePost.id] = effectivePost.likedByIds - user.uid
        LocalLikedByNames[effectivePost.id] = effectivePost.likedByNames - actorName
        LocalNotifications.removeAll { it.id == likeDocId }
    } else {
        LocalLikedByIds[effectivePost.id] = (effectivePost.likedByIds + user.uid).distinct()
        LocalLikedByNames[effectivePost.id] = (effectivePost.likedByNames + actorName).distinct()
        LocalNotifications.removeAll { it.id == likeDocId }
        LocalNotifications.add(
            0,
            NotificationUiModel(
                id = likeDocId,
                actorId = user.uid,
                actorName = actorName,
                type = NotificationType.LIKE,
                postId = effectivePost.id,
                createdAtMillis = System.currentTimeMillis(),
            ),
        )
    }

    val updates = if (alreadyLiked) {
        mapOf(
            "likes" to (effectivePost.likesCount - 1L).coerceAtLeast(0L),
            "likedByIds" to FieldValue.arrayRemove(user.uid),
            "likedByNames" to FieldValue.arrayRemove(actorName),
        )
    } else {
        mapOf(
            "likes" to effectivePost.likesCount + 1L,
            "likedByIds" to FieldValue.arrayUnion(user.uid),
            "likedByNames" to FieldValue.arrayUnion(actorName),
        )
    }

    val recipientUid = effectivePost.authorId.ifBlank { user.uid }
    if (alreadyLiked) {
        db.collection("users")
            .document(recipientUid)
            .collection("notifications")
            .document(likeDocId)
            .delete()
    } else {
        sendPostNotification(
            db = db,
            recipientUid = recipientUid,
            actorId = user.uid,
            actorName = actorName,
            postId = effectivePost.id,
            type = NotificationType.LIKE,
            deterministicId = likeDocId,
        )
    }

    postRef.update(updates)
        .addOnFailureListener { e ->
            val isPermissionDenied = (e as? FirebaseFirestoreException)?.code ==
                FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                e.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
            if (!isPermissionDenied) {
                onError(e.localizedMessage ?: "Gagal memperbarui suka.")
            }
        }
}

/**
 * Mengunggah foto (bila ada) dan menyimpan dokumen postingan ke koleksi `posts`,
 * serta mengirim notifikasi ke teman yang ditandai.
 */
fun submitNewPost(
    context: Context,
    auth: FirebaseAuth,
    db: FirebaseFirestore,
    authorName: String,
    caption: String,
    category: String = "",
    photoUri: Uri?,
    taggedFriends: List<FollowingFriend>,
    onWaitingUpload: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
) {
    val trimmedCategory = category.trim()

    fun savePostForUser(uid: String) {
        val postRef = db.collection("posts").document()

        fun notifyTaggedFriendsAndComplete() {
            if (trimmedCategory.isNotEmpty()) {
                LocalPostCategories[postRef.id] = trimmedCategory
            }
            taggedFriends.forEach { friend ->
                sendPostNotification(
                    db = db,
                    recipientUid = friend.uid,
                    actorId = uid,
                    actorName = authorName,
                    postId = postRef.id,
                    type = NotificationType.TAG,
                    deterministicId = "tag_${postRef.id}_${friend.uid}",
                )
            }
            onSuccess()
        }

        fun writeDocument(imageUrl: String? = null) {
            val keywords = buildSearchKeywords(
                caption = caption,
                category = trimmedCategory,
                authorName = authorName,
                taggedFriends = taggedFriends,
                hasPhoto = imageUrl != null,
            )
            val basePost = hashMapOf<String, Any>(
                "authorId" to uid,
                "authorName" to authorName,
                "caption" to caption,
                "createdAt" to FieldValue.serverTimestamp(),
            )
            if (imageUrl != null) basePost["imageUrl"] = imageUrl
            basePost.putAll(taggedFriendsFields(taggedFriends))

            val fullPost = HashMap(basePost).apply {
                put("searchKeywords", keywords)
                if (trimmedCategory.isNotEmpty()) put("category", trimmedCategory)
            }

            postRef.set(fullPost)
                .addOnSuccessListener {
                    notifyTaggedFriendsAndComplete()
                }
                .addOnFailureListener { error ->
                    val isPermissionDenied = (error as? FirebaseFirestoreException)?.code ==
                        FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                        error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
                    if (isPermissionDenied) {
                        // Fallback jika Firestore Rules di server belum mengizinkan field category/searchKeywords
                        postRef.set(basePost)
                            .addOnSuccessListener { notifyTaggedFriendsAndComplete() }
                            .addOnFailureListener { fallbackError ->
                                onError("Gagal menyimpan postingan: ${fallbackError.localizedMessage}")
                            }
                    } else {
                        onError("Gagal menyimpan postingan: ${error.localizedMessage}")
                    }
                }
        }

        if (photoUri == null) {
            writeDocument()
            return
        }

        PostPhotoUploader.upload(
            context = context,
            uri = photoUri,
            onSuccess = { url -> writeDocument(url) },
            onError = onError,
            onWaiting = onWaitingUpload,
        )
    }

    val currentUser = auth.currentUser
    if (currentUser != null) {
        savePostForUser(currentUser.uid)
    } else {
        auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                if (uid != null) savePostForUser(uid)
                else onError("ID pengguna tidak ditemukan.")
            }
            .addOnFailureListener { error ->
                onError("Login gagal: ${error.message}")
            }
    }
}

/** Menyinkronkan profil pengguna Firebase Auth aktif ke dokumen `users/{uid}` di Firestore. */
fun syncCurrentUserToFirestore() {
    val user = currentUserOrNull() ?: return
    val displayName = user.resolveDisplayName("")
    if (displayName.isBlank()) return
    try {
        val db = FirebaseFirestore.getInstance()
        val payload = hashMapOf<String, Any>(
            "displayName" to displayName,
            "username" to displayName,
            "name" to displayName,
            "updatedAt" to FieldValue.serverTimestamp(),
        )
        user.photoUrl?.toString()?.takeIf { it.isNotBlank() }?.let {
            payload["photoUrl"] = it
        }
        db.collection("users")
            .document(user.uid)
            .set(payload, SetOptions.merge())
    } catch (_: Exception) {
    }
}

private var hasSeededCurrentSession = false

/**
 * Mengisi otomatis koleksi awal di Firestore (`users`, `users/{uid}/following`,
 * `users/{uid}/notifications`, `posts`, dan `chats` beserta `messages`) untuk akun
 * yang sedang login apabila masih kosong, sehingga pengguna tidak perlu input manual di Firebase Console.
 */
fun seedInitialFirestoreCollectionsIfNeeded(onComplete: () -> Unit = {}) {
    val user = currentUserOrNull() ?: return
    if (hasSeededCurrentSession) return
    hasSeededCurrentSession = true

    val uid = user.uid
    val myName = user.resolveDisplayName("Anda")
    val db = FirebaseFirestore.getInstance()

    val seedFriends = listOf(
        FollowingFriend(uid = "user_monster_basket", displayName = "Monster Basket"),
        FollowingFriend(uid = "user_kenangan_terindah", displayName = "KenanganTerindah"),
        FollowingFriend(uid = "user_sayur_olahraga", displayName = "SayurOlahraga"),
        FollowingFriend(uid = "user_bibi_olahraga", displayName = "BibiOlahraga"),
        FollowingFriend(uid = "user_gurita_bola", displayName = "Gurita bola"),
        FollowingFriend(uid = "user_kucing_padel", displayName = "Kucing padel"),
    )

    try {
        // 1. Pastikan dokumen users/{uid} milik pengguna aktif memiliki counter & profil lengkap
        val myUserRef = db.collection("users").document(uid)
        myUserRef.get().addOnSuccessListener { doc ->
            val updates = hashMapOf<String, Any>(
                "displayName" to myName,
                "username" to myName,
                "name" to myName,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            if (doc == null || !doc.exists() || doc.get("followingCount") == null) {
                updates["followersCount"] = 12L
                updates["followingCount"] = seedFriends.size.toLong()
                updates["postsCount"] = 2L
            }
            myUserRef.set(updates, SetOptions.merge())
        }

        // 2. Isi sub-koleksi users/{uid}/following bila masih kosong & daftarkan profil teman di koleksi users
        val followingCol = myUserRef.collection("following")
        followingCol.limit(1).get().addOnSuccessListener { snap ->
            if (snap != null && snap.isEmpty) {
                seedFriends.forEach { friend ->
                    followingCol.document(friend.uid).set(
                        mapOf(
                            "uid" to friend.uid,
                            "displayName" to friend.displayName,
                            "name" to friend.displayName,
                            "username" to friend.displayName,
                            "createdAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    )
                    db.collection("users").document(friend.uid).set(
                        mapOf(
                            "displayName" to friend.displayName,
                            "username" to friend.displayName,
                            "name" to friend.displayName,
                            "followersCount" to 24L,
                            "followingCount" to 18L,
                            "postsCount" to 3L,
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    )
                }
            }
        }

        // 3. Isi postingan awal di koleksi posts bila koleksi posts masih kosong
        val postsCol = db.collection("posts")
        postsCol.limit(1).get().addOnSuccessListener { postSnap ->
            if (postSnap != null && postSnap.isEmpty) {
                val post1Id = "seed_post_sayur"
                val post2Id = "seed_post_jogging"
                val post3Id = "seed_post_gym"

                val post1 = hashMapOf<String, Any>(
                    "authorId" to uid,
                    "authorName" to "SayurOlahraga",
                    "caption" to "Ayo mulai konsumsi sayur supaya harimu makin sehat",
                    "category" to "Makanan Sehat",
                    "searchKeywords" to buildSearchKeywords(
                        caption = "Ayo mulai konsumsi sayur supaya harimu makin sehat",
                        category = "Makanan Sehat",
                        authorName = "SayurOlahraga",
                    ),
                    "likes" to 57L,
                    "likedByIds" to listOf("user_monster_basket"),
                    "likedByNames" to listOf("Monster Basket"),
                    "comment" to 1L,
                    "Share" to 8L,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
                val post2 = hashMapOf<String, Any>(
                    "authorId" to uid,
                    "authorName" to "KenanganTerindah",
                    "caption" to "Jogging santuy lagi guys with my bestie",
                    "category" to "Olahraga Pagi, Kardio",
                    "searchKeywords" to buildSearchKeywords(
                        caption = "Jogging santuy lagi guys with my bestie",
                        category = "Olahraga Pagi, Kardio",
                        authorName = "KenanganTerindah",
                        taggedFriends = listOf(FollowingFriend(uid, myName)),
                    ),
                    "taggedFriendIds" to listOf(uid),
                    "taggedFriends" to listOf(mapOf("uid" to uid, "displayName" to myName)),
                    "likes" to 57L,
                    "likedByIds" to listOf("user_monster_basket"),
                    "likedByNames" to listOf("Monster Basket"),
                    "comment" to 0L,
                    "Share" to 8L,
                    "createdAt" to FieldValue.serverTimestamp(),
                )
                val post3 = hashMapOf<String, Any>(
                    "authorId" to uid,
                    "authorName" to "BibiOlahraga",
                    "caption" to "Always on Proces",
                    "category" to "Bulking, Foto Orang",
                    "searchKeywords" to buildSearchKeywords(
                        caption = "Always on Proces",
                        category = "Bulking, Foto Orang",
                        authorName = "BibiOlahraga",
                    ),
                    "likes" to 57L,
                    "likedByIds" to listOf("user_kenangan_terindah", "user_monster_basket"),
                    "likedByNames" to listOf("NamaPengguna", "Monster Basket"),
                    "comment" to 0L,
                    "Share" to 8L,
                    "createdAt" to FieldValue.serverTimestamp(),
                )

                postsCol.document(post1Id).set(post1, SetOptions.merge())
                postsCol.document(post2Id).set(post2, SetOptions.merge())
                postsCol.document(post3Id).set(post3, SetOptions.merge())
            }
        }

        // 4. Isi sub-koleksi users/{uid}/notifications bila masih kosong
        val notifCol = myUserRef.collection("notifications")
        notifCol.limit(1).get().addOnSuccessListener { notifSnap ->
            if (notifSnap != null && notifSnap.isEmpty) {
                notifCol.document("seed_notif_like_monster").set(
                    mapOf(
                        "actorId" to uid,
                        "actorName" to "Monster Basket",
                        "postId" to "seed_post_gym",
                        "type" to NotificationType.LIKE.code,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
                notifCol.document("seed_notif_tag_kenangan").set(
                    mapOf(
                        "actorId" to uid,
                        "actorName" to "KenanganTerindah",
                        "postId" to "seed_post_jogging",
                        "type" to NotificationType.TAG.code,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
                notifCol.document("seed_notif_comment_sayur").set(
                    mapOf(
                        "actorId" to uid,
                        "actorName" to "SayurOlahraga",
                        "postId" to "seed_post_sayur",
                        "type" to NotificationType.COMMENT.code,
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
            }
        }

        // 5. Isi koleksi chats & sub-koleksi messages bila masih kosong
        val chatsCol = db.collection("chats")
        chatsCol.limit(1).get().addOnSuccessListener { chatSnap ->
            if (chatSnap != null && chatSnap.isEmpty) {
                val dmMonsterId = directChatRoomId(uid, "user_monster_basket")
                val dmKenanganId = directChatRoomId(uid, "user_kenangan_terindah")
                val groupGymId = "group_gymclub"

                val monsterChatRef = chatsCol.document(dmMonsterId)
                monsterChatRef.set(
                    mapOf(
                        "name" to "Monster Basket",
                        "isGroup" to false,
                        "statusOrMembers" to "aktif 2 jam lalu",
                        "lastMessage" to "Ayo bro",
                        "dateLabel" to "4/11",
                        "participantIds" to listOf(uid, "user_monster_basket"),
                        "participantNames" to listOf(myName, "Monster Basket"),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
                monsterChatRef.collection("messages").document("m1").set(
                    mapOf(
                        "senderId" to "user_monster_basket",
                        "senderName" to "Monster Basket",
                        "text" to "Bro nanti olahraga bareng dong, kepengen banget main basket gue",
                        "timeText" to "11:20",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
                monsterChatRef.collection("messages").document("m2").set(
                    mapOf(
                        "senderId" to uid,
                        "senderName" to myName,
                        "text" to "Kapan kapan gas bro",
                        "timeText" to "11:21",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )

                val kenanganChatRef = chatsCol.document(dmKenanganId)
                kenanganChatRef.set(
                    mapOf(
                        "name" to "KenanganTerindah",
                        "isGroup" to false,
                        "statusOrMembers" to "aktif 1 jam lalu",
                        "lastMessage" to "Ayok olahraga bareng temannn",
                        "dateLabel" to "4/11",
                        "participantIds" to listOf(uid, "user_kenangan_terindah"),
                        "participantNames" to listOf(myName, "KenanganTerindah"),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
                kenanganChatRef.collection("messages").document("k1").set(
                    mapOf(
                        "senderId" to "user_kenangan_terindah",
                        "senderName" to "KenanganTerindah",
                        "text" to "Ayok olahraga bareng temannn",
                        "timeText" to "11:22",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )

                val gymGroupRef = chatsCol.document(groupGymId)
                gymGroupRef.set(
                    mapOf(
                        "name" to "GymClub",
                        "isGroup" to true,
                        "statusOrMembers" to "Anda,Monster Basket,KenanganTerindah",
                        "lastMessage" to "Monster Basket: Siapa mau gym soree",
                        "dateLabel" to "4/11",
                        "participantIds" to listOf(uid, "user_monster_basket", "user_kenangan_terindah"),
                        "participantNames" to listOf(myName, "Monster Basket", "KenanganTerindah"),
                        "updatedAt" to FieldValue.serverTimestamp(),
                    ),
                    SetOptions.merge(),
                )
                gymGroupRef.collection("messages").document("g1").set(
                    mapOf(
                        "senderId" to "user_monster_basket",
                        "senderName" to "Monster Basket",
                        "text" to "Siapa mau gym soree",
                        "timeText" to "11:20",
                        "createdAt" to FieldValue.serverTimestamp(),
                    ),
                )
            }
            onComplete()
        }
    } catch (_: Exception) {
    }
}

/**
 * Memuat daftar teman/akun dari Firebase:
 * 1. Sub-koleksi `users/{uid}/following`
 * 2. Field `following` pada dokumen `users/{uid}` (bila ada)
 * 3. Koleksi `users` dan penulis unik pada koleksi `posts` (selain akun yang sedang login)
 */
fun loadFirebaseFriends(
    uid: String?,
    currentUserName: String = "",
    onResult: (List<FollowingFriend>) -> Unit,
    onError: (String) -> Unit = {},
) {
    val db = FirebaseFirestore.getInstance()
    val collected = mutableListOf<FollowingFriend>()

    fun publishMerged() {
        val filtered = collected.filterNot { friend ->
            (uid != null && friend.uid == uid) ||
                (currentUserName.isNotBlank() && friend.displayName.equals(currentUserName, ignoreCase = true))
        }
        onResult(sortFollowingFriends(filtered))
    }

    fun fetchFromUsersAndPosts() {
        db.collection("users")
            .limit(50)
            .get()
            .addOnSuccessListener { usersSnap ->
                usersSnap.documents.forEach { doc ->
                    followingFriendFrom(doc.id, doc.data.orEmpty())?.let { collected.add(it) }
                }
                publishMerged()
            }
            .addOnCompleteListener {
                db.collection("posts")
                    .limit(50)
                    .get()
                    .addOnSuccessListener { postsSnap ->
                        postsSnap.documents.forEach { doc ->
                            val authorName = doc.getString("authorName")?.trim().orEmpty()
                            val authorId = doc.getString("authorId")?.trim().orEmpty()
                                .ifEmpty { "author_${authorName.lowercase()}" }
                            if (authorName.isNotEmpty()) {
                                collected.add(FollowingFriend(uid = authorId, displayName = authorName))
                            }
                            val taggedRaw = doc.get("taggedFriends") as? List<*>
                            taggedRaw.orEmpty().forEach { item ->
                                val map = item as? Map<*, *> ?: return@forEach
                                val tUid = (map["uid"] as? String)?.trim().orEmpty()
                                val tName = (map["displayName"] as? String)?.trim().orEmpty()
                                if (tUid.isNotEmpty() && tName.isNotEmpty()) {
                                    collected.add(FollowingFriend(uid = tUid, displayName = tName))
                                }
                            }
                        }
                        publishMerged()
                    }
                    .addOnFailureListener { err ->
                        if (collected.isEmpty()) {
                            onError(err.localizedMessage ?: "Gagal memuat daftar akun dari Firebase.")
                        } else {
                            publishMerged()
                        }
                    }
            }
    }

    if (uid == null) {
        fetchFromUsersAndPosts()
        return
    }

    db.collection("users")
        .document(uid)
        .collection("following")
        .get()
        .addOnSuccessListener { snap ->
            snap.documents.forEach { doc ->
                followingFriendFrom(doc.id, doc.data.orEmpty())?.let { collected.add(it) }
            }
            publishMerged()
        }
        .addOnCompleteListener {
            db.collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener { userDoc ->
                    val rawFollowing = userDoc.get("following") as? List<*>
                    rawFollowing.orEmpty().forEachIndexed { idx, entry ->
                        when (entry) {
                            is String -> if (entry.isNotBlank()) {
                                collected.add(FollowingFriend(uid = entry.trim(), displayName = entry.trim()))
                            }
                            is Map<*, *> -> {
                                @Suppress("UNCHECKED_CAST")
                                followingFriendFrom(
                                    documentId = (entry["uid"] as? String) ?: "follow_$idx",
                                    data = entry as Map<String, Any?>,
                                )?.let { collected.add(it) }
                            }
                        }
                    }
                    publishMerged()
                }
                .addOnCompleteListener {
                    fetchFromUsersAndPosts()
                }
        }
}

