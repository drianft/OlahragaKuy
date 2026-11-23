package com.example.olahragakuy.pages.venuefinder

import android.content.Context
import android.content.ContextWrapper
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.DashPathEffect
import android.graphics.Paint
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Polyline
import java.io.File

private const val DefaultMapZoom = 16.0
private const val FocusVenueZoom = 17.0
private const val MinMapZoom = 5.0
private const val MaxMapZoom = 19.0
private const val FocusAnimationMs = 650L

private val VenueCardWidth = 224.dp
private val MarkerDotSize = 12.dp
private val RouteFocusPadding = 96.dp

/**
 * Peta OSMDroid bergaya gelap untuk layar Venue Finder.
 *
 * Menampilkan pin lokasi pengguna, kartu marker venue yang mengikuti koordinat layar saat
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
    val mapView = remember(context) {
        configureOsmdroid(context)
        createDarkMapView(context, selectedVenue?.coordinate ?: userLocation)
    }
    val cameraTick = remember(mapView) { mutableIntStateOf(0) }
    val readCameraTick = remember(cameraTick) { { cameraTick.intValue } }
    val lifecycleOwner = remember(context) { context.findLifecycleOwner() }

    // Sinkronkan lifecycle MapView tanpa LocalLifecycleOwner.
    DisposableEffect(mapView, lifecycleOwner) {
        mapView.onCameraFrame = { cameraTick.intValue++ }
        val lifecycle = lifecycleOwner?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                else -> Unit
            }
        }
        if (lifecycle != null) lifecycle.addObserver(observer) else mapView.onResume()
        onDispose {
            lifecycle?.removeObserver(observer)
            mapView.onCameraFrame = null
            mapView.onPause()
            mapView.onDetach()
        }
    }

    val showRoute = mode == VenueFinderMode.ROUTE && (routeInfo?.points?.size ?: 0) >= 2
    val routeCasing = remember(mapView) {
        createRoutePolyline(mapView, KuyNavy.copy(alpha = 0.85f), with(density) { 11.dp.toPx() })
    }
    val routeLine = remember(mapView) {
        createRoutePolyline(mapView, KuyOrange, with(density) { 6.dp.toPx() })
    }
    val dashIntervals = remember(density) {
        with(density) { floatArrayOf(12.dp.toPx(), 8.dp.toPx()) }
    }

    LaunchedEffect(mapView, routeInfo, showRoute) {
        mapView.overlays.remove(routeCasing)
        mapView.overlays.remove(routeLine)
        if (showRoute && routeInfo != null) {
            val points = routeInfo.points.map { it.toGeoPoint() }
            routeCasing.setPoints(points)
            routeLine.setPoints(points)
            routeLine.outlinePaint.pathEffect =
                if (routeInfo.isFallback) DashPathEffect(dashIntervals, 0f) else null
            mapView.overlays.add(routeCasing)
            mapView.overlays.add(routeLine)
        }
        mapView.invalidate()
    }

    val hasAutoCenteredOnUser = remember(mapView) { mutableStateOf(false) }

    // Auto-focus kamera ke rute (mode rute) atau ke venue terpilih.
    val routePaddingPx = with(density) { RouteFocusPadding.roundToPx() }
    LaunchedEffect(mapView, showRoute, routeInfo, selectedVenue?.id) {
        if (showRoute || selectedVenue != null) {
            hasAutoCenteredOnUser.value = true
        }
        mapView.runWhenLaidOut {
            when {
                showRoute && routeInfo != null -> {
                    val box = BoundingBox.fromGeoPointsSafe(routeInfo.points.map { it.toGeoPoint() })
                    mapView.zoomToBoundingBox(box, true, routePaddingPx)
                }

                selectedVenue != null -> mapView.controller.animateTo(
                    selectedVenue.coordinate.toGeoPoint(),
                    FocusVenueZoom,
                    FocusAnimationMs,
                )
            }
        }
    }

    // Auto-center ke lokasi user hanya sekali saat pertama buka tab Lokasi tanpa target venue/rute.
    LaunchedEffect(mapView, initialUserCenterCoordinate) {
        val target = initialUserCenterCoordinate ?: return@LaunchedEffect
        if (!hasAutoCenteredOnUser.value && selectedVenue == null && !showRoute) {
            hasAutoCenteredOnUser.value = true
            mapView.runWhenLaidOut { mapView.controller.animateTo(target.toGeoPoint()) }
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
            modifier = Modifier.anchoredToGeo(mapView, userLocation, readCameraTick),
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
                        coordinate = venue.coordinate,
                        cameraTick = readCameraTick,
                        bottomInsetPx = dotInsetPx,
                    ),
                )
            }
        }

        OsmAttribution(Modifier.align(Alignment.BottomEnd))
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
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.semantics { contentDescription = "Lokasi kamu" },
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
private fun OsmAttribution(modifier: Modifier = Modifier) {
    Text(
        "© OpenStreetMap contributors",
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
            "Pratinjau peta OSMDroid",
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
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 120.dp),
        )
        OsmAttribution(Modifier.align(Alignment.BottomEnd))
    }
}

/**
 * MapView yang memberi tahu Compose setiap kali kamera (pusat, zoom, rotasi, atau ukuran)
 * berubah, sehingga overlay Compose bisa mengikuti koordinat layar terbaru.
 */
