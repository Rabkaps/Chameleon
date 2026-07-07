package com.hambalapps.chameleon.ui.main

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private class WaveCache(points: Int) {
    val cosAngle = FloatArray(points + 1)
    val sinAngle = FloatArray(points + 1)
    
    val cos2a = FloatArray(points + 1)
    val sin2a = FloatArray(points + 1)
    
    val cos3a = FloatArray(points + 1)
    val sin3a = FloatArray(points + 1)
    
    val cos4a = FloatArray(points + 1)
    val sin4a = FloatArray(points + 1)
    
    val cos5a = FloatArray(points + 1)
    val sin5a = FloatArray(points + 1)
    
    val cos6a = FloatArray(points + 1)
    val sin6a = FloatArray(points + 1)
    
    val cos7a = FloatArray(points + 1)
    val sin7a = FloatArray(points + 1)
    
    val cos11a = FloatArray(points + 1)
    val sin11a = FloatArray(points + 1)
    
    init {
        val step = (2f * Math.PI / points).toFloat()
        for (i in 0..points) {
            val angle = i * step
            cosAngle[i] = kotlin.math.cos(angle)
            sinAngle[i] = kotlin.math.sin(angle)
            
            cos2a[i] = kotlin.math.cos(angle * 2f)
            sin2a[i] = kotlin.math.sin(angle * 2f)
            
            cos3a[i] = kotlin.math.cos(angle * 3f)
            sin3a[i] = kotlin.math.sin(angle * 3f)
            
            cos4a[i] = kotlin.math.cos(angle * 4f)
            sin4a[i] = kotlin.math.sin(angle * 4f)
            
            cos5a[i] = kotlin.math.cos(angle * 5f)
            sin5a[i] = kotlin.math.sin(angle * 5f)
            
            cos6a[i] = kotlin.math.cos(angle * 6f)
            sin6a[i] = kotlin.math.sin(angle * 6f)
            
            cos7a[i] = kotlin.math.cos(angle * 7f)
            sin7a[i] = kotlin.math.sin(angle * 7f)
            
            cos11a[i] = kotlin.math.cos(angle * 11f)
            sin11a[i] = kotlin.math.sin(angle * 11f)
        }
    }
}

