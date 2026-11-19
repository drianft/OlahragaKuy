package com.example.olahragakuy.ui.components
import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.olahragakuy.ui.theme.*
@Composable
fun KuyPrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth().height(60.dp),
        shape = RoundedCornerShape(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = KuyButton,
            contentColor = KuyText,
            disabledContainerColor = KuyButton.copy(alpha = 0.5f),
            disabledContentColor = KuyText.copy(alpha = 0.6f),
        ),
        contentPadding = PaddingValues(10.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelLarge)
    }
}
@Composable
fun KuyTextField(
    value: String, onValueChange: (String) -> Unit, label: String,
    modifier: Modifier = Modifier, errorMessage: String? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    contentType: ContentType? = null,
    trailingContent: (@Composable () -> Unit)? = null,
) {
    BasicTextField(value = value, onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth().height(60.dp).semantics {
            contentDescription = label
            if (contentType != null) {
                this.contentType = contentType
            }
            errorMessage?.let { if (it.isNotEmpty()) error(it) }
        }, singleLine = true,
        textStyle = MaterialTheme.typography.bodySmall.copy(color = KuyText),
        cursorBrush = SolidColor(KuyOrange), visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions, keyboardActions = keyboardActions,
        decorationBox = { innerTextField ->
            Surface(shape = RoundedCornerShape(12.dp), color = KuyNavy,
                border = BorderStroke(1.dp, if (errorMessage == null) KuyButton else KuyError)) {
                Row(
                    Modifier.fillMaxSize().padding(horizontal = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (value.isEmpty()) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = KuyText.copy(alpha = 0.55f),
                            )
                            innerTextField()
                        } else {
                            Column(verticalArrangement = Arrangement.Center) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontSize = 10.sp,
                                        lineHeight = 12.sp,
                                    ),
                                    color = if (errorMessage == null) KuyOrange else KuyError,
                                )
                                innerTextField()
                            }
                        }
                    }
                    if (trailingContent != null) {
                        Spacer(Modifier.width(8.dp))
                        trailingContent()
                    }
                }
            }
        })
}

@Composable
fun KuyPasswordVisibilityToggle(
    visible: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val description = if (visible) "Sembunyikan kata sandi" else "Lihat kata sandi"
    val iconColor = if (visible) KuyOrange else KuyText.copy(alpha = 0.8f)

    Box(
        modifier = modifier
            .size(36.dp)
            .semantics {
                contentDescription = description
                role = Role.Button
            }
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val strokeWidth = 1.8.dp.toPx()
            val eyePath = Path().apply {
                moveTo(w * 0.08f, h * 0.5f)
                quadraticTo(w * 0.5f, h * 0.14f, w * 0.92f, h * 0.5f)
                quadraticTo(w * 0.5f, h * 0.86f, w * 0.08f, h * 0.5f)
                close()
            }
            drawPath(
                path = eyePath,
                color = iconColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
            drawCircle(
                color = iconColor,
                radius = w * 0.14f,
                center = Offset(w * 0.5f, h * 0.5f),
                style = Stroke(width = strokeWidth),
            )
            if (!visible) {
                drawLine(
                    color = iconColor,
                    start = Offset(w * 0.18f, h * 0.82f),
                    end = Offset(w * 0.82f, h * 0.18f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

private const val POPUP_ENTER_MS = 600
private val PopupCardColor = Color(0xFF1E3A5F)

/**
 * Figma modal overlay (Move in Direction Up, Ease Out, 600ms; scrim 25%; non-dismissible).
 */
@Composable
fun KuySuccessPopupOverlay(
    visible: Boolean,
    @DrawableRes illustrationRes: Int,
    subtitle: String,
    onContinue: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "Selamat",
) {
    // Non-dismissible: swallow system back and outside taps while visible.
    BackHandler(enabled = visible) { }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(POPUP_ENTER_MS, easing = FastOutSlowInEasing)),
        exit = fadeOut(tween(200)),
        modifier = modifier.fillMaxSize(),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.25f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {},
                ),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .animateEnterExit(
                        enter = slideInVertically(
                            initialOffsetY = { it },
                            animationSpec = tween(
                                durationMillis = POPUP_ENTER_MS,
                                easing = FastOutSlowInEasing,
                            ),
                        ) + fadeIn(
                            animationSpec = tween(
                                durationMillis = POPUP_ENTER_MS,
                                easing = FastOutSlowInEasing,
                            ),
                        ),
                    )
                    .fillMaxWidth(0.88f)
                    .widthIn(max = 380.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = PopupCardColor),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painterResource(illustrationRes),
                        contentDescription = null,
                        modifier = Modifier.size(110.dp),
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = title,
                        color = Color.White,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = subtitle,
                        color = KuyText,
                        fontFamily = Poppins,
                        fontWeight = FontWeight.Normal,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(20.dp))
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = KuyButton,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(
                            text = "Lanjutkan",
                            fontFamily = Poppins,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KuyPullToRefreshBox(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        contentAlignment = contentAlignment,
        indicator = {
            androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                state = state,
                isRefreshing = isRefreshing,
                containerColor = Color(0xFF16324F),
                color = KuyOrange,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        },
        content = content,
    )
}

