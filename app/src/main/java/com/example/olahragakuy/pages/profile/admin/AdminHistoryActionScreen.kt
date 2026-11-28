package com.example.olahragakuy.pages.profile.admin

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins

private val HistoryCardDividerColor = Color(0xFF10243C)

/**
 * Halaman History Action khusus Admin (`Admin - History Action`).
 * Menampilkan gabungan riwayat seluruh aksi moderasi dari semua pengguna ber-role `admin`
 * di koleksi `reports` secara real-time. Menekan salah satu kartu membuka
 * [AdminHistoryActionDetailScreen] untuk melihat detail atau memilih Hapus Permanent / Pulihkan.
 */
@Composable
fun AdminHistoryActionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    historyItems: List<AdminActionHistoryItem>? = null,
    onPermanentDelete: ((AdminActionHistoryItem) -> Unit)? = null,
    onRestore: ((AdminActionHistoryItem) -> Unit)? = null,
) {
    val isPreview = LocalInspectionMode.current
    val useFirestoreBackend = !isPreview && historyItems == null

    var remoteHistory by remember { mutableStateOf<List<AdminActionHistoryItem>>(emptyList()) }
    var loadingHistory by remember { mutableStateOf(useFirestoreBackend) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var retryCount by remember { mutableIntStateOf(0) }
    var actionBusy by remember { mutableStateOf(false) }
    var actionError by remember { mutableStateOf<String?>(null) }

    DisposableEffect(useFirestoreBackend, retryCount) {
        if (!useFirestoreBackend) {
            loadingHistory = false
            loadError = null
            onDispose {}
        } else {
            loadingHistory = true
            loadError = null
            val registration = observeAdminActionHistory(
                onHistoryChanged = { items ->
                    remoteHistory = items
                    loadingHistory = false
                    loadError = null
                },
                onError = { message ->
                    loadingHistory = false
                    loadError = message
                },
            )
            onDispose { registration.remove() }
        }
    }

    val sourceItems = when {
        historyItems != null -> historyItems
        isPreview -> SampleAdminHistoryItems
        else -> remoteHistory
    }

    var selectedHistoryId by rememberSaveable { mutableStateOf<String?>(null) }
    var removedIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val visibleItems = remember(sourceItems, removedIds) {
        val removedSet = removedIds.toSet()
        sourceItems.filterNot { it.id in removedSet }
    }

    val selectedItem = remember(visibleItems, selectedHistoryId) {
        selectedHistoryId?.let { id -> visibleItems.firstOrNull { it.id == id } }
    }

    if (selectedItem != null) {
        AdminHistoryActionDetailScreen(
            item = selectedItem,
            actionBusy = actionBusy,
            onBack = { if (!actionBusy) selectedHistoryId = null },
            onPermanentDelete = { target ->
                if (useFirestoreBackend) {
                    actionBusy = true
                    permanentlyDeleteAdminHistoryItem(
                        item = target,
                        onSuccess = {
                            actionBusy = false
                            removedIds = removedIds + target.id
                            selectedHistoryId = null
                            feedbackMessage = "Riwayat aksi terhadap ${target.report.reportedUsername} dihapus permanen"
                            onPermanentDelete?.invoke(target)
                        },
                        onError = { err ->
                            actionBusy = false
                            actionError = err
                        },
                    )
                } else {
                    removedIds = removedIds + target.id
                    selectedHistoryId = null
                    feedbackMessage = "Riwayat aksi terhadap ${target.report.reportedUsername} dihapus permanen"
                    onPermanentDelete?.invoke(target)
                }
            },
            onRestore = { target ->
                if (useFirestoreBackend) {
                    actionBusy = true
                    restoreAdminHistoryItem(
                        item = target,
                        onSuccess = {
                            actionBusy = false
                            removedIds = removedIds + target.id
                            selectedHistoryId = null
                            feedbackMessage = "Tindakan terhadap ${target.report.reportedUsername} berhasil dipulihkan"
                            onRestore?.invoke(target)
                        },
                        onError = { err ->
                            actionBusy = false
                            actionError = err
                        },
                    )
                } else {
                    removedIds = removedIds + target.id
                    selectedHistoryId = null
                    feedbackMessage = "Tindakan terhadap ${target.report.reportedUsername} berhasil dipulihkan"
                    onRestore?.invoke(target)
                }
            },
            modifier = modifier,
        )

        actionError?.let { msg ->
            AlertDialog(
                onDismissRequest = { actionError = null },
                title = { Text("Gagal memproses tindakan") },
                text = { Text(msg) },
                confirmButton = {
                    TextButton(onClick = { actionError = null }) { Text("Mengerti") }
                },
            )
        }
        return
    }

    BackHandler { onBack() }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            kotlinx.coroutines.delay(2500L)
            feedbackMessage = null
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
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp),
            ) {
                Spacer(Modifier.height(12.dp))

                KuyBackButton(onClick = onBack)

                Spacer(Modifier.height(20.dp))

                when {
                    loadingHistory -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator(color = HomeOrange)
                        }
                    }
                    loadError != null -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            ) {
                                Text(
                                    text = "Gagal memuat riwayat aksi",
                                    color = HomeText,
                                    fontFamily = Poppins,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = loadError.orEmpty(),
                                    color = HomeText.copy(alpha = 0.75f),
                                    fontFamily = Poppins,
                                    fontSize = 12.sp,
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(
                                    onClick = { retryCount++ },
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HomeOrange,
                                        contentColor = Color.White,
                                    ),
                                ) {
                                    Text(
                                        text = "Coba Lagi",
                                        fontFamily = Poppins,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                    )
                                }
                            }
                        }
                    }
                    visibleItems.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Belum ada riwayat aksi admin",
                                    color = HomeText,
                                    fontFamily = Poppins,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = "Tindakan dari seluruh admin pada Detail Laporan akan tercatat di sini.",
                                    color = HomeText.copy(alpha = 0.7f),
                                    fontFamily = Poppins,
                                    fontSize = 12.sp,
                                )
                            }
                        }
                    }
                    else -> {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            verticalArrangement = Arrangement.spacedBy(16.dp),
                            contentPadding = PaddingValues(bottom = 24.dp),
                        ) {
                            items(visibleItems, key = { it.id }) { historyItem ->
                                AdminHistoryActionCard(
                                    item = historyItem,
                                    onClick = { selectedHistoryId = historyItem.id },
                                )
                            }
                        }
                    }
                }
            }

            AnimatedVisibility(
                visible = feedbackMessage != null,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = HomeBlue,
                    border = BorderStroke(1.dp, HomeOrange),
                    shadowElevation = 8.dp,
                ) {
                    Text(
                        text = feedbackMessage.orEmpty(),
                        color = Color.White,
                        fontFamily = Poppins,
                        fontSize = 12.5.sp,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun AdminHistoryActionCard(
    item: AdminActionHistoryItem,
    onClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(20.dp)
    val isCommentCard = item.report.type == AdminReportType.COMMENT
    val displayTitle = when (item.report.type) {
        AdminReportType.LOCATION, AdminReportType.COMMENT -> item.report.reportedUsername
        AdminReportType.POST, AdminReportType.ACCOUNT -> "@${item.report.reportedUsername}"
    }
    val summaryText = if (item.actionStatus == REPORT_STATUS_RESOLVED_WARNED && item.warningTitle.isNotBlank()) {
        "Judul peringatan: ${item.warningTitle}"
    } else {
        item.report.contentPreview.ifBlank { item.report.reportDescription }
    }

    Surface(
        shape = cardShape,
        color = HomeBlue,
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        if (isCommentCard) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = displayTitle,
                            color = Color.White,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.actionLabel,
                            color = HomeText.copy(alpha = 0.78f),
                            fontFamily = Poppins,
                            fontSize = 11.5.sp,
                            maxLines = 1,
                        )
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        thickness = 1.dp,
                        color = HistoryCardDividerColor,
                    )

                    Text(
                        text = summaryText,
                        color = HomeText.copy(alpha = 0.9f),
                        fontFamily = Poppins,
                        fontSize = 11.5.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Timestamp: ${item.actionDateShort}",
                        color = HomeText.copy(alpha = 0.85f),
                        fontFamily = Poppins,
                        fontSize = 11.5.sp,
                    )
                }

                Spacer(Modifier.width(12.dp))
                HistoryStatusIndicatorDot()
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HistoryThumbnail(
                        thumbnailUrl = item.report.thumbnailUrl,
                        type = item.report.type,
                    )
                    Spacer(Modifier.height(5.dp))
                    Text(
                        text = item.actionDateShort,
                        color = HomeText.copy(alpha = 0.9f),
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                    )
                }

                Spacer(Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = displayTitle,
                        color = Color.White,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = item.actionLabel,
                        color = HomeText.copy(alpha = 0.78f),
                        fontFamily = Poppins,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 6.dp),
                        thickness = 1.dp,
                        color = HistoryCardDividerColor,
                    )

                    Text(
                        text = summaryText,
                        color = HomeText.copy(alpha = 0.9f),
                        fontFamily = Poppins,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.width(10.dp))
                HistoryStatusIndicatorDot()
            }
        }
    }
}

@Composable
private fun HistoryThumbnail(
    thumbnailUrl: String?,
    type: AdminReportType,
) {
    val thumbShape = RoundedCornerShape(14.dp)
    Box(
        modifier = Modifier
            .size(64.dp)
            .clip(thumbShape)
            .background(HomeNavy)
            .border(1.2.dp, HomeOrange.copy(alpha = 0.85f), thumbShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = thumbnailUrl,
                contentDescription = "Cuplikan konten ditindak",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = { ReportThumbnailFallback(type) },
            )
        } else {
            ReportThumbnailFallback(type)
        }
    }
}

@Composable
private fun HistoryStatusIndicatorDot() {
    Box(
        modifier = Modifier
            .size(20.dp)
            .clip(CircleShape)
            .background(Color.White)
            .border(1.8.dp, HomeOrange, CircleShape),
    )
}

@Preview(name = "Admin - History Action", showBackground = true, showSystemUi = true)
@Composable
private fun AdminHistoryActionPreview() {
    OlahragaKuyTheme {
        AdminHistoryActionScreen(onBack = {})
    }
}
