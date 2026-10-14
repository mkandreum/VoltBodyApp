package com.voltbody.app.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.animation.togetherWith
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

// ───────────────────────── Movimiento base ─────────────────────────

/** Easing "expo out": arranque rapido y llegada muy suave (contadores, dibujos). */
val EaseOutExpo: Easing = CubicBezierEasing(0.16f, 1f, 0.3f, 1f)

/** Spring estandar de entradas (dampingRatio 0.8, stiffness 800). */
fun <T> voltSpring(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = 800f)

/** true si el usuario desactivo las animaciones del sistema (ANIMATOR_DURATION_SCALE = 0). */
@Composable
fun rememberReduceMotion(): Boolean {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    return remember {
        try { android.provider.Settings.Global.getFloat(ctx.contentResolver, android.provider.Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
        catch (_: Throwable) { false }
    }
}

/** Aparece con fade + subida + escala; usa `index` para escalonar listas (30 ms por elemento). Lectura en fase draw. */
fun Modifier.staggeredEntry(index: Int = 0, delayPerItem: Int = 30): Modifier = composed {
    val reduce = rememberReduceMotion()
    val a = remember { Animatable(if (reduce) 1f else 0f) }
    LaunchedEffect(Unit) {
        if (reduce) return@LaunchedEffect
        delay((index.coerceIn(0, 5) * delayPerItem).toLong())
        a.animateTo(1f, spring(dampingRatio = 0.8f, stiffness = 700f))
    }
    this.graphicsLayer {
        val v = a.value
        alpha = v.coerceIn(0f, 1f)
        translationY = (1f - v) * 20.dp.toPx()
        val s = 0.98f + 0.02f * v
        scaleX = s; scaleY = s
    }
}

/** Pulsar = escala 0.94 con spring rebotona + haptica ligera + click. Sin lecturas de estado en composicion. */
fun Modifier.pressScale(onClick: () -> Unit): Modifier = pressScaleIf(true, 0.94f, onClick)

/** Variante con `enabled` y escala configurable. */
fun Modifier.pressScaleIf(enabled: Boolean = true, to: Float = 0.94f, onClick: () -> Unit): Modifier = composed {
    val src = remember { MutableInteractionSource() }
    val scale = remember { Animatable(1f) }
    val ctx = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(src) {
        src.interactions.collect { i ->
            when (i) {
                is PressInteraction.Press -> launch { scale.animateTo(to, spring(0.8f, 900f)) }
                is PressInteraction.Release, is PressInteraction.Cancel -> launch { scale.animateTo(1f, spring(0.8f, 700f)) }
                else -> Unit
            }
        }
    }
    this.graphicsLayer { scaleX = scale.value; scaleY = scale.value }
        .clickable(interactionSource = src, indication = null, enabled = enabled) {
            com.voltbody.app.util.Haptics.light(ctx)
            onClick()
        }
}

/** Sacudida horizontal cada vez que `trigger` aumenta (errores de formulario). */
fun Modifier.shake(trigger: Int): Modifier = composed {
    val x = remember { Animatable(0f) }
    LaunchedEffect(trigger) {
        if (trigger > 0) {
            for (t in floatArrayOf(-1f, 1f, -0.7f, 0.7f, -0.35f, 0.35f, 0f)) x.animateTo(t, tween(50, easing = LinearEasing))
        }
    }
    this.graphicsLayer { translationX = x.value * 10.dp.toPx() }
}

// ───────────────────────── Numeros y graficas ─────────────────────────

/** Numero que cuenta desde 0 hasta [value] (y de un valor a otro al cambiar). Solo se recompone cuando cambia el texto. */
@Composable
fun CountUpText(
    value: Float, modifier: Modifier = Modifier, decimals: Int = 0, suffix: String = "",
    style: TextStyle = VoltType.numberM.copy(color = Volt.text),
) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(value) { anim.animateTo(value, tween(600, easing = EaseOutExpo)) }
    val scale = remember(decimals) { 10f.pow(decimals) }
    val shown by remember(scale) { derivedStateOf { (anim.value * scale).roundToInt() } }
    Text(
        if (decimals == 0) "$shown$suffix" else "${"%.${decimals}f".format(shown / scale)}$suffix",
        modifier, style = style, maxLines = 1,
    )
}

/** Donut de progreso (sweep con degradado y punto luminoso en la cabeza). progress 0..1. */
@Composable
fun DonutProgress(
    progress: Float, modifier: Modifier = Modifier.size(200.dp), stroke: Dp = 22.dp,
    color: Color = Volt.orange, track: Color = Volt.surface2, center: @Composable () -> Unit = {},
) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(progress) { p.animateTo(progress.coerceIn(0f, 1f), tween(400, easing = EaseOutExpo)) }
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val sw = stroke.toPx()
            val d = size.minDimension - sw
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            val sz = Size(d, d)
            val v = p.value
            drawArc(track, 135f, 270f, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
            if (v > 0.001f) {
                rotate(135f) {
                    val brush = Brush.sweepGradient(
                        0f to color.copy(alpha = 0.35f), (0.75f * v).coerceAtLeast(0.02f) to color, 1f to color,
                    )
                    drawArc(brush, 0f, 270f * v, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                    // punto luminoso en la cabeza
                    val ang = Math.toRadians((270f * v).toDouble())
                    val r = d / 2f
                    drawCircle(Color.White.copy(alpha = 0.9f), sw * 0.16f, Offset(size.width / 2f + (r * cos(ang)).toFloat(), size.height / 2f + (r * sin(ang)).toFloat()))
                }
            }
        }
        center()
    }
}

