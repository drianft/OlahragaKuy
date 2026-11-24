package com.example.olahragakuy.pages.steptracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.olahragakuy.MainActivity
import com.example.olahragakuy.R
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.UUID

/**
 * `ForegroundService` (`foregroundServiceType="location"`) untuk melacak langkah, durasi,
 * dan jejak rute GPS secara real-time di latar belakang.
 *
 * Aman untuk `minSdk = 26` hingga `targetSdk = 37` (Android 14+):
 * - Wajib memverifikasi izin lokasi sebelum memanggil `startForeground(..., LOCATION)`.
 * - Memprioritaskan `Sensor.TYPE_STEP_COUNTER` (fallback `TYPE_STEP_DETECTOR`), lalu fallback
 *   otomatis ke estimasi langkah dari jarak GPS bila sensor tidak ada atau izin `ACTIVITY_RECOGNITION`
 *   ditolak.
 * - Menggunakan fungsi murni di `StepTrackerLogic.kt` untuk Anti-GPS Drift dan offset Pause/Resume.
 */
class StepTrackerService : Service(), SensorEventListener {

    private var sensorManager: SensorManager? = null
    private var activeStepSensor: Sensor? = null
    private var pedometerAvailableAndGranted: Boolean = false

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationUpdatesActive: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var tickerRunning: Boolean = false

    private var isSessionActive: Boolean = false
    private var isForegroundPromoted: Boolean = false
    private var trackingStatus: StepTrackingStatus = StepTrackingStatus.IDLE
    private var startedAtMillis: Long = 0L
    private var accumulatedActiveMillis: Long = 0L
    private var activeSegmentStartElapsedMs: Long = 0L

    private var stepCounterState: StepCounterState = StepCounterState()
    private var gpsTrackState: GpsTrackState = GpsTrackState()
    private var latestKnownLocation: StepPoint? = null
    private var lastNotificationUpdateElapsedMs: Long = 0L

