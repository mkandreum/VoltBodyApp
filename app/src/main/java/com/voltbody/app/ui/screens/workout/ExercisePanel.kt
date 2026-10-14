package com.voltbody.app.ui.screens.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.Exercise
import com.voltbody.app.data.Repository
import com.voltbody.app.data.WorkoutLog
import com.voltbody.app.ui.BentoCard
import com.voltbody.app.ui.PrimaryButton
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.util.Haptics
import com.voltbody.app.util.evaluateAchievements
import com.voltbody.app.util.getExerciseHistory
import com.voltbody.app.util.getProgressiveSuggestion
import com.voltbody.app.util.logDayKey
import com.voltbody.app.util.rememberHaptics
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import java.time.Instant
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/** Alcance de vida de la app: el guardado de series no debe cancelarse al cambiar de ejercicio. */
internal val appWorkScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

/** Evalúa logros tras registrar series y los persiste en el servidor (POST /data/achievements) (+ toast + banner). */
internal suspend fun checkAchievements(repo: Repository, haptics: Haptics, onBanner: (Banner) -> Unit) {
    val fresh = runCatching { evaluateAchievements(repo.state.value) }.getOrDefault(emptyList())
    if (fresh.isEmpty()) return
    val stamp = Instant.now().toString()
    fresh.forEach { a ->
        haptics.heavy()
        repo.toast("success", "Logro desbloqueado: ${a.label}", a.description)
        onBanner(Banner("Logro desbloqueado: ${a.label}", a.description, "achievement"))
    }
    fresh.forEach { repo.unlockAchievement(if (it.unlockedAt == null) it.copy(unlockedAt = stamp) else it) }
}

@Composable
private fun OneRepMax(weight: () -> String, reps: () -> String) {
    val w = weight().toD()
    val r = reps().toD().toInt()
    AnimatedVisibility(w > 0 && r > 1, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Volt.surface2).padding(12.dp), horizontalArrangement = Arrangement.Center) {
            Text("1RM estimado (Epley): ", color = Volt.textDim, style = VoltType.body)
            Text("${(w * (1 + r / 30.0)).roundToInt()} kg", color = Volt.accent, style = VoltType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold))
        }
    }
}

