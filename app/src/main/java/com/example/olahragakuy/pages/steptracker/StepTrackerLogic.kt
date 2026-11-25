package com.example.olahragakuy.pages.steptracker

import java.time.LocalDate
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.math.sqrt

private const val EARTH_RADIUS_METERS = 6_371_000.0
private const val MIN_ROUTE_SPAN_DEGREES = 1e-6
private const val MAX_PACE_DISPLAY_SECONDS = 99L * 60L + 59L

// ============================================================================
// 1. Sensor Langkah (TYPE_STEP_COUNTER & TYPE_STEP_DETECTOR) + Pause/Resume
// ============================================================================

/**
 * Memproses pembacaan baru dari `Sensor.TYPE_STEP_COUNTER` secara murni.
 *
 * - Pembacaan pertama saat aktif menjadi *baseline* segmen.
 * - Jika counter turun (mis. perangkat/sensor reboot), langkah segmen sebelumnya dikunci
 *   ke [StepCounterState.committedSteps] dan *baseline* di-reset ke [rawCounterValue].
 * - Saat [StepCounterState.isPaused] bernilai `true`, langkah yang terjadi selama jeda
 *   diabaikan dan tidak menambah total langkah.
 */
fun onStepCounterEvent(
    state: StepCounterState,
    rawCounterValue: Float,
): StepCounterState {
    if (!rawCounterValue.isFinite() || rawCounterValue < 0f) {
        return state
    }

    if (state.isPaused) {
        return state.copy(
            segmentBaseline = null,
            lastRawCounter = rawCounterValue,
        )
    }

    val baseline = state.segmentBaseline
    if (baseline == null) {
        return state.copy(
            segmentBaseline = rawCounterValue,
            currentSegmentSteps = 0,
            lastRawCounter = rawCounterValue,
        )
    }

    val previousRaw = state.lastRawCounter ?: baseline
    if (rawCounterValue < previousRaw || rawCounterValue < baseline) {
        // Sensor reboot atau counter reset: simpan progres sejauh ini dan mulai baseline baru.
        return state.copy(
            committedSteps = state.totalSteps,
            currentSegmentSteps = 0,
            segmentBaseline = rawCounterValue,
            lastRawCounter = rawCounterValue,
        )
    }

    val segmentDelta = (rawCounterValue - baseline).toInt().coerceAtLeast(0)
    return state.copy(
        currentSegmentSteps = segmentDelta,
        lastRawCounter = rawCounterValue,
    )
}

/**
 * Menambahkan langkah dari fallback `Sensor.TYPE_STEP_DETECTOR` (hanya saat tidak dijeda).
 */
fun onStepDetectorPulse(
    state: StepCounterState,
    detectedSteps: Int = 1,
): StepCounterState {
    if (state.isPaused || detectedSteps <= 0) return state
    return state.copy(
        committedSteps = state.committedSteps + detectedSteps,
    )
}

/**
 * Menjeda penghitungan sensor langkah dengan mengunci progres segmen aktif terlebih dahulu.
 */
fun pauseStepCounter(state: StepCounterState): StepCounterState {
    if (state.isPaused) return state
    return state.copy(
        committedSteps = state.totalSteps,
        currentSegmentSteps = 0,
        segmentBaseline = null,
        isPaused = true,
    )
}

/**
 * Melanjutkan penghitungan sensor langkah dari baseline baru agar langkah selama jeda tidak terhitung.
 */
fun resumeStepCounter(state: StepCounterState): StepCounterState {
    if (!state.isPaused) return state
    return state.copy(
        currentSegmentSteps = 0,
        segmentBaseline = null,
        isPaused = false,
    )
}

// ============================================================================
// 2. Fallback Estimasi Langkah dari Akumulasi Jarak GPS
// ============================================================================

/**
 * Mengestimasi jumlah langkah dari total jarak tempuh rute GPS valid (`0,75 m/langkah`).
 */
fun estimateStepsFromDistanceMeters(
    distanceMeters: Double,
    stepLengthMeters: Double = GPS_FALLBACK_STEP_LENGTH_METERS,
): Int {
    if (!distanceMeters.isFinite() || distanceMeters <= 0.0) return 0
    if (!stepLengthMeters.isFinite() || stepLengthMeters <= 0.0) return 0
    return (distanceMeters / stepLengthMeters).roundToInt().coerceAtLeast(0)
}

/**
 * Menentukan jumlah langkah efektif dan sumber aktifnya:
 * - Jika sensor pedometer tersedia & izin diberikan → gunakan [pedometerSteps] ([StepSourceMode.PEDOMETER]).
 * - Jika tidak → fallback otomatis ke estimasi jarak GPS ([StepSourceMode.GPS_FALLBACK]).
 */
