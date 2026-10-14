package com.voltbody.app.ui.screens.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableLongState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.withFrameNanos
import com.voltbody.app.data.WorkoutLog
import com.voltbody.app.ui.gradientBorder
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.util.ExerciseSession
import com.voltbody.app.util.rememberHaptics
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sin

// ───────────────────────── Número gigante ajustable por arrastre vertical ─────────────────────────

/**
 * Número enorme que se ajusta arrastrando verticalmente (arriba = más) con háptica en cada paso; tocar para teclear.
 * [value] es una lambda para que solo este composable se recomponga al cambiar el valor.
 */
@Composable
fun DragNumber(
    label: String, unit: String, value: () -> String, onChange: (String) -> Unit, step: Double, min: Double, max: Double,
    modifier: Modifier = Modifier, decimal: Boolean = false, numberSize: Int = 52, startAt: Double = min,
) {
    val haptics = rememberHaptics()
    val scope = rememberCoroutineScope()
    val pulse = remember { Animatable(0f) }
    val tickPx = with(LocalDensity.current) { 20.dp.toPx() }
    var acc by remember { mutableFloatStateOf(0f) }
    var editing by remember { mutableStateOf(false) }
    val current = value()
    val curState = rememberUpdatedState(current)
    val onChangeState = rememberUpdatedState(onChange)
    val glow = Volt.accent

    fun applyTicks(ticks: Int) {
        val cur = curState.value
        val next = if (cur.isEmpty()) { if (ticks > 0) startAt else return } else (cur.toD() + ticks * step).coerceIn(min, max)
        val nv = round(next * 100.0) / 100.0
        if (cur.isNotEmpty() && nv == cur.toD()) return
        onChangeState.value(fmtNum(nv))
        haptics.selection()
        scope.launch { pulse.snapTo(1f); pulse.animateTo(0f, spring(0.5f, 500f)) }
    }

    val dragState = rememberDraggableState { d ->
        acc -= d
        val n = (acc / tickPx).toInt()
        if (n != 0) { acc -= n * tickPx; applyTicks(n) }
    }
    val focus = remember { FocusRequester() }
    var hadFocus by remember { mutableStateOf(false) }
    LaunchedEffect(editing) { if (editing) { hadFocus = false; runCatching { focus.requestFocus() } } }

    Column(
        modifier.clip(RoundedCornerShape(28.dp)).background(Volt.surface2)
            .drawBehind { drawRect(Brush.radialGradient(listOf(glow.copy(alpha = 0.10f), Color.Transparent), center = center, radius = size.maxDimension * 0.7f)) }
            .draggable(orientation = Orientation.Vertical, state = dragState)
            .clickable { editing = true }
            .padding(vertical = 14.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        FieldLabel(label)
        Icon(Icons.Filled.KeyboardArrowUp, null, tint = Volt.textDim.copy(alpha = 0.45f), modifier = Modifier.size(20.dp))
        val numStyle = VoltType.numberXL.copy(fontSize = numberSize.sp, lineHeight = (numberSize + 4).sp, textAlign = TextAlign.Center)
        if (editing) {
            BasicTextField(
                value = current,
                onValueChange = { raw ->
                    val t = raw.replace(',', '.')
                    val ok = if (decimal) t.count { it == '.' } <= 1 && t.all { it.isDigit() || it == '.' } else t.all { it.isDigit() }
                    if (ok && t.length <= 6) onChange(t)
                },
                singleLine = true, textStyle = numStyle.copy(color = Volt.text), cursorBrush = SolidColor(Volt.accent),
                keyboardOptions = KeyboardOptions(keyboardType = if (decimal) KeyboardType.Decimal else KeyboardType.Number, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { editing = false }),
                modifier = Modifier.fillMaxWidth().focusRequester(focus).onFocusChanged { if (it.isFocused) hadFocus = true else if (hadFocus) editing = false },
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.Center) {
                Text(
                    current.ifEmpty { "–" }, style = numStyle.copy(brush = Volt.gradient), maxLines = 1,
                    modifier = Modifier.graphicsLayer { val s = 1f + 0.10f * pulse.value; scaleX = s; scaleY = s },
                )
                if (unit.isNotEmpty()) Text(unit, color = Volt.textDim, style = VoltType.title, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            }
        }
        Icon(Icons.Filled.KeyboardArrowDown, null, tint = Volt.textDim.copy(alpha = 0.45f), modifier = Modifier.size(20.dp))
    }
}

// ───────────────────────── Cronómetro con anillo (isométricos / cardio) ─────────────────────────

@Composable
private fun StopwatchText(ms: MutableLongState) {
    val s by remember { derivedStateOf { (ms.longValue / 1000L).toInt() } }
    Text(formatDuration(s), style = VoltType.numberXL.copy(fontSize = 60.sp, lineHeight = 64.sp, brush = Volt.gradient), maxLines = 1)
}

/** Cronómetro grande con anillo. El tiempo es estado local (por fotograma, leído solo en draw); [onStop] devuelve segundos. */
@Composable
fun StopwatchRing(targetSec: Int?, onStop: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptics = rememberHaptics()
    var running by remember { mutableStateOf(false) }
    val ms = remember { mutableLongStateOf(0L) }
    LaunchedEffect(running) {
        if (!running) return@LaunchedEffect
        val base = withFrameNanos { it } - ms.longValue * 1_000_000L
        while (true) withFrameNanos { now -> ms.longValue = (now - base) / 1_000_000L }
    }
    val reached by remember { derivedStateOf { targetSec != null && targetSec > 0 && ms.longValue >= targetSec * 1000L } }
    LaunchedEffect(reached) { if (reached) haptics.success() }
    val started by remember { derivedStateOf { ms.longValue > 0L } }
    val c1 = Volt.accent
    val c2 = Volt.accent2
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.size(240.dp).drawBehind {
                val sw = 14.dp.toPx()
                val d = size.minDimension - sw * 2.2f
                val tl = Offset((size.width - d) / 2f, (size.height - d) / 2f)
                val sz = Size(d, d)
                val t = ms.longValue / 1000f
                val frac = if (targetSec != null && targetSec > 0) (t / targetSec).coerceIn(0f, 1f) else ((ms.longValue % 60000L) / 60000f)
                drawArc(Color.White.copy(alpha = 0.07f), 0f, 360f, false, tl, sz, style = Stroke(sw))
                if (frac > 0.001f) rotate(-90f) {
                    drawArc(c1.copy(alpha = 0.18f), 0f, 360f * frac, false, tl, sz, style = Stroke(sw * 2.2f, cap = StrokeCap.Round))
                    drawArc(Brush.sweepGradient(listOf(c1, c2, c1)), 0f, 360f * frac, false, tl, sz, style = Stroke(sw, cap = StrokeCap.Round))
                    val a = Math.toRadians((360f * frac).toDouble())
                    val r = d / 2f
                    drawCircle(Color.White, 5.dp.toPx(), Offset(size.width / 2f + (r * cos(a)).toFloat(), size.height / 2f + (r * sin(a)).toFloat()))
                }
            },
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                StopwatchText(ms)
                if (targetSec != null && targetSec > 0) Text("objetivo ${formatDuration(targetSec)}", color = Volt.textDim, style = VoltType.body)
            }
        }
        Spacer(Modifier.height(16.dp))
        if (running) {
            Row(
                Modifier.fillMaxWidth().height(58.dp).clip(CircleShape).background(Volt.danger.copy(alpha = 0.18f))
                    .pressScale { running = false; onStop((ms.longValue / 1000L).toInt().coerceAtLeast(1)); haptics.medium() },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Stop, null, tint = Volt.danger, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Text("Detener y usar tiempo", color = Volt.danger, style = VoltType.title)
            }
        } else {
            Row(
                Modifier.fillMaxWidth().height(58.dp).clip(CircleShape).background(Volt.gradient)
                    .pressScale { ms.longValue = 0L; running = true },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Timer, null, tint = Volt.onAccent, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp))
                Text(if (started) "Repetir cronómetro" else "Iniciar cronómetro", color = Volt.onAccent, style = VoltType.title)
            }
        }
    }
}

