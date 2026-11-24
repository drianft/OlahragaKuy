package com.example.olahragakuy.pages.profile.user

import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.text.DateFormat

internal data class UserProfileSummary(
    val username: String,
    val displayName: String,
    val bio: String,
    val photoUrl: String? = null,
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val postsCount: Int = 0,
)

/** Uses only fields permitted by the current users rules. Identity is the document ID. */
internal fun initialProfileFields(
    preferredUsername: String?,
    displayName: String?,
    email: String?,
    photoUrl: String?,
): Map<String, Any> {
    val username = preferredUsername?.trim()?.takeIf { it.isNotEmpty() }
        ?: displayName?.trim()?.takeIf { it.isNotEmpty() }
        ?: email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        ?: "Pengguna"
    return buildMap {
        put("username", username)
        put("displayName", displayName?.trim()?.takeIf { it.isNotEmpty() } ?: username)
        put("name", displayName?.trim()?.takeIf { it.isNotEmpty() } ?: username)
        put("followersCount", 0)
        put("followingCount", 0)
        put("postsCount", 0)
        photoUrl?.takeIf { it.isNotBlank() }?.let { put("photoUrl", it) }
    }
}

/** Builds the Firestore update map when the user saves profile changes. */
internal fun updatedProfileFields(
    username: String,
    bio: String,
    fallbackUsername: String = "Pengguna",
): Map<String, Any> {
    val resolvedUsername = username.trim().takeIf { it.isNotEmpty() }
        ?: fallbackUsername.trim().takeIf { it.isNotEmpty() }
        ?: "Pengguna"
    return mapOf(
        "username" to resolvedUsername,
        "displayName" to resolvedUsername,
        "name" to resolvedUsername,
        "bio" to bio.trim(),
    )
}

