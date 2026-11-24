package com.example.olahragakuy.pages.steptracker

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.LocationOff
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.pages.posts.currentUserOrNull
import com.example.olahragakuy.pages.posts.resolveDisplayName
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.components.KuyPrimaryButton
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.KuyOrange
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import kotlinx.coroutines.delay
import java.util.UUID

private val StepPanelSurface = Color(0xFF16324F)
private val StepPanelMutedText = Color(0xFFA4B8CF)
private val StepStatusActiveGreen = Color(0xFF43D166)
private val StepStatusPausedAmber = Color(0xFFFFB74D)

private enum class PendingStepPermissionAction {
    START_SESSION,
    RECENTER_LOCATION,
    ENABLE_LOCATION_BANNER,
}

/**
 * Layar utama **Pelacak Langkah (Step Tracker — Mini-Strava)** dengan 3 state:
 * 1. **Idle:** Peta full-screen MapLibre Dark Navy + tombol **`Mulai Langkah`**.
 * 2. **Aktif / Jeda:** Jejak rute oranye live, metrik real-time (Langkah, Waktu, Jarak, Pace, Kalori),
 *    serta tombol **`Jeda` / `Lanjut`** dan **`Berhenti`**.
 * 3. **Ringkasan & Riwayat:** Sheet ringkasan sesi + share PNG ala Strava dan sheet progres harian.
 */
