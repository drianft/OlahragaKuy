package com.example.olahragakuy.pages.profile.admin

import com.example.olahragakuy.pages.posts.NotificationType
import com.example.olahragakuy.pages.posts.PostUiModel
import com.example.olahragakuy.pages.posts.currentUserOrNull
import com.example.olahragakuy.pages.posts.resolveDisplayName
import com.example.olahragakuy.pages.profile.user.UserPost
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

const val REPORTS_COLLECTION = "reports"
const val REPORT_STATUS_PENDING = "PENDING"
const val REPORT_STATUS_IGNORED = "IGNORED"
const val REPORT_STATUS_RESOLVED_DELETED = "RESOLVED_DELETED"
const val REPORT_STATUS_RESOLVED_WARNED = "RESOLVED_WARNED"

val RESOLVED_REPORT_STATUSES = listOf(
    REPORT_STATUS_IGNORED,
    REPORT_STATUS_RESOLVED_DELETED,
    REPORT_STATUS_RESOLVED_WARNED,
)

private const val ONE_HOUR_MILLIS = 60L * 60L * 1000L

/**
 * Representasi satu item riwayat tindakan admin (gabungan dari seluruh akun ber-role admin).
 */
data class AdminActionHistoryItem(
    val id: String,
    val report: AdminReportItem,
    val actionStatus: String,
    val actionLabel: String,
    val actionDateShort: String,
    val actionDateLong: String,
    val actionTimestampMillis: Long = 0L,
    val resolvedByUid: String = "",
    val resolvedByName: String = "Admin",
    val warningTitle: String = "",
    val warningDescription: String = "",
    val warningNotificationId: String = "",
    val warningRecipientUid: String = "",
    val deletedPostBackup: Map<String, Any?>? = null,
)

val SampleAdminHistoryItems = listOf(
    AdminActionHistoryItem(
        id = "history-1",
        report = AdminReportItem(
            id = "history-1",
            reportedUsername = "BibiOlahraga",
            reporterUsername = "PecintaJogging",
            reportCategory = "Kekerasan, kebencian atau eksploitasi",
            reportDate = "10 November 2026",
            type = AdminReportType.POST,
            contentPreview = "Always on Proces",
            postTime = "2 jam lalu",
            likesCount = 57,
            commentsCount = 4,
            sharesCount = 8,
            reportDescription = "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis...",
            postDocumentId = "sample-post-1",
        ),
        actionStatus = REPORT_STATUS_RESOLVED_DELETED,
        actionLabel = "Removed Post",
        actionDateShort = "24/11/2026",
        actionDateLong = "24 November 2026",
        actionTimestampMillis = 1_795_478_400_000L,
        resolvedByName = "BibiOlahraga",
    ),
    AdminActionHistoryItem(
        id = "history-2",
        report = AdminReportItem(
            id = "history-2",
            reportedUsername = "BolaKuy",
            reporterUsername = "RunnerBandung",
            reportCategory = "Hoax atau informasi palsu",
            reportDate = "12 November 2026",
            type = AdminReportType.LOCATION,
            contentPreview = "BolaKuy adalah ruang berkumpulnya para pemain...",
            postTime = "5 jam lalu",
            likesCount = 24,
            commentsCount = 2,
            sharesCount = 3,
            reportDescription = "Alamat lokasi tidak sesuai dengan titik peta sebenarnya.",
        ),
        actionStatus = REPORT_STATUS_RESOLVED_DELETED,
        actionLabel = "Removed Location",
        actionDateShort = "24/11/2026",
        actionDateLong = "24 November 2026",
        actionTimestampMillis = 1_795_474_800_000L,
        resolvedByName = "AdminLapangan",
    ),
    AdminActionHistoryItem(
        id = "history-3",
        report = AdminReportItem(
            id = "history-3",
            reportedUsername = "BibiOlahraga",
            reporterUsername = "PecintaJogging",
            reportCategory = "Kekerasan, kebencian atau eksploitasi",
            reportDate = "15 November 2026",
            type = AdminReportType.POST,
            contentPreview = "Always on Proces",
            postTime = "2 jam lalu",
            likesCount = 57,
            commentsCount = 4,
            sharesCount = 8,
            reportDescription = "Konten mengandung unsur vulgaritas yang perlu diperingatkan.",
            postDocumentId = "sample-post-3",
        ),
        actionStatus = REPORT_STATUS_RESOLVED_WARNED,
        actionLabel = "Warning sent",
        actionDateShort = "24/11/2026",
        actionDateLong = "24 November 2026",
        actionTimestampMillis = 1_795_471_200_000L,
        resolvedByName = "BibiOlahraga",
        warningTitle = "Vulgaritas",
        warningDescription = "Mohon jaga kesopanan konten sesuai pedoman komunitas.",
    ),
    AdminActionHistoryItem(
        id = "history-4",
        report = AdminReportItem(
            id = "history-4",
            reportedUsername = "KenanganIndah",
            reporterUsername = "BibiOlahraga",
            reportCategory = "Penipuan, penggelapan atau spam",
            reportDate = "18 November 2026",
            type = AdminReportType.COMMENT,
            contentPreview = "Ketika kalian jooging jangan lupa untuk abadikan moment saat kalian bersama ya...",
            postTime = "1 hari lalu",
            likesCount = 12,
            commentsCount = 1,
            sharesCount = 0,
            reportDescription = "Komentar berulang bersifat spam pada beberapa postingan.",
        ),
        actionStatus = REPORT_STATUS_RESOLVED_DELETED,
        actionLabel = "Removed Comment",
        actionDateShort = "20/11/2026",
        actionDateLong = "20 November 2026",
        actionTimestampMillis = 1_795_132_800_000L,
        resolvedByName = "AdminKomunitas",
    ),
)