fun resolveEffectiveSteps(
    pedometerSteps: Int,
    distanceMeters: Double,
    pedometerAvailableAndGranted: Boolean,
    stepLengthMeters: Double = GPS_FALLBACK_STEP_LENGTH_METERS,
): Pair<Int, StepSourceMode> {
    return if (pedometerAvailableAndGranted) {
        pedometerSteps.coerceAtLeast(0) to StepSourceMode.PEDOMETER
    } else {
        estimateStepsFromDistanceMeters(distanceMeters, stepLengthMeters) to StepSourceMode.GPS_FALLBACK
    }
}

// ============================================================================
// 3. Haversine & Filter Anti-GPS Drift + Penanganan Pause/Resume
// ============================================================================

/**
 * Menghitung jarak geodesik Haversine (dalam meter) antara dua titik koordinat.
 */
fun haversineDistanceMeters(
    lat1: Double,
    lng1: Double,
    lat2: Double,
    lng2: Double,
): Double {
    if (!isValidCoordinate(lat1, lng1) || !isValidCoordinate(lat2, lng2)) return 0.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val rLat1 = Math.toRadians(lat1)
    val rLat2 = Math.toRadians(lat2)

    val sinDLat = sin(dLat / 2.0)
    val sinDLng = sin(dLng / 2.0)
    val a = sinDLat * sinDLat + cos(rLat1) * cos(rLat2) * sinDLng * sinDLng
    val c = 2.0 * atan2(sqrt(a.coerceIn(0.0, 1.0)), sqrt((1.0 - a).coerceIn(0.0, 1.0)))
    return EARTH_RADIUS_METERS * c
}

fun haversineDistanceMeters(a: StepPoint, b: StepPoint): Double =
    haversineDistanceMeters(a.lat, a.lng, b.lat, b.lng)

/**
 * Menghitung akumulasi jarak tempuh dari urutan titik rute (segmen per segmen, aman untuk rute memutar).
 */
fun calculateCumulativeDistanceMeters(points: List<StepPoint>): Double {
    if (points.size < 2) return 0.0
    var total = 0.0
    for (index in 1 until points.size) {
        total += haversineDistanceMeters(points[index - 1], points[index])
    }
    return total
}

fun isValidCoordinate(lat: Double, lng: Double): Boolean {
    if (!lat.isFinite() || !lng.isFinite()) return false
    if (lat !in -90.0..90.0 || lng !in -180.0..180.0) return false
    if (lat == 0.0 && lng == 0.0) return false
    return true
}

/**
 * Mengevaluasi satu sampel GPS terhadap filter Anti-GPS Drift tanpa mengubah state.
 */
fun evaluateGpsSample(
    state: GpsTrackState,
    sample: GpsSample,
    maxAccuracyMeters: Float = MAX_GPS_ACCURACY_METERS,
    minDisplacementMeters: Double = MIN_GPS_DISPLACEMENT_METERS,
    maxReasonableSpeedMps: Double = MAX_REASONABLE_SPEED_MPS,
): GpsFilterDecision {
    if (state.isPaused) {
        return GpsFilterDecision(
            accepted = false,
            rejectReason = GpsFilterRejectReason.PAUSED,
        )
    }

    if (!isValidCoordinate(sample.lat, sample.lng)) {
        return GpsFilterDecision(
            accepted = false,
            rejectReason = GpsFilterRejectReason.INVALID_COORDINATE,
        )
    }

    if (!sample.accuracyMeters.isFinite() || sample.accuracyMeters <= 0f || sample.accuracyMeters > maxAccuracyMeters) {
        return GpsFilterDecision(
            accepted = false,
            rejectReason = GpsFilterRejectReason.POOR_ACCURACY,
        )
    }

    val newPoint = StepPoint(
        lat = sample.lat,
        lng = sample.lng,
        timestampMillis = sample.timestampMillis,
    )

    val previous = state.lastAcceptedPoint
    if (previous == null || state.awaitingResumeAnchor) {
        // Titik pertama sesi atau titik jangkar pertama setelah Resume:
        // diterima tanpa menambah jarak dari titik sebelum jeda dan tanpa cek kecepatan palsu.
        return GpsFilterDecision(
            accepted = true,
            addedDistanceMeters = 0.0,
            point = newPoint,
        )
    }

    val segmentMeters = haversineDistanceMeters(previous, newPoint)
    if (segmentMeters < minDisplacementMeters) {
        return GpsFilterDecision(
            accepted = false,
            rejectReason = GpsFilterRejectReason.TOO_SMALL_DISPLACEMENT,
        )
    }

    val elapsedMillis = sample.timestampMillis - previous.timestampMillis
    if (elapsedMillis <= 0L) {
        return GpsFilterDecision(
            accepted = false,
            rejectReason = GpsFilterRejectReason.UNREALISTIC_SPEED,
        )
    }

    val speedMps = segmentMeters / (elapsedMillis / 1000.0)
    if (!speedMps.isFinite() || speedMps > maxReasonableSpeedMps) {
        return GpsFilterDecision(
            accepted = false,
            rejectReason = GpsFilterRejectReason.UNREALISTIC_SPEED,
        )
    }

    return GpsFilterDecision(
        accepted = true,
        addedDistanceMeters = segmentMeters,
        point = newPoint,
    )
}

