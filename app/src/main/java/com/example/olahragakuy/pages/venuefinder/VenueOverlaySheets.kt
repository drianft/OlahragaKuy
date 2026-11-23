package com.example.olahragakuy.pages.venuefinder

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DirectionsCar
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.NearMe
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
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
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.KuyNavy
import com.example.olahragakuy.ui.theme.KuyOrange
import com.example.olahragakuy.ui.theme.KuyText
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins

private val SheetTopShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * Ruang aman bawah agar tombol aksi pada seluruh overlay tidak tertutup `KuyBottomNavBar`.
 */
val VenueBottomNavClearance: Dp = 80.dp

/**
 * Header pencarian untuk State 1–3 pada layar Venue Finder:
 * tombol [KuyBackButton], kolom pencarian `"Cari Lokasi"` dengan tombol ikon Filter terintegrasi,
 * baris active filter pills (hanya muncul bila ada filter aktif), serta dropdown hasil pencarian.
 */
@Composable
fun VenueSearchHeader(
    query: String,
    onQueryChange: (String) -> Unit,
    searchResults: List<VenueUiModel>,
    onSelectVenue: (VenueUiModel) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    showDropdown: Boolean = query.isNotBlank(),
    selectedSportFilter: VenueSportFilter = VenueSportFilter.ALL,
    selectedRadiusFilter: VenueRadiusFilter = VenueRadiusFilter.ALL,
    onFilterClick: () -> Unit = {},
    onClearSportFilter: () -> Unit = {},
    onClearRadiusFilter: () -> Unit = {},
    showEmptyFilterNotice: Boolean = false,
    onResetFilters: () -> Unit = {},
    onSearchSubmit: () -> Unit = {
        searchResults.firstOrNull()?.let(onSelectVenue)
    },
) {
    val hasActiveFilters = selectedSportFilter != VenueSportFilter.ALL ||
        selectedRadiusFilter != VenueRadiusFilter.ALL

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        KuyBackButton(onClick = onBackClick)

        VenueSearchBar(
            query = query,
            onQueryChange = onQueryChange,
            onSearchSubmit = onSearchSubmit,
            hasActiveFilters = hasActiveFilters,
            onFilterClick = onFilterClick,
        )

        AnimatedVisibility(
            visible = hasActiveFilters,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            VenueActiveFilterPillsRow(
                selectedSportFilter = selectedSportFilter,
                selectedRadiusFilter = selectedRadiusFilter,
                onClearSportFilter = onClearSportFilter,
                onClearRadiusFilter = onClearRadiusFilter,
            )
        }

        AnimatedVisibility(
            visible = showDropdown,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            VenueSearchDropdown(
                results = searchResults,
                onSelectVenue = onSelectVenue,
            )
        }

        AnimatedVisibility(
            visible = !showDropdown && showEmptyFilterNotice,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            VenueEmptyFilterNotice(onResetFilters = onResetFilters)
        }
    }
}

@Composable
private fun VenueActiveFilterPillsRow(
    selectedSportFilter: VenueSportFilter,
    selectedRadiusFilter: VenueRadiusFilter,
    onClearSportFilter: () -> Unit,
    onClearRadiusFilter: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        contentPadding = PaddingValues(horizontal = 2.dp),
    ) {
        if (selectedSportFilter != VenueSportFilter.ALL) {
            item(key = "active_sport_filter") {
                VenueActiveFilterPill(
                    label = selectedSportFilter.label,
                    onRemoveClick = onClearSportFilter,
                )
            }
        }
        if (selectedRadiusFilter != VenueRadiusFilter.ALL) {
            item(key = "active_radius_filter") {
                VenueActiveFilterPill(
                    label = "≤ ${selectedRadiusFilter.label}",
                    onRemoveClick = onClearRadiusFilter,
                )
            }
        }
    }
}