internal fun parseAdminReportType(raw: String?): AdminReportType =
    AdminReportType.entries.firstOrNull {
        it.name.equals(raw?.trim(), ignoreCase = true) ||
            it.label.equals(raw?.trim(), ignoreCase = true)
    } ?: AdminReportType.POST

internal fun isWithinLastHour(
    createdAtMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): Boolean {
    if (createdAtMillis <= 0L) return false
    val diff = nowMillis - createdAtMillis
    return diff in 0L..ONE_HOUR_MILLIS
}

internal fun formatReportDateIndonesian(millis: Long): String {
    if (millis <= 0L) return "-"
    val formatter = SimpleDateFormat("d MMMM yyyy", Locale.forLanguageTag("id-ID"))
    return formatter.format(Date(millis))
}

internal fun formatReportDateShort(millis: Long): String {
    if (millis <= 0L) return "-"
    val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.forLanguageTag("id-ID"))
    return formatter.format(Date(millis))
}

internal fun formatAdminActionLabel(status: String, type: AdminReportType): String =
    when (status.trim().uppercase()) {
        REPORT_STATUS_RESOLVED_DELETED -> when (type) {
            AdminReportType.POST -> "Removed Post"
            AdminReportType.COMMENT -> "Removed Comment"
            AdminReportType.LOCATION -> "Removed Location"
            AdminReportType.ACCOUNT -> "Removed Account"
        }
        REPORT_STATUS_RESOLVED_WARNED -> "Warning sent"
        REPORT_STATUS_IGNORED -> "Ignored Report"
        else -> "Handled"
    }

