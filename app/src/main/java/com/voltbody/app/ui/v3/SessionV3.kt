package com.voltbody.app.ui.v3

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.Exercise
import com.voltbody.app.data.Repository
import com.voltbody.app.data.WorkoutLog
import com.voltbody.app.ui.screens.home.ExerciseImage
import com.voltbody.app.ui.screens.home.HM_XP_PER_LEVEL
import com.voltbody.app.ui.screens.home.HM_XP_PER_LOG
import com.voltbody.app.ui.screens.home.HM_XP_PER_STREAK_DAY
import com.voltbody.app.ui.screens.home.rememberGifLoader
import com.voltbody.app.ui.screens.workout.ConfettiBurst
import com.voltbody.app.ui.screens.workout.SummaryDialog
import com.voltbody.app.ui.screens.workout.WorkoutSummaryData
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.Haptics
import com.voltbody.app.util.computeSmartStreak
import com.voltbody.app.util.getExerciseHistory
import com.voltbody.app.util.getProgressiveSuggestion
import com.voltbody.app.util.logLocalDate
import com.voltbody.app.util.mapRoutineByWeekday
import com.voltbody.app.util.unlockNewAchievements
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

private fun firstInt(s: String, def: Int): Int = Regex("\\d+").find(s)?.value?.toIntOrNull() ?: def
private fun mmss(sec: Int): String = "%02d:%02d".format(sec / 60, sec % 60)

/** Valores editables de la serie actual. */
private class SetInputs(w: Double, r: Int, rir: Int, sec: Int, rpe: Int, min: Int) {
    var weight by mutableDoubleStateOf(w)
    var reps by mutableIntStateOf(r)
    var rir by mutableIntStateOf(rir)
    var seconds by mutableIntStateOf(sec)
    var rpe by mutableIntStateOf(rpe)
    var minutes by mutableIntStateOf(min)
}

