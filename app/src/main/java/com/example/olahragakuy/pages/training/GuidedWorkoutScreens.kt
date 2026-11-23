package com.example.olahragakuy.pages.training

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.components.KuyBottomNavBar
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme

private val DayTileBg = Color(0xFF172F4B)
private val DayTileUnlockedBg = Color(0xFF1C3A5C)
private val DayTileBorder = Color(0xFF284C75)
private val ExerciseCardTopBg = Color(0xFF1D3C61)
private val ExerciseCardBottomBg = Color(0xFF0E2137)
private val GuideSheetBg = Color(0xFF204168)

@Composable
fun GuidedWorkoutDaysScreen(
    modifier: Modifier = Modifier,
    days: List<GuidedWorkoutDay> = WorkoutFirestoreStore.allDays(),
    isLoading: Boolean = WorkoutFirestoreStore.isLoading,
    errorMessage: String? = WorkoutFirestoreStore.errorMessage,
    isSeeding: Boolean = WorkoutFirestoreStore.isSeeding,
    seedMessage: String? = WorkoutFirestoreStore.seedMessage,
    highestUnlockedDay: Int = GuidedWorkoutProgressStore.highestUnlockedDay,
    completedDays: Set<Int> = GuidedWorkoutProgressStore.completedDays,
    onBack: () -> Unit = {},
    onTabSelected: (HomeNavTab) -> Unit = {},
    onDayClick: (Int) -> Unit = {},
    onSeedClick: () -> Unit = { WorkoutCatalogSeeder.seedToFirestore(onlyIfEmpty = false) },
) {
    var lockedNotice by rememberSaveable { mutableStateOf<String?>(null) }
    val columns = 7
    val dayRows = days.sortedBy { it.dayNumber }.chunked(columns)

    Scaffold(
        modifier = modifier,
        containerColor = HomeNavy,
        contentColor = HomeText,
        bottomBar = {
            KuyBottomNavBar(
                selectedTab = HomeNavTab.LATIHAN,
                onTabSelected = onTabSelected,
            )
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            KuyBackButton(onClick = onBack)

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "Latihan Terarah",
                color = HomeText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Selesaikan latihan sebelumnya untuk membuka latihan berikutnya",
                color = HomeText.copy(alpha = 0.72f),
                fontSize = 11.sp,
                lineHeight = 15.sp,
            )

            Spacer(modifier = Modifier.height(18.dp))

            when {
                isLoading && days.isEmpty() -> {
                    FirestoreStatusCard(
                        title = "Memuat dari Firestore...",
                        description = "Mengambil daftar hari dan panduan latihan langsung dari Cloud Firestore.",
                        showProgress = true,
                        actionLabel = null,
                        onActionClick = {},
                    )
                }

                days.isEmpty() -> {
                    FirestoreStatusCard(
                        title = if (errorMessage != null) {
                            "Gagal Memuat Data Firestore"
                        } else {
                            "Belum Ada Data di Firestore"
                        },
                        description = buildString {
                            if (errorMessage != null) {
                                append(errorMessage)
                            } else {
                                append("Koleksi `workout_exercises` dan `guided_workout_days` di Firestore masih kosong.")
                            }
                            if (!seedMessage.isNullOrBlank()) {
                                append("\n\n")
                                append(seedMessage)
                            }
                        },
                        showProgress = isSeeding,
                        actionLabel = if (isSeeding) "Mengunggah..." else "Seed Data Latihan ke Firestore",
                        actionEnabled = !isSeeding,
                        onActionClick = onSeedClick,
                    )
                }

                else -> {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        dayRows.forEach { rowDays ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                rowDays.forEach { day ->
                                    val status = resolveGuidedDayStatus(
                                        dayNumber = day.dayNumber,
                                        highestUnlockedDay = highestUnlockedDay,
                                        completedDays = completedDays,
                                    )
                                    GuidedDayTile(
                                        dayNumber = day.dayNumber,
                                        status = status,
                                        modifier = Modifier.weight(1f),
                                        onClick = {
                                            if (status == GuidedDayStatus.LOCKED) {
                                                lockedNotice =
                                                    "Selesaikan Hari $highestUnlockedDay terlebih dahulu untuk membuka Hari ${day.dayNumber}."
                                            } else {
                                                onDayClick(day.dayNumber)
                                            }
                                        },
                                    )
                                }
                                repeat(columns - rowDays.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    lockedNotice?.let { message ->
        AlertDialog(
            onDismissRequest = { lockedNotice = null },
            title = { Text("Latihan Terkunci") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { lockedNotice = null }) {
                    Text("Mengerti", color = HomeOrange)
                }
            },
        )
    }
}

@Composable
private fun FirestoreStatusCard(
    title: String,
    description: String,
    showProgress: Boolean,
    actionLabel: String?,
    onActionClick: () -> Unit,
    actionEnabled: Boolean = true,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = DayTileUnlockedBg,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 22.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (showProgress) {
                CircularProgressIndicator(
                    color = HomeOrange,
                    modifier = Modifier.size(32.dp),
                    strokeWidth = 3.dp,
                )
            }
            Text(
                text = title,
                color = HomeText,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )
            Text(
                text = description,
                color = HomeText.copy(alpha = 0.78f),
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center,
            )
            if (actionLabel != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = onActionClick,
                    enabled = actionEnabled,
                    shape = RoundedCornerShape(22.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KuyButton,
                        contentColor = HomeText,
                    ),
                ) {
                    Text(
                        text = actionLabel,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
private fun GuidedDayTile(
    dayNumber: Int,
    status: GuidedDayStatus,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(10.dp)
    val bgColor = when (status) {
        GuidedDayStatus.LOCKED -> DayTileBg
        GuidedDayStatus.UNLOCKED, GuidedDayStatus.COMPLETED -> DayTileUnlockedBg
    }
    val borderColor = when (status) {
        GuidedDayStatus.COMPLETED -> HomeOrange.copy(alpha = 0.7f)
        GuidedDayStatus.UNLOCKED -> DayTileBorder.copy(alpha = 0.65f)
        GuidedDayStatus.LOCKED -> DayTileBorder.copy(alpha = 0.25f)
    }

    Box(
        modifier = modifier
            .aspectRatio(0.92f)
            .clip(shape)
            .background(bgColor)
            .border(width = 1.dp, color = borderColor, shape = shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (status == GuidedDayStatus.LOCKED) {
            Icon(
                imageVector = Icons.Rounded.Lock,
                contentDescription = "Hari $dayNumber terkunci",
                tint = HomeText.copy(alpha = 0.85f),
                modifier = Modifier.size(18.dp),
            )
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Hari",
                    color = HomeText.copy(alpha = 0.72f),
                    fontSize = 9.sp,
                    lineHeight = 11.sp,
                )
                Text(
                    text = dayNumber.toString(),
                    color = HomeText,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    lineHeight = 20.sp,
                )
            }
            if (status == GuidedDayStatus.COMPLETED) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = "Selesai",
                    tint = HomeOrange,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(3.dp)
                        .size(11.dp),
                )
            }
        }
    }
}

@Composable
fun GuidedWorkoutDetailScreen(
    dayNumber: Int,
    modifier: Modifier = Modifier,
    dayPlan: GuidedWorkoutDay? = WorkoutFirestoreStore.dayPlanOrNull(dayNumber),
    exerciseLookup: (String) -> WorkoutExerciseDetail? = WorkoutFirestoreStore::exerciseOrNull,
    onBack: () -> Unit = {},
    onExerciseClick: (String) -> Unit = {},
    onStartWorkoutClick: (() -> Unit)? = null,
) {
    var startNotice by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = HomeNavy,
        contentColor = HomeText,
        bottomBar = {
            if (dayPlan != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(HomeNavy)
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                ) {
                    Button(
                        onClick = {
                            if (onStartWorkoutClick != null) {
                                onStartWorkoutClick()
                            } else {
                                startNotice = "Sesi latihan ${dayPlan.title} segera hadir."
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(26.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KuyButton,
                            contentColor = HomeText,
                        ),
                    ) {
                        Text(
                            text = "Mulai",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        },
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 14.dp),
        ) {
            KuyBackButton(onClick = onBack)

            Spacer(modifier = Modifier.height(22.dp))

            if (dayPlan == null) {
                FirestoreStatusCard(
                    title = "Data Hari $dayNumber Belum Tersedia",
                    description = "Jadwal latihan hari ini belum ditemukan di koleksi `guided_workout_days` Firestore.",
                    showProgress = WorkoutFirestoreStore.isLoading,
                    actionLabel = "Kembali",
                    onActionClick = onBack,
                )
                return@Column
            }

            Text(
                text = dayPlan.title,
                color = HomeText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            if (dayPlan.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = dayPlan.description,
                    color = HomeText.copy(alpha = 0.72f),
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Estimasi Latihan",
                color = HomeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(28.dp),
                verticalAlignment = Alignment.Top,
            ) {
                WorkoutMetricColumn(
                    value = dayPlan.calories.toString(),
                    label = "KKAL",
                )
                WorkoutMetricColumn(
                    value = dayPlan.durationLabel,
                    label = "Menit",
                )
                WorkoutMetricColumn(
                    value = "Kesulitan",
                    label = dayPlan.difficultyLabel,
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                dayPlan.exercises.forEach { step ->
                    val detail = exerciseLookup(step.exerciseId)
                    GuidedExerciseCard(
                        exerciseName = detail?.name ?: step.exerciseId,
                        targetLabel = step.targetLabel,
                        exerciseType = detail?.type ?: step.exerciseType,
                        onClick = { onExerciseClick(step.exerciseId) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }

    startNotice?.let { message ->
        AlertDialog(
            onDismissRequest = { startNotice = null },
            title = { Text("Informasi") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { startNotice = null }) {
                    Text("Mengerti", color = HomeOrange)
                }
            },
        )
    }
}

@Composable
private fun WorkoutMetricColumn(
    value: String,
    label: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            color = HomeOrange,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            color = HomeText.copy(alpha = 0.78f),
            fontSize = 10.sp,
        )
    }
}

@Composable
private fun GuidedExerciseCard(
    exerciseName: String,
    targetLabel: String,
    exerciseType: ExerciseType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(ExerciseCardTopBg, ExerciseCardBottomBg),
                ),
            )
            .border(
                width = 1.dp,
                color = DayTileBorder.copy(alpha = 0.35f),
                shape = shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkoutExerciseIllustration(
            type = exerciseType,
            modifier = Modifier.size(width = 62.dp, height = 46.dp),
        )

        Spacer(modifier = Modifier.width(14.dp))

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = exerciseName,
                color = HomeText,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = targetLabel,
                color = HomeText.copy(alpha = 0.74f),
                fontSize = 11.sp,
            )
        }

        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = "Lihat panduan $exerciseName",
            tint = HomeText.copy(alpha = 0.9f),
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
fun ExerciseDetailScreen(
    exerciseKey: String,
    modifier: Modifier = Modifier,
    detail: WorkoutExerciseDetail? = WorkoutFirestoreStore.exerciseOrNull(exerciseKey),
    onBack: () -> Unit = {},
) {
    Scaffold(
        modifier = modifier,
        containerColor = HomeNavy,
        contentColor = HomeText,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = insets.calculateTopPadding()),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                KuyBackButton(onClick = onBack)

                Spacer(modifier = Modifier.height(22.dp))

                if (detail == null) {
                    FirestoreStatusCard(
                        title = "Panduan Gerakan Belum Tersedia",
                        description = "Dokumen `$exerciseKey` tidak ditemukan di koleksi `workout_exercises` Firestore.",
                        showProgress = WorkoutFirestoreStore.isLoading,
                        actionLabel = "Kembali",
                        onActionClick = onBack,
                    )
                    return@Column
                }

                Text(
                    text = detail.name,
                    color = HomeText,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(modifier = Modifier.height(16.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    WorkoutExerciseIllustration(
                        type = detail.type,
                        modifier = Modifier.size(width = 230.dp, height = 205.dp),
                    )
                }
            }

            if (detail == null) return@Column

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                color = GuideSheetBg,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Text(
                        text = "Panduan Gerakkan",
                        color = HomeOrange,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                    )

                    if (detail.description.isNotBlank()) {
                        Text(
                            text = detail.description,
                            color = HomeText.copy(alpha = 0.85f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                        )
                    }

                    detail.steps.forEachIndexed { index, stepText ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = "${index + 1}.",
                                color = HomeText,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                            )
                            Text(
                                text = stepText,
                                color = HomeText,
                                fontSize = 13.sp,
                                lineHeight = 20.sp,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }
    }
}

private val PreviewExercisesMap by lazy {
    WorkoutCatalogSeeder.buildSeedExercises().associateBy { it.id.uppercase() }
}

@Preview(name = "Latihan Terarah - Hari", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun GuidedWorkoutDaysScreenPreview() {
    OlahragaKuyTheme {
        GuidedWorkoutDaysScreen(
            days = WorkoutCatalogSeeder.buildSeedGuidedDays(),
            isLoading = false,
            errorMessage = null,
            isSeeding = false,
            seedMessage = null,
        )
    }
}

@Preview(name = "Detail Latihan Terarah", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun GuidedWorkoutDetailScreenPreview() {
    OlahragaKuyTheme {
        GuidedWorkoutDetailScreen(
            dayNumber = 1,
            dayPlan = WorkoutCatalogSeeder.buildSeedGuidedDays().first(),
            exerciseLookup = { PreviewExercisesMap[it.uppercase()] },
        )
    }
}

@Preview(name = "Detail Gerakkan - Sit Up", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun ExerciseDetailScreenPreview() {
    OlahragaKuyTheme {
        ExerciseDetailScreen(
            exerciseKey = ExerciseType.SIT_UP.name,
            detail = PreviewExercisesMap[ExerciseType.SIT_UP.name],
        )
    }
}
