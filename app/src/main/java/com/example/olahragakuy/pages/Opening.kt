package com.example.olahragakuy.pages

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Transition
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.R
import com.example.olahragakuy.ui.theme.KuyButton
import com.example.olahragakuy.ui.theme.KuyNavy
import com.example.olahragakuy.ui.theme.KuyOrange
import com.example.olahragakuy.ui.theme.OlahragaKuyTheme
import com.example.olahragakuy.ui.theme.Poppins

/** The three Figma frames, connected by Smart Animate transitions. */
enum class OpeningStep { INITIAL, WORDMARK_SHOWN, ONBOARDING }

// Figma: Smart Animate, Ease Out, 800ms
private const val WORDMARK_ANIM_MS = 800

// Figma: On Tap -> Smart Animate, Ease Out, 600ms
private const val ONBOARDING_ANIM_MS = 600

// Splash layout (steps 1 & 2)
private val SplashLogoSize = 140.dp
private val SplashWordmarkWidth = 180.dp
private val SplashWordmarkSpacing = 16.dp
private val SplashLogoLiftY = (-60).dp

// Header layout (step 3)
private val HeaderTopPadding = 48.dp
private val HeaderLogoSize = 44.dp
private val HeaderWordmarkWidth = 120.dp
private val HeaderSpacing = 8.dp

// Fallback if the wordmark's intrinsic size is unavailable.
private const val DEFAULT_WORDMARK_ASPECT = 5f

private val CtaSlideDistance = 24.dp
private val OnboardingSlideDistance = 40.dp

private val DaftarBackground = Color(0xFFEAEAEA)

@Composable
fun OpeningScreen(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Saveable so rotation or returning from Login/Register doesn't replay the intro.
    var step by rememberSaveable { mutableStateOf(OpeningStep.INITIAL) }

    LaunchedEffect(Unit) {
        if (step == OpeningStep.INITIAL) {
            // Render the first frame at INITIAL (matching the system splash screen position & scale 1:1),
            // then immediately animate upward to WORDMARK_SHOWN on the very next frame.
            withFrameNanos { }
            step = OpeningStep.WORDMARK_SHOWN
        }
    }

    OpeningContent(
        step = step,
        onStartJourney = { step = OpeningStep.ONBOARDING },
        onLoginClick = onLoginClick,
        onRegisterClick = onRegisterClick,
        modifier = modifier,
    )
}

