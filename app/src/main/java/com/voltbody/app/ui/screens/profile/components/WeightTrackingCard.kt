package com.voltbody.app.ui.screens.profile.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.domain.model.WeightLog
import com.voltbody.app.ui.components.LiquidGlassCard
import com.voltbody.app.ui.components.LiquidGlassButton
import com.voltbody.app.ui.components.LiquidButtonStyle
import com.voltbody.app.ui.components.GlowText
import com.voltbody.app.ui.components.neuroRaised
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors
import com.voltbody.app.ui.theme.MonoMetric
import dev.chrisbanes.haze.HazeState
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightTrackingCard(
    logs: List<WeightLog>,
    onLogWeight: (Float) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current

    val thisWeekLog = logs.find { 
        runCatching {
            val date = LocalDate.parse(it.date.take(10))
            date.isAfter(LocalDate.now().minusDays(7))
        }.getOrDefault(false)
    }
    
    var weightInput by remember { mutableStateOf("") }

    LiquidGlassCard(modifier = modifier, hazeState = hazeState) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier.size(44.dp).neuroRaised(cornerRadius = 22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.MonitorWeight, contentDescription = null, tint = vb.accent, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        "EVOLUCIÓN DE PESO", 
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ), 
                        color = ColorWhite
                    )
                }
            }

            // Input section
            if (thisWeekLog == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ej: 75.5", color = vb.textMuted) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = vb.accent,
                            unfocusedBorderColor = vb.border,
                            cursorColor = vb.accent,
                            focusedTextColor = ColorWhite,
                            unfocusedTextColor = ColorWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    LiquidGlassButton(
                        text = "LOG",
                        onClick = {
                            weightInput.toFloatOrNull()?.let { onLogWeight(it) }
                            weightInput = ""
                        },
                        enabled = weightInput.isNotBlank(),
                        style = LiquidButtonStyle.Primary,
                        hazeState = hazeState
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(vb.accent.copy(alpha = 0.1f))
                        .border(1.dp, vb.accent.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("PESO REGISTRADO ESTA SEMANA: ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = vb.textMuted)
                    GlowText("${thisWeekLog.weight} KG", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
                }
            }

            // Chart
            val recentLogs = logs.sortedBy { it.date }.takeLast(8)
            if (recentLogs.size >= 2) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(vb.surfaceElevated.copy(alpha = 0.3f))
                        .border(1.dp, ColorWhite.copy(alpha = 0.05f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 24.dp, vertical = 32.dp)
                ) {
                    val weights = recentLogs.map { it.weight }
                    val minW = (weights.minOrNull() ?: 0f) * 0.95f
                    val maxW = (weights.maxOrNull() ?: 0f) * 1.05f
                    val range = if (maxW - minW == 0f) 10f else maxW - minW
                    
                    val pointColor = vb.accent
                    val lineColor = vb.accent.copy(alpha = 0.8f)
                    
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val stepX = w / (weights.size - 1)
                        
                        val points = weights.mapIndexed { index, weight ->
                            val x = index * stepX
                            val y = h - ((weight - minW) / range) * h
                            Offset(x, y)
                        }

                        val path = Path().apply {
                            moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                val p0 = points[i - 1]
                                val p1 = points[i]
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
                                width = 4.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )

                        points.forEach { pt ->
                            drawCircle(color = vb.bg, radius = 6.dp.toPx(), center = pt)
                            drawCircle(color = pointColor, radius = 4.dp.toPx(), center = pt)
                        }
                    }
                }
            }

            // History list
            if (recentLogs.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    recentLogs.reversed().take(5).forEach { log ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(vb.surfaceElevated.copy(alpha = 0.2f))
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                log.date.take(10).uppercase(), 
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), 
                                color = vb.textMuted
                            )
                            GlowText(
                                "${log.weight} KG", 
                                style = MonoMetric.copy(fontSize = 16.sp, fontWeight = FontWeight.Black)
                            )
                        }
                    }
                }
            }
        }
    }
}
