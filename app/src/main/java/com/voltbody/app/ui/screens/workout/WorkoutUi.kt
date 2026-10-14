package com.voltbody.app.ui.screens.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import coil.request.ImageRequest
import com.voltbody.app.ui.EaseOutExpo
import com.voltbody.app.ui.MeshBackground
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.util.rememberHaptics
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/** Dorado de record / PR. */
internal val PrGold = Color(0xFFFFC857)

// ───────────────────────── Imagen / GIF (Coil + decoder GIF) ─────────────────────────

private var sharedGifLoader: ImageLoader? = null

@Composable
fun rememberGifLoader(): ImageLoader {
    val app = LocalContext.current.applicationContext
    return remember {
        sharedGifLoader ?: ImageLoader.Builder(app)
            .components { add(ImageDecoderDecoder.Factory()) } // minSdk 28 -> soporta GIF/WebP animados
            .crossfade(false)
            .build().also { sharedGifLoader = it }
    }
}

/** Imagen/GIF del ejercicio con tamaño acotado ([maxPx]) y fallback a una foto genérica si falla o está vacía. */
@Composable
fun ExerciseImage(
    url: String?, name: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop, maxPx: Int = 360,
) {
    val ctx = LocalContext.current
    val loader = rememberGifLoader()
    var failed by remember(url) { mutableStateOf(false) }
    val request = remember(url, failed, maxPx) {
        ImageRequest.Builder(ctx).data(if (failed || url.isNullOrBlank()) FALLBACK_IMG else url).size(maxPx).crossfade(false).build()
    }
    Box(modifier.background(Volt.surface2)) {
        AsyncImage(
            model = request, contentDescription = name, imageLoader = loader, contentScale = contentScale,
            modifier = Modifier.fillMaxSize(), onError = { failed = true },
        )
    }
}

// ───────────────────────── Confeti ─────────────────────────

private class Particle(
    val vx: Float, val vy: Float, val color: Color, val size: Float, val rot0: Float, val rotSpeed: Float, val circle: Boolean,
)

/** Explosion de confeti cada vez que cambia [trigger] (>0). [big] = celebracion final. Se dibuja en fase draw. */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier, big: Boolean = false) {
    val progress = remember { Animatable(1f) }
    var particles by remember { mutableStateOf(emptyList<Particle>()) }
    val palette = listOf(Volt.accent, Volt.accent2, Volt.water, Color.White, Volt.danger, PrGold)
    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        val n = if (big) 120 else 56
        particles = List(n) {
            val angle = Random.nextFloat() * (PI.toFloat() * 2f)
            val speed = (if (big) 0.55f else 0.4f) * (0.35f + Random.nextFloat() * 0.85f)
            Particle(
                vx = cos(angle) * speed, vy = sin(angle) * speed - 0.35f,
                color = palette[Random.nextInt(palette.size)], size = 5f + Random.nextFloat() * 7f,
                rot0 = Random.nextFloat() * 360f, rotSpeed = (Random.nextFloat() - 0.5f) * 720f, circle = Random.nextInt(4) == 0,
            )
        }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(if (big) 2600 else 1600, easing = LinearEasing))
    }
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f || particles.isEmpty()) return@Canvas
        val ox = size.width / 2f
        val oy = size.height * (if (big) 0.38f else 0.5f)
        val alpha = (1f - t * t * t).coerceIn(0f, 1f)
        particles.forEach { p ->
            val x = ox + p.vx * t * size.width * 1.6f
            val y = oy + (p.vy * t + 1.5f * t * t) * size.height * 0.62f
            val s = p.size.dp.toPx()
            val c = p.color.copy(alpha = alpha)
            if (p.circle) drawCircle(c, s / 2f, Offset(x, y))
            else rotate(p.rot0 + p.rotSpeed * t, Offset(x, y)) {
                drawRoundRect(c, Offset(x - s / 2f, y - s / 3f), Size(s, s * 0.66f), CornerRadius(2.dp.toPx()))
            }
        }
    }
}

// ───────────────────────── Descanso: temporizador circular + órbita ─────────────────────────

/** Texto m:ss del descanso: solo este composable se recompone (una vez por segundo). */
@Composable
private fun RestSeconds(left: Animatable<Float, AnimationVector1D>) {
    val haptics = rememberHaptics()
    val secs by remember { derivedStateOf { ceil(left.value).toInt().coerceAtLeast(0) } }
    LaunchedEffect(secs) { if (secs in 1..3) haptics.light() }
    Text(
        "${secs / 60}:${"%02d".format(secs % 60)}",
        style = VoltType.numberXL.copy(fontSize = 76.sp, lineHeight = 80.sp, brush = Volt.gradient), maxLines = 1,
    )
}

