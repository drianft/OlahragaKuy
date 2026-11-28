package com.example.olahragakuy.pages.profile.user

import com.example.olahragakuy.pages.posts.FollowingFriend

data class UserPost(
    val id: Int,
    val caption: String,
    val time: String,
    val imageUrl: String? = null,
    val documentId: String = id.toString(),
    val category: String = "",
    val taggedNames: List<String> = emptyList(),
    val taggedFriends: List<FollowingFriend> = emptyList(),
)
