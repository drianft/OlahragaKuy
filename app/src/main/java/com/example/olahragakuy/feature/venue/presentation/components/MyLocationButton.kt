package com.example.olahragakuy.feature.venue.presentation.components

import android.Manifest
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.example.olahragakuy.core.location.DeviceLocationReader
import kotlinx.coroutines.launch

@Composable
fun MyLocationButton(
    onLocationFound: (Location) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val latestOnLocationFound by rememberUpdatedState(onLocationFound)

    var isLoading by remember {
        mutableStateOf(false)
    }

    fun readLocation() {
        if (isLoading) return

        isLoading = true

        scope.launch {
            try {
                val location = DeviceLocationReader.read(context)

                if (location != null) {
                    latestOnLocationFound(location)
                } else {
                    Toast.makeText(
                        context,
                        "Lokasi belum tersedia. Pastikan GPS aktif, " +
                                "lalu coba lagi di tempat dengan sinyal lebih baik.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                isLoading = false
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted =
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ] == true

        if (granted) {
            readLocation()
        } else {
            Toast.makeText(
                context,
                "Izin lokasi belum diberikan. Peta tetap bisa digunakan. " +
                        "Jika dialog tidak muncul lagi, buka izin aplikasi " +
                        "melalui Setelan HP.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    Button(
        modifier = modifier,
        enabled = !isLoading,
        onClick = {
            if (DeviceLocationReader.hasPermission(context)) {
                readLocation()
            } else {
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        }
    ) {
        Text(
            text = if (isLoading) {
                "Mencari lokasi..."
            } else {
                "Lokasi saya"
            }
        )
    }
}