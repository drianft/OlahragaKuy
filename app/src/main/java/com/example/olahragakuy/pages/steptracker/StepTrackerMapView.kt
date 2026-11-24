package com.example.olahragakuy.pages.steptracker

import android.content.ComponentCallbacks2
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.view.View
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import com.example.olahragakuy.ui.components.KuyUserAvatar
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyNavy
import com.example.olahragakuy.ui.theme.KuyOrange
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import org.maplibre.android.MapLibre
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
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

private const val StepOpenFreeMapFiordUri = "https://tiles.openfreemap.org/styles/fiord"

private const val StepRouteSourceId = "step-tracker-route-source"
private const val StepRouteCasingLayerId = "step-tracker-route-casing-layer"
private const val StepRouteSolidLayerId = "step-tracker-route-solid-layer"

private val StepRouteLayerIds = setOf(
    StepRouteCasingLayerId,
    StepRouteSolidLayerId,
)

// Palet dark navy yang konsisten dengan VenueMapView.
private val StepMapBackgroundColor = Color(0xFF142133)
private val StepMapWaterColor = Color(0xFF0B1524)
private val StepMapParkColor = Color(0xFF142A36)
private val StepMapLandColor = Color(0xFF17263A)
private val StepMapLandAccentColor = Color(0xFF1A2B42)
private val StepMapRoadCasingColor = Color(0xFF0E1928)
private val StepMapMajorRoadColor = Color(0xFF314B6E)
private val StepMapMinorRoadColor = Color(0xFF233752)
private val StepMapBoundaryRailColor = Color(0xFF263B57)
private val StepMapLabelTextColor = Color(0xFF8EA6C2)
private val StepMapLabelHaloColor = Color(0xFF0E1A2A)

private const val StepDefaultMapZoom = 16.2
private const val StepRecenterZoom = 16.8
private const val StepMinMapZoom = 4.0
private const val StepMaxMapZoom = 19.0
private const val StepFocusAnimationMs = 600
private const val StepFollowAnimationMs = 450

/**
 * Peta vector MapLibre full-screen bergaya *dark navy* (OpenFreeMap Fiord tanpa layer `building`)
 * khusus fitur Pelacak Langkah.
 *
 * Menampilkan pin lokasi pengguna ([KuyUserAvatar] berbingkai oranye), jejak rute oranye live
 * ([LineLayer]), dukungan *follow user* yang tidak memaksa *snap-back* saat user menggeser peta
 * secara manual, serta placeholder aman saat `LocalInspectionMode.current` (`@Preview`).
 */
