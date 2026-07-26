package com.hambalapps.chameleon.desktop.ui.components

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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Material 3 Expressive Hero Connection Dial Button.
 * Animates glowing pulse waves when connecting/connected.
 */
@Composable
fun HeroConnectionDial(
    vpnState: String,
    onClick: () -> Unit
) {
    val isConnected = vpnState == "CONNECTED"
    val isConnecting = vpnState == "CONNECTING"

    val infiniteTransition = rememberInfiniteTransition(label = "DialPulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1.0f,
        targetValue = if (isConnected || isConnecting) 1.15f else 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = if (isConnected || isConnecting) 0.05f else 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "PulseAlpha"
    )

    val dialColor = when {
        isConnected -> Color(0xFF10B981)
        isConnecting -> Color(0xFFF59E0B)
        else -> MaterialTheme.colorScheme.primary
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scaleState by animateFloatAsState(
        targetValue = if (isPressed) 0.90f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "DialPressSpring"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(160.dp)
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
        // Outer Glowing Pulse Ring
        if (isConnected || isConnecting) {
            Canvas(modifier = Modifier.fillMaxSize().graphicsLayer {
                scaleX = pulseScale
                scaleY = pulseScale
                alpha = pulseAlpha
            }) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(dialColor.copy(alpha = 0.6f), Color.Transparent)
                    )
                )
            }
        }

        // Main Expressive Dial Container
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            dialColor.copy(alpha = 0.9f),
                            dialColor
                        )
                    )
                )
        ) {
            Icon(
                imageVector = Icons.Default.PowerSettingsNew,
                contentDescription = "Toggle VPN",
                tint = Color.White,
                modifier = Modifier.size(54.dp)
            )
        }
    }
}
