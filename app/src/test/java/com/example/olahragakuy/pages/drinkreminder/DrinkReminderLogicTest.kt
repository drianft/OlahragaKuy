package com.example.olahragakuy.pages.drinkreminder

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class DrinkReminderLogicTest {

    private val testZone: ZoneId = ZoneId.of("Asia/Jakarta")

    // =========================================================================
    // 1. Mode INTERVAL
    // =========================================================================

    @Test
    fun intervalDaily_withinActiveWindow_picksNextSlot() {
        val alarm = DrinkAlarm(
            id = "interval-1",
            scheduleMode = DrinkScheduleMode.INTERVAL,
            intervalMinutes = 30,
            activeStartHour = 7,
            activeStartMinute = 0,
            activeEndHour = 21,
            activeEndMinute = 0,
            repeatMode = DrinkRepeatMode.DAILY,
            isEnabled = true,
        )
        // Senin, 23 Nov 2026 jam 09:10
        val now = LocalDateTime.of(2026, 11, 23, 9, 10)

        val next = computeNextTriggerDateTime(alarm, now)

        assertEquals(LocalDateTime.of(2026, 11, 23, 9, 30), next)
    }

    @Test
    fun intervalDaily_exactOnSlot_advancesToFollowingSlot() {
        val alarm = DrinkAlarm(
            id = "interval-exact",
            scheduleMode = DrinkScheduleMode.INTERVAL,
            intervalMinutes = 30,
            activeStartHour = 7,
            activeStartMinute = 0,
            activeEndHour = 21,
            activeEndMinute = 0,
            repeatMode = DrinkRepeatMode.DAILY,
            isEnabled = true,
        )
        val now = LocalDateTime.of(2026, 11, 23, 9, 0, 0)

        val next = computeNextTriggerDateTime(alarm, now)

        assertEquals(LocalDateTime.of(2026, 11, 23, 9, 30), next)
    }

    @Test
    fun intervalDaily_beforeActiveStart_schedulesAtActiveStartSameDay() {
        val alarm = DrinkAlarm(
            id = "interval-early",
            scheduleMode = DrinkScheduleMode.INTERVAL,
            intervalMinutes = 60,
            activeStartHour = 7,
            activeStartMinute = 0,
            activeEndHour = 21,
            activeEndMinute = 0,
            repeatMode = DrinkRepeatMode.DAILY,
            isEnabled = true,
        )
        val now = LocalDateTime.of(2026, 11, 23, 6, 15)

        val next = computeNextTriggerDateTime(alarm, now)

        assertEquals(LocalDateTime.of(2026, 11, 23, 7, 0), next)
    }

    @Test
    fun intervalDaily_afterActiveEnd_jumpsToNextDayActiveStart() {
        val alarm = DrinkAlarm(
            id = "interval-late",
            scheduleMode = DrinkScheduleMode.INTERVAL,
            intervalMinutes = 60,
            activeStartHour = 7,
            activeStartMinute = 0,
            activeEndHour = 21,
            activeEndMinute = 0,
            repeatMode = DrinkRepeatMode.DAILY,
            isEnabled = true,
        )
        val now = LocalDateTime.of(2026, 11, 23, 21, 5)

        val next = computeNextTriggerDateTime(alarm, now)

        assertEquals(LocalDateTime.of(2026, 11, 24, 7, 0), next)
    }

    @Test
    fun intervalCustomDays_afterActiveEnd_jumpsToNextSelectedDayStart() {
        val alarm = DrinkAlarm(
            id = "interval-custom",
            scheduleMode = DrinkScheduleMode.INTERVAL,
            intervalMinutes = 60,
            activeStartHour = 7,
            activeStartMinute = 0,
            activeEndHour = 21,
            activeEndMinute = 0,
            repeatMode = DrinkRepeatMode.CUSTOM_DAYS,
            customDays = setOf(1, 3), // Senin & Rabu
            isEnabled = true,
        )
        // 23 Nov 2026 adalah Senin, sudah melewati 21:00
        val now = LocalDateTime.of(2026, 11, 23, 21, 10)

        val next = computeNextTriggerDateTime(alarm, now)

        // Harus melompat ke Rabu, 25 Nov 2026 jam 07:00
        assertEquals(LocalDateTime.of(2026, 11, 25, 7, 0), next)
    }

    @Test
    fun intervalOnce_withinActiveWindow_picksNearestSlot() {
        val alarm = DrinkAlarm(
            id = "interval-once",
            scheduleMode = DrinkScheduleMode.INTERVAL,
            intervalMinutes = 15,
            activeStartHour = 7,
            activeStartMinute = 0,
            activeEndHour = 21,
            activeEndMinute = 0,
            repeatMode = DrinkRepeatMode.ONCE,
            isEnabled = true,
        )
        val now = LocalDateTime.of(2026, 11, 23, 10, 5)

        val next = computeNextTriggerDateTime(alarm, now)

        assertEquals(LocalDateTime.of(2026, 11, 23, 10, 15), next)
    }

    @Test
    fun intervalCustomDays_emptyDays_returnsNull() {
        val alarm = DrinkAlarm(
            id = "interval-empty",
            scheduleMode = DrinkScheduleMode.INTERVAL,
            repeatMode = DrinkRepeatMode.CUSTOM_DAYS,
            customDays = emptySet(),
            isEnabled = true,
        )
        val now = LocalDateTime.of(2026, 11, 23, 9, 0)

        assertNull(computeNextTriggerDateTime(alarm, now))
    }

    // =========================================================================
    // 2. Mode SPECIFIC_TIME
    // =========================================================================

    @Test
    fun specificTimeDaily_beforeTarget_triggersSameDay() {
        val alarm = DrinkAlarm(
            id = "spec-same-day",
            scheduleMode = DrinkScheduleMode.SPECIFIC_TIME,
            specificHour = 8,
            specificMinute = 0,
            repeatMode = DrinkRepeatMode.DAILY,
            isEnabled = true,
        )
        val now = LocalDateTime.of(2026, 11, 23, 7, 30)

        val next = computeNextTriggerDateTime(alarm, now)

        assertEquals(LocalDateTime.of(2026, 11, 23, 8, 0), next)
    }

    @Test
    fun specificTimeDaily_atOrAfterTarget_triggersNextDay() {
        val alarm = DrinkAlarm(
            id = "spec-next-day",
            scheduleMode = DrinkScheduleMode.SPECIFIC_TIME,
            specificHour = 8,
            specificMinute = 0,
            repeatMode = DrinkRepeatMode.DAILY,
            isEnabled = true,
        )
        val nowExact = LocalDateTime.of(2026, 11, 23, 8, 0, 0)
        val nowAfter = LocalDateTime.of(2026, 11, 23, 8, 15)

        assertEquals(LocalDateTime.of(2026, 11, 24, 8, 0), computeNextTriggerDateTime(alarm, nowExact))
        assertEquals(LocalDateTime.of(2026, 11, 24, 8, 0), computeNextTriggerDateTime(alarm, nowAfter))
    }

    @Test
    fun specificTimeCustomDays_skipsNonMatchingDays() {
        val alarm = DrinkAlarm(
            id = "spec-custom",
            scheduleMode = DrinkScheduleMode.SPECIFIC_TIME,
            specificHour = 8,
            specificMinute = 30,
            repeatMode = DrinkRepeatMode.CUSTOM_DAYS,
            customDays = setOf(5), // Jumat saja
            isEnabled = true,
        )
        // Selasa, 24 Nov 2026 jam 07:00
        val now = LocalDateTime.of(2026, 11, 24, 7, 0)

        val next = computeNextTriggerDateTime(alarm, now)

        // Jumat, 27 Nov 2026 jam 08:30
        assertEquals(LocalDateTime.of(2026, 11, 27, 8, 30), next)
    }

    @Test
    fun disabledAlarm_returnsNull() {
        val alarm = DrinkAlarm(
            id = "disabled",
            scheduleMode = DrinkScheduleMode.SPECIFIC_TIME,
            specificHour = 9,
            specificMinute = 0,
            isEnabled = false,
        )
        val now = LocalDateTime.of(2026, 11, 23, 8, 0)

        assertNull(computeNextTriggerDateTime(alarm, now))
    }

    // =========================================================================
    // 3. Countdown & Alarm Terdekat lintas Alarm
    // =========================================================================

    @Test
    fun findNearestNextAlarm_picksSoonestEnabledAcrossModesAndIgnoresDisabled() {
        val now = LocalDateTime.of(2026, 11, 23, 9, 10)
        val disabledSoonest = DrinkAlarm(
            id = "disabled-soon",
            scheduleMode = DrinkScheduleMode.SPECIFIC_TIME,
            specificHour = 9,
            specificMinute = 15,
            isEnabled = false,
        )
        val intervalAlarm = DrinkAlarm(
            id = "interval-60",
            scheduleMode = DrinkScheduleMode.INTERVAL,
            intervalMinutes = 60,
            activeStartHour = 7,
            activeStartMinute = 0,
            activeEndHour = 21,
            activeEndMinute = 0,
            isEnabled = true,
        ) // 10:00 (50 mnt lagi)
        val specificAlarm = DrinkAlarm(
            id = "spec-935",
            scheduleMode = DrinkScheduleMode.SPECIFIC_TIME,
            specificHour = 9,
            specificMinute = 35,
            isEnabled = true,
        ) // 09:35 (25 mnt lagi)

        val nearest = findNearestNextAlarm(
            alarms = listOf(disabledSoonest, intervalAlarm, specificAlarm),
            now = now,
            zoneId = testZone,
        )

        assertNotNull(nearest)
        assertEquals("spec-935", nearest?.alarm?.id)
        assertEquals(25L, nearest?.remainingMinutes)
        assertEquals("Alarm selanjutnya dalam 00:25", formatCountdownText(nearest))
    }

    @Test
    fun formatCountdownText_whenNoActiveAlarm_returnsFriendlyFallback() {
        val now = LocalDateTime.of(2026, 11, 23, 9, 10)
        val disabledAlarm = DrinkAlarm(id = "off", isEnabled = false)

        val nearestEmpty = findNearestNextAlarm(emptyList(), now, testZone)
        val nearestAllOff = findNearestNextAlarm(listOf(disabledAlarm), now, testZone)

        assertNull(nearestEmpty)
        assertNull(nearestAllOff)
        assertEquals(NO_ACTIVE_ALARM_TEXT, formatCountdownText(nearestEmpty))
        assertEquals("Belum ada alarm aktif", formatCountdownText(nearestAllOff))
    }

    @Test
    fun formatCountdownText_roundsUpPartialMinuteSoNeverShowsZeroBeforeTrigger() {
        val now = LocalDateTime.of(2026, 11, 23, 9, 59, 30)
        val alarm = DrinkAlarm(
            id = "soon",
            scheduleMode = DrinkScheduleMode.SPECIFIC_TIME,
            specificHour = 10,
            specificMinute = 0,
            isEnabled = true,
        )

        val nearest = findNearestNextAlarm(listOf(alarm), now, testZone)

        assertEquals(1L, nearest?.remainingMinutes)
        assertEquals("Alarm selanjutnya dalam 00:01", formatCountdownText(nearest))
    }

    // =========================================================================
    // 4. Tracker Hidrasi, Auto-Reset, & Formatter
    // =========================================================================

    @Test
    fun normalizeHydrationProgress_resetsConsumedOnDateChangeAndKeepsTarget() {
        val yesterday = HydrationProgress(
            consumedMl = 1500,
            targetMl = 2500,
            dateKey = "2026-11-23",
        )
        val nextDay = LocalDate.of(2026, 11, 24)

        val normalized = normalizeHydrationProgress(yesterday, nextDay)

        assertEquals(0, normalized.consumedMl)
        assertEquals(2500, normalized.targetMl)
        assertEquals("2026-11-24", normalized.dateKey)
    }

    @Test
    fun applyWaterIntake_addsStepAndPreventsNegativeUndo() {
        val today = LocalDate.of(2026, 11, 23)
        val initial = HydrationProgress(consumedMl = 0, targetMl = 2000, dateKey = "2026-11-23")

        val afterAdd = applyWaterIntake(initial, DEFAULT_DRINK_STEP_ML, today)
        assertEquals(250, afterAdd.consumedMl)

        val afterDoubleUndo = applyWaterIntake(
            applyWaterIntake(afterAdd, -DEFAULT_DRINK_STEP_ML, today),
            -DEFAULT_DRINK_STEP_ML,
            today,
        )
        assertEquals(0, afterDoubleUndo.consumedMl)
    }

    @Test
    fun applyWaterIntake_afterMidnight_resetsPreviousDayBeforeAdding() {
        val yesterdayProgress = HydrationProgress(
            consumedMl = 1750,
            targetMl = 2000,
            dateKey = "2026-11-23",
        )
        val newDay = LocalDate.of(2026, 11, 24)

        val updated = applyWaterIntake(yesterdayProgress, 250, newDay)

        assertEquals(250, updated.consumedMl)
        assertEquals("2026-11-24", updated.dateKey)
    }

    @Test
    fun hydrationFractionAndPercent_clampedProperly() {
        assertEquals(0.0f, calculateHydrationFraction(0, 2000), 0.0001f)
        assertEquals(0.625f, calculateHydrationFraction(1250, 2000), 0.0001f)
        assertEquals(1.0f, calculateHydrationFraction(2500, 2000), 0.0001f)

        assertEquals(0, calculateHydrationPercent(0, 2000))
        assertEquals(63, calculateHydrationPercent(1250, 2000))
        assertEquals(100, calculateHydrationPercent(2500, 2000))
    }

    @Test
    fun formatters_matchExpectedIndonesianDisplayFormat() {
        assertEquals("1.250 / 2.000 ml", formatHydrationProgressText(1250, 2000))
        assertEquals("0 / 2.000 ml", formatHydrationProgressText(0, 2000))

        val intervalDaily = DrinkAlarm(
            id = "fmt-1",
            scheduleMode = DrinkScheduleMode.INTERVAL,
            intervalMinutes = 30,
            activeStartHour = 7,
            activeStartMinute = 0,
            activeEndHour = 21,
            activeEndMinute = 0,
            repeatMode = DrinkRepeatMode.DAILY,
        )
        assertEquals(
            "30 Menit sekali • 07:00–21:00 (Harian)",
            formatAlarmScheduleSummary(intervalDaily),
        )

        val intervalCustom = DrinkAlarm(
            id = "fmt-2",
            scheduleMode = DrinkScheduleMode.INTERVAL,
            intervalMinutes = 90,
            activeStartHour = 8,
            activeStartMinute = 30,
            activeEndHour = 20,
            activeEndMinute = 0,
            repeatMode = DrinkRepeatMode.CUSTOM_DAYS,
            customDays = setOf(5, 1, 3),
        )
        assertEquals(
            "1 Jam 30 Menit sekali • 08:30–20:00 (Sen, Rab, Jum)",
            formatAlarmScheduleSummary(intervalCustom),
        )

        val specificDaily = DrinkAlarm(
            id = "fmt-3",
            scheduleMode = DrinkScheduleMode.SPECIFIC_TIME,
            specificHour = 8,
            specificMinute = 0,
            repeatMode = DrinkRepeatMode.DAILY,
        )
        assertEquals("08:00 (Harian)", formatAlarmScheduleSummary(specificDaily))

        val specificOnce = specificDaily.copy(repeatMode = DrinkRepeatMode.ONCE)
        assertEquals("08:00 (Sekali)", formatAlarmScheduleSummary(specificOnce))
    }
}
