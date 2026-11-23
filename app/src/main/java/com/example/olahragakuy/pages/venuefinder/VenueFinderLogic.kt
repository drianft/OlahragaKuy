package com.example.olahragakuy.pages.venuefinder

import com.google.firebase.firestore.GeoPoint
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/** Batas jarak (< 1,5 km) untuk memilih estimasi jalan kaki otomatis. */
const val WALKING_THRESHOLD_METERS = 1_500.0

/** Kecepatan jalan kaki santai (~4,5 km/jam) agar estimasi waktu realistis. */
const val WALKING_SPEED_KMH = 4.5

/** Kecepatan berkendara rata-rata perkotaan (~30 km/jam) saat fallback offline. */
const val DRIVING_SPEED_KMH = 30.0

private const val EARTH_RADIUS_METERS = 6_371_000.0

/**
 * Mengembalikan hanya venue yang aktif (`isActive == true`) untuk ditampilkan di peta user.
 */
fun filterActiveVenues(venues: List<VenueUiModel>): List<VenueUiModel> =
    venues.filter { it.isActive }

/**
 * Memfilter venue aktif berdasarkan kata kunci pencarian pada nama, kategori olahraga,
 * alamat, atau deskripsi singkat.
 */
fun filterVenues(
    venues: List<VenueUiModel>,
    query: String,
): List<VenueUiModel> {
    val activeVenues = filterActiveVenues(venues)
    val tokens = query.trim().lowercase(Locale.ROOT)
        .split(Regex("\\s+"))
        .filter { it.isNotEmpty() }
    if (tokens.isEmpty()) return activeVenues

    return activeVenues.filter { venue ->
        val searchable = buildString {
            append(venue.name).append(' ')
            append(venue.sportCategory).append(' ')
            append(venue.address).append(' ')
            append(venue.shortDescription)
        }.lowercase(Locale.ROOT)
        tokens.all { token -> searchable.contains(token) }
    }
}

/**
 * Memvalidasi koordinat GPS perangkat. Jika null atau di luar rentang geografis valid,
 * otomatis mengembalikan [DefaultUsuCoordinate] dengan penanda fallback `true`.
 */
fun resolveUserCoordinate(gpsCoordinate: GeoCoordinate?): Pair<GeoCoordinate, Boolean> {
    val valid = gpsCoordinate != null &&
        gpsCoordinate.latitude.isFinite() &&
        gpsCoordinate.longitude.isFinite() &&
        gpsCoordinate.latitude in -90.0..90.0 &&
        gpsCoordinate.longitude in -180.0..180.0 &&
        !(gpsCoordinate.latitude == 0.0 && gpsCoordinate.longitude == 0.0)
    return if (valid) {
        gpsCoordinate to false
    } else {
        DefaultUsuCoordinate to true
    }
}

/**
 * Menghitung jarak permukaan bumi (meter) antara dua koordinat menggunakan rumus Haversine.
 */
fun haversineDistanceMeters(
    from: GeoCoordinate,
    to: GeoCoordinate,
): Double {
    val dLat = Math.toRadians(to.latitude - from.latitude)
    val dLon = Math.toRadians(to.longitude - from.longitude)
    val lat1 = Math.toRadians(from.latitude)
    val lat2 = Math.toRadians(to.latitude)

    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(lat1) * cos(lat2) * sin(dLon / 2) * sin(dLon / 2)
    val c = 2 * atan2(sqrt(a), sqrt((1.0 - a).coerceAtLeast(0.0)))
    return EARTH_RADIUS_METERS * c
}

/**
 * Memilih mode perjalanan otomatis: jalan kaki jika jarak < 1,5 km, berkendara jika >= 1,5 km.
 */
fun selectTravelMode(distanceMeters: Double): TravelMode =
    if (distanceMeters < WALKING_THRESHOLD_METERS) TravelMode.WALKING else TravelMode.DRIVING

/**
 * Menghitung estimasi durasi (menit).
 *
 * - Mode [TravelMode.WALKING] selalu dihitung lokal dari jarak rute (~4,5 km/jam)
 *   karena server publik OSRM sering hanya mengembalikan durasi profil mobil.
 * - Mode [TravelMode.DRIVING] memakai durasi OSRM jika tersedia dan valid, atau fallback lokal.
 */