    private val durationTicker = object : Runnable {
        override fun run() {
            if (!tickerRunning || trackingStatus != StepTrackingStatus.TRACKING) return
            publishLiveStateAndNotification(forceNotification = true)
            mainHandler.postDelayed(this, TICK_INTERVAL_MS)
        }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            if (!isSessionActive) return
            var stateChanged = false
            for (location in result.locations) {
                val timestamp = if (location.time > 0L) location.time else System.currentTimeMillis()
                if (isValidCoordinate(location.latitude, location.longitude)) {
                    latestKnownLocation = StepPoint(
                        lat = location.latitude,
                        lng = location.longitude,
                        timestampMillis = timestamp,
                    )
                    stateChanged = true
                }
                val sample = GpsSample(
                    lat = location.latitude,
                    lng = location.longitude,
                    accuracyMeters = location.accuracy,
                    timestampMillis = timestamp,
                )
                val (updatedTrack, decision) = processGpsSample(gpsTrackState, sample)
                gpsTrackState = updatedTrack
                if (decision.accepted) {
                    stateChanged = true
                }
            }
            if (stateChanged) {
                publishLiveStateAndNotification(forceNotification = false)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        sensorManager = getSystemService(SENSOR_SERVICE) as? SensorManager
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> handleStartTracking()
            ACTION_PAUSE -> handlePauseTracking()
            ACTION_RESUME -> handleResumeTracking()
            ACTION_STOP -> handleStopTracking()
            else -> {
                if (!isSessionActive) {
                    stopSelf()
                }
            }
        }
        return START_NOT_STICKY
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null || !isSessionActive) return
        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val rawValue = event.values.firstOrNull() ?: return
                stepCounterState = onStepCounterEvent(stepCounterState, rawValue)
                publishLiveStateAndNotification(forceNotification = false)
            }

            Sensor.TYPE_STEP_DETECTOR -> {
                val detected = event.values.size.coerceAtLeast(1)
                stepCounterState = onStepDetectorPulse(stepCounterState, detected)
                publishLiveStateAndNotification(forceNotification = false)
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onDestroy() {
        val wasActive = isSessionActive
        cleanupHardwareAndTicker()
        isSessionActive = false
        trackingStatus = StepTrackingStatus.IDLE
        isForegroundPromoted = false
        if (wasActive) {
            StepTrackerStateHolder.resetToIdle()
        }
        super.onDestroy()
    }

    private fun handleStartTracking() {
        if (!hasLocationPermission(this)) {
            StepTrackerStateHolder.resetToIdle()
            stopSelf()
            return
        }

        if (isSessionActive) {
            if (trackingStatus == StepTrackingStatus.PAUSED) {
                handleResumeTracking()
            } else {
                publishLiveStateAndNotification(forceNotification = true)
            }
            return
        }

        val preservedLocation = StepTrackerStateHolder.liveState.value.currentLocation
        isSessionActive = true
        trackingStatus = StepTrackingStatus.TRACKING
        startedAtMillis = System.currentTimeMillis()
        accumulatedActiveMillis = 0L
        activeSegmentStartElapsedMs = SystemClock.elapsedRealtime()
        stepCounterState = StepCounterState()
        gpsTrackState = GpsTrackState()
        latestKnownLocation = preservedLocation

        registerStepSensor()
        val initialLiveState = buildCurrentLiveState()
        if (!promoteToForeground(initialLiveState)) {
            cleanupHardwareAndTicker()
            isSessionActive = false
            trackingStatus = StepTrackingStatus.IDLE
            StepTrackerStateHolder.resetToIdle()
            stopSelf()
            return
        }

        StepTrackerStateHolder.setLiveState(initialLiveState)
        startLocationUpdates()
        startTicker()
    }

    private fun handlePauseTracking() {
        if (!isSessionActive) {
            stopSelf()
            return
        }
        if (trackingStatus == StepTrackingStatus.PAUSED) return

        val nowElapsed = SystemClock.elapsedRealtime()
        accumulatedActiveMillis += (nowElapsed - activeSegmentStartElapsedMs).coerceAtLeast(0L)
        trackingStatus = StepTrackingStatus.PAUSED

        stopTicker()
        stepCounterState = pauseStepCounter(stepCounterState)
        gpsTrackState = pauseGpsTrack(gpsTrackState)
        unregisterStepSensor()
        stopLocationUpdates()

        publishLiveStateAndNotification(forceNotification = true)
    }

    private fun handleResumeTracking() {
        if (!isSessionActive) {
            stopSelf()
            return
        }
        if (trackingStatus == StepTrackingStatus.TRACKING) return

        if (!hasLocationPermission(this)) {
            handleStopTracking()
            return
        }

        trackingStatus = StepTrackingStatus.TRACKING
        activeSegmentStartElapsedMs = SystemClock.elapsedRealtime()
        stepCounterState = resumeStepCounter(stepCounterState)
        gpsTrackState = resumeGpsTrack(gpsTrackState)

        registerStepSensor()
        startLocationUpdates()
        startTicker()

        publishLiveStateAndNotification(forceNotification = true)
    }

    private fun handleStopTracking() {
        if (isSessionActive) {
            if (trackingStatus == StepTrackingStatus.TRACKING) {
                val nowElapsed = SystemClock.elapsedRealtime()
                accumulatedActiveMillis += (nowElapsed - activeSegmentStartElapsedMs).coerceAtLeast(0L)
            }

            val finalState = buildCurrentLiveState()
            cleanupHardwareAndTicker()
            isSessionActive = false
            trackingStatus = StepTrackingStatus.IDLE

            val hasRecordedActivity = finalState.durationSeconds > 0L ||
                finalState.steps > 0 ||
                finalState.distanceMeters > 0.0

            if (hasRecordedActivity) {
                val endedAtMillis = System.currentTimeMillis()
                val record = buildStepSessionRecord(
                    id = UUID.randomUUID().toString(),
                    startedAtMillis = startedAtMillis.takeIf { it > 0L } ?: endedAtMillis,
                    endedAtMillis = endedAtMillis,
                    durationSeconds = finalState.durationSeconds,
                    steps = finalState.steps,
                    distanceMeters = finalState.distanceMeters,
                    routePoints = finalState.routePoints,
                    stepSource = finalState.stepSource,
                )
                StepTrackerStorage.recordCompletedSession(applicationContext, record)
                StepTrackerStateHolder.publishCompletedSession(record)
            }
        } else {
            cleanupHardwareAndTicker()
        }

        StepTrackerStateHolder.resetToIdle()
        stopForegroundSafely()
        stopSelf()
    }

    private fun promoteToForeground(liveState: StepTrackerLiveState): Boolean {
        if (!hasLocationPermission(this)) return false
        ensureNotificationChannel()
        val notification = buildTrackingNotification(liveState)
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            isForegroundPromoted = true
            lastNotificationUpdateElapsedMs = SystemClock.elapsedRealtime()
            true
        } catch (_: SecurityException) {
            false
        } catch (_: IllegalStateException) {
            false
        }
    }

    private fun stopForegroundSafely() {
        if (isForegroundPromoted) {
            runCatching { stopForeground(STOP_FOREGROUND_REMOVE) }
            isForegroundPromoted = false
        }
        runCatching { NotificationManagerCompat.from(this).cancel(NOTIFICATION_ID) }
    }

    private fun registerStepSensor() {
        unregisterStepSensor()
        val manager = sensorManager
        if (manager == null || !hasActivityRecognitionPermission(this)) {
            pedometerAvailableAndGranted = false
            return
        }

        val stepCounter = manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        if (stepCounter != null) {
            val registered = manager.registerListener(
                this,
                stepCounter,
                SensorManager.SENSOR_DELAY_UI,
            )
            if (registered) {
                activeStepSensor = stepCounter
                pedometerAvailableAndGranted = true
                return
            }
        }

        val stepDetector = manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        if (stepDetector != null) {
            val registered = manager.registerListener(
                this,
                stepDetector,
                SensorManager.SENSOR_DELAY_UI,
            )
            if (registered) {
                activeStepSensor = stepDetector
                pedometerAvailableAndGranted = true
                return
            }
        }

        pedometerAvailableAndGranted = false
    }

    private fun unregisterStepSensor() {
        activeStepSensor?.let {
            runCatching { sensorManager?.unregisterListener(this) }
        }
        activeStepSensor = null
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (locationUpdatesActive || !hasLocationPermission(this)) return
        val client = fusedLocationClient ?: return
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, LOCATION_INTERVAL_MS)
            .setMinUpdateIntervalMillis(LOCATION_MIN_INTERVAL_MS)
            .build()

        try {
            client.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
            locationUpdatesActive = true
        } catch (_: SecurityException) {
            locationUpdatesActive = false
        } catch (_: Exception) {
            locationUpdatesActive = false
        }
    }

    private fun stopLocationUpdates() {
        if (!locationUpdatesActive) return
        runCatching { fusedLocationClient?.removeLocationUpdates(locationCallback) }
        locationUpdatesActive = false
    }

    private fun startTicker() {
        if (tickerRunning) return
        tickerRunning = true
        mainHandler.removeCallbacks(durationTicker)
        mainHandler.postDelayed(durationTicker, TICK_INTERVAL_MS)
    }

    private fun stopTicker() {
        tickerRunning = false
        mainHandler.removeCallbacks(durationTicker)
    }

    private fun cleanupHardwareAndTicker() {
        stopTicker()
        unregisterStepSensor()
        stopLocationUpdates()
    }

    private fun currentDurationSeconds(): Long {
        val activeMillis = if (trackingStatus == StepTrackingStatus.TRACKING) {
            accumulatedActiveMillis + (SystemClock.elapsedRealtime() - activeSegmentStartElapsedMs).coerceAtLeast(0L)
        } else {
            accumulatedActiveMillis
        }
        return (activeMillis / 1_000L).coerceAtLeast(0L)
    }

    private fun buildCurrentLiveState(): StepTrackerLiveState {
        val durationSeconds = currentDurationSeconds()
        val distanceMeters = gpsTrackState.totalDistanceMeters
        val (effectiveSteps, sourceMode) = resolveEffectiveSteps(
            pedometerSteps = stepCounterState.totalSteps,
            distanceMeters = distanceMeters,
            pedometerAvailableAndGranted = pedometerAvailableAndGranted,
        )
        val paceSecPerKm = calculatePaceSecPerKm(distanceMeters, durationSeconds)
        val caloriesKcal = calculateCaloriesKcal(effectiveSteps, distanceMeters)
        val previousLastCompleted = StepTrackerStateHolder.liveState.value.lastCompletedSession

        return StepTrackerLiveState(
            status = trackingStatus,
            startedAtMillis = startedAtMillis,
            durationSeconds = durationSeconds,
            steps = effectiveSteps,
            distanceMeters = distanceMeters,
            paceSecPerKm = paceSecPerKm,
            caloriesKcal = caloriesKcal,
            routePoints = gpsTrackState.acceptedPoints,
            currentLocation = latestKnownLocation ?: gpsTrackState.lastAcceptedPoint,
            stepSource = sourceMode,
            isServiceRunning = isSessionActive,
            isForegroundFallbackMode = false,
            lastCompletedSession = previousLastCompleted,
        )
    }

    private fun publishLiveStateAndNotification(forceNotification: Boolean) {
        val liveState = buildCurrentLiveState()
        StepTrackerStateHolder.setLiveState(liveState)

        if (!isForegroundPromoted) return
        val nowElapsed = SystemClock.elapsedRealtime()
        if (!forceNotification && (nowElapsed - lastNotificationUpdateElapsedMs) < MIN_NOTIFICATION_UPDATE_MS) {
            return
        }
        lastNotificationUpdateElapsedMs = nowElapsed
        updateNotification(liveState)
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Menampilkan progres langkah, jarak, dan waktu secara langsung saat sesi aktif."
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        manager.createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    private fun updateNotification(liveState: StepTrackerLiveState) {
        if (!hasPostNotificationsPermission(this)) return
        val notification = buildTrackingNotification(liveState)
        runCatching {
            NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, notification)
        }
    }

    private fun buildTrackingNotification(liveState: StepTrackerLiveState): Notification {
        val isPaused = liveState.status == StepTrackingStatus.PAUSED
        val title = if (isPaused) "Pelacak Langkah • Dijeda" else "Menghitung Langkah"
        val stepsText = formatStepCount(liveState.steps)
        val distanceText = formatDistance(liveState.distanceMeters)
        val durationText = formatDuration(liveState.durationSeconds)
        val contentText = "$stepsText langkah • $distanceText • $durationText"

        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_STEP_TRACKER, true)
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            REQUEST_CODE_OPEN_APP,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val toggleAction = if (isPaused) ACTION_RESUME else ACTION_PAUSE
        val toggleLabel = if (isPaused) "Lanjut" else "Jeda"
        val toggleIntent = Intent(this, StepTrackerService::class.java).apply {
            action = toggleAction
        }
        val togglePendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_TOGGLE_PAUSE,
            toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val stopIntent = Intent(this, StepTrackerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            REQUEST_CODE_STOP,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mingcute_notification_fill)
            .setContentTitle(title)
            .setContentText(contentText)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setContentIntent(openAppPendingIntent)
            .addAction(0, toggleLabel, togglePendingIntent)
            .addAction(0, "Berhenti", stopPendingIntent)
            .build()
    }

    companion object {
        const val EXTRA_OPEN_STEP_TRACKER = "open_step_tracker"

        const val ACTION_START = "com.example.olahragakuy.action.STEP_TRACKER_START"
        const val ACTION_PAUSE = "com.example.olahragakuy.action.STEP_TRACKER_PAUSE"
        const val ACTION_RESUME = "com.example.olahragakuy.action.STEP_TRACKER_RESUME"
        const val ACTION_STOP = "com.example.olahragakuy.action.STEP_TRACKER_STOP"

        private const val CHANNEL_ID = "olahragakuy_step_tracker_live"
        private const val CHANNEL_NAME = "Pelacak Langkah"
        private const val NOTIFICATION_ID = 42_100
        private const val REQUEST_CODE_OPEN_APP = 62_100
        private const val REQUEST_CODE_TOGGLE_PAUSE = 62_101
        private const val REQUEST_CODE_STOP = 62_102

        private const val TICK_INTERVAL_MS = 1_000L
        private const val LOCATION_INTERVAL_MS = 2_500L
        private const val LOCATION_MIN_INTERVAL_MS = 1_500L
        private const val MIN_NOTIFICATION_UPDATE_MS = 900L

        fun hasLocationPermission(context: Context): Boolean {
            val appContext = context.applicationContext
            val fine = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            val coarse = ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.ACCESS_COARSE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
            return fine || coarse
        }

        fun hasActivityRecognitionPermission(context: Context): Boolean {
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContextCompat.checkSelfPermission(
                    context.applicationContext,
                    Manifest.permission.ACTIVITY_RECOGNITION,
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                true
            }
        }

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

        fun hasHardwareStepSensor(context: Context): Boolean {
            val manager = context.applicationContext.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
                ?: return false
            return manager.getDefaultSensor(Sensor.TYPE_STEP_COUNTER) != null ||
                manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR) != null
        }

        /**
         * Memulai pelacakan langkah via `ForegroundService` (`location`).
         * Mengembalikan `false` secara aman jika izin lokasi belum diberikan.
         */
        fun startTracking(context: Context): Boolean {
            val appContext = context.applicationContext
            if (!hasLocationPermission(appContext)) return false
            val intent = Intent(appContext, StepTrackerService::class.java).apply {
                action = ACTION_START
            }
            return try {
                ContextCompat.startForegroundService(appContext, intent)
                true
            } catch (_: SecurityException) {
                false
            } catch (_: IllegalStateException) {
                false
            }
        }

        fun pauseTracking(context: Context) {
            dispatchControlAction(context, ACTION_PAUSE)
        }

        fun resumeTracking(context: Context) {
            dispatchControlAction(context, ACTION_RESUME)
        }

        fun stopTracking(context: Context) {
            dispatchControlAction(context, ACTION_STOP)
        }

        private fun dispatchControlAction(context: Context, actionName: String) {
            val appContext = context.applicationContext
            val intent = Intent(appContext, StepTrackerService::class.java).apply {
                action = actionName
            }
            runCatching { appContext.startService(intent) }
        }
    }
}