/** Barras animadas con entrada escalonada; la barra [highlight] usa el degradado de marca y el resto una version apagada. */
@Composable
fun AnimatedBars(
    values: List<Float>, modifier: Modifier = Modifier.fillMaxWidth().height(90.dp),
    highlight: Int = values.indexOfFirst { it == values.maxOrNull() },
) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) { progress.snapTo(0f); progress.animateTo(1f, tween(400, easing = EaseOutExpo)) }
    val max = (values.maxOrNull() ?: 1f).coerceAtLeast(0.0001f)
    val dim = Volt.accent.copy(alpha = 0.26f)
    val c1 = Volt.accent
    val c2 = Volt.accent2
    Canvas(modifier) {
        if (values.isEmpty()) return@Canvas
        val gap = 6.dp.toPx()
        val w = (size.width - gap * (values.size - 1)) / values.size
        val n = values.size
        values.forEachIndexed { i, v ->
            val local = ((progress.value * (1f + 0.5f) - i * 0.5f / n)).coerceIn(0f, 1f)
            val h = (v / max) * size.height * local
            if (h <= 0f) return@forEachIndexed
            val top = size.height - h
            val brush: Brush? = if (i == highlight) Brush.verticalGradient(listOf(c2, c1), startY = top, endY = size.height) else null
            val r = CornerRadius(w / 2, w / 2)
            if (brush != null) drawRoundRect(brush, Offset(i * (w + gap), top), Size(w, h), r)
            else drawRoundRect(dim, Offset(i * (w + gap), top), Size(w, h), r)
        }
    }
}

/** Sparkline suave: se dibuja de izquierda a derecha con relleno degradado y punto final. */
@Composable
fun Sparkline(values: List<Float>, modifier: Modifier = Modifier.fillMaxWidth().height(40.dp), color: Color = Volt.water) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) { progress.snapTo(0f); progress.animateTo(1f, tween(400, easing = EaseOutExpo)) }
    Spacer(
        modifier.drawWithCache {
            val ok = values.size >= 2 && size.width > 0f
            val line = Path()
            val area = Path()
            val measure = PathMeasure()
            if (ok) {
                val mn = values.min(); val mx = values.max(); val range = (mx - mn).coerceAtLeast(0.0001f)
                val pts = values.mapIndexed { i, v ->
                    Offset(i * size.width / (values.size - 1), size.height - ((v - mn) / range) * size.height * 0.7f - size.height * 0.15f)
                }
                line.moveTo(pts[0].x, pts[0].y)
                for (i in 1 until pts.size) {
                    val m = (pts[i - 1].x + pts[i].x) / 2
                    line.cubicTo(m, pts[i - 1].y, m, pts[i].y, pts[i].x, pts[i].y)
                }
                area.addPath(line)
                area.lineTo(size.width, size.height); area.lineTo(0f, size.height); area.close()
                measure.setPath(line, false)
            }
            val fill = Brush.verticalGradient(listOf(color.copy(alpha = 0.30f), Color.Transparent))
            val sw = 3.dp.toPx()
            onDrawBehind {
                if (!ok) return@onDrawBehind
                val p = progress.value
                clipRect(left = 0f, top = 0f, right = size.width * p, bottom = size.height) { drawPath(area, fill) }
                val out = Path()
                measure.getSegment(0f, measure.length * p, out, true)
                drawPath(out, color, style = Stroke(sw, cap = StrokeCap.Round))
                if (p > 0.01f) {
                    val pos = measure.getPosition(measure.length * p)
                    drawCircle(color.copy(alpha = 0.25f), sw * 2.6f, pos)
                    drawCircle(color, sw * 1.15f, pos)
                }
            }
        }
    )
}

