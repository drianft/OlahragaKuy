package com.example.olahragakuy.pages.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.R
import com.example.olahragakuy.ui.components.HomeNavTab
import com.example.olahragakuy.ui.components.KuyBottomNavBar
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeOrange
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme

data class StreakDayUiModel(
    val label: String,
    val completed: Boolean = true,
)

enum class TrainingRoutineType {
    HOME_WORKOUT,
    STEP_TRACKER,
    WATER_REMINDER,
}

data class TrainingRoutineUiModel(
    val type: TrainingRoutineType,
    val title: String,
    val description: String,
    val gradientColors: List<Color>,
)

private val StreakCardBg = Color(0xFF16324F)
private val StreakPillBg = Color(0xFF0C2138)
private val StreakCheckOrange = Color(0xFFE0620E)
private val TopBarDivider = Color(0xFF162B42)

private val DefaultStreakDays = listOf(
    StreakDayUiModel("Sen", true),
    StreakDayUiModel("Sel", true),
    StreakDayUiModel("Rab", true),
    StreakDayUiModel("Kam", true),
    StreakDayUiModel("Jum", true),
    StreakDayUiModel("Sab", true),
    StreakDayUiModel("Min", true),
)

private val DefaultTrainingRoutines = listOf(
    TrainingRoutineUiModel(
        type = TrainingRoutineType.HOME_WORKOUT,
        title = "Workout Rumahan",
        description = "Jaga badan anda tetap bugar dengan\nlatihan tubuh",
        gradientColors = listOf(Color(0xFFEF6B4B), Color(0xFFE53E1A)),
    ),
    TrainingRoutineUiModel(
        type = TrainingRoutineType.STEP_TRACKER,
        title = "Pelacak Langkah",
        description = "Hitung setiap langkah yang anda\njalankan",
        gradientColors = listOf(Color(0xFF28B44B), Color(0xFF43D166)),
    ),
    TrainingRoutineUiModel(
        type = TrainingRoutineType.WATER_REMINDER,
        title = "Pengingat Minum",
        description = "Jaga diri anda selalu terhidrasi",
        gradientColors = listOf(Color(0xFF4C80C8), Color(0xFF8AB5EE)),
    ),
)

@Composable
fun TrainingHomeScreen(
    modifier: Modifier = Modifier,
    streakDaysCount: Int = 30,
    streakDays: List<StreakDayUiModel> = DefaultStreakDays,
    routines: List<TrainingRoutineUiModel> = DefaultTrainingRoutines,
    onTabSelected: (HomeNavTab) -> Unit = {},
    onRoutineClick: (TrainingRoutineType) -> Unit = {},
    onHomeWorkoutClick: (() -> Unit)? = null,
    onStepTrackerClick: (() -> Unit)? = null,
    onWaterReminderClick: (() -> Unit)? = null,
) {
    var notice by rememberSaveable { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier,
        containerColor = HomeNavy,
        contentColor = HomeText,
        topBar = {
            TrainingTopBar()
        },
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            TrainingStreakCard(
                streakDaysCount = streakDaysCount,
                streakDays = streakDays,
            )

            Text(
                text = "Rutinitas Olahraga",
                color = HomeText,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 2.dp),
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                routines.forEach { routine ->
                    TrainingRoutineCard(
                        routine = routine,
                        onClick = {
                            onRoutineClick(routine.type)
                            when {
                                routine.type == TrainingRoutineType.HOME_WORKOUT && onHomeWorkoutClick != null -> {
                                    onHomeWorkoutClick()
                                }
                                routine.type == TrainingRoutineType.STEP_TRACKER && onStepTrackerClick != null -> {
                                    onStepTrackerClick()
                                }
                                routine.type == TrainingRoutineType.WATER_REMINDER && onWaterReminderClick != null -> {
                                    onWaterReminderClick()
                                }
                                else -> {
                                    notice = "Fitur ${routine.title} segera hadir."
                                }
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }

    notice?.let { message ->
        AlertDialog(
            onDismissRequest = { notice = null },
            title = { Text("Informasi") },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = { notice = null }) {
                    Text("Mengerti", color = HomeOrange)
                }
            },
        )
    }
}

@Composable
private fun TrainingTopBar(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(HomeNavy)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(R.drawable.login_wordmark),
                contentDescription = "OlahragaKUY",
                modifier = Modifier.height(22.dp),
                contentScale = ContentScale.FillHeight,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(TopBarDivider),
        )
    }
}

