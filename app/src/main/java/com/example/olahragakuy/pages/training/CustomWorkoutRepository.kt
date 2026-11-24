package com.example.olahragakuy.pages.training

import com.example.olahragakuy.ui.components.RefreshRegistry
import com.example.olahragakuy.ui.components.addRefreshableSnapshotListener

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.util.Date

const val CUSTOM_WORKOUTS_SUBCOLLECTION = "custom_workouts"
const val CUSTOM_WORKOUT_NAME_MAX_LENGTH = 30
const val CUSTOM_WORKOUT_DIFFICULTY_LABEL = "Kostumisasi"

data class CustomWorkoutPlan(
    val id: String,
    val name: String,
    val calories: Int,
    val durationLabel: String,
    val difficultyLabel: String = CUSTOM_WORKOUT_DIFFICULTY_LABEL,
    val exercises: List<GuidedWorkoutStep>,
    val createdAtMillis: Long = 0L,
)

data class CustomWorkoutEstimate(
    val calories: Int,
    val durationLabel: String,
)

/**
 * Menghitung estimasi kalori dan durasi (rentang menit) dari daftar gerakan yang dipilih.
 */
fun estimateCustomWorkoutStats(steps: List<GuidedWorkoutStep>): CustomWorkoutEstimate {
    if (steps.isEmpty()) {
        return CustomWorkoutEstimate(calories = 10, durationLabel = "1-3")
    }
    val calories = steps.sumOf { step ->
        when (val target = step.target) {
            is WorkoutTarget.Repetitions -> (target.count * 0.75).toInt().coerceAtLeast(6)
            is WorkoutTarget.Duration -> (target.seconds * 0.30).toInt().coerceAtLeast(7)
        }
    }.coerceIn(10, 5000)

    val totalSeconds = steps.sumOf { step ->
        when (val target = step.target) {
            is WorkoutTarget.Repetitions -> (target.count * 3) + 20
            is WorkoutTarget.Duration -> target.seconds + 20
        }
    }
    val minMinutes = ((totalSeconds + 59) / 60).coerceAtLeast(1)
    val maxMinutes = minMinutes + 2
    return CustomWorkoutEstimate(
        calories = calories,
        durationLabel = "$minMinutes-$maxMinutes",
    )
}

/**
 * Mengelompokkan gerakan yang berasal dari Firestore berdasarkan kategorinya
 * sambil mempertahankan urutan (`order`) masing-masing latihan.
 */
fun groupExercisesByCategory(
    exercises: List<WorkoutExerciseDetail>,
): List<Pair<String, List<WorkoutExerciseDetail>>> {
    if (exercises.isEmpty()) return emptyList()
    val grouped = linkedMapOf<String, MutableList<WorkoutExerciseDetail>>()
    exercises.sortedBy { it.order }.forEach { exercise ->
        val categoryLabel = exercise.category.trim().ifBlank { "Latihan Umum" }
        grouped.getOrPut(categoryLabel) { mutableListOf() }.add(exercise)
    }
    return grouped.entries.map { it.key to it.value.toList() }
}

fun buildCustomWorkoutPlan(
    id: String,
    name: String,
    selectedExercises: List<WorkoutExerciseDetail>,
    createdAtMillis: Long = 0L,
): CustomWorkoutPlan {
    val cleanName = name.trim().take(CUSTOM_WORKOUT_NAME_MAX_LENGTH)
    val steps = selectedExercises.mapIndexed { index, exercise ->
        GuidedWorkoutStep(
            id = "cw_${index + 1}_${exercise.id}",
            exerciseId = exercise.id,
            exerciseType = exercise.type,
            target = exercise.defaultTarget,
        )
    }
    val estimate = estimateCustomWorkoutStats(steps)
    return CustomWorkoutPlan(
        id = id,
        name = cleanName,
        calories = estimate.calories,
        durationLabel = estimate.durationLabel,
        difficultyLabel = CUSTOM_WORKOUT_DIFFICULTY_LABEL,
        exercises = steps,
        createdAtMillis = createdAtMillis,
    )
}

fun CustomWorkoutPlan.toFirestoreMap(includeServerTimestamps: Boolean = false): Map<String, Any> {
    val map = linkedMapOf<String, Any>(
        "name" to name,
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
    )
    if (includeServerTimestamps) {
        map["createdAt"] = FieldValue.serverTimestamp()
        map["updatedAt"] = FieldValue.serverTimestamp()
    }
    return map
}

