package com.example.olahragakuy.pages.steptracker

const val DEFAULT_DAILY_STEP_TARGET = 6_000
const val MIN_DAILY_STEP_TARGET = 500
const val MAX_DAILY_STEP_TARGET = 100_000

const val GPS_FALLBACK_STEP_LENGTH_METERS = 0.75
const val CALORIES_PER_STEP = 0.04
const val CALORIES_PER_KM = 60.0

const val MAX_GPS_ACCURACY_METERS = 25.0f
const val MIN_GPS_DISPLACEMENT_METERS = 3.0
const val MAX_REASONABLE_SPEED_MPS = 12.0
const val MIN_DISTANCE_FOR_PACE_METERS = 20.0

const val MAX_STORED_STEP_SESSIONS = 100
const val MAX_STORED_ROUTE_POINTS = 400

/**
 * Representasi satu titik koordinat jejak rute beserta waktu perekamannya.
 */
data class StepPoint(
    val lat: Double,
    val lng: Double,
    val timestampMillis: Long = 0L,
)

/** Koordinat fallback default di kawasan Universitas Sumatera Utara (USU), Medan. */
val DefaultStepTrackerCoordinate = StepPoint(
    lat = 3.5625,
    lng = 98.6567,
    timestampMillis = 0L,
)

/** Status sesi pelacakan langkah saat ini. */
enum class StepTrackingStatus {
    IDLE,
    TRACKING,
    PAUSED,
}

/** Sumber perhitungan langkah yang sedang digunakan pada sesi. */
enum class StepSourceMode {
    PEDOMETER,
    GPS_FALLBACK,
}

/** Gaya tampilan kartu gambar PNG saat membagikan aktivitas ala Strava. */
enum class ShareCardStyle {
    DARK_NAVY,
    TRANSPARENT_STICKER,
}

/**
 * Rekaman satu sesi pelacakan langkah yang tersimpan lokal pada perangkat.
 */
data class StepSessionRecord(
    val id: String,
    val startedAtMillis: Long,
    val endedAtMillis: Long,
    val durationSeconds: Long,
    val steps: Int,
    val distanceMeters: Double,
    val paceSecPerKm: Double?,
    val caloriesKcal: Int,
    val routePoints: List<StepPoint>,
    val stepSource: StepSourceMode,
)

/**
 * Progres total langkah harian yang otomatis di-reset pada pergantian tanggal (`00:00`).
 */
data class DailyStepProgress(
    val dateKey: String,
    val totalSteps: Int = 0,
    val targetSteps: Int = DEFAULT_DAILY_STEP_TARGET,
)

/**
 * State live yang disinkronkan antara mesin pelacak (`StepTrackerService` / fallback foreground)
 * dengan layar UI Compose `StepTrackerScreen`.
 */
data class StepTrackerLiveState(
    val status: StepTrackingStatus = StepTrackingStatus.IDLE,
    val startedAtMillis: Long = 0L,
    val durationSeconds: Long = 0L,
    val steps: Int = 0,
    val distanceMeters: Double = 0.0,
    val paceSecPerKm: Double? = null,
    val caloriesKcal: Int = 0,
    val routePoints: List<StepPoint> = emptyList(),
    val currentLocation: StepPoint? = null,
    val stepSource: StepSourceMode = StepSourceMode.PEDOMETER,
    val isServiceRunning: Boolean = false,
    val isForegroundFallbackMode: Boolean = false,
    val lastCompletedSession: StepSessionRecord? = null,
)

/**
 * State murni akumulator sensor `Sensor.TYPE_STEP_COUNTER` (termasuk penanganan jeda & reboot).
 */
data class StepCounterState(
    val committedSteps: Int = 0,
    val currentSegmentSteps: Int = 0,
    val segmentBaseline: Float? = null,
    val lastRawCounter: Float? = null,
    val isPaused: Boolean = false,
) {
    val totalSteps: Int
        get() = (committedSteps + currentSegmentSteps).coerceAtLeast(0)
}

/**
 * Sampel lokasi mentah dari GPS sebelum disaring oleh filter Anti-GPS Drift.
 */
data class GpsSample(
    val lat: Double,
    val lng: Double,
    val accuracyMeters: Float,
    val timestampMillis: Long,
)

/** Alasan sebuah sampel GPS ditolak oleh filter Anti-GPS Drift. */
enum class GpsFilterRejectReason {
    PAUSED,
    INVALID_COORDINATE,
    POOR_ACCURACY,
    TOO_SMALL_DISPLACEMENT,
    UNREALISTIC_SPEED,
}

/** Hasil evaluasi filter Anti-GPS Drift untuk satu sampel lokasi. */
data class GpsFilterDecision(
    val accepted: Boolean,
    val addedDistanceMeters: Double = 0.0,
    val rejectReason: GpsFilterRejectReason? = null,
    val point: StepPoint? = null,
)

/**
 * State murni perekaman titik GPS valid, jarak kumulatif, serta status jeda/lanjut.
 */
data class GpsTrackState(
    val acceptedPoints: List<StepPoint> = emptyList(),
    val lastAcceptedPoint: StepPoint? = null,
    val totalDistanceMeters: Double = 0.0,
    val isPaused: Boolean = false,
    val awaitingResumeAnchor: Boolean = false,
)

/**
 * Koordinat 2D ter-normalisasi (`0f..1f`) untuk menggambar jalur rute pada preview & kartu share PNG.
 */
data class NormalizedRoutePoint(
    val x: Float,
    val y: Float,
)
