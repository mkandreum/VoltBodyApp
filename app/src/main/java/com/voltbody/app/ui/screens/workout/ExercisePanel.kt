package com.voltbody.app.ui.screens.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.AppState
import com.voltbody.app.data.Exercise
import com.voltbody.app.data.Repository
import com.voltbody.app.data.WorkoutLog
import com.voltbody.app.ui.VoltCard
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.theme.Volt
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
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val Amber = Color(0xFFFBBF24)

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
private fun SetDot(n: Int, done: Boolean, onClick: () -> Unit) {
    val sc = remember { Animatable(if (done) 0.4f else 1f) }
    LaunchedEffect(done) { if (done) sc.animateTo(1f, spring(0.4f, 400f)) else sc.snapTo(1f) }
    Box(
        Modifier.size(44.dp).scale(sc.value).clip(RoundedCornerShape(12.dp)).background(if (done) Volt.accent else Volt.surface2)
            .let { if (done) it.clickable(onClick = onClick) else it },
        contentAlignment = Alignment.Center,
    ) {
        if (done) Icon(Icons.Filled.Check, "Editar serie $n", tint = Color.Black, modifier = Modifier.size(20.dp))
        else Text("$n", color = Volt.textDim, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun Accordion(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, open: Boolean, onToggle: () -> Unit, trailing: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onToggle).padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = Volt.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(title, color = Volt.accent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            if (trailing != null) { Spacer(Modifier.width(6.dp)); Text(trailing, color = Volt.textDim, fontSize = 12.sp) }
        }
        AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(Modifier.fillMaxWidth().padding(top = 4.dp), content = content)
        }
    }
}

