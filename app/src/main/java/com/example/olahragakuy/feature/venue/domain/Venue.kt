package com.example.olahragakuy.feature.venue.domain

enum class SportType(val label: String) {
    BADMINTON("Badminton"),
    FUTSAL("Futsal"),
    GYM("Gym"),
    PADEL("Padel")
}

data class Venue(
    val id: String,
    val name: String,
    val sportTypes: List<SportType>,
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val description: String,
    val facilities: List<String> = emptyList(),
    val photoUrl: String? = null,
    val isActive: Boolean = true
)