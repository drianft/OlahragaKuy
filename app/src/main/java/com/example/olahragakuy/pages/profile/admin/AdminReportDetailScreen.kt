package com.example.olahragakuy.pages.profile.admin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.olahragakuy.R
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.components.KuyUserAvatar
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyBlue
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins

private val WarningInputBg = Color(0xFF0E223A)

internal enum class ReportActionOverlay {
    NONE,
    ACTION_MENU,
    CONFIRM_DELETE,
    WARNING_FORM,
}

/**
 * Halaman Detail Laporan Admin beserta overlay Tindakan, Hapus Postingan, dan Notifikasi Warning.
 * Semua overlay dirender in-screen agar dapat diuji secara penuh di Interactive Preview maupun perangkat asli.
 */
@Composable
fun AdminReportDetailScreen(
    report: AdminReportItem,
    onBack: () -> Unit,
    onIgnore: (AdminReportItem) -> Unit,
    onDeletePost: (AdminReportItem) -> Unit,
    onSendWarning: (report: AdminReportItem, title: String, description: String) -> Unit,
    modifier: Modifier = Modifier,
    actionBusy: Boolean = false,
) {
    var activeOverlay by remember(report.id) { mutableStateOf(ReportActionOverlay.NONE) }
    var warningTitle by remember(report.id) { mutableStateOf("Notifikasi Warning") }
    var warningDescription by remember(report.id) { mutableStateOf("") }

    BackHandler {
        if (actionBusy) return@BackHandler
        if (activeOverlay != ReportActionOverlay.NONE) {
            activeOverlay = ReportActionOverlay.NONE
        } else {
            onBack()
        }
    }

    Scaffold(
        modifier = modifier,
        containerColor = HomeNavy,
        contentColor = HomeText,
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .navigationBarsPadding(),
            ) {
                // Header tombol Kembali
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    KuyBackButton(onClick = onBack)
                }

                // Konten utama scrollable
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(6.dp))

                    // Kartu cuplikan postingan yang dilaporkan
                    ReportedPostPreviewCard(report = report)

                    Spacer(Modifier.height(20.dp))

                    // Bagian Detail Laporan
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                    ) {
                        Text(
                            text = "Detail Laporan",
                            color = Color.White,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider(
                            thickness = 1.5.dp,
                            color = HomeOrange,
                        )
                        Spacer(Modifier.height(14.dp))

                        ReportDetailInfoRow(label = "Pelapor", value = report.reporterUsername)
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Tanggal laporan", value = report.reportDate)
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Jenis laporan", value = report.reportCategory)
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Deskripsi", value = report.reportDescription)
                        Spacer(Modifier.height(24.dp))
                    }
                }

                // Tombol bawah: Abaikan & Tindakan
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Button(
                        enabled = !actionBusy,
                        onClick = { onIgnore(report) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KuyBlue,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                    ) {
                        Text(
                            text = "Abaikan",
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }

                    Button(
                        enabled = !actionBusy,
                        onClick = { activeOverlay = ReportActionOverlay.ACTION_MENU },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HomeOrange,
                            contentColor = Color.White,
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                    ) {
                        Text(
                            text = "Tindakan",
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }

            // Scrim gelap saat overlay aktif atau proses Firestore sedang berjalan
            AnimatedVisibility(
                visible = activeOverlay != ReportActionOverlay.NONE || actionBusy,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.6f))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { if (!actionBusy) activeOverlay = ReportActionOverlay.NONE },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (actionBusy && activeOverlay == ReportActionOverlay.NONE) {
                        CircularProgressIndicator(color = HomeOrange)
                    }
                }
            }

            // Overlay 1: Menu Tindakan (Hapus Postingan / Beri Peringatan)
            AnimatedVisibility(
                visible = activeOverlay == ReportActionOverlay.ACTION_MENU,
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit = fadeOut() + scaleOut(targetScale = 0.92f),
                modifier = Modifier.align(Alignment.Center),
            ) {
                AdminActionMenuCard(
                    onDeletePostClick = { activeOverlay = ReportActionOverlay.CONFIRM_DELETE },
                    onGiveWarningClick = {
                        warningTitle = "Notifikasi Warning"
                        warningDescription = ""
                        activeOverlay = ReportActionOverlay.WARNING_FORM
                    },
                )
            }

            // Overlay 2: Konfirmasi Hapus Postingan ("Are you sure? Yes / No")
            AnimatedVisibility(
                visible = activeOverlay == ReportActionOverlay.CONFIRM_DELETE,
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit = fadeOut() + scaleOut(targetScale = 0.92f),
                modifier = Modifier.align(Alignment.Center),
            ) {
                AdminDeletePostConfirmCard(
                    onYes = {
                        activeOverlay = ReportActionOverlay.NONE
                        onDeletePost(report)
                    },
                    onNo = { activeOverlay = ReportActionOverlay.NONE },
                )
            }

            // Overlay 3: Form Notifikasi Warning
            AnimatedVisibility(
                visible = activeOverlay == ReportActionOverlay.WARNING_FORM,
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit = fadeOut() + scaleOut(targetScale = 0.92f),
                modifier = Modifier.align(Alignment.Center),
            ) {
                AdminWarningNotificationCard(
                    title = warningTitle,
                    onTitleChange = { warningTitle = it },
                    description = warningDescription,
                    onDescriptionChange = { warningDescription = it },
                    onSend = {
                        val finalTitle = warningTitle.trim().ifEmpty { "Notifikasi Warning" }
                        val finalDesc = warningDescription.trim()
                        activeOverlay = ReportActionOverlay.NONE
                        onSendWarning(report, finalTitle, finalDesc)
                    },
                )
            }
        }
    }
}