/**
 * Pantalla de ejercicio (estilo "The Power Start"): GIF héroe sobre degradado de marca, nombre gigante, chips, técnica en tarjetas,
 * registro con números gigantes (arrastre vertical + háptica), series de hoy animadas, sobrecarga progresiva, PoseCoach e historial.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExercisePanel(
    repo: Repository, logs: List<WorkoutLog>, bodyWeight: Double, ex: Exercise, index: Int, count: Int, dateKey: String,
    onPrev: (() -> Unit)?, onNext: (() -> Unit)?, onLogged: (restSeconds: Int) -> Unit, onBanner: (Banner) -> Unit,
    onRecord: () -> Unit, onOpenPose: () -> Unit, modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val type = remember(ex) { exerciseTypeOf(ex) }
    val targetSets = ex.targetSets()
    val isDuration = type == T_ISOMETRIC || type == T_CARDIO

    // ── Derivados de los logs (siempre bajo remember) ──
    val todayIdx = remember(logs, ex.id, dateKey) { logs.indices.filter { logs[it].exerciseId == ex.id && logDayKey(logs[it].date) == dateKey } }
    val doneSets = todayIdx.size
    val exLogs = remember(logs, ex.id) { logs.filter { it.exerciseId == ex.id } }
    val prWeight = remember(exLogs) { exLogs.maxOfOrNull { it.weight } ?: 0.0 }
    val prReps = remember(exLogs) { exLogs.maxOfOrNull { it.reps } ?: 0 }
    val lastEntry = remember(exLogs) { exLogs.maxByOrNull { it.date } }
    val bestTime = remember(exLogs) { exLogs.mapNotNull { it.duration }.maxOrNull() }
    val lastDurLog = remember(exLogs) { exLogs.filter { it.duration != null }.maxByOrNull { it.date } }
    val suggestion = remember(logs, ex.id) { getProgressiveSuggestion(ex.id, logs) }
    val history = remember(logs, ex.id) { getExerciseHistory(ex.id, logs) }
    val rows = remember(logs, ex.id, dateKey, type) {
        fun metric(l: WorkoutLog): Double = if (isDuration) (l.duration ?: 0).toDouble() else if (type == T_BODYWEIGHT) l.reps.toDouble() else l.weight
        val prevMax = logs.filter { it.exerciseId == ex.id && logDayKey(it.date) < dateKey }.maxOfOrNull { metric(it) } ?: 0.0
        var soFar = prevMax
        todayIdx.map { gi ->
            val m = metric(logs[gi])
            val pr = prevMax > 0 && m > soFar
            if (m > soFar) soFar = m
            SetRow(gi, logs[gi], pr)
        }
    }

    // ── Entradas ──
    var weightText by remember(ex.id) {
        mutableStateOf(lastEntry?.weight?.takeIf { it > 0 }?.let { fmtNum(it) } ?: ex.weight.takeIf { it > 0 }?.let { fmtNum(it) } ?: "")
    }
    var repsText by remember(ex.id) { mutableStateOf("") }
    var durText by remember(ex.id) { mutableStateOf("") }
    var setsN by remember(ex.id) { mutableIntStateOf(1) }
    var rpe by remember(ex.id) { mutableIntStateOf(-1) }
    var rir by remember(ex.id) { mutableIntStateOf(-1) }
    var timerTick by remember(ex.id) { mutableIntStateOf(0) }
    var syncStatus by remember(ex.id) { mutableStateOf("idle") }
    var locked by remember(ex.id) { mutableStateOf(false) }
    var editIdx by remember(ex.id) { mutableStateOf<Int?>(null) }
    var editWeight by remember(ex.id) { mutableStateOf("") }
    var editReps by remember(ex.id) { mutableStateOf("") }
    var editDur by remember(ex.id) { mutableStateOf("") }

    val canSave by remember(type) {
        derivedStateOf {
            when (type) {
                T_ISOMETRIC, T_CARDIO -> durText.toD() > 0
                T_BODYWEIGHT -> repsText.toD() > 0
                else -> weightText.toD() > 0 && repsText.toD() > 0
            }
        }
    }
    val weightEmpty by remember { derivedStateOf { weightText.isEmpty() } }

    fun commit(newLogs: List<WorkoutLog>, title: String, msg: String, rest: Int, record: Boolean) {
        haptics.success()
        onLogged(rest)
        onBanner(Banner(title, msg))
        if (record) {
            onRecord()
            onBanner(Banner("Nuevo récord", ex.name, "achievement"))
        }
        syncStatus = "syncing"
        appWorkScope.launch {
            val jobs = newLogs.map { async { repo.addLog(it) } }
            yield() // deja que el guardado local ocurra antes de evaluar logros
            checkAchievements(repo, haptics, onBanner)
            jobs.awaitAll()
            val synced = repo.state.value.logs
            syncStatus = if (newLogs.all { l -> synced.any { it.date == l.date && it.exerciseId == l.exerciseId && it.id != null } }) "synced" else "local"
        }
        locked = true
        appWorkScope.launch { delay(700); locked = false }
    }

    fun save() {
        if (!canSave || locked) return
        val c = setsN.coerceAtLeast(1)
        val titleTxt = if (c > 1) "$c series registradas" else "Serie registrada"
        when (type) {
            T_ISOMETRIC, T_CARDIO -> {
                val secs = if (type == T_CARDIO) (durText.toD() * 60).roundToInt() else durText.toD().roundToInt()
                if (secs <= 0) return
                val newLogs = List(c) { WorkoutLog(nowIso(it.toLong()), ex.id, 0.0, 0, duration = secs, rpe = rpe.takeIf { r -> r >= 1 }) }
                val label = if (type == T_CARDIO) "${secs / 60} min" else formatDuration(secs)
                val record = (bestTime ?: 0) > 0 && secs > (bestTime ?: 0)
                commit(newLogs, titleTxt, if (rpe >= 1) "$label · RPE $rpe" else label, 60, record)
                val target = ex.durationTarget
                if (type == T_ISOMETRIC && target != null && secs >= target) {
                    val pastBeats = logs.count { it.exerciseId == ex.id && (it.duration ?: 0) >= target }
                    if (pastBeats >= 2) onBanner(Banner("Objetivo isométrico superado", "Considera una variante más exigente en la próxima sesión.", "info"))
                }
                durText = ""; rpe = -1; setsN = 1; timerTick++
            }
            T_BODYWEIGHT -> {
                val reps = repsText.toD().roundToInt()
                if (reps <= 0) return
                val newLogs = List(c) { WorkoutLog(nowIso(it.toLong()), ex.id, bodyWeight, reps, rir = rir.takeIf { r -> r >= 0 }) }
                commit(newLogs, titleTxt, if (rir >= 0) "$reps reps · RIR $rir" else "$reps reps", 90, prReps > 0 && reps > prReps)
                repsText = ""; rir = -1; setsN = 1
            }
            else -> {
                val w = weightText.toD(); val reps = repsText.toD().roundToInt()
                if (w <= 0 || reps <= 0) return
                val newLogs = List(c) { WorkoutLog(nowIso(it.toLong()), ex.id, w, reps, rir = rir.takeIf { r -> r >= 0 }) }
                commit(newLogs, titleTxt, if (rir >= 0) "${fmtNum(w)}kg × $reps reps · RIR $rir" else "${fmtNum(w)}kg × $reps reps", 90, prWeight > 0 && w > prWeight)
                if (doneSets + c >= targetSets && ex.weight > 0 && w >= ex.weight) {
                    val next = ((w + 2.5) * 2).roundToLong() / 2.0
                    onBanner(Banner("Objetivo cumplido", "Considera subir a ${fmtNum(next)}kg la próxima sesión.", "info"))
                }
                rir = -1; setsN = 1 // peso y reps se conservan para la siguiente serie
            }
        }
    }

    fun saveEdit() {
        val i = editIdx ?: return
        val old = logs.getOrNull(i) ?: return
        val newW = if (isDuration) old.weight else if (type == T_BODYWEIGHT) old.weight else editWeight.toD()
        val newR = if (isDuration) old.reps else editReps.toD().roundToInt()
        val newD = if (isDuration) (if (type == T_CARDIO) (editDur.toD() * 60).roundToInt() else editDur.toD().roundToInt()) else old.duration
        if (isDuration && (newD ?: 0) <= 0) return
        if (!isDuration && newR <= 0) return
        appWorkScope.launch { repo.updateLog(old.copy(weight = newW, reps = newR, duration = newD)) }
        haptics.medium()
        onBanner(Banner("Serie actualizada", if (isDuration) "${newD}s" else "${fmtNum(newW)}kg × $newR reps"))
        editIdx = null
    }

    fun deleteEdit() {
        val i = editIdx ?: return
        val old = logs.getOrNull(i) ?: return
        appWorkScope.launch { repo.deleteLog(old) }
        haptics.medium()
        onBanner(Banner("Serie eliminada", ex.name))
        editIdx = null
    }

    val c1 = Volt.accent
    val c2 = Volt.accent2
    val bg = Volt.bg

    Column(modifier) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // ── Héroe: GIF sobre degradado de marca ──
            Box(
                Modifier.fillMaxWidth().height(330.dp).drawBehind {
                    drawRect(Brush.linearGradient(listOf(c1.copy(alpha = 0.85f), c2.copy(alpha = 0.55f)), start = Offset.Zero, end = Offset(size.width, size.height)))
                    drawRect(Brush.verticalGradient(listOf(Color.Transparent, bg), startY = size.height * 0.55f, endY = size.height))
                },
            ) {
                ExerciseImage(
                    ex.gifUrl, ex.name, maxPx = 720,
                    modifier = Modifier.align(Alignment.TopCenter).padding(start = 20.dp, end = 20.dp, top = 8.dp).fillMaxWidth().height(290.dp).clip(RoundedCornerShape(36.dp)),
                )
                Box(
                    Modifier.align(Alignment.TopEnd).padding(top = 22.dp, end = 32.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 14.dp, vertical = 7.dp),
                ) { Text("${index + 1} / $count", color = Color.White, style = VoltType.label) }
            }

            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Column {
                    Text(ex.muscleGroup.ifBlank { "General" }.uppercase(), color = Volt.accent, style = VoltType.label)
                    Spacer(Modifier.height(4.dp))
                    Text(ex.name, color = Volt.text, style = VoltType.display.copy(fontSize = 40.sp, lineHeight = 42.sp), maxLines = 3)
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip("${ex.sets} × ${ex.reps}", color = Volt.text)
                    when (type) {
                        T_ISOMETRIC -> ex.durationTarget?.let { InfoChip("Objetivo ${it}s", color = Volt.accent) }
                        T_WEIGHTED -> {
                            val sugg = suggestion?.suggestedWeight ?: ex.weight.takeIf { it > 0 }
                            if (sugg != null) InfoChip("Sugerido ${fmtNum(sugg)} kg", color = Volt.accent)
                        }
                        else -> Unit
                    }
                    if (isDuration) {
                        lastDurLog?.duration?.let { d -> InfoChip("Último ${formatDuration(d)}" + (lastDurLog?.rpe?.let { " · RPE $it" } ?: "")) }
                        if (bestTime != null) InfoChip("PR ${formatDuration(bestTime)}", color = PrGold)
                    } else {
                        if ((lastEntry?.weight ?: 0.0) > 0) InfoChip("Último ${fmtNum(lastEntry!!.weight)} kg × ${lastEntry.reps}")
                        else if (type == T_BODYWEIGHT && lastEntry != null) InfoChip("Último ${lastEntry.reps} reps")
                        if (type != T_BODYWEIGHT && prWeight > 0) InfoChip("PR ${fmtNum(prWeight)} kg", color = PrGold)
                        if (type == T_BODYWEIGHT && prReps > 0) InfoChip("PR $prReps reps", color = PrGold)
                    }
                }

                // Sobrecarga progresiva
                AnimatedVisibility(suggestion != null && type == T_WEIGHTED, enter = slideInVertically { -it / 2 } + fadeIn(tween(400))) {
                    suggestion?.let { sg ->
                        BentoCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Volt.accent)
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text("Sobrecarga progresiva", color = Volt.accent, style = VoltType.label)
                                    Text("${fmtNum(sg.currentWeight)} kg en ${sg.sessionsAnalyzed} sesiones: prueba ${fmtNum(sg.suggestedWeight)} kg", color = Volt.text, style = VoltType.body)
                                }
                                Box(
                                    Modifier.clip(CircleShape).background(Volt.gradient).pressScale { weightText = fmtNum(sg.suggestedWeight); haptics.light() }
                                        .padding(horizontal = 18.dp, vertical = 12.dp),
                                ) { Text("Usar", color = Volt.onAccent, style = VoltType.title.copy(fontSize = 14.sp)) }
                            }
                        }
                    }
                }

                // PoseCoach
                if (type != T_CARDIO) {
                    Row(
                        Modifier.fillMaxWidth().clip(CircleShape).background(Volt.surface).pressScale { haptics.light(); onOpenPose() }.padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(Modifier.size(40.dp).clip(CircleShape).background(Volt.gradient), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.Videocam, null, tint = Volt.onAccent, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("PoseCoach", color = Volt.text, style = VoltType.title)
                            Text("Corrección de postura en tiempo real", color = Volt.textDim, style = VoltType.body)
                        }
                        Text("Abrir", color = Volt.accent, style = VoltType.label)
                    }
                }

                // Técnica
                techniqueOf(ex)?.let { TechniqueSection(it) }

                // ── Series de hoy ──
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("Series de hoy", color = Volt.text, style = VoltType.h2, modifier = Modifier.weight(1f))
                        Text(
                            when (syncStatus) { "syncing" -> "sincronizando…"; "synced" -> "sincronizado"; "local" -> "solo local"; else -> "$doneSets/$targetSets" },
                            color = if (syncStatus == "synced") Volt.accent2 else Volt.textDim, style = VoltType.label,
                        )
                    }
                    SetsList(rows, targetSets, type, onEdit = { r ->
                        val l = r.log
                        editIdx = r.gi
                        editWeight = if (l.weight > 0) fmtNum(l.weight) else ""
                        editReps = if (l.reps > 0) l.reps.toString() else ""
                        editDur = l.duration?.let { if (type == T_CARDIO) (it / 60).toString() else it.toString() } ?: ""
                        haptics.selection()
                    })
                    AnimatedVisibility(editIdx != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        EditSetCard(
                            type, editWeight, editReps, editDur, { editWeight = it }, { editReps = it }, { editDur = it },
                            valid = if (isDuration) editDur.toD() > 0 else editReps.toD() > 0,
                            onSave = { saveEdit() }, onDelete = { deleteEdit() }, onClose = { editIdx = null },
                        )
                    }
                }

                // ── Registro ──
                BentoCard(Modifier.fillMaxWidth(), hero = true, padding = 16.dp) {
                    Text("Registrar serie", color = Volt.text, style = VoltType.h2)
                    Text("Arrastra el número hacia arriba o abajo. Toca para teclear.", color = Volt.textDim, style = VoltType.body)
                    Spacer(Modifier.height(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        when (type) {
                            T_ISOMETRIC, T_CARDIO -> {
                                androidx.compose.runtime.key(timerTick) {
                                    StopwatchRing(
                                        targetSec = if (type == T_ISOMETRIC) ex.durationTarget else null,
                                        onStop = { secs -> durText = if (type == T_CARDIO) ceil(secs / 60.0).toInt().coerceAtLeast(1).toString() else secs.toString() },
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    if (type == T_CARDIO) {
                                        DragNumber("Duración", "min", { durText }, { durText = it }, 1.0, 0.0, 300.0, Modifier.weight(1.4f), startAt = 20.0)
                                    } else {
                                        DragNumber("Duración", "s", { durText }, { durText = it }, 5.0, 0.0, 3600.0, Modifier.weight(1.4f), startAt = 30.0)
                                    }
                                    DragNumber("Series", "", { setsN.toString() }, { setsN = it.toIntOrNull()?.coerceIn(1, 10) ?: 1 }, 1.0, 1.0, 10.0, Modifier.weight(1f), numberSize = 40)
                                }
                                if (type == T_ISOMETRIC) {
                                    DragNumber(
                                        "RPE (esfuerzo)", "", { if (rpe < 1) "" else rpe.toString() }, { rpe = it.toIntOrNull() ?: -1 },
                                        1.0, 1.0, 10.0, Modifier.fillMaxWidth(), numberSize = 40, startAt = 8.0,
                                    )
                                }
                            }
                            T_BODYWEIGHT -> {
                                if (bodyWeight > 0) {
                                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Volt.surface2).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                        Column(Modifier.weight(1f)) {
                                            FieldLabel("Peso corporal de referencia")
                                            Text("${fmtNum(bodyWeight)} kg", color = Volt.text, style = VoltType.numberS)
                                        }
                                    }
                                }
                                DragNumber("Repeticiones", "", { repsText }, { repsText = it }, 1.0, 0.0, 200.0, Modifier.fillMaxWidth(), startAt = 10.0)
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DragNumber("RIR", "", { if (rir < 0) "" else rir.toString() }, { rir = it.toIntOrNull() ?: -1 }, 1.0, 0.0, 5.0, Modifier.weight(1f), numberSize = 40, startAt = 2.0)
                                    DragNumber("Series", "", { setsN.toString() }, { setsN = it.toIntOrNull()?.coerceIn(1, 10) ?: 1 }, 1.0, 1.0, 10.0, Modifier.weight(1f), numberSize = 40)
                                }
                            }
                            else -> {
                                WeightCalculator(
                                    exerciseId = ex.id, exerciseName = ex.name, targetWeight = ex.weight, userBodyweight = bodyWeight,
                                    weightIsEmpty = weightEmpty, onWeightChange = { weightText = fmtNum(it) },
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DragNumber("Peso", "kg", { weightText }, { weightText = it }, 2.5, 0.0, 500.0, Modifier.weight(1.15f), decimal = true, startAt = 20.0)
                                    DragNumber("Reps", "", { repsText }, { repsText = it }, 1.0, 0.0, 100.0, Modifier.weight(1f), startAt = 10.0)
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    DragNumber("RIR", "", { if (rir < 0) "" else rir.toString() }, { rir = it.toIntOrNull() ?: -1 }, 1.0, 0.0, 5.0, Modifier.weight(1f), numberSize = 40, startAt = 2.0)
                                    DragNumber("Series", "", { setsN.toString() }, { setsN = it.toIntOrNull()?.coerceIn(1, 10) ?: 1 }, 1.0, 1.0, 10.0, Modifier.weight(1f), numberSize = 40)
                                }
                                OneRepMax({ weightText }, { repsText })
                            }
                        }
                    }
                }

                HistorySection(history, type)
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Barra de acciones fija ──
        Row(
            Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Volt.bg, Volt.bg))).padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (onPrev != null) RoundBtn(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Ejercicio anterior", onPrev, size = 58.dp)
            PrimaryButton(
                if (setsN > 1) "Guardar $setsN series" else "Guardar serie", onClick = { save() },
                modifier = Modifier.weight(1f), enabled = canSave && !locked,
            )
            if (onNext != null) {
                val done = doneSets >= targetSets
                Box(
                    Modifier.size(58.dp).clip(CircleShape).background(if (done) Volt.gradient else Brush.linearGradient(listOf(Volt.surface2, Volt.surface2))).pressScale(onNext),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Siguiente ejercicio", tint = if (done) Volt.onAccent else Volt.text) }
            }
        }
    }
}
