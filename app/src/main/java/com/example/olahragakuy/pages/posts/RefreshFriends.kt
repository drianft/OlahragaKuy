package com.example.olahragakuy.pages.posts

import com.example.olahragakuy.ui.components.awaitRefresh
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source

/** Reload the same friend sources used by loadFirebaseFriends, with one final result. */
suspend fun refreshFirebaseFriends(uid: String?, currentUserName: String): List<FollowingFriend> {
    val db = FirebaseFirestore.getInstance()
    val collected = mutableListOf<FollowingFriend>()
    if (uid != null) {
        val userRef = db.collection("users").document(uid)
        userRef.collection("following").get(Source.SERVER).awaitRefresh().documents.forEach { doc ->
            followingFriendFrom(doc.id, doc.data.orEmpty())?.let(collected::add)
        }
        val user = userRef.get(Source.SERVER).awaitRefresh()
        (user.get("following") as? List<*>).orEmpty().forEachIndexed { index, entry ->
            when (entry) {
                is String -> if (entry.isNotBlank()) collected.add(FollowingFriend(entry.trim(), entry.trim()))
                is Map<*, *> -> {
                    @Suppress("UNCHECKED_CAST")
                    followingFriendFrom(
                        (entry["uid"] as? String) ?: "follow_$index",
                        entry as Map<String, Any?>,
                    )?.let(collected::add)
                }
            }
        }
    }
    db.collection("users").limit(50).get(Source.SERVER).awaitRefresh().documents.forEach { doc ->
        followingFriendFrom(doc.id, doc.data.orEmpty())?.let(collected::add)
    }
    db.collection("posts").limit(50).get(Source.SERVER).awaitRefresh().documents.forEach { doc ->
        val name = doc.getString("authorName")?.trim().orEmpty()
        val id = doc.getString("authorId")?.trim().orEmpty().ifEmpty { "author_${name.lowercase()}" }
        if (name.isNotEmpty()) collected.add(FollowingFriend(id, name))
        (doc.get("taggedFriends") as? List<*>).orEmpty().forEach { entry ->
            val data = entry as? Map<*, *> ?: return@forEach
            val friendId = (data["uid"] as? String)?.trim().orEmpty()
            val friendName = (data["displayName"] as? String)?.trim().orEmpty()
            if (friendId.isNotEmpty() && friendName.isNotEmpty()) {
                collected.add(FollowingFriend(friendId, friendName))
            }
        }
    }
    return sortFollowingFriends(collected.filterNot {
        it.uid == uid || (currentUserName.isNotBlank() && it.displayName.equals(currentUserName, true))
    })
}