@Composable
private fun OpeningContent(
    step: OpeningStep,
    onStartJourney: () -> Unit,
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val transition = updateTransition(targetState = step, label = "opening")

    val wordmarkPainter = painterResource(R.drawable.login_wordmark)
    val wordmarkAspect = wordmarkPainter.intrinsicSize.let {
        if (it.isSpecified && (it.width > 0f) && (it.height > 0f)) it.width / it.height
        else DEFAULT_WORDMARK_ASPECT
    }
    // The wordmark is always laid out at its splash size and scaled down via graphicsLayer.
    val splashWordmarkHeight = SplashWordmarkWidth / wordmarkAspect
    val safeDrawingTop = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val safeDrawingBottom = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

    // Full-window BoxWithConstraints (no safeDrawingPadding on the root) so that centerX/centerY
    // match the Android system SplashScreen icon's exact window center (0px jump on handoff).
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(KuyNavy),
    ) {
        val centerX = maxWidth / 2
        val centerY = maxHeight / 2

        // Header row (step 3): [logo 44dp] 8dp [wordmark 120dp], centered horizontally below status bar.
        val headerRowWidth = HeaderLogoSize + HeaderSpacing + HeaderWordmarkWidth
        val headerCenterY = safeDrawingTop + HeaderTopPadding + HeaderLogoSize / 2
        val headerLogoCenterX = centerX - headerRowWidth / 2 + HeaderLogoSize / 2
        val headerWordmarkCenterX =
            centerX + headerRowWidth / 2 - HeaderWordmarkWidth / 2

        // ---------- Animated values ----------
        val logoScale by transition.animateFloat(
            transitionSpec = { stepSpec() },
            label = "logo-scale",
        ) { if (it == OpeningStep.ONBOARDING) HeaderLogoSize / SplashLogoSize else 1f }

        val logoCenterX by transition.animateDp(
            transitionSpec = { stepSpec() },
            label = "logo-x",
        ) { if (it == OpeningStep.ONBOARDING) headerLogoCenterX else centerX }

        val logoCenterY by transition.animateDp(
            transitionSpec = { stepSpec() },
            label = "logo-y",
        ) {
            when (it) {
                OpeningStep.INITIAL -> centerY
                OpeningStep.WORDMARK_SHOWN -> centerY + SplashLogoLiftY
                OpeningStep.ONBOARDING -> headerCenterY
            }
        }

        val wordmarkScale by transition.animateFloat(
            transitionSpec = { stepSpec() },
            label = "wordmark-scale",
        ) { if (it == OpeningStep.ONBOARDING) HeaderWordmarkWidth / SplashWordmarkWidth else 1f }

        val wordmarkCenterX by transition.animateDp(
            transitionSpec = { stepSpec() },
            label = "wordmark-x",
        ) { if (it == OpeningStep.ONBOARDING) headerWordmarkCenterX else centerX }

        val wordmarkCenterY by transition.animateDp(
            transitionSpec = { stepSpec() },
            label = "wordmark-y",
        ) {
            // Directly below the logo with a 16dp gap, or vertically centered in the header row.
            val belowLogo = SplashLogoSize / 2 + SplashWordmarkSpacing + splashWordmarkHeight / 2
            when (it) {
                OpeningStep.INITIAL -> centerY + belowLogo
                OpeningStep.WORDMARK_SHOWN -> centerY + SplashLogoLiftY + belowLogo
                OpeningStep.ONBOARDING -> headerCenterY
            }
        }

        val wordmarkAlpha by transition.animateFloat(
            transitionSpec = { stepSpec() },
            label = "wordmark-alpha",
        ) { if (it == OpeningStep.INITIAL) 0f else 1f }

        val ctaAlpha by transition.animateFloat(
            transitionSpec = { stepSpec() },
            label = "cta-alpha",
        ) { if (it == OpeningStep.WORDMARK_SHOWN) 1f else 0f }

        val ctaOffsetY by transition.animateDp(
            transitionSpec = { stepSpec() },
            label = "cta-y",
        ) { if (it == OpeningStep.ONBOARDING) CtaSlideDistance else 0.dp }

        val onboardingAlpha by transition.animateFloat(
            transitionSpec = { stepSpec() },
            label = "onboarding-alpha",
        ) { if (it == OpeningStep.ONBOARDING) 1f else 0f }

        val onboardingOffsetY by transition.animateDp(
            transitionSpec = { stepSpec() },
            label = "onboarding-y",
        ) { if (it == OpeningStep.ONBOARDING) 0.dp else OnboardingSlideDistance }

        // ---------- Onboarding content (step 3) ----------
        // Only composed once requested, so invisible buttons can never receive clicks.
        if (step == OpeningStep.ONBOARDING) {
            OnboardingBody(
                onLoginClick = onLoginClick,
                onRegisterClick = onRegisterClick,
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .offset { IntOffset(0, onboardingOffsetY.roundToPx()) }
                    .graphicsLayer { alpha = onboardingAlpha },
            )
        }

        // ---------- Logo ----------
        // Fixed layout size; position/scale are applied in the layout/draw phases only.
        Image(
            painter = painterResource(R.drawable.logo_olahragakuy_1),
            contentDescription = "Logo OlahragaKuy",
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (logoCenterX - SplashLogoSize / 2).roundToPx(),
                        y = (logoCenterY - SplashLogoSize / 2).roundToPx(),
                    )
                }
                .size(SplashLogoSize)
                .graphicsLayer {
                    scaleX = logoScale
                    scaleY = logoScale
                },
        )

        // ---------- Wordmark ----------
        Image(
            painter = wordmarkPainter,
            contentDescription = "OlahragaKuy",
            contentScale = ContentScale.FillWidth,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (wordmarkCenterX - SplashWordmarkWidth / 2).roundToPx(),
                        y = (wordmarkCenterY - splashWordmarkHeight / 2).roundToPx(),
                    )
                }
                .width(SplashWordmarkWidth)
                .height(splashWordmarkHeight)
                .graphicsLayer {
                    scaleX = wordmarkScale
                    scaleY = wordmarkScale
                    alpha = wordmarkAlpha
                },
        )

        // ---------- "Mulai Perjalanan" CTA ----------
        // Removed once the onboarding transition has fully settled (keeps a11y tree clean).
        val ctaSettledHidden = transition.currentState == OpeningStep.ONBOARDING &&
            transition.targetState == OpeningStep.ONBOARDING
        if (!ctaSettledHidden) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = safeDrawingBottom + 80.dp)
                    .offset { IntOffset(0, ctaOffsetY.roundToPx()) }
                    .graphicsLayer { alpha = ctaAlpha },
            ) {
                Text(
                    text = "Mulai Perjalanan",
                    color = KuyOrange,
                    fontFamily = Poppins,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    textDecoration = TextDecoration.Underline,
                    modifier = Modifier.clickable(
                        enabled = step == OpeningStep.WORDMARK_SHOWN,
                        role = Role.Button,
                        onClick = onStartJourney,
                    ),
                )
            }
        }
    }
}

