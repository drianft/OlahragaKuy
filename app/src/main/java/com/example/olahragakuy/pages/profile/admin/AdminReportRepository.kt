package com.example.olahragakuy.pages.profile.admin

import com.example.olahragakuy.pages.posts.NotificationType
import com.example.olahragakuy.pages.posts.PostUiModel
import com.example.olahragakuy.pages.posts.currentUserOrNull
import com.example.olahragakuy.pages.posts.resolveDisplayName
import com.example.olahragakuy.pages.profile.user.UserPost
import com.google.firebase.Timestamp
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

private const val ONE_HOUR_MILLIS = 60L * 60L * 1000L

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
    warningTitle: String? = null,
    warningDescription: String? = null,
    resolvedAt: Any = FieldValue.serverTimestamp(),
): Map<String, Any> = buildMap {
    put("status", status)
    put("resolvedBy", resolvedBy.trim())
    put("resolvedAt", resolvedAt)
    if (warningTitle != null) {
        put("warningTitle", warningTitle.trim().take(120))
    }
    if (warningDescription != null) {
        put("warningDescription", warningDescription.trim().take(1000))
    }
}

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

/** Mengabaikan laporan dengan memperbarui status menjadi `IGNORED`. */
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
    val updates = buildResolveReportPayload(
        status = REPORT_STATUS_IGNORED,
        resolvedBy = admin.uid,
    )
    db.collection(REPORTS_COLLECTION)
        .document(report.id)
        .update(updates)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e ->
            onError(mapAdminFirestoreError(e, "Gagal mengabaikan laporan."))
        }
}

/**
 * Menghapus postingan yang dilaporkan sekaligus menandai laporan sebagai `RESOLVED_DELETED`
 * secara atomik menggunakan WriteBatch.
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
    val batch = db.batch()
    val postId = report.postDocumentId?.trim().orEmpty()
    if (postId.isNotEmpty()) {
        batch.delete(db.collection("posts").document(postId))
    }
    val reportRef = db.collection(REPORTS_COLLECTION).document(report.id)
    batch.update(
        reportRef,
        buildResolveReportPayload(
            status = REPORT_STATUS_RESOLVED_DELETED,
            resolvedBy = admin.uid,
        ),
    )
    batch.commit()
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { e ->
            onError(mapAdminFirestoreError(e, "Gagal menghapus postingan yang dilaporkan."))
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

    fun commitWarningForUser(targetUid: String) {
        val batch = db.batch()
        val notifRef = db.collection("users")
            .document(targetUid)
            .collection("notifications")
            .document()
        val notifPayload = buildAdminWarningNotificationPayload(
            adminUid = admin.uid,
            adminDisplayName = admin.resolveDisplayName("Admin"),
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
                warningTitle = title,
                warningDescription = description,
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
        return
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
