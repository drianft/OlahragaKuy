package com.example.olahragakuy.pages.steptracker

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class StepTrackerLogicTest {

    // =========================================================================
    // 1. Sensor Langkah (TYPE_STEP_COUNTER & TYPE_STEP_DETECTOR) + Pause/Resume
    // =========================================================================

    @Test
    fun stepCounter_setsInitialBaselineAndCalculatesNormalDelta() {
        var state = StepCounterState()

        state = onStepCounterEvent(state, 10_000f)
        assertEquals(0, state.totalSteps)
        assertEquals(10_000f, state.segmentBaseline)

        state = onStepCounterEvent(state, 10_042f)
        assertEquals(42, state.totalSteps)

        state = onStepCounterEvent(state, 10_120f)
        assertEquals(120, state.totalSteps)
    }

    @Test
    fun stepCounter_handlesSensorRebootWithoutLosingAccumulatedSteps() {
        var state = StepCounterState()
        state = onStepCounterEvent(state, 5_000f)
        state = onStepCounterEvent(state, 5_080f)
        assertEquals(80, state.totalSteps)

        // Simulasi perangkat / sensor reboot sehingga counter turun ke 10f.
        state = onStepCounterEvent(state, 10f)
        assertEquals(80, state.totalSteps)
        assertEquals(10f, state.segmentBaseline)

        // Langkah baru setelah reboot tetap ditambahkan di atas 80 langkah sebelumnya.
        state = onStepCounterEvent(state, 35f)
        assertEquals(105, state.totalSteps)
    }

    @Test
    fun stepCounter_ignoresStepsDuringPauseAndResetsBaselineOnResume() {
        var state = StepCounterState()
        state = onStepCounterEvent(state, 1_000f)
        state = onStepCounterEvent(state, 1_050f)
        assertEquals(50, state.totalSteps)

        state = pauseStepCounter(state)
        assertTrue(state.isPaused)
        assertEquals(50, state.totalSteps)

        // User berjalan 75 langkah saat dijeda -> harus diabaikan.
        state = onStepCounterEvent(state, 1_090f)
        state = onStepCounterEvent(state, 1_125f)
        assertEquals(50, state.totalSteps)

        // Lanjut -> pembacaan pertama menjadi baseline baru (1_125f), tidak bocor langkah jeda.
        state = resumeStepCounter(state)
        assertFalse(state.isPaused)
        state = onStepCounterEvent(state, 1_125f)
        assertEquals(50, state.totalSteps)

        state = onStepCounterEvent(state, 1_155f)
        assertEquals(80, state.totalSteps)
    }

    @Test
    fun stepCounter_ignoresInvalidValuesAndStepDetectorRespectsPause() {
        var state = StepCounterState()
        state = onStepCounterEvent(state, Float.NaN)
        state = onStepCounterEvent(state, -5f)
        assertEquals(0, state.totalSteps)

        state = onStepDetectorPulse(state, 3)
        assertEquals(3, state.totalSteps)

        state = pauseStepCounter(state)
        state = onStepDetectorPulse(state, 4)
        assertEquals(3, state.totalSteps)

        state = resumeStepCounter(state)
        state = onStepDetectorPulse(state, 2)
        assertEquals(5, state.totalSteps)
    }

    // =========================================================================
    // 2. Fallback Estimasi Langkah dari Akumulasi Jarak GPS
    // =========================================================================

    @Test
    fun estimateStepsFromDistanceMeters_usesDefaultStepLengthAndGuardsInvalidInput() {
        assertEquals(0, estimateStepsFromDistanceMeters(0.0))
        assertEquals(0, estimateStepsFromDistanceMeters(-15.0))
        assertEquals(0, estimateStepsFromDistanceMeters(Double.NaN))
        // 75 m / 0.75 m = 100 langkah
        assertEquals(100, estimateStepsFromDistanceMeters(75.0))
        // 750 m / 0.75 m = 1000 langkah
        assertEquals(1_000, estimateStepsFromDistanceMeters(750.0))
    }

    @Test
    fun resolveEffectiveSteps_switchesBetweenPedometerAndGpsFallback() {
        val (pedometerSteps, pedometerMode) = resolveEffectiveSteps(
            pedometerSteps = 320,
            distanceMeters = 500.0,
            pedometerAvailableAndGranted = true,
        )
        assertEquals(320, pedometerSteps)
        assertEquals(StepSourceMode.PEDOMETER, pedometerMode)

        val (fallbackSteps, fallbackMode) = resolveEffectiveSteps(
            pedometerSteps = 0,
            distanceMeters = 150.0,
            pedometerAvailableAndGranted = false,
        )
        assertEquals(200, fallbackSteps)
        assertEquals(StepSourceMode.GPS_FALLBACK, fallbackMode)
    }

    // =========================================================================
    // 3. Filter Anti-GPS Drift, Haversine Kumulatif & Jangkar Resume
    // =========================================================================

    @Test
    fun gpsFilter_rejectsPoorAccuracySmallJitterAndUnrealisticSpeed() {
        var track = GpsTrackState()

        // 1. Akurasi buruk (> 25 m) ditolak.
        val (t1, d1) = processGpsSample(
            track,
            GpsSample(lat = 3.5625, lng = 98.6567, accuracyMeters = 32f, timestampMillis = 1_000L),
        )
        assertFalse(d1.accepted)
        assertEquals(GpsFilterRejectReason.POOR_ACCURACY, d1.rejectReason)
        track = t1

        // 2. Titik valid pertama diterima dengan jarak tambah 0 m.
        val (t2, d2) = processGpsSample(
            track,
            GpsSample(lat = 3.5625, lng = 98.6567, accuracyMeters = 8f, timestampMillis = 2_000L),
        )
        assertTrue(d2.accepted)
        assertEquals(0.0, d2.addedDistanceMeters, 0.001)
        assertEquals(1, t2.acceptedPoints.size)
        track = t2

        // 3. Perpindahan terlalu kecil (< 3 m, sekitar ~1,1 m) ditolak sebagai jitter.
        val (t3, d3) = processGpsSample(
            track,
            GpsSample(lat = 3.56251, lng = 98.6567, accuracyMeters = 6f, timestampMillis = 5_000L),
        )
        assertFalse(d3.accepted)
        assertEquals(GpsFilterRejectReason.TOO_SMALL_DISPLACEMENT, d3.rejectReason)
        track = t3

        // 4. Lonjakan kecepatan tidak wajar (> 12 m/s, ~111 m dalam 2 detik = ~55,5 m/s) ditolak.
        val (t4, d4) = processGpsSample(
            track,
            GpsSample(lat = 3.5635, lng = 98.6567, accuracyMeters = 7f, timestampMillis = 4_000L),
        )
        assertFalse(d4.accepted)
        assertEquals(GpsFilterRejectReason.UNREALISTIC_SPEED, d4.rejectReason)
        track = t4

        // 5. Segmen jalan kaki wajar (~22 m dalam 15 detik = ~1,5 m/s) diterima.
        val (t5, d5) = processGpsSample(
            track,
            GpsSample(lat = 3.5627, lng = 98.6567, accuracyMeters = 8f, timestampMillis = 17_000L),
        )
        assertTrue(d5.accepted)
        assertTrue(d5.addedDistanceMeters in 20.0..25.0)
        assertEquals(2, t5.acceptedPoints.size)
        assertEquals(d5.addedDistanceMeters, t5.totalDistanceMeters, 0.001)
    }

    @Test
    fun gpsFilter_pauseAndResumeAnchorsNextValidPointWithoutCountingPausedTravel() {
        var track = GpsTrackState()

        // Titik 1 -> Titik 2 (~22 m dalam 12 detik).
        track = processGpsSample(
            track,
            GpsSample(3.5625, 98.6567, accuracyMeters = 6f, timestampMillis = 1_000L),
        ).first
        track = processGpsSample(
            track,
            GpsSample(3.5627, 98.6567, accuracyMeters = 6f, timestampMillis = 13_000L),
        ).first
        val distanceBeforePause = track.totalDistanceMeters
        assertTrue(distanceBeforePause in 20.0..25.0)

        // Jeda -> sampel selama jeda harus ditolak.
        track = pauseGpsTrack(track)
        val (_, pausedDecision) = processGpsSample(
            track,
            GpsSample(3.5630, 98.6567, accuracyMeters = 6f, timestampMillis = 20_000L),
        )
        assertFalse(pausedDecision.accepted)
        assertEquals(GpsFilterRejectReason.PAUSED, pausedDecision.rejectReason)

        // Resume di lokasi jauh (~550 m) dalam waktu singkat: diterima sebagai jangkar baru (jarak tambah 0).
        track = resumeGpsTrack(track)
        val (afterResumeAnchor, anchorDecision) = processGpsSample(
            track,
            GpsSample(3.5677, 98.6567, accuracyMeters = 6f, timestampMillis = 25_000L),
        )
        assertTrue(anchorDecision.accepted)
        assertEquals(0.0, anchorDecision.addedDistanceMeters, 0.001)
        assertEquals(distanceBeforePause, afterResumeAnchor.totalDistanceMeters, 0.001)
        assertFalse(afterResumeAnchor.awaitingResumeAnchor)

        // Titik berikutnya setelah jangkar kembali menambahkan jarak normal (~22 m).
        val (afterResumeWalk, walkDecision) = processGpsSample(
            afterResumeAnchor,
            GpsSample(3.5679, 98.6567, accuracyMeters = 6f, timestampMillis = 38_000L),
        )
        assertTrue(walkDecision.accepted)
        assertTrue(walkDecision.addedDistanceMeters in 20.0..25.0)
        assertEquals(
            distanceBeforePause + walkDecision.addedDistanceMeters,
            afterResumeWalk.totalDistanceMeters,
            0.001,
        )
    }

    @Test
    fun cumulativeDistance_sumsLoopRouteSegmentsAccurately() {
        val start = StepPoint(3.5625, 98.6567, 1_000L)
        val north = StepPoint(3.5635, 98.6567, 60_000L) // ~111 m utara
        val backToStart = StepPoint(3.5625, 98.6567, 120_000L) // kembali ke awal (~111 m lagi)

        val total = calculateCumulativeDistanceMeters(listOf(start, north, backToStart))
        // Rute memutar harus menghitung total segmen (~222 m), bukan jarak garis lurus start-akhir (0 m).
        assertTrue(total in 215.0..230.0)
    }

    // =========================================================================
    // 4. Metrik (Pace, Kalori) & Formatter
    // =========================================================================

    @Test
    fun paceAndFormatters_handleShortDistanceAndValidValues() {
        assertNull(calculatePaceSecPerKm(distanceMeters = 15.0, durationSeconds = 30L))
        assertEquals("--:--", formatPace(null))

        // 1 km dalam 6 menit 30 detik (390 detik) -> 06:30
        val pace = calculatePaceSecPerKm(distanceMeters = 1_000.0, durationSeconds = 390L)
        assertNotNull(pace)
        assertEquals(390.0, pace!!, 0.01)
        assertEquals("06:30", formatPace(pace))

        // Durasi < 1 jam vs >= 1 jam
        assertEquals("00:00", formatDuration(0L))
        assertEquals("05:09", formatDuration(309L))
        assertEquals("01:02:05", formatDuration(3_725L))

        // Jarak < 1 km vs >= 1 km
        assertEquals("0 m", formatDistance(0.0))
        assertEquals("450 m", formatDistance(450.0))
        assertEquals("1,25 km", formatDistance(1_250.0))

        // Format ribuan lokal
        assertEquals("0", formatStepCount(0))
        assertEquals("950", formatStepCount(950))
        assertEquals("6.000", formatStepCount(6_000))
        assertEquals("125.430", formatStepCount(125_430))
    }

    @Test
    fun calculateCaloriesKcal_worksForBothIndoorStepsAndOutdoorDistance() {
        // Indoor tanpa GPS: 1.000 langkah * 0,04 = 40 kkal
        assertEquals(40, calculateCaloriesKcal(steps = 1_000, distanceMeters = 0.0))
        // Outdoor 2 km: 2 * 60 = 120 kkal (lebih tinggi dari 2.000 * 0,04 = 80 kkal)
        assertEquals(120, calculateCaloriesKcal(steps = 2_000, distanceMeters = 2_000.0))
    }

    // =========================================================================
    // 5. Progres Harian (Auto-Reset 00:00), Normalisasi Kanvas & Downsampling
    // =========================================================================

    @Test
    fun dailyStepProgress_resetsOnNewDateAndClampsTargetBounds() {
        val yesterday = LocalDate.of(2026, 11, 23)
        val today = LocalDate.of(2026, 11, 24)

        val storedYesterday = DailyStepProgress(
            dateKey = formatStepDateKey(yesterday),
            totalSteps = 4_800,
            targetSteps = 8_000,
        )

        val resetToday = normalizeDailyStepProgress(storedYesterday, today)
        assertEquals("2026-11-24", resetToday.dateKey)
        assertEquals(0, resetToday.totalSteps)
        assertEquals(8_000, resetToday.targetSteps)

        val afterSession = applyCompletedSessionSteps(resetToday, 2_000, today)
        assertEquals(2_000, afterSession.totalSteps)
        assertEquals(0.25f, calculateDailyStepFraction(afterSession), 0.001f)
        assertEquals(25, calculateDailyStepPercent(afterSession))

        // Validasi batas aman target harian.
        val clampedLow = updateDailyStepTarget(afterSession, 50, today)
        assertEquals(MIN_DAILY_STEP_TARGET, clampedLow.targetSteps)
        val clampedHigh = updateDailyStepTarget(afterSession, 999_999, today)
        assertEquals(MAX_DAILY_STEP_TARGET, clampedHigh.targetSteps)
    }

    @Test
    fun normalizeRouteToCanvas_preservesAspectRatioPaddingAndHandlesShortRoutes() {
        // < 2 titik atau titik identik -> kosong (memicu ilustrasi jejak langkah minimalis).
        assertTrue(normalizeRouteToCanvas(emptyList()).isEmpty())
        assertTrue(normalizeRouteToCanvas(listOf(StepPoint(3.5625, 98.6567))).isEmpty())
        assertTrue(
            normalizeRouteToCanvas(
                listOf(StepPoint(3.5625, 98.6567), StepPoint(3.5625, 98.6567)),
            ).isEmpty(),
        )

        // Rute L-shape dari selatan ke utara lalu ke timur.
        val route = listOf(
            StepPoint(3.5600, 98.6550),
            StepPoint(3.5620, 98.6550),
            StepPoint(3.5620, 98.6570),
        )
        val normalized = normalizeRouteToCanvas(route, paddingFraction = 0.12f)
        assertEquals(3, normalized.size)
        normalized.forEach { pt ->
            assertTrue(pt.x in 0.119f..0.881f)
            assertTrue(pt.y in 0.119f..0.881f)
        }
        // Titik lebih utara (lat lebih besar) harus memiliki Y kanvas lebih kecil (lebih atas).
        assertTrue(normalized[1].y < normalized[0].y)
        // Titik lebih timur (lng lebih besar) harus memiliki X kanvas lebih besar (lebih kanan).
        assertTrue(normalized[2].x > normalized[1].x)
    }

    @Test
    fun downsampleRoutePoints_capsSizeAndPreservesExactEndpoints() {
        val denseRoute = List(650) { idx ->
            StepPoint(
                lat = 3.5600 + idx * 0.00001,
                lng = 98.6550 + idx * 0.00001,
                timestampMillis = idx * 1_000L,
            )
        }

        val sampled = downsampleRoutePoints(denseRoute, maxPoints = MAX_STORED_ROUTE_POINTS)
        assertEquals(MAX_STORED_ROUTE_POINTS, sampled.size)
        assertEquals(denseRoute.first(), sampled.first())
        assertEquals(denseRoute.last(), sampled.last())

        val record = buildStepSessionRecord(
            id = "session-1",
            startedAtMillis = 1_000L,
            endedAtMillis = 651_000L,
            durationSeconds = 650L,
            steps = 1_200,
            distanceMeters = 900.0,
            routePoints = denseRoute,
            stepSource = StepSourceMode.PEDOMETER,
        )
        assertEquals(MAX_STORED_ROUTE_POINTS, record.routePoints.size)
        assertNotNull(record.paceSecPerKm)
        assertTrue(record.caloriesKcal > 0)
    }
}