/**
 * Descanso a pantalla completa: anillo restante (degradado de marca) + órbita elíptica que se dibuja con el tiempo.
 * Todo el estado del tiempo es local: no recompone al padre. Al llegar a 0 llama [onFinished].
 */
@Composable
fun RestOverlay(
    initial: Int, title: String, subtitle: String, onFinished: () -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val left = remember { Animatable(initial.toFloat().coerceAtLeast(1f)) }
    var total by remember { mutableFloatStateOf(initial.toFloat().coerceAtLeast(1f)) }
    var bump by remember { mutableIntStateOf(0) }
    LaunchedEffect(bump) {
        left.animateTo(0f, tween((left.value * 1000f).toInt().coerceAtLeast(1), easing = LinearEasing))
        onFinished()
    }
    fun adjust(d: Int) {
        haptics.light()
        scope.launch {
            val nv = (left.value + d).coerceAtLeast(1f)
            left.snapTo(nv)
            if (nv > total) total = nv
            bump++
        }
    }
    val c1 = Volt.accent
    val c2 = Volt.accent2
    val spin = rememberInfiniteTransition(label = "restSpin").animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Restart), label = "restSpinV")

    Box(modifier.fillMaxSize().background(Volt.bg.copy(alpha = 0.97f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}) {
        MeshBackground(Modifier.fillMaxSize(), colorA = Volt.accent, colorB = Volt.accent2, intensity = 0.7f)
        Column(Modifier.fillMaxSize().statusBarsPadding().padding(horizontal = 24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text("DESCANSO", color = Volt.textDim, style = VoltType.label)
            Spacer(Modifier.height(6.dp))
            Text(title, color = Volt.text, style = VoltType.h1, maxLines = 2, textAlign = TextAlign.Center)
            Spacer(Modifier.height(32.dp))
            Box(
                Modifier.size(300.dp).drawWithCache {
                    val sw = 14.dp.toPx()
                    val d = size.minDimension * 0.70f
                    val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                    val arcSize = Size(d, d)
                    // órbita: elipse inclinada
                    val orbit = Path().apply {
                        addOval(Rect(Offset(size.width * 0.02f, size.height * 0.20f), Size(size.width * 0.96f, size.height * 0.60f)))
                    }
                    val pm = PathMeasure().apply { setPath(orbit, false) }
                    val seg = Path()
                    val tilt = -24f
                    val sweep = Brush.sweepGradient(listOf(c1, c2, c1))
                    onDrawBehind {
                        val frac = (left.value / total).coerceIn(0f, 1f)
                        drawArc(Color.White.copy(alpha = 0.07f), 0f, 360f, false, tl, arcSize, style = Stroke(sw))
                        rotate(-90f) {
                            drawArc(c1.copy(alpha = 0.18f), 0f, 360f * frac, false, tl, arcSize, style = Stroke(sw * 2.2f, cap = StrokeCap.Round))
                            drawArc(sweep, 0f, 360f * frac, false, tl, arcSize, style = Stroke(sw, cap = StrokeCap.Round))
                        }
                        rotate(tilt) {
                            val elapsed = 1f - frac
                            drawPath(orbit, Color.White.copy(alpha = 0.06f), style = Stroke(1.5.dp.toPx()))
                            seg.rewind()
                            pm.getSegment(0f, pm.length * elapsed, seg, true)
                            drawPath(seg, Brush.linearGradient(listOf(c2, c1)), style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
                            val pos = pm.getPosition(pm.length * elapsed)
                            drawCircle(c2.copy(alpha = 0.28f), 14.dp.toPx(), pos)
                            drawCircle(Color.White, 5.5.dp.toPx(), pos)
                        }
                        // chispa que gira lentamente en el borde del anillo
                        val a = Math.toRadians((spin.value - 90f).toDouble())
                        val r = d / 2f + sw * 1.9f
                        drawCircle(Color.White.copy(alpha = 0.35f), 2.5.dp.toPx(), Offset(size.width / 2f + (r * cos(a)).toFloat(), size.height / 2f + (r * sin(a)).toFloat()))
                    }
                },
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RestSeconds(left)
                    Text(subtitle, color = Volt.textDim, style = VoltType.body, textAlign = TextAlign.Center)
                }
            }
            Spacer(Modifier.height(36.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                GhostPill("−15 s", Modifier.weight(1f)) { adjust(-15) }
                Box(
                    Modifier.weight(1.5f).height(60.dp).clip(CircleShape).background(Volt.gradient).pressScale(onSkip),
                    contentAlignment = Alignment.Center,
                ) { Text("Saltar descanso", color = Volt.onAccent, style = VoltType.title) }
                GhostPill("+15 s", Modifier.weight(1f)) { adjust(15) }
            }
        }
    }
}

@Composable
private fun GhostPill(text: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(60.dp).clip(CircleShape).background(Volt.surface2).pressScale(onClick),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = Volt.text, style = VoltType.title) }
}

