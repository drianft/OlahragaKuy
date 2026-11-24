package com.example.olahragakuy.pages.drinkreminder

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Vibration
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.components.KuyPrimaryButton
import com.example.olahragakuy.ui.components.KuyTextField
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyBlue
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.KuyOrange
import com.example.olahragakuy.ui.theme.KuyText
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins
import java.util.Locale
import java.util.UUID

private val EditorCardBg = Color(0xFF16324F)
private val EditorInnerBg = Color(0xFF0C2138)
private val EditorBorder = Color(0xFF284B70)
private val EditorPopupBg = Color(0xFF1E3A5F)
private val WaterLightBlue = Color(0xFF8AB5EE)
private val WaterMidBlue = Color(0xFF4C80C8)
private val SystemDefaultRingtoneLabel = "Default Sistem"

private enum class ActiveTimePickerTarget {
    ACTIVE_START,
    ACTIVE_END,
    SPECIFIC_TIME,
}

@Composable
fun DrinkAlarmEditorScreen(
    initialAlarm: DrinkAlarm?,
    onBack: () -> Unit,
    onSaveAlarm: (DrinkAlarm) -> Unit,
    onCompleted: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val isEditMode = initialAlarm != null

    val initialIntervalTotal = (initialAlarm?.intervalMinutes ?: DEFAULT_INTERVAL_MINUTES)
        .coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)

    var scheduleModeName by rememberSaveable(initialAlarm?.id) {
        mutableStateOf((initialAlarm?.scheduleMode ?: DrinkScheduleMode.INTERVAL).name)
    }
    var intervalHours by rememberSaveable(initialAlarm?.id) {
        mutableIntStateOf(initialIntervalTotal / 60)
    }
    var intervalMinutes by rememberSaveable(initialAlarm?.id) {
        mutableIntStateOf(initialIntervalTotal % 60)
    }
    var activeStartHour by rememberSaveable(initialAlarm?.id) {
        mutableIntStateOf(initialAlarm?.activeStartHour ?: DEFAULT_ACTIVE_START_HOUR)
    }
    var activeStartMinute by rememberSaveable(initialAlarm?.id) {
        mutableIntStateOf(initialAlarm?.activeStartMinute ?: DEFAULT_ACTIVE_START_MINUTE)
    }
    var activeEndHour by rememberSaveable(initialAlarm?.id) {
        mutableIntStateOf(initialAlarm?.activeEndHour ?: DEFAULT_ACTIVE_END_HOUR)
    }
    var activeEndMinute by rememberSaveable(initialAlarm?.id) {
        mutableIntStateOf(initialAlarm?.activeEndMinute ?: DEFAULT_ACTIVE_END_MINUTE)
    }
    var specificHour by rememberSaveable(initialAlarm?.id) {
        mutableIntStateOf(initialAlarm?.specificHour ?: DEFAULT_SPECIFIC_HOUR)
    }
    var specificMinute by rememberSaveable(initialAlarm?.id) {
        mutableIntStateOf(initialAlarm?.specificMinute ?: DEFAULT_SPECIFIC_MINUTE)
    }
    var repeatModeName by rememberSaveable(initialAlarm?.id) {
        mutableStateOf((initialAlarm?.repeatMode ?: DrinkRepeatMode.DAILY).name)
    }
    var customDaysList by rememberSaveable(initialAlarm?.id) {
        mutableStateOf(
            (initialAlarm?.customDays ?: ALL_WEEK_DAYS)
                .filter { it in 1..7 }
                .distinct()
                .sorted(),
        )
    }
    var label by rememberSaveable(initialAlarm?.id) {
        mutableStateOf(initialAlarm?.label ?: DEFAULT_ALARM_LABEL)
    }
    var ringtoneUri by rememberSaveable(initialAlarm?.id) {
        mutableStateOf(initialAlarm?.ringtoneUri)
    }
    var ringtoneTitle by rememberSaveable(initialAlarm?.id) {
        mutableStateOf(
            if (initialAlarm == null || initialAlarm.ringtoneUri.isNullOrBlank()) {
                SystemDefaultRingtoneLabel
            } else {
                initialAlarm.ringtoneTitle.ifBlank { SystemDefaultRingtoneLabel }
            },
        )
    }
    var vibrate by rememberSaveable(initialAlarm?.id) {
        mutableStateOf(initialAlarm?.vibrate ?: true)
    }
    var activeTimePicker by remember { mutableStateOf<ActiveTimePickerTarget?>(null) }
    var showSuccessPopup by rememberSaveable { mutableStateOf(false) }

    val scheduleMode = runCatching { DrinkScheduleMode.valueOf(scheduleModeName) }
        .getOrDefault(DrinkScheduleMode.INTERVAL)
    val repeatMode = runCatching { DrinkRepeatMode.valueOf(repeatModeName) }
        .getOrDefault(DrinkRepeatMode.DAILY)
    val customDaysSet = customDaysList.toSet().ifEmpty { ALL_WEEK_DAYS }
    val totalIntervalMinutes = (intervalHours * 60 + intervalMinutes)
        .coerceIn(MIN_INTERVAL_MINUTES, MAX_INTERVAL_MINUTES)

    val previewAlarm = DrinkAlarm(
        id = initialAlarm?.id ?: "preview",
        label = label.trim().ifBlank { DEFAULT_ALARM_LABEL },
        scheduleMode = scheduleMode,
        intervalMinutes = totalIntervalMinutes,
        activeStartHour = activeStartHour,
        activeStartMinute = activeStartMinute,
        activeEndHour = activeEndHour,
        activeEndMinute = activeEndMinute,
        specificHour = specificHour,
        specificMinute = specificMinute,
        repeatMode = repeatMode,
        customDays = customDaysSet,
        ringtoneUri = ringtoneUri,
        ringtoneTitle = ringtoneTitle,
        vibrate = vibrate,
        isEnabled = true,
        createdAtMillis = initialAlarm?.createdAtMillis ?: 0L,
    )

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@rememberLauncherForActivityResult
        val pickedUri: Uri? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
        }
        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (pickedUri == null || pickedUri == defaultUri) {
            ringtoneUri = null
            ringtoneTitle = SystemDefaultRingtoneLabel
        } else {
            ringtoneUri = pickedUri.toString()
            ringtoneTitle = runCatching {
                RingtoneManager.getRingtone(context, pickedUri)?.getTitle(context)
            }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Nada Kustom"
        }
    }

    BackHandler(enabled = !showSuccessPopup) {
        onBack()
    }

    Box(modifier = modifier.fillMaxSize().background(HomeNavy)) {
        Scaffold(
            containerColor = HomeNavy,
            contentColor = HomeText,
            topBar = {
                EditorTopBar(
                    title = if (isEditMode) "Edit Alarm" else "Tambah Alarm",
                    onBack = onBack,
                )
            },
            bottomBar = {
                Surface(
                    color = HomeNavy,
                    shadowElevation = 8.dp,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .imePadding()
                            .padding(horizontal = 20.dp, vertical = 14.dp),
                    ) {
                        KuyPrimaryButton(
                            text = if (isEditMode) "Simpan Perubahan" else "Simpan Alarm",
                            enabled = true,
                            onClick = {
                                val normalizedStartTotal = activeStartHour * 60 + activeStartMinute
                                val normalizedEndTotal = activeEndHour * 60 + activeEndMinute
                                val finalEndTotal = maxOf(normalizedEndTotal, normalizedStartTotal)
                                val alarmToSave = DrinkAlarm(
                                    id = initialAlarm?.id ?: UUID.randomUUID().toString(),
                                    label = label.trim().ifBlank { DEFAULT_ALARM_LABEL },
                                    scheduleMode = scheduleMode,
                                    intervalMinutes = totalIntervalMinutes,
                                    activeStartHour = activeStartHour,
                                    activeStartMinute = activeStartMinute,
                                    activeEndHour = finalEndTotal / 60,
                                    activeEndMinute = finalEndTotal % 60,
                                    specificHour = specificHour,
                                    specificMinute = specificMinute,
                                    repeatMode = repeatMode,
                                    customDays = customDaysSet,
                                    ringtoneUri = ringtoneUri,
                                    ringtoneTitle = ringtoneTitle.ifBlank { SystemDefaultRingtoneLabel },
                                    vibrate = vibrate,
                                    isEnabled = true,
                                    createdAtMillis = initialAlarm?.createdAtMillis
                                        ?: System.currentTimeMillis(),
                                )
                                onSaveAlarm(alarmToSave)
                                showSuccessPopup = true
                            },
                        )
                    }
                }
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                ScheduleModeSegmentedToggle(
                    selectedMode = scheduleMode,
                    onModeSelected = { scheduleModeName = it.name },
                )

                if (scheduleMode == DrinkScheduleMode.INTERVAL) {
                    IntervalScheduleCard(
                        intervalHours = intervalHours,
                        intervalMinutes = intervalMinutes,
                        activeStartHour = activeStartHour,
                        activeStartMinute = activeStartMinute,
                        activeEndHour = activeEndHour,
                        activeEndMinute = activeEndMinute,
                        onSelectPresetMinutes = { minutes ->
                            intervalHours = minutes / 60
                            intervalMinutes = minutes % 60
                        },
                        onChangeHours = { newHours ->
                            val clampedHours = newHours.coerceIn(0, 12)
                            intervalHours = clampedHours
                            if (clampedHours == 12) {
                                intervalMinutes = 0
                            } else if (clampedHours == 0 && intervalMinutes < MIN_INTERVAL_MINUTES) {
                                intervalMinutes = MIN_INTERVAL_MINUTES
                            }
                        },
                        onChangeMinutes = { newMinutes ->
                            val wrapped = ((newMinutes % 60) + 60) % 60
                            if (intervalHours == 12 && wrapped > 0) {
                                intervalMinutes = 0
                            } else if (intervalHours == 0 && wrapped < MIN_INTERVAL_MINUTES) {
                                intervalMinutes = MIN_INTERVAL_MINUTES
                            } else {
                                intervalMinutes = wrapped
                            }
                        },
                        onPickStartTime = { activeTimePicker = ActiveTimePickerTarget.ACTIVE_START },
                        onPickEndTime = { activeTimePicker = ActiveTimePickerTarget.ACTIVE_END },
                    )
                } else {
                    SpecificTimeScheduleCard(
                        hour = specificHour,
                        minute = specificMinute,
                        onChangeHour = { specificHour = ((it % 24) + 24) % 24 },
                        onChangeMinute = { specificMinute = ((it % 60) + 60) % 60 },
                        onOpenClockPicker = {
                            activeTimePicker = ActiveTimePickerTarget.SPECIFIC_TIME
                        },
                    )
                }

                // Pratinjau ringkasan jadwal yang akan tersimpan
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = EditorInnerBg,
                    border = BorderStroke(1.dp, EditorBorder),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AccessTime,
                            contentDescription = null,
                            tint = WaterLightBlue,
                            modifier = Modifier.size(18.dp),
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Ringkasan Jadwal",
                                color = HomeText.copy(alpha = 0.68f),
                                fontFamily = Poppins,
                                fontSize = 11.sp,
                            )
                            Text(
                                text = formatAlarmScheduleSummary(previewAlarm),
                                color = HomeText,
                                fontFamily = Poppins,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }

                // Pengaturan pengulangan
                RepeatSettingsCard(
                    repeatMode = repeatMode,
                    selectedDays = customDaysSet,
                    onSelectRepeatMode = { repeatModeName = it.name },
                    onToggleDay = { day ->
                        val updated = customDaysSet.toMutableSet()
                        if (day in updated) {
                            if (updated.size > 1) updated.remove(day)
                        } else {
                            updated.add(day)
                        }
                        customDaysList = updated.sorted()
                    },
                )

                // Pengaturan label, nada dering, & getar
                ReminderDetailSettingsCard(
                    label = label,
                    onLabelChange = { label = it.take(60) },
                    ringtoneTitle = ringtoneTitle,
                    onPickRingtone = {
                        if (isPreview) return@ReminderDetailSettingsCard
                        val defaultUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                        val existingUri = ringtoneUri?.let { runCatching { it.toUri() }.getOrNull() }
                            ?: defaultUri
                        val pickerIntent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_TYPE,
                                RingtoneManager.TYPE_NOTIFICATION,
                            )
                            putExtra(
                                RingtoneManager.EXTRA_RINGTONE_TITLE,
                                "Pilih Nada Pengingat Minum",
                            )
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI, defaultUri)
                            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, existingUri)
                        }
                        runCatching { ringtonePickerLauncher.launch(pickerIntent) }
                    },
                    vibrate = vibrate,
                    onVibrateChange = { vibrate = it },
                )

                Spacer(modifier = Modifier.height(8.dp))
            }
        }

        // Dialog pemilih jam Material 3 untuk Jam Aktif maupun Jam Tertentu
        activeTimePicker?.let { target ->
            val (dialogTitle, initialH, initialM) = when (target) {
                ActiveTimePickerTarget.ACTIVE_START -> Triple(
                    "Jam Aktif Mulai",
                    activeStartHour,
                    activeStartMinute,
                )
                ActiveTimePickerTarget.ACTIVE_END -> Triple(
                    "Jam Aktif Selesai",
                    activeEndHour,
                    activeEndMinute,
                )
                ActiveTimePickerTarget.SPECIFIC_TIME -> Triple(
                    "Pilih Jam Pengingat",
                    specificHour,
                    specificMinute,
                )
            }
            DrinkTimePickerDialog(
                title = dialogTitle,
                initialHour = initialH,
                initialMinute = initialM,
                onDismiss = { activeTimePicker = null },
                onConfirm = { pickedHour, pickedMinute ->
                    when (target) {
                        ActiveTimePickerTarget.ACTIVE_START -> {
                            activeStartHour = pickedHour
                            activeStartMinute = pickedMinute
                            val startTotal = pickedHour * 60 + pickedMinute
                            val endTotal = activeEndHour * 60 + activeEndMinute
                            if (endTotal < startTotal) {
                                activeEndHour = pickedHour
                                activeEndMinute = pickedMinute
                            }
                        }
                        ActiveTimePickerTarget.ACTIVE_END -> {
                            val startTotal = activeStartHour * 60 + activeStartMinute
                            val pickedTotal = pickedHour * 60 + pickedMinute
                            if (pickedTotal < startTotal) {
                                activeEndHour = activeStartHour
                                activeEndMinute = activeStartMinute
                            } else {
                                activeEndHour = pickedHour
                                activeEndMinute = pickedMinute
                            }
                        }
                        ActiveTimePickerTarget.SPECIFIC_TIME -> {
                            specificHour = pickedHour
                            specificMinute = pickedMinute
                        }
                    }
                    activeTimePicker = null
                },
            )
        }

        DrinkSuccessPopupOverlay(
            visible = showSuccessPopup,
            subtitle = if (isEditMode) {
                "Pengingat minum anda berhasil diperbarui"
            } else {
                "Pengingat minum anda berhasil ditambahkan"
            },
            onContinue = {
                showSuccessPopup = false
                onCompleted()
            },
        )
    }
}

