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
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

enum class AdminReportType(val label: String) {
    POST("Post"),
    COMMENT("Comment"),
    LOCATION("Location"),
    ACCOUNT("Account"),
}

data class AdminReportItem(
    val id: String,
    val reportedUsername: String,
    val reporterUsername: String,
    val reportCategory: String,
    val reportDate: String,
    val type: AdminReportType = AdminReportType.POST,
    val withinLastHour: Boolean = false,
    val thumbnailUrl: String? = null,
    val contentPreview: String = "",
    val postTime: String = "2 jam lalu",
    val likesCount: Int = 57,
    val commentsCount: Int = 4,
    val sharesCount: Int = 8,
    val reportDescription: String = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis...",
    val postDocumentId: String? = null,
)

data class AdminReportFilterState(
    val selectedTypes: Set<AdminReportType> = AdminReportType.entries.toSet(),
    val selectedCategories: Set<String> = DefaultAdminReportCategories.toSet(),
    val onlyLastHour: Boolean = false,
)

const val ALL_REPORT_CATEGORIES = "Semua Kategori"

val DefaultAdminReportCategories = listOf(
    "Kekerasan, kebencian atau eksploitasi",
    "Aktivitas ilegal",
    "Hoax atau informasi palsu",
    "Penipuan, penggelapan atau spam",
    "Pelanggaran hak cipta",
)

val SampleAdminReports = listOf(
    AdminReportItem(
        id = "report-1",
        reportedUsername = "BibiOlahraga",
        reporterUsername = "PecintaJogging",
        reportCategory = "Kekerasan, kebencian atau eksploitasi",
        reportDate = "30 Oktober 2026",
        type = AdminReportType.POST,
        withinLastHour = true,
        contentPreview = "Always on Proces",
    ),
    AdminReportItem(
        id = "report-2",
        reportedUsername = "BibiOlahraga",
        reporterUsername = "PecintaJogging",
        reportCategory = "Kekerasan, kebencian atau eksploitasi",
        reportDate = "30 Oktober 2026",
        type = AdminReportType.COMMENT,
        withinLastHour = true,
        contentPreview = "Komentar pada postingan latihan pagi",
    ),
    AdminReportItem(
        id = "report-3",
        reportedUsername = "PelariSantai",
        reporterUsername = "PecintaJogging",
        reportCategory = "Penipuan, penggelapan atau spam",
        reportDate = "30 Oktober 2026",
        type = AdminReportType.POST,
        withinLastHour = false,
        contentPreview = "Promo suplemen tidak resmi",
    ),
    AdminReportItem(
        id = "report-4",
        reportedUsername = "GorMerdeka",
        reporterUsername = "RunnerBandung",
        reportCategory = "Hoax atau informasi palsu",
        reportDate = "30 Oktober 2026",
        type = AdminReportType.LOCATION,
        withinLastHour = false,
        contentPreview = "Lokasi venue tidak sesuai alamat asli",
    ),
    AdminReportItem(
        id = "report-5",
        reportedUsername = "AkunPalsu99",
        reporterUsername = "BibiOlahraga",
        reportCategory = "Pelanggaran hak cipta",
        reportDate = "30 Oktober 2026",
        type = AdminReportType.ACCOUNT,
        withinLastHour = true,
        contentPreview = "Menggunakan foto profil milik pengguna lain",
    ),
)

internal fun filterAdminReports(
    reports: List<AdminReportItem>,
    query: String,
    filter: AdminReportFilterState,
): List<AdminReportItem> {
    val cleanQuery = query.trim().lowercase()
    return reports.filter { report ->
        val matchesType = report.type in filter.selectedTypes
        val matchesCategory = filter.selectedCategories.any {
            it.equals(report.reportCategory, ignoreCase = true)
        }
        val matchesTime = !filter.onlyLastHour || report.withinLastHour
        val matchesQuery = cleanQuery.isEmpty() ||
            report.reportedUsername.lowercase().contains(cleanQuery) ||
            report.reporterUsername.lowercase().contains(cleanQuery) ||
            report.reportCategory.lowercase().contains(cleanQuery) ||
            report.reportDate.lowercase().contains(cleanQuery) ||
            report.contentPreview.lowercase().contains(cleanQuery)
        matchesType && matchesCategory && matchesTime && matchesQuery
    }
}

