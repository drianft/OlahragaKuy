package com.example.olahragakuy.pages.posts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EditPostTest {
    @Test
    fun isValidEditedCaptionChecksTrimmedBounds() {
        assertFalse(isValidEditedCaption(""))
        assertFalse(isValidEditedCaption("   \n\t  "))
        assertTrue(isValidEditedCaption("A"))
        assertTrue(isValidEditedCaption("a".repeat(MAX_POST_CAPTION_LENGTH)))
        assertFalse(isValidEditedCaption("a".repeat(MAX_POST_CAPTION_LENGTH + 1)))
    }

    @Test
    fun hasCaptionChangedIgnoresOuterWhitespace() {
        assertFalse(hasCaptionChanged("Lari pagi", "  Lari pagi  "))
        assertTrue(hasCaptionChanged("Lari pagi", "Lari pagi 5 km"))
    }

    @Test
    fun hasTaggedFriendsChangedAndNewlyAddedDetectAdditionsAndRemovals() {
        val raka = FollowingFriend("u1", "Raka")
        val dina = FollowingFriend("u2", "Dina")
        val budi = FollowingFriend("u3", "Budi")

        assertFalse(hasTaggedFriendsChanged(listOf(raka, dina), listOf(dina, raka)))
        assertTrue(hasTaggedFriendsChanged(listOf(raka), listOf(raka, dina)))
        assertTrue(hasTaggedFriendsChanged(listOf(raka, dina), listOf(raka)))

        assertEquals(
            listOf(dina, budi),
            newlyAddedTaggedFriends(listOf(raka), listOf(raka, dina, budi, dina)),
        )
        assertEquals(
            emptyList<FollowingFriend>(),
            newlyAddedTaggedFriends(listOf(raka, dina), listOf(raka)),
        )
    }

    @Test
    fun canSaveEditedPostAllowsCaptionOnlyOrTagOnlyChanges() {
        val raka = FollowingFriend("u1", "Raka")
        val dina = FollowingFriend("u2", "Dina")

        assertFalse(
            canSaveEditedPost(
                originalCaption = "Lari pagi",
                editedCaption = "Lari pagi",
                originalTaggedFriends = listOf(raka),
                editedTaggedFriends = listOf(raka),
                isSaving = false,
            ),
        )
        assertTrue(
            canSaveEditedPost(
                originalCaption = "Lari pagi",
                editedCaption = "Lari pagi",
                originalTaggedFriends = listOf(raka),
                editedTaggedFriends = listOf(raka, dina),
                isSaving = false,
            ),
        )
        assertFalse(
            canSaveEditedPost(
                originalCaption = "Lari pagi",
                editedCaption = "   ",
                originalTaggedFriends = listOf(raka),
                editedTaggedFriends = listOf(raka, dina),
                isSaving = false,
            ),
        )
        assertFalse(
            canSaveEditedPost(
                originalCaption = "Lari pagi",
                editedCaption = "Lari sore",
                originalTaggedFriends = emptyList(),
                editedTaggedFriends = emptyList(),
                isSaving = true,
            ),
        )
        assertTrue(canSaveEditedCaption("Lari pagi", "  Lari sore santai  ", isSaving = false))
    }

    @Test
    fun buildEditPostUpdatePayloadOmitsTagFieldsWhenTagsUnchanged() {
        val raka = FollowingFriend("u1", "Raka")
        val payload = buildEditPostUpdatePayload(
            caption = "  Jogging sore 3 km  ",
            category = "Jogging, Kardio",
            authorName = "BibiOlahraga",
            taggedFriends = listOf(raka),
            includeTaggedFriends = false,
            hasPhoto = true,
        )
        assertNotNull(payload)
        assertEquals(setOf("caption", "searchKeywords"), payload!!.keys)
        assertEquals("Jogging sore 3 km", payload["caption"])
        @Suppress("UNCHECKED_CAST")
        val keywords = payload["searchKeywords"] as List<String>
        assertTrue("jogging" in keywords)
        assertTrue("sore" in keywords)
        assertTrue("kardio" in keywords)
        assertTrue("bibiolahraga" in keywords)
        assertTrue("raka" in keywords)
        assertTrue("foto" in keywords)
        assertTrue("gambar" in keywords)
    }

    @Test
    fun buildEditPostUpdatePayloadIncludesTaggedFriendsWhenChanged() {
        val raka = FollowingFriend("u1", "Raka")
        val dina = FollowingFriend("u2", "Dina")
        val payload = buildEditPostUpdatePayload(
            caption = "Jogging sore 3 km",
            category = "Jogging",
            authorName = "BibiOlahraga",
            taggedFriends = listOf(raka, dina, raka),
            includeTaggedFriends = true,
            hasPhoto = false,
        )
        assertNotNull(payload)
        assertEquals(
            setOf("caption", "searchKeywords", "taggedFriendIds", "taggedFriends"),
            payload!!.keys,
        )
        assertEquals(listOf("u1", "u2"), payload["taggedFriendIds"])
        assertEquals(
            listOf(
                mapOf("uid" to "u1", "displayName" to "Raka"),
                mapOf("uid" to "u2", "displayName" to "Dina"),
            ),
            payload["taggedFriends"],
        )
        @Suppress("UNCHECKED_CAST")
        val keywords = payload["searchKeywords"] as List<String>
        assertTrue("dina" in keywords)
    }

    @Test
    fun buildEditPostUpdatePayloadRejectsBlankAndTooLongCaption() {
        assertNull(buildEditPostUpdatePayload("   ", "", "Bibi"))
        assertNull(
            buildEditPostUpdatePayload(
                "a".repeat(MAX_POST_CAPTION_LENGTH + 1),
                "",
                "Bibi",
            ),
        )
    }

    @Test
    fun taggedFriendsFromParsesValidEntriesAndDeduplicatesByUid() {
        val parsed = taggedFriendsFrom(
            listOf(
                mapOf("uid" to "u1", "displayName" to "Raka"),
                mapOf("uid" to "u1", "displayName" to "Raka Duplikat"),
                mapOf("uid" to "", "displayName" to "TanpaUid"),
                mapOf("uid" to "u2", "displayName" to "  Dina  "),
            ),
        )
        assertEquals(
            listOf(
                FollowingFriend("u1", "Raka"),
                FollowingFriend("u2", "Dina"),
            ),
            parsed,
        )
    }

    @Test
    fun editPostBridgeStoresPeeksAndClearsTarget() {
        val target = EditPostTarget(
            postId = "post-42",
            authorId = "u-me",
            authorName = "Bibi",
            caption = "Caption awal",
            imageUrl = "https://example.com/photo.jpg",
        )
        EditPostBridge.setPendingTarget(target)
        assertEquals(target, EditPostBridge.peekTarget())
        EditPostBridge.clear()
        assertNull(EditPostBridge.peekTarget())
    }
}
