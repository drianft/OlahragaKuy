package com.example.olahragakuy.pages.profile.admin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyBlue
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins

/**
 * Halaman Detail Laporan & Action dari riwayat tindakan admin.
 * Menampilkan cuplikan konten yang ditindak, rincian laporan + aksi yang dilakukan admin,
 * serta tombol **Hapus Permanent** dan **Pulihkan**.
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
    var showConfirmDelete by remember(item.id) { mutableStateOf(false) }

    BackHandler {
        if (actionBusy) return@BackHandler
        if (showConfirmDelete) {
            showConfirmDelete = false
        } else {
            onBack()
        }
    }

    val actorRowLabel = if (item.actionStatus == REPORT_STATUS_RESOLVED_DELETED) {
        "Dihapus oleh"
    } else {
        "Ditangani oleh"
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
                    KuyBackButton(onClick = { if (!actionBusy) onBack() })
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
                            text = "Detail Laporan & Action",
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

                        ReportDetailInfoRow(label = "Pelapor", value = item.report.reporterUsername)
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Tanggal laporan", value = item.report.reportDate)
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Jenis laporan", value = item.report.reportCategory)
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Deskripsi", value = item.report.reportDescription)
                        if (item.warningTitle.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            ReportDetailInfoRow(label = "Judul peringatan", value = item.warningTitle)
                        }
                        if (item.warningDescription.isNotBlank()) {
                            Spacer(Modifier.height(10.dp))
                            ReportDetailInfoRow(label = "Isi peringatan", value = item.warningDescription)
                        }
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Aksi dilakukan", value = item.actionLabel)
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = "Tanggal aksi", value = item.actionDateLong)
                        Spacer(Modifier.height(10.dp))
                        ReportDetailInfoRow(label = actorRowLabel, value = item.resolvedByName)
                        Spacer(Modifier.height(24.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 18.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    Button(
                        enabled = !actionBusy,
                        onClick = { showConfirmDelete = true },
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
                            text = "Hapus Permanent",
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            maxLines = 1,
                        )
                    }

                    Button(
                        enabled = !actionBusy,
                        onClick = { onRestore(item) },
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
                            text = "Pulihkan",
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = showConfirmDelete || actionBusy,
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
                            onClick = { if (!actionBusy) showConfirmDelete = false },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (actionBusy && !showConfirmDelete) {
                        CircularProgressIndicator(color = HomeOrange)
                    }
                }
            }

            AnimatedVisibility(
                visible = showConfirmDelete,
                enter = fadeIn() + scaleIn(initialScale = 0.92f),
                exit = fadeOut() + scaleOut(targetScale = 0.92f),
                modifier = Modifier.align(Alignment.Center),
            ) {
                AdminDeletePostConfirmCard(
                    onYes = {
                        showConfirmDelete = false
                        onPermanentDelete(item)
                    },
                    onNo = { showConfirmDelete = false },
                )
            }
        }
    }
}

@Preview(name = "Detail Laporan & Action", showBackground = true, showSystemUi = true)
@Composable
private fun AdminHistoryActionDetailPreview() {
    OlahragaKuyTheme {
        AdminHistoryActionDetailScreen(
            item = SampleAdminHistoryItems.first(),
            onBack = {},
            onPermanentDelete = {},
            onRestore = {},
        )
    }
}
