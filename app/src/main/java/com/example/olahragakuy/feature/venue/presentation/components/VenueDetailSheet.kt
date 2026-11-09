package com.example.olahragakuy.feature.venue.presentation.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.olahragakuy.feature.venue.domain.Venue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VenueDetailSheet(
    venue: Venue,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    bottom = 24.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = venue.name,
                style = MaterialTheme.typography.headlineSmall
            )

            Text(
                text = venue.sportTypes.joinToString(" • ") {
                    it.label
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Alamat",
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = venue.address,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Tentang tempat",
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = venue.description,
                style = MaterialTheme.typography.bodyMedium
            )

            Text(
                text = "Fasilitas",
                style = MaterialTheme.typography.titleSmall
            )

            Text(
                text = venue.facilities
                    .takeIf { it.isNotEmpty() }
                    ?.joinToString(" • ")
                    ?: "Informasi fasilitas belum tersedia.",
                style = MaterialTheme.typography.bodyMedium
            )

            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Tutup")
            }
        }
    }
}