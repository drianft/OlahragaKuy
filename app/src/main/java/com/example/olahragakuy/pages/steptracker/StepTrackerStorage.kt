package com.example.olahragakuy.pages.steptracker

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

private const val STEP_PREFS_NAME = "olahragakuy_step_tracker_prefs"
private const val KEY_DAILY_DATE = "daily_step_date_key"
private const val KEY_DAILY_TOTAL_STEPS = "daily_step_total"
private const val KEY_DAILY_TARGET_STEPS = "daily_step_target"
private const val KEY_SESSIONS_JSON = "step_sessions_json"

/**
 * Penyimpanan lokal 100% offline untuk fitur Pelacak Langkah menggunakan
 * [SharedPreferences] + `org.json`. Aman dipanggil dari UI Compose maupun `StepTrackerService`.
 */
object StepTrackerStorage {

    private fun prefs(context: Context): SharedPreferences {
        return context.applicationContext.getSharedPreferences(STEP_PREFS_NAME, Context.MODE_PRIVATE)
    }

    @Synchronized
    fun loadDailyProgress(
        context: Context,
        today: LocalDate = LocalDate.now(),
    ): DailyStepProgress {
        val sp = prefs(context)
        val stored = DailyStepProgress(
            dateKey = sp.getString(KEY_DAILY_DATE, "").orEmpty(),
            totalSteps = sp.getInt(KEY_DAILY_TOTAL_STEPS, 0),
            targetSteps = sp.getInt(KEY_DAILY_TARGET_STEPS, DEFAULT_DAILY_STEP_TARGET),
        )
        val normalized = normalizeDailyStepProgress(stored, today)
        if (normalized != stored) {
            saveDailyProgressLocked(sp, normalized, commit = false)
        }
        return normalized
    }

    @Synchronized
    fun addDailySteps(
        context: Context,
        deltaSteps: Int,
        today: LocalDate = LocalDate.now(),
    ): DailyStepProgress {
        val sp = prefs(context)
        val current = DailyStepProgress(
            dateKey = sp.getString(KEY_DAILY_DATE, "").orEmpty(),
            totalSteps = sp.getInt(KEY_DAILY_TOTAL_STEPS, 0),
            targetSteps = sp.getInt(KEY_DAILY_TARGET_STEPS, DEFAULT_DAILY_STEP_TARGET),
        )
        val updated = applyCompletedSessionSteps(current, deltaSteps, today)
        saveDailyProgressLocked(sp, updated, commit = true)
        return updated
    }

    @Synchronized
    fun updateDailyTarget(
        context: Context,
        newTargetSteps: Int,
        today: LocalDate = LocalDate.now(),
    ): DailyStepProgress {
        val sp = prefs(context)
        val current = DailyStepProgress(
            dateKey = sp.getString(KEY_DAILY_DATE, "").orEmpty(),
            totalSteps = sp.getInt(KEY_DAILY_TOTAL_STEPS, 0),
            targetSteps = sp.getInt(KEY_DAILY_TARGET_STEPS, DEFAULT_DAILY_STEP_TARGET),
        )
        val updated = updateDailyStepTarget(current, newTargetSteps, today)
        saveDailyProgressLocked(sp, updated, commit = true)
        return updated
    }

    @Synchronized
    fun loadSessions(context: Context): List<StepSessionRecord> {
        val rawJson = prefs(context).getString(KEY_SESSIONS_JSON, null) ?: return emptyList()
        return parseSessionsJson(rawJson)
    }

    @Synchronized
    fun saveSessions(context: Context, sessions: List<StepSessionRecord>) {
        val trimmed = sessions
            .sortedByDescending { it.startedAtMillis }
            .take(MAX_STORED_STEP_SESSIONS)
            .map { session ->
                if (session.routePoints.size > MAX_STORED_ROUTE_POINTS) {
                    session.copy(
                        routePoints = downsampleRoutePoints(
                            points = session.routePoints,
                            maxPoints = MAX_STORED_ROUTE_POINTS,
                        ),
                    )
                } else {
                    session
                }
            }
        val serialized = serializeSessionsJson(trimmed)
        prefs(context).edit(commit = true) {
            putString(KEY_SESSIONS_JSON, serialized)
        }
    }

    /**
     * Menyimpan sesi baru ke daftar riwayat (maksimal 100 sesi terbaru, rute ter-downsample)
     * sekaligus menambahkan jumlah langkah sesi ke progres harian.
     */
    @Synchronized
    fun recordCompletedSession(
        context: Context,
        session: StepSessionRecord,
        today: LocalDate = LocalDate.now(),
    ): Pair<DailyStepProgress, List<StepSessionRecord>> {
        val sanitizedSession = session.copy(
            routePoints = downsampleRoutePoints(
                points = session.routePoints,
                maxPoints = MAX_STORED_ROUTE_POINTS,
            ),
        )
        val updatedDaily = addDailySteps(
            context = context,
            deltaSteps = sanitizedSession.steps,
            today = today,
        )
        val existing = loadSessions(context).filterNot { it.id == sanitizedSession.id }
        val updatedSessions = (listOf(sanitizedSession) + existing)
            .sortedByDescending { it.startedAtMillis }
            .take(MAX_STORED_STEP_SESSIONS)
        saveSessions(context, updatedSessions)
        return updatedDaily to updatedSessions
    }

