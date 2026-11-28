package com.example.olahragakuy.pages.posts

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import com.example.olahragakuy.ui.components.RefreshRegistry
import com.example.olahragakuy.ui.components.addRefreshableSnapshotListener
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query

private const val TAG_REALTIME_BADGE = "RealtimeBadgeTracker"
private const val BADGE_PREFS_NAME = "olahragakuy_realtime_badges_prefs"
private const val KEY_PREFIX_NOTIF_SEEN = "notif_seen_"
private const val KEY_PREFIX_CHAT_READ = "chat_read_"
private const val KEY_NOTIF_PERMISSION_PROMPTED = "notif_permission_prompted"
private const val MAX_OBSERVED_CHAT_ROOMS = 30
private const val MAX_OBSERVED_MESSAGES_PER_ROOM = 50L
private const val MAX_OBSERVED_NOTIFICATIONS = 50L

data class BadgeChatMessageMeta(
    val id: String,
    val senderId: String,
    val createdAtMillis: Long,
    val senderName: String = "",
    val text: String = "",
    val venueInviteTitle: String? = null,
)

data class BadgeNotificationMeta(
    val id: String,
    val actorId: String,
    val createdAtMillis: Long,
    val actorName: String = "",
    val postId: String = "",
    val type: NotificationType = NotificationType.LIKE,
)

private data class ObservedChatRoomMeta(
    val roomId: String,
    val displayName: String,
    val isGroup: Boolean,
)

/**
 * Memformat angka badge untuk UI: mengembalikan `null` jika `<= 0`, angka langsung untuk `1..99`,
 * dan `"99+"` jika lebih dari 99.
 */
fun formatBadgeCount(count: Int): String? = when {
    count <= 0 -> null
    count > 99 -> "99+"
    else -> count.toString()
}

/**
 * Menghitung jumlah pesan belum dibaca pada satu ruang chat.
 *
 * - Pesan milik diri sendiri (`senderId == currentUserId`) tidak dihitung sebagai belum dibaca.
 * - Jika pengguna sudah pernah membalas di ruang tersebut (termasuk dari perangkat lain),
 *   pesan yang muncul sebelum atau bersamaan dengan balasan terakhir pengguna dianggap sudah dibaca.
 */
fun countUnreadChatMessages(
    messages: List<BadgeChatMessageMeta>,
    currentUserId: String,
    persistedLastReadMillis: Long,
): Int {
    val cleanUid = currentUserId.trim()
    if (cleanUid.isEmpty() || messages.isEmpty()) return 0

    val latestMyMessageMillis = messages
        .asSequence()
        .filter { it.senderId.trim() == cleanUid }
        .maxOfOrNull { it.createdAtMillis }
        ?: 0L

    val effectiveLastReadMillis = maxOf(
        persistedLastReadMillis.coerceAtLeast(0L),
        latestMyMessageMillis.coerceAtLeast(0L),
    )

    return messages.count { msg ->
        val sender = msg.senderId.trim()
        sender.isNotEmpty() &&
            sender != cleanUid &&
            msg.createdAtMillis > effectiveLastReadMillis
    }
}

/**
 * Menghitung jumlah notifikasi yang belum dilihat pengguna.
 * Notifikasi yang dipicu oleh diri sendiri (`actorId == currentUserId`) tidak dihitung.
 */
fun countUnreadNotifications(
    notifications: List<BadgeNotificationMeta>,
    currentUserId: String,
    lastSeenMillis: Long,
): Int {
    val cleanUid = currentUserId.trim()
    if (cleanUid.isEmpty() || notifications.isEmpty()) return 0

    val safeLastSeen = lastSeenMillis.coerceAtLeast(0L)
    return notifications.count { notif ->
        val actor = notif.actorId.trim()
        actor != cleanUid && notif.createdAtMillis > safeLastSeen
    }
}

