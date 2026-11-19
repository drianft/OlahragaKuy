package com.example.olahragakuy.pages.profile.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

@Composable
internal fun ProfileFeedContent(
    username: String,
    posts: List<UserPost>,
    onPostClick: (UserPost) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
) {
    LazyColumn(modifier = modifier) {
        item { header() }
        if (posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Belum ada postingan",
                        color = TextWhite.copy(alpha = 0.6f),
                        fontFamily = Poppins,
                        fontSize = 15.sp,
                    )
                }
            }
        } else {
            items(posts, key = { it.documentId }) { post ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(NavyPost)
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                ) {
                    PostItem(
                        username = username,
                        time = post.time,
                        caption = post.caption,
                        imageUrl = post.imageUrl,
                        onClick = { onPostClick(post) },
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

@Composable
private fun PostItem(
    username: String,
    time: String,
    caption: String,
    imageUrl: String? = null,
    onClick: () -> Unit = {},
) {
    Column(modifier = Modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProfileAvatar(size = 64.dp)
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = username,
                    color = TextWhite,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
                Text(
                    text = time,
                    color = OrangeBright,
                    fontFamily = Poppins,
                    fontSize = 15.sp,
                )
            }
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opsi",
                tint = TextWhite,
                modifier = Modifier.size(28.dp),
            )
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = caption,
            color = TextWhite,
            fontFamily = Poppins,
            fontSize = 16.sp,
        )
        Spacer(Modifier.height(14.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(NavyDark),
            contentAlignment = Alignment.Center,
        ) {
            if (imageUrl != null) {
                SubcomposeAsyncImage(
                    model = imageUrl,
                    contentDescription = "Foto postingan oleh $username",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    loading = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = OrangeBright)
                        }
                    },
                    error = {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = OrangeBright,
                                modifier = Modifier.size(72.dp),
                            )
                        }
                    },
                )
            } else {
                Icon(
                    imageVector = Icons.Default.FitnessCenter,
                    contentDescription = null,
                    tint = OrangeBright,
                    modifier = Modifier.size(72.dp),
                )
            }
        }
    }
}