private val ReportChipLightBg = Color(0xFFE6E8EC)
private val ReportDividerColor = Color(0xFF10243C)

/**
 * Halaman Dashboard Report khusus Admin (`admin-dashboardreport`).
 */
@Composable
fun AdminDashboardReportScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    reports: List<AdminReportItem> = SampleAdminReports,
    onReportClick: ((AdminReportItem) -> Unit)? = null,
    onIgnoreReport: (AdminReportItem) -> Unit = {},
    onDeleteReportedPost: (AdminReportItem) -> Unit = {},
    onSendWarning: (report: AdminReportItem, title: String, description: String) -> Unit = { _, _, _ -> },
) {
    val focusManager = LocalFocusManager.current
    val availableCategories = remember(reports) {
        (DefaultAdminReportCategories + reports.map { it.reportCategory }).distinct()
    }

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var filterState by remember(availableCategories) {
        mutableStateOf(
            AdminReportFilterState(
                selectedTypes = AdminReportType.entries.toSet(),
                selectedCategories = availableCategories.toSet(),
                onlyLastHour = false,
            ),
        )
    }
    var showFilterSheet by remember { mutableStateOf(false) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var selectedReportId by rememberSaveable { mutableStateOf<String?>(null) }
    var dismissedReportIds by rememberSaveable { mutableStateOf(listOf<String>()) }
    var feedbackMessage by remember { mutableStateOf<String?>(null) }

    val activeReports = remember(reports, dismissedReportIds) {
        val dismissedSet = dismissedReportIds.toSet()
        reports.filterNot { it.id in dismissedSet }
    }

    val selectedReport = remember(activeReports, selectedReportId) {
        selectedReportId?.let { id -> activeReports.firstOrNull { it.id == id } }
    }

    if (selectedReport != null) {
        AdminReportDetailScreen(
            report = selectedReport,
            onBack = { selectedReportId = null },
            onIgnore = { report ->
                dismissedReportIds = dismissedReportIds + report.id
                selectedReportId = null
                feedbackMessage = "Laporan terhadap @${report.reportedUsername} diabaikan"
                onIgnoreReport(report)
            },
            onDeletePost = { report ->
                dismissedReportIds = dismissedReportIds + report.id
                selectedReportId = null
                feedbackMessage = "Postingan @${report.reportedUsername} berhasil dihapus"
                onDeleteReportedPost(report)
            },
            onSendWarning = { report, title, description ->
                dismissedReportIds = dismissedReportIds + report.id
                selectedReportId = null
                feedbackMessage = "Peringatan \"$title\" dikirim ke @${report.reportedUsername}"
                onSendWarning(report, title, description)
            },
            modifier = modifier,
        )
        return
    }

    LaunchedEffect(feedbackMessage) {
        if (feedbackMessage != null) {
            kotlinx.coroutines.delay(2500L)
            feedbackMessage = null
        }
    }

    val filteredReports = remember(activeReports, searchQuery, filterState) {
        filterAdminReports(
            reports = activeReports,
            query = searchQuery,
            filter = filterState,
        )
    }

    val allCategoriesSelected = availableCategories.all { it in filterState.selectedCategories }
    val categoryChipLabel = when {
        allCategoriesSelected -> ALL_REPORT_CATEGORIES
        filterState.selectedCategories.size == 1 -> filterState.selectedCategories.first()
        else -> "${filterState.selectedCategories.size} Kategori"
    }

    val toggleType: (AdminReportType) -> Unit = { type ->
        val nextTypes = if (type in filterState.selectedTypes) {
            filterState.selectedTypes - type
        } else {
            filterState.selectedTypes + type
        }
        filterState = filterState.copy(selectedTypes = nextTypes)
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

            KuyPostSearchBar(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                onSearch = { focusManager.clearFocus() },
                placeholder = "Cari..",
            )

            Spacer(Modifier.height(14.dp))

            // Baris chip filter (Filter | Post x | Comment x | Semua Kategori | dst.)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterPillChip(
                    onClick = { showFilterSheet = true },
                )

                ToggleFilterChip(
                    label = AdminReportType.POST.label,
                    selected = AdminReportType.POST in filterState.selectedTypes,
                    onClick = { toggleType(AdminReportType.POST) },
                )

                ToggleFilterChip(
                    label = AdminReportType.COMMENT.label,
                    selected = AdminReportType.COMMENT in filterState.selectedTypes,
                    onClick = { toggleType(AdminReportType.COMMENT) },
                )

                Box {
                    CategoryFilterChip(
                        label = categoryChipLabel,
                        onClick = { categoryMenuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = categoryMenuExpanded,
                        onDismissRequest = { categoryMenuExpanded = false },
                        containerColor = HomeBlue,
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = ALL_REPORT_CATEGORIES,
                                    color = if (allCategoriesSelected) HomeOrange else HomeText,
                                    fontFamily = Poppins,
                                    fontWeight = if (allCategoriesSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 13.sp,
                                )
                            },
                            onClick = {
                                filterState = filterState.copy(
                                    selectedCategories = availableCategories.toSet(),
                                )
                                categoryMenuExpanded = false
                            },
                        )
                        availableCategories.forEach { category ->
                            val isOnlyThis = !allCategoriesSelected &&
                                filterState.selectedCategories.size == 1 &&
                                category in filterState.selectedCategories
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = category,
                                        color = if (isOnlyThis) HomeOrange else HomeText,
                                        fontFamily = Poppins,
                                        fontWeight = if (isOnlyThis) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                    )
                                },
                                onClick = {
                                    filterState = filterState.copy(
                                        selectedCategories = setOf(category),
                                    )
                                    categoryMenuExpanded = false
                                },
                            )
                        }
                    }
                }

                ToggleFilterChip(
                    label = AdminReportType.LOCATION.label,
                    selected = AdminReportType.LOCATION in filterState.selectedTypes,
                    onClick = { toggleType(AdminReportType.LOCATION) },
                )

                ToggleFilterChip(
                    label = AdminReportType.ACCOUNT.label,
                    selected = AdminReportType.ACCOUNT in filterState.selectedTypes,
                    onClick = { toggleType(AdminReportType.ACCOUNT) },
                )

                if (filterState.onlyLastHour) {
                    ToggleFilterChip(
                        label = "1 Jam terakhir",
                        selected = true,
                        onClick = { filterState = filterState.copy(onlyLastHour = false) },
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            if (filteredReports.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Tidak ada laporan yang cocok",
                            color = HomeText,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = "Coba ubah kata kunci pencarian atau filter aktif.",
                            color = HomeText.copy(alpha = 0.7f),
                            fontFamily = Poppins,
                            fontSize = 12.sp,
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 24.dp),
                ) {
                    items(filteredReports, key = { it.id }) { report ->
                        AdminReportCard(
                            report = report,
                            onClick = {
                                if (onReportClick != null) {
                                    onReportClick(report)
                                } else {
                                    selectedReportId = report.id
                                }
                            },
                        )
                    }
                }
            }
        }

        // In-screen Filter Bottom Sheet Overlay (bekerja mulus di Interactive Preview & Device)
        if (showFilterSheet) {
            BackHandler { showFilterSheet = false }
        }

        AnimatedVisibility(
            visible = showFilterSheet,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { showFilterSheet = false },
                    ),
            )
        }

        AnimatedVisibility(
            visible = showFilterSheet,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it }),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.85f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = HomeNavy,
                shadowElevation = 16.dp,
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    FilterSheetDragHandle()
                    AdminReportFilterSheetContent(
                        initialFilter = filterState,
                        categories = availableCategories,
                        onApply = { applied ->
                            filterState = applied
                            showFilterSheet = false
                        },
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
        }

        // Banner status singkat setelah tindakan (Abaikan / Hapus / Peringatan)
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
private fun FilterSheetDragHandle() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .width(120.dp)
                .height(10.dp)
                .clip(RoundedCornerShape(50))
                .background(HomeBlue),
        )
    }
}

