package com.example.olahragakuy.pages.drinkreminder

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccessTime
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.components.KuyPrimaryButton
import com.example.olahragakuy.ui.components.KuyTextField
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyBlue
import com.example.olahragakuy.ui.theme.KuyError
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import kotlin.math.PI
import kotlin.math.sin
import kotlin.time.Duration.Companion.seconds

private val CardSurface = Color(0xFF16324F)
private val InnerSurface = Color(0xFF0C2138)
private val CardBorder = Color(0xFF284B70)
private val WaterLight = Color(0xFF8AB5EE)
private val WaterMid = Color(0xFF4C80C8)
private val WaterDeep = Color(0xFF2E63B3)
private val BannerAmber = Color(0xFFFFB74D)

private sealed interface DrinkEditorTarget {
    data object Create : DrinkEditorTarget
    data class Edit(val alarm: DrinkAlarm) : DrinkEditorTarget
}

/**
 * Entry point tunggal fitur Pengingat Minum yang mengorkestrasi Layar Utama
 * dan [DrinkAlarmEditorScreen] (mode Tambah & Edit) lengkap dengan [BackHandler].
 */
@Composable
fun DrinkReminderScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current

    var uiState by remember {
        mutableStateOf(
            if (isPreview) {
                previewFilledState()
            } else {
                buildDrinkReminderUiState(
                    progress = DrinkReminderStorage.loadHydrationProgress(context),
                    alarms = DrinkReminderStorage.loadAlarms(context),
                )
            },
        )
    }
    var editorTarget by remember { mutableStateOf<DrinkEditorTarget?>(null) }
    var alarmPendingDelete by remember { mutableStateOf<DrinkAlarm?>(null) }
    var showTargetDialog by remember { mutableStateOf(false) }
    var hasNotificationPermission by remember {
        mutableStateOf(
            if (isPreview) true else DrinkReminderScheduler.hasPostNotificationsPermission(context),
        )
    }

    val refreshState: () -> Unit = {
        if (!isPreview) {
            uiState = buildDrinkReminderUiState(
                progress = DrinkReminderStorage.loadHydrationProgress(context),
                alarms = DrinkReminderStorage.loadAlarms(context),
                now = LocalDateTime.now(),
            )
        }
    }

    com.example.olahragakuy.ui.components.RegisterKuyRefresh {
        refreshState()
        if (!isPreview) {
            hasNotificationPermission = DrinkReminderScheduler.hasPostNotificationsPermission(context)
        }
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        hasNotificationPermission = granted
    }

    val requestNotificationPermissionIfNeeded: () -> Unit = {
        if (
            !isPreview &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !DrinkReminderScheduler.hasPostNotificationsPermission(context)
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    if (!isPreview) {
        // Sinkronisasi real-time jika progres atau status alarm diubah dari panel notifikasi / receiver.
        DisposableEffect(context) {
            val unregister = DrinkReminderStorage.registerOnChangeListener(context) {
                refreshState()
            }
            onDispose { unregister() }
        }

        // Perbarui hitung mundur alarm terdekat dan deteksi pergantian hari (00:00) secara berkala.
        LaunchedEffect(Unit) {
            refreshState()
            while (true) {
                delay(15.seconds)
                hasNotificationPermission = DrinkReminderScheduler.hasPostNotificationsPermission(context)
                refreshState()
            }
        }
    }

    BackHandler(enabled = editorTarget != null) {
        editorTarget = null
    }

    AnimatedContent(
        targetState = editorTarget,
        transitionSpec = {
            fadeIn(tween(220)) togetherWith fadeOut(tween(180))
        },
        label = "drink_reminder_nav",
        modifier = modifier.fillMaxSize(),
    ) { target ->
        if (target != null) {
            val initialAlarm = (target as? DrinkEditorTarget.Edit)?.alarm
            DrinkAlarmEditorScreen(
                initialAlarm = initialAlarm,
                onBack = { editorTarget = null },
                onSaveAlarm = { savedAlarm ->
                    if (!isPreview) {
                        val updatedAlarms = DrinkReminderStorage.upsertAlarm(context, savedAlarm)
                        DrinkReminderScheduler.scheduleAlarm(context, savedAlarm)
                        uiState = buildDrinkReminderUiState(
                            progress = DrinkReminderStorage.loadHydrationProgress(context),
                            alarms = updatedAlarms,
                        )
                        requestNotificationPermissionIfNeeded()
                    } else {
                        val nextAlarms = uiState.alarms.filterNot { it.id == savedAlarm.id } + savedAlarm
                        uiState = buildDrinkReminderUiState(uiState.progress, nextAlarms)
                    }
                },
                onCompleted = {
                    editorTarget = null
                    refreshState()
                },
            )
        } else {
            DrinkReminderMainContent(
                uiState = uiState,
                showNotificationPermissionBanner = !hasNotificationPermission &&
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                onBack = onBack,
                onRequestNotificationPermission = requestNotificationPermissionIfNeeded,
                onAddWater = {
                    if (!isPreview) {
                        val updated = DrinkReminderStorage.addWaterIntake(context, DEFAULT_DRINK_STEP_ML)
                        uiState = buildDrinkReminderUiState(updated, uiState.alarms)
                    } else {
                        val updated = uiState.progress.copy(
                            consumedMl = uiState.progress.consumedMl + DEFAULT_DRINK_STEP_ML,
                        )
                        uiState = uiState.copy(progress = updated)
                    }
                },
                onUndoWater = {
                    if (!isPreview) {
                        val updated = DrinkReminderStorage.addWaterIntake(context, -DEFAULT_DRINK_STEP_ML)
                        uiState = buildDrinkReminderUiState(updated, uiState.alarms)
                    } else {
                        val updated = uiState.progress.copy(
                            consumedMl = (uiState.progress.consumedMl - DEFAULT_DRINK_STEP_ML).coerceAtLeast(0),
                        )
                        uiState = uiState.copy(progress = updated)
                    }
                },
                onEditTargetClick = { showTargetDialog = true },
                onAddAlarmClick = {
                    requestNotificationPermissionIfNeeded()
                    editorTarget = DrinkEditorTarget.Create
                },
                onAlarmClick = { alarm -> editorTarget = DrinkEditorTarget.Edit(alarm) },
                onToggleAlarm = { alarm, enabled ->
                    if (!isPreview) {
                        val updatedAlarms = DrinkReminderStorage.setAlarmEnabled(context, alarm.id, enabled)
                        val updatedAlarm = alarm.copy(isEnabled = enabled)
                        if (enabled) {
                            requestNotificationPermissionIfNeeded()
                            DrinkReminderScheduler.scheduleAlarm(context, updatedAlarm)
                        } else {
                            DrinkReminderScheduler.cancelAlarm(context, alarm.id)
                        }
                        uiState = buildDrinkReminderUiState(
                            progress = DrinkReminderStorage.loadHydrationProgress(context),
                            alarms = updatedAlarms,
                        )
                    } else {
                        val updatedAlarms = uiState.alarms.map {
                            if (it.id == alarm.id) it.copy(isEnabled = enabled) else it
                        }
                        uiState = buildDrinkReminderUiState(uiState.progress, updatedAlarms)
                    }
                },
                onDeleteAlarmClick = { alarmPendingDelete = it },
            )
        }
    }

    if (showTargetDialog) {
        EditDailyTargetDialog(
            currentTargetMl = uiState.progress.targetMl,
            onDismiss = { showTargetDialog = false },
            onSaveTarget = { newTargetMl ->
                if (!isPreview) {
                    val updated = DrinkReminderStorage.updateDailyTarget(context, newTargetMl)
                    uiState = buildDrinkReminderUiState(updated, uiState.alarms)
                } else {
                    uiState = uiState.copy(
                        progress = uiState.progress.copy(targetMl = newTargetMl),
                    )
                }
                showTargetDialog = false
            },
        )
    }

    alarmPendingDelete?.let { alarm ->
        AlertDialog(
            onDismissRequest = { alarmPendingDelete = null },
            containerColor = CardSurface,
            titleContentColor = HomeText,
            textContentColor = HomeText,
            title = {
                Text(
                    text = "Hapus Alarm?",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            },
            text = {
                Text(
                    text = "Pengingat \"${alarm.label}\" (${formatAlarmScheduleSummary(alarm)}) akan dihapus dan tidak lagi berbunyi.",
                    fontFamily = Poppins,
                    fontSize = 13.sp,
                    color = HomeText.copy(alpha = 0.85f),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (!isPreview) {
                            DrinkReminderScheduler.cancelAlarm(context, alarm.id)
                            val updatedAlarms = DrinkReminderStorage.deleteAlarm(context, alarm.id)
                            uiState = buildDrinkReminderUiState(
                                progress = DrinkReminderStorage.loadHydrationProgress(context),
                                alarms = updatedAlarms,
                            )
                        } else {
                            val updatedAlarms = uiState.alarms.filterNot { it.id == alarm.id }
                            uiState = buildDrinkReminderUiState(uiState.progress, updatedAlarms)
                        }
                        alarmPendingDelete = null
                    },
                ) {
                    Text(
                        text = "Hapus",
                        color = KuyError,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { alarmPendingDelete = null }) {
                    Text(
                        text = "Batal",
                        color = HomeText.copy(alpha = 0.8f),
                        fontFamily = Poppins,
                    )
                }
            },
        )
    }
}

@Composable
private fun DrinkReminderMainContent(
    uiState: DrinkReminderUiState,
    showNotificationPermissionBanner: Boolean,
    onBack: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onAddWater: () -> Unit,
    onUndoWater: () -> Unit,
    onEditTargetClick: () -> Unit,
    onAddAlarmClick: () -> Unit,
    onAlarmClick: (DrinkAlarm) -> Unit,
    onToggleAlarm: (DrinkAlarm, Boolean) -> Unit,
    onDeleteAlarmClick: (DrinkAlarm) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeAlarmCount = uiState.alarms.count { it.isEnabled }

    Scaffold(
        modifier = modifier,
        containerColor = HomeNavy,
        contentColor = HomeText,
        topBar = {
            DrinkReminderTopBar(onBack = onBack)
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
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    KuyPrimaryButton(
                        text = "+ Tambah Alarm",
                        enabled = true,
                        onClick = onAddAlarmClick,
                    )
                }
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (showNotificationPermissionBanner) {
                item(key = "notif_permission_banner") {
                    NotificationPermissionBanner(
                        onAllowClick = onRequestNotificationPermission,
                    )
                }
            }

            item(key = "hydration_card") {
                HydrationSummaryCard(
                    progress = uiState.progress,
                    countdownText = uiState.countdownText,
                    hasActiveAlarm = uiState.nextAlarmInfo != null,
                    onAddWater = onAddWater,
                    onUndoWater = onUndoWater,
                    onEditTargetClick = onEditTargetClick,
                )
            }

            item(key = "alarm_section_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Alarm minum anda",
                        color = HomeText,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                    )
                    if (uiState.alarms.isNotEmpty()) {
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = InnerSurface,
                            border = BorderStroke(1.dp, CardBorder),
                        ) {
                            Text(
                                text = "$activeAlarmCount aktif",
                                color = if (activeAlarmCount > 0) WaterLight else HomeText.copy(alpha = 0.6f),
                                fontFamily = Poppins,
                                fontWeight = FontWeight.Medium,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            )
                        }
                    }
                }
            }

            if (uiState.alarms.isEmpty()) {
                item(key = "empty_alarms") {
                    EmptyAlarmsCard()
                }
            } else {
                items(
                    items = uiState.alarms,
                    key = { it.id },
                ) { alarm ->
                    DrinkAlarmItemCard(
                        alarm = alarm,
                        onClick = { onAlarmClick(alarm) },
                        onToggle = { enabled -> onToggleAlarm(alarm, enabled) },
                        onDeleteClick = { onDeleteAlarmClick(alarm) },
                    )
                }
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}

@Composable
private fun DrinkReminderTopBar(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(HomeNavy)
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        KuyBackButton(onClick = onBack)
        Text(
            text = "Pengingat Minum",
            color = HomeText,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun NotificationPermissionBanner(
    onAllowClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, BannerAmber.copy(alpha = 0.55f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.NotificationsOff,
                contentDescription = null,
                tint = BannerAmber,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Izin notifikasi belum aktif",
                    color = HomeText,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Text(
                    text = "Aktifkan agar pengingat minum bisa muncul tepat waktu.",
                    color = HomeText.copy(alpha = 0.72f),
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }
            TextButton(onClick = onAllowClick) {
                Text(
                    text = "Izinkan",
                    color = HomeOrange,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                )
            }
        }
    }
}

@Composable
private fun HydrationSummaryCard(
    progress: HydrationProgress,
    countdownText: String,
    hasActiveAlarm: Boolean,
    onAddWater: () -> Unit,
    onUndoWater: () -> Unit,
    onEditTargetClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val fraction = calculateHydrationFraction(progress.consumedMl, progress.targetMl)
    val percent = calculateHydrationPercent(progress.consumedMl, progress.targetMl)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "hydration_fraction",
    )

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, CardBorder),
        shadowElevation = 4.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Baris judul & tombol edit target harian
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Progres Hidrasi Hari Ini",
                    color = HomeText,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
                Surface(
                    onClick = onEditTargetClick,
                    shape = RoundedCornerShape(10.dp),
                    color = InnerSurface,
                    border = BorderStroke(1.dp, CardBorder),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Text(
                            text = "Target ${formatVolumeMl(progress.targetMl)} ml",
                            color = WaterLight,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                        )
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Ubah target harian",
                            tint = WaterLight,
                            modifier = Modifier.size(13.dp),
                        )
                    }
                }
            }

            // Lingkaran air animasi
            AnimatedWaterCircle(
                fillFraction = animatedFraction,
                percent = percent,
                progressText = formatHydrationProgressText(progress.consumedMl, progress.targetMl),
                modifier = Modifier.size(172.dp),
            )

            // Pill hitung mundur alarm terdekat
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                color = InnerSurface,
                border = BorderStroke(
                    1.dp,
                    if (hasActiveAlarm) WaterMid.copy(alpha = 0.65f) else CardBorder,
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.AccessTime,
                        contentDescription = null,
                        tint = if (hasActiveAlarm) WaterLight else HomeText.copy(alpha = 0.55f),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = countdownText,
                        color = if (hasActiveAlarm) HomeText else HomeText.copy(alpha = 0.7f),
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            // Progress bar biru + keterangan persentase
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (progress.consumedMl >= progress.targetMl) {
                            "Target harian tercapai!"
                        } else {
                            "Sisa ${formatVolumeMl((progress.targetMl - progress.consumedMl).coerceAtLeast(0))} ml lagi"
                        },
                        color = HomeText.copy(alpha = 0.76f),
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                    )
                    Text(
                        text = "$percent%",
                        color = WaterLight,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(InnerSurface),
                ) {
                    if (animatedFraction > 0f) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedFraction.coerceIn(0.02f, 1f))
                                .clip(RoundedCornerShape(999.dp))
                                .background(Brush.horizontalGradient(listOf(WaterMid, WaterLight))),
                        )
                    }
                }
            }

            // Tombol cepat + Minum (+250 ml) & Undo (-250 ml)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onUndoWater,
                    enabled = progress.consumedMl > 0,
                    modifier = Modifier.height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(
                        1.dp,
                        if (progress.consumedMl > 0) CardBorder else CardBorder.copy(alpha = 0.4f),
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = InnerSurface,
                        contentColor = HomeText,
                        disabledContainerColor = InnerSurface.copy(alpha = 0.5f),
                        disabledContentColor = HomeText.copy(alpha = 0.35f),
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Kurangi $DEFAULT_DRINK_STEP_ML ml",
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "$DEFAULT_DRINK_STEP_ML ml",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 12.sp,
                    )
                }

                Button(
                    onClick = onAddWater,
                    modifier = Modifier
                        .weight(1f)
                        .height(46.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KuyBlue,
                        contentColor = Color.White,
                    ),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Minum (+$DEFAULT_DRINK_STEP_ML ml)",
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun AnimatedWaterCircle(
    fillFraction: Float,
    percent: Int,
    progressText: String,
    modifier: Modifier = Modifier,
) {
    val isPreview = LocalInspectionMode.current
    val wavePhase = if (isPreview) {
        0.4f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "water_wave")
        val animatedPhase by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = (2.0 * PI).toFloat(),
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
            label = "wave_phase",
        )
        animatedPhase
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 4.dp.toPx()
            val diameter = size.minDimension - strokePx
            val left = (size.width - diameter) / 2f
            val top = (size.height - diameter) / 2f
            val circleRect = Rect(left, top, left + diameter, top + diameter)
            val circlePath = Path().apply { addOval(circleRect) }

            // Dasar lingkaran gelap
            drawCircle(
                color = InnerSurface,
                radius = diameter / 2f,
                center = center,
            )

            // Isi gelombang air di dalam lingkaran
            clipPath(circlePath) {
                val clamped = fillFraction.coerceIn(0f, 1f)
                if (clamped > 0.005f) {
                    val waterTopY = circleRect.bottom - (diameter * clamped)
                    val amplitude = if (clamped >= 0.98f) 2.dp.toPx() else 6.dp.toPx()
                    val stepPx = 6f

                    val backWave = Path().apply {
                        moveTo(circleRect.left, circleRect.bottom)
                        var x = circleRect.left
                        while (x <= circleRect.right + stepPx) {
                            val ratio = (x - circleRect.left) / diameter
                            val y = waterTopY + sin(ratio * 2f * PI.toFloat() + wavePhase + 1.4f) *
                                (amplitude * 0.85f)
                            lineTo(x, y)
                            x += stepPx
                        }
                        lineTo(circleRect.right, circleRect.bottom)
                        close()
                    }
                    drawPath(
                        path = backWave,
                        color = WaterDeep.copy(alpha = 0.8f),
                    )

                    val frontWave = Path().apply {
                        moveTo(circleRect.left, circleRect.bottom)
                        var x = circleRect.left
                        while (x <= circleRect.right + stepPx) {
                            val ratio = (x - circleRect.left) / diameter
                            val y = waterTopY + sin(ratio * 2f * PI.toFloat() + wavePhase) * amplitude
                            lineTo(x, y)
                            x += stepPx
                        }
                        lineTo(circleRect.right, circleRect.bottom)
                        close()
                    }
                    drawPath(
                        path = frontWave,
                        brush = Brush.verticalGradient(
                            colors = listOf(WaterLight, WaterMid),
                            startY = waterTopY - amplitude,
                            endY = circleRect.bottom,
                        ),
                    )
                }
            }

            // Cincin luar gradien biru
            drawCircle(
                brush = Brush.sweepGradient(
                    colors = listOf(WaterMid, WaterLight, KuyBlue, WaterMid),
                ),
                radius = diameter / 2f,
                center = center,
                style = Stroke(width = strokePx),
            )
            // Kilau halus di sudut kiri atas
            drawCircle(
                color = Color.White.copy(alpha = 0.14f),
                radius = diameter * 0.06f,
                center = Offset(circleRect.left + diameter * 0.25f, circleRect.top + diameter * 0.22f),
            )
        }

        // Badge teks angka nyata agar selalu kontras di semua ketinggian air
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(HomeNavy.copy(alpha = 0.58f))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Rounded.WaterDrop,
                contentDescription = null,
                tint = WaterLight,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "$percent%",
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            )
            Text(
                text = progressText,
                color = HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun DrinkAlarmItemCard(
    alarm: DrinkAlarm,
    onClick: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contentAlpha = if (alarm.isEnabled) 1f else 0.58f
    val modeBadgeText = when (alarm.scheduleMode) {
        DrinkScheduleMode.INTERVAL -> "Interval"
        DrinkScheduleMode.SPECIFIC_TIME -> "Jam Tertentu"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = CardSurface,
        border = BorderStroke(
            1.dp,
            if (alarm.isEnabled) WaterMid.copy(alpha = 0.55f) else CardBorder.copy(alpha = 0.65f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 14.dp, end = 8.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (alarm.isEnabled) HomeBlue else InnerSurface,
                ) {
                    Text(
                        text = modeBadgeText,
                        color = WaterLight.copy(alpha = contentAlpha),
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }

                Text(
                    text = formatAlarmScheduleSummary(alarm),
                    color = HomeText.copy(alpha = contentAlpha),
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                )

                Text(
                    text = alarm.label.ifBlank { DEFAULT_ALARM_LABEL },
                    color = HomeText.copy(alpha = contentAlpha * 0.78f),
                    fontFamily = Poppins,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = KuyBlue,
                        uncheckedThumbColor = HomeText.copy(alpha = 0.78f),
                        uncheckedTrackColor = InnerSurface,
                        uncheckedBorderColor = CardBorder,
                    ),
                )
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(36.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Hapus alarm",
                        tint = HomeText.copy(alpha = 0.65f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyAlarmsCard(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = BorderStroke(1.dp, CardBorder),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(InnerSurface),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.WaterDrop,
                    contentDescription = null,
                    tint = WaterLight,
                    modifier = Modifier.size(28.dp),
                )
            }
            Text(
                text = "Belum ada alarm minum",
                color = HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Tambahkan pengingat pertama dengan mode Interval atau Jam Tertentu agar tubuh tetap terhidrasi.",
                color = HomeText.copy(alpha = 0.7f),
                fontFamily = Poppins,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EditDailyTargetDialog(
    currentTargetMl: Int,
    onDismiss: () -> Unit,
    onSaveTarget: (Int) -> Unit,
) {
    var inputText by remember(currentTargetMl) { mutableStateOf(currentTargetMl.toString()) }
    val presets = listOf(1500, 2000, 2500, 3000)
    val parsedValue = inputText.toIntOrNull()
    val errorText = when {
        inputText.isBlank() -> "Masukkan target minum harian"
        parsedValue == null -> "Gunakan angka saja"
        parsedValue < MIN_DAILY_TARGET_ML -> "Minimal ${formatVolumeMl(MIN_DAILY_TARGET_ML)} ml"
        parsedValue > MAX_DAILY_TARGET_ML -> "Maksimal ${formatVolumeMl(MAX_DAILY_TARGET_ML)} ml"
        else -> null
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardSurface,
        titleContentColor = HomeText,
        textContentColor = HomeText,
        title = {
            Text(
                text = "Target Minum Harian",
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pilih preset cepat atau masukkan target harian anda (ml):",
                    color = HomeText.copy(alpha = 0.78f),
                    fontFamily = Poppins,
                    fontSize = 12.sp,
                )

                // Preset 2x2
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.chunked(2).forEach { rowPresets ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            rowPresets.forEach { presetMl ->
                                val selected = parsedValue == presetMl
                                Surface(
                                    onClick = { inputText = presetMl.toString() },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(38.dp),
                                    shape = RoundedCornerShape(10.dp),
                                    color = if (selected) KuyBlue else InnerSurface,
                                    border = BorderStroke(
                                        1.dp,
                                        if (selected) WaterLight else CardBorder,
                                    ),
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "${formatVolumeMl(presetMl)} ml",
                                            color = if (selected) Color.White else HomeText,
                                            fontFamily = Poppins,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                KuyTextField(
                    value = inputText,
                    onValueChange = { raw ->
                        inputText = raw.filter { it.isDigit() }.take(5)
                    },
                    label = "Target custom (ml)",
                    errorMessage = errorText,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                )

                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = KuyError,
                        fontFamily = Poppins,
                        fontSize = 11.sp,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = errorText == null,
                onClick = { parsedValue?.let(onSaveTarget) },
            ) {
                Text(
                    text = "Simpan",
                    color = if (errorText == null) HomeOrange else HomeText.copy(alpha = 0.4f),
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

private fun previewFilledState(): DrinkReminderUiState {
    val sampleAlarms = listOf(
        DrinkAlarm(
            id = "preview-1",
            label = "Waktunya Minum Air!",
            scheduleMode = DrinkScheduleMode.INTERVAL,
            intervalMinutes = 30,
            activeStartHour = 7,
            activeStartMinute = 0,
            activeEndHour = 21,
            activeEndMinute = 0,
            repeatMode = DrinkRepeatMode.DAILY,
            isEnabled = true,
        ),
        DrinkAlarm(
            id = "preview-2",
            label = "Minum setelah latihan pagi",
            scheduleMode = DrinkScheduleMode.SPECIFIC_TIME,
            specificHour = 8,
            specificMinute = 0,
            repeatMode = DrinkRepeatMode.DAILY,
            isEnabled = true,
        ),
        DrinkAlarm(
            id = "preview-3",
            label = "Hidrasi sore",
            scheduleMode = DrinkScheduleMode.SPECIFIC_TIME,
            specificHour = 16,
            specificMinute = 30,
            repeatMode = DrinkRepeatMode.CUSTOM_DAYS,
            customDays = setOf(1, 3, 5),
            isEnabled = false,
        ),
    )
    return DrinkReminderUiState(
        progress = HydrationProgress(
            consumedMl = 1250,
            targetMl = 2000,
            dateKey = "2026-11-23",
        ),
        alarms = sampleAlarms,
        nextAlarmInfo = NextAlarmInfo(
            alarm = sampleAlarms.first(),
            triggerAtMillis = 0L,
            remainingMinutes = 25L,
        ),
        countdownText = "Alarm selanjutnya dalam 00:25",
    )
}

@Preview(name = "Pengingat Minum (Terisi)", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun DrinkReminderScreenFilledPreview() {
    OlahragaKuyTheme {
        DrinkReminderMainContent(
            uiState = previewFilledState(),
            showNotificationPermissionBanner = false,
            onBack = {},
            onRequestNotificationPermission = {},
            onAddWater = {},
            onUndoWater = {},
            onEditTargetClick = {},
            onAddAlarmClick = {},
            onAlarmClick = {},
            onToggleAlarm = { _, _ -> },
            onDeleteAlarmClick = {},
        )
    }
}

@Preview(name = "Pengingat Minum (Kosong)", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun DrinkReminderScreenEmptyPreview() {
    OlahragaKuyTheme {
        DrinkReminderMainContent(
            uiState = DrinkReminderUiState(
                progress = HydrationProgress(consumedMl = 0, targetMl = 2000, dateKey = "2026-11-23"),
                alarms = emptyList(),
                nextAlarmInfo = null,
                countdownText = NO_ACTIVE_ALARM_TEXT,
            ),
            showNotificationPermissionBanner = true,
            onBack = {},
            onRequestNotificationPermission = {},
            onAddWater = {},
            onUndoWater = {},
            onEditTargetClick = {},
            onAddAlarmClick = {},
            onAlarmClick = {},
            onToggleAlarm = { _, _ -> },
            onDeleteAlarmClick = {},
        )
    }
}
