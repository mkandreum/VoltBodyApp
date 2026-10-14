package com.voltbody.app.ui.screens.workout

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.ImageDecoderDecoder
import com.voltbody.app.ui.theme.Volt
import kotlin.random.Random

// ───────────────────────── Imagen / GIF (Coil + decoder GIF) ─────────────────────────

private var sharedGifLoader: ImageLoader? = null

@Composable
fun rememberGifLoader(): ImageLoader {
    val app = LocalContext.current.applicationContext
    return remember {
        sharedGifLoader ?: ImageLoader.Builder(app)
            .components { add(ImageDecoderDecoder.Factory()) } // minSdk 28 -> soporta GIF/WebP animados
            .crossfade(300)
            .build().also { sharedGifLoader = it }
    }
}

/** Imagen/GIF del ejercicio con fallback a una foto genérica si falla o está vacía. */
@Composable
fun ExerciseImage(url: String?, name: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    val loader = rememberGifLoader()
    var failed by remember(url) { mutableStateOf(false) }
    val model = if (failed || url.isNullOrBlank()) FALLBACK_IMG else url
    Box(modifier.background(Volt.surface2)) {
        AsyncImage(
            model = model, contentDescription = name, imageLoader = loader, contentScale = contentScale,
            modifier = Modifier.fillMaxSize(), onError = { failed = true },
        )
    }
}

// ───────────────────────── Confeti ─────────────────────────

private class Particle(
    val vx: Float, val vy: Float, val color: Color, val size: Float, val rot0: Float, val rotSpeed: Float, val circle: Boolean,
)

/** Explosion de confeti cada vez que cambia [trigger] (>0). [big] = celebracion final. */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier, big: Boolean = false) {
    val progress = remember { Animatable(1f) }
    var particles by remember { mutableStateOf(emptyList<Particle>()) }
    val palette = listOf(Volt.accent, Volt.orange, Volt.water, Color.White, Volt.danger, Color(0xFFFACC15))
    LaunchedEffect(trigger) {
        if (trigger <= 0) return@LaunchedEffect
        val n = if (big) 130 else 64
        particles = List(n) {
            val angle = Random.nextFloat() * (Math.PI.toFloat() * 2f)
            val speed = (if (big) 0.55f else 0.4f) * (0.35f + Random.nextFloat() * 0.85f)
            Particle(
                vx = kotlin.math.cos(angle) * speed, vy = kotlin.math.sin(angle) * speed - 0.35f,
                color = palette[Random.nextInt(palette.size)], size = 5f + Random.nextFloat() * 7f,
                rot0 = Random.nextFloat() * 360f, rotSpeed = (Random.nextFloat() - 0.5f) * 720f, circle = Random.nextInt(4) == 0,
            )
        }
        progress.snapTo(0f)
        progress.animateTo(1f, tween(if (big) 2600 else 1700, easing = LinearEasing))
    }
    Canvas(modifier.fillMaxSize()) {
        val t = progress.value
        if (t >= 1f || particles.isEmpty()) return@Canvas
        val ox = size.width / 2f
        val oy = size.height * (if (big) 0.38f else 0.55f)
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

// ───────────────────────── Descanso (anillo animado) ─────────────────────────

@Composable
fun RestTimerCard(left: Int, total: Int, onAdjust: (Int) -> Unit, onSkip: () -> Unit, modifier: Modifier = Modifier) {
    val frac by animateFloatAsState(if (total > 0) left.toFloat() / total else 0f, tween(1000, easing = LinearEasing), label = "restFrac")
    val urgent = left in 1..5
    val pulse by rememberInfiniteTransition(label = "restPulse").animateFloat(
        1f, 1.08f, infiniteRepeatable(tween(450, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "restPulseV",
    )
    val shape = RoundedCornerShape(28.dp)
    Row(
        modifier.clip(shape).background(Color.Black.copy(alpha = 0.88f)).border(1.dp, Volt.accent.copy(alpha = 0.45f), shape)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(64.dp).scale(if (urgent) pulse else 1f), contentAlignment = Alignment.Center) {
            val track = Color.White.copy(alpha = 0.12f)
            val col = if (urgent) Volt.orange else Volt.accent
            Canvas(Modifier.fillMaxSize()) {
                val sw = 6.dp.toPx()
                val d = size.minDimension - sw
                val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                drawArc(track, -90f, 360f, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
                drawArc(col, -90f, 360f * frac, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
            }
            Text("${left / 60}:${"%02d".format(left % 60)}", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Column(Modifier.weight(1f, fill = false)) {
            Text("Descanso", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            Text(if (urgent) "¡Prepárate!" else "Respira y recupera", color = Volt.textDim, fontSize = 12.sp)
        }
        MiniPill("-15", onClick = { onAdjust(-15) })
        MiniPill("+15", onClick = { onAdjust(15) })
        Box(
            Modifier.size(34.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.1f)).clickable(onClick = onSkip),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Close, "Saltar descanso", tint = Volt.text, modifier = Modifier.size(18.dp)) }
    }
}

@Composable
fun MiniPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, selected: Boolean = false) {
    Box(
        modifier.clip(RoundedCornerShape(50)).background(if (selected) Volt.accent else Color.White.copy(alpha = 0.1f))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (selected) Color.Black else Volt.text, fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

// ───────────────────────── Entradas numericas ─────────────────────────

@Composable
fun RoundBtn(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit, modifier: Modifier = Modifier, size: Dp = 46.dp) {
    Box(
        modifier.size(size).clip(RoundedCornerShape(14.dp)).background(Volt.surface2).border(1.dp, Volt.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = Volt.text, modifier = Modifier.size(18.dp)) }
}

/** Campo numerico compacto. [decimal] permite un punto. */
@Composable
fun NumField(
    value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, decimal: Boolean = false, hint: String = "0", height: Dp = 48.dp,
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
        textStyle = TextStyle(color = Volt.text, fontSize = 20.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center),
        cursorBrush = SolidColor(Volt.accent),
        keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number),
        modifier = modifier.height(height).clip(RoundedCornerShape(14.dp)).background(Volt.surface2).border(1.dp, Volt.border, RoundedCornerShape(14.dp)),
        decorationBox = { inner ->
            Box(Modifier.fillMaxSize().padding(horizontal = 6.dp), contentAlignment = Alignment.Center) {
                if (value.isEmpty()) Text(hint, color = Volt.textDim.copy(alpha = 0.6f), fontSize = 20.sp, fontWeight = FontWeight.Bold)
                inner()
            }
        },
    )
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(text.uppercase(), color = Volt.textDim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp, modifier = modifier)
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
            RoundBtn(Icons.Filled.Remove, "Restar", onMinus, size = 42.dp)
            NumField(value, onChange, Modifier.weight(1f), decimal = decimal)
            RoundBtn(Icons.Filled.Add, "Sumar", onPlus, size = 42.dp)
        }
    }
}

/** Fila de selección 0..n (RIR / RPE): vuelve a tocar para deseleccionar. */
@Composable
fun ScaleSelector(options: List<Int>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        options.forEach { r ->
            val on = selected == r
            val bg by animateColorAsState(if (on) Volt.accent else Volt.surface2, tween(200), label = "scaleBg")
            val sc by animateFloatAsState(if (on) 1.08f else 1f, spring(0.5f, 500f), label = "scaleSc")
            Box(
                Modifier.weight(1f).height(42.dp).scale(sc).clip(RoundedCornerShape(12.dp)).background(bg)
                    .clickable { onSelect(if (on) -1 else r) },
                contentAlignment = Alignment.Center,
            ) { Text("$r", color = if (on) Color.Black else Volt.textDim, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
        }
    }
}

@Composable
fun InfoChip(text: String, modifier: Modifier = Modifier, color: Color = Volt.textDim, bg: Color = Volt.surface2) {
    Box(modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 12.dp, vertical = 6.dp)) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

/** Barra de progreso animada. */
@Composable
fun AnimatedProgressBar(fraction: Float, modifier: Modifier = Modifier, height: Dp = 6.dp, color: Color = Volt.accent) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), tween(600, easing = FastOutSlowInEasing), label = "bar")
    Box(modifier.height(height).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = 0.08f))) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(f).clip(RoundedCornerShape(50)).background(color))
    }
}

