package com.example.olahragakuy.core.location

import android.location.Location

object DistanceCalculator {

    fun metersBetween(
        startLatitude: Double,
        startLongitude: Double,
        endLatitude: Double,
        endLongitude: Double
    ): Float {
        val results = FloatArray(1)

        Location.distanceBetween(
            startLatitude,
            startLongitude,
            endLatitude,
            endLongitude,
            results
        )

        return results[0]
    }
}