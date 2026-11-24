package com.example.olahragakuy.pages.venuefinder

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import coil.compose.SubcomposeAsyncImage
import com.example.olahragakuy.ui.components.KuyUserAvatar
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyNavy
import com.example.olahragakuy.ui.theme.KuyOrange
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapLibreMapOptions
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.BackgroundLayer
import org.maplibre.android.style.layers.FillLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.layers.SymbolLayer
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.FeatureCollection
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import java.util.Locale
import kotlin.math.roundToInt

private const val OpenFreeMapFiordStyleUri = "https://tiles.openfreemap.org/styles/fiord"

private const val RouteSourceId = "venue-route-source"
private const val RouteCasingLayerId = "venue-route-casing-layer"
private const val RouteSolidLayerId = "venue-route-solid-layer"
private const val RouteDashedLayerId = "venue-route-dashed-layer"

private val RouteLayerIds = setOf(
    RouteCasingLayerId,
    RouteSolidLayerId,
    RouteDashedLayerId,
)

// Palet dark navy peta agar menyatu dengan tema aplikasi namun tetap sedikit lebih terang dari kartu (#0D1B2A).
private val MapBackgroundColor = Color(0xFF142133)
private val MapWaterColor = Color(0xFF0B1524)
private val MapParkColor = Color(0xFF142A36)
private val MapLandColor = Color(0xFF17263A)
private val MapLandAccentColor = Color(0xFF1A2B42)
private val MapRoadCasingColor = Color(0xFF0E1928)
private val MapMajorRoadColor = Color(0xFF314B6E)
private val MapMinorRoadColor = Color(0xFF233752)
private val MapBoundaryRailColor = Color(0xFF263B57)
private val MapLabelTextColor = Color(0xFF8EA6C2)
private val MapLabelHaloColor = Color(0xFF0E1A2A)

private const val DefaultMapZoom = 15.9
private const val RecenterUserZoom = 15.9
private const val FocusVenueZoom = 16.8
private const val MinMapZoom = 4.0
private const val MaxMapZoom = 19.0
private const val FocusAnimationMs = 650

private val VenueCardWidth = 224.dp
private val MarkerDotSize = 12.dp
private val RouteFocusHorizontalPadding = 72.dp
private val RouteFocusTopPadding = 128.dp
private val RouteFocusBottomPadding = 220.dp

/**
 * Peta vector MapLibre bergaya dark navy (OpenFreeMap Fiord) untuk layar Venue Finder.
 *
 * Menampilkan pin lokasi pengguna, kartu marker venue yang mengikuti proyeksi koordinat layar saat
 * peta digeser/di-zoom, serta garis rute oranye ketika [mode] berada di [VenueFinderMode.ROUTE].
 * Di Compose Preview (inspection mode) peta diganti placeholder agar preview tetap aman.
 */
