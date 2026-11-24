package com.example.olahragakuy.pages.drinkreminder

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private const val PREFS_NAME = "olahragakuy_drink_reminder_prefs"
private const val KEY_ALARMS_JSON = "alarms_json"
private const val KEY_CONSUMED_ML = "consumed_ml"
private const val KEY_TARGET_ML = "target_ml"
private const val KEY_DATE_KEY = "date_key"

/**
 * Penyimpanan lokal 100% offline untuk fitur Pengingat Minum menggunakan [SharedPreferences] + `org.json`.
 * Aman dipanggil baik dari layar Compose maupun dari `BroadcastReceiver` di background.
 */
object DrinkReminderStorage {

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun loadHydrationProgress(
        context: Context,
        today: LocalDate = LocalDate.now(),
    ): HydrationProgress {
        val sp = prefs(context)
        val stored = HydrationProgress(
            consumedMl = sp.getInt(KEY_CONSUMED_ML, 0),
            targetMl = sp.getInt(KEY_TARGET_ML, DEFAULT_DAILY_TARGET_ML),
            dateKey = sp.getString(KEY_DATE_KEY, "").orEmpty(),
        )
        val normalized = normalizeHydrationProgress(stored, today)
        if (normalized != stored) {
            saveHydrationProgressLocked(sp, normalized, commit = false)
        }
        return normalized
    }

    @Synchronized
    fun addWaterIntake(
        context: Context,
        deltaMl: Int,
        today: LocalDate = LocalDate.now(),
    ): HydrationProgress {
        val sp = prefs(context)
        val current = HydrationProgress(
            consumedMl = sp.getInt(KEY_CONSUMED_ML, 0),
            targetMl = sp.getInt(KEY_TARGET_ML, DEFAULT_DAILY_TARGET_ML),
            dateKey = sp.getString(KEY_DATE_KEY, "").orEmpty(),
        )
        val updated = applyWaterIntake(current, deltaMl, today)
        saveHydrationProgressLocked(sp, updated, commit = true)
        return updated
    }

    @Synchronized
    fun updateDailyTarget(
        context: Context,
        newTargetMl: Int,
        today: LocalDate = LocalDate.now(),
    ): HydrationProgress {
        val sp = prefs(context)
        val current = HydrationProgress(
            consumedMl = sp.getInt(KEY_CONSUMED_ML, 0),
            targetMl = sp.getInt(KEY_TARGET_ML, DEFAULT_DAILY_TARGET_ML),
            dateKey = sp.getString(KEY_DATE_KEY, "").orEmpty(),
        )
        val updated = updateDailyTarget(current, newTargetMl, today)
        saveHydrationProgressLocked(sp, updated, commit = true)
        return updated
    }

    @Synchronized
    fun loadAlarms(context: Context): List<DrinkAlarm> {
        val rawJson = prefs(context).getString(KEY_ALARMS_JSON, null) ?: return emptyList()
        return parseAlarmsJson(rawJson)
    }

    @Synchronized
    fun saveAlarms(context: Context, alarms: List<DrinkAlarm>) {
        val serialized = serializeAlarmsJson(alarms)
        prefs(context).edit(commit = true) {
            putString(KEY_ALARMS_JSON, serialized)
        }
    }

    @Synchronized
    fun findAlarmById(context: Context, alarmId: String): DrinkAlarm? {
        return loadAlarms(context).firstOrNull { it.id == alarmId }
    }

    @Synchronized
    fun upsertAlarm(context: Context, alarm: DrinkAlarm): List<DrinkAlarm> {
        val current = loadAlarms(context).toMutableList()
        val existingIndex = current.indexOfFirst { it.id == alarm.id }
        if (existingIndex >= 0) {
            current[existingIndex] = alarm
        } else {
            current.add(alarm)
        }
        val updated = current.toList()
        saveAlarms(context, updated)
        return updated
    }

    @Synchronized
    fun deleteAlarm(context: Context, alarmId: String): List<DrinkAlarm> {
        val updated = loadAlarms(context).filterNot { it.id == alarmId }
        saveAlarms(context, updated)
        return updated
    }

    @Synchronized
    fun setAlarmEnabled(
        context: Context,
        alarmId: String,
        enabled: Boolean,
    ): List<DrinkAlarm> {
        val updated = loadAlarms(context).map { alarm ->
            if (alarm.id == alarmId) alarm.copy(isEnabled = enabled) else alarm
        }
        saveAlarms(context, updated)
        return updated
    }

