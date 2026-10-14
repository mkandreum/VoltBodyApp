package com.voltbody.app.ui.v3

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.PlaylistAdd
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.voltbody.app.data.AppState
import com.voltbody.app.data.Exercise
import com.voltbody.app.data.Repository
import com.voltbody.app.data.WorkoutDay
import com.voltbody.app.ui.screens.home.ExerciseImage
import com.voltbody.app.ui.screens.home.rememberGifLoader
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.Haptics
import com.voltbody.app.util.WEEKDAY_LABELS
import com.voltbody.app.util.logLocalDate
import com.voltbody.app.util.mapRoutineByWeekday
import com.voltbody.app.util.mondayFirstIndex
import com.voltbody.app.util.mondayOf
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate

const val MIN_PER_EXERCISE = 6
private val WEEK_LETTERS = listOf("L", "M", "X", "J", "V", "S", "D")

/** Datos de la pestana Entrenar (solo lo que usa, para no recomponer con cada cambio del estado). */
private data class TrainUi(val week: List<WorkoutDay?>, val doneByDay: List<Set<String>>, val custom: List<Exercise>)

private fun trainUi(st: AppState, today: LocalDate): TrainUi {
    val week = mapRoutineByWeekday(st.routine)
    val monday = mondayOf(today)
    val byDate = st.logs.groupBy { logLocalDate(it.date) }
    val done = (0..6).map { i -> byDate[monday.plusDays(i.toLong())].orEmpty().map { it.exerciseId }.toSet() }
    return TrainUi(week, done, st.customWorkout)
}

@Composable
fun TrainV3(repo: Repository) {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val today = remember { LocalDate.now() }
    val flow = remember(repo) { repo.state.map { trainUi(it, today) }.distinctUntilChanged() }
    val ui by flow.collectAsStateWithLifecycle(initialValue = trainUi(repo.state.value, today))
    val todayIdx = remember { mondayFirstIndex(today) }
    var sel by remember { mutableIntStateOf(todayIdx) }
    val gif = rememberGifLoader()
    val trainDays = ui.week.count { it != null }
    val doneDays = ui.week.indices.count { i -> ui.week[i] != null && ui.doneByDay[i].isNotEmpty() }

    Screen {
        Row(Modifier.fillMaxWidth().enter(0), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Esta semana · $doneDays de $trainDays días", style = T.hello, color = Volt.textDim)
                MixTitle("Hoy, a entrenar", maxLines = 1)
            }
            IconCircle(Icons.Outlined.PlaylistAdd, desc = "Rutina personal") { nav.push(Route.Section("library")) }
        }

        // Selector de semana
        Row(Modifier.fillMaxWidth().enter(1), horizontalArrangement = Arrangement.SpaceBetween) {
            WEEK_LETTERS.forEachIndexed { i, l ->
                val active = i == sel
                val has = ui.week[i] != null
                val done = has && ui.doneByDay[i].isNotEmpty()
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape)
                            .background(if (active) Volt.accent else Volt.surface)
                            .tap(0.9f, haptic = false) { sel = i; Haptics.selection(ctx) },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (done && !active) Icon(Icons.Filled.Check, null, tint = Volt.accent, modifier = Modifier.size(18.dp))
                        else Text(l, style = T.cardTitle, color = if (active) Color(0xFF0A0A0A) else if (has) Volt.text else Volt.textDim)
                    }
                    Spacer(Modifier.height(5.dp))
                    Box(Modifier.size(5.dp).clip(CircleShape).background(if (i == todayIdx) Volt.accent else if (has) Volt.textDim.copy(alpha = 0.5f) else Color.Transparent))
                }
            }
        }

        AnimatedContent(sel, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(90)) }, label = "day", modifier = Modifier.enter(2)) { i ->
            val day = ui.week[i]
            Column(verticalArrangement = Arrangement.spacedBy(K.gap)) {
                if (day == null) RestHero(i == todayIdx) { nav.push(Route.Section("library")) }
                else {
                    DayHero(day, if (i == todayIdx) "Hoy" else WEEKDAY_LABELS[i].full, ui.doneByDay[i], gif,
                        onStart = { nav.push(Route.Session(i)) }, onOpen = { nav.push(Route.Day(i)) })
                    SectionLabel("Programa", action = "Ver todo") { nav.push(Route.Day(i)) }
                    day.exercises.take(3).forEachIndexed { k, ex ->
                        ExerciseRow(ex, k, ex.id in ui.doneByDay[i], gif) { nav.push(Route.Session(i, k)) }
                    }
                }
            }
        }

        Row(Modifier.fillMaxWidth().enter(3), horizontalArrangement = Arrangement.spacedBy(K.gap)) {
            MiniAction(Icons.Outlined.FitnessCenter, "Rutina personal", "${ui.custom.size} ejercicios", Modifier.weight(1f)) {
                if (ui.custom.isNotEmpty()) nav.push(Route.Session(-1, 0, custom = true)) else nav.push(Route.Section("library"))
            }
            MiniAction(Icons.Outlined.Insights, "Fatiga muscular", "Volumen semanal", Modifier.weight(1f)) { nav.push(Route.Section("fatigue")) }
        }
    }
}

