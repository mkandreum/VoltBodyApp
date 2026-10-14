package com.voltbody.app.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.AnimatedCheck
import com.voltbody.app.ui.EaseOutExpo
import com.voltbody.app.ui.GradientText
import com.voltbody.app.ui.MeshBackground
import com.voltbody.app.ui.rememberPulse
import com.voltbody.app.ui.rememberReduceMotion
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** Resplandor ambiental estatico. */
fun Modifier.ambientGlow(color: Color, center: Offset, radius: Float): Modifier = this.drawBehind {
    drawRect(Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius))
}

/** Icono de marca: rayo en squircle con halo pulsante (lectura del pulso en draw). */
@Composable
fun BoltLogo(size: Dp = 96.dp, pulsing: Boolean = true, modifier: Modifier = Modifier) {
    val p = rememberPulse(1f, 1.14f, 1100)
    val halo = Volt.accent.copy(alpha = 0.25f)
    val grad = Volt.gradient
    val shape = RoundedCornerShape(size * 0.30f)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSize().graphicsLayer { val s = if (pulsing) p.value else 1f; scaleX = s; scaleY = s }.clip(shape).background(halo))
        Box(Modifier.matchParentSize().clip(shape).background(grad), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Bolt, null, tint = Volt.onAccent, modifier = Modifier.size(size * 0.56f))
        }
    }
}

private class Particle(val orbit: Int, val phase: Float, val speed: Int, val size: Float, val tone: Int)

private val PARTICLES: List<Particle> = run {
    val r = java.util.Random(11)
    List(54) { i -> Particle(i % 3, r.nextFloat(), 1 + r.nextInt(3), 1.2f + r.nextFloat() * 3.2f, i % 3) }
}

/** Figura cinetica: nucleo con pulso, tres orbitas inclinadas y particulas que giran (todo en fase draw, sin fotos). */
@Composable
fun OrbitFigure(modifier: Modifier = Modifier, speedMs: Int = 20000) {
    val reduce = rememberReduceMotion()
    val t = rememberInfiniteTransition(label = "orbit").animateFloat(0f, if (reduce) 0f else 1f, infiniteRepeatable(tween(speedMs, easing = LinearEasing)), label = "orbitT")
    val beat = rememberPulse(0.92f, 1.08f, 1100)
    val c1 = Volt.accent
    val c2 = Volt.accent2
    Canvas(modifier) {
        val cx = size.width / 2f; val cy = size.height / 2f
        val base = size.minDimension / 2f
        val tt = t.value
        // nucleo
        val core = base * 0.30f * beat.value
        drawCircle(Brush.radialGradient(listOf(c1.copy(alpha = 0.55f), Color.Transparent), Offset(cx, cy), core * 3.2f), core * 3.2f, Offset(cx, cy))
        drawCircle(Brush.linearGradient(listOf(c1, c2), Offset(cx - core, cy - core), Offset(cx + core, cy + core)), core, Offset(cx, cy))
        // orbitas
        val tilts = floatArrayOf(-28f, 24f, 78f)
        val radii = floatArrayOf(0.95f, 0.78f, 0.58f)
        for (o in 0..2) {
            rotate(tilts[o]) {
                val rx = base * radii[o]; val ry = rx * 0.38f
                drawOval(Color.White.copy(alpha = 0.10f), Offset(cx - rx, cy - ry), Size(rx * 2, ry * 2), style = Stroke(1.2.dp.toPx()))
            }
        }
        // particulas
        for (p in PARTICLES) {
            val ang = (2.0 * PI * (p.phase + p.speed * tt)).toFloat()
            val rx = base * radii[p.orbit]; val ry = rx * 0.38f
            val tilt = Math.toRadians(tilts[p.orbit].toDouble())
            val x0 = rx * cos(ang); val y0 = ry * sin(ang)
            val x = cx + x0 * cos(tilt).toFloat() - y0 * sin(tilt).toFloat()
            val y = cy + x0 * sin(tilt).toFloat() + y0 * cos(tilt).toFloat()
            val depth = 0.5f + 0.5f * sin(ang)
            val color = when (p.tone) { 0 -> c1; 1 -> c2; else -> Color.White }
            drawCircle(color.copy(alpha = 0.25f + 0.65f * depth), p.size.dp.toPx() * (0.7f + 0.6f * depth), Offset(x, y))
        }
    }
}

/** Overlay de carga durante login/registro: anillo orbital + marca. */
@Composable
fun SplashOverlay(modifier: Modifier = Modifier) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(450, easing = EaseOutExpo)) }
    Box(modifier.fillMaxSize().background(Volt.bg.copy(alpha = 0.94f)).graphicsLayer { alpha = enter.value }, contentAlignment = Alignment.Center) {
        MeshBackground(Modifier.fillMaxSize(), intensity = 0.6f)
        Column(Modifier.graphicsLayer { val s = 0.9f + 0.1f * enter.value; scaleX = s; scaleY = s }, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(180.dp), contentAlignment = Alignment.Center) {
                OrbitFigure(Modifier.fillMaxSize())
                BoltLogo(56.dp)
            }
            GradientText("VoltBody", style = VoltType.h1)
            Text("Conectando…", color = Volt.textDim, style = VoltType.body)
        }
    }
}