fun estimateDurationMinutes(
    distanceMeters: Double,
    travelMode: TravelMode,
    osrmDurationSeconds: Double? = null,
): Int {
    if (distanceMeters <= 0.0) return 0

    return when (travelMode) {
        TravelMode.WALKING -> {
            val metersPerMinute = (WALKING_SPEED_KMH * 1_000.0) / 60.0
            ceil(distanceMeters / metersPerMinute).toInt().coerceAtLeast(1)
        }
        TravelMode.DRIVING -> {
            if (osrmDurationSeconds != null && osrmDurationSeconds.isFinite() && osrmDurationSeconds > 0.0) {
                ceil(osrmDurationSeconds / 60.0).toInt().coerceAtLeast(1)
            } else {
                val metersPerMinute = (DRIVING_SPEED_KMH * 1_000.0) / 60.0
                ceil(distanceMeters / metersPerMinute).toInt().coerceAtLeast(1)
            }
        }
    }
}

/**
 * Membangun rute fallback offline berbasis jarak Haversine agar mode rute tetap usable
 * saat jaringan tidak tersedia atau server OSRM gagal dijangkau.
 */
fun buildFallbackRoute(
    from: GeoCoordinate,
    to: GeoCoordinate,
): RouteInfo {
    val distance = haversineDistanceMeters(from, to)
    val mode = selectTravelMode(distance)
    val duration = estimateDurationMinutes(distanceMeters = distance, travelMode = mode)
    return RouteInfo(
        points = listOf(from, to),
        distanceMeters = distance,
        durationMinutes = duration,
        travelMode = mode,
        isFallback = true,
    )
}

/**
 * Memformat jarak ke tampilan ringkas berbahasa Indonesia (misal `"250m"`, `"1,5 km"`, `"2 km"`).
 */
fun formatDistance(distanceMeters: Double): String {
    val clamped = distanceMeters.coerceAtLeast(0.0)
    if (clamped < 1_000.0) {
        return "${clamped.roundToInt()}m"
    }
    val km = clamped / 1_000.0
    val roundedTenth = (km * 10.0).roundToInt() / 10.0
    return if (roundedTenth % 1.0 == 0.0) {
        "${roundedTenth.toInt()} km"
    } else {
        "${String.format(Locale.US, "%.1f", roundedTenth).replace('.', ',')} km"
    }
}

/**
 * Memformat durasi menit ke teks yang ramah dibaca (misal `"4 menit"`, `"1 jam 15 menit"`).
 */
fun formatDuration(durationMinutes: Int): String {
    val safeMinutes = durationMinutes.coerceAtLeast(0)
    if (safeMinutes < 60) {
        return "$safeMinutes menit"
    }
    val hours = safeMinutes / 60
    val remaining = safeMinutes % 60
    return if (remaining == 0) {
        "$hours jam"
    } else {
        "$hours jam $remaining menit"
    }
}

/**
 * Membentuk tautan OpenStreetMap yang dapat disalin dan dibagikan untuk lokasi [venue].
 */
fun buildVenueShareLink(venue: VenueUiModel): String {
    val lat = String.format(Locale.US, "%.6f", venue.latitude)
    val lon = String.format(Locale.US, "%.6f", venue.longitude)
    return "https://www.openstreetmap.org/?mlat=$lat&mlon=$lon#map=17/$lat/$lon"
}

/**
 * Menyediakan deskripsi singkat untuk kartu undangan chat. Jika [shortDescription] kosong,
 * otomatis memotong dari [description] secara rapi pada batas kata.
 */
fun buildShortDescription(
    shortDescription: String?,
    description: String,
    maxLength: Int = 90,
): String {
    val explicit = shortDescription?.trim().orEmpty()
    if (explicit.isNotEmpty()) return explicit

    val cleanDesc = description.trim().replace(Regex("\\s+"), " ")
    if (cleanDesc.length <= maxLength) return cleanDesc

    val cut = cleanDesc.take(maxLength)
    val lastSpace = cut.lastIndexOf(' ')
    val base = if (lastSpace >= maxLength / 2) cut.substring(0, lastSpace) else cut
    return "${base.trimEnd('.', ',', ';', ':')}..."
}