@Composable
private fun ReportedPostPreviewCard(report: AdminReportItem) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = HomeBlue,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                KuyUserAvatar(
                    name = report.reportedUsername,
                    size = 48.dp,
                    fontSize = 18.sp,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = report.reportedUsername,
                        color = Color.White,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                    Text(
                        text = report.postTime,
                        color = HomeOrange,
                        fontFamily = Poppins,
                        fontSize = 12.sp,
                    )
                }
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Opsi postingan",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = report.contentPreview.ifBlank { "(Tanpa caption)" },
                color = HomeText,
                fontFamily = Poppins,
                fontSize = 13.sp,
            )

            Spacer(Modifier.height(10.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth(0.68f)
                    .height(168.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(HomeNavy),
                contentAlignment = Alignment.Center,
            ) {
                if (!report.thumbnailUrl.isNullOrBlank()) {
                    SubcomposeAsyncImage(
                        model = report.thumbnailUrl,
                        contentDescription = "Cuplikan konten dilaporkan",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                        error = { ReportThumbnailFallback(report.type) },
                    )
                } else {
                    ReportThumbnailFallback(report.type)
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                ReportPostStatItem(
                    iconRes = R.drawable.ic_like,
                    contentDescription = "Suka",
                    count = report.likesCount,
                )
                ReportPostStatItem(
                    iconRes = R.drawable.ic_comment,
                    contentDescription = "Komentar",
                    count = report.commentsCount,
                )
                ReportPostStatItem(
                    iconRes = R.drawable.ic_share,
                    contentDescription = "Bagikan",
                    count = report.sharesCount,
                )
            }
        }
    }
}

@Composable
private fun ReportPostStatItem(
    iconRes: Int,
    contentDescription: String,
    count: Int,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = Color.White,
            modifier = Modifier.size(21.dp),
        )
        Text(
            text = count.toString(),
            color = HomeText,
            fontFamily = Poppins,
            fontSize = 12.sp,
        )
    }
}

@Composable
private fun ReportDetailInfoRow(
    label: String,
    value: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            color = HomeText.copy(alpha = 0.9f),
            fontFamily = Poppins,
            fontSize = 12.5.sp,
            modifier = Modifier.width(120.dp),
        )
        Text(
            text = ":",
            color = HomeText.copy(alpha = 0.9f),
            fontFamily = Poppins,
            fontSize = 12.5.sp,
            modifier = Modifier.width(18.dp),
            textAlign = TextAlign.Center,
        )
        Text(
            text = value,
            color = HomeText.copy(alpha = 0.85f),
            fontFamily = Poppins,
            fontSize = 12.5.sp,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
internal fun AdminActionMenuCard(
    onDeletePostClick: () -> Unit,
    onGiveWarningClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(0.84f)
            .widthIn(max = 320.dp),
        shape = RoundedCornerShape(20.dp),
        color = HomeNavy,
        border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.6f)),
        shadowElevation = 18.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onDeletePostClick)
                    .padding(vertical = 22.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Hapus Postingan",
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }

            HorizontalDivider(
                thickness = 1.5.dp,
                color = HomeBlue,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onGiveWarningClick)
                    .padding(vertical = 22.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Beri Peringatan",
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
        }
    }
}

