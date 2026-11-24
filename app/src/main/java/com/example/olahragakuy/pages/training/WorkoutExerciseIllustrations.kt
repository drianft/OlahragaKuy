package com.example.olahragakuy.pages.training

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

private val ShirtOrange = Color(0xFFFF7A26)
private val ShirtOrangeShade = Color(0xFFE66212)
private val ShortsNavy = Color(0xFF1F304D)
private val ShortsNavyDark = Color(0xFF17243A)
private val SkinLight = Color(0xFFF9BE97)
private val SkinShade = Color(0xFFE6A47B)
private val HairDark = Color(0xFF24334B)
private val ShoeDark = Color(0xFF1A283F)

@Composable
fun WorkoutExerciseIllustration(
    type: ExerciseType,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        when (type) {
            ExerciseType.SIT_UP -> drawSitUpPose()
            ExerciseType.PLANK_LURUS -> drawPlankLurusPose()
            ExerciseType.SQUAT -> drawSquatPose()
            ExerciseType.PUSH_UP -> drawPushUpPose()
            ExerciseType.JUMPING_JACK -> drawJumpingJackPose()
            ExerciseType.LUNGES -> drawLungesPose()
            ExerciseType.GLUTE_BRIDGE -> drawGluteBridgePose()
            ExerciseType.MOUNTAIN_CLIMBER -> drawMountainClimberPose()
        }
    }
}

private fun DrawScope.drawSitUpPose() {
    val w = size.width
    val h = size.height
    val limb = minOf(w, h) * 0.105f

    // Back arm behind head
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.43f, h * 0.34f),
        end = Offset(w * 0.56f, h * 0.25f),
        strokeWidth = limb * 0.72f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.56f, h * 0.25f),
        end = Offset(w * 0.43f, h * 0.16f),
        strokeWidth = limb * 0.68f,
        cap = StrokeCap.Round,
    )

    // Back bent leg
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.50f, h * 0.65f),
        end = Offset(w * 0.67f, h * 0.44f),
        strokeWidth = limb * 1.1f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.67f, h * 0.44f),
        end = Offset(w * 0.80f, h * 0.73f),
        strokeWidth = limb * 0.92f,
        cap = StrokeCap.Round,
    )
    drawOval(
        color = ShortsNavyDark,
        topLeft = Offset(w * 0.76f, h * 0.70f),
        size = Size(w * 0.14f, h * 0.08f),
    )

    // Front bent leg
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.46f, h * 0.69f),
        end = Offset(w * 0.62f, h * 0.46f),
        strokeWidth = limb * 1.18f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.62f, h * 0.46f),
        end = Offset(w * 0.75f, h * 0.76f),
        strokeWidth = limb * 0.96f,
        cap = StrokeCap.Round,
    )
    // Front shoe
    drawOval(
        color = ShoeDark,
        topLeft = Offset(w * 0.71f, h * 0.73f),
        size = Size(w * 0.15f, h * 0.09f),
    )

    // Shorts
    val shorts = Path().apply {
        moveTo(w * 0.34f, h * 0.61f)
        lineTo(w * 0.52f, h * 0.57f)
        lineTo(w * 0.58f, h * 0.74f)
        quadraticTo(w * 0.44f, h * 0.81f, w * 0.34f, h * 0.74f)
        close()
    }
    drawPath(shorts, color = ShortsNavy)

    // Torso (orange t-shirt)
    val shirt = Path().apply {
        moveTo(w * 0.25f, h * 0.31f)
        lineTo(w * 0.47f, h * 0.30f)
        lineTo(w * 0.50f, h * 0.60f)
        lineTo(w * 0.33f, h * 0.63f)
        close()
    }
    drawPath(shirt, color = ShirtOrange)

    // Front raised arm behind head
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.27f, h * 0.35f),
        end = Offset(w * 0.19f, h * 0.25f),
        strokeWidth = limb * 0.78f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.19f, h * 0.25f),
        end = Offset(w * 0.32f, h * 0.17f),
        strokeWidth = limb * 0.74f,
        cap = StrokeCap.Round,
    )
    // Shirt sleeves
    drawCircle(color = ShirtOrange, radius = limb * 0.58f, center = Offset(w * 0.28f, h * 0.34f))
    drawCircle(color = ShirtOrangeShade, radius = limb * 0.50f, center = Offset(w * 0.46f, h * 0.33f))

    // Neck & head
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.37f, h * 0.29f),
        end = Offset(w * 0.38f, h * 0.24f),
        strokeWidth = limb * 0.60f,
        cap = StrokeCap.Round,
    )
    drawCircle(color = HairDark, radius = limb * 0.92f, center = Offset(w * 0.36f, h * 0.16f))
    drawCircle(color = SkinLight, radius = limb * 0.80f, center = Offset(w * 0.39f, h * 0.19f))
    // Hair sweep over forehead
    drawArc(
        color = HairDark,
        startAngle = 165f,
        sweepAngle = 145f,
        useCenter = true,
        topLeft = Offset((w * 0.39f) - (limb * 0.88f), (h * 0.19f) - (limb * 0.94f)),
        size = Size(limb * 1.76f, limb * 1.45f),
    )
}

