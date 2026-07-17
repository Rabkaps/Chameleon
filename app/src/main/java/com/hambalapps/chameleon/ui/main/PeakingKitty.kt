package com.hambalapps.chameleon.ui.main

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch

@Composable
fun PeakingKitty(
    modifier: Modifier = Modifier,
    catColor: Color = Color(0xFFFFE5B4), // Peach/Cream cat
    earInnerColor: Color = Color(0xFFFFB7C5) // Pink inner ear
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    val coroutineScope = rememberCoroutineScope()

    // Animatable variables for tactile bounce physics
    val kittyScaleX = remember { Animatable(1f) }
    val kittyScaleY = remember { Animatable(1f) }
    val earRotation = remember { Animatable(0f) }

    // Floating heart animatable values
    val heartY = remember { Animatable(0f) }
    val heartX = remember { Animatable(0f) }
    val heartAlpha = remember { Animatable(0f) }
    val heartScale = remember { Animatable(0.5f) }

    // Idle blinking loop (300ms blink every 5 seconds, using a low-overhead coroutine delay to avoid continuous recompositions)
    var isBlinking by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        while (true) {
            kotlinx.coroutines.delay(4700)
            isBlinking = true
            kotlinx.coroutines.delay(300)
            isBlinking = false
        }
    }

    val onClick: () -> Unit = {
        coroutineScope.launch {
            // Tactile cartoon squish physics
            launch {
                kittyScaleY.animateTo(0.72f, tween(110, easing = FastOutSlowInEasing))
                kittyScaleY.animateTo(1.12f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
                kittyScaleY.animateTo(0.96f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                kittyScaleY.animateTo(1f, spring(stiffness = Spring.StiffnessLow))
            }
            launch {
                kittyScaleX.animateTo(1.18f, tween(110, easing = FastOutSlowInEasing))
                kittyScaleX.animateTo(0.88f, spring(dampingRatio = Spring.DampingRatioHighBouncy, stiffness = Spring.StiffnessMedium))
                kittyScaleX.animateTo(1.04f, spring(dampingRatio = Spring.DampingRatioMediumBouncy))
                kittyScaleX.animateTo(1f, spring(stiffness = Spring.StiffnessLow))
            }
            // Ear twitch
            launch {
                earRotation.animateTo(12f, tween(80))
                earRotation.animateTo(-12f, tween(100))
                earRotation.animateTo(6f, tween(80))
                earRotation.animateTo(0f, tween(80))
            }
            // Floating drifting heart particle
            launch {
                heartY.snapTo(0f)
                heartX.snapTo(0f)
                heartAlpha.snapTo(1f)
                heartScale.snapTo(0.5f)

                launch {
                    heartY.animateTo(-65f, tween(1100, easing = LinearOutSlowInEasing))
                }
                launch {
                    // Sway left and right
                    heartX.animateTo(10f, tween(550, easing = FastOutSlowInEasing))
                    heartX.animateTo(-6f, tween(550, easing = FastOutSlowInEasing))
                }
                launch {
                    heartAlpha.animateTo(0f, tween(1100, easing = FastOutLinearInEasing))
                }
                launch {
                    heartScale.animateTo(1.25f, tween(1100, easing = LinearOutSlowInEasing))
                }
            }
        }
    }

    Box(
        modifier = modifier
            .wrapContentSize()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
    ) {
        // Floating Heart Overlay
        if (heartAlpha.value > 0f) {
            Text(
                text = "❤️",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset {
                        IntOffset(
                            x = heartX.value.dp.roundToPx(),
                            y = (heartY.value.dp - 24.dp).roundToPx()
                        )
                    }
                    .graphicsLayer {
                        alpha = heartAlpha.value
                        scaleX = heartScale.value
                        scaleY = heartScale.value
                    }
            )
        }

        // Cat Canvas
        Canvas(
            modifier = Modifier
                .height(36.dp)
                .width(50.dp)
        ) {
            scale(
                scaleX = kittyScaleX.value,
                scaleY = kittyScaleY.value,
                pivot = Offset(size.width * 0.5f, size.height)
            ) {
                val width = size.width
                val height = size.height

                // Head Base
                val headRadius = height * 0.55f
                val headCenterX = width * 0.5f
                val headCenterY = height

                // Ears coordinates with dynamic twitches
                val leftEarTipX = headCenterX - headRadius * 0.75f - earRotation.value * 0.25f
                val leftEarTipY = height - headRadius * 1.45f + Math.abs(earRotation.value) * 0.1f
                val rightEarTipX = headCenterX + headRadius * 0.75f + earRotation.value * 0.25f
                val rightEarTipY = height - headRadius * 1.45f + Math.abs(earRotation.value) * 0.1f

                // Draw Left Ear
                val leftEarPath = Path().apply {
                    moveTo(headCenterX - headRadius * 0.85f, height - headRadius * 0.5f)
                    lineTo(leftEarTipX, leftEarTipY)
                    lineTo(headCenterX - headRadius * 0.25f, height - headRadius * 0.85f)
                    close()
                }
                drawPath(leftEarPath, color = catColor)
                drawPath(leftEarPath, color = outlineColor, style = Stroke(width = 1.5.dp.toPx()))

                // Left Ear Inner
                val leftEarInnerPath = Path().apply {
                    moveTo(headCenterX - headRadius * 0.75f, height - headRadius * 0.6f)
                    lineTo(leftEarTipX + 1.dp.toPx(), leftEarTipY + 2.dp.toPx())
                    lineTo(headCenterX - headRadius * 0.35f, height - headRadius * 0.8f)
                    close()
                }
                drawPath(leftEarInnerPath, color = earInnerColor)

                // Draw Right Ear
                val rightEarPath = Path().apply {
                    moveTo(headCenterX + headRadius * 0.25f, height - headRadius * 0.85f)
                    lineTo(rightEarTipX, rightEarTipY)
                    lineTo(headCenterX + headRadius * 0.85f, height - headRadius * 0.5f)
                    close()
                }
                drawPath(rightEarPath, color = catColor)
                drawPath(rightEarPath, color = outlineColor, style = Stroke(width = 1.5.dp.toPx()))

                // Right Ear Inner
                val rightEarInnerPath = Path().apply {
                    moveTo(headCenterX + headRadius * 0.35f, height - headRadius * 0.8f)
                    lineTo(rightEarTipX - 1.dp.toPx(), rightEarTipY + 2.dp.toPx())
                    lineTo(headCenterX + headRadius * 0.75f, height - headRadius * 0.6f)
                    close()
                }
                drawPath(rightEarInnerPath, color = earInnerColor)

                // Head Circle (drawn as a semi-circle from 180 degrees to 360/0 degrees)
                drawArc(
                    color = catColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(headCenterX - headRadius, headCenterY - headRadius),
                    size = Size(headRadius * 2, headRadius * 2)
                )
                // Head outline (only the top curve, so it blends into the bottom edge)
                drawArc(
                    color = outlineColor,
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(headCenterX - headRadius, headCenterY - headRadius),
                    size = Size(headRadius * 2, headRadius * 2),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Face coordinates
                val eyeY = height - headRadius * 0.45f
                val leftEyeX = headCenterX - headRadius * 0.35f
                val rightEyeX = headCenterX + headRadius * 0.35f
                val eyeWidth = headRadius * 0.16f
                val eyeHeight = headRadius * 0.24f
                val eyeRadius = 3.dp.toPx()

                // Eyes: Open (anime style with shines) or Blink
                if (isBlinking) {
                    // Draw horizontal curved lines representing a blink
                    drawArc(
                        color = outlineColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(leftEyeX - eyeWidth / 2, eyeY - eyeHeight / 4),
                        size = Size(eyeWidth, eyeHeight / 2),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                    drawArc(
                        color = outlineColor,
                        startAngle = 180f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(rightEyeX - eyeWidth / 2, eyeY - eyeHeight / 4),
                        size = Size(eyeWidth, eyeHeight / 2),
                        style = Stroke(width = 1.5.dp.toPx())
                    )
                } else {
                    // Open Anime Eyes (filled ovals + white shines)
                    drawOval(
                        color = outlineColor,
                        topLeft = Offset(leftEyeX - eyeWidth / 2, eyeY - eyeHeight / 2),
                        size = Size(eyeWidth, eyeHeight)
                    )
                    drawOval(
                        color = outlineColor,
                        topLeft = Offset(rightEyeX - eyeWidth / 2, eyeY - eyeHeight / 2),
                        size = Size(eyeWidth, eyeHeight)
                    )

                    // Highlights (Eye shines)
                    val shineRadius = eyeWidth * 0.25f
                    val leftShineCenter = Offset(leftEyeX + eyeWidth * 0.18f, eyeY - eyeHeight * 0.18f)
                    val rightShineCenter = Offset(rightEyeX + eyeWidth * 0.18f, eyeY - eyeHeight * 0.18f)
                    drawCircle(
                        color = Color.White,
                        radius = shineRadius,
                        center = leftShineCenter
                    )
                    drawCircle(
                        color = Color.White,
                        radius = shineRadius,
                        center = rightShineCenter
                    )
                }

                // Draw Blush (two pink circles under eyes)
                drawCircle(
                    color = earInnerColor.copy(alpha = 0.6f),
                    radius = eyeRadius * 1.5f,
                    center = Offset(leftEyeX - 2.dp.toPx(), eyeY + 4.dp.toPx())
                )
                drawCircle(
                    color = earInnerColor.copy(alpha = 0.6f),
                    radius = eyeRadius * 1.5f,
                    center = Offset(rightEyeX + 2.dp.toPx(), eyeY + 4.dp.toPx())
                )

                // Draw Nose (small pink triangle)
                val nosePath = Path().apply {
                    moveTo(headCenterX - 2.dp.toPx(), headCenterY - headRadius * 0.3f)
                    lineTo(headCenterX + 2.dp.toPx(), headCenterY - headRadius * 0.3f)
                    lineTo(headCenterX, headCenterY - headRadius * 0.23f)
                    close()
                }
                drawPath(nosePath, color = earInnerColor)
                drawPath(nosePath, color = outlineColor, style = Stroke(width = 1.dp.toPx()))

                // Draw Mouth (two small curves w using quadraticTo)
                val mouthY = headCenterY - headRadius * 0.2f
                val mouthPath = Path().apply {
                    moveTo(headCenterX - 4.dp.toPx(), mouthY)
                    quadraticTo(headCenterX - 2.dp.toPx(), mouthY + 2.dp.toPx(), headCenterX, mouthY)
                    quadraticTo(headCenterX + 2.dp.toPx(), mouthY + 2.dp.toPx(), headCenterX + 4.dp.toPx(), mouthY)
                }
                drawPath(mouthPath, color = outlineColor, style = Stroke(width = 1.5.dp.toPx()))

                // Draw Whiskers (two lines on each side)
                drawLine(
                    color = outlineColor,
                    start = Offset(headCenterX - headRadius * 0.6f, headCenterY - headRadius * 0.25f),
                    end = Offset(headCenterX - headRadius * 1.1f, headCenterY - headRadius * 0.3f),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = outlineColor,
                    start = Offset(headCenterX - headRadius * 0.6f, headCenterY - headRadius * 0.15f),
                    end = Offset(headCenterX - headRadius * 1.1f, headCenterY - headRadius * 0.15f),
                    strokeWidth = 1.5.dp.toPx()
                )

                drawLine(
                    color = outlineColor,
                    start = Offset(headCenterX + headRadius * 0.6f, headCenterY - headRadius * 0.25f),
                    end = Offset(headCenterX + headRadius * 1.1f, headCenterY - headRadius * 0.3f),
                    strokeWidth = 1.5.dp.toPx()
                )
                drawLine(
                    color = outlineColor,
                    start = Offset(headCenterX + headRadius * 0.6f, headCenterY - headRadius * 0.15f),
                    end = Offset(headCenterX + headRadius * 1.1f, headCenterY - headRadius * 0.15f),
                    strokeWidth = 1.5.dp.toPx()
                )

                // Draw Paws (at the bottom, peeking over edge)
                val pawWidth = 8.dp.toPx()
                val pawHeight = 6.dp.toPx()

                // Left Paw
                drawRoundRect(
                    color = catColor,
                    topLeft = Offset(headCenterX - headRadius * 0.7f, headCenterY - pawHeight),
                    size = Size(pawWidth, pawHeight * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight)
                )
                drawRoundRect(
                    color = outlineColor,
                    topLeft = Offset(headCenterX - headRadius * 0.7f, headCenterY - pawHeight),
                    size = Size(pawWidth, pawHeight * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Right Paw
                drawRoundRect(
                    color = catColor,
                    topLeft = Offset(headCenterX + headRadius * 0.7f - pawWidth, headCenterY - pawHeight),
                    size = Size(pawWidth, pawHeight * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight)
                )
                drawRoundRect(
                    color = outlineColor,
                    topLeft = Offset(headCenterX + headRadius * 0.7f - pawWidth, headCenterY - pawHeight),
                    size = Size(pawWidth, pawHeight * 2),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight),
                    style = Stroke(width = 1.5.dp.toPx())
                )
            }
        }
    }
}

@Composable
fun PawPrint(
    modifier: Modifier = Modifier,
    color: Color
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val padRadius = width * 0.28f
        val padCenterX = width / 2f
        val padCenterY = height * 0.6f

        // Main pad
        drawCircle(color = color, radius = padRadius, center = Offset(padCenterX, padCenterY))

        // 4 toes
        val toeRadius = width * 0.12f
        // Leftmost toe
        drawCircle(color = color, radius = toeRadius, center = Offset(padCenterX - padRadius * 1.2f, padCenterY - padRadius * 0.8f))
        // Middle left toe
        drawCircle(color = color, radius = toeRadius, center = Offset(padCenterX - padRadius * 0.4f, padCenterY - padRadius * 1.5f))
        // Middle right toe
        drawCircle(color = color, radius = toeRadius, center = Offset(padCenterX + padRadius * 0.4f, padCenterY - padRadius * 1.5f))
        // Rightmost toe
        drawCircle(color = color, radius = toeRadius, center = Offset(padCenterX + padRadius * 1.2f, padCenterY - padRadius * 0.8f))
    }
}