@Composable
private fun MiniAction(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, sub: String, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier, onClick = onClick, padding = 14.dp) {
        Icon(icon, null, tint = Volt.accent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(8.dp))
        Text(title, style = T.cardTitle, color = Volt.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(sub, style = T.caption, color = Volt.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** Tarjeta heroe del dia: arte de vidrio estriado + titulo grande + GIF en circulo con burbuja (referencias del usuario). */
@Composable
private fun DayHero(day: WorkoutDay, tag: String, done: Set<String>, gif: ImageLoader, onStart: () -> Unit, onOpen: () -> Unit) {
    val first = day.exercises.firstOrNull()
    val min = day.exercises.size * MIN_PER_EXERCISE
    val doneCount = day.exercises.count { it.id in done }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Box(Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(30.dp)).tap(0.985f, onClick = onOpen)) {
            FlutedArt(Modifier.matchParentSize(), ArtPalette.forKey(day.focus), seed = day.focus.length)
            Column(Modifier.align(Alignment.TopStart).padding(18.dp).fillMaxWidth(0.6f)) {
                Text(tag.uppercase(), style = T.label, color = Color.White, modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(Color(0x66000000)).padding(horizontal = 10.dp, vertical = 5.dp))
                Spacer(Modifier.height(14.dp))
                FitText(gymTerm(day.focus.ifBlank { "Workout" }), T.headline, Color.White, maxLines = 3, minScale = 0.6f)
            }
            Row(Modifier.align(Alignment.BottomStart).padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.AccessTime, null, tint = Color.White, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("$min min · ${day.exercises.size} ejercicios", style = T.body, color = Color.White, maxLines = 1)
            }
            CircleMedia(120.dp, if (doneCount > 0) "$doneCount/${day.exercises.size}" else "${day.exercises.size}", Modifier.align(Alignment.CenterEnd).padding(end = 18.dp), ring = Color(0x00000000)) {
                if (first != null && first.gifUrl.isNotBlank()) ExerciseImage(first.gifUrl, gif, Modifier.fillMaxSize(), 360, 360)
                else Icon(Icons.Outlined.FitnessCenter, null, tint = Volt.textDim, modifier = Modifier.size(40.dp))
            }
        }
        GooeyCta(if (doneCount > 0) "Continuar entreno" else "Empezar entreno", onStart)
    }
}

@Composable
private fun RestHero(isToday: Boolean, onCustom: () -> Unit) {
    Card(Modifier.fillMaxWidth().height(220.dp), padding = 20.dp) {
        Icon(Icons.Outlined.Hotel, null, tint = Volt.accent, modifier = Modifier.size(30.dp))
        Spacer(Modifier.weight(1f))
        Text(if (isToday) "Hoy toca descansar" else "Día de descanso", style = T.screen.copy(fontSize = androidx.compose.ui.unit.TextUnit(22f, androidx.compose.ui.unit.TextUnitType.Sp)), color = Volt.text)
        Text("El músculo crece cuando descansa. Si te apetece, haz una sesión libre.", style = T.body, color = Volt.textDim)
        Spacer(Modifier.height(12.dp))
        DarkButton("Sesión libre", onCustom, icon = Icons.Filled.Add)
    }
}

/** Fila de ejercicio del mockup: miniatura redondeada + nombre + series. */
@Composable
fun ExerciseRow(ex: Exercise, index: Int, done: Boolean, gif: ImageLoader, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(Volt.surface).tap(0.98f, onClick = onClick).padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(Volt.surface2), contentAlignment = Alignment.Center) {
            if (ex.gifUrl.isNotBlank()) ExerciseImage(ex.gifUrl, gif, Modifier.fillMaxSize(), 200, 200)
            else Text("${index + 1}", style = T.section, color = Volt.textDim)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            FitText(ex.name, T.cardTitle.copy(fontSize = androidx.compose.ui.unit.TextUnit(14f, androidx.compose.ui.unit.TextUnitType.Sp)), Volt.text, maxLines = 2)
            Text(setsLabel(ex) + if (ex.muscleGroup.isNotBlank()) " · ${ex.muscleGroup}" else "", style = T.caption, color = Volt.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (done) Box(Modifier.size(28.dp).clip(CircleShape).background(Volt.accent), contentAlignment = Alignment.Center) { Icon(Icons.Filled.Check, "Hecho", tint = Color(0xFF0A0A0A), modifier = Modifier.size(16.dp)) }
        else Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Volt.textDim)
        Spacer(Modifier.width(4.dp))
    }
}

