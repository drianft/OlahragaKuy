package com.example.olahragakuy.pages.drinkreminder

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.util.Locale
import kotlin.math.roundToInt

private const val MINUTES_PER_DAY = 24 * 60
private const val MILLIS_PER_MINUTE = 60_000L

private val DAY_LABELS_ID = mapOf(
    1 to "Sen",
    2 to "Sel",
    3 to "Rab",
    4 to "Kam",
    5 to "Jum",
    6 to "Sab",
    7 to "Min",
)

fun formatDateKey(date: LocalDate): String = date.toString()

fun normalizeHydrationProgress(
    progress: HydrationProgress,
    today: LocalDate,
): HydrationProgress {
    val sanitizedTarget = progress.targetMl.coerceIn(MIN_DAILY_TARGET_ML, MAX_DAILY_TARGET_ML)
    val todayKey = formatDateKey(today)
    return if (progress.dateKey != todayKey) {
        HydrationProgress(
            consumedMl = 0,
            targetMl = sanitizedTarget,
            dateKey = todayKey,
        )
    } else {
        progress.copy(
            consumedMl = progress.consumedMl.coerceAtLeast(0),
            targetMl = sanitizedTarget,
        )
    }
}

fun applyWaterIntake(
    progress: HydrationProgress,
    deltaMl: Int,
    today: LocalDate,
): HydrationProgress {
    val normalized = normalizeHydrationProgress(progress, today)
    val updatedConsumed = (normalized.consumedMl + deltaMl).coerceAtLeast(0)
    return normalized.copy(consumedMl = updatedConsumed)
}

fun updateDailyTarget(
    progress: HydrationProgress,
    newTargetMl: Int,
    today: LocalDate,
): HydrationProgress {
    val normalized = normalizeHydrationProgress(progress, today)
    return normalized.copy(
        targetMl = newTargetMl.coerceIn(MIN_DAILY_TARGET_ML, MAX_DAILY_TARGET_ML),
    )
}

fun calculateHydrationFraction(consumedMl: Int, targetMl: Int): Float {
    if (targetMl <= 0) return 0f
    return (consumedMl.coerceAtLeast(0).toFloat() / targetMl.toFloat()).coerceIn(0f, 1f)
}

fun calculateHydrationPercent(consumedMl: Int, targetMl: Int): Int {
    if (targetMl <= 0) return 0
    val ratio = consumedMl.coerceAtLeast(0).toDouble() / targetMl.toDouble()
    return (ratio * 100.0).roundToInt().coerceIn(0, 100)
}

fun isDayActiveForAlarm(alarm: DrinkAlarm, date: LocalDate): Boolean {
    return when (alarm.repeatMode) {
        DrinkRepeatMode.DAILY, DrinkRepeatMode.ONCE -> true
        DrinkRepeatMode.CUSTOM_DAYS -> {
            val validDays = alarm.customDays.filter { it in 1..7 }
            validDays.isNotEmpty() && date.dayOfWeek.value in validDays
        }
    }
}

/**
 * Menghitung [LocalDateTime] pemicu berikutnya (selalu strictly setelah [now]) untuk sebuah [DrinkAlarm].
 * Mengembalikan `null` jika alarm nonaktif atau mode `CUSTOM_DAYS` tidak memiliki hari aktif.
 */
fun computeNextTriggerDateTime(
    alarm: DrinkAlarm,
    now: LocalDateTime,
): LocalDateTime? {
    if (!alarm.isEnabled) return null
    if (alarm.repeatMode == DrinkRepeatMode.CUSTOM_DAYS && alarm.customDays.none { it in 1..7 }) {
        return null
    }

    return when (alarm.scheduleMode) {
        DrinkScheduleMode.SPECIFIC_TIME -> computeNextSpecificTime(alarm, now)
        DrinkScheduleMode.INTERVAL -> computeNextIntervalTime(alarm, now)
    }
}