/** Pulso suave infinito. Lee `.value` solo en fase draw (graphicsLayer/drawBehind). */
@Composable
fun rememberPulse(from: Float = 1f, to: Float = 1.08f, ms: Int = 900): State<Float> {
    val t = rememberInfiniteTransition(label = "pulse")
    return t.animateFloat(from, to, infiniteRepeatable(tween(ms, easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)), RepeatMode.Reverse), label = "pulseV")
}

// ───────────────────────── Fondos y titulares ─────────────────────────

/** Antes: malla de degradados. Ahora fondo negro solido (decision de diseno: sin degradados de color). Firma conservada. */
@Composable
fun MeshBackground(modifier: Modifier = Modifier, colorA: () -> Color, colorB: () -> Color, intensity: Float = 1f, periodMs: Int = 26000) {
    val bg = Volt.bg
    Canvas(modifier.fillMaxSize()) { drawRect(bg) }
}

@Composable
fun MeshBackground(modifier: Modifier = Modifier, colorA: Color = Volt.accent, colorB: Color = Volt.accent2, intensity: Float = 1f) {
    MeshBackground(modifier, { colorA }, { colorB }, intensity)
}

@Composable
private fun RevealWord(text: String, delayMs: Int, style: TextStyle, color: Color) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { delay(delayMs.toLong()); a.animateTo(1f, spring(0.78f, 260f)) }
    Text(
        text, style = style, color = color, maxLines = 1,
        modifier = Modifier.graphicsLayer {
            val v = a.value
            alpha = v.coerceIn(0f, 1f)
            translationY = (1f - v) * 36.dp.toPx()
            val s = 0.9f + 0.1f * v
            scaleX = s; scaleY = s
        },
    )
}

/** Titular gigante que se revela palabra a palabra. Las lineas con indice >= [dimFromLine] van en gris tenue (bicolor). */
@Composable
fun KineticTitle(
    lines: List<String>, modifier: Modifier = Modifier, dimFromLine: Int = Int.MAX_VALUE,
    style: TextStyle = VoltType.display, startDelayMs: Int = 150, wordDelayMs: Int = 110,
) {
    val bright = Volt.text
    val dim = Volt.textDim.copy(alpha = 0.65f)
    var n = 0
    Column(modifier) {
        lines.forEachIndexed { li, line ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                line.split(" ").filter { it.isNotEmpty() }.forEach { word ->
                    RevealWord(word, startDelayMs + n * wordDelayMs, style, if (li >= dimFromLine) dim else bright)
                    n++
                }
            }
        }
    }
}

// ───────────────────────── Controles ─────────────────────────