@Composable
fun StepTrackerMapView(
    userLocation: StepPoint?,
    routePoints: List<StepPoint>,
    followUser: Boolean,
    recenterSignal: Int,
    modifier: Modifier = Modifier,
    userName: String = "Kamu",
    userPhotoUrl: String? = null,
    onUserPanDetected: () -> Unit = {},
    onUserLocationClick: () -> Unit = {},
) {
    val displayLocation = userLocation ?: DefaultStepTrackerCoordinate

    if (LocalInspectionMode.current) {
        StepMapInspectionPlaceholder(
            hasRoute = routePoints.size >= 2,
            userName = userName,
            userPhotoUrl = userPhotoUrl,
            modifier = modifier,
        )
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = remember(context) { context.findStepMapLifecycleOwner() }
    val latestOnUserPanDetected by rememberUpdatedState(onUserPanDetected)

    val initialCenter = remember { displayLocation }
    val mapView = remember(context) {
        MapLibre.getInstance(context)
        createStepMapLibreMapView(
            context = context,
            initialCenter = initialCenter,
            initialZoom = StepDefaultMapZoom,
        )
    }

    var mapLibreMap by remember(mapView) { mutableStateOf<MapLibreMap?>(null) }
    var loadedStyle by remember(mapView) { mutableStateOf<Style?>(null) }
    var isManuallyPanned by remember(mapView) { mutableStateOf(false) }
    var hasAutoCenteredInitialFix by remember(mapView) { mutableStateOf(false) }
    val cameraTick = remember(mapView) { mutableIntStateOf(0) }
    val readCameraTick = remember(cameraTick) { { cameraTick.intValue } }

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

        val cameraMoveStartedListener = MapLibreMap.OnCameraMoveStartedListener { reason ->
            if (reason == MapLibreMap.OnCameraMoveStartedListener.REASON_API_GESTURE) {
                isManuallyPanned = true
                latestOnUserPanDetected()
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
        moveToCreated()

        mapView.getMapAsync { map ->
            if (isDestroyed) return@getMapAsync
            configureStepMapLibreMap(map)
            map.addOnCameraMoveStartedListener(cameraMoveStartedListener)
            map.addOnCameraMoveListener(cameraMoveListener)
            map.addOnCameraIdleListener(cameraIdleListener)
            mapLibreMap = map
            cameraTick.intValue++

            map.setStyle(Style.Builder().fromUri(StepOpenFreeMapFiordUri)) { style ->
                if (isDestroyed) return@setStyle
                applyStepDarkNavyMapTheme(style)
                ensureStepRouteLayers(style)
                loadedStyle = style
                cameraTick.intValue++
            }
        }

        onDispose {
            mapView.removeOnLayoutChangeListener(layoutChangeListener)
            mapLibreMap?.let { map ->
                map.removeOnCameraMoveStartedListener(cameraMoveStartedListener)
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

    // Perbarui GeoJSON polyline jejak rute oranye setiap kali titik rute bertambah/berubah.
    LaunchedEffect(loadedStyle, routePoints) {
        val style = loadedStyle ?: return@LaunchedEffect
        updateStepRouteOnStyle(style = style, routePoints = routePoints)
    }

    // Pusatkan kamera sekali saat fix GPS pertama diterima, atau ikuti user secara halus saat followUser aktif.
    LaunchedEffect(mapLibreMap, userLocation, followUser) {
        val map = mapLibreMap ?: return@LaunchedEffect
        val target = userLocation ?: return@LaunchedEffect
        if (!isValidCoordinate(target.lat, target.lng)) return@LaunchedEffect

        if (!hasAutoCenteredInitialFix && !isManuallyPanned) {
            hasAutoCenteredInitialFix = true
            mapView.runWhenStepMapLaidOut {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(target.toLatLng(), StepRecenterZoom),
                    StepFocusAnimationMs,
                )
            }
        } else if (followUser && !isManuallyPanned) {
            mapView.runWhenStepMapLaidOut {
                map.animateCamera(
                    CameraUpdateFactory.newLatLng(target.toLatLng()),
                    StepFollowAnimationMs,
                )
            }
        }
    }

    // Fokus ulang ke posisi user ketika tombol My Location ditekan.
    val latestDisplayLocation by rememberUpdatedState(displayLocation)
    val lastHandledRecenterSignal = remember(mapView) { mutableIntStateOf(0) }
    LaunchedEffect(mapLibreMap, recenterSignal) {
        val map = mapLibreMap ?: return@LaunchedEffect
        if (recenterSignal > 0 && recenterSignal != lastHandledRecenterSignal.intValue) {
            lastHandledRecenterSignal.intValue = recenterSignal
            isManuallyPanned = false
            hasAutoCenteredInitialFix = true
            val target = latestDisplayLocation
            mapView.runWhenStepMapLaidOut {
                map.animateCamera(
                    CameraUpdateFactory.newLatLngZoom(target.toLatLng(), StepRecenterZoom),
                    StepFocusAnimationMs,
                )
            }
        }
    }

    Box(modifier.background(HomeNavy).clipToBounds()) {
        AndroidView(factory = { mapView }, modifier = Modifier.fillMaxSize())

        StepUserLocationPin(
            userName = userName,
            userPhotoUrl = userPhotoUrl,
            onClick = onUserLocationClick,
            modifier = Modifier.anchoredToStepGeo(
                mapView = mapView,
                mapLibreMap = mapLibreMap,
                coordinate = displayLocation,
                cameraTick = readCameraTick,
            ),
        )

        StepMapAttribution(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 6.dp, end = 6.dp),
        )
    }
}

@Composable
private fun StepUserLocationPin(
    userName: String,
    userPhotoUrl: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .semantics { contentDescription = "Lokasi kamu saat ini" }
            .clickable(onClickLabel = "Pusatkan ke lokasi kamu", onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .shadow(8.dp, CircleShape)
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
        StepPinPointer(color = HomeOrange)
    }
}

@Composable
private fun StepPinPointer(color: Color) {
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
private fun StepMapAttribution(modifier: Modifier = Modifier) {
    Text(
        text = "© OpenFreeMap • © OpenStreetMap contributors",
        color = HomeText.copy(alpha = 0.72f),
        fontSize = 9.sp,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(HomeNavy.copy(alpha = 0.75f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

@Composable
private fun StepMapInspectionPlaceholder(
    hasRoute: Boolean,
    userName: String,
    userPhotoUrl: String?,
    modifier: Modifier = Modifier,
) {
    Box(modifier.background(StepMapBackgroundColor).clipToBounds()) {
        Canvas(Modifier.fillMaxSize()) {
            val step = 48.dp.toPx()
            val gridColor = HomeBlue.copy(alpha = 0.32f)
            var x = 0f
            while (x < size.width) {
                drawLine(gridColor, Offset(x, 0f), Offset(x, size.height), 1.dp.toPx())
                x += step
            }
            var y = 0f
            while (y < size.height) {
                drawLine(gridColor, Offset(0f, y), Offset(size.width, y), 1.dp.toPx())
                y += step
            }

            if (hasRoute) {
                val samplePath = Path().apply {
                    moveTo(size.width * 0.22f, size.height * 0.62f)
                    lineTo(size.width * 0.36f, size.height * 0.48f)
                    lineTo(size.width * 0.54f, size.height * 0.52f)
                    lineTo(size.width * 0.50f, size.height * 0.42f)
                }
                drawPath(
                    path = samplePath,
                    color = KuyNavy.copy(alpha = 0.85f),
                    style = Stroke(
                        width = 10.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
                drawPath(
                    path = samplePath,
                    color = KuyOrange,
                    style = Stroke(
                        width = 5.5.dp.toPx(),
                        cap = StrokeCap.Round,
                        join = StrokeJoin.Round,
                    ),
                )
            }
        }

        StepUserLocationPin(
            userName = userName,
            userPhotoUrl = userPhotoUrl,
            onClick = {},
            modifier = Modifier.align(Alignment.Center),
        )

        StepMapAttribution(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(6.dp),
        )
    }
}

private fun Modifier.anchoredToStepGeo(
    mapView: MapView,
    mapLibreMap: MapLibreMap?,
    coordinate: StepPoint,
    cameraTick: () -> Int,
): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
    layout(placeable.width, placeable.height) {
        cameraTick()
        val point = mapLibreMap?.stepScreenPointOrNull(mapView, coordinate)
        if (point != null) {
            placeable.place(
                x = point.x - placeable.width / 2,
                y = point.y - placeable.height,
            )
        }
    }
}

private fun MapLibreMap.stepScreenPointOrNull(
    mapView: MapView,
    coordinate: StepPoint,
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

private fun MapView.runWhenStepMapLaidOut(action: () -> Unit) {
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

private fun createStepMapLibreMapView(
    context: Context,
    initialCenter: StepPoint,
    initialZoom: Double,
): MapView {
    val options = MapLibreMapOptions.createFromAttributes(context)
        .foregroundLoadColor(StepMapBackgroundColor.toArgb())
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
        setBackgroundColor(StepMapBackgroundColor.toArgb())
    }
}

private fun configureStepMapLibreMap(map: MapLibreMap) {
    map.setMinZoomPreference(StepMinMapZoom)
    map.setMaxZoomPreference(StepMaxMapZoom)
    map.uiSettings.apply {
        isCompassEnabled = false
        isLogoEnabled = false
        isAttributionEnabled = false
        isTiltGesturesEnabled = false
    }
}

private fun applyStepDarkNavyMapTheme(style: Style) {
    val backgroundArgb = StepMapBackgroundColor.toArgb()
    val labelTextArgb = StepMapLabelTextColor.toArgb()
    val labelHaloArgb = StepMapLabelHaloColor.toArgb()

    style.layers.forEach { layer ->
        val rawId = layer.id
        if (rawId in StepRouteLayerIds) return@forEach

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
                    PropertyFactory.fillColor(resolveStepMapFillColorArgb(id)),
                )
            }

            is LineLayer -> {
                resolveStepMapLineColorArgb(id)?.let { colorArgb ->
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

private fun resolveStepMapFillColorArgb(layerId: String): Int {
    return when {
        layerId.containsAnyToken("water", "ocean", "lake", "river") -> StepMapWaterColor.toArgb()
        layerId.containsAnyToken("park", "grass", "wood", "forest", "green", "pitch", "leisure") ->
            StepMapParkColor.toArgb()
        layerId.containsAnyToken("commercial", "industrial", "aeroway") -> StepMapLandAccentColor.toArgb()
        else -> StepMapLandColor.toArgb()
    }
}

private fun resolveStepMapLineColorArgb(layerId: String): Int? {
    if (layerId in StepRouteLayerIds) return null
    return when {
        layerId.containsAnyToken("water", "waterway") -> StepMapWaterColor.toArgb()
        layerId.containsAnyToken("casing", "outline") -> StepMapRoadCasingColor.toArgb()
        layerId.containsAnyToken("motorway", "trunk", "primary", "secondary", "major") ->
            StepMapMajorRoadColor.toArgb()
        layerId.containsAnyToken("boundary", "rail") -> StepMapBoundaryRailColor.toArgb()
        layerId.containsAnyToken(
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
        ) -> StepMapMinorRoadColor.toArgb()
        else -> null
    }
}

private fun String.containsAnyToken(vararg tokens: String): Boolean =
    tokens.any { token -> contains(token) }

private fun ensureStepRouteLayers(style: Style) {
    if (style.getSource(StepRouteSourceId) == null) {
        style.addSource(GeoJsonSource(StepRouteSourceId))
    }
    if (style.getLayer(StepRouteCasingLayerId) == null) {
        style.addLayer(
            LineLayer(StepRouteCasingLayerId, StepRouteSourceId).withProperties(
                PropertyFactory.lineColor(KuyNavy.copy(alpha = 0.88f).toArgb()),
                PropertyFactory.lineWidth(10f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.visibility(Property.NONE),
            ),
        )
    }
    if (style.getLayer(StepRouteSolidLayerId) == null) {
        style.addLayer(
            LineLayer(StepRouteSolidLayerId, StepRouteSourceId).withProperties(
                PropertyFactory.lineColor(KuyOrange.toArgb()),
                PropertyFactory.lineWidth(6f),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.visibility(Property.NONE),
            ),
        )
    }
}

private fun updateStepRouteOnStyle(
    style: Style,
    routePoints: List<StepPoint>,
) {
    if (!style.isFullyLoaded) return
    val source = style.getSourceAs<GeoJsonSource>(StepRouteSourceId) ?: return
    val casingLayer = style.getLayer(StepRouteCasingLayerId)
    val solidLayer = style.getLayer(StepRouteSolidLayerId)

    val validPoints = routePoints.filter { isValidCoordinate(it.lat, it.lng) }
    if (validPoints.size >= 2) {
        val linePoints = validPoints.map { Point.fromLngLat(it.lng, it.lat) }
        source.setGeoJson(Feature.fromGeometry(LineString.fromLngLats(linePoints)))
        casingLayer?.setProperties(PropertyFactory.visibility(Property.VISIBLE))
        solidLayer?.setProperties(PropertyFactory.visibility(Property.VISIBLE))
    } else {
        source.setGeoJson(FeatureCollection.fromFeatures(emptyArray()))
        casingLayer?.setProperties(PropertyFactory.visibility(Property.NONE))
        solidLayer?.setProperties(PropertyFactory.visibility(Property.NONE))
    }
}

private fun StepPoint.toLatLng(): LatLng = LatLng(lat, lng)

private fun Context.findStepMapLifecycleOwner(): LifecycleOwner? {
    var current: Context? = this
    while (current != null) {
        if (current is LifecycleOwner) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}

@Preview(name = "Peta Pelacak Langkah", widthDp = 360, heightDp = 520)
@Composable
private fun StepTrackerMapViewPreview() {
    OlahragaKuyTheme {
        StepTrackerMapView(
            userLocation = DefaultStepTrackerCoordinate,
            routePoints = listOf(
                StepPoint(3.5618, 98.6558, 1_000L),
                StepPoint(3.5625, 98.6567, 5_000L),
            ),
            followUser = true,
            recenterSignal = 0,
            userName = "Andi",
            modifier = Modifier.fillMaxSize(),
        )
    }
}
