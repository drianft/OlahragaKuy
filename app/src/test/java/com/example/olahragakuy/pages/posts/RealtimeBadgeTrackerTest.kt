package com.example.olahragakuy.pages.posts

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class RealtimeBadgeTrackerTest {

    @Before
    fun setUp() {
        RealtimeBadgeStore.reset()
    }

    @After
    fun tearDown() {
        RealtimeBadgeStore.reset()
    }

    @Test
    fun formatBadgeCountHandlesZeroNormalAndOverflow() {
        assertNull(formatBadgeCount(-3))
        assertNull(formatBadgeCount(0))
        assertEquals("1", formatBadgeCount(1))
        assertEquals("9", formatBadgeCount(9))
        assertEquals("99", formatBadgeCount(99))
        assertEquals("99+", formatBadgeCount(100))
        assertEquals("99+", formatBadgeCount(450))
    }

    @Test
    fun countUnreadChatMessagesIgnoresOwnMessagesAndMessagesBeforeOwnReply() {
        val messages = listOf(
            BadgeChatMessageMeta(id = "m1", senderId = "friend_1", createdAtMillis = 1_000L),
            BadgeChatMessageMeta(id = "m2", senderId = "me_uid", createdAtMillis = 2_000L),
            BadgeChatMessageMeta(id = "m3", senderId = "friend_1", createdAtMillis = 3_000L),
            BadgeChatMessageMeta(id = "m4", senderId = "friend_1", createdAtMillis = 4_000L),
        )

        // Meskipun persistedLastReadMillis = 0 (perangkat baru), m1 sebelum balasan m2 dianggap terbaca;
        // hanya m3 dan m4 yang dihitung belum dibaca.
        assertEquals(2, countUnreadChatMessages(messages, currentUserId = "me_uid", persistedLastReadMillis = 0L))

        // Jika sudah dibaca hingga 3_000L, hanya m4 yang tersisa
        assertEquals(1, countUnreadChatMessages(messages, currentUserId = "me_uid", persistedLastReadMillis = 3_000L))

        // Jika sudah dibaca hingga 4_000L, menjadi 0
        assertEquals(0, countUnreadChatMessages(messages, currentUserId = "me_uid", persistedLastReadMillis = 4_000L))
    }

    @Test
    fun countUnreadNotificationsIgnoresSelfAndSeenNotifications() {
        val notifications = listOf(
            BadgeNotificationMeta(id = "n1", actorId = "friend_1", createdAtMillis = 1_000L),
            BadgeNotificationMeta(id = "n2", actorId = "me_uid", createdAtMillis = 2_500L),
            BadgeNotificationMeta(id = "n3", actorId = "friend_2", createdAtMillis = 3_000L),
            BadgeNotificationMeta(id = "n4", actorId = "friend_3", createdAtMillis = 4_000L),
        )

        assertEquals(3, countUnreadNotifications(notifications, currentUserId = "me_uid", lastSeenMillis = 0L))
        assertEquals(2, countUnreadNotifications(notifications, currentUserId = "me_uid", lastSeenMillis = 1_000L))
        assertEquals(0, countUnreadNotifications(notifications, currentUserId = "me_uid", lastSeenMillis = 4_000L))
    }

    @Test
    fun readTimestampsRoundTripCleanly() {
        val original = mapOf(
            "dm_a_b" to 1_760_000_001_000L,
            "group_gym" to 1_760_000_009_500L,
        )
        val raw = serializeReadTimestamps(original)
        val parsed = parseReadTimestamps(raw)
        assertEquals(original, parsed)
        assertEquals(emptyMap<String, Long>(), parseReadTimestamps("invalid-line"))
        assertEquals(emptyMap<String, Long>(), parseReadTimestamps(null))
    }

    @Test
    fun storeTracksRealtimeUnreadChatAndClearsWhenRoomOpened() {
        RealtimeBadgeStore.bindUser(userId = "me_uid")

        // Masuk 1 pesan baru dari teman di room_1
        RealtimeBadgeStore.updateRoomMessages(
            userId = "me_uid",
            roomId = "room_1",
            messages = listOf(
                BadgeChatMessageMeta(id = "m1", senderId = "friend_1", createdAtMillis = 10_000L),
            ),
            nowMillis = 10_500L,
        )
        assertEquals(1, RealtimeBadgeStore.unreadChatTotalFor("me_uid"))
        assertEquals(1, RealtimeBadgeStore.unreadCountForRoom("me_uid", "room_1"))

        // Masuk 2 pesan baru di room_2 -> total menjadi 3
        RealtimeBadgeStore.updateRoomMessages(
            userId = "me_uid",
            roomId = "room_2",
            messages = listOf(
                BadgeChatMessageMeta(id = "m2", senderId = "friend_2", createdAtMillis = 11_000L),
                BadgeChatMessageMeta(id = "m3", senderId = "friend_2", createdAtMillis = 12_000L),
            ),
            nowMillis = 12_500L,
        )
        assertEquals(3, RealtimeBadgeStore.unreadChatTotalFor("me_uid"))
        assertEquals(2, RealtimeBadgeStore.unreadCountForRoom("me_uid", "room_2"))

        // Pengguna membuka room_1 -> badge room_1 langsung 0, total menjadi 2
        RealtimeBadgeStore.setActiveOpenChatRoom(userId = "me_uid", roomId = "room_1", nowMillis = 13_000L)
        assertEquals(0, RealtimeBadgeStore.unreadCountForRoom("me_uid", "room_1"))
        assertEquals(2, RealtimeBadgeStore.unreadChatTotalFor("me_uid"))

        // Pesan baru datang di room_1 SAAT pengguna masih membuka room_1 -> tetap 0
        RealtimeBadgeStore.updateRoomMessages(
            userId = "me_uid",
            roomId = "room_1",
            messages = listOf(
                BadgeChatMessageMeta(id = "m1", senderId = "friend_1", createdAtMillis = 10_000L),
                BadgeChatMessageMeta(id = "m4", senderId = "friend_1", createdAtMillis = 14_000L),
            ),
            nowMillis = 14_100L,
        )
        assertEquals(0, RealtimeBadgeStore.unreadCountForRoom("me_uid", "room_1"))
        assertEquals(2, RealtimeBadgeStore.unreadChatTotalFor("me_uid"))

        // Pengguna keluar dari room_1 lalu teman kirim pesan baru -> badge room_1 naik jadi 1
        RealtimeBadgeStore.clearActiveOpenChatRoom("room_1")
        RealtimeBadgeStore.updateRoomMessages(
            userId = "me_uid",
            roomId = "room_1",
            messages = listOf(
                BadgeChatMessageMeta(id = "m1", senderId = "friend_1", createdAtMillis = 10_000L),
                BadgeChatMessageMeta(id = "m4", senderId = "friend_1", createdAtMillis = 14_000L),
                BadgeChatMessageMeta(id = "m5", senderId = "friend_1", createdAtMillis = 15_000L),
            ),
            nowMillis = 15_100L,
        )
        assertEquals(1, RealtimeBadgeStore.unreadCountForRoom("me_uid", "room_1"))
        assertEquals(3, RealtimeBadgeStore.unreadChatTotalFor("me_uid"))
    }

    @Test
    fun storeTracksNotificationsAndIsolatesAccounts() {
        RealtimeBadgeStore.bindUser(userId = "user_a")
        RealtimeBadgeStore.updateNotifications(
            userId = "user_a",
            notifications = listOf(
                BadgeNotificationMeta(id = "n1", actorId = "user_b", createdAtMillis = 5_000L),
            ),
            nowMillis = 5_500L,
        )
        assertEquals(1, RealtimeBadgeStore.unreadNotificationTotalFor("user_a"))
        assertEquals(0, RealtimeBadgeStore.unreadNotificationTotalFor("user_b"))

        // Membuka layar Notifikasi langsung membersihkan badge
        RealtimeBadgeStore.setNotificationScreenOpen(isOpen = true, userId = "user_a", nowMillis = 6_000L)
        assertEquals(0, RealtimeBadgeStore.unreadNotificationTotalFor("user_a"))

        // Keluar dari layar Notifikasi lalu ada notifikasi baru -> naik jadi 1
        RealtimeBadgeStore.setNotificationScreenOpen(isOpen = false, userId = "user_a", nowMillis = 6_100L)
        RealtimeBadgeStore.updateNotifications(
            userId = "user_a",
            notifications = listOf(
                BadgeNotificationMeta(id = "n1", actorId = "user_b", createdAtMillis = 5_000L),
                BadgeNotificationMeta(id = "n2", actorId = "user_c", createdAtMillis = 7_000L),
            ),
            nowMillis = 7_100L,
        )
        assertEquals(1, RealtimeBadgeStore.unreadNotificationTotalFor("user_a"))

        // Pindah akun -> data user_a ter-reset dan snapshot terlambat user_a diabaikan
        RealtimeBadgeStore.bindUser(userId = "user_b")
        assertEquals(0, RealtimeBadgeStore.unreadNotificationTotalFor("user_b"))
        RealtimeBadgeStore.updateNotifications(
            userId = "user_a",
            notifications = listOf(
                BadgeNotificationMeta(id = "n3", actorId = "user_c", createdAtMillis = 9_000L),
            ),
            nowMillis = 9_100L,
        )
        assertEquals(0, RealtimeBadgeStore.unreadNotificationTotalFor("user_b"))
    }

    @Test
    fun systemNotificationBuildersFormatChatAndSocialMessages() {
        val dm = buildChatNotificationContent(
            roomName = "Monster Basket",
            isGroup = false,
            senderName = "Monster Basket",
            messageText = "Ayo main basket sore ini!",
        )
        assertEquals("Monster Basket", dm.title)
        assertEquals("Ayo main basket sore ini!", dm.body)

        val groupVenue = buildChatNotificationContent(
            roomName = "GymClub",
            isGroup = true,
            senderName = "KenanganTerindah",
            messageText = "",
            venueInviteTitle = "GOR Cikal USU",
        )
        assertEquals("GymClub", groupVenue.title)
        assertEquals("KenanganTerindah: Mengirim ajakan olahraga di GOR Cikal USU", groupVenue.body)

        val tagNotif = buildSocialNotificationContent(
            actorName = "KenanganTerindah",
            type = NotificationType.TAG,
        )
        assertEquals("Ditandai di Postingan", tagNotif.title)
        assertEquals("KenanganTerindah menandai Anda di sebuah postingan.", tagNotif.body)

        val commentNotif = buildSocialNotificationContent(
            actorName = "BibiOlahraga",
            type = NotificationType.COMMENT,
        )
        assertEquals("Komentar Baru", commentNotif.title)
        assertEquals("BibiOlahraga mengomentari postingan Anda.", commentNotif.body)

        val likeNotif = buildSocialNotificationContent(
            actorName = "Monster Basket",
            type = NotificationType.LIKE,
        )
        assertEquals("Suka Baru", likeNotif.title)
        assertEquals("Monster Basket menyukai postingan Anda.", likeNotif.body)

        assertEquals(chatNotificationIdForRoom("dm_a_b"), chatNotificationIdForRoom("dm_a_b"))
        assertEquals(socialNotificationIdFor("notif_1"), socialNotificationIdFor("notif_1"))
    }
}
