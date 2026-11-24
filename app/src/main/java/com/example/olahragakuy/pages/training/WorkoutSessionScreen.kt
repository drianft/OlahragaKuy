package com.example.olahragakuy.pages.training

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.olahragakuy.ui.components.KuyBackButton
import com.example.olahragakuy.ui.theme.HomeNavy
import com.example.olahragakuy.ui.theme.HomeText
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

private val SessionSheetBg = Color(0xFF1F3E64)
private val PopupCardBg = Color(0xFF1B3A5E)
private val SecondaryButtonBg = Color(0xFFEEE8E2)
private val SecondaryButtonAccent = Color(0xFF9C4716)

@Composable
fun WorkoutSessionScreen(
    exercises: List<GuidedWorkoutStep>,
    modifier: Modifier = Modifier,
    initialStepIndex: Int = 0,
    exerciseLookup: (String) -> WorkoutExerciseDetail? = WorkoutFirestoreStore::exerciseOrNull,
    onQuitConfirmed: () -> Unit = {},
    onWorkoutCompleted: () -> Unit = {},
    onCompletedContinue: () -> Unit = {},
) {
    if (exercises.isEmpty()) {
        LaunchedEffect(Unit) { onQuitConfirmed() }
        return
    }

    var currentIndex by rememberSaveable {
        mutableIntStateOf(initialStepIndex.coerceIn(0, exercises.lastIndex))
    }
    var showQuitDialog by rememberSaveable { mutableStateOf(false) }
    var showCompletedDialog by rememberSaveable { mutableStateOf(false) }

    val currentStep = exercises[currentIndex]
    val exerciseDetail = exerciseLookup(currentStep.exerciseId)

    var remainingSeconds by rememberSaveable(currentIndex, currentStep.id) {
        mutableIntStateOf((currentStep.target as? WorkoutTarget.Duration)?.seconds ?: 0)
    }

    LaunchedEffect(currentIndex, showQuitDialog, showCompletedDialog) {
        if (currentStep.target is WorkoutTarget.Duration && !showQuitDialog && !showCompletedDialog) {
            while (remainingSeconds > 0) {
                delay(1.seconds)
                remainingSeconds = (remainingSeconds - 1).coerceAtLeast(0)
            }
        }
    }

    BackHandler(enabled = !showCompletedDialog) {
        if (showQuitDialog) {
            showQuitDialog = false
        } else {
            showQuitDialog = true
        }
    }

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
                KuyBackButton(onClick = { showQuitDialog = true })

                Spacer(modifier = Modifier.height(22.dp))

                Text(
                    text = exerciseDetail?.name ?: currentStep.exerciseId,
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
                        type = currentStep.exerciseType,
                        modifier = Modifier.size(width = 230.dp, height = 205.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                color = SessionSheetBg,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Lakukan Gerakkan",
                        color = HomeText,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    when (val target = currentStep.target) {
                        is WorkoutTarget.Repetitions -> {
                            Text(
                                text = "${target.count} kali",
                                color = HomeText,
                                fontSize = 44.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Repetisi",
                                color = HomeText.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            )
                        }

                        is WorkoutTarget.Duration -> {
                            Text(
                                text = formatDurationClock(remainingSeconds),
                                color = HomeText,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (target.seconds < 60) "Detik" else "Menit",
                                color = HomeText.copy(alpha = 0.85f),
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Button(
                        onClick = {
                            if (currentIndex < exercises.lastIndex) {
                                currentIndex += 1
                            } else {
                                onWorkoutCompleted()
                                showCompletedDialog = true
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
                            text = "Lanjutkan",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }

    if (showQuitDialog) {
        WorkoutQuitDialog(
            onDismiss = { showQuitDialog = false },
            onConfirmQuit = {
                showQuitDialog = false
                onQuitConfirmed()
            },
        )
    }

    if (showCompletedDialog) {
        WorkoutCompletedDialog(
            onContinue = {
                showCompletedDialog = false
                onCompletedContinue()
            },
        )
    }
}

@Composable
fun WorkoutCompletedDialog(
    onContinue: () -> Unit,
) {
    Dialog(
        onDismissRequest = {},
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false,
        ),
    ) {
        WorkoutCompletedCard(onContinue = onContinue)
    }
}

@Composable
private fun WorkoutCompletedCard(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = PopupCardBg,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WorkoutCompletedIllustration(modifier = Modifier.size(width = 132.dp, height = 112.dp))

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Selamat",
                color = HomeText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "akun anda berhasil menyelesaikan latihan",
                color = HomeText.copy(alpha = 0.88f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = onContinue,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(25.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KuyButton,
                    contentColor = HomeText,
                ),
            ) {
                Text(
                    text = "Lanjutkan",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

@Composable
fun WorkoutQuitDialog(
    onDismiss: () -> Unit,
    onConfirmQuit: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        WorkoutQuitCard(
            onDismiss = onDismiss,
            onConfirmQuit = onConfirmQuit,
        )
    }
}

@Composable
private fun WorkoutQuitCard(
    onDismiss: () -> Unit,
    onConfirmQuit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(26.dp),
        color = PopupCardBg,
        shadowElevation = 10.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            WorkoutQuitIllustration(modifier = Modifier.size(width = 132.dp, height = 112.dp))

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Menyerah?",
                color = HomeText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "apakah anda yakin ingin mengakhiri latihan?",
                color = HomeText.copy(alpha = 0.88f),
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 100.dp)
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, SecondaryButtonAccent),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = SecondaryButtonBg,
                        contentColor = SecondaryButtonAccent,
                    ),
                ) {
                    Text(
                        text = "Kembali",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Button(
                    onClick = onConfirmQuit,
                    modifier = Modifier
                        .weight(1f)
                        .widthIn(min = 100.dp)
                        .height(44.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = KuyButton,
                        contentColor = HomeText,
                    ),
                ) {
                    Text(
                        text = "Yakin",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

@Composable
internal fun WorkoutCompletedIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCompletedIllustration()
    }
}

private fun DrawScope.drawCompletedIllustration() {
    val w = size.width
    val h = size.height

    // White ground oval
    drawOval(
        color = Color(0xFFF3F6FA),
        topLeft = Offset(w * 0.08f, h * 0.42f),
        size = Size(w * 0.84f, h * 0.54f),
    )

    // Soft outdoor background trees & clouds
    val treeColor = Color(0xFFDCE4EE)
    listOf(0.18f to 0.36f, 0.27f to 0.28f, 0.72f to 0.30f, 0.82f to 0.38f).forEach { (x, y) ->
        drawCircle(color = treeColor, radius = h * 0.09f, center = Offset(w * x, h * y))
        drawLine(
            color = Color(0xFF9FB0C4),
            start = Offset(w * x, h * (y + 0.06f)),
            end = Offset(w * x, h * (y + 0.20f)),
            strokeWidth = 2.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
    // Small sun & thumbs-up badge
    drawCircle(color = Color(0xFFF4A48A), radius = h * 0.035f, center = Offset(w * 0.16f, h * 0.12f))
    val badgeCenter = Offset(w * 0.48f, h * 0.14f)
    drawCircle(color = Color.White, radius = h * 0.085f, center = badgeCenter)
    drawCircle(color = Color(0xFFE87A24), radius = h * 0.052f, center = badgeCenter)

    // Yoga blocks on left
    drawRoundRect(
        color = Color(0xFFF1BE90),
        topLeft = Offset(w * 0.14f, h * 0.56f),
        size = Size(w * 0.13f, h * 0.15f),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
    drawRoundRect(
        color = Color(0xFFE5AB78),
        topLeft = Offset(w * 0.23f, h * 0.60f),
        size = Size(w * 0.12f, h * 0.13f),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )

    // Stretching person seated on ground
    // Extended leg to right
    drawLine(
        color = Color(0xFFF6B796),
        start = Offset(w * 0.54f, h * 0.69f),
        end = Offset(w * 0.72f, h * 0.84f),
        strokeWidth = 7.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Folded leg to left
    drawLine(
        color = Color(0xFFF6B796),
        start = Offset(w * 0.42f, h * 0.69f),
        end = Offset(w * 0.32f, h * 0.78f),
        strokeWidth = 7.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFF6B796),
        start = Offset(w * 0.32f, h * 0.78f),
        end = Offset(w * 0.49f, h * 0.79f),
        strokeWidth = 6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Shorts
    val shorts = Path().apply {
        moveTo(w * 0.36f, h * 0.62f)
        lineTo(w * 0.57f, h * 0.64f)
        lineTo(w * 0.59f, h * 0.75f)
        lineTo(w * 0.33f, h * 0.74f)
        close()
    }
    drawPath(shorts, color = Color(0xFF3A495C))
    // Torso (orange top)
    val top = Path().apply {
        moveTo(w * 0.41f, h * 0.40f)
        lineTo(w * 0.56f, h * 0.44f)
        lineTo(w * 0.55f, h * 0.65f)
        lineTo(w * 0.37f, h * 0.63f)
        close()
    }
    drawPath(top, color = Color(0xFFE87A24))
    // Stretching arms
    drawLine(
        color = Color(0xFFF6B796),
        start = Offset(w * 0.43f, h * 0.42f),
        end = Offset(w * 0.40f, h * 0.25f),
        strokeWidth = 4.5.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFF6B796),
        start = Offset(w * 0.40f, h * 0.25f),
        end = Offset(w * 0.54f, h * 0.26f),
        strokeWidth = 4.2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFF6B796),
        start = Offset(w * 0.55f, h * 0.48f),
        end = Offset(w * 0.70f, h * 0.74f),
        strokeWidth = 4.2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Head & ponytail
    drawCircle(color = Color(0xFF2B3849), radius = h * 0.085f, center = Offset(w * 0.55f, h * 0.36f))
    drawCircle(color = Color(0xFFF6B796), radius = h * 0.072f, center = Offset(w * 0.51f, h * 0.38f))

    // Water bottle on right
    drawRoundRect(
        color = Color(0xFFC56B1E),
        topLeft = Offset(w * 0.73f, h * 0.53f),
        size = Size(w * 0.075f, h * 0.18f),
        cornerRadius = CornerRadius(3.dp.toPx()),
    )
    drawRoundRect(
        color = Color(0xFF2B3849),
        topLeft = Offset(w * 0.745f, h * 0.495f),
        size = Size(w * 0.045f, h * 0.04f),
        cornerRadius = CornerRadius(1.5.dp.toPx()),
    )
}

@Composable
private fun WorkoutQuitIllustration(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawQuitIllustration()
    }
}

private fun DrawScope.drawQuitIllustration() {
    val w = size.width
    val h = size.height

    // Sandy crossroads oval base
    drawOval(
        color = Color(0xFFF5C99E),
        topLeft = Offset(w * 0.08f, h * 0.44f),
        size = Size(w * 0.84f, h * 0.52f),
    )

    // Forked white crossroads paths
    val leftPath = Path().apply {
        moveTo(w * 0.46f, h * 0.64f)
        lineTo(w * 0.54f, h * 0.64f)
        lineTo(w * 0.25f, h * 0.90f)
        lineTo(w * 0.14f, h * 0.82f)
        close()
    }
    val rightPath = Path().apply {
        moveTo(w * 0.46f, h * 0.64f)
        lineTo(w * 0.54f, h * 0.64f)
        lineTo(w * 0.86f, h * 0.82f)
        lineTo(w * 0.75f, h * 0.90f)
        close()
    }
    drawPath(leftPath, color = Color(0xFFF6F7FA))
    drawPath(rightPath, color = Color(0xFFF6F7FA))

    // Left & right signposts
    val postColor = Color(0xFF3E4E60)
    drawLine(
        color = postColor,
        start = Offset(w * 0.24f, h * 0.46f),
        end = Offset(w * 0.24f, h * 0.74f),
        strokeWidth = 2.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawRoundRect(
        color = postColor,
        topLeft = Offset(w * 0.15f, h * 0.48f),
        size = Size(w * 0.16f, h * 0.06f),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
    drawRoundRect(
        color = Color(0xFF53667C),
        topLeft = Offset(w * 0.18f, h * 0.56f),
        size = Size(w * 0.15f, h * 0.06f),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )

    drawLine(
        color = postColor,
        start = Offset(w * 0.76f, h * 0.46f),
        end = Offset(w * 0.76f, h * 0.74f),
        strokeWidth = 2.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawRoundRect(
        color = postColor,
        topLeft = Offset(w * 0.69f, h * 0.48f),
        size = Size(w * 0.16f, h * 0.06f),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
    drawRoundRect(
        color = Color(0xFF53667C),
        topLeft = Offset(w * 0.67f, h * 0.56f),
        size = Size(w * 0.15f, h * 0.06f),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )

    // Confusion swirl halo behind head
    drawCircle(
        color = Color.White.copy(alpha = 0.22f),
        radius = h * 0.16f,
        center = Offset(w * 0.50f, h * 0.22f),
        style = Stroke(width = 1.2.dp.toPx()),
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.16f),
        radius = h * 0.21f,
        center = Offset(w * 0.50f, h * 0.22f),
        style = Stroke(width = 1.dp.toPx()),
    )

    // Floating question marks around person
    listOf(
        Offset(w * 0.20f, h * 0.20f),
        Offset(w * 0.28f, h * 0.15f),
        Offset(w * 0.75f, h * 0.18f),
        Offset(w * 0.83f, h * 0.30f),
    ).forEach { pos ->
        drawArc(
            color = Color.White.copy(alpha = 0.85f),
            startAngle = 190f,
            sweepAngle = 210f,
            useCenter = false,
            topLeft = Offset(pos.x - 4.dp.toPx(), pos.y - 4.dp.toPx()),
            size = Size(8.dp.toPx(), 8.dp.toPx()),
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round),
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.85f),
            radius = 1.2.dp.toPx(),
            center = Offset(pos.x, pos.y + 6.dp.toPx()),
        )
    }

    // Standing confused person in center
    // Shadow
    drawOval(
        color = Color(0xFFCE9E74),
        topLeft = Offset(w * 0.39f, h * 0.72f),
        size = Size(w * 0.22f, h * 0.07f),
    )
    // Legs
    drawLine(
        color = Color(0xFF303E50),
        start = Offset(w * 0.47f, h * 0.46f),
        end = Offset(w * 0.44f, h * 0.73f),
        strokeWidth = 5.5.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFF253242),
        start = Offset(w * 0.53f, h * 0.46f),
        end = Offset(w * 0.56f, h * 0.73f),
        strokeWidth = 5.5.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // White shirt + orange open jacket
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(w * 0.44f, h * 0.28f),
        size = Size(w * 0.12f, h * 0.20f),
        cornerRadius = CornerRadius(4.dp.toPx()),
    )
    drawRoundRect(
        color = Color(0xFFF17829),
        topLeft = Offset(w * 0.42f, h * 0.27f),
        size = Size(w * 0.05f, h * 0.20f),
        cornerRadius = CornerRadius(3.dp.toPx()),
    )
    drawRoundRect(
        color = Color(0xFFF17829),
        topLeft = Offset(w * 0.53f, h * 0.27f),
        size = Size(w * 0.05f, h * 0.20f),
        cornerRadius = CornerRadius(3.dp.toPx()),
    )
    // Raised arms holding head
    drawLine(
        color = Color(0xFFF6C2A2),
        start = Offset(w * 0.43f, h * 0.30f),
        end = Offset(w * 0.37f, h * 0.22f),
        strokeWidth = 3.5.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFF6C2A2),
        start = Offset(w * 0.37f, h * 0.22f),
        end = Offset(w * 0.46f, h * 0.16f),
        strokeWidth = 3.2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFF6C2A2),
        start = Offset(w * 0.57f, h * 0.30f),
        end = Offset(w * 0.63f, h * 0.22f),
        strokeWidth = 3.5.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFF6C2A2),
        start = Offset(w * 0.63f, h * 0.22f),
        end = Offset(w * 0.54f, h * 0.16f),
        strokeWidth = 3.2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Head
    drawCircle(color = Color(0xFF283442), radius = h * 0.068f, center = Offset(w * 0.50f, h * 0.17f))
    drawCircle(color = Color(0xFFF6C2A2), radius = h * 0.060f, center = Offset(w * 0.50f, h * 0.19f))
}

@Preview(name = "Mulai Latihan - Jumlah", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun WorkoutSessionRepsPreview() {
    val previewExercises = WorkoutCatalogSeeder.buildSeedExercises().associateBy { it.id.uppercase() }
    OlahragaKuyTheme {
        WorkoutSessionScreen(
            exercises = WorkoutCatalogSeeder.buildSeedGuidedDays().first().exercises,
            initialStepIndex = 0,
            exerciseLookup = { previewExercises[it.uppercase()] },
        )
    }
}

@Preview(name = "Mulai Latihan - Waktu", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun WorkoutSessionTimerPreview() {
    val previewExercises = WorkoutCatalogSeeder.buildSeedExercises().associateBy { it.id.uppercase() }
    OlahragaKuyTheme {
        WorkoutSessionScreen(
            exercises = WorkoutCatalogSeeder.buildSeedGuidedDays().first().exercises,
            initialStepIndex = 1,
            exerciseLookup = { previewExercises[it.uppercase()] },
        )
    }
}

@Preview(name = "Popup Selesai Latihan", showBackground = true, backgroundColor = 0xFF0A1A2C)
@Composable
private fun WorkoutCompletedCardPreview() {
    OlahragaKuyTheme {
        Box(modifier = Modifier.padding(20.dp)) {
            WorkoutCompletedCard(onContinue = {})
        }
    }
}

@Preview(name = "Popup Menyerah", showBackground = true, backgroundColor = 0xFF0A1A2C)
@Composable
private fun WorkoutQuitCardPreview() {
    OlahragaKuyTheme {
        Box(modifier = Modifier.padding(20.dp)) {
            WorkoutQuitCard(onDismiss = {}, onConfirmQuit = {})
        }
    }
}