fun serializeReadTimestamps(map: Map<String, Long>): String {
    return map.entries
        .asSequence()
        .mapNotNull { (roomId, millis) ->
            val cleanId = roomId.trim().replace("\n", "").replace("\r", "")
            if (cleanId.isNotEmpty() && millis > 0L) "$cleanId=$millis" else null
        }
        .joinToString(separator = "\n")
}

fun parseReadTimestamps(raw: String?): Map<String, Long> {
    if (raw.isNullOrBlank()) return emptyMap()
    val result = linkedMapOf<String, Long>()
    raw.lineSequence().forEach { line ->
        val sep = line.lastIndexOf('=')
        if (sep > 0) {
            val key = line.substring(0, sep).trim()
            val value = line.substring(sep + 1).trim().toLongOrNull() ?: 0L
            if (key.isNotEmpty() && value > 0L) {
                result[key] = value
            }
        }
    }
    return result
}

/**
 * Penyimpanan lokal status "sudah dibaca/dilihat" per akun (`uid`) menggunakan [SharedPreferences].
 */
object RealtimeBadgePrefs {
    private fun prefs(context: Context): SharedPreferences =
        context.applicationContext.getSharedPreferences(BADGE_PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun loadLastSeenNotificationMillis(context: Context, userId: String): Long {
        val cleanUid = userId.trim()
        if (cleanUid.isEmpty()) return 0L
        return prefs(context).getLong(KEY_PREFIX_NOTIF_SEEN + cleanUid, 0L).coerceAtLeast(0L)
    }

    @Synchronized
    fun saveLastSeenNotificationMillis(context: Context, userId: String, millis: Long) {
        val cleanUid = userId.trim()
        if (cleanUid.isEmpty()) return
        val safeMillis = millis.coerceAtLeast(0L)
        val sp = prefs(context)
        val previous = sp.getLong(KEY_PREFIX_NOTIF_SEEN + cleanUid, 0L)
        if (safeMillis > previous) {
            sp.edit { putLong(KEY_PREFIX_NOTIF_SEEN + cleanUid, safeMillis) }
        }
    }

    @Synchronized
    fun loadChatReadMap(context: Context, userId: String): Map<String, Long> {
        val cleanUid = userId.trim()
        if (cleanUid.isEmpty()) return emptyMap()
        return parseReadTimestamps(prefs(context).getString(KEY_PREFIX_CHAT_READ + cleanUid, null))
    }

    @Synchronized
    fun saveChatReadMap(context: Context, userId: String, map: Map<String, Long>) {
        val cleanUid = userId.trim()
        if (cleanUid.isEmpty()) return
        prefs(context).edit {
            putString(KEY_PREFIX_CHAT_READ + cleanUid, serializeReadTimestamps(map))
        }
    }

    @Synchronized
    fun hasRequestedNotificationPermission(context: Context): Boolean {
        return prefs(context).getBoolean(KEY_NOTIF_PERMISSION_PROMPTED, false)
    }

    @Synchronized
    fun markNotificationPermissionRequested(context: Context) {
        prefs(context).edit { putBoolean(KEY_NOTIF_PERMISSION_PROMPTED, true) }
    }
}

/**
 * Store reaktif untuk menghitung dan menyinkronkan jumlah badge belum dibaca
 * (Notifikasi & Chat) secara realtime khusus untuk akun yang sedang login.
 */
object RealtimeBadgeStore {
    var activeUserId by mutableStateOf<String?>(null)
        private set

    var unreadNotificationCount by mutableIntStateOf(0)
        private set

    var unreadChatCount by mutableIntStateOf(0)
        private set

    var unreadChatCountsByRoom by mutableStateOf<Map<String, Int>>(emptyMap())
        private set

    var lastSeenNotificationMillis by mutableLongStateOf(0L)
        private set

    private var chatReadMillisByRoom: Map<String, Long> = emptyMap()
    private var roomMessagesByRoom: Map<String, List<BadgeChatMessageMeta>> = emptyMap()
    private var cachedNotifications: List<BadgeNotificationMeta> = emptyList()
    private var activeOpenChatRoomId: String? = null
    private var isNotificationScreenOpen: Boolean = false

