package com.example.olahragakuy.pages.venuefinder

import android.util.Log
import com.example.olahragakuy.pages.posts.ChatRoomUiModel
import com.example.olahragakuy.pages.posts.FIRESTORE_CHATS_COLLECTION
import com.example.olahragakuy.pages.posts.FollowingFriend
import com.example.olahragakuy.pages.posts.LocalChatRooms
import com.example.olahragakuy.pages.posts.directChatRoomId
import com.example.olahragakuy.pages.posts.followingFriendFrom
import com.example.olahragakuy.pages.posts.isValidChatUserUid
import com.example.olahragakuy.pages.posts.loadFirebaseFriends
import com.example.olahragakuy.pages.posts.resolveChatRoomDisplayName
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import java.util.Locale

private const val TAG_VENUE_REPOSITORY = "VenueRepository"

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
 * untuk ditampilkan di bottom sheet "Bagikan ke:", dengan memastikan target DM selalu
 * memakai UID pengguna asli dari koleksi `users` agar `roomId` identik di pengirim & penerima.
 */
fun loadVenueShareRecipients(
    currentUserId: String?,
    currentUserName: String,
    onResult: (List<ShareRecipient>) -> Unit,
) {
    val safeUid = currentUserId?.trim().orEmpty().ifEmpty { "me" }
    val safeName = currentUserName.trim().ifEmpty { "Anda" }
    var loadedUsers: List<FollowingFriend> = emptyList()
    var loadedFriends: List<FollowingFriend> = emptyList()
    var loadedRooms: List<ChatRoomUiModel> = LocalChatRooms.toList()
    var canonicalUsersByName: Map<String, String> = emptyMap()
    var knownUserIds: Set<String> = emptySet()

    fun publish() {
        onResult(
            mergeShareRecipients(
                chatRooms = loadedRooms,
                friends = loadedUsers + loadedFriends,
                currentUserId = safeUid,
                currentUserName = safeName,
                canonicalUsersByName = canonicalUsersByName,
                knownUserIds = knownUserIds,
            ),
        )
    }

    // Publikasikan dulu cache ruang chat lokal agar UI langsung responsif.
    publish()

    val db = try {
        FirebaseFirestore.getInstance()
    } catch (error: Exception) {
        Log.e(TAG_VENUE_REPOSITORY, "Gagal menginisialisasi Firestore: ${error.message}", error)
        return
    }

    // 1. Muat koleksi kanonis `users` agar setiap penerima DM dipetakan ke UID asli FirebaseAuth.
    try {
        db.collection("users")
            .limit(100)
            .get()
            .addOnSuccessListener { snap ->
                val ids = mutableSetOf<String>()
                val byName = mutableMapOf<String, String>()
                val usersAsFriends = mutableListOf<FollowingFriend>()
                snap.documents.forEach { doc ->
                    val uid = doc.id.trim()
                    if (uid.isEmpty() || uid == safeUid) return@forEach
                    ids.add(uid)
                    val data = doc.data.orEmpty()
                    listOf("displayName", "username", "name").forEach { key ->
                        val raw = (data[key] as? String)?.trim().orEmpty()
                        if (raw.isNotEmpty()) {
                            byName.putIfAbsent(raw.lowercase(Locale.ROOT), uid)
                        }
                    }
                    followingFriendFrom(uid, data)?.let { friend ->
                        if (friend.uid != safeUid) {
                            usersAsFriends.add(friend.copy(uid = uid))
                        }
                    }
                }
                knownUserIds = ids
                canonicalUsersByName = byName
                loadedUsers = usersAsFriends
                publish()
            }
            .addOnFailureListener { error ->
                Log.e(TAG_VENUE_REPOSITORY, "Gagal memuat daftar pengguna dari koleksi users: ${error.message}", error)
            }
    } catch (error: Exception) {
        Log.e(TAG_VENUE_REPOSITORY, "Exception saat memuat koleksi users: ${error.message}", error)
    }

    // 2. Muat daftar akun diikuti dari helper yang sama dengan ChatScreen.kt.
    try {
        loadFirebaseFriends(
            uid = currentUserId,
            currentUserName = safeName,
            onResult = { friends ->
                loadedFriends = friends
                publish()
            },
            onError = { message ->
                Log.e(TAG_VENUE_REPOSITORY, "Gagal memuat daftar teman: $message")
            },
        )
    } catch (error: Exception) {
        Log.e(TAG_VENUE_REPOSITORY, "Exception saat memuat daftar teman: ${error.message}", error)
    }

    // 3. Muat daftar ruang chat aktif (DM & grup komunitas) dari koleksi `chats` yang sama dengan ChatScreen.kt.
    try {
        db.collection(FIRESTORE_CHATS_COLLECTION)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(50)
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
            .addOnFailureListener { error ->
                Log.e(TAG_VENUE_REPOSITORY, "Gagal memuat daftar ruang chat: ${error.message}", error)
            }
    } catch (error: Exception) {
        Log.e(TAG_VENUE_REPOSITORY, "Exception saat memuat daftar ruang chat: ${error.message}", error)
    }
}

/**
 * Menggabungkan ruang obrolan aktif (DM & grup) dengan daftar akun teman diikuti,
 * memastikan setiap penerima DM memakai UID pengguna asli serta `roomId = directChatRoomId(...)`,
 * lalu menghapus duplikasi agar setiap penerima tampil sekali di daftar "Bagikan ke:".
 */
