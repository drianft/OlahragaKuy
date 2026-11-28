package com.example.olahragakuy.pages.posts

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.olahragakuy.MainActivity
import com.example.olahragakuy.R

data class SystemNotificationContent(
    val title: String,
    val body: String,
)

/**
 * Menyusun judul dan isi notifikasi sistem untuk pesan chat baru (termasuk ajakan venue).
 */
fun buildChatNotificationContent(
    roomName: String,
    isGroup: Boolean,
    senderName: String,
    messageText: String,
    venueInviteTitle: String? = null,
): SystemNotificationContent {
    val cleanSender = senderName.trim().ifEmpty { "Teman" }
    val cleanRoom = roomName.trim().ifEmpty { if (isGroup) "Grup Olahraga" else cleanSender }
    val cleanVenue = venueInviteTitle?.trim().orEmpty()
    val preview = when {
        cleanVenue.isNotEmpty() -> "Mengirim ajakan olahraga di $cleanVenue"
        messageText.trim().isNotEmpty() -> messageText.trim()
        else -> "Mengirim pesan baru"
    }
    return if (isGroup) {
        SystemNotificationContent(
            title = cleanRoom,
            body = "$cleanSender: $preview",
        )
    } else {
        SystemNotificationContent(
            title = cleanSender,
            body = preview,
        )
    }
}

/**
 * Menyusun judul dan isi notifikasi sistem untuk aktivitas sosial (Tag, Komentar, Suka).
 */
fun buildSocialNotificationContent(
    actorName: String,
    type: NotificationType,
): SystemNotificationContent {
    val cleanActor = actorName.trim().ifEmpty { "Seseorang" }
    return when (type) {
        NotificationType.TAG -> SystemNotificationContent(
            title = "Ditandai di Postingan",
            body = "$cleanActor menandai Anda di sebuah postingan.",
        )
        NotificationType.COMMENT -> SystemNotificationContent(
            title = "Komentar Baru",
            body = "$cleanActor mengomentari postingan Anda.",
        )
        NotificationType.LIKE -> SystemNotificationContent(
            title = "Suka Baru",
            body = "$cleanActor menyukai postingan Anda.",
        )
    }
}

fun chatNotificationIdForRoom(roomId: String): Int =
    40_000 + (roomId.hashCode() and 0x7FFF)

fun socialNotificationIdFor(notificationId: String): Int =
    80_000 + (notificationId.hashCode() and 0x7FFF)

/**
 * Penerbit notifikasi sistem Android (`NotificationCompat` heads-up) untuk Chat baru
 * dan Aktivitas Sosial (Tag Post, Komentar, Suka).
 */
object RealtimeSystemNotifier {
    const val CHANNEL_CHAT_ID = "olahragakuy_chat_messages_v1"
    const val CHANNEL_SOCIAL_ID = "olahragakuy_social_activity_v1"

    const val EXTRA_OPEN_CHAT = "extra_open_chat_from_notification"
    const val EXTRA_OPEN_CHAT_ROOM_ID = "extra_open_chat_room_id"
    const val EXTRA_OPEN_NOTIFICATIONS = "extra_open_notifications_from_notification"
    const val EXTRA_OPEN_NOTIFICATION_POST_ID = "extra_open_notification_post_id"

    private val activeSocialNotificationIds = mutableSetOf<Int>()

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context.applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun ensureChannels(context: Context) {
        val manager = context.applicationContext.getSystemService(NotificationManager::class.java)
            ?: return

        if (manager.getNotificationChannel(CHANNEL_CHAT_ID) == null) {
            val chatChannel = NotificationChannel(
                CHANNEL_CHAT_ID,
                "Pesan Chat",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifikasi pesan masuk dan ajakan olahraga di chat"
                enableVibration(true)
            }
            manager.createNotificationChannel(chatChannel)
        }

        if (manager.getNotificationChannel(CHANNEL_SOCIAL_ID) == null) {
            val socialChannel = NotificationChannel(
                CHANNEL_SOCIAL_ID,
                "Aktivitas Sosial",
                NotificationManager.IMPORTANCE_HIGH,
            ).apply {
                description = "Notifikasi saat ditandai di postingan, dikomentari, atau disukai"
                enableVibration(true)
            }
            manager.createNotificationChannel(socialChannel)
        }
    }

    fun showChatNotification(
        context: Context,
        roomId: String,
        roomName: String,
        isGroup: Boolean,
        senderName: String,
        messageText: String,
        venueInviteTitle: String? = null,
    ) {
        val appContext = context.applicationContext
        if (!hasNotificationPermission(appContext)) return
        ensureChannels(appContext)

        val content = buildChatNotificationContent(
            roomName = roomName,
            isGroup = isGroup,
            senderName = senderName,
            messageText = messageText,
            venueInviteTitle = venueInviteTitle,
        )
        val notificationId = chatNotificationIdForRoom(roomId)

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_CHAT, true)
            putExtra(EXTRA_OPEN_CHAT_ROOM_ID, roomId)
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_CHAT_ID)
            .setSmallIcon(R.drawable.ic_mingcute_message_3_fill)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        } catch (_: SecurityException) {
        }
    }

    fun showSocialNotification(
        context: Context,
        notificationDocId: String,
        actorName: String,
        type: NotificationType,
        postId: String,
    ) {
        val appContext = context.applicationContext
        if (!hasNotificationPermission(appContext)) return
        ensureChannels(appContext)

        val content = buildSocialNotificationContent(actorName = actorName, type = type)
        val notificationId = socialNotificationIdFor(notificationDocId)

        val openIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_NOTIFICATIONS, true)
            if (postId.isNotBlank()) {
                putExtra(EXTRA_OPEN_NOTIFICATION_POST_ID, postId)
            }
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_SOCIAL_ID)
            .setSmallIcon(R.drawable.ic_mingcute_notification_fill)
            .setContentTitle(content.title)
            .setContentText(content.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content.body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        synchronized(activeSocialNotificationIds) {
            activeSocialNotificationIds += notificationId
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        try {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        } catch (_: SecurityException) {
        }
    }

    fun cancelChatNotification(context: Context, roomId: String) {
        if (roomId.isBlank()) return
        runCatching {
            NotificationManagerCompat.from(context.applicationContext)
                .cancel(chatNotificationIdForRoom(roomId))
        }
    }

    fun cancelAllSocialNotifications(context: Context) {
        val idsToCancel = synchronized(activeSocialNotificationIds) {
            val copy = activeSocialNotificationIds.toList()
            activeSocialNotificationIds.clear()
            copy
        }
        val manager = NotificationManagerCompat.from(context.applicationContext)
        idsToCancel.forEach { id ->
            runCatching { manager.cancel(id) }
        }
    }
}