fun parseCustomWorkoutFromMap(
    docId: String,
    data: Map<String, Any?>,
    exerciseLookup: (String) -> WorkoutExerciseDetail? = WorkoutFirestoreStore::exerciseOrNull,
): CustomWorkoutPlan? {
    val id = docId.trim().takeIf { it.isNotEmpty() } ?: return null
    val name = (data["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    val rawExercises = (data["exercises"] as? List<*>).orEmpty()
    val parsedSteps = rawExercises.mapIndexedNotNull { index, entry ->
        val item = entry as? Map<*, *> ?: return@mapIndexedNotNull null
        val exerciseId = (item["exerciseId"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: return@mapIndexedNotNull null
        val stepId = (item["id"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
            ?: "cw_${index + 1}_$exerciseId"
        val mode = (item["mode"] as? String)?.trim()
        val targetValue = (item["targetValue"] as? Number)?.toInt()?.coerceAtLeast(1) ?: 12
        val exerciseType = exerciseLookup(exerciseId)?.type ?: resolveExerciseType(exerciseId)
        GuidedWorkoutStep(
            id = stepId,
            exerciseId = exerciseId,
            exerciseType = exerciseType,
            target = WorkoutTarget.fromRaw(mode, targetValue),
        )
    }
    if (parsedSteps.isEmpty()) return null

    val fallbackEstimate = estimateCustomWorkoutStats(parsedSteps)
    val calories = (data["calories"] as? Number)?.toInt()?.coerceAtLeast(1) ?: fallbackEstimate.calories
    val durationLabel = (data["durationLabel"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: fallbackEstimate.durationLabel
    val difficultyLabel = (data["difficultyLabel"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: CUSTOM_WORKOUT_DIFFICULTY_LABEL
    val createdAtMillis = when (val raw = data["createdAt"]) {
        is Timestamp -> raw.toDate().time
        is Date -> raw.time
        is Number -> raw.toLong()
        else -> 0L
    }

    return CustomWorkoutPlan(
        id = id,
        name = name,
        calories = calories,
        durationLabel = durationLabel,
        difficultyLabel = difficultyLabel,
        exercises = parsedSteps,
        createdAtMillis = createdAtMillis,
    )
}

/**
 * Penyimpanan reaktif murni dari koleksi `users/{uid}/custom_workouts` di Firestore.
 */
object CustomWorkoutStore {
    var workouts by mutableStateOf<List<CustomWorkoutPlan>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var isSaving by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun workoutById(workoutId: String): CustomWorkoutPlan? =
        workouts.firstOrNull { it.id == workoutId }

    fun setWorkoutsFromFirestore(list: List<CustomWorkoutPlan>) {
        workouts = list.sortedWith(
            compareByDescending<CustomWorkoutPlan> { it.createdAtMillis }
                .thenBy { it.name.lowercase() },
        )
        isLoading = false
        errorMessage = null
    }

    fun markLoading() {
        isLoading = true
        errorMessage = null
    }

    fun markSaving(saving: Boolean) {
        isSaving = saving
    }

    fun setError(message: String) {
        isLoading = false
        isSaving = false
        errorMessage = message
    }

    fun clear() {
        workouts = emptyList()
        isLoading = false
        isSaving = false
        errorMessage = null
    }
}

fun observeUserCustomWorkouts(
    userId: String,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    refreshRegistry: RefreshRegistry? = null,
): ListenerRegistration {
    if (userId.isBlank()) {
        CustomWorkoutStore.clear()
        return ListenerRegistration { }
    }

    CustomWorkoutStore.markLoading()
    return try {
        firestore.collection("users")
            .document(userId)
            .collection(CUSTOM_WORKOUTS_SUBCOLLECTION)
            .addRefreshableSnapshotListener(refreshRegistry) { snapshot, error ->
                if (error != null) {
                    CustomWorkoutStore.setError(
                        error.localizedMessage ?: "Gagal memuat kostumisasi latihan dari Firestore.",
                    )
                    return@addRefreshableSnapshotListener
                }
                val parsed = snapshot?.documents.orEmpty().mapNotNull { doc ->
                    parseCustomWorkoutFromMap(doc.id, doc.data.orEmpty())
                }
                CustomWorkoutStore.setWorkoutsFromFirestore(parsed)
            }
    } catch (e: Exception) {
        CustomWorkoutStore.setError(e.localizedMessage ?: "Gagal terhubung ke Firestore.")
        ListenerRegistration { }
    }
}

fun saveCustomWorkoutToFirestore(
    userId: String,
    name: String,
    selectedExercises: List<WorkoutExerciseDetail>,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onResult: (Boolean, String?) -> Unit = { _, _ -> },
) {
    val cleanName = name.trim()
    if (userId.isBlank()) {
        onResult(false, "Silakan login terlebih dahulu untuk menyimpan latihan.")
        return
    }
    if (cleanName.isEmpty()) {
        onResult(false, "Nama latihan tidak boleh kosong.")
        return
    }
    if (selectedExercises.isEmpty()) {
        onResult(false, "Pilih minimal satu gerakan terlebih dahulu.")
        return
    }
    if (CustomWorkoutStore.isSaving) return

    CustomWorkoutStore.markSaving(true)
    try {
        val docRef = firestore.collection("users")
            .document(userId)
            .collection(CUSTOM_WORKOUTS_SUBCOLLECTION)
            .document()

        val plan = buildCustomWorkoutPlan(
            id = docRef.id,
            name = cleanName,
            selectedExercises = selectedExercises,
        )

        docRef.set(plan.toFirestoreMap(includeServerTimestamps = true))
            .addOnSuccessListener {
                CustomWorkoutStore.markSaving(false)
                onResult(true, null)
            }
            .addOnFailureListener { err ->
                CustomWorkoutStore.markSaving(false)
                onResult(false, err.localizedMessage ?: "Gagal menyimpan latihan ke Firestore.")
            }
    } catch (e: Exception) {
        CustomWorkoutStore.markSaving(false)
        onResult(false, e.localizedMessage ?: "Gagal menyimpan latihan ke Firestore.")
    }
}

fun deleteCustomWorkoutFromFirestore(
    userId: String,
    workoutId: String,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onResult: (Boolean, String?) -> Unit = { _, _ -> },
) {
    if (userId.isBlank() || workoutId.isBlank()) {
        onResult(false, "Data latihan tidak valid.")
        return
    }
    try {
        firestore.collection("users")
            .document(userId)
            .collection(CUSTOM_WORKOUTS_SUBCOLLECTION)
            .document(workoutId)
            .delete()
            .addOnSuccessListener {
                onResult(true, null)
            }
            .addOnFailureListener { err ->
                onResult(false, err.localizedMessage ?: "Gagal menghapus latihan.")
            }
    } catch (e: Exception) {
        onResult(false, e.localizedMessage ?: "Gagal menghapus latihan.")
    }
}
