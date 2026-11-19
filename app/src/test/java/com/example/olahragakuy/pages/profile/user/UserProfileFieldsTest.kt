package com.example.olahragakuy.pages.profile.user

import org.junit.Assert.*
import org.junit.Test

class UserProfileFieldsTest {
    @Test fun registrationUsesRequestedNameAndOnlyAllowedFields() {
        val fields = initialProfileFields(" ian ", null, "ian@example.com", null)
        assertEquals(
            mapOf(
                "username" to "ian",
                "displayName" to "ian",
                "name" to "ian",
                "followersCount" to 0,
                "followingCount" to 0,
                "postsCount" to 0,
            ),
            fields,
        )
        assertFalse(fields.keys.any { it in listOf("uid", "email", "password", "role", "createdAt", "bio") })
    }

    @Test fun oldAccountUsesAuthNameAndPhoto() {
        val fields = initialProfileFields(null, " Ian ", "ian@example.com", "https://example.com/photo")
        assertEquals("Ian", fields["username"])
        assertEquals("https://example.com/photo", fields["photoUrl"])
    }

    @Test fun missingNameFallsBackToEmailThenGenericName() {
        assertEquals("ian", initialProfileFields(null, " ", "ian@example.com", "")["username"])
        assertEquals("Pengguna", initialProfileFields(null, null, null, null)["username"])
    }

    @Test fun preferredUsernameDoesNotReplaceAuthDisplayName() {
        val fields = initialProfileFields("ian_123", "Ian", null, null)
        assertEquals("ian_123", fields["username"])
        assertEquals("Ian", fields["displayName"])
    }

    @Test fun updatedProfileFieldsTrimsAndProvidesFallbacks() {
        val fields = updatedProfileFields("  BibiBaru  ", "  Semangat olahraga!  ", "BibiLama")
        assertEquals(
            mapOf(
                "username" to "BibiBaru",
                "displayName" to "BibiBaru",
                "name" to "BibiBaru",
                "bio" to "Semangat olahraga!",
            ),
            fields,
        )

        val fallbackFields = updatedProfileFields("   ", "Bio", "BibiLama")
        assertEquals("BibiLama", fallbackFields["username"])
    }

    @Test fun parseAndSortUserPostsOrdersNewestFirstAndKeepsImageUrl() {
        val posts = parseAndSortUserPosts(
            records = listOf(
                "older" to mapOf("caption" to "Lama", "createdAt" to 1000L),
                "newer" to mapOf(
                    "caption" to "Baru",
                    "createdAt" to 2000L,
                    "imageUrl" to "https://example.com/img.jpg",
                ),
            ),
            formatTime = { "t=$it" },
        )
        assertEquals(2, posts.size)
        assertEquals("newer", posts[0].documentId)
        assertEquals("Baru", posts[0].caption)
        assertEquals("t=2000", posts[0].time)
        assertEquals("https://example.com/img.jpg", posts[0].imageUrl)
        assertEquals("older", posts[1].documentId)
        assertNull(posts[1].imageUrl)
    }

    @Test fun parseUserProfileSnapshotReadsAllOtherUserFields() {
        val summary = parseUserProfileSnapshot(
            data = mapOf(
                "username" to "PecintaJogging",
                "displayName" to "Pecinta Jogging",
                "bio" to "Be yourself and Never Surrender",
                "photoUrl" to "https://example.com/avatar.png",
                "followersCount" to 24L,
                "followingCount" to 42L,
                "postsCount" to 10L,
            ),
            fallbackName = "Fallback",
        )
        assertEquals("PecintaJogging", summary.username)
        assertEquals("Pecinta Jogging", summary.displayName)
        assertEquals("Be yourself and Never Surrender", summary.bio)
        assertEquals("https://example.com/avatar.png", summary.photoUrl)
        assertEquals(24, summary.followersCount)
        assertEquals(42, summary.followingCount)
        assertEquals(10, summary.postsCount)
    }

    @Test fun parseUserProfileSnapshotFallsBackWhenMissing() {
        val summary = parseUserProfileSnapshot(emptyMap(), "Pecinta Jogging")
        assertEquals("Pecinta Jogging", summary.username)
        assertEquals("Pecinta Jogging", summary.displayName)
        assertEquals("", summary.bio)
        assertNull(summary.photoUrl)
        assertEquals(0, summary.followersCount)
        assertEquals(0, summary.followingCount)
        assertEquals(0, summary.postsCount)
    }

    @Test fun calculateFollowerDisplayCountAdjustsOptimistically() {
        assertEquals(25, calculateFollowerDisplayCount(24, wasInitiallyFollowing = false, isCurrentlyFollowing = true))
        assertEquals(24, calculateFollowerDisplayCount(24, wasInitiallyFollowing = false, isCurrentlyFollowing = false))
        assertEquals(23, calculateFollowerDisplayCount(24, wasInitiallyFollowing = true, isCurrentlyFollowing = false))
        assertEquals(0, calculateFollowerDisplayCount(0, wasInitiallyFollowing = true, isCurrentlyFollowing = false))
        assertEquals(1, calculateFollowerDisplayCount(0, wasInitiallyFollowing = true, isCurrentlyFollowing = true))
    }

    @Test fun shouldShowProfileLoadErrorIgnoresPermissionDenied() {
        assertFalse(shouldShowProfileLoadError(null))
        assertFalse(shouldShowProfileLoadError("PERMISSION_DENIED"))
        assertTrue(shouldShowProfileLoadError("UNAVAILABLE"))
    }
}
