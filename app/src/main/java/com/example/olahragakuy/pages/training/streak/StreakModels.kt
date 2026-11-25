package com.example.olahragakuy.pages.training.streak

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.time.LocalDate

const val STREAK_DAYS_SUBCOLLECTION = "streak_days"
const val STREAK_MIN_STEP_DISTANCE_METERS = 1000.0
const val STREAK_MIN_STEP_DISTANCE_KM = 1.0
const val STREAK_MAX_STEP_DISTANCE_METERS = 500_000.0

data class StreakDayUiModel(
    val label: String,
    val completed: Boolean = false,
)

enum class StepDistanceUpdateMode {
    /** Menambahkan jarak sesi baru ke total jarak harian. */
    ADD_DELTA,

    /** Menyetel total jarak harian (mempertahankan nilai tertinggi agar progres tidak mundur). */
    SET_DAILY_TOTAL,
}

/**
 * Catatan aktivitas harian pengguna untuk perhitungan streak pada satu tanggal lokal (`yyyy-MM-dd`).
 *
 * Sebuah hari dianggap memenuhi syarat streak ([isQualified]) apabila pengguna:
 * 1. Menyelesaikan latihan (Latihan Terarah atau Kostumisasi Latihan) pada hari tersebut, ATAU
 * 2. Menggunakan Pelacak Langkah dengan total jarak minimal 1 km ([STREAK_MIN_STEP_DISTANCE_METERS]).
 */
data class DailyStreakActivity(
    val dateKey: String,
    val workoutCompleted: Boolean = false,
    val stepDistanceMeters: Double = 0.0,
) {
    val sanitizedStepDistanceMeters: Double
        get() = sanitizeStepDistanceMeters(stepDistanceMeters)

    val stepDistanceKm: Double
        get() = sanitizedStepDistanceMeters / 1000.0

    val hasReachedStepTarget: Boolean
        get() = sanitizedStepDistanceMeters >= STREAK_MIN_STEP_DISTANCE_METERS

    val isQualified: Boolean
        get() = isDailyActivityQualified(
            workoutCompleted = workoutCompleted,
            stepDistanceMeters = sanitizedStepDistanceMeters,
        )
}

data class StreakSummary(
    val currentStreakDays: Int = 0,
    val longestStreakDays: Int = 0,
    val completedToday: Boolean = false,
    val todayWorkoutCompleted: Boolean = false,
    val todayStepDistanceMeters: Double = 0.0,
    val weekDays: List<StreakDayUiModel> = defaultEmptyWeekStreakDays(),
)

/**
 * Penyimpanan state streak reaktif per akun pengguna (`activeUserId`).
 * Otomatis di-reset saat berganti akun atau logout, dan disinkronkan dengan
 * Firestore pada subkoleksi `users/{uid}/streak_days/{yyyy-MM-dd}`.
 */
object TrainingStreakStore {
    var activeUserId by mutableStateOf<String?>(null)
        private set

    var activitiesByDate by mutableStateOf<Map<String, DailyStreakActivity>>(emptyMap())
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun bindUser(userId: String?) {
        val normalized = userId?.trim()?.takeIf { it.isNotEmpty() }
        if (activeUserId != normalized) {
            activeUserId = normalized
            activitiesByDate = emptyMap()
            errorMessage = null
        }
        isLoading = normalized != null
    }

    fun applyFromFirestore(
        userId: String,
        activities: Map<String, DailyStreakActivity>,
    ) {
        val normalized = userId.trim()
        if (activeUserId != normalized) return
        activitiesByDate = activities
        isLoading = false
        errorMessage = null
    }

    fun upsertLocalActivity(
        userId: String,
        activity: DailyStreakActivity,
    ) {
        val normalized = userId.trim()
        if (normalized.isEmpty()) return
        if (activeUserId != normalized) {
            bindUser(normalized)
        }
        activitiesByDate = activitiesByDate + (activity.dateKey to activity)
        errorMessage = null
    }

    fun setError(userId: String, message: String) {
        if (activeUserId != userId.trim()) return
        isLoading = false
        errorMessage = message
    }

    fun activityForDate(
        userId: String,
        date: LocalDate = LocalDate.now(),
    ): DailyStreakActivity? {
        if (activeUserId != userId.trim()) return null
        return activitiesByDate[formatStreakDateKey(date)]
    }

    fun summaryFor(
        userId: String?,
        today: LocalDate = LocalDate.now(),
    ): StreakSummary {
        val normalized = userId?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized == null || activeUserId != normalized) {
            return StreakSummary()
        }
        return computeStreakSummary(
            activitiesByDate = activitiesByDate,
            today = today,
        )
    }

    fun reset() {
        activeUserId = null
        activitiesByDate = emptyMap()
        isLoading = false
        errorMessage = null
    }
}
