package com.voltbody.app.ui.screens.calendar

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.AppState
import com.voltbody.app.data.Exercise
import com.voltbody.app.ui.*
import com.voltbody.app.ui.screens.home.STEPS_GOAL
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.util.WEEKDAY_LABELS
import com.voltbody.app.util.mapRoutineByWeekday
import com.voltbody.app.util.mondayFirstIndex
import com.voltbody.app.util.logLocalDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

val ES: Locale = Locale("es", "ES")
val DAY_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, d MMMM", ES)
val MONTH_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM yyyy", ES)
private val INITIALS = listOf("L", "M", "X", "J", "V", "S", "D")
private val CAL_OK = Color(0xFF34D399)

fun String.capFirst(): String = replaceFirstChar { it.uppercase() }

/** Selector Semanal/Mensual con indicador de degradado que se desliza (lectura en draw). */
@Composable
fun ModeToggle(monthly: Boolean, onChange: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val pos = animateFloatAsState(if (monthly) 1f else 0f, spring(0.75f, 420f), label = "modePos")
    val grad = Volt.gradient
    val track = Volt.surface
    val haptic = LocalHapticFeedback.current
    val c0 = animateColorAsState(if (!monthly) Volt.onAccent else Volt.textDim, tween(200), label = "modeC0")
    val c1 = animateColorAsState(if (monthly) Volt.onAccent else Volt.textDim, tween(200), label = "modeC1")
    Row(
        modifier.height(52.dp).clip(CircleShape).drawBehind {
            drawRect(track)
            val pad = 4.dp.toPx()
            val w = (size.width - pad * 2) / 2f
            drawRoundRect(grad, Offset(pad + w * pos.value, pad), Size(w, size.height - pad * 2), CornerRadius(size.height))
        },
    ) {
        listOf("Semanal", "Mensual").forEachIndexed { i, label ->
            Box(
                Modifier.weight(1f).fillMaxHeight().clickable {
                    if ((i == 1) != monthly) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onChange(i == 1) }
                },
                contentAlignment = Alignment.Center,
            ) { Text(label, color = if (i == 0) c0.value else c1.value, style = VoltType.title) }
        }
    }
}

/** Un dia de la tira: "hoy" = pastilla de degradado de marca; seleccionado = anillo claro; punto de actividad. */
@Composable
fun DayPill(date: LocalDate, selected: Boolean, isToday: Boolean, hasLogs: Boolean, hasRoutine: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sel = animateFloatAsState(if (selected) 1f else 0f, spring(0.55f, 500f), label = "pillSel")
    val grad = Volt.gradient
    val base = Volt.surface
    val ink = Volt.onAccent
    val shape = RoundedCornerShape(32.dp)
    val dot = when {
        hasLogs -> if (isToday) ink else CAL_OK
        hasRoutine -> if (isToday) ink.copy(alpha = 0.5f) else Volt.accent.copy(alpha = 0.7f)
        else -> Color.Transparent
    }
    Column(
        modifier.graphicsLayer { val s = 1f + 0.07f * sel.value; scaleX = s; scaleY = s }
            .clip(shape)
            .drawBehind { drawRect(base); if (isToday) drawRect(grad) }
            .border(if (selected && !isToday) 1.5.dp else 1.dp, if (selected && !isToday) Volt.text.copy(alpha = 0.6f) else Volt.border, shape)
            .pressScale(onClick).padding(vertical = 14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(INITIALS[mondayFirstIndex(date)], color = if (isToday) ink.copy(alpha = 0.7f) else Volt.textDim, style = VoltType.label)
        Spacer(Modifier.height(6.dp))
        Text("${date.dayOfMonth}", color = if (isToday) ink else Volt.text, style = VoltType.numberM)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
    }
}

/** Rejilla mensual (lunes primero). */
@Composable
fun MonthGrid(month: LocalDate, selected: LocalDate, st: AppState, onSelect: (LocalDate) -> Unit) {
    val first = month.withDayOfMonth(1)
    val offset = mondayFirstIndex(first)
    val days = first.lengthOfMonth()
    val rows = (offset + days + 6) / 7
    val today = LocalDate.now()
    val routineIdx = remember(st.routine) { mapRoutineByWeekday(st.routine).mapIndexedNotNull { i, d -> if (d != null) i else null }.toSet() }
    val logDays = remember(st.logs) { st.logs.mapNotNull { logLocalDate(it.date) }.toSet() }
    val grad = Volt.gradient
    val surface2 = Volt.surface2
    val ink = Volt.onAccent
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            INITIALS.forEach { Text(it, color = Volt.textDim, style = VoltType.label, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        repeat(rows) { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(7) { c ->
                    val n = r * 7 + c - offset + 1
                    if (n < 1 || n > days) Spacer(Modifier.weight(1f).aspectRatio(1f))
                    else {
                        val d = first.plusDays((n - 1).toLong())
                        val isToday = d == today
                        val sel = d == selected
                        val shape = RoundedCornerShape(16.dp)
                        val dotColor = when {
                            d in logDays -> if (isToday) ink else CAL_OK
                            mondayFirstIndex(d) in routineIdx -> if (isToday) ink.copy(alpha = 0.5f) else Volt.accent.copy(alpha = 0.7f)
                            else -> Color.Transparent
                        }
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(shape)
                                .drawBehind { drawRect(surface2); if (isToday) drawRect(grad) }
                                .then(if (sel && !isToday) Modifier.border(1.5.dp, Volt.text.copy(alpha = 0.6f), shape) else Modifier)
                                .clickable { onSelect(d) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$n", color = if (isToday) ink else Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.ExtraBold))
                            Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 5.dp).size(5.dp).clip(CircleShape).background(dotColor))
                        }
                    }
                }
            }
        }
    }
}

