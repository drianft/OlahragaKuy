package com.example.olahragakuy.pages.venuefinder

import android.util.Log
import com.example.olahragakuy.pages.posts.ChatMessageUiModel
import com.example.olahragakuy.pages.posts.ChatRoomUiModel
import com.example.olahragakuy.pages.posts.FIRESTORE_CHATS_COLLECTION
import com.example.olahragakuy.pages.posts.FIRESTORE_CHAT_MESSAGES_SUBCOLLECTION
import com.example.olahragakuy.pages.posts.LocalChatMessages
import com.example.olahragakuy.pages.posts.LocalChatRooms
import com.example.olahragakuy.pages.posts.VenueInviteUiModel
import com.example.olahragakuy.pages.posts.buildChatMessageFirestorePayload
import com.example.olahragakuy.pages.posts.buildChatRoomFirestorePayload
import com.example.olahragakuy.pages.posts.currentUserOrNull
import com.example.olahragakuy.pages.posts.ensureDirectChatRoom
import com.example.olahragakuy.pages.posts.isValidChatUserUid
import com.example.olahragakuy.pages.posts.resolveChatRoomId
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG_VENUE_CHAT_BRIDGE = "VenueChatBridge"

private val VENUE_INVITE_HEADER_REGEX = Regex(
    "^Yuk olahraga bareng di (.+?) \\(([^()]+)\\)!(?:\\s+(https?://\\S+))?$",
)

/**
 * Representasi target obrolan hasil pengiriman ajakan venue dari fitur "Ajak Teman".
 *
 * Objek ini dapat diteruskan ke layer navigasi untuk membuka langsung ruang obrolan personal (DM)
 * maupun grup komunitas yang relevan tanpa mengubah struktur data `ChatScreen.kt`.
 */
data class VenueChatTarget(
    val roomId: String,
    val room: ChatRoomUiModel,
    val recipientId: String,
    val recipientName: String,
    val isGroup: Boolean,
    val messageId: String,
    val venue: VenueUiModel,
)

/**
 * Jembatan ringan antara fitur Venue Finder ("Ajak Teman") dan alur obrolan proyek
 * (`LocalChatRooms`, `LocalChatMessages`, serta koleksi Firestore `chats/{roomId}/messages`).
 */
object VenueChatBridge {

    @Volatile
    private var pendingTarget: VenueChatTarget? = null

    /** Menyimpan target chat terbaru yang dipicu dari Venue Finder agar bisa dibaca alur navigasi. */
    fun setPendingTarget(target: VenueChatTarget?) {
        pendingTarget = target
    }

    /** Melihat target chat tertunda tanpa menghapusnya. */
    fun peekPendingTarget(): VenueChatTarget? = pendingTarget

    /** Mengambil sekaligus mengosongkan target chat tertunda. */
    fun consumePendingTarget(): VenueChatTarget? {
        val current = pendingTarget
        pendingTarget = null
        return current
    }