private fun DrawScope.drawPlankLurusPose() {
    val w = size.width
    val h = size.height
    val limb = minOf(w, h) * 0.105f

    // Supporting back arm
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.69f, h * 0.44f),
        end = Offset(w * 0.69f, h * 0.78f),
        strokeWidth = limb * 0.68f,
        cap = StrokeCap.Round,
    )

    // Legs straight back to toes
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.42f, h * 0.54f),
        end = Offset(w * 0.14f, h * 0.72f),
        strokeWidth = limb * 0.84f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.44f, h * 0.56f),
        end = Offset(w * 0.16f, h * 0.75f),
        strokeWidth = limb * 0.90f,
        cap = StrokeCap.Round,
    )
    // Shoes on toes
    drawRoundRect(
        color = ShoeDark,
        topLeft = Offset(w * 0.08f, h * 0.70f),
        size = Size(w * 0.08f, h * 0.12f),
        cornerRadius = CornerRadius(limb * 0.3f),
    )

    // Shorts
    val shorts = Path().apply {
        moveTo(w * 0.36f, h * 0.47f)
        lineTo(w * 0.52f, h * 0.42f)
        lineTo(w * 0.52f, h * 0.56f)
        lineTo(w * 0.36f, h * 0.60f)
        close()
    }
    drawPath(shorts, color = ShortsNavy)

    // Torso (orange shirt)
    val torso = Path().apply {
        moveTo(w * 0.50f, h * 0.40f)
        lineTo(w * 0.74f, h * 0.33f)
        lineTo(w * 0.75f, h * 0.48f)
        lineTo(w * 0.51f, h * 0.55f)
        close()
    }
    drawPath(torso, color = ShirtOrange)

    // Front straight arm
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.73f, h * 0.43f),
        end = Offset(w * 0.74f, h * 0.80f),
        strokeWidth = limb * 0.75f,
        cap = StrokeCap.Round,
    )
    // Hand flat on floor
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.74f, h * 0.80f),
        end = Offset(w * 0.81f, h * 0.80f),
        strokeWidth = limb * 0.42f,
        cap = StrokeCap.Round,
    )

    // Head looking forward-down
    drawCircle(color = HairDark, radius = limb * 0.80f, center = Offset(w * 0.84f, h * 0.34f))
    drawCircle(color = SkinLight, radius = limb * 0.70f, center = Offset(w * 0.85f, h * 0.38f))
}

