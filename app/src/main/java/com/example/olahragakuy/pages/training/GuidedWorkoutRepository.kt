package com.example.olahragakuy.pages.training

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration

const val WORKOUT_EXERCISES_COLLECTION = "workout_exercises"
const val GUIDED_WORKOUT_DAYS_COLLECTION = "guided_workout_days"

fun guidedWorkoutDayDocId(dayNumber: Int): String =
    "day_%02d".format(dayNumber.coerceIn(1, GUIDED_WORKOUT_TOTAL_DAYS))

fun WorkoutExerciseDetail.toFirestoreMap(includeServerTimestamp: Boolean = false): Map<String, Any> {
    val map = linkedMapOf<String, Any>(
        "id" to id,
        "name" to name,
        "description" to description,
        "category" to category,
        "illustrationKey" to type.name,
        "defaultMode" to defaultTarget.modeKey,
        "defaultTargetValue" to defaultTarget.numericValue,
        "steps" to steps,
        "order" to order,
        "isActive" to true,
    )
    if (includeServerTimestamp) {
        map["updatedAt"] = FieldValue.serverTimestamp()
    }
    return map
}

fun GuidedWorkoutDay.toFirestoreMap(includeServerTimestamp: Boolean = false): Map<String, Any> {
    val map = linkedMapOf<String, Any>(
        "dayNumber" to dayNumber,
        "title" to title,
        "description" to description,
        "calories" to calories,
        "durationLabel" to durationLabel,
        "difficultyLabel" to difficultyLabel,
        "exercises" to exercises.map { step ->
            mapOf(
                "id" to step.id,
                "exerciseId" to step.exerciseId,
                "mode" to step.target.modeKey,
                "targetValue" to step.target.numericValue,
            )
        },
        "isActive" to true,
    )
    if (includeServerTimestamp) {
        map["updatedAt"] = FieldValue.serverTimestamp()
    }
    return map
}