internal fun parseAdminReportItem(
    id: String,
    data: Map<String, Any?>,
    nowMillis: Long = System.currentTimeMillis(),
): AdminReportItem {
    val createdAtMillis = (data["createdAt"] as? Timestamp)?.toDate()?.time
        ?: (data["createdAtMillis"] as? Number)?.toLong()
        ?: 0L
    val reportedUserId = (data["reportedUserId"] as? String)?.trim().orEmpty()
    val reportedUsername = (data["reportedUsername"] as? String)?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "Pengguna"
    val reporterUsername = (data["reporterUsername"] as? String)?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "Pengguna"
    val reportCategory = (data["reportCategory"] as? String)?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: DefaultAdminReportCategories.first()
    val reportDescription = (data["reportDescription"] as? String)?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "Tidak ada deskripsi tambahan."
    val contentPreview = (data["contentPreview"] as? String)?.trim().orEmpty()
    val thumbnailUrl = (data["thumbnailUrl"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
    val postDocumentId = (data["postDocumentId"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
    val postTime = (data["postTime"] as? String)?.trim()?.takeIf { it.isNotEmpty() } ?: "-"
    val likesCount = (data["likesCount"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
    val commentsCount = (data["commentsCount"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
    val sharesCount = (data["sharesCount"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0

    return AdminReportItem(
        id = id,
        reportedUserId = reportedUserId,
        reportedUsername = reportedUsername,
        reporterUsername = reporterUsername,
        reportCategory = reportCategory,
        reportDate = formatReportDateIndonesian(createdAtMillis),
        type = parseAdminReportType(data["type"] as? String),
        withinLastHour = isWithinLastHour(createdAtMillis, nowMillis),
        thumbnailUrl = thumbnailUrl,
        contentPreview = contentPreview,
        postTime = postTime,
        likesCount = likesCount,
        commentsCount = commentsCount,
        sharesCount = sharesCount,
        reportDescription = reportDescription,
        postDocumentId = postDocumentId,
        createdAtMillis = createdAtMillis,
    )
}

internal fun parseAdminActionHistoryItem(
    id: String,
    data: Map<String, Any?>,
    nowMillis: Long = System.currentTimeMillis(),
): AdminActionHistoryItem? {
    val status = (data["status"] as? String)?.trim().orEmpty()
    if (status !in RESOLVED_REPORT_STATUSES) return null

    val report = parseAdminReportItem(id = id, data = data, nowMillis = nowMillis)
    val resolvedAtMillis = (data["resolvedAt"] as? Timestamp)?.toDate()?.time
        ?: (data["resolvedAtMillis"] as? Number)?.toLong()
        ?: report.createdAtMillis
    val resolvedByUid = (data["resolvedBy"] as? String)?.trim().orEmpty()
    val resolvedByName = (data["resolvedByName"] as? String)?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: "Admin"
    val warningTitle = (data["warningTitle"] as? String)?.trim().orEmpty()
    val warningDescription = (data["warningDescription"] as? String)?.trim().orEmpty()
    val warningNotificationId = (data["warningNotificationId"] as? String)?.trim().orEmpty()
    val warningRecipientUid = (data["warningRecipientUid"] as? String)?.trim().orEmpty()

    @Suppress("UNCHECKED_CAST")
    val deletedPostBackup = (data["deletedPostBackup"] as? Map<*, *>)
        ?.entries
        ?.mapNotNull { (k, v) -> (k as? String)?.let { key -> key to v } }
        ?.toMap()

    return AdminActionHistoryItem(
        id = id,
        report = report,
        actionStatus = status,
        actionLabel = formatAdminActionLabel(status, report.type),
        actionDateShort = formatReportDateShort(resolvedAtMillis),
        actionDateLong = formatReportDateIndonesian(resolvedAtMillis),
        actionTimestampMillis = resolvedAtMillis,
        resolvedByUid = resolvedByUid,
        resolvedByName = resolvedByName,
        warningTitle = warningTitle,
        warningDescription = warningDescription,
        warningNotificationId = warningNotificationId,
        warningRecipientUid = warningRecipientUid,
        deletedPostBackup = deletedPostBackup,
    )
}

internal fun buildCreateReportPayload(
    type: AdminReportType,
    reportedUserId: String,
    reportedUsername: String,
    reporterUserId: String,
    reporterUsername: String,
    reportCategory: String,
    reportDescription: String,
    contentPreview: String,
    thumbnailUrl: String? = null,
    postDocumentId: String? = null,
    postTime: String = "",
    likesCount: Int = 0,
    commentsCount: Int = 0,
    sharesCount: Int = 0,
    createdAt: Any = FieldValue.serverTimestamp(),
): Map<String, Any> {
    val cleanReportedUsername = reportedUsername.trim().ifEmpty { "Pengguna" }.take(100)
    val cleanReporterUsername = reporterUsername.trim().ifEmpty { "Pengguna" }.take(100)
    val cleanCategory = reportCategory.trim().ifEmpty { DefaultAdminReportCategories.first() }.take(120)
    val cleanDescription = reportDescription.trim().take(1000)
    val cleanPreview = contentPreview.trim().take(500)

    return buildMap {
        put("type", type.name)
        put("status", REPORT_STATUS_PENDING)
        put("reportedUserId", reportedUserId.trim().take(128))
        put("reportedUsername", cleanReportedUsername)
        put("reporterUserId", reporterUserId.trim())
        put("reporterUsername", cleanReporterUsername)
        put("reportCategory", cleanCategory)
        put("reportDescription", cleanDescription)
        put("contentPreview", cleanPreview)
        if (!thumbnailUrl.isNullOrBlank()) {
            put("thumbnailUrl", thumbnailUrl.trim().take(1000))
        }
        if (!postDocumentId.isNullOrBlank()) {
            put("postDocumentId", postDocumentId.trim().take(128))
        }
        if (postTime.isNotBlank()) {
            put("postTime", postTime.trim().take(64))
        }
        put("likesCount", likesCount.coerceAtLeast(0))
        put("commentsCount", commentsCount.coerceAtLeast(0))
        put("sharesCount", sharesCount.coerceAtLeast(0))
        put("createdAt", createdAt)
    }
}

internal fun buildResolveReportPayload(
    status: String,
    resolvedBy: String,
    resolvedByName: String = "Admin",
    warningTitle: String? = null,
    warningDescription: String? = null,
    warningNotificationId: String? = null,
    warningRecipientUid: String? = null,
    deletedPostBackup: Map<String, Any>? = null,
    resolvedAt: Any = FieldValue.serverTimestamp(),
): Map<String, Any> = buildMap {
    put("status", status)
    put("resolvedBy", resolvedBy.trim())
    put("resolvedByName", resolvedByName.trim().ifEmpty { "Admin" }.take(100))
    put("resolvedAt", resolvedAt)
    if (warningTitle != null) {
        put("warningTitle", warningTitle.trim().take(120))
    }
    if (warningDescription != null) {
        put("warningDescription", warningDescription.trim().take(1000))
    }
    if (!warningNotificationId.isNullOrBlank()) {
        put("warningNotificationId", warningNotificationId.trim().take(128))
    }
    if (!warningRecipientUid.isNullOrBlank()) {
        put("warningRecipientUid", warningRecipientUid.trim().take(128))
    }
    if (!deletedPostBackup.isNullOrEmpty()) {
        put("deletedPostBackup", deletedPostBackup)
    }
}

internal fun sanitizePostBackupMap(raw: Map<String, Any?>?): Map<String, Any>? {
    if (raw.isNullOrEmpty()) return null
    val allowedKeys = setOf(
        "authorId", "authorName", "caption", "category", "searchKeywords",
        "imageUrl", "taggedFriendIds", "taggedFriends",
        "likes", "likedByIds", "likedByNames", "comment", "Share",
    )
    return buildMap {
        raw.forEach { (key, value) ->
            if (key in allowedKeys && value != null) {
                put(key, value)
            }
        }
    }.takeIf { it.isNotEmpty() }
}

internal fun buildRestorePostPayload(
    item: AdminActionHistoryItem,
    restoredAt: Any = FieldValue.serverTimestamp(),
): Map<String, Any>? {
    val postId = item.report.postDocumentId?.trim().orEmpty()
    if (postId.isEmpty()) return null

    val backup = item.deletedPostBackup
    val authorId = ((backup?.get("authorId") as? String)?.trim()?.takeIf { it.isNotEmpty() })
        ?: item.report.reportedUserId.trim()
    val authorName = ((backup?.get("authorName") as? String)?.trim()?.takeIf { it.isNotEmpty() })
        ?: item.report.reportedUsername.trim().ifEmpty { "Pengguna" }
    val caption = ((backup?.get("caption") as? String) ?: item.report.contentPreview).take(2000)

    if (authorId.isEmpty()) return null

    return buildMap {
        put("authorId", authorId.take(128))
        put("authorName", authorName.take(100))
        put("caption", caption)
        (backup?.get("category") as? String)?.takeIf { it.isNotBlank() }?.let {
            put("category", it.take(200))
        }
        (backup?.get("searchKeywords") as? List<*>)
            ?.mapNotNull { (it as? String)?.takeIf { s -> s.isNotBlank() } }
            ?.take(100)
            ?.takeIf { it.isNotEmpty() }
            ?.let { put("searchKeywords", it) }
        val imageUrl = ((backup?.get("imageUrl") as? String)?.takeIf { it.isNotBlank() })
            ?: item.report.thumbnailUrl
        if (!imageUrl.isNullOrBlank()) {
            put("imageUrl", imageUrl.take(1000))
        }
        (backup?.get("taggedFriendIds") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.take(50)
            ?.takeIf { it.isNotEmpty() }
            ?.let { put("taggedFriendIds", it) }
        (backup?.get("taggedFriends") as? List<*>)
            ?.filterIsInstance<Map<*, *>>()
            ?.take(50)
            ?.takeIf { it.isNotEmpty() }
            ?.let { put("taggedFriends", it) }
        put("likes", ((backup?.get("likes") as? Number)?.toLong() ?: item.report.likesCount.toLong()).coerceAtLeast(0L))
        (backup?.get("likedByIds") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.take(500)
            ?.let { put("likedByIds", it) }
        (backup?.get("likedByNames") as? List<*>)
            ?.mapNotNull { it as? String }
            ?.take(500)
            ?.let { put("likedByNames", it) }
        put("comment", ((backup?.get("comment") as? Number)?.toLong() ?: item.report.commentsCount.toLong()).coerceAtLeast(0L))
        put("Share", ((backup?.get("Share") as? Number)?.toLong() ?: item.report.sharesCount.toLong()).coerceAtLeast(0L))
        put("createdAt", restoredAt)
    }
}

internal fun buildRestoreReportPayload(): Map<String, Any> = mapOf(
    "status" to REPORT_STATUS_PENDING,
    "resolvedBy" to FieldValue.delete(),
    "resolvedByName" to FieldValue.delete(),
    "resolvedAt" to FieldValue.delete(),
    "warningTitle" to FieldValue.delete(),
    "warningDescription" to FieldValue.delete(),
    "warningNotificationId" to FieldValue.delete(),
    "warningRecipientUid" to FieldValue.delete(),
    "deletedPostBackup" to FieldValue.delete(),
)

internal fun buildAdminWarningNotificationPayload(
    adminUid: String,
    adminDisplayName: String,
    postId: String?,
    title: String,
    description: String,
    createdAt: Any = FieldValue.serverTimestamp(),
): Map<String, Any> {
    val cleanTitle = title.trim().ifEmpty { "Notifikasi Warning" }.take(120)
    val cleanDescription = description.trim().take(1000)
    val cleanAdminName = adminDisplayName.trim().ifEmpty { "Admin" }.take(100)
    return buildMap {
        put("actorId", adminUid.trim())
        put("actorName", cleanAdminName)
        if (!postId.isNullOrBlank()) {
            put("postId", postId.trim().take(100))
        }
        put("type", NotificationType.WARNING.code)
        put("title", cleanTitle)
        put("description", cleanDescription)
        put("createdAt", createdAt)
    }
}

internal fun mapAdminFirestoreError(error: Throwable, fallback: String): String {
    val isDenied = (error as? FirebaseFirestoreException)?.code ==
        FirebaseFirestoreException.Code.PERMISSION_DENIED ||
        error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
    return if (isDenied) {
        "Akses ditolak oleh Firestore. Pastikan akun memiliki role 'admin' dan Firestore Rules terbaru sudah dipublikasikan."
    } else {
        error.localizedMessage?.takeIf { it.isNotBlank() } ?: fallback
    }
}

private fun resolveAdminActorName(
    db: FirebaseFirestore,
    admin: FirebaseUser,
    onResolved: (String) -> Unit,
) {
    val fallback = admin.resolveDisplayName("Admin")
    db.collection("users")
        .document(admin.uid)
        .get()
        .addOnSuccessListener { doc ->
            val name = doc.getString("username")?.trim()?.takeIf { it.isNotEmpty() }
                ?: doc.getString("displayName")?.trim()?.takeIf { it.isNotEmpty() }
                ?: fallback
            onResolved(name)
        }
        .addOnFailureListener {
            onResolved(fallback)
        }
}

/**
 * Mengamati daftar laporan berstatus PENDING dari Firestore secara real-time.
 * Pengurutan dilakukan di sisi klien agar tidak memerlukan composite index manual.
 */
fun observePendingAdminReports(
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onReportsChanged: (List<AdminReportItem>) -> Unit,
    onError: (String) -> Unit,
): ListenerRegistration {
    return db.collection(REPORTS_COLLECTION)
        .whereEqualTo("status", REPORT_STATUS_PENDING)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(mapAdminFirestoreError(error, "Gagal memuat daftar laporan."))
                return@addSnapshotListener
            }
            val nowMillis = System.currentTimeMillis()
            val items = snapshot?.documents.orEmpty()
                .map { doc -> parseAdminReportItem(doc.id, doc.data.orEmpty(), nowMillis) }
                .sortedByDescending { it.createdAtMillis }
            onReportsChanged(items)
        }
}

/**
 * Mengamati riwayat aksi moderasi dari SELURUH pengguna dengan role admin secara real-time.
 * Semua dokumen `reports` yang sudah ditangani (`IGNORED`, `RESOLVED_DELETED`, `RESOLVED_WARNED`)
 * digabung dan diurutkan berdasarkan waktu aksi terbaru di sisi klien.
 */
fun observeAdminActionHistory(
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    onHistoryChanged: (List<AdminActionHistoryItem>) -> Unit,
    onError: (String) -> Unit,
): ListenerRegistration {
    return db.collection(REPORTS_COLLECTION)
        .whereIn("status", RESOLVED_REPORT_STATUSES)
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                onError(mapAdminFirestoreError(error, "Gagal memuat riwayat aksi admin."))
                return@addSnapshotListener
            }
            val nowMillis = System.currentTimeMillis()
            val items = snapshot?.documents.orEmpty()
                .mapNotNull { doc -> parseAdminActionHistoryItem(doc.id, doc.data.orEmpty(), nowMillis) }
                .sortedByDescending { it.actionTimestampMillis }
            onHistoryChanged(items)
        }
}

/** Mengirim laporan baru untuk sebuah [PostUiModel] ke koleksi `reports`. */
fun submitPostReport(
    post: PostUiModel,
    reportCategory: String,
    reportDescription: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val user = currentUserOrNull()
    if (user == null) {
        onError("Silakan masuk terlebih dahulu untuk melaporkan postingan.")
        return
    }
    val payload = buildCreateReportPayload(
        type = AdminReportType.POST,
        reportedUserId = post.authorId,
        reportedUsername = post.authorName,
        reporterUserId = user.uid,
        reporterUsername = user.resolveDisplayName("Pengguna"),
        reportCategory = reportCategory,
        reportDescription = reportDescription,
        contentPreview = post.caption,
        thumbnailUrl = post.imageUrl,
        postDocumentId = post.id,
        postTime = post.time,
        likesCount = post.likesCount.toInt(),
        commentsCount = post.commentsCount.toInt(),
        sharesCount = post.sharesCount.toInt(),
    )
    db.collection(REPORTS_COLLECTION)
        .add(payload)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Gagal mengirim laporan ke server.")
        }
}

/** Mengirim laporan baru untuk sebuah [UserPost] (dari halaman profil pengguna lain). */
fun submitUserPostReport(
    post: UserPost,
    reportedUserId: String,
    reportedUsername: String,
    reportCategory: String,
    reportDescription: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val user = currentUserOrNull()
    if (user == null) {
        onError("Silakan masuk terlebih dahulu untuk melaporkan postingan.")
        return
    }
    val payload = buildCreateReportPayload(
        type = AdminReportType.POST,
        reportedUserId = reportedUserId,
        reportedUsername = reportedUsername,
        reporterUserId = user.uid,
        reporterUsername = user.resolveDisplayName("Pengguna"),
        reportCategory = reportCategory,
        reportDescription = reportDescription,
        contentPreview = post.caption,
        thumbnailUrl = post.imageUrl,
        postDocumentId = post.documentId,
        postTime = post.time,
    )
    db.collection(REPORTS_COLLECTION)
        .add(payload)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e ->
            onError(e.localizedMessage ?: "Gagal mengirim laporan ke server.")
        }
}

/** Mengabaikan laporan dengan memperbarui status menjadi `IGNORED` dan mencatat pelaksana admin. */
fun ignoreAdminReport(
    report: AdminReportItem,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val admin = currentUserOrNull()
    if (admin == null) {
        onError("Sesi admin tidak ditemukan. Silakan masuk kembali.")
        return
    }
    resolveAdminActorName(db, admin) { adminName ->
        val updates = buildResolveReportPayload(
            status = REPORT_STATUS_IGNORED,
            resolvedBy = admin.uid,
            resolvedByName = adminName,
        )
        db.collection(REPORTS_COLLECTION)
            .document(report.id)
            .update(updates)
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(mapAdminFirestoreError(e, "Gagal mengabaikan laporan."))
            }
    }
}

/**
 * Menghapus postingan yang dilaporkan sekaligus menandai laporan sebagai `RESOLVED_DELETED`
 * secara atomik menggunakan WriteBatch. Snapshot data postingan disimpan agar bisa dipulihkan
 * lewat halaman History Action.
 */
fun deleteReportedPost(
    report: AdminReportItem,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val admin = currentUserOrNull()
    if (admin == null) {
        onError("Sesi admin tidak ditemukan. Silakan masuk kembali.")
        return
    }
    val postId = report.postDocumentId?.trim().orEmpty()

    fun commitDelete(adminName: String, backup: Map<String, Any>?) {
        val batch = db.batch()
        if (postId.isNotEmpty()) {
            batch.delete(db.collection("posts").document(postId))
        }
        val reportRef = db.collection(REPORTS_COLLECTION).document(report.id)
        batch.update(
            reportRef,
            buildResolveReportPayload(
                status = REPORT_STATUS_RESOLVED_DELETED,
                resolvedBy = admin.uid,
                resolvedByName = adminName,
                deletedPostBackup = backup,
            ),
        )
        batch.commit()
            .addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e ->
                onError(mapAdminFirestoreError(e, "Gagal menghapus postingan yang dilaporkan."))
            }
    }

    resolveAdminActorName(db, admin) { adminName ->
        if (postId.isEmpty()) {
            commitDelete(adminName, null)
        } else {
            db.collection("posts")
                .document(postId)
                .get()
                .addOnSuccessListener { postDoc ->
                    val backup = sanitizePostBackupMap(postDoc.data)
                    commitDelete(adminName, backup)
                }
                .addOnFailureListener {
                    commitDelete(adminName, null)
                }
        }
    }
}

/**
 * Mengirim notifikasi peringatan ke `users/{reportedUserId}/notifications` sekaligus
 * menandai laporan sebagai `RESOLVED_WARNED` secara atomik.
 */
fun sendAdminWarningNotification(
    report: AdminReportItem,
    title: String,
    description: String,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val admin = currentUserOrNull()
    if (admin == null) {
        onError("Sesi admin tidak ditemukan. Silakan masuk kembali.")
        return
    }

    resolveAdminActorName(db, admin) { adminName ->
        fun commitWarningForUser(targetUid: String) {
            val batch = db.batch()
            val notifRef = db.collection("users")
                .document(targetUid)
                .collection("notifications")
                .document()
            val notifPayload = buildAdminWarningNotificationPayload(
                adminUid = admin.uid,
                adminDisplayName = adminName,
                postId = report.postDocumentId,
                title = title,
                description = description,
            )
            batch.set(notifRef, notifPayload)

            val reportRef = db.collection(REPORTS_COLLECTION).document(report.id)
            batch.update(
                reportRef,
                buildResolveReportPayload(
                    status = REPORT_STATUS_RESOLVED_WARNED,
                    resolvedBy = admin.uid,
                    resolvedByName = adminName,
                    warningTitle = title,
                    warningDescription = description,
                    warningNotificationId = notifRef.id,
                    warningRecipientUid = targetUid,
                ),
            )
            batch.commit()
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { e ->
                    onError(mapAdminFirestoreError(e, "Gagal mengirim notifikasi peringatan."))
                }
        }

        val directUid = report.reportedUserId.trim()
        if (directUid.isNotEmpty()) {
            commitWarningForUser(directUid)
            return@resolveAdminActorName
        }

        // Fallback 1: cari authorId dari dokumen postingan jika tersedia
        val postId = report.postDocumentId?.trim().orEmpty()
        if (postId.isNotEmpty()) {
            db.collection("posts")
                .document(postId)
                .get()
                .addOnSuccessListener { postDoc ->
                    val authorId = postDoc.getString("authorId")?.trim().orEmpty()
                    if (authorId.isNotEmpty()) {
                        commitWarningForUser(authorId)
                    } else {
                        resolveUserIdByUsername(db, report.reportedUsername, ::commitWarningForUser, onError)
                    }
                }
                .addOnFailureListener {
                    resolveUserIdByUsername(db, report.reportedUsername, ::commitWarningForUser, onError)
                }
        } else {
            resolveUserIdByUsername(db, report.reportedUsername, ::commitWarningForUser, onError)
        }
    }
}

/**
 * Menghapus permanen dokumen riwayat aksi dari koleksi `reports`.
 */
fun permanentlyDeleteAdminHistoryItem(
    item: AdminActionHistoryItem,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val admin = currentUserOrNull()
    if (admin == null) {
        onError("Sesi admin tidak ditemukan. Silakan masuk kembali.")
        return
    }
    val batch = db.batch()
    batch.delete(db.collection(REPORTS_COLLECTION).document(item.id))
    val postId = item.report.postDocumentId?.trim().orEmpty()
    if (item.actionStatus == REPORT_STATUS_RESOLVED_DELETED && postId.isNotEmpty()) {
        batch.delete(db.collection("posts").document(postId))
    }
    batch.commit()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e ->
            onError(mapAdminFirestoreError(e, "Gagal menghapus riwayat secara permanen."))
        }
}

/**
 * Memulihkan item riwayat aksi:
 * - Jika `RESOLVED_DELETED` dan memiliki data postingan: membuat ulang dokumen di `posts/{postId}`.
 * - Jika `RESOLVED_WARNED` dan memiliki ID notifikasi: menghapus notifikasi peringatan tersebut.
 * - Mengembalikan status dokumen laporan menjadi `PENDING` sehingga kembali muncul di Dashboard Report.
 */
fun restoreAdminHistoryItem(
    item: AdminActionHistoryItem,
    onSuccess: () -> Unit,
    onError: (String) -> Unit,
    db: FirebaseFirestore = FirebaseFirestore.getInstance(),
) {
    val admin = currentUserOrNull()
    if (admin == null) {
        onError("Sesi admin tidak ditemukan. Silakan masuk kembali.")
        return
    }
    val batch = db.batch()
    if (item.actionStatus == REPORT_STATUS_RESOLVED_DELETED) {
        val postId = item.report.postDocumentId?.trim().orEmpty()
        val restorePayload = buildRestorePostPayload(item)
        if (postId.isNotEmpty() && restorePayload != null) {
            batch.set(db.collection("posts").document(postId), restorePayload)
        }
    }
    if (item.actionStatus == REPORT_STATUS_RESOLVED_WARNED) {
        val notifId = item.warningNotificationId.trim()
        val recipientUid = item.warningRecipientUid.trim().ifEmpty { item.report.reportedUserId.trim() }
        if (notifId.isNotEmpty() && recipientUid.isNotEmpty()) {
            batch.delete(
                db.collection("users")
                    .document(recipientUid)
                    .collection("notifications")
                    .document(notifId),
            )
        }
    }
    batch.update(
        db.collection(REPORTS_COLLECTION).document(item.id),
        buildRestoreReportPayload(),
    )
    batch.commit()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e ->
            onError(mapAdminFirestoreError(e, "Gagal memulihkan tindakan admin."))
        }
}

private fun resolveUserIdByUsername(
    db: FirebaseFirestore,
    username: String,
    onFound: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val cleanName = username.trim()
    if (cleanName.isEmpty()) {
        onError("Akun pengguna yang dilaporkan tidak memiliki ID tujuan.")
        return
    }
    db.collection("users")
        .whereEqualTo("username", cleanName)
        .limit(1)
        .get()
        .addOnSuccessListener { snap ->
            val foundId = snap.documents.firstOrNull()?.id
            if (!foundId.isNullOrBlank()) {
                onFound(foundId)
            } else {
                db.collection("users")
                    .whereEqualTo("displayName", cleanName)
                    .limit(1)
                    .get()
                    .addOnSuccessListener { displaySnap ->
                        val fallbackId = displaySnap.documents.firstOrNull()?.id
                        if (!fallbackId.isNullOrBlank()) {
                            onFound(fallbackId)
                        } else {
                            onError("Akun @$cleanName tidak ditemukan di database untuk menerima peringatan.")
                        }
                    }
                    .addOnFailureListener { e ->
                        onError(mapAdminFirestoreError(e, "Gagal mencari akun tujuan peringatan."))
                    }
            }
        }
        .addOnFailureListener { e ->
            onError(mapAdminFirestoreError(e, "Gagal mencari akun tujuan peringatan."))
        }
}
