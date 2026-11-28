package com.example.olahragakuy.pages.posts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PostReportTest {
    private val otherUsersPost = PostUiModel(
        id = "post_123",
        authorId = "user_other",
        authorName = "SayurOlahraga",
        time = "Baru saja",
        caption = "Jogging pagi 20 menit!",
        imageUrl = "https://example.com/photo.jpg",
        likesCount = 12L,
        commentsCount = 3L,
        sharesCount = 1L,
    )

    private val ownPost = PostUiModel(
        id = "post_456",
        authorId = "user_me",
        authorName = "Aku",
        time = "Baru saja",
        caption = "Stretching ringan",
    )

    @Test
    fun ownPostCannotBeReportedWhileOtherUsersPostCan() {
        assertTrue(isOwnPost(ownPost, "user_me"))
        assertFalse(canReportPost(ownPost, "user_me"))

        assertFalse(isOwnPost(otherUsersPost, "user_me"))
        assertTrue(canReportPost(otherUsersPost, "user_me"))
        assertTrue(canReportPost(otherUsersPost, null))
    }

    @Test
    fun blankPostIdCannotBeReported() {
        val invalidPost = otherUsersPost.copy(id = "   ")
        assertFalse(canReportPost(invalidPost, "user_me"))
        assertNotNull(
            validatePostReportInput(
                post = invalidPost,
                reporterId = "user_me",
                reason = PostReportReason.SPAM,
                detail = "",
            ),
        )
    }

    @Test
    fun validationRejectsSelfReportMissingAuthMissingReasonAndBlankOtherDetail() {
        assertNotNull(
            validatePostReportInput(
                post = otherUsersPost,
                reporterId = null,
                reason = PostReportReason.SPAM,
                detail = "",
            ),
        )
        assertNotNull(
            validatePostReportInput(
                post = ownPost,
                reporterId = "user_me",
                reason = PostReportReason.SPAM,
                detail = "",
            ),
        )
        assertNotNull(
            validatePostReportInput(
                post = otherUsersPost,
                reporterId = "user_me",
                reason = null,
                detail = "",
            ),
        )
        assertNotNull(
            validatePostReportInput(
                post = otherUsersPost,
                reporterId = "user_me",
                reason = PostReportReason.OTHER,
                detail = "   ",
            ),
        )
        assertNotNull(
            validatePostReportInput(
                post = otherUsersPost,
                reporterId = "user_me",
                reason = PostReportReason.HARASSMENT,
                detail = "a".repeat(MAX_POST_REPORT_DETAIL_LENGTH + 1),
            ),
        )
    }

    @Test
    fun validationAcceptsValidReasonsAndBuildsTopLevelReportsPayload() {
        assertNull(
            validatePostReportInput(
                post = otherUsersPost,
                reporterId = "user_me",
                reason = PostReportReason.BODY_SHAMING,
                detail = "",
            ),
        )
        assertNull(
            validatePostReportInput(
                post = otherUsersPost,
                reporterId = "user_me",
                reason = PostReportReason.OTHER,
                detail = "Komentar promosi judi online",
            ),
        )

        assertEquals("post_post_123_user_me", postReportDocumentId(" post_123 ", " user_me "))

        val payload = buildPostReportData(
            post = otherUsersPost,
            reporterId = "user_me",
            reporterName = "  Budi  ",
            reason = PostReportReason.PRIVACY,
            detail = "  Menampilkan alamat rumah  ",
        )
        assertEquals("POST", payload["type"])
        assertEquals("PENDING", payload["status"])
        assertEquals("user_other", payload["reportedUserId"])
        assertEquals("SayurOlahraga", payload["reportedUsername"])
        assertEquals("user_me", payload["reporterUserId"])
        assertEquals("Budi", payload["reporterUsername"])
        assertEquals(PostReportReason.PRIVACY.label, payload["reportCategory"])
        assertEquals("Menampilkan alamat rumah", payload["reportDescription"])
        assertEquals("Jogging pagi 20 menit!", payload["contentPreview"])
        assertEquals("https://example.com/photo.jpg", payload["thumbnailUrl"])
        assertEquals("post_123", payload["postDocumentId"])
        assertEquals("Baru saja", payload["postTime"])
        assertEquals(12L, payload["likesCount"])
        assertEquals(3L, payload["commentsCount"])
        assertEquals(1L, payload["sharesCount"])
        assertNotNull(payload["createdAt"])
    }

    @Test
    fun payloadOmitsOptionalFieldsWhenBlank() {
        val minimalPost = PostUiModel(
            id = "post_min",
            authorId = "",
            authorName = "Anon",
            time = "",
            caption = "",
            imageUrl = null,
        )
        val payload = buildPostReportData(
            post = minimalPost,
            reporterId = "user_me",
            reporterName = "Budi",
            reason = PostReportReason.SPAM,
            detail = "   ",
        )
        assertFalse("reportedUserId" in payload)
        assertFalse("reportDescription" in payload)
        assertFalse("contentPreview" in payload)
        assertFalse("thumbnailUrl" in payload)
        assertFalse("postTime" in payload)
        assertEquals("POST", payload["type"])
        assertEquals("PENDING", payload["status"])
        assertEquals("post_min", payload["postDocumentId"])
    }
}
