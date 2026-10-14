package com.voltbody.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.AppState
import com.voltbody.app.data.Repository
import com.voltbody.app.data.WorkoutDay
import com.voltbody.app.ui.BentoCard
import com.voltbody.app.ui.GlowRing
import com.voltbody.app.ui.KineticTitle
import com.voltbody.app.ui.SectionTitle
import com.voltbody.app.ui.VoltChip
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.pressScaleIf
import com.voltbody.app.ui.screens.workout.*
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.util.WEEKDAY_LABELS
import com.voltbody.app.util.computeFatigueIndex
import com.voltbody.app.util.fatigueStatusColor
import com.voltbody.app.util.fatigueStatusLabel
import com.voltbody.app.util.logDayKey
import com.voltbody.app.util.mapRoutineByWeekday
import com.voltbody.app.util.mondayFirstIndex
import com.voltbody.app.util.mondayOf
import com.voltbody.app.util.rememberHaptics
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.LocalDate

private fun norm(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").trim()

/** Porción del estado: solo recompone cuando cambia ese campo (no se recolecta AppState entero). */
@Composable
private fun <T> rememberSlice(repo: Repository, select: (AppState) -> T): State<T> {
    val flow = remember(repo) { repo.state.map(select).distinctUntilChanged() }
    return flow.collectAsStateWithLifecycle(select(repo.state.value))
}

/** Tarjeta grande de un día de la semana: el foco del día en tipografía enorme. */
@Composable
private fun WeekCard(
    label: String, dayNum: Int, focus: String?, exCount: Int, progress: Int, selected: Boolean, isToday: Boolean,
    isSource: Boolean, editing: Boolean, enabled: Boolean, onClick: () -> Unit,
) {
    val sel = animateFloatAsState(if (selected) 1f else 0f, spring(0.8f, 400f), label = "wkSel")
    val grad = Volt.gradient
    val surf = Volt.surface
    val srcColor = Volt.accent2
    val shape = RoundedCornerShape(32.dp)
    val ink = animateColorAsState(if (selected) Volt.onAccent else Volt.text, tween(200), label = "wkInk")
    val dim = animateColorAsState(if (selected) Volt.onAccent.copy(alpha = 0.7f) else Volt.textDim, tween(200), label = "wkDim")
    Column(
        Modifier.width(164.dp).height(232.dp).pressScaleIf(enabled, 0.95f, onClick).clip(shape)
            .drawBehind {
                drawRect(surf)
                drawRect(grad, alpha = sel.value)
                if (isSource) drawRect(srcColor.copy(alpha = 0.28f))
            }
            .border(1.dp, Volt.border, shape).padding(18.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label.uppercase(), color = dim.value, style = VoltType.label, modifier = Modifier.weight(1f))
            if (isToday) Text("HOY", color = ink.value, style = VoltType.label, modifier = Modifier.clip(CircleShape).background(ink.value.copy(alpha = 0.14f)).padding(horizontal = 8.dp, vertical = 3.dp))
            else Text("$dayNum", color = dim.value, style = VoltType.label)
        }
        if (focus != null) {
            Text(focus, color = ink.value, style = VoltType.h1.copy(fontSize = 28.sp, lineHeight = 30.sp), maxLines = 4, overflow = TextOverflow.Ellipsis)
        } else {
            Text(if (editing) "Mover aquí" else "Descanso", color = dim.value.copy(alpha = if (editing) 1f else 0.6f), style = VoltType.h2)
        }
        if (focus != null) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("$exCount ejercicios", color = dim.value, style = VoltType.body)
                AnimatedProgressBar(progress / 100f, Modifier.fillMaxWidth(), height = 5.dp, color = if (selected) Volt.onAccent.copy(alpha = 0.85f) else null)
            }
        } else Spacer(Modifier.height(5.dp))
    }
}

/**
 * Pestaña Rutina: semana en tarjetas grandes, entreno del día con progreso, detalle (Full Body burn), sesión activa a pantalla
 * completa por ejercicio, constructor de entreno personalizado desde la biblioteca e índice de fatiga semanal.
 */
