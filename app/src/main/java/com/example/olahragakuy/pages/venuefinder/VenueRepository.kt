package com.example.olahragakuy.pages.venuefinder

import com.example.olahragakuy.pages.posts.ChatRoomUiModel
import com.example.olahragakuy.pages.posts.FollowingFriend
import com.example.olahragakuy.pages.posts.LocalChatRooms
import com.example.olahragakuy.pages.posts.directChatRoomId
import com.example.olahragakuy.pages.posts.loadFirebaseFriends
import com.example.olahragakuy.pages.posts.resolveChatRoomDisplayName
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Locale

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
            .collection("venues")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) {
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
    } catch (_: Exception) {
        onResult(filterActiveVenues(DefaultUsuVenues), true)
        null
    }
}

/**
 * Memuat daftar kontak (teman DM dan grup komunitas) secara read-only dari Firestore
 * untuk ditampilkan di bottom sheet "Bagikan ke:".
 */
fun loadVenueShareRecipients(
    currentUserId: String?,
    currentUserName: String,
    onResult: (List<ShareRecipient>) -> Unit,
) {
    val safeUid = currentUserId?.trim().orEmpty().ifEmpty { "me" }
    val safeName = currentUserName.trim().ifEmpty { "Anda" }
    var loadedFriends: List<FollowingFriend> = emptyList()
    var loadedRooms: List<ChatRoomUiModel> = LocalChatRooms.toList()

    fun publish() {
        onResult(
            mergeShareRecipients(
                chatRooms = loadedRooms,
                friends = loadedFriends,
                currentUserId = safeUid,
                currentUserName = safeName,
            ),
        )
    }

    // Publikasikan dulu cache ruang chat lokal agar UI langsung responsif.
    publish()

    try {
        loadFirebaseFriends(
            uid = currentUserId,
            currentUserName = safeName,
            onResult = { friends ->
                loadedFriends = friends
                publish()
            },
        )
    } catch (_: Exception) {
    }

    try {
        FirebaseFirestore.getInstance()
            .collection("chats")
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(30)
            .get()
            .addOnSuccessListener { snap ->
                val remoteRooms = snap.documents.mapNotNull { doc ->
                    val data = doc.data.orEmpty()
                    val rawName = (data["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
                        ?: return@mapNotNull null
                    val isGroup = (data["isGroup"] as? Boolean) ?: false
                    val memberIds = (data["participantIds"] as? List<*>)
                        .orEmpty()
                        .mapNotNull { it as? String }
                    val memberNames = (data["participantNames"] as? List<*>)
                        .orEmpty()
                        .mapNotNull { it as? String }

                    val isParticipant = memberIds.isEmpty() ||
                        safeUid in memberIds ||
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
    } catch (_: Exception) {
    }
}

/**
 * Menggabungkan ruang obrolan aktif (DM & grup) dengan daftar akun teman diikuti,
 * lalu menghapus duplikasi agar setiap penerima tampil sekali di daftar "Bagikan ke:".
 */
fun mergeShareRecipients(
    chatRooms: List<ChatRoomUiModel>,
    friends: List<FollowingFriend>,
    currentUserId: String,
    currentUserName: String,
): List<ShareRecipient> {
    val result = mutableListOf<ShareRecipient>()
    val seenRoomIds = mutableSetOf<String>()
    val seenDirectNames = mutableSetOf<String>()

    chatRooms.forEach { room ->
        if (room.id.isBlank() || room.name.isBlank()) return@forEach
        if (!seenRoomIds.add(room.id)) return@forEach

        val targetUid = if (room.isGroup) {
            room.id
        } else {
            room.participantIds.firstOrNull { it.isNotBlank() && it != currentUserId }
                ?: room.id
        }
        if (!room.isGroup) {
            seenDirectNames.add(room.name.trim().lowercase(Locale.ROOT))
        }
        result.add(
            ShareRecipient(
                id = targetUid,
                name = room.name.trim(),
                isGroup = room.isGroup,
                roomId = room.id,
                avatarColorHex = if (room.isGroup) 0xFFE05A47L else pickRecipientAvatarColor(room.name),
                participantIds = room.participantIds,
                participantNames = room.participantNames,
            ),
        )
    }

    friends.forEach { friend ->
        val cleanName = friend.displayName.trim()
        if (friend.uid.isBlank() || cleanName.isEmpty()) return@forEach

        val dmId = directChatRoomId(currentUserId, friend.uid)
        val lowerName = cleanName.lowercase(Locale.ROOT)
        if (dmId in seenRoomIds || lowerName in seenDirectNames) return@forEach

        seenRoomIds.add(dmId)
        seenDirectNames.add(lowerName)
        result.add(
            ShareRecipient(
                id = friend.uid,
                name = cleanName,
                isGroup = false,
                roomId = dmId,
                avatarUrl = friend.photoUrl,
                avatarColorHex = pickRecipientAvatarColor(cleanName),
                participantIds = listOf(currentUserId, friend.uid).filter { it.isNotBlank() }.distinct(),
                participantNames = listOf(currentUserName, cleanName).filter { it.isNotBlank() }.distinct(),
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