// ───────────────────────── Técnica: tarjetas desplegables ─────────────────────────

@Composable
private fun TechCard(n: Int, text: String) {
    var expanded by remember { mutableStateOf(false) }
    Row(
        Modifier.fillMaxWidth().animateContentSize(spring(0.85f, 500f)).clip(RoundedCornerShape(22.dp)).background(Volt.surface)
            .pressScale { expanded = !expanded }.padding(14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            Modifier.size(30.dp).clip(CircleShape).background(if (expanded) Volt.gradient else SolidColor(Volt.surface2)),
            contentAlignment = Alignment.Center,
        ) { Text("$n", color = if (expanded) Volt.onAccent else Volt.textDim, style = VoltType.numberS.copy(fontSize = 14.sp)) }
        Spacer(Modifier.width(12.dp))
        Text(text, color = if (expanded) Volt.text else Volt.textDim, style = VoltType.body, maxLines = if (expanded) Int.MAX_VALUE else 2, modifier = Modifier.weight(1f))
    }
}

@Composable
fun TechniqueSection(tech: String, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val steps = remember(tech) { tech.split("\n").map { it.trim().replace(Regex("^\\d+[.)]\\s*"), "") }.filter { it.isNotEmpty() } }
    val rot = animateFloatAsState(if (open) 180f else 0f, spring(0.8f, 400f), label = "techRot")
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable { open = !open }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Técnica", color = Volt.text, style = VoltType.h2, modifier = Modifier.weight(1f))
            Text("${steps.size} pasos", color = Volt.textDim, style = VoltType.body)
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = Volt.textDim, modifier = Modifier.graphicsLayer { rotationZ = rot.value })
        }
        AnimatedVisibility(open, enter = expandVertically(spring(0.85f, 400f)) + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                steps.forEachIndexed { i, s -> TechCard(i + 1, s) }
            }
        }
    }
}

