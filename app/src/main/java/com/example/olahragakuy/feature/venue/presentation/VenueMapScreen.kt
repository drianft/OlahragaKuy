package com.example.olahragakuy.feature.venue.presentation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.olahragakuy.feature.venue.data.FakeVenueRepository
import com.example.olahragakuy.feature.venue.domain.Venue
import com.example.olahragakuy.feature.venue.domain.VenueRepository
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.core.maps.MapConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import org.maplibre.android.MapLibre
import org.maplibre.android.RenderingEngine
import org.maplibre.android.WellKnownTileServer
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import android.graphics.RectF
import com.example.olahragakuy.feature.venue.presentation.components.VenueDetailSheet
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.rememberUpdatedState
import com.example.olahragakuy.feature.venue.domain.SportType
import com.example.olahragakuy.feature.venue.presentation.components.VenueFilterBar
import android.location.Location
import com.example.olahragakuy.feature.venue.presentation.components.MyLocationButton
import org.maplibre.android.camera.CameraUpdateFactory
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import com.example.olahragakuy.core.location.DistanceCalculator
import com.example.olahragakuy.feature.venue.presentation.components.VenueRadiusFilter
import com.example.olahragakuy.feature.venue.presentation.components.VenueSearchField

@Composable
fun VenueMapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val uriHandler = LocalUriHandler.current

    val repository: VenueRepository = remember {
        FakeVenueRepository()
    }

    var venues by remember {
        mutableStateOf<List<Venue>>(emptyList())
    }

    var loadedStyle by remember {
        mutableStateOf<Style?>(null)
    }

    var selectedVenue by remember {
        mutableStateOf<Venue?>(null)
    }

    var userLocation by remember {
        mutableStateOf<Location?>(null)
    }

    var selectedSport by remember {
        mutableStateOf<SportType?>(null)
    }

    var selectedRadiusKm by remember {
        mutableStateOf<Int?>(null)
    }

    var searchQuery by remember {
        mutableStateOf("")
    }

    val filteredVenues = remember(
        venues,
        selectedSport,
        selectedRadiusKm,
        userLocation,
        searchQuery
    ) {
        val sport = selectedSport
        val radiusKm = selectedRadiusKm
        val location = userLocation
        val query = searchQuery.trim()

        venues.filter { venue ->
            val matchesName =
                query.isBlank() ||
                        venue.name.contains(
                            other = query,
                            ignoreCase = true
                        )

            val matchesSport =
                sport == null || sport in venue.sportTypes

            val matchesRadius = when {
                radiusKm == null -> true
                location == null -> false
                else -> {
                    val distanceMeters = DistanceCalculator.metersBetween(
                        startLatitude = location.latitude,
                        startLongitude = location.longitude,
                        endLatitude = venue.latitude,
                        endLongitude = venue.longitude
                    )

                    distanceMeters <= radiusKm * 1000f
                }
            }

            venue.isActive &&
                    matchesName &&
                    matchesSport &&
                    matchesRadius
        }
    }

    // Listener peta selalu membaca hasil filter terbaru.
    val currentFilteredVenues by rememberUpdatedState(filteredVenues)

    LaunchedEffect(repository) {
        venues = repository.getVenues()
    }

    val mapView = remember(context) {
        // Sesuai dependency android-sdk-opengl yang kita pasang.
        MapLibre.getInstance(
            context.applicationContext,
            null,
            WellKnownTileServer.MapLibre,
            RenderingEngine.Type.OPENGL
        )

        MapConfiguration.configureNetwork(context)

        MapView(context).apply {
            onCreate(null)

            getMapAsync { map ->
                map.setPrefetchZoomDelta(0)

                map.setStyle(
                    Style.Builder()
                        .fromJson(MapConfiguration.osmStyleJson)
                ) { style ->
                    loadedStyle = style
                }

                map.cameraPosition = CameraPosition.Builder()
                    .target(LatLng(3.5825, 98.6670))
                    .zoom(13.0)
                    .build()

                map.addOnMapClickListener click@{ coordinate ->
                    // Abaikan ketukan sebelum layer marker tersedia.
                    if (
                        map.style?.getLayer(
                            VenueMarkerRenderer.LAYER_ID
                        ) == null
                    ) {
                        return@click false
                    }

                    val screenPoint = map.projection.toScreenLocation(
                        coordinate
                    )

                    // Beri toleransi agar marker mudah ditekan.
                    val tolerance = 24f * context.resources
                        .displayMetrics.density

                    val touchArea = RectF(
                        screenPoint.x - tolerance,
                        screenPoint.y - tolerance,
                        screenPoint.x + tolerance,
                        screenPoint.y + tolerance
                    )

                    val features = map.queryRenderedFeatures(
                        touchArea,
                        VenueMarkerRenderer.LAYER_ID
                    )

                    val venueId = features
                        .firstOrNull()
                        ?.getStringProperty("venueId")

                    val clickedVenue = currentFilteredVenues.firstOrNull {
                        it.id == venueId
                    }

                    if (clickedVenue != null) {
                        selectedVenue = clickedVenue
                        true
                    } else {
                        false
                    }
                }
            }
        }
    }

    LaunchedEffect(loadedStyle, filteredVenues) {
        val style = loadedStyle ?: return@LaunchedEffect

        VenueMarkerRenderer.render(
            style = style,
            venues = filteredVenues
        )
    }

    ManageMapLifecycle(mapView, lifecycle)

    selectedVenue?.let { venue ->
        VenueDetailSheet(
            venue = venue,
            onDismiss = {
                selectedVenue = null
            }
        )
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.matchParentSize()
        )

        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            VenueSearchField(
                query = searchQuery,
                onQueryChange = { newQuery ->
                    selectedVenue = null
                    searchQuery = newQuery
                },
                modifier = Modifier.fillMaxWidth()
            )

            VenueFilterBar(
                selectedSport = selectedSport,
                resultCount = filteredVenues.size,
                onSportSelected = { sport ->
                    selectedVenue = null
                    selectedSport = sport
                },
                modifier = Modifier.fillMaxWidth()
            )

            VenueRadiusFilter(
                selectedRadiusKm = selectedRadiusKm,
                hasLocation = userLocation != null,
                onRadiusSelected = { radius ->
                    selectedVenue = null
                    selectedRadiusKm = radius
                },
                modifier = Modifier.fillMaxWidth()
            )
        }

        MyLocationButton(
            onLocationFound = { location ->
                userLocation = location

                mapView.getMapAsync { map ->
                    map.animateCamera(
                        CameraUpdateFactory.newLatLngZoom(
                            LatLng(
                                location.latitude,
                                location.longitude
                            ),
                            15.0
                        )
                    )
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(
                    end = 16.dp,
                    bottom = 48.dp
                )
        )

        Text(
            text = "© OpenStreetMap contributors",
            color = Color.Black,
            fontSize = 11.sp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .background(Color.White.copy(alpha = 0.95f))
                .clickable {
                    uriHandler.openUri(
                        "https://www.openstreetmap.org/copyright"
                    )
                }
                .padding(horizontal = 6.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun ManageMapLifecycle(
    mapView: MapView,
    lifecycle: Lifecycle
) {
    DisposableEffect(mapView, lifecycle) {
        var started = false
        var resumed = false
        var destroyed = false

        fun releaseMap() {
            if (!destroyed) {
                if (resumed) {
                    mapView.onPause()
                    resumed = false
                }
                if (started) {
                    mapView.onStop()
                    started = false
                }
                mapView.onDestroy()
                destroyed = true
            }
        }

        val observer = object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (!destroyed && !started) {
                    mapView.onStart()
                    started = true
                }
            }

            override fun onResume(owner: LifecycleOwner) {
                if (!destroyed && !resumed) {
                    mapView.onResume()
                    resumed = true
                }
            }

            override fun onPause(owner: LifecycleOwner) {
                if (!destroyed && resumed) {
                    mapView.onPause()
                    resumed = false
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                if (!destroyed && started) {
                    mapView.onStop()
                    started = false
                }
            }

            override fun onDestroy(owner: LifecycleOwner) {
                releaseMap()
            }
        }

        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            releaseMap()
        }
    }
}