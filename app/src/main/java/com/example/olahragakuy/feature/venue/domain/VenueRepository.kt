package com.example.olahragakuy.feature.venue.domain

interface VenueRepository {
    suspend fun getVenues(): List<Venue>
}