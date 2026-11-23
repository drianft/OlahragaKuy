package com.example.olahragakuy.pages.venuefinder

/**
 * Representasi titik koordinat geografis (lintang & bujur) untuk peta dan perhitungan rute.
 */
data class GeoCoordinate(
    val latitude: Double,
    val longitude: Double,
)

/** Koordinat fallback default di kawasan Universitas Sumatera Utara (USU), Medan. */
val DefaultUsuCoordinate = GeoCoordinate(
    latitude = 3.5625,
    longitude = 98.6567,
)

/**
 * Model data lokasi olahraga (venue) yang ditampilkan di peta, bottom sheet detail,
 * dan kartu undangan obrolan.
 *
 * Field [isActive] dan [createdBy] disiapkan agar kompatibel dengan fitur moderasi Admin.
 */
data class VenueUiModel(
    val id: String,
    val name: String,
    val sportCategory: String,
    val shortDescription: String,
    val description: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double,
    val address: String = "",
    val isActive: Boolean = true,
    val createdBy: String = "",
) {
    val coordinate: GeoCoordinate
        get() = GeoCoordinate(latitude = latitude, longitude = longitude)
}

/** Mode perjalanan otomatis berdasarkan jarak dari pengguna ke venue tujuan. */
enum class TravelMode {
    WALKING,
    DRIVING,
}

/**
 * Hasil rute dari posisi pengguna menuju venue tujuan, berisi daftar titik polyline,
 * estimasi jarak (meter), dan estimasi waktu tempuh (menit).
 */
data class RouteInfo(
    val points: List<GeoCoordinate>,
    val distanceMeters: Double,
    val durationMinutes: Int,
    val travelMode: TravelMode,
    val isFallback: Boolean = false,
)

/**
 * Target penerima (teman DM atau grup komunitas) pada bottom sheet "Bagikan ke:".
 */
data class ShareRecipient(
    val id: String,
    val name: String,
    val isGroup: Boolean,
    val roomId: String? = null,
    val avatarUrl: String? = null,
    val avatarColorHex: Long = 0xFF5B7CFAL,
    val participantIds: List<String> = emptyList(),
    val participantNames: List<String> = emptyList(),
)

/** Keempat tampilan overlay utama pada layar Venue Finder (State 1 sampai State 4). */
enum class VenueFinderMode {
    DEFAULT,
    DETAIL,
    ROUTE,
    SHARE,
}

/** State menyeluruh layar Venue Finder agar alur antar state tetap konsisten. */
data class VenueFinderUiState(
    val venues: List<VenueUiModel> = emptyList(),
    val searchQuery: String = "",
    val mode: VenueFinderMode = VenueFinderMode.DEFAULT,
    val selectedVenue: VenueUiModel? = null,
    val userLocation: GeoCoordinate = DefaultUsuCoordinate,
    val isUsingFallbackLocation: Boolean = true,
    val routeInfo: RouteInfo? = null,
    val isLoadingRoute: Boolean = false,
    val isLoadingVenues: Boolean = false,
    val shareRecipients: List<ShareRecipient> = emptyList(),
)

/**
 * Data venue bawaan di area USU & Medan sebagai fallback read-only saat koleksi
 * Firestore `venues` kosong atau perangkat sedang offline.
 */
val DefaultUsuVenues: List<VenueUiModel> = listOf(
    VenueUiModel(
        id = "venue_gor_cikal_usu",
        name = "GOR Cikal USU",
        sportCategory = "Basket",
        shortDescription = "Lapangan Basket mahasiswa Universitas Sumatera Utara",
        description = "GOR Cikal USU adalah salah satu lapangan basket favorit mahasiswa " +
            "dan komunitas olahraga di kawasan Universitas Sumatera Utara. Dengan area " +
            "yang luas dan lingkungan terbuka, tempat ini cocok untuk latihan santai, " +
            "sparring, maupun pertandingan antar tim.",
        imageUrl = "https://images.unsplash.com/photo-1546519638-68e109498ffc?auto=format&fit=crop&w=800&q=80",
        latitude = 3.5646,
        longitude = 98.6562,
        address = "Jl. Universitas, Padang Bulan, Kec. Medan Baru, Kota Medan",
    ),
    VenueUiModel(
        id = "venue_bolakuy_usu",
        name = "BolaKuy",
        sportCategory = "Sepak Bola",
        shortDescription = "Lapangan sepak bola dan mini soccer ramah pemula di sekitar USU",
        description = "BolaKuy menyediakan lapangan sepak bola rumput rapi dengan pencahayaan " +
            "nyaman untuk sesi latihan pagi maupun sore bersama teman dan komunitas lokal.",
        imageUrl = "https://images.unsplash.com/photo-1575361204480-aadea25e6e68?auto=format&fit=crop&w=800&q=80",
        latitude = 3.5605,
        longitude = 98.6584,
        address = "Jl. Dr. Mansyur, Padang Bulan Selayang I, Kota Medan",
    ),
    VenueUiModel(
        id = "venue_stadion_mini_usu",
        name = "Stadion Mini USU",
        sportCategory = "Lari & Atletik",
        shortDescription = "Lintasan lari terbuka di kompleks kampus Universitas Sumatera Utara",
        description = "Stadion Mini USU memiliki lintasan lari terbuka yang cocok untuk jogging " +
            "ringan, jalan cepat, dan pemanasan pemula dengan suasana kampus yang asri.",
        imageUrl = "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=800&q=80",
        latitude = 3.5594,
        longitude = 98.6542,
        address = "Jl. Almamater, Kampus USU, Kota Medan",
    ),
    VenueUiModel(
        id = "venue_lapangan_merdeka",
        name = "Lapangan Merdeka",
        sportCategory = "Jogging",
        shortDescription = "Ruang terbuka publik populer untuk jogging santai di pusat Kota Medan",
        description = "Lapangan Merdeka Medan menjadi titik kumpul favorit warga untuk lari pagi, " +
            "senam santai, dan aktivitas kebugaran ringan bersama komunitas.",
        imageUrl = "https://images.unsplash.com/photo-1552674605-db6ffd4facb5?auto=format&fit=crop&w=800&q=80",
        latitude = 3.5912,
        longitude = 98.6776,
        address = "Jl. Pulau Pinang, Kesawan, Kec. Medan Barat, Kota Medan",
    ),
)