// ───────────────────────── Lista de series de hoy ─────────────────────────

data class SetRow(val gi: Int, val log: WorkoutLog, val pr: Boolean)

@Composable
fun SetsList(rows: List<SetRow>, target: Int, type: String, onEdit: (SetRow) -> Unit, modifier: Modifier = Modifier) {
    val pending = (target - rows.size).coerceAtLeast(0)
    Column(modifier.fillMaxWidth().animateContentSize(spring(0.85f, 450f)), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        rows.forEachIndexed { i, r ->
            androidx.compose.runtime.key(r.log.date) {
                val l = r.log
                val main = when (type) {
                    T_ISOMETRIC -> formatDuration(l.duration ?: 0)
                    T_CARDIO -> "${(l.duration ?: 0) / 60} min"
                    T_BODYWEIGHT -> "${l.reps} reps"
                    else -> "${fmtNum(l.weight)} kg × ${l.reps}"
                }
                val sub = listOfNotNull(l.rir?.let { "RIR $it" }, l.rpe?.let { "RPE $it" }).joinToString(" · ")
                Row(
                    Modifier.fillMaxWidth().staggeredEntry(0).clip(RoundedCornerShape(22.dp)).background(Volt.surface)
                        .let { if (r.pr) it.gradientBorder(1.dp, RoundedCornerShape(22.dp), Brush.linearGradient(listOf(PrGold, Color.Transparent))) else it }
                        .pressScale { onEdit(r) }.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(34.dp).clip(CircleShape).background(Volt.gradient), contentAlignment = Alignment.Center) {
                        Text("${i + 1}", color = Volt.onAccent, style = VoltType.numberS.copy(fontSize = 15.sp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(main, color = Volt.text, style = VoltType.numberM.copy(fontSize = 22.sp, lineHeight = 26.sp))
                        if (sub.isNotEmpty()) Text(sub, color = Volt.textDim, style = VoltType.body)
                    }
                    if (r.pr) {
                        Icon(Icons.Filled.EmojiEvents, "Récord", tint = PrGold, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(Icons.Filled.Edit, "Editar serie ${i + 1}", tint = Volt.textDim.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                }
            }
        }
        repeat(pending) { k ->
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.03f)).padding(horizontal = 14.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(34.dp).clip(CircleShape).background(Volt.surface2), contentAlignment = Alignment.Center) {
                    Text("${rows.size + k + 1}", color = Volt.textDim, style = VoltType.numberS.copy(fontSize = 15.sp))
                }
                Spacer(Modifier.width(12.dp))
                Text("Pendiente", color = Volt.textDim.copy(alpha = 0.7f), style = VoltType.body)
            }
        }
    }
}

// ───────────────────────── Historial ─────────────────────────

@Composable
private fun PrBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(22.dp)).background(Volt.surface2).padding(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FieldLabel(label)
        Text(value, color = PrGold, style = VoltType.numberM)
    }
}