fun parseWorkoutExerciseFromMap(
    docId: String,
    data: Map<String, Any?>,
): WorkoutExerciseDetail? {
    val isActive = (data["isActive"] as? Boolean) ?: true
    if (!isActive) return null

    val id = (data["id"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: docId.trim().takeIf { it.isNotEmpty() }
        ?: return null
    val name = (data["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val steps = (data["steps"] as? List<*>)
        .orEmpty()
        .mapNotNull { (it as? String)?.trim()?.takeIf { line -> line.isNotEmpty() } }
    if (steps.isEmpty()) return null

    val illustrationRaw = (data["illustrationKey"] as? String)?.trim().orEmpty()
    val resolvedType = resolveExerciseType(illustrationRaw.ifEmpty { id })
    val defaultMode = (data["defaultMode"] as? String)?.trim()
    val defaultValue = (data["defaultTargetValue"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 12
    val order = (data["order"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 999

    return WorkoutExerciseDetail(
        id = id,
        type = resolvedType,
        name = name,
        description = (data["description"] as? String)?.trim().orEmpty(),
        category = (data["category"] as? String)?.trim().orEmpty(),
        defaultTarget = WorkoutTarget.fromRaw(defaultMode, defaultValue),
        order = order,
        steps = steps,
    )
}

fun parseGuidedWorkoutDayFromMap(
    docId: String,
    data: Map<String, Any?>,
): GuidedWorkoutDay? {
    val isActive = (data["isActive"] as? Boolean) ?: true
    if (!isActive) return null

    val dayNumberFromDoc = docId.removePrefix("day_").toIntOrNull()
    val dayNumber = (data["dayNumber"] as? Number)?.toInt() ?: dayNumberFromDoc ?: return null
    if (dayNumber !in (1..GUIDED_WORKOUT_TOTAL_DAYS)) return null

    val rawExercises = (data["exercises"] as? List<*>).orEmpty()
    val parsedSteps = rawExercises.mapIndexedNotNull { index, entry ->
        val item = entry as? Map<*, *> ?: return@mapIndexedNotNull null
        val exerciseId = (item["exerciseId"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: return@mapIndexedNotNull null
        val stepId = (item["id"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: "d${dayNumber}_${index + 1}"
        val mode = (item["mode"] as? String)?.trim()
        val targetValue = (item["targetValue"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 12
        val exerciseType = WorkoutFirestoreStore.exerciseOrNull(exerciseId)?.type
            ?: resolveExerciseType(exerciseId)
        GuidedWorkoutStep(
            id = stepId,
            exerciseId = exerciseId,
            exerciseType = exerciseType,
            target = WorkoutTarget.fromRaw(mode, targetValue),
        )
    }
    if (parsedSteps.isEmpty()) return null

    val calories = (data["calories"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 40
    val durationLabel = (data["durationLabel"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: "5-7"
    val difficultyLabel = (data["difficultyLabel"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: "Mudah"
    val title = (data["title"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: "Hari $dayNumber"
    val description = (data["description"] as? String)?.trim().orEmpty()

    return GuidedWorkoutDay(
        dayNumber = dayNumber,
        title = title,
        description = description,
        calories = calories,
        durationLabel = durationLabel,
        difficultyLabel = difficultyLabel,
        exercises = parsedSteps,
    )
}

/**
 * Mengamati koleksi `workout_exercises` dan `guided_workout_days` secara real-time
 * langsung dari Firestore tanpa fallback lokal.
 *
 * Jika [autoSeedIfEmpty] aktif dan koleksi di Firestore masih kosong, seeding otomatis
 * dijalankan sekali agar data latihan langsung masuk ke Firestore lalu dibaca kembali
 * lewat snapshot listener.
 */
fun observeWorkoutFirestoreData(
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    autoSeedIfEmpty: Boolean = true,
): ListenerRegistration {
    WorkoutFirestoreStore.markLoading()
    val registrations = mutableListOf<ListenerRegistration>()
    var autoSeedTriggered = false

    fun triggerAutoSeedOnceIfNeeded() {
        if (!autoSeedIfEmpty || autoSeedTriggered) return
        val finishedLoading = !WorkoutFirestoreStore.isLoading
        val needsSeed = WorkoutFirestoreStore.exercisesById.isEmpty() ||
            WorkoutFirestoreStore.daysByNumber.isEmpty()
        if (finishedLoading && needsSeed) {
            autoSeedTriggered = true
            WorkoutCatalogSeeder.seedToFirestore(
                firestore = firestore,
                onlyIfEmpty = true,
            )
        }
    }

    try {
        registrations += firestore.collection(WORKOUT_EXERCISES_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    WorkoutFirestoreStore.setError(
                        error.localizedMessage ?: "Gagal memuat workout_exercises dari Firestore.",
                    )
                    return@addSnapshotListener
                }
                val parsed = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    parseWorkoutExerciseFromMap(doc.id, doc.data.orEmpty())
                }
                WorkoutFirestoreStore.setExercisesFromFirestore(parsed)
                triggerAutoSeedOnceIfNeeded()
            }
    } catch (e: Exception) {
        WorkoutFirestoreStore.setError(e.localizedMessage ?: "Gagal terhubung ke Firestore.")
    }

    try {
        registrations += firestore.collection(GUIDED_WORKOUT_DAYS_COLLECTION)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    WorkoutFirestoreStore.setError(
                        error.localizedMessage ?: "Gagal memuat guided_workout_days dari Firestore.",
                    )
                    return@addSnapshotListener
                }
                val parsed = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    parseGuidedWorkoutDayFromMap(doc.id, doc.data.orEmpty())
                }
                WorkoutFirestoreStore.setDaysFromFirestore(parsed)
                triggerAutoSeedOnceIfNeeded()
            }
    } catch (e: Exception) {
        WorkoutFirestoreStore.setError(e.localizedMessage ?: "Gagal terhubung ke Firestore.")
    }

    return ListenerRegistration {
        registrations.forEach { reg ->
            try {
                reg.remove()
            } catch (_: Exception) {
            }
        }
    }
}