@Composable
fun StepTrackerScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current

    val currentUser = remember(isPreview) { if (isPreview) null else currentUserOrNull() }
    val userName = remember(currentUser) { currentUser.resolveDisplayName("Kamu") }
    val userPhotoUrl = currentUser?.photoUrl?.toString()

    val liveState by StepTrackerStateHolder.liveState.collectAsState()
    val completedSessionEvent by StepTrackerStateHolder.completedSession.collectAsState()

    var dailyProgress by remember {
        mutableStateOf(
            if (isPreview) {
                DailyStepProgress(dateKey = "2026-11-24", totalSteps = 2_450, targetSteps = DEFAULT_DAILY_STEP_TARGET)
            } else {
                StepTrackerStorage.loadDailyProgress(context)
            },
        )
    }
    var sessionHistory by remember {
        mutableStateOf(
            if (isPreview) emptyList() else StepTrackerStorage.loadSessions(context),
        )
    }

    var idleUserLocation by remember { mutableStateOf<StepPoint?>(null) }
    var locationRefreshToken by remember { mutableIntStateOf(0) }
    var recenterSignal by remember { mutableIntStateOf(0) }
    var followUser by remember { mutableStateOf(true) }

    var showExitDialog by rememberSaveable { mutableStateOf(false) }
    var showHistorySheet by rememberSaveable { mutableStateOf(false) }
    var activeSummarySession by remember { mutableStateOf<StepSessionRecord?>(null) }
    var selectedShareStyle by rememberSaveable { mutableStateOf(ShareCardStyle.DARK_NAVY) }
    var showLocationDeniedBanner by rememberSaveable { mutableStateOf(false) }
    var pendingPermissionAction by remember { mutableStateOf<PendingStepPermissionAction?>(null) }

    // State internal fallback foreground-only (tanpa izin lokasi, aman dari SecurityException targetSdk 37).
    var fallbackCounterState by remember { mutableStateOf(StepCounterState()) }
    var fallbackStartedAtMillis by remember { mutableLongStateOf(0L) }
    var fallbackAccumulatedMs by remember { mutableLongStateOf(0L) }
    var fallbackSegmentStartElapsedMs by remember { mutableLongStateOf(0L) }

    fun refreshLocalHistory() {
        if (!isPreview) {
            dailyProgress = StepTrackerStorage.loadDailyProgress(context)
            sessionHistory = StepTrackerStorage.loadSessions(context)
        }
    }

    fun currentFallbackDurationSeconds(status: StepTrackingStatus): Long {
        val activeMs = if (status == StepTrackingStatus.TRACKING) {
            fallbackAccumulatedMs + (SystemClock.elapsedRealtime() - fallbackSegmentStartElapsedMs).coerceAtLeast(0L)
        } else {
            fallbackAccumulatedMs
        }
        return (activeMs / 1_000L).coerceAtLeast(0L)
    }

    fun publishFallbackLiveState(status: StepTrackingStatus, counter: StepCounterState) {
        val durationSec = currentFallbackDurationSeconds(status)
        val steps = counter.totalSteps
        val calories = calculateCaloriesKcal(steps = steps, distanceMeters = 0.0)
        StepTrackerStateHolder.updateLiveState { current ->
            current.copy(
                status = status,
                startedAtMillis = fallbackStartedAtMillis,
                durationSeconds = durationSec,
                steps = steps,
                distanceMeters = 0.0,
                paceSecPerKm = null,
                caloriesKcal = calories,
                routePoints = emptyList(),
                stepSource = StepSourceMode.PEDOMETER,
                isServiceRunning = false,
                isForegroundFallbackMode = true,
            )
        }
    }

    fun startForegroundOnlyFallbackSession() {
        val hasSensor = StepTrackerService.hasHardwareStepSensor(context)
        val hasActivityPerm = StepTrackerService.hasActivityRecognitionPermission(context)
        if (!hasSensor || !hasActivityPerm) {
            Toast.makeText(
                context,
                if (!hasSensor) {
                    "Perangkat tanpa sensor pedometer — aktifkan izin lokasi agar langkah dihitung lewat GPS."
                } else {
                    "Izin lokasi & aktivitas fisik ditolak — aktifkan salah satunya agar langkah dapat terhitung."
                },
                Toast.LENGTH_LONG,
            ).show()
        }
        fallbackStartedAtMillis = System.currentTimeMillis()
        fallbackAccumulatedMs = 0L
        fallbackSegmentStartElapsedMs = SystemClock.elapsedRealtime()
        fallbackCounterState = StepCounterState()
        showLocationDeniedBanner = true
        publishFallbackLiveState(StepTrackingStatus.TRACKING, fallbackCounterState)
    }

    fun pauseForegroundOnlyFallbackSession() {
        val nowElapsed = SystemClock.elapsedRealtime()
        fallbackAccumulatedMs += (nowElapsed - fallbackSegmentStartElapsedMs).coerceAtLeast(0L)
        fallbackCounterState = pauseStepCounter(fallbackCounterState)
        publishFallbackLiveState(StepTrackingStatus.PAUSED, fallbackCounterState)
    }

    fun resumeForegroundOnlyFallbackSession() {
        fallbackSegmentStartElapsedMs = SystemClock.elapsedRealtime()
        fallbackCounterState = resumeStepCounter(fallbackCounterState)
        publishFallbackLiveState(StepTrackingStatus.TRACKING, fallbackCounterState)
    }

    fun stopForegroundOnlyFallbackSession() {
        val currentStatus = liveState.status
        if (currentStatus == StepTrackingStatus.TRACKING) {
            val nowElapsed = SystemClock.elapsedRealtime()
            fallbackAccumulatedMs += (nowElapsed - fallbackSegmentStartElapsedMs).coerceAtLeast(0L)
        }
        val finalDuration = (fallbackAccumulatedMs / 1_000L).coerceAtLeast(0L)
        val finalSteps = fallbackCounterState.totalSteps
        val hasActivity = finalDuration > 0L || finalSteps > 0

        if (hasActivity) {
            val endedAt = System.currentTimeMillis()
            val record = buildStepSessionRecord(
                id = UUID.randomUUID().toString(),
                startedAtMillis = fallbackStartedAtMillis.takeIf { it > 0L } ?: endedAt,
                endedAtMillis = endedAt,
                durationSeconds = finalDuration,
                steps = finalSteps,
                distanceMeters = 0.0,
                routePoints = emptyList(),
                stepSource = StepSourceMode.PEDOMETER,
            )
            StepTrackerStorage.recordCompletedSession(context, record)
            StepTrackerStateHolder.publishCompletedSession(record)
        }
        fallbackCounterState = StepCounterState()
        fallbackAccumulatedMs = 0L
        fallbackStartedAtMillis = 0L
        StepTrackerStateHolder.resetToIdle()
    }

    fun startSessionAccordingToPermissions() {
        followUser = true
        if (StepTrackerService.hasLocationPermission(context)) {
            showLocationDeniedBanner = false
            val started = StepTrackerService.startTracking(context)
            if (!started) {
                startForegroundOnlyFallbackSession()
            }
        } else {
            startForegroundOnlyFallbackSession()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { _ ->
        val action = pendingPermissionAction
        pendingPermissionAction = null

        val locationGranted = StepTrackerService.hasLocationPermission(context)
        if (locationGranted) {
            showLocationDeniedBanner = false
            locationRefreshToken++
            recenterSignal++
        }

        when (action) {
            PendingStepPermissionAction.START_SESSION -> {
                startSessionAccordingToPermissions()
            }

            PendingStepPermissionAction.RECENTER_LOCATION -> {
                if (!locationGranted) {
                    Toast.makeText(
                        context,
                        "Izin lokasi diperlukan untuk memusatkan peta ke posisimu.",
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    followUser = true
                }
            }

            PendingStepPermissionAction.ENABLE_LOCATION_BANNER -> {
                if (locationGranted) {
                    Toast.makeText(
                        context,
                        "Izin lokasi aktif! Sesi berikutnya akan melacak rute GPS & latar belakang.",
                        Toast.LENGTH_SHORT,
                    ).show()
                } else {
                    Toast.makeText(
                        context,
                        "Izin lokasi masih nonaktif. Sesi tetap berjalan dengan sensor langkah.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }

            null -> Unit
        }
    }

    fun buildMissingStartPermissions(): List<String> {
        val missing = mutableListOf<String>()
        if (!StepTrackerService.hasLocationPermission(context)) {
            missing.add(Manifest.permission.ACCESS_FINE_LOCATION)
            missing.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            !StepTrackerService.hasActivityRecognitionPermission(context)
        ) {
            missing.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !StepTrackerService.hasPostNotificationsPermission(context)
        ) {
            missing.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return missing
    }

    // Ambil fix lokasi GPS awal saat layar dibuka (bila izin lokasi sudah diberikan).
    if (!isPreview) {
        DisposableEffect(locationRefreshToken) {
            val cancelLocation = if (StepTrackerService.hasLocationPermission(context)) {
                fetchSingleStepLocation(context) { resolved ->
                    if (resolved != null) {
                        idleUserLocation = resolved
                        StepTrackerStateHolder.updateLiveState { current ->
                            if (current.currentLocation == null) {
                                current.copy(currentLocation = resolved)
                            } else {
                                current
                            }
                        }
                    }
                }
            } else {
                {}
            }
            onDispose { cancelLocation() }
        }
    }

    // Sensor pedometer untuk mode fallback foreground-only (tanpa memulai location FGS).
    val isFallbackActive = !isPreview &&
        liveState.isForegroundFallbackMode &&
        liveState.status != StepTrackingStatus.IDLE
    val isFallbackTracking = isFallbackActive && liveState.status == StepTrackingStatus.TRACKING

    if (!isPreview) {
        DisposableEffect(isFallbackTracking) {
            if (!isFallbackTracking) {
                onDispose {}
            } else {
                val manager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                val canUseSensor = manager != null && StepTrackerService.hasActivityRecognitionPermission(context)
                val stepSensor = if (canUseSensor) {
                    manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
                        ?: manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
                } else {
                    null
                }
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent?) {
                        if (event == null) return
                        when (event.sensor.type) {
                            Sensor.TYPE_STEP_COUNTER -> {
                                val raw = event.values.firstOrNull() ?: return
                                fallbackCounterState = onStepCounterEvent(fallbackCounterState, raw)
                                publishFallbackLiveState(StepTrackingStatus.TRACKING, fallbackCounterState)
                            }

                            Sensor.TYPE_STEP_DETECTOR -> {
                                val detected = event.values.size.coerceAtLeast(1)
                                fallbackCounterState = onStepDetectorPulse(fallbackCounterState, detected)
                                publishFallbackLiveState(StepTrackingStatus.TRACKING, fallbackCounterState)
                            }
                        }
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                if (stepSensor != null) {
                    manager?.registerListener(listener, stepSensor, SensorManager.SENSOR_DELAY_UI)
                }
                onDispose {
                    if (stepSensor != null) {
                        runCatching { manager?.unregisterListener(listener) }
                    }
                }
            }
        }

        // Ticker durasi 1 detik untuk mode fallback foreground-only.
        LaunchedEffect(isFallbackTracking) {
            while (isFallbackTracking) {
                delay(1_000L)
                publishFallbackLiveState(StepTrackingStatus.TRACKING, fallbackCounterState)
            }
        }
    }

    // Observasi sesi yang baru selesai (dari tombol Berhenti di UI maupun tombol aksi di notifikasi).
    LaunchedEffect(completedSessionEvent) {
        val completed = completedSessionEvent ?: return@LaunchedEffect
        refreshLocalHistory()
        activeSummarySession = completed
        StepTrackerStateHolder.consumeCompletedSession()
    }

    val isSessionActive = liveState.status != StepTrackingStatus.IDLE
    BackHandler(enabled = isSessionActive) {
        showExitDialog = true
    }

    val effectiveUserLocation = liveState.currentLocation ?: idleUserLocation

    StepTrackerScreenContent(
        liveState = liveState,
        dailyProgress = dailyProgress,
        userLocation = effectiveUserLocation,
        userName = userName,
        userPhotoUrl = userPhotoUrl,
        followUser = followUser && liveState.status == StepTrackingStatus.TRACKING,
        recenterSignal = recenterSignal,
        showLocationDeniedBanner = showLocationDeniedBanner && isSessionActive,
        onUserPanDetected = { followUser = false },
        onBackClick = {
            if (isSessionActive) {
                showExitDialog = true
            } else {
                onBack()
            }
        },
        onOpenHistoryClick = {
            refreshLocalHistory()
            showHistorySheet = true
        },
        onMyLocationClick = {
            if (isPreview) return@StepTrackerScreenContent
            if (StepTrackerService.hasLocationPermission(context)) {
                followUser = true
                locationRefreshToken++
                recenterSignal++
            } else {
                pendingPermissionAction = PendingStepPermissionAction.RECENTER_LOCATION
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                    ),
                )
            }
        },
        onRequestLocationBannerClick = {
            if (isPreview) return@StepTrackerScreenContent
            pendingPermissionAction = PendingStepPermissionAction.ENABLE_LOCATION_BANNER
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                ),
            )
        },
        onStartClick = {
            if (isPreview) return@StepTrackerScreenContent
            val missing = buildMissingStartPermissions()
            if (missing.isEmpty()) {
                startSessionAccordingToPermissions()
            } else {
                pendingPermissionAction = PendingStepPermissionAction.START_SESSION
                permissionLauncher.launch(missing.toTypedArray())
            }
        },
        onPauseResumeClick = {
            if (isPreview) return@StepTrackerScreenContent
            if (liveState.isServiceRunning) {
                if (liveState.status == StepTrackingStatus.PAUSED) {
                    followUser = true
                    StepTrackerService.resumeTracking(context)
                } else {
                    StepTrackerService.pauseTracking(context)
                }
            } else if (liveState.isForegroundFallbackMode) {
                if (liveState.status == StepTrackingStatus.PAUSED) {
                    resumeForegroundOnlyFallbackSession()
                } else {
                    pauseForegroundOnlyFallbackSession()
                }
            }
        },
        onStopClick = {
            if (isPreview) return@StepTrackerScreenContent
            if (liveState.isServiceRunning) {
                StepTrackerService.stopTracking(context)
            } else if (liveState.isForegroundFallbackMode) {
                stopForegroundOnlyFallbackSession()
            } else {
                StepTrackerStateHolder.resetToIdle()
            }
        },
        modifier = modifier,
    )

    if (showExitDialog) {
        StepExitConfirmationDialog(
            isForegroundServiceRunning = liveState.isServiceRunning,
            onDismiss = { showExitDialog = false },
            onContinueInBackground = {
                showExitDialog = false
                onBack()
            },
            onStopAndSave = {
                showExitDialog = false
                if (liveState.isServiceRunning) {
                    StepTrackerService.stopTracking(context)
                } else if (liveState.isForegroundFallbackMode) {
                    stopForegroundOnlyFallbackSession()
                }
            },
        )
    }

    if (showHistorySheet) {
        StepHistoryAndDailySheet(
            dailyProgress = dailyProgress,
            sessions = sessionHistory,
            onUpdateDailyTarget = { newTarget ->
                if (!isPreview) {
                    dailyProgress = StepTrackerStorage.updateDailyTarget(context, newTarget)
                }
            },
            onSessionClick = { session ->
                showHistorySheet = false
                activeSummarySession = session
            },
            onDeleteSession = { sessionId ->
                if (!isPreview) {
                    sessionHistory = StepTrackerStorage.deleteSession(context, sessionId)
                }
            },
            onDismiss = { showHistorySheet = false },
        )
    }

    activeSummarySession?.let { summary ->
        StepSessionSummarySheet(
            record = summary,
            selectedStyle = selectedShareStyle,
            onStyleSelected = { selectedShareStyle = it },
            onShareClick = {
                if (!isPreview) {
                    val launched = shareSessionCard(context, summary, selectedShareStyle)
                    if (!launched) {
                        Toast.makeText(
                            context,
                            "Tidak dapat membuka menu bagikan di perangkat ini.",
                            Toast.LENGTH_SHORT,
                        ).show()
                    }
                }
            },
            onDismiss = { activeSummarySession = null },
        )
    }
}

@Composable
internal fun StepTrackerScreenContent(
    liveState: StepTrackerLiveState,
    dailyProgress: DailyStepProgress,
    userLocation: StepPoint?,
    userName: String,
    userPhotoUrl: String?,
    followUser: Boolean,
    recenterSignal: Int,
    showLocationDeniedBanner: Boolean,
    onUserPanDetected: () -> Unit,
    onBackClick: () -> Unit,
    onOpenHistoryClick: () -> Unit,
    onMyLocationClick: () -> Unit,
    onRequestLocationBannerClick: () -> Unit,
    onStartClick: () -> Unit,
    onPauseResumeClick: () -> Unit,
    onStopClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isSessionActive = liveState.status != StepTrackingStatus.IDLE
    val displayedDailySteps = dailyProgress.totalSteps + if (isSessionActive) liveState.steps else 0

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HomeNavy),
    ) {
        // 1. Peta MapLibre Full-Screen Dark Navy.
        StepTrackerMapView(
            userLocation = userLocation,
            routePoints = liveState.routePoints,
            followUser = followUser,
            recenterSignal = recenterSignal,
            userName = userName,
            userPhotoUrl = userPhotoUrl,
            onUserPanDetected = onUserPanDetected,
            onUserLocationClick = onMyLocationClick,
            modifier = Modifier.fillMaxSize(),
        )

        // 2. Header Melayang Atas: KuyBackButton + Pill Total Langkah Hari Ini & Riwayat.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            KuyBackButton(
                onClick = onBackClick,
                modifier = Modifier.alpha(if (isSessionActive) 0.8f else 1f),
            )

            DailyStepsPill(
                totalSteps = displayedDailySteps,
                targetSteps = dailyProgress.targetSteps,
                onClick = onOpenHistoryClick,
            )
        }

        // 3. Area Bawah: Tombol My Location, Banner opsional izin lokasi, & Panel Bawah Dark Navy.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Tombol melayang My Location di sisi kanan atas panel bawah.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
            ) {
                Surface(
                    onClick = onMyLocationClick,
                    shape = CircleShape,
                    color = StepPanelSurface.copy(alpha = 0.95f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (followUser) KuyOrange else HomeBlue,
                    ),
                    shadowElevation = 8.dp,
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterEnd)
                        .semantics { contentDescription = "Pusatkan ke lokasi saya" },
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.MyLocation,
                            contentDescription = "Lokasi saya",
                            tint = if (followUser) KuyOrange else HomeText,
                            modifier = Modifier.size(22.dp),
                        )
                    }
                }
            }

            if (showLocationDeniedBanner) {
                LocationFallbackBanner(
                    onEnableClick = onRequestLocationBannerClick,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }

            StepTrackerBottomPanel(
                liveState = liveState,
                onStartClick = onStartClick,
                onPauseResumeClick = onPauseResumeClick,
                onStopClick = onStopClick,
            )
        }
    }
}

