package com.example.olahragakuy.pages.venuefinder

import android.util.Log
import com.example.olahragakuy.pages.posts.CHAT_COLLECTION
import com.example.olahragakuy.pages.posts.ChatRoomUiModel
import com.example.olahragakuy.pages.posts.FollowingFriend
import com.example.olahragakuy.pages.posts.LocalChatRooms
import com.example.olahragakuy.pages.posts.directChatRoomId
import com.example.olahragakuy.pages.posts.followingFriendFrom
import com.example.olahragakuy.pages.posts.isValidChatUserUid
import com.example.olahragakuy.pages.posts.loadFirebaseFriends
import com.example.olahragakuy.pages.posts.resolveChatRoomDisplayName
import com.example.olahragakuy.pages.posts.sanitizeChatParticipantIds
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Locale

private const val TAG_VENUE_REPOSITORY = "VenueRepository"
private const val USERS_COLLECTION = "users"
private const val VENUES_COLLECTION = "venues"

private val RecipientAvatarPalette = listOf(
    0xFF59C39CL,
    0xFF6B8FF7L,
    0xFFF2B84BL,
    0xFF7FA7E8L,
    0xFFE05A47L,
)

/**
 * Mengamati koleksi `venues` di Firestore secara read-only & real-time menggunakan
 * `addSnapshotListener`.
 *
 * - Hanya mengembalikan venue dengan `isActive == true`.
 * - Jika koleksi kosong, offline, atau gagal diakses, otomatis memakai fallback [DefaultUsuVenues].
 * - Tidak pernah menulis atau membuat koleksi baru di Firestore.
 */
fun observeActiveVenues(
    onResult: (venues: List<VenueUiModel>, isFallback: Boolean) -> Unit,
): ListenerRegistration? {
    return try {
        FirebaseFirestore.getInstance()
            .collection(VENUES_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(
                        TAG_VENUE_REPOSITORY,
                        "Gagal mengamati koleksi $VENUES_COLLECTION: ${error.message}",
                        error,
                    )
                    onResult(filterActiveVenues(DefaultUsuVenues), true)
                    return@addSnapshotListener
                }
                if (snapshot == null) {
                    onResult(filterActiveVenues(DefaultUsuVenues), true)
                    return@addSnapshotListener
                }
                val parsedActive = filterActiveVenues(
                    snapshot.documents.mapNotNull { doc ->
                        parseVenueFromMap(doc.id, doc.data.orEmpty())
                    },
                )
                if (parsedActive.isEmpty()) {
                    onResult(filterActiveVenues(DefaultUsuVenues), true)
                } else {
                    onResult(parsedActive, false)
                }
            }
    } catch (error: Exception) {
        Log.e(
            TAG_VENUE_REPOSITORY,
            "Exception saat mengamati koleksi $VENUES_COLLECTION: ${error.message}",
            error,
        )
        onResult(filterActiveVenues(DefaultUsuVenues), true)
        null
    }
}

/**
 * Memuat daftar kontak (teman DM dan grup komunitas) secara read-only dari Firestore
 * untuk ditampilkan di bottom sheet "Bagikan ke:".
 *
 * Selalu memuat koleksi kanonis `users` agar penerima DM memakai UID pengguna asli
 * (tidak pernah fallback ke `room.id`, nama tampilan, atau ID `author_...`).
 */