    @Synchronized
    fun deleteSession(
        context: Context,
        sessionId: String,
    ): List<StepSessionRecord> {
        val updated = loadSessions(context).filterNot { it.id == sessionId }
        saveSessions(context, updated)
        return updated
    }

    @Synchronized
    fun findSessionById(
        context: Context,
        sessionId: String,
    ): StepSessionRecord? {
        return loadSessions(context).firstOrNull { it.id == sessionId }
    }

    @Synchronized
    fun clearAllSessions(context: Context) {
        prefs(context).edit(commit = true) {
            putString(KEY_SESSIONS_JSON, "[]")
        }
    }

    private fun saveDailyProgressLocked(
        sp: SharedPreferences,
        progress: DailyStepProgress,
        commit: Boolean,
    ) {
        sp.edit(commit = commit) {
            putString(KEY_DAILY_DATE, progress.dateKey)
            putInt(KEY_DAILY_TOTAL_STEPS, progress.totalSteps)
            putInt(KEY_DAILY_TARGET_STEPS, progress.targetSteps)
        }
    }

    private fun serializeSessionsJson(sessions: List<StepSessionRecord>): String {
        val array = JSONArray()
        sessions.forEach { session ->
            val pointsArray = JSONArray()
            session.routePoints.forEach { point ->
                val pointObj = JSONObject().apply {
                    put("lat", point.lat)
                    put("lng", point.lng)
                    put("ts", point.timestampMillis)
                }
                pointsArray.put(pointObj)
            }

            val obj = JSONObject().apply {
                put("id", session.id)
                put("startedAtMillis", session.startedAtMillis)
                put("endedAtMillis", session.endedAtMillis)
                put("durationSeconds", session.durationSeconds)
                put("steps", session.steps)
                put("distanceMeters", session.distanceMeters)
                if (session.paceSecPerKm != null && session.paceSecPerKm.isFinite()) {
                    put("paceSecPerKm", session.paceSecPerKm)
                } else {
                    put("paceSecPerKm", JSONObject.NULL)
                }
                put("caloriesKcal", session.caloriesKcal)
                put("routePoints", pointsArray)
                put("stepSource", session.stepSource.name)
            }
            array.put(obj)
        }
        return array.toString()
    }

    private fun parseSessionsJson(rawJson: String): List<StepSessionRecord> {
        return try {
            val array = JSONArray(rawJson)
            buildList(array.length()) {
                for (index in 0 until array.length()) {
                    val obj = array.optJSONObject(index) ?: continue
                    val id = obj.optString("id", "").trim()
                    if (id.isEmpty()) continue

                    val startedAtMillis = obj.optLong("startedAtMillis", 0L)
                    val endedAtMillis = obj.optLong("endedAtMillis", startedAtMillis)
                    val durationSeconds = obj.optLong("durationSeconds", 0L).coerceAtLeast(0L)
                    val steps = obj.optInt("steps", 0).coerceAtLeast(0)
                    val distanceMeters = obj.optDouble("distanceMeters", 0.0)
                        .let { if (it.isFinite() && it >= 0.0) it else 0.0 }
                    val paceSecPerKm = if (obj.isNull("paceSecPerKm")) {
                        null
                    } else {
                        obj.optDouble("paceSecPerKm", Double.NaN)
                            .takeIf { it.isFinite() && it > 0.0 }
                    }
                    val caloriesKcal = obj.optInt("caloriesKcal", 0).coerceAtLeast(0)

                    val stepSource = runCatching {
                        StepSourceMode.valueOf(
                            obj.optString("stepSource", StepSourceMode.PEDOMETER.name),
                        )
                    }.getOrDefault(StepSourceMode.PEDOMETER)

                    val pointsJson = obj.optJSONArray("routePoints")
                    val routePoints = if (pointsJson != null) {
                        buildList(pointsJson.length()) {
                            for (pIdx in 0 until pointsJson.length()) {
                                val pObj = pointsJson.optJSONObject(pIdx) ?: continue
                                val lat = pObj.optDouble("lat", Double.NaN)
                                val lng = pObj.optDouble("lng", Double.NaN)
                                if (!isValidCoordinate(lat, lng)) continue
                                add(
                                    StepPoint(
                                        lat = lat,
                                        lng = lng,
                                        timestampMillis = pObj.optLong("ts", 0L),
                                    ),
                                )
                            }
                        }
                    } else {
                        emptyList()
                    }

                    add(
                        StepSessionRecord(
                            id = id,
                            startedAtMillis = startedAtMillis,
                            endedAtMillis = endedAtMillis,
                            durationSeconds = durationSeconds,
                            steps = steps,
                            distanceMeters = distanceMeters,
                            paceSecPerKm = paceSecPerKm,
                            caloriesKcal = caloriesKcal,
                            routePoints = routePoints,
                            stepSource = stepSource,
                        ),
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
