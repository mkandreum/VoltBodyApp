package com.voltbody.app.ui.v3

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.AppState
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.screens.calendar.MonthGrid
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.Haptics
import com.voltbody.app.util.logLocalDate
import com.voltbody.app.util.mapRoutineByWeekday
import com.voltbody.app.util.mondayFirstIndex
import com.voltbody.app.util.mondayOf
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale("es", "ES")
private const val STEP_GOAL = 8000
private const val WATER_GOAL = 2500

/** Resumen de un dia (o semana) calculado a partir del estado. */
data class DaySummary(
    val kcal: Int, val kcalTarget: Int, val steps: Int, val km: Double, val waterMl: Int,
    val sets: Int, val plannedExercises: Int, val doneExercises: Int, val focus: String?,
    val mealsEaten: Int, val meals: Int, val weight: Double?, val goalPct: Int,
)

fun summarize(st: AppState, days: List<LocalDate>): DaySummary {
    val meals = st.diet?.meals.orEmpty()
    val byDay = mapRoutineByWeekday(st.routine)
    val logsByDay = st.logs.groupBy { logLocalDate(it.date) }
    var kcal = 0; var steps = 0; var km = 0.0; var water = 0; var sets = 0; var planned = 0; var done = 0; var eaten = 0
    var pctSum = 0f
    days.forEach { d ->
        val key = d.toString()
        val ids = st.mealEaten[key].orEmpty()
        val dk = meals.filter { it.id in ids }.sumOf { it.calories }
        val m = st.metrics.firstOrNull { it.date == key }
        val dl = logsByDay[d].orEmpty()
        val r = byDay[mondayFirstIndex(d)]
        val p = r?.exercises?.size ?: 0
        val dn = r?.exercises?.count { ex -> dl.any { it.exerciseId == ex.id } } ?: 0
        kcal += dk; steps += m?.steps ?: 0; km += m?.distanceKm ?: 0.0; water += m?.waterMl ?: 0
        sets += dl.size; planned += p; done += dn; eaten += ids.size
        val parts = buildList {
            val target = st.diet?.dailyCalories ?: 0
            if (target > 0) add((dk / target.toFloat()).coerceAtMost(1f))
            add(((m?.steps ?: 0) / STEP_GOAL.toFloat()).coerceAtMost(1f))
            add(((m?.waterMl ?: 0) / WATER_GOAL.toFloat()).coerceAtMost(1f))
            if (p > 0) add(dn / p.toFloat())
        }
        pctSum += if (parts.isEmpty()) 0f else parts.average().toFloat()
    }
    val last = days.lastOrNull()
    return DaySummary(
        kcal, (st.diet?.dailyCalories ?: 0) * days.size, steps, km, water, sets, planned, done,
        if (days.size == 1 && last != null) byDay[mondayFirstIndex(last)]?.focus else null,
        eaten, meals.size * days.size,
        st.weightLogs.lastOrNull { w -> days.any { it.toString() == w.date } }?.weight,
        Math.round(pctSum / days.size.coerceAtLeast(1) * 100),
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityV3(repo: Repository) {
    val st by repo.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val today = remember { LocalDate.now() }
    var selected by remember { mutableStateOf(today) }
    var weekly by remember { mutableStateOf(false) }
    var showMonth by remember { mutableStateOf(false) }
    val days = remember(selected, weekly) {
        if (weekly) mondayOf(selected).let { m -> (0..6).map { m.plusDays(it.toLong()) } } else listOf(selected)
    }
    val sum = remember(st, days) { summarize(st, days) }
    val stepSeries = remember(st.metrics, selected) {
        (6 downTo 0).map { d -> (st.metrics.firstOrNull { it.date == selected.minusDays(d.toLong()).toString() }?.steps ?: 0).toFloat() }
    }
    val waterSeries = remember(st.metrics, selected) {
        (6 downTo 0).map { d -> (st.metrics.firstOrNull { it.date == selected.minusDays(d.toLong()).toString() }?.waterMl ?: 0).toFloat() }
    }

    Screen {
        Row(Modifier.fillMaxWidth().enter(0), verticalAlignment = Alignment.CenterVertically) {
            Text("Tus actividades", style = T.screen, color = Volt.text, modifier = Modifier.weight(1f))
            IconCircle(Icons.Outlined.CalendarMonth, desc = "Calendario") { showMonth = true }
        }

        DayStrip(today, selected, Modifier.enter(1)) { selected = it; Haptics.selection(ctx) }

        Row(Modifier.fillMaxWidth().padding(top = 6.dp).enter(2), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = Volt.orange, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Calorías", style = T.cardTitle, color = Volt.text, modifier = Modifier.weight(1f))
            SmallPill(if (weekly) "Semanal" else "Diario", icon = Icons.Filled.KeyboardArrowDown) { weekly = !weekly }
        }
        AnimatedContent(sum.kcal, transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(90)) }, label = "kcal", modifier = Modifier.enter(2)) { k ->
            ValueUnit(fmt(k), "kcal", style = T.heroXL)
        }

        Box(Modifier.fillMaxWidth().enter(3), contentAlignment = Alignment.Center) {
            BigDonut(sum.goalPct / 100f, Modifier.size(214.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    AnimatedContent(sum.goalPct, transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(90)) }, label = "pct") { p ->
                        Text("$p%", style = T.hero, color = Volt.text)
                    }
                    Text("Objetivos", style = T.caption, color = Volt.textDim)
                }
            }
        }

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).enter(4), horizontalArrangement = Arrangement.spacedBy(K.gap)) {
            Card(Modifier.weight(1f).fillMaxHeight(), onClick = { nav.push(Route.Section("health")) }) {
                CardHeader(Icons.AutoMirrored.Filled.DirectionsWalk, Volt.accent, "Pasos")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    ValueUnit(fmt(sum.steps), "km ${fmt1(sum.km)}", Modifier.weight(1f), unitBelow = true)
                    MiniBars(stepSeries)
                }
            }
            Card(Modifier.weight(1f).fillMaxHeight(), onClick = { nav.tab(Tab.Home) }) {
                CardHeader(Icons.Filled.WaterDrop, Volt.water, "Agua")
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    ValueUnit(fmt(sum.waterMl), "ml", Modifier.weight(1f), unitBelow = true)
                    WaveLine(waterSeries, Modifier.width(56.dp).height(34.dp), color = Volt.water)
                }
            }
        }

        // Resumen del dia/semana: filas compactas, cada una abre su pantalla.
        Card(Modifier.fillMaxWidth().enter(5), padding = 8.dp) {
            SummaryRow(Icons.Outlined.FitnessCenter, Volt.accent, "Entreno",
                when {
                    sum.sets > 0 -> "${sum.sets} series · ${sum.doneExercises}/${sum.plannedExercises} ejercicios"
                    sum.plannedExercises > 0 -> (sum.focus ?: "Planificado") + " · pendiente"
                    else -> "Descanso"
                },
            ) { if (!weekly && sum.plannedExercises > 0) nav.push(Route.Day(mondayFirstIndex(selected))) else nav.tab(Tab.Train) }
            SummaryRow(Icons.Outlined.Restaurant, Volt.orange, "Comidas", "${sum.mealsEaten} de ${sum.meals} hechas") { nav.tab(Tab.Food) }
            SummaryRow(Icons.Outlined.MonitorWeight, Volt.water, "Peso", sum.weight?.let { "${trim(it)} kg" } ?: "Sin registro") { nav.push(Route.Section("weight")) }
        }
    }

    if (showMonth) {
        ModalBottomSheet(onDismissRequest = { showMonth = false }, containerColor = Volt.surface) {
            Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
                Text(selected.month.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() } + " " + selected.year, style = T.section, color = Volt.text)
                Spacer(Modifier.height(12.dp))
                MonthGrid(selected.withDayOfMonth(1), selected, st) { d -> selected = d; weekly = false; showMonth = false; Haptics.selection(ctx) }
            }
        }
    }
}

