package com.hambalapps.chameleon.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun BandwidthSpeedGraph(
    history: List<Float>,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        if (history.isEmpty()) {
            drawLine(
                color = primaryColor.copy(alpha = 0.2f),
                start = Offset(0f, height),
                end = Offset(width, height),
                strokeWidth = 1.dp.toPx()
            )
            return@Canvas
        }

        val maxVal = maxOf(50f, history.maxOrNull() ?: 50f)
        val path = Path()
        val stepX = width / maxOf(1, history.size - 1)

        val points = history.mapIndexed { idx, value ->
            val x = idx * stepX
            val y = height - (value / maxVal) * height * 0.8f
            Offset(x, y)
        }

        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlX = (p0.x + p1.x) / 2f
                path.cubicTo(
                    x1 = controlX, y1 = p0.y,
                    x2 = controlX, y2 = p1.y,
                    x3 = p1.x, y3 = p1.y
                )
            }
            
            drawPath(
                path = path,
                color = primaryColor,
                style = Stroke(
                    width = 2.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            val fillPath = Path().apply {
                addPath(path)
                lineTo(width, height)
                lineTo(0f, height)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                    startY = 0f,
                    endY = height
                )
            )
        }
    }
}
