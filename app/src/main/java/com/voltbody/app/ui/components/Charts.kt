package com.voltbody.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.voltbody.app.ui.theme.LocalVoltBodyColors

@Composable
fun VoltBodyAreaChart(
    data: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color? = null,
    fillColor: Color? = null
) {
    val vb = LocalVoltBodyColors.current
    val strokeColor = lineColor ?: vb.accent
    val areaColor = fillColor ?: vb.accent.copy(alpha = 0.15f)

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(1f, tween(1500, easing = FastOutSlowInEasing))
    }

    Canvas(modifier = modifier) {
        if (data.size < 2) return@Canvas

        val width = size.width
        val height = size.height
        val maxVal = (data.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val stepX = width / (data.size - 1)

        val points = data.mapIndexed { index, value ->
            Offset(
                x = index * stepX,
                y = height - (value / maxVal * height * animationProgress.value)
            )
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            points.forEachIndexed { index, offset ->
                if (index > 0) {
                    val prev = points[index - 1]
                    // Bezier curve for smoothness
                    cubicTo(
                        prev.x + stepX / 2f, prev.y,
                        offset.x - stepX / 2f, offset.y,
                        offset.x, offset.y
                    )
                }
            }
        }

        val fillPath = Path().apply {
            addPath(path)
            lineTo(width, height)
            lineTo(0f, height)
            close()
        }

        // Draw Fill
        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(areaColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw Line
        drawPath(
            path = path,
            color = strokeColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        // Draw glow on top of line
        drawPath(
            path = path,
            color = strokeColor.copy(alpha = 0.3f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
    }
}