    fun bindUser(
        userId: String?,
        persistedNotifSeenMillis: Long = 0L,
        persistedChatReadMap: Map<String, Long> = emptyMap(),
    ) {
        val cleanUid = userId?.trim()?.takeIf { it.isNotEmpty() }
        if (activeUserId != cleanUid) {
            activeUserId = cleanUid
            lastSeenNotificationMillis = persistedNotifSeenMillis.coerceAtLeast(0L)
            chatReadMillisByRoom = persistedChatReadMap
            roomMessagesByRoom = emptyMap()
            cachedNotifications = emptyList()
            unreadChatCountsByRoom = emptyMap()
            unreadChatCount = 0
            unreadNotificationCount = 0
            activeOpenChatRoomId = null
            isNotificationScreenOpen = false
        }
    }

    fun updateNotifications(
        userId: String,
        notifications: List<BadgeNotificationMeta>,
        context: Context? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val cleanUid = userId.trim()
        if (activeUserId != cleanUid) return
        cachedNotifications = notifications

        if (isNotificationScreenOpen) {
            val latestMillis = notifications.maxOfOrNull { it.createdAtMillis } ?: 0L
            advanceNotificationSeenLocked(cleanUid, maxOf(nowMillis, latestMillis), context)
        } else {
            recomputeNotificationBadgeLocked(cleanUid)
        }
    }

    fun setNotificationScreenOpen(
        isOpen: Boolean,
        userId: String?,
        context: Context? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        isNotificationScreenOpen = isOpen
        val cleanUid = userId?.trim().orEmpty()
        if (isOpen && cleanUid.isNotEmpty() && activeUserId == cleanUid) {
            val latestMillis = cachedNotifications.maxOfOrNull { it.createdAtMillis } ?: 0L
            advanceNotificationSeenLocked(cleanUid, maxOf(nowMillis, latestMillis), context)
            if (context != null) {
                RealtimeSystemNotifier.cancelAllSocialNotifications(context)
            }
        }
    }

    fun isNotificationScreenCurrentlyOpen(): Boolean = isNotificationScreenOpen

    fun currentOpenChatRoomId(): String? = activeOpenChatRoomId

    fun lastReadMillisForRoom(userId: String?, roomId: String): Long {
        val cleanUid = userId?.trim().orEmpty()
        if (cleanUid.isEmpty() || activeUserId != cleanUid) return 0L
        return chatReadMillisByRoom[roomId] ?: 0L
    }

    fun markNotificationsSeen(
        userId: String?,
        latestTimestampMillis: Long = System.currentTimeMillis(),
        context: Context? = null,
    ) {
        val cleanUid = userId?.trim().orEmpty()
        if (cleanUid.isEmpty() || activeUserId != cleanUid) return
        val cachedLatest = cachedNotifications.maxOfOrNull { it.createdAtMillis } ?: 0L
        advanceNotificationSeenLocked(cleanUid, maxOf(latestTimestampMillis, cachedLatest), context)
        if (context != null) {
            RealtimeSystemNotifier.cancelAllSocialNotifications(context)
        }
    }

    fun updateRoomMessages(
        userId: String,
        roomId: String,
        messages: List<BadgeChatMessageMeta>,
        context: Context? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val cleanUid = userId.trim()
        val cleanRoomId = roomId.trim()
        if (activeUserId != cleanUid || cleanRoomId.isEmpty()) return

        roomMessagesByRoom = roomMessagesByRoom + (cleanRoomId to messages)

        if (activeOpenChatRoomId == cleanRoomId) {
            val latestMillis = messages.maxOfOrNull { it.createdAtMillis } ?: 0L
            advanceChatRoomReadLocked(cleanUid, cleanRoomId, maxOf(nowMillis, latestMillis), context)
            if (context != null) {
                RealtimeSystemNotifier.cancelChatNotification(context, cleanRoomId)
            }
        } else {
            recomputeChatBadgesLocked(cleanUid)
        }
    }

