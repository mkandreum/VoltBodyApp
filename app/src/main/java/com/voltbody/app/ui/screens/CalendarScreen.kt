package com.voltbody.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.DailyMetric
import com.voltbody.app.data.Repository
import com.voltbody.app.data.WorkoutDay
import com.voltbody.app.ui.*
import com.voltbody.app.ui.screens.calendar.*
import com.voltbody.app.ui.screens.home.*
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Calendario / "Tus actividades". [onNavigate] recibe "workout" para saltar a la pestana de rutina (opcional).
 */
@Composable
fun CalendarScreen(repo: Repository, onNavigate: (String) -> Unit = {}) {
    val st by repo.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val health = rememberHealthState(repo)
    val today = remember { LocalDate.now() }

    var selectedStr by rememberSaveable { mutableStateOf(today.toString()) }
    var anchorStr by rememberSaveable { mutableStateOf(today.toString()) }
    var monthly by rememberSaveable { mutableStateOf(false) }
    var rescheduling by remember { mutableStateOf(false) }
    val selected = LocalDate.parse(selectedStr)
    val anchor = LocalDate.parse(anchorStr)

    val weekStart = mondayOf(anchor)
    val weekDays = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }
    val routinesByDay = remember(st.routine) { mapRoutineByWeekday(st.routine) }
    val logDays = remember(st.logs) { st.logs.mapNotNull { logLocalDate(it.date) }.toSet() }
    val streak = remember(st.logs, st.routine) { computeSmartStreak(st.logs, st.routine) }

    val water = st.waterOn(selected)
    val steps = if (selected == today) (health.snap?.steps ?: st.stepsOn(selected)) else st.stepsOn(selected)
    val stats = dayStats(st, selected, water, steps)
    val caloriesTarget = st.diet?.dailyCalories ?: 0

    fun select(d: LocalDate) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        selectedStr = d.toString(); rescheduling = false
    }

    fun moveSession(targetIdx: Int) {
        val planned = stats.planned ?: return
        val fromIdx = mondayFirstIndex(selected)
        if (targetIdx == fromIdx) { repo.toast("info", "Mismo día seleccionado", "Elige otro día para mover la sesión."); return }
        if (routinesByDay[targetIdx] != null) { repo.toast("info", "Conflicto detectado", "Ese día ya tiene entreno asignado."); return }
        val draft: MutableList<WorkoutDay?> = routinesByDay.toMutableList()
        draft[fromIdx] = null
        draft[targetIdx] = planned.copy(day = WEEKDAY_LABELS[targetIdx].full)
        val updated = draft.filterNotNull()
        rescheduling = false
        scope.launch {
            repo.saveProfile(withPlan = true) { it.copy(routine = updated) }
            repo.toast("success", "Sesión reprogramada", "${WEEKDAY_LABELS[fromIdx].full} → ${WEEKDAY_LABELS[targetIdx].full}")
        }
    }

    val viewHasToday = if (monthly) (anchor.year == today.year && anchor.month == today.month) else weekDays.contains(today)

    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Cabecera + racha
        item {
            Row(Modifier.fillMaxWidth().staggeredEntry(0), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Tus actividades", color = Volt.text, fontSize = 28.sp, fontWeight = FontWeight.Black)
                    Text("Planificación semanal y registro de actividad", color = Volt.textDim, fontSize = 12.sp)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(20.dp)).background(Volt.surface).border(1.dp, Volt.border, RoundedCornerShape(20.dp)).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = Volt.orange, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(6.dp))
                    Column {
                        Text("RACHA", color = Volt.textDim, fontSize = 9.sp, fontWeight = FontWeight.SemiBold)
                        Text("$streak días", color = Volt.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Selector semanal/mensual + navegacion
        item {
            Column(Modifier.staggeredEntry(1), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    VoltChip("Semanal", !monthly, { monthly = false })
                    VoltChip("Mensual", monthly, { monthly = true })
                    Spacer(Modifier.weight(1f))
                    AnimatedVisibility(!viewHasToday) {
                        Box(Modifier.clip(RoundedCornerShape(50)).background(Volt.accent.copy(.15f)).border(1.dp, Volt.accent.copy(.4f), RoundedCornerShape(50))
                            .pressScale { anchorStr = today.toString(); selectedStr = today.toString() }.padding(horizontal = 12.dp, vertical = 6.dp)) {
                            Text("Hoy", color = Volt.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NavArrow(Icons.Filled.ChevronLeft, "Anterior") { anchorStr = (if (monthly) anchor.minusMonths(1) else anchor.minusWeeks(1)).toString() }
                    Text(
                        anchor.format(MONTH_FMT).capFirst(), color = Volt.text, fontSize = 17.sp, fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                    )
                    NavArrow(Icons.Filled.ChevronRight, "Siguiente") { anchorStr = (if (monthly) anchor.plusMonths(1) else anchor.plusWeeks(1)).toString() }
                }
                AnimatedContent(
                    targetState = monthly,
                    transitionSpec = { (fadeIn() + slideInHorizontally { if (targetState) it / 4 else -it / 4 }) togetherWith (fadeOut() + slideOutHorizontally { if (targetState) -it / 4 else it / 4 }) using SizeTransform(clip = false) },
                    label = "calMode",
                ) { isMonthly ->
                    if (isMonthly) MonthGrid(anchor, selected, st) { select(it) }
                    else Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        weekDays.forEach { d ->
                            DayPill(
                                d, d == selected, d == today, d in logDays, routinesByDay[mondayFirstIndex(d)] != null,
                                { select(d) }, Modifier.weight(1f),
                            )
                        }
                    }
                }
            }
        }

        // Kcal + donut de objetivos
        item {
            VoltCard(Modifier.fillMaxWidth().staggeredEntry(2), padding = 20.dp) {
                Row(verticalAlignment = Alignment.Bottom) {
                    CountUpText(stats.consumedKcal.toFloat(), style = TextStyle(color = Volt.text, fontSize = 44.sp, fontWeight = FontWeight.Black))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.padding(bottom = 8.dp)) {
                        Text("kcal consumidas", color = Volt.textDim, fontSize = 12.sp)
                        if (caloriesTarget > 0) Text("de $caloriesTarget kcal", color = Volt.orange, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    DonutProgress(stats.objectivesPct / 100f, Modifier.size(240.dp), stroke = 28.dp, color = Volt.orange) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CountUpText(stats.objectivesPct.toFloat(), suffix = "%", style = TextStyle(color = Volt.text, fontSize = 48.sp, fontWeight = FontWeight.Black))
                            Text("Objetivos", color = Volt.textDim, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Entreno", if (stats.planned != null) "${stats.sessionProgress}%" else "Descanso", Modifier.weight(1f))
                    StatPill("Comidas", if (stats.meals.isNotEmpty()) "${stats.eatenIds.size}/${stats.meals.size}" else "—", Modifier.weight(1f), Volt.orange)
                    StatPill("Agua", "${(stats.water * 100 / WATER_GOAL_ML).coerceAtMost(100)}%", Modifier.weight(1f), Volt.water)
                }
            }
        }

        // Pasos y agua del dia
        item {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).staggeredEntry(3), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StepsDayCard(stats.steps, Modifier.weight(1f).fillMaxHeight())
                WaterTile(stats.water, { d ->
                    scope.launch { repo.saveMetrics(selected.toString(), DailyMetric(selected.toString(), waterMl = (water + d).coerceAtLeast(0))) }
                }, Modifier.weight(1f).fillMaxHeight())
            }
        }

        // Detalle del dia (transicion lateral segun direccion)
        item {
            AnimatedContent(
                targetState = selected,
                transitionSpec = {
                    val fwd = targetState.isAfter(initialState)
                    (fadeIn(androidx.compose.animation.core.tween(250)) + slideInHorizontally { if (fwd) it / 5 else -it / 5 }) togetherWith
                        (fadeOut(androidx.compose.animation.core.tween(150)) + slideOutHorizontally { if (fwd) -it / 5 else it / 5 }) using SizeTransform(clip = false)
                },
                label = "dayDetail",
            ) { d ->
                // El contenido saliente conserva su fecha; el entrante usa la actual
                val s = if (d == selected) stats else dayStats(st, d, st.waterOn(d), st.stepsOn(d))
                DayDetail(
                    st, s, d == today, rescheduling && d == selected,
                    { rescheduling = !rescheduling }, { moveSession(it) }, { onNavigate("workout") },
                )
            }
        }
    }
}

@Composable
private fun NavArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(Volt.surface).border(1.dp, Volt.border, CircleShape).pressScale(onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = Volt.text)
    }
}