@Composable
fun VenueMapView(
    venues: List<VenueUiModel>,
    userLocation: GeoCoordinate,
    selectedVenue: VenueUiModel?,
    routeInfo: RouteInfo?,
    mode: VenueFinderMode,
    onVenueClick: (VenueUiModel) -> Unit,
    onVenueDetailClick: (VenueUiModel) -> Unit,
    modifier: Modifier = Modifier,
    userName: String = "Kamu",
    userPhotoUrl: String? = null,
    initialUserCenterCoordinate: GeoCoordinate? = null,
    recenterToUserRequest: Int = 0,
    onUserLocationClick: () -> Unit = {},
) {
    if (LocalInspectionMode.current) {
        VenueMapInspectionPlaceholder(
            venues = venues,
            selectedVenue = selectedVenue,
            userName = userName,
            userPhotoUrl = userPhotoUrl,
            modifier = modifier,
        )
        return
    }

    val context = LocalContext.current
    val density = LocalDensity.current
    val lifecycleOwner = remember(context) { context.findLifecycleOwner() }
    val initialCameraCoordinate = remember {
        selectedVenue?.coordinate ?: initialUserCenterCoordinate ?: userLocation
    }
    val initialCameraZoom = remember {
        if (selectedVenue != null) FocusVenueZoom else DefaultMapZoom
    }

    val mapView = remember(context) {
        MapLibre.getInstance(context)
        createMapLibreMapView(
            context = context,
            initialCenter = initialCameraCoordinate,
            initialZoom = initialCameraZoom,
        )
    }

    var mapLibreMap by remember(mapView) { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember(mapView) { mutableStateOf<Style?>(null) }
    val cameraTick = remember(mapView) { mutableIntStateOf(0) }
    val readCameraTick = remember(cameraTick) { { cameraTick.intValue } }

    // Kelola lifecycle penuh MapView (onCreate..onDestroy & onLowMemory) dari LifecycleOwner context.
    DisposableEffect(mapView, lifecycleOwner) {
        var isCreated = false
        var isStarted = false
        var isResumed = false
        var isDestroyed = false

        fun moveToCreated() {
            if (!isCreated && !isDestroyed) {
                mapView.onCreate(null)
                isCreated = true
            }
        }

        fun moveToStarted() {
            moveToCreated()
            if (!isStarted && !isDestroyed) {
                mapView.onStart()
                isStarted = true
            }
        }

        fun moveToResumed() {
            moveToStarted()
            if (!isResumed && !isDestroyed) {
                mapView.onResume()
                isResumed = true
            }
        }

        fun moveToPaused() {
            if (isResumed) {
                mapView.onPause()
                isResumed = false
            }
        }

        fun moveToStopped() {
            moveToPaused()
            if (isStarted) {
                mapView.onStop()
                isStarted = false
            }
        }

        fun moveToDestroyed() {
            moveToStopped()
            if (isCreated && !isDestroyed) {
                mapView.onDestroy()
                isDestroyed = true
            }
        }

        val cameraMoveListener = MapLibreMap.OnCameraMoveListener {
            cameraTick.intValue++
        }
        val cameraIdleListener = MapLibreMap.OnCameraIdleListener {
            cameraTick.intValue++
        }
        val layoutChangeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
            cameraTick.intValue++
        }
        mapView.addOnLayoutChangeListener(layoutChangeListener)

        val lifecycle = lifecycleOwner?.lifecycle
        val lifecycleObserver = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_CREATE -> moveToCreated()
                Lifecycle.Event.ON_START -> moveToStarted()
                Lifecycle.Event.ON_RESUME -> moveToResumed()
                Lifecycle.Event.ON_PAUSE -> moveToPaused()
                Lifecycle.Event.ON_STOP -> moveToStopped()
                Lifecycle.Event.ON_DESTROY -> moveToDestroyed()
                else -> Unit
            }
        }

        val componentCallbacks = object : ComponentCallbacks2 {
            override fun onConfigurationChanged(newConfig: Configuration) = Unit

            @Deprecated("Deprecated in Java")
            override fun onLowMemory() {
                if (!isDestroyed) {
                    mapView.onLowMemory()
                }
            }

            override fun onTrimMemory(level: Int) {
                if (!isDestroyed && level >= ComponentCallbacks2.TRIM_MEMORY_BACKGROUND) {
                    mapView.onLowMemory()
                }
            }
        }

        val appContext = context.applicationContext
        appContext.registerComponentCallbacks(componentCallbacks)
        if (lifecycle != null) {
            lifecycle.addObserver(lifecycleObserver)
        } else {
            moveToResumed()
        }
        // Pastikan onCreate telah terpanggil sebelum meminta instance MapLibreMap.
        moveToCreated()

        mapView.getMapAsync { map ->
            if (isDestroyed) return@getMapAsync
            configureMapLibreMap(map)
            map.addOnCameraMoveListener(cameraMoveListener)
            map.addOnCameraIdleListener(cameraIdleListener)
            mapLibreMap = map
            cameraTick.intValue++

            map.setStyle(Style.Builder().fromUri(OpenFreeMapFiordStyleUri)) { style ->
                if (isDestroyed) return@setStyle
                applyDarkNavyMapTheme(style)
                ensureRouteLayers(style)
                loadedStyle = style
                cameraTick.intValue++
            }
        }

        onDispose {
            mapView.removeOnLayoutChangeListener(layoutChangeListener)
            mapLibreMap?.let { map ->
                map.removeOnCameraMoveListener(cameraMoveListener)
                map.removeOnCameraIdleListener(cameraIdleListener)
            }
            loadedStyle = null
            mapLibreMap = null
            lifecycle?.removeObserver(lifecycleObserver)
            appContext.unregisterComponentCallbacks(componentCallbacks)
            moveToDestroyed()
        }
    }

    val showRoute = mode == VenueFinderMode.ROUTE && (routeInfo?.points?.size ?: 0) >= 2

    // Sinkronkan sumber GeoJSON & layer garis rute oranye setiap kali rute atau style siap.
    LaunchedEffect(loadedStyle, routeInfo, showRoute) {
        val style = loadedStyle ?: return@LaunchedEffect
        updateRouteOnStyle(
            style = style,
            routeInfo = routeInfo,
            showRoute = showRoute,
        )
    }

    val hasAutoCenteredOnUser = remember(mapView) { mutableStateOf(false) }

    // Auto-fit bounds saat mode rute aktif, atau fokus halus ke venue terpilih.
    val routeHorizontalPaddingPx = with(density) { RouteFocusHorizontalPadding.roundToPx() }
    val routeTopPaddingPx = with(density) { RouteFocusTopPadding.roundToPx() }
    val routeBottomPaddingPx = with(density) { RouteFocusBottomPadding.roundToPx() }
    LaunchedEffect(mapLibreMap, showRoute, routeInfo, selectedVenue?.id) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (showRoute || selectedVenue != null) {
            hasAutoCenteredOnUser.value = true
        }
        mapView.runWhenLaidOut {
            when {
                showRoute && routeInfo != null -> {
                    val uniquePoints = routeInfo.points
                        .map { it.toLatLng() }
                        .distinctBy { "${it.latitude},${it.longitude}" }
                    if (uniquePoints.size >= 2) {
                        val maxHorizontal = (mapView.width / 4).coerceAtLeast(16)
                        val maxTop = (mapView.height / 4).coerceAtLeast(16)
                        val maxBottom = (mapView.height / 3).coerceAtLeast(16)
                        val horizontalPad = routeHorizontalPaddingPx.coerceAtMost(maxHorizontal)
                        val topPad = routeTopPaddingPx.coerceAtMost(maxTop)
                        val bottomPad = routeBottomPaddingPx.coerceAtMost(maxBottom)
                        val bounds = LatLngBounds.Builder()
                            .includes(uniquePoints)
                            .build()
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngBounds(
                                bounds,
                                horizontalPad,
                                topPad,
                                horizontalPad,
                                bottomPad,
                            ),
                            FocusAnimationMs,
                        )
                    } else if (uniquePoints.size == 1) {
                        map.animateCamera(
                            CameraUpdateFactory.newLatLngZoom(uniquePoints.first(), FocusVenueZoom),
                            FocusAnimationMs,
                        )
                    }
                }

                selectedVenue != null -> {
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            selectedVenue.coordinate.toLatLng(),
                            FocusVenueZoom,
                        ),
                        FocusAnimationMs,
                    )
                }
            }
        }
    }

    // Auto-center ke lokasi user hanya sekali saat pertama buka tab Lokasi tanpa target venue/rute.
    LaunchedEffect(mapLibreMap, initialUserCenterCoordinate) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val target = initialUserCenterCoordinate ?: return@LaunchedEffect
        if (!hasAutoCenteredOnUser.value && selectedVenue == null && !showRoute) {
            hasAutoCenteredOnUser.value = true
            mapView.runWhenLaidOut {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(target.toLatLng(), RecenterUserZoom),
                    FocusAnimationMs,
                )
            }
        }
    }

    // Fokus manual ke posisi user saat dipicu lewat recenterToUserRequest tanpa menimpa guard GPS.
    val latestUserLocation by rememberUpdatedState(userLocation)
    val lastHandledRecenterRequest = remember(mapView) { mutableIntStateOf(0) }
    LaunchedEffect(mapLibreMap, recenterToUserRequest) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (recenterToUserRequest > 0 && recenterToUserRequest != lastHandledRecenterRequest.intValue) {
            lastHandledRecenterRequest.intValue = recenterToUserRequest
            hasAutoCenteredOnUser.value = true
            val target = latestUserLocation
            mapView.runWhenLaidOut {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(target.toLatLng(), RecenterUserZoom),
                    FocusAnimationMs,
                )
            }
        }
    }

    val visibleVenues = remember(venues, selectedVenue, showRoute) {
        if (showRoute) {
            listOfNotNull(selectedVenue)
        } else {
            venues.filter { it.isActive }.sortedBy { it.id == selectedVenue?.id }
        }
    }
    val dotInsetPx = with(density) { (MarkerDotSize / 2).roundToPx() }

    Box(modifier.background(HomeNavy).clipToBounds()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        UserLocationPin(
            userName = userName,
            userPhotoUrl = userPhotoUrl,
            onClick = onUserLocationClick,
            modifier = Modifier.anchoredToGeo(
                mapView = mapView,
                mapLibreMap = mapLibreMap,
                coordinate = userLocation,
                cameraTick = readCameraTick,
            ),
        )

        visibleVenues.forEach { venue ->
            key(venue.id) {
                VenueMarkerCard(
                    venue = venue,
                    isSelected = venue.id == selectedVenue?.id,
                    onClick = { onVenueClick(venue) },
                    onDetailClick = { onVenueDetailClick(venue) },
                    modifier = Modifier.anchoredToGeo(
                        mapView = mapView,
                        mapLibreMap = mapLibreMap,
                        coordinate = venue.coordinate,
                        cameraTick = readCameraTick,
                        bottomInsetPx = dotInsetPx,
                    ),
                )
            }
        }

        MapAttribution(Modifier.align(Alignment.BottomEnd))
    }
}