@Composable
private fun DailyStepsPill(
    totalSteps: Int,
    targetSteps: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        color = HomeNavy.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, HomeBlue),
        shadowElevation = 6.dp,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(KuyOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                    contentDescription = "Total langkah hari ini",
                    tint = KuyOrange,
                    modifier = Modifier.size(15.dp),
                )
            }
            Column {
                Text(
                    text = formatStepCount(totalSteps),
                    color = HomeText,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    lineHeight = 14.sp,
                )
                Text(
                    text = "/ ${formatStepCount(targetSteps)}",
                    color = StepPanelMutedText,
                    fontFamily = Poppins,
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun LocationFallbackBanner(
    onEnableClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = StepPanelSurface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, KuyOrange.copy(alpha = 0.65f)),
        shadowElevation = 4.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.LocationOff,
                contentDescription = null,
                tint = KuyOrange,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Mode tanpa GPS • Rute peta & latar belakang nonaktif",
                color = HomeText,
                fontFamily = Poppins,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onEnableClick) {
                Text(
                    text = "Aktifkan",
                    color = KuyOrange,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun StepTrackerBottomPanel(
    liveState: StepTrackerLiveState,
    onStartClick: () -> Unit,
    onPauseResumeClick: () -> Unit,
    onStopClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        color = HomeNavy.copy(alpha = 0.97f),
        border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.6f)),
        shadowElevation = 14.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (liveState.status == StepTrackingStatus.IDLE) {
                IdleBottomPanelContent(onStartClick = onStartClick)
            } else {
                ActiveBottomPanelContent(
                    liveState = liveState,
                    onPauseResumeClick = onPauseResumeClick,
                    onStopClick = onStopClick,
                )
            }
        }
    }
}