/**
 * Panel de un ejercicio dentro de la sesión: imagen/GIF, PR, sobrecarga progresiva, PoseCoach, técnica,
 * registro de series (peso/reps/RIR · duración/RPE · peso corporal), edición, historial y barra de acciones.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExercisePanel(
    repo: Repository, st: AppState, ex: Exercise, index: Int, count: Int, dateKey: String,
    onPrev: (() -> Unit)?, onNext: (() -> Unit)?, onLogged: (restSeconds: Int) -> Unit, onBanner: (Banner) -> Unit,
    onOpenPose: () -> Unit, modifier: Modifier = Modifier,
) {
    val haptics = rememberHaptics()
    val type = remember(ex) { exerciseTypeOf(ex) }
    val logs = st.logs
    val bodyWeight = st.profile.weight ?: 0.0
    val targetSets = ex.targetSets()
    val isDuration = type == T_ISOMETRIC || type == T_CARDIO

    // ── Derivados de los logs ──
    val todayIdx = remember(logs, ex.id, dateKey) { logs.indices.filter { logs[it].exerciseId == ex.id && logDayKey(logs[it].date) == dateKey } }
    val doneSets = todayIdx.size
    val exLogs = remember(logs, ex.id) { logs.filter { it.exerciseId == ex.id } }
    val prWeight = remember(exLogs) { exLogs.maxOfOrNull { it.weight } ?: 0.0 }
    val lastEntry = remember(exLogs) { exLogs.maxByOrNull { it.date } }
    val bestTime = remember(exLogs) { exLogs.mapNotNull { it.duration }.maxOrNull() }
    val lastDurLog = remember(exLogs) { exLogs.filter { it.duration != null }.maxByOrNull { it.date } }
    val suggestion = remember(logs, ex.id) { getProgressiveSuggestion(ex.id, logs) }
    val history = remember(logs, ex.id) { getExerciseHistory(ex.id, logs) }

    // ── Entradas ──
    var weightText by remember(ex.id) {
        mutableStateOf(lastEntry?.weight?.takeIf { it > 0 }?.let { fmtNum(it) } ?: ex.weight.takeIf { it > 0 }?.let { fmtNum(it) } ?: "")
    }
    var repsText by remember(ex.id) { mutableStateOf("") }
    var durText by remember(ex.id) { mutableStateOf("") }
    var setsN by remember(ex.id) { mutableIntStateOf(1) }
    var rpe by remember(ex.id) { mutableIntStateOf(-1) }
    var rir by remember(ex.id) { mutableIntStateOf(-1) }
    var running by remember(ex.id) { mutableStateOf(false) }
    var elapsed by remember(ex.id) { mutableIntStateOf(0) }
    var showTechnique by remember(ex.id) { mutableStateOf(false) }
    var showHistory by remember(ex.id) { mutableStateOf(false) }
    var syncStatus by remember(ex.id) { mutableStateOf("idle") }
    var locked by remember(ex.id) { mutableStateOf(false) }
    var editIdx by remember(ex.id) { mutableStateOf<Int?>(null) }
    var editWeight by remember(ex.id) { mutableStateOf("") }
    var editReps by remember(ex.id) { mutableStateOf("") }
    var editDur by remember(ex.id) { mutableStateOf("") }

    LaunchedEffect(running) { if (running) while (true) { delay(1000); elapsed++ } }

    val canSave = when (type) {
        T_ISOMETRIC, T_CARDIO -> durText.toD() > 0
        T_BODYWEIGHT -> repsText.toD() > 0
        else -> weightText.toD() > 0 && repsText.toD() > 0
    }

    fun commit(newLogs: List<WorkoutLog>, title: String, msg: String, rest: Int) {
        haptics.success()
        onLogged(rest)
        onBanner(Banner(title, msg))
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
        val titleTxt = if (c > 1) "$c series registradas 💪" else "Serie registrada 💪"
        when (type) {
            T_ISOMETRIC, T_CARDIO -> {
                val secs = if (type == T_CARDIO) (durText.toD() * 60).roundToInt() else durText.toD().roundToInt()
                if (secs <= 0) return
                val newLogs = List(c) { WorkoutLog(nowIso(it.toLong()), ex.id, 0.0, 0, duration = secs, rpe = rpe.takeIf { r -> r >= 1 }) }
                val label = if (type == T_CARDIO) "${secs / 60} min" else formatDuration(secs)
                commit(newLogs, titleTxt, if (rpe >= 1) "$label · RPE $rpe" else label, 60)
                val target = ex.durationTarget
                if (type == T_ISOMETRIC && target != null && secs >= target) {
                    val pastBeats = logs.count { it.exerciseId == ex.id && (it.duration ?: 0) >= target }
                    if (pastBeats >= 2) onBanner(Banner("🔥 ¡Objetivo isométrico superado!", "Considera una variante más exigente en la próxima sesión.", "info"))
                }
                durText = ""; rpe = -1; setsN = 1; elapsed = 0
            }
            T_BODYWEIGHT -> {
                val reps = repsText.toD().roundToInt()
                if (reps <= 0) return
                val newLogs = List(c) { WorkoutLog(nowIso(it.toLong()), ex.id, bodyWeight, reps, rir = rir.takeIf { r -> r >= 0 }) }
                commit(newLogs, titleTxt, if (rir >= 0) "$reps reps · RIR $rir" else "$reps reps", 90)
                repsText = ""; rir = -1; setsN = 1
            }
            else -> {
                val w = weightText.toD(); val reps = repsText.toD().roundToInt()
                if (w <= 0 || reps <= 0) return
                val newLogs = List(c) { WorkoutLog(nowIso(it.toLong()), ex.id, w, reps, rir = rir.takeIf { r -> r >= 0 }) }
                commit(newLogs, titleTxt, if (rir >= 0) "${fmtNum(w)}kg × $reps reps · RIR $rir" else "${fmtNum(w)}kg × $reps reps", 90)
                if (doneSets + c >= targetSets && ex.weight > 0 && w >= ex.weight) {
                    val next = ((w + 2.5) * 2).roundToLong() / 2.0
                    onBanner(Banner("📈 ¡Objetivo cumplido!", "Considera subir a ${fmtNum(next)}kg la próxima sesión.", "info"))
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
        onBanner(Banner("Serie actualizada ✏️", if (isDuration) "${newD}s" else "${fmtNum(newW)}kg × $newR reps"))
        editIdx = null
    }

    fun deleteEdit() {
        val i = editIdx ?: return
        val old = logs.getOrNull(i) ?: return
        appWorkScope.launch { repo.deleteLog(old) }
        haptics.medium()
        onBanner(Banner("Serie eliminada 🗑️", ex.name))
        editIdx = null
    }

    Column(modifier) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState())) {
            // ── Cabecera con imagen ──
            Box(Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp))) {
                ExerciseImage(ex.gifUrl, ex.name, Modifier.fillMaxSize())
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f), Volt.bg.copy(alpha = 0.95f)))))
                Box(
                    Modifier.align(Alignment.TopEnd).padding(14.dp).clip(RoundedCornerShape(50)).background(Color.Black.copy(alpha = 0.6f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                ) { Text("${index + 1}/$count", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
            }

            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Column {
                    Text(ex.name, color = Volt.text, fontSize = 24.sp, fontWeight = FontWeight.Black)
                    if (type == T_ISOMETRIC || type == T_CARDIO) {
                        if (bestTime != null || lastDurLog != null) {
                            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 4.dp)) {
                                if (bestTime != null) Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Filled.EmojiEvents, null, tint = Amber, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
                                    Text("PR: ${formatDuration(bestTime)}", color = Amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                                }
                                lastDurLog?.duration?.let { d ->
                                    Text("Último: ${formatDuration(d)}" + (lastDurLog?.rpe?.let { " · RPE $it" } ?: ""), color = Volt.textDim, fontSize = 12.sp)
                                }
                            }
                        }
                    } else if (prWeight > 0 || (lastEntry?.weight ?: 0.0) > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(14.dp), modifier = Modifier.padding(top = 4.dp)) {
                            if (prWeight > 0) Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.EmojiEvents, null, tint = Amber, modifier = Modifier.size(14.dp)); Spacer(Modifier.width(4.dp))
                                Text("PR: ${fmtNum(prWeight)} kg", color = Amber, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            if ((lastEntry?.weight ?: 0.0) > 0) Text("Último: ${fmtNum(lastEntry!!.weight)} kg", color = Volt.textDim, fontSize = 12.sp)
                        }
                    }
                }

                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InfoChip(ex.muscleGroup.ifBlank { "General" }, color = Volt.accent)
                    InfoChip(when (type) { T_ISOMETRIC -> "⏱ Isométrico"; T_BODYWEIGHT -> "🤸 Peso corporal"; T_CARDIO -> "🏃 Cardio"; else -> "🏋️ Con peso" })
                    InfoChip("${ex.sets} × ${ex.reps}")
                    if (type == T_WEIGHTED && ex.weight > 0) InfoChip("Meta: ${fmtNum(ex.weight)} kg", color = Volt.accent)
                    if (type == T_ISOMETRIC && ex.durationTarget != null) InfoChip("Objetivo: ${ex.durationTarget}s", color = Volt.accent)
                }

                // Sobrecarga progresiva
                AnimatedVisibility(suggestion != null && type == T_WEIGHTED, enter = slideInVertically { -it / 2 } + fadeIn(tween(400))) {
                    suggestion?.let { sg ->
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Volt.accent.copy(alpha = 0.08f))
                                .border(1.dp, Volt.accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = Volt.accent)
                            Spacer(Modifier.width(10.dp))
                            Column(Modifier.weight(1f)) {
                                Text("📈 Sobrecarga progresiva sugerida", color = Volt.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                Text("${fmtNum(sg.currentWeight)} kg en ${sg.sessionsAnalyzed} sesiones → prueba ${fmtNum(sg.suggestedWeight)} kg", color = Volt.text, fontSize = 12.sp)
                            }
                            Box(
                                Modifier.clip(RoundedCornerShape(14.dp)).background(Volt.accent).pressScale { weightText = fmtNum(sg.suggestedWeight); haptics.light() }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                            ) { Text("Usar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                        }
                    }
                }

                // PoseCoach
                if (type != T_CARDIO) {
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Volt.surface).border(1.dp, Volt.accent.copy(alpha = 0.35f), RoundedCornerShape(20.dp))
                            .pressScale { haptics.light(); onOpenPose() }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Videocam, null, tint = Volt.accent)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("🎥 PoseCoach — Análisis de postura", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text("Corrección angular en tiempo real con IA", color = Volt.textDim, fontSize = 11.sp)
                        }
                        Text("Abrir →", color = Volt.accent, fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                    }
                }

                // Técnica
                techniqueOf(ex)?.let { tech ->
                    Accordion(if (showTechnique) "Ocultar guía técnica" else "Ver guía técnica", Icons.Filled.Info, showTechnique, { showTechnique = !showTechnique }) {
                        Column(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Volt.surface).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) { tech.split("\n").forEach { Text(it, color = Volt.textDim, fontSize = 13.sp, lineHeight = 18.sp) } }
                    }
                }

                // ── Registro de series ──
                VoltCard(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bolt, null, tint = Volt.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Registrar series", color = Volt.text, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        Text(
                            when (syncStatus) { "syncing" -> "sincronizando..."; "synced" -> "sincronizado ✅"; "local" -> "error al guardar"; else -> "$doneSets/$targetSets" },
                            color = if (syncStatus == "synced") Color(0xFF34D399) else Volt.textDim, fontSize = 11.sp,
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        repeat(targetSets.coerceAtLeast(doneSets)) { i ->
                            val done = i < doneSets
                            SetDot(i + 1, done) {
                                val gi = todayIdx.getOrNull(i) ?: return@SetDot
                                val l = logs[gi]
                                editIdx = gi
                                editWeight = if (l.weight > 0) fmtNum(l.weight) else ""
                                editReps = if (l.reps > 0) l.reps.toString() else ""
                                editDur = l.duration?.let { if (type == T_CARDIO) (it / 60).toString() else it.toString() } ?: ""
                                haptics.selection()
                            }
                        }
                    }

                    AnimatedVisibility(editIdx != null, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                        Column(
                            Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Volt.surface2).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("✏️ Editar serie realizada", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                                Box(Modifier.size(32.dp).clip(RoundedCornerShape(16.dp)).clickable { editIdx = null }, contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Close, "Cerrar edición", tint = Volt.textDim, modifier = Modifier.size(18.dp))
                                }
                            }
                            if (isDuration) {
                                FieldLabel(if (type == T_CARDIO) "Duración (min)" else "Duración (seg)")
                                NumField(editDur, { editDur = it }, Modifier.fillMaxWidth())
                            } else {
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FieldLabel(if (type == T_BODYWEIGHT) "Peso ref. (kg)" else "Peso (kg)")
                                        NumField(editWeight, { editWeight = it }, Modifier.fillMaxWidth(), decimal = true, readOnly = type == T_BODYWEIGHT)
                                    }
                                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        FieldLabel("Reps")
                                        NumField(editReps, { editReps = it }, Modifier.fillMaxWidth())
                                    }
                                }
                            }
                            val valid = if (isDuration) editDur.toD() > 0 else editReps.toD() > 0
                            Box(
                                Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(14.dp)).background(if (valid) Volt.accent else Volt.surface)
                                    .let { if (valid) it.pressScale { saveEdit() } else it },
                                contentAlignment = Alignment.Center,
                            ) { Text("Guardar cambios", color = if (valid) Color.Black else Volt.textDim, fontWeight = FontWeight.Bold) }
                            Box(
                                Modifier.fillMaxWidth().height(42.dp).clip(RoundedCornerShape(14.dp)).background(Volt.danger.copy(alpha = 0.14f))
                                    .pressScale { deleteEdit() },
                                contentAlignment = Alignment.Center,
                            ) { Text("Eliminar serie", color = Volt.danger, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    when (type) {
                        T_ISOMETRIC, T_CARDIO -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (type == T_ISOMETRIC) {
                                Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Volt.surface2).padding(14.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        FieldLabel("Cronómetro en vivo", Modifier.weight(1f))
                                        Text(formatDuration(elapsed), color = if (running) Volt.accent else Volt.text, fontSize = 30.sp, fontWeight = FontWeight.Black)
                                    }
                                    Spacer(Modifier.height(10.dp))
                                    if (running) {
                                        Row(
                                            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(Volt.danger.copy(alpha = 0.18f))
                                                .border(1.dp, Volt.danger.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                                                .pressScale { running = false; durText = elapsed.toString(); haptics.medium() },
                                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                                        ) {
                                            Icon(Icons.Filled.Stop, null, tint = Volt.danger, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(8.dp))
                                            Text("DETENER — ${formatDuration(elapsed)}", color = Volt.danger, fontWeight = FontWeight.Bold)
                                        }
                                    } else {
                                        Row(
                                            Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(14.dp)).background(Volt.accent.copy(alpha = 0.12f))
                                                .border(1.dp, Volt.accent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                                .pressScale { elapsed = 0; running = true; haptics.light() },
                                            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                                        ) {
                                            Icon(Icons.Filled.Timer, null, tint = Volt.accent, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(8.dp))
                                            Text("Iniciar serie en vivo", color = Volt.accent, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                val step = if (type == T_CARDIO) 1 else 5
                                StepperField(
                                    if (type == T_CARDIO) "Duración (min)" else "Duración (seg)", durText, { durText = it },
                                    { durText = (durText.toD().toInt() - step).coerceAtLeast(0).let { if (it == 0) "" else it.toString() } },
                                    { durText = (durText.toD().toInt() + step).toString() }, Modifier.weight(1.2f),
                                )
                                StepperField(
                                    "Series", setsN.toString(), { setsN = it.toIntOrNull()?.coerceIn(1, 10) ?: 1 },
                                    { setsN = (setsN - 1).coerceAtLeast(1) }, { setsN = (setsN + 1).coerceAtMost(10) }, Modifier.weight(1f),
                                )
                            }
                            if (type == T_ISOMETRIC) {
                                FieldLabel("RPE (esfuerzo 1-10)")
                                ScaleSelector((1..5).toList(), rpe, { rpe = it })
                                ScaleSelector((6..10).toList(), rpe, { rpe = it })
                            }
                        }
                        T_BODYWEIGHT -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (bodyWeight > 0) {
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Volt.surface2).padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Text("🤸", fontSize = 24.sp); Spacer(Modifier.width(12.dp))
                                    Column {
                                        FieldLabel("Peso corporal de referencia")
                                        Text("${fmtNum(bodyWeight)} kg", color = Volt.text, fontWeight = FontWeight.Black, fontSize = 16.sp)
                                    }
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                StepperField(
                                    "Reps", repsText, { repsText = it },
                                    { repsText = (repsText.toD().toInt() - 1).coerceAtLeast(0).let { if (it == 0) "" else it.toString() } },
                                    { repsText = (repsText.toD().toInt() + 1).toString() }, Modifier.weight(1f),
                                )
                                StepperField(
                                    "Series", setsN.toString(), { setsN = it.toIntOrNull()?.coerceIn(1, 10) ?: 1 },
                                    { setsN = (setsN - 1).coerceAtLeast(1) }, { setsN = (setsN + 1).coerceAtMost(10) }, Modifier.weight(1f),
                                )
                            }
                            FieldLabel("RIR (reps en reserva)")
                            ScaleSelector((0..4).toList(), rir, { rir = it })
                        }
                        else -> Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            WeightCalculator(
                                exerciseId = ex.id, exerciseName = ex.name, targetWeight = ex.weight, userBodyweight = bodyWeight,
                                weightIsEmpty = weightText.isEmpty(), onWeightChange = { weightText = fmtNum(it) },
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FieldLabel("Peso (kg)"); NumField(weightText, { weightText = it }, Modifier.fillMaxWidth(), decimal = true)
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FieldLabel("Reps"); NumField(repsText, { repsText = it }, Modifier.fillMaxWidth())
                                }
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    FieldLabel("Series"); NumField(setsN.toString(), { setsN = it.toIntOrNull()?.coerceIn(1, 10) ?: 1 }, Modifier.fillMaxWidth())
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                QuickStep("-2.5", "+2.5", Modifier.weight(1f), { weightText = fmtNum(((weightText.toD() - 2.5) * 10).roundToLong() / 10.0).let { if (weightText.toD() - 2.5 <= 0) "" else it } }, { weightText = fmtNum(((weightText.toD() + 2.5) * 10).roundToLong() / 10.0) })
                                QuickStep("-1", "+1", Modifier.weight(1f), { repsText = (repsText.toD().toInt() - 1).coerceAtLeast(0).let { if (it == 0) "" else it.toString() } }, { repsText = (repsText.toD().toInt() + 1).toString() })
                                QuickStep("-1", "+1", Modifier.weight(1f), { setsN = (setsN - 1).coerceAtLeast(1) }, { setsN = (setsN + 1).coerceAtMost(10) })
                            }
                            val w = weightText.toD(); val r = repsText.toD().toInt()
                            AnimatedVisibility(w > 0 && r > 1, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Volt.surface2).padding(10.dp), horizontalArrangement = Arrangement.Center) {
                                    Text("1RM estimado (Epley): ", color = Volt.textDim, fontSize = 12.sp)
                                    Text("${(w * (1 + r / 30.0)).roundToInt()} kg", color = Volt.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                            FieldLabel("RIR (reps en reserva)")
                            ScaleSelector((0..4).toList(), rir, { rir = it })
                        }
                    }
                }

                // Historial
                Accordion(
                    if (showHistory) "Ocultar historial" else "Ver historial de progresiones", Icons.Filled.History, showHistory,
                    { showHistory = !showHistory }, trailing = if (history.isNotEmpty()) "(${history.size} sesiones)" else null,
                ) {
                    if (history.isEmpty()) {
                        Text("Sin registros anteriores en este ejercicio.", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(16.dp))
                    } else {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Volt.surface).padding(14.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                if (isDuration) {
                                    PrBox("⏱ PR TIEMPO", formatDuration(history.maxOf { it.maxDuration }), Modifier.weight(1f))
                                    PrBox("📊 SESIONES", history.size.toString(), Modifier.weight(1f))
                                } else if (type == T_BODYWEIGHT) {
                                    PrBox("🔁 PR REPS", history.maxOf { it.maxReps }.toString(), Modifier.weight(1f))
                                    PrBox("📊 SESIONES", history.size.toString(), Modifier.weight(1f))
                                } else {
                                    PrBox("🥇 PR PESO", "${fmtNum(history.maxOf { it.maxWeight })} kg", Modifier.weight(1f))
                                    PrBox("🔁 PR REPS", history.maxOf { it.maxReps }.toString(), Modifier.weight(1f))
                                }
                            }
                            FieldLabel(if (isDuration) "Progresión de tiempo" else if (type == T_BODYWEIGHT) "Progresión de repeticiones" else "Progresión de peso")
                            ProgressLineChart(
                                history.map { it.date },
                                history.map { s -> (if (isDuration) s.maxDuration.toDouble() else if (type == T_BODYWEIGHT) s.maxReps.toDouble() else s.maxWeight).toFloat() },
                                if (isDuration) "s" else if (type == T_BODYWEIGHT) "" else " kg",
                            )
                            FieldLabel("Últimas sesiones")
                            history.asReversed().take(8).forEach { s ->
                                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.03f)).padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(s.date, color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.weight(1.3f))
                                    Text(
                                        if (isDuration) formatDuration(s.maxDuration) else if (type == T_BODYWEIGHT) "${s.maxReps} reps" else "${fmtNum(s.maxWeight)} kg",
                                        color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 12.sp, modifier = Modifier.weight(1f),
                                    )
                                    Text("${s.sets} sets", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.weight(0.8f))
                                    if (!isDuration && type == T_WEIGHTED) Text("Vol: ${s.totalVolume.roundToInt()}", color = Volt.textDim, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }

        // ── Barra de acciones ──
        Row(
            Modifier.fillMaxWidth().background(Volt.bg.copy(alpha = 0.96f)).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (onPrev != null) RoundBtn(Icons.AutoMirrored.Filled.KeyboardArrowLeft, "Ejercicio anterior", onPrev, size = 54.dp)
            val enabled = canSave && !locked
            Box(
                Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(if (enabled) Volt.accent else Volt.surface2)
                    .let { if (enabled) it.pressScale { save() } else it },
                contentAlignment = Alignment.Center,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Bolt, null, tint = if (enabled) Color.Black else Volt.textDim, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(if (setsN > 1) "Guardar $setsN series" else "Guardar serie", color = if (enabled) Color.Black else Volt.textDim, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            if (onNext != null) {
                val done = doneSets >= targetSets
                Box(
                    Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(if (done) Volt.accent else Volt.surface2).pressScale(onNext),
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, "Siguiente ejercicio", tint = if (done) Color.Black else Volt.text) }
            }
        }
    }
}

@Composable
private fun PrBox(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(14.dp)).background(Volt.surface2).padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        FieldLabel(label)
        Text(value, color = Amber, fontWeight = FontWeight.Black, fontSize = 17.sp)
    }
}

@Composable
private fun QuickStep(minus: String, plus: String, modifier: Modifier, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf(minus to onMinus, plus to onPlus).forEach { (t, f) ->
            Box(
                Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(10.dp)).background(Volt.surface2).clickable(onClick = f),
                contentAlignment = Alignment.Center,
            ) { Text(t, color = Volt.textDim, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
        }
    }
}
