package com.example.olahragakuy.pages.training

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class GuidedWorkoutModelsTest {

    @Before
    fun setUp() {
        GuidedWorkoutProgressStore.reset()
        WorkoutFirestoreStore.clear()
    }

    @After
    fun tearDown() {
        WorkoutFirestoreStore.clear()
    }

    @Test
    fun firestoreStoreStartsEmptyWithoutHardcodedFallback() {
        assertTrue(WorkoutFirestoreStore.allExercises().isEmpty())
        assertTrue(WorkoutFirestoreStore.allDays().isEmpty())
        assertNull(WorkoutFirestoreStore.dayPlanOrNull(1))
        assertNull(WorkoutFirestoreStore.exerciseOrNull("SIT_UP"))
    }

    @Test
    fun seedDayOneMatchesExpectedPlan() {
        val dayOne = WorkoutCatalogSeeder.buildSeedGuidedDays().first()

        assertEquals(1, dayOne.dayNumber)
        assertEquals(43, dayOne.calories)
        assertEquals("5-7", dayOne.durationLabel)
        assertEquals("Mudah", dayOne.difficultyLabel)
        assertEquals(5, dayOne.exercises.size)
        assertEquals(
            listOf(
                ExerciseType.SIT_UP to "12 kali",
                ExerciseType.PLANK_LURUS to "00:30 detik",
                ExerciseType.SQUAT to "12 kali",
                ExerciseType.SIT_UP to "12 kali",
                ExerciseType.PLANK_LURUS to "00:30 detik",
            ),
            dayOne.exercises.map { it.exerciseType to it.targetLabel },
        )
    }

    @Test
    fun allThirtySeedDaysReferenceValidSeedExercises() {
        val exercisesById = WorkoutCatalogSeeder.buildSeedExercises().associateBy { it.id }
        val days = WorkoutCatalogSeeder.buildSeedGuidedDays()

        assertEquals(GUIDED_WORKOUT_TOTAL_DAYS, days.size)
        days.forEachIndexed { index, plan ->
            assertEquals(index + 1, plan.dayNumber)
            assertTrue("Hari ${plan.dayNumber} harus memiliki daftar gerakan", plan.exercises.isNotEmpty())
            plan.exercises.forEach { step ->
                val detail = exercisesById[step.exerciseId]
                assertTrue("${step.exerciseId} harus ada di daftar seed", detail != null)
                assertTrue("${step.exerciseId} harus memiliki panduan langkah", detail!!.steps.isNotEmpty())
            }
        }
    }

    @Test
    fun completingDayUnlocksNextDayOnlyUpToThirty() {
        assertEquals(
            GuidedDayStatus.UNLOCKED,
            resolveGuidedDayStatus(1, GuidedWorkoutProgressStore.highestUnlockedDay, GuidedWorkoutProgressStore.completedDays),
        )
        assertEquals(
            GuidedDayStatus.LOCKED,
            resolveGuidedDayStatus(2, GuidedWorkoutProgressStore.highestUnlockedDay, GuidedWorkoutProgressStore.completedDays),
        )

        GuidedWorkoutProgressStore.completeDay(1)

        assertEquals(2, GuidedWorkoutProgressStore.highestUnlockedDay)
        assertEquals(
            GuidedDayStatus.COMPLETED,
            resolveGuidedDayStatus(1, GuidedWorkoutProgressStore.highestUnlockedDay, GuidedWorkoutProgressStore.completedDays),
        )
        assertEquals(
            GuidedDayStatus.UNLOCKED,
            resolveGuidedDayStatus(2, GuidedWorkoutProgressStore.highestUnlockedDay, GuidedWorkoutProgressStore.completedDays),
        )
        assertEquals(
            GuidedDayStatus.LOCKED,
            resolveGuidedDayStatus(3, GuidedWorkoutProgressStore.highestUnlockedDay, GuidedWorkoutProgressStore.completedDays),
        )

        GuidedWorkoutProgressStore.completeDay(30)
        assertEquals(30, GuidedWorkoutProgressStore.highestUnlockedDay)
    }

    @Test
    fun workoutTargetsDistinguishRepetitionsAndDuration() {
        val dayOne = WorkoutCatalogSeeder.buildSeedGuidedDays().first()
        val firstStep = dayOne.exercises[0]
        val secondStep = dayOne.exercises[1]

        assertEquals(WorkoutTarget.Repetitions(12), firstStep.target)
        assertEquals("12 kali", firstStep.targetLabel)

        assertEquals(WorkoutTarget.Duration(30), secondStep.target)
        assertEquals("00:30 detik", secondStep.targetLabel)
        assertEquals("00:30", formatDurationClock(30))
        assertEquals("01:05", formatDurationClock(65))
        assertEquals("00:00", formatDurationClock(-5))
    }

    @Test
    fun switchingAccountResetsProgressAndIgnoresStaleCallbacks() {
        GuidedWorkoutProgressStore.bindUser("user_A")
        GuidedWorkoutProgressStore.completeDay(1)
        GuidedWorkoutProgressStore.completeDay(2)

        assertEquals("user_A", GuidedWorkoutProgressStore.activeUserId)
        assertEquals(3, GuidedWorkoutProgressStore.highestUnlockedDay)
        assertEquals(setOf(1, 2), GuidedWorkoutProgressStore.completedDays)

        // Ganti ke akun lain: progres akun sebelumnya tidak boleh terbawa
        GuidedWorkoutProgressStore.bindUser("user_B")

        assertEquals("user_B", GuidedWorkoutProgressStore.activeUserId)
        assertEquals(1, GuidedWorkoutProgressStore.highestUnlockedDay)
        assertTrue(GuidedWorkoutProgressStore.completedDays.isEmpty())

        // Callback terlambat dari user_A harus diabaikan
        GuidedWorkoutProgressStore.applyFromFirestore(
            userId = "user_A",
            progress = GuidedWorkoutProgress(highestUnlockedDay = 5, completedDays = setOf(1, 2, 3, 4)),
        )
        assertEquals(1, GuidedWorkoutProgressStore.highestUnlockedDay)
        assertTrue(GuidedWorkoutProgressStore.completedDays.isEmpty())

        // Callback untuk user_B diterapkan dengan benar
        GuidedWorkoutProgressStore.applyFromFirestore(
            userId = "user_B",
            progress = GuidedWorkoutProgress(highestUnlockedDay = 2, completedDays = setOf(1)),
        )
        assertEquals(2, GuidedWorkoutProgressStore.highestUnlockedDay)
        assertEquals(setOf(1), GuidedWorkoutProgressStore.completedDays)
    }
}