    fun removeRoom(userId: String, roomId: String) {
        val cleanUid = userId.trim()
        if (activeUserId != cleanUid) return
        if (roomId !in roomMessagesByRoom) return
        roomMessagesByRoom = roomMessagesByRoom - roomId
        recomputeChatBadgesLocked(cleanUid)
    }

    fun setActiveOpenChatRoom(
        userId: String?,
        roomId: String?,
        context: Context? = null,
        nowMillis: Long = System.currentTimeMillis(),
    ) {
        val cleanRoomId = roomId?.trim()?.takeIf { it.isNotEmpty() }
        activeOpenChatRoomId = cleanRoomId
        val cleanUid = userId?.trim().orEmpty()
        if (cleanRoomId != null && cleanUid.isNotEmpty() && activeUserId == cleanUid) {
            val cachedLatest = roomMessagesByRoom[cleanRoomId]?.maxOfOrNull { it.createdAtMillis } ?: 0L
            advanceChatRoomReadLocked(cleanUid, cleanRoomId, maxOf(nowMillis, cachedLatest), context)
            if (context != null) {
                RealtimeSystemNotifier.cancelChatNotification(context, cleanRoomId)
            }
        }
    }

    fun clearActiveOpenChatRoom(roomId: String?) {
        if (roomId == null || activeOpenChatRoomId == roomId.trim()) {
            activeOpenChatRoomId = null
        }
    }

    fun markChatRoomRead(
        userId: String?,
        roomId: String,
        latestMessageMillis: Long = System.currentTimeMillis(),
        context: Context? = null,
    ) {
        val cleanUid = userId?.trim().orEmpty()
        val cleanRoomId = roomId.trim()
        if (cleanUid.isEmpty() || cleanRoomId.isEmpty() || activeUserId != cleanUid) return
        val cachedLatest = roomMessagesByRoom[cleanRoomId]?.maxOfOrNull { it.createdAtMillis } ?: 0L
        advanceChatRoomReadLocked(cleanUid, cleanRoomId, maxOf(latestMessageMillis, cachedLatest), context)
        if (context != null) {
            RealtimeSystemNotifier.cancelChatNotification(context, cleanRoomId)
        }
    }

    fun unreadCountForRoom(userId: String?, roomId: String): Int {
        val cleanUid = userId?.trim().orEmpty()
        if (cleanUid.isEmpty() || activeUserId != cleanUid) return 0
        return unreadChatCountsByRoom[roomId] ?: 0
    }

    fun unreadChatTotalFor(userId: String?): Int {
        val cleanUid = userId?.trim().orEmpty()
        if (cleanUid.isEmpty() || activeUserId != cleanUid) return 0
        return unreadChatCount
    }

    fun unreadNotificationTotalFor(userId: String?): Int {
        val cleanUid = userId?.trim().orEmpty()
        if (cleanUid.isEmpty() || activeUserId != cleanUid) return 0
        return unreadNotificationCount
    }

    fun reset() {
        activeUserId = null
        unreadNotificationCount = 0
        unreadChatCount = 0
        unreadChatCountsByRoom = emptyMap()
        lastSeenNotificationMillis = 0L
        chatReadMillisByRoom = emptyMap()
        roomMessagesByRoom = emptyMap()
        cachedNotifications = emptyList()
        activeOpenChatRoomId = null
        isNotificationScreenOpen = false
    }

    private fun advanceNotificationSeenLocked(
        cleanUid: String,
        targetMillis: Long,
        context: Context?,
    ) {
        val updated = maxOf(lastSeenNotificationMillis, targetMillis.coerceAtLeast(0L))
        lastSeenNotificationMillis = updated
        if (context != null) {
            RealtimeBadgePrefs.saveLastSeenNotificationMillis(context, cleanUid, updated)
        }
        recomputeNotificationBadgeLocked(cleanUid)
    }