fun mergeShareRecipients(
    chatRooms: List<ChatRoomUiModel>,
    friends: List<FollowingFriend>,
    currentUserId: String,
    currentUserName: String,
    canonicalUsersByName: Map<String, String> = emptyMap(),
    knownUserIds: Set<String> = emptySet(),
): List<ShareRecipient> {
    val result = mutableListOf<ShareRecipient>()
    val seenRoomIds = mutableSetOf<String>()
    val seenRecipientIds = mutableSetOf<String>()
    val seenDirectNames = mutableSetOf<String>()

    val prioritizedFriends = friends.sortedBy { friend ->
        val uid = friend.uid.trim()
        val name = friend.displayName.trim()
        when {
            uid in knownUserIds -> 0
            isValidChatUserUid(uid, currentUserId) && !uid.equals(name, ignoreCase = true) -> 1
            isValidChatUserUid(uid, currentUserId) -> 2
            else -> 3
        }
    }

    val friendUidByName = buildMap {
        prioritizedFriends.forEach { friend ->
            val cleanName = friend.displayName.trim()
            val cleanUid = friend.uid.trim()
            if (cleanName.isNotEmpty() && isValidChatUserUid(cleanUid, currentUserId)) {
                val lower = cleanName.lowercase(Locale.ROOT)
                if (knownUserIds.isEmpty() || cleanUid in knownUserIds || !cleanUid.equals(cleanName, ignoreCase = true)) {
                    putIfAbsent(lower, cleanUid)
                }
            }
        }
    }

    fun resolveDirectRoomTargetUid(room: ChatRoomUiModel): String? {
        val validParticipants = room.participantIds
            .map { it.trim() }
            .filter { isValidChatUserUid(it, currentUserId) }

        if (knownUserIds.isNotEmpty()) {
            validParticipants.firstOrNull { it in knownUserIds }?.let { return it }
        }
        val lowerName = room.name.trim().lowercase(Locale.ROOT)
        canonicalUsersByName[lowerName]
            ?.takeIf { isValidChatUserUid(it, currentUserId) }
            ?.let { return it }
        validParticipants.firstOrNull { !it.equals(room.name.trim(), ignoreCase = true) }
            ?.let { return it }
        friendUidByName[lowerName]?.let { return it }
        return validParticipants.firstOrNull()
    }

    chatRooms.forEach { room ->
        val cleanRoomName = room.name.trim()
        if (room.id.isBlank() || cleanRoomName.isEmpty()) return@forEach

        if (room.isGroup) {
            val groupRoomId = room.id.trim()
            if (!seenRoomIds.add(groupRoomId) || !seenRecipientIds.add(groupRoomId)) return@forEach
            result.add(
                ShareRecipient(
                    id = groupRoomId,
                    name = cleanRoomName,
                    isGroup = true,
                    roomId = groupRoomId,
                    avatarColorHex = 0xFFE05A47L,
                    participantIds = room.participantIds.filter { it.isNotBlank() }.distinct(),
                    participantNames = room.participantNames.filter { it.isNotBlank() }.distinct(),
                ),
            )
            return@forEach
        }

        val targetUid = resolveDirectRoomTargetUid(room) ?: return@forEach
        val canonicalRoomId = directChatRoomId(currentUserId, targetUid)
        val lowerName = cleanRoomName.lowercase(Locale.ROOT)
        if (canonicalRoomId in seenRoomIds || targetUid in seenRecipientIds || lowerName in seenDirectNames) {
            return@forEach
        }

        seenRoomIds.add(canonicalRoomId)
        seenRecipientIds.add(targetUid)
        seenDirectNames.add(lowerName)

        val cleanParticipantIds = (
            room.participantIds.filter { isValidChatUserUid(it) } +
                listOf(currentUserId, targetUid)
            )
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

        val cleanParticipantNames = (
            room.participantNames + listOf(currentUserName, cleanRoomName)
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
                avatarColorHex = pickRecipientAvatarColor(cleanRoomName),
                participantIds = cleanParticipantIds,
                participantNames = cleanParticipantNames,
            ),
        )
    }

    prioritizedFriends.forEach { friend ->
        val cleanName = friend.displayName.trim()
        if (cleanName.isEmpty()) return@forEach
        val lowerName = cleanName.lowercase(Locale.ROOT)

        val rawUid = friend.uid.trim()
        val resolvedUid = when {
            rawUid in knownUserIds -> rawUid
            canonicalUsersByName[lowerName] != null -> canonicalUsersByName.getValue(lowerName)
            else -> rawUid
        }
        if (!isValidChatUserUid(resolvedUid, currentUserId)) return@forEach
        if (knownUserIds.isNotEmpty() &&
            resolvedUid !in knownUserIds &&
            resolvedUid.equals(cleanName, ignoreCase = true)
        ) {
            return@forEach
        }

        val dmId = directChatRoomId(currentUserId, resolvedUid)
        if (dmId in seenRoomIds || resolvedUid in seenRecipientIds || lowerName in seenDirectNames) {
            return@forEach
        }

        seenRoomIds.add(dmId)
        seenRecipientIds.add(resolvedUid)
        seenDirectNames.add(lowerName)
        result.add(
            ShareRecipient(
                id = resolvedUid,
                name = cleanName,
                isGroup = false,
                roomId = dmId,
                avatarUrl = friend.photoUrl,
                avatarColorHex = pickRecipientAvatarColor(cleanName),
                participantIds = listOf(currentUserId, resolvedUid).filter { it.isNotBlank() }.distinct(),
                participantNames = listOf(currentUserName, cleanName)
                    .filter { it.isNotBlank() && !it.equals("Anda", ignoreCase = true) }
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
