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

    @Test
    fun parsePostUiModel_parsesFieldsAndClampsNegativeCounts() {
        val model = parsePostUiModel(
            id = "post-42",
            data = mapOf(
                "authorName" to "Gurita bola",
                "caption" to "Olahraga pagi",
                "imageUrl" to "https://example.com/a.jpg",
                "taggedFriends" to listOf(mapOf("uid" to "1", "displayName" to "Monster basket")),
                "likes" to 5,
                "comment" to -2,
                "Share" to 3L,
            ),
            formattedTime = "17 Nov 2026",
        )

        assertEquals("post-42", model.id)
        assertEquals("Gurita bola", model.authorName)
        assertEquals("17 Nov 2026", model.time)
        assertEquals("Olahraga pagi", model.caption)
        assertEquals("https://example.com/a.jpg", model.imageUrl)
        assertEquals(listOf("Monster basket"), model.taggedNames)
        assertEquals(5L, model.likesCount)
        assertEquals(0L, model.commentsCount)
        assertEquals(3L, model.sharesCount)
    }

    @Test
    fun matchesPostQuery_matchesCategoryCaptionPhotoAndTokens() {
        val healthyPost = parsePostUiModel(
            id = "p1",
            data = mapOf(
                "authorName" to "SayurOlahraga",
                "caption" to "Ayo mulai konsumsi sayur supaya harimu makin sehat",
                "category" to "Makanan Sehat",
                "imageUrl" to "https://example.com/sayur.jpg",
                "searchKeywords" to buildSearchKeywords(
                    caption = "Ayo mulai konsumsi sayur supaya harimu makin sehat",
                    category = "Makanan Sehat",
                    authorName = "SayurOlahraga",
                    hasPhoto = true,
                ),
            ),
        )

        assertTrue(matchesPostQuery(healthyPost, "Makanan Sehat"))
        assertTrue(matchesPostQuery(healthyPost, "sayur sehat"))
        assertTrue(matchesPostQuery(healthyPost, "foto"))
    }

    @Test
    fun toggleCategoryTag_addsAndRemovesTagCaseInsensitively() {
        val added = toggleCategoryTag("Makanan Sehat", "Foto Orang")
        val removed = toggleCategoryTag(added, "makanan sehat")

        assertEquals("Makanan Sehat, Foto Orang", added)
        assertEquals("Foto Orang", removed)
    }

    @Test
    fun parseNotificationUiModel_andGroupingWorkAsExpected() {
        val now = 1_700_000_000_000L
        val twoDaysAgo = now - (2L * 24 * 60 * 60 * 1000)
        val tenDaysAgo = now - (10L * 24 * 60 * 60 * 1000)

        val likeNotif = parseNotificationUiModel(
            id = "n1",
            data = mapOf(
                "type" to "LIKE",
                "actorName" to "Monster Basket",
                "postId" to "p1",
            ),
            fallbackMillis = twoDaysAgo,
        )!!
        val tagNotif = parseNotificationUiModel(
            id = "n2",
            data = mapOf(
                "type" to "TAG",
                "actorName" to "KenanganTerindah",
                "postId" to "p2",
            ),
            fallbackMillis = tenDaysAgo,
        )!!

        assertEquals(NotificationType.LIKE, likeNotif.type)
        assertEquals("menyukai postingan anda", likeNotif.type.message)
        assertEquals("menandai anda di sebuah postingan", tagNotif.type.message)

        val grouped = groupNotificationsByPeriod(
            notifications = listOf(likeNotif, tagNotif),
            nowMillis = now,
        )
        assertEquals(2, grouped.size)
        assertEquals("7 hari terakhir", grouped[0].first)
        assertEquals(listOf("n1"), grouped[0].second.map { it.id })
        assertEquals("30 hari terakhir", grouped[1].first)
        assertEquals(listOf("n2"), grouped[1].second.map { it.id })
    }

    @Test
    fun chatHelpers_mergeFilterAndCreateGroupWorkAsExpected() {
        val following = listOf(
            FollowingFriend("u_monster", "Monster Basket"),
            FollowingFriend("u_baru", "Teman Baru"),
        )
        val merged = mergeChatRoomsWithFollowing(
            existingRooms = listOf(
                ChatRoomUiModel(
                    id = "dm_monster",
                    name = "Monster Basket",
                    isGroup = false,
                    statusOrMembers = "aktif 2 jam lalu",
                    lastMessage = "Oke broo",
                    dateLabel = "4/11",
                ),
            ),
            followingFriends = following,
        )
        assertEquals(2, merged.size)

        val newGroup = createGroupChatRoom(
            groupName = "GymClub",
            selectedFriends = following,
        )
        assertTrue(newGroup.isGroup)
        assertEquals("GymClub", newGroup.name)
        assertEquals("Anda,Monster Basket,Teman Baru", newGroup.statusOrMembers)

        val allRooms = merged + newGroup
        val peopleOnly = filterChatRooms(allRooms, ChatTab.ORANG, "Teman")
        val groupOnly = filterChatRooms(allRooms, ChatTab.KOMUNITAS, "Gym")

        assertEquals(listOf("Teman Baru"), peopleOnly.map { it.name })
        assertEquals(listOf("GymClub"), groupOnly.map { it.name })
    }
}