private fun computeNextSpecificTime(
    alarm: DrinkAlarm,
    now: LocalDateTime,
): LocalDateTime? {
    val targetTime = LocalTime.of(
        alarm.specificHour.coerceIn(0, 23),
        alarm.specificMinute.coerceIn(0, 59),
    )
    val today = now.toLocalDate()

    for (dayOffset in 0L..7L) {
        val candidateDate = today.plusDays(dayOffset)
        if (!isDayActiveForAlarm(alarm, candidateDate)) continue
        val candidate = LocalDateTime.of(candidateDate, targetTime)
        if (candidate.isAfter(now)) {
            return candidate
        }
    }
    return null
}

private fun computeNextIntervalTime(
    alarm: DrinkAlarm,
    now: LocalDateTime,
): LocalDateTime? {
    val stepMinutes = alarm.intervalMinutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
    val startMinuteOfDay = alarm.activeStartHour.coerceIn(0, 23) * 60 +
        alarm.activeStartMinute.coerceIn(0, 59)
    val rawEndMinuteOfDay = alarm.activeEndHour.coerceIn(0, 23) * 60 +
        alarm.activeEndMinute.coerceIn(0, 59)
    val endMinuteOfDay = maxOf(rawEndMinuteOfDay, startMinuteOfDay).coerceAtMost(MINUTES_PER_DAY - 1)

    val startTime = LocalTime.of(startMinuteOfDay / 60, startMinuteOfDay % 60)
    val today = now.toLocalDate()

    for (dayOffset in 0L..7L) {
        val candidateDate = today.plusDays(dayOffset)
        if (!isDayActiveForAlarm(alarm, candidateDate)) continue

        val windowStart = LocalDateTime.of(candidateDate, startTime)
        if (dayOffset > 0L || windowStart.isAfter(now)) {
            return windowStart
        }

        // Hari ini dan waktu sekarang sudah >= awal jam aktif.
        val elapsedSeconds = Duration.between(windowStart, now).seconds.coerceAtLeast(0L)
        val stepSeconds = stepMinutes.toLong() * 60L
        val nextStepIndex = (elapsedSeconds / stepSeconds) + 1L
        val nextMinuteOfDay = startMinuteOfDay + (nextStepIndex * stepMinutes)

        if (nextMinuteOfDay <= endMinuteOfDay && nextMinuteOfDay < MINUTES_PER_DAY) {
            val hour = (nextMinuteOfDay / 60).toInt()
            val minute = (nextMinuteOfDay % 60).toInt()
            return LocalDateTime.of(candidateDate, LocalTime.of(hour, minute))
        }
    }
    return null
}

fun computeNextTriggerMillis(
    alarm: DrinkAlarm,
    now: LocalDateTime,
    zoneId: ZoneId = ZoneId.systemDefault(),
): Long? {
    val nextDateTime = computeNextTriggerDateTime(alarm, now) ?: return null
    return nextDateTime.atZone(zoneId).toInstant().toEpochMilli()
}

fun findNearestNextAlarm(
    alarms: List<DrinkAlarm>,
    now: LocalDateTime,
    zoneId: ZoneId = ZoneId.systemDefault(),
): NextAlarmInfo? {
    val nowMillis = now.atZone(zoneId).toInstant().toEpochMilli()
    var best: NextAlarmInfo? = null

    for (alarm in alarms) {
        val triggerMillis = computeNextTriggerMillis(alarm, now, zoneId) ?: continue
        val diffMillis = (triggerMillis - nowMillis).coerceAtLeast(0L)
        val remainingMinutes = if (diffMillis == 0L) {
            0L
        } else {
            ((diffMillis + MILLIS_PER_MINUTE - 1L) / MILLIS_PER_MINUTE).coerceAtLeast(1L)
        }
        val candidate = NextAlarmInfo(
            alarm = alarm,
            triggerAtMillis = triggerMillis,
            remainingMinutes = remainingMinutes,
        )
        if (best == null || candidate.triggerAtMillis < best.triggerAtMillis) {
            best = candidate
        }
    }
    return best
}

