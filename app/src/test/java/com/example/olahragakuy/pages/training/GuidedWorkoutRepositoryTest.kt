package com.example.olahragakuy.pages.training

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GuidedWorkoutRepositoryTest {

    @Before
    fun setUp() {
        WorkoutFirestoreStore.clear()
    }

    @After
    fun tearDown() {
        WorkoutFirestoreStore.clear()
    }

    @Test
    fun workoutExerciseRoundTripsThroughFirestoreMap() {
        val sitUp = WorkoutCatalogSeeder.buildSeedExercises().first { it.type == ExerciseType.SIT_UP }
        val firestoreMap = sitUp.toFirestoreMap(includeServerTimestamp = false)
        val parsed = parseWorkoutExerciseFromMap(sitUp.id, firestoreMap)

        assertNotNull(parsed)
        assertEquals(sitUp.id, parsed?.id)
        assertEquals(sitUp.name, parsed?.name)
        assertEquals(sitUp.description, parsed?.description)
        assertEquals(sitUp.category, parsed?.category)
        assertEquals(sitUp.type, parsed?.type)
        assertEquals(sitUp.defaultTarget, parsed?.defaultTarget)
        assertEquals(sitUp.steps, parsed?.steps)
        assertEquals(sitUp.order, parsed?.order)
    }

    @Test
    fun guidedWorkoutDayRoundTripsThroughFirestoreMap() {
        val dayOne = WorkoutCatalogSeeder.buildSeedGuidedDays().first()
        val docId = guidedWorkoutDayDocId(dayOne.dayNumber)
        assertEquals("day_01", docId)

        val firestoreMap = dayOne.toFirestoreMap(includeServerTimestamp = false)
        val parsed = parseGuidedWorkoutDayFromMap(docId, firestoreMap)

        assertNotNull(parsed)
        assertEquals(1, parsed?.dayNumber)
        assertEquals("Hari 1", parsed?.title)
        assertEquals(dayOne.description, parsed?.description)
        assertEquals(43, parsed?.calories)
        assertEquals("5-7", parsed?.durationLabel)
        assertEquals("Mudah", parsed?.difficultyLabel)
        assertEquals(5, parsed?.exercises?.size)
        assertEquals(WorkoutTarget.Repetitions(12), parsed?.exercises?.get(0)?.target)
        assertEquals(WorkoutTarget.Duration(30), parsed?.exercises?.get(1)?.target)
    }

    @Test
    fun inactiveOrMalformedFirestoreDocumentsAreFilteredOut() {
        val inactiveExercise = parseWorkoutExerciseFromMap(
            docId = "SIT_UP",
            data = mapOf(
                "id" to "SIT_UP",
                "name" to "Sit Up",
                "steps" to listOf("Langkah 1"),
                "isActive" to false,
            ),
        )
        assertNull(inactiveExercise)

        val missingStepsExercise = parseWorkoutExerciseFromMap(
            docId = "SIT_UP",
            data = mapOf(
                "id" to "SIT_UP",
                "name" to "Sit Up",
                "isActive" to true,
            ),
        )
        assertNull(missingStepsExercise)

        val inactiveDay = parseGuidedWorkoutDayFromMap(
            docId = "day_01",
            data = mapOf(
                "dayNumber" to 1,
                "isActive" to false,
                "exercises" to listOf(
                    mapOf("id" to "d1_1", "exerciseId" to "SIT_UP", "mode" to "REPETITIONS", "targetValue" to 12),
                ),
            ),
        )
        assertNull(inactiveDay)
    }

    @Test
    fun firestoreStoreReflectsOnlyDocumentsLoadedFromFirestore() {
        assertNull(WorkoutFirestoreStore.exerciseOrNull("SIT_UP"))
        assertNull(WorkoutFirestoreStore.dayPlanOrNull(1))

        val remoteExercise = WorkoutExerciseDetail(
            id = "SIT_UP",
            type = ExerciseType.SIT_UP,
            name = "Sit Up Pemula (Firestore)",
            description = "Deskripsi langsung dari Firestore.",
            category = "Otot Perut",
            defaultTarget = WorkoutTarget.Repetitions(15),
            order = 1,
            steps = listOf("Langkah dari Firestore."),
        )
        val remoteDay = WorkoutCatalogSeeder.buildSeedGuidedDays().first()

        WorkoutFirestoreStore.setExercisesFromFirestore(listOf(remoteExercise))
        WorkoutFirestoreStore.setDaysFromFirestore(listOf(remoteDay))

        assertEquals(1, WorkoutFirestoreStore.allExercises().size)
        assertEquals("Sit Up Pemula (Firestore)", WorkoutFirestoreStore.exerciseOrNull("SIT_UP")?.name)
        assertEquals("Deskripsi langsung dari Firestore.", WorkoutFirestoreStore.exerciseOrNull("SIT_UP")?.description)
        assertEquals(1, WorkoutFirestoreStore.allDays().size)
        assertNotNull(WorkoutFirestoreStore.dayPlanOrNull(1))
        assertNull(WorkoutFirestoreStore.dayPlanOrNull(2))
    }

    @Test
    fun guidedProgressRoundTripsThroughUserCollectionFields() {
        val progress = GuidedWorkoutProgress(
            highestUnlockedDay = 3,
            completedDays = setOf(1, 2),
        )
        val userDocFields = progress.toUserFieldsMap(includeServerTimestamp = false)
        assertEquals(3, userDocFields[FIELD_GUIDED_HIGHEST_DAY])
        assertEquals(listOf(1, 2), userDocFields[FIELD_GUIDED_COMPLETED_DAYS])

        val parsed = parseGuidedWorkoutProgressFromMap(userDocFields)
        assertEquals(3, parsed.highestUnlockedDay)
        assertEquals(setOf(1, 2), parsed.completedDays)

        val sanitized = parseGuidedWorkoutProgressFromMap(
            mapOf(
                FIELD_GUIDED_HIGHEST_DAY to 1,
                FIELD_GUIDED_COMPLETED_DAYS to listOf(0, 1, 4, 99),
            ),
        )
        assertEquals(setOf(1, 4), sanitized.completedDays)
        assertEquals(5, sanitized.highestUnlockedDay)

        // Dokumen users/{uid} baru tanpa field progres -> default Hari 1 terbuka & belum ada centang
        val freshAccount = parseGuidedWorkoutProgressFromMap(
            mapOf("username" to "akun_baru", "displayName" to "Akun Baru"),
        )
        assertEquals(1, freshAccount.highestUnlockedDay)
        assertEquals(emptySet<Int>(), freshAccount.completedDays)
    }
}
