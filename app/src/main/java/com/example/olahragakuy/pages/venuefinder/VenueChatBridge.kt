package com.example.olahragakuy.pages.venuefinder

import android.util.Log
import com.example.olahragakuy.pages.posts.CHAT_COLLECTION
import com.example.olahragakuy.pages.posts.CHAT_MESSAGES_SUBCOLLECTION
import com.example.olahragakuy.pages.posts.ChatMessageUiModel
import com.example.olahragakuy.pages.posts.ChatRoomUiModel
import com.example.olahragakuy.pages.posts.LocalChatMessages
import com.example.olahragakuy.pages.posts.LocalChatRooms
import com.example.olahragakuy.pages.posts.VenueInviteUiModel
import com.example.olahragakuy.pages.posts.buildChatMessageFirestorePayload
import com.example.olahragakuy.pages.posts.buildChatRoomFirestorePayload
import com.example.olahragakuy.pages.posts.buildVenueInvitePayload
import com.example.olahragakuy.pages.posts.canonicalChatRoomId
import com.example.olahragakuy.pages.posts.ensureDirectChatRoom
import com.example.olahragakuy.pages.posts.isValidChatUserUid
import com.example.olahragakuy.pages.posts.parseVenueInvitePayload
import com.example.olahragakuy.pages.posts.sanitizeChatParticipantIds
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val TAG_VENUE_CHAT_BRIDGE = "VenueChatBridge"

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
 * Jembatan antara fitur Venue Finder ("Ajak Teman") dan alur obrolan proyek
 * (`LocalChatRooms`, `LocalChatMessages`, serta koleksi kanonis Firestore
 * [CHAT_COLLECTION]/[CHAT_MESSAGES_SUBCOLLECTION] yang bersumber tunggal di `ChatScreen.kt`).
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
     * 1. Menyiapkan/memperbarui [ChatRoomUiModel] di [LocalChatRooms] dengan `roomId` kanonis.
     * 2. Menambahkan [ChatMessageUiModel] dengan [VenueInviteUiModel] ke [LocalChatMessages].
     * 3. Menyinkronkan dokumen ruang obrolan & pesan ke Firestore memakai builder tunggal `ChatScreen.kt`
     *    (termasuk field map `venueInvite` langsung sesuai `firestore.rules`).
     */
    fun sendVenueInvite(
        venue: VenueUiModel,
        recipient: ShareRecipient,
        currentUserId: String,
        currentUserName: String,
        syncToFirestore: Boolean = true,
        nowMillis: Long = System.currentTimeMillis(),
    ): VenueChatTarget {
        val activeAuthUid = resolveActiveFirebaseUid()
        val safeUserId: String = when {
            activeAuthUid != null && isValidChatUserUid(activeAuthUid) -> activeAuthUid
            isValidChatUserUid(currentUserId) -> currentUserId.trim()
            else -> currentUserId.trim().ifEmpty { "me" }
        }
        val safeUserName = currentUserName.trim().ifEmpty { "Anda" }
        val safeRecipientName = recipient.name.trim().ifEmpty { "Teman" }
        val resolvedTargetUid = resolveDirectRecipientUid(recipient, safeUserId)

        val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))
        val dateLabel = SimpleDateFormat("d/M", Locale.getDefault()).format(Date(nowMillis))

        val resolvedRoom = resolveOrCreateTargetRoom(
            recipient = recipient,
            resolvedTargetUid = resolvedTargetUid,
            recipientName = safeRecipientName,
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
        val inviteText = buildVenueInviteFallbackText(venue)
        val messageId = "venue_invite_${nowMillis}_${venue.id.take(12)}"

        val newMessage = ChatMessageUiModel(
            id = messageId,
            senderId = safeUserId,
            senderName = if (resolvedRoom.isGroup) "Anda" else safeUserName,
            text = inviteText,
            timeText = timeText,
            isFromMe = true,
            venueInvite = inviteUi,
            createdAtMillis = nowMillis,
        )

        val existingMessages = LocalChatMessages[resolvedRoom.id].orEmpty()
        LocalChatMessages[resolvedRoom.id] = (existingMessages + newMessage).distinctBy { it.id }

        val previewText = if (resolvedRoom.isGroup) {
            "$safeUserName mengajak olahraga di ${venue.name}"
        } else {
            "Anda mengirim ajakan olahraga bersama"
        }

        val mergedParticipantIds = sanitizeChatParticipantIds(
            resolvedRoom.participantIds +
                recipient.participantIds +
                listOfNotNull(
                    safeUserId,
                    resolvedTargetUid.takeUnless { recipient.isGroup },
                ),
        )

        val mergedParticipantNames = (
            resolvedRoom.participantNames +
                recipient.participantNames +
                listOf(safeUserName, safeRecipientName)
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
            recipientId = if (updatedRoom.isGroup) updatedRoom.id else (resolvedTargetUid ?: recipient.id),
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
                senderName = safeUserName,
                inviteText = inviteText,
                timeText = timeText,
                venue = venue,
                shortDescription = shortDesc,
            )
        }

        return target
    }

    private fun resolveActiveFirebaseUid(): String? = try {
        FirebaseAuth.getInstance().currentUser?.uid?.trim()?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) {
        null
    }

    private fun resolveDirectRecipientUid(
        recipient: ShareRecipient,
        currentUserId: String,
    ): String? {
        if (recipient.isGroup) return null
        val directId = recipient.id.trim()
        if (isValidChatUserUid(directId) && directId != currentUserId) {
            return directId
        }
        return sanitizeChatParticipantIds(recipient.participantIds)
            .firstOrNull { it != currentUserId }
    }

    private fun resolveOrCreateTargetRoom(
        recipient: ShareRecipient,
        resolvedTargetUid: String?,
        recipientName: String,
        currentUserId: String,
        currentUserName: String,
    ): ChatRoomUiModel {
        val roomId = canonicalChatRoomId(
            isGroup = recipient.isGroup,
            currentUserId = currentUserId,
            targetId = resolvedTargetUid ?: recipient.id,
            existingRoomId = recipient.roomId,
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

        val cleanParticipants = sanitizeChatParticipantIds(
            recipient.participantIds + currentUserId,
        )

        return ChatRoomUiModel(
            id = roomId,
            name = recipientName,
            isGroup = recipient.isGroup,
            statusOrMembers = statusOrMembers,
            lastMessage = "",
            dateLabel = "",
            avatarColorHex = recipient.avatarColorHex,
            participantIds = cleanParticipants,
            participantNames = recipient.participantNames,
        )
    }

    private fun persistInviteToFirestore(
        room: ChatRoomUiModel,
        messageId: String,
        senderName: String,
        inviteText: String,
        timeText: String,
        venue: VenueUiModel,
        shortDescription: String,
    ) {
        val authUid = try {
            FirebaseAuth.getInstance().currentUser?.uid?.trim().orEmpty()
        } catch (error: Exception) {
            Log.e(TAG_VENUE_CHAT_BRIDGE, "Gagal membaca FirebaseAuth currentUser: ${error.message}", error)
            ""
        }
        if (!isValidChatUserUid(authUid)) {
            Log.e(
                TAG_VENUE_CHAT_BRIDGE,
                "Batal mengirim ajakan venue ke Firestore: user FirebaseAuth belum login atau UID tidak valid.",
            )
            return
        }

        try {
            val db = FirebaseFirestore.getInstance()
            val chatRef = db.collection(CHAT_COLLECTION).document(room.id)
            val roomPayload = buildChatRoomFirestorePayload(
                name = room.name,
                isGroup = room.isGroup,
                statusOrMembers = room.statusOrMembers,
                lastMessage = room.lastMessage,
                dateLabel = room.dateLabel,
                participantIds = room.participantIds + authUid,
                participantNames = room.participantNames + senderName,
            )
            chatRef.set(roomPayload, SetOptions.merge())
                .addOnFailureListener { error ->
                    Log.e(
                        TAG_VENUE_CHAT_BRIDGE,
                        "Gagal menyimpan metadata ruang chat room=${room.id}: ${error.message}",
                        error,
                    )
                }

            val messagePayload = buildVenueInviteFirestorePayload(
                senderId = authUid,
                senderName = senderName,
                fallbackText = inviteText,
                timeText = timeText,
                venue = venue,
                shortDescription = shortDescription,
            )
            chatRef.collection(CHAT_MESSAGES_SUBCOLLECTION)
                .document(messageId)
                .set(messagePayload)
                .addOnFailureListener { error ->
                    Log.e(
                        TAG_VENUE_CHAT_BRIDGE,
                        "Gagal mengirim pesan ajakan venue room=${room.id} msg=$messageId: ${error.message}",
                        error,
                    )
                }
        } catch (error: Exception) {
            Log.e(
                TAG_VENUE_CHAT_BRIDGE,
                "Exception saat sinkronisasi ajakan venue room=${room.id}: ${error.message}",
                error,
            )
        }
    }
}

