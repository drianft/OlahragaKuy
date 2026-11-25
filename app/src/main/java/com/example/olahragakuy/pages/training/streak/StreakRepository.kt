package com.example.olahragakuy.pages.training.streak

import com.example.olahragakuy.ui.components.RefreshRegistry
import com.example.olahragakuy.ui.components.addRefreshableSnapshotListener
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import java.time.LocalDate

const val FIELD_STREAK_DATE_KEY = "dateKey"
const val FIELD_STREAK_WORKOUT_COMPLETED = "workoutCompleted"
const val FIELD_STREAK_STEP_DISTANCE_METERS = "stepDistanceMeters"
const val FIELD_STREAK_QUALIFIED = "qualified"

fun DailyStreakActivity.toFirestoreMap(includeServerTimestamp: Boolean = false): Map<String, Any> {
    val safeMeters = sanitizedStepDistanceMeters
    val map = linkedMapOf<String, Any>(
        FIELD_STREAK_DATE_KEY to dateKey,
        FIELD_STREAK_WORKOUT_COMPLETED to workoutCompleted,
        FIELD_STREAK_STEP_DISTANCE_METERS to safeMeters,
        FIELD_STREAK_QUALIFIED to isDailyActivityQualified(workoutCompleted, safeMeters),
    )
    if (includeServerTimestamp) {
        map["updatedAt"] = FieldValue.serverTimestamp()
    }
    return map
}

