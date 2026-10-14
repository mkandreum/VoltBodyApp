package com.voltbody.app.ui.screens.diet

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.CountUpText
import com.voltbody.app.ui.EaseOutExpo
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import kotlinx.coroutines.delay

val ProteinColor = Color(0xFFFF6B7A)
val CarbsColor = Color(0xFFFFC24D)
val FatColor = Color(0xFF5B9BFF)

private class RingData(val consumed: Int, val target: Int, val color: Color, val label: String, val unit: String)

/** Anillos concentricos (kcal, proteina, carbos, grasas) que se dibujan al entrar; valores animados leidos solo en fase draw. */
@Composable
fun NutriRings(
    consumedKcal: Int, targetKcal: Int, consumedProtein: Int, targetProtein: Int,
    consumedCarbs: Int, targetCarbs: Int, consumedFat: Int, targetFat: Int,
    modifier: Modifier = Modifier,
) {
    val accent = Volt.accent
    val accent2 = Volt.accent2
    val rings = listOf(
        RingData(consumedKcal, targetKcal, accent, "Calorías", "kcal"),
        RingData(consumedProtein, targetProtein, ProteinColor, "Proteínas", "g"),
        RingData(consumedCarbs, targetCarbs, CarbsColor, "Carbohidratos", "g"),
        RingData(consumedFat, targetFat, FatColor, "Grasas", "g"),
    )
    val anims = remember { List(4) { Animatable(0f) } }
    rings.forEachIndexed { i, r ->
        val goal = (r.consumed.toFloat() / r.target.coerceAtLeast(1)).coerceIn(0f, 1f)
        LaunchedEffect(goal) {
            delay(i * 90L)
            anims[i].animateTo(goal, tween(1100, easing = EaseOutExpo))
        }
    }
    val remaining = (targetKcal - consumedKcal).coerceAtLeast(0)

    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(156.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val sw = 10.dp.toPx()
                val step = sw + 6.dp.toPx()
                rings.forEachIndexed { i, r ->
                    val inset = i * step + sw / 2
                    val d = size.minDimension - inset * 2
                    if (d <= 0f) return@forEachIndexed
                    val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
                    val sz = Size(d, d)
                    drawArc(r.color.copy(alpha = 0.14f), 0f, 360f, false, tl, sz, style = Stroke(sw))
                    val sweep = 360f * anims[i].value
                    if (sweep > 0.5f) {
                        rotate(-90f) {
                            if (i == 0) {
                                drawArc(Brush.sweepGradient(listOf(accent, accent2, accent)), 0f, sweep, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                            } else {
                                drawArc(r.color, 0f, sweep, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                            }
                        }
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RESTAN", color = Volt.textDim, style = VoltType.label)
                CountUpText(remaining.toFloat(), style = VoltType.numberM.copy(color = Volt.text))
                Text("kcal", color = Volt.textDim, style = VoltType.body)
            }
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            rings.forEach { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(10.dp).clip(CircleShape).background(r.color))
                    Spacer(Modifier.width(8.dp))
                    Text(r.label, color = Volt.text, style = VoltType.body, modifier = Modifier.weight(1f), maxLines = 1)
                    Text(if (r.unit == "kcal") "${r.consumed}" else "${r.consumed}g", color = r.color, style = VoltType.numberS.copy(fontSize = 14.sp))
                    Text(if (r.unit == "kcal") "/${r.target}" else "/${r.target}g", color = Volt.textDim, style = VoltType.body)
                }
            }
        }
    }
}

/** Tarjeta de macro con barra que se dibuja (lectura animada en draw). */
@Composable
fun MacroCard(label: String, consumed: Int, target: Int, color: Color, modifier: Modifier = Modifier) {
    val goal = (consumed.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    val a = remember { Animatable(0f) }
    LaunchedEffect(goal) { a.animateTo(goal, tween(900, easing = EaseOutExpo)) }
    Column(
        modifier.clip(RoundedCornerShape(24.dp)).background(Volt.surface).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label.uppercase(), color = color, style = VoltType.label, maxLines = 1)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$consumed", color = Volt.text, style = VoltType.numberM)
            Text("/${target}g", color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(start = 3.dp, bottom = 3.dp))
        }
        Box(
            Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).drawBehind {
                drawRect(color.copy(alpha = 0.16f))
                drawRoundRect(color, Offset.Zero, Size(size.width * a.value, size.height), CornerRadius(size.height / 2))
            },
        )
    }
}
