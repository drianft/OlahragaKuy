package com.example.olahragakuy.pages.posts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TagFriendsTest {

    @Test
    fun followingFriendFrom_usesFallbackFields() {
        val friend = followingFriendFrom("doc-1", mapOf("name" to " Gurita bola ", "avatarUrl" to "https://a/b.png"))

        assertEquals(FollowingFriend("doc-1", "Gurita bola", "https://a/b.png"), friend)
    }

    @Test
    fun followingFriendFrom_prefersUidFieldAndDefaultsName() {
        val friend = followingFriendFrom("doc-1", mapOf("uid" to "user-9"))

        assertEquals(FollowingFriend("user-9", "Teman"), friend)
    }

    @Test
    fun followingFriendFrom_blankIdReturnsNull() {
        assertNull(followingFriendFrom("", emptyMap()))
    }

    @Test
    fun sortFollowingFriends_sortsByNameAndRemovesDuplicates() {
        val sorted = sortFollowingFriends(
            listOf(
                FollowingFriend("2", "monster basket"),
                FollowingFriend("1", "Gurita bola"),
                FollowingFriend("2", "Monster basket"),
            )
        )

        assertEquals(listOf("1", "2"), sorted.map { it.uid })
    }

    @Test
    fun toggleTaggedFriend_addsThenRemoves() {
        val friend = FollowingFriend("1", "Kucing padel")

        val added = toggleTaggedFriend(emptyList(), friend)
        val removed = toggleTaggedFriend(added, friend)

        assertEquals(listOf(friend), added)
        assertTrue(removed.isEmpty())
    }

    @Test
    fun taggedFriendsFields_emptyWhenNothingSelected() {
        assertTrue(taggedFriendsFields(emptyList()).isEmpty())
    }

    @Test
    fun taggedFriendsFields_andNamesRoundTrip() {
        val fields = taggedFriendsFields(listOf(FollowingFriend("1", "Kenanganindah")))

        assertEquals(listOf("1"), fields["taggedFriendIds"])
        assertEquals(listOf("Kenanganindah"), taggedFriendNamesFrom(fields["taggedFriends"]))
        assertTrue(taggedFriendNamesFrom(null).isEmpty())
    }
}
