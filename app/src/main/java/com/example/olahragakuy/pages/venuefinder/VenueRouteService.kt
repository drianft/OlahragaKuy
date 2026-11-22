package com.example.olahragakuy.pages.venuefinder

import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val OSRM_BASE_URL = "https://router.project-osrm.org/route/v1"
private const val ROUTE_CONNECT_TIMEOUT_MS = 5_000
private const val ROUTE_READ_TIMEOUT_MS = 6_000

/**
 * Mengambil rute (polyline + jarak + estimasi waktu) dari posisi user [from] ke venue [to]
 * menggunakan OSRM public API di [Dispatchers.IO].
 *
 * - Mode perjalanan dipilih otomatis: jalan kaki (< 1,5 km, durasi dihitung lokal ~4,5 km/jam)
 *   atau berkendara (>= 1,5 km).
 * - Jika offline, timeout, atau server mengembalikan error, otomatis fallback ke [buildFallbackRoute].
 */
suspend fun fetchVenueRoute(
    from: GeoCoordinate,
    to: GeoCoordinate,
): RouteInfo = withContext(Dispatchers.IO) {
    val straightLineMeters = haversineDistanceMeters(from, to)
    val initialProfile = if (selectTravelMode(straightLineMeters) == TravelMode.WALKING) {
        "foot"
    } else {
        "driving"
    }

    val urlString = buildOsrmRouteUrl(
        from = from,
        to = to,
        profile = initialProfile,
    )

    var connection: HttpURLConnection? = null
    try {
        connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = ROUTE_CONNECT_TIMEOUT_MS
            readTimeout = ROUTE_READ_TIMEOUT_MS
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "OlahragaKuy-Android/1.0")
        }

        if (connection.responseCode !in 200..299) {
            return@withContext buildFallbackRoute(from, to)
        }

        val responseText = connection.inputStream.bufferedReader().use { it.readText() }
        parseOsrmRouteJson(responseText, from, to) ?: buildFallbackRoute(from, to)
    } catch (_: Exception) {
        buildFallbackRoute(from, to)
    } finally {
        connection?.disconnect()
    }
}

/**
 * Membentuk URL request OSRM public routing dengan format GeoJSON penuh.
 */
fun buildOsrmRouteUrl(
    from: GeoCoordinate,
    to: GeoCoordinate,
    profile: String = "foot",
): String {
    val fromLon = String.format(Locale.US, "%.6f", from.longitude)
    val fromLat = String.format(Locale.US, "%.6f", from.latitude)
    val toLon = String.format(Locale.US, "%.6f", to.longitude)
    val toLat = String.format(Locale.US, "%.6f", to.latitude)
    return "$OSRM_BASE_URL/$profile/$fromLon,$fromLat;$toLon,$toLat?overview=full&geometries=geojson"
}

/**
 * Mem-parse respons JSON OSRM menjadi [RouteInfo].
 *
 * Durasi untuk jarak < 1,5 km selalu dihitung secara lokal (~4,5 km/jam) agar estimasi
 * jalan kaki tetap realistis meskipun server publik OSRM mengembalikan durasi mobil.
 */
fun parseOsrmRouteJson(
    jsonText: String,
    from: GeoCoordinate,
    to: GeoCoordinate,
): RouteInfo? {
    return try {
        val root = JSONObject(jsonText)
        val routes = root.optJSONArray("routes") ?: return null
        if (routes.length() == 0) return null

        val firstRoute = routes.getJSONObject(0)
        val rawDistance = firstRoute.optDouble("distance", Double.NaN)
        val rawDurationSeconds = firstRoute.optDouble("duration", Double.NaN)
        val geometry = firstRoute.optJSONObject("geometry")
        val coordinatesArray = geometry?.optJSONArray("coordinates")

        val points = mutableListOf<GeoCoordinate>()
        if (coordinatesArray != null) {
            for (index in 0 until coordinatesArray.length()) {
                val pair = coordinatesArray.optJSONArray(index) ?: continue
                if (pair.length() < 2) continue
                val lon = pair.optDouble(0, Double.NaN)
                val lat = pair.optDouble(1, Double.NaN)
                if (lat.isFinite() && lon.isFinite()) {
                    points.add(GeoCoordinate(latitude = lat, longitude = lon))
                }
            }
        }

        if (points.size < 2) return null

        val distanceMeters = if (rawDistance.isFinite() && rawDistance >= 0.0) {
            rawDistance
        } else {
            haversineDistanceMeters(from, to)
        }
        val travelMode = selectTravelMode(distanceMeters)
        val osrmDurationOrNull = rawDurationSeconds.takeIf { it.isFinite() && it > 0.0 }
        val durationMinutes = estimateDurationMinutes(
            distanceMeters = distanceMeters,
            travelMode = travelMode,
            osrmDurationSeconds = osrmDurationOrNull,
        )

        RouteInfo(
            points = points,
            distanceMeters = distanceMeters,
            durationMinutes = durationMinutes,
            travelMode = travelMode,
            isFallback = false,
        )
    } catch (_: Exception) {
        null
    }
}
