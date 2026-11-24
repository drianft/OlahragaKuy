package com.example.olahragakuy.pages.training

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.draw.shadow
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
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme

enum class HomeWorkoutMode {
    GUIDED,
    CUSTOM,
}

data class HomeWorkoutOptionUiModel(
    val mode: HomeWorkoutMode,
    val title: String,
    val description: String,
)

private val OptionCardTopBg = Color(0xFF1E3E63)
private val OptionCardBottomBg = Color(0xFF0F233A)
private val OptionCardBorder = Color(0xFF2D537F)

val DefaultHomeWorkoutOptions = listOf(
    HomeWorkoutOptionUiModel(
        mode = HomeWorkoutMode.GUIDED,
        title = "Latihan Terarah",
        description = "Ikuti panduan latihan tersedia",
    ),
    HomeWorkoutOptionUiModel(
        mode = HomeWorkoutMode.CUSTOM,
        title = "Kostumisasi Latihan",
        description = "Atur jadwal latihan anda sendiri",
    ),
)

@Composable
fun HomeWorkoutScreen(
    modifier: Modifier = Modifier,
    options: List<HomeWorkoutOptionUiModel> = DefaultHomeWorkoutOptions,
    onBack: () -> Unit = {},
    onTabSelected: (HomeNavTab) -> Unit = {},
    onGuidedWorkoutClick: (() -> Unit)? = null,
    onCustomWorkoutClick: (() -> Unit)? = null,
) {
    var notice by rememberSaveable { mutableStateOf<String?>(null) }

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
                text = "Workout Rumahan",
                color = HomeText,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Max),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                options.forEach { option ->
                    HomeWorkoutOptionCard(
                        option = option,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        onClick = {
                            when (option.mode) {
                                HomeWorkoutMode.GUIDED -> {
                                    if (onGuidedWorkoutClick != null) {
                                        onGuidedWorkoutClick()
                                    } else {
                                        notice = "Fitur ${option.title} segera hadir."
                                    }
                                }
                                HomeWorkoutMode.CUSTOM -> {
                                    if (onCustomWorkoutClick != null) {
                                        onCustomWorkoutClick()
                                    } else {
                                        notice = "Fitur ${option.title} segera hadir."
                                    }
                                }
                            }
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
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
private fun HomeWorkoutOptionCard(
    option: HomeWorkoutOptionUiModel,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier = modifier
            .shadow(elevation = 6.dp, shape = shape, clip = false)
            .clip(shape)
            .background(
                Brush.verticalGradient(
                    colors = listOf(OptionCardTopBg, OptionCardBottomBg),
                ),
            )
            .border(
                width = 1.dp,
                color = OptionCardBorder.copy(alpha = 0.45f),
                shape = shape,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 20.dp),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top,
        ) {
            HomeWorkoutIllustration(
                mode = option.mode,
                modifier = Modifier.size(width = 124.dp, height = 104.dp),
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = option.title,
                color = HomeText,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                lineHeight = 19.sp,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = option.description,
                color = HomeText.copy(alpha = 0.72f),
                fontSize = 10.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun HomeWorkoutIllustration(
    mode: HomeWorkoutMode,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        when (mode) {
            HomeWorkoutMode.GUIDED -> drawGuidedWorkoutIllustration()
            HomeWorkoutMode.CUSTOM -> drawCustomWorkoutIllustration()
        }
    }
}

private fun DrawScope.drawGuidedWorkoutIllustration() {
    val w = size.width
    val h = size.height

    // Isometric floor circle pad
    drawOval(
        color = Color(0xFFF3F6FA),
        topLeft = Offset(w * 0.10f, h * 0.48f),
        size = Size(w * 0.80f, h * 0.48f),
    )
    drawOval(
        color = Color(0xFFDCE3EE),
        topLeft = Offset(w * 0.14f, h * 0.62f),
        size = Size(w * 0.72f, h * 0.28f),
    )

    // Left vertical board/mirror panel
    val leftBoard = Path().apply {
        moveTo(w * 0.16f, h * 0.26f)
        lineTo(w * 0.36f, h * 0.14f)
        lineTo(w * 0.36f, h * 0.56f)
        lineTo(w * 0.16f, h * 0.68f)
        close()
    }
    drawPath(leftBoard, color = Color(0xFFF7F9FC))
    drawPath(
        leftBoard,
        color = Color(0xFFD1DAE6),
        style = Stroke(width = 1.dp.toPx()),
    )

    // Right workout checklist board
    val rightBoard = Path().apply {
        moveTo(w * 0.72f, h * 0.20f)
        lineTo(w * 0.88f, h * 0.28f)
        lineTo(w * 0.88f, h * 0.56f)
        lineTo(w * 0.72f, h * 0.48f)
        close()
    }
    drawPath(rightBoard, color = Color(0xFFF7F9FC))
    drawLine(
        color = Color(0xFFB6C4D4),
        start = Offset(w * 0.76f, h * 0.30f),
        end = Offset(w * 0.84f, h * 0.34f),
        strokeWidth = 1.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFB6C4D4),
        start = Offset(w * 0.76f, h * 0.37f),
        end = Offset(w * 0.84f, h * 0.41f),
        strokeWidth = 1.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFF17829),
        start = Offset(w * 0.76f, h * 0.44f),
        end = Offset(w * 0.83f, h * 0.47f),
        strokeWidth = 1.8.dp.toPx(),
        cap = StrokeCap.Round,
    )

    // Orange checkmark badge above center
    val badgeCenter = Offset(w * 0.46f, h * 0.23f)
    val badgeRadius = h * 0.085f
    drawCircle(color = Color.White, radius = badgeRadius + 1.5.dp.toPx(), center = badgeCenter)
    drawCircle(color = Color(0xFFF17829), radius = badgeRadius, center = badgeCenter)
    val checkPath = Path().apply {
        moveTo(badgeCenter.x - (badgeRadius * 0.42f), badgeCenter.y + (badgeRadius * 0.02f))
        lineTo(badgeCenter.x - (badgeRadius * 0.08f), badgeCenter.y + (badgeRadius * 0.36f))
        lineTo(badgeCenter.x + (badgeRadius * 0.46f), badgeCenter.y - (badgeRadius * 0.32f))
    }
    drawPath(
        path = checkPath,
        color = Color.White,
        style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
    )

    // Dumbbell rack (left foreground)
    val rackColor = Color(0xFF2B394A)
    drawLine(
        color = rackColor,
        start = Offset(w * 0.24f, h * 0.45f),
        end = Offset(w * 0.18f, h * 0.78f),
        strokeWidth = 2.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = rackColor,
        start = Offset(w * 0.44f, h * 0.36f),
        end = Offset(w * 0.40f, h * 0.74f),
        strokeWidth = 2.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = rackColor,
        start = Offset(w * 0.20f, h * 0.76f),
        end = Offset(w * 0.42f, h * 0.73f),
        strokeWidth = 2.4.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Dumbbell rows on rack
    val rowOffsets = listOf(0.50f to 0.44f, 0.59f to 0.53f, 0.68f to 0.62f)
    rowOffsets.forEach { (yLeft, yRight) ->
        drawLine(
            color = Color(0xFF475A70),
            start = Offset(w * 0.21f, h * yLeft),
            end = Offset(w * 0.43f, h * yRight),
            strokeWidth = 2.2.dp.toPx(),
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = Color(0xFF1F2A38),
            radius = 3.2.dp.toPx(),
            center = Offset(w * 0.26f, h * (yLeft - 0.015f)),
        )
        drawCircle(
            color = Color(0xFF1F2A38),
            radius = 3.2.dp.toPx(),
            center = Offset(w * 0.36f, h * (yRight + 0.015f)),
        )
    }

    // Standing athlete (center-right)
    // Legs
    drawLine(
        color = Color(0xFF344557),
        start = Offset(w * 0.58f, h * 0.56f),
        end = Offset(w * 0.54f, h * 0.80f),
        strokeWidth = 4.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFF283747),
        start = Offset(w * 0.64f, h * 0.56f),
        end = Offset(w * 0.66f, h * 0.82f),
        strokeWidth = 4.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Sneakers
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(w * 0.50f, h * 0.79f),
        size = Size(w * 0.07f, h * 0.05f),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(w * 0.63f, h * 0.81f),
        size = Size(w * 0.07f, h * 0.05f),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
    // Torso (orange athletic top)
    drawRoundRect(
        color = Color(0xFFF17829),
        topLeft = Offset(w * 0.55f, h * 0.34f),
        size = Size(w * 0.13f, h * 0.23f),
        cornerRadius = CornerRadius(5.dp.toPx()),
    )
    // Arms
    drawLine(
        color = Color(0xFFF6C2A2),
        start = Offset(w * 0.56f, h * 0.38f),
        end = Offset(w * 0.48f, h * 0.45f),
        strokeWidth = 3.2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFF6C2A2),
        start = Offset(w * 0.67f, h * 0.38f),
        end = Offset(w * 0.71f, h * 0.51f),
        strokeWidth = 3.2.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Head & hair
    drawCircle(
        color = Color(0xFF283442),
        radius = h * 0.072f,
        center = Offset(w * 0.63f, h * 0.25f),
    )
    drawCircle(
        color = Color(0xFFF6C2A2),
        radius = h * 0.062f,
        center = Offset(w * 0.61f, h * 0.26f),
    )

    // Water bottle on right floor
    drawRoundRect(
        color = Color(0xFFE86F1F),
        topLeft = Offset(w * 0.76f, h * 0.65f),
        size = Size(w * 0.055f, h * 0.14f),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
    drawRoundRect(
        color = Color(0xFF2B394A),
        topLeft = Offset(w * 0.77f, h * 0.625f),
        size = Size(w * 0.035f, h * 0.03f),
        cornerRadius = CornerRadius(1.dp.toPx()),
    )
}

private fun DrawScope.drawCustomWorkoutIllustration() {
    val w = size.width
    val h = size.height

    // Isometric floor circle pad
    drawOval(
        color = Color(0xFFF3F6FA),
        topLeft = Offset(w * 0.06f, h * 0.42f),
        size = Size(w * 0.88f, h * 0.52f),
    )
    drawOval(
        color = Color(0xFFE0E6F0),
        topLeft = Offset(w * 0.12f, h * 0.56f),
        size = Size(w * 0.76f, h * 0.30f),
    )

    // Diagonal beige workout mat
    val matPath = Path().apply {
        moveTo(w * 0.14f, h * 0.60f)
        lineTo(w * 0.38f, h * 0.48f)
        lineTo(w * 0.86f, h * 0.72f)
        lineTo(w * 0.62f, h * 0.86f)
        close()
    }
    drawPath(matPath, color = Color(0xFFF7DFC6))
    drawPath(
        matPath,
        color = Color(0xFFE2C3A3),
        style = Stroke(width = 1.2.dp.toPx()),
    )

    // Stability / exercise ball on upper right
    val ballCenter = Offset(w * 0.70f, h * 0.40f)
    val ballRadius = h * 0.16f
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFFC4733B), Color(0xFF8B471C)),
            center = Offset(ballCenter.x - ballRadius * 0.3f, ballCenter.y - ballRadius * 0.3f),
            radius = ballRadius * 1.4f,
        ),
        radius = ballRadius,
        center = ballCenter,
    )

    // Person doing crunch / sit-up on mat
    // Bent legs (skin tone)
    drawLine(
        color = Color(0xFFF5BFA0),
        start = Offset(w * 0.54f, h * 0.63f),
        end = Offset(w * 0.62f, h * 0.50f),
        strokeWidth = 5.5.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFF5BFA0),
        start = Offset(w * 0.62f, h * 0.50f),
        end = Offset(w * 0.72f, h * 0.66f),
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Back leg slightly offset
    drawLine(
        color = Color(0xFFE8AE8E),
        start = Offset(w * 0.50f, h * 0.60f),
        end = Offset(w * 0.58f, h * 0.47f),
        strokeWidth = 5.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFE8AE8E),
        start = Offset(w * 0.58f, h * 0.47f),
        end = Offset(w * 0.67f, h * 0.62f),
        strokeWidth = 4.5.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Sneakers
    drawRoundRect(
        color = Color(0xFF283747),
        topLeft = Offset(w * 0.69f, h * 0.65f),
        size = Size(w * 0.08f, h * 0.055f),
        cornerRadius = CornerRadius(2.5.dp.toPx()),
    )
    // Shorts (dark slate)
    val shortsPath = Path().apply {
        moveTo(w * 0.44f, h * 0.58f)
        lineTo(w * 0.56f, h * 0.56f)
        lineTo(w * 0.58f, h * 0.67f)
        lineTo(w * 0.45f, h * 0.67f)
        close()
    }
    drawPath(shortsPath, color = Color(0xFF344557))
    // Torso (orange shirt)
    val torsoPath = Path().apply {
        moveTo(w * 0.30f, h * 0.46f)
        lineTo(w * 0.42f, h * 0.44f)
        lineTo(w * 0.49f, h * 0.60f)
        lineTo(w * 0.37f, h * 0.64f)
        close()
    }
    drawPath(torsoPath, color = Color(0xFFF17829))
    // Raised arms behind head
    drawLine(
        color = Color(0xFFF5BFA0),
        start = Offset(w * 0.34f, h * 0.48f),
        end = Offset(w * 0.26f, h * 0.38f),
        strokeWidth = 3.6.dp.toPx(),
        cap = StrokeCap.Round,
    )
    drawLine(
        color = Color(0xFFF5BFA0),
        start = Offset(w * 0.26f, h * 0.38f),
        end = Offset(w * 0.34f, h * 0.34f),
        strokeWidth = 3.4.dp.toPx(),
        cap = StrokeCap.Round,
    )
    // Head & hair
    drawCircle(
        color = Color(0xFF283442),
        radius = h * 0.072f,
        center = Offset(w * 0.37f, h * 0.35f),
    )
    drawCircle(
        color = Color(0xFFF5BFA0),
        radius = h * 0.064f,
        center = Offset(w * 0.39f, h * 0.37f),
    )

    // Water bottle on left foreground
    drawRoundRect(
        color = Color(0xFF9C5427),
        topLeft = Offset(w * 0.16f, h * 0.58f),
        size = Size(w * 0.055f, h * 0.15f),
        cornerRadius = CornerRadius(2.dp.toPx()),
    )
    drawRoundRect(
        color = Color(0xFF2B394A),
        topLeft = Offset(w * 0.17f, h * 0.555f),
        size = Size(w * 0.035f, h * 0.03f),
        cornerRadius = CornerRadius(1.dp.toPx()),
    )
}

@Preview(name = "Workout Rumahan", showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun HomeWorkoutScreenPreview() {
    OlahragaKuyTheme {
        HomeWorkoutScreen()
    }
}