@Composable
fun SessionV3(repo: Repository, route: Route.Session) {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val st by repo.state.collectAsStateWithLifecycle()
    // La lista de ejercicios se fija al entrar (no cambia aunque se refresque el estado).
    val exercises = remember {
        val s = repo.state.value
        if (route.custom) s.customWorkout else mapRoutineByWeekday(s.routine).getOrNull(route.weekday)?.exercises.orEmpty()
    }
    val title = remember {
        if (route.custom) "Rutina personal" else gymTerm(mapRoutineByWeekday(repo.state.value.routine).getOrNull(route.weekday)?.focus ?: "Workout")
    }
    if (exercises.isEmpty()) { LaunchedEffect(Unit) { repo.toast("info", "Sin ejercicios", "Añade ejercicios a la rutina."); nav.pop() }; return }

    val today = remember { LocalDate.now() }
    val startMs = remember { System.currentTimeMillis() }
    var idx by rememberSaveable { mutableIntStateOf(route.start.coerceIn(0, exercises.lastIndex)) }
    val ex = exercises[idx]
    val type = ex.exerciseType ?: "weighted"
    val todayLogs = remember(st.logs) { st.logs.filter { logLocalDate(it.date) == today } }
    val doneSets = todayLogs.count { it.exerciseId == ex.id }
    val history = remember(st.logs, ex.id) { getExerciseHistory(ex.id, st.logs, 8) }
    val inputs = remember(ex.id) {
        val last = st.logs.lastOrNull { it.exerciseId == ex.id }
        val sug = getProgressiveSuggestion(ex.id, st.logs)?.suggestedWeight
        SetInputs(
            w = sug ?: last?.weight ?: ex.weight.takeIf { type == "weighted" } ?: 0.0,
            r = last?.reps ?: firstInt(ex.reps, 10), rir = last?.rir ?: 2,
            sec = ex.durationTarget ?: last?.duration ?: firstInt(ex.reps, 30), rpe = last?.rpe ?: 7,
            min = (last?.duration?.div(60)) ?: firstInt(ex.reps, 20),
        )
    }
    var restEnd by remember { mutableLongStateOf(0L) }
    var restTotal by remember { mutableIntStateOf(90) }
    val resting = restEnd > 0L
    var confetti by remember { mutableIntStateOf(0) }
    var saving by remember { mutableStateOf(false) }
    var summary by remember { mutableStateOf<WorkoutSummaryData?>(null) }
    val gif = rememberGifLoader()

    fun goTo(i: Int) { if (i in exercises.indices && i != idx) { Haptics.selection(ctx); idx = i; restEnd = 0L } }

    fun finish() {
        scope.launch {
            val s = repo.state.value
            val logsToday = s.logs.filter { logLocalDate(it.date) == today && exercises.any { e -> e.id == it.exerciseId } }
            val streak = computeSmartStreak(s.logs, s.routine)
            val xp = s.logs.size * HM_XP_PER_LOG + streak * HM_XP_PER_STREAK_DAY
            val fresh = unlockNewAchievements(repo)
            Haptics.heavy(ctx)
            if (fresh.isNotEmpty()) repo.toast("success", "Logro desbloqueado", fresh.first().label)
            summary = WorkoutSummaryData(
                focus = title, exerciseCount = exercises.size, completedSets = logsToday.size, plannedSets = exercises.sumOf { it.sets },
                streak = streak, level = xp / HM_XP_PER_LEVEL + 1, totalXP = xp, xpGained = logsToday.size * HM_XP_PER_LOG,
                userName = s.profile.name,
                exercises = exercises.map { Triple(it.name, logsToday.count { l -> l.exerciseId == it.id }, it.reps) },
                durationMin = ((System.currentTimeMillis() - startMs) / 60000).toInt().coerceAtLeast(1),
                volumeKg = logsToday.sumOf { it.weight * it.reps }.toInt(),
            )
        }
    }

    fun saveSet() {
        if (saving) return
        saving = true
        val log = when (type) {
            "isometric" -> WorkoutLog(Instant.now().toString(), ex.id, inputs.weight, 1, duration = inputs.seconds, rpe = inputs.rpe)
            "cardio" -> WorkoutLog(Instant.now().toString(), ex.id, 0.0, 1, duration = inputs.minutes * 60)
            else -> WorkoutLog(Instant.now().toString(), ex.id, inputs.weight, inputs.reps, rir = inputs.rir)
        }
        val isPr = type == "weighted" && history.isNotEmpty() && inputs.weight > history.maxOf { it.maxWeight }
        scope.launch {
            repo.addLog(log)
            saving = false
            if (isPr) { Haptics.success(ctx); confetti++; repo.toast("success", "¡Nuevo PR! 🏆", "${trim(inputs.weight)} kg en ${ex.name}") }
            else Haptics.medium(ctx)
            val nowDone = doneSets + 1
            val lastExercise = idx == exercises.lastIndex
            if (nowDone >= ex.sets && lastExercise) { finish(); return@launch }
            restTotal = when (type) { "isometric", "cardio" -> 45; "bodyweight" -> 75; else -> 90 }
            restEnd = System.currentTimeMillis() + restTotal * 1000L
            if (nowDone >= ex.sets) idx += 1
        }
    }

    // Fin automatico del descanso
    LaunchedEffect(restEnd) {
        if (restEnd > 0L) {
            delay((restEnd - System.currentTimeMillis()).coerceAtLeast(0))
            restEnd = 0L
            Haptics.heavy(ctx)
        }
    }

    Box(Modifier.fillMaxSize().background(Volt.bg)) {
        Column(
            Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = K.pad).padding(top = 8.dp, bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TopBar(title, { nav.pop() }, trailing = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IconCircle(Icons.Outlined.CameraAlt, size = 40.dp, desc = "PoseCoach") { nav.push(Route.Pose(ex.name)) }
                    SmallPill("Terminar") { finish() }
                }
            })
            Spacer(Modifier.height(10.dp))
            // Progreso de ejercicios (segmentos tocables)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                exercises.forEachIndexed { i, e ->
                    val done = todayLogs.count { it.exerciseId == e.id } >= e.sets
                    Box(
                        Modifier.weight(1f).height(5.dp).clip(RoundedCornerShape(3.dp))
                            .background(when { i == idx -> Volt.accent; done -> Volt.accent.copy(alpha = 0.45f); else -> Volt.surface2 })
                            .tap(0.9f, haptic = false) { goTo(i) },
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            // Cronometro (sesion o descanso)
            Ticker(startMs, restEnd)
            Spacer(Modifier.height(6.dp))

            // Anillo con la figura del ejercicio dentro
            Box(Modifier.size(272.dp), contentAlignment = Alignment.Center) {
                SessionRing(
                    progress = if (resting) -1f else (doneSets / ex.sets.toFloat().coerceAtLeast(1f)),
                    restEnd = restEnd, restTotal = restTotal, modifier = Modifier.matchParentSize(),
                )
                Box(Modifier.size(210.dp).clip(CircleShape).background(Volt.surface), contentAlignment = Alignment.Center) {
                    AnimatedContent(ex.id, transitionSpec = { (fadeIn(tween(200)) + scaleIn(tween(240), initialScale = 0.9f)) togetherWith (fadeOut(tween(100)) + scaleOut(tween(120), targetScale = 1.05f)) }, label = "exImg") { id ->
                        val e = exercises.firstOrNull { it.id == id } ?: ex
                        if (e.gifUrl.isNotBlank()) ExerciseImage(e.gifUrl, gif, Modifier.fillMaxSize(), 480, 480)
                        else Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Icon(Icons.Outlined.FitnessCenter, null, tint = Volt.textDim, modifier = Modifier.size(64.dp)) }
                    }
                }
                ConfettiBurst(confetti, Modifier.matchParentSize(), big = true)
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(ex.id, transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(90)) }, label = "exName") { _ ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    FitText(ex.name, T.section.copy(fontSize = 19.sp), Volt.text, maxLines = 2, textAlign = TextAlign.Center)
                    Text(
                        if (resting) "Descansa · siguiente serie ${(doneSets + 1).coerceAtMost(ex.sets)} de ${ex.sets}"
                        else "Serie ${(doneSets + 1).coerceAtMost(ex.sets)} de ${ex.sets} · ${setsLabel(ex)}" + (ex.technique?.lineSequence()?.firstOrNull()?.let { "\n" + it.removePrefix("1. ") } ?: ""),
                        style = T.caption, color = Volt.textDim, textAlign = TextAlign.Center, maxLines = 2, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            Spacer(Modifier.height(14.dp))

            // Registro de la serie
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                when (type) {
                    "isometric" -> {
                        Stepper("Segundos", "${inputs.seconds}", Modifier.weight(1f), { inputs.seconds = (inputs.seconds - 5).coerceAtLeast(5) }) { inputs.seconds += 5 }
                        Stepper("RPE", "${inputs.rpe}", Modifier.weight(1f), { inputs.rpe = (inputs.rpe - 1).coerceAtLeast(1) }) { inputs.rpe = (inputs.rpe + 1).coerceAtMost(10) }
                    }
                    "cardio" -> Stepper("Minutos", "${inputs.minutes}", Modifier.weight(1f), { inputs.minutes = (inputs.minutes - 1).coerceAtLeast(1) }) { inputs.minutes += 1 }
                    else -> {
                        Stepper(if (type == "bodyweight") "Lastre kg" else "Peso kg", trim(inputs.weight), Modifier.weight(1f), { inputs.weight = (inputs.weight - 2.5).coerceAtLeast(0.0) }) { inputs.weight += 2.5 }
                        Stepper("Reps", "${inputs.reps}", Modifier.weight(1f), { inputs.reps = (inputs.reps - 1).coerceAtLeast(1) }) { inputs.reps = (inputs.reps + 1).coerceAtMost(200) }
                        Stepper("RIR", "${inputs.rir}", Modifier.weight(0.8f), { inputs.rir = (inputs.rir - 1).coerceAtLeast(0) }) { inputs.rir = (inputs.rir + 1).coerceAtMost(5) }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            // Curva de progreso (estilo "327 / 412 / 487 kcal")
            HistoryCurve(history.map { if (type == "isometric" || type == "cardio") it.maxDuration.toFloat() else it.maxWeight.toFloat() },
                unit = if (type == "isometric") "s" else if (type == "cardio") "s" else "kg", modifier = Modifier.weight(1f, fill = true))

            // Controles: anterior · accion principal · siguiente
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                IconCircle(Icons.Filled.ChevronLeft, size = 54.dp, desc = "Anterior", tint = if (idx > 0) Volt.text else Volt.textDim) { goTo(idx - 1) }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (resting) SmallPill("+15 s") { restEnd += 15_000; restTotal += 15; Haptics.selection(ctx) }
                    Box(
                        Modifier.size(76.dp).clip(CircleShape).background(Volt.accent).tap(0.9f, haptic = false) {
                            if (resting) { restEnd = 0L; Haptics.light(ctx) } else saveSet()
                        },
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(if (resting) Icons.Filled.SkipNext else Icons.Filled.Check, if (resting) "Saltar descanso" else "Guardar serie", tint = Color(0xFF0A0A0A), modifier = Modifier.size(34.dp))
                    }
                }
                IconCircle(Icons.Filled.ChevronRight, size = 54.dp, desc = "Siguiente", tint = if (idx < exercises.lastIndex) Volt.text else Volt.textDim) { goTo(idx + 1) }
            }
        }
    }

    summary?.let { data ->
        SummaryDialog(data, onDismiss = { summary = null; nav.pop() }, onToast = { t, m -> repo.toast("info", t, m) })
    }
}