// ───────────────────────── Grafica de progresion ─────────────────────────

/** Línea animada con puntos. [labels] y [values] del mismo tamaño (viejo -> nuevo). */
@Composable
fun ProgressLineChart(labels: List<String>, values: List<Float>, unit: String, modifier: Modifier = Modifier, color: Color = Volt.accent) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) { progress.snapTo(0f); progress.animateTo(1f, tween(1000, easing = FastOutSlowInEasing)) }
    if (values.isEmpty()) return
    val mn = values.min(); val mx = values.max()
    Column(modifier) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("máx ${fmtNum(mx.toDouble())}$unit", color = Volt.textDim, fontSize = 10.sp)
            Text("mín ${fmtNum(mn.toDouble())}$unit", color = Volt.textDim, fontSize = 10.sp)
        }
        Canvas(Modifier.fillMaxWidth().height(110.dp).padding(vertical = 6.dp)) {
            val padX = 8.dp.toPx()
            val range = (mx - mn).takeIf { it > 0.0001f } ?: 1f
            fun pt(i: Int): Offset {
                val x = if (values.size == 1) size.width / 2f else padX + i * (size.width - 2 * padX) / (values.size - 1)
                val norm = if (mx - mn <= 0.0001f) 0.5f else (values[i] - mn) / range
                return Offset(x, size.height - norm * size.height * 0.8f - size.height * 0.1f)
            }
            for (g in 0..2) drawLine(Color.White.copy(alpha = 0.06f), Offset(0f, size.height * g / 2f), Offset(size.width, size.height * g / 2f), 1.dp.toPx())
            val pts = values.indices.map { pt(it) }
            if (pts.size > 1) {
                val path = Path().apply {
                    moveTo(pts[0].x, pts[0].y)
                    for (i in 1 until pts.size) { val m = (pts[i - 1].x + pts[i].x) / 2f; cubicTo(m, pts[i - 1].y, m, pts[i].y, pts[i].x, pts[i].y) }
                }
                val measure = PathMeasure().apply { setPath(path, false) }
                val out = Path(); measure.getSegment(0f, measure.length * progress.value, out, true)
                drawPath(out, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round))
            }
            pts.forEachIndexed { i, p ->
                val reach = if (pts.size == 1) 0f else i.toFloat() / (pts.size - 1)
                if (progress.value >= reach) { drawCircle(Color.Black, 5.dp.toPx(), p); drawCircle(color, 3.5f.dp.toPx(), p) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(labels.first().drop(5), color = Volt.textDim, fontSize = 10.sp)
            if (labels.size > 1) Text(labels.last().drop(5), color = Volt.textDim, fontSize = 10.sp)
        }
    }
}

/** Banner efimero dentro de la sesión (los toasts del Repository quedan tapados por el diálogo a pantalla completa). */
data class Banner(val title: String, val message: String?, val kind: String = "success", val id: Long = System.nanoTime())