    private fun advanceChatRoomReadLocked(
        cleanUid: String,
        cleanRoomId: String,
        targetMillis: Long,
        context: Context?,
    ) {
        val previous = chatReadMillisByRoom[cleanRoomId] ?: 0L
        val updated = maxOf(previous, targetMillis.coerceAtLeast(0L))
        if (updated > previous) {
            chatReadMillisByRoom = chatReadMillisByRoom + (cleanRoomId to updated)
            if (context != null) {
                RealtimeBadgePrefs.saveChatReadMap(context, cleanUid, chatReadMillisByRoom)
            }
        }
        recomputeChatBadgesLocked(cleanUid)
    }

    private fun recomputeNotificationBadgeLocked(cleanUid: String) {
        unreadNotificationCount = countUnreadNotifications(
            notifications = cachedNotifications,
            currentUserId = cleanUid,
            lastSeenMillis = lastSeenNotificationMillis,
        )
    }

    private fun recomputeChatBadgesLocked(cleanUid: String) {
        val perRoom = linkedMapOf<String, Int>()
        var total = 0
        roomMessagesByRoom.forEach { (roomId, messages) ->
            val unread = countUnreadChatMessages(
                messages = messages,
                currentUserId = cleanUid,
                persistedLastReadMillis = chatReadMillisByRoom[roomId] ?: 0L,
            )
            if (unread > 0) {
                perRoom[roomId] = unread
                total += unread
            }
        }
        unreadChatCountsByRoom = perRoom
        unreadChatCount = total
    }
}

/**
 * Memasang listener Firestore realtime untuk badge Notifikasi (`users/{uid}/notifications`)
 * dan badge Chat (`chats` + subkoleksi `messages` pada room tempat akun berpartisipasi).
 */
fun observeRealtimeBadges(
    context: Context,
    userId: String,
    userDisplayName: String,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    refreshRegistry: RefreshRegistry? = null,
): ListenerRegistration {
    val cleanUid = userId.trim()
    if (cleanUid.isEmpty()) {
        RealtimeBadgeStore.reset()
        return ListenerRegistration { }
    }

    val appContext = context.applicationContext
    RealtimeSystemNotifier.ensureChannels(appContext)
    RealtimeBadgeStore.bindUser(
        userId = cleanUid,
        persistedNotifSeenMillis = RealtimeBadgePrefs.loadLastSeenNotificationMillis(appContext, cleanUid),
        persistedChatReadMap = RealtimeBadgePrefs.loadChatReadMap(appContext, cleanUid),
    )

    val registrations = mutableListOf<ListenerRegistration>()
    val roomMessageListeners = linkedMapOf<String, ListenerRegistration>()
    val roomMetaById = linkedMapOf<String, ObservedChatRoomMeta>()
    val notifiedSocialIds = mutableSetOf<String>()
    val notifiedMessageIds = mutableSetOf<String>()
    val initializedRoomIds = mutableSetOf<String>()
    var isInitialNotifSnapshot = true
    var isInitialChatsSnapshot = true
    var disposed = false

    // 1. Listener realtime untuk notifikasi akun (users/{uid}/notifications)
    try {
        val notifReg = firestore.collection("users")
            .document(cleanUid)
            .collection("notifications")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .limit(MAX_OBSERVED_NOTIFICATIONS)
            .addRefreshableSnapshotListener(refreshRegistry) { snapshot, error ->
                if (disposed || error != null) return@addRefreshableSnapshotListener
                val parsed = snapshot?.documents.orEmpty().map { doc ->
                    val createdAtMillis = doc.getTimestamp(
                        "createdAt",
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                    )?.toDate()?.time ?: 0L
                    BadgeNotificationMeta(
                        id = doc.id,
                        actorId = doc.getString("actorId").orEmpty(),
                        createdAtMillis = createdAtMillis,
                        actorName = doc.getString("actorName").orEmpty(),
                        postId = doc.getString("postId").orEmpty(),
                        type = NotificationType.fromCode(doc.getString("type")),
                    )
                }

                if (isInitialNotifSnapshot) {
                    parsed.forEach { notifiedSocialIds += it.id }
                    isInitialNotifSnapshot = false
                } else if (!RealtimeBadgeStore.isNotificationScreenCurrentlyOpen()) {
                    val lastSeen = RealtimeBadgeStore.lastSeenNotificationMillis
                    parsed
                        .filter { notif ->
                            notif.id !in notifiedSocialIds &&
                                notif.actorId.trim() != cleanUid &&
                                notif.createdAtMillis > lastSeen
                        }
                        .sortedBy { it.createdAtMillis }
                        .forEach { freshNotif ->
                            notifiedSocialIds += freshNotif.id
                            RealtimeSystemNotifier.showSocialNotification(
                                context = appContext,
                                notificationDocId = freshNotif.id,
                                actorName = freshNotif.actorName,
                                type = freshNotif.type,
                                postId = freshNotif.postId,
                            )
                        }
                } else {
                    parsed.forEach { notifiedSocialIds += it.id }
                }

                RealtimeBadgeStore.updateNotifications(
                    userId = cleanUid,
                    notifications = parsed,
                    context = appContext,
                )
            }
        registrations += notifReg
    } catch (e: Exception) {
        Log.e(TAG_REALTIME_BADGE, "Gagal memasang listener badge notifikasi: ${e.message}", e)
    }

    // 2. Listener realtime untuk ruang chat yang diikuti akun
    try {
        val chatsReg = firestore.collection(CHAT_COLLECTION)
            .orderBy("updatedAt", Query.Direction.DESCENDING)
            .limit(50)
            .addRefreshableSnapshotListener(refreshRegistry) { snapshot, error ->
                if (disposed || error != null) return@addRefreshableSnapshotListener

                val wasInitialChatsSnapshot = isInitialChatsSnapshot
                isInitialChatsSnapshot = false

                val activeRoomIds = linkedSetOf<String>()
                snapshot?.documents.orEmpty().forEach { doc ->
                    if (activeRoomIds.size >= MAX_OBSERVED_CHAT_ROOMS) return@forEach
                    val data = doc.data.orEmpty()
                    val rawName = (data["name"] as? String).orEmpty()
                    val isGroup = (data["isGroup"] as? Boolean) ?: false
                    val memberIds = sanitizeChatParticipantIds(
                        (data["participantIds"] as? List<*>)
                            .orEmpty()
                            .filterIsInstance<String>(),
                    )
                    val memberNames = (data["participantNames"] as? List<*>)
                        .orEmpty()
                        .filterIsInstance<String>()
                    val isParticipant = memberIds.isEmpty() ||
                        cleanUid in memberIds ||
                        (userDisplayName.isNotBlank() &&
                            memberNames.any { it.equals(userDisplayName, ignoreCase = true) })
                    if (isParticipant) {
                        activeRoomIds += doc.id
                        val resolvedDisplayName = resolveChatRoomDisplayName(
                            fallbackName = rawName.ifBlank { if (isGroup) "Grup Olahraga" else "Teman" },
                            isGroup = isGroup,
                            participantNames = memberNames,
                            currentUserName = userDisplayName,
                        )
                        roomMetaById[doc.id] = ObservedChatRoomMeta(
                            roomId = doc.id,
                            displayName = resolvedDisplayName,
                            isGroup = isGroup,
                        )
                    }
                }

                // Lepas listener untuk room yang sudah tidak masuk daftar aktif
                val removedIds = roomMessageListeners.keys.filter { it !in activeRoomIds }
                removedIds.forEach { removedId ->
                    runCatching { roomMessageListeners.remove(removedId)?.remove() }
                    roomMetaById.remove(removedId)
                    initializedRoomIds.remove(removedId)
                    RealtimeBadgeStore.removeRoom(cleanUid, removedId)
                }

                // Pasang listener pesan untuk room baru
                activeRoomIds.forEach { roomId ->
                    if (roomId in roomMessageListeners) return@forEach
                    val isNewRoomAfterAppStart = !wasInitialChatsSnapshot
                    try {
                        val msgReg = firestore.collection(CHAT_COLLECTION)
                            .document(roomId)
                            .collection(CHAT_MESSAGES_SUBCOLLECTION)
                            .orderBy("createdAt", Query.Direction.DESCENDING)
                            .limit(MAX_OBSERVED_MESSAGES_PER_ROOM)
                            .addRefreshableSnapshotListener(refreshRegistry) { msgSnap, msgErr ->
                                if (disposed || msgErr != null) return@addRefreshableSnapshotListener
                                val parsedMsgs = msgSnap?.documents.orEmpty().map { msgDoc ->
                                    val tsMillis = msgDoc.getTimestamp(
                                        "createdAt",
                                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE,
                                    )?.toDate()?.time ?: 0L
                                    val venueTitle = parseVenueInvitePayload(msgDoc.get("venueInvite"))?.title
                                    BadgeChatMessageMeta(
                                        id = msgDoc.id,
                                        senderId = msgDoc.getString("senderId").orEmpty(),
                                        createdAtMillis = tsMillis,
                                        senderName = msgDoc.getString("senderName").orEmpty(),
                                        text = msgDoc.getString("text").orEmpty(),
                                        venueInviteTitle = venueTitle,
                                    )
                                }

                                val isFirstRoomSnapshot = initializedRoomIds.add(roomId)
                                val shouldSuppressInitialBaseline = isFirstRoomSnapshot && !isNewRoomAfterAppStart
                                val isRoomCurrentlyOpen = RealtimeBadgeStore.currentOpenChatRoomId() == roomId

                                if (shouldSuppressInitialBaseline || isRoomCurrentlyOpen) {
                                    parsedMsgs.forEach { notifiedMessageIds += it.id }
                                } else {
                                    val lastRead = RealtimeBadgeStore.lastReadMillisForRoom(cleanUid, roomId)
                                    val latestMyMsgMillis = parsedMsgs
                                        .asSequence()
                                        .filter { it.senderId.trim() == cleanUid }
                                        .maxOfOrNull { it.createdAtMillis }
                                        ?: 0L
                                    val effectiveLastRead = maxOf(lastRead, latestMyMsgMillis)
                                    val freshIncoming = parsedMsgs
                                        .filter { msg ->
                                            val sender = msg.senderId.trim()
                                            msg.id !in notifiedMessageIds &&
                                                sender.isNotEmpty() &&
                                                sender != cleanUid &&
                                                msg.createdAtMillis > effectiveLastRead
                                        }
                                        .maxByOrNull { it.createdAtMillis }

                                    parsedMsgs.forEach { notifiedMessageIds += it.id }
                                    if (freshIncoming != null) {
                                        val meta = roomMetaById[roomId]
                                        RealtimeSystemNotifier.showChatNotification(
                                            context = appContext,
                                            roomId = roomId,
                                            roomName = meta?.displayName ?: freshIncoming.senderName,
                                            isGroup = meta?.isGroup ?: false,
                                            senderName = freshIncoming.senderName,
                                            messageText = freshIncoming.text,
                                            venueInviteTitle = freshIncoming.venueInviteTitle,
                                        )
                                    }
                                }

                                RealtimeBadgeStore.updateRoomMessages(
                                    userId = cleanUid,
                                    roomId = roomId,
                                    messages = parsedMsgs,
                                    context = appContext,
                                )
                            }
                        roomMessageListeners[roomId] = msgReg
                    } catch (e: Exception) {
                        Log.e(
                            TAG_REALTIME_BADGE,
                            "Gagal memasang listener pesan room=$roomId: ${e.message}",
                            e,
                        )
                    }
                }
            }
        registrations += chatsReg
    } catch (e: Exception) {
        Log.e(TAG_REALTIME_BADGE, "Gagal memasang listener badge chat: ${e.message}", e)
    }

    return ListenerRegistration {
        disposed = true
        registrations.forEach { reg -> runCatching { reg.remove() } }
        roomMessageListeners.values.forEach { reg -> runCatching { reg.remove() } }
        roomMessageListeners.clear()
        roomMetaById.clear()
    }
}