@Composable
fun HistorySection(history: List<ExerciseSession>, type: String, modifier: Modifier = Modifier) {
    var open by remember { mutableStateOf(false) }
    val isDuration = type == T_ISOMETRIC || type == T_CARDIO
    val rot = animateFloatAsState(if (open) 180f else 0f, spring(0.8f, 400f), label = "histRot")
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).clickable { open = !open }.padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Historial", color = Volt.text, style = VoltType.h2, modifier = Modifier.weight(1f))
            if (history.isNotEmpty()) Text("${history.size} sesiones", color = Volt.textDim, style = VoltType.body)
            Icon(Icons.Filled.KeyboardArrowDown, null, tint = Volt.textDim, modifier = Modifier.graphicsLayer { rotationZ = rot.value })
        }
        AnimatedVisibility(open, enter = expandVertically(spring(0.85f, 400f)) + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            if (history.isEmpty()) {
                Text("Sin registros anteriores en este ejercicio.", color = Volt.textDim, style = VoltType.body, modifier = Modifier.fillMaxWidth().padding(16.dp))
            } else {
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Volt.surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (isDuration) {
                            PrBox("PR tiempo", formatDuration(history.maxOf { it.maxDuration }), Modifier.weight(1f))
                            PrBox("Sesiones", history.size.toString(), Modifier.weight(1f))
                        } else if (type == T_BODYWEIGHT) {
                            PrBox("PR reps", history.maxOf { it.maxReps }.toString(), Modifier.weight(1f))
                            PrBox("Sesiones", history.size.toString(), Modifier.weight(1f))
                        } else {
                            PrBox("PR peso", "${fmtNum(history.maxOf { it.maxWeight })} kg", Modifier.weight(1f))
                            PrBox("PR reps", history.maxOf { it.maxReps }.toString(), Modifier.weight(1f))
                        }
                    }
                    FieldLabel(if (isDuration) "Progresión de tiempo" else if (type == T_BODYWEIGHT) "Progresión de repeticiones" else "Progresión de peso")
                    val labels = remember(history) { history.map { it.date } }
                    val values = remember(history, type) {
                        history.map { s -> (if (isDuration) s.maxDuration.toDouble() else if (type == T_BODYWEIGHT) s.maxReps.toDouble() else s.maxWeight).toFloat() }
                    }
                    ProgressLineChart(labels, values, if (isDuration) "s" else if (type == T_BODYWEIGHT) "" else " kg")
                    FieldLabel("Últimas sesiones")
                    history.asReversed().take(8).forEach { s ->
                        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.03f)).padding(horizontal = 12.dp, vertical = 10.dp)) {
                            Text(s.date, color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.weight(1.3f))
                            Text(
                                if (isDuration) formatDuration(s.maxDuration) else if (type == T_BODYWEIGHT) "${s.maxReps} reps" else "${fmtNum(s.maxWeight)} kg",
                                color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f),
                            )
                            Text("${s.sets} sets", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.weight(0.8f))
                            if (!isDuration && type == T_WEIGHTED) Text("Vol: ${s.totalVolume.roundToInt()}", color = Volt.textDim, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

/** Edición inline de una serie ya registrada. */
@Composable
fun EditSetCard(
    type: String, weight: String, reps: String, dur: String,
    onWeight: (String) -> Unit, onReps: (String) -> Unit, onDur: (String) -> Unit,
    valid: Boolean, onSave: () -> Unit, onDelete: () -> Unit, onClose: () -> Unit, modifier: Modifier = Modifier,
) {
    val isDuration = type == T_ISOMETRIC || type == T_CARDIO
    Column(
        modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Volt.surface2).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Editar serie", color = Volt.text, style = VoltType.title, modifier = Modifier.weight(1f))
            Text("Cerrar", color = Volt.textDim, style = VoltType.label, modifier = Modifier.clip(CircleShape).clickable(onClick = onClose).padding(10.dp))
        }
        if (isDuration) {
            FieldLabel(if (type == T_CARDIO) "Duración (min)" else "Duración (seg)")
            NumField(dur, onDur, Modifier.fillMaxWidth())
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel(if (type == T_BODYWEIGHT) "Peso ref. (kg)" else "Peso (kg)")
                    NumField(weight, onWeight, Modifier.fillMaxWidth(), decimal = true, readOnly = type == T_BODYWEIGHT)
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    FieldLabel("Reps")
                    NumField(reps, onReps, Modifier.fillMaxWidth())
                }
            }
        }
        Box(
            Modifier.fillMaxWidth().height(52.dp).clip(CircleShape).background(if (valid) Volt.gradient else SolidColor(Volt.surface))
                .let { if (valid) it.pressScale(onSave) else it },
            contentAlignment = Alignment.Center,
        ) { Text("Guardar cambios", color = if (valid) Volt.onAccent else Volt.textDim, style = VoltType.title) }
        Box(
            Modifier.fillMaxWidth().height(46.dp).clip(CircleShape).background(Volt.danger.copy(alpha = 0.14f)).pressScale(onDelete),
            contentAlignment = Alignment.Center,
        ) { Text("Eliminar serie", color = Volt.danger, style = VoltType.title.copy(fontSize = 14.sp)) }
    }
}

@Suppress("unused")
private fun absSafe(x: Float) = abs(x)