fun formatCountdownText(nextAlarm: NextAlarmInfo?): String {
    if (nextAlarm == null) return NO_ACTIVE_ALARM_TEXT
    val totalMinutes = nextAlarm.remainingMinutes.coerceAtLeast(0L)
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    val formatted = String.format(Locale.ROOT, "%02d:%02d", hours, minutes)
    return "Alarm selanjutnya dalam $formatted"
}

fun formatVolumeMl(ml: Int): String {
    val safeValue = ml.coerceAtLeast(0)
    val digits = safeValue.toString()
    if (digits.length <= 3) return digits

    val builder = StringBuilder(digits.length + digits.length / 3)
    val remainder = digits.length % 3
    for (index in digits.indices) {
        if (index > 0 && (index - remainder) % 3 == 0) {
            builder.append('.')
        }
        builder.append(digits[index])
    }
    return builder.toString()
}

fun formatHydrationProgressText(consumedMl: Int, targetMl: Int): String {
    return "${formatVolumeMl(consumedMl)} / ${formatVolumeMl(targetMl)} ml"
}

fun formatTimeHHmm(hour: Int, minute: Int): String {
    return String.format(
        Locale.ROOT,
        "%02d:%02d",
        hour.coerceIn(0, 23),
        minute.coerceIn(0, 59),
    )
}

fun formatIntervalDuration(intervalMinutes: Int): String {
    val safeMinutes = intervalMinutes.coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)
    val hours = safeMinutes / 60
    val minutes = safeMinutes % 60
    return when {
        hours > 0 && minutes > 0 -> "$hours Jam $minutes Menit sekali"
        hours > 0 -> "$hours Jam sekali"
        else -> "$minutes Menit sekali"
    }
}

fun formatRepeatSummary(
    repeatMode: DrinkRepeatMode,
    customDays: Set<Int>,
): String {
    return when (repeatMode) {
        DrinkRepeatMode.DAILY -> "Harian"
        DrinkRepeatMode.ONCE -> "Sekali"
        DrinkRepeatMode.CUSTOM_DAYS -> {
            val sortedValidDays = customDays.filter { it in 1..7 }.distinct().sorted()
            when {
                sortedValidDays.isEmpty() -> "Sekali"
                sortedValidDays.size == 7 -> "Harian"
                else -> sortedValidDays.mapNotNull { DAY_LABELS_ID[it] }.joinToString(", ")
            }
        }
    }
}

fun formatAlarmScheduleSummary(alarm: DrinkAlarm): String {
    val repeatText = formatRepeatSummary(alarm.repeatMode, alarm.customDays)
    return when (alarm.scheduleMode) {
        DrinkScheduleMode.INTERVAL -> {
            val intervalText = formatIntervalDuration(alarm.intervalMinutes)
            val startText = formatTimeHHmm(alarm.activeStartHour, alarm.activeStartMinute)
            val endText = formatTimeHHmm(alarm.activeEndHour, alarm.activeEndMinute)
            "$intervalText • $startText–$endText ($repeatText)"
        }
        DrinkScheduleMode.SPECIFIC_TIME -> {
            val timeText = formatTimeHHmm(alarm.specificHour, alarm.specificMinute)
            "$timeText ($repeatText)"
        }
    }
}

fun dayShortLabelId(isoDayOfWeek: Int): String = DAY_LABELS_ID[isoDayOfWeek] ?: ""

fun buildDrinkReminderUiState(
    progress: HydrationProgress,
    alarms: List<DrinkAlarm>,
    now: LocalDateTime = LocalDateTime.now(),
    zoneId: ZoneId = ZoneId.systemDefault(),
): DrinkReminderUiState {
    val normalizedProgress = normalizeHydrationProgress(progress, now.toLocalDate())
    val nearest = findNearestNextAlarm(alarms, now, zoneId)
    return DrinkReminderUiState(
        progress = normalizedProgress,
        alarms = alarms,
        nextAlarmInfo = nearest,
        countdownText = formatCountdownText(nearest),
    )
}