    /**
     * Mengirim pesan ajakan venue ke target kontak (DM) atau grup komunitas:
     * 1. Menyiapkan/memperbarui [ChatRoomUiModel] di [LocalChatRooms] dengan helper `roomId` yang sama seperti `ChatScreen.kt`.
     * 2. Menambahkan [ChatMessageUiModel] dengan [VenueInviteUiModel] ke [LocalChatMessages].
     * 3. Menyinkronkan dokumen ruang obrolan & pesan ke Firestore memakai payload builder yang sama dengan `ChatScreen.kt`.
     */
    fun sendVenueInvite(
        venue: VenueUiModel,
        recipient: ShareRecipient,
        currentUserId: String,
        currentUserName: String,
        syncToFirestore: Boolean = true,
        nowMillis: Long = System.currentTimeMillis(),
    ): VenueChatTarget {
        val authUid = if (syncToFirestore) currentUserOrNull()?.uid?.trim().orEmpty() else ""
        val safeUserId = when {
            authUid.isNotEmpty() -> authUid
            else -> currentUserId.trim().ifEmpty { "me" }
        }
        val safeUserName = currentUserName.trim().ifEmpty { "Anda" }
        val safeRecipientName = recipient.name.trim().ifEmpty { "Teman" }

        val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))
        val dateLabel = SimpleDateFormat("d/M", Locale.getDefault()).format(Date(nowMillis))

        val resolvedTargetUid = if (recipient.isGroup) {
            null
        } else {
            resolveDirectTargetUid(recipient = recipient, currentUserId = safeUserId)
        }

        val resolvedRoom = resolveOrCreateTargetRoom(
            recipient = recipient,
            recipientName = safeRecipientName,
            resolvedTargetUid = resolvedTargetUid,
            currentUserId = safeUserId,
            currentUserName = safeUserName,
        )

        val shortDesc = buildShortDescription(
            shortDescription = venue.shortDescription,
            description = venue.description,
        )
        val inviteUi = VenueInviteUiModel(
            title = venue.name,
            sport = venue.sportCategory,
            description = shortDesc,
        )
        val fallbackText = buildVenueInviteFallbackText(venue = venue, shortDescription = shortDesc)
        val messageId = "venue_invite_${nowMillis}_${venue.id.take(12)}"

        val newMessage = ChatMessageUiModel(
            id = messageId,
            senderId = safeUserId,
            senderName = if (resolvedRoom.isGroup) "Anda" else safeUserName,
            text = fallbackText,
            timeText = timeText,
            isFromMe = true,
            venueInvite = inviteUi,
            createdAtMillis = nowMillis,
        )

        val existingMessages = LocalChatMessages[resolvedRoom.id].orEmpty()
        LocalChatMessages[resolvedRoom.id] = (existingMessages + newMessage).distinctBy { it.id }

        val previewText = "$safeUserName mengajak olahraga di ${venue.name}"

        val mergedParticipantIds = (
            resolvedRoom.participantIds +
                recipient.participantIds +
                listOfNotNull(safeUserId, resolvedTargetUid)
            )
            .map { it.trim() }
            .filter {
                it.isNotEmpty() &&
                    !it.startsWith("dm_") &&
                    !it.startsWith("group_") &&
                    !it.startsWith("author_")
            }
            .distinct()

        val mergedParticipantNames = (
            resolvedRoom.participantNames +
                recipient.participantNames +
                if (resolvedRoom.isGroup) {
                    listOf(safeUserName)
                } else {
                    listOf(safeUserName, safeRecipientName)
                }
            )
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals("Anda", ignoreCase = true) }
            .distinct()

        val updatedRoom = resolvedRoom.copy(
            lastMessage = previewText,
            dateLabel = dateLabel,
            participantIds = mergedParticipantIds,
            participantNames = mergedParticipantNames,
        )

        val existingIndex = LocalChatRooms.indexOfFirst { it.id == updatedRoom.id }
        if (existingIndex >= 0) {
            LocalChatRooms.removeAt(existingIndex)
        }
        LocalChatRooms.add(0, updatedRoom)

        val target = VenueChatTarget(
            roomId = updatedRoom.id,
            room = updatedRoom,
            recipientId = resolvedTargetUid ?: recipient.id,
            recipientName = safeRecipientName,
            isGroup = updatedRoom.isGroup,
            messageId = messageId,
            venue = venue,
        )
        pendingTarget = target

        if (syncToFirestore) {
            persistInviteToFirestore(
                room = updatedRoom,
                messageId = messageId,
                senderId = safeUserId,
                senderName = safeUserName,
                fallbackText = fallbackText,
                timeText = timeText,
                venue = venue,
                shortDescription = shortDesc,
            )
        }

        return target
    }

    private fun resolveDirectTargetUid(
        recipient: ShareRecipient,
        currentUserId: String,
    ): String? {
        if (isValidChatUserUid(recipient.id, currentUserId)) {
            return recipient.id.trim()
        }
        return recipient.participantIds
            .map { it.trim() }
            .firstOrNull { isValidChatUserUid(it, currentUserId) }
    }

    private fun resolveOrCreateTargetRoom(
        recipient: ShareRecipient,
        recipientName: String,
        resolvedTargetUid: String?,
        currentUserId: String,
        currentUserName: String,
    ): ChatRoomUiModel {
        val roomId = resolveChatRoomId(
            isGroup = recipient.isGroup,
            currentUserId = currentUserId,
            targetId = resolvedTargetUid ?: recipient.id,
            explicitRoomId = recipient.roomId,
        )

        val existing = LocalChatRooms.firstOrNull { it.id == roomId }
        if (existing != null) return existing

        if (!recipient.isGroup && resolvedTargetUid != null) {
            return ensureDirectChatRoom(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                targetUid = resolvedTargetUid,
                targetName = recipientName,
            )
        }

        val statusOrMembers = if (recipient.isGroup) {
            val others = recipient.participantNames.filterNot {
                it.equals(currentUserName, ignoreCase = true) || it.equals("Anda", ignoreCase = true)
            }
            (listOf("Anda") + others).distinct().joinToString(",")
        } else {
            "Aktif"
        }

        return ChatRoomUiModel(
            id = roomId,
            name = recipientName,
            isGroup = recipient.isGroup,
            statusOrMembers = statusOrMembers,
            lastMessage = "",
            dateLabel = "",
            avatarColorHex = recipient.avatarColorHex,
            participantIds = recipient.participantIds,
            participantNames = recipient.participantNames,
        )
    }

    private fun persistInviteToFirestore(
        room: ChatRoomUiModel,
        messageId: String,
        senderId: String,
        senderName: String,
        fallbackText: String,
        timeText: String,
        venue: VenueUiModel,
        shortDescription: String,
    ) {
        try {
            val authUid = currentUserOrNull()?.uid?.trim()
            if (authUid.isNullOrEmpty()) {
                Log.e(
                    TAG_VENUE_CHAT_BRIDGE,
                    "Pengguna belum login FirebaseAuth saat mengirim ajakan venue (roomId=${room.id}); Firestore kemungkinan menolak dengan PERMISSION_DENIED.",
                )
            } else if (authUid != senderId) {
                Log.e(
                    TAG_VENUE_CHAT_BRIDGE,
                    "Mismatch senderId ($senderId) != authUid ($authUid) untuk roomId=${room.id}.",
                )
            }

            val db = FirebaseFirestore.getInstance()
            val chatRef = db.collection(FIRESTORE_CHATS_COLLECTION).document(room.id)
            val roomPayload = buildChatRoomFirestorePayload(
                roomName = room.name,
                isGroup = room.isGroup,
                statusOrMembers = room.statusOrMembers,
                lastMessage = room.lastMessage,
                dateLabel = room.dateLabel,
                participantIds = room.participantIds,
                participantNames = room.participantNames,
            )

            chatRef.set(roomPayload, SetOptions.merge())
                .addOnFailureListener { error ->
                    Log.e(
                        TAG_VENUE_CHAT_BRIDGE,
                        "Gagal menyimpan metadata ruang chat (roomId=${room.id}): ${error.message}",
                        error,
                    )
                }

            val messagePayload = buildVenueInviteFirestorePayload(
                senderId = senderId,
                senderName = senderName,
                fallbackText = fallbackText,
                timeText = timeText,
                venue = venue,
                shortDescription = shortDescription,
            )
            val messageRef = chatRef
                .collection(FIRESTORE_CHAT_MESSAGES_SUBCOLLECTION)
                .document(messageId)

            messageRef.set(messagePayload)
                .addOnFailureListener { error ->
                    Log.e(
                        TAG_VENUE_CHAT_BRIDGE,
                        "Gagal mengirim pesan ajakan venue (roomId=${room.id}, messageId=$messageId): ${error.message}",
                        error,
                    )
                    val isPermissionDenied = (error as? FirebaseFirestoreException)?.code ==
                        FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                        error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
                    if (isPermissionDenied) {
                        Log.e(
                            TAG_VENUE_CHAT_BRIDGE,
                            "Firestore Rules menolak field 'venueInvite' (PERMISSION_DENIED). Mengirim ulang dengan 5 field standar agar pesan tetap tersimpan.",
                        )
                        val standardPayload = buildChatMessageFirestorePayload(
                            senderId = senderId,
                            senderName = senderName,
                            text = fallbackText,
                            timeText = timeText,
                            venueInvite = null,
                        )
                        messageRef.set(standardPayload)
                            .addOnFailureListener { fallbackError ->
                                Log.e(
                                    TAG_VENUE_CHAT_BRIDGE,
                                    "Fallback pengiriman ajakan venue juga gagal (roomId=${room.id}, messageId=$messageId): ${fallbackError.message}",
                                    fallbackError,
                                )
                            }
                    }
                }
        } catch (error: Exception) {
            Log.e(
                TAG_VENUE_CHAT_BRIDGE,
                "Exception saat sinkronisasi ajakan venue (roomId=${room.id}): ${error.message}",
                error,
            )
        }
    }
}

