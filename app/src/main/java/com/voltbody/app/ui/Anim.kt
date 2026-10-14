package com.voltbody.app.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.composed
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.theme.Volt
import kotlinx.coroutines.delay

/** Aparece con fade + subida; usa `index` para escalonar listas. */
fun Modifier.staggeredEntry(index: Int = 0, delayPerItem: Int = 60): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay((index * delayPerItem).toLong()); a.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
    this.graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 40f }
}

/** Efecto de pulsacion (escala) + click. */
fun Modifier.pressScale(onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.95f else 1f, spring(dampingRatio = 0.5f, stiffness = 600f), label = "press")
    this.scale(s).clickable(interactionSource = src, indication = null, onClick = onClick)
}

/** Numero que cuenta desde 0 (o desde el valor anterior) hasta [value]. */
@Composable
fun CountUpText(value: Float, modifier: Modifier = Modifier, decimals: Int = 0, suffix: String = "", style: TextStyle = TextStyle(color = Volt.text, fontSize = 28.sp, fontWeight = FontWeight.Bold)) {
    val v by animateFloatAsState(value, tween(1000, easing = FastOutSlowInEasing), label = "count")
    Text(if (decimals == 0) "${v.toInt()}$suffix" else "${"%.${decimals}f".format(v)}$suffix", modifier, style = style)
}

/** Donut de progreso animado (como el "78% Goals" del mockup). progress 0..1. */
@Composable
fun DonutProgress(progress: Float, modifier: Modifier = Modifier.size(200.dp), stroke: Dp = 22.dp, color: Color = Volt.orange, track: Color = Volt.surface2, center: @Composable () -> Unit = {}) {
    val p by animateFloatAsState(progress.coerceIn(0f, 1f), tween(1400, easing = FastOutSlowInEasing), label = "donut")
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val sw = stroke.toPx()
            val d = size.minDimension - sw
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            drawArc(track, 135f, 270f, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
            drawArc(color, 135f, 270f * p, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
        }
        center()
    }
}


/** Barras animadas (crecen al aparecer); la barra [highlight] usa el acento brillante y el resto una version apagada. */
@Composable
fun AnimatedBars(values: List<Float>, modifier: Modifier = Modifier.fillMaxWidth().height(90.dp), highlight: Int = values.indexOfFirst { it == values.maxOrNull() }) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) { progress.snapTo(0f); progress.animateTo(1f, tween(900, easing = FastOutSlowInEasing)) }
    val max = (values.maxOrNull() ?: 1f).coerceAtLeast(0.0001f)
    val dim = Volt.accent.copy(alpha = 0.35f)
    val on = Volt.accent
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val gap = 6.dp.toPx()
        val w = (size.width - gap * (values.size - 1)) / values.size
        values.forEachIndexed { i, v ->
            val h = (v / max) * size.height * progress.value
            drawRoundRect(if (i == highlight) on else dim, Offset(i * (w + gap), size.height - h), Size(w, h), androidx.compose.ui.geometry.CornerRadius(w / 2, w / 2))
        }
    }
}

/** Sparkline suave animada (linea de agua/peso del mockup). */
@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier.fillMaxWidth().height(40.dp), color: Color = Volt.water) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) { progress.snapTo(0f); progress.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val mn = values.min(); val mx = values.max(); val range = (mx - mn).coerceAtLeast(0.0001f)
        val pts = values.mapIndexed { i, v -> Offset(i * size.width / (values.size - 1), size.height - ((v - mn) / range) * size.height * 0.8f - size.height * 0.1f) }
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) { val m = (pts[i - 1].x + pts[i].x) / 2; cubicTo(m, pts[i - 1].y, m, pts[i].y, pts[i].x, pts[i].y) }
        }
        val measure = androidx.compose.ui.graphics.PathMeasure().apply { setPath(path, false) }
        val out = Path(); measure.getSegment(0f, measure.length * progress.value, out, true)
        drawPath(out, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** Pulso suave infinito (para boton central, iconos activos...). */
@Composable
fun rememberPulse(from: Float = 1f, to: Float = 1.08f, ms: Int = 900): State<Float> {
    val t = rememberInfiniteTransition(label = "pulse")
    return t.animateFloat(from, to, infiniteRepeatable(tween(ms, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "pulseV")
}