@Composable
private fun IdleBottomPanelContent(
    onStartClick: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = "Siap Mulai Melangkah?",
                color = HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
            Text(
                text = "Catat langkah, jarak, pace, dan rute secara lokal tanpa tag lokasi publik.",
                color = StepPanelMutedText,
                fontFamily = Poppins,
                fontSize = 12.sp,
                lineHeight = 17.sp,
            )
        }

        KuyPrimaryButton(
            text = "Mulai Langkah",
            onClick = onStartClick,
            enabled = true,
        )
    }
}

@Composable
private fun ActiveBottomPanelContent(
    liveState: StepTrackerLiveState,
    onPauseResumeClick: () -> Unit,
    onStopClick: () -> Unit,
) {
    val isPaused = liveState.status == StepTrackingStatus.PAUSED
    val statusColor = if (isPaused) StepStatusPausedAmber else StepStatusActiveGreen
    val statusLabel = if (isPaused) "Dijeda" else "Menghitung Langkah"

    // Header status & badge mode estimasi GPS.
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(statusColor),
            )
            Text(
                text = statusLabel,
                color = HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }

        if (liveState.stepSource == StepSourceMode.GPS_FALLBACK) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = StepPanelSurface,
                border = BorderStroke(1.dp, HomeBlue),
            ) {
                Text(
                    text = "Estimasi GPS",
                    color = HomeOrange,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
                )
            }
        }
    }

    // Metrik hero: Jumlah Langkah.
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = formatStepCount(liveState.steps),
            color = KuyOrange,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            lineHeight = 44.sp,
        )
        Text(
            text = "Langkah",
            color = StepPanelMutedText,
            fontFamily = Poppins,
            fontSize = 12.sp,
        )
    }

    // Grid 4 metrik live: Waktu, Jarak, Pace, Kalori.
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StepPanelSurface,
        border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.65f)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            LiveMetricColumn(
                label = "Waktu",
                value = formatDuration(liveState.durationSeconds),
                modifier = Modifier.weight(1f),
            )
            LiveMetricColumn(
                label = "Jarak",
                value = formatDistance(liveState.distanceMeters),
                modifier = Modifier.weight(1f),
            )
            LiveMetricColumn(
                label = "Pace",
                value = formatPace(liveState.paceSecPerKm),
                modifier = Modifier.weight(1f),
            )
            LiveMetricColumn(
                label = "Kalori",
                value = "${liveState.caloriesKcal}",
                unit = "kkal",
                modifier = Modifier.weight(1f),
            )
        }
    }

    // 2 Tombol Aksi Wajib: Jeda/Lanjut & Berhenti (putih ber-border oranye).
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = onPauseResumeClick,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = KuyButton,
                contentColor = Color.White,
            ),
        ) {
            Icon(
                imageVector = if (isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = if (isPaused) "Lanjut" else "Jeda",
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
            )
        }

        OutlinedButton(
            onClick = onStopClick,
            modifier = Modifier
                .weight(1f)
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            border = BorderStroke(2.dp, KuyOrange),
            colors = ButtonDefaults.outlinedButtonColors(
                containerColor = Color.White,
                contentColor = KuyOrange,
            ),
        ) {
            Icon(
                imageVector = Icons.Rounded.Stop,
                contentDescription = null,
                tint = KuyOrange,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(6.dp))
            Text(
                text = "Berhenti",
                color = KuyOrange,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
            )
        }
    }
}