@Composable
private fun VenueMarkerCard(
    venue: VenueUiModel,
    isSelected: Boolean,
    onClick: () -> Unit,
    onDetailClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(16.dp),
            color = HomeNavy,
            contentColor = HomeText,
            border = BorderStroke(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) HomeOrange else HomeBlue,
            ),
            shadowElevation = if (isSelected) 10.dp else 4.dp,
        ) {
            Row(
                modifier = Modifier
                    .width(VenueCardWidth)
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                VenueThumbnail(venue, Modifier.size(48.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        venue.name,
                        color = HomeText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        venue.sportCategory,
                        color = HomeOrange,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = onDetailClick,
                    modifier = Modifier.height(32.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KuyOrange,
                        contentColor = Color.White,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Text("Detail", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        MarkerPointer(color = if (isSelected) HomeOrange else HomeNavy)
        Spacer(Modifier.height(2.dp))
        Box(
            Modifier
                .size(MarkerDotSize)
                .clip(CircleShape)
                .background(HomeOrange)
                .border(2.dp, HomeText, CircleShape),
        )
    }
}

@Composable
private fun VenueThumbnail(venue: VenueUiModel, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(12.dp)
    SubcomposeAsyncImage(
        model = venue.imageUrl.takeIf { it.isNotBlank() },
        contentDescription = "Foto ${venue.name}",
        contentScale = ContentScale.Crop,
        modifier = modifier
            .clip(shape)
            .background(HomeBlue),
        loading = { VenueThumbnailFallback(venue, isLoading = true) },
        error = { VenueThumbnailFallback(venue, isLoading = false) },
    )
}

@Composable
private fun VenueThumbnailFallback(venue: VenueUiModel, isLoading: Boolean) {
    Box(
        Modifier
            .fillMaxSize()
            .background(HomeBlue),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                color = HomeOrange,
                strokeWidth = 2.dp,
                modifier = Modifier.size(18.dp),
            )
        } else {
            Text(
                venue.sportCategory.ifBlank { venue.name }.take(1).uppercase(),
                color = HomeOrange,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        }
    }
}

@Composable
private fun UserLocationPin(
    userName: String,
    userPhotoUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .semantics { contentDescription = "Lokasi kamu" }
            .clickable(onClickLabel = "Pusatkan ke lokasi kamu", onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .shadow(6.dp, CircleShape)
                .size(46.dp)
                .clip(CircleShape)
                .background(HomeOrange)
                .padding(3.dp),
            contentAlignment = Alignment.Center,
        ) {
            KuyUserAvatar(
                name = userName,
                size = 40.dp,
                fontSize = 16.sp,
                photoUrl = userPhotoUrl,
            )
        }
        MarkerPointer(color = HomeOrange)
    }
}

@Composable
private fun MarkerPointer(color: Color) {
    Canvas(Modifier.size(width = 16.dp, height = 8.dp)) {
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width, 0f)
            lineTo(size.width / 2f, size.height)
            close()
        }
        drawPath(path, color)
    }
}