/** Tarjeta de pasos del dia seleccionado. */
@Composable
fun StepsDayCard(steps: Long, modifier: Modifier = Modifier) {
    BentoCard(modifier, padding = 18.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DirectionsWalk, null, tint = Volt.accent, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text("PASOS", color = Volt.textDim, style = VoltType.label)
        }
        Spacer(Modifier.height(12.dp))
        RollingNumber(steps.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(), style = VoltType.numberL)
        Text("meta $STEPS_GOAL", color = Volt.textDim, style = VoltType.body)
        Spacer(Modifier.height(12.dp))
        val p = animateFloatAsState((steps.toFloat() / STEPS_GOAL).coerceIn(0f, 1f), tween(900, easing = EaseOutExpo), label = "stp")
        val grad = Volt.gradient
        val track = Volt.surface2
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).drawBehind {
                drawRect(track)
                drawRect(grad, size = Size(size.width * p.value, size.height))
            },
        )
        Spacer(Modifier.height(8.dp))
        Text("${(steps * 100 / STEPS_GOAL).coerceAtMost(100)}% de la meta", color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp))
    }
}

@Composable
private fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.15f)).padding(horizontal = 12.dp, vertical = 5.dp)) {
        Text(text, color = color, style = VoltType.label.copy(letterSpacing = 0.sp))
    }
}

@Composable
private fun SectionHeader(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, label: String, trailing: @Composable () -> Unit = {}) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text(label, color = Volt.textDim, style = VoltType.label, modifier = Modifier.weight(1f))
        trailing()
    }
}

