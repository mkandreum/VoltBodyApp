package com.voltbody.app.ui.v3

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.theme.Jakarta
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.Haptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// ─────────────────────────────── Tipografia (medida, como el mockup) ───────────────────────────────

private fun ts(size: Int, w: Int, lh: Int = (size * 1.28f).toInt(), ls: Float = 0f) =
    TextStyle(fontFamily = Jakarta, fontSize = size.sp, fontWeight = FontWeight(w), lineHeight = lh.sp, letterSpacing = ls.em)

/** Fuente de display con caracter (Unbounded): titulares y cifras. */
@OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)
val Display: androidx.compose.ui.text.font.FontFamily = androidx.compose.ui.text.font.FontFamily(
    listOf(500, 600, 700, 800).map { w ->
        androidx.compose.ui.text.font.Font(com.voltbody.app.R.font.unbounded, FontWeight(w),
            variationSettings = androidx.compose.ui.text.font.FontVariation.Settings(androidx.compose.ui.text.font.FontVariation.weight(w)))
    }
)
private fun ds(size: Int, w: Int, lh: Int = (size * 1.2f).toInt(), ls: Float = -0.02f) =
    TextStyle(fontFamily = Display, fontSize = size.sp, fontWeight = FontWeight(w), lineHeight = lh.sp, letterSpacing = ls.em)

object T {
    val hello = ts(12, 500)
    val screen = ts(30, 500, 34, -0.035f)
    val headline = ts(34, 500, 38, -0.04f)
    val section = ts(16, 600, 22)
    val cardTitle = ts(13, 600, 18)
    val value = ds(18, 700, 24)
    val unit = ts(11, 500, 14)
    val hero = ds(30, 700, 36)
    val heroXL = ds(36, 800, 42, -0.03f)
    val display = ds(20, 600, 26)
    val body = ts(13, 500, 19)
    val caption = ts(11, 500, 15)
    val button = ts(15, 700, 20)
    val label = ts(10, 700, 13, 0.06f)
}

object K {
    val pad = 20.dp
    val gap = 12.dp
    val radius = 24.dp
    val navSpace = 112.dp
}

// ─────────────────────────────── Movimiento ───────────────────────────────

/** Entrada corta y nitida (fade + subida 14dp). Lectura en fase draw: no recompone. */
fun Modifier.enter(index: Int = 0): Modifier = composed {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(8) * 28L)
        a.animateTo(1f, tween(240, easing = FastOutSlowInEasing))
    }
    graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 14.dp.toPx() }
}

/** Pulsar: escala con spring + haptico de clic real. */
fun Modifier.tap(scaleTo: Float = 0.96f, haptic: Boolean = true, onClick: () -> Unit): Modifier = composed {
    val ctx = LocalContext.current
    val src = remember { MutableInteractionSource() }
    val s = remember { Animatable(1f) }
    LaunchedEffect(src) {
        src.interactions.collect { i ->
            when (i) {
                is PressInteraction.Press -> launch { s.animateTo(scaleTo, spring(0.7f, 1400f)) }
                is PressInteraction.Release, is PressInteraction.Cancel -> launch { s.animateTo(1f, spring(0.45f, 900f)) }
            }
        }
    }
    graphicsLayer { scaleX = s.value; scaleY = s.value }
        .clickable(interactionSource = src, indication = null) { if (haptic) Haptics.light(ctx); onClick() }
}

// ─────────────────────────────── Estructura ───────────────────────────────

/** Pantalla de pestana: fondo negro hasta la barra de estado, padding lateral 20, hueco para la barra flotante. */
@Composable
fun Screen(scroll: Boolean = true, bottomSpace: Dp = K.navSpace, content: @Composable ColumnScope.() -> Unit) {
    val s = rememberScrollState()
    Column(
        Modifier.fillMaxSize().background(Volt.bg)
            .then(if (scroll) Modifier.verticalScroll(s) else Modifier)
            .statusBarsPadding().padding(horizontal = K.pad).padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(K.gap),
    ) {
        content()
        Spacer(Modifier.navigationBarsPadding().height(bottomSpace))
    }
}

@Composable
fun Card(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: Dp = 16.dp, color: Color = Volt.surface, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(K.radius)).background(color)
            .then(if (onClick != null) Modifier.tap(0.97f, onClick = onClick) else Modifier)
            .padding(padding),
        content = content,
    )
}

