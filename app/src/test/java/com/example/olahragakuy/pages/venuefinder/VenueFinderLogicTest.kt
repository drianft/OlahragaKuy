package com.example.olahragakuy.pages.venuefinder

import com.example.olahragakuy.pages.posts.CHAT_COLLECTION
import com.example.olahragakuy.pages.posts.CHAT_MESSAGES_SUBCOLLECTION
import com.example.olahragakuy.pages.posts.ChatMessageUiModel
import com.example.olahragakuy.pages.posts.ChatRoomUiModel
import com.example.olahragakuy.pages.posts.FollowingFriend
import com.example.olahragakuy.pages.posts.LocalChatMessages
import com.example.olahragakuy.pages.posts.LocalChatRooms
import com.example.olahragakuy.pages.posts.VenueInviteUiModel
import com.example.olahragakuy.pages.posts.buildChatMessageFirestorePayload
import com.example.olahragakuy.pages.posts.buildChatRoomFirestorePayload
import com.example.olahragakuy.pages.posts.buildVenueInvitePayload
import com.example.olahragakuy.pages.posts.canonicalChatRoomId
import com.example.olahragakuy.pages.posts.directChatRoomId
import com.example.olahragakuy.pages.posts.isValidChatUserUid
import com.example.olahragakuy.pages.posts.mergeChatMessages
import com.example.olahragakuy.pages.posts.parseVenueInvitePayload
import com.example.olahragakuy.pages.posts.sanitizeChatParticipantIds
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

    // -------------------------------------------------------------------------
    // Sinkronisasi ChatScreen <-> VenueChatBridge <-> VenueRepository
    // -------------------------------------------------------------------------

    @Test
    fun chatAndVenueBridgePayloads_alignWithCanonicalSchemaAndFirestoreRules() {
        assertEquals("chats", CHAT_COLLECTION)
        assertEquals("messages", CHAT_MESSAGES_SUBCOLLECTION)

        val allowedRoomKeys = setOf(
            "name", "isGroup", "statusOrMembers", "lastMessage",
            "dateLabel", "participantIds", "participantNames", "updatedAt",
        )
        val allowedMessageKeys = setOf(
            "senderId", "senderName", "text", "timeText", "venueInvite", "createdAt",
        )

        val roomPayload = buildChatRoomFirestorePayload(
            name = "Monster Basket",
            isGroup = false,
            statusOrMembers = "Aktif",
            lastMessage = "Anda mengirim ajakan olahraga bersama",
            dateLabel = "23/11",
            participantIds = listOf("uid_alice", "uid_bob", "dm_uid_alice_uid_bob", "group_gym", "author_x", "me"),
            participantNames = listOf("Anda", "Alice", "Monster Basket"),
            updatedAt = "SERVER_TS",
        )
        assertTrue(roomPayload.keys.all { it in allowedRoomKeys })
        assertEquals(listOf("uid_alice", "uid_bob"), roomPayload["participantIds"])
        assertEquals(listOf("Alice", "Monster Basket"), roomPayload["participantNames"])

        val plainMsgPayload = buildChatMessageFirestorePayload(
            senderId = "uid_alice",
            senderName = "Alice",
            text = "Halo!",
            timeText = "10:15",
            createdAt = "SERVER_TS",
        )
        assertEquals(
            setOf("senderId", "senderName", "text", "timeText", "createdAt"),
            plainMsgPayload.keys,
        )
        assertFalse("venueInvite" in plainMsgPayload)

        val venue = DefaultUsuVenues.first { it.id == "venue_gor_cikal_usu" }
        val expectedInviteMap = buildVenueInvitePayload(
            venueId = venue.id,
            title = venue.name,
            sport = venue.sportCategory,
            description = buildShortDescription(venue.shortDescription, venue.description),
            imageUrl = venue.imageUrl,
            latitude = venue.latitude,
            longitude = venue.longitude,
        )
        val chatScreenInvitePayload = buildChatMessageFirestorePayload(
            senderId = "uid_alice",
            senderName = "Alice",
            text = buildVenueInviteFallbackText(venue),
            timeText = "10:20",
            venueInvite = expectedInviteMap,
            createdAt = "SERVER_TS",
        )
        val bridgeInvitePayload = buildVenueInviteFirestorePayload(
            senderId = "uid_alice",
            senderName = "Alice",
            fallbackText = buildVenueInviteFallbackText(venue),
            timeText = "10:20",
            venue = venue,
            createdAt = "SERVER_TS",
        )

        assertEquals(chatScreenInvitePayload, bridgeInvitePayload)
        assertEquals(allowedMessageKeys, bridgeInvitePayload.keys)

        val parsedInvite = parseVenueInvitePayload(bridgeInvitePayload["venueInvite"])
        assertNotNull(parsedInvite)
        assertEquals(venue.name, parsedInvite?.title)
        assertEquals(venue.sportCategory, parsedInvite?.sport)
        assertEquals(
            buildShortDescription(venue.shortDescription, venue.description),
            parsedInvite?.description,
        )
    }

    @Test
    fun uidValidationAndCanonicalRoomId_rejectFakeIdsAndNormalizeDirectRooms() {
        assertTrue(isValidChatUserUid("uid_alice_123"))
        assertTrue(isValidChatUserUid("user_monster_basket"))

        assertFalse(isValidChatUserUid(null))
        assertFalse(isValidChatUserUid(""))
        assertFalse(isValidChatUserUid("   "))
        assertFalse(isValidChatUserUid("me"))
        assertFalse(isValidChatUserUid("anon"))
        assertFalse(isValidChatUserUid("dm_uidA_uidB"))
        assertFalse(isValidChatUserUid("group_gymclub"))
        assertFalse(isValidChatUserUid("author_monster"))
        assertFalse(isValidChatUserUid("Monster Basket"))
        assertFalse(isValidChatUserUid("users/uid_alice"))

        assertEquals(
            listOf("uid_alice", "uid_bob"),
            sanitizeChatParticipantIds(
                listOf("uid_alice", "dm_uid_alice_uid_bob", "group_1", "author_bob", "me", "uid_bob", "uid_alice"),
            ),
        )

        assertEquals(
            directChatRoomId("uid_alice", "uid_bob"),
            directChatRoomId("uid_bob", "uid_alice"),
        )
        assertEquals(
            "dm_uid_alice_uid_bob",
            canonicalChatRoomId(
                isGroup = false,
                currentUserId = "uid_bob",
                targetId = "uid_alice",
                existingRoomId = "dm_legacy_id",
            ),
        )
        assertEquals(
            "group_gymclub",
            canonicalChatRoomId(
                isGroup = true,
                currentUserId = "uid_bob",
                targetId = "group_gymclub",
                existingRoomId = "group_gymclub",
            ),
        )
    }

    @Test
    fun mergeShareRecipients_usesCanonicalUserUidsAndNeverFallsBackToRoomOrAuthorIds() {
        val currentUid = "uid_me_100"
        val canonicalUsers = listOf(
            FollowingFriend(uid = "uid_monster_200", displayName = "Monster Basket", photoUrl = "https://img/m.png"),
            FollowingFriend(uid = "uid_citra_300", displayName = "Citra"),
        )
        val friendsFromMixedSources = listOf(
            FollowingFriend(uid = "author_budi", displayName = "Budi"),
            FollowingFriend(uid = "author_monster", displayName = "Monster Basket"),
            FollowingFriend(uid = "dm_corrupt_room", displayName = "Room Palsu"),
        )
        val chatRooms = listOf(
            // Room DM lama tanpa participantIds -> harus teresolusi ke UID kanonis dari koleksi `users`
            ChatRoomUiModel(
                id = "dm_monster",
                name = "Monster Basket",
                isGroup = false,
                statusOrMembers = "Aktif",
                lastMessage = "Halo",
                dateLabel = "23/11",
                participantIds = listOf("dm_monster"),
                participantNames = listOf("Monster Basket"),
            ),
            // Room DM tanpa UID valid di mana pun -> wajib dibuang, jangan fallback ke room.id/nama
            ChatRoomUiModel(
                id = "dm_tanpa_uid",
                name = "Akun Hantu",
                isGroup = false,
                statusOrMembers = "Aktif",
                lastMessage = "Tes",
                dateLabel = "23/11",
                participantIds = emptyList(),
                participantNames = listOf("Akun Hantu"),
            ),
            // Room grup -> tetap dipertahankan dengan ID dokumen grup
            ChatRoomUiModel(
                id = "group_gymclub",
                name = "GymClub",
                isGroup = true,
                statusOrMembers = "Anda,Monster Basket",
                lastMessage = "Yuk gym",
                dateLabel = "23/11",
                participantIds = listOf(currentUid, "uid_monster_200", "group_gymclub"),
                participantNames = listOf("Saya", "Monster Basket"),
            ),
        )

        val recipients = mergeShareRecipients(
            chatRooms = chatRooms,
            friends = friendsFromMixedSources,
            currentUserId = currentUid,
            currentUserName = "Saya",
            canonicalUsers = canonicalUsers,
        )

        val monster = recipients.firstOrNull { !it.isGroup && it.name == "Monster Basket" }
        assertNotNull(monster)
        requireNotNull(monster)
        assertEquals("uid_monster_200", monster.id)
        assertEquals(directChatRoomId(currentUid, "uid_monster_200"), monster.roomId)
        assertEquals(listOf(currentUid, "uid_monster_200"), monster.participantIds)

        val citra = recipients.firstOrNull { !it.isGroup && it.name == "Citra" }
        assertNotNull(citra)
        requireNotNull(citra)
        assertEquals("uid_citra_300", citra.id)
        assertEquals(directChatRoomId(currentUid, "uid_citra_300"), citra.roomId)

        val group = recipients.firstOrNull { it.isGroup && it.name == "GymClub" }
        assertNotNull(group)
        requireNotNull(group)
        assertEquals("group_gymclub", group.id)
        assertEquals("group_gymclub", group.roomId)
        assertEquals(listOf(currentUid, "uid_monster_200"), group.participantIds)

        assertFalse(recipients.any { it.name == "Budi" || it.id.startsWith("author_") })
        assertFalse(recipients.any { it.name == "Akun Hantu" || it.id == "dm_tanpa_uid" })
        assertFalse(recipients.any { it.name == "Room Palsu" })
    }

    @Test
    fun sendVenueInvite_normalizesDirectRoomIdAndSanitizesParticipantIds() {
        val venue = DefaultUsuVenues.first { it.id == "venue_gor_cikal_usu" }
        val recipient = ShareRecipient(
            id = "uid_bob_99",
            name = "Bob",
            isGroup = false,
            roomId = "dm_legacy_wrong",
            participantIds = listOf("dm_legacy_wrong", "group_noise", "uid_bob_99"),
            participantNames = listOf("Bob"),
        )
        val expectedRoomId = directChatRoomId("uid_alice_11", "uid_bob_99")

        try {
            val target = VenueChatBridge.sendVenueInvite(
                venue = venue,
                recipient = recipient,
                currentUserId = "uid_alice_11",
                currentUserName = "Alice",
                syncToFirestore = false,
                nowMillis = 1_700_000_000_000L,
            )

            assertEquals(expectedRoomId, target.roomId)
            assertEquals("uid_bob_99", target.recipientId)
            assertEquals(listOf("uid_alice_11", "uid_bob_99"), target.room.participantIds)
            assertTrue(target.room.participantIds.none { it.startsWith("dm_") || it.startsWith("group_") })

            val savedMsg = LocalChatMessages[expectedRoomId]?.firstOrNull { it.id == target.messageId }
            assertNotNull(savedMsg)
            assertEquals(venue.name, savedMsg?.venueInvite?.title)
            assertEquals("uid_alice_11", savedMsg?.senderId)
        } finally {
            LocalChatRooms.removeAll { it.id == expectedRoomId }
            LocalChatMessages.remove(expectedRoomId)
            VenueChatBridge.consumePendingTarget()
        }
    }

    @Test
    fun mergeChatMessages_preservesVenueInviteAcrossLocalAndRemoteSync() {
        val invite = VenueInviteUiModel(
            title = "GOR Cikal USU",
            sport = "Basket",
            description = "Lapangan basket kampus USU",
        )
        val localMessages = listOf(
            ChatMessageUiModel(
                id = "venue_invite_1",
                senderId = "uid_alice",
                senderName = "Alice",
                text = "Yuk olahraga bareng di GOR Cikal USU (Basket)!",
                timeText = "10:20",
                isFromMe = true,
                venueInvite = invite,
                createdAtMillis = 2_000L,
            ),
            ChatMessageUiModel(
                id = "local_only_2",
                senderId = "uid_alice",
                senderName = "Alice",
                text = "Aku otw ya",
                timeText = "10:21",
                isFromMe = true,
                createdAtMillis = 3_000L,
            ),
        )
        val remoteMessages = listOf(
            ChatMessageUiModel(
                id = "remote_older_0",
                senderId = "uid_bob",
                senderName = "Bob",
                text = "Mau main di mana?",
                timeText = "10:19",
                isFromMe = false,
                createdAtMillis = 1_000L,
            ),
            // Simulasi snapshot remote dengan ID sama namun field venueInvite null
            ChatMessageUiModel(
                id = "venue_invite_1",
                senderId = "uid_alice",
                senderName = "Alice",
                text = "Yuk olahraga bareng di GOR Cikal USU (Basket)!",
                timeText = "10:20",
                isFromMe = true,
                venueInvite = null,
                createdAtMillis = 2_050L,
            ),
        )

        val merged = mergeChatMessages(
            localMessages = localMessages,
            remoteMessages = remoteMessages,
        )

        assertEquals(listOf("remote_older_0", "venue_invite_1", "local_only_2"), merged.map { it.id })
        val mergedInviteMsg = merged.first { it.id == "venue_invite_1" }
        assertEquals(invite, mergedInviteMsg.venueInvite)
        assertEquals(2_050L, mergedInviteMsg.createdAtMillis)
        assertTrue(mergedInviteMsg.isFromMe)
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
