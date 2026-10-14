package com.voltbody.app.ui.screens.workout

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import com.voltbody.app.data.Repository
import com.voltbody.app.health.BleHeartRateClient
import com.voltbody.app.ui.PoseCoachScreen
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.computeSmartStreak
import com.voltbody.app.util.logDayKey
import com.voltbody.app.util.rememberHaptics
import kotlinx.coroutines.delay
import java.time.LocalDate

private fun blePermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

/**
 * Sesión activa a pantalla completa: pasos por ejercicio con animación, series/reps/peso/RIR/RPE, descanso con anillo,
 * confeti, pulso cardíaco BLE en vivo, PoseCoach y resumen final compartible.
 */
@Composable
fun SessionDialog(repo: Repository, spec: SessionSpec, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val haptics = rememberHaptics()
    val st by repo.state.collectAsState()
    val dateKey = remember { LocalDate.now().toString() }
    val startMs = remember { System.currentTimeMillis() }
    var idx by remember { mutableIntStateOf(spec.startIndex.coerceIn(0, (spec.exercises.size - 1).coerceAtLeast(0))) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { while (true) { delay(1000); elapsedSec = ((System.currentTimeMillis() - startMs) / 1000).toInt() } }

    // Descanso
    var restLeft by remember { mutableIntStateOf(0) }
    var restTotal by remember { mutableIntStateOf(90) }
    var restRun by remember { mutableIntStateOf(0) }
    // Efectos
    var smallBurst by remember { mutableIntStateOf(0) }
    var bigBurst by remember { mutableIntStateOf(0) }
    var banner by remember { mutableStateOf<Banner?>(null) }
    var showPose by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }
    var showCompletion by remember { mutableStateOf(false) }

    LaunchedEffect(banner?.id) { if (banner != null) { delay(3400); banner = null } }
    val pushBanner: (Banner) -> Unit = { b -> banner = b; if (b.kind == "achievement") smallBurst++ }

    LaunchedEffect(restRun) {
        if (restRun == 0) return@LaunchedEffect
        while (restLeft > 0) {
            delay(1000)
            if (restLeft > 0) {
                restLeft -= 1
                if (restLeft == 0) { haptics.heavy(); pushBanner(Banner("Descanso terminado", "¡Siguiente serie!", "info")) }
            }
        }
    }

    // ── Progreso de la sesión (logs de hoy) ──
    val setsByEx = remember(st.logs, dateKey) { st.logs.filter { logDayKey(it.date) == dateKey }.groupingBy { it.exerciseId }.eachCount() }
    val plannedSets = spec.exercises.sumOf { it.targetSets() }
    val completedSets = spec.exercises.sumOf { minOf(setsByEx[it.id] ?: 0, it.targetSets()) }
    val allDone = spec.exercises.isNotEmpty() && spec.exercises.all { (setsByEx[it.id] ?: 0) >= it.targetSets() }
    var prevAllDone by remember { mutableStateOf(allDone) }
    LaunchedEffect(allDone) {
        if (allDone && !prevAllDone) { bigBurst++; haptics.heavy(); showCompletion = true }
        prevAllDone = allDone
    }

    // ── Pulso cardíaco BLE ──
    val ble = remember { BleHeartRateClient(ctx.applicationContext) }
    DisposableEffect(Unit) { onDispose { runCatching { ble.stop() } } }
    val bpm by ble.bpm.collectAsState()
    val bleState by ble.state.collectAsState()
    var hrSum by remember { mutableLongStateOf(0L) }
    var hrCount by remember { mutableIntStateOf(0) }
    var hrMax by remember { mutableIntStateOf(0) }
    LaunchedEffect(bpm) { bpm?.takeIf { it in 30..230 }?.let { hrSum += it; hrCount++; if (it > hrMax) hrMax = it } }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.all { it }) ble.start() else pushBanner(Banner("Permiso necesario", "Activa Bluetooth/Dispositivos cercanos para el pulsómetro.", "error"))
    }
    fun toggleHr() {
        if (bleState == "disconnected" || bleState == "error") {
            val missing = blePermissions().filter { ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED }
            if (missing.isEmpty()) ble.start() else permLauncher.launch(missing.toTypedArray())
        } else ble.stop()
        haptics.light()
    }

    val currentEx = spec.exercises.getOrNull(idx)

    fun summaryData(): WorkoutSummaryData {
        val streak = computeSmartStreak(st.logs, st.routine)
        val ids = spec.exercises.map { it.id }.toSet()
        val todaySpec = st.logs.filter { it.exerciseId in ids && logDayKey(it.date) == dateKey }
        val totalXP = st.logs.size * XP_PER_LOG + streak * XP_PER_STREAK_DAY
        return WorkoutSummaryData(
            focus = spec.title.ifBlank { "Entrenamiento" }, exerciseCount = spec.exercises.size, completedSets = completedSets, plannedSets = plannedSets,
            streak = streak, level = totalXP / XP_PER_LEVEL + 1, totalXP = totalXP, xpGained = todaySpec.size * XP_PER_LOG,
            userName = st.profile.name ?: st.user?.name,
            exercises = spec.exercises.map { Triple(it.name, it.sets, it.reps) },
            durationMin = (elapsedSec / 60).coerceAtLeast(1),
            volumeKg = todaySpec.sumOf { it.weight * it.reps }.toInt(),
            avgBpm = if (hrCount > 0) (hrSum / hrCount).toInt() else null, maxBpm = if (hrCount > 0) hrMax else null,
        )
    }

    Dialog(
        onDismissRequest = { if (!showPose && !showSummary) onClose() },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
        Box(Modifier.fillMaxSize().background(Volt.bg).systemBarsPadding().imePadding()) {
            Column(Modifier.fillMaxSize()) {
                // ── Barra superior ──
                Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(42.dp).clip(CircleShape).background(Volt.surface).pressScale(onClose), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, "Cerrar sesión", tint = Volt.text)
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(spec.title.ifBlank { "Entrenamiento" }, color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp, maxLines = 1)
                        Text("${formatDuration(elapsedSec)} · $completedSets/$plannedSets series", color = Volt.textDim, fontSize = 12.sp)
                    }
                    HeartChip(bpm = bpm, state = bleState, age = st.profile.age, onClick = { toggleHr() })
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.clip(RoundedCornerShape(50)).background(Volt.accent)
                            .pressScale { if (completedSets == 0) onClose() else showSummary = true }.padding(horizontal = 14.dp, vertical = 10.dp),
                    ) { Text("Terminar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
                AnimatedProgressBar(if (plannedSets > 0) completedSets.toFloat() / plannedSets else 0f, Modifier.fillMaxWidth().padding(horizontal = 16.dp), height = 6.dp)

                // ── Pasos ──
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    spec.exercises.forEachIndexed { i, e ->
                        val done = (setsByEx[e.id] ?: 0) >= e.targetSets()
                        val cur = i == idx
                        Box(
                            Modifier.size(34.dp).scale(if (cur) 1.12f else 1f).clip(CircleShape)
                                .background(if (cur) Volt.accent else if (done) Volt.accent.copy(alpha = 0.25f) else Volt.surface2)
                                .border(1.dp, if (done && !cur) Volt.accent else Color.Transparent, CircleShape)
                                .clickable { if (i != idx) { idx = i; haptics.selection() } },
                            contentAlignment = Alignment.Center,
                        ) {
                            if (done && !cur) Icon(Icons.Filled.Check, null, tint = Volt.accent, modifier = Modifier.size(16.dp))
                            else Text("${i + 1}", color = if (cur) Color.Black else Volt.textDim, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }

                // ── Ejercicio actual (slide + fade entre pasos) ──
                if (currentEx == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Esta sesión no tiene ejercicios.", color = Volt.textDim) }
                } else {
                    AnimatedContent(
                        targetState = idx,
                        transitionSpec = {
                            val dir = if (targetState > initialState) 1 else -1
                            (slideInHorizontally(tween(320)) { it * dir } + fadeIn(tween(320))) togetherWith
                                (slideOutHorizontally(tween(320)) { -it * dir } + fadeOut(tween(200)))
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(), label = "sessionExercise",
                    ) { i ->
                        val ex = spec.exercises[i.coerceIn(0, spec.exercises.size - 1)]
                        ExercisePanel(
                            repo = repo, st = st, ex = ex, index = i, count = spec.exercises.size, dateKey = dateKey,
                            onPrev = if (i > 0) ({ idx = i - 1; haptics.selection() }) else null,
                            onNext = if (i < spec.exercises.size - 1) ({ idx = i + 1; haptics.selection() }) else null,
                            onLogged = { rest -> restTotal = rest; restLeft = rest; restRun++; smallBurst++ },
                            onBanner = pushBanner, onOpenPose = { showPose = true }, modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            ConfettiBurst(smallBurst, Modifier.fillMaxSize())
            ConfettiBurst(bigBurst, Modifier.fillMaxSize(), big = true)

            // Descanso
            AnimatedVisibility(
                restLeft > 0, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 86.dp, start = 12.dp, end = 12.dp),
                enter = slideInVertically { it } + fadeIn(), exit = slideOutVertically { it } + fadeOut(),
            ) {
                RestTimerCard(
                    restLeft, restTotal,
                    onAdjust = { d -> restLeft = (restLeft + d).coerceAtLeast(1); if (restLeft > restTotal) restTotal = restLeft },
                    onSkip = { restLeft = 0; haptics.light() },
                )
            }

            // Banner superior
            AnimatedVisibility(
                banner != null, modifier = Modifier.align(Alignment.TopCenter).padding(top = 64.dp, start = 16.dp, end = 16.dp),
                enter = slideInVertically { -it * 2 } + fadeIn(), exit = slideOutVertically { -it * 2 } + fadeOut(),
            ) {
                val b = banner
                if (b != null) {
                    val gold = b.kind == "achievement"
                    val bg = when { gold -> Color(0xFFFACC15); b.kind == "error" -> Volt.danger; b.kind == "info" -> Volt.surface2; else -> Volt.accent }
                    val fg = if (b.kind == "info") Volt.text else Color.Black
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(bg).padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (gold) { Icon(Icons.Filled.EmojiEvents, null, tint = fg); Spacer(Modifier.width(10.dp)) }
                        Column {
                            Text(b.title, color = fg, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            b.message?.let { Text(it, color = fg.copy(alpha = 0.8f), fontSize = 12.sp) }
                        }
                    }
                }
            }

            // Celebración de sesión completada
            AnimatedVisibility(
                showCompletion, modifier = Modifier.align(Alignment.Center).padding(24.dp),
                enter = slideInVertically { -it } + fadeIn(tween(400)), exit = fadeOut(),
            ) {
                val wob by rememberInfiniteTransition(label = "trophy").animateFloat(-8f, 8f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "wob")
                Column(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.Black.copy(alpha = 0.92f))
                        .border(1.dp, Volt.accent.copy(alpha = 0.55f), RoundedCornerShape(28.dp)).padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text("🏆", fontSize = 56.sp, modifier = Modifier.rotate(wob))
                    Text("¡SESIÓN COMPLETADA!", color = Volt.accent, fontWeight = FontWeight.Black, fontSize = 13.sp, letterSpacing = 3.sp)
                    Text(spec.title.ifBlank { "Entrenamiento" }, color = Volt.text, fontWeight = FontWeight.Black, fontSize = 22.sp)
                    Text("$completedSets/$plannedSets series · ${formatDuration(elapsedSec)}", color = Volt.textDim, fontSize = 13.sp)
                    Spacer(Modifier.height(4.dp))
                    Box(
                        Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(25.dp)).background(Volt.accent)
                            .pressScale { showCompletion = false; showSummary = true },
                        contentAlignment = Alignment.Center,
                    ) { Text("Ver resumen y compartir", color = Color.Black, fontWeight = FontWeight.Bold) }
                    Text("Seguir entrenando", color = Volt.textDim, modifier = Modifier.pressScale { showCompletion = false }.padding(8.dp))
                }
            }
        }
    }

    if (showPose && currentEx != null) {
        Dialog(
            onDismissRequest = { showPose = false },
            properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
        ) { Box(Modifier.fillMaxSize().background(Color.Black)) { PoseCoachScreen(exerciseName = currentEx.name, onClose = { showPose = false }) } }
    }

    if (showSummary) {
        SummaryDialog(
            data = remember(showSummary) { summaryData() },
            onDismiss = { showSummary = false; onClose() },
            onToast = { t, m -> repo.toast("error", t, m) },
        )
    }
}

/** Chip de pulso: toca para conectar/desconectar la banda BLE; late al ritmo real y colorea por zona. */
@Composable
fun HeartChip(bpm: Int?, state: String, age: Int?, onClick: () -> Unit) {
    val maxHr = 220 - (age ?: 30)
    val pct = if (bpm != null) bpm.toFloat() / maxHr else 0f
    val (zoneColor, zoneName) = when {
        bpm == null -> Volt.textDim to ""
        pct < 0.6f -> Volt.water to "Suave"
        pct < 0.7f -> Color(0xFF34D399) to "Quema grasa"
        pct < 0.8f -> Color(0xFFFACC15) to "Aeróbica"
        pct < 0.9f -> Volt.orange to "Intensa"
        else -> Volt.danger to "Máximo"
    }
    val beatMs = if (bpm != null && bpm > 0) (30000 / bpm).coerceIn(150, 750) else 600
    val pulse by rememberInfiniteTransition(label = "hr").animateFloat(
        1f, if (bpm != null) 1.35f else 1f, infiniteRepeatable(tween(beatMs), RepeatMode.Reverse), label = "hrPulse",
    )
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Volt.surface).border(1.dp, if (bpm != null) zoneColor.copy(alpha = 0.6f) else Volt.border, RoundedCornerShape(50))
            .clickable(onClick = onClick).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Favorite, "Pulsómetro", tint = if (bpm != null) zoneColor else Volt.textDim, modifier = Modifier.size(16.dp).scale(pulse))
        Spacer(Modifier.width(6.dp))
        when {
            bpm != null && state == "connected" -> Column {
                Text("$bpm", color = Volt.text, fontWeight = FontWeight.Black, fontSize = 14.sp)
                if (zoneName.isNotEmpty()) Text(zoneName, color = zoneColor, fontSize = 9.sp)
            }
            state == "scanning" -> Text("Buscando…", color = Volt.textDim, fontSize = 11.sp)
            state == "connecting" -> Text("Conectando…", color = Volt.textDim, fontSize = 11.sp)
            state == "error" -> Text("Error", color = Volt.danger, fontSize = 11.sp)
            state == "connected" -> Text("--", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            else -> Text("Pulso", color = Volt.textDim, fontSize = 11.sp)
        }
    }
}
