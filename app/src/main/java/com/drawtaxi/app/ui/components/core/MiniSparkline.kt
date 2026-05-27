package com.drawtaxi.app.ui.components.core

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun MiniSparkline(
    data: List<Double>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF6366F1),
    fillColor: Color = Color(0xFF6366F1).copy(alpha = 0.2f)
) {
    if (data.isEmpty()) return

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height

        val maxVal = (data.maxOrNull() ?: 0.0).coerceAtLeast(1.0)
        val minVal = data.minOrNull() ?: 0.0
        val range = (maxVal - minVal).coerceAtLeast(1.0)

        val points = data.mapIndexed { index, valRaw ->
            val x = if (data.size > 1) index * (width / (data.size - 1)) else width / 2f
            val y = height - ((valRaw - minVal) / range * (height - 8f) + 4f).toFloat()
            Offset(x, y)
        }

        val path = Path().apply {
            if (points.isNotEmpty()) {
                moveTo(points.first().x, points.first().y)
                for (i in 1 until points.size) {
                    val pPrev = points[i - 1]
                    val pCurr = points[i]
                    val controlX = (pPrev.x + pCurr.x) / 2
                    cubicTo(
                        controlX, pPrev.y,
                        controlX, pCurr.y,
                        pCurr.x, pCurr.y
                    )
                }
            }
        }

        // Draw fill gradient first
        val fillPath = Path().apply {
            addPath(path)
            if (points.isNotEmpty()) {
                lineTo(points.last().x, height)
                lineTo(points.first().x, height)
                close()
            }
        }

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(fillColor, Color.Transparent),
                startY = 0f,
                endY = height
            )
        )

        // Draw line
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw last point dot
        if (points.isNotEmpty()) {
            val lastPoint = points.last()
            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = lastPoint
            )
            drawCircle(
                color = lineColor.copy(alpha = 0.3f),
                radius = 7.dp.toPx(),
                center = lastPoint
            )
        }
    }
}