@Composable
private fun VenueActiveFilterPill(
    label: String,
    onRemoveClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onRemoveClick,
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = HomeNavy.copy(alpha = 0.94f),
        border = BorderStroke(1.dp, HomeOrange),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier.padding(start = 11.dp, top = 5.dp, end = 8.dp, bottom = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Text(
                text = label,
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.Medium,
                fontSize = 11.sp,
                maxLines = 1,
            )
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = "Hapus filter $label",
                tint = HomeOrange,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

/**
 * Modal Bottom Sheet Filter untuk memilih jenis olahraga dan radius jarak.
 * Tampil di atas seluruh layar termasuk Bottom Navigation Bar dengan tema dark navy & oranye.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueFilterBottomSheet(
    selectedSportFilter: VenueSportFilter,
    selectedRadiusFilter: VenueRadiusFilter,
    onApplyFilters: (VenueSportFilter, VenueRadiusFilter) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var draftSportFilter by remember(selectedSportFilter) { mutableStateOf(selectedSportFilter) }
    var draftRadiusFilter by remember(selectedRadiusFilter) { mutableStateOf(selectedRadiusFilter) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = SheetTopShape,
        containerColor = KuyNavy,
        contentColor = HomeText,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                contentAlignment = Alignment.Center,
            ) {
                SheetDragHandle()
            }
        },
    ) {
        VenueFilterSheetContent(
            selectedSportFilter = draftSportFilter,
            selectedRadiusFilter = draftRadiusFilter,
            onSportSelected = { draftSportFilter = it },
            onRadiusSelected = { draftRadiusFilter = it },
            onResetClick = {
                draftSportFilter = VenueSportFilter.ALL
                draftRadiusFilter = VenueRadiusFilter.ALL
                onApplyFilters(VenueSportFilter.ALL, VenueRadiusFilter.ALL)
            },
            onApplyClick = {
                onApplyFilters(draftSportFilter, draftRadiusFilter)
                onDismiss()
            },
            onCloseClick = onDismiss,
        )
    }
}

@Composable
private fun VenueFilterSheetContent(
    selectedSportFilter: VenueSportFilter,
    selectedRadiusFilter: VenueRadiusFilter,
    onSportSelected: (VenueSportFilter) -> Unit,
    onRadiusSelected: (VenueRadiusFilter) -> Unit,
    onResetClick: () -> Unit,
    onApplyClick: () -> Unit,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .navigationBarsPadding()
            .padding(start = 20.dp, top = 6.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Filter Lokasi",
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = "Sesuaikan jenis olahraga dan jarak terdekat",
                    color = HomeText.copy(alpha = 0.72f),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Normal,
                    fontSize = 12.sp,
                )
            }
            IconButton(
                onClick = onCloseClick,
                modifier = Modifier.size(36.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "Tutup filter lokasi",
                    tint = HomeText,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = "Jenis Olahraga",
                color = HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VenueSportFilter.entries.forEach { sport ->
                    VenueFilterChip(
                        label = sport.label,
                        selected = sport == selectedSportFilter,
                        onClick = { onSportSelected(sport) },
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.NearMe,
                    contentDescription = null,
                    tint = HomeOrange,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Radius Jarak",
                    color = HomeText,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                VenueRadiusFilter.entries.forEach { radius ->
                    VenueFilterChip(
                        label = radius.label,
                        selected = radius == selectedRadiusFilter,
                        onClick = { onRadiusSelected(radius) },
                    )
                }
            }
        }

        HorizontalDivider(color = HomeBlue.copy(alpha = 0.65f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onResetClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.2.dp, HomeBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = HomeNavy,
                    contentColor = HomeText,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Reset",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }

            Button(
                onClick = onApplyClick,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KuyButton,
                    contentColor = Color.White,
                ),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Text(
                    text = "Terapkan",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun VenueFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = if (selected) KuyButton else HomeNavy,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) HomeOrange else HomeBlue.copy(alpha = 0.9f),
        ),
        shadowElevation = if (selected) 4.dp else 0.dp,
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else HomeText.copy(alpha = 0.92f),
            fontFamily = Poppins,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            fontSize = 12.sp,
            maxLines = 1,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
        )
    }
}

@Composable
private fun VenueEmptyFilterNotice(
    onResetFilters: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = HomeNavy.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, HomeOrange.copy(alpha = 0.7f)),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "Tidak ada lokasi olahraga yang cocok.",
                color = HomeText.copy(alpha = 0.9f),
                fontFamily = Poppins,
                fontWeight = FontWeight.Normal,
                fontSize = 11.sp,
                modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Reset",
                color = HomeOrange,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Reset filter lokasi",
                        onClick = onResetFilters,
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
}

@Composable
private fun VenueSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    hasActiveFilters: Boolean,
    onFilterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        singleLine = true,
        textStyle = TextStyle(
            fontFamily = Poppins,
            fontWeight = FontWeight.Normal,
            fontSize = 13.sp,
            color = HomeText,
        ),
        cursorBrush = SolidColor(HomeOrange),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() }),
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp),
        decorationBox = { innerTextField ->
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = HomeNavy.copy(alpha = 0.96f),
                border = BorderStroke(1.2.dp, KuyButton),
                shadowElevation = 6.dp,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(start = 18.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Cari Lokasi",
                                color = HomeText.copy(alpha = 0.72f),
                                fontFamily = Poppins,
                                fontWeight = FontWeight.Normal,
                                fontSize = 13.sp,
                            )
                        }
                        innerTextField()
                    }
                    if (query.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = "Hapus pencarian",
                                    onClick = { onQueryChange("") },
                                ),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = "Hapus pencarian",
                                tint = HomeText.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Cari lokasi",
                                onClick = onSearchSubmit,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_baseline_search),
                            contentDescription = "Cari lokasi",
                            tint = Color.White,
                            modifier = Modifier.size(21.dp),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .width(1.dp)
                            .height(20.dp)
                            .background(HomeBlue.copy(alpha = 0.85f)),
                    )
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (hasActiveFilters) {
                                    HomeOrange.copy(alpha = 0.16f)
                                } else {
                                    Color.Transparent
                                },
                            )
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Buka filter lokasi",
                                onClick = onFilterClick,
                            ),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Tune,
                            contentDescription = "Filter lokasi",
                            tint = if (hasActiveFilters) HomeOrange else Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                        if (hasActiveFilters) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(top = 5.dp, end = 5.dp)
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(HomeOrange)
                                    .border(1.5.dp, HomeNavy, CircleShape),
                            )
                        }
                    }
                }
            }
        },
    )
}

@Composable
private fun VenueSearchDropdown(
    results: List<VenueUiModel>,
    onSelectVenue: (VenueUiModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = HomeNavy.copy(alpha = 0.98f),
        border = BorderStroke(1.dp, HomeBlue),
        shadowElevation = 10.dp,
    ) {
        if (results.isEmpty()) {
            Text(
                text = "Lokasi olahraga tidak ditemukan.",
                color = HomeText.copy(alpha = 0.75f),
                fontFamily = Poppins,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 220.dp),
            ) {
                itemsIndexed(
                    items = results,
                    key = { _, venue -> venue.id },
                ) { index, venue ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                role = Role.Button,
                                onClickLabel = "Pilih lokasi ${venue.name}",
                                onClick = { onSelectVenue(venue) },
                            )
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(HomeBlue),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = null,
                                tint = HomeOrange,
                                modifier = Modifier.size(18.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Text(
                                    text = venue.name,
                                    color = Color.White,
                                    fontFamily = Poppins,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false),
                                )
                                Text(
                                    text = venue.sportCategory,
                                    color = HomeOrange,
                                    fontFamily = Poppins,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                )
                            }
                            val subtitle = venue.address.ifBlank { venue.shortDescription }
                            if (subtitle.isNotBlank()) {
                                Text(
                                    text = subtitle,
                                    color = HomeText.copy(alpha = 0.72f),
                                    fontFamily = Poppins,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    if (index < results.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            color = HomeBlue.copy(alpha = 0.7f),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Panel Bawah State 1 (Default): panel navy dengan sudut membulat bertuliskan ajakan
 * untuk memilih lokasi olahraga pada peta.
 */
@Composable
fun VenueDefaultBottomPanel(
    modifier: Modifier = Modifier,
    isLoadingVenues: Boolean = false,
    isEmptyResult: Boolean = false,
    hasActiveFilters: Boolean = false,
    onResetFilters: () -> Unit = {},
    bottomContentPadding: Dp = 0.dp,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SheetTopShape,
        color = KuyNavy,
        border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.65f)),
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = 24.dp,
                    top = 18.dp,
                    end = 24.dp,
                    bottom = 18.dp + bottomContentPadding,
                ),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SheetDragHandle()
            when {
                isLoadingVenues -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        CircularProgressIndicator(
                            color = HomeOrange,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                        Text(
                            text = "Memuat lokasi olahraga...",
                            color = HomeText.copy(alpha = 0.85f),
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Normal,
                            fontSize = 13.sp,
                        )
                    }
                }

                isEmptyResult -> {
                    Text(
                        text = if (hasActiveFilters) {
                            "Tidak ada lokasi olahraga yang cocok dengan filter."
                        } else {
                            "Belum ada lokasi olahraga yang tersedia."
                        },
                        color = HomeText.copy(alpha = 0.9f),
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    if (hasActiveFilters) {
                        Text(
                            text = "Tampilkan semua lokasi",
                            color = HomeOrange,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(
                                    role = Role.Button,
                                    onClickLabel = "Tampilkan semua lokasi",
                                    onClick = onResetFilters,
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                else -> {
                    Text(
                        text = "Pilih tempat Olahraga untuk melihat detail..",
                        color = HomeText,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

/**
 * Bottom Sheet State 2 (Detail Venue):
 * panah kembali di kiri atas sheet, foto venue di kiri (Coil + fallback placeholder),
 * nama venue + kategori oranye + deskripsi lengkap di kanan, serta 2 tombol aksi
 * `"Ajak Teman"` (oranye [KuyButton]) dan `"Lokasi"` (putih berteks & border oranye).
 */
@Composable
fun VenueDetailBottomSheet(
    venue: VenueUiModel,
    onBackClick: () -> Unit,
    onInviteClick: () -> Unit,
    onLocationClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoadingRoute: Boolean = false,
    bottomContentPadding: Dp = 0.dp,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SheetTopShape,
        color = KuyNavy,
        border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.65f)),
        shadowElevation = 14.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = 20.dp,
                    top = 14.dp,
                    end = 20.dp,
                    bottom = 14.dp + bottomContentPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .size(38.dp),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Tutup detail lokasi",
                        tint = HomeText,
                    )
                }
                SheetDragHandle()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top,
            ) {
                VenuePhotoThumbnail(
                    imageUrl = venue.imageUrl,
                    venueName = venue.name,
                    sportCategory = venue.sportCategory,
                    modifier = Modifier
                        .width(124.dp)
                        .height(152.dp),
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(max = 180.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = venue.name,
                        color = Color.White,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        lineHeight = 22.sp,
                    )
                    Text(
                        text = venue.sportCategory,
                        color = KuyOrange,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp,
                        lineHeight = 16.sp,
                    )
                    Text(
                        text = venue.description.ifBlank { venue.shortDescription },
                        color = HomeText.copy(alpha = 0.9f),
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Button(
                    onClick = onInviteClick,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KuyButton,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    Text(
                        text = "Ajak Teman",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        maxLines = 1,
                    )
                }

                OutlinedButton(
                    onClick = onLocationClick,
                    enabled = !isLoadingRoute,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.5.dp, KuyOrange),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White,
                        contentColor = KuyOrange,
                        disabledContainerColor = Color.White.copy(alpha = 0.8f),
                        disabledContentColor = KuyOrange.copy(alpha = 0.6f),
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                ) {
                    if (isLoadingRoute) {
                        CircularProgressIndicator(
                            color = KuyOrange,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp),
                        )
                    } else {
                        Text(
                            text = "Lokasi",
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun VenuePhotoThumbnail(
    imageUrl: String,
    venueName: String,
    sportCategory: String,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = modifier
            .clip(shape)
            .background(HomeBlue)
            .border(1.dp, HomeBlue.copy(alpha = 0.9f), shape),
        contentAlignment = Alignment.Center,
    ) {
        if (imageUrl.isBlank()) {
            VenuePhotoFallbackPlaceholder(sportCategory = sportCategory)
        } else {
            SubcomposeAsyncImage(
                model = imageUrl,
                contentDescription = "Foto $venueName",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            color = HomeOrange,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp),
                        )
                    }
                },
                error = {
                    VenuePhotoFallbackPlaceholder(sportCategory = sportCategory)
                },
            )
        }
    }
}

@Composable
private fun VenuePhotoFallbackPlaceholder(
    sportCategory: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(HomeBlue, HomeNavy),
                ),
            )
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Image,
            contentDescription = null,
            tint = HomeOrange.copy(alpha = 0.85f),
            modifier = Modifier.size(30.dp),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = sportCategory.ifBlank { "Olahraga" },
            color = HomeText.copy(alpha = 0.85f),
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Panel State 3 (Mode Rute):
 * judul `"Jarak ke lokasi"`, metrik jarak & estimasi waktu tempuh, serta tombol lebar `"Berhenti"`.
 */
@Composable
fun VenueRoutePanel(
    venue: VenueUiModel,
    routeInfo: RouteInfo?,
    isLoadingRoute: Boolean,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SheetTopShape,
        color = KuyNavy,
        border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.65f)),
        shadowElevation = 14.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = 20.dp,
                    top = 16.dp,
                    end = 20.dp,
                    bottom = 16.dp + bottomContentPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                SheetDragHandle()
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Jarak ke lokasi",
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = venue.name,
                    color = HomeOrange,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            if (isLoadingRoute && routeInfo == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(HomeBlue)
                        .padding(horizontal = 16.dp, vertical = 18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    CircularProgressIndicator(
                        color = HomeOrange,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = "Menghitung rute menuju lokasi...",
                        color = HomeText,
                        fontFamily = Poppins,
                        fontSize = 13.sp,
                    )
                }
            } else if (routeInfo != null) {
                val travelIcon = when (routeInfo.travelMode) {
                    TravelMode.WALKING -> Icons.AutoMirrored.Rounded.DirectionsWalk
                    TravelMode.DRIVING -> Icons.Rounded.DirectionsCar
                }
                val travelLabel = when (routeInfo.travelMode) {
                    TravelMode.WALKING -> "Jalan kaki"
                    TravelMode.DRIVING -> "Berkendara"
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    RouteMetricCard(
                        icon = Icons.Rounded.NearMe,
                        value = formatDistance(routeInfo.distanceMeters),
                        label = "Jarak tempuh",
                        modifier = Modifier.weight(1f),
                    )
                    RouteMetricCard(
                        icon = travelIcon,
                        value = formatDuration(routeInfo.durationMinutes),
                        label = travelLabel,
                        modifier = Modifier.weight(1f),
                    )
                }

                if (routeInfo.isFallback) {
                    Text(
                        text = "Menampilkan estimasi jalur langsung (mode offline).",
                        color = HomeText.copy(alpha = 0.7f),
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                    )
                }
            } else {
                Text(
                    text = "Informasi rute belum tersedia.",
                    color = HomeText.copy(alpha = 0.75f),
                    fontFamily = Poppins,
                    fontSize = 13.sp,
                )
            }

            Button(
                onClick = onStopClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KuyButton,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Berhenti",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }
        }
    }
}