@Composable
private fun LiveMetricColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    unit: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = StepPanelMutedText,
            fontFamily = Poppins,
            fontSize = 10.sp,
        )
        Text(
            text = if (unit != null) "$value $unit" else value,
            color = HomeText,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@SuppressLint("MissingPermission")
private fun fetchSingleStepLocation(
    context: Context,
    onLocationResolved: (StepPoint?) -> Unit,
): () -> Unit {
    if (!StepTrackerService.hasLocationPermission(context)) {
        onLocationResolved(null)
        return {}
    }

    val cancellationSource = CancellationTokenSource()
    var disposed = false

    fun deliver(lat: Double, lng: Double, ts: Long) {
        if (disposed) return
        if (isValidCoordinate(lat, lng)) {
            onLocationResolved(StepPoint(lat = lat, lng = lng, timestampMillis = ts))
        }
    }

    try {
        val client = LocationServices.getFusedLocationProviderClient(context)
        client.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                deliver(location.latitude, location.longitude, location.time)
            }
        }

        val request = CurrentLocationRequest.Builder()
            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
            .setDurationMillis(5_000L)
            .setMaxUpdateAgeMillis(30_000L)
            .build()

        client.getCurrentLocation(request, cancellationSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    deliver(location.latitude, location.longitude, location.time)
                }
            }
    } catch (_: SecurityException) {
        onLocationResolved(null)
    } catch (_: Exception) {
        onLocationResolved(null)
    }

    return {
        disposed = true
        cancellationSource.cancel()
    }
}