private class TrackingMapView(context: Context) : MapView(context) {
    var onCameraFrame: (() -> Unit)? = null

    private var lastLatitude = Double.NaN
    private var lastLongitude = Double.NaN
    private var lastZoom = Double.NaN
    private var lastOrientation = Float.NaN
    private var lastWidth = -1
    private var lastHeight = -1

    override fun dispatchDraw(canvas: android.graphics.Canvas) {
        super.dispatchDraw(canvas)
        val center = mapCenter
        val zoom = zoomLevelDouble
        val orientation = mapOrientation
        val changed = center.latitude != lastLatitude ||
            center.longitude != lastLongitude ||
            zoom != lastZoom ||
            orientation != lastOrientation ||
            width != lastWidth ||
            height != lastHeight
        if (changed) {
            lastLatitude = center.latitude
            lastLongitude = center.longitude
            lastZoom = zoom
            lastOrientation = orientation
            lastWidth = width
            lastHeight = height
            onCameraFrame?.invoke()
        }
    }
}

/**
 * Menempatkan composable sehingga titik bawah-tengahnya berada tepat di [coordinate].
 * State kamera dibaca di fase placement agar pan/zoom tidak memicu rekomposisi penuh.
 */
private fun Modifier.anchoredToGeo(
    mapView: MapView,
    coordinate: GeoCoordinate,
    cameraTick: () -> Int,
    bottomInsetPx: Int = 0,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(placeable.width, placeable.height) {
        cameraTick()
        val point = mapView.screenPointOrNull(coordinate)
        if (point != null) {
            placeable.place(
                x = point.x - placeable.width / 2,
                y = point.y - placeable.height + bottomInsetPx,
            )
        }
    }
}

private fun MapView.screenPointOrNull(coordinate: GeoCoordinate): IntOffset? {
    if (width == 0 || height == 0) return null
    val point = projection.toPixels(coordinate.toGeoPoint(), null)
    val margin = maxOf(width, height) / 2
    val isNearViewport = point.x in -margin..(width + margin) &&
        point.y in -margin..(height + margin)
    return if (isNearViewport) IntOffset(point.x, point.y) else null
}

private fun MapView.runWhenLaidOut(action: () -> Unit) {
    if (width > 0 && height > 0) {
        action()
    } else {
        addOnFirstLayoutListener { _, _, _, _, _ -> action() }
    }
}

