package com.example.olahragakuy.pages.drinkreminder

const val DEFAULT_DAILY_TARGET_ML = 2000
const val MIN_DAILY_TARGET_ML = 250
const val MAX_DAILY_TARGET_ML = 10000
const val DEFAULT_DRINK_STEP_ML = 250

const val DEFAULT_INTERVAL_MINUTES = 60
const val MIN_INTERVAL_MINUTES = 1
const val MAX_INTERVAL_MINUTES = 12 * 60

const val DEFAULT_ACTIVE_START_HOUR = 7
const val DEFAULT_ACTIVE_START_MINUTE = 0
const val DEFAULT_ACTIVE_END_HOUR = 21
const val DEFAULT_ACTIVE_END_MINUTE = 0

const val DEFAULT_SPECIFIC_HOUR = 8
const val DEFAULT_SPECIFIC_MINUTE = 0

const val DEFAULT_ALARM_LABEL = "Waktunya Minum Air!"
const val DEFAULT_RINGTONE_TITLE = "Default Notifikasi"

/** ISO-8601 day-of-week values: 1 = Senin .. 7 = Minggu. */
val ALL_WEEK_DAYS: Set<Int> = setOf(1, 2, 3, 4, 5, 6, 7)

enum class DrinkScheduleMode {
    INTERVAL,
    SPECIFIC_TIME,
}

enum class DrinkRepeatMode {
    DAILY,
    ONCE,
    CUSTOM_DAYS,
}

data class DrinkAlarm(
    val id: String,
    val label: String = DEFAULT_ALARM_LABEL,
    val scheduleMode: DrinkScheduleMode = DrinkScheduleMode.INTERVAL,
    val intervalMinutes: Int = DEFAULT_INTERVAL_MINUTES,
    val activeStartHour: Int = DEFAULT_ACTIVE_START_HOUR,
    val activeStartMinute: Int = DEFAULT_ACTIVE_START_MINUTE,
    val activeEndHour: Int = DEFAULT_ACTIVE_END_HOUR,
    val activeEndMinute: Int = DEFAULT_ACTIVE_END_MINUTE,
    val specificHour: Int = DEFAULT_SPECIFIC_HOUR,
    val specificMinute: Int = DEFAULT_SPECIFIC_MINUTE,
    val repeatMode: DrinkRepeatMode = DrinkRepeatMode.DAILY,
    val customDays: Set<Int> = ALL_WEEK_DAYS,
    val ringtoneUri: String? = null,
    val ringtoneTitle: String = DEFAULT_RINGTONE_TITLE,
    val vibrate: Boolean = true,
    val isEnabled: Boolean = true,
    val createdAtMillis: Long = 0L,
)

data class HydrationProgress(
    val consumedMl: Int = 0,
    val targetMl: Int = DEFAULT_DAILY_TARGET_ML,
    val dateKey: String = "",
)

data class NextAlarmInfo(
    val alarm: DrinkAlarm,
    val triggerAtMillis: Long,
    val remainingMinutes: Long,
)

data class DrinkReminderUiState(
    val progress: HydrationProgress = HydrationProgress(),
    val alarms: List<DrinkAlarm> = emptyList(),
    val nextAlarmInfo: NextAlarmInfo? = null,
    val countdownText: String = NO_ACTIVE_ALARM_TEXT,
)

const val NO_ACTIVE_ALARM_TEXT = "Belum ada alarm aktif"