/**
 * Membentuk teks ajakan olahraga yang ringkas, ramah, sekaligus dapat di-parse ulang menjadi
 * [VenueInviteUiModel] apabila server Firestore masih memakai aturan 5 field standar.
 */
fun buildVenueInviteFallbackText(
    venue: VenueUiModel,
    shortDescription: String = buildShortDescription(venue.shortDescription, venue.description),
): String {
    val category = venue.sportCategory.trim().ifEmpty { "Olahraga" }
    val shareLink = buildVenueShareLink(venue)
    val cleanDesc = shortDescription.trim()
    return if (cleanDesc.isNotEmpty()) {
        "Yuk olahraga bareng di ${venue.name} ($category)!\n$cleanDesc\n$shareLink"
    } else {
        "Yuk olahraga bareng di ${venue.name} ($category)! $shareLink"
    }
}

/**
 * Menyusun payload dokumen Firestore `chats/{roomId}/messages/{messageId}` agar 100% selaras
 * dengan skema `ChatScreen.kt` sekaligus membawa metadata `venueInvite`.
 */
fun buildVenueInviteFirestorePayload(
    senderId: String,
    senderName: String,
    fallbackText: String,
    timeText: String,
    venue: VenueUiModel,
    shortDescription: String = buildShortDescription(venue.shortDescription, venue.description),
): Map<String, Any> = buildChatMessageFirestorePayload(
    senderId = senderId,
    senderName = senderName,
    text = fallbackText,
    timeText = timeText,
    venueInvite = mapOf(
        "venueId" to venue.id,
        "title" to venue.name,
        "sport" to venue.sportCategory,
        "description" to shortDescription,
        "imageUrl" to venue.imageUrl,
        "latitude" to venue.latitude,
        "longitude" to venue.longitude,
    ),
)