/** Texto del cronometro: solo esta pieza se recompone cada segundo. */
@Composable
private fun Ticker(startMs: Long, restEnd: Long) {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(250) } }
    val resting = restEnd > now
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(if (resting) "DESCANSO" else "SESIÓN", style = T.label, color = if (resting) Volt.accent else Volt.textDim)
        Spacer(Modifier.width(8.dp))
        Text(mmss(if (resting) ((restEnd - now) / 1000).toInt() + 1 else ((now - startMs) / 1000).toInt()), style = T.section.copy(fontSize = 18.sp), color = Volt.text)
    }
}

/** Anillo: progreso de series, o cuenta atras del descanso (dibujado a 120 Hz en fase draw). Orbita punteada exterior. */
@Composable
private fun SessionRing(progress: Float, restEnd: Long, restTotal: Int, modifier: Modifier) {
    val accent = Volt.accent
    val track = Volt.surface2
    val anim = remember { Animatable(0f) }
    LaunchedEffect(progress) { if (progress >= 0f) anim.animateTo(progress.coerceIn(0f, 1f), tween(500, easing = FastOutSlowInEasing)) }
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(restEnd) {
        if (restEnd > 0L) while (System.currentTimeMillis() < restEnd) { withFrameMillis { frame = it } }
    }
    Canvas(modifier) {
        @Suppress("UNUSED_EXPRESSION") frame
        val sw = 10.dp.toPx()
        val d = size.minDimension - sw - 16.dp.toPx()
        val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
        // orbita punteada
        drawCircle(Color.White.copy(alpha = 0.10f), size.minDimension / 2 - 1.dp.toPx(), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 6.dp.toPx()))))
        drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
        val p = if (restEnd > 0L) ((restEnd - System.currentTimeMillis()).coerceAtLeast(0) / (restTotal * 1000f)).coerceIn(0f, 1f) else anim.value
        if (p > 0.001f) {
            drawArc(accent, -90f, 360f * p, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
            // punto en el extremo
            val ang = Math.toRadians((-90f + 360f * p).toDouble())
            val r = d / 2
            drawCircle(Color.White, sw * 0.42f, Offset(center.x + (r * kotlin.math.cos(ang)).toFloat(), center.y + (r * kotlin.math.sin(ang)).toFloat()))
        }
    }
}

