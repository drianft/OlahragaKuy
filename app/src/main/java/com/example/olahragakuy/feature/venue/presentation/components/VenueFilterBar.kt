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
import com.example.olahragakuy.feature.venue.domain.SportType

@Composable
fun VenueFilterBar(
    selectedSport: SportType?,
    resultCount: Int,
    onSportSelected: (SportType?) -> Unit,
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
                text = "Cari tempat olahraga",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedSport == null,
                    onClick = { onSportSelected(null) },
                    label = { Text("Semua") }
                )

                SportType.entries.forEach { sport ->
                    FilterChip(
                        selected = selectedSport == sport,
                        onClick = { onSportSelected(sport) },
                        label = { Text(sport.label) }
                    )
                }
            }

            Text(
                text = if (resultCount == 0) {
                    "Tidak ada venue yang cocok dengan filter."
                } else {
                    "$resultCount venue ditemukan"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}