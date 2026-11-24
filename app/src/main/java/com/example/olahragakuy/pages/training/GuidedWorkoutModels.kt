package com.example.olahragakuy.pages.training

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

const val GUIDED_WORKOUT_TOTAL_DAYS = 30

enum class ExerciseType {
    SIT_UP,
    PLANK_LURUS,
    SQUAT,
    PUSH_UP,
    JUMPING_JACK,
    LUNGES,
    GLUTE_BRIDGE,
    MOUNTAIN_CLIMBER,
}

fun resolveExerciseType(key: String?): ExerciseType {
    val normalized = key?.trim().orEmpty()
    return ExerciseType.entries.firstOrNull { it.name.equals(normalized, ignoreCase = true) }
        ?: ExerciseType.SIT_UP
}

sealed class WorkoutTarget {
    abstract val listLabel: String
    abstract val modeKey: String
    abstract val numericValue: Int

    data class Repetitions(val count: Int) : WorkoutTarget() {
        override val listLabel: String get() = "$count kali"
        override val modeKey: String get() = MODE_REPETITIONS
        override val numericValue: Int get() = count
    }

    data class Duration(val seconds: Int) : WorkoutTarget() {
        override val listLabel: String
            get() {
                val formatted = formatDurationClock(seconds)
                return if (seconds < 60) "$formatted detik" else "$formatted menit"
            }
        override val modeKey: String get() = MODE_DURATION
        override val numericValue: Int get() = seconds
    }

    companion object {
        const val MODE_REPETITIONS = "REPETITIONS"
        const val MODE_DURATION = "DURATION"

        fun fromRaw(mode: String?, value: Int): WorkoutTarget {
            val safeValue = value.coerceAtLeast(1)
            return if (mode.equals(MODE_DURATION, ignoreCase = true)) {
                Duration(safeValue)
            } else {
                Repetitions(safeValue)
            }
        }
    }
}

fun formatDurationClock(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "%02d:%02d".format(mins, secs)
}

data class WorkoutExerciseDetail(
    val type: ExerciseType,
    val name: String,
    val steps: List<String>,
    val id: String = type.name,
    val description: String = "",
    val category: String = "",
    val defaultTarget: WorkoutTarget = WorkoutTarget.Repetitions(12),
    val order: Int = 1,
)

data class GuidedWorkoutStep(
    val id: String,
    val exerciseType: ExerciseType,
    val target: WorkoutTarget,
    val exerciseId: String = exerciseType.name,
) {
    val targetLabel: String get() = target.listLabel
}

data class GuidedWorkoutDay(
    val dayNumber: Int,
    val calories: Int,
    val durationLabel: String,
    val difficultyLabel: String,
    val exercises: List<GuidedWorkoutStep>,
    val title: String = "Hari $dayNumber",
    val description: String = "",
)

enum class GuidedDayStatus {
    COMPLETED,
    UNLOCKED,
    LOCKED,
}

fun resolveGuidedDayStatus(
    dayNumber: Int,
    highestUnlockedDay: Int,
    completedDays: Set<Int>,
): GuidedDayStatus {
    val safeHighest = highestUnlockedDay.coerceIn(1, GUIDED_WORKOUT_TOTAL_DAYS)
    return when {
        dayNumber in completedDays -> GuidedDayStatus.COMPLETED
        dayNumber <= safeHighest -> GuidedDayStatus.UNLOCKED
        else -> GuidedDayStatus.LOCKED
    }
}

/**
 * Penyimpanan reaktif murni dari data Firestore (tanpa data bawaan/fallback lokal).
 * UI hanya menampilkan data yang benar-benar diterima dari Cloud Firestore.
 */
object WorkoutFirestoreStore {
    var exercisesById by mutableStateOf<Map<String, WorkoutExerciseDetail>>(emptyMap())
        private set

    var daysByNumber by mutableStateOf<Map<Int, GuidedWorkoutDay>>(emptyMap())
        private set

    var isLoadingExercises by mutableStateOf(true)
        private set

    var isLoadingDays by mutableStateOf(true)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    var isSeeding by mutableStateOf(false)
        private set

    var seedMessage by mutableStateOf<String?>(null)
        private set

    val isLoading: Boolean
        get() = isLoadingExercises || isLoadingDays

    fun allExercises(): List<WorkoutExerciseDetail> =
        exercisesById.values.sortedBy { it.order }

