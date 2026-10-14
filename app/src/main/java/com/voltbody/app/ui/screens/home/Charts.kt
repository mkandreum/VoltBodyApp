package com.voltbody.app.ui.screens.home

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.AppState
import com.voltbody.app.ui.theme.Volt
import java.time.LocalDate

const val WATER_GOAL_ML = 2500
const val STEPS_GOAL = 8000

/** Agua (ml) y pasos de un día, desde DailyMetric (servidor). */
fun AppState.waterOn(d: LocalDate): Int = metrics.firstOrNull { it.date == d.toString() }?.waterMl ?: 0
fun AppState.stepsOn(d: LocalDate): Long = (metrics.firstOrNull { it.date == d.toString() }?.steps ?: 0).toLong()

/** Preferencia del dispositivo: SOLO el flag de conexión con Health Connect. */
object VoltPrefs {
    private fun p(ctx: Context) = ctx.getSharedPreferences("voltbody_prefs", Context.MODE_PRIVATE)
    fun healthEnabled(ctx: Context): Boolean = p(ctx).getBoolean("healthConnectEnabled", false)
    fun setHealthEnabled(ctx: Context, v: Boolean) { p(ctx).edit().putBoolean("healthConnectEnabled", v).apply() }
}

/** Grafica de area suave que se dibuja de izquierda a derecha, con etiquetas opcionales bajo el eje. */
@Composable
fun AreaChart(
    values: List<Float>, labels: List<String> = emptyList(), modifier: Modifier = Modifier.fillMaxWidth().height(110.dp),
    color: Color = Volt.accent,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) { progress.snapTo(0f); progress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
    Column(Modifier.fillMaxWidth()) {
        Canvas(modifier) {
            if (values.isEmpty()) return@Canvas
            val mn = values.min().coerceAtMost(values.max() - 0.0001f); val mx = values.max()
            val range = (mx - mn).coerceAtLeast(0.0001f)
            val n = values.size
            val pts = values.mapIndexed { i, v ->
                Offset(if (n == 1) size.width / 2 else i * size.width / (n - 1), size.height - ((v - mn) / range) * size.height * 0.75f - size.height * 0.12f)
            }
            val line = Path().apply {
                moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) { val m = (pts[i - 1].x + pts[i].x) / 2; cubicTo(m, pts[i - 1].y, m, pts[i].y, pts[i].x, pts[i].y) }
            }
            val fill = Path().apply { addPath(line); lineTo(pts.last().x, size.height); lineTo(pts.first().x, size.height); close() }
            clipRect(right = size.width * progress.value) {
                drawPath(fill, Brush.verticalGradient(listOf(color.copy(alpha = 0.32f), Color.Transparent)))
                drawPath(line, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                pts.forEach { drawCircle(color, 3.5.dp.toPx(), it) }
            }
        }
        if (labels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach { Text(it, color = Volt.textDim, fontSize = 10.sp) }
            }
        }
    }
}
