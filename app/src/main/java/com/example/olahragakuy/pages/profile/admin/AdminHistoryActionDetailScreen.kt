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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyBlue
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins

private val HistoryDeletePermanentColor = Color(0xFFD32F2F)

private enum class HistoryDetailOverlay {
    NONE,
    CONFIRM_DELETE_PERMANENT,
    CONFIRM_RESTORE,
}

/**
 * Halaman Detail Riwayat Tindakan Admin.
 * Menampilkan cuplikan konten, detail laporan & tindakan moderasi, serta aksi
 * Hapus Permanen dan Pulihkan dengan overlay konfirmasi in-screen.
 */
@Composable
fun AdminHistoryActionDetailScreen(
    item: AdminActionHistoryItem,
    onBack: () -> Unit,
    onPermanentDelete: (AdminActionHistoryItem) -> Unit,
    onRestore: (AdminActionHistoryItem) -> Unit,
    modifier: Modifier = Modifier,
    actionBusy: Boolean = false,
) {
    var activeOverlay by remember(item.id) { mutableStateOf(HistoryDetailOverlay.NONE) }

    BackHandler {
        if (actionBusy) return@BackHandler
        if (activeOverlay != HistoryDetailOverlay.NONE) {
            activeOverlay = HistoryDetailOverlay.NONE
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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                ) {
                    KuyBackButton(onClick = onBack)
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                ) {
                    Spacer(Modifier.height(6.dp))

                    ReportedPostPreviewCard(report = item.report)

                    Spacer(Modifier.height(20.dp))

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 22.dp),
                    ) {
                        Text(
                            text = "Detail Tindakan",
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

                        ReportDetailInfoRow(label = "Tindakan", value = item.actionLabel)
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Admin", value = "@${item.resolvedByName}")
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Tanggal tindakan", value = item.actionDateLong)
                        if (item.warningTitle.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            ReportDetailInfoRow(label = "Judul peringatan", value = item.warningTitle)
                        }
                        if (item.warningDescription.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            ReportDetailInfoRow(label = "Isi peringatan", value = item.warningDescription)
                        }
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Pelapor", value = item.report.reporterUsername)
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Tanggal laporan", value = item.report.reportDate)
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Jenis laporan", value = item.report.reportCategory)
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Deskripsi", value = item.report.reportDescription)
                        Spacer(Modifier.height(24.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Button(
                        enabled = !actionBusy,
                        onClick = { activeOverlay = HistoryDetailOverlay.CONFIRM_DELETE_PERMANENT },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HistoryDeletePermanentColor,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                    ) {
                        Text(
                            text = "Hapus Permanen",
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                        )
                    }

                    Button(
                        enabled = !actionBusy,
                        onClick = { activeOverlay = HistoryDetailOverlay.CONFIRM_RESTORE },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HomeOrange,
                            contentColor = Color.White,
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp),
                    ) {
                        Text(
                            text = "Pulihkan",
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            maxLines = 1,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = activeOverlay != HistoryDetailOverlay.NONE || actionBusy,
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
                            onClick = { if (!actionBusy) activeOverlay = HistoryDetailOverlay.NONE },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (actionBusy && activeOverlay == HistoryDetailOverlay.NONE) {
                        CircularProgressIndicator(color = HomeOrange)
                    }
                }
            }

            AnimatedVisibility(
                visible = activeOverlay == HistoryDetailOverlay.CONFIRM_DELETE_PERMANENT,
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit = fadeOut() + scaleOut(targetScale = 0.92f),
                modifier = Modifier.align(Alignment.Center),
            ) {
                HistoryActionConfirmCard(
                    title = "Hapus Permanen?",
                    message = "Riwayat tindakan ini akan dihapus permanen dan tidak dapat dipulihkan lagi.",
                    confirmLabel = "Hapus",
                    confirmColor = HistoryDeletePermanentColor,
                    onConfirm = {
                        activeOverlay = HistoryDetailOverlay.NONE
                        onPermanentDelete(item)
                    },
                    onCancel = { activeOverlay = HistoryDetailOverlay.NONE },
                )
            }

            AnimatedVisibility(
                visible = activeOverlay == HistoryDetailOverlay.CONFIRM_RESTORE,
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit = fadeOut() + scaleOut(targetScale = 0.92f),
                modifier = Modifier.align(Alignment.Center),
            ) {
                HistoryActionConfirmCard(
                    title = "Pulihkan Tindakan?",
                    message = "Laporan akan dikembalikan ke Dashboard Report dengan status pending.",
                    confirmLabel = "Pulihkan",
                    confirmColor = KuyButton,
                    onConfirm = {
                        activeOverlay = HistoryDetailOverlay.NONE
                        onRestore(item)
                    },
                    onCancel = { activeOverlay = HistoryDetailOverlay.NONE },
                )
            }
        }
    }
}

@Composable
private fun HistoryActionConfirmCard(
    title: String,
    message: String,
    confirmLabel: String,
    confirmColor: Color,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier
            .fillMaxWidth(0.88f)
            .widthIn(min = 300.dp, max = 380.dp),
        shape = RoundedCornerShape(18.dp),
        color = HomeNavy,
        border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.7f)),
        shadowElevation = 18.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = title,
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = message,
                color = HomeText.copy(alpha = 0.85f),
                fontFamily = Poppins,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onCancel,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KuyBlue,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                ) {
                    Text(
                        text = "Batal",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
                Button(
                    onClick = onConfirm,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = confirmColor,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                ) {
                    Text(
                        text = confirmLabel,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                    )
                }
            }
        }
    }
}

@Preview(name = "Admin History Action Detail", showBackground = true, showSystemUi = true)
@Composable
private fun AdminHistoryActionDetailScreenPreview() {
    OlahragaKuyTheme {
        AdminHistoryActionDetailScreen(
            item = SampleAdminHistoryItems.first(),
            onBack = {},
            onPermanentDelete = {},
            onRestore = {},
        )
    }
}
