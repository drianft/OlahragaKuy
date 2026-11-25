package com.example.olahragakuy.pages.training.streak

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

class StreakLogicTest {

    @Before
    fun setUp() {
        TrainingStreakStore.reset()
    }

    @After
    fun tearDown() {
        TrainingStreakStore.reset()
    }

    @Test
    fun qualificationRequiresWorkoutOrAtLeastOneKilometer() {
        assertFalse(isDailyActivityQualified(workoutCompleted = false, stepDistanceMeters = 0.0))
        assertFalse(isDailyActivityQualified(workoutCompleted = false, stepDistanceMeters = 999.9))
        assertFalse(isDailyActivityQualified(workoutCompleted = false, stepDistanceMeters = -500.0))
        assertFalse(isDailyActivityQualified(workoutCompleted = false, stepDistanceMeters = Double.NaN))

        assertTrue(isDailyActivityQualified(workoutCompleted = true, stepDistanceMeters = 0.0))
        assertTrue(isDailyActivityQualified(workoutCompleted = false, stepDistanceMeters = 1000.0))
        assertTrue(isDailyActivityQualified(workoutCompleted = false, stepDistanceMeters = 2450.5))
        assertTrue(isDailyActivityQualified(workoutCompleted = true, stepDistanceMeters = 1500.0))
    }

    @Test
    fun currentStreakCountsConsecutiveDaysAndStaysAliveIfYesterdayQualified() {
        val today = LocalDate.of(2026, 11, 25) // Rabu
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        val fourDaysAgo = today.minusDays(4)

        // Kosong -> 0
        assertEquals(0, calculateCurrentStreakDays(emptyMap(), today))

        // Hanya kemarin yang memenuhi syarat (via step >= 1 km) -> streak tetap 1 sampai akhir hari ini
        val yesterdayOnly = mapOf(
            formatStreakDateKey(yesterday) to DailyStreakActivity(
                dateKey = formatStreakDateKey(yesterday),
                workoutCompleted = false,
                stepDistanceMeters = 1200.0,
            ),
        )
        assertEquals(1, calculateCurrentStreakDays(yesterdayOnly, today))

        // Hari ini belum 1 km & belum workout -> tidak menambah streak
        val todayUnderOneKm = yesterdayOnly + (
            formatStreakDateKey(today) to DailyStreakActivity(
                dateKey = formatStreakDateKey(today),
                workoutCompleted = false,
                stepDistanceMeters = 850.0,
            )
            )
        assertEquals(1, calculateCurrentStreakDays(todayUnderOneKm, today))

        // 3 hari beruntun (twoDaysAgo workout, yesterday >= 1 km, today workout) + jeda sebelum fourDaysAgo
        val threeDayChain = mapOf(
            formatStreakDateKey(fourDaysAgo) to DailyStreakActivity(
                dateKey = formatStreakDateKey(fourDaysAgo),
                workoutCompleted = true,
            ),
            formatStreakDateKey(twoDaysAgo) to DailyStreakActivity(
                dateKey = formatStreakDateKey(twoDaysAgo),
                workoutCompleted = true,
            ),
            formatStreakDateKey(yesterday) to DailyStreakActivity(
                dateKey = formatStreakDateKey(yesterday),
                workoutCompleted = false,
                stepDistanceMeters = 1000.0,
            ),
            formatStreakDateKey(today) to DailyStreakActivity(
                dateKey = formatStreakDateKey(today),
                workoutCompleted = true,
                stepDistanceMeters = 300.0,
            ),
        )
        assertEquals(3, calculateCurrentStreakDays(threeDayChain, today))
        assertEquals(3, calculateLongestStreakDays(threeDayChain))

        // Putus karena terakhir aktivitas 2 hari lalu (kemarin & hari ini kosong) -> 0
        val brokenChain = mapOf(
            formatStreakDateKey(twoDaysAgo) to DailyStreakActivity(
                dateKey = formatStreakDateKey(twoDaysAgo),
                workoutCompleted = true,
            ),
        )
        assertEquals(0, calculateCurrentStreakDays(brokenChain, today))
        assertEquals(1, calculateLongestStreakDays(brokenChain))
    }

    @Test
    fun weeklyIndicatorsMapMondayThroughSundayForCurrentWeek() {
        val wednesday = LocalDate.of(2026, 11, 25) // Rabu
        val monday = LocalDate.of(2026, 11, 23)
        val tuesday = LocalDate.of(2026, 11, 24)
        val previousSunday = LocalDate.of(2026, 11, 22)

        val activities = mapOf(
            formatStreakDateKey(previousSunday) to DailyStreakActivity(
                dateKey = formatStreakDateKey(previousSunday),
                workoutCompleted = true,
            ),
            formatStreakDateKey(monday) to DailyStreakActivity(
                dateKey = formatStreakDateKey(monday),
                workoutCompleted = true,
            ),
            formatStreakDateKey(tuesday) to DailyStreakActivity(
                dateKey = formatStreakDateKey(tuesday),
                workoutCompleted = false,
                stepDistanceMeters = 700.0, // belum 1 km
            ),
            formatStreakDateKey(wednesday) to DailyStreakActivity(
                dateKey = formatStreakDateKey(wednesday),
                workoutCompleted = false,
                stepDistanceMeters = 1500.0, // >= 1 km
            ),
        )

        val week = buildCurrentWeekStreakDays(activities, today = wednesday)
        assertEquals(7, week.size)
        assertEquals(listOf("Sen", "Sel", "Rab", "Kam", "Jum", "Sab", "Min"), week.map { it.label })
        assertEquals(listOf(true, false, true, false, false, false, false), week.map { it.completed })
    }