private val PLAN_STEPS = listOf(
    "Analizando tu perfil", "Calculando tus macronutrientes", "Diseñando tu rutina semanal",
    "Estructurando la progresión", "Buscando ejercicios y animaciones", "Puliendo los últimos detalles",
)
private val PLAN_PHRASES = listOf(
    "Nuestra IA está calculando tus macros y estructurando tu progresión.",
    "Puede tardar hasta un minuto. No cierres la app.",
    "Adaptando series, repeticiones y cargas a tu nivel.",
    "Ajustando las comidas a tus horarios y gustos.",
)

/** "Generando tu plan": tres anillos que se dibujan, órbita de partículas, porcentaje rodante, textos que rotan y pasos con check dibujado. */
@Composable
fun GeneratingPlanScreen(modifier: Modifier = Modifier) {
    var stage by remember { mutableIntStateOf(0) }
    var phrase by remember { mutableIntStateOf(0) }
    val p1 = remember { Animatable(0f) }
    val p2 = remember { Animatable(0f) }
    val p3 = remember { Animatable(0f) }
    LaunchedEffect(Unit) { p1.animateTo(0.94f, tween(40000, easing = EaseOutExpo)) }
    LaunchedEffect(Unit) { delay(500); p2.animateTo(0.86f, tween(40000, easing = EaseOutExpo)) }
    LaunchedEffect(Unit) { delay(1000); p3.animateTo(0.78f, tween(40000, easing = EaseOutExpo)) }
    LaunchedEffect(Unit) { while (true) { delay(3200); if (stage < PLAN_STEPS.size - 1) stage++ } }
    LaunchedEffect(Unit) { while (true) { delay(4500); phrase = (phrase + 1) % PLAN_PHRASES.size } }
    val c1 = Volt.accent
    val c2 = Volt.accent2

    Box(modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Box(Modifier.size(250.dp), contentAlignment = Alignment.Center) {
                OrbitFigure(Modifier.fillMaxSize().graphicsLayer { alpha = 0.9f }, 14000)
                Canvas(Modifier.fillMaxSize()) {
                    val rings = listOf(p1 to 0.0f, p2 to 0.14f, p3 to 0.28f)
                    rings.forEachIndexed { i, (a, inset) ->
                        val sw = (7 - i).dp.toPx()
                        val d = size.minDimension * (1f - inset) - sw
                        val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
                        drawArc(Color.White.copy(alpha = 0.06f), 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
                        rotate(-90f) {
                            drawArc(c1.copy(alpha = 0.18f), 0f, 360f * a.value, false, tl, Size(d, d), style = Stroke(sw * 2f, cap = StrokeCap.Round))
                            drawArc(Brush.sweepGradient(if (i % 2 == 0) listOf(c1, c2, c1) else listOf(c2, c1, c2)), 0f, 360f * a.value, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
                        }
                    }
                }
                val pct = remember { androidx.compose.runtime.derivedStateOf { (p1.value / 0.94f * 99f).toInt() } }
                GradientText("${pct.value}%", style = VoltType.numberL)
            }
            Spacer(Modifier.height(28.dp))
            Text("Creando tu plan", color = Volt.text, style = VoltType.h1, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                phrase,
                transitionSpec = { (slideInVertically(tween(400, easing = EaseOutExpo)) { it / 2 } + fadeIn(tween(400))) togetherWith (slideOutVertically(tween(200)) { -it / 2 } + fadeOut(tween(200))) },
                label = "phrase",
            ) { i -> Text(PLAN_PHRASES[i], color = Volt.textDim, style = VoltType.body, textAlign = TextAlign.Center, modifier = Modifier.heightIn(min = 44.dp)) }
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PLAN_STEPS.forEachIndexed { i, label ->
                    if (i <= stage) {
                        Row(Modifier.staggeredEntry(0), verticalAlignment = Alignment.CenterVertically) {
                            if (i < stage) AnimatedCheck(true, Modifier.size(22.dp))
                            else {
                                val pulse = rememberPulse(0.6f, 1.2f, 700)
                                Box(Modifier.size(22.dp), contentAlignment = Alignment.Center) {
                                    Box(Modifier.size(9.dp).graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }.clip(CircleShape).background(c1))
                                }
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(label, color = if (i < stage) Volt.textDim else Volt.text, style = VoltType.body)
                        }
                    }
                }
            }
        }
    }
}