@Preview(name = "Pelacak Langkah • Idle", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun StepTrackerIdlePreview() {
    OlahragaKuyTheme {
        StepTrackerScreenContent(
            liveState = StepTrackerLiveState(status = StepTrackingStatus.IDLE),
            dailyProgress = DailyStepProgress(
                dateKey = "2026-11-24",
                totalSteps = 2_450,
                targetSteps = 6_000,
            ),
            userLocation = DefaultStepTrackerCoordinate,
            userName = "Andi",
            userPhotoUrl = null,
            followUser = true,
            recenterSignal = 0,
            showLocationDeniedBanner = false,
            onUserPanDetected = {},
            onBackClick = {},
            onOpenHistoryClick = {},
            onMyLocationClick = {},
            onRequestLocationBannerClick = {},
            onStartClick = {},
            onPauseResumeClick = {},
            onStopClick = {},
        )
    }
}

@Preview(name = "Pelacak Langkah • Aktif", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun StepTrackerTrackingPreview() {
    OlahragaKuyTheme {
        StepTrackerScreenContent(
            liveState = StepTrackerLiveState(
                status = StepTrackingStatus.TRACKING,
                durationSeconds = 845L,
                steps = 1_840,
                distanceMeters = 1_380.0,
                paceSecPerKm = 612.0,
                caloriesKcal = 83,
                routePoints = listOf(
                    StepPoint(3.5612, 98.6554, 1_000L),
                    StepPoint(3.5625, 98.6567, 5_000L),
                ),
                currentLocation = DefaultStepTrackerCoordinate,
                stepSource = StepSourceMode.PEDOMETER,
                isServiceRunning = true,
            ),
            dailyProgress = DailyStepProgress(
                dateKey = "2026-11-24",
                totalSteps = 2_450,
                targetSteps = 6_000,
            ),
            userLocation = DefaultStepTrackerCoordinate,
            userName = "Andi",
            userPhotoUrl = null,
            followUser = true,
            recenterSignal = 0,
            showLocationDeniedBanner = false,
            onUserPanDetected = {},
            onBackClick = {},
            onOpenHistoryClick = {},
            onMyLocationClick = {},
            onRequestLocationBannerClick = {},
            onStartClick = {},
            onPauseResumeClick = {},
            onStopClick = {},
        )
    }
}

@Preview(name = "Pelacak Langkah • Dijeda (Fallback GPS)", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun StepTrackerPausedPreview() {
    OlahragaKuyTheme {
        StepTrackerScreenContent(
            liveState = StepTrackerLiveState(
                status = StepTrackingStatus.PAUSED,
                durationSeconds = 420L,
                steps = 960,
                distanceMeters = 720.0,
                paceSecPerKm = 583.0,
                caloriesKcal = 43,
                routePoints = listOf(
                    StepPoint(3.5612, 98.6554, 1_000L),
                    StepPoint(3.5625, 98.6567, 5_000L),
                ),
                currentLocation = DefaultStepTrackerCoordinate,
                stepSource = StepSourceMode.GPS_FALLBACK,
                isServiceRunning = true,
            ),
            dailyProgress = DailyStepProgress(
                dateKey = "2026-11-24",
                totalSteps = 1_200,
                targetSteps = 6_000,
            ),
            userLocation = DefaultStepTrackerCoordinate,
            userName = "Andi",
            userPhotoUrl = null,
            followUser = false,
            recenterSignal = 0,
            showLocationDeniedBanner = true,
            onUserPanDetected = {},
            onBackClick = {},
            onOpenHistoryClick = {},
            onMyLocationClick = {},
            onRequestLocationBannerClick = {},
            onStartClick = {},
            onPauseResumeClick = {},
            onStopClick = {},
        )
    }
}