@Composable
fun MiniPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false) {
    Box(
        modifier.clip(CircleShape).background(if (selected) Volt.accent else Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick).padding(horizontal = 12.dp, vertical = 9.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (selected) Volt.onAccent else Volt.text, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

// ───────────────────────── Entradas numericas ─────────────────────────

@Composable
fun RoundBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 48.dp) {
    Box(
        modifier.size(size).clip(CircleShape).background(Volt.surface2).pressScale(onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Volt.text, modifier = Modifier.size(20.dp)) }
}

/** Campo numerico compacto. [decimal] permite un punto. */
@Composable
fun NumField(
    value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, decimal: Boolean = false, hint: String = "0", height: Dp = 52.dp,
    readOnly: Boolean = false,
) {
    BasicTextField(
        value = value,
        onValueChange = { raw ->
            val t = raw.replace(',', '.')
            val ok = if (decimal) t.count { it == '.' } <= 1 && t.all { it.isDigit() || it == '.' } else t.all { it.isDigit() }
            if (ok && t.length <= 7) onChange(t)
        },
        readOnly = readOnly,
        singleLine = true,
        textStyle = VoltType.numberM.copy(color = Volt.text, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(Volt.accent),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier.height(height).clip(RoundedCornerShape(18.dp)).background(Volt.surface2),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize().padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text(hint, color = Volt.textDim.copy(alpha = 0.5f), style = VoltType.numberM)
                inner()
            }
        },
    )
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), color = Volt.textDim, style = VoltType.label, modifier = modifier)
}

/** [-] campo [+] */
@Composable
fun StepperField(
    label: String, value: String, onChange: (String) -> Unit, onMinus: () -> Unit, onPlus: () -> Unit,
    modifier: Modifier = Modifier, decimal: Boolean = false,
) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FieldLabel(label)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            RoundBtn(Icons.Filled.Remove, "Restar", onMinus, size = 44.dp)
            NumField(value, onChange, Modifier.weight(1f), decimal = decimal)
            RoundBtn(Icons.Filled.Add, "Sumar", onPlus, size = 44.dp)
        }
    }
}

