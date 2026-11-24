package com.example.olahragakuy.pages.training

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class CustomWorkoutRepositoryTest {

    @Before
    fun setUp() {
        WorkoutFirestoreStore.clear()
        CustomWorkoutStore.clear()
    }

    @After
    fun tearDown() {
        WorkoutFirestoreStore.clear()
        CustomWorkoutStore.clear()
    }

    @Test
    fun groupExercisesByCategoryGroupsFirestoreExercisesInOrder() {
        val exercises = listOf(
            WorkoutExerciseDetail(
                id = "PUSH_UP",
                type = ExerciseType.PUSH_UP,
                name = "Push Up",
                category = "Latihan Tangan",
                order = 2,
                steps = listOf("Langkah 1"),
            ),
            WorkoutExerciseDetail(
                id = "PLANK_LURUS",
                type = ExerciseType.PLANK_LURUS,
                name = "Plank Lurus",
                category = "Latihan Tangan",
                order = 1,
                steps = listOf("Langkah 1"),
            ),
            WorkoutExerciseDetail(
                id = "SQUAT",
                type = ExerciseType.SQUAT,
                name = "Squat",
                category = "Latihan Kaki",
                order = 3,
                steps = listOf("Langkah 1"),
            ),
            WorkoutExerciseDetail(
                id = "CUSTOM_MOVE",
                type = ExerciseType.SIT_UP,
                name = "Gerakan Baru",
                category = "",
                order = 4,
                steps = listOf("Langkah 1"),
            ),
        )

        val grouped = groupExercisesByCategory(exercises)

        assertEquals(3, grouped.size)
        assertEquals("Latihan Tangan", grouped[0].first)
        assertEquals(listOf("PLANK_LURUS", "PUSH_UP"), grouped[0].second.map { it.id })
        assertEquals("Latihan Kaki", grouped[1].first)
        assertEquals(listOf("SQUAT"), grouped[1].second.map { it.id })
        assertEquals("Latihan Umum", grouped[2].first)
        assertEquals(listOf("CUSTOM_MOVE"), grouped[2].second.map { it.id })
    }

    @Test
    fun customWorkoutRoundTripsThroughFirestoreMap() {
        val exercises = WorkoutCatalogSeeder.buildSeedExercises().take(3)
        WorkoutFirestoreStore.setExercisesFromFirestore(exercises)

        val plan = buildCustomWorkoutPlan(
            id = "cw_doc_1",
            name = "Latihan Bg Zahran",
            selectedExercises = exercises,
        )

        assertEquals("Latihan Bg Zahran", plan.name)
        assertEquals(CUSTOM_WORKOUT_DIFFICULTY_LABEL, plan.difficultyLabel)
        assertEquals(3, plan.exercises.size)
        assertTrue(plan.calories >= 10)
        assertTrue(plan.durationLabel.contains("-"))

        val map = plan.toFirestoreMap(includeServerTimestamps = false)
        val parsed = parseCustomWorkoutFromMap(plan.id, map)

        assertNotNull(parsed)
        assertEquals(plan.id, parsed?.id)
        assertEquals(plan.name, parsed?.name)
        assertEquals(plan.calories, parsed?.calories)
        assertEquals(plan.durationLabel, parsed?.durationLabel)
        assertEquals(CUSTOM_WORKOUT_DIFFICULTY_LABEL, parsed?.difficultyLabel)
        assertEquals(3, parsed?.exercises?.size)
        assertEquals(exercises.map { it.id }, parsed?.exercises?.map { it.exerciseId })
    }

    @Test
    fun malformedCustomWorkoutDocumentReturnsNull() {
        val missingName = parseCustomWorkoutFromMap(
            docId = "doc1",
            data = mapOf(
                "exercises" to listOf(
                    mapOf("id" to "s1", "exerciseId" to "SIT_UP", "mode" to "REPETITIONS", "targetValue" to 12),
                ),
            ),
        )
        assertNull(missingName)

        val emptyExercises = parseCustomWorkoutFromMap(
            docId = "doc2",
            data = mapOf(
                "name" to "Latihan Kosong",
                "exercises" to emptyList<Any>(),
            ),
        )
        assertNull(emptyExercises)
    }

    @Test
    fun customWorkoutStoreSortsNewestFirstAndFindsById() {
        val exercises = WorkoutCatalogSeeder.buildSeedExercises().take(2)
        val older = buildCustomWorkoutPlan(
            id = "old_1",
            name = "Latihan Lama",
            selectedExercises = exercises,
            createdAtMillis = 1000L,
        )
        val newer = buildCustomWorkoutPlan(
            id = "new_1",
            name = "Latihan Raja Gym",
            selectedExercises = exercises,
            createdAtMillis = 2000L,
        )

        CustomWorkoutStore.setWorkoutsFromFirestore(listOf(older, newer))

        assertEquals(listOf("new_1", "old_1"), CustomWorkoutStore.workouts.map { it.id })
        assertEquals("Latihan Raja Gym", CustomWorkoutStore.workoutById("new_1")?.name)
        assertNull(CustomWorkoutStore.workoutById("tidak_ada"))
    }
}
