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

    // Idle blinking loop (300ms blink every 5 seconds)
    val infiniteTransition = rememberInfiniteTransition(label = "KittyBlink")
    val blinkProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 5000
                0f at 0
                0f at 4700
                1f at 4850
                0f at 5000
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "BlinkProgress"
    )

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
                val headRadius = height * 0.52f
                val headCenterX = width * 0.5f
                val headCenterY = height

                // Ears coordinates with dynamic twitches
                val leftEarTipX = headCenterX - headRadius * 0.75f - earRotation.value * 0.25f
                val leftEarTipY = height - headRadius * 1.45f + Math.abs(earRotation.value) * 0.1f
                val rightEarTipX = headCenterX + headRadius * 0.75f + earRotation.value * 0.25f
                val rightEarTipY = height - headRadius * 1.45f + Math.abs(earRotation.value) * 0.1f

                // Draw Left Ear
                val leftEarPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(headCenterX - headRadius * 0.85f, height - headRadius * 0.5f)
                    lineTo(leftEarTipX, leftEarTipY)
                    lineTo(headCenterX - headRadius * 0.25f, height - headRadius * 0.85f)
                }
                drawPath(leftEarPath, color = catColor)
                drawPath(leftEarPath, color = outlineColor, style = Stroke(width = 1.5.dp.toPx()))

                // Left Ear Inner
                val leftEarInnerPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(headCenterX - headRadius * 0.75f, height - headRadius * 0.6f)
                    lineTo(leftEarTipX + 1.dp.toPx(), leftEarTipY + 2.dp.toPx())
                    lineTo(headCenterX - headRadius * 0.35f, height - headRadius * 0.8f)
                }
                drawPath(leftEarInnerPath, color = earInnerColor)

                // Draw Right Ear
                val rightEarPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(headCenterX + headRadius * 0.25f, height - headRadius * 0.85f)
                    lineTo(rightEarTipX, rightEarTipY)
                    lineTo(headCenterX + headRadius * 0.85f, height - headRadius * 0.5f)
                }
                drawPath(rightEarPath, color = catColor)
                drawPath(rightEarPath, color = outlineColor, style = Stroke(width = 1.5.dp.toPx()))

                // Right Ear Inner
                val rightEarInnerPath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(headCenterX + headRadius * 0.35f, height - headRadius * 0.8f)
                    lineTo(rightEarTipX - 1.dp.toPx(), rightEarTipY + 2.dp.toPx())
                    lineTo(headCenterX + headRadius * 0.75f, height - headRadius * 0.6f)
                }
                drawPath(rightEarInnerPath, color = earInnerColor)

                // Head Circle
                drawCircle(
                    color = catColor,
                    radius = headRadius,
                    center = Offset(headCenterX, headCenterY)
                )
                drawCircle(
                    color = outlineColor,
                    radius = headRadius,
                    center = Offset(headCenterX, headCenterY),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Face coordinates
                val eyeY = height - headRadius * 0.55f
                val leftEyeX = headCenterX - headRadius * 0.36f
                val rightEyeX = headCenterX + headRadius * 0.36f
                val eyeWidth = headRadius * 0.16f
                val eyeHeight = headRadius * 0.24f

                // Eyes: Open (anime style with shines) or Blink
                if (blinkProgress > 0.94f) {
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

                // Cute Nose (pink triangle)
                val noseY = height - headRadius * 0.38f
                val noseWidth = headRadius * 0.14f
                val noseHeight = headRadius * 0.08f
                val nosePath = androidx.compose.ui.graphics.Path().apply {
                    moveTo(headCenterX, noseY + noseHeight / 2)
                    lineTo(headCenterX - noseWidth / 2, noseY - noseHeight / 2)
                    lineTo(headCenterX + noseWidth / 2, noseY - noseHeight / 2)
                    close()
                }
                drawPath(nosePath, color = Color(0xFFFFB7C5))
                drawPath(nosePath, color = outlineColor, style = Stroke(width = 1.dp.toPx()))

                // Mouth (W curve shape)
                val mouthY = height - headRadius * 0.30f
                val mouthRadius = headRadius * 0.10f
                drawArc(
                    color = outlineColor,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(headCenterX - mouthRadius, mouthY),
                    size = Size(mouthRadius, mouthRadius),
                    style = Stroke(width = 1.5.dp.toPx())
                )
                drawArc(
                    color = outlineColor,
                    startAngle = 0f,
                    sweepAngle = 180f,
                    useCenter = false,
                    topLeft = Offset(headCenterX, mouthY),
                    size = Size(mouthRadius, mouthRadius),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Paws (at the bottom, peeking over edge)
                val pawWidth = width * 0.16f
                val pawHeight = height * 0.22f

                // Left Paw
                drawRoundRect(
                    color = catColor,
                    topLeft = Offset(headCenterX - headRadius * 0.68f, height - pawHeight * 0.7f),
                    size = Size(pawWidth, pawHeight * 1.4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight * 0.7f)
                )
                drawRoundRect(
                    color = outlineColor,
                    topLeft = Offset(headCenterX - headRadius * 0.68f, height - pawHeight * 0.7f),
                    size = Size(pawWidth, pawHeight * 1.4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight * 0.7f),
                    style = Stroke(width = 1.5.dp.toPx())
                )

                // Right Paw
                drawRoundRect(
                    color = catColor,
                    topLeft = Offset(headCenterX + headRadius * 0.68f - pawWidth, height - pawHeight * 0.7f),
                    size = Size(pawWidth, pawHeight * 1.4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight * 0.7f)
                )
                drawRoundRect(
                    color = outlineColor,
                    topLeft = Offset(headCenterX + headRadius * 0.68f - pawWidth, height - pawHeight * 0.7f),
                    size = Size(pawWidth, pawHeight * 1.4f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight * 0.7f),
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

        val padRadius = width * 0.25f
        val padCenterX = width * 0.5f
        val padCenterY = height * 0.6f

        // Main pad
        drawCircle(color = color, radius = padRadius, center = Offset(padCenterX, padCenterY))

        // 4 toes
        val toeRadius = padRadius * 0.35f
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
