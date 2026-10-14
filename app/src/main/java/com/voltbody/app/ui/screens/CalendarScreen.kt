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
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.util.*
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Calendario / "Tus actividades". [onNavigate] recibe "workout" para saltar a la pestana de rutina (opcional).
 * Tocar un dia abre una hoja inferior con el detalle (entrenos, comidas, peso).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(repo: Repository, onNavigate: (String) -> Unit = {}) {
    val st by repo.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val health = rememberHealthState(repo)
    val today = remember { LocalDate.now() }

    var selectedStr by rememberSaveable { mutableStateOf(today.toString()) }
    var anchorStr by rememberSaveable { mutableStateOf(today.toString()) }
    var monthly by rememberSaveable { mutableStateOf(false) }
    var rescheduling by remember { mutableStateOf(false) }
    var showSheet by rememberSaveable { mutableStateOf(false) }
    val selected = remember(selectedStr) { LocalDate.parse(selectedStr) }
    val anchor = remember(anchorStr) { LocalDate.parse(anchorStr) }

    val weekStart = remember(anchor) { mondayOf(anchor) }
    val weekDays = remember(weekStart) { (0..6).map { weekStart.plusDays(it.toLong()) } }
    val routinesByDay = remember(st.routine) { mapRoutineByWeekday(st.routine) }
    val logDays = remember(st.logs) { st.logs.mapNotNull { logLocalDate(it.date) }.toSet() }
    val streak = remember(st.logs, st.routine) { computeSmartStreak(st.logs, st.routine) }

    val water = st.waterOn(selected)
    val steps = if (selected == today) (health.snap?.steps ?: st.stepsOn(selected)) else st.stepsOn(selected)
    val stats = remember(st, selected, water, steps) { dayStats(st, selected, water, steps) }
    val caloriesTarget = st.diet?.dailyCalories ?: 0
    val brand = Volt.gradient

    fun select(d: LocalDate) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        selectedStr = d.toString(); rescheduling = false; showSheet = true
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

    // Tira horizontal de dias con scroll centrado al dia seleccionado
    val stripState = rememberLazyListState()
    val itemPx = with(LocalDensity.current) { 62.dp.toPx() }
    LaunchedEffect(selectedStr, weekStart, monthly) {
        if (monthly) return@LaunchedEffect
        val idx = weekDays.indexOf(selected).takeIf { it >= 0 } ?: weekDays.indexOf(today).takeIf { it >= 0 } ?: 0
        val vw = stripState.layoutInfo.viewportSize.width
        if (vw > 0) stripState.animateScrollToItem(idx, -((vw - itemPx) / 2f).toInt()) else stripState.scrollToItem(idx)
    }

    LazyColumn(
        modifier = Modifier.statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Titular enorme + racha
        item(key = "head", contentType = "head") {
            Column(Modifier.staggeredEntry(0), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("PLAN Y REGISTRO", color = Volt.textDim, style = VoltType.label, modifier = Modifier.weight(1f))
                    Row(
                        Modifier.clip(CircleShape).background(Volt.surface).padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.LocalFireDepartment, null, tint = Volt.orange, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("$streak días", color = Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.ExtraBold))
                    }
                }
                KineticTitle(listOf("Tus", "actividades"), dimFromLine = 1, style = VoltType.display.copy(fontSize = 46.sp, lineHeight = 48.sp))
            }
        }

        // Selector semanal/mensual + navegacion + tira de dias
        item(key = "strip", contentType = "strip") {
            Column(Modifier.staggeredEntry(1), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                ModeToggle(monthly, { monthly = it }, Modifier.fillMaxWidth())
                Row(verticalAlignment = Alignment.CenterVertically) {
                    NavArrow(Icons.Filled.ChevronLeft, "Anterior") { anchorStr = (if (monthly) anchor.minusMonths(1) else anchor.minusWeeks(1)).toString() }
                    Text(
                        anchor.format(MONTH_FMT).capFirst(), color = Volt.text, style = VoltType.h2,
                        textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                    )
                    AnimatedVisibility(!viewHasToday) {
                        Box(
                            Modifier.padding(end = 8.dp).clip(CircleShape).background(brand)
                                .pressScale { anchorStr = today.toString(); selectedStr = today.toString() }.padding(horizontal = 14.dp, vertical = 10.dp),
                        ) { Text("Hoy", color = Volt.onAccent, style = VoltType.body.copy(fontWeight = FontWeight.ExtraBold)) }
                    }
                    NavArrow(Icons.Filled.ChevronRight, "Siguiente") { anchorStr = (if (monthly) anchor.plusMonths(1) else anchor.plusWeeks(1)).toString() }
                }
                AnimatedContent(
                    targetState = monthly,
                    transitionSpec = { (fadeIn() + slideInHorizontally { if (targetState) it / 4 else -it / 4 }) togetherWith (fadeOut() + slideOutHorizontally { if (targetState) -it / 4 else it / 4 }) using SizeTransform(clip = false) },
                    label = "calMode",
                ) { isMonthly ->
                    if (isMonthly) MonthGrid(anchor, selected, st) { select(it) }
                    else LazyRow(
                        state = stripState,
                        modifier = Modifier.layout { m, c ->
                            // la tira sangra hasta los bordes de la pantalla (compensa el padding de 20dp)
                            val extra = 40.dp.roundToPx()
                            val p = m.measure(c.copy(minWidth = c.maxWidth + extra, maxWidth = c.maxWidth + extra))
                            layout(c.maxWidth, p.height) { p.place(-extra / 2, 0) }
                        },
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(weekDays, key = { it.toString() }, contentType = { "day" }) { d ->
                            DayPill(
                                d, d == selected, d == today, d in logDays, routinesByDay[mondayFirstIndex(d)] != null,
                                { select(d) }, Modifier.width(54.dp),
                            )
                        }
                    }
                }
            }
        }

        // Kcal gigantes + donut enorme de objetivos
        item(key = "hero", contentType = "hero") {
            BentoCard(Modifier.fillMaxWidth().staggeredEntry(2), hero = true, padding = 24.dp) {
                Text("CONSUMIDAS", color = Volt.textDim, style = VoltType.label)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    RollingNumber(stats.consumedKcal, style = VoltType.numberXL, brush = brand)
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.padding(bottom = 10.dp)) {
                        Text("kcal", color = Volt.textDim, style = VoltType.title)
                        if (caloriesTarget > 0) Text("de $caloriesTarget", color = Volt.orange, style = VoltType.body.copy(fontWeight = FontWeight.Bold))
                    }
                }
                Spacer(Modifier.height(20.dp))
                Box(Modifier.fillMaxWidth().glowBackdrop(Volt.accent, 0.18f), contentAlignment = Alignment.Center) {
                    DonutProgress(
                        stats.objectivesPct / 100f,
                        Modifier.fillMaxWidth().widthIn(max = 320.dp).aspectRatio(1f),
                        stroke = 34.dp, color = Volt.accent,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            RollingNumber(stats.objectivesPct, style = VoltType.numberXL.copy(fontSize = 72.sp, lineHeight = 74.sp), suffix = "%", brush = brand)
                            Text("Objetivos", color = Volt.textDim, style = VoltType.title)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Entreno", if (stats.planned != null) "${stats.sessionProgress}%" else "Descanso", Modifier.weight(1f))
                    StatPill("Comidas", if (stats.meals.isNotEmpty()) "${stats.eatenIds.size}/${stats.meals.size}" else "—", Modifier.weight(1f), Volt.orange)
                    StatPill("Agua", "${(stats.water * 100 / WATER_GOAL_ML).coerceAtMost(100)}%", Modifier.weight(1f), Volt.water)
                }
            }
        }

        // Pasos y agua del dia
        item(key = "stepsWater", contentType = "stepsWater") {
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).staggeredEntry(3), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StepsDayCard(stats.steps, Modifier.weight(1f).fillMaxHeight())
                WaterTile(stats.water, { d ->
                    scope.launch { repo.saveMetrics(selected.toString(), DailyMetric(selected.toString(), waterMl = (water + d).coerceAtLeast(0))) }
                }, Modifier.weight(1f).fillMaxHeight())
            }
        }

        // Acceso al detalle del dia
        item(key = "detail", contentType = "detail") {
            Box(Modifier.staggeredEntry(4)) {
                PrimaryButton("Ver detalle del día", { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); showSheet = true })
            }
        }
    }

    if (showSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showSheet = false; rescheduling = false },
            sheetState = sheetState,
            containerColor = Volt.bg,
            contentColor = Volt.text,
            shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp),
        ) {
            Column(
                Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, end = 20.dp, bottom = 24.dp).navigationBarsPadding(),
            ) {
                DayDetail(
                    st, stats, selected == today, rescheduling,
                    { rescheduling = !rescheduling }, { moveSession(it) },
                    { showSheet = false; rescheduling = false; onNavigate("workout") },
                )
            }
        }
    }
}

@Composable
private fun NavArrow(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Box(Modifier.size(48.dp).clip(CircleShape).background(Volt.surface).pressScale(onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = Volt.text)
    }
}
