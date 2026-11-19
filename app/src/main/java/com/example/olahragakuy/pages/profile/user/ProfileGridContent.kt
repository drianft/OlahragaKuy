package com.example.olahragakuy.pages.profile.user

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

@Composable
internal fun ProfileGridContent(
    posts: List<UserPost>,
    tagged: Boolean,
    onPostClick: (UserPost) -> Unit,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
) {
    val rows = posts.chunked(3)
    val cornerRadius = 18.dp
    val gridGap = 2.dp
    val borderColor = BlueTab.copy(alpha = 0.7f)

    LazyColumn(
        modifier = modifier,
    ) {
        item { header() }
        if (posts.isEmpty()) {
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (tagged) "Tidak ada postingan yang ditandai" else "Belum ada postingan",
                        color = TextWhite.copy(alpha = 0.6f),
                        fontFamily = Poppins,
                        fontSize = 15.sp,
                    )
                }
            }
        } else {
            itemsIndexed(
                items = rows,
                key = { rowIndex, row -> "row_${rowIndex}_${row.first().documentId}" },
            ) { rowIndex, rowPosts ->
                val isFirstRow = rowIndex == 0
                val isLastRow = rowIndex == rows.lastIndex
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = if (isLastRow) 20.dp else gridGap),
                    horizontalArrangement = Arrangement.spacedBy(gridGap),
                ) {
                    rowPosts.forEachIndexed { colIndex, post ->
                        val isLastInSingleRow = rows.size == 1 && colIndex == rowPosts.lastIndex
                        val tileShape = RoundedCornerShape(
                            topStart = if (isFirstRow && colIndex == 0) cornerRadius else 0.dp,
                            topEnd = if (isFirstRow && (colIndex == 2 || isLastInSingleRow)) cornerRadius else 0.dp,
                            bottomStart = if (isLastRow && colIndex == 0) cornerRadius else 0.dp,
                            bottomEnd = if (isLastRow && (colIndex == rowPosts.lastIndex)) cornerRadius else 0.dp,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(0.9f)
                                .clip(tileShape)
                                .background(NavyPost)
                                .border(0.75.dp, borderColor, tileShape)
                                .clickable { onPostClick(post) },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (post.imageUrl != null) {
                                SubcomposeAsyncImage(
                                    model = post.imageUrl,
                                    contentDescription = post.caption,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                    loading = {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            CircularProgressIndicator(
                                                color = OrangeBright,
                                                modifier = Modifier.size(24.dp),
                                                strokeWidth = 2.dp,
                                            )
                                        }
                                    },
                                    error = {
                                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = Icons.Default.FitnessCenter,
                                                contentDescription = post.caption,
                                                tint = OrangeBright,
                                                modifier = Modifier.size(40.dp),
                                            )
                                        }
                                    },
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FitnessCenter,
                                    contentDescription = post.caption,
                                    tint = OrangeBright,
                                    modifier = Modifier.size(40.dp),
                                )
                            }
                        }
                    }
                    repeat(3 - rowPosts.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1D3D)
@Composable
private fun ProfileGridContentPreview() {
    ProfileGridContent(
        posts = List(5) { idx ->
            UserPost(id = idx + 1, caption = "Post ${idx + 1}", time = "Baru saja")
        },
        tagged = false,
        onPostClick = {},
        header = {},
    )
}
