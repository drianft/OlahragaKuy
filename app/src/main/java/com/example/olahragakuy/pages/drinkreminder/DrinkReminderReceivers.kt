package com.example.olahragakuy.pages.drinkreminder

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import java.time.LocalDateTime

/**
 * Menerima pemicu alarm dari [android.app.AlarmManager], menampilkan notifikasi pengingat minum,
 * lalu menonaktifkan alarm (untuk mode `ONCE`) atau menjadwalkan pemicu berikutnya secara otomatis.
 */
class DrinkAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != DrinkReminderScheduler.ACTION_DRINK_ALARM_TRIGGER) return

        val alarmId = intent.getStringExtra(DrinkReminderScheduler.EXTRA_ALARM_ID)
            ?.trim()
            .orEmpty()
        if (alarmId.isEmpty()) return

        val appContext = context.applicationContext
        val alarm = DrinkReminderStorage.findAlarmById(appContext, alarmId) ?: return
        if (!alarm.isEnabled) {
            DrinkReminderScheduler.cancelAlarm(appContext, alarm.id)
            return
        }

        DrinkReminderScheduler.showDrinkNotification(appContext, alarm)

        if (alarm.repeatMode == DrinkRepeatMode.ONCE) {
            DrinkReminderStorage.setAlarmEnabled(appContext, alarm.id, enabled = false)
            DrinkReminderScheduler.cancelAlarm(appContext, alarm.id)
        } else {
            // Geser acuan 5 detik ke depan agar tidak memicu ulang slot yang sama
            // bila sistem membangunkan receiver beberapa milidetik lebih awal.
            DrinkReminderScheduler.scheduleAlarm(
                context = appContext,
                alarm = alarm,
                now = LocalDateTime.now().plusSeconds(5),
            )
        }
    }
}

/**
 * Menangani klik tombol aksi "Sudah Minum (+250 ml)" langsung dari panel notifikasi
 * tanpa perlu membuka aplikasi.
 */
class DrinkActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != DrinkReminderScheduler.ACTION_LOG_DRINK_FROM_NOTIFICATION) return

        val appContext = context.applicationContext
        val deltaMl = intent
            .getIntExtra(DrinkReminderScheduler.EXTRA_DRINK_DELTA_ML, DEFAULT_DRINK_STEP_ML)
            .coerceAtLeast(1)
        val notificationId = intent.getIntExtra(DrinkReminderScheduler.EXTRA_NOTIFICATION_ID, -1)

        DrinkReminderStorage.addWaterIntake(appContext, deltaMl)
        DrinkReminderScheduler.cancelNotification(appContext, notificationId)
    }
}

/**
 * Memasang ulang seluruh alarm Pengingat Minum yang aktif saat perangkat selesai restart
 * atau ketika waktu/zona waktu perangkat berubah.
 */
class DrinkBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val isSupportedAction = action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        if (!isSupportedAction) return

        DrinkReminderScheduler.rescheduleAllActiveAlarms(context.applicationContext)
    }
}
