package com.voltbody.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.Repository
import com.voltbody.app.data.WorkoutDay
import com.voltbody.app.ui.DonutProgress
import com.voltbody.app.ui.EmptyState
import com.voltbody.app.ui.SectionTitle
import com.voltbody.app.ui.StatPill
import com.voltbody.app.ui.VoltCard
import com.voltbody.app.ui.VoltChip
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.screens.workout.*
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.WEEKDAY_LABELS
import com.voltbody.app.util.computeFatigueIndex
import com.voltbody.app.util.fatigueStatusColor
import com.voltbody.app.util.fatigueStatusLabel
import com.voltbody.app.util.logDayKey
import com.voltbody.app.util.mapRoutineByWeekday
import com.voltbody.app.util.mondayFirstIndex
import com.voltbody.app.util.mondayOf
import com.voltbody.app.util.rememberHaptics
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.LocalDate

private fun norm(s: String) = Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{Mn}+"), "").trim()

/**
 * Pestaña Rutina: semana por días, entreno del día con progreso, detalle (diseño 3) y sesión activa,
 * constructor de entreno personalizado desde la biblioteca e índice de fatiga semanal.
 */
@Composable
fun WorkoutScreen(repo: Repository) {
    val st by repo.state.collectAsStateWithLifecycle()
    val haptics = rememberHaptics()
    val today = remember { LocalDate.now() }
    val todayIdx = mondayFirstIndex(today)

    var selectedDay by rememberSaveable { mutableIntStateOf(todayIdx) }
    var editingDays by remember { mutableStateOf(false) }
    var moveSource by remember { mutableStateOf<Int?>(null) }
    var detailDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var session by remember { mutableStateOf<SessionSpec?>(null) }
    var group by rememberSaveable { mutableStateOf("Todos") }

    val routinesByDay = remember(st.routine) { mapRoutineByWeekday(st.routine) }
    val activeDays = remember(routinesByDay) { routinesByDay.mapIndexedNotNull { i, d -> if (d != null) i else null } }
    val selectedRoutine: WorkoutDay? = routinesByDay.getOrNull(selectedDay)
    val custom = st.customWorkout

    // Si el día seleccionado no tiene plan, salta a hoy o al primer día activo
    LaunchedEffect(routinesByDay, selectedDay) {
        if (routinesByDay.getOrNull(selectedDay) != null) return@LaunchedEffect
        if (routinesByDay.getOrNull(todayIdx) != null) selectedDay = todayIdx
        else if (activeDays.isNotEmpty()) selectedDay = activeDays.first()
    }

    // Enriquecer GIFs en silencio (el servidor solo rellena vacíos). Corrige el bug de la web que descartaba el resultado.
    LaunchedEffect(st.authToken) {
        if (st.authToken == null) return@LaunchedEffect
        val current = repo.state.value.routine
        if (current.isEmpty() || !routineNeedsEnrichment(current)) return@LaunchedEffect
        val enriched = repo.enrichRoutine(current)
        val changed = enriched.size == current.size && enriched.indices.any { di ->
            enriched[di].exercises.size == current[di].exercises.size &&
                enriched[di].exercises.indices.any { ei -> enriched[di].exercises[ei].gifUrl != current[di].exercises[ei].gifUrl }
        }
        if (changed) repo.saveProfile(withPlan = true) { it.copy(routine = enriched) }
    }

    val selectedDateKey = remember(selectedDay) { mondayOf(today).plusDays(selectedDay.toLong()).toString() }
    val setsByEx = remember(st.logs, selectedDateKey) {
        st.logs.filter { logDayKey(it.date) == selectedDateKey }.groupingBy { it.exerciseId }.eachCount()
    }
    val exercises = selectedRoutine?.exercises.orEmpty()
    val plannedSets = exercises.sumOf { it.targetSets() }
    val completedSets = exercises.sumOf { minOf(setsByEx[it.id] ?: 0, it.targetSets()) }
    val progress = if (plannedSets > 0) completedSets * 100 / plannedSets else 0
    val etaMinutes = ((plannedSets - completedSets) * 2).coerceAtLeast(0)
    val estimatedMinutes = (st.profile.sessionMinutes?.takeIf { it > 0 } ?: (plannedSets * 3).coerceAtLeast(10))

    val sess = st.profile.weeklySpecialSession
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

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 110.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // Cabecera
        item {
            Column(Modifier.staggeredEntry(0)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FitnessCenter, null, tint = Volt.accent, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(10.dp))
                    Text("Rutina de hoy", color = Volt.text, fontSize = 28.sp, fontWeight = FontWeight.Black)
                }
                Text(selectedRoutine?.focus?.ifBlank { null } ?: "Hoy toca activar el cuerpo", color = Volt.accent, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }

        // Semana de entrenamiento
        item {
            VoltCard(Modifier.fillMaxWidth().staggeredEntry(1)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Semana de entrenamiento", color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                        Text(
                            if (editingDays) "Paso 1: toca un día activo. Paso 2: toca un día bloqueado para moverlo."
                            else "Selecciona un día. Los días sin plan quedan bloqueados.",
                            color = Volt.textDim, fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    VoltChip(if (editingDays) "Cancelar" else "Editar días", selected = editingDays, onClick = { editingDays = !editingDays; moveSource = null })
                }
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WEEKDAY_LABELS.forEachIndexed { i, label ->
                        val has = routinesByDay.getOrNull(i) != null
                        val sel = selectedDay == i
                        val isSource = moveSource == i
                        val bg by animateColorAsState(
                            when { isSource -> Volt.orange.copy(alpha = 0.3f); sel -> Volt.accent; else -> Volt.surface2 }, tween(250), label = "dayBg",
                        )
                        Column(
                            Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(16.dp)).background(bg)
                                .border(1.dp, if (editingDays && !has) Volt.textDim.copy(alpha = 0.5f) else if (isSource) Volt.orange else Color.Transparent, RoundedCornerShape(16.dp))
                                .clickable(enabled = editingDays || has) { onWeekdayTap(i) },
                            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
                        ) {
                            Text(
                                label.short, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                                color = when { sel -> Color.Black; !has && !editingDays -> Volt.textDim.copy(alpha = 0.4f); else -> Volt.text },
                            )
                            Box(Modifier.padding(top = 4.dp).size(5.dp).clip(CircleShape).background(
                                if (has) (if (sel) Color.Black else Volt.accent) else Color.Transparent,
                            ))
                        }
                    }
                }
            }
        }

        // Hero del día seleccionado
        item {
            VoltCard(Modifier.fillMaxWidth().staggeredEntry(2), padding = 20.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("🎯 SESIÓN PRIORITARIA", color = Volt.textDim, fontSize = 11.sp, letterSpacing = 1.5.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(selectedRoutine?.focus?.ifBlank { null } ?: "Crea tu sesión personalizada", color = Volt.text, fontSize = 24.sp, fontWeight = FontWeight.Black, lineHeight = 28.sp)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            if (selectedRoutine != null) "${exercises.size} ejercicios listos para ejecutar. Hoy toca. Sin excusas."
                            else "No hay rutina asignada hoy. Arma una sesión en 1 minuto.",
                            color = Volt.textDim, fontSize = 13.sp,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    DonutProgress(progress / 100f, Modifier.size(92.dp), stroke = 10.dp, color = Volt.accent) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$progress%", color = Volt.text, fontWeight = FontWeight.Black, fontSize = 18.sp)
                            Icon(Icons.Filled.LocalFireDepartment, null, tint = Volt.orange, modifier = Modifier.size(14.dp))
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("estado", if (selectedRoutine != null) "activo" else "custom", Modifier.weight(1f))
                    StatPill("ejercicios", "${exercises.size}", Modifier.weight(1f))
                    StatPill("tu lista", "${custom.size}", Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("Checklist de sesión", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
                    Text("$completedSets/$plannedSets series", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
                Spacer(Modifier.height(8.dp))
                AnimatedProgressBar(progress / 100f, Modifier.fillMaxWidth(), height = 8.dp)
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text("Progreso: $progress%", color = Volt.accent, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text("ETA: $etaMinutes min", color = Volt.textDim, fontSize = 11.sp)
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (selectedRoutine != null && exercises.isNotEmpty()) {
                        Box(
                            Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(Volt.surface2).border(1.dp, Volt.border, RoundedCornerShape(26.dp))
                                .pressScale { haptics.light(); detailDay = selectedDay },
                            contentAlignment = Alignment.Center,
                        ) { Text("Ver entreno", color = Volt.text, fontWeight = FontWeight.Bold) }
                        Row(
                            Modifier.weight(1f).height(52.dp).clip(RoundedCornerShape(26.dp)).background(Volt.accent)
                                .pressScale {
                                    val first = exercises.indexOfFirst { (setsByEx[it.id] ?: 0) < it.targetSets() }.let { if (it < 0) 0 else it }
                                    openSession(selectedRoutine, first)
                                },
                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                        ) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Color.Black)
                            Spacer(Modifier.width(4.dp))
                            Text(if (completedSets in 1 until plannedSets) "Continuar" else "Empezar", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    } else {
                        Box(
                            Modifier.fillMaxWidth().height(52.dp).clip(RoundedCornerShape(26.dp)).background(Volt.accent).pressScale {
                                if (custom.isEmpty()) repo.toast("info", "Sin rutina automática", "Añade ejercicios en “Arma tu Entrenamiento”.")
                                else openSession(WorkoutDay("Hoy", "Mi rutina personal", custom), 0)
                            },
                            contentAlignment = Alignment.Center,
                        ) { Text(if (custom.isEmpty()) "Armar mi sesión" else "Entrenar mi rutina personal", color = Color.Black, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }

        if (isSpecialToday) {
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Volt.accent.copy(alpha = 0.08f)).border(1.dp, Volt.accent.copy(alpha = 0.3f), RoundedCornerShape(24.dp))
                        .padding(16.dp).staggeredEntry(3),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.CalendarMonth, null, tint = Volt.accent, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text("Hoy toca clase especial 🎯", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text("Prioriza técnica y ritmo para sumar calidad al progreso.", color = Volt.textDim, fontSize = 12.sp)
                    }
                }
            }
        }

        // Lista de ejercicios del día
        if (exercises.isEmpty()) {
            item {
                VoltCard(Modifier.fillMaxWidth().staggeredEntry(3)) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.FitnessCenter, null, tint = Volt.accent.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(10.dp))
                        Text("Sin rutina para hoy", color = Volt.text, fontWeight = FontWeight.Bold)
                        Text("Genera tu plan con IA o añade ejercicios desde “Arma tu Entrenamiento”", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.padding(horizontal = 24.dp))
                    }
                }
            }
        } else {
            itemsIndexedSafe(exercises) { i, e ->
                val done = setsByEx[e.id] ?: 0
                val complete = done >= e.targetSets()
                val pct = (done.toFloat() / e.targetSets()).coerceAtMost(1f)
                Column(
                    Modifier.staggeredEntry(3 + i).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Volt.surface)
                        .border(1.dp, if (complete) Volt.accent.copy(alpha = 0.6f) else Volt.border, RoundedCornerShape(24.dp))
                        .pressScale { selectedRoutine?.let { openSession(it, i) } },
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(64.dp).clip(RoundedCornerShape(18.dp))) {
                            ExerciseImage(e.gifUrl, e.name, Modifier.fillMaxSize())
                            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.35f)), contentAlignment = Alignment.Center) {
                                Icon(if (complete) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow, null, tint = Volt.accent, modifier = Modifier.size(24.dp))
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.name, color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${e.sets} sets × ${e.reps} reps", color = Volt.textDim, fontSize = 12.sp)
                            if (done > 0) Text(if (complete) "✅ Completado" else "$done/${e.targetSets()} series completadas", color = Volt.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Volt.textDim)
                    }
                    AnimatedProgressBar(pct, Modifier.fillMaxWidth(), height = 3.dp)
                }
            }
        }

        if (sess?.enabled == true) {
            item {
                VoltCard(Modifier.fillMaxWidth().staggeredEntry(4)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, null, tint = Volt.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("⭐ Clase Especial Semanal", color = Volt.text, fontWeight = FontWeight.Bold)
                    }
                    Text("${sess.activity} • ${sess.day} • ${sess.durationMinutes} min", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.padding(top = 6.dp))
                }
            }
        }

        // Índice de fatiga
        item {
            val fatigue = remember(st.logs, st.routine, custom) { computeFatigueIndex(st.logs, st.routine, today, custom) }
            VoltCard(Modifier.fillMaxWidth().staggeredEntry(5)) {
                SectionTitle("🔋 Índice de fatiga semanal")
                Text("Series de esta semana frente al volumen máximo recuperable (MRV) de cada músculo.", color = Volt.textDim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp, bottom = 10.dp))
                if (fatigue.isEmpty()) {
                    Text("Aún no hay series registradas esta semana.", color = Volt.textDim, fontSize = 13.sp, modifier = Modifier.padding(vertical = 8.dp))
                }
                fatigue.forEach { f ->
                    val c = fatigueStatusColor(f.status)
                    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(f.muscleGroup, color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                            Text("${f.weeklyVolume}/${f.mrv} series · ${f.percent}%", color = Volt.textDim, fontSize = 11.sp)
                            Spacer(Modifier.width(8.dp))
                            Text(fatigueStatusLabel(f.status), color = c, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Spacer(Modifier.height(6.dp))
                        AnimatedProgressBar((f.percent / 100f).coerceAtMost(1f), Modifier.fillMaxWidth(), height = 6.dp, color = c)
                    }
                }
            }
        }

        // Arma tu entrenamiento
        item {
            val groups = remember { listOf("Todos") + defaultExerciseLibrary.map { it.muscleGroup }.distinct() }
            val filtered = if (group == "Todos") defaultExerciseLibrary else defaultExerciseLibrary.filter { it.muscleGroup == group }
            VoltCard(Modifier.fillMaxWidth().staggeredEntry(6)) {
                SectionTitle("🏋️ Arma tu Entrenamiento")
                Row(Modifier.horizontalScroll(rememberScrollState()).padding(vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    groups.forEach { g -> VoltChip(g, group == g, onClick = { group = g; haptics.selection() }) }
                }
                filtered.forEach { entry ->
                    val added = custom.any { it.id == entry.id }
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(18.dp)).background(Volt.surface2).padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(entry.name, color = Volt.text, fontWeight = FontWeight.Medium, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${entry.muscleGroup} • ${entry.defaultSets}x${entry.defaultReps}", color = Volt.textDim, fontSize = 12.sp)
                        }
                        Box(
                            Modifier.size(44.dp).clip(CircleShape).then(if (added) Modifier else Modifier.pressScale {
                                haptics.medium()
                                appWorkScope.launch {
                                    val cur = repo.state.value.customWorkout
                                    if (cur.none { it.id == entry.id }) repo.saveCustomWorkout(cur + entry.toExercise())
                                }
                                repo.toast("success", "Añadido a tu rutina", entry.name)
                            }),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(if (added) Icons.Filled.CheckCircle else Icons.Filled.AddCircle, "Añadir ${entry.name}", tint = if (added) Volt.textDim else Volt.accent)
                        }
                    }
                }
            }
        }

        // Rutina personal
        if (custom.isNotEmpty()) {
            item {
                VoltCard(Modifier.fillMaxWidth().staggeredEntry(7)) {
                    SectionTitle("📋 Tu Rutina Personal (${custom.size})")
                    Spacer(Modifier.height(8.dp))
                    custom.forEach { e ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(18.dp)).background(Volt.surface2).padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(e.name, color = Volt.text, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                Text(e.muscleGroup, color = Volt.textDim, fontSize = 12.sp)
                            }
                            Box(
                                Modifier.size(44.dp).clip(CircleShape).pressScale {
                                    haptics.light()
                                    appWorkScope.launch { repo.saveCustomWorkout(repo.state.value.customWorkout.filter { it.id != e.id }) }
                                },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.Delete, "Eliminar ${e.name}", tint = Volt.danger) }
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Box(
                        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(Volt.accent)
                            .pressScale { openSession(WorkoutDay("Hoy", "Mi rutina personal", custom), 0) },
                        contentAlignment = Alignment.Center,
                    ) { Text("Entrenar mi rutina personal", color = Color.Black, fontWeight = FontWeight.Bold) }
                }
            }
        }
    }

    // ── Detalle del día (diseño 3) ──
    val dd = detailDay
    val detailRoutine = dd?.let { routinesByDay.getOrNull(it) }
    if (dd != null && detailRoutine != null && session == null) {
        DayDetailDialog(
            day = detailRoutine, setsByExercise = setsByEx, estimatedMinutes = estimatedMinutes,
            onClose = { detailDay = null },
            onStart = { start -> detailDay = null; openSession(detailRoutine, start) },
        )
    }

    // ── Sesión activa ──
    session?.let { s ->
        SessionDialog(repo = repo, spec = s, onClose = { session = null })
    }
}

/** items(indexed) sin depender de imports adicionales del LazyListScope en el cuerpo principal. */
private inline fun <T> androidx.compose.foundation.lazy.LazyListScope.itemsIndexedSafe(
    list: List<T>, crossinline content: @Composable (Int, T) -> Unit,
) {
    items(list.size) { i -> content(i, list[i]) }
}