/**
 * Memproses sampel GPS ke [GpsTrackState] dan mengembalikan state terbaru beserta keputusannya.
 */
fun processGpsSample(
    state: GpsTrackState,
    sample: GpsSample,
    maxAccuracyMeters: Float = MAX_GPS_ACCURACY_METERS,
    minDisplacementMeters: Double = MIN_GPS_DISPLACEMENT_METERS,
    maxReasonableSpeedMps: Double = MAX_REASONABLE_SPEED_MPS,
): Pair<GpsTrackState, GpsFilterDecision> {
    val decision = evaluateGpsSample(
        state = state,
        sample = sample,
        maxAccuracyMeters = maxAccuracyMeters,
        minDisplacementMeters = minDisplacementMeters,
        maxReasonableSpeedMps = maxReasonableSpeedMps,
    )
    if (!decision.accepted || decision.point == null) {
        return state to decision
    }
    val updatedState = state.copy(
        acceptedPoints = state.acceptedPoints + decision.point,
        lastAcceptedPoint = decision.point,
        totalDistanceMeters = state.totalDistanceMeters + decision.addedDistanceMeters,
        awaitingResumeAnchor = false,
    )
    return updatedState to decision
}

/** Menjeda perekaman GPS agar perpindahan saat jeda tidak direkam. */
fun pauseGpsTrack(state: GpsTrackState): GpsTrackState {
    if (state.isPaused) return state
    return state.copy(isPaused = true)
}

/**
 * Melanjutkan perekaman GPS dengan menandai titik valid berikutnya sebagai jangkar baru
 * sehingga jarak selama jeda tidak terhitung dan tidak ditolak sebagai lonjakan kecepatan.
 */
fun resumeGpsTrack(state: GpsTrackState): GpsTrackState {
    if (!state.isPaused) return state
    return state.copy(
        isPaused = false,
        awaitingResumeAnchor = state.lastAcceptedPoint != null,
    )
}

// ============================================================================
// 4. Metrik (Pace, Kalori) & Formatter
// ============================================================================

/**
 * Menghitung pace dalam detik per kilometer (`sec/km`).
 * Mengembalikan `null` bila jarak masih `< 20 m` atau durasi `<= 0` untuk mencegah angka ekstrem/Infinity.
 */
fun calculatePaceSecPerKm(
    distanceMeters: Double,
    durationSeconds: Long,
    minDistanceMeters: Double = MIN_DISTANCE_FOR_PACE_METERS,
): Double? {
    if (!distanceMeters.isFinite() || distanceMeters < minDistanceMeters || durationSeconds <= 0L) {
        return null
    }
    val distanceKm = distanceMeters / 1000.0
    val pace = durationSeconds.toDouble() / distanceKm
    return if (pace.isFinite() && pace > 0.0) pace else null
}

/**
 * Memformat pace (`sec/km`) menjadi `"MM:SS"` atau `"--:--"` bila belum tersedia.
 */
fun formatPace(paceSecPerKm: Double?): String {
    if (paceSecPerKm == null || !paceSecPerKm.isFinite() || paceSecPerKm <= 0.0) {
        return "--:--"
    }
    val totalSeconds = paceSecPerKm.roundToLong().coerceIn(1L, MAX_PACE_DISPLAY_SECONDS)
    val minutes = totalSeconds / 60L
    val seconds = totalSeconds % 60L
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

/**
 * Mengestimasi kalori terbakar (`kkal`):
 * - Berbasis langkah: `~0,04 kkal/langkah` (aman untuk aktivitas indoor tanpa GPS).
 * - Berbasis jarak GPS: `~60 kkal/km` saat jarak valid `>= 20 m`.
 */
fun calculateCaloriesKcal(
    steps: Int,
    distanceMeters: Double,
): Int {
    val safeSteps = steps.coerceAtLeast(0)
    val safeDistance = if (distanceMeters.isFinite() && distanceMeters > 0.0) distanceMeters else 0.0
    val stepBased = safeSteps * CALORIES_PER_STEP
    val distanceBased = if (safeDistance >= MIN_DISTANCE_FOR_PACE_METERS) {
        (safeDistance / 1000.0) * CALORIES_PER_KM
    } else {
        0.0
    }
    return max(stepBased, distanceBased).roundToInt().coerceAtLeast(0)
}

/**
 * Memformat durasi detik menjadi `"MM:SS"` atau `"HH:MM:SS"` bila mencapai 1 jam atau lebih.
 */
fun formatDuration(durationSeconds: Long): String {
    val safe = durationSeconds.coerceAtLeast(0L)
    val hours = safe / 3600L
    val minutes = (safe % 3600L) / 60L
    val seconds = safe % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%02d:%02d", minutes, seconds)
    }
}

