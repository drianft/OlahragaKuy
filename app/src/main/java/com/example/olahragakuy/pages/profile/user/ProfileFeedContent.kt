package com.example.olahragakuy.pages.profile.user

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage

private val FeedMenuBg = Color(0xFF16324F)

@Composable
internal fun ProfileFeedContent(
    username: String,
    posts: List<UserPost>,
    onPostClick: (UserPost) -> Unit,
    modifier: Modifier = Modifier,
    onDeletePost: ((UserPost) -> Unit)? = null,
    onEditPost: ((UserPost, String) -> Unit)? = null,
    onReportPost: ((UserPost) -> Unit)? = null,
    header: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var postPendingDelete by remember { mutableStateOf<UserPost?>(null) }
    var postPendingEdit by remember { mutableStateOf<UserPost?>(null) }
    var editCaptionInput by remember { mutableStateOf("") }
    var infoMessage by remember { mutableStateOf<String?>(null) }

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
                        post = post,
                        onClick = { onPostClick(post) },
                        canEdit = onEditPost != null,
                        canDelete = onDeletePost != null,
                        onEditClick = {
                            editCaptionInput = post.caption
                            postPendingEdit = post
                        },
                        onCopyClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                            clipboard?.setPrimaryClip(ClipData.newPlainText("Caption", post.caption))
                            Toast.makeText(context, "Caption disalin", Toast.LENGTH_SHORT).show()
                        },
                        onShareClick = {
                            val shareText = buildString {
                                append("$username: ${post.caption}")
                                if (!post.imageUrl.isNullOrBlank()) {
                                    append("\n${post.imageUrl}")
                                }
                            }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, shareText)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Bagikan postingan"),
                            )
                        },
                        onDeleteClick = {
                            postPendingDelete = post
                        },
                        onReportClick = {
                            if (onReportPost != null) {
                                onReportPost(post)
                            } else {
                                infoMessage = "Terima kasih, laporan untuk postingan ini telah dicatat."
                            }
                        },
                    )
                }
                Spacer(Modifier.height(10.dp))
            }
        }
    }

    postPendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { postPendingDelete = null },
            title = { Text("Hapus Postingan?") },
            text = { Text("Postingan ini akan dihapus secara permanen dan tidak dapat dikembalikan.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        postPendingDelete = null
                        onDeletePost?.invoke(target)
                    },
                ) {
                    Text("Hapus", color = RedLogout, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { postPendingDelete = null }) {
                    Text("Batal")
                }
            },
        )
    }

    postPendingEdit?.let { target ->
        AlertDialog(
            onDismissRequest = { postPendingEdit = null },
            title = { Text("Edit Caption") },
            text = {
                OutlinedTextField(
                    value = editCaptionInput,
                    onValueChange = { editCaptionInput = it },
                    label = { Text("Caption") },
                    singleLine = false,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth(),
                )
            },
            confirmButton = {
                TextButton(
                    enabled = editCaptionInput.trim().isNotEmpty(),
                    onClick = {
                        val updated = editCaptionInput.trim()
                        postPendingEdit = null
                        onEditPost?.invoke(target, updated)
                    },
                ) {
                    Text("Simpan", color = OrangeBright, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { postPendingEdit = null }) {
                    Text("Batal")
                }
            },
        )
    }

    infoMessage?.let { msg ->
        AlertDialog(
            onDismissRequest = { infoMessage = null },
            title = { Text("Informasi") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = { infoMessage = null }) {
                    Text("Mengerti")
                }
            },
        )
    }
}

@Composable
private fun PostItem(
    username: String,
    post: UserPost,
    onClick: () -> Unit = {},
    canEdit: Boolean = false,
    canDelete: Boolean = false,
    onEditClick: () -> Unit = {},
    onCopyClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {},
    onReportClick: () -> Unit = {},
) {
    var menuExpanded by remember { mutableStateOf(false) }

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
                    text = post.time,
                    color = OrangeBright,
                    fontFamily = Poppins,
                    fontSize = 15.sp,
                )
            }
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi postingan",
                        tint = TextWhite,
                        modifier = Modifier.size(28.dp),
                    )
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    containerColor = FeedMenuBg,
                ) {
                    if (canEdit) {
                        DropdownMenuItem(
                            text = { Text("Edit Caption", color = TextWhite, fontFamily = Poppins) },
                            leadingIcon = {
                                Icon(Icons.Default.Edit, contentDescription = null, tint = TextWhite)
                            },
                            onClick = {
                                menuExpanded = false
                                onEditClick()
                            },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Salin Caption", color = TextWhite, fontFamily = Poppins) },
                        leadingIcon = {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, tint = TextWhite)
                        },
                        onClick = {
                            menuExpanded = false
                            onCopyClick()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Bagikan", color = TextWhite, fontFamily = Poppins) },
                        leadingIcon = {
                            Icon(Icons.Default.Share, contentDescription = null, tint = TextWhite)
                        },
                        onClick = {
                            menuExpanded = false
                            onShareClick()
                        },
                    )
                    HorizontalDivider(color = BlueTab.copy(alpha = 0.25f))
                    if (canDelete) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    "Hapus Postingan",
                                    color = RedLogout,
                                    fontFamily = Poppins,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = RedLogout)
                            },
                            onClick = {
                                menuExpanded = false
                                onDeleteClick()
                            },
                        )
                    } else {
                        DropdownMenuItem(
                            text = { Text("Laporkan Postingan", color = OrangeBright, fontFamily = Poppins) },
                            leadingIcon = {
                                Icon(Icons.Default.Flag, contentDescription = null, tint = OrangeBright)
                            },
                            onClick = {
                                menuExpanded = false
                                onReportClick()
                            },
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            text = post.caption,
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
            if (post.imageUrl != null) {
                SubcomposeAsyncImage(
                    model = post.imageUrl,
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
