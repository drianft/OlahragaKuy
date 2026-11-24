package com.example.olahragakuy.pages.training

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions

const val WORKOUT_EXERCISES_COLLECTION = "workout_exercises"
const val GUIDED_WORKOUT_DAYS_COLLECTION = "guided_workout_days"
const val FIELD_GUIDED_HIGHEST_DAY = "guidedHighestUnlockedDay"
const val FIELD_GUIDED_COMPLETED_DAYS = "guidedCompletedDays"

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

fun GuidedWorkoutProgress.toUserFieldsMap(includeServerTimestamp: Boolean = false): Map<String, Any> {
    val sanitizedDays = completedDays
        .filter { it in 1..GUIDED_WORKOUT_TOTAL_DAYS }
        .distinct()
        .sorted()
    val highestFromCompleted = sanitizedDays.maxOrNull()
        ?.let { (it + 1).coerceAtMost(GUIDED_WORKOUT_TOTAL_DAYS) }
        ?: 1
    val safeHighest = maxOf(
        highestUnlockedDay.coerceIn(1, GUIDED_WORKOUT_TOTAL_DAYS),
        highestFromCompleted,
    )
    val map = linkedMapOf<String, Any>(
        FIELD_GUIDED_HIGHEST_DAY to safeHighest,
        FIELD_GUIDED_COMPLETED_DAYS to sanitizedDays,
    )
    if (includeServerTimestamp) {
        map["updatedAt"] = FieldValue.serverTimestamp()
    }
    return map
}

fun parseGuidedWorkoutProgressFromMap(data: Map<String, Any?>): GuidedWorkoutProgress {
    val rawCompleted = (data[FIELD_GUIDED_COMPLETED_DAYS] as? List<*>)
        ?: (data["completedDays"] as? List<*>)
    val completedDays = rawCompleted
        .orEmpty()
        .mapNotNull { (it as? Number)?.toInt() }
        .filter { it in 1..GUIDED_WORKOUT_TOTAL_DAYS }
        .toSet()
    val highestFromCompleted = completedDays.maxOrNull()
        ?.let { (it + 1).coerceAtMost(GUIDED_WORKOUT_TOTAL_DAYS) }
        ?: 1
    val rawHighest = (data[FIELD_GUIDED_HIGHEST_DAY] as? Number)?.toInt()
        ?: (data["highestUnlockedDay"] as? Number)?.toInt()
        ?: 1
    val safeHighest = maxOf(
        rawHighest.coerceIn(1, GUIDED_WORKOUT_TOTAL_DAYS),
        highestFromCompleted,
    )
    return GuidedWorkoutProgress(
        highestUnlockedDay = safeHighest,
        completedDays = completedDays,
    )
}

/**
 * Mengamati progres Latihan Terarah khusus untuk akun [userId] langsung dari dokumen
 * koleksi `users/{userId}`.
 */
fun observeUserGuidedProgress(
    userId: String,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
): ListenerRegistration {
    val cleanUserId = userId.trim()
    if (cleanUserId.isEmpty()) {
        GuidedWorkoutProgressStore.reset()
        return ListenerRegistration { }
    }

    GuidedWorkoutProgressStore.bindUser(cleanUserId)
    return try {
        firestore.collection("users")
            .document(cleanUserId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    GuidedWorkoutProgressStore.setError(
                        cleanUserId,
                        error.localizedMessage ?: "Gagal memuat progres latihan dari Firestore.",
                    )
                    return@addSnapshotListener
                }
                val progress = if (snapshot != null && snapshot.exists()) {
                    parseGuidedWorkoutProgressFromMap(snapshot.data.orEmpty())
                } else {
                    GuidedWorkoutProgress()
                }
                GuidedWorkoutProgressStore.applyFromFirestore(cleanUserId, progress)
            }
    } catch (e: Exception) {
        GuidedWorkoutProgressStore.setError(
            cleanUserId,
            e.localizedMessage ?: "Gagal terhubung ke Firestore.",
        )
        ListenerRegistration { }
    }
}

/**
 * Menyimpan penyelesaian hari Latihan Terarah langsung ke dokumen akun `users/{userId}`.
 */
fun completeGuidedWorkoutDayForUser(
    userId: String,
    dayNumber: Int,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onResult: (Boolean, String?) -> Unit = { _, _ -> },
) {
    val cleanUserId = userId.trim()
    if (cleanUserId.isEmpty()) {
        onResult(false, "Akun belum login.")
        return
    }
    if (GuidedWorkoutProgressStore.activeUserId != cleanUserId) {
        GuidedWorkoutProgressStore.bindUser(cleanUserId)
    }
    val updated = GuidedWorkoutProgressStore.completeDay(dayNumber)
    try {
        firestore.collection("users")
            .document(cleanUserId)
            .set(updated.toUserFieldsMap(includeServerTimestamp = true), SetOptions.merge())
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { err ->
                onResult(false, err.localizedMessage ?: "Gagal menyimpan progres latihan.")
            }
    } catch (e: Exception) {
        onResult(false, e.localizedMessage ?: "Gagal menyimpan progres latihan.")
    }
}
