package com.voltbody.app.ui.screens.diet

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.CountUpText
import com.voltbody.app.ui.theme.Volt

val ProteinColor = Color(0xFFF87171)
val CarbsColor = Color(0xFFFBBF24)
val FatColor = Color(0xFF38BDF8)

private data class RingData(val consumed: Int, val target: Int, val color: Color, val label: String, val unit: String)

@Composable
private fun animatedFraction(consumed: Int, target: Int): Float {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val goal = (consumed.toFloat() / target.coerceAtLeast(1)).coerceIn(0f, 1f)
    return animateFloatAsState(if (started) goal else 0f, tween(1200, easing = FastOutSlowInEasing), label = "ring").value
}

/** Anillos concentricos (kcal, proteina, carbos, grasas) con leyenda numerica, como NutriRings de la web. */
@Composable
fun NutriRings(
    consumedKcal: Int, targetKcal: Int, consumedProtein: Int, targetProtein: Int,
    consumedCarbs: Int, targetCarbs: Int, consumedFat: Int, targetFat: Int,
    modifier: Modifier = Modifier,
) {
    val rings = listOf(
        RingData(consumedKcal, targetKcal, Volt.accent, "Calorías", "kcal"),
        RingData(consumedProtein, targetProtein, ProteinColor, "Proteínas", "g"),
        RingData(consumedCarbs, targetCarbs, CarbsColor, "Carbohidratos", "g"),
        RingData(consumedFat, targetFat, FatColor, "Grasas", "g"),
    )
    val fractions = rings.map { animatedFraction(it.consumed, it.target) }
    val remaining = (targetKcal - consumedKcal).coerceAtLeast(0)

    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Volt.surface2.copy(alpha = 0.6f)).padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.matchParentSize()) {
                val sw = 9.dp.toPx()
                val step = sw + 5.dp.toPx()
                rings.forEachIndexed { i, r ->
                    val inset = i * step + sw / 2
                    val d = size.minDimension - inset * 2
                    if (d <= 0f) return@forEachIndexed
                    val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
                    drawArc(r.color.copy(alpha = 0.16f), 0f, 360f, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
                    val sweep = 360f * fractions[i]
                    if (sweep > 0.5f) drawArc(r.color, -90f, sweep, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("RESTAN", color = Volt.textDim, fontSize = 9.sp, letterSpacing = 1.sp)
                CountUpText(remaining.toFloat(), style = TextStyle(color = Volt.text, fontSize = 22.sp, fontWeight = FontWeight.Black))
                Text("kcal", color = Volt.textDim, fontSize = 10.sp)
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            rings.forEach { r ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(9.dp).clip(CircleShape).background(r.color))
                    Spacer(Modifier.width(8.dp))
                    Text(r.label, color = Volt.text, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text(
                        if (r.unit == "kcal") "${r.consumed}" else "${r.consumed}g",
                        color = r.color, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                    )
                    Text(
                        if (r.unit == "kcal") " / ${r.target}" else " / ${r.target}g",
                        color = Volt.textDim, fontSize = 11.sp,
                    )
                }
            }
        }
    }
}

/** Tarjeta de macro con barra animada. */
@Composable
fun MacroCard(label: String, consumed: Int, target: Int, color: Color, modifier: Modifier = Modifier) {
    val frac = animatedFraction(consumed, target)
    Column(
        modifier.clip(RoundedCornerShape(20.dp)).background(Volt.surface).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(label.uppercase(), color = color, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.8.sp)
        Row(verticalAlignment = Alignment.Bottom) {
            Text("${consumed}g", color = Volt.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Text(" / ${target}g", color = Volt.textDim, fontSize = 11.sp)
        }
        Box(Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.Black.copy(alpha = 0.4f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}