/** Deslizador "desliza para empezar" (tambien completa con un toque). */
@Composable
fun SwipeToStart(text: String, onComplete: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val offset = remember { Animatable(0f) }
    var maxPx by remember { mutableFloatStateOf(1f) }
    val thumb = 64.dp
    val pad = 4.dp
    val padPx = with(density) { pad.toPx() }
    val thumbPx = with(density) { thumb.toPx() }
    val hint = rememberInfiniteTransition(label = "swipeHint").animateFloat(0f, 1f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "hint")
    val brush = Volt.gradient
    val onAccent = Volt.onAccent
    val latest = rememberUpdatedState(onComplete)

    fun finish() {
        scope.launch {
            offset.animateTo(maxPx, spring(0.8f, 500f))
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            latest.value()
            delay(500)
            offset.animateTo(0f, spring(0.8f, 300f))
        }
    }

    Box(
        modifier.fillMaxWidth().height(72.dp).clip(CircleShape).background(Volt.surface)
            .onSizeChanged { maxPx = (it.width - thumbPx - padPx * 2).coerceAtLeast(1f) }
            .drawBehind {
                val w = offset.value + thumbPx + padPx * 2
                drawRoundRect(brush, Offset.Zero, Size(w, size.height), CornerRadius(size.height / 2), alpha = 0.35f)
            }
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { finish() },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            Modifier.fillMaxSize().graphicsLayer { alpha = (1f - offset.value / maxPx * 1.4f).coerceIn(0f, 1f) },
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
        ) {
            Spacer(Modifier.size(thumb))
            Text(text, style = VoltType.title.copy(fontSize = 19.sp), color = Volt.text, textAlign = TextAlign.Center)
            Spacer(Modifier.size(10.dp))
            Box(Modifier.graphicsLayer { alpha = 0.35f + 0.65f * (1f - abs(hint.value * 2f - 1f)); translationX = hint.value * 6.dp.toPx() }) {
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = Volt.text, modifier = Modifier.size(20.dp))
            }
        }
        Box(
            Modifier.padding(start = pad)
                .offset { IntOffset(offset.value.roundToInt(), 0) }
                .size(thumb).clip(CircleShape).background(brush)
                .draggable(
                    orientation = Orientation.Horizontal,
                    state = rememberDraggableState { d -> scope.launch { offset.snapTo((offset.value + d).coerceIn(0f, maxPx)) } },
                    onDragStopped = {
                        if (offset.value > maxPx * 0.7f) finish()
                        else scope.launch { offset.animateTo(0f, spring(0.8f, 700f)) }
                    },
                ),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Filled.ArrowForward, null, tint = onAccent, modifier = Modifier.size(28.dp)) }
    }
}

/** Anillo con brillo y degradado de marca que se dibuja hasta [progress] (0..1). */
@Composable
fun GlowRing(
    progress: Float, modifier: Modifier = Modifier.size(120.dp), stroke: Dp = 10.dp,
    content: @Composable BoxScope.() -> Unit = {},
) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(progress) { a.animateTo(progress.coerceIn(0f, 1f), tween(400, easing = EaseOutExpo)) }
    val c1 = Volt.accent
    val c2 = Volt.accent2
    Box(modifier, contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize()) {
            val sw = stroke.toPx()
            val d = size.minDimension - sw * 2.4f
            val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
            val sz = Size(d, d)
            drawArc(Color.White.copy(alpha = 0.07f), 0f, 360f, false, tl, sz, style = Stroke(sw))
            val v = a.value
            if (v > 0.001f) {
                rotate(-90f) {
                    drawArc(c1.copy(alpha = 0.16f), 0f, 360f * v, false, tl, sz, style = Stroke(sw * 2.2f, cap = StrokeCap.Round))
                    drawArc(Brush.sweepGradient(listOf(c1, c2, c1)), 0f, 360f * v, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                }
            }
        }
        content()
    }
}

/** Barra segmentada estilo "stories": `current` es el segmento activo (los anteriores y el activo se rellenan). */
@Composable
fun SegmentedProgress(total: Int, current: Int, modifier: Modifier = Modifier, onJump: ((Int) -> Unit)? = null) {
    val brush = Volt.gradient
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        repeat(total) { i ->
            val fill = animateFloatAsState(if (i <= current) 1f else 0f, tween(300, easing = EaseOutExpo), label = "seg")
            Box(
                Modifier.weight(1f).height(14.dp).then(
                    if (onJump != null && i < current) Modifier.clickable { onJump(i) } else Modifier
                ),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.14f))
                        .drawBehind { drawRect(brush, size = Size(size.width * fill.value, size.height)) },
                )
            }
        }
    }
}

