package com.example.olahragakuy.ui.components

import android.net.Uri
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.SubcomposeAsyncImage
import com.example.olahragakuy.R
import com.example.olahragakuy.pages.posts.DefaultPostCategories
import com.example.olahragakuy.pages.posts.FollowingFriend
import com.example.olahragakuy.pages.posts.MAX_POST_REPORT_DETAIL_LENGTH
import com.example.olahragakuy.pages.posts.PostReportReason
import com.example.olahragakuy.pages.posts.PostUiModel
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.KuyError
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins

@Composable
fun HomePlaceholder(
    label: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp),
) {
    Box(
        modifier
            .clip(shape)
            .background(Color(0xFF315575))
            .border(1.dp, Color(0xFF6587A4), shape)
            .padding(4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            label,
            color = HomeText.copy(alpha = 0.8f),
            fontSize = 10.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun KuyUserAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    fontSize: TextUnit = 22.sp,
    photoUrl: String? = null,
    backgroundColor: Color = Color(0xFFFFE35B),
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            name.take(1).uppercase(),
            color = HomeNavy,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
        )
        if (photoUrl != null) {
            AsyncImage(
                model = photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
fun StreakBanner(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    streakDaysCount: Int = 0,
) {
    Row(
        modifier
            .padding(horizontal = 20.dp, vertical = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Brush.linearGradient(listOf(Color(0xFFFFA183), HomeOrange)))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                "Pertahankan Streak anda!",
                color = Color(0xFF713000),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("🔥", fontSize = 30.sp)
                Text(
                    text = streakDaysCount.coerceAtLeast(0).toString(),
                    color = HomeText,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
            OutlinedButton(
                onClick = onClick,
                contentPadding = PaddingValues(horizontal = 28.dp, vertical = 0.dp),
                border = BorderStroke(1.5.dp, Color(0xFF8C3C00)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color(0xFFE9E9E9),
                    contentColor = Color(0xFF8C3C00),
                ),
            ) { Text("Pergi") }
        }
        Image(
            painterResource(R.drawable.login_wordmark),
            "Ilustrasi olahraga bersama",
            Modifier.width(100.dp).height(120.dp),
        )
    }
}

@Composable
fun CreatePostPromptBar(
    userDisplayName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(HomeBlue)
            .clickable(role = Role.Button, onClickLabel = "Buat postingan", onClick = onClick)
            .padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        HomePlaceholder("Isi avatar\ndi sini", Modifier.size(40.dp), CircleShape)
        Text(
            "Apa yang anda pikirkan, $userDisplayName?",
            modifier = Modifier
                .weight(1f)
                .border(1.dp, HomeOrange, RoundedCornerShape(16.dp))
                .padding(16.dp),
            color = HomeText.copy(alpha = 0.75f),
            fontSize = 12.sp,
        )
        Text("Foto +", color = HomeOrange, fontSize = 12.sp)
    }
}

@Composable
fun HomePostCard(
    post: PostUiModel,
    onAction: (String) -> Unit,
    onCommentClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLiked: Boolean = false,
    onLikeClick: (() -> Unit)? = null,
    onAuthorClick: (() -> Unit)? = null,
    preparedImage: androidx.compose.ui.graphics.painter.Painter? = null,
    isOwnPost: Boolean = false,
    isReported: Boolean = false,
    onEditClick: (() -> Unit)? = null,
    onCopyClick: (() -> Unit)? = null,
    onShareClick: (() -> Unit)? = null,
    onDeleteClick: (() -> Unit)? = null,
    onReportClick: (() -> Unit)? = null,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val showEdit = isOwnPost && (onEditClick != null)
    val showDelete = isOwnPost && (onDeleteClick != null)
    val showReport = !isOwnPost && (onReportClick != null)
    val hasTopActions = showEdit || (onCopyClick != null) || (onShareClick != null)
    val hasBottomAction = showDelete || showReport
    val hasMenuActions = hasTopActions || hasBottomAction

    Column(
        modifier
            .fillMaxWidth()
            .background(HomeBlue)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val avatarPalette = listOf(
                Color(0xFF5B7CFA),
                Color(0xFF4FC3A1),
                Color(0xFFF2B84B),
                Color(0xFF7FA7E8),
            )
            val avatarColor = avatarPalette[post.authorName.hashCode().mod(avatarPalette.size)]
            Row(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (onAuthorClick != null) {
                            Modifier.clickable(
                                role = Role.Button,
                                onClickLabel = "Buka profil ${post.authorName}",
                                onClick = onAuthorClick,
                            )
                        } else {
                            Modifier
                        },
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KuyUserAvatar(
                    name = post.authorName,
                    size = 50.dp,
                    fontSize = 20.sp,
                    backgroundColor = avatarColor,
                )
                Column(Modifier.weight(1f)) {
                    Text(post.authorName, color = HomeText, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    if (post.taggedNames.isNotEmpty()) {
                        Text(
                            "bersama ${post.taggedNames.joinToString()}",
                            color = HomeText.copy(alpha = 0.8f),
                            fontSize = 12.sp,
                        )
                    }
                    if (post.category.isNotBlank()) {
                        Text(
                            post.category,
                            color = HomeText.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                        )
                    }
                    Text(post.time, color = HomeOrange, fontSize = 12.sp)
                }
            }
            Box {
                IconButton(
                    onClick = {
                        if (hasMenuActions) {
                            menuExpanded = true
                        } else {
                            onAction("Menu postingan belum tersedia.")
                        }
                    },
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "Opsi postingan",
                        tint = HomeText,
                        modifier = Modifier.size(26.dp),
                    )
                }
                if (hasMenuActions) {
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        containerColor = Color(0xFF16324F),
                    ) {
                        if (showEdit) {
                            DropdownMenuItem(
                                text = { Text("Edit Caption", color = HomeText, fontFamily = Poppins) },
                                leadingIcon = {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = HomeText)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onEditClick.invoke()
                                },
                            )
                        }
                        if (onCopyClick != null) {
                            DropdownMenuItem(
                                text = { Text("Salin Caption", color = HomeText, fontFamily = Poppins) },
                                leadingIcon = {
                                    Icon(Icons.Default.ContentCopy, contentDescription = null, tint = HomeText)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onCopyClick()
                                },
                            )
                        }
                        if (onShareClick != null) {
                            DropdownMenuItem(
                                text = { Text("Bagikan", color = HomeText, fontFamily = Poppins) },
                                leadingIcon = {
                                    Icon(Icons.Default.Share, contentDescription = null, tint = HomeText)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onShareClick()
                                },
                            )
                        }
                        if (hasTopActions && hasBottomAction) {
                            HorizontalDivider(color = Color(0xFF4A86C8).copy(alpha = 0.25f))
                        }
                        if (showDelete) {
                            val deleteRed = Color(0xFFE53935)
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = "Hapus Postingan",
                                        color = deleteRed,
                                        fontFamily = Poppins,
                                        fontWeight = FontWeight.SemiBold,
                                    )
                                },
                                leadingIcon = {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = deleteRed)
                                },
                                onClick = {
                                    menuExpanded = false
                                    onDeleteClick.invoke()
                                },
                            )
                        } else if (showReport) {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = if (isReported) "Sudah Dilaporkan" else "Laporkan Postingan",
                                        color = if (isReported) HomeText.copy(alpha = 0.55f) else HomeOrange,
                                        fontFamily = Poppins,
                                        fontWeight = FontWeight.Medium,
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Flag,
                                        contentDescription = null,
                                        tint = if (isReported) HomeText.copy(alpha = 0.55f) else HomeOrange,
                                    )
                                },
                                enabled = !isReported,
                                onClick = {
                                    menuExpanded = false
                                    onReportClick.invoke()
                                },
                            )
                        }
                    }
                }
            }
        }
        Text(post.caption, color = HomeText, fontSize = 14.sp)
        if (post.imageUrl != null && preparedImage != null) {
            Image(
                painter = preparedImage,
                contentDescription = "Foto postingan oleh ${post.authorName}",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxWidth().aspectRatio(1.28f)
                    .clip(RoundedCornerShape(24.dp)),
            )
        } else if (post.imageUrl != null) {
            SubcomposeAsyncImage(
                model = post.imageUrl,
                contentDescription = "Foto postingan oleh ${post.authorName}",
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.28f)
                    .clip(RoundedCornerShape(24.dp)),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = HomeOrange)
                    }
                },
                error = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Foto gagal dimuat", color = HomeText)
                    }
                },
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PostStatButton(
                iconRes = if (isLiked) R.drawable.ic_likes else R.drawable.ic_like,
                contentDescription = "Suka",
                count = post.likesCount,
                fontSize = 14.sp,
                iconTint = if (isLiked) HomeOrange else HomeText,
                onClick = {
                    if (onLikeClick != null) onLikeClick()
                    else onAction("Fitur suka belum tersedia.")
                },
            )
            PostStatButton(
                iconRes = R.drawable.ic_comment,
                contentDescription = "Komentar",
                count = post.commentsCount,
                onClick = onCommentClick,
            )
            PostStatButton(
                iconRes = R.drawable.ic_share,
                contentDescription = "Bagikan",
                count = post.sharesCount,
                onClick = { onAction("Fitur bagikan belum tersedia.") },
            )
        }
        if (post.likedByNames.isNotEmpty()) {
            LikedBySummaryRow(likedByNames = post.likedByNames)
        }
    }
}

