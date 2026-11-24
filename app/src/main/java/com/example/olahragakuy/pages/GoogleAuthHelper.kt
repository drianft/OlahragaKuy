package com.example.olahragakuy.pages

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.NoCredentialException
import com.example.olahragakuy.pages.profile.user.ensureUserProfile
import com.google.android.gms.tasks.Task
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.GoogleAuthProvider
import java.security.MessageDigest
import java.security.SecureRandom
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

private const val TAG = "GoogleAuthHelper"

internal sealed interface GoogleSignInResult {
    data class Success(val role: String, val isNewUser: Boolean) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
    data class Error(val message: String) : GoogleSignInResult
}

internal fun generateHashedNonce(random: SecureRandom = SecureRandom()): String {
    val rawBytes = ByteArray(32)
    random.nextBytes(rawBytes)
    val digest = MessageDigest.getInstance("SHA-256").digest(rawBytes)
    return digest.joinToString(separator = "") { byte -> "%02x".format(byte) }
}

@SuppressLint("DiscouragedApi")
internal fun resolveGoogleWebClientId(context: Context): String? {
    val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
    if (resId == 0) return null
    return context.getString(resId).trim().takeIf { it.isNotEmpty() }
}

internal fun mapGoogleAuthError(exception: Throwable?): String = when (exception) {
    is NoCredentialException ->
        "Tidak ada akun Google yang tersedia di perangkat ini. Masuk ke akun Google di Pengaturan perangkat terlebih dahulu."
    is GetCredentialInterruptedException ->
        "Proses masuk dengan Google terganggu. Silakan coba lagi."
    is GetCredentialException ->
        "Gagal membuka pilihan akun Google. Pastikan Google Play Services aktif dan koneksi internet lancar."
    is GoogleIdTokenParsingException ->
        "Token akun Google tidak valid. Silakan coba lagi."
    is FirebaseAuthInvalidUserException ->
        if (exception.errorCode == "ERROR_USER_DISABLED") {
            "Akun dengan email ini telah dinonaktifkan."
        } else {
            "Gagal masuk dengan Google. Silakan coba lagi."
        }
    is FirebaseAuthUserCollisionException ->
        "Email ini sudah terdaftar dengan metode masuk lain."
    is FirebaseNetworkException ->
        "Koneksi internet bermasalah. Periksa jaringan Anda."
    is FirebaseTooManyRequestsException ->
        "Terlalu banyak percobaan masuk. Coba lagi nanti."
    else ->
        "Gagal masuk dengan Google. Silakan coba lagi."
}

internal suspend fun signInWithGoogle(context: Context): GoogleSignInResult {
    val webClientId = resolveGoogleWebClientId(context)
        ?: return GoogleSignInResult.Error(
            "Konfigurasi Google Sign-In belum lengkap. Pastikan google-services.json sudah diperbarui.",
        )

    val credentialContext = context.findActivity() ?: context
    val credentialManager = CredentialManager.create(credentialContext)
    val googleOption = GetSignInWithGoogleOption.Builder(serverClientId = webClientId)
        .setNonce(generateHashedNonce())
        .build()
    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleOption)
        .build()

    val idToken = try {
        val response = credentialManager.getCredential(
            context = credentialContext,
            request = request,
        )
        val credential = response.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            GoogleIdTokenCredential.createFrom(credential.data).idToken
        } else {
            return GoogleSignInResult.Error("Jenis kredensial Google tidak dikenali. Silakan coba lagi.")
        }
    } catch (_: GetCredentialCancellationException) {
        return GoogleSignInResult.Cancelled
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Google Credential Manager failed", e)
        return GoogleSignInResult.Error(mapGoogleAuthError(e))
    }

    val auth = FirebaseAuth.getInstance()
    val authResult = try {
        val firebaseCredential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(firebaseCredential).awaitTask()
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "Firebase Google signInWithCredential failed", e)
        return GoogleSignInResult.Error(mapGoogleAuthError(e))
    }

    val user = authResult.user
        ?: return GoogleSignInResult.Error("Gagal masuk dengan Google. Silakan coba lagi.")
    val isNewUser = authResult.additionalUserInfo?.isNewUser == true

    return try {
        val role = ensureUserProfile(user).awaitTask()
        if (auth.currentUser?.uid != user.uid) {
            GoogleSignInResult.Cancelled
        } else {
            GoogleSignInResult.Success(role = role, isNewUser = isNewUser)
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        Log.w(TAG, "ensureUserProfile failed after Google sign-in", e)
        if (auth.currentUser?.uid == user.uid) {
            auth.signOut()
        }
        GoogleSignInResult.Error(
            "Akun Google berhasil diautentikasi, tetapi profil Firestore belum dapat disiapkan. Periksa koneksi dan izin database, lalu coba masuk lagi.",
        )
    }
}

internal suspend fun clearGoogleCredentialState(context: Context) {
    runCatching {
        CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
    }.onFailure { e ->
        Log.w(TAG, "Failed to clear Credential Manager state on logout", e)
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

private suspend fun <T> Task<T>.awaitTask(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { exception ->
        if (cont.isActive) cont.resumeWithException(exception)
    }
    addOnCanceledListener {
        cont.cancel()
    }
}
