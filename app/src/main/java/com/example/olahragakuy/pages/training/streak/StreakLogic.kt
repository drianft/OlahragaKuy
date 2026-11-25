package com.example.olahragakuy.pages.training.streak

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.TemporalAdjusters

private val STREAK_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE

private val WEEKDAY_LABELS_ID = listOf(
    "Sen",
    "Sel",
    "Rab",
    "Kam",
    "Jum",
    "Sab",
    "Min",
)

fun defaultEmptyWeekStreakDays(): List<StreakDayUiModel> =
    WEEKDAY_LABELS_ID.map { label -> StreakDayUiModel(label = label, completed = false) }

fun formatStreakDateKey(date: LocalDate): String = date.format(STREAK_DATE_FORMATTER)

fun parseStreakDateKey(raw: String?): LocalDate? {
    val clean = raw?.trim().orEmpty()
    if (clean.length != 10) return null
    return try {
        LocalDate.parse(clean, STREAK_DATE_FORMATTER)
    } catch (_: DateTimeParseException) {
        null
    }
}

fun sanitizeStepDistanceMeters(distanceMeters: Double): Double {
    if (!distanceMeters.isFinite()) return 0.0
    return distanceMeters.coerceIn(0.0, STREAK_MAX_STEP_DISTANCE_METERS)
}

/**
 * Menentukan apakah aktivitas pada satu hari memenuhi syarat streak:
 * - Pengguna sudah menyelesaikan latihan hari itu, ATAU
 * - Pengguna menggunakan pelacak langkah dengan jarak minimal 1 km (1000 m).
 */
fun isDailyActivityQualified(
    workoutCompleted: Boolean,
    stepDistanceMeters: Double,
): Boolean {
    return workoutCompleted || sanitizeStepDistanceMeters(stepDistanceMeters) >= STREAK_MIN_STEP_DISTANCE_METERS
}

fun mergeWorkoutCompleted(
    existing: DailyStreakActivity?,
    date: LocalDate = LocalDate.now(),
): DailyStreakActivity {
    val dateKey = formatStreakDateKey(date)
    val preservedDistance = existing?.sanitizedStepDistanceMeters ?: 0.0
    return DailyStreakActivity(
        dateKey = dateKey,
        workoutCompleted = true,
        stepDistanceMeters = preservedDistance,
    )
}

fun mergeStepTrackerDistance(
    existing: DailyStreakActivity?,
    distanceMeters: Double,
    date: LocalDate = LocalDate.now(),
    mode: StepDistanceUpdateMode = StepDistanceUpdateMode.ADD_DELTA,
): DailyStreakActivity {
    val dateKey = formatStreakDateKey(date)
    val safeIncoming = sanitizeStepDistanceMeters(distanceMeters)
    val existingDistance = existing?.sanitizedStepDistanceMeters ?: 0.0
    val updatedDistance = when (mode) {
        StepDistanceUpdateMode.ADD_DELTA -> sanitizeStepDistanceMeters(existingDistance + safeIncoming)
        StepDistanceUpdateMode.SET_DAILY_TOTAL -> sanitizeStepDistanceMeters(maxOf(existingDistance, safeIncoming))
    }
    return DailyStreakActivity(
        dateKey = dateKey,
        workoutCompleted = existing?.workoutCompleted ?: false,
        stepDistanceMeters = updatedDistance,
    )
}

/**
 * Menghitung jumlah hari beruntun aktif sampai [today].
 *
 * - Jika [today] sudah memenuhi syarat, hitung mundur mulai dari [today].
 * - Jika [today] belum memenuhi syarat tetapi kemarin (`today - 1`) memenuhi syarat,
 *   streak masih aktif sampai akhir hari ini, sehingga dihitung mundur mulai dari kemarin.
 * - Jika [today] maupun kemarin tidak memenuhi syarat, streak bernilai 0.
 */
fun calculateCurrentStreakDays(
    activitiesByDate: Map<String, DailyStreakActivity>,
    today: LocalDate = LocalDate.now(),
): Int {
    val qualifiedDates = activitiesByDate.values
        .filter { it.isQualified }
        .mapNotNull { parseStreakDateKey(it.dateKey) }
        .filter { !it.isAfter(today) }
        .toSet()

    val anchorDate = when {
        today in qualifiedDates -> today
        today.minusDays(1) in qualifiedDates -> today.minusDays(1)
        else -> return 0
    }

    var streakCount = 0
    var cursor = anchorDate
    while (cursor in qualifiedDates) {
        streakCount += 1
        cursor = cursor.minusDays(1)
    }
    return streakCount
}

fun calculateLongestStreakDays(
    activitiesByDate: Map<String, DailyStreakActivity>,
): Int {
    val sortedQualifiedDates = activitiesByDate.values
        .filter { it.isQualified }
        .mapNotNull { parseStreakDateKey(it.dateKey) }
        .distinct()
        .sorted()

    if (sortedQualifiedDates.isEmpty()) return 0

    var longest = 1
    var current = 1
    for (index in 1 until sortedQualifiedDates.size) {
        if (sortedQualifiedDates[index] == sortedQualifiedDates[index - 1].plusDays(1)) {
            current += 1
            if (current > longest) longest = current
        } else {
            current = 1
        }
    }
    return longest
}

/**
 * Membangun status harian Senin sampai Minggu pada minggu berjalan untuk [today].
 */
fun buildCurrentWeekStreakDays(
    activitiesByDate: Map<String, DailyStreakActivity>,
    today: LocalDate = LocalDate.now(),
): List<StreakDayUiModel> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return WEEKDAY_LABELS_ID.mapIndexed { offset, label ->
        val date = monday.plusDays(offset.toLong())
        val activity = activitiesByDate[formatStreakDateKey(date)]
        StreakDayUiModel(
            label = label,
            completed = activity?.isQualified == true,
        )
    }
}

fun computeStreakSummary(
    activitiesByDate: Map<String, DailyStreakActivity>,
    today: LocalDate = LocalDate.now(),
): StreakSummary {
    val todayActivity = activitiesByDate[formatStreakDateKey(today)]
    return StreakSummary(
        currentStreakDays = calculateCurrentStreakDays(activitiesByDate, today),
        longestStreakDays = calculateLongestStreakDays(activitiesByDate),
        completedToday = todayActivity?.isQualified == true,
        todayWorkoutCompleted = todayActivity?.workoutCompleted == true,
        todayStepDistanceMeters = todayActivity?.sanitizedStepDistanceMeters ?: 0.0,
        weekDays = buildCurrentWeekStreakDays(activitiesByDate, today),
    )
}

fun buildStreakSubtitle(summary: StreakSummary): String {
    return when {
        summary.completedToday -> "Target hari ini tercapai! Pertahankan streak anda."
        summary.currentStreakDays > 0 -> "Latihan atau jalan minimal 1 km hari ini agar streak berlanjut!"
        else -> "Selesaikan latihan atau jalan minimal 1 km untuk mulai streak!"
    }
}