@Composable
private fun Stepper(label: String, value: String, modifier: Modifier, onMinus: () -> Unit, onPlus: () -> Unit) {
    val ctx = LocalContext.current
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Volt.surface).padding(vertical = 10.dp, horizontal = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label.uppercase(), style = T.label, color = Volt.textDim, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            SmallRound(Icons.Filled.Remove, "Menos") { Haptics.selection(ctx); onMinus() }
            AnimatedContent(value, transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(80)) }, label = "step", modifier = Modifier.weight(1f)) { v ->
                Text(v, style = T.value.copy(fontSize = 22.sp), color = Volt.text, textAlign = TextAlign.Center, maxLines = 1, modifier = Modifier.fillMaxWidth())
            }
            SmallRound(Icons.Filled.Add, "Más") { Haptics.selection(ctx); onPlus() }
        }
    }
}

@Composable
private fun SmallRound(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, size: Dp = 30.dp, onClick: () -> Unit) {
    Box(Modifier.size(size).clip(CircleShape).background(Volt.surface2).tap(0.85f, haptic = false, onClick = onClick), contentAlignment = Alignment.Center) {
        Icon(icon, desc, tint = Volt.text, modifier = Modifier.size(16.dp))
    }
}

/** Curva de historial con 3 etiquetas (como 327 / 412 / 487 kcal del mockup). */
@Composable
private fun HistoryCurve(values: List<Float>, unit: String, modifier: Modifier) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.Center) {
        if (values.size < 2) {
            Text("Tu progreso aparecerá aquí tras dos sesiones.", style = T.caption, color = Volt.textDim, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        } else {
            val shown = values.takeLast(6)
            WaveLine(shown, Modifier.fillMaxWidth().height(46.dp), color = Volt.water)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                val picks = listOf(shown.first(), shown[shown.size / 2], shown.last())
                picks.forEachIndexed { i, v ->
                    Text("${trim(v.toDouble())} $unit", style = T.caption, color = if (i == 2) Volt.text else Volt.textDim)
                }
            }
        }
    }
}