@Composable
private fun LikedBySummaryRow(likedByNames: List<String>) {
    val avatarColors = listOf(Color(0xFF4FC3A1), Color(0xFFF2B84B), Color(0xFF5B7CFA))
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
            likedByNames.take(2).forEachIndexed { index, name ->
                KuyUserAvatar(
                    name = name,
                    size = 18.dp,
                    fontSize = 9.sp,
                    backgroundColor = avatarColors[index % avatarColors.size],
                )
            }
        }
        Text(
            text = buildAnnotatedString {
                append("disukai oleh ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(likedByNames.take(3).joinToString(", "))
                }
            },
            color = HomeText,
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun PostStatButton(
    @DrawableRes iconRes: Int,
    contentDescription: String,
    count: Long,
    fontSize: TextUnit = 12.sp,
    iconTint: Color = HomeText,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = contentDescription,
                tint = iconTint,
                modifier = Modifier.size(20.dp),
            )
            Text("$count", color = HomeText, fontSize = fontSize)
        }
    }
}

@Composable
fun SelectedPhotoSection(
    uri: Uri,
    enabled: Boolean,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Foto yang dipilih",
            color = HomeText,
            style = MaterialTheme.typography.titleSmall,
        )
        AsyncImage(
            model = uri,
            contentDescription = "Preview foto postingan",
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(HomeBlue),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(enabled = enabled, onClick = onChangePhoto) {
                Text("Ganti Foto", color = HomeText)
            }
            TextButton(enabled = enabled, onClick = onRemovePhoto) {
                Text("Hapus", color = HomeOrange)
            }
        }
        Text(
            "Foto akan diunggah saat kamu menekan Posting.",
            color = HomeText.copy(alpha = 0.7f),
            style = MaterialTheme.typography.bodySmall,
        )
    }
}