/**
 * Konten pop-up filter laporan (Jenis Laporan, Kategori Laporan, Waktu Laporan, dan Apply Filter).
 */
@Composable
internal fun AdminReportFilterSheetContent(
    initialFilter: AdminReportFilterState,
    categories: List<String>,
    onApply: (AdminReportFilterState) -> Unit,
    modifier: Modifier = Modifier,
) {
    var draftTypes by remember(initialFilter) { mutableStateOf(initialFilter.selectedTypes) }
    var draftCategories by remember(initialFilter) { mutableStateOf(initialFilter.selectedCategories) }
    var draftOnlyLastHour by remember(initialFilter) { mutableStateOf(initialFilter.onlyLastHour) }

    val allTypesChecked = AdminReportType.entries.all { it in draftTypes }
    val allCategoriesChecked = categories.isNotEmpty() && categories.all { it in draftCategories }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(horizontal = 28.dp, vertical = 6.dp),
    ) {
        // Bagian 1: Jenis Laporan
        FilterSectionHeader(title = "Jenis Laporan")
        FilterCheckboxRow(
            label = "Pilih Semua",
            checked = allTypesChecked,
            onToggle = {
                draftTypes = if (allTypesChecked) emptySet() else AdminReportType.entries.toSet()
            },
        )
        AdminReportType.entries.forEach { type ->
            val isChecked = type in draftTypes
            FilterCheckboxRow(
                label = type.label,
                checked = isChecked,
                onToggle = {
                    draftTypes = if (isChecked) draftTypes - type else draftTypes + type
                },
            )
        }

        Spacer(Modifier.height(14.dp))

        // Bagian 2: Kategori Laporan
        FilterSectionHeader(title = "Kategori Laporan")
        FilterCheckboxRow(
            label = "Pilih Semua",
            checked = allCategoriesChecked,
            onToggle = {
                draftCategories = if (allCategoriesChecked) emptySet() else categories.toSet()
            },
        )
        categories.forEach { category ->
            val isChecked = category in draftCategories
            FilterCheckboxRow(
                label = category,
                checked = isChecked,
                onToggle = {
                    draftCategories = if (isChecked) draftCategories - category else draftCategories + category
                },
            )
        }

        Spacer(Modifier.height(14.dp))

        // Bagian 3: Waktu Laporan
        FilterSectionHeader(title = "Waktu Laporan")
        FilterCheckboxRow(
            label = "Pilih Semua",
            checked = !draftOnlyLastHour,
            onToggle = { draftOnlyLastHour = false },
        )
        FilterCheckboxRow(
            label = "1 Jam terakhir",
            checked = draftOnlyLastHour,
            onToggle = { draftOnlyLastHour = !draftOnlyLastHour },
        )

        Spacer(Modifier.height(18.dp))

        // Tombol Apply Filter
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 14.dp),
            contentAlignment = Alignment.Center,
        ) {
            Button(
                onClick = {
                    onApply(
                        AdminReportFilterState(
                            selectedTypes = draftTypes,
                            selectedCategories = draftCategories,
                            onlyLastHour = draftOnlyLastHour,
                        ),
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = HomeOrange,
                    contentColor = Color.White,
                ),
                modifier = Modifier
                    .widthIn(min = 190.dp)
                    .height(48.dp),
            ) {
                Text(
                    text = "Apply Filter",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            }
        }
    }
}

