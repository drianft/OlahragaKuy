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
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.SubcomposeAsyncImage
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.components.KuyPostSearchBar
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyBlue
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins
import kotlinx.coroutines.delay

private enum class HistoryActionFilter(val label: String) {
    ALL("Semua"),
    DELETED("Dihapus"),
    WARNED("Peringatan"),
    IGNORED("Diabaikan"),
}

private val HistoryChipLightBg = Color(0xFFE6E8EC)
private val HistoryDividerColor = Color(0xFF10243C)

internal fun filterAdminHistoryItems(
    items: List<AdminActionHistoryItem>,
    query: String,
    statusFilter: String? = null,
): List<AdminActionHistoryItem> {
    val cleanQuery = query.trim().lowercase()
    return items.filter { item ->
        val matchesStatus = statusFilter == null || item.actionStatus == statusFilter
        val matchesQuery = cleanQuery.isEmpty() ||
            item.report.reportedUsername.lowercase().contains(cleanQuery) ||
            item.report.reporterUsername.lowercase().contains(cleanQuery) ||
            item.report.reportCategory.lowercase().contains(cleanQuery) ||
            item.report.contentPreview.lowercase().contains(cleanQuery) ||
            item.actionLabel.lowercase().contains(cleanQuery) ||
            item.resolvedByName.lowercase().contains(cleanQuery) ||
            item.actionDateShort.lowercase().contains(cleanQuery) ||
            item.actionDateLong.lowercase().contains(cleanQuery)
        matchesStatus && matchesQuery
    }
}

/**
 * Halaman History Action khusus Admin yang menampilkan riwayat seluruh tindakan moderasi
 * (hapus konten, kirim peringatan, abaikan laporan) beserta akses ke [AdminHistoryActionDetailScreen].
 */
