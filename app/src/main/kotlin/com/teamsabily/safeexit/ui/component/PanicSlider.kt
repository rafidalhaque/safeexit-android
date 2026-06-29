package com.teamsabily.safeexit.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.teamsabily.safeexit.ui.theme.DarkNavy
import com.teamsabily.safeexit.ui.theme.PrimaryRed
import com.teamsabily.safeexit.ui.theme.SecondaryBlue
import com.teamsabily.safeexit.ui.theme.SurfaceLight
import com.teamsabily.safeexit.ui.theme.TextPrimary
import com.teamsabily.safeexit.ui.theme.TextSecondary
import com.teamsabily.safeexit.ui.theme.WarningOrange
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun PanicSlider(
    enabled: Boolean,
    isDeviceOwner: Boolean,
    onTrigger: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current
    val density = LocalDensity.current

    val thumbHeightDp = 50.dp
    val trackWidthDp = 60.dp
    val thumbHeightPx = with(density) { thumbHeightDp.toPx() }

    var trackHeightPx by remember { mutableFloatStateOf(0f) }
    val maxDragDistance = remember(trackHeightPx) { (trackHeightPx - thumbHeightPx).coerceAtLeast(1f) }

    val thumbOffsetY = remember { Animatable(0f) }
    var progress by remember { mutableFloatStateOf(0f) }
    var isInCountdown by remember { mutableStateOf(false) }
    var countdownSeconds by remember { mutableFloatStateOf(2f) }
    var countdownJob by remember { mutableStateOf<Job?>(null) }
    var hasTriggeredHaptic by remember { mutableStateOf(false) }
    var showDisabledTooltip by remember { mutableStateOf(false) }

    // The thumb starts at the bottom. thumbOffsetY=0 means bottom, thumbOffsetY=maxDragDistance means top.
    // But in UI, offset from top: (maxDragDistance - thumbOffsetY.value)
    val uiOffsetY = maxDragDistance - thumbOffsetY.value

    // Progress: 0 = bottom, 1 = top
    LaunchedEffect(thumbOffsetY.value, maxDragDistance) {
        progress = if (maxDragDistance > 0f) (thumbOffsetY.value / maxDragDistance).coerceIn(0f, 1f) else 0f
    }

    // Color interpolation based on progress
    val fillColor = when {
        progress < 0.5f -> lerp(SecondaryBlue, WarningOrange, progress * 2f)
        else -> lerp(WarningOrange, PrimaryRed, (progress - 0.5f) * 2f)
    }

    val glowAlpha = if (progress > 0.7f) ((progress - 0.7f) / 0.3f) * 0.4f else 0f

    val isInteractable = enabled && isDeviceOwner

    Box(
        modifier = modifier
            .width(trackWidthDp)
            .fillMaxHeight()
            .alpha(if (isInteractable) 1f else 0.45f)
            .then(
                if (!isInteractable) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        showDisabledTooltip = true
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        // Track background
        Box(
            modifier = Modifier
                .width(trackWidthDp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(30.dp))
                .background(SurfaceLight.copy(alpha = 0.5f))
                .onSizeChanged { size ->
                    trackHeightPx = size.height.toFloat()
                },
        ) {
            // Filled portion (grows from bottom)
            Canvas(
                modifier = Modifier
                    .width(trackWidthDp)
                    .fillMaxHeight(),
            ) {
                val fillHeight = (progress * this.size.height).coerceAtLeast(0f)
                val cornerRadiusPx = 30.dp.toPx()

                // Glow effect
                if (glowAlpha > 0f) {
                    drawRoundRect(
                        color = fillColor.copy(alpha = glowAlpha),
                        topLeft = Offset(
                            x = -4.dp.toPx(),
                            y = this.size.height - fillHeight - 4.dp.toPx(),
                        ),
                        size = Size(
                            width = this.size.width + 8.dp.toPx(),
                            height = fillHeight + 8.dp.toPx(),
                        ),
                        cornerRadius = CornerRadius(cornerRadiusPx + 4.dp.toPx()),
                    )
                }

                // Fill
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(fillColor, fillColor.copy(alpha = 0.6f)),
                        startY = this.size.height - fillHeight,
                        endY = this.size.height,
                    ),
                    topLeft = Offset(x = 0f, y = this.size.height - fillHeight),
                    size = Size(width = this.size.width, height = fillHeight),
                    cornerRadius = CornerRadius(cornerRadiusPx),
                )
            }

            // Directional hint arrows (subtle)
            if (progress < 0.1f && isInteractable && !isInCountdown) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.4f),
                        modifier = Modifier.size(20.dp),
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.3f),
                        modifier = Modifier.size(20.dp),
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = null,
                        tint = TextSecondary.copy(alpha = 0.2f),
                        modifier = Modifier.size(20.dp),
                    )
                }
            }

            // Countdown text
            if (isInCountdown) {
                Column(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "${countdownSeconds.toInt()}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = PrimaryRed,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Slide\ndown\nto\ncancel",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp,
                    )
                }
            }
        }

        // Thumb
        if (isInteractable) {
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, uiOffsetY.roundToInt()) }
                    .size(width = trackWidthDp, height = thumbHeightDp)
                    .shadow(
                        elevation = 6.dp,
                        shape = RoundedCornerShape(25.dp),
                        ambientColor = fillColor.copy(alpha = 0.3f),
                        spotColor = fillColor.copy(alpha = 0.3f),
                    )
                    .clip(RoundedCornerShape(25.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(fillColor, fillColor.copy(alpha = 0.8f)),
                        )
                    )
                    .pointerInput(isInteractable) {
                        detectVerticalDragGestures(
                            onDragStart = {
                                hasTriggeredHaptic = false
                            },
                            onDragEnd = {
                                if (!isInCountdown) {
                                    // Snap back
                                    scope.launch {
                                        thumbOffsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.6f,
                                                stiffness = 300f,
                                            ),
                                        )
                                    }
                                }
                            },
                            onDragCancel = {
                                if (!isInCountdown) {
                                    scope.launch {
                                        thumbOffsetY.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.6f,
                                                stiffness = 300f,
                                            ),
                                        )
                                    }
                                }
                            },
                            onVerticalDrag = { _, dragAmount ->
                                if (isInCountdown) {
                                    // Drag down to cancel
                                    if (dragAmount > 10f) {
                                        isInCountdown = false
                                        countdownJob?.cancel()
                                        scope.launch {
                                            thumbOffsetY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.6f,
                                                    stiffness = 300f,
                                                ),
                                            )
                                        }
                                    }
                                } else {
                                    // Negative dragAmount = dragging up = increasing thumbOffsetY
                                    val newValue = (thumbOffsetY.value - dragAmount)
                                        .coerceIn(0f, maxDragDistance)
                                    scope.launch {
                                        thumbOffsetY.snapTo(newValue)
                                    }

                                    // Check for 100% progress
                                    val currentProgress = newValue / maxDragDistance
                                    if (currentProgress >= 0.98f && !hasTriggeredHaptic) {
                                        hasTriggeredHaptic = true
                                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                                        isInCountdown = true
                                        countdownSeconds = 2f
                                        countdownJob = scope.launch {
                                            for (i in 2 downTo 1) {
                                                countdownSeconds = i.toFloat()
                                                delay(1000L)
                                            }
                                            // Countdown complete — trigger
                                            isInCountdown = false
                                            onTrigger()
                                            thumbOffsetY.animateTo(
                                                targetValue = 0f,
                                                animationSpec = spring(
                                                    dampingRatio = 0.6f,
                                                    stiffness = 300f,
                                                ),
                                            )
                                        }
                                    }
                                }
                            },
                        )
                    },
                contentAlignment = Alignment.Center,
            ) {
                if (!isInCountdown) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Slide up to trigger",
                        tint = DarkNavy,
                        modifier = Modifier.size(28.dp),
                    )
                }
            }
        }

        // Lock icon for non-device-owner
        if (!isDeviceOwner) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Not device owner",
                    tint = TextSecondary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }

        // Tooltip for disabled state
        if (showDisabledTooltip) {
            LaunchedEffect(showDisabledTooltip) {
                delay(2000L)
                showDisabledTooltip = false
            }

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(8.dp))
                    .background(SurfaceLight)
                    .then(Modifier.width(trackWidthDp)),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = if (!isDeviceOwner) "Not enrolled" else "Select apps first",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .width(trackWidthDp)
                        .background(SurfaceLight, RoundedCornerShape(8.dp))
                        .then(Modifier.size(width = trackWidthDp, height = 40.dp)),
                )
            }
        }
    }
}

/**
 * Linearly interpolate between two colors.
 */
private fun lerp(start: Color, end: Color, fraction: Float): Color {
    val f = fraction.coerceIn(0f, 1f)
    return Color(
        red = start.red + (end.red - start.red) * f,
        green = start.green + (end.green - start.green) * f,
        blue = start.blue + (end.blue - start.blue) * f,
        alpha = start.alpha + (end.alpha - start.alpha) * f,
    )
}
