package com.example.olahragakuy.feature.venue.data

import com.example.olahragakuy.feature.venue.domain.SportType
import com.example.olahragakuy.feature.venue.domain.Venue
import com.example.olahragakuy.feature.venue.domain.VenueRepository

class FakeVenueRepository : VenueRepository {

    override suspend fun getVenues(): List<Venue> {
        // Data simulasi untuk pengembangan.
        // Koordinat bukan lokasi venue nyata yang telah diverifikasi.
        return listOf(
            Venue(
                id = "dummy_badminton_01",
                name = "GOR Demo Badminton",
                sportTypes = listOf(SportType.BADMINTON),
                latitude = 3.5650,
                longitude = 98.6540,
                address = "Alamat simulasi A, Medan",
                description = "Venue dummy untuk menguji fitur badminton.",
                facilities = listOf("Parkir", "Toilet")
            ),
            Venue(
                id = "dummy_futsal_01",
                name = "Lapangan Demo Futsal",
                sportTypes = listOf(SportType.FUTSAL),
                latitude = 3.5750,
                longitude = 98.6640,
                address = "Alamat simulasi B, Medan",
                description = "Venue dummy untuk menguji fitur futsal.",
                facilities = listOf("Parkir", "Ruang ganti")
            ),
            Venue(
                id = "dummy_gym_01",
                name = "Gym Demo",
                sportTypes = listOf(SportType.GYM),
                latitude = 3.6000,
                longitude = 98.6800,
                address = "Alamat simulasi C, Medan",
                description = "Venue dummy untuk menguji fitur gym.",
                facilities = listOf("Toilet", "Loker")
            )
        )
    }
}