/**
 * Memformat jarak tempuh menjadi `"450 m"` (< 1 km) atau `"1,25 km"` (>= 1 km).
 */
fun formatDistance(distanceMeters: Double): String {
    val safe = if (distanceMeters.isFinite() && distanceMeters > 0.0) distanceMeters else 0.0
    if (safe < 999.5) {
        return "${safe.roundToInt()} m"
    }
    val km = safe / 1000.0
    val formattedKm = String.format(Locale.US, "%.2f", km).replace('.', ',')
    return "$formattedKm km"
}

/**
 * Memformat angka ribuan menggunakan pemisah titik bergaya lokal Indonesia (mis. `6.000`).
 */
fun formatStepCount(steps: Int): String {
    val safe = steps.coerceAtLeast(0)
    val digits = safe.toString()
    if (digits.length <= 3) return digits
    val sb = StringBuilder(digits.length + digits.length / 3)
    val remainder = digits.length % 3
    for (i in digits.indices) {
        if (i > 0 && (i - remainder) % 3 == 0) {
            sb.append('.')
        }
        sb.append(digits[i])
    }
    return sb.toString()
}

// ============================================================================
// 5. Progres Langkah Harian (Auto-Reset 00:00)
// ============================================================================

fun formatStepDateKey(date: LocalDate): String = date.toString()

fun clampDailyStepTarget(targetSteps: Int): Int =
    targetSteps.coerceIn(MIN_DAILY_STEP_TARGET, MAX_DAILY_STEP_TARGET)

fun normalizeDailyStepProgress(
    progress: DailyStepProgress,
    today: LocalDate,
): DailyStepProgress {
    val sanitizedTarget = clampDailyStepTarget(progress.targetSteps)
    val todayKey = formatStepDateKey(today)
    return if (progress.dateKey != todayKey) {
        DailyStepProgress(
            dateKey = todayKey,
            totalSteps = 0,
            targetSteps = sanitizedTarget,
        )
    } else {
        progress.copy(
            totalSteps = progress.totalSteps.coerceAtLeast(0),
            targetSteps = sanitizedTarget,
        )
    }
}

fun applyCompletedSessionSteps(
    progress: DailyStepProgress,
    sessionSteps: Int,
    today: LocalDate,
): DailyStepProgress {
    val normalized = normalizeDailyStepProgress(progress, today)
    val added = sessionSteps.coerceAtLeast(0)
    return normalized.copy(
        totalSteps = (normalized.totalSteps + added).coerceAtLeast(0),
    )
}

fun updateDailyStepTarget(
    progress: DailyStepProgress,
    newTargetSteps: Int,
    today: LocalDate,
): DailyStepProgress {
    val normalized = normalizeDailyStepProgress(progress, today)
    return normalized.copy(
        targetSteps = clampDailyStepTarget(newTargetSteps),
    )
}

fun calculateDailyStepFraction(progress: DailyStepProgress): Float {
    if (progress.targetSteps <= 0) return 0f
    return (progress.totalSteps.coerceAtLeast(0).toFloat() / progress.targetSteps.toFloat())
        .coerceIn(0f, 1f)
}

fun calculateDailyStepPercent(progress: DailyStepProgress): Int {
    if (progress.targetSteps <= 0) return 0
    val ratio = progress.totalSteps.coerceAtLeast(0).toDouble() / progress.targetSteps.toDouble()
    return (ratio * 100.0).roundToInt().coerceIn(0, 100)
}

// ============================================================================
// 6. Normalisasi Polyline untuk Share Card & Downsampling Titik Rute
// ============================================================================

