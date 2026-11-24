package com.example.olahragakuy.pages.profile.admin

import com.example.olahragakuy.pages.posts.NotificationType
import com.example.olahragakuy.pages.posts.parseNotificationUiModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AdminReportRepositoryTest {

    @Test
    fun parseAdminReportItemMapsAllFieldsAndLastHourFlag() {
        val nowMillis = 1_760_000_000_000L
        val thirtyMinutesAgo = nowMillis - 30L * 60L * 1000L

        val item = parseAdminReportItem(
            id = "rep-123",
            data = mapOf(
                "type" to "POST",
                "reportedUserId" to "uid-bibi",
                "reportedUsername" to "BibiOlahraga",
                "reporterUsername" to "PecintaJogging",
                "reportCategory" to "Aktivitas ilegal",
                "reportDescription" to "Konten melanggar pedoman komunitas.",
                "contentPreview" to "Always on Proces",
                "thumbnailUrl" to "https://example.com/photo.jpg",
                "postDocumentId" to "post-77",
                "postTime" to "2 jam lalu",
                "likesCount" to 12L,
                "commentsCount" to 3L,
                "sharesCount" to 1L,
                "createdAtMillis" to thirtyMinutesAgo,
            ),
            nowMillis = nowMillis,
        )

        assertEquals("rep-123", item.id)
        assertEquals(AdminReportType.POST, item.type)
        assertEquals("uid-bibi", item.reportedUserId)
        assertEquals("BibiOlahraga", item.reportedUsername)
        assertEquals("PecintaJogging", item.reporterUsername)
        assertEquals("Aktivitas ilegal", item.reportCategory)
        assertEquals("Konten melanggar pedoman komunitas.", item.reportDescription)
        assertEquals("Always on Proces", item.contentPreview)
        assertEquals("https://example.com/photo.jpg", item.thumbnailUrl)
        assertEquals("post-77", item.postDocumentId)
        assertEquals(12, item.likesCount)
        assertEquals(3, item.commentsCount)
        assertEquals(1, item.sharesCount)
        assertTrue(item.withinLastHour)
    }

    @Test
    fun isWithinLastHourHandlesOlderAndMissingTimestamps() {
        val now = 1_760_000_000_000L
        assertTrue(isWithinLastHour(now - 59L * 60L * 1000L, now))
        assertFalse(isWithinLastHour(now - 61L * 60L * 1000L, now))
        assertFalse(isWithinLastHour(0L, now))
    }

    @Test
    fun buildCreateReportPayloadMatchesAllowedSchemaAndBounds() {
        val payload = buildCreateReportPayload(
            type = AdminReportType.POST,
            reportedUserId = "uid-target",
            reportedUsername = "  BibiOlahraga  ",
            reporterUserId = "uid-reporter",
            reporterUsername = "PecintaJogging",
            reportCategory = "Hoax atau informasi palsu",
            reportDescription = "  Informasi menyesatkan  ",
            contentPreview = "Caption postingan",
            thumbnailUrl = null,
            postDocumentId = "post-99",
            postTime = "1 jam lalu",
            likesCount = -5,
            commentsCount = 2,
            sharesCount = 1,
            createdAt = "SERVER_TIME",
        )

        assertEquals("POST", payload["type"])
        assertEquals(REPORT_STATUS_PENDING, payload["status"])
        assertEquals("uid-target", payload["reportedUserId"])
        assertEquals("BibiOlahraga", payload["reportedUsername"])
        assertEquals("uid-reporter", payload["reporterUserId"])
        assertEquals("PecintaJogging", payload["reporterUsername"])
        assertEquals("Hoax atau informasi palsu", payload["reportCategory"])
        assertEquals("Informasi menyesatkan", payload["reportDescription"])
        assertEquals("post-99", payload["postDocumentId"])
        assertEquals(0, payload["likesCount"])
        assertNull(payload["thumbnailUrl"])
        assertEquals("SERVER_TIME", payload["createdAt"])
    }

    @Test
    fun buildResolveReportAndWarningPayloadsMatchExpectedStructure() {
        val resolve = buildResolveReportPayload(
            status = REPORT_STATUS_RESOLVED_WARNED,
            resolvedBy = "uid-admin",
            warningTitle = "Peringatan Keras",
            warningDescription = "Harap patuhi aturan komunitas.",
            resolvedAt = "SERVER_TIME",
        )
        assertEquals(REPORT_STATUS_RESOLVED_WARNED, resolve["status"])
        assertEquals("uid-admin", resolve["resolvedBy"])
        assertEquals("Peringatan Keras", resolve["warningTitle"])
        assertEquals("Harap patuhi aturan komunitas.", resolve["warningDescription"])

        val warningNotif = buildAdminWarningNotificationPayload(
            adminUid = "uid-admin",
            adminDisplayName = "Admin Utama",
            postId = "post-99",
            title = "  ",
            description = "Konten Anda melanggar aturan.",
            createdAt = "SERVER_TIME",
        )
        assertEquals("uid-admin", warningNotif["actorId"])
        assertEquals("Admin Utama", warningNotif["actorName"])
        assertEquals("post-99", warningNotif["postId"])
        assertEquals(NotificationType.WARNING.code, warningNotif["type"])
        assertEquals("Notifikasi Warning", warningNotif["title"])
        assertEquals("Konten Anda melanggar aturan.", warningNotif["description"])
    }

    @Test
    fun parseNotificationUiModelSupportsAdminWarning() {
        val notif = parseNotificationUiModel(
            id = "warn-1",
            data = mapOf(
                "actorId" to "uid-admin",
                "actorName" to "Admin OlahragaKuy",
                "postId" to "post-99",
                "type" to "WARNING",
                "title" to "Notifikasi Warning",
                "description" to "Postingan Anda mengandung ujaran kebencian.",
                "createdAtMillis" to 1_760_000_000_000L,
            ),
        )
        assertEquals(NotificationType.WARNING, notif?.type)
        assertEquals("Notifikasi Warning", notif?.title)
        assertEquals("Postingan Anda mengandung ujaran kebencian.", notif?.description)
        assertEquals("post-99", notif?.postId)
    }
}
