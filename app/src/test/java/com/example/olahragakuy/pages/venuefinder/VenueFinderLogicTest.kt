package com.example.olahragakuy.pages.venuefinder

import com.google.firebase.firestore.GeoPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VenueFinderLogicTest {

    // -------------------------------------------------------------------------
    // Haversine & Resolusi Koordinat Pengguna
    // -------------------------------------------------------------------------

    @Test
    fun haversineDistanceMeters_sameCoordinate_returnsZero() {
        val distance = haversineDistanceMeters(DefaultUsuCoordinate, DefaultUsuCoordinate)

        assertEquals(0.0, distance, 0.001)
    }

    @Test
    fun haversineDistanceMeters_oneDegreeLatitude_matchesExpectedMeters() {
        val distance = haversineDistanceMeters(
            from = GeoCoordinate(0.0, 0.0),
            to = GeoCoordinate(1.0, 0.0),
        )

        assertEquals(111_195.0, distance, 200.0)
    }

    @Test
    fun haversineDistanceMeters_isSymmetricAndRealisticForUsuVenues() {
        val gorCikal = DefaultUsuVenues.first { it.id == "venue_gor_cikal_usu" }.coordinate
        val lapanganMerdeka = DefaultUsuVenues.first { it.id == "venue_lapangan_merdeka" }.coordinate

        val ab = haversineDistanceMeters(DefaultUsuCoordinate, gorCikal)
        val ba = haversineDistanceMeters(gorCikal, DefaultUsuCoordinate)
        val toMerdeka = haversineDistanceMeters(DefaultUsuCoordinate, lapanganMerdeka)

        assertEquals(ab, ba, 0.0001)
        assertTrue("Jarak USU ke GOR Cikal harus < 1,5 km (actual=$ab)", ab in 150.0..500.0)
        assertTrue("Jarak USU ke Lapangan Merdeka harus >= 1,5 km (actual=$toMerdeka)", toMerdeka > 3_000.0)
    }

    @Test
    fun resolveUserCoordinate_validCoordinate_returnsOriginalWithoutFallback() {
        val gps = GeoCoordinate(3.5710, 98.6612)

        val (resolved, isFallback) = resolveUserCoordinate(gps)

        assertEquals(gps, resolved)
        assertFalse(isFallback)
    }

    @Test
    fun resolveUserCoordinate_nullZeroOrInvalid_fallsBackToDefaultUsu() {
        val invalidInputs = listOf(
            null,
            GeoCoordinate(0.0, 0.0),
            GeoCoordinate(95.0, 98.6),
            GeoCoordinate(3.5, 200.0),
            GeoCoordinate(Double.NaN, 98.6),
            GeoCoordinate(3.5, Double.POSITIVE_INFINITY),
        )

        invalidInputs.forEach { input ->
            val (resolved, isFallback) = resolveUserCoordinate(input)
            assertEquals(DefaultUsuCoordinate, resolved)
            assertTrue("Input $input harus fallback ke USU", isFallback)
        }
    }

    // -------------------------------------------------------------------------
    // Mode Rute Otomatis (< 1,5 km Jalan Kaki ~4,5 km/jam vs >= 1,5 km Berkendara)
    // -------------------------------------------------------------------------

    @Test
    fun selectTravelMode_belowThreshold_returnsWalking_andAtOrAboveReturnsDriving() {
        assertEquals(TravelMode.WALKING, selectTravelMode(0.0))
        assertEquals(TravelMode.WALKING, selectTravelMode(750.0))
        assertEquals(TravelMode.WALKING, selectTravelMode(1_499.9))
        assertEquals(TravelMode.DRIVING, selectTravelMode(WALKING_THRESHOLD_METERS))
        assertEquals(TravelMode.DRIVING, selectTravelMode(3_800.0))
    }

    @Test
    fun estimateDurationMinutes_zeroOrNegativeDistance_returnsZero() {
        assertEquals(0, estimateDurationMinutes(0.0, TravelMode.WALKING))
        assertEquals(0, estimateDurationMinutes(-50.0, TravelMode.DRIVING, osrmDurationSeconds = 120.0))
    }

    @Test
    fun estimateDurationMinutes_walking_alwaysUsesLocalWalkingSpeedAndIgnoresOsrm() {
        // 4,5 km/jam = 75 meter/menit -> 750m = 10 menit, 300m = 4 menit
        assertEquals(
            10,
            estimateDurationMinutes(
                distanceMeters = 750.0,
                travelMode = TravelMode.WALKING,
                osrmDurationSeconds = 60.0, // durasi mobil OSRM harus diabaikan
            ),
        )
        assertEquals(4, estimateDurationMinutes(distanceMeters = 300.0, travelMode = TravelMode.WALKING))
        assertEquals(1, estimateDurationMinutes(distanceMeters = 10.0, travelMode = TravelMode.WALKING))
    }

    @Test
    fun estimateDurationMinutes_driving_prefersValidOsrmAndFallsBackToLocalSpeed() {
        assertEquals(
            10,
            estimateDurationMinutes(
                distanceMeters = 4_000.0,
                travelMode = TravelMode.DRIVING,
                osrmDurationSeconds = 600.0,
            ),
        )
        assertEquals(
            2,
            estimateDurationMinutes(
                distanceMeters = 1_600.0,
                travelMode = TravelMode.DRIVING,
                osrmDurationSeconds = 61.0,
            ),
        )
        // Fallback lokal 30 km/jam = 500 meter/menit -> 2.500m = 5 menit
        assertEquals(
            5,
            estimateDurationMinutes(
                distanceMeters = 2_500.0,
                travelMode = TravelMode.DRIVING,
                osrmDurationSeconds = null,
            ),
        )
        assertEquals(
            5,
            estimateDurationMinutes(
                distanceMeters = 2_500.0,
                travelMode = TravelMode.DRIVING,
                osrmDurationSeconds = 0.0,
            ),
        )
        assertEquals(
            5,
            estimateDurationMinutes(
                distanceMeters = 2_500.0,
                travelMode = TravelMode.DRIVING,
                osrmDurationSeconds = Double.NaN,
            ),
        )
    }

    @Test
    fun buildFallbackRoute_selectsWalkingForShortAndDrivingForLongDistance() {
        val gorCikal = DefaultUsuVenues.first { it.id == "venue_gor_cikal_usu" }.coordinate
        val lapanganMerdeka = DefaultUsuVenues.first { it.id == "venue_lapangan_merdeka" }.coordinate

        val shortRoute = buildFallbackRoute(DefaultUsuCoordinate, gorCikal)
        assertTrue(shortRoute.isFallback)
        assertEquals(listOf(DefaultUsuCoordinate, gorCikal), shortRoute.points)
        assertEquals(TravelMode.WALKING, shortRoute.travelMode)
        assertEquals(
            estimateDurationMinutes(shortRoute.distanceMeters, TravelMode.WALKING),
            shortRoute.durationMinutes,
        )

        val longRoute = buildFallbackRoute(DefaultUsuCoordinate, lapanganMerdeka)
        assertTrue(longRoute.isFallback)
        assertEquals(listOf(DefaultUsuCoordinate, lapanganMerdeka), longRoute.points)
        assertEquals(TravelMode.DRIVING, longRoute.travelMode)
        assertEquals(
            estimateDurationMinutes(longRoute.distanceMeters, TravelMode.DRIVING),
            longRoute.durationMinutes,
        )
    }

    // -------------------------------------------------------------------------
    // Format Jarak, Durasi, Share Link, & Deskripsi Singkat
    // -------------------------------------------------------------------------

    @Test
    fun formatDistance_formatsMetersWholeKmAndDecimalKmWithComma() {
        assertEquals("0m", formatDistance(-15.0))
        assertEquals("0m", formatDistance(0.0))
        assertEquals("250m", formatDistance(250.4))
        assertEquals("999m", formatDistance(999.0))
        assertEquals("1 km", formatDistance(1_000.0))
        assertEquals("1,5 km", formatDistance(1_500.0))
        assertEquals("2 km", formatDistance(2_000.0))
        assertEquals("3,9 km", formatDistance(3_920.0))
    }

    @Test
    fun formatDuration_formatsMinutesHoursAndMixed() {
        assertEquals("0 menit", formatDuration(-5))
        assertEquals("0 menit", formatDuration(0))
        assertEquals("4 menit", formatDuration(4))
        assertEquals("59 menit", formatDuration(59))
        assertEquals("1 jam", formatDuration(60))
        assertEquals("1 jam 15 menit", formatDuration(75))
        assertEquals("2 jam", formatDuration(120))
        assertEquals("2 jam 5 menit", formatDuration(125))
    }

    @Test
    fun buildVenueShareLink_usesDotDecimalCoordinatesInOsmUrl() {
        val venue = DefaultUsuVenues.first { it.id == "venue_gor_cikal_usu" }

        val link = buildVenueShareLink(venue)

        assertEquals(
            "https://www.openstreetmap.org/?mlat=3.564600&mlon=98.656200#map=17/3.564600/98.656200",
            link,
        )
    }

    @Test
    fun buildShortDescription_prefersExplicitAndTruncatesLongFallbackCleanly() {
        assertEquals(
            "Lapangan basket kampus",
            buildShortDescription("  Lapangan basket kampus  ", "Deskripsi panjang lainnya"),
        )

        assertEquals(
            "Deskripsi ringkas apa adanya",
            buildShortDescription("   ", "Deskripsi   ringkas   apa adanya", maxLength = 60),
        )

        val longDesc = "GOR Cikal USU adalah lapangan basket terbuka yang luas untuk latihan pagi dan sore bersama komunitas mahasiswa Medan."
        val shortened = buildShortDescription(null, longDesc, maxLength = 50)
        assertTrue("Harus diakhiri elipsis: $shortened", shortened.endsWith("..."))
        assertTrue("Panjang sebelum elipsis harus <= 50: $shortened", shortened.length <= 53)
        assertFalse("Tidak boleh terpotong di tengah kata dengan spasi gantung", shortened.endsWith(" ..."))
    }

    // -------------------------------------------------------------------------
    // Filter Pencarian & Status isActive
    // -------------------------------------------------------------------------

    @Test
    fun filterActiveVenues_excludesInactiveVenues() {
        val active = sampleVenue(id = "v1", name = "GOR Aktif", isActive = true)
        val inactive = sampleVenue(id = "v2", name = "GOR Nonaktif", isActive = false)

        val result = filterActiveVenues(listOf(active, inactive))

        assertEquals(listOf(active), result)
    }

    @Test
    fun filterVenues_matchesAcrossFieldsMultiTokenAndIgnoresInactive() {
        val venues = listOf(
            sampleVenue(
                id = "v1",
                name = "GOR Cikal USU",
                sportCategory = "Basket",
                address = "Jl. Universitas, Padang Bulan",
                shortDescription = "Lapangan mahasiswa USU",
                isActive = true,
            ),
            sampleVenue(
                id = "v2",
                name = "BolaKuy Arena",
                sportCategory = "Sepak Bola",
                address = "Jl. Dr. Mansyur",
                shortDescription = "Mini soccer ramah pemula",
                isActive = true,
            ),
            sampleVenue(
                id = "v3",
                name = "GOR Basket Lama",
                sportCategory = "Basket",
                address = "Jl. Universitas",
                shortDescription = "Sedang renovasi",
                isActive = false,
            ),
        )

        // Query kosong -> semua venue aktif
        assertEquals(listOf("v1", "v2"), filterVenues(venues, "   ").map { it.id })
        // Cocok berdasarkan kategori, tetap membuang yang isActive = false
        assertEquals(listOf("v1"), filterVenues(venues, "basket").map { it.id })
        // Cocok multi-token lintas nama & alamat
        assertEquals(listOf("v1"), filterVenues(venues, "cikal padang").map { it.id })
        // Cocok berdasarkan deskripsi singkat
        assertEquals(listOf("v2"), filterVenues(venues, "ramah pemula").map { it.id })
        // Query cocok hanya dengan venue nonaktif -> hasil kosong
        assertTrue(filterVenues(venues, "renovasi").isEmpty())
    }

    // -------------------------------------------------------------------------
    // Parser Firestore Defensif (Double vs GeoPoint, Fallback & Field Opsional)
    // -------------------------------------------------------------------------

    @Test
    fun parseVenueFromMap_parsesDoubleCoordinatesAndOptionalDefaults() {
        val parsed = parseVenueFromMap(
            id = " venue_1 ",
            data = mapOf(
                "name" to "  Lapangan Basket USU  ",
                "latitude" to 3.5646,
                "longitude" to 98.6562,
                "description" to "Lapangan basket terbuka di tengah kampus USU.",
            ),
        )

        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals("venue_1", parsed.id)
        assertEquals("Lapangan Basket USU", parsed.name)
        assertEquals("Olahraga", parsed.sportCategory)
        assertEquals("Lapangan basket terbuka di tengah kampus USU.", parsed.shortDescription)
        assertEquals("Lapangan basket terbuka di tengah kampus USU.", parsed.description)
        assertEquals("", parsed.imageUrl)
        assertEquals("", parsed.address)
        assertEquals("", parsed.createdBy)
        assertTrue(parsed.isActive)
        assertEquals(3.5646, parsed.latitude, 0.00001)
        assertEquals(98.6562, parsed.longitude, 0.00001)
    }

    @Test
    fun parseVenueFromMap_parsesGeoPointAndFallbackShortDescriptionWhenDescriptionsEmpty() {
        val parsed = parseVenueFromMap(
            id = "venue_gp",
            data = mapOf(
                "title" to "Stadion Mini USU",
                "sport" to "Lari",
                "location" to GeoPoint(3.5594, 98.6542),
                "isActive" to false,
                "alamat" to "Jl. Almamater",
                "photoUrl" to "https://example.com/stadion.jpg",
                "createdBy" to "admin_1",
            ),
        )

        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals("Stadion Mini USU", parsed.name)
        assertEquals("Lari", parsed.sportCategory)
        assertEquals("Stadion Mini USU - Lari", parsed.shortDescription)
        assertEquals("Stadion Mini USU - Lari", parsed.description)
        assertEquals(3.5594, parsed.latitude, 0.00001)
        assertEquals(98.6542, parsed.longitude, 0.00001)
        assertEquals("Jl. Almamater", parsed.address)
        assertEquals("https://example.com/stadion.jpg", parsed.imageUrl)
        assertEquals("admin_1", parsed.createdBy)
        assertFalse(parsed.isActive)
    }

    @Test
    fun parseVenueFromMap_supportsNestedCoordinateMapAndStringNumbers() {
        val parsed = parseVenueFromMap(
            id = "venue_nested",
            data = mapOf(
                "name" to "Taman Cadika",
                "category" to "Jogging",
                "coordinate" to mapOf("lat" to "3,5342", "lng" to "98,6635"),
                "isActive" to "false",
            ),
        )

        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals(3.5342, parsed.latitude, 0.00001)
        assertEquals(98.6635, parsed.longitude, 0.00001)
        assertFalse(parsed.isActive)
    }

    @Test
    fun parseVenueFromMap_returnsNullOnBlankIdMissingNameOrInvalidCoordinates() {
        assertNull(
            parseVenueFromMap(
                id = "   ",
                data = mapOf("name" to "GOR", "latitude" to 3.5, "longitude" to 98.6),
            ),
        )
        assertNull(
            parseVenueFromMap(
                id = "v_no_name",
                data = mapOf("latitude" to 3.5, "longitude" to 98.6),
            ),
        )
        assertNull(
            parseVenueFromMap(
                id = "v_no_coord",
                data = mapOf("name" to "GOR Tanpa Koordinat"),
            ),
        )
        assertNull(
            parseVenueFromMap(
                id = "v_bad_coord",
                data = mapOf("name" to "GOR Koordinat Salah", "latitude" to 120.0, "longitude" to 98.6),
            ),
        )
    }

    @Test
    fun parseVenueInviteMap_parsesValidMapAndSafelyReturnsNullWhenMissingOrMalformed() {
        val parsed = parseVenueInviteMap(
            mapOf(
                "title" to "  GOR Cikal USU ",
                "sport" to "Basket",
                "description" to "Lapangan basket mahasiswa USU",
            ),
        )
        assertNotNull(parsed)
        requireNotNull(parsed)
        assertEquals("GOR Cikal USU", parsed.title)
        assertEquals("Basket", parsed.sport)
        assertEquals("Lapangan basket mahasiswa USU", parsed.description)

        val defaultSport = parseVenueInviteMap(mapOf("title" to "BolaKuy"))
        assertNotNull(defaultSport)
        assertEquals("Olahraga", defaultSport?.sport)
        assertEquals("", defaultSport?.description)

        assertNull(parseVenueInviteMap(null))
        assertNull(parseVenueInviteMap("bukan map"))
        assertNull(parseVenueInviteMap(emptyMap<String, Any>()))
        assertNull(parseVenueInviteMap(mapOf("title" to "   ", "sport" to "Basket")))
    }

    private fun sampleVenue(
        id: String,
        name: String,
        sportCategory: String = "Olahraga",
        address: String = "",
        shortDescription: String = "",
        isActive: Boolean = true,
    ): VenueUiModel = VenueUiModel(
        id = id,
        name = name,
        sportCategory = sportCategory,
        shortDescription = shortDescription,
        description = shortDescription,
        imageUrl = "",
        latitude = 3.5625,
        longitude = 98.6567,
        address = address,
        isActive = isActive,
    )
}