private fun DrawScope.drawSquatPose() {
    val w = size.width
    val h = size.height
    val limb = minOf(w, h) * 0.105f

    // Back leg
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.43f, h * 0.56f),
        end = Offset(w * 0.58f, h * 0.60f),
        strokeWidth = limb * 0.96f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.58f, h * 0.60f),
        end = Offset(w * 0.54f, h * 0.84f),
        strokeWidth = limb * 0.84f,
        cap = StrokeCap.Round,
    )

    // Front leg bent in squat
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.39f, h * 0.58f),
        end = Offset(w * 0.55f, h * 0.62f),
        strokeWidth = limb * 1.04f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.55f, h * 0.62f),
        end = Offset(w * 0.48f, h * 0.86f),
        strokeWidth = limb * 0.88f,
        cap = StrokeCap.Round,
    )
    // Shoes
    drawOval(
        color = ShoeDark,
        topLeft = Offset(w * 0.46f, h * 0.83f),
        size = Size(w * 0.14f, h * 0.08f),
    )

    // Shorts
    val shorts = Path().apply {
        moveTo(w * 0.30f, h * 0.51f)
        lineTo(w * 0.46f, h * 0.51f)
        lineTo(w * 0.50f, h * 0.64f)
        lineTo(w * 0.32f, h * 0.63f)
        close()
    }
    drawPath(shorts, color = ShortsNavy)

    // Leaning torso (orange shirt)
    val torso = Path().apply {
        moveTo(w * 0.38f, h * 0.27f)
        lineTo(w * 0.53f, h * 0.31f)
        lineTo(w * 0.44f, h * 0.53f)
        lineTo(w * 0.30f, h * 0.50f)
        close()
    }
    drawPath(torso, color = ShirtOrange)

    // Arms extended straight forward for balance
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.50f, h * 0.33f),
        end = Offset(w * 0.76f, h * 0.33f),
        strokeWidth = limb * 0.66f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.47f, h * 0.35f),
        end = Offset(w * 0.78f, h * 0.36f),
        strokeWidth = limb * 0.74f,
        cap = StrokeCap.Round,
    )

    // Head
    drawCircle(color = HairDark, radius = limb * 0.84f, center = Offset(w * 0.49f, h * 0.18f))
    drawCircle(color = SkinLight, radius = limb * 0.74f, center = Offset(w * 0.51f, h * 0.20f))
}

private fun DrawScope.drawPushUpPose() {
    val w = size.width
    val h = size.height
    val limb = minOf(w, h) * 0.105f

    // Legs straight
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.44f, h * 0.62f),
        end = Offset(w * 0.14f, h * 0.76f),
        strokeWidth = limb * 0.88f,
        cap = StrokeCap.Round,
    )
    drawRoundRect(
        color = ShoeDark,
        topLeft = Offset(w * 0.08f, h * 0.70f),
        size = Size(w * 0.08f, h * 0.12f),
        cornerRadius = CornerRadius(limb * 0.3f),
    )

    // Shorts & lowered torso
    val shorts = Path().apply {
        moveTo(w * 0.36f, h * 0.55f)
        lineTo(w * 0.52f, h * 0.52f)
        lineTo(w * 0.52f, h * 0.65f)
        lineTo(w * 0.36f, h * 0.67f)
        close()
    }
    drawPath(shorts, color = ShortsNavy)

    val torso = Path().apply {
        moveTo(w * 0.50f, h * 0.50f)
        lineTo(w * 0.75f, h * 0.47f)
        lineTo(w * 0.75f, h * 0.62f)
        lineTo(w * 0.50f, h * 0.64f)
        close()
    }
    drawPath(torso, color = ShirtOrange)

    // Bent pushing arm
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.72f, h * 0.56f),
        end = Offset(w * 0.64f, h * 0.67f),
        strokeWidth = limb * 0.74f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.64f, h * 0.67f),
        end = Offset(w * 0.73f, h * 0.80f),
        strokeWidth = limb * 0.74f,
        cap = StrokeCap.Round,
    )

    // Head
    drawCircle(color = HairDark, radius = limb * 0.80f, center = Offset(w * 0.84f, h * 0.50f))
    drawCircle(color = SkinLight, radius = limb * 0.70f, center = Offset(w * 0.85f, h * 0.54f))
}