/** Parses a Firestore `users/{uid}` map safely into [UserProfileSummary]. */
internal fun parseUserProfileSnapshot(
    data: Map<String, Any?>,
    fallbackName: String,
): UserProfileSummary {
    val cleanFallback = fallbackName.trim().ifEmpty { "Pengguna" }
    val username = (data["username"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: (data["displayName"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: (data["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: cleanFallback
    val displayName = (data["displayName"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: (data["name"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: username
    val bio = (data["bio"] as? String)?.trim().orEmpty()
    val photoUrl = (data["photoUrl"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
        ?: (data["avatarUrl"] as? String)?.trim()?.takeIf { it.isNotEmpty() }
    val followers = (data["followersCount"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
    val following = (data["followingCount"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
    val posts = (data["postsCount"] as? Number)?.toInt()?.coerceAtLeast(0) ?: 0
    return UserProfileSummary(
        username = username,
        displayName = displayName,
        bio = bio,
        photoUrl = photoUrl,
        followersCount = followers,
        followingCount = following,
        postsCount = posts,
    )
}

/**
 * Returns whether a Firestore profile read error should surface a blocking retry dialog.
 * `PERMISSION_DENIED` falls back gracefully to post author info so the screen stays usable
 * while Firestore rules are being updated.
 */
internal fun shouldShowProfileLoadError(errorCodeName: String?): Boolean =
    !errorCodeName.isNullOrBlank() && errorCodeName != "PERMISSION_DENIED"

/** Adjusts displayed follower count optimistically when toggling follow on another user's profile. */
internal fun calculateFollowerDisplayCount(
    baseFollowers: Int,
    wasInitiallyFollowing: Boolean,
    isCurrentlyFollowing: Boolean,
): Int {
    val delta = when {
        !wasInitiallyFollowing && isCurrentlyFollowing -> 1
        wasInitiallyFollowing && !isCurrentlyFollowing -> -1
        else -> 0
    }
    val adjusted = (baseFollowers + delta).coerceAtLeast(0)
    return if (isCurrentlyFollowing) maxOf(adjusted, 1) else adjusted
}

/** Maps raw Firestore post records into [UserPost] sorted newest-first without needing a composite index. */
internal fun parseAndSortUserPosts(
    records: List<Pair<String, Map<String, Any?>>>,
    formatTime: (Long) -> String = { millis ->
        DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(millis)
    },
): List<UserPost> {
    data class TimedPost(val createdAtMillis: Long, val post: UserPost)
    return records
        .map { (docId, data) ->
            val createdAtMillis = when (val raw = data["createdAt"]) {
                is Timestamp -> raw.toDate().time
                is Number -> raw.toLong()
                else -> 0L
            }
            val caption = (data["caption"] as? String)?.takeIf { it.isNotBlank() } ?: "(Tanpa caption)"
            val imageUrl = (data["imageUrl"] as? String)?.takeIf { it.isNotBlank() }
            val formattedTime = if (createdAtMillis > 0L) formatTime(createdAtMillis) else "Baru saja"
            TimedPost(
                createdAtMillis = createdAtMillis,
                post = UserPost(
                    id = docId.hashCode(),
                    caption = caption,
                    time = formattedTime,
                    imageUrl = imageUrl,
                    documentId = docId,
                ),
            )
        }
        .sortedByDescending { it.createdAtMillis }
        .map { it.post }
}

internal fun List<DocumentSnapshot>.toSortedUserPosts(): List<UserPost> =
    parseAndSortUserPosts(map { it.id to it.data.orEmpty() })

/**
 * Ensures the user's `users/{uid}` document exists with the standard profile schema
 * (`displayName`, `followersCount`, `followingCount`, `name`, `postsCount`, `updatedAt`, `username`)
 * and syncs the loaded `username` / `displayName` back to FirebaseAuth if blank.
 */
internal fun ensureUserProfile(
    user: FirebaseUser,
    preferredUsername: String? = null,
): Task<String> {
    val db = FirebaseFirestore.getInstance()
    val reference = db.collection("users").document(user.uid)
    val defaults = initialProfileFields(
        preferredUsername, user.displayName, user.email, user.photoUrl?.toString(),
    )
    return db.runTransaction { transaction ->
        val document = transaction.get(reference)
        if (!document.exists()) {
            transaction.set(
                reference,
                defaults + ("updatedAt" to FieldValue.serverTimestamp()),
            )
            val createdName = (defaults["displayName"] as? String)
                ?: (defaults["username"] as? String)
            if (user.displayName.isNullOrBlank() && !createdName.isNullOrBlank()) {
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(createdName).build(),
                )
            }
        } else {
            val missingFields = mutableMapOf<String, Any>()
            for ((key, defaultVal) in defaults) {
                val existing = document.get(key)
                val isMissing = when (existing) {
                    null -> true
                    is String -> existing.isBlank()
                    else -> false
                }
                if (isMissing) {
                    missingFields[key] = defaultVal
                }
            }
            if (missingFields.isNotEmpty()) {
                missingFields["updatedAt"] = FieldValue.serverTimestamp()
                transaction.set(reference, missingFields, SetOptions.merge())
            }
            val loadedName = document.getString("displayName")?.takeIf { it.isNotBlank() }
                ?: document.getString("username")?.takeIf { it.isNotBlank() }
                ?: document.getString("name")?.takeIf { it.isNotBlank() }
            if (!loadedName.isNullOrBlank() && user.displayName != loadedName) {
                user.updateProfile(
                    UserProfileChangeRequest.Builder().setDisplayName(loadedName).build(),
                )
            }
        }
        document.getString("role") ?: "member"
    }
}

/**
 * Uploads edited profile fields (`username`, `displayName`, `name`, `bio`, `updatedAt`) to
 * `users/{uid}` in Firestore, falling back to rules without `bio` if the server rules haven't
 * added `bio` yet, and keeps FirebaseAuth `displayName` synchronized.
 */
internal fun saveUserProfileToFirestore(
    user: FirebaseUser,
    username: String,
    bio: String,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {},
) {
    val db = FirebaseFirestore.getInstance()
    val reference = db.collection("users").document(user.uid)
    val fallbackName = user.displayName?.takeIf { it.isNotBlank() }
        ?: user.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        ?: "Pengguna"
    val fields = updatedProfileFields(username, bio, fallbackName)
    val resolvedUsername = fields["username"] as String
    val fullPayload = fields + ("updatedAt" to FieldValue.serverTimestamp())

    user.updateProfile(
        UserProfileChangeRequest.Builder().setDisplayName(resolvedUsername).build(),
    )

    reference.set(fullPayload, SetOptions.merge())
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener {
            val compatPayload = mapOf(
                "username" to resolvedUsername,
                "displayName" to resolvedUsername,
                "name" to resolvedUsername,
                "updatedAt" to FieldValue.serverTimestamp(),
            )
            reference.set(compatPayload, SetOptions.merge())
                .addOnSuccessListener { onSuccess() }
                .addOnFailureListener { err ->
                    onError(err.localizedMessage ?: "Gagal menyimpan perubahan profil ke Firestore.")
                }
        }
}

/**
 * Follows or unfollows [targetUid] atomically in Firestore:
 * - updates `users/{currentUid}/following/{targetUid}` & `users/{currentUid}.followingCount`
 * - updates `users/{targetUid}/followers/{currentUid}` & `users/{targetUid}.followersCount`
 * so follower counts update live across all devices.
 */
internal fun toggleFollowUser(
    currentUser: FirebaseUser,
    targetUid: String,
    targetUsername: String,
    targetDisplayName: String,
    targetPhotoUrl: String?,
    currentlyFollowing: Boolean,
    onSuccess: (isNowFollowing: Boolean) -> Unit = {},
    onError: (String) -> Unit = {},
) {
    if (targetUid.isBlank() || targetUid == currentUser.uid) {
        onError("Akun ini belum dapat diikuti.")
        return
    }
    val db = FirebaseFirestore.getInstance()
    val currentUserRef = db.collection("users").document(currentUser.uid)
    val followingDocRef = currentUserRef.collection("following").document(targetUid)
    val targetUserRef = db.collection("users").document(targetUid)
    val targetFollowerDocRef = targetUserRef.collection("followers").document(currentUser.uid)

    val currentName = currentUser.displayName?.takeIf { it.isNotBlank() }
        ?: currentUser.email?.substringBefore("@")?.takeIf { it.isNotBlank() }
        ?: "Pengguna"

    val followPayload = buildMap<String, Any> {
        put("uid", targetUid)
        put("username", targetUsername.ifBlank { targetDisplayName })
        put("displayName", targetDisplayName.ifBlank { targetUsername })
        put("name", targetDisplayName.ifBlank { targetUsername })
        targetPhotoUrl?.takeIf { it.isNotBlank() }?.let { put("photoUrl", it) }
        put("createdAt", FieldValue.serverTimestamp())
    }

    val followerPayload = buildMap<String, Any> {
        put("uid", currentUser.uid)
        put("username", currentName)
        put("displayName", currentName)
        put("name", currentName)
        currentUser.photoUrl?.toString()?.takeIf { it.isNotBlank() }?.let { put("photoUrl", it) }
        put("createdAt", FieldValue.serverTimestamp())
    }

    db.runTransaction { transaction ->
        val followingSnap = transaction.get(followingDocRef)
        val currentUserSnap = transaction.get(currentUserRef)
        val targetUserSnap = transaction.get(targetUserRef)

        val actualFollowing = followingSnap.exists()
        val shouldFollow = if (currentlyFollowing == actualFollowing) !actualFollowing else !currentlyFollowing

        val currentFollowingCount = (currentUserSnap.getLong("followingCount") ?: 0L).coerceAtLeast(0L)
        val nextFollowingCount = if (shouldFollow) {
            currentFollowingCount + 1L
        } else {
            (currentFollowingCount - 1L).coerceAtLeast(0L)
        }

        val currentFollowersCount = (targetUserSnap.getLong("followersCount") ?: 0L).coerceAtLeast(0L)
        val nextFollowersCount = if (shouldFollow) {
            currentFollowersCount + 1L
        } else {
            (currentFollowersCount - 1L).coerceAtLeast(0L)
        }

        if (shouldFollow) {
            transaction.set(followingDocRef, followPayload, SetOptions.merge())
            transaction.set(targetFollowerDocRef, followerPayload, SetOptions.merge())
        } else {
            transaction.delete(followingDocRef)
            transaction.delete(targetFollowerDocRef)
        }

        if (currentUserSnap.exists()) {
            transaction.update(
                currentUserRef,
                mapOf(
                    "followingCount" to nextFollowingCount,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        } else {
            transaction.set(
                currentUserRef,
                mapOf(
                    "followingCount" to nextFollowingCount,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
                SetOptions.merge(),
            )
        }

        if (targetUserSnap.exists()) {
            transaction.update(
                targetUserRef,
                mapOf(
                    "followersCount" to nextFollowersCount,
                    "updatedAt" to FieldValue.serverTimestamp(),
                ),
            )
        }

        shouldFollow
    }
        .addOnSuccessListener { isNowFollowing ->
            onSuccess(isNowFollowing)
        }
        .addOnFailureListener { err ->
            val isPermissionDenied = err.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
            onError(
                if (isPermissionDenied) {
                    "Izin update pengikut ditolak oleh Firestore Rules. Publikasikan aturan Firestore terbaru lalu coba lagi."
                } else {
                    err.localizedMessage ?: "Gagal memperbarui status mengikuti."
                },
            )
        }
}

/** Validates and builds the Firestore update payload for editing a post caption. */
internal fun updatedPostCaptionFields(newCaption: String): Map<String, Any>? {
    val trimmed = newCaption.trim()
    if (trimmed.isEmpty()) return null
    return mapOf("caption" to trimmed)
}

/** Deletes the user's own post document from `posts/{documentId}` and syncs `postsCount`. */
internal fun deleteUserPostFromFirestore(
    user: FirebaseUser,
    documentId: String,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {},
) {
    val cleanId = documentId.trim()
    if (cleanId.isEmpty()) {
        onError("ID postingan tidak valid.")
        return
    }
    val db = FirebaseFirestore.getInstance()
    db.collection("posts").document(cleanId)
        .delete()
        .addOnSuccessListener {
            db.collection("posts")
                .whereEqualTo("authorId", user.uid)
                .get()
                .addOnSuccessListener { snap ->
                    db.collection("users").document(user.uid).set(
                        mapOf(
                            "postsCount" to snap.size(),
                            "updatedAt" to FieldValue.serverTimestamp(),
                        ),
                        SetOptions.merge(),
                    )
                }
            onSuccess()
        }
        .addOnFailureListener { err ->
            val isPermissionDenied = err.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
            onError(
                if (isPermissionDenied) {
                    "Hapus postingan ditolak oleh Firestore Rules. Pastikan aturan delete pada koleksi posts sudah dipublikasikan."
                } else {
                    err.localizedMessage ?: "Gagal menghapus postingan."
                },
            )
        }
}

/** Updates the caption of the user's own post in `posts/{documentId}`. */
internal fun updateUserPostCaptionInFirestore(
    documentId: String,
    newCaption: String,
    onSuccess: () -> Unit = {},
    onError: (String) -> Unit = {},
) {
    val cleanId = documentId.trim()
    val payload = updatedPostCaptionFields(newCaption)
    if (cleanId.isEmpty() || payload == null) {
        onError("Caption postingan tidak boleh kosong.")
        return
    }
    FirebaseFirestore.getInstance()
        .collection("posts")
        .document(cleanId)
        .update(payload)
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { err ->
            val isPermissionDenied = err.message?.contains("PERMISSION_DENIED", ignoreCase = true) == true
            onError(
                if (isPermissionDenied) {
                    "Edit postingan ditolak oleh Firestore Rules. Pastikan aturan update pada koleksi posts sudah dipublikasikan."
                } else {
                    err.localizedMessage ?: "Gagal memperbarui caption postingan."
                },
            )
        }
}