private const val OsmdroidUserAgent = "OlahragaKuy/1.0 (Android; Medan)"
private const val OsmdroidCacheDirName = "osmdroid_osm_v2"

private fun configureOsmdroid(context: Context) {
    val config = Configuration.getInstance()
    config.userAgentValue = OsmdroidUserAgent
    val basePath = File(context.cacheDir, OsmdroidCacheDirName).apply { mkdirs() }
    config.osmdroidBasePath = basePath
    config.osmdroidTileCache = File(config.osmdroidBasePath, "tiles").apply { mkdirs() }
    config.tileDownloadThreads = 2
}

private fun createDarkMapView(context: Context, center: GeoCoordinate): TrackingMapView =
    TrackingMapView(context).apply {
        setTileSource(TileSourceFactory.MAPNIK)
        setMultiTouchControls(true)
        zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        setTilesScaledToDpi(true)
        setMinZoomLevel(MinMapZoom)
        setMaxZoomLevel(MaxMapZoom)
        setBackgroundColor(HomeNavy.toArgb())
        overlayManager.tilesOverlay.apply {
            setColorFilter(createDarkNavyColorFilter())
            setLoadingBackgroundColor(HomeNavy.toArgb())
            setLoadingLineColor(HomeBlue.toArgb())
        }
        controller.setZoom(DefaultMapZoom)
        controller.setCenter(center.toGeoPoint())
    }

/**
 * Filter warna minimalis bertema dark navy untuk tile standar OpenStreetMap Mapnik.
 * Menenggelamkan warna latar tanah dan blok bangunan pucat ke nuansa dark navy,
 * sambil mempertahankan kontras jalan utama, sungai/taman, dan label nama jalan.
 */
private fun createDarkNavyColorFilter(): ColorMatrixColorFilter {
    // 1. Inversi warna dasar tile terang Mapnik.
    val matrix = ColorMatrix(
        floatArrayOf(
            -1f, 0f, 0f, 0f, 255f,
            0f, -1f, 0f, 0f, 255f,
            0f, 0f, -1f, 0f, 255f,
            0f, 0f, 0f, 1f, 0f,
        ),
    )

    // 2. Rotasi hue 180 derajat agar perairan tetap biru, taman hijau, dan jalan utama hangat.
    matrix.postConcat(
        ColorMatrix(
            floatArrayOf(
                -0.574f, 1.430f, 0.144f, 0f, 0f,
                0.426f, 0.430f, 0.144f, 0f, 0f,
                0.426f, 1.430f, -0.856f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )

    // 3. Redam saturasi agar tampilan peta lebih tenang dan fokus pada jalur utama.
    matrix.postConcat(ColorMatrix().apply { setSaturation(0.68f) })

    // 4. Kurva kontras & offset dark navy: tekan area terang pucat (tanah/bangunan) ke gelap,
    //    tetapi pertahankan keterbacaan garis jalan utama dan label teks.
    matrix.postConcat(
        ColorMatrix(
            floatArrayOf(
                1.32f, 0f, 0f, 0f, -36f,
                0f, 1.34f, 0f, 0f, -14f,
                0f, 0f, 1.36f, 0f, 16f,
                0f, 0f, 0f, 1f, 0f,
            ),
        ),
    )

    return ColorMatrixColorFilter(matrix)
}

private fun createRoutePolyline(mapView: MapView, color: Color, widthPx: Float): Polyline =
    Polyline(mapView).apply {
        outlinePaint.color = color.toArgb()
        outlinePaint.strokeWidth = widthPx
        outlinePaint.strokeCap = Paint.Cap.ROUND
        outlinePaint.strokeJoin = Paint.Join.ROUND
        outlinePaint.isAntiAlias = true
        // Abaikan tap agar info window bawaan polyline tidak muncul.
        setOnClickListener { _, _, _ -> false }
    }

private fun GeoCoordinate.toGeoPoint(): GeoPoint = GeoPoint(latitude, longitude)

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