    @Test
    fun mergeHelpersPreserveOtherSignalAndAccumulateStepDistance() {
        val date = LocalDate.of(2026, 11, 25)

        val firstWalk = mergeStepTrackerDistance(
            existing = null,
            distanceMeters = 600.0,
            date = date,
            mode = StepDistanceUpdateMode.ADD_DELTA,
        )
        assertFalse(firstWalk.isQualified)
        assertEquals(600.0, firstWalk.stepDistanceMeters, 0.001)

        val secondWalk = mergeStepTrackerDistance(
            existing = firstWalk,
            distanceMeters = 450.0,
            date = date,
            mode = StepDistanceUpdateMode.ADD_DELTA,
        )
        assertTrue(secondWalk.isQualified)
        assertEquals(1050.0, secondWalk.stepDistanceMeters, 0.001)

        // Menyelesaikan workout tidak menghapus jarak step tracker yang sudah tercatat
        val afterWorkout = mergeWorkoutCompleted(existing = secondWalk, date = date)
        assertTrue(afterWorkout.workoutCompleted)
        assertEquals(1050.0, afterWorkout.stepDistanceMeters, 0.001)
        assertTrue(afterWorkout.isQualified)

        // SET_DAILY_TOTAL menjaga nilai maksimum agar sync parsial tidak menurunkan jarak
        val lowerSync = mergeStepTrackerDistance(
            existing = afterWorkout,
            distanceMeters = 800.0,
            date = date,
            mode = StepDistanceUpdateMode.SET_DAILY_TOTAL,
        )
        assertTrue(lowerSync.workoutCompleted)
        assertEquals(1050.0, lowerSync.stepDistanceMeters, 0.001)

        val higherSync = mergeStepTrackerDistance(
            existing = afterWorkout,
            distanceMeters = 2300.0,
            date = date,
            mode = StepDistanceUpdateMode.SET_DAILY_TOTAL,
        )
        assertTrue(higherSync.workoutCompleted)
        assertEquals(2300.0, higherSync.stepDistanceMeters, 0.001)
    }

    @Test
    fun firestoreMapRoundTripsAndFiltersInvalidDocuments() {
        val activity = DailyStreakActivity(
            dateKey = "2026-11-25",
            workoutCompleted = false,
            stepDistanceMeters = 1250.5,
        )
        val map = activity.toFirestoreMap(includeServerTimestamp = false)
        assertEquals("2026-11-25", map[FIELD_STREAK_DATE_KEY])
        assertEquals(false, map[FIELD_STREAK_WORKOUT_COMPLETED])
        assertEquals(1250.5, map[FIELD_STREAK_STEP_DISTANCE_METERS])
        assertEquals(true, map[FIELD_STREAK_QUALIFIED])

        val parsed = parseDailyStreakActivityFromMap("2026-11-25", map)
        assertNotNull(parsed)
        assertEquals(activity, parsed)

        // Dokumen dengan dateKey tidak valid harus diabaikan
        assertNull(parseDailyStreakActivityFromMap("invalid-doc-id", mapOf("workoutCompleted" to true)))

        // Nilai jarak negatif disanitasi menjadi 0
        val sanitized = parseDailyStreakActivityFromMap(
            docId = "2026-11-24",
            data = mapOf(
                FIELD_STREAK_WORKOUT_COMPLETED to false,
                FIELD_STREAK_STEP_DISTANCE_METERS to -120.0,
            ),
        )
        assertNotNull(sanitized)
        assertEquals(0.0, sanitized!!.stepDistanceMeters, 0.001)
        assertFalse(sanitized.isQualified)
    }

    @Test
    fun streakStoreIsolatesDataPerAccountAndResetsOnSwitchOrLogout() {
        val today = LocalDate.of(2026, 11, 25)
        val todayKey = formatStreakDateKey(today)

        TrainingStreakStore.bindUser("user_a")
        TrainingStreakStore.applyFromFirestore(
            userId = "user_a",
            activities = mapOf(
                todayKey to DailyStreakActivity(dateKey = todayKey, workoutCompleted = true),
            ),
        )
        assertEquals(1, TrainingStreakStore.summaryFor("user_a", today).currentStreakDays)
        assertEquals(0, TrainingStreakStore.summaryFor("user_b", today).currentStreakDays)

        // Pindah ke akun user_b -> data lama user_a harus ter-reset
        TrainingStreakStore.bindUser("user_b")
        assertEquals(0, TrainingStreakStore.summaryFor("user_b", today).currentStreakDays)

        // Snapshot terlambat milik user_a tidak boleh mencemari user_b
        TrainingStreakStore.applyFromFirestore(
            userId = "user_a",
            activities = mapOf(
                todayKey to DailyStreakActivity(dateKey = todayKey, workoutCompleted = true),
            ),
        )
        assertEquals(0, TrainingStreakStore.summaryFor("user_b", today).currentStreakDays)

        // Logout membersihkan state
        TrainingStreakStore.reset()
        assertNull(TrainingStreakStore.activeUserId)
        assertEquals(0, TrainingStreakStore.summaryFor("user_a", today).currentStreakDays)
    }
}
