package com.example.olahragakuy.pages.venuefinder

import android.util.Log
import com.example.olahragakuy.pages.posts.ChatMessageUiModel
import com.example.olahragakuy.pages.posts.ChatRoomUiModel
import com.example.olahragakuy.pages.posts.LocalChatMessages
import com.example.olahragakuy.pages.posts.LocalChatRooms
import com.example.olahragakuy.pages.posts.VenueInviteUiModel
import com.example.olahragakuy.pages.posts.directChatRoomId
import com.example.olahragakuy.pages.posts.ensureDirectChatRoom
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
     * 1. Menyiapkan/memperbarui [ChatRoomUiModel] di [LocalChatRooms].
     * 2. Menambahkan [ChatMessageUiModel] dengan [VenueInviteUiModel] ke [LocalChatMessages].
     * 3. Menyinkronkan dokumen ruang obrolan & pesan ke Firestore mengikuti skema `ChatScreen.kt`.
     */
    fun sendVenueInvite(
        venue: VenueUiModel,
        recipient: ShareRecipient,
        currentUserId: String,
        currentUserName: String,
        syncToFirestore: Boolean = true,
        nowMillis: Long = System.currentTimeMillis(),
    ): VenueChatTarget {
        val safeUserId = currentUserId.trim().ifEmpty { "me" }
        val safeUserName = currentUserName.trim().ifEmpty { "Anda" }
        val safeRecipientName = recipient.name.trim().ifEmpty { "Teman" }

        val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(nowMillis))
        val dateLabel = SimpleDateFormat("d/M", Locale.getDefault()).format(Date(nowMillis))

        val resolvedRoom = resolveOrCreateTargetRoom(
            recipient = recipient,
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
        val fallbackText = buildVenueInviteFallbackText(venue)
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

        val previewText = if (resolvedRoom.isGroup) {
            "$safeUserName mengajak olahraga di ${venue.name}"
        } else {
            "Anda mengirim ajakan olahraga bersama"
        }

        val mergedParticipantIds = (
            resolvedRoom.participantIds +
                recipient.participantIds +
                listOfNotNull(safeUserId, recipient.id.takeUnless { recipient.isGroup })
            )
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()

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
            recipientId = recipient.id,
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

    private fun resolveOrCreateTargetRoom(
        recipient: ShareRecipient,
        recipientName: String,
        currentUserId: String,
        currentUserName: String,
    ): ChatRoomUiModel {
        val explicitRoomId = recipient.roomId?.trim().orEmpty()
        val roomId = when {
            explicitRoomId.isNotEmpty() -> explicitRoomId
            recipient.isGroup -> recipient.id.trim().ifEmpty { "group_${System.currentTimeMillis()}" }
            else -> directChatRoomId(currentUserId, recipient.id)
        }

        val existing = LocalChatRooms.firstOrNull { it.id == roomId }
        if (existing != null) return existing

        if (!recipient.isGroup && explicitRoomId.isEmpty()) {
            return ensureDirectChatRoom(
                currentUserId = currentUserId,
                currentUserName = currentUserName,
                targetUid = recipient.id,
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
            val db = FirebaseFirestore.getInstance()
            val chatRef = db.collection("chats").document(room.id)

            chatRef.set(
                mapOf(
                    "name" to room.name,
                    "isGroup" to room.isGroup,
                    "statusOrMembers" to room.statusOrMembers,
                    "lastMessage" to room.lastMessage,
                    "dateLabel" to room.dateLabel,
                    "participantIds" to room.participantIds,
                    "participantNames" to room.participantNames,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            ).addOnFailureListener { error ->
                Log.e(TAG_VENUE_CHAT_BRIDGE, "Gagal menyimpan metadata ruang chat: ${error.message}", error)
            }

            val messagePayload = buildVenueInviteFirestorePayload(
                senderId = senderId,
                senderName = senderName,
                fallbackText = fallbackText,
                timeText = timeText,
                venue = venue,
                shortDescription = shortDescription,
            )
            chatRef.collection("messages").document(messageId).set(messagePayload)
                .addOnFailureListener { error ->
                    Log.e(TAG_VENUE_CHAT_BRIDGE, "Gagal mengirim pesan ajakan venue: ${error.message}", error)
                }
        } catch (error: Exception) {
            Log.e(TAG_VENUE_CHAT_BRIDGE, "Exception saat sinkronisasi ajakan venue: ${error.message}", error)
        }
    }
}

/**
 * Membentuk teks ajakan olahraga yang ringkas dan ramah sebagai teks cadangan pesan Firestore.
 */
fun buildVenueInviteFallbackText(venue: VenueUiModel): String {
    val category = venue.sportCategory.trim().ifEmpty { "Olahraga" }
    val shareLink = buildVenueShareLink(venue)
    return "Yuk olahraga bareng di ${venue.name} ($category)! $shareLink"
}

/**
 * Menyusun payload dokumen Firestore `chats/{roomId}/messages/{messageId}` agar selaras
 * dengan skema `ChatScreen.kt` sekaligus membawa metadata `venueInvite`.
 */
fun buildVenueInviteFirestorePayload(
    senderId: String,
    senderName: String,
    fallbackText: String,
    timeText: String,
    venue: VenueUiModel,
    shortDescription: String = buildShortDescription(venue.shortDescription, venue.description),
): Map<String, Any> = mapOf(
    "senderId" to senderId,
    "senderName" to senderName,
    "text" to fallbackText,
    "timeText" to timeText,
    "venueInvite" to mapOf(
        "venueId" to venue.id,
        "title" to venue.name,
        "sport" to venue.sportCategory,
        "description" to shortDescription,
        "imageUrl" to venue.imageUrl,
        "latitude" to venue.latitude,
        "longitude" to venue.longitude,
    ),
    "createdAt" to FieldValue.serverTimestamp(),
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
