package com.voltbody.app.ui.screens.home

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.unit.dp
import com.voltbody.app.data.AppState
import com.voltbody.app.ui.EaseOutExpo
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
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

/**
 * Grafica de area suave que se dibuja de izquierda a derecha (fase draw, geometria cacheada),
 * con linea en degradado de marca y etiquetas opcionales bajo el eje.
 */
@Composable
fun AreaChart(
    values: List<Float>, labels: List<String> = emptyList(), modifier: Modifier = Modifier.fillMaxWidth().height(110.dp),
    color: Color = Volt.accent,
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) { progress.snapTo(0f); progress.animateTo(1f, tween(1000, easing = EaseOutExpo)) }
    val c2 = Volt.accent2
    Column(Modifier.fillMaxWidth()) {
        Spacer(
            modifier.drawWithCache {
                if (values.isEmpty() || size.width <= 0f) return@drawWithCache onDrawBehind { }
                val mn = values.min().coerceAtMost(values.max() - 0.0001f)
                val mx = values.max()
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
                val fillBrush = Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), Color.Transparent))
                val lineBrush = Brush.horizontalGradient(listOf(color, c2))
                val sw = 3.dp.toPx()
                val showDots = n <= 12
                onDrawBehind {
                    clipRect(right = size.width * progress.value) {
                        drawPath(fill, fillBrush)
                        drawPath(line, lineBrush, style = Stroke(sw, cap = StrokeCap.Round))
                        if (showDots) pts.forEach { drawCircle(color, sw * 1.1f, it) }
                        val last = pts.last()
                        drawCircle(c2.copy(alpha = 0.25f), sw * 3f, last)
                        drawCircle(Color.White, sw * 1.2f, last)
                    }
                }
            }
        )
        if (labels.isNotEmpty()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                labels.forEach { Text(it, color = Volt.textDim, style = VoltType.label) }
            }
        }
    }
}
