package com.example.olahragakuy.pages.venuefinder

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.CurrentLocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource

/**
 * Mengecek apakah aplikasi telah memiliki izin lokasi presisi (`FINE`) atau perkiraan (`COARSE`).
 */
fun hasLocationPermission(context: Context): Boolean {
    val fineGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    val coarseGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    return fineGranted || coarseGranted
}

/**
 * Mengambil koordinat GPS asli perangkat melalui [LocationServices.getFusedLocationProviderClient].
 *
 * - Jika izin lokasi belum diberikan atau sinyal indoor belum ter-lock, otomatis menggunakan
 *   fallback [DefaultUsuCoordinate] (`isFallback = true`).
 * - Mengembalikan fungsi pembatalan untuk dibersihkan saat composable keluar dari layar.
 */
@SuppressLint("MissingPermission")
fun fetchCurrentUserLocation(
    context: Context,
    onLocationResolved: (coordinate: GeoCoordinate, isFallback: Boolean) -> Unit,
): () -> Unit {
    if (!hasLocationPermission(context)) {
        onLocationResolved(DefaultUsuCoordinate, true)
        return {}
    }

    val cancellationSource = CancellationTokenSource()
    var disposed = false
    var hasValidGpsFix = false

    fun publishIfActive(raw: GeoCoordinate?) {
        if (disposed) return
        val (resolved, isFallback) = resolveUserCoordinate(raw)
        if (!isFallback) {
            hasValidGpsFix = true
            onLocationResolved(resolved, false)
        } else if (!hasValidGpsFix) {
            onLocationResolved(resolved, true)
        }
    }

    try {
        val client = LocationServices.getFusedLocationProviderClient(context)

        // 1. Coba lastLocation dulu agar pin cepat tampil bila GPS sudah pernah lock.
        client.lastLocation
            .addOnSuccessListener { location ->
                if (location != null) {
                    publishIfActive(GeoCoordinate(location.latitude, location.longitude))
                }
            }

        // 2. Minta lokasi terkini dengan timeout singkat agar indoor tetap cepat fallback.
        val fineGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED
        val request = CurrentLocationRequest.Builder()
            .setPriority(
                if (fineGranted) Priority.PRIORITY_HIGH_ACCURACY
                else Priority.PRIORITY_BALANCED_POWER_ACCURACY,
            )
            .setDurationMillis(5_000L)
            .setMaxUpdateAgeMillis(30_000L)
            .build()

        client.getCurrentLocation(request, cancellationSource.token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    publishIfActive(GeoCoordinate(location.latitude, location.longitude))
                } else {
                    publishIfActive(null)
                }
            }
            .addOnFailureListener {
                publishIfActive(null)
            }
    } catch (_: SecurityException) {
        publishIfActive(null)
    } catch (_: Exception) {
        publishIfActive(null)
    }

    return {
        disposed = true
        cancellationSource.cancel()
    }
}