/** Cabecera de tarjeta del mockup: icono de color + titulo + chevron. */
@Composable
fun CardHeader(icon: ImageVector, tint: Color, title: String, chevron: Boolean = true, trailing: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(title, style = T.cardTitle, color = Volt.text, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
        if (trailing != null) trailing()
        else if (chevron) Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Volt.text, modifier = Modifier.size(20.dp))
    }
}

/** Valor + unidad en linea base. */
@Composable
fun ValueUnit(value: String, unit: String?, modifier: Modifier = Modifier, style: TextStyle = T.value, unitBelow: Boolean = false) {
    if (unitBelow) {
        Column(modifier) {
            Text(value, style = style, color = Volt.text, maxLines = 1)
            if (unit != null) Text(unit, style = T.unit, color = Volt.textDim, maxLines = 1)
        }
    } else {
        Row(modifier, verticalAlignment = Alignment.Bottom) {
            Text(value, style = style, color = Volt.text, maxLines = 1, modifier = Modifier.alignByBaseline())
            if (unit != null) {
                Spacer(Modifier.width(4.dp))
                Text(unit, style = T.unit, color = Volt.textDim, maxLines = 1, modifier = Modifier.alignByBaseline())
            }
        }
    }
}

@Composable
fun IconCircle(icon: ImageVector, modifier: Modifier = Modifier, size: Dp = 44.dp, bg: Color = Volt.surface2, tint: Color = Volt.text, desc: String? = null, onClick: (() -> Unit)? = null) {
    Box(
        modifier.size(size).clip(CircleShape).background(bg).then(if (onClick != null) Modifier.tap(0.9f, onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(size * 0.46f)) }
}

/** Boton principal lima (CTA "Start" del mockup). */
@Composable
fun LimeButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, loading: Boolean = false, height: Dp = 56.dp) {
    val bg = if (enabled) Volt.accent else Volt.surface2
    val fg = if (enabled) Color(0xFF0A0A0A) else Volt.textDim
    Row(
        modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(height / 2)).background(bg)
            .then(if (enabled && !loading) Modifier.tap(0.97f, onClick = onClick) else Modifier),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = fg, strokeWidth = 2.dp)
        else {
            Text(text, style = T.button, color = fg, maxLines = 1)
            if (icon != null) { Spacer(Modifier.width(8.dp)); Icon(icon, null, tint = fg, modifier = Modifier.size(18.dp)) }
        }
    }
}

/** Boton secundario oscuro. */
@Composable
fun DarkButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, height: Dp = 52.dp, tint: Color = Volt.text) {
    Row(
        modifier.height(height).clip(RoundedCornerShape(height / 2)).background(Volt.surface2).tap(0.97f, onClick = onClick).padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, style = T.cardTitle, color = tint, maxLines = 1)
    }
}

/** Pildora pequena ("Weekly v" del mockup). */
@Composable
fun SmallPill(text: String, modifier: Modifier = Modifier, icon: ImageVector? = null, selected: Boolean = false, onClick: (() -> Unit)? = null) {
    Row(
        modifier.height(34.dp).clip(RoundedCornerShape(17.dp)).background(if (selected) Volt.accent else Volt.surface2)
            .then(if (onClick != null) Modifier.tap(0.94f, onClick = onClick) else Modifier).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, style = T.cardTitle.copy(fontSize = 12.sp), color = if (selected) Color(0xFF0A0A0A) else Volt.text, maxLines = 1)
        if (icon != null) { Spacer(Modifier.width(4.dp)); Icon(icon, null, tint = if (selected) Color(0xFF0A0A0A) else Volt.text, modifier = Modifier.size(16.dp)) }
    }
}

/** Barra superior de pantallas secundarias: atras redondo + titulo centrado + accion opcional. */
@Composable
fun TopBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, trailing: (@Composable () -> Unit)? = null) {
    Box(modifier.fillMaxWidth().height(48.dp)) {
        IconCircle(Icons.AutoMirrored.Filled.ArrowBack, Modifier.align(Alignment.CenterStart), desc = "Atrás", onClick = onBack)
        FitText(title, T.section, Volt.text, Modifier.align(Alignment.Center).padding(horizontal = 56.dp), maxLines = 1, minScale = 0.75f, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        if (trailing != null) Box(Modifier.align(Alignment.CenterEnd)) { trailing() }
    }
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth().padding(top = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = T.section, color = Volt.text, modifier = Modifier.weight(1f), maxLines = 1)
        if (action != null && onAction != null) Text(action, style = T.caption, color = Volt.accent, modifier = Modifier.tap(0.94f, onClick = onAction).padding(4.dp))
    }
}

