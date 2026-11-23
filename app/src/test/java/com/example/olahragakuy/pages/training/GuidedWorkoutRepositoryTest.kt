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
}
