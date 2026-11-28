package com.example.olahragakuy.pages.posts

import androidx.compose.runtime.mutableStateMapOf
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException

const val MAX_POST_REPORT_DETAIL_LENGTH = 300

/** Pilihan alasan pelaporan postingan yang selaras dengan nilai komunitas OlahragaKuy. */
enum class PostReportReason(val label: String) {
    SPAM("Spam atau promosi berlebihan"),
    HARASSMENT("Perundungan, ejekan, atau bahasa kasar"),
    BODY_SHAMING("Body shaming atau merendahkan pemula"),
    MISINFORMATION("Info olahraga/kesehatan menyesatkan"),
    PRIVACY("Membagikan privasi atau lokasi tanpa izin"),
    INAPPROPRIATE("Konten tidak pantas"),
    OTHER("Lainnya"),
}

/** State lokal sesi aktif untuk menandai postingan yang sudah dilaporkan pengguna. */
val LocalReportedPostIds = mutableStateMapOf<String, PostReportReason>()

/** Mengecek apakah postingan dimiliki oleh pengguna yang sedang masuk. */
fun isOwnPost(post: PostUiModel, currentUserId: String?): Boolean {
    val uid = currentUserId?.trim().orEmpty()
    val authorId = post.authorId.trim()
    return uid.isNotEmpty() && authorId.isNotEmpty() && (uid == authorId)
}

/** Postingan hanya dapat dilaporkan bila valid dan bukan milik pengguna sendiri. */
fun canReportPost(post: PostUiModel, currentUserId: String?): Boolean =
    post.id.trim().isNotEmpty() && !isOwnPost(post, currentUserId)

/** Memvalidasi input laporan sebelum dikirim ke Firestore. Mengembalikan pesan error atau null jika valid. */
fun validatePostReportInput(
    post: PostUiModel,
    reporterId: String?,
    reason: PostReportReason?,
    detail: String,
): String? {
    val uid = reporterId?.trim().orEmpty()
    if (uid.isEmpty()) return "Silakan masuk untuk melaporkan postingan."
    if (post.id.trim().isEmpty()) return "Postingan tidak valid untuk dilaporkan."
    if (isOwnPost(post, uid)) return "Kamu tidak dapat melaporkan postingan milik sendiri."
    if (reason == null) return "Pilih alasan pelaporan terlebih dahulu."
    val trimmedDetail = detail.trim()
    if (reason == PostReportReason.OTHER && trimmedDetail.isEmpty()) {
        return "Mohon jelaskan singkat alasan pelaporan."
    }
    if (trimmedDetail.length > MAX_POST_REPORT_DETAIL_LENGTH) {
        return "Catatan tambahan maksimal $MAX_POST_REPORT_DETAIL_LENGTH karakter."
    }
    return null
}

/** Membentuk ID dokumen deterministik untuk mencegah laporan ganda per pengguna per postingan. */
fun postReportDocumentId(postId: String, reporterId: String): String =
    "post_${postId.trim()}_${reporterId.trim()}"

/**
 * Membentuk payload dokumen Firestore untuk koleksi top-level `reports/{reportId}`
 * sesuai skema moderasi admin (`isValidReportCreate` di `firestore.rules`).
 */
fun buildPostReportData(
    post: PostUiModel,
    reporterId: String,
    reporterName: String,
    reason: PostReportReason,
    detail: String,
): Map<String, Any> = buildMap {
    put("type", "POST")
    put("status", "PENDING")
    val authorId = post.authorId.trim().take(128)
    if (authorId.isNotEmpty()) {
        put("reportedUserId", authorId)
    }
    put("reportedUsername", post.authorName.trim().ifEmpty { "Pengguna" }.take(100))
    put("reporterUserId", reporterId.trim())
    put("reporterUsername", reporterName.trim().ifEmpty { "Pengguna" }.take(100))
    put("reportCategory", reason.label.take(120))
    val trimmedDetail = detail.trim().take(MAX_POST_REPORT_DETAIL_LENGTH)
    if (trimmedDetail.isNotEmpty()) {
        put("reportDescription", trimmedDetail)
    }
    val preview = post.caption.trim().take(500)
    if (preview.isNotEmpty()) {
        put("contentPreview", preview)
    }
    val thumbnail = post.imageUrl?.trim()?.take(1000).orEmpty()
    if (thumbnail.isNotEmpty()) {
        put("thumbnailUrl", thumbnail)
    }
    put("postDocumentId", post.id.trim().take(128))
    val postTime = post.time.trim().take(64)
    if (postTime.isNotEmpty()) {
        put("postTime", postTime)
    }
    put("likesCount", post.likesCount.coerceAtLeast(0L))
    put("commentsCount", post.commentsCount.coerceAtLeast(0L))
    put("sharesCount", post.sharesCount.coerceAtLeast(0L))
    put("createdAt", FieldValue.serverTimestamp())
}

/**
 * Mengirim laporan postingan ke koleksi top-level `reports/{reportId}` agar masuk ke antrean
 * moderasi admin (`type = "POST"`, `status = "PENDING"`), sekaligus mencegah duplikasi per pengguna.
 */
fun submitPostReport(
    post: PostUiModel,
    reason: PostReportReason,
    detail: String,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val user = currentUserOrNull()
    val validationError = validatePostReportInput(
        post = post,
        reporterId = user?.uid,
        reason = reason,
        detail = detail,
    )
    if (validationError != null) {
        onError(validationError)
        return
    }
    if (post.id in LocalReportedPostIds) {
        onError("Kamu sudah melaporkan postingan ini sebelumnya.")
        return
    }

    val safeUser = user ?: return
    val reporterName = safeUser.resolveDisplayName("Pengguna")
    val payload = buildPostReportData(
        post = post,
        reporterId = safeUser.uid,
        reporterName = reporterName,
        reason = reason,
        detail = detail,
    )

    val reportDocId = postReportDocumentId(post.id, safeUser.uid)
    val reportRef = FirebaseFirestore.getInstance()
        .collection("reports")
        .document(reportDocId)

    reportRef.set(payload)
        .addOnSuccessListener {
            LocalReportedPostIds[post.id] = reason
            onSuccess("Laporan berhasil dikirim. Terima kasih telah membantu menjaga komunitas OlahragaKuy tetap positif.")
        }
        .addOnFailureListener { error ->
            val isPermissionDenied = (error as? FirebaseFirestoreException)?.code ==
                FirebaseFirestoreException.Code.PERMISSION_DENIED ||
                error.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
            if (isPermissionDenied) {
                LocalReportedPostIds[post.id] = reason
                onSuccess("Terima kasih, laporan untuk postingan ini telah dicatat.")
            } else {
                onError(error.localizedMessage ?: "Gagal mengirim laporan. Silakan coba lagi.")
            }
        }
}
