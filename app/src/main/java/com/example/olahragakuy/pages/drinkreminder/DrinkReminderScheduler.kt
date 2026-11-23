package com.example.olahragakuy.pages.drinkreminder

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.example.olahragakuy.MainActivity
import com.example.olahragakuy.R
import java.time.LocalDateTime

/**
 * Mesin penjadwalan [AlarmManager] dan penerbitan notifikasi [NotificationCompat]
 * untuk fitur Pengingat Minum. Aman lintas versi Android (`minSdk = 26`..`targetSdk = 37`).
 */
object DrinkReminderScheduler {

    const val EXTRA_OPEN_DRINK_REMINDER = "open_drink_reminder"
    const val EXTRA_ALARM_ID = "drink_alarm_id"
    const val EXTRA_NOTIFICATION_ID = "drink_notification_id"
    const val EXTRA_DRINK_DELTA_ML = "drink_delta_ml"

    const val ACTION_DRINK_ALARM_TRIGGER = "com.example.olahragakuy.action.DRINK_ALARM_TRIGGER"
    const val ACTION_LOG_DRINK_FROM_NOTIFICATION =
        "com.example.olahragakuy.action.LOG_DRINK_FROM_NOTIFICATION"

    private const val NOTIFICATION_ID_BASE = 41_000
    private const val ACTION_REQUEST_CODE_BASE = 51_000
    private const val OPEN_APP_REQUEST_CODE = 61_000
    private const val ID_MODULO = 10_000

    private val VIBRATION_PATTERN = longArrayOf(0L, 250L, 150L, 250L)

    fun hasPostNotificationsPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context.applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun canScheduleExactAlarms(context: Context): Boolean {
        val alarmManager = context.applicationContext.getSystemService(AlarmManager::class.java)
            ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun scheduleAlarm(
        context: Context,
        alarm: DrinkAlarm,
        now: LocalDateTime = LocalDateTime.now(),
    ): Long? {
        val appContext = context.applicationContext
        if (!alarm.isEnabled) {
            cancelAlarm(appContext, alarm.id)
            return null
        }

        val triggerAtMillis = computeNextTriggerMillis(alarm, now)
        if (triggerAtMillis == null) {
            cancelAlarm(appContext, alarm.id)
            return null
        }

        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return null
        val pendingIntent = buildAlarmPendingIntent(
            context = appContext,
            alarmId = alarm.id,
            flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return null

        val useExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        try {
            if (useExact) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            } else {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent,
                )
            }
        } catch (_: SecurityException) {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent,
            )
        }
        return triggerAtMillis
    }

    fun cancelAlarm(context: Context, alarmId: String) {
        val appContext = context.applicationContext
        val alarmManager = appContext.getSystemService(AlarmManager::class.java) ?: return
        val existingIntent = buildAlarmPendingIntent(
            context = appContext,
            alarmId = alarmId,
            flags = PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )
        if (existingIntent != null) {
            alarmManager.cancel(existingIntent)
            existingIntent.cancel()
        }
    }

    fun rescheduleAllActiveAlarms(
        context: Context,
        now: LocalDateTime = LocalDateTime.now(),
    ) {
        val appContext = context.applicationContext
        val alarms = DrinkReminderStorage.loadAlarms(appContext)
        alarms.forEach { alarm ->
            if (alarm.isEnabled) {
                scheduleAlarm(appContext, alarm, now)
            } else {
                cancelAlarm(appContext, alarm.id)
            }
        }
    }

    fun showDrinkNotification(context: Context, alarm: DrinkAlarm) {
        val appContext = context.applicationContext
        if (!hasPostNotificationsPermission(appContext)) return

        val channelId = ensureNotificationChannel(appContext, alarm)
        val notificationId = notificationIdForAlarm(alarm.id)
        val progress = DrinkReminderStorage.loadHydrationProgress(appContext)

        val openAppIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_DRINK_REMINDER, true)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            appContext,
            OPEN_APP_REQUEST_CODE,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val logDrinkIntent = Intent(appContext, DrinkActionReceiver::class.java).apply {
            action = ACTION_LOG_DRINK_FROM_NOTIFICATION
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
            putExtra(EXTRA_DRINK_DELTA_ML, DEFAULT_DRINK_STEP_ML)
        }
        val logDrinkPendingIntent = PendingIntent.getBroadcast(
            appContext,
            actionRequestCodeForAlarm(alarm.id),
            logDrinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val title = alarm.label.ifBlank { DEFAULT_ALARM_LABEL }
        val body = "Progres hari ini: ${formatHydrationProgressText(progress.consumedMl, progress.targetMl)}"

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_mingcute_notification_fill)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("$body\nYuk luangkan sejenak untuk minum segelas air."),
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                0,
                "Sudah Minum (+$DEFAULT_DRINK_STEP_ML ml)",
                logDrinkPendingIntent,
            )
            .build()

        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        try {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
        } catch (_: SecurityException) {
            // Izin notifikasi dicabut saat runtime; abaikan secara aman.
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        if (notificationId < 0) return
        NotificationManagerCompat.from(context.applicationContext).cancel(notificationId)
    }

    private fun ensureNotificationChannel(context: Context, alarm: DrinkAlarm): String {
        val normalizedSoundUri = alarm.ringtoneUri?.trim()?.takeIf { it.isNotEmpty() }
        val soundKey = normalizedSoundUri?.hashCode()?.toUInt()?.toString(16) ?: "default"
        val vibrateKey = if (alarm.vibrate) "vib_on" else "vib_off"
        val channelId = "drink_reminder_${soundKey}_${vibrateKey}"

        val notificationManager = context.getSystemService(NotificationManager::class.java)
            ?: return channelId

        if (notificationManager.getNotificationChannel(channelId) != null) {
            return channelId
        }

        val channelName = buildString {
            append("Pengingat Minum")
            if (normalizedSoundUri != null) {
                append(" • ")
                append(alarm.ringtoneTitle.ifBlank { DEFAULT_RINGTONE_TITLE }.take(24))
            }
            if (!alarm.vibrate) {
                append(" (Tanpa Getar)")
            }
        }

        val soundUri: Uri? = normalizedSoundUri?.let { raw ->
            runCatching { raw.toUri() }.getOrNull()
        } ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifikasi jadwal pengingat minum air OlahragaKuy"
            setSound(soundUri, audioAttributes)
            enableVibration(alarm.vibrate)
            if (alarm.vibrate) {
                vibrationPattern = VIBRATION_PATTERN
            }
        }

        notificationManager.createNotificationChannel(channel)
        return channelId
    }

    private fun buildAlarmPendingIntent(
        context: Context,
        alarmId: String,
        flags: Int,
    ): PendingIntent? {
        val intent = Intent(context, DrinkAlarmReceiver::class.java).apply {
            action = ACTION_DRINK_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarmId)
        }
        return PendingIntent.getBroadcast(
            context,
            alarmRequestCode(alarmId),
            intent,
            flags,
        )
    }

    private fun alarmRequestCode(alarmId: String): Int {
        return (alarmId.hashCode() and 0x7FFFFFFF).coerceAtLeast(1)
    }

    private fun notificationIdForAlarm(alarmId: String): Int {
        return NOTIFICATION_ID_BASE + (alarmRequestCode(alarmId) % ID_MODULO)
    }

    private fun actionRequestCodeForAlarm(alarmId: String): Int {
        return ACTION_REQUEST_CODE_BASE + (alarmRequestCode(alarmId) % ID_MODULO)
    }
}