/** Fila de menu (Perfil). */
@Composable
fun MenuRow(icon: ImageVector, title: String, subtitle: String? = null, value: String? = null, tint: Color = Volt.accent, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).tap(0.98f, onClick = onClick).padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Volt.surface2), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            FitText(title, T.cardTitle.copy(fontSize = 14.sp), Volt.text, maxLines = 1)
            if (subtitle != null) Text(subtitle, style = T.caption, color = Volt.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (value != null) Text(value, style = T.caption, color = Volt.textDim, maxLines = 1, modifier = Modifier.padding(start = 8.dp))
        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Volt.textDim, modifier = Modifier.size(20.dp))
    }
}

// ─────────────────────────────── Graficas (estilo mockup) ───────────────────────────────

/**
 * Barras redondeadas: apagadas en verde oliva y la destacada en lima con burbuja de valor encima.
 * Crecen al aparecer (lectura en draw).
 */
@Composable
fun MockBars(values: List<Float>, highlight: Int, bubble: String?, modifier: Modifier = Modifier.fillMaxWidth().height(86.dp), onSelect: ((Int) -> Unit)? = null) {
    val grow = remember { Animatable(0f) }
    LaunchedEffect(values) { grow.snapTo(0f); grow.animateTo(1f, tween(520, easing = FastOutSlowInEasing)) }
    val hl = animateFloatAsState(highlight.toFloat(), spring(0.8f, 700f), label = "barsHl")
    val on = Volt.accent
    val off = Volt.accent.copy(alpha = 0.32f)
    val bubbleBg = Color.White
    val textMeasurer = rememberTextMeasurer()
    val bubbleStyle = T.label.copy(color = Color(0xFF0A0A0A), fontSize = 10.sp, letterSpacing = 0.sp)
    Canvas(
        modifier.then(
            if (onSelect != null) Modifier.pointerTapIndex(values.size, onSelect) else Modifier,
        ),
    ) {
        if (values.isEmpty()) return@Canvas
        val top = 20.dp.toPx()
        val n = values.size
        val w = minOf(22.dp.toPx(), size.width / (n * 1.6f))
        val gap = if (n > 1) (size.width - w * n) / (n - 1) else 0f
        val max = (values.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val avail = size.height - top
        values.forEachIndexed { i, v ->
            val h = (avail * (0.18f + 0.82f * (v / max))) * grow.value
            val sel = 1f - kotlin.math.abs(i - hl.value).coerceAtMost(1f)
            val c = androidx.compose.ui.graphics.lerp(off, on, sel)
            if (sel > 0.5f) drawCircle(Brush.radialGradient(listOf(on.copy(alpha = 0.35f * sel), Color.Transparent), Offset(i * (w + gap) + w / 2, size.height - h / 2), w * 1.8f), w * 1.8f, Offset(i * (w + gap) + w / 2, size.height - h / 2))
            drawRoundRect(c, Offset(i * (w + gap), size.height - h), Size(w, h), CornerRadius(w / 2.2f))
        }
        if (bubble != null && highlight in values.indices) {
            val lay = textMeasurer.measure(bubble, bubbleStyle)
            val bw = lay.size.width + 12.dp.toPx()
            val bh = lay.size.height + 6.dp.toPx()
            val cx = hl.value * (w + gap) + w / 2
            val v = values[highlight]
            val barTop = size.height - (avail * (0.18f + 0.82f * (v / max))) * grow.value
            val by = (barTop - bh - 4.dp.toPx()).coerceAtLeast(0f)
            val bx = (cx - bw / 2).coerceIn(0f, size.width - bw)
            drawRoundRect(bubbleBg, Offset(bx, by), Size(bw, bh), CornerRadius(bh / 2), alpha = grow.value)
            drawText(lay, topLeft = Offset(bx + 6.dp.toPx(), by + 3.dp.toPx()), alpha = grow.value)
        }
    }
}

private fun Modifier.pointerTapIndex(n: Int, onSelect: (Int) -> Unit): Modifier = composed {
    val ctx = LocalContext.current
    pointerInput(n) {
        detectTapGestures { o ->
            val i = ((o.x / size.width) * n).toInt().coerceIn(0, n - 1)
            Haptics.selection(ctx); onSelect(i)
        }
    }
}

/** Mini donut (tarjeta Calorias del mockup). */
@Composable
fun MiniDonut(progress: Float, modifier: Modifier = Modifier.size(42.dp), color: Color = Volt.orange, stroke: Dp = 6.dp) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(progress) { p.animateTo(progress.coerceIn(0f, 1f), tween(700, easing = FastOutSlowInEasing)) }
    val track = Volt.surface2
    Canvas(modifier) {
        val sw = stroke.toPx()
        val d = size.minDimension - sw
        val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
        drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
        drawArc(color, -90f, 360f * p.value, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
    }
}

/** Donut grande con hueco abajo (pantalla "Your Activities"). */
@Composable
fun BigDonut(progress: Float, modifier: Modifier = Modifier.size(220.dp), color: Color = Volt.orange, stroke: Dp = 26.dp, center: @Composable BoxScope.() -> Unit) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(progress) { p.animateTo(progress.coerceIn(0f, 1f), tween(900, easing = FastOutSlowInEasing)) }
    val track = Volt.surface2
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val sw = stroke.toPx()
            val d = size.minDimension - sw
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
            if (p.value > 0.001f) drawArc(color, -90f, 360f * p.value, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
        }
        center()
    }
}

/** Linea ondulada suave que se dibuja (tarjeta Agua del mockup). */
@Composable
fun WaveLine(values: List<Float>, modifier: Modifier = Modifier.fillMaxWidth().height(36.dp), color: Color = Volt.accent2, strokeWidth: Dp = 2.5.dp) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(values) { p.snapTo(0f); p.animateTo(1f, tween(800, easing = FastOutSlowInEasing)) }
    Canvas(modifier) {
        val vs = if (values.size >= 2) values else listOf(0.3f, 0.6f, 0.4f, 0.8f, 0.5f, 0.7f)
        val mn = vs.min(); val mx = vs.max(); val r = (mx - mn).coerceAtLeast(0.0001f)
        val pts = vs.mapIndexed { i, v -> Offset(i * size.width / (vs.size - 1), size.height * 0.88f - ((v - mn) / r) * size.height * 0.76f) }
        val path = Path().apply {
            moveTo(pts[0].x, pts[0].y)
            for (i in 1 until pts.size) { val m = (pts[i - 1].x + pts[i].x) / 2; cubicTo(m, pts[i - 1].y, m, pts[i].y, pts[i].x, pts[i].y) }
        }
        val pm = PathMeasure().apply { setPath(path, false) }
        val out = Path(); pm.getSegment(0f, pm.length * p.value, out, true)
        drawPath(out, color, style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round))
    }
}

/** Mini barras verticales (tarjeta Pasos pequena de "Your Activities"). */
@Composable
fun MiniBars(values: List<Float>, modifier: Modifier = Modifier.size(width = 54.dp, height = 40.dp)) {
    val g = remember { Animatable(0f) }
    LaunchedEffect(values) { g.snapTo(0f); g.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
    val on = Volt.accent
    val off = Volt.accent.copy(alpha = 0.35f)
    Canvas(modifier) {
        val vs = values.ifEmpty { listOf(0.4f, 0.7f, 0.5f, 1f) }.takeLast(4)
        val gap = 5.dp.toPx(); val n = vs.size
        val w = (size.width - gap * (n - 1)) / n
        val mx = (vs.maxOrNull() ?: 1f).coerceAtLeast(0.0001f)
        vs.forEachIndexed { i, v ->
            val h = size.height * (0.25f + 0.75f * v / mx) * g.value
            drawRoundRect(if (i == n - 1) on else off, Offset(i * (w + gap), size.height - h), Size(w, h), CornerRadius(w / 2))
        }
    }
}

/** Oscurece un GIF de fondo blanco para integrarlo en negro (como la foto oscura del mockup). */
val heroDarken: androidx.compose.ui.graphics.ColorFilter =
    androidx.compose.ui.graphics.ColorFilter.tint(Color(0xFF3A3A3A), androidx.compose.ui.graphics.BlendMode.Multiply)

/**
 * Firma visual de VoltBody: linea de "voltaje" tipo electrocardiograma con una chispa que la recorre.
 * Todo se dibuja en fase draw (no recompone).
 */
@Composable
fun VoltPulse(modifier: Modifier = Modifier.fillMaxWidth().height(28.dp), color: Color = Volt.accent, glow: Boolean = true, periodMs: Int = 2400) {
    val t = androidx.compose.animation.core.rememberInfiniteTransition(label = "pulse")
    val ph = t.animateFloat(0f, 1f, androidx.compose.animation.core.infiniteRepeatable(tween(periodMs, easing = androidx.compose.animation.core.LinearEasing)), label = "ph")
    Canvas(modifier) {
        val w = size.width; val h = size.height; val mid = h / 2
        val path = Path().apply {
            moveTo(0f, mid)
            val beats = 3
            for (b in 0 until beats) {
                val x0 = w * b / beats
                val bw = w / beats
                lineTo(x0 + bw * 0.38f, mid)
                lineTo(x0 + bw * 0.44f, mid - h * 0.18f)
                lineTo(x0 + bw * 0.50f, mid + h * 0.42f)
                lineTo(x0 + bw * 0.56f, mid - h * 0.48f)
                lineTo(x0 + bw * 0.62f, mid + h * 0.12f)
                lineTo(x0 + bw * 0.68f, mid)
            }
            lineTo(w, mid)
        }
        drawPath(path, color.copy(alpha = 0.28f), style = Stroke(1.6.dp.toPx(), join = androidx.compose.ui.graphics.StrokeJoin.Round))
        val pm = PathMeasure().apply { setPath(path, false) }
        val len = pm.length
        val head = len * ph.value
        val seg = Path(); pm.getSegment((head - len * 0.16f).coerceAtLeast(0f), head, seg, true)
        drawPath(seg, color, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        val pos = pm.getPosition(head)
        if (glow) drawCircle(Brush.radialGradient(listOf(color.copy(alpha = 0.55f), Color.Transparent), pos, 10.dp.toPx()), 10.dp.toPx(), pos)
        drawCircle(Color.White, 2.dp.toPx(), pos)
    }
}

/** Palabra gigante en contorno (marca de agua detras de los heroes). */
@Composable
fun Watermark(text: String, modifier: Modifier = Modifier, size: Int = 64, color: Color = Color.White.copy(alpha = 0.10f)) {
    Text(
        text.uppercase(), modifier = modifier, maxLines = 1, softWrap = false,
        style = TextStyle(fontFamily = Display, fontWeight = FontWeight(800), fontSize = size.sp, letterSpacing = (-0.02f).em,
            drawStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)),
        color = color,
    )
}

/** Brillo radial lima detras de un elemento activo. */
fun Modifier.limeGlow(color: Color, radiusFraction: Float = 0.7f, alpha: Float = 0.30f): Modifier = this.drawBehind {
    val r = size.maxDimension * radiusFraction
    drawCircle(Brush.radialGradient(listOf(color.copy(alpha = alpha), Color.Transparent), center, r), r, center)
}

/** Degradado del banner "Stay Active". */
val bannerBrush: Brush @Composable get() = androidx.compose.ui.graphics.SolidColor(Volt.accent)

/**
 * Texto que se encoge (hasta [minScale]) para caber en [maxLines] sin cortarse con "...".
 * Evita nombres truncados y mantiene la coherencia visual.
 */
@Composable
fun FitText(
    text: String, style: TextStyle, color: Color, modifier: Modifier = Modifier, maxLines: Int = 2,
    minScale: Float = 0.7f, textAlign: androidx.compose.ui.text.style.TextAlign? = null,
) {
    var scale by remember(text, style) { mutableFloatStateOf(1f) }
    var ready by remember(text, style) { mutableStateOf(false) }
    val fs = style.fontSize * scale
    val lh = if (style.lineHeight.isSpecified) style.lineHeight * scale else style.lineHeight
    Text(
        text, style = style.copy(fontSize = fs, lineHeight = lh), color = color, maxLines = maxLines, softWrap = true,
        overflow = if (scale <= minScale) TextOverflow.Ellipsis else TextOverflow.Clip, textAlign = textAlign,
        modifier = modifier.drawWithContent { if (ready) drawContent() },
        onTextLayout = { r -> if (r.hasVisualOverflow && scale > minScale) scale = (scale - 0.06f).coerceAtLeast(minScale) else ready = true },
    )
}

/** Jerga de gimnasio: algunos terminos suenan mejor en ingles (Full body, Push, Pull, Upper/Lower...). */
fun gymTerm(s: String): String {
    var r = s
    val rules = listOf(
        Regex("(?i)cuerpo completo|cuerpo entero") to "Full body",
        Regex("(?i)tren superior") to "Upper body", Regex("(?i)tren inferior") to "Lower body",
        Regex("(?i)\bempuje\b") to "Push", Regex("(?i)\btir[oó]n\b|\btracci[oó]n\b") to "Pull",
        Regex("(?i)abdominales|zona media") to "Core", Regex("(?i)\bhiit\b") to "HIIT",
        Regex("(?i)movilidad y estiramientos") to "Mobility", Regex("(?i)d[ií]a de pierna") to "Leg day",
    )
    rules.forEach { (re, v) -> r = r.replace(re, v) }
    return r.replaceFirstChar { it.uppercase() }
}
