package com.hambalapps.chameleon.ui.main

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.ui.unit.dp

@Composable
fun PeakingKitty(
    modifier: Modifier = Modifier,
    catColor: Color = Color(0xFFFFE5B4), // Peach/Cream cat
    earInnerColor: Color = Color(0xFFFFB7C5) // Pink inner ear
) {
    val outlineColor = MaterialTheme.colorScheme.outline
    Canvas(modifier = modifier.height(36.dp).width(50.dp)) {
        val width = size.width
        val height = size.height

        // Head Base
        val headRadius = width * 0.35f
        val headCenterX = width * 0.5f
        val headCenterY = height * 0.95f

        // Draw Left Ear
        val leftEarPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(headCenterX - headRadius * 0.8f, headCenterY - headRadius * 0.5f)
            lineTo(headCenterX - headRadius * 0.9f, headCenterY - headRadius * 1.3f)
            lineTo(headCenterX - headRadius * 0.2f, headCenterY - headRadius * 0.9f)
        }
        drawPath(leftEarPath, color = catColor)
        drawPath(leftEarPath, color = outlineColor, style = Stroke(width = 1.5.dp.toPx()))

        // Left Ear Inner Pink
        val leftEarInnerPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(headCenterX - headRadius * 0.7f, headCenterY - headRadius * 0.6f)
            lineTo(headCenterX - headRadius * 0.8f, headCenterY - headRadius * 1.15f)
            lineTo(headCenterX - headRadius * 0.35f, headCenterY - headRadius * 0.85f)
        }
        drawPath(leftEarInnerPath, color = earInnerColor)

        // Draw Right Ear
        val rightEarPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(headCenterX + headRadius * 0.2f, headCenterY - headRadius * 0.9f)
            lineTo(headCenterX + headRadius * 0.9f, headCenterY - headRadius * 1.3f)
            lineTo(headCenterX + headRadius * 0.8f, headCenterY - headRadius * 0.5f)
        }
        drawPath(rightEarPath, color = catColor)
        drawPath(rightEarPath, color = outlineColor, style = Stroke(width = 1.5.dp.toPx()))

        // Right Ear Inner Pink
        val rightEarInnerPath = androidx.compose.ui.graphics.Path().apply {
            moveTo(headCenterX + headRadius * 0.35f, headCenterY - headRadius * 0.85f)
            lineTo(headCenterX + headRadius * 0.8f, headCenterY - headRadius * 1.15f)
            lineTo(headCenterX + headRadius * 0.7f, headCenterY - headRadius * 0.6f)
        }
        drawPath(rightEarInnerPath, color = earInnerColor)

        // Draw Head Circle
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

        // Eyes (Closed / Happy Arcs)
        val eyeRadius = headRadius * 0.15f
        val leftEyeCenter = Offset(headCenterX - headRadius * 0.4f, headCenterY - headRadius * 0.1f)
        val rightEyeCenter = Offset(headCenterX + headRadius * 0.4f, headCenterY - headRadius * 0.1f)
        
        drawArc(
            color = outlineColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(leftEyeCenter.x - eyeRadius, leftEyeCenter.y - eyeRadius),
            size = androidx.compose.ui.geometry.Size(eyeRadius * 2, eyeRadius * 2),
            style = Stroke(width = 1.5.dp.toPx())
        )

        drawArc(
            color = outlineColor,
            startAngle = 180f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(rightEyeCenter.x - eyeRadius, rightEyeCenter.y - eyeRadius),
            size = androidx.compose.ui.geometry.Size(eyeRadius * 2, eyeRadius * 2),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Nose (Small triangle)
        val nosePath = androidx.compose.ui.graphics.Path().apply {
            moveTo(headCenterX, headCenterY + headRadius * 0.05f)
            lineTo(headCenterX - headRadius * 0.08f, headCenterY - headRadius * 0.03f)
            lineTo(headCenterX + headRadius * 0.08f, headCenterY - headRadius * 0.03f)
            close()
        }
        drawPath(nosePath, color = Color(0xFFFFB7C5)) // Pink nose
        drawPath(nosePath, color = outlineColor, style = Stroke(width = 1.dp.toPx()))

        // Mouth (W shape)
        val mouthRadius = headRadius * 0.12f
        drawArc(
            color = outlineColor,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(headCenterX - mouthRadius, headCenterY + headRadius * 0.03f),
            size = androidx.compose.ui.geometry.Size(mouthRadius, mouthRadius),
            style = Stroke(width = 1.5.dp.toPx())
        )
        drawArc(
            color = outlineColor,
            startAngle = 0f,
            sweepAngle = 180f,
            useCenter = false,
            topLeft = Offset(headCenterX, headCenterY + headRadius * 0.03f),
            size = androidx.compose.ui.geometry.Size(mouthRadius, mouthRadius),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Paws (peeking over the edge)
        val pawWidth = width * 0.16f
        val pawHeight = height * 0.22f
        // Left Paw
        drawRoundRect(
            color = catColor,
            topLeft = Offset(headCenterX - headRadius * 0.7f, headCenterY - pawHeight),
            size = androidx.compose.ui.geometry.Size(pawWidth, pawHeight * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(headCenterX - headRadius * 0.7f, headCenterY - pawHeight),
            size = androidx.compose.ui.geometry.Size(pawWidth, pawHeight * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight),
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Right Paw
        drawRoundRect(
            color = catColor,
            topLeft = Offset(headCenterX + headRadius * 0.7f - pawWidth, headCenterY - pawHeight),
            size = androidx.compose.ui.geometry.Size(pawWidth, pawHeight * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight)
        )
        drawRoundRect(
            color = outlineColor,
            topLeft = Offset(headCenterX + headRadius * 0.7f - pawWidth, headCenterY - pawHeight),
            size = androidx.compose.ui.geometry.Size(pawWidth, pawHeight * 2),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(pawWidth / 2, pawHeight),
            style = Stroke(width = 1.5.dp.toPx())
        )
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