private fun DrawScope.drawJumpingJackPose() {
    val w = size.width
    val h = size.height
    val limb = minOf(w, h) * 0.10f

    // Wide legs
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.46f, h * 0.56f),
        end = Offset(w * 0.30f, h * 0.84f),
        strokeWidth = limb * 0.86f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.54f, h * 0.56f),
        end = Offset(w * 0.70f, h * 0.84f),
        strokeWidth = limb * 0.86f,
        cap = StrokeCap.Round,
    )
    drawOval(color = ShoeDark, topLeft = Offset(w * 0.23f, h * 0.82f), size = Size(w * 0.12f, h * 0.07f))
    drawOval(color = ShoeDark, topLeft = Offset(w * 0.65f, h * 0.82f), size = Size(w * 0.12f, h * 0.07f))

    // Shorts
    drawRoundRect(
        color = ShortsNavy,
        topLeft = Offset(w * 0.41f, h * 0.49f),
        size = Size(w * 0.18f, h * 0.13f),
        cornerRadius = CornerRadius(limb * 0.3f),
    )
    // Arms overhead
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.42f, h * 0.33f),
        end = Offset(w * 0.28f, h * 0.14f),
        strokeWidth = limb * 0.70f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.58f, h * 0.33f),
        end = Offset(w * 0.72f, h * 0.14f),
        strokeWidth = limb * 0.70f,
        cap = StrokeCap.Round,
    )
    // Torso
    drawRoundRect(
        color = ShirtOrange,
        topLeft = Offset(w * 0.41f, h * 0.29f),
        size = Size(w * 0.18f, h * 0.22f),
        cornerRadius = CornerRadius(limb * 0.4f),
    )
    // Head
    drawCircle(color = HairDark, radius = limb * 0.84f, center = Offset(w * 0.50f, h * 0.19f))
    drawCircle(color = SkinLight, radius = limb * 0.74f, center = Offset(w * 0.50f, h * 0.21f))
}

private fun DrawScope.drawLungesPose() {
    val w = size.width
    val h = size.height
    val limb = minOf(w, h) * 0.10f

    // Back bent leg
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.44f, h * 0.56f),
        end = Offset(w * 0.34f, h * 0.77f),
        strokeWidth = limb * 0.90f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.34f, h * 0.77f),
        end = Offset(w * 0.20f, h * 0.80f),
        strokeWidth = limb * 0.80f,
        cap = StrokeCap.Round,
    )

    // Front 90-deg leg
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.50f, h * 0.56f),
        end = Offset(w * 0.68f, h * 0.60f),
        strokeWidth = limb * 0.96f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.68f, h * 0.60f),
        end = Offset(w * 0.68f, h * 0.84f),
        strokeWidth = limb * 0.86f,
        cap = StrokeCap.Round,
    )
    drawOval(color = ShoeDark, topLeft = Offset(w * 0.66f, h * 0.82f), size = Size(w * 0.14f, h * 0.08f))

    // Shorts & upright torso
    drawRoundRect(
        color = ShortsNavy,
        topLeft = Offset(w * 0.40f, h * 0.49f),
        size = Size(w * 0.16f, h * 0.13f),
        cornerRadius = CornerRadius(limb * 0.3f),
    )
    drawRoundRect(
        color = ShirtOrange,
        topLeft = Offset(w * 0.40f, h * 0.27f),
        size = Size(w * 0.16f, h * 0.24f),
        cornerRadius = CornerRadius(limb * 0.4f),
    )
    // Hand on hip
    val armPath = Path().apply {
        moveTo(w * 0.45f, h * 0.31f)
        lineTo(w * 0.35f, h * 0.42f)
        lineTo(w * 0.44f, h * 0.50f)
    }
    drawPath(
        path = armPath,
        color = SkinLight,
        style = Stroke(width = limb * 0.66f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
    // Head
    drawCircle(color = HairDark, radius = limb * 0.84f, center = Offset(w * 0.48f, h * 0.17f))
    drawCircle(color = SkinLight, radius = limb * 0.74f, center = Offset(w * 0.50f, h * 0.19f))
}

private fun DrawScope.drawGluteBridgePose() {
    val w = size.width
    val h = size.height
    val limb = minOf(w, h) * 0.105f

    // Supporting arm flat on floor
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.30f, h * 0.73f),
        end = Offset(w * 0.56f, h * 0.78f),
        strokeWidth = limb * 0.68f,
        cap = StrokeCap.Round,
    )

    // Back bent leg
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.55f, h * 0.50f),
        end = Offset(w * 0.72f, h * 0.52f),
        strokeWidth = limb * 0.94f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.72f, h * 0.52f),
        end = Offset(w * 0.75f, h * 0.78f),
        strokeWidth = limb * 0.86f,
        cap = StrokeCap.Round,
    )

    // Front bent leg
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.52f, h * 0.53f),
        end = Offset(w * 0.69f, h * 0.54f),
        strokeWidth = limb * 1.0f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.69f, h * 0.54f),
        end = Offset(w * 0.72f, h * 0.80f),
        strokeWidth = limb * 0.90f,
        cap = StrokeCap.Round,
    )
    drawOval(
        color = ShoeDark,
        topLeft = Offset(w * 0.69f, h * 0.77f),
        size = Size(w * 0.14f, h * 0.08f),
    )

    // Raised hips (shorts)
    val shorts = Path().apply {
        moveTo(w * 0.43f, h * 0.50f)
        lineTo(w * 0.58f, h * 0.46f)
        lineTo(w * 0.57f, h * 0.61f)
        lineTo(w * 0.44f, h * 0.64f)
        close()
    }
    drawPath(shorts, color = ShortsNavy)

    // Sloped torso from shoulders on floor to lifted hips
    val torso = Path().apply {
        moveTo(w * 0.25f, h * 0.64f)
        lineTo(w * 0.46f, h * 0.50f)
        lineTo(w * 0.46f, h * 0.64f)
        lineTo(w * 0.27f, h * 0.76f)
        close()
    }
    drawPath(torso, color = ShirtOrange)

    // Head resting on floor
    drawCircle(color = HairDark, radius = limb * 0.82f, center = Offset(w * 0.16f, h * 0.72f))
    drawCircle(color = SkinLight, radius = limb * 0.72f, center = Offset(w * 0.18f, h * 0.69f))
}