@Composable
private fun MapAttribution(modifier: Modifier = Modifier) {
    Text(
        "© OpenFreeMap • © OpenStreetMap contributors",
        color = HomeText.copy(alpha = 0.75f),
        fontSize = 9.sp,
        modifier = modifier
            .padding(6.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(HomeNavy.copy(alpha = 0.75f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun VenueMapInspectionPlaceholder(
    venues: List<VenueUiModel>,
    selectedVenue: VenueUiModel?,
    userName: String,
    userPhotoUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(HomeNavy).clipToBounds()) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            val lineColor = HomeBlue.copy(alpha = 0.35f)
            var x = 0f
            while (x < size.width) {
                drawLine(lineColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(lineColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                y += step
            }
        }
        Text(
            "Pratinjau peta MapLibre",
            color = HomeText.copy(alpha = 0.6f),
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(12.dp),
        )
        val previewVenue = selectedVenue ?: venues.firstOrNull()
        if (previewVenue != null) {
            VenueMarkerCard(
                venue = previewVenue,
                isSelected = true,
                onClick = {},
                onDetailClick = {},
                modifier = Modifier.align(Alignment.Center),
            )
        }
        UserLocationPin(
            userName = userName,
            userPhotoUrl = userPhotoUrl,
            onClick = {},
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 120.dp),
        )
        MapAttribution(Modifier.align(Alignment.BottomEnd))
    }
}

/**
 * Menempatkan composable sehingga titik bawah-tengahnya berada tepat di proyeksi layar [coordinate].
 * State kamera dibaca pada fase placement agar pan/zoom tidak memicu rekomposisi penuh.
 */
private fun Modifier.anchoredToGeo(
    mapView: MapView,
    mapLibreMap: MapLibreMap?,
    coordinate: GeoCoordinate,
    cameraTick: () -> Int,
    bottomInsetPx: Int = 0,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(placeable.width, placeable.height) {
        cameraTick()
        val point = mapLibreMap?.screenPointOrNull(mapView, coordinate)
        if (point != null) {
            placeable.place(
                x = point.x - placeable.width / 2,
                y = point.y - placeable.height + bottomInsetPx,
            )
        }
    }
}

private fun MapLibreMap.screenPointOrNull(
    mapView: MapView,
    coordinate: GeoCoordinate,
): IntOffset? {
    val width = mapView.width
    val height = mapView.height
    if (width == 0 || height == 0) return null
    val screenPoint = projection.toScreenLocation(coordinate.toLatLng())
    if (!screenPoint.x.isFinite() || !screenPoint.y.isFinite()) return null
    val x = screenPoint.x.roundToInt()
    val y = screenPoint.y.roundToInt()
    val margin = maxOf(width, height) / 2
    val isNearViewport = x in -margin..(width + margin) &&
        y in -margin..(height + margin)
    return if (isNearViewport) IntOffset(x, y) else null
}

private fun MapView.runWhenLaidOut(action: () -> Unit) {
    if (isLaidOut && width > 0 && height > 0) {
        action()
    } else {
        val listener = object : View.OnLayoutChangeListener {
            override fun onLayoutChange(
                v: View,
                left: Int,
                top: Int,
                right: Int,
                bottom: Int,
                oldLeft: Int,
                oldTop: Int,
                oldRight: Int,
                oldBottom: Int,
            ) {
                if (v.width > 0 && v.height > 0) {
                    v.removeOnLayoutChangeListener(this)
                    action()
                }
            }
        }
        addOnLayoutChangeListener(listener)
    }
}

private fun createMapLibreMapView(
    context: Context,
    initialCenter: GeoCoordinate,
    initialZoom: Double,
): MapView {
    val options = MapLibreMapOptions.createFromAttributes(context)
        .foregroundLoadColor(MapBackgroundColor.toArgb())
        .compassEnabled(false)
        .logoEnabled(false)
        .attributionEnabled(false)
        .camera(
            CameraPosition.Builder()
                .target(initialCenter.toLatLng())
                .zoom(initialZoom)
                .build(),
        )
    return MapView(context, options).apply {
        setBackgroundColor(MapBackgroundColor.toArgb())
    }
}

private fun configureMapLibreMap(map: MapLibreMap) {
    map.setMinZoomPreference(MinMapZoom)
    map.setMaxZoomPreference(MaxMapZoom)
    map.uiSettings.apply {
        isCompassEnabled = false
        isLogoEnabled = false
        isAttributionEnabled = false
        isTiltGesturesEnabled = false
    }
}

/**
 * Menyembunyikan seluruh layer bangunan dan menyelaraskan palet vector map OpenFreeMap Fiord
 * ke nuansa dark navy aplikasi tanpa mengubah layer garis rute oranye milik aplikasi.
 */
private fun applyDarkNavyMapTheme(style: Style) {
    val backgroundArgb = MapBackgroundColor.toArgb()
    val labelTextArgb = MapLabelTextColor.toArgb()
    val labelHaloArgb = MapLabelHaloColor.toArgb()

    style.layers.forEach { layer ->
        val rawId = layer.id
        if (rawId in RouteLayerIds) return@forEach

        val id = rawId.lowercase(Locale.ROOT)
        if (id.contains("building")) {
            layer.setProperties(PropertyFactory.visibility(Property.NONE))
            return@forEach
        }

        when (layer) {
            is BackgroundLayer -> {
                layer.setProperties(PropertyFactory.backgroundColor(backgroundArgb))
            }

            is FillLayer -> {
                layer.setProperties(
                    PropertyFactory.fillColor(resolveDarkNavyFillColorArgb(id)),
                )
            }

            is LineLayer -> {
                resolveDarkNavyLineColorArgb(id)?.let { colorArgb ->
                    layer.setProperties(PropertyFactory.lineColor(colorArgb))
                }
            }

            is SymbolLayer -> {
                layer.setProperties(
                    PropertyFactory.textColor(labelTextArgb),
                    PropertyFactory.textHaloColor(labelHaloArgb),
                )
            }
        }
    }
}

internal fun resolveDarkNavyFillColorArgb(layerId: String): Int {
    val id = layerId.lowercase(Locale.ROOT)
    return when {
        id.containsAny("water", "ocean", "lake", "river") -> MapWaterColor.toArgb()
        id.containsAny("park", "grass", "wood", "forest", "green", "pitch", "leisure") ->
            MapParkColor.toArgb()
        id.containsAny("commercial", "industrial", "aeroway") -> MapLandAccentColor.toArgb()
        else -> MapLandColor.toArgb()
    }
}

internal fun resolveDarkNavyLineColorArgb(layerId: String): Int? {
    if (layerId in RouteLayerIds) return null
    val id = layerId.lowercase(Locale.ROOT)
    return when {
        id.containsAny("water", "waterway") -> MapWaterColor.toArgb()
        id.containsAny("casing", "outline") -> MapRoadCasingColor.toArgb()
        id.containsAny("motorway", "trunk", "primary", "secondary", "major") ->
            MapMajorRoadColor.toArgb()
        id.containsAny("boundary", "rail") -> MapBoundaryRailColor.toArgb()
        id.containsAny(
            "road",
            "street",
            "tertiary",
            "minor",
            "service",
            "path",
            "track",
            "bridge",
            "tunnel",
            "highway",
        ) -> MapMinorRoadColor.toArgb()
        else -> null
    }
}

private fun String.containsAny(vararg tokens: String): Boolean =
    tokens.any { token -> contains(token) }

private fun ensureRouteLayers(style: Style) {
    if (style.getSource(RouteSourceId) == null) {
        style.addSource(GeoJsonSource(RouteSourceId))
    }
    if (style.getLayer(RouteCasingLayerId) == null) {
        style.addLayer(
            LineLayer(RouteCasingLayerId, RouteSourceId).withProperties(
                PropertyFactory.lineColor(KuyNavy.copy(alpha = 0.85f).toArgb()),
                PropertyFactory.lineWidth(9f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.visibility(Property.NONE),
            ),
        )
    }
    if (style.getLayer(RouteSolidLayerId) == null) {
        style.addLayer(
            LineLayer(RouteSolidLayerId, RouteSourceId).withProperties(
                PropertyFactory.lineColor(KuyOrange.toArgb()),
                PropertyFactory.lineWidth(5.5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.visibility(Property.NONE),
            ),
        )
    }
    if (style.getLayer(RouteDashedLayerId) == null) {
        style.addLayer(
            LineLayer(RouteDashedLayerId, RouteSourceId).withProperties(
                PropertyFactory.lineColor(KuyOrange.toArgb()),
                PropertyFactory.lineWidth(5.5f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineDasharray(arrayOf(2.4f, 1.6f)),
                PropertyFactory.visibility(Property.NONE),
            ),
        )
    }
}

private fun updateRouteOnStyle(
    style: Style,
    routeInfo: RouteInfo?,
    showRoute: Boolean,
) {
    if (!style.isFullyLoaded) return
    val source = style.getSourceAs<GeoJsonSource>(RouteSourceId) ?: return
    val casingLayer = style.getLayer(RouteCasingLayerId)
    val solidLayer = style.getLayer(RouteSolidLayerId)
    val dashedLayer = style.getLayer(RouteDashedLayerId)

    if (showRoute && routeInfo != null && routeInfo.points.size >= 2) {
        val linePoints = routeInfo.points.map { Point.fromLngLat(it.longitude, it.latitude) }
        source.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(linePoints)))
        casingLayer?.setProperties(PropertyFactory.visibility(Property.VISIBLE))
        if (routeInfo.isFallback) {
            solidLayer?.setProperties(PropertyFactory.visibility(Property.NONE))
            dashedLayer?.setProperties(PropertyFactory.visibility(Property.VISIBLE))
        } else {
            solidLayer?.setProperties(PropertyFactory.visibility(Property.VISIBLE))
            dashedLayer?.setProperties(PropertyFactory.visibility(Property.NONE))
        }
    } else {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
        casingLayer?.setProperties(PropertyFactory.visibility(Property.NONE))
        solidLayer?.setProperties(PropertyFactory.visibility(Property.NONE))
        dashedLayer?.setProperties(PropertyFactory.visibility(Property.NONE))
    }
}

private fun GeoCoordinate.toLatLng(): LatLng = LatLng(latitude, longitude)

private fun Context.findLifecycleOwner(): LifecycleOwner? {
    var current: Context? = this
    while (current != null) {
        if (current is LifecycleOwner) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}

@Preview(widthDp = 360, heightDp = 480)
@Composable
private fun VenueMapViewPreview() {
    VenueMapView(
        venues = DefaultUsuVenues,
        userLocation = DefaultUsuCoordinate,
        selectedVenue = DefaultUsuVenues.first(),
        routeInfo = null,
        mode = VenueFinderMode.DETAIL,
        onVenueClick = {},
        onVenueDetailClick = {},
        userName = "Andi",
    )
}