@Composable
private fun FilterSectionHeader(title: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            color = HomeText,
            fontFamily = Poppins,
            fontWeight = FontWeight.Normal,
            fontSize = 15.sp,
        )
        Spacer(Modifier.height(4.dp))
        HorizontalDivider(
            thickness = 1.2.dp,
            color = HomeOrange,
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun FilterCheckboxRow(
    label: String,
    checked: Boolean,
    onToggle: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(role = Role.Checkbox, onClick = onToggle)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val boxShape = RoundedCornerShape(3.dp)
        Box(
            modifier = Modifier
                .size(18.dp)
                .clip(boxShape)
                .background(if (checked) HomeOrange else Color.Transparent)
                .border(
                    width = 1.6.dp,
                    color = if (checked) HomeOrange else Color.White,
                    shape = boxShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (checked) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(13.dp),
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Text(
            text = label,
            color = HomeText.copy(alpha = 0.9f),
            fontFamily = Poppins,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
        )
    }
}

@Composable
private fun FilterPillChip(
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = KuyBlue,
        border = BorderStroke(1.2.dp, ReportChipLightBg),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Default.FilterAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = "Filter",
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun ToggleFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = if (selected) ReportChipLightBg else HomeBlue,
        border = BorderStroke(1.2.dp, if (selected) KuyBlue else KuyBlue.copy(alpha = 0.7f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = label,
                color = if (selected) KuyBlue else HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
            )
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.Cancel,
                    contentDescription = "Hapus filter $label",
                    tint = KuyBlue,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = ReportChipLightBg,
        border = BorderStroke(1.2.dp, KuyBlue),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                color = KuyBlue,
                fontFamily = Poppins,
                fontWeight = FontWeight.Medium,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun AdminReportCard(
    report: AdminReportItem,
    onClick: () -> Unit,
) {
    val cardShape = RoundedCornerShape(20.dp)
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
            ReportThumbnail(
                thumbnailUrl = report.thumbnailUrl,
                type = report.type,
            )

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "@${report.reportedUsername}",
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = "Reported by: @${report.reporterUsername}",
                    color = HomeText.copy(alpha = 0.8f),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 6.dp),
                    thickness = 1.dp,
                    color = ReportDividerColor,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Jenis Laporan: ${report.reportCategory}",
                            color = HomeText.copy(alpha = 0.88f),
                            fontFamily = Poppins,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "Tanggal Laporan: ${report.reportDate}",
                            color = HomeText.copy(alpha = 0.88f),
                            fontFamily = Poppins,
                            fontSize = 11.5.sp,
                            lineHeight = 15.sp,
                        )
                    }

                    Spacer(Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Lihat detail laporan",
                        tint = HomeOrange,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ReportThumbnail(
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
                contentDescription = "Cuplikan konten dilaporkan",
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

@Composable
internal fun ReportThumbnailFallback(type: AdminReportType) {
    val icon = when (type) {
        AdminReportType.POST -> Icons.Default.FitnessCenter
        AdminReportType.COMMENT -> Icons.Default.ChatBubbleOutline
        AdminReportType.LOCATION -> Icons.Default.LocationOn
        AdminReportType.ACCOUNT -> Icons.Default.Person
    }
    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = HomeOrange,
        modifier = Modifier.size(30.dp),
    )
}

@Preview(name = "Admin Dashboard Report", showBackground = true, showSystemUi = true)
@Composable
private fun AdminDashboardReportPreview() {
    OlahragaKuyTheme {
        var showDashboard by remember { mutableStateOf(true) }
        if (showDashboard) {
            AdminDashboardReportScreen(
                onBack = { showDashboard = false },
            )
        } else {
            AdminProfileScreen(
                onOpenDashboardReport = { showDashboard = true },
            )
        }
    }
}

@Preview(name = "Filter Pop-up", showBackground = true)
@Composable
private fun AdminReportFilterSheetPreview() {
    OlahragaKuyTheme {
        Surface(
            color = HomeNavy,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column {
                FilterSheetDragHandle()
                AdminReportFilterSheetContent(
                    initialFilter = AdminReportFilterState(
                        selectedTypes = emptySet(),
                        selectedCategories = emptySet(),
                        onlyLastHour = false,
                    ),
                    categories = DefaultAdminReportCategories,
                    onApply = {},
                )
            }
        }
    }
}