@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, color: Color = Volt.textDim, bg: Color = Color.White.copy(alpha = 0.08f)) {
    Box(modifier.clip(CircleShape).background(bg).padding(horizontal = 14.dp, vertical = 8.dp)) {
        Text(text, color = color, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** Barra de progreso: relleno con degradado de marca (o [color] plano); el valor animado se lee en draw. */
@Composable
fun AnimatedProgressBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 6.dp, color: Color? = null) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(fraction) { a.animateTo(fraction.coerceIn(0f, 1f), tween(700, easing = EaseOutExpo)) }
    val c1 = color ?: Volt.accent
    val c2 = color ?: Volt.accent2
    Box(
        modifier.height(height).clip(CircleShape).background(Color.White.copy(alpha = 0.08f)).drawBehind {
            drawRect(Brush.horizontalGradient(listOf(c1, c2)), size = Size(size.width * a.value, size.height))
        },
    )
}

// ───────────────────────── Grafica de progresion con tooltip ─────────────────────────

/** Línea suave que se dibuja; toca o arrastra para ver el valor de cada sesión en un tooltip. */
@Composable
fun ProgressLineChart(labels: List<String>, values: List<Float>, unit: String, modifier: Modifier = Modifier, color: Color = Volt.accent) {
    if (values.isEmpty()) return
    val haptics = rememberHaptics()
    val progress = remember(values) { Animatable(0f) }
    LaunchedEffect(values) { progress.snapTo(0f); progress.animateTo(1f, tween(1000, easing = EaseOutExpo)) }
    var sel by remember(values) { mutableIntStateOf(values.lastIndex) }
    var w by remember { mutableIntStateOf(0) }
    var h by remember { mutableIntStateOf(0) }
    var tipW by remember { mutableIntStateOf(0) }
    val mn = remember(values) { values.min() }
    val mx = remember(values) { values.max() }
    val c2 = Volt.accent2
    val dotBg = Volt.bg
    val pts = remember(values, w, h) {
        val padX = 18f
        val range = (mx - mn).takeIf { it > 0.0001f } ?: 1f
        values.indices.map { i ->
            val x = if (values.size == 1) w / 2f else padX + i * (w - 2 * padX) / (values.size - 1)
            val norm = if (mx - mn <= 0.0001f) 0.5f else (values[i] - mn) / range
            Offset(x, h - norm * h * 0.62f - h * 0.14f)
        }
    }
    fun pick(x: Float) {
        if (pts.isEmpty()) return
        val i = pts.indices.minByOrNull { abs(pts[it].x - x) } ?: return
        if (i != sel) { sel = i; haptics.selection() }
    }
    Column(modifier) {
        Box(
            Modifier.fillMaxWidth().height(170.dp).onSizeChanged { w = it.width; h = it.height }
                .pointerInput(pts) { detectTapGestures { pick(it.x) } }
                .pointerInput(pts) { detectHorizontalDragGestures { change, _ -> pick(change.position.x) } },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                if (pts.isEmpty()) return@Canvas
                for (g in 0..3) drawLine(Color.White.copy(alpha = 0.05f), Offset(0f, size.height * g / 3f), Offset(size.width, size.height * g / 3f), 1.dp.toPx())
                val p = progress.value
                if (pts.size > 1) {
                    val path = Path().apply {
                        moveTo(pts[0].x, pts[0].y)
                        for (i in 1 until pts.size) { val m = (pts[i - 1].x + pts[i].x) / 2f; cubicTo(m, pts[i - 1].y, m, pts[i].y, pts[i].x, pts[i].y) }
                    }
                    val measure = PathMeasure().apply { setPath(path, false) }
                    val out = Path(); measure.getSegment(0f, measure.length * p, out, true)
                    val area = Path().apply {
                        addPath(out); lineTo(pts.last().x.coerceAtMost(measure.getPosition(measure.length * p).x), size.height); lineTo(pts[0].x, size.height); close()
                    }
                    drawPath(area, Brush.verticalGradient(listOf(color.copy(alpha = 0.28f), Color.Transparent)))
                    drawPath(out, Brush.horizontalGradient(listOf(color, c2)), style = Stroke(3.5f.dp.toPx(), cap = StrokeCap.Round))
                }
                pts.forEachIndexed { i, pt ->
                    val reach = if (pts.size == 1) 0f else i.toFloat() / (pts.size - 1)
                    if (p >= reach) { drawCircle(dotBg, 5.5f.dp.toPx(), pt); drawCircle(color, 3.5f.dp.toPx(), pt) }
                }
                val s = pts.getOrNull(sel)
                if (s != null && p > 0.98f) {
                    drawLine(color.copy(alpha = 0.35f), Offset(s.x, 0f), Offset(s.x, size.height), 1.5f.dp.toPx())
                    drawCircle(color.copy(alpha = 0.25f), 13.dp.toPx(), s)
                    drawCircle(Color.White, 6.dp.toPx(), s)
                    drawCircle(color, 3.5f.dp.toPx(), s)
                }
            }
            val s = pts.getOrNull(sel)
            if (s != null) {
                Column(
                    Modifier.offset { IntOffset((s.x - tipW / 2f).coerceIn(0f, (w - tipW).coerceAtLeast(0).toFloat()).roundToInt(), 0) }
                        .onSizeChanged { tipW = it.width }
                        .clip(RoundedCornerShape(14.dp)).background(Volt.surface2).padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("${fmtNum(values[sel].toDouble())}$unit", color = Volt.text, style = VoltType.numberS)
                    Text(labels.getOrNull(sel)?.drop(5) ?: "", color = Volt.textDim, style = VoltType.label)
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("mín ${fmtNum(mn.toDouble())}$unit", color = Volt.textDim, fontSize = 12.sp)
            Text("máx ${fmtNum(mx.toDouble())}$unit", color = Volt.textDim, fontSize = 12.sp)
        }
    }
}

/** Banner efimero dentro de la sesión (los toasts del Repository quedan tapados por el diálogo a pantalla completa). */
data class Banner(val title: String, val message: String?, val kind: String = "success", val id: Long = System.nanoTime())