/**
 * Parser defensif untuk dokumen Firestore pada koleksi `venues`.
 *
 * Aman menghadapi field opsional kosong, otomatis mengisi `shortDescription` dari `description`,
 * dan mendukung koordinat baik dalam format `latitude`/`longitude` (`Number`/`String`) maupun
 * `GeoPoint` Firestore atau map koordinat bersarang.
 */
fun parseVenueFromMap(
    id: String,
    data: Map<String, Any?>,
): VenueUiModel? {
    val cleanId = id.trim()
    if (cleanId.isEmpty()) return null

    val name = firstNonBlankString(data["name"], data["title"]) ?: return null
    val coordinate = extractCoordinate(data) ?: return null

    val sportCategory = firstNonBlankString(
        data["sportCategory"],
        data["category"],
        data["sport"],
    ) ?: "Olahraga"
    val description = firstNonBlankString(data["description"], data["desc"]).orEmpty()
    val shortDescription = buildShortDescription(
        shortDescription = firstNonBlankString(data["shortDescription"], data["summary"]),
        description = description.ifEmpty { "$name - $sportCategory" },
    )
    val imageUrl = firstNonBlankString(
        data["imageUrl"],
        data["photoUrl"],
        data["image"],
    ).orEmpty()
    val address = firstNonBlankString(data["address"], data["alamat"]).orEmpty()
    val isActive = parseBooleanOrDefault(data["isActive"], defaultValue = true)
    val createdBy = firstNonBlankString(data["createdBy"]).orEmpty()

    return VenueUiModel(
        id = cleanId,
        name = name,
        sportCategory = sportCategory,
        shortDescription = shortDescription,
        description = description.ifEmpty { shortDescription },
        imageUrl = imageUrl,
        latitude = coordinate.latitude,
        longitude = coordinate.longitude,
        address = address,
        isActive = isActive,
        createdBy = createdBy,
    )
}

private fun extractCoordinate(data: Map<String, Any?>): GeoCoordinate? {
    val directLat = parseDouble(data["latitude"] ?: data["lat"])
    val directLon = parseDouble(data["longitude"] ?: data["lng"] ?: data["lon"])
    if (isValidLatLon(directLat, directLon)) {
        return GeoCoordinate(latitude = directLat!!, longitude = directLon!!)
    }

    val nestedCandidates = listOf(data["location"], data["geoPoint"], data["coordinate"], data["coordinates"])
    for (candidate in nestedCandidates) {
        when (candidate) {
            is GeoPoint -> {
                if (isValidLatLon(candidate.latitude, candidate.longitude)) {
                    return GeoCoordinate(candidate.latitude, candidate.longitude)
                }
            }
            is Map<*, *> -> {
                val lat = parseDouble(candidate["latitude"] ?: candidate["lat"])
                val lon = parseDouble(candidate["longitude"] ?: candidate["lng"] ?: candidate["lon"])
                if (isValidLatLon(lat, lon)) {
                    return GeoCoordinate(lat!!, lon!!)
                }
            }
        }
    }
    return null
}

private fun isValidLatLon(lat: Double?, lon: Double?): Boolean =
    lat != null &&
        lon != null &&
        lat.isFinite() &&
        lon.isFinite() &&
        lat in -90.0..90.0 &&
        lon in -180.0..180.0

private fun parseDouble(value: Any?): Double? = when (value) {
    is Number -> value.toDouble()
    is String -> value.trim().replace(',', '.').toDoubleOrNull()
    else -> null
}

private fun parseBooleanOrDefault(value: Any?, defaultValue: Boolean): Boolean = when (value) {
    is Boolean -> value
    is String -> when (value.trim().lowercase(Locale.ROOT)) {
        "true", "1" -> true
        "false", "0" -> false
        else -> defaultValue
    }
    else -> defaultValue
}

private fun firstNonBlankString(vararg values: Any?): String? =
    values.firstNotNullOfOrNull { raw ->
        (raw as? String)?.trim()?.takeIf { it.isNotEmpty() }
    }