@Composable
private fun RouteMetricCard(
    icon: ImageVector,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        color = HomeBlue,
        border = BorderStroke(1.dp, KuyButton.copy(alpha = 0.45f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(HomeNavy),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = HomeOrange,
                    modifier = Modifier.size(20.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = value,
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = label,
                    color = HomeText.copy(alpha = 0.78f),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * Bottom Sheet State 4 (Ajak Teman):
 * opsi `"Salin tautan"` dan daftar horizontal `"Bagikan ke"` memakai [KuyUserAvatar].
 */
@Composable
fun VenueShareBottomSheet(
    venue: VenueUiModel,
    recipients: List<ShareRecipient>,
    onCopyLinkClick: () -> Unit,
    onRecipientClick: (ShareRecipient) -> Unit,
    onDismissClick: () -> Unit,
    modifier: Modifier = Modifier,
    bottomContentPadding: Dp = 0.dp,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SheetTopShape,
        color = KuyNavy,
        border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.65f)),
        shadowElevation = 14.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(
                    start = 20.dp,
                    top = 16.dp,
                    end = 20.dp,
                    bottom = 16.dp + bottomContentPadding,
                ),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                SheetDragHandle()
                IconButton(
                    onClick = onDismissClick,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Tutup bagikan lokasi",
                        tint = HomeText,
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Ajak Teman",
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
                Text(
                    text = "${venue.name} • ${venue.sportCategory}",
                    color = HomeOrange,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Surface(
                onClick = onCopyLinkClick,
                shape = RoundedCornerShape(16.dp),
                color = HomeBlue,
                border = BorderStroke(1.dp, KuyButton.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(HomeNavy),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ContentCopy,
                            contentDescription = "Salin tautan",
                            tint = HomeOrange,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Salin tautan",
                            color = Color.White,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = buildVenueShareLink(venue),
                            color = HomeText.copy(alpha = 0.72f),
                            fontFamily = Poppins,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Bagikan ke",
                    color = HomeText,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )

                if (recipients.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = HomeBlue.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Belum ada teman atau grup obrolan untuk dibagikan.",
                            color = HomeText.copy(alpha = 0.75f),
                            fontFamily = Poppins,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        )
                    }
                } else {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp),
                    ) {
                        items(
                            items = recipients,
                            key = { recipient -> recipient.id },
                        ) { recipient ->
                            ShareRecipientItem(
                                recipient = recipient,
                                onClick = { onRecipientClick(recipient) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShareRecipientItem(
    recipient: ShareRecipient,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(72.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                role = Role.Button,
                onClickLabel = "Bagikan ke ${recipient.name}",
                onClick = onClick,
            )
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            KuyUserAvatar(
                name = recipient.name,
                size = 54.dp,
                fontSize = 20.sp,
                photoUrl = recipient.avatarUrl,
                backgroundColor = Color(recipient.avatarColorHex),
            )
            if (recipient.isGroup) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(KuyOrange)
                        .border(1.5.dp, KuyNavy, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Groups,
                        contentDescription = "Grup",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }
        }
        Text(
            text = recipient.name,
            color = HomeText,
            fontFamily = Poppins,
            fontWeight = FontWeight.Medium,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun SheetDragHandle(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(42.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(KuyText.copy(alpha = 0.35f)),
    )
}

/**
 * Tombol bulat melayang bergaya Google Maps (`48.dp`) untuk memusatkan peta ke posisi pengguna.
 */
@Composable
fun VenueMyLocationButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLocationPermissionGranted: Boolean = true,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(48.dp),
        shape = CircleShape,
        color = HomeNavy.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.9f)),
        shadowElevation = 6.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                imageVector = Icons.Rounded.MyLocation,
                contentDescription = "Posisi saya",
                tint = if (isLocationPermissionGranted) HomeOrange else Color.White,
                modifier = Modifier.size(22.dp),
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF142133)
@Composable
private fun VenueMyLocationButtonPreview() {
    OlahragaKuyTheme {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            VenueMyLocationButton(onClick = {}, isLocationPermissionGranted = true)
            VenueMyLocationButton(onClick = {}, isLocationPermissionGranted = false)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1A2C)
@Composable
private fun VenueSearchHeaderPreview() {
    OlahragaKuyTheme {
        VenueSearchHeader(
            query = "",
            onQueryChange = {},
            searchResults = DefaultUsuVenues.take(2),
            onSelectVenue = {},
            onBackClick = {},
            selectedSportFilter = VenueSportFilter.BADMINTON,
            selectedRadiusFilter = VenueRadiusFilter.KM_3,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B192C)
@Composable
private fun VenueFilterSheetContentPreview() {
    OlahragaKuyTheme {
        VenueFilterSheetContent(
            selectedSportFilter = VenueSportFilter.BADMINTON,
            selectedRadiusFilter = VenueRadiusFilter.KM_3,
            onSportSelected = {},
            onRadiusSelected = {},
            onResetClick = {},
            onApplyClick = {},
            onCloseClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1A2C)
@Composable
private fun VenueDefaultBottomPanelPreview() {
    OlahragaKuyTheme {
        VenueDefaultBottomPanel()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1A2C)
@Composable
private fun VenueDetailBottomSheetPreview() {
    OlahragaKuyTheme {
        VenueDetailBottomSheet(
            venue = DefaultUsuVenues.first(),
            onBackClick = {},
            onInviteClick = {},
            onLocationClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1A2C)
@Composable
private fun VenueRoutePanelPreview() {
    OlahragaKuyTheme {
        VenueRoutePanel(
            venue = DefaultUsuVenues.first(),
            routeInfo = RouteInfo(
                points = listOf(DefaultUsuCoordinate, DefaultUsuVenues.first().coordinate),
                distanceMeters = 250.0,
                durationMinutes = 4,
                travelMode = TravelMode.WALKING,
            ),
            isLoadingRoute = false,
            onStopClick = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1A2C)
@Composable
private fun VenueShareBottomSheetPreview() {
    OlahragaKuyTheme {
        VenueShareBottomSheet(
            venue = DefaultUsuVenues.first(),
            recipients = listOf(
                ShareRecipient(id = "r1", name = "Budi Santoso", isGroup = false, avatarColorHex = 0xFF59C39CL),
                ShareRecipient(id = "r2", name = "Komunitas Basket USU", isGroup = true, avatarColorHex = 0xFF6B8FF7L),
                ShareRecipient(id = "r3", name = "Siti Aisyah", isGroup = false, avatarColorHex = 0xFFF2B84BL),
            ),
            onCopyLinkClick = {},
            onRecipientClick = {},
            onDismissClick = {},
        )
    }
}