/**
 * Membentuk teks ajakan olahraga yang ringkas dan ramah untuk field `text` pesan Firestore.
 */
fun buildVenueInviteFallbackText(venue: VenueUiModel): String {
    val category = venue.sportCategory.trim().ifEmpty { "Olahraga" }
    val shareLink = buildVenueShareLink(venue)
    return "Yuk olahraga bareng di ${venue.name} ($category)! $shareLink"
}

/**
 * Menyusun payload dokumen Firestore `chats/{roomId}/messages/{messageId}` memakai builder
 * tunggal dari `ChatScreen.kt` beserta field map `venueInvite` sesuai `firestore.rules`.
 */
fun buildVenueInviteFirestorePayload(
    senderId: String,
    senderName: String,
    fallbackText: String,
    timeText: String,
    venue: VenueUiModel,
    shortDescription: String = buildShortDescription(venue.shortDescription, venue.description),
    createdAt: Any = FieldValue.serverTimestamp(),
): Map<String, Any> = buildChatMessageFirestorePayload(
    senderId = senderId,
    senderName = senderName,
    text = fallbackText,
    timeText = timeText,
    venueInvite = buildVenueInvitePayload(
        venueId = venue.id,
        title = venue.name,
        sport = venue.sportCategory,
        description = shortDescription,
        imageUrl = venue.imageUrl,
        latitude = venue.latitude,
        longitude = venue.longitude,
    ),
    createdAt = createdAt,
)

/**
 * Parser defensif untuk membaca field map `venueInvite` dari dokumen pesan Firestore menjadi
 * [VenueInviteUiModel] dengan mendelegasikan ke [parseVenueInvitePayload].
 */
fun parseVenueInviteMap(raw: Any?): VenueInviteUiModel? = parseVenueInvitePayload(raw)