@Composable
private fun TrainingStreakCard(
    streakDaysCount: Int,
    streakDays: List<StreakDayUiModel>,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = StreakCardBg,
        shadowElevation = 4.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_nav_workout),
                contentDescription = null,
                tint = Color(0xFFB3CCE6),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(28.dp),
            )

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(modifier = Modifier.height(4.dp))
                StreakFlameIllustration(modifier = Modifier.size(72.dp))
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "$streakDaysCount Hari Beruntun",
                    color = HomeText,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Pertahankan Streak Olahraga anda!",
                    color = HomeText.copy(alpha = 0.8f),
                    fontSize = 11.sp,
                    textAlign = TextAlign.Center,
                )
                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(StreakPillBg)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    streakDays.forEach { day ->
                        StreakDayItem(day = day)
                    }
                }
            }
        }
    }
}

@Composable
private fun StreakDayItem(
    day: StreakDayUiModel,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(if (day.completed) StreakCheckOrange else Color(0xFF2B4764)),
            contentAlignment = Alignment.Center,
        ) {
            if (day.completed) {
                Canvas(modifier = Modifier.size(11.dp)) {
                    val path = Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.55f)
                        lineTo(size.width * 0.42f, size.height * 0.82f)
                        lineTo(size.width * 0.88f, size.height * 0.22f)
                    }
                    drawPath(
                        path = path,
                        color = StreakPillBg,
                        style = Stroke(
                            width = 2.2.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }
        Text(
            text = day.label,
            color = HomeText.copy(alpha = 0.9f),
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun StreakFlameIllustration(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_mingcute_fire_fill),
        contentDescription = "Streak Api",
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun TrainingRoutineCard(
    routine: TrainingRoutineUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.horizontalGradient(routine.gradientColors))
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    text = routine.title,
                    color = HomeText,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = routine.description,
                    color = HomeText.copy(alpha = 0.92f),
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                )
            }

            RoutineIllustration(
                type = routine.type,
                modifier = Modifier
                    .width(92.dp)
                    .height(74.dp),
            )
        }
    }
}

@Composable
private fun RoutineIllustration(
    type: TrainingRoutineType,
    modifier: Modifier = Modifier,
) {
    if (type == TrainingRoutineType.HOME_WORKOUT) {
        Canvas(modifier = modifier) { drawHomeWorkoutIllustration() }
        return
    }
    Image(
        painter = painterResource(
            if (type == TrainingRoutineType.STEP_TRACKER) R.drawable.running else R.drawable.drink
        ),
        contentDescription = null,
        modifier = modifier,
        contentScale = ContentScale.Fit,
    )
}

private fun DrawScope.drawHomeWorkoutIllustration() {
    val w = size.width
    val h = size.height
    // Base oval mat
    drawOval(
        color = Color(0xFFF3F5F8),
        topLeft = Offset(w * 0.08f, h * 0.56f),
        size = Size(w * 0.84f, h * 0.38f),
    )
    // Window frame in background
    drawRoundRect(
        color = Color.White.copy(alpha = 0.85f),
        topLeft = Offset(w * 0.14f, h * 0.12f),
        size = Size(w * 0.18f, h * 0.34f),
        cornerRadius = CornerRadius(3.dp.toPx()),
    )
    // Hex dumbbell left & right
    drawCircle(
        color = Color(0xFF394858),
        radius = h * 0.15f,
        center = Offset(w * 0.28f, h * 0.67f),
    )
    drawCircle(
        color = Color(0xFF4F6275),
        radius = h * 0.14f,
        center = Offset(w * 0.72f, h * 0.54f),
    )
    // Person torso (yellow top) & dark pants
    drawRoundRect(
        color = Color(0xFF283442),
        topLeft = Offset(w * 0.42f, h * 0.50f),
        size = Size(w * 0.20f, h * 0.22f),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )
    drawRoundRect(
        color = Color(0xFFFFB733),
        topLeft = Offset(w * 0.44f, h * 0.30f),
        size = Size(w * 0.16f, h * 0.22f),
        cornerRadius = CornerRadius(6.dp.toPx()),
    )
    // Head
    drawCircle(
        color = Color(0xFFFFCCAA),
        radius = h * 0.09f,
        center = Offset(w * 0.52f, h * 0.21f),
    )
    // Water bottle on right
    drawRoundRect(
        color = Color(0xFFE07A16),
        topLeft = Offset(w * 0.74f, h * 0.68f),
        size = Size(w * 0.08f, h * 0.18f),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
}

@Preview(name = "Beranda Training", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun TrainingHomeScreenPreview() {
    OlahragaKuyTheme {
        TrainingHomeScreen()
    }
}