private fun centerIndex(s: LazyListState): Int {
    val li = s.layoutInfo
    val c = (li.viewportStartOffset + li.viewportEndOffset) / 2
    return li.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - c) }?.index ?: s.firstVisibleItemIndex
}

/**
 * Rueda numerica vertical con snap y tic haptico. `value` y `range` en las unidades reales; `step` paso (0.5 para peso).
 * Muestra 5 filas; la central se resalta con el degradado de marca.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BigNumberPicker(
    value: Float, onValueChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, modifier: Modifier = Modifier,
    step: Float = 1f, unit: String = "", itemHeight: Dp = 60.dp,
    textStyle: TextStyle = VoltType.numberL.copy(fontSize = 44.sp),
) {
    val count = ((range.endInclusive - range.start) / step).roundToInt() + 1
    fun idxOf(v: Float) = ((v - range.start) / step).roundToInt().coerceIn(0, count - 1)
    val decimals = if (step % 1f != 0f) 1 else 0
    val state = rememberLazyListState(initialFirstVisibleItemIndex = idxOf(value))
    val haptic = LocalHapticFeedback.current
    val itemPx = with(LocalDensity.current) { itemHeight.toPx() }
    val currentValue = rememberUpdatedState(value)
    val callback = rememberUpdatedState(onValueChange)
    LaunchedEffect(state) {
        snapshotFlow { centerIndex(state) }.distinctUntilChanged().collect { i ->
            if (i != idxOf(currentValue.value)) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                callback.value(range.start + i * step)
            }
        }
    }
    LaunchedEffect(value) {
        val t = idxOf(value)
        if (!state.isScrollInProgress && centerIndex(state) != t) state.scrollToItem(t)
    }
    val brush = Volt.gradient
    val style = textStyle.copy(brush = brush)
    Box(modifier.fillMaxWidth().height(itemHeight * 5), contentAlignment = Alignment.Center) {
        Box(Modifier.fillMaxWidth().height(itemHeight).clip(RoundedCornerShape(24.dp)).background(Volt.surface2))
        LazyColumn(
            state = state, flingBehavior = rememberSnapFlingBehavior(state),
            contentPadding = PaddingValues(vertical = itemHeight * 2), modifier = Modifier.fillMaxSize(),
        ) {
            items(count, key = { it }) { i ->
                Box(
                    Modifier.fillMaxWidth().height(itemHeight).graphicsLayer {
                        val li = state.layoutInfo
                        val info = li.visibleItemsInfo.firstOrNull { x -> x.index == i }
                        val c = (li.viewportStartOffset + li.viewportEndOffset) / 2f
                        val d = if (info == null) 3f else abs(info.offset + info.size / 2f - c) / itemPx
                        val k = (1f - d / 2.4f).coerceIn(0f, 1f)
                        alpha = 0.10f + 0.90f * k * k
                        val s = 0.55f + 0.45f * k
                        scaleX = s; scaleY = s
                    },
                    contentAlignment = Alignment.Center,
                ) {
                    val v = range.start + i * step
                    Text(if (decimals == 0) "${v.roundToInt()}" else "%.1f".format(v), style = style, maxLines = 1)
                }
            }
        }
        if (unit.isNotEmpty()) {
            Text(unit, style = VoltType.title, color = Volt.textDim, modifier = Modifier.align(Alignment.CenterEnd).padding(end = 28.dp))
        }
    }
}

// ───────────────────────── Acabado premium ─────────────────────────

/** Barrido de brillo suave para skeletons. Aplicar sobre un Box con forma ya recortada. */
fun Modifier.shimmer(): Modifier = composed {
    val reduce = rememberReduceMotion()
    val t = rememberInfiniteTransition(label = "shimmer").animateFloat(-0.4f, 1.4f, infiniteRepeatable(tween(1400, easing = LinearEasing)), label = "shimmerT")
    this.drawWithContent {
        drawContent()
        if (!reduce) {
            val x = size.width * t.value
            drawRect(
                Brush.linearGradient(
                    listOf(Color.Transparent, Color.White.copy(alpha = 0.10f), Color.Transparent),
                    start = Offset(x - size.width * 0.25f, 0f), end = Offset(x + size.width * 0.25f, size.height * 0.4f),
                ),
            )
        }
    }
}