@Composable
private fun SummaryRow(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color, title: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).tap(0.98f, onClick = onClick).padding(horizontal = 8.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(title, style = T.cardTitle, color = Volt.text, modifier = Modifier.width(76.dp))
        Text(value, style = T.caption, color = Volt.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
    }
}

/** Tira de dias del mockup: numeros sueltos y el seleccionado en pildora lima ("Hoy, 14 oct"). */
@Composable
fun DayStrip(today: LocalDate, selected: LocalDate, modifier: Modifier = Modifier, onSelect: (LocalDate) -> Unit) {
    val days = remember(today) { (-21..7).map { today.plusDays(it.toLong()) } }
    val state = rememberLazyListState(initialFirstVisibleItemIndex = (days.indexOf(today) - 2).coerceAtLeast(0))
    LazyRow(modifier.fillMaxWidth(), state = state, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        itemsIndexed(days, key = { _, d -> d.toEpochDay() }) { _, d ->
            val sel = d == selected
            val label = when {
                sel && d == today -> "Hoy, ${d.dayOfMonth} ${d.month.getDisplayName(TextStyle.SHORT, ES).trimEnd('.')}"
                sel -> "${d.dayOfWeek.getDisplayName(TextStyle.SHORT, ES).trimEnd('.').replaceFirstChar { it.uppercase() }} ${d.dayOfMonth}"
                else -> d.dayOfMonth.toString()
            }
            Box(
                Modifier.height(38.dp).clip(RoundedCornerShape(19.dp)).background(if (sel) Volt.accent else Color.Transparent)
                    .tap(0.92f, haptic = false) { onSelect(d) }.padding(horizontal = if (sel) 16.dp else 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(label, style = T.cardTitle.copy(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)),
                    color = when { sel -> Color(0xFF0A0A0A); d.isAfter(today) -> Volt.textDim.copy(alpha = 0.5f); else -> Volt.textDim }, maxLines = 1)
            }
        }
    }
}