private fun DrawScope.drawMountainClimberPose() {
    val w = size.width
    val h = size.height
    val limb = minOf(w, h) * 0.105f

    // Supporting back arm
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.69f, h * 0.43f),
        end = Offset(w * 0.69f, h * 0.78f),
        strokeWidth = limb * 0.66f,
        cap = StrokeCap.Round,
    )

    // Extended back leg
    drawLine(
        color = SkinShade,
        start = Offset(w * 0.42f, h * 0.53f),
        end = Offset(w * 0.14f, h * 0.74f),
        strokeWidth = limb * 0.86f,
        cap = StrokeCap.Round,
    )
    drawRoundRect(
        color = ShoeDark,
        topLeft = Offset(w * 0.08f, h * 0.70f),
        size = Size(w * 0.08f, h * 0.11f),
        cornerRadius = CornerRadius(limb * 0.3f),
    )

    // Front knee driven toward chest
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.46f, h * 0.55f),
        end = Offset(w * 0.57f, h * 0.66f),
        strokeWidth = limb * 0.96f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.57f, h * 0.66f),
        end = Offset(w * 0.46f, h * 0.77f),
        strokeWidth = limb * 0.86f,
        cap = StrokeCap.Round,
    )
    drawOval(
        color = ShoeDark,
        topLeft = Offset(w * 0.39f, h * 0.74f),
        size = Size(w * 0.12f, h * 0.08f),
    )

    // Shorts & torso
    val shorts = Path().apply {
        moveTo(w * 0.36f, h * 0.46f)
        lineTo(w * 0.52f, h * 0.42f)
        lineTo(w * 0.52f, h * 0.56f)
        lineTo(w * 0.36f, h * 0.58f)
        close()
    }
    drawPath(shorts, color = ShortsNavy)

    val torso = Path().apply {
        moveTo(w * 0.50f, h * 0.40f)
        lineTo(w * 0.74f, h * 0.33f)
        lineTo(w * 0.75f, h * 0.48f)
        lineTo(w * 0.51f, h * 0.54f)
        close()
    }
    drawPath(torso, color = ShirtOrange)

    // Front arm
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.73f, h * 0.43f),
        end = Offset(w * 0.74f, h * 0.80f),
        strokeWidth = limb * 0.74f,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = SkinLight,
        start = Offset(w * 0.74f, h * 0.80f),
        end = Offset(w * 0.81f, h * 0.80f),
        strokeWidth = limb * 0.42f,
        cap = StrokeCap.Round,
    )

    // Head
    drawCircle(color = HairDark, radius = limb * 0.80f, center = Offset(w * 0.84f, h * 0.34f))
    drawCircle(color = SkinLight, radius = limb * 0.70f, center = Offset(w * 0.85f, h * 0.38f))
}