@Composable
fun WaveVisualizer(
    state: String,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier
) {
    val isAnimating = state == "CONNECTED" || state == "CONNECTING"
    
    val phase1State = remember { androidx.compose.animation.core.Animatable(0f) }
    val phase2State = remember { androidx.compose.animation.core.Animatable(2f * Math.PI.toFloat()) }
    
    LaunchedEffect(isAnimating) {
        if (isAnimating) {
            launch {
                phase1State.animateTo(
                    targetValue = 2f * Math.PI.toFloat(),
                    animationSpec = infiniteRepeatable(
                        animation = tween(4500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
            launch {
                phase2State.animateTo(
                    targetValue = 0f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(6500, easing = LinearEasing),
                        repeatMode = RepeatMode.Restart
                    )
                )
            }
        } else {
            phase1State.snapTo(0f)
            phase2State.snapTo(2f * Math.PI.toFloat())
        }
    }
    
    val phase1 = phase1State.value
    val phase2 = phase2State.value
    
    val amplitudeMultiplier by animateFloatAsState(
        targetValue = if (state == "CONNECTED" || state == "CONNECTING") 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessVeryLow
        ),
        label = "amplitude"
    )
    
    val scaleFactor by animateFloatAsState(
        targetValue = if (state == "CONNECTED" || state == "CONNECTING") 1f else 0.5f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )
    
    val density = LocalDensity.current
    val baseRadius1Px = remember(density) { density.run { 61.dp.toPx() } }
    val baseRadius2Px = remember(density) { density.run { 73.dp.toPx() } }
    val baseRadius3Px = remember(density) { density.run { 85.dp.toPx() } }
    
    val stroke1Px = remember(density) { density.run { 3.5.dp.toPx() } }
    val stroke2Px = remember(density) { density.run { 2.dp.toPx() } }
    val stroke3Px = remember(density) { density.run { 1.5.dp.toPx() } }
    
    val amp1Px = remember(density) { density.run { 6.dp.toPx() } }
    val amp2Px = remember(density) { density.run { 9.dp.toPx() } }
    val amp3Px = remember(density) { density.run { 7.dp.toPx() } }
    
    val path1 = remember { Path() }
    val path2 = remember { Path() }
    val path3 = remember { Path() }
    
    val waveCache = remember { WaveCache(80) }
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerX = width / 2f
        val centerY = height / 2f
        
        if (amplitudeMultiplier > 0.01f) {
            val cosP1 = kotlin.math.cos(phase1)
            val sinP1 = kotlin.math.sin(phase1)
            val cosP2 = kotlin.math.cos(phase2)
            val sinP2 = kotlin.math.sin(phase2)
            
            val breathing1 = 1f + 0.08f * sinP1
            val breathing2 = 1f + 0.12f * cosP2
            val breathing3 = 1f + 0.15f * sinP1
            
            // Draw Wave 1 (Main Circle)
            path1.reset()
            val points1 = 80
            for (i in 0..points1) {
                val s4 = waveCache.sin4a[i] * cosP1 + waveCache.cos4a[i] * sinP1
                val c6 = waveCache.cos6a[i] * cosP2 + waveCache.sin6a[i] * sinP2
                val s2 = waveCache.sin2a[i] * cosP1 + waveCache.cos2a[i] * sinP1
                
                val waveOffset = s4 * 0.5f + c6 * 0.3f + s2 * 0.2f
                val wave = waveOffset * amp1Px * amplitudeMultiplier * breathing1
                val r = (baseRadius1Px + wave) * scaleFactor
                val x = centerX + r * waveCache.cosAngle[i]
                val y = centerY + r * waveCache.sinAngle[i]
                if (i == 0) {
                    path1.moveTo(x, y)
                } else {
                    path1.lineTo(x, y)
                }
            }
            path1.close()
            drawPath(
                path = path1,
                color = primaryColor.copy(alpha = 0.85f * amplitudeMultiplier),
                style = Stroke(width = stroke1Px)
            )
            
            // Draw Wave 2 (Middle Stripe)
            path2.reset()
            val points2 = 80
            for (i in 0..points2) {
                val s5 = waveCache.sin5a[i] * cosP2 - waveCache.cos5a[i] * sinP2
                val c3 = waveCache.cos3a[i] * cosP1 - waveCache.sin3a[i] * sinP1
                val s7 = waveCache.sin7a[i] * cosP2 - waveCache.cos7a[i] * sinP2
                
                val waveOffset = s5 * 0.6f + c3 * 0.3f + s7 * 0.1f
                val wave = waveOffset * amp2Px * amplitudeMultiplier * breathing2
                val r = (baseRadius2Px + wave) * scaleFactor
                val x = centerX + r * waveCache.cosAngle[i]
                val y = centerY + r * waveCache.sinAngle[i]
                if (i == 0) {
                    path2.moveTo(x, y)
                } else {
                    path2.lineTo(x, y)
                }
            }
            path2.close()
            drawPath(
                path = path2,
                color = secondaryColor.copy(alpha = 0.5f * amplitudeMultiplier),
                style = Stroke(width = stroke2Px)
            )
            
            // Draw Wave 3 (Outer Stripe)
            path3.reset()
            val points3 = 80
            for (i in 0..points3) {
                val c3 = waveCache.cos3a[i] * cosP1 - waveCache.sin3a[i] * sinP1
                val s7 = waveCache.sin7a[i] * cosP2 - waveCache.cos7a[i] * sinP2
                val c11 = waveCache.cos11a[i] * cosP1 - waveCache.sin11a[i] * sinP1
                
                val waveOffset = c3 * 0.5f + s7 * 0.3f + c11 * 0.2f
                val wave = waveOffset * amp3Px * amplitudeMultiplier * breathing3
                val r = (baseRadius3Px + wave) * scaleFactor
                val x = centerX + r * waveCache.cosAngle[i]
                val y = centerY + r * waveCache.sinAngle[i]
                if (i == 0) {
                    path3.moveTo(x, y)
                } else {
                    path3.lineTo(x, y)
                }
            }
            path3.close()
            drawPath(
                path = path3,
                color = primaryColor.copy(alpha = 0.3f * amplitudeMultiplier),
                style = Stroke(width = stroke3Px)
            )
        }
    }
}
