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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.AppState
import com.voltbody.app.data.Exercise
import com.voltbody.app.ui.*
import com.voltbody.app.ui.screens.home.STEPS_GOAL
import com.voltbody.app.ui.theme.Volt
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

fun String.capFirst(): String = replaceFirstChar { it.uppercase() }

/** Un dia de la tira semanal: pill lima si seleccionado, borde si es hoy, punto de actividad. */
@Composable
fun DayPill(date: LocalDate, selected: Boolean, isToday: Boolean, hasLogs: Boolean, hasRoutine: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val bg by animateColorAsState(if (selected) Volt.accent else Volt.surface, tween(250), label = "pillBg")
    val sc by animateFloatAsState(if (selected) 1.06f else 1f, spring(0.5f, 500f), label = "pillSc")
    Column(
        modifier.scale(sc).clip(RoundedCornerShape(22.dp)).background(bg)
            .then(if (isToday && !selected) Modifier.border(1.5.dp, Volt.accent.copy(.7f), RoundedCornerShape(22.dp)) else Modifier.border(1.dp, Volt.border, RoundedCornerShape(22.dp)))
            .pressScale(onClick).padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(INITIALS[mondayFirstIndex(date)], color = if (selected) Color.Black.copy(.7f) else Volt.textDim, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Text("${date.dayOfMonth}", color = if (selected) Color.Black else Volt.text, fontSize = 17.sp, fontWeight = FontWeight.Black)
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.size(6.dp).clip(CircleShape).background(
                when {
                    hasLogs -> if (selected) Color.Black else Color(0xFF34D399)
                    hasRoutine -> if (selected) Color.Black.copy(.5f) else Volt.accent.copy(.6f)
                    else -> Color.Transparent
                },
            ),
        )
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
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth()) {
            INITIALS.forEach { Text(it, color = Volt.textDim, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.weight(1f)) }
        }
        repeat(rows) { r ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(7) { c ->
                    val n = r * 7 + c - offset + 1
                    if (n < 1 || n > days) Spacer(Modifier.weight(1f).aspectRatio(1f))
                    else {
                        val d = first.plusDays((n - 1).toLong())
                        val sel = d == selected
                        val bg by animateColorAsState(if (sel) Volt.accent else Volt.surface2, tween(200), label = "mbg")
                        Box(
                            Modifier.weight(1f).aspectRatio(1f).clip(RoundedCornerShape(14.dp)).background(bg)
                                .then(if (d == today && !sel) Modifier.border(1.5.dp, Volt.accent.copy(.7f), RoundedCornerShape(14.dp)) else Modifier)
                                .clickable { onSelect(d) },
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("$n", color = if (sel) Color.Black else Volt.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            Box(
                                Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(5.dp).clip(CircleShape).background(
                                    when {
                                        d in logDays -> if (sel) Color.Black else Color(0xFF34D399)
                                        mondayFirstIndex(d) in routineIdx -> if (sel) Color.Black.copy(.5f) else Volt.accent.copy(.6f)
                                        else -> Color.Transparent
                                    },
                                ),
                            )
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
    VoltCard(modifier, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DirectionsWalk, null, tint = Volt.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Pasos", color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        CountUpText(steps.toFloat(), style = TextStyle(color = Volt.text, fontSize = 26.sp, fontWeight = FontWeight.Black))
        Text("meta $STEPS_GOAL", color = Volt.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(10.dp))
        val p by animateFloatAsState((steps.toFloat() / STEPS_GOAL).coerceIn(0f, 1f), tween(800), label = "stp")
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Volt.surface2)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape(4.dp)).background(Volt.accent))
        }
        Spacer(Modifier.height(6.dp))
        Text("${(steps * 100 / STEPS_GOAL).coerceAtMost(100)}% de la meta", color = Volt.textDim, fontSize = 11.sp)
    }
}

@Composable
private fun Pill(text: String, color: Color, modifier: Modifier = Modifier) {
    Box(modifier.clip(RoundedCornerShape(50)).background(color.copy(.15f)).border(1.dp, color.copy(.35f), RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 4.dp)) {
        Text(text, color = color, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

/** Detalle del dia: sesion planificada, reprogramar, ejercicios, registro, comidas, peso y objetivo nutricional. */
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
            Icon(Icons.Filled.CalendarMonth, null, tint = Volt.accent)
            Spacer(Modifier.width(8.dp))
            Text(s.date.format(DAY_FMT).capFirst(), color = Volt.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (isToday) Pill("HOY", Volt.accent)
        }

        if (planned != null) {
            VoltCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(RoundedCornerShape(16.dp)).background(Volt.surface2), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.FitnessCenter, null, tint = Volt.accent)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("SESIÓN PLANIFICADA", color = Volt.textDim, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                        Text(planned.focus, color = Volt.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text("${planned.exercises.size} ejercicios programados", color = Volt.textDim, fontSize = 12.sp)
                    }
                    Pill("${s.sessionProgress}%", if (s.sessionProgress == 100) Color(0xFF34D399) else if (s.sessionProgress > 0) Volt.accent else Volt.textDim)
                }
                Spacer(Modifier.height(14.dp))
                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Volt.surface2).padding(14.dp)) {
                    Row {
                        Text("🎯 Progreso de series", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("${s.completedSets} / ${s.totalSets} series", color = Volt.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(8.dp))
                    val p by animateFloatAsState(s.sessionProgress / 100f, tween(900), label = "sess")
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Volt.bg)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape(5.dp)).background(Volt.accent))
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        Modifier.clip(RoundedCornerShape(14.dp)).background(if (rescheduling) Volt.accent.copy(.18f) else Volt.surface2)
                            .border(1.dp, if (rescheduling) Volt.accent.copy(.5f) else Volt.border, RoundedCornerShape(14.dp))
                            .pressScale(onToggleReschedule).padding(horizontal = 14.dp, vertical = 9.dp),
                    ) {
                        Text(if (rescheduling) "Cancelar reprogramación" else "↻ Reprogramar sesión", color = if (rescheduling) Volt.accent else Volt.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Box(Modifier.clip(RoundedCornerShape(14.dp)).background(Volt.surface2).border(1.dp, Volt.border, RoundedCornerShape(14.dp)).pressScale(onGoWorkout).padding(horizontal = 14.dp, vertical = 9.dp)) {
                        Text("Entrenar en Rutina", color = Volt.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                androidx.compose.animation.AnimatedVisibility(rescheduling) {
                    Column(Modifier.padding(top = 12.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp)).border(1.dp, Volt.accent.copy(.4f), RoundedCornerShape(16.dp)).padding(12.dp)) {
                        Text("Selecciona un día libre para mover esta sesión:", color = Volt.textDim, fontSize = 12.sp)
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                            WEEKDAY_LABELS.forEachIndexed { i, d ->
                                val occupied = routinesByDay[i] != null
                                val current = i == selIdx
                                val enabled = !occupied && !current
                                Column(
                                    Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp))
                                        .background(if (current) Volt.accent.copy(.12f) else if (occupied) Volt.surface2 else Color(0xFF34D399).copy(.15f))
                                        .then(if (enabled) Modifier.clickable { onMove(i) } else Modifier),
                                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                                ) {
                                    Text(d.short, color = if (current) Volt.accent else if (occupied) Volt.textDim.copy(.6f) else Color(0xFF6EE7B7), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    Text(if (occupied) (if (current) "Actual" else "Ocupado") else "Libre", color = Volt.textDim, fontSize = 8.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                planned.exercises.groupBy { it.muscleGroup.ifBlank { "General" } }.forEach { (group, exs) ->
                    Pill(group.uppercase(), Volt.textDim)
                    Spacer(Modifier.height(6.dp))
                    exs.forEachIndexed { i, ex -> ExerciseRow(ex, s.setsByExercise[ex.id] ?: 0, i); Spacer(Modifier.height(6.dp)) }
                    Spacer(Modifier.height(6.dp))
                }
            }
        } else {
            VoltCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.size(64.dp).clip(RoundedCornerShape(22.dp)).background(Volt.surface2), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.AutoAwesome, null, tint = Volt.accent, modifier = Modifier.size(28.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                    Text("Día de Descanso", color = Volt.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("No hay entrenamiento programado para esta fecha. Aprovecha para recuperar o añade un ejercicio libre.", color = Volt.textDim, fontSize = 12.sp, textAlign = TextAlign.Center)
                    Spacer(Modifier.height(14.dp))
                    PrimaryButton("+ Añadir Ejercicio Libre", onGoWorkout)
                }
            }
        }

        // Registro de entrenamiento
        VoltCard(Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF34D399), modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("REGISTRO DE ENTRENAMIENTO", color = Volt.textDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (s.dayLogs.isNotEmpty()) Pill("${s.dayLogs.size} series", Volt.textDim)
            }
            Spacer(Modifier.height(10.dp))
            if (s.dayLogs.isEmpty()) Text("No hay registros de series guardadas para este día.", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
            s.dayLogs.forEachIndexed { i, log ->
                Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).staggeredEntry(i, 35).clip(RoundedCornerShape(16.dp)).background(Volt.surface2).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FitnessCenter, null, tint = Volt.textDim, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(10.dp))
                    Text(nameById[log.exerciseId] ?: "Ejercicio", color = Volt.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("${if (log.weight % 1.0 == 0.0) log.weight.toInt().toString() else log.weight.toString()} kg", color = Volt.textDim, fontSize = 12.sp)
                    Spacer(Modifier.width(8.dp))
                    Text("x${log.reps}", color = Volt.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Comidas del dia
        if (s.meals.isNotEmpty()) {
            VoltCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Restaurant, null, tint = Volt.orange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("COMIDAS DEL DÍA", color = Volt.textDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Pill("${s.eatenIds.size}/${s.meals.size}", Volt.orange)
                }
                Spacer(Modifier.height(10.dp))
                s.meals.forEachIndexed { i, m ->
                    val eaten = m.id in s.eatenIds
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp).staggeredEntry(i, 35).clip(RoundedCornerShape(16.dp)).background(Volt.surface2).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(if (eaten) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked, null, tint = if (eaten) Volt.accent else Volt.textDim, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(m.name, color = Volt.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                            if (m.time.isNotBlank()) Text(m.time, color = Volt.textDim, fontSize = 11.sp)
                        }
                        Text("${m.calories} kcal", color = if (eaten) Volt.orange else Volt.textDim, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Peso del dia
        s.weight?.let { w ->
            VoltCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.MonitorWeight, null, tint = Volt.accent)
                    Spacer(Modifier.width(10.dp))
                    Text("Peso registrado", color = Volt.textDim, modifier = Modifier.weight(1f))
                    Text("${"%.1f".format(w)} kg", color = Volt.text, fontWeight = FontWeight.Black, fontSize = 20.sp)
                }
            }
        }

        // Objetivo nutricional
        st.diet?.let { diet ->
            VoltCard(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = Volt.orange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("OBJETIVO NUTRICIONAL", color = Volt.textDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    Text("Plan Activo", color = Volt.orange, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(10.dp))
                Text("${diet.dailyCalories} kcal / día", color = Volt.text, fontSize = 24.sp, fontWeight = FontWeight.Black)
                Spacer(Modifier.height(10.dp))
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
    Row(Modifier.fillMaxWidth().staggeredEntry(index, 35).clip(RoundedCornerShape(16.dp)).background(Volt.surface2).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(ex.name, color = Volt.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text("$doneSets/$target series · ${ex.reps} reps", color = Volt.textDim, fontSize = 11.sp)
        }
        Pill(if (done) "Hecho" else "$progress%", if (done) Color(0xFF34D399) else Volt.textDim)
    }
}
