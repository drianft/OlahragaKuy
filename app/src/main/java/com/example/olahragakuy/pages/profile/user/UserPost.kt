package com.example.olahragakuy.pages.profile.user

data class UserPost(
    val id: Int,
    val caption: String,
    val time: String,
    val imageUrl: String? = null,
    val documentId: String = id.toString(),
)
