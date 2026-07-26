package com.hambalapps.chameleon.desktop.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Material 3 Expressive Shapes
val ExpressiveCardShape = RoundedCornerShape(topStart = 28.dp, bottomEnd = 28.dp, topEnd = 10.dp, bottomStart = 10.dp)
val ExpressiveButtonShape = RoundedCornerShape(topStart = 16.dp, bottomEnd = 16.dp, topEnd = 6.dp, bottomStart = 6.dp)
val ExpressiveChipShape = RoundedCornerShape(topStart = 10.dp, bottomEnd = 10.dp, topEnd = 4.dp, bottomStart = 4.dp)
val ExpressivePillShape = CircleShape

/**
 * Spring press scale effect for Material 3 Expressive feel.
 */
fun Modifier.expressiveSpringPress(
    targetScale: Float = 0.94f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ExpressiveSpringScale"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .then(
            if (onClick != null) {
                Modifier.clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onClick
                )
            } else Modifier
        )
}

/**
 * Premium Expressive Glass Card container respecting card styles.
 */
@Composable
fun ExpressiveGlassCard(
    modifier: Modifier = Modifier,
    cardStyle: String = "glass",
    shape: RoundedCornerShape = ExpressiveCardShape,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    
    val containerBrush = remember(cardStyle, colorScheme) {
        when (cardStyle) {
            "vibrant" -> Brush.linearGradient(
                colors = listOf(
                    colorScheme.primaryContainer.copy(alpha = 0.95f),
                    colorScheme.secondaryContainer.copy(alpha = 0.85f)
                )
            )
            "solid" -> Brush.linearGradient(
                colors = listOf(
                    colorScheme.surfaceContainerHigh,
                    colorScheme.surfaceContainerHigh
                )
            )
            "tonal" -> Brush.linearGradient(
                colors = listOf(
                    colorScheme.surfaceContainerLow,
                    colorScheme.surfaceContainer
                )
            )
            else -> Brush.linearGradient( // Glass
                colors = listOf(
                    colorScheme.surfaceVariant.copy(alpha = 0.40f),
                    colorScheme.surfaceVariant.copy(alpha = 0.15f)
                )
            )
        }
    }

    val borderBrush = remember(cardStyle, colorScheme) {
        Brush.linearGradient(
            colors = listOf(
                colorScheme.outline.copy(alpha = 0.35f),
                colorScheme.outline.copy(alpha = 0.10f)
            )
        )
    }

    Box(
        modifier = modifier
            .clip(shape)
            .background(brush = containerBrush, shape = shape)
            .border(width = 1.dp, brush = borderBrush, shape = shape)
            .then(
                if (onClick != null) Modifier.expressiveSpringPress(onClick = onClick)
                else Modifier
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            content = content
        )
    }
}

/**
 * Expressive Status Badge (Connected / Connecting / Disconnected).
 */
@Composable
fun ExpressiveStatusBadge(
    status: String
) {
    val (badgeColor, statusText) = when (status) {
        "CONNECTED" -> Color(0xFF10B981) to "CONNECTED"
        "CONNECTING" -> Color(0xFFF59E0B) to "CONNECTING..."
        "DISCONNECTING" -> Color(0xFFEF4444) to "DISCONNECTING..."
        else -> Color(0xFF6B7280) to "DISCONNECTED"
    }

    Surface(
        color = badgeColor.copy(alpha = 0.15f),
        shape = ExpressiveChipShape,
        border = androidx.compose.foundation.BorderStroke(1.dp, badgeColor.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(badgeColor)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = statusText,
                color = badgeColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}