/** INITIAL -> WORDMARK_SHOWN uses 800ms; anything going to/from ONBOARDING uses 600ms. */
private fun <T> Transition.Segment<OpeningStep>.stepSpec(): FiniteAnimationSpec<T> =
    if (initialState == OpeningStep.INITIAL && targetState == OpeningStep.WORDMARK_SHOWN) {
        tween(durationMillis = WORDMARK_ANIM_MS, easing = FastOutSlowInEasing)
    } else {
        tween(durationMillis = ONBOARDING_ANIM_MS, easing = FastOutSlowInEasing)
    }

@Composable
private fun OnboardingBody(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Reserve the space taken by the animated header row.
        Spacer(Modifier.height(HeaderTopPadding + HeaderLogoSize))

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.amico_home),
                contentDescription = "Ilustrasi dua orang sedang berlari",
                modifier = Modifier.size(260.dp),
            )
            Spacer(Modifier.height(24.dp))
            Text(
                text = "Mulailah Kehidupan Olahraga yang\nMenyenangkan Bersama Ribuan Orang",
                color = Color.White,
                fontFamily = Poppins,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center,
            )
        }

        OnboardingButtons(onLoginClick = onLoginClick, onRegisterClick = onRegisterClick)

        Spacer(Modifier.height(48.dp))
    }
}

@Composable
private fun OnboardingButtons(
    onLoginClick: () -> Unit,
    onRegisterClick: () -> Unit,
) {
    val buttonShape = RoundedCornerShape(25.dp)

    Button(
        onClick = onLoginClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = buttonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = KuyButton,
            contentColor = Color.White,
        ),
    ) {
        Text(
            text = "Masuk",
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }

    Spacer(Modifier.height(12.dp))

    Button(
        onClick = onRegisterClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = buttonShape,
        border = BorderStroke(1.5.dp, KuyOrange),
        colors = ButtonDefaults.buttonColors(
            containerColor = DaftarBackground,
            contentColor = KuyOrange,
        ),
    ) {
        Text(
            text = "Daftar",
            fontFamily = Poppins,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
        )
    }
}

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@Preview(name = "1 - Initial", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun OpeningInitialPreview() {
    OlahragaKuyTheme {
        OpeningContent(OpeningStep.INITIAL, {}, {}, {})
    }
}

@Preview(name = "2 - Wordmark shown", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun OpeningWordmarkPreview() {
    OlahragaKuyTheme {
        OpeningContent(OpeningStep.WORDMARK_SHOWN, {}, {}, {})
    }
}

@Preview(name = "3 - Onboarding", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun OpeningOnboardingPreview() {
    OlahragaKuyTheme {
        OpeningContent(OpeningStep.ONBOARDING, {}, {}, {})
    }
}

@Preview(name = "Animated (Interactive)", showBackground = true, widthDp = 360, heightDp = 780)
@Composable
private fun OpeningScreenPreview() {
    OlahragaKuyTheme {
        OpeningScreen(onLoginClick = {}, onRegisterClick = {})
    }
}