fun parseDailyStreakActivityFromMap(
    docId: String,
    data: Map<String, Any?>,
): DailyStreakActivity? {
    val rawDateKey = (data[FIELD_STREAK_DATE_KEY] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: docId.trim()
    val parsedDate = parseStreakDateKey(rawDateKey) ?: return null
    val normalizedDateKey = formatStreakDateKey(parsedDate)
    val workoutCompleted = (data[FIELD_STREAK_WORKOUT_COMPLETED] as? Boolean) ?: false
    val stepDistanceMeters = sanitizeStepDistanceMeters(
        (data[FIELD_STREAK_STEP_DISTANCE_METERS] as? Number)?.toDouble() ?: 0.0,
    )
    return DailyStreakActivity(
        dateKey = normalizedDateKey,
        workoutCompleted = workoutCompleted,
        stepDistanceMeters = stepDistanceMeters,
    )
}

/**
 * Mengamati data streak harian khusus untuk akun [userId] dari Firestore pada subkoleksi
 * `users/{userId}/streak_days/{yyyy-MM-dd}`.
 */
fun observeUserStreak(
    userId: String,
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    refreshRegistry: RefreshRegistry? = null,
): ListenerRegistration {
    val cleanUserId = userId.trim()
    if (cleanUserId.isEmpty()) {
        TrainingStreakStore.reset()
        return ListenerRegistration { }
    }

    TrainingStreakStore.bindUser(cleanUserId)
    return try {
        firestore.collection("users")
            .document(cleanUserId)
            .collection(STREAK_DAYS_SUBCOLLECTION)
            .addRefreshableSnapshotListener(refreshRegistry) { snapshot, error ->
                if (error != null) {
                    TrainingStreakStore.setError(
                        cleanUserId,
                        error.localizedMessage ?: "Gagal memuat data streak dari Firestore.",
                    )
                    return@addRefreshableSnapshotListener
                }
                val parsed = snapshot?.documents
                    .orEmpty()
                    .mapNotNull { doc -> parseDailyStreakActivityFromMap(doc.id, doc.data.orEmpty()) }
                    .associateBy { it.dateKey }
                TrainingStreakStore.applyFromFirestore(cleanUserId, parsed)
            }
    } catch (e: Exception) {
        TrainingStreakStore.setError(
            cleanUserId,
            e.localizedMessage ?: "Gagal terhubung ke Firestore.",
        )
        ListenerRegistration { }
    }
}

/**
 * Mencatat bahwa akun [userId] telah menyelesaikan sesi latihan pada tanggal [date]
 * ke dokumen `users/{userId}/streak_days/{yyyy-MM-dd}`.
 */
fun recordWorkoutCompletedForStreak(
    userId: String,
    date: LocalDate = LocalDate.now(),
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onResult: (Boolean, String?) -> Unit = { _, _ -> },
) {
    val cleanUserId = userId.trim()
    if (cleanUserId.isEmpty()) {
        onResult(false, "Akun belum login.")
        return
    }
    if (TrainingStreakStore.activeUserId != cleanUserId) {
        TrainingStreakStore.bindUser(cleanUserId)
    }

    val existing = TrainingStreakStore.activityForDate(cleanUserId, date)
    val updated = mergeWorkoutCompleted(existing = existing, date = date)
    TrainingStreakStore.upsertLocalActivity(cleanUserId, updated)

    persistDailyStreakActivity(
        userId = cleanUserId,
        activity = updated,
        firestore = firestore,
        onResult = onResult,
    )
}

/**
 * Mencatat jarak pelacak langkah (dalam meter) untuk akun [userId] pada tanggal [date].
 * Hari dianggap memenuhi streak otomatis ketika total jarak >= 1000 meter (1 km).
 */
fun recordStepDistanceForStreak(
    userId: String,
    distanceMeters: Double,
    mode: StepDistanceUpdateMode = StepDistanceUpdateMode.ADD_DELTA,
    date: LocalDate = LocalDate.now(),
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onResult: (Boolean, String?) -> Unit = { _, _ -> },
) {
    val cleanUserId = userId.trim()
    if (cleanUserId.isEmpty()) {
        onResult(false, "Akun belum login.")
        return
    }
    val safeDistance = sanitizeStepDistanceMeters(distanceMeters)
    if (safeDistance <= 0.0) {
        onResult(true, null)
        return
    }
    if (TrainingStreakStore.activeUserId != cleanUserId) {
        TrainingStreakStore.bindUser(cleanUserId)
    }

    val existing = TrainingStreakStore.activityForDate(cleanUserId, date)
    val updated = mergeStepTrackerDistance(
        existing = existing,
        distanceMeters = safeDistance,
        date = date,
        mode = mode,
    )
    TrainingStreakStore.upsertLocalActivity(cleanUserId, updated)

    persistDailyStreakActivity(
        userId = cleanUserId,
        activity = updated,
        firestore = firestore,
        onResult = onResult,
    )
}

/**
 * Overload praktis untuk mencatat jarak pelacak langkah dalam satuan kilometer.
 */
fun recordStepDistanceKmForStreak(
    userId: String,
    distanceKm: Double,
    mode: StepDistanceUpdateMode = StepDistanceUpdateMode.ADD_DELTA,
    date: LocalDate = LocalDate.now(),
    firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onResult: (Boolean, String?) -> Unit = { _, _ -> },
) {
    val meters = if (distanceKm.isFinite()) distanceKm * 1000.0 else 0.0
    recordStepDistanceForStreak(
        userId = userId,
        distanceMeters = meters,
        mode = mode,
        date = date,
        firestore = firestore,
        onResult = onResult,
    )
}

private fun persistDailyStreakActivity(
    userId: String,
    activity: DailyStreakActivity,
    firestore: FirebaseFirestore,
    onResult: (Boolean, String?) -> Unit,
) {
    try {
        firestore.collection("users")
            .document(userId)
            .collection(STREAK_DAYS_SUBCOLLECTION)
            .document(activity.dateKey)
            .set(activity.toFirestoreMap(includeServerTimestamp = true), SetOptions.merge())
            .addOnSuccessListener { onResult(true, null) }
            .addOnFailureListener { err ->
                onResult(false, err.localizedMessage ?: "Gagal menyimpan streak ke Firestore.")
            }
    } catch (e: Exception) {
        onResult(false, e.localizedMessage ?: "Gagal menyimpan streak ke Firestore.")
    }
}
