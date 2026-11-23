package com.example.olahragakuy.pages.venuefinder

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.tooling.preview.Preview
import com.example.olahragakuy.pages.posts.currentUserOrNull
import com.example.olahragakuy.pages.posts.resolveDisplayName
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.components.KuyBottomNavBar
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * Layar utama Venue Finder yang menyatukan peta interaktif ([VenueMapView]) serta 4 state overlay:
 * 1. [VenueFinderMode.DEFAULT]: header pencarian + [VenueDefaultBottomPanel].
 * 2. [VenueFinderMode.DETAIL]: header pencarian + [VenueDetailBottomSheet].
 * 3. [VenueFinderMode.ROUTE]: header pencarian + polyline rute oranye + [VenueRoutePanel].
 * 4. [VenueFinderMode.SHARE]: scrim redup + [VenueShareBottomSheet] ("Ajak Teman").
 */
@Composable
fun VenueFinderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialFocusedVenueName: String? = null,
    onTabSelected: (HomeNavTab) -> Unit = {},
    onNavigateToChat: (VenueChatTarget) -> Unit = {},
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val coroutineScope = rememberCoroutineScope()

    val currentUser = remember(isPreview) { if (isPreview) null else currentUserOrNull() }
    val currentUserId = currentUser?.uid ?: "me"
    val currentUserName = remember(currentUser) { currentUser.resolveDisplayName("Kamu") }
    val currentUserPhotoUrl = currentUser?.photoUrl?.toString()

    val hasInitialVenueTarget = !initialFocusedVenueName.isNullOrBlank()

    var uiState by remember {
        mutableStateOf(
            VenueFinderUiState(
                venues = if (isPreview) filterActiveVenues(DefaultUsuVenues) else emptyList(),
                isLoadingVenues = !isPreview,
            ),
        )
    }
    var showSearchDropdown by rememberSaveable { mutableStateOf(false) }
    var showFilterSheet by rememberSaveable { mutableStateOf(false) }
    var hasAppliedInitialFocus by rememberSaveable(initialFocusedVenueName) { mutableStateOf(false) }
    var hasAutoCenteredInitialUser by rememberSaveable(initialFocusedVenueName) {
        mutableStateOf(hasInitialVenueTarget)
    }
    var initialUserCenterCoordinate by remember(initialFocusedVenueName) {
        mutableStateOf<GeoCoordinate?>(null)
    }
    var hasRequestedPermission by rememberSaveable { mutableStateOf(false) }
    var locationPermissionGranted by remember {
        mutableStateOf(!isPreview && hasLocationPermission(context))
    }
    var activeRouteJob by remember { mutableStateOf<Job?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { grantResults ->
        val granted = grantResults[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            grantResults[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        locationPermissionGranted = granted
        if (!granted) {
            uiState = uiState.copy(
                userLocation = DefaultUsuCoordinate,
                isUsingFallbackLocation = true,
            )
        }
    }

    if (!isPreview) {
        // Minta izin lokasi runtime sekali saat masuk layar bila belum diberikan.
        LaunchedEffect(Unit) {
            if (!locationPermissionGranted && !hasRequestedPermission) {
                hasRequestedPermission = true
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        }

        // Ambil koordinat GPS perangkat dengan fallback otomatis ke kawasan USU Medan.
        DisposableEffect(locationPermissionGranted) {
            val cancelLocationRequest = fetchCurrentUserLocation(
                context = context,
                onLocationResolved = { coordinate, isFallback ->
                    uiState = uiState.copy(
                        userLocation = coordinate,
                        isUsingFallbackLocation = isFallback,
                    )
                    if (
                        !isFallback &&
                        !hasAutoCenteredInitialUser &&
                        !hasInitialVenueTarget &&
                        uiState.selectedVenue == null &&
                        uiState.mode != VenueFinderMode.ROUTE
                    ) {
                        hasAutoCenteredInitialUser = true
                        initialUserCenterCoordinate = coordinate
                    }
                },
            )
            onDispose { cancelLocationRequest() }
        }

        // Observasi venue aktif secara real-time (isActive == true, fallback lokal USU Medan).
        DisposableEffect(Unit) {
            val registration = observeActiveVenues { activeVenues, _ ->
                val activeOnly = filterActiveVenues(activeVenues)
                val currentSelected = uiState.selectedVenue
                val refreshedSelected = currentSelected?.let { selected ->
                    activeOnly.firstOrNull { it.id == selected.id }
                }
                uiState = uiState.copy(
                    venues = activeOnly,
                    selectedVenue = refreshedSelected ?: currentSelected,
                    isLoadingVenues = false,
                )
            }
            onDispose { registration?.remove() }
        }

        // Muat daftar kontak & grup komunitas agar bottom sheet "Ajak Teman" langsung siap.
        LaunchedEffect(currentUserId, currentUserName) {
            loadVenueShareRecipients(
                currentUserId = currentUser?.uid,
                currentUserName = currentUserName,
                onResult = { recipients ->
                    uiState = uiState.copy(shareRecipients = recipients)
                },
            )
        }
    }

    // Jika pengguna sudah memilih venue atau masuk mode rute, matikan auto-center GPS awal.
    LaunchedEffect(uiState.selectedVenue?.id, uiState.mode) {
        if (uiState.selectedVenue != null || uiState.mode == VenueFinderMode.ROUTE) {
            hasAutoCenteredInitialUser = true
        }
    }

    // Jika layar dibuka dari tautan/kartu undangan venue tertentu, fokuskan ke venue tersebut.
    LaunchedEffect(uiState.venues, initialFocusedVenueName, hasAppliedInitialFocus) {
        val targetName = initialFocusedVenueName?.trim().orEmpty()
        if (targetName.isNotEmpty() && !hasAppliedInitialFocus && uiState.venues.isNotEmpty()) {
            val matched = uiState.venues.firstOrNull {
                it.name.equals(targetName, ignoreCase = true)
            } ?: uiState.venues.firstOrNull {
                it.name.contains(targetName, ignoreCase = true)
            }
            if (matched != null) {
                hasAppliedInitialFocus = true
                hasAutoCenteredInitialUser = true
                uiState = uiState.copy(
                    selectedSportFilter = VenueSportFilter.ALL,
                    selectedRadiusFilter = VenueRadiusFilter.ALL,
                    selectedVenue = matched,
                    mode = VenueFinderMode.DETAIL,
                )
            }
        }
    }

    val mapFilteredVenues = remember(
        uiState.venues,
        uiState.selectedSportFilter,
        uiState.selectedRadiusFilter,
        uiState.userLocation,
    ) {
        filterVenues(
            venues = uiState.venues,
            query = "",
            sportFilter = uiState.selectedSportFilter,
            radiusFilter = uiState.selectedRadiusFilter,
            userLocation = uiState.userLocation,
        )
    }

    val filteredVenues = remember(
        uiState.venues,
        uiState.searchQuery,
        uiState.selectedSportFilter,
        uiState.selectedRadiusFilter,
        uiState.userLocation,
    ) {
        filterVenues(
            venues = uiState.venues,
            query = uiState.searchQuery,
            sportFilter = uiState.selectedSportFilter,
            radiusFilter = uiState.selectedRadiusFilter,
            userLocation = uiState.userLocation,
        )
    }

    val hasActiveFilters = uiState.selectedSportFilter != VenueSportFilter.ALL ||
        uiState.selectedRadiusFilter != VenueRadiusFilter.ALL

    val isEmptyFilterResult = !uiState.isLoadingVenues &&
        mapFilteredVenues.isEmpty() &&
        (hasActiveFilters || uiState.venues.isNotEmpty())

    // Tangani dengan aman bila selectedVenue terfilter keluar oleh filter aktif / pembaruan lokasi.
    LaunchedEffect(uiState.isLoadingVenues, mapFilteredVenues, uiState.selectedVenue?.id) {
        val currentSelected = uiState.selectedVenue ?: return@LaunchedEffect
        if (!uiState.isLoadingVenues && mapFilteredVenues.none { it.id == currentSelected.id }) {
            activeRouteJob?.cancel()
            activeRouteJob = null
            uiState = uiState.copy(
                selectedVenue = null,
                mode = VenueFinderMode.DEFAULT,
                routeInfo = null,
                isLoadingRoute = false,
            )
        }
    }

    val applyFilters: (VenueSportFilter, VenueRadiusFilter) -> Unit = { sportFilter, radiusFilter ->
        val nextVisible = filterVenues(
            venues = uiState.venues,
            query = "",
            sportFilter = sportFilter,
            radiusFilter = radiusFilter,
            userLocation = uiState.userLocation,
        )
        val currentSelected = uiState.selectedVenue
        val isSelectedStillVisible = currentSelected == null ||
            nextVisible.any { it.id == currentSelected.id }
        if (!isSelectedStillVisible) {
            activeRouteJob?.cancel()
            activeRouteJob = null
        }
        uiState = uiState.copy(
            selectedSportFilter = sportFilter,
            selectedRadiusFilter = radiusFilter,
            selectedVenue = if (isSelectedStillVisible) currentSelected else null,
            mode = if (isSelectedStillVisible) uiState.mode else VenueFinderMode.DEFAULT,
            routeInfo = if (isSelectedStillVisible) uiState.routeInfo else null,
            isLoadingRoute = isSelectedStillVisible && uiState.isLoadingRoute,
        )
    }

    val resetFilters: () -> Unit = {
        applyFilters(VenueSportFilter.ALL, VenueRadiusFilter.ALL)
    }

    val dismissSearchUi = {
        showSearchDropdown = false
        focusManager.clearFocus()
        keyboardController?.hide()
    }

    val stopRouteMode: () -> Unit = {
        activeRouteJob?.cancel()
        activeRouteJob = null
        uiState = uiState.copy(
            mode = if (uiState.selectedVenue != null) {
                VenueFinderMode.DETAIL
            } else {
                VenueFinderMode.DEFAULT
            },
            routeInfo = null,
            isLoadingRoute = false,
        )
    }

    val startRouteMode: (VenueUiModel) -> Unit = { venue ->
        dismissSearchUi()
        activeRouteJob?.cancel()
        val origin = uiState.userLocation
        uiState = uiState.copy(
            selectedVenue = venue,
            mode = VenueFinderMode.ROUTE,
            routeInfo = null,
            isLoadingRoute = true,
        )
        activeRouteJob = coroutineScope.launch {
            val calculated = fetchVenueRoute(
                from = origin,
                to = venue.coordinate,
            )
            if (uiState.mode == VenueFinderMode.ROUTE && uiState.selectedVenue?.id == venue.id) {
                uiState = uiState.copy(
                    routeInfo = calculated,
                    isLoadingRoute = false,
                )
            }
        }
    }

    val focusVenueOnMap: (VenueUiModel) -> Unit = { venue ->
        dismissSearchUi()
        activeRouteJob?.cancel()
        activeRouteJob = null
        uiState = uiState.copy(
            searchQuery = venue.name,
            selectedVenue = venue,
            mode = VenueFinderMode.DEFAULT,
            routeInfo = null,
            isLoadingRoute = false,
        )
    }

    val openVenueDetail: (VenueUiModel) -> Unit = { venue ->
        dismissSearchUi()
        uiState = uiState.copy(
            selectedVenue = venue,
            mode = VenueFinderMode.DETAIL,
        )
    }

    val openShareSheet: () -> Unit = {
        dismissSearchUi()
        if (!isPreview && uiState.shareRecipients.isEmpty()) {
            loadVenueShareRecipients(
                currentUserId = currentUser?.uid,
                currentUserName = currentUserName,
                onResult = { recipients ->
                    uiState = uiState.copy(shareRecipients = recipients)
                },
            )
        }
        uiState = uiState.copy(mode = VenueFinderMode.SHARE)
    }

    val handleOverlayBack: () -> Unit = {
        when {
            showSearchDropdown -> dismissSearchUi()
            uiState.mode == VenueFinderMode.SHARE -> {
                uiState = uiState.copy(mode = VenueFinderMode.DETAIL)
            }
            uiState.mode == VenueFinderMode.ROUTE -> stopRouteMode()
            uiState.mode == VenueFinderMode.DETAIL -> {
                uiState = uiState.copy(mode = VenueFinderMode.DEFAULT)
            }
            else -> onBack()
        }
    }

    BackHandler(enabled = showSearchDropdown || uiState.mode != VenueFinderMode.DEFAULT) {
        handleOverlayBack()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeNavy),
    ) {
        VenueMapView(
            venues = mapFilteredVenues,
            userLocation = uiState.userLocation,
            selectedVenue = uiState.selectedVenue,
            routeInfo = uiState.routeInfo,
            mode = uiState.mode,
            onVenueClick = { venue ->
                dismissSearchUi()
                hasAutoCenteredInitialUser = true
                uiState = uiState.copy(selectedVenue = venue)
            },
            onVenueDetailClick = openVenueDetail,
            userName = currentUserName,
            userPhotoUrl = currentUserPhotoUrl,
            initialUserCenterCoordinate = initialUserCenterCoordinate,
            modifier = Modifier.fillMaxSize(),
        )

        // Header pencarian + tombol filter terintegrasi & active pills tampil pada State 1–3.
        AnimatedVisibility(
            visible = uiState.mode != VenueFinderMode.SHARE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding(),
        ) {
            VenueSearchHeader(
                query = uiState.searchQuery,
                onQueryChange = { newQuery ->
                    uiState = uiState.copy(searchQuery = newQuery)
                    showSearchDropdown = newQuery.isNotBlank()
                },
                searchResults = filteredVenues,
                onSelectVenue = focusVenueOnMap,
                onBackClick = onBack,
                showDropdown = showSearchDropdown,
                selectedSportFilter = uiState.selectedSportFilter,
                selectedRadiusFilter = uiState.selectedRadiusFilter,
                onFilterClick = {
                    dismissSearchUi()
                    showFilterSheet = true
                },
                onClearSportFilter = {
                    applyFilters(VenueSportFilter.ALL, uiState.selectedRadiusFilter)
                },
                onClearRadiusFilter = {
                    applyFilters(uiState.selectedSportFilter, VenueRadiusFilter.ALL)
                },
                showEmptyFilterNotice = isEmptyFilterResult,
                onResetFilters = resetFilters,
                onSearchSubmit = {
                    val topMatch = filteredVenues.firstOrNull()
                    if (topMatch != null) {
                        focusVenueOnMap(topMatch)
                    } else {
                        showSearchDropdown = uiState.searchQuery.isNotBlank()
                        keyboardController?.hide()
                    }
                },
            )
        }

        // Scrim redup ketika State 4 ("Ajak Teman") aktif.
        AnimatedVisibility(
            visible = uiState.mode == VenueFinderMode.SHARE,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.45f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = { uiState = uiState.copy(mode = VenueFinderMode.DETAIL) },
                    ),
            )
        }

        // Panel & Bottom Sheet sesuai 4 state aktif (dengan ruang aman di atas Bottom Navigation Bar).
        AnimatedContent(
            targetState = uiState.mode,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 3 }) togetherWith
                    (fadeOut() + slideOutVertically { it / 3 })
            },
            label = "VenueOverlaySheetTransition",
            modifier = Modifier.align(Alignment.BottomCenter),
        ) { mode ->
            val selectedVenue = uiState.selectedVenue
            when (mode) {
                VenueFinderMode.DEFAULT -> {
                    VenueDefaultBottomPanel(
                        isLoadingVenues = uiState.isLoadingVenues,
                        isEmptyResult = isEmptyFilterResult,
                        hasActiveFilters = hasActiveFilters,
                        onResetFilters = resetFilters,
                        bottomContentPadding = VenueBottomNavClearance,
                    )
                }

                VenueFinderMode.DETAIL -> {
                    if (selectedVenue != null) {
                        VenueDetailBottomSheet(
                            venue = selectedVenue,
                            onBackClick = {
                                uiState = uiState.copy(mode = VenueFinderMode.DEFAULT)
                            },
                            onInviteClick = openShareSheet,
                            onLocationClick = { startRouteMode(selectedVenue) },
                            isLoadingRoute = uiState.isLoadingRoute,
                            bottomContentPadding = VenueBottomNavClearance,
                        )
                    } else {
                        VenueDefaultBottomPanel(
                            isLoadingVenues = uiState.isLoadingVenues,
                            isEmptyResult = isEmptyFilterResult,
                            hasActiveFilters = hasActiveFilters,
                            onResetFilters = resetFilters,
                            bottomContentPadding = VenueBottomNavClearance,
                        )
                    }
                }

                VenueFinderMode.ROUTE -> {
                    if (selectedVenue != null) {
                        VenueRoutePanel(
                            venue = selectedVenue,
                            routeInfo = uiState.routeInfo,
                            isLoadingRoute = uiState.isLoadingRoute,
                            onStopClick = stopRouteMode,
                            bottomContentPadding = VenueBottomNavClearance,
                        )
                    } else {
                        VenueDefaultBottomPanel(
                            isLoadingVenues = uiState.isLoadingVenues,
                            isEmptyResult = isEmptyFilterResult,
                            hasActiveFilters = hasActiveFilters,
                            onResetFilters = resetFilters,
                            bottomContentPadding = VenueBottomNavClearance,
                        )
                    }
                }

                VenueFinderMode.SHARE -> {
                    if (selectedVenue != null) {
                        VenueShareBottomSheet(
                            venue = selectedVenue,
                            recipients = uiState.shareRecipients,
                            onCopyLinkClick = {
                                val copied = copyVenueLinkToClipboard(context, selectedVenue)
                                Toast.makeText(
                                    context,
                                    if (copied) "Tautan lokasi disalin" else "Gagal menyalin tautan",
                                    Toast.LENGTH_SHORT,
                                ).show()
                            },
                            onRecipientClick = { recipient ->
                                val target = VenueChatBridge.sendVenueInvite(
                                    venue = selectedVenue,
                                    recipient = recipient,
                                    currentUserId = currentUserId,
                                    currentUserName = currentUserName,
                                    syncToFirestore = !isPreview,
                                )
                                uiState = uiState.copy(mode = VenueFinderMode.DETAIL)
                                Toast.makeText(
                                    context,
                                    "Ajakan dikirim ke ${recipient.name}",
                                    Toast.LENGTH_SHORT,
                                ).show()
                                onNavigateToChat(target)
                            },
                            onDismissClick = {
                                uiState = uiState.copy(mode = VenueFinderMode.DETAIL)
                            },
                            bottomContentPadding = VenueBottomNavClearance,
                        )
                    } else {
                        VenueDefaultBottomPanel(
                            isLoadingVenues = uiState.isLoadingVenues,
                            isEmptyResult = isEmptyFilterResult,
                            hasActiveFilters = hasActiveFilters,
                            onResetFilters = resetFilters,
                            bottomContentPadding = VenueBottomNavClearance,
                        )
                    }
                }
            }
        }

        // Bottom Navigation Bar 4 ikon konsisten dengan tab utama lainnya (tab Lokasi aktif).
        KuyBottomNavBar(
            selectedTab = HomeNavTab.LOKASI,
            onTabSelected = onTabSelected,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    if (showFilterSheet) {
        VenueFilterBottomSheet(
            selectedSportFilter = uiState.selectedSportFilter,
            selectedRadiusFilter = uiState.selectedRadiusFilter,
            onApplyFilters = applyFilters,
            onDismiss = { showFilterSheet = false },
        )
    }
}

private fun copyVenueLinkToClipboard(
    context: Context,
    venue: VenueUiModel,
): Boolean {
    return try {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
            ?: return false
        val link = buildVenueShareLink(venue)
        val clip = ClipData.newPlainText("Tautan ${venue.name}", link)
        clipboard.setPrimaryClip(clip)
        true
    } catch (_: Exception) {
        false
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A1A2C)
@Composable
private fun VenueFinderScreenPreview() {
    OlahragaKuyTheme {
        VenueFinderScreen(
            onBack = {},
        )
    }
}