/**
 * Parser defensif untuk membaca field `venueInvite` dari dokumen pesan Firestore menjadi
 * [VenueInviteUiModel] saat dibutuhkan oleh layar obrolan.
 */
fun parseVenueInviteMap(raw: Any?): VenueInviteUiModel? {
    val map = raw as? Map<*, *> ?: return null
    val title = (map["title"] as? String)?.trim().orEmpty()
    if (title.isEmpty()) return null
    val sport = (map["sport"] as? String)?.trim().orEmpty().ifEmpty { "Olahraga" }
    val description = (map["description"] as? String)?.trim().orEmpty()
    return VenueInviteUiModel(
        title = title,
        sport = sport,
        description = description,
    )
}

/**
 * Parser cadangan untuk membaca pesan undangan venue dari `text` apabila dokumen ditulis
 * memakai fallback 5 field standar (`senderId`, `senderName`, `text`, `timeText`, `createdAt`).
 */
fun parseVenueInviteFromFallbackText(text: String?): VenueInviteUiModel? {
    val raw = text?.trim().orEmpty()
    if (!raw.startsWith("Yuk olahraga bareng di ")) return null
    val lines = raw.lines().map { it.trim() }.filter { it.isNotEmpty() }
    val firstLine = lines.firstOrNull() ?: return null
    val match = VENUE_INVITE_HEADER_REGEX.matchEntire(firstLine) ?: return null
    val title = match.groupValues[1].trim()
    if (title.isEmpty()) return null
    val sport = match.groupValues[2].trim().ifEmpty { "Olahraga" }
    val description = lines
        .drop(1)
        .filterNot { it.startsWith("http://") || it.startsWith("https://") }
        .joinToString("\n")
        .trim()
    return VenueInviteUiModel(
        title = title,
        sport = sport,
        description = description,
    )
}