fun loadVenueShareRecipients(
    currentUserId: String?,
    currentUserName: String,
    onResult: (List<ShareRecipient>) -> Unit,
) {
    val safeUid = currentUserId?.trim().orEmpty()
    val safeName = currentUserName.trim().ifEmpty { "Anda" }
    var loadedUsers: List<FollowingFriend> = emptyList()
    var loadedFriends: List<FollowingFriend> = emptyList()
    var loadedRooms: List<ChatRoomUiModel> = LocalChatRooms.toList()

    fun publish() {
        onResult(
            mergeShareRecipients(
                chatRooms = loadedRooms,
                friends = loadedFriends,
                currentUserId = safeUid,
                currentUserName = safeName,
                canonicalUsers = loadedUsers,
            ),
        )
    }

    // Publikasikan dulu cache ruang chat lokal agar UI langsung responsif.
    publish()

    val db = try {
        FirebaseFirestore.getInstance()
    } catch (error: Exception) {
        Log.e(TAG_VENUE_REPOSITORY, "Gagal mengakses FirebaseFirestore: ${error.message}", error)
        return
    }

    // 1. Muat koleksi kanonis `users` untuk resolusi UID pengguna asli.
    try {
        db.collection(USERS_COLLECTION)
            .limit(50)
            .get()
            .addOnSuccessListener { usersSnap ->
                loadedUsers = usersSnap.documents.mapNotNull { doc ->
                    val canonicalUid = doc.id.trim()
                    if (!isValidChatUserUid(canonicalUid) || canonicalUid == safeUid) {
                        return@mapNotNull null
                    }
                    val parsed = followingFriendFrom(canonicalUid, doc.data.orEmpty())
                        ?: return@mapNotNull null
                    parsed.copy(uid = canonicalUid)
                }
                publish()
            }
            .addOnFailureListener { error ->
                Log.e(
                    TAG_VENUE_REPOSITORY,
                    "Gagal memuat koleksi $USERS_COLLECTION: ${error.message}",
                    error,
                )
            }
    } catch (error: Exception) {
        Log.e(
            TAG_VENUE_REPOSITORY,
            "Exception saat memuat koleksi $USERS_COLLECTION: ${error.message}",
            error,
        )
    }

    // 2. Muat daftar akun diikuti dari helper Firebase (hanya UID valid yang dipakai di merge).
    try {
        loadFirebaseFriends(
            uid = currentUserId,
            currentUserName = safeName,
            onResult = { friends ->
                loadedFriends = friends.filter { isValidChatUserUid(it.uid) }
                publish()
            },
            onError = { message ->
                Log.e(TAG_VENUE_REPOSITORY, "Gagal memuat daftar teman Firebase: $message")
            },
        )
    } catch (error: Exception) {
        Log.e(TAG_VENUE_REPOSITORY, "Exception saat memuat daftar teman Firebase: ${error.message}", error)
    }

    // 3. Muat daftar ruang obrolan aktif (DM & grup komunitas) dari koleksi kanonis `chats`.
    try {
        db.collection(CHAT_COLLECTION)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(30)
            .get()
            .addOnSuccessListener { snap ->
                val remoteRooms = snap.documents.mapNotNull { doc ->
                    val data = doc.data.orEmpty()
                    val rawName = (data["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                        ?: return@mapNotNull null
                    val isGroup = (data["isGroup"] as? Boolean) ?: false
                    val memberIds = sanitizeChatParticipantIds(
                        (data["participantIds"] as? List<*>)
                            .orEmpty()
                            .mapNotNull { it as? String },
                    )
                    val memberNames = (data["participantNames"] as? List<*>)
                        .orEmpty()
                        .mapNotNull { it as? String }

                    val isParticipant = memberIds.isEmpty() ||
                        (isValidChatUserUid(safeUid) && safeUid in memberIds) ||
                        memberNames.any { it.equals(safeName, ignoreCase = true) }
                    if (!isParticipant) return@mapNotNull null

                    val displayName = resolveChatRoomDisplayName(
                        fallbackName = rawName,
                        isGroup = isGroup,
                        participantNames = memberNames,
                        currentUserName = safeName,
                    )
                    ChatRoomUiModel(
                        id = doc.id,
                        name = displayName,
                        isGroup = isGroup,
                        statusOrMembers = (data["statusOrMembers"] as? String).orEmpty(),
                        lastMessage = (data["lastMessage"] as? String).orEmpty(),
                        dateLabel = (data["dateLabel"] as? String).orEmpty(),
                        avatarColorHex = if (isGroup) 0xFFE05A47L else pickRecipientAvatarColor(displayName),
                        participantIds = memberIds,
                        participantNames = memberNames,
                    )
                }
                loadedRooms = (remoteRooms + LocalChatRooms.toList()).distinctBy { it.id }
                publish()
            }
            .addOnFailureListener { error ->
                Log.e(
                    TAG_VENUE_REPOSITORY,
                    "Gagal memuat koleksi $CHAT_COLLECTION: ${error.message}",
                    error,
                )
            }
    } catch (error: Exception) {
        Log.e(
            TAG_VENUE_REPOSITORY,
            "Exception saat memuat koleksi $CHAT_COLLECTION: ${error.message}",
            error,
        )
    }
}

/**
 * Menggabungkan ruang obrolan aktif (DM & grup), daftar pengguna kanonis (`users`), dan daftar
 * teman diikuti, lalu menghapus duplikasi agar setiap penerima tampil sekali di daftar "Bagikan ke:".
 *
 * Aturan resolusi UID untuk DM:
 * - Hanya menerima UID pengguna asli ([isValidChatUserUid]) dan tidak pernah fallback ke `room.id`,
 *   nama tampilan, atau ID sintetis `author_...`.
 * - Setiap penerima DM selalu menggunakan `roomId = directChatRoomId(currentUserId, targetUid)`.
 */
fun mergeShareRecipients(
    chatRooms: List<ChatRoomUiModel>,
    friends: List<FollowingFriend>,
    currentUserId: String,
    currentUserName: String,
    canonicalUsers: List<FollowingFriend> = emptyList(),
): List<ShareRecipient> {
    val cleanCurrentUid = currentUserId.trim()
    val cleanCurrentName = currentUserName.trim().ifEmpty { "Anda" }

    // Bangun peta pengguna valid berdasarkan nama (prioritaskan koleksi kanonis `users`).
    val validUsersOrdered = (canonicalUsers + friends)
        .mapNotNull { user ->
            val uid = user.uid.trim()
            val name = user.displayName.trim()
            if (!isValidChatUserUid(uid) || uid == cleanCurrentUid || name.isEmpty()) {
                null
            } else {
                user.copy(uid = uid, displayName = name)
            }
        }
        .distinctBy { it.uid }

    val validUsersByLowerName = LinkedHashMap<String, FollowingFriend>()
    validUsersOrdered.forEach { user ->
        val key = user.displayName.lowercase(Locale.ROOT)
        if (key !in validUsersByLowerName) {
            validUsersByLowerName[key] = user
        }
    }
    val validUsersByUid = validUsersOrdered.associateBy { it.uid }

    val result = mutableListOf<ShareRecipient>()
    val seenRoomIds = mutableSetOf<String>()
    val seenDirectUids = mutableSetOf<String>()
    val seenDirectNames = mutableSetOf<String>()

    chatRooms.forEach { room ->
        val cleanRoomId = room.id.trim()
        val cleanRoomName = room.name.trim()
        if (cleanRoomId.isEmpty() || cleanRoomName.isEmpty()) return@forEach

        if (room.isGroup) {
            if (!seenRoomIds.add(cleanRoomId)) return@forEach
            val cleanParticipants = sanitizeChatParticipantIds(
                room.participantIds + listOfNotNull(cleanCurrentUid.takeIf { isValidChatUserUid(it) }),
            )
            result.add(
                ShareRecipient(
                    id = cleanRoomId,
                    name = cleanRoomName,
                    isGroup = true,
                    roomId = cleanRoomId,
                    avatarColorHex = 0xFFE05A47L,
                    participantIds = cleanParticipants,
                    participantNames = room.participantNames
                        .map { it.trim() }
                        .filter { it.isNotEmpty() }
                        .distinct(),
                ),
            )
            return@forEach
        }

        // Untuk DM: wajib menemukan UID pengguna asli (tanpa fallback ke room.id / nama / author_...).
        val lowerName = cleanRoomName.lowercase(Locale.ROOT)
        val targetUidFromParticipants = sanitizeChatParticipantIds(room.participantIds)
            .firstOrNull { it != cleanCurrentUid }
        val resolvedUser = targetUidFromParticipants?.let { validUsersByUid[it] }
            ?: validUsersByLowerName[lowerName]
        val targetUid = targetUidFromParticipants ?: resolvedUser?.uid
        if (!isValidChatUserUid(targetUid) || targetUid == null || targetUid == cleanCurrentUid) {
            return@forEach
        }

        val canonicalRoomId = directChatRoomId(cleanCurrentUid, targetUid)
        if (canonicalRoomId in seenRoomIds || targetUid in seenDirectUids || lowerName in seenDirectNames) {
            return@forEach
        }

        seenRoomIds.add(canonicalRoomId)
        seenDirectUids.add(targetUid)
        seenDirectNames.add(lowerName)

        val cleanParticipantIds = sanitizeChatParticipantIds(
            room.participantIds + listOf(cleanCurrentUid, targetUid),
        )
        val cleanParticipantNames = (
            room.participantNames + listOf(cleanCurrentName, cleanRoomName)
            )
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals("Anda", ignoreCase = true) }
            .distinct()

        result.add(
            ShareRecipient(
                id = targetUid,
                name = cleanRoomName,
                isGroup = false,
                roomId = canonicalRoomId,
                avatarUrl = resolvedUser?.photoUrl,
                avatarColorHex = pickRecipientAvatarColor(cleanRoomName),
                participantIds = cleanParticipantIds,
                participantNames = cleanParticipantNames,
            ),
        )
    }

    validUsersOrdered.forEach { user ->
        val cleanName = user.displayName
        val lowerName = cleanName.lowercase(Locale.ROOT)
        if (cleanName.equals(cleanCurrentName, ignoreCase = true)) return@forEach

        val dmId = directChatRoomId(cleanCurrentUid, user.uid)
        if (dmId in seenRoomIds || user.uid in seenDirectUids || lowerName in seenDirectNames) {
            return@forEach
        }

        seenRoomIds.add(dmId)
        seenDirectUids.add(user.uid)
        seenDirectNames.add(lowerName)

        result.add(
            ShareRecipient(
                id = user.uid,
                name = cleanName,
                isGroup = false,
                roomId = dmId,
                avatarUrl = user.photoUrl,
                avatarColorHex = pickRecipientAvatarColor(cleanName),
                participantIds = sanitizeChatParticipantIds(listOf(cleanCurrentUid, user.uid)),
                participantNames = listOf(cleanCurrentName, cleanName)
                    .map { it.trim() }
                    .filter { it.isNotEmpty() && !it.equals("Anda", ignoreCase = true) }
                    .distinct(),
            ),
        )
    }

    return result
}

private fun pickRecipientAvatarColor(name: String): Long {
    val lower = name.lowercase(Locale.ROOT)
    return when {
        lower.contains("monster") -> 0xFF59C39CL
        lower.contains("kenangan") -> 0xFF6B8FF7L
        else -> RecipientAvatarPalette[name.hashCode().mod(RecipientAvatarPalette.size)]
    }
}