@Composable
fun TaggedFriendsRow(
    friends: List<FollowingFriend>,
    enabled: Boolean,
    onRemove: (FollowingFriend) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Bersama", color = HomeText, style = MaterialTheme.typography.titleSmall)
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            friends.forEach { friend ->
                Surface(
                    onClick = { onRemove(friend) },
                    enabled = enabled,
                    shape = RoundedCornerShape(20.dp),
                    color = HomeBlue,
                    contentColor = HomeText,
                ) {
                    Text(
                        "${friend.displayName}  ✕",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun PostActionCard(
    label: String,
    photo: Boolean,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.height(106.dp),
        shape = RoundedCornerShape(24.dp),
        color = HomeBlue,
        contentColor = HomeText,
    ) {
        Column(
            Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Canvas(Modifier.size(34.dp)) {
                val tint = Color(0xFFB5CFF3)
                if (photo) {
                    drawRoundRect(
                        Color(0xFF85B9CF),
                        topLeft = Offset(size.width * 0.1f, size.height * 0.08f),
                        size = Size(size.width * 0.75f, size.height * 0.66f),
                        cornerRadius = CornerRadius(3.dp.toPx()),
                    )
                    drawCircle(tint, size.width * 0.08f, Offset(size.width * 0.64f, size.height * 0.25f))
                    val mountain = Path().apply {
                        moveTo(size.width * 0.2f, size.height * 0.62f)
                        lineTo(size.width * 0.4f, size.height * 0.35f)
                        lineTo(size.width * 0.65f, size.height * 0.62f)
                        close()
                    }
                    drawPath(mountain, tint)
                    val center = Offset(size.width * 0.77f, size.height * 0.72f)
                    drawCircle(Color(0xFF55BD4D), size.width * 0.18f, center)
                    drawLine(Color.White, center - Offset(4.dp.toPx(), 0f), center + Offset(4.dp.toPx(), 0f), 2.dp.toPx())
                    drawLine(Color.White, center - Offset(0f, 4.dp.toPx()), center + Offset(0f, 4.dp.toPx()), 2.dp.toPx())
                } else {
                    drawCircle(tint, size.width * 0.2f, Offset(size.width * 0.38f, size.height * 0.24f))
                    drawRoundRect(
                        tint,
                        topLeft = Offset(size.width * 0.05f, size.height * 0.5f),
                        size = Size(size.width * 0.65f, size.height * 0.35f),
                        cornerRadius = CornerRadius(6.dp.toPx()),
                    )
                    val tag = Path().apply {
                        moveTo(size.width * 0.6f, size.height * 0.48f)
                        lineTo(size.width * 0.77f, size.height * 0.48f)
                        lineTo(size.width * 0.98f, size.height * 0.7f)
                        lineTo(size.width * 0.8f, size.height * 0.88f)
                        lineTo(size.width * 0.58f, size.height * 0.66f)
                        close()
                    }
                    drawPath(tag, HomeBlue, style = Stroke(4.dp.toPx()))
                    drawPath(tag, tint)
                    drawCircle(HomeBlue, 1.5.dp.toPx(), Offset(size.width * 0.68f, size.height * 0.57f))
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                label,
                textAlign = TextAlign.Center,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

@Composable
fun PostCategoryInputSection(
    category: String,
    enabled: Boolean,
    onCategoryChange: (String) -> Unit,
    onToggleQuickTag: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "Topik / Kata Kunci Pencarian",
            color = HomeText,
            style = MaterialTheme.typography.titleSmall,
        )
        OutlinedTextField(
            value = category,
            onValueChange = onCategoryChange,
            enabled = enabled,
            singleLine = true,
            placeholder = {
                Text("Misal: Makanan Sehat, Foto Orang, Bulking...", fontSize = 11.sp)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = HomeText),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = HomeOrange,
                unfocusedBorderColor = HomeOrange,
                focusedTextColor = HomeText,
                unfocusedTextColor = HomeText,
                focusedPlaceholderColor = HomeText.copy(alpha = 0.7f),
                unfocusedPlaceholderColor = HomeText.copy(alpha = 0.7f),
                cursorColor = HomeOrange,
            ),
        )
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val activeTags = category.split(",")
                .map { it.trim().lowercase() }
                .filter { it.isNotEmpty() }
            DefaultPostCategories.forEach { tag ->
                val isSelected = tag.lowercase() in activeTags
                Surface(
                    onClick = { onToggleQuickTag(tag) },
                    enabled = enabled,
                    shape = RoundedCornerShape(20.dp),
                    color = if (isSelected) KuyButton else HomeBlue,
                    border = BorderStroke(1.dp, if (isSelected) HomeOrange else Color.Transparent),
                    contentColor = HomeText,
                ) {
                    Text(
                        tag,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
fun KuyPostSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Cari..",
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(
            color = HomeText,
            fontSize = 12.sp,
        ),
        cursorBrush = SolidColor(HomeOrange),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch() }),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        decorationBox = { innerTextField ->
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = HomeNavy,
                border = BorderStroke(1.dp, KuyButton),
            ) {
                Row(
                    Modifier
                        .fillMaxSize()
                        .padding(start = 18.dp, end = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                placeholder,
                                color = HomeText.copy(alpha = 0.85f),
                                fontSize = 12.sp,
                            )
                        }
                        innerTextField()
                    }
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(role = Role.Button, onClick = onSearch),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_search),
                            contentDescription = "Cari postingan",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                }
            }
        },
    )
}

@Composable
fun ReportPostDialog(
    authorName: String,
    submitting: Boolean,
    errorMessage: String?,
    onDismiss: () -> Unit,
    onSubmit: (PostReportReason, String) -> Unit,
) {
    var selectedReason by remember { mutableStateOf<PostReportReason?>(null) }
    var detail by remember { mutableStateOf("") }
    val requiresDetail = selectedReason == PostReportReason.OTHER
    val canSubmit = !submitting &&
        selectedReason != null &&
        (!requiresDetail || detail.trim().isNotEmpty())

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        containerColor = Color(0xFF16324F),
        titleContentColor = HomeText,
        textContentColor = HomeText,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Flag,
                    contentDescription = null,
                    tint = HomeOrange,
                    modifier = Modifier.size(22.dp),
                )
                Text(
                    text = "Laporkan Postingan",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Pilih alasan melaporkan postingan dari $authorName. Laporanmu bersifat rahasia.",
                    fontFamily = Poppins,
                    fontSize = 12.sp,
                    color = HomeText.copy(alpha = 0.8f),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    PostReportReason.entries.forEach { reason ->
                        val selected = selectedReason == reason
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (selected) HomeOrange.copy(alpha = 0.14f) else Color.Transparent,
                                )
                                .selectable(
                                    selected = selected,
                                    enabled = !submitting,
                                    role = Role.RadioButton,
                                    onClick = { selectedReason = reason },
                                )
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selected,
                                onClick = null,
                                enabled = !submitting,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = HomeOrange,
                                    unselectedColor = HomeText.copy(alpha = 0.65f),
                                ),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = reason.label,
                                fontFamily = Poppins,
                                fontSize = 13.sp,
                                color = if (selected) HomeText else HomeText.copy(alpha = 0.9f),
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = detail,
                    onValueChange = { next ->
                        if (next.length <= MAX_POST_REPORT_DETAIL_LENGTH) {
                            detail = next
                        }
                    },
                    enabled = !submitting,
                    label = {
                        Text(
                            text = if (requiresDetail) {
                                "Jelaskan alasan laporan *"
                            } else {
                                "Catatan tambahan (opsional)"
                            },
                            fontFamily = Poppins,
                            fontSize = 12.sp,
                        )
                    },
                    supportingText = {
                        Text(
                            text = "${detail.length}/$MAX_POST_REPORT_DETAIL_LENGTH",
                            fontFamily = Poppins,
                            fontSize = 11.sp,
                            color = HomeText.copy(alpha = 0.65f),
                        )
                    },
                    minLines = 2,
                    maxLines = 4,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HomeOrange,
                        unfocusedBorderColor = HomeText.copy(alpha = 0.35f),
                        focusedTextColor = HomeText,
                        unfocusedTextColor = HomeText,
                        focusedLabelColor = HomeOrange,
                        unfocusedLabelColor = HomeText.copy(alpha = 0.7f),
                        cursorColor = HomeOrange,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                if (!errorMessage.isNullOrBlank()) {
                    Text(
                        text = errorMessage,
                        color = KuyError,
                        fontFamily = Poppins,
                        fontSize = 12.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSubmit,
                onClick = { selectedReason?.let { onSubmit(it, detail.trim()) } },
            ) {
                if (submitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = HomeOrange,
                    )
                } else {
                    Text(
                        text = "Kirim Laporan",
                        color = if (canSubmit) HomeOrange else HomeText.copy(alpha = 0.4f),
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = !submitting,
                onClick = onDismiss,
            ) {
                Text(
                    text = "Batal",
                    color = HomeText.copy(alpha = 0.8f),
                    fontFamily = Poppins,
                )
            }
        },
    )
}

@Preview(name = "Dialog Laporkan Postingan", showBackground = true, widthDp = 393, heightDp = 700)
@Composable
private fun ReportPostDialogPreview() {
    OlahragaKuyTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(HomeNavy),
        ) {
            ReportPostDialog(
                authorName = "SayurOlahraga",
                submitting = false,
                errorMessage = null,
                onDismiss = {},
                onSubmit = { _, _ -> },
            )
        }
    }
}
