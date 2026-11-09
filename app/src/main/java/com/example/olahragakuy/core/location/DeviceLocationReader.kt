package com.example.olahragakuy.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

object DeviceLocationReader {

    fun hasPermission(context: Context): Boolean {
        return hasFinePermission(context) ||
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasFinePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun read(context: Context): Location? {
        if (!hasPermission(context)) return null

        val manager = context.getSystemService(
            Context.LOCATION_SERVICE
        ) as LocationManager

        if (!LocationManagerCompat.isLocationEnabled(manager)) {
            return null
        }

        val providers = buildList {
            if (
                hasFinePermission(context) &&
                manager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            ) {
                add(LocationManager.GPS_PROVIDER)
            }

            if (
                manager.isProviderEnabled(
                    LocationManager.NETWORK_PROVIDER
                )
            ) {
                add(LocationManager.NETWORK_PROVIDER)
            }
        }

        for (provider in providers) {
            val location = withTimeoutOrNull(12_000L) {
                suspendCancellableCoroutine<Location?> { continuation ->
                    val cancellationSignal = CancellationSignal()

                    continuation.invokeOnCancellation {
                        cancellationSignal.cancel()
                    }

                    try {
                        LocationManagerCompat.getCurrentLocation(
                            manager,
                            provider,
                            cancellationSignal,
                            ContextCompat.getMainExecutor(context)
                        ) { result ->
                            if (continuation.isActive) {
                                continuation.resume(result)
                            }
                        }
                    } catch (_: SecurityException) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    } catch (_: IllegalArgumentException) {
                        if (continuation.isActive) {
                            continuation.resume(null)
                        }
                    }
                }
            }

            if (location != null) return location
        }

        return null
    }
}