@Composable
fun AdminHistoryActionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    historyItems: List<AdminActionHistoryItem>? = null,
    onItemClick: ((AdminActionHistoryItem) -> Unit)? = null,
    onPermanentDelete: ((AdminActionHistoryItem) -> Unit)? = null,
    onRestore: ((AdminActionHistoryItem) -> Unit)? = null,
) {
    val isPreview = LocalInspectionMode.current
    val useFirestoreBackend = !isPreview && historyItems == null
    val focusManager = LocalFocusManager.current

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

    val sourceHistory = when {
        historyItems != null -> historyItems
        isPreview -> SampleAdminHistoryItems
        else -> remoteHistory
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var selectedFilter by rememberSaveable { mutableStateOf(HistoryActionFilter.ALL) }
    var selectedItemId by rememberSaveable { mutableStateOf<String?>(null) }
    var removedIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val activeHistory = remember(sourceHistory, removedIds) {
        val removedSet = removedIds.toSet()
        sourceHistory.filterNot { it.id in removedSet }
    }

    val selectedItem = remember(activeHistory, selectedItemId) {
        selectedItemId?.let { id -> activeHistory.firstOrNull { it.id == id } }
    }

    if (selectedItem != null) {
        AdminHistoryActionDetailScreen(
            item = selectedItem,
            actionBusy = actionBusy,
            onBack = { if (!actionBusy) selectedItemId = null },
            onPermanentDelete = { item ->
                if (useFirestoreBackend) {
                    actionBusy = true
                    permanentlyDeleteAdminHistoryItem(
                        item = item,
                        onSuccess = {
                            actionBusy = false
                            removedIds = removedIds + item.id
                            selectedItemId = null
                            feedbackMessage = "Riwayat tindakan @${item.report.reportedUsername} dihapus permanen"
                            onPermanentDelete?.invoke(item)
                        },
                        onError = { err ->
                            actionBusy = false
                            actionError = err
                        },
                    )
                } else {
                    removedIds = removedIds + item.id
                    selectedItemId = null
                    feedbackMessage = "Riwayat tindakan @${item.report.reportedUsername} dihapus permanen"
                    onPermanentDelete?.invoke(item)
                }
            },
            onRestore = { item ->
                if (useFirestoreBackend) {
                    actionBusy = true
                    restoreAdminHistoryItem(
                        item = item,
                        onSuccess = {
                            actionBusy = false
                            removedIds = removedIds + item.id
                            selectedItemId = null
                            feedbackMessage = "Tindakan terhadap @${item.report.reportedUsername} berhasil dipulihkan"
                            onRestore?.invoke(item)
                        },
                        onError = { err ->
                            actionBusy = false
                            actionError = err
                        },
                    )
                } else {
                    removedIds = removedIds + item.id
                    selectedItemId = null
                    feedbackMessage = "Tindakan terhadap @${item.report.reportedUsername} berhasil dipulihkan"
                    onRestore?.invoke(item)
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

    BackHandler {
        onBack()
    }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            delay(2500L)
            feedbackMessage = null
        }
    }

    val statusFilterCode = when (selectedFilter) {
        HistoryActionFilter.ALL -> null
        HistoryActionFilter.DELETED -> REPORT_STATUS_RESOLVED_DELETED
        HistoryActionFilter.WARNED -> REPORT_STATUS_RESOLVED_WARNED
        HistoryActionFilter.IGNORED -> REPORT_STATUS_IGNORED
    }

    val filteredHistory = remember(activeHistory, searchQuery, statusFilterCode) {
        filterAdminHistoryItems(
            items = activeHistory,
            query = searchQuery,
            statusFilter = statusFilterCode,
        )
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    KuyBackButton(onClick = onBack)
                    Spacer(Modifier.width(14.dp))
                    Text(
                        text = "History Action",
                        color = Color.White,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                Spacer(Modifier.height(18.dp))

                KuyPostSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onSearch = { focusManager.clearFocus() },
                    placeholder = "Cari riwayat aksi..",
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    HistoryActionFilter.entries.forEach { filter ->
                        val selected = selectedFilter == filter
                        Surface(
                            onClick = { selectedFilter = filter },
                            shape = RoundedCornerShape(20.dp),
                            color = if (selected) HistoryChipLightBg else HomeBlue,
                            border = BorderStroke(
                                width = 1.2.dp,
                                color = if (selected) KuyBlue else KuyBlue.copy(alpha = 0.7f),
                            ),
                        ) {
                            Text(
                                text = filter.label,
                                color = if (selected) KuyBlue else HomeText,
                                fontFamily = Poppins,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))

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

                    filteredHistory.isEmpty() -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = if (activeHistory.isEmpty()) {
                                        "Belum ada riwayat tindakan"
                                    } else {
                                        "Tidak ada riwayat yang cocok"
                                    },
                                    color = HomeText,
                                    fontFamily = Poppins,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp,
                                )
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    text = if (activeHistory.isEmpty()) {
                                        "Riwayat moderasi dari seluruh admin akan muncul di sini."
                                    } else {
                                        "Coba ubah kata kunci pencarian atau filter status."
                                    },
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
                            items(filteredHistory, key = { it.id }) { item ->
                                AdminHistoryActionCard(
                                    item = item,
                                    onClick = {
                                        if (onItemClick != null) {
                                            onItemClick(item)
                                        } else {
                                            selectedItemId = item.id
                                        }
                                    },
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
    val badgeColor = when (item.actionStatus) {
        REPORT_STATUS_RESOLVED_DELETED -> Color(0xFFE53935)
        REPORT_STATUS_RESOLVED_WARNED -> HomeOrange
        else -> KuyBlue
    }

    Surface(
        shape = cardShape,
        color = HomeBlue,
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .clickable(role = Role.Button, onClick = onClick),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HistoryThumbnail(
                thumbnailUrl = item.report.thumbnailUrl,
                type = item.report.type,
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "@${item.report.reportedUsername}",
                        color = Color.White,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = item.actionDateShort,
                        color = HomeText.copy(alpha = 0.75f),
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                    )
                }

                Spacer(Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = badgeColor.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, badgeColor),
                    ) {
                        Text(
                            text = item.actionLabel,
                            color = Color.White,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 10.5.sp,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                    Text(
                        text = "oleh @${item.resolvedByName}",
                        color = HomeText.copy(alpha = 0.78f),
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    thickness = 1.dp,
                    color = HistoryDividerColor,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        if (item.report.contentPreview.isNotBlank()) {
                            Text(
                                text = item.report.contentPreview,
                                color = HomeText.copy(alpha = 0.9f),
                                fontFamily = Poppins,
                                fontSize = 11.5.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                        }
                        Text(
                            text = "Kategori: ${item.report.reportCategory}",
                            color = HomeText.copy(alpha = 0.78f),
                            fontFamily = Poppins,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Lihat detail riwayat",
                        tint = HomeOrange,
                        modifier = Modifier.size(22.dp),
                    )
                }
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
            .size(66.dp)
            .clip(thumbShape)
            .background(HomeNavy)
            .border(1.dp, HomeOrange.copy(alpha = 0.7f), thumbShape),
        contentAlignment = Alignment.Center,
    ) {
        if (!thumbnailUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = thumbnailUrl,
                contentDescription = "Cuplikan konten riwayat",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                error = {
                    ReportThumbnailFallback(type)
                },
            )
        } else {
            ReportThumbnailFallback(type)
        }
    }
}

@Preview(name = "Admin History Action", showBackground = true, showSystemUi = true)
@Composable
private fun AdminHistoryActionScreenPreview() {
    OlahragaKuyTheme {
        AdminHistoryActionScreen(onBack = {})
    }
}