/**
 * Mengonversi daftar [StepPoint] ke koordinat kanvas ter-normalisasi (`0f..1f`)
 * dengan mempertahankan rasio aspek asli (termasuk koreksi kosinus lintang) dan padding rapi.
 *
 * Mengembalikan daftar kosong bila jumlah titik `< 2` atau bentangan koordinat terlalu kecil
 * sehingga pemanggil dapat menampilkan ilustrasi jejak langkah minimalis.
 */
fun normalizeRouteToCanvas(
    points: List<StepPoint>,
    paddingFraction: Float = 0.12f,
): List<NormalizedRoutePoint> {
    val validPoints = points.filter { isValidCoordinate(it.lat, it.lng) }
    if (validPoints.size < 2) return emptyList()

    val safePadding = paddingFraction.coerceIn(0f, 0.4f)
    val usableSpan = (1.0 - 2.0 * safePadding).coerceAtLeast(0.1)

    val avgLat = validPoints.map { it.lat }.average()
    val cosLat = cos(Math.toRadians(avgLat)).coerceAtLeast(0.1)

    val projected = validPoints.map { point ->
        (point.lng * cosLat) to point.lat
    }

    var minX = Double.POSITIVE_INFINITY
    var maxX = Double.NEGATIVE_INFINITY
    var minY = Double.POSITIVE_INFINITY
    var maxY = Double.NEGATIVE_INFINITY
    for ((x, y) in projected) {
        if (x < minX) minX = x
        if (x > maxX) maxX = x
        if (y < minY) minY = y
        if (y > maxY) maxY = y
    }

    val spanX = maxX - minX
    val spanY = maxY - minY
    val maxSpan = max(spanX, spanY)
    if (!maxSpan.isFinite() || maxSpan < MIN_ROUTE_SPAN_DEGREES) {
        return emptyList()
    }

    val scale = usableSpan / maxSpan
    val drawnWidth = spanX * scale
    val drawnHeight = spanY * scale
    val offsetX = safePadding + (usableSpan - drawnWidth) / 2.0
    val offsetY = safePadding + (usableSpan - drawnHeight) / 2.0

    return projected.map { (x, y) ->
        val normX = (offsetX + (x - minX) * scale).toFloat().coerceIn(0f, 1f)
        // Sumbu Y kanvas mengarah ke bawah, sedangkan lintang (utara) mengarah ke atas.
        val normY = (offsetY + (maxY - y) * scale).toFloat().coerceIn(0f, 1f)
        NormalizedRoutePoint(x = normX, y = normY)
    }
}

/**
 * Mengurangi jumlah titik rute secara merata hingga paling banyak [maxPoints] titik
 * dengan selalu mempertahankan titik awal dan titik akhir secara persis.
 */
fun downsampleRoutePoints(
    points: List<StepPoint>,
    maxPoints: Int = MAX_STORED_ROUTE_POINTS,
): List<StepPoint> {
    if (maxPoints < 2) return points.take(maxPoints.coerceAtLeast(0))
    if (points.size <= maxPoints) return points

    val lastIndex = points.lastIndex
    val step = lastIndex.toDouble() / (maxPoints - 1).toDouble()
    return List(maxPoints) { i ->
        val sourceIndex = when (i) {
            0 -> 0
            maxPoints - 1 -> lastIndex
            else -> (i * step).roundToInt().coerceIn(0, lastIndex)
        }
        points[sourceIndex]
    }
}

/**
 * Menyusun [StepSessionRecord] tervalidasi dari metrik sesi yang selesai.
 */
fun buildStepSessionRecord(
    id: String,
    startedAtMillis: Long,
    endedAtMillis: Long,
    durationSeconds: Long,
    steps: Int,
    distanceMeters: Double,
    routePoints: List<StepPoint>,
    stepSource: StepSourceMode,
    maxStoredRoutePoints: Int = MAX_STORED_ROUTE_POINTS,
): StepSessionRecord {
    val safeDuration = durationSeconds.coerceAtLeast(0L)
    val safeSteps = steps.coerceAtLeast(0)
    val safeDistance = if (distanceMeters.isFinite() && distanceMeters > 0.0) distanceMeters else 0.0
    val safeEnded = if (endedAtMillis >= startedAtMillis) endedAtMillis else startedAtMillis
    return StepSessionRecord(
        id = id,
        startedAtMillis = startedAtMillis,
        endedAtMillis = safeEnded,
        durationSeconds = safeDuration,
        steps = safeSteps,
        distanceMeters = safeDistance,
        paceSecPerKm = calculatePaceSecPerKm(safeDistance, safeDuration),
        caloriesKcal = calculateCaloriesKcal(safeSteps, safeDistance),
        routePoints = downsampleRoutePoints(routePoints, maxStoredRoutePoints),
        stepSource = stepSource,
    )
}
