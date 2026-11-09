package com.example.olahragakuy.feature.venue.presentation.components

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun VenueRadiusFilter(
    selectedRadiusKm: Int?,
    hasLocation: Boolean,
    onRadiusSelected: (Int?) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        tonalElevation = 3.dp,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "Radius pencarian",
                style = MaterialTheme.typography.titleSmall
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedRadiusKm == null,
                    onClick = { onRadiusSelected(null) },
                    label = { Text("Semua jarak") }
                )

                listOf(1, 3, 5).forEach { radius ->
                    FilterChip(
                        selected = selectedRadiusKm == radius,
                        enabled = hasLocation,
                        onClick = { onRadiusSelected(radius) },
                        label = { Text("$radius km") }
                    )
                }
            }

            Text(
                text = if (hasLocation) {
                    "Dari lokasi terakhir yang dibaca • jarak garis lurus"
                } else {
                    "Tekan “Lokasi saya” untuk mengaktifkan radius."
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}