/** Bloque skeleton con shimmer (sustituye spinners en cargas). */
@Composable
fun SkeletonBox(modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(16.dp)) {
    Box(modifier.clip(shape).background(Volt.surface2).shimmer())
}

/** Texto con degradado de marca (cifras heroe, CTA). */
@Composable
fun GradientText(text: String, modifier: Modifier = Modifier, style: TextStyle = VoltType.numberL, brush: Brush = Volt.gradient, maxLines: Int = 1, textAlign: TextAlign? = null) {
    Text(text, modifier, style = style.copy(brush = brush), maxLines = maxLines, textAlign = textAlign)
}

/** Brillo radial sutil detras de un numero heroe: `Modifier.glowBackdrop()` en el contenedor del numero. */
fun Modifier.glowBackdrop(color: Color? = null, alpha: Float = 0.22f, radiusFraction: Float = 0.75f): Modifier = composed {
    val c = color ?: Volt.accent
    this.drawBehind {
        val r = size.maxDimension * radiusFraction
        drawRect(Brush.radialGradient(listOf(c.copy(alpha = alpha), Color.Transparent), center = center, radius = r))
    }
}

/** Contenedor con brillo radial detras del contenido. */
@Composable
fun GlowBackdrop(modifier: Modifier = Modifier, color: Color = Volt.accent, alpha: Float = 0.22f, content: @Composable BoxScope.() -> Unit) {
    Box(modifier.glowBackdrop(color, alpha), contentAlignment = Alignment.Center, content = content)
}

/** Numero entero con digitos que ruedan (cada digito sube/baja con spring al cambiar). */
@Composable
fun RollingNumber(value: Int, modifier: Modifier = Modifier, style: TextStyle = VoltType.numberL, suffix: String = "", brush: Brush? = null) {
    val s = value.coerceAtLeast(0).toString()
    val st = if (brush != null) style.copy(brush = brush) else style
    Row(modifier, verticalAlignment = Alignment.Bottom) {
        s.forEachIndexed { idx, ch ->
            androidx.compose.animation.AnimatedContent(
                targetState = ch, label = "roll$idx",
                transitionSpec = {
                    val up = targetState > initialState
                    (androidx.compose.animation.slideInVertically(spring(0.8f, 380f)) { if (up) it else -it } + androidx.compose.animation.fadeIn(tween(150))) togetherWith
                        (androidx.compose.animation.slideOutVertically(spring(0.8f, 380f)) { if (up) -it else it } + androidx.compose.animation.fadeOut(tween(120)))
                },
            ) { c -> Text(c.toString(), style = st, maxLines = 1) }
        }
        if (suffix.isNotEmpty()) Text(suffix, style = VoltType.title, color = Volt.textDim, modifier = Modifier.padding(start = 4.dp, bottom = 6.dp))
    }
}

/** Check que se dibuja (trazo animado) dentro de un circulo con el degradado de marca. */
@Composable
fun AnimatedCheck(visible: Boolean, modifier: Modifier = Modifier.size(28.dp)) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(visible) { a.animateTo(if (visible) 1f else 0f, tween(if (visible) 280 else 150, easing = EaseOutExpo)) }
    val brush = Volt.gradient
    val ink = Volt.onAccent
    Canvas(modifier) {
        val v = a.value
        if (v <= 0.001f) return@Canvas
        drawCircle(brush, radius = size.minDimension / 2f * (0.6f + 0.4f * v), alpha = v.coerceIn(0f, 1f))
        val p = Path().apply {
            moveTo(size.width * 0.28f, size.height * 0.52f)
            lineTo(size.width * 0.44f, size.height * 0.67f)
            lineTo(size.width * 0.72f, size.height * 0.36f)
        }
        val m = PathMeasure().apply { setPath(p, false) }
        val out = Path()
        m.getSegment(0f, m.length * v, out, true)
        drawPath(out, ink, style = Stroke(size.minDimension * 0.11f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}
