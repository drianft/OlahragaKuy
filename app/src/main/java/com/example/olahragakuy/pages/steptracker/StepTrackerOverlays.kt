package com.example.olahragakuy.pages.steptracker

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.DirectionsWalk
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.ui.theme.HomeBlue
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.KuyError
import com.example.olahragakuy.ui.theme.KuyNavy
import com.example.olahragakuy.ui.theme.KuyOrange
import com.example.olahragakuy.ui.theme.KuyText
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins

private val StepCardBg = Color(0xFF16324F)
private val StepCardDarkBg = Color(0xFF0E2238)
private val StepMutedText = Color(0xFFA4B8CF)
private val StepStartDotGreen = Color(0xFF43D166)
private val CheckerLight = Color(0xFF2B3D52)
private val CheckerDark = Color(0xFF1D2C3E)

private val DailyTargetPresets = listOf(4_000, 6_000, 8_000, 10_000)

/**
 * Dialog konfirmasi saat pengguna menekan tombol Kembali atau Back sistem ketika sesi masih aktif/dijeda.
 */
@Composable
fun StepExitConfirmationDialog(
    isForegroundServiceRunning: Boolean,
    onDismiss: () -> Unit,
    onContinueInBackground: () -> Unit,
    onStopAndSave: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = StepCardBg,
        titleContentColor = HomeText,
        textContentColor = HomeText.copy(alpha = 0.88f),
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = if (isForegroundServiceRunning) {
                    "Sesi Sedang Berjalan"
                } else {
                    "Hentikan Sesi Langkah?"
                },
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = if (isForegroundServiceRunning) {
                        "Pelacakan langkah masih aktif di latar belakang. Kamu bisa keluar layar tanpa menghentikan sesi, atau hentikan dan simpan progres sekarang."
                    } else {
                        "Pelacakan berjalan di layar tanpa izin lokasi. Keluar sekarang akan menghentikan sesi. Hentikan dan simpan progres kamu?"
                    },
                    fontFamily = Poppins,
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = HomeText.copy(alpha = 0.85f),
                )

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (isForegroundServiceRunning) {
                        Button(
                            onClick = onContinueInBackground,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KuyButton,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = "Lanjutkan di Latar Belakang",
                                fontFamily = Poppins,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                        }

                        OutlinedButton(
                            onClick = onStopAndSave,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.5.dp, KuyOrange),
                            colors = ButtonDefaults.outlinedButtonColors(
                                containerColor = Color.Transparent,
                                contentColor = KuyOrange,
                            ),
                        ) {
                            Text(
                                text = "Hentikan & Simpan",
                                fontFamily = Poppins,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                        }
                    } else {
                        Button(
                            onClick = onStopAndSave,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp),
                            shape = RoundedCornerShape(24.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KuyButton,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(
                                text = "Hentikan & Simpan",
                                fontFamily = Poppins,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                            )
                        }
                    }

                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = "Batal",
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = StepMutedText,
                        )
                    }
                }
            }
        },
        confirmButton = {},
    )
}