@Composable
fun WorkoutScreen(repo: Repository) {
    val routine by rememberSlice(repo) { it.routine }
    val logs by rememberSlice(repo) { it.logs }
    val custom by rememberSlice(repo) { it.customWorkout }
    val profile by rememberSlice(repo) { it.profile }
    val authToken by rememberSlice(repo) { it.authToken }
    val haptics = rememberHaptics()
    val today = remember { LocalDate.now() }
    val todayIdx = mondayFirstIndex(today)

    var selectedDay by rememberSaveable { mutableIntStateOf(todayIdx) }
    var editingDays by remember { mutableStateOf(false) }
    var moveSource by remember { mutableStateOf<Int?>(null) }
    var detailDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var session by remember { mutableStateOf<SessionSpec?>(null) }
    var group by rememberSaveable { mutableStateOf("Todos") }

    val routinesByDay = remember(routine) { mapRoutineByWeekday(routine) }
    val activeDays = remember(routinesByDay) { routinesByDay.mapIndexedNotNull { i, d -> if (d != null) i else null } }
    val selectedRoutine: WorkoutDay? = routinesByDay.getOrNull(selectedDay)

    // Si el día seleccionado no tiene plan, salta a hoy o al primer día activo
    LaunchedEffect(routinesByDay, selectedDay) {
        if (routinesByDay.getOrNull(selectedDay) != null) return@LaunchedEffect
        if (routinesByDay.getOrNull(todayIdx) != null) selectedDay = todayIdx
        else if (activeDays.isNotEmpty()) selectedDay = activeDays.first()
    }

    // Enriquecer GIFs en silencio (el servidor solo rellena vacíos). Corrige el bug de la web que descartaba el resultado.
    LaunchedEffect(authToken) {
        if (authToken == null) return@LaunchedEffect
        val current = repo.state.value.routine
        if (current.isEmpty() || !routineNeedsEnrichment(current)) return@LaunchedEffect
        val enriched = repo.enrichRoutine(current)
        val changed = enriched.size == current.size && enriched.indices.any { di ->
            enriched[di].exercises.size == current[di].exercises.size &&
                enriched[di].exercises.indices.any { ei -> enriched[di].exercises[ei].gifUrl != current[di].exercises[ei].gifUrl }
        }
        if (changed) repo.saveProfile(withPlan = true) { it.copy(routine = enriched) }
    }

    // Series por día de la semana (lun..dom) y % de avance: se calcula una vez por cambio de logs/rutina
    val weekKeys = remember(today) { (0..6).map { mondayOf(today).plusDays(it.toLong()).toString() } }
    val setsByDay = remember(logs, weekKeys) {
        val by = logs.groupBy { logDayKey(it.date) }
        weekKeys.map { k -> by[k].orEmpty().groupingBy { it.exerciseId }.eachCount() }
    }
    val dayProgress = remember(setsByDay, routinesByDay) {
        routinesByDay.mapIndexed { i, d ->
            val planned = d?.exercises?.sumOf { it.targetSets() } ?: 0
            val done = d?.exercises?.sumOf { minOf(setsByDay[i][it.id] ?: 0, it.targetSets()) } ?: 0
            if (planned > 0) done * 100 / planned else 0
        }
    }
    val setsByEx = setsByDay[selectedDay]
    val exercises = selectedRoutine?.exercises.orEmpty()
    val plannedSets = exercises.sumOf { it.targetSets() }
    val completedSets = exercises.sumOf { minOf(setsByEx[it.id] ?: 0, it.targetSets()) }
    val progress = dayProgress.getOrElse(selectedDay) { 0 }
    val etaMinutes = ((plannedSets - completedSets) * 2).coerceAtLeast(0)
    val estimatedMinutes = (profile.sessionMinutes?.takeIf { it > 0 } ?: (plannedSets * 3).coerceAtLeast(10))

    val sess = profile.weeklySpecialSession
    val isSpecialToday = sess?.enabled == true && norm(sess.day) == norm(WEEKDAY_LABELS[todayIdx].full)

    fun openSession(day: WorkoutDay, start: Int) {
        haptics.medium()
        if (day.exercises.isEmpty()) return
        session = SessionSpec(day.focus.ifBlank { "Entrenamiento" }, day.exercises, start.coerceIn(0, day.exercises.size - 1))
    }

    fun moveTrainingDay(src: Int, target: Int) {
        val source = routinesByDay[src] ?: return
        val draft = routinesByDay.toMutableList()
        draft[src] = null
        draft[target] = source.copy(day = WEEKDAY_LABELS[target].full)
        val updated = draft.filterNotNull()
        selectedDay = target; moveSource = null; editingDays = false
        appWorkScope.launch {
            repo.saveProfile(withPlan = true) { it.copy(routine = updated) }
            repo.toast("success", "Días de entreno actualizados", "${WEEKDAY_LABELS[src].full} movido a ${WEEKDAY_LABELS[target].full}.")
        }
    }

    fun onWeekdayTap(i: Int) {
        val has = routinesByDay.getOrNull(i) != null
        haptics.selection()
        if (!editingDays) { if (has) selectedDay = i; return }
        val src = moveSource
        if (src == null) {
            if (!has) { repo.toast("info", "Elige un día con entreno", "Primero selecciona el día que quieres mover."); return }
            moveSource = i; return
        }
        if (i == src) { moveSource = null; return }
        if (has) { repo.toast("info", "Día ocupado", "Selecciona un día bloqueado para mover el entreno."); return }
        moveTrainingDay(src, i)
    }

    val weekState = rememberLazyListState(initialFirstVisibleItemIndex = (selectedDay - 1).coerceAtLeast(0))
    LaunchedEffect(selectedDay) { weekState.animateScrollToItem((selectedDay - 1).coerceAtLeast(0)) }

    val fatigue = remember(logs, routine, custom) { computeFatigueIndex(logs, routine, today, custom) }
    val groups = remember { listOf("Todos") + defaultExerciseLibrary.map { it.muscleGroup }.distinct() }
    val filtered = remember(group) { if (group == "Todos") defaultExerciseLibrary else defaultExerciseLibrary.filter { it.muscleGroup == group } }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Cabecera
        item(key = "header", contentType = "header") {
            Column {
                Text("RUTINA", color = Volt.accent, style = VoltType.label)
                Spacer(Modifier.height(6.dp))
                KineticTitle(listOf("Tu semana", "en movimiento"), dimFromLine = 1, style = VoltType.display.copy(fontSize = 40.sp, lineHeight = 42.sp))
            }
        }

        // Semana de entrenamiento: tarjetas grandes
        item(key = "week", contentType = "week") {
            Column(Modifier.staggeredEntry(1), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (editingDays) "Toca un día activo y luego un día de descanso para moverlo."
                        else "Elige un día. Los de descanso quedan bloqueados.",
                        color = Volt.textDim, style = VoltType.body, modifier = Modifier.weight(1f),
                    )
                    Spacer(Modifier.width(8.dp))
                    VoltChip(if (editingDays) "Cancelar" else "Editar días", selected = editingDays, onClick = { editingDays = !editingDays; moveSource = null })
                }
                LazyRow(
                    state = weekState, horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(end = 20.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    items(7, key = { it }, contentType = { "weekCard" }) { i ->
                        val d = routinesByDay.getOrNull(i)
                        WeekCard(
                            label = WEEKDAY_LABELS[i].short, dayNum = mondayOf(today).plusDays(i.toLong()).dayOfMonth,
                            focus = d?.focus?.ifBlank { "Entrenamiento" }, exCount = d?.exercises?.size ?: 0,
                            progress = dayProgress.getOrElse(i) { 0 }, selected = selectedDay == i, isToday = i == todayIdx,
                            isSource = moveSource == i, editing = editingDays, enabled = editingDays || d != null,
                            onClick = { onWeekdayTap(i) },
                        )
                    }
                }
            }
        }

        // Héroe del día seleccionado
        item(key = "hero", contentType = "hero") {
            BentoCard(Modifier.fillMaxWidth().staggeredEntry(2), hero = true, padding = 24.dp) {
                AnimatedContent(
                    targetState = selectedDay,
                    transitionSpec = { (fadeIn(tween(260)) + slideInVertically(spring(0.85f, 380f)) { it / 14 }) togetherWith fadeOut(tween(120)) },
                    label = "dayHero",
                ) { d ->
                    val r = routinesByDay.getOrNull(d)
                    val exs = r?.exercises.orEmpty()
                    val pl = exs.sumOf { it.targetSets() }
                    val dn = exs.sumOf { minOf(setsByDay[d][it.id] ?: 0, it.targetSets()) }
                    val pr = dayProgress.getOrElse(d) { 0 }
                    Column {
                        Text(
                            (if (d == todayIdx) "HOY · " else "") + WEEKDAY_LABELS[d].full.uppercase(),
                            color = Volt.accent, style = VoltType.label,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            r?.focus?.ifBlank { null } ?: if (r != null) "Entrenamiento" else "Crea tu sesión",
                            color = Volt.text, style = VoltType.display.copy(fontSize = 46.sp, lineHeight = 48.sp), maxLines = 3,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (r != null) "${exs.size} ejercicios listos. Hoy toca. Sin excusas." else "No hay rutina asignada. Arma una sesión en 1 minuto.",
                            color = Volt.textDim, style = VoltType.body,
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            GlowRing(pr / 100f, Modifier.size(104.dp), stroke = 10.dp) {
                                Text("$pr%", color = Volt.text, style = VoltType.numberM)
                            }
                            Spacer(Modifier.width(20.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                HeroStat("Series", "$dn/$pl")
                                HeroStat("Faltan", "${((pl - dn) * 2).coerceAtLeast(0)} min")
                            }
                        }
                    }
                }
                Spacer(Modifier.height(22.dp))
                if (selectedRoutine != null && exercises.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            Modifier.weight(1f).height(60.dp).clip(CircleShape).background(Volt.surface2).pressScale { haptics.light(); detailDay = selectedDay },
                            contentAlignment = Alignment.Center,
                        ) { Text("Ver entreno", color = Volt.text, style = VoltType.title) }
                        Row(
                            Modifier.weight(1.2f).height(60.dp).clip(CircleShape).background(Volt.gradient)
                                .pressScale {
                                    val first = exercises.indexOfFirst { (setsByEx[it.id] ?: 0) < it.targetSets() }.let { if (it < 0) 0 else it }
                                    openSession(selectedRoutine, first)
                                },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Volt.onAccent)
                            Spacer(Modifier.width(4.dp))
                            Text(if (completedSets in 1 until plannedSets) "Continuar" else "Empezar", color = Volt.onAccent, style = VoltType.title)
                        }
                    }
                } else {
                    Box(
                        Modifier.fillMaxWidth().height(60.dp).clip(CircleShape).background(Volt.gradient).pressScale {
                            if (custom.isEmpty()) repo.toast("info", "Sin rutina automática", "Añade ejercicios en “Arma tu entrenamiento”.")
                            else openSession(WorkoutDay("Hoy", "Mi rutina personal", custom), 0)
                        },
                        contentAlignment = Alignment.Center,
                    ) { Text(if (custom.isEmpty()) "Armar mi sesión" else "Entrenar mi rutina personal", color = Volt.onAccent, style = VoltType.title) }
                }
            }
        }

        if (isSpecialToday) {
            item(key = "specialToday", contentType = "special") {
                BentoCard(Modifier.fillMaxWidth().staggeredEntry(3), padding = 18.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.CalendarMonth, null, tint = Volt.accent, modifier = Modifier.size(28.dp))
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text("Hoy toca clase especial", color = Volt.text, style = VoltType.title)
                            Text("Prioriza técnica y ritmo para sumar calidad al progreso.", color = Volt.textDim, style = VoltType.body)
                        }
                    }
                }
            }
        }

        // Programa del día
        if (exercises.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                BentoCard(Modifier.fillMaxWidth().staggeredEntry(3)) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.FitnessCenter, null, tint = Volt.accent.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Sin rutina para este día", color = Volt.text, style = VoltType.title)
                        Text("Genera tu plan con IA o añade ejercicios desde “Arma tu entrenamiento”.", color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp))
                    }
                }
            }
        } else {
            item(key = "programTitle", contentType = "title") { SectionTitle("Programa del día") }
            items(exercises.size, key = { "ex_${exercises[it].id}_$it" }, contentType = { "exercise" }) { i ->
                val e = exercises[i]
                val done = setsByEx[e.id] ?: 0
                val target = e.targetSets()
                val complete = done >= target
                Row(
                    Modifier.then(if (i < 5) Modifier.staggeredEntry(3 + i) else Modifier).fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp)).background(Volt.surface)
                        .pressScale { selectedRoutine?.let { openSession(it, i) } }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(72.dp).clip(RoundedCornerShape(22.dp))) {
                        ExerciseImage(e.gifUrl, e.name, Modifier.fillMaxSize(), maxPx = 220)
                        if (complete) Box(Modifier.fillMaxSize().background(Volt.bg.copy(alpha = 0.55f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Check, null, tint = Volt.accent, modifier = Modifier.size(28.dp))
                        }
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.name, color = Volt.text, style = VoltType.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text("${e.sets} series × ${e.reps}", color = Volt.textDim, style = VoltType.body)
                        Spacer(Modifier.height(6.dp))
                        AnimatedProgressBar((done.toFloat() / target).coerceAtMost(1f), Modifier.fillMaxWidth(), height = 4.dp)
                    }
                }
            }
        }

        if (sess?.enabled == true) {
            item(key = "special", contentType = "special") {
                BentoCard(Modifier.fillMaxWidth().staggeredEntry(4), padding = 18.dp) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = Volt.accent, modifier = Modifier.size(22.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Clase especial semanal", color = Volt.text, style = VoltType.title)
                    }
                    Text("${sess.activity} • ${sess.day} • ${sess.durationMinutes} min", color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        // Índice de fatiga
        item(key = "fatigue", contentType = "fatigue") {
            BentoCard(Modifier.fillMaxWidth().staggeredEntry(5)) {
                Text("Índice de fatiga semanal", color = Volt.text, style = VoltType.h2)
                Text("Series de esta semana frente al volumen máximo recuperable (MRV) de cada músculo.", color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
                if (fatigue.isEmpty()) {
                    Text("Aún no hay series registradas esta semana.", color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(vertical = 8.dp))
                }
                fatigue.forEach { f ->
                    val c = fatigueStatusColor(f.status)
                    Column(Modifier.fillMaxWidth().padding(vertical = 7.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(f.muscleGroup, color = Volt.text, style = VoltType.title.copy(fontSize = 15.sp), modifier = Modifier.weight(1f))
                            Text("${f.weeklyVolume}/${f.mrv} · ${f.percent}%", color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp))
                            Spacer(Modifier.width(8.dp))
                            Text(fatigueStatusLabel(f.status).uppercase(), color = c, style = VoltType.label)
                        }
                        Spacer(Modifier.height(6.dp))
                        AnimatedProgressBar((f.percent / 100f).coerceAtMost(1f), Modifier.fillMaxWidth(), height = 6.dp, color = c)
                    }
                }
            }
        }

        // Arma tu entrenamiento
        item(key = "builderTitle", contentType = "title") {
            Column {
                SectionTitle("Arma tu entrenamiento")
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.forEach { g -> VoltChip(g, group == g, onClick = { group = g; haptics.selection() }) }
                }
            }
        }
        items(filtered, key = { "lib_${it.id}" }, contentType = { "library" }) { entry ->
            val added = custom.any { it.id == entry.id }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Volt.surface).padding(start = 18.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(entry.name, color = Volt.text, style = VoltType.title.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${entry.muscleGroup} • ${entry.defaultSets}x${entry.defaultReps}", color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp))
                }
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(if (added) androidx.compose.ui.graphics.Brush.linearGradient(listOf(Volt.surface2, Volt.surface2)) else Volt.gradient)
                        .then(
                            if (added) Modifier else Modifier.pressScale {
                                haptics.medium()
                                appWorkScope.launch {
                                    val cur = repo.state.value.customWorkout
                                    if (cur.none { it.id == entry.id }) repo.saveCustomWorkout(cur + entry.toExercise())
                                }
                                repo.toast("success", "Añadido a tu rutina", entry.name)
                            },
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(if (added) Icons.Filled.Check else Icons.Filled.Add, if (added) "Añadido ${entry.name}" else "Añadir ${entry.name}", tint = if (added) Volt.textDim else Volt.onAccent)
                }
            }
        }

        // Rutina personal
        if (custom.isNotEmpty()) {
            item(key = "customTitle", contentType = "title") { SectionTitle("Tu rutina personal (${custom.size})") }
            items(custom, key = { "cus_${it.id}" }, contentType = { "custom" }) { e ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Volt.surface).padding(start = 18.dp, top = 8.dp, bottom = 8.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(e.name, color = Volt.text, style = VoltType.title.copy(fontSize = 15.sp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(e.muscleGroup, color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp))
                    }
                    Box(
                        Modifier.size(48.dp).clip(CircleShape).pressScale {
                            haptics.light()
                            appWorkScope.launch { repo.saveCustomWorkout(repo.state.value.customWorkout.filter { it.id != e.id }) }
                        },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.Filled.Delete, "Eliminar ${e.name}", tint = Volt.danger) }
                }
            }
            item(key = "customCta", contentType = "cta") {
                Box(
                    Modifier.fillMaxWidth().height(60.dp).clip(CircleShape).background(Volt.gradient)
                        .pressScale { openSession(WorkoutDay("Hoy", "Mi rutina personal", custom), 0) },
                    contentAlignment = Alignment.Center,
                ) { Text("Entrenar mi rutina personal", color = Volt.onAccent, style = VoltType.title) }
            }
        }
    }

    // ── Detalle del día ──
    val dd = detailDay
    val detailRoutine = dd?.let { routinesByDay.getOrNull(it) }
    if (dd != null && detailRoutine != null && session == null) {
        DayDetailDialog(
            day = detailRoutine, setsByExercise = setsByDay[dd], estimatedMinutes = estimatedMinutes,
            onClose = { detailDay = null },
            onStart = { start -> detailDay = null; openSession(detailRoutine, start) },
        )
    }

    // ── Sesión activa ──
    session?.let { s ->
        SessionDialog(repo = repo, spec = s, onClose = { session = null })
    }
}

@Composable
private fun HeroStat(label: String, value: String) {
    Column {
        Text(label.uppercase(), color = Volt.textDim, style = VoltType.label)
        Text(value, color = Volt.text, style = VoltType.numberM)
    }
}