    /**
     * Mendaftarkan listener perubahan [SharedPreferences] agar UI otomatis sinkron
     * ketika pengguna menekan aksi "Sudah Minum (+250 ml)" atau alarm mengubah statusnya di background.
     * Mengembalikan fungsi untuk melepas listener.
     */
    fun registerOnChangeListener(
        context: Context,
        onChanged: () -> Unit,
    ): () -> Unit {
        val sp = prefs(context)
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, _ ->
            onChanged()
        }
        sp.registerOnSharedPreferenceChangeListener(listener)
        return {
            sp.unregisterOnSharedPreferenceChangeListener(listener)
        }
    }

    private fun saveHydrationProgressLocked(
        sp: SharedPreferences,
        progress: HydrationProgress,
        commit: Boolean,
    ) {
        sp.edit(commit = commit) {
            putInt(KEY_CONSUMED_ML, progress.consumedMl)
            putInt(KEY_TARGET_ML, progress.targetMl)
            putString(KEY_DATE_KEY, progress.dateKey)
        }
    }

    private fun serializeAlarmsJson(alarms: List<DrinkAlarm>): String {
        val array = JSONArray()
        alarms.forEach { alarm ->
            val daysArray = JSONArray()
            alarm.customDays.filter { it in 1..7 }.distinct().sorted().forEach { day ->
                daysArray.put(day)
            }
            val obj = JSONObject().apply {
                put("id", alarm.id)
                put("label", alarm.label)
                put("scheduleMode", alarm.scheduleMode.name)
                put("intervalMinutes", alarm.intervalMinutes)
                put("activeStartHour", alarm.activeStartHour)
                put("activeStartMinute", alarm.activeStartMinute)
                put("activeEndHour", alarm.activeEndHour)
                put("activeEndMinute", alarm.activeEndMinute)
                put("specificHour", alarm.specificHour)
                put("specificMinute", alarm.specificMinute)
                put("repeatMode", alarm.repeatMode.name)
                put("customDays", daysArray)
                if (alarm.ringtoneUri != null) {
                    put("ringtoneUri", alarm.ringtoneUri)
                } else {
                    put("ringtoneUri", JSONObject.NULL)
                }
                put("ringtoneTitle", alarm.ringtoneTitle)
                put("vibrate", alarm.vibrate)
                put("isEnabled", alarm.isEnabled)
                put("createdAtMillis", alarm.createdAtMillis)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseAlarmsJson(rawJson: String): List<DrinkAlarm> {
        return try {
            val array = JSONArray(rawJson)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val id = obj.optString("id", "").trim()
                    if (id.isEmpty()) continue

                    val scheduleMode = runCatching {
                        DrinkScheduleMode.valueOf(
                            obj.optString("scheduleMode", DrinkScheduleMode.INTERVAL.name),
                        )
                    }.getOrDefault(DrinkScheduleMode.INTERVAL)

                    val repeatMode = runCatching {
                        DrinkRepeatMode.valueOf(
                            obj.optString("repeatMode", DrinkRepeatMode.DAILY.name),
                        )
                    }.getOrDefault(DrinkRepeatMode.DAILY)

                    val daysJson = obj.optJSONArray("customDays")
                    val customDays = if (daysJson != null) {
                        buildSet {
                            for (dayIdx in 0 until daysJson.length()) {
                                val day = daysJson.optInt(dayIdx, -1)
                                if (day in 1..7) add(day)
                            }
                        }.ifEmpty { ALL_WEEK_DAYS }
                    } else {
                        ALL_WEEK_DAYS
                    }

                    val ringtoneUri = if (obj.isNull("ringtoneUri")) {
                        null
                    } else {
                        obj.optString("ringtoneUri", "").ifBlank { null }
                    }

                    add(
                        DrinkAlarm(
                            id = id,
                            label = obj.optString("label", DEFAULT_ALARM_LABEL)
                                .ifBlank { DEFAULT_ALARM_LABEL },
                            scheduleMode = scheduleMode,
                            intervalMinutes = obj.optInt("intervalMinutes", DEFAULT_INTERVAL_MINUTES)
                                .coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES),
                            activeStartHour = obj.optInt("activeStartHour", DEFAULT_ACTIVE_START_HOUR)
                                .coerceIn(0, 23),
                            activeStartMinute = obj.optInt("activeStartMinute", DEFAULT_ACTIVE_START_MINUTE)
                                .coerceIn(0, 59),
                            activeEndHour = obj.optInt("activeEndHour", DEFAULT_ACTIVE_END_HOUR)
                                .coerceIn(0, 23),
                            activeEndMinute = obj.optInt("activeEndMinute", DEFAULT_ACTIVE_END_MINUTE)
                                .coerceIn(0, 59),
                            specificHour = obj.optInt("specificHour", DEFAULT_SPECIFIC_HOUR)
                                .coerceIn(0, 23),
                            specificMinute = obj.optInt("specificMinute", DEFAULT_SPECIFIC_MINUTE)
                                .coerceIn(0, 59),
                            repeatMode = repeatMode,
                            customDays = customDays,
                            ringtoneUri = ringtoneUri,
                            ringtoneTitle = obj.optString("ringtoneTitle", DEFAULT_RINGTONE_TITLE)
                                .ifBlank { DEFAULT_RINGTONE_TITLE },
                            vibrate = obj.optBoolean("vibrate", true),
                            isEnabled = obj.optBoolean("isEnabled", true),
                            createdAtMillis = obj.optLong("createdAtMillis", 0L),
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
