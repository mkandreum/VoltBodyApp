package com.voltbody.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.domain.usecase.ExerciseSession
import com.voltbody.app.ui.theme.LocalVoltBodyColors
import com.voltbody.app.ui.theme.MonoMetric
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ExerciseHistoryChart(
    history: List<ExerciseSession>,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current

    if (history.isEmpty()) return

    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Progreso del Peso Máximo",
                style = MaterialTheme.typography.labelMedium,
                color = vb.textMuted
            )
            // PR (Personal Record)
            val pr = history.maxOfOrNull { it.maxWeight } ?: 0f
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("PR: ", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                Text(
                    "${if (pr % 1 == 0f) pr.toInt() else pr} kg",
                    style = MonoMetric.copy(fontSize = 14.sp),
                    color = vb.accent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(vb.surface)
                .padding(horizontal = 16.dp, vertical = 24.dp)
        ) {
            val weights = history.map { it.maxWeight }
            val minW = (weights.minOrNull() ?: 0f) * 0.9f
            val maxW = (weights.maxOrNull() ?: 0f) * 1.1f
            val range = if (maxW - minW == 0f) 10f else maxW - minW
            
            val pointColor = vb.accent
            val lineColor = vb.accent.copy(alpha = 0.6f)
            
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val stepX = if (weights.size > 1) w / (weights.size - 1) else w
                
                val points = weights.mapIndexed { index, weight ->
                    val x = index * stepX
                    val y = h - ((weight - minW) / range) * h
                    Offset(x, y)
                }

                // Draw line
                if (points.size > 1) {
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 1 until points.size) {
                            val p0 = points[i - 1]
                            val p1 = points[i]
                            // Simple curve
                            cubicTo(
                                p0.x + (p1.x - p0.x) / 2f, p0.y,
                                p0.x + (p1.x - p0.x) / 2f, p1.y,
                                p1.x, p1.y
                            )
                        }
                    }
                    drawPath(
                        path = path,
                        color = lineColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Draw points
                points.forEach { pt ->
                    drawCircle(
                        color = vb.surface,
                        radius = 6.dp.toPx(),
                        center = pt
                    )
                    drawCircle(
                        color = pointColor,
                        radius = 4.dp.toPx(),
                        center = pt
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // History List
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            history.reversed().take(5).forEach { session ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(vb.surfaceElevated)
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val dateStr = runCatching {
                        LocalDate.parse(session.date).format(DateTimeFormatter.ofPattern("d MMM"))
                    }.getOrDefault(session.date)
                    
                    Text(dateStr, style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
                    
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${if (session.maxWeight % 1 == 0f) session.maxWeight.toInt() else session.maxWeight} kg",
                            style = MonoMetric.copy(fontSize = 15.sp),
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "${session.maxReps} reps · Vol: ${session.totalVolume.toInt()}",
                            style = MaterialTheme.typography.labelSmall,
                            color = vb.textMuted
                        )
                    }
                }
            }
        }
    }
}