    fun allDays(): List<GuidedWorkoutDay> =
        daysByNumber.values.sortedBy { it.dayNumber }

    fun exerciseOrNull(key: String?): WorkoutExerciseDetail? {
        val normalized = key?.trim()?.uppercase().orEmpty()
        if (normalized.isEmpty()) return null
        return exercisesById[normalized]
    }

    fun dayPlanOrNull(dayNumber: Int): GuidedWorkoutDay? =
        daysByNumber[dayNumber]

    fun setExercisesFromFirestore(exercises: List<WorkoutExerciseDetail>) {
        exercisesById = exercises.associateBy { it.id.uppercase() }
        isLoadingExercises = false
        errorMessage = null
    }

    fun setDaysFromFirestore(days: List<GuidedWorkoutDay>) {
        daysByNumber = days.associateBy { it.dayNumber }
        isLoadingDays = false
        errorMessage = null
    }

    fun setError(message: String) {
        isLoadingExercises = false
        isLoadingDays = false
        errorMessage = message
    }

    fun setSeedingState(seeding: Boolean, message: String? = null) {
        isSeeding = seeding
        if (message != null) {
            seedMessage = message
        }
    }

    fun markLoading() {
        isLoadingExercises = true
        isLoadingDays = true
        errorMessage = null
    }

    fun clear() {
        exercisesById = emptyMap()
        daysByNumber = emptyMap()
        isLoadingExercises = false
        isLoadingDays = false
        errorMessage = null
        isSeeding = false
        seedMessage = null
    }
}

data class GuidedWorkoutProgress(
    val highestUnlockedDay: Int = 1,
    val completedDays: Set<Int> = emptySet(),
)

/**
 * Penyimpanan progres Latihan Terarah per akun pengguna (`activeUserId`).
 * Otomatis di-reset saat berganti akun atau logout dan disinkronkan ke Firestore
 * pada dokumen `users/{uid}/workout_progress/guided`.
 */
object GuidedWorkoutProgressStore {
    var activeUserId by mutableStateOf<String?>(null)
        private set

    var highestUnlockedDay by mutableIntStateOf(1)
        private set

    var completedDays by mutableStateOf<Set<Int>>(emptySet())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun bindUser(userId: String?) {
        val normalized = userId?.trim()?.takeIf { it.isNotEmpty() }
        if (activeUserId != normalized) {
            activeUserId = normalized
            highestUnlockedDay = 1
            completedDays = emptySet()
            errorMessage = null
        }
        isLoading = normalized != null
    }

    fun applyFromFirestore(userId: String, progress: GuidedWorkoutProgress) {
        val normalized = userId.trim()
        if (activeUserId != normalized) return
        val sanitizedDays = progress.completedDays
            .filter { it in 1..GUIDED_WORKOUT_TOTAL_DAYS }
            .toSet()
        val highestFromCompleted = sanitizedDays.maxOrNull()
            ?.let { (it + 1).coerceAtMost(GUIDED_WORKOUT_TOTAL_DAYS) }
            ?: 1
        completedDays = sanitizedDays
        highestUnlockedDay = maxOf(
            progress.highestUnlockedDay.coerceIn(1, GUIDED_WORKOUT_TOTAL_DAYS),
            highestFromCompleted,
        )
        isLoading = false
        errorMessage = null
    }

    fun setError(userId: String, message: String) {
        if (activeUserId != userId.trim()) return
        isLoading = false
        errorMessage = message
    }

    fun completeDay(dayNumber: Int): GuidedWorkoutProgress {
        val safeDay = dayNumber.coerceIn(1, GUIDED_WORKOUT_TOTAL_DAYS)
        val updatedCompleted = completedDays + safeDay
        val nextUnlocked = (safeDay + 1).coerceAtMost(GUIDED_WORKOUT_TOTAL_DAYS)
        val updatedHighest = maxOf(highestUnlockedDay, nextUnlocked)
            .coerceIn(1, GUIDED_WORKOUT_TOTAL_DAYS)
        completedDays = updatedCompleted
        highestUnlockedDay = updatedHighest
        return GuidedWorkoutProgress(
            highestUnlockedDay = updatedHighest,
            completedDays = updatedCompleted,
        )
    }

    fun reset() {
        activeUserId = null
        highestUnlockedDay = 1
        completedDays = emptySet()
        isLoading = false
        errorMessage = null
    }
}