fun setsLabel(ex: Exercise): String = when (ex.exerciseType) {
    "isometric" -> "${ex.sets} × ${ex.durationTarget?.let { "${it}s" } ?: ex.reps}"
    "cardio" -> "${ex.reps} min"
    else -> "${ex.sets} × ${ex.reps}"
}

/** Detalle de dia — tercera pantalla del mockup ("Full Body burn" + "Program" + "Start"). */
@Composable
fun DayDetailV3(repo: Repository, weekday: Int) {
    val nav = LocalNav.current
    val st by repo.state.collectAsStateWithLifecycle()
    val day = remember(st.routine, weekday) { mapRoutineByWeekday(st.routine).getOrNull(weekday) }
    val today = remember { LocalDate.now() }
    val done = remember(st.logs) {
        val d = mondayOf(today).plusDays(weekday.toLong())
        st.logs.filter { logLocalDate(it.date) == d }.map { it.exerciseId }.toSet()
    }
    val gif = rememberGifLoader()
    if (day == null) { LaunchedEffect(Unit) { nav.pop() }; return }
    val groups = remember(day) { day.exercises.map { it.muscleGroup }.filter { it.isNotBlank() }.distinct().take(3).joinToString(", ") }
    val totalSets = day.exercises.sumOf { it.sets }

    Box(Modifier.fillMaxSize().background(Volt.bg)) {
        LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 120.dp)) {
            item(key = "hero") {
                Box(Modifier.fillMaxWidth().height(330.dp).clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))) {
                    FlutedArt(Modifier.matchParentSize(), ArtPalette.forKey(day.focus), seed = day.focus.length)
                    val first = day.exercises.firstOrNull()
                    CircleMedia(196.dp, "${day.exercises.size * MIN_PER_EXERCISE}'", Modifier.align(Alignment.Center).padding(top = 30.dp), ring = Color.Transparent) {
                        if (first != null && first.gifUrl.isNotBlank()) ExerciseImage(first.gifUrl, gif, Modifier.fillMaxSize(), 560, 560)
                        else Icon(Icons.Outlined.FitnessCenter, null, tint = Volt.textDim, modifier = Modifier.size(64.dp))
                    }
                    IconCircle(Icons.AutoMirrored.Filled.ArrowBack, Modifier.statusBarsPadding().padding(16.dp), bg = Color(0x99000000), tint = Color.White, desc = "Atrás") { nav.pop() }
                }
            }
            item(key = "title") {
                Column(Modifier.padding(horizontal = K.pad).enter(0)) {
                    FitText(gymTerm(day.focus.ifBlank { "Workout" }), T.headline, Volt.text, maxLines = 2, minScale = 0.65f)
                    Spacer(Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.AccessTime, null, tint = Volt.textDim, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("${day.exercises.size * MIN_PER_EXERCISE} minutos", style = T.body, color = Volt.textDim)
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        "Sesión de ${day.exercises.size} ejercicios y $totalSets series" + (if (groups.isNotBlank()) " centrada en $groups." else ".") +
                            " Controla la bajada y descansa entre series.",
                        style = T.body, color = Volt.textDim,
                    )
                    Spacer(Modifier.height(18.dp))
                    Text("Programa", style = T.section, color = Volt.text)
                    Spacer(Modifier.height(10.dp))
                }
            }
            itemsIndexed(day.exercises, key = { i, e -> "${e.id}-$i" }) { i, ex ->
                Box(Modifier.padding(horizontal = K.pad, vertical = 5.dp).enter(i + 1)) {
                    ExerciseRow(ex, i, ex.id in done, gif) { nav.push(Route.Session(weekday, i)) }
                }
            }
        }
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Volt.bg, Volt.bg)))
                .navigationBarsPadding().padding(horizontal = K.pad).padding(top = 24.dp, bottom = 14.dp),
        ) {
            GooeyCta(if (done.isNotEmpty()) "Continuar" else "Empezar", { nav.push(Route.Session(weekday, 0)) })
        }
    }
}