@Composable
internal fun AdminDeletePostConfirmCard(
    onYes: () -> Unit,
    onNo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(0.92f)
            .widthIn(min = 320.dp, max = 380.dp),
        shape = RoundedCornerShape(18.dp),
        color = HomeNavy,
        border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.6f)),
        shadowElevation = 18.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Are you sure?",
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.Normal,
                fontSize = 15.5.sp,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.weight(1f),
            )

            Button(
                onClick = onYes,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KuyButton,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                modifier = Modifier
                    .widthIn(min = 68.dp)
                    .height(38.dp),
            ) {
                Text(
                    text = "Yes",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                )
            }

            Button(
                onClick = onNo,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KuyButton,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 18.dp, vertical = 4.dp),
                modifier = Modifier
                    .widthIn(min = 68.dp)
                    .height(38.dp),
            ) {
                Text(
                    text = "No",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.5.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
internal fun AdminWarningNotificationCard(
    title: String,
    onTitleChange: (String) -> Unit,
    description: String,
    onDescriptionChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(0.90f)
            .widthIn(max = 360.dp),
        shape = RoundedCornerShape(20.dp),
        color = HomeNavy,
        border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.5f)),
        shadowElevation = 18.dp,
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(HomeOrange)
                    .padding(vertical = 14.dp, horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Notifikasi Warning",
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BasicTextField(
                    value = title,
                    onValueChange = onTitleChange,
                    singleLine = true,
                    textStyle = TextStyle(
                        color = HomeText,
                        fontFamily = Poppins,
                        fontSize = 13.sp,
                    ),
                    cursorBrush = SolidColor(HomeOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(WarningInputBg)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterStart) {
                            if (title.isEmpty()) {
                                Text(
                                    text = "Notifikasi Warning",
                                    color = HomeText.copy(alpha = 0.55f),
                                    fontFamily = Poppins,
                                    fontSize = 13.sp,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                BasicTextField(
                    value = description,
                    onValueChange = onDescriptionChange,
                    textStyle = TextStyle(
                        color = HomeText,
                        fontFamily = Poppins,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                    ),
                    cursorBrush = SolidColor(HomeOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 110.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(WarningInputBg)
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.TopStart) {
                            if (description.isEmpty()) {
                                Text(
                                    text = "Type your description here...",
                                    color = HomeText.copy(alpha = 0.55f),
                                    fontFamily = Poppins,
                                    fontSize = 13.sp,
                                )
                            }
                            innerTextField()
                        }
                    },
                )

                Button(
                    onClick = onSend,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HomeOrange,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 6.dp),
                    modifier = Modifier
                        .align(Alignment.End)
                        .height(38.dp),
                ) {
                    Text(
                        text = "Send",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                    )
                }
            }
        }
    }
}

@Preview(name = "Report Detail", showBackground = true, showSystemUi = true)
@Composable
private fun AdminReportDetailPreview() {
    OlahragaKuyTheme {
        AdminReportDetailScreen(
            report = SampleAdminReports.first(),
            onBack = {},
            onIgnore = {},
            onDeletePost = {},
            onSendWarning = { _, _, _ -> },
        )
    }
}

@Preview(name = "Overlay Tindakan", showBackground = true)
@Composable
private fun AdminActionMenuOverlayPreview() {
    OlahragaKuyTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFF06101B))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AdminActionMenuCard(
                onDeletePostClick = {},
                onGiveWarningClick = {},
            )
        }
    }
}

@Preview(name = "Overlay Hapus Postingan", showBackground = true)
@Composable
private fun AdminDeleteConfirmOverlayPreview() {
    OlahragaKuyTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFF06101B))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AdminDeletePostConfirmCard(
                onYes = {},
                onNo = {},
            )
        }
    }
}

@Preview(name = "Overlay Peringatan", showBackground = true)
@Composable
private fun AdminWarningOverlayPreview() {
    OlahragaKuyTheme {
        Box(
            modifier = Modifier
                .background(Color(0xFF06101B))
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            AdminWarningNotificationCard(
                title = "Notifikasi Warning",
                onTitleChange = {},
                description = "",
                onDescriptionChange = {},
                onSend = {},
            )
        }
    }
}
