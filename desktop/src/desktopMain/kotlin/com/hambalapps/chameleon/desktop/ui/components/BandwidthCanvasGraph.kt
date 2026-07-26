package com.hambalapps.chameleon.desktop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

/**
 * Real-time Bandwidth Speed Graph Canvas rendering upload and download curves.
 */
@Composable
fun BandwidthCanvasGraph(
    uploadHistory: List<Long>,
    downloadHistory: List<Long>,
    modifier: Modifier = Modifier.fillMaxWidth().height(120.dp)
) {
    val upColor = Color(0xFF10B981)
    val downColor = Color(0xFF3B82F6)

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (width <= 0f || height <= 0f) return@Canvas

        val maxVal = maxOf(
            uploadHistory.maxOrNull() ?: 1L,
            downloadHistory.maxOrNull() ?: 1L,
            1024L * 10L
        ).toFloat()

        fun drawSpeedCurve(history: List<Long>, color: Color) {
            if (history.size < 2) return
            val stepX = width / (history.size - 1).coerceAtLeast(1)
            
            val path = Path()
            val fillPath = Path()
            
            history.forEachIndexed { i, valBytes ->
                val x = i * stepX
                val y = height - ((valBytes.toFloat() / maxVal) * height).coerceIn(4f, height - 4f)
                if (i == 0) {
                    path.moveTo(x, y)
                    fillPath.moveTo(x, height)
                    fillPath.lineTo(x, y)
                } else {
                    val prevX = (i - 1) * stepX
                    val prevY = height - ((history[i - 1].toFloat() / maxVal) * height).coerceIn(4f, height - 4f)
                    val controlX1 = prevX + stepX / 2f
                    val controlX2 = prevX + stepX / 2f
                    path.cubicTo(controlX1, prevY, controlX2, y, x, y)
                    fillPath.cubicTo(controlX1, prevY, controlX2, y, x, y)
                }
            }
            fillPath.lineTo(width, height)
            fillPath.close()

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(color.copy(alpha = 0.25f), Color.Transparent)
                )
            )

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 2.5f)
            )
        }

        drawSpeedCurve(downloadHistory, downColor)
        drawSpeedCurve(uploadHistory, upColor)
    }
}