/** Detalle del dia (contenido de la hoja inferior): sesion, reprogramar, ejercicios, registro, comidas, peso y objetivo. */
@Composable
fun DayDetail(
    st: AppState, s: DayStats, isToday: Boolean, rescheduling: Boolean,
    onToggleReschedule: () -> Unit, onMove: (Int) -> Unit, onGoWorkout: () -> Unit,
) {
    val planned = s.planned
    val selIdx = mondayFirstIndex(s.date)
    val routinesByDay = remember(st.routine) { mapRoutineByWeekday(st.routine) }
    val nameById = remember(st.routine, st.customWorkout) {
        (st.routine.flatMap { it.exercises } + st.customWorkout).associate { it.id to it.name }
    }
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(s.date.format(DAY_FMT).capFirst(), color = Volt.text, style = VoltType.h1.copy(fontSize = 28.sp), modifier = Modifier.weight(1f))
            if (isToday) Pill("HOY", Volt.accent)
        }

        if (planned != null) {
            BentoCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(52.dp).clip(RoundedCornerShape(18.dp)).background(Volt.gradient), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.FitnessCenter, null, tint = Volt.onAccent)
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SESIÓN PLANIFICADA", color = Volt.textDim, style = VoltType.label)
                        Text(planned.focus, color = Volt.text, style = VoltType.h2)
                        Text("${planned.exercises.size} ejercicios programados", color = Volt.textDim, style = VoltType.body)
                    }
                    Pill("${s.sessionProgress}%", if (s.sessionProgress == 100) CAL_OK else if (s.sessionProgress > 0) Volt.accent else Volt.textDim)
                }
                Spacer(Modifier.height(16.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Volt.surface2).padding(16.dp)) {
                    Row {
                        Text("Progreso de series", color = Volt.textDim, style = VoltType.body, modifier = Modifier.weight(1f))
                        Text("${s.completedSets} / ${s.totalSets} series", color = Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.Bold))
                    }
                    Spacer(Modifier.height(10.dp))
                    val p = animateFloatAsState(s.sessionProgress / 100f, tween(900, easing = EaseOutExpo), label = "sess")
                    val grad = Volt.gradient
                    val track = Volt.bg
                    Box(
                        Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).drawBehind {
                            drawRect(track)
                            drawRect(grad, size = Size(size.width * p.value, size.height))
                        },
                    )
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.clip(CircleShape).background(if (rescheduling) Volt.accent.copy(alpha = 0.18f) else Volt.surface2)
                            .pressScale(onToggleReschedule).padding(horizontal = 16.dp, vertical = 12.dp),
                    ) {
                        Text(if (rescheduling) "Cancelar" else "Reprogramar", color = if (rescheduling) Volt.accent else Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.Bold))
                    }
                    Box(Modifier.clip(CircleShape).background(Volt.surface2).pressScale(onGoWorkout).padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("Entrenar en Rutina", color = Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.Bold))
                    }
                }
                androidx.compose.animation.AnimatedVisibility(rescheduling) {
                    Column(Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).border(1.dp, Volt.accent.copy(alpha = 0.4f), RoundedCornerShape(20.dp)).padding(12.dp)) {
                        Text("Selecciona un día libre para mover esta sesión:", color = Volt.textDim, style = VoltType.body)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            WEEKDAY_LABELS.forEachIndexed { i, d ->
                                val occupied = routinesByDay[i] != null
                                val current = i == selIdx
                                val enabled = !occupied && !current
                                Column(
                                    Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(14.dp))
                                        .background(if (current) Volt.accent.copy(alpha = 0.12f) else if (occupied) Volt.surface2 else CAL_OK.copy(alpha = 0.15f))
                                        .then(if (enabled) Modifier.clickable { onMove(i) } else Modifier),
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(d.short, color = if (current) Volt.accent else if (occupied) Volt.textDim.copy(alpha = 0.6f) else CAL_OK, style = VoltType.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold))
                                    Text(if (occupied) (if (current) "Actual" else "Ocupado") else "Libre", color = Volt.textDim, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                planned.exercises.groupBy { it.muscleGroup.ifBlank { "General" } }.forEach { (group, exs) ->
                    Pill(group.uppercase(), Volt.textDim)
                    Spacer(Modifier.height(8.dp))
                    exs.forEachIndexed { i, ex -> ExerciseRow(ex, s.setsByExercise[ex.id] ?: 0, i); Spacer(Modifier.height(6.dp)) }
                    Spacer(Modifier.height(6.dp))
                }
            }
        } else {
            BentoCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(Volt.surface2), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Volt.accent, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Día de Descanso", color = Volt.text, style = VoltType.h2)
                    Spacer(Modifier.height(6.dp))
                    Text("No hay entrenamiento programado para esta fecha. Aprovecha para recuperar o añade un ejercicio libre.", color = Volt.textDim, style = VoltType.body, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(16.dp))
                    PrimaryButton("+ Añadir Ejercicio Libre", onGoWorkout)
                }
            }
        }

        // Registro de entrenamiento
        BentoCard(Modifier.fillMaxWidth()) {
            SectionHeader(Icons.Filled.CheckCircle, CAL_OK, "REGISTRO DE ENTRENAMIENTO") {
                if (s.dayLogs.isNotEmpty()) Pill("${s.dayLogs.size} series", Volt.textDim)
            }
            Spacer(Modifier.height(12.dp))
            if (s.dayLogs.isEmpty()) Text("No hay registros de series guardadas para este día.", color = Volt.textDim, style = VoltType.body, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            s.dayLogs.forEachIndexed { i, log ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).staggeredEntry(i, 35).clip(RoundedCornerShape(18.dp)).background(Volt.surface2).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FitnessCenter, null, tint = Volt.textDim, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(nameById[log.exerciseId] ?: "Ejercicio", color = Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                    Text("${if (log.weight % 1.0 == 0.0) log.weight.toInt().toString() else log.weight.toString()} kg", color = Volt.textDim, style = VoltType.body)
                    Spacer(Modifier.width(8.dp))
                    Text("x${log.reps}", color = Volt.accent, style = VoltType.body.copy(fontWeight = FontWeight.ExtraBold))
                }
            }
        }

        // Comidas del dia
        if (s.meals.isNotEmpty()) {
            BentoCard(Modifier.fillMaxWidth()) {
                SectionHeader(Icons.Filled.Restaurant, Volt.orange, "COMIDAS DEL DÍA") {
                    Pill("${s.eatenIds.size}/${s.meals.size}", Volt.orange)
                }
                Spacer(Modifier.height(12.dp))
                s.meals.forEachIndexed { i, m ->
                    val eaten = m.id in s.eatenIds
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).staggeredEntry(i, 35).clip(RoundedCornerShape(18.dp)).background(Volt.surface2).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (eaten) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (eaten) Volt.accent else Volt.textDim, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.name, color = Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.Bold))
                            if (m.time.isNotBlank()) Text(m.time, color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp))
                        }
                        Text("${m.calories} kcal", color = if (eaten) Volt.orange else Volt.textDim, style = VoltType.body.copy(fontWeight = FontWeight.ExtraBold))
                    }
                }
            }
        }

        // Peso del dia
        s.weight?.let { w ->
            BentoCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MonitorWeight, null, tint = Volt.accent)
                    Spacer(Modifier.width(10.dp))
                    Text("Peso registrado", color = Volt.textDim, style = VoltType.body, modifier = Modifier.weight(1f))
                    Text("${"%.1f".format(w)} ", color = Volt.text, style = VoltType.numberM)
                    Text("kg", color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(bottom = 3.dp))
                }
            }
        }

        // Objetivo nutricional
        st.diet?.let { diet ->
            BentoCard(Modifier.fillMaxWidth()) {
                SectionHeader(Icons.Filled.LocalFireDepartment, Volt.orange, "OBJETIVO NUTRICIONAL") {
                    Text("Plan Activo", color = Volt.orange, style = VoltType.label)
                }
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    GradientText("${diet.dailyCalories}", style = VoltType.numberL)
                    Text(" kcal / día", color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(bottom = 5.dp))
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Proteína", "${diet.macros.protein}g", Modifier.weight(1f), Volt.text)
                    StatPill("Carbos", "${diet.macros.carbs}g", Modifier.weight(1f), Volt.text)
                    StatPill("Grasas", "${diet.macros.fat}g", Modifier.weight(1f), Volt.text)
                }
            }
        }
    }
}

@Composable
private fun ExerciseRow(ex: Exercise, doneSets: Int, index: Int) {
    val target = maxOf(1, ex.sets)
    val progress = minOf(100, Math.round(doneSets * 100f / target))
    val done = doneSets >= target
    Row(Modifier.fillMaxWidth().staggeredEntry(index, 35).clip(RoundedCornerShape(18.dp)).background(Volt.surface2).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(ex.name, color = Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.Bold))
            Text("$doneSets/$target series · ${ex.reps} reps", color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp))
        }
        Pill(if (done) "Hecho" else "$progress%", if (done) CAL_OK else Volt.textDim)
    }
}