/**
 * Sheet Ringkasan Sesi Selesai & pratinjau kartu share ala Strava (tanpa peta dasar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepSessionSummarySheet(
    record: StepSessionRecord,
    selectedStyle: ShareCardStyle,
    onStyleSelected: (ShareCardStyle) -> Unit,
    onShareClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HomeNavy,
        contentColor = HomeText,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = HomeBlue) },
    ) {
        StepSessionSummaryContent(
            record = record,
            selectedStyle = selectedStyle,
            onStyleSelected = onStyleSelected,
            onShareClick = onShareClick,
            onDismiss = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
        )
    }
}

@Composable
internal fun StepSessionSummaryContent(
    record: StepSessionRecord,
    selectedStyle: ShareCardStyle,
    onStyleSelected: (ShareCardStyle) -> Unit,
    onShareClick: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Ringkasan Aktivitas",
                    color = HomeText,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                )
                Text(
                    text = formatSessionDateIndonesian(record.startedAtMillis),
                    color = StepMutedText,
                    fontFamily = Poppins,
                    fontSize = 12.sp,
                )
            }
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = StepCardBg,
                border = BorderStroke(1.dp, HomeBlue),
            ) {
                Text(
                    text = if (record.stepSource == StepSourceMode.GPS_FALLBACK) {
                        "Estimasi GPS"
                    } else {
                        "Sensor Langkah"
                    },
                    color = HomeOrange,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 10.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                )
            }
        }

        // Pemilih gaya kartu share: Dark Navy vs Stiker Transparan.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            ShareStyleOptionChip(
                label = "Kartu Gelap",
                selected = selectedStyle == ShareCardStyle.DARK_NAVY,
                onClick = { onStyleSelected(ShareCardStyle.DARK_NAVY) },
                modifier = Modifier.weight(1f),
            )
            ShareStyleOptionChip(
                label = "Stiker Transparan",
                selected = selectedStyle == ShareCardStyle.TRANSPARENT_STICKER,
                onClick = { onStyleSelected(ShareCardStyle.TRANSPARENT_STICKER) },
                modifier = Modifier.weight(1f),
            )
        }

        // Pratinjau visual jalur rute oranye polos (tanpa peta dasar).
        StepSharePreviewCard(
            record = record,
            style = selectedStyle,
            modifier = Modifier.fillMaxWidth(),
        )

        if (selectedStyle == ShareCardStyle.TRANSPARENT_STICKER) {
            Text(
                text = "Latar kotak-kotak hanya penanda area transparan — PNG hasil ekspor bebas latar dan siap ditempel di IG Story.",
                color = StepMutedText,
                fontFamily = Poppins,
                fontSize = 11.sp,
                lineHeight = 16.sp,
            )
        }

        // Detail lengkap metrik sesi.
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            color = StepCardBg,
            border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.7f)),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SummaryMetricTile(
                        label = "Langkah",
                        value = formatStepCount(record.steps),
                        accent = true,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMetricTile(
                        label = "Jarak",
                        value = formatDistance(record.distanceMeters),
                        accent = false,
                        modifier = Modifier.weight(1f),
                    )
                }

                HorizontalDivider(color = HomeBlue.copy(alpha = 0.5f))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    SummaryMetricTile(
                        label = "Waktu",
                        value = formatDuration(record.durationSeconds),
                        accent = false,
                        compact = true,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMetricTile(
                        label = "Pace",
                        value = "${formatPace(record.paceSecPerKm)}/km",
                        accent = false,
                        compact = true,
                        modifier = Modifier.weight(1f),
                    )
                    SummaryMetricTile(
                        label = "Kalori",
                        value = "${record.caloriesKcal} kkal",
                        accent = false,
                        compact = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Tombol Bagikan Aktivitas & Selesai.
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Button(
                onClick = onShareClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(28.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KuyButton,
                    contentColor = Color.White,
                ),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Bagikan Aktivitas",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
            }

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.2.dp, HomeBlue),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = HomeText,
                ),
            ) {
                Text(
                    text = "Selesai",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun ShareStyleOptionChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        shape = RoundedCornerShape(20.dp),
        color = if (selected) KuyOrange.copy(alpha = 0.18f) else StepCardDarkBg,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) KuyOrange else HomeBlue,
        ),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
            Text(
                text = label,
                color = if (selected) KuyOrange else HomeText.copy(alpha = 0.85f),
                fontFamily = Poppins,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 12.sp,
            )
        }
    }
}

@Composable
private fun StepSharePreviewCard(
    record: StepSessionRecord,
    style: ShareCardStyle,
    modifier: Modifier = Modifier,
) {
    val isTransparent = style == ShareCardStyle.TRANSPARENT_STICKER
    val normalizedPoints = remember(record.routePoints) {
        normalizeRouteToCanvas(record.routePoints, paddingFraction = 0.14f)
    }

    Box(
        modifier = modifier
            .height(214.dp)
            .clip(RoundedCornerShape(20.dp))
            .border(
                width = 1.dp,
                color = if (isTransparent) KuyOrange.copy(alpha = 0.55f) else HomeBlue,
                shape = RoundedCornerShape(20.dp),
            ),
    ) {
        if (isTransparent) {
            CheckerboardBackground(modifier = Modifier.fillMaxSize())
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(HomeNavy, Color(0xFF122B48), KuyNavy),
                        ),
                    ),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Olahraga",
                        color = Color.White,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                    Text(
                        text = "KUY",
                        color = KuyOrange,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
                Text(
                    text = "Tanpa Peta Dasar",
                    color = if (isTransparent) Color.White.copy(alpha = 0.9f) else StepMutedText,
                    fontFamily = Poppins,
                    fontSize = 10.sp,
                )
            }

            // Area Canvas polyline rute oranye polos / ilustrasi jejak langkah.
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(vertical = 6.dp),
            ) {
                if (normalizedPoints.size >= 2) {
                    val path = Path()
                    val mapped = normalizedPoints.map { pt ->
                        Offset(pt.x * size.width, pt.y * size.height)
                    }
                    mapped.forEachIndexed { index, offset ->
                        if (index == 0) path.moveTo(offset.x, offset.y)
                        else path.lineTo(offset.x, offset.y)
                    }

                    drawPath(
                        path = path,
                        color = if (isTransparent) KuyNavy.copy(alpha = 0.7f) else KuyOrange.copy(alpha = 0.25f),
                        style = Stroke(
                            width = 9.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                    drawPath(
                        path = path,
                        color = KuyOrange,
                        style = Stroke(
                            width = 4.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )

                    val start = mapped.first()
                    val end = mapped.last()
                    drawCircle(Color.White, radius = 5.5.dp.toPx(), center = start)
                    drawCircle(StepStartDotGreen, radius = 3.5.dp.toPx(), center = start)
                    drawCircle(Color.White, radius = 6.dp.toPx(), center = end)
                    drawCircle(KuyOrange, radius = 3.8.dp.toPx(), center = end)
                } else {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val arcPath = Path().apply {
                        moveTo(cx - 56.dp.toPx(), cy + 20.dp.toPx())
                        quadraticTo(cx - 6.dp.toPx(), cy - 34.dp.toPx(), cx + 56.dp.toPx(), cy - 12.dp.toPx())
                    }
                    drawPath(
                        path = arcPath,
                        color = KuyOrange.copy(alpha = 0.4f),
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                    )
                    // Jejak langkah kiri & kanan minimalis.
                    rotate(-14f, pivot = Offset(cx - 22.dp.toPx(), cy + 6.dp.toPx())) {
                        drawOval(
                            color = KuyOrange,
                            topLeft = Offset(cx - 32.dp.toPx(), cy - 14.dp.toPx()),
                            size = Size(20.dp.toPx(), 24.dp.toPx()),
                        )
                        drawOval(
                            color = KuyOrange,
                            topLeft = Offset(cx - 29.dp.toPx(), cy + 13.dp.toPx()),
                            size = Size(14.dp.toPx(), 15.dp.toPx()),
                        )
                    }
                    rotate(12f, pivot = Offset(cx + 22.dp.toPx(), cy - 12.dp.toPx())) {
                        drawOval(
                            color = KuyOrange,
                            topLeft = Offset(cx + 12.dp.toPx(), cy - 32.dp.toPx()),
                            size = Size(20.dp.toPx(), 24.dp.toPx()),
                        )
                        drawOval(
                            color = KuyOrange,
                            topLeft = Offset(cx + 15.dp.toPx(), cy - 5.dp.toPx()),
                            size = Size(14.dp.toPx(), 15.dp.toPx()),
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${formatDistance(record.distanceMeters)} • ${formatStepCount(record.steps)} langkah",
                    color = Color.White,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
                Text(
                    text = "${formatDuration(record.durationSeconds)} • ${formatPace(record.paceSecPerKm)}/km",
                    color = KuyOrange,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun CheckerboardBackground(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val tile = 12.dp.toPx()
        var y = 0f
        var row = 0
        while (y < size.height) {
            var x = 0f
            var col = 0
            while (x < size.width) {
                val color = if ((row + col) % 2 == 0) CheckerLight else CheckerDark
                drawRect(
                    color = color,
                    topLeft = Offset(x, y),
                    size = Size(tile, tile),
                )
                x += tile
                col++
            }
            y += tile
            row++
        }
    }
}

@Composable
private fun SummaryMetricTile(
    label: String,
    value: String,
    accent: Boolean,
    modifier: Modifier = Modifier,
    compact: Boolean = false,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = label,
            color = StepMutedText,
            fontFamily = Poppins,
            fontSize = 11.sp,
        )
        Text(
            text = value,
            color = if (accent) KuyOrange else HomeText,
            fontFamily = Poppins,
            fontWeight = FontWeight.Bold,
            fontSize = if (compact) 14.sp else 20.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Sheet Total Langkah Hari Ini (dengan edit target harian) & daftar Riwayat Sesi Lokal.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StepHistoryAndDailySheet(
    dailyProgress: DailyStepProgress,
    sessions: List<StepSessionRecord>,
    onUpdateDailyTarget: (Int) -> Unit,
    onSessionClick: (StepSessionRecord) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showTargetEditor by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteSessionId by rememberSaveable { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = HomeNavy,
        contentColor = HomeText,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = HomeBlue) },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Langkah Hari Ini & Riwayat",
                color = HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp,
            )

            // Kartu Progres Harian.
            DailyStepProgressCard(
                progress = dailyProgress,
                onEditTargetClick = { showTargetEditor = true },
            )

            // Header Riwayat.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Riwayat Sesi",
                    color = HomeText,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                )
                Text(
                    text = "${sessions.size} sesi tersimpan",
                    color = StepMutedText,
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                )
            }

            if (sessions.isEmpty()) {
                StepHistoryEmptyState()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 340.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 4.dp),
                ) {
                    items(sessions, key = { it.id }) { session ->
                        StepHistorySessionCard(
                            session = session,
                            onClick = { onSessionClick(session) },
                            onDeleteClick = { pendingDeleteSessionId = session.id },
                        )
                    }
                }
            }
        }
    }

    if (showTargetEditor) {
        EditDailyStepTargetDialog(
            currentTarget = dailyProgress.targetSteps,
            onDismiss = { showTargetEditor = false },
            onSaveTarget = { newTarget ->
                onUpdateDailyTarget(newTarget)
                showTargetEditor = false
            },
        )
    }

    pendingDeleteSessionId?.let { targetId ->
        AlertDialog(
            onDismissRequest = { pendingDeleteSessionId = null },
            containerColor = StepCardBg,
            titleContentColor = HomeText,
            textContentColor = HomeText.copy(alpha = 0.88f),
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Hapus Riwayat Sesi?",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                )
            },
            text = {
                Text(
                    text = "Riwayat sesi yang dihapus tidak dapat dikembalikan.",
                    fontFamily = Poppins,
                    fontSize = 13.sp,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSession(targetId)
                        pendingDeleteSessionId = null
                    },
                ) {
                    Text(
                        text = "Hapus",
                        color = KuyError,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteSessionId = null }) {
                    Text(
                        text = "Batal",
                        color = StepMutedText,
                        fontFamily = Poppins,
                    )
                }
            },
        )
    }
}

@Composable
private fun DailyStepProgressCard(
    progress: DailyStepProgress,
    onEditTargetClick: () -> Unit,
) {
    val fraction = calculateDailyStepFraction(progress)
    val percent = calculateDailyStepPercent(progress)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = StepCardBg,
        border = BorderStroke(1.dp, HomeBlue),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(KuyOrange.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                            contentDescription = null,
                            tint = KuyOrange,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Column {
                        Text(
                            text = "Target Harian",
                            color = StepMutedText,
                            fontFamily = Poppins,
                            fontSize = 11.sp,
                        )
                        Text(
                            text = "${formatStepCount(progress.targetSteps)} langkah",
                            color = HomeText,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                        )
                    }
                }

                Surface(
                    onClick = onEditTargetClick,
                    shape = RoundedCornerShape(16.dp),
                    color = StepCardDarkBg,
                    border = BorderStroke(1.dp, HomeBlue),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Edit,
                            contentDescription = "Ubah target harian",
                            tint = KuyOrange,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Ubah",
                            color = HomeText,
                            fontFamily = Poppins,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = formatStepCount(progress.totalSteps),
                        color = KuyOrange,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "/ ${formatStepCount(progress.targetSteps)}",
                        color = StepMutedText,
                        fontFamily = Poppins,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(bottom = 3.dp),
                    )
                }
                Text(
                    text = "$percent%",
                    color = HomeText,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
            }

            LinearProgressIndicator(
                progress = { fraction },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(8.dp)),
                color = KuyOrange,
                trackColor = StepCardDarkBg,
            )

            Text(
                text = "Otomatis di-reset setiap hari pukul 00:00",
                color = StepMutedText,
                fontFamily = Poppins,
                fontSize = 10.sp,
            )
        }
    }
}

@Composable
private fun StepHistorySessionCard(
    session: StepSessionRecord,
    onClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = StepCardBg,
        border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.65f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = formatSessionDateIndonesian(session.startedAtMillis),
                    color = StepMutedText,
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                )
                IconButton(
                    onClick = onDeleteClick,
                    modifier = Modifier.size(28.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Hapus riwayat sesi",
                        tint = StepMutedText,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${formatStepCount(session.steps)} langkah",
                    color = HomeText,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                )
                Text(
                    text = formatDistance(session.distanceMeters),
                    color = KuyOrange,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${formatDuration(session.durationSeconds)} • ${formatPace(session.paceSecPerKm)}/km • ${session.caloriesKcal} kkal",
                    color = StepMutedText,
                    fontFamily = Poppins,
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Bagikan",
                    color = HomeOrange,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                )
            }
        }
    }
}

@Composable
private fun StepHistoryEmptyState() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StepCardDarkBg,
        border = BorderStroke(1.dp, HomeBlue.copy(alpha = 0.5f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.DirectionsWalk,
                contentDescription = null,
                tint = KuyOrange.copy(alpha = 0.85f),
                modifier = Modifier.size(34.dp),
            )
            Text(
                text = "Belum ada riwayat sesi",
                color = HomeText,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Text(
                text = "Tekan Mulai Langkah untuk mencatat perjalanan pertamamu.",
                color = StepMutedText,
                fontFamily = Poppins,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EditDailyStepTargetDialog(
    currentTarget: Int,
    onDismiss: () -> Unit,
    onSaveTarget: (Int) -> Unit,
) {
    var inputText by rememberSaveable(currentTarget) { mutableStateOf(currentTarget.toString()) }
    val parsedValue = inputText.trim().toIntOrNull()
    val isValid = parsedValue != null && parsedValue in MIN_DAILY_STEP_TARGET..MAX_DAILY_STEP_TARGET

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.imePadding(),
        containerColor = StepCardBg,
        titleContentColor = HomeText,
        textContentColor = HomeText,
        shape = RoundedCornerShape(22.dp),
        title = {
            Text(
                text = "Ubah Target Langkah Harian",
                fontFamily = Poppins,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Pilih preset cepat atau masukkan target antara ${formatStepCount(MIN_DAILY_STEP_TARGET)} hingga ${formatStepCount(MAX_DAILY_STEP_TARGET)} langkah.",
                    color = StepMutedText,
                    fontFamily = Poppins,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    DailyTargetPresets.forEach { preset ->
                        val isSelected = parsedValue == preset
                        Surface(
                            onClick = { inputText = preset.toString() },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) KuyOrange.copy(alpha = 0.22f) else StepCardDarkBg,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isSelected) KuyOrange else HomeBlue,
                            ),
                        ) {
                            Text(
                                text = formatStepCount(preset),
                                color = if (isSelected) KuyOrange else HomeText,
                                fontFamily = Poppins,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = inputText,
                    onValueChange = { raw ->
                        val digitsOnly = raw.filter { it.isDigit() }.take(6)
                        inputText = digitsOnly
                    },
                    singleLine = true,
                    label = {
                        Text(
                            text = "Target langkah",
                            fontFamily = Poppins,
                            fontSize = 12.sp,
                        )
                    },
                    isError = inputText.isNotEmpty() && !isValid,
                    supportingText = if (inputText.isNotEmpty() && !isValid) {
                        {
                            Text(
                                text = "Masukkan angka ${formatStepCount(MIN_DAILY_STEP_TARGET)} – ${formatStepCount(MAX_DAILY_STEP_TARGET)}",
                                color = KuyError,
                                fontFamily = Poppins,
                                fontSize = 11.sp,
                            )
                        }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = HomeText,
                        unfocusedTextColor = HomeText,
                        focusedBorderColor = KuyOrange,
                        unfocusedBorderColor = HomeBlue,
                        focusedLabelColor = KuyOrange,
                        unfocusedLabelColor = StepMutedText,
                        cursorColor = KuyOrange,
                        errorBorderColor = KuyError,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { parsedValue?.let(onSaveTarget) },
                enabled = isValid,
                shape = RoundedCornerShape(20.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KuyButton,
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    text = "Simpan",
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Batal",
                    color = KuyText.copy(alpha = 0.75f),
                    fontFamily = Poppins,
                )
            }
        },
    )
}

@Preview(name = "Ringkasan Sesi (Kartu Gelap)", showBackground = true, backgroundColor = 0xFF0A1A2C)
@Composable
private fun StepSessionSummaryDarkPreview() {
    OlahragaKuyTheme {
        StepSessionSummaryContent(
            record = StepSessionRecord(
                id = "preview-1",
                startedAtMillis = 1_763_966_400_000L,
                endedAtMillis = 1_763_967_600_000L,
                durationSeconds = 1_215L,
                steps = 2_480,
                distanceMeters = 1_860.0,
                paceSecPerKm = 653.0,
                caloriesKcal = 112,
                routePoints = listOf(
                    StepPoint(3.5610, 98.6552, 1_000L),
                    StepPoint(3.5622, 98.6561, 3_000L),
                    StepPoint(3.5630, 98.6574, 6_000L),
                    StepPoint(3.5619, 98.6580, 9_000L),
                ),
                stepSource = StepSourceMode.PEDOMETER,
            ),
            selectedStyle = ShareCardStyle.DARK_NAVY,
            onStyleSelected = {},
            onShareClick = {},
            onDismiss = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(HomeNavy)
                .padding(20.dp),
        )
    }
}

@Preview(name = "Ringkasan Sesi (Stiker Transparan)", showBackground = true, backgroundColor = 0xFF0A1A2C)
@Composable
private fun StepSessionSummaryStickerPreview() {
    OlahragaKuyTheme {
        StepSessionSummaryContent(
            record = StepSessionRecord(
                id = "preview-2",
                startedAtMillis = 1_763_966_400_000L,
                endedAtMillis = 1_763_967_000_000L,
                durationSeconds = 600L,
                steps = 920,
                distanceMeters = 0.0,
                paceSecPerKm = null,
                caloriesKcal = 37,
                routePoints = emptyList(),
                stepSource = StepSourceMode.PEDOMETER,
            ),
            selectedStyle = ShareCardStyle.TRANSPARENT_STICKER,
            onStyleSelected = {},
            onShareClick = {},
            onDismiss = {},
            modifier = Modifier
                .fillMaxWidth()
                .background(HomeNavy)
                .padding(20.dp),
        )
    }
}
