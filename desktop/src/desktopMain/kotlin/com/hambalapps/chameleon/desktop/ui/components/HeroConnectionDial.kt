package com.hambalapps.chameleon.desktop.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Material 3 Expressive Hero Connection Dial Button.
 * Multi-ring glowing fluid wave animations with spring feedback.
 */
@Composable
fun HeroConnectionDial(
    vpnState: String,
    onClick: () -> Unit
) {
    val isConnected = vpnState == "CONNECTED"
    val isConnecting = vpnState == "CONNECTING"
    val isDisconnecting = vpnState == "DISCONNECTING"

    val infiniteTransition = rememberInfiniteTransition(label = "DialInfiniteAnimation")

    val pulseScale1 by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isConnected || isConnecting) 1.25f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale1"
    )

    val pulseAlpha1 by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = if (isConnected || isConnecting) 0.05f else 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha1"
    )

    val pulseScale2 by infiniteTransition.animateFloat(
        initialValue = 1.05f,
        targetValue = if (isConnected || isConnecting) 1.45f else 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale2"
    )

    val pulseAlpha2 by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (isConnected || isConnecting) 0.0f else 0.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2400, delayMillis = 400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha2"
    )

    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(if (isConnecting) 2500 else 8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DialRotation"
    )

    val targetDialColor = when {
        isConnected -> Color(0xFF10B981) // Emerald
        isConnecting -> Color(0xFFF59E0B) // Amber
        isDisconnecting -> Color(0xFFEF4444) // Rose Red
        else -> MaterialTheme.colorScheme.primary
    }

    val dialColor by animateColorAsState(
        targetValue = targetDialColor,
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "DialColorAnim"
    )

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleState by animateFloatAsState(
        targetValue = if (isPressed) 0.88f else 1.0f,
        animationSpec = spring(
            dampingRatio = 0.55f, // M3 Expressive spatial bounce
            stiffness = Spring.StiffnessLow
        ),
        label = "DialPressSpring"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(190.dp)
            .graphicsLayer {
                scaleX = scaleState
                scaleY = scaleState
            }
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        // Outer Secondary Ambient Glow Ring
        if (isConnected || isConnecting) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pulseScale2
                        scaleY = pulseScale2
                        alpha = pulseAlpha2
                    }
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(dialColor.copy(alpha = 0.45f), Color.Transparent)
                    )
                )
            }
        }

        // Inner Pulsing Wave Ring
        if (isConnected || isConnecting) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        scaleX = pulseScale1
                        scaleY = pulseScale1
                        alpha = pulseAlpha1
                    }
            ) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(dialColor.copy(alpha = 0.65f), Color.Transparent)
                    )
                )
            }
        }

        // Subtle Orbiting Dashed Track Ring
        Canvas(
            modifier = Modifier
                .size(148.dp)
                .graphicsLayer { rotationZ = rotationAngle }
        ) {
            drawCircle(
                color = dialColor.copy(alpha = if (isConnected || isConnecting) 0.7f else 0.25f),
                style = Stroke(
                    width = 2.5f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f),
                    cap = StrokeCap.Round
                )
            )
        }

        // Main Expressive Tactile Dial Circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(118.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            dialColor.copy(alpha = 0.95f),
                            dialColor
                        )
                    )
                )
        ) {
            // Inner Depth Shadow / Glow
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.22f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Toggle VPN",
                tint = Color.White,
                modifier = Modifier.size(52.dp)
            )
        }
    }
}