@Composable
private fun EditorTopBar(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HomeNavy)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            KuyBackButton(onClick = onBack)
            Text(
                text = title,
                color = HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun ScheduleModeSegmentedToggle(
    selectedMode: DrinkScheduleMode,
    onModeSelected: (DrinkScheduleMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = EditorInnerBg,
        border = BorderStroke(1.dp, EditorBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SegmentedTabItem(
                label = "Interval",
                selected = selectedMode == DrinkScheduleMode.INTERVAL,
                onClick = { onModeSelected(DrinkScheduleMode.INTERVAL) },
                modifier = Modifier.weight(1f),
            )
            SegmentedTabItem(
                label = "Jam Tertentu",
                selected = selectedMode == DrinkScheduleMode.SPECIFIC_TIME,
                onClick = { onModeSelected(DrinkScheduleMode.SPECIFIC_TIME) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SegmentedTabItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) KuyBlue else Color.Transparent,
        contentColor = if (selected) Color.White else HomeText.copy(alpha = 0.72f),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                fontFamily = Poppins,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun IntervalScheduleCard(
    intervalHours: Int,
    intervalMinutes: Int,
    activeStartHour: Int,
    activeStartMinute: Int,
    activeEndHour: Int,
    activeEndMinute: Int,
    onSelectPresetMinutes: (Int) -> Unit,
    onChangeHours: (Int) -> Unit,
    onChangeMinutes: (Int) -> Unit,
    onPickStartTime: () -> Unit,
    onPickEndTime: () -> Unit,
) {
    val totalMinutes = intervalHours * 60 + intervalMinutes
    val presets = listOf(30 to "30 mnt", 60 to "1 jam", 120 to "2 jam")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = EditorCardBg,
        border = BorderStroke(1.dp, EditorBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Frekuensi Pengingat",
                color = HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )

            // Preset cepat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                presets.forEach { (presetMinutes, label) ->
                    val isSelected = totalMinutes == presetMinutes
                    Surface(
                        onClick = { onSelectPresetMinutes(presetMinutes) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) KuyBlue else EditorInnerBg,
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) WaterLightBlue else EditorBorder,
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else HomeText.copy(alpha = 0.85f),
                                fontFamily = Poppins,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            // Custom Jam : Menit
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(EditorInnerBg)
                    .padding(vertical = 12.dp, horizontal = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Atur Custom (Jam : Menit)",
                    color = HomeText.copy(alpha = 0.7f),
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    TimeNumberStepper(
                        value = intervalHours,
                        unitLabel = "Jam",
                        onIncrement = { onChangeHours(intervalHours + 1) },
                        onDecrement = { onChangeHours(intervalHours - 1) },
                        canIncrement = intervalHours < 12,
                        canDecrement = intervalHours > 0,
                    )
                    Text(
                        text = ":",
                        color = WaterLightBlue,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp,
                        modifier = Modifier.padding(bottom = 18.dp),
                    )
                    TimeNumberStepper(
                        value = intervalMinutes,
                        unitLabel = "Menit",
                        onIncrement = { onChangeMinutes(intervalMinutes + 1) },
                        onDecrement = { onChangeMinutes(intervalMinutes - 1) },
                        canIncrement = intervalHours < 12,
                        canDecrement = !(intervalHours == 0 && intervalMinutes <= MIN_INTERVAL_MINUTES),
                    )
                }
                Text(
                    text = "Minimal 1 menit untuk pengujian cepat",
                    color = HomeText.copy(alpha = 0.55f),
                    fontFamily = Poppins,
                    fontSize = 10.sp,
                )
            }

            // Pengaturan Jam Aktif
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Jam Aktif",
                    color = HomeText,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Text(
                    text = "Pengingat hanya berbunyi pada rentang jam ini agar tidak mengganggu waktu tidur.",
                    color = HomeText.copy(alpha = 0.68f),
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                    lineHeight = 16.sp,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    ActiveWindowTimeTile(
                        caption = "Mulai",
                        timeText = formatTimeHHmm(activeStartHour, activeStartMinute),
                        onClick = onPickStartTime,
                        modifier = Modifier.weight(1f),
                    )
                    ActiveWindowTimeTile(
                        caption = "Selesai",
                        timeText = formatTimeHHmm(activeEndHour, activeEndMinute),
                        onClick = onPickEndTime,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveWindowTimeTile(
    caption: String,
    timeText: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = EditorInnerBg,
        border = BorderStroke(1.dp, EditorBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column {
                Text(
                    text = caption,
                    color = HomeText.copy(alpha = 0.65f),
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                )
                Text(
                    text = timeText,
                    color = HomeText,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }
            Icon(
                imageVector = Icons.Rounded.AccessTime,
                contentDescription = "Ubah $caption",
                tint = WaterLightBlue,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}

@Composable
private fun SpecificTimeScheduleCard(
    hour: Int,
    minute: Int,
    onChangeHour: (Int) -> Unit,
    onChangeMinute: (Int) -> Unit,
    onOpenClockPicker: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = EditorCardBg,
        border = BorderStroke(1.dp, EditorBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Pilih Jam Pengingat",
                color = HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(EditorInnerBg)
                    .padding(vertical = 18.dp, horizontal = 12.dp),
                contentAlignment = Alignment.Center,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    TimeNumberStepper(
                        value = hour,
                        unitLabel = "Jam",
                        onIncrement = { onChangeHour(hour + 1) },
                        onDecrement = { onChangeHour(hour - 1) },
                        largeDisplay = true,
                    )
                    Text(
                        text = ":",
                        color = WaterLightBlue,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 42.sp,
                        modifier = Modifier.padding(bottom = 20.dp),
                    )
                    TimeNumberStepper(
                        value = minute,
                        unitLabel = "Menit",
                        onIncrement = { onChangeMinute(minute + 1) },
                        onDecrement = { onChangeMinute(minute - 1) },
                        largeDisplay = true,
                    )
                }
            }

            TextButton(onClick = onOpenClockPicker) {
                Icon(
                    imageVector = Icons.Rounded.AccessTime,
                    contentDescription = null,
                    tint = WaterLightBlue,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "Pilih lewat Jam Digital",
                    color = WaterLightBlue,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun TimeNumberStepper(
    value: Int,
    unitLabel: String,
    onIncrement: () -> Unit,
    onDecrement: () -> Unit,
    canIncrement: Boolean = true,
    canDecrement: Boolean = true,
    largeDisplay: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        IconButton(
            onClick = onIncrement,
            enabled = canIncrement,
            modifier = Modifier.size(38.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = "Tambah $unitLabel",
                tint = if (canIncrement) WaterLightBlue else HomeText.copy(alpha = 0.28f),
                modifier = Modifier.size(28.dp),
            )
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = EditorCardBg,
            border = BorderStroke(1.dp, EditorBorder),
        ) {
            Box(
                modifier = Modifier
                    .widthIn(min = if (largeDisplay) 86.dp else 72.dp)
                    .padding(horizontal = 14.dp, vertical = if (largeDisplay) 10.dp else 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = String.format(Locale.ROOT, "%02d", value),
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (largeDisplay) 38.sp else 28.sp,
                )
            }
        }

        IconButton(
            onClick = onDecrement,
            enabled = canDecrement,
            modifier = Modifier.size(38.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowDown,
                contentDescription = "Kurangi $unitLabel",
                tint = if (canDecrement) WaterLightBlue else HomeText.copy(alpha = 0.28f),
                modifier = Modifier.size(28.dp),
            )
        }

        Text(
            text = unitLabel,
            color = HomeText.copy(alpha = 0.68f),
            fontFamily = Poppins,
            fontSize = 11.sp,
        )
    }
}

@Composable
private fun RepeatSettingsCard(
    repeatMode: DrinkRepeatMode,
    selectedDays: Set<Int>,
    onSelectRepeatMode: (DrinkRepeatMode) -> Unit,
    onToggleDay: (Int) -> Unit,
) {
    val options = listOf(
        DrinkRepeatMode.DAILY to "Harian",
        DrinkRepeatMode.ONCE to "Sekali",
        DrinkRepeatMode.CUSTOM_DAYS to "Pilih Hari",
    )

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = EditorCardBg,
        border = BorderStroke(1.dp, EditorBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Ulangi",
                color = HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                options.forEach { (mode, title) ->
                    val selected = mode == repeatMode
                    Surface(
                        onClick = { onSelectRepeatMode(mode) },
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp),
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) KuyBlue else EditorInnerBg,
                        border = BorderStroke(
                            1.dp,
                            if (selected) WaterLightBlue else EditorBorder,
                        ),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = title,
                                color = if (selected) Color.White else HomeText.copy(alpha = 0.8f),
                                fontFamily = Poppins,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 13.sp,
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = repeatMode == DrinkRepeatMode.CUSTOM_DAYS) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        (1..7).forEach { isoDay ->
                            val active = isoDay in selectedDays
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(if (active) KuyOrange else EditorInnerBg)
                                    .clickable(role = Role.Checkbox) { onToggleDay(isoDay) },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = dayShortLabelId(isoDay),
                                    color = if (active) Color.White else HomeText.copy(alpha = 0.72f),
                                    fontFamily = Poppins,
                                    fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 11.sp,
                                )
                            }
                        }
                    }
                    Text(
                        text = "Minimal pilih 1 hari aktif",
                        color = HomeText.copy(alpha = 0.58f),
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun ReminderDetailSettingsCard(
    label: String,
    onLabelChange: (String) -> Unit,
    ringtoneTitle: String,
    onPickRingtone: () -> Unit,
    vibrate: Boolean,
    onVibrateChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = EditorCardBg,
        border = BorderStroke(1.dp, EditorBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = "Pengaturan Pengingat",
                color = HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )

            KuyTextField(
                value = label,
                onValueChange = onLabelChange,
                label = "Label pengingat",
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )

            // Pemilih Nada Dering bawaan sistem
            Surface(
                onClick = onPickRingtone,
                shape = RoundedCornerShape(14.dp),
                color = EditorInnerBg,
                border = BorderStroke(1.dp, EditorBorder),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(HomeBlue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            tint = WaterLightBlue,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Nada Dering",
                            color = HomeText,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = ringtoneTitle.ifBlank { SystemDefaultRingtoneLabel },
                            color = HomeText.copy(alpha = 0.68f),
                            fontFamily = Poppins,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = "Pilih Nada Dering",
                        tint = HomeText.copy(alpha = 0.65f),
                    )
                }
            }

            // Toggle Getar
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = EditorInnerBg,
                border = BorderStroke(1.dp, EditorBorder),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(HomeBlue),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Vibration,
                            contentDescription = null,
                            tint = WaterLightBlue,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Getar",
                            color = HomeText,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                        )
                        Text(
                            text = "Getarkan perangkat saat notifikasi muncul",
                            color = HomeText.copy(alpha = 0.68f),
                            fontFamily = Poppins,
                            fontSize = 11.sp,
                        )
                    }
                    Switch(
                        checked = vibrate,
                        onCheckedChange = onVibrateChange,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = KuyBlue,
                            uncheckedThumbColor = HomeText.copy(alpha = 0.8f),
                            uncheckedTrackColor = EditorCardBg,
                            uncheckedBorderColor = EditorBorder,
                        ),
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DrinkTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int, Int) -> Unit,
) {
    val state = rememberTimePickerState(
        initialHour = initialHour.coerceIn(0, 23),
        initialMinute = initialMinute.coerceIn(0, 59),
        is24Hour = true,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = EditorCardBg,
        titleContentColor = HomeText,
        textContentColor = HomeText,
        title = {
            Text(
                text = title,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
        },
        text = {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                TimePicker(
                    state = state,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = EditorInnerBg,
                        clockDialSelectedContentColor = Color.White,
                        clockDialUnselectedContentColor = HomeText,
                        selectorColor = KuyBlue,
                        containerColor = EditorCardBg,
                        timeSelectorSelectedContainerColor = KuyBlue,
                        timeSelectorUnselectedContainerColor = EditorInnerBg,
                        timeSelectorSelectedContentColor = Color.White,
                        timeSelectorUnselectedContentColor = HomeText,
                    ),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(
                    text = "Pilih",
                    color = HomeOrange,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Batal",
                    color = HomeText.copy(alpha = 0.78f),
                    fontFamily = Poppins,
                )
            }
        },
    )
}

/**
 * Overlay pop-up sukses bergaya identik dengan `KuySuccessPopupOverlay`,
 * memakai ilustrasi tetes air & centang dari Canvas tanpa menambah file drawable baru.
 */
@Composable
private fun DrinkSuccessPopupOverlay(
    visible: Boolean,
    subtitle: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Selamat",
) {
    BackHandler(enabled = visible) { }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(600, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(200)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(600, easing = FastOutSlowInEasing),
                        ) + fadeIn(
                            animationSpec = tween(600, easing = FastOutSlowInEasing),
                        ),
                    )
                    .fillMaxWidth(0.88f)
                    .widthIn(max = 380.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = EditorPopupBg),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    HydrationSuccessIllustration(modifier = Modifier.size(110.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = KuyText,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KuyButton,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "Lanjutkan",
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HydrationSuccessIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val center = Offset(w / 2f, h / 2f)

        // Lingkaran latar lembut
        drawCircle(
            color = WaterMidBlue.copy(alpha = 0.22f),
            radius = w * 0.46f,
            center = center,
        )
        drawCircle(
            color = WaterLightBlue.copy(alpha = 0.28f),
            radius = w * 0.36f,
            center = center,
        )

        // Bentuk tetes air
        val dropPath = Path().apply {
            moveTo(w * 0.50f, h * 0.16f)
            cubicTo(
                w * 0.72f, h * 0.43f,
                w * 0.76f, h * 0.62f,
                w * 0.68f, h * 0.74f,
            )
            cubicTo(
                w * 0.60f, h * 0.85f,
                w * 0.40f, h * 0.85f,
                w * 0.32f, h * 0.74f,
            )
            cubicTo(
                w * 0.24f, h * 0.62f,
                w * 0.28f, h * 0.43f,
                w * 0.50f, h * 0.16f,
            )
            close()
        }
        drawPath(
            path = dropPath,
            brush = Brush.verticalGradient(
                colors = listOf(WaterLightBlue, KuyBlue),
                startY = h * 0.16f,
                endY = h * 0.84f,
            ),
        )

        // Kilau kecil pada tetes air
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = w * 0.045f,
            center = Offset(w * 0.42f, h * 0.52f),
        )

        // Badge centang oranye di kanan bawah
        val badgeCenter = Offset(w * 0.70f, h * 0.72f)
        val badgeRadius = w * 0.17f
        drawCircle(
            color = EditorPopupBg,
            radius = badgeRadius + 3.dp.toPx(),
            center = badgeCenter,
        )
        drawCircle(
            color = KuyOrange,
            radius = badgeRadius,
            center = badgeCenter,
        )
        val checkPath = Path().apply {
            moveTo(badgeCenter.x - badgeRadius * 0.42f, badgeCenter.y + badgeRadius * 0.02f)
            lineTo(badgeCenter.x - badgeRadius * 0.10f, badgeCenter.y + badgeRadius * 0.34f)
            lineTo(badgeCenter.x + badgeRadius * 0.44f, badgeCenter.y - badgeRadius * 0.28f)
        }
        drawPath(
            path = checkPath,
            color = Color.White,
            style = Stroke(
                width = 3.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

@Preview(name = "Tambah Alarm (Interval)", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun DrinkAlarmEditorIntervalPreview() {
    OlahragaKuyTheme {
        DrinkAlarmEditorScreen(
            initialAlarm = null,
            onBack = {},
            onSaveAlarm = {},
            onCompleted = {},
        )
    }
}

@Preview(name = "Edit Alarm (Jam Tertentu)", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun DrinkAlarmEditorSpecificPreview() {
    OlahragaKuyTheme {
        DrinkAlarmEditorScreen(
            initialAlarm = DrinkAlarm(
                id = "demo-1",
                label = "Minum setelah olahraga",
                scheduleMode = DrinkScheduleMode.SPECIFIC_TIME,
                specificHour = 8,
                specificMinute = 30,
                repeatMode = DrinkRepeatMode.CUSTOM_DAYS,
                customDays = setOf(1, 3, 5),
                ringtoneTitle = "Default Sistem",
                vibrate = true,
            ),
            onBack = {},
            onSaveAlarm = {},
            onCompleted = {},
        )
    }
}
