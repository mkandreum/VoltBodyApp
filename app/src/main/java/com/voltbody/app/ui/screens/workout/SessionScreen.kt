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
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.Repository
import com.voltbody.app.health.BleHeartRateClient
import com.voltbody.app.ui.BentoCard
import com.voltbody.app.ui.GradientText
import com.voltbody.app.ui.MeshBackground
import com.voltbody.app.ui.PoseCoachScreen
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.util.computeSmartStreak
import com.voltbody.app.util.logDayKey
import com.voltbody.app.util.rememberHaptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate

private fun blePermissions(): Array<String> =
    if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

/** Acumulador de pulso fuera del estado de Compose: no provoca recomposiciones. */
private class HrStats {
    var sum = 0L; var count = 0; var max = 0
    fun add(b: Int) { sum += b; count++; if (b > max) max = b }
}

/** Reloj de la sesión: el estado del tiempo es local, solo este texto se recompone cada segundo. */
@Composable
private fun SessionClock(startMs: Long, trailing: String) {
    var sec by remember { mutableIntStateOf(0) }
    LaunchedEffect(startMs) { while (true) { sec = ((System.currentTimeMillis() - startMs) / 1000).toInt(); delay(1000) } }
    Text("${formatDuration(sec)} · $trailing", color = Volt.textDim, style = VoltType.body, maxLines = 1)
}

/** Segmentos tipo "stories": tocar cualquiera salta a ese ejercicio. Relleno = serie(s) completada(s) o ejercicio actual. */
@Composable
private fun SessionSegments(done: List<Boolean>, current: Int, onSelect: (Int) -> Unit) {
    val brush = Volt.gradient
    Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        done.forEachIndexed { i, d ->
            Box(Modifier.weight(1f).height(18.dp).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { onSelect(i) }, contentAlignment = Alignment.Center) {
                Box(
                    Modifier.fillMaxWidth().height(if (i == current) 6.dp else 4.dp).clip(CircleShape).drawBehind {
                        drawRect(Color.White.copy(alpha = 0.14f))
                        if (d || i == current) drawRect(brush, alpha = if (i == current) 1f else 0.55f, size = Size(size.width, size.height))
                    },
                )
            }
        }
    }
}

/**
 * Sesión activa a pantalla completa: una pantalla por ejercicio, descanso circular, confeti en récords, pulso BLE en vivo,
 * PoseCoach y resumen final compartible. El tiempo (sesión, descanso, cronómetro) vive en estado local de cada composable.
 */
@Composable
fun SessionDialog(repo: Repository, spec: SessionSpec, onClose: () -> Unit) {
    val ctx = LocalContext.current
    val haptics = rememberHaptics()
    val logs by remember(repo) { repo.state.map { it.logs }.distinctUntilChanged() }.collectAsStateWithLifecycle(repo.state.value.logs)
    val bodyWeight = remember { repo.state.value.profile.weight ?: 0.0 }
    val age = remember { repo.state.value.profile.age }
    val dateKey = remember { LocalDate.now().toString() }
    val startMs = remember { System.currentTimeMillis() }
    var idx by remember { mutableIntStateOf(spec.startIndex.coerceIn(0, (spec.exercises.size - 1).coerceAtLeast(0))) }

    // Descanso (el conteo vive dentro de RestOverlay)
    var restActive by remember { mutableStateOf(false) }
    var restTotal by remember { mutableIntStateOf(90) }
    var restKey by remember { mutableIntStateOf(0) }
    // Efectos
    var smallBurst by remember { mutableIntStateOf(0) }
    var bigBurst by remember { mutableIntStateOf(0) }
    var banner by remember { mutableStateOf<Banner?>(null) }
    var showPose by remember { mutableStateOf(false) }
    var showSummary by remember { mutableStateOf(false) }
    var showCompletion by remember { mutableStateOf(false) }

    LaunchedEffect(banner?.id) { if (banner != null) { delay(3400); banner = null } }
    val pushBanner: (Banner) -> Unit = remember { { b -> banner = b; if (b.kind == "achievement") smallBurst++ } }

    // ── Progreso de la sesión (logs de hoy) ──
    val setsByEx = remember(logs, dateKey) { logs.filter { logDayKey(it.date) == dateKey }.groupingBy { it.exerciseId }.eachCount() }
    val plannedSets = remember(spec) { spec.exercises.sumOf { it.targetSets() } }
    val completedSets = spec.exercises.sumOf { minOf(setsByEx[it.id] ?: 0, it.targetSets()) }
    val doneFlags = spec.exercises.map { (setsByEx[it.id] ?: 0) >= it.targetSets() }
    val allDone = spec.exercises.isNotEmpty() && doneFlags.all { it }
    var prevAllDone by remember { mutableStateOf(allDone) }
    LaunchedEffect(allDone) {
        if (allDone && !prevAllDone) { bigBurst++; haptics.heavy(); showCompletion = true }
        prevAllDone = allDone
    }

    // ── Pulso cardíaco BLE ──
    val ble = remember { BleHeartRateClient(ctx.applicationContext) }
    DisposableEffect(Unit) { onDispose { runCatching { ble.stop() } } }
    val hr = remember { HrStats() }
    LaunchedEffect(ble) { ble.bpm.collect { b -> if (b != null && b in 30..230) hr.add(b) } }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.all { it }) ble.start() else pushBanner(Banner("Permiso necesario", "Activa Bluetooth/Dispositivos cercanos para el pulsómetro.", "error"))
    }
    fun toggleHr() {
        val s = ble.state.value
        if (s == "disconnected" || s == "error") {
            val missing = blePermissions().filter { ContextCompat.checkSelfPermission(ctx, it) != PackageManager.PERMISSION_GRANTED }
            if (missing.isEmpty()) ble.start() else permLauncher.launch(missing.toTypedArray())
        } else ble.stop()
        haptics.light()
    }

    val currentEx = spec.exercises.getOrNull(idx)

    fun summaryData(): WorkoutSummaryData {
        val s = repo.state.value
        val streak = computeSmartStreak(s.logs, s.routine)
        val ids = spec.exercises.map { it.id }.toSet()
        val todaySpec = s.logs.filter { it.exerciseId in ids && logDayKey(it.date) == dateKey }
        val totalXP = s.logs.size * XP_PER_LOG + streak * XP_PER_STREAK_DAY
        val elapsedSec = ((System.currentTimeMillis() - startMs) / 1000).toInt()
        return WorkoutSummaryData(
            focus = spec.title.ifBlank { "Entrenamiento" }, exerciseCount = spec.exercises.size, completedSets = completedSets, plannedSets = plannedSets,
            streak = streak, level = totalXP / XP_PER_LEVEL + 1, totalXP = totalXP, xpGained = todaySpec.size * XP_PER_LOG,
            userName = s.profile.name ?: s.user?.name,
            exercises = spec.exercises.map { Triple(it.name, it.sets, it.reps) },
            durationMin = (elapsedSec / 60).coerceAtLeast(1),
            volumeKg = todaySpec.sumOf { it.weight * it.reps }.toInt(),
            avgBpm = if (hr.count > 0) (hr.sum / hr.count).toInt() else null, maxBpm = if (hr.count > 0) hr.max else null,
        )
    }

    Dialog(
        onDismissRequest = { if (!showPose && !showSummary) { if (restActive) restActive = false else onClose() } },
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false, dismissOnClickOutside = false),
    ) {
    Box(Modifier.fillMaxSize().background(Volt.bg)) {
        MeshBackground(Modifier.fillMaxSize(), colorA = Volt.accent, colorB = Volt.accent2, intensity = 0.45f)
        Column(Modifier.fillMaxSize().statusBarsPadding().imePadding()) {
                // ── Barra superior ──
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(48.dp).clip(CircleShape).background(Volt.surface).pressScale(onClose), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, "Cerrar sesión", tint = Volt.text)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(spec.title.ifBlank { "Entrenamiento" }, color = Volt.text, style = VoltType.title, maxLines = 1)
                        SessionClock(startMs, "$completedSets/$plannedSets series")
                    }
                    HeartChip(ble = ble, age = age, onClick = { toggleHr() })
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier.height(46.dp).clip(CircleShape).background(Volt.gradient)
                            .pressScale { if (completedSets == 0) onClose() else showSummary = true }.padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Terminar", color = Volt.onAccent, style = VoltType.title.copy(fontSize = 14.sp)) }
                }
                SessionSegments(doneFlags, idx) { if (it != idx) { idx = it; haptics.selection() } }
                Spacer(Modifier.height(6.dp))

                // ── Ejercicio actual (slide + fade entre pasos) ──
                if (currentEx == null) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { Text("Esta sesión no tiene ejercicios.", color = Volt.textDim, style = VoltType.body) }
                } else {
                    AnimatedContent(
                        targetState = idx,
                        transitionSpec = {
                            val dir = if (targetState > initialState) 1 else -1
                            (slideInHorizontally(spring(0.85f, 380f)) { it * dir } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(260)) { -it * dir / 3 } + fadeOut(tween(150)))
                        },
                        modifier = Modifier.weight(1f).fillMaxWidth(), label = "sessionExercise",
                    ) { i ->
                        val ex = spec.exercises[i.coerceIn(0, spec.exercises.size - 1)]
                        ExercisePanel(
                            repo = repo, logs = logs, bodyWeight = bodyWeight, ex = ex, index = i, count = spec.exercises.size, dateKey = dateKey,
                            onPrev = if (i > 0) ({ idx = i - 1; haptics.selection() }) else null,
                            onNext = if (i < spec.exercises.size - 1) ({ idx = i + 1; haptics.selection() }) else null,
                            onLogged = { rest -> restTotal = rest; restKey++; restActive = true; smallBurst++ },
                            onBanner = pushBanner, onRecord = { bigBurst++; haptics.heavy() },
                            onOpenPose = { showPose = true }, modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            ConfettiBurst(smallBurst, Modifier.fillMaxSize())
            ConfettiBurst(bigBurst, Modifier.fillMaxSize(), big = true)

            // Descanso a pantalla completa
            AnimatedVisibility(restActive, enter = fadeIn(tween(300)) + scaleIn(spring(0.85f, 380f), initialScale = 0.94f), exit = fadeOut(tween(150))) {
                androidx.compose.runtime.key(restKey) {
                    val ex = currentEx
                    RestOverlay(
                        initial = restTotal,
                        title = ex?.name ?: "Descanso",
                        subtitle = if (ex != null) "Serie ${minOf(setsByEx[ex.id] ?: 0, ex.targetSets())} de ${ex.targetSets()}" else "Respira y recupera",
                        onFinished = {
                            restActive = false; haptics.heavy()
                            pushBanner(Banner("Descanso terminado", "Siguiente serie", "info"))
                        },
                        onSkip = { restActive = false; haptics.light() },
                    )
                }
            }

            // Banner superior
            AnimatedVisibility(
                banner != null, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 64.dp, start = 16.dp, end = 16.dp),
                enter = slideInVertically(spring(0.8f, 420f)) { -it * 2 } + fadeIn(), exit = slideOutVertically { -it * 2 } + fadeOut(),
            ) {
                val b = banner
                if (b != null) {
                    val gold = b.kind == "achievement"
                    val fill = when {
                        gold -> Brush.linearGradient(listOf(PrGold, Volt.accent2))
                        b.kind == "error" -> Brush.linearGradient(listOf(Volt.danger, Volt.danger))
                        b.kind == "info" -> Brush.linearGradient(listOf(Volt.surface2, Volt.surface2))
                        else -> Volt.gradient
                    }
                    val fg = if (b.kind == "info") Volt.text else Volt.onAccent
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(fill).padding(horizontal = 18.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (gold) { Icon(Icons.Filled.EmojiEvents, null, tint = fg); Spacer(Modifier.width(10.dp)) }
                        Column {
                            Text(b.title, color = fg, style = VoltType.title.copy(fontSize = 15.sp))
                            b.message?.let { Text(it, color = fg.copy(alpha = 0.8f), style = VoltType.body.copy(fontSize = 12.sp)) }
                        }
                    }
                }
            }

            // Celebración de sesión completada
            AnimatedVisibility(
                showCompletion, modifier = Modifier.align(Alignment.Center).padding(24.dp),
                enter = slideInVertically(spring(0.7f, 300f)) { -it } + fadeIn(tween(400)), exit = fadeOut(),
            ) {
                val wob = rememberInfiniteTransition(label = "trophy").animateFloat(-8f, 8f, infiniteRepeatable(tween(500), RepeatMode.Reverse), label = "wob")
                BentoCard(Modifier.fillMaxWidth(), hero = true, padding = 24.dp) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("🏆", fontSize = 60.sp, modifier = Modifier.graphicsLayer { rotationZ = wob.value })
                        Text("SESIÓN COMPLETADA", color = Volt.accent, style = VoltType.label)
                        GradientText(spec.title.ifBlank { "Entrenamiento" }, style = VoltType.h1, maxLines = 2, textAlign = TextAlign.Center)
                        SessionClockStatic(startMs, "$completedSets/$plannedSets series")
                        Spacer(Modifier.height(6.dp))
                        Box(
                            Modifier.fillMaxWidth().height(58.dp).clip(CircleShape).background(Volt.gradient).pressScale { showCompletion = false; showSummary = true },
                            contentAlignment = Alignment.Center,
                        ) { Text("Ver resumen y compartir", color = Volt.onAccent, style = VoltType.title) }
                        Text("Seguir entrenando", color = Volt.textDim, style = VoltType.body, modifier = Modifier.pressScale { showCompletion = false }.padding(10.dp))
                    }
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

/** Duración fija en el momento de mostrarse (sin ticker): para la tarjeta de celebración. */
@Composable
private fun SessionClockStatic(startMs: Long, trailing: String) {
    val sec = remember { ((System.currentTimeMillis() - startMs) / 1000).toInt() }
    Text("${formatDuration(sec)} · $trailing", color = Volt.textDim, style = VoltType.body)
}

/** Pulso en vivo: toca para conectar/desconectar la banda BLE; late al ritmo real y colorea por zona. Recolecta el BLE por sí mismo. */
@Composable
fun HeartChip(ble: BleHeartRateClient, age: Int?, onClick: () -> Unit) {
    val bpm by ble.bpm.collectAsState()
    val state by ble.state.collectAsState()
    val maxHr = 220 - (age ?: 30)
    val pct = if (bpm != null) bpm!!.toFloat() / maxHr else 0f
    val (zoneColor, zoneName) = when {
        bpm == null -> Volt.textDim to ""
        pct < 0.6f -> Volt.water to "Suave"
        pct < 0.7f -> Color(0xFF5BD0C4) to "Quema grasa"
        pct < 0.8f -> PrGold to "Aeróbica"
        pct < 0.9f -> Volt.accent2 to "Intensa"
        else -> Volt.danger to "Máximo"
    }
    val beatMs = bpm?.takeIf { it > 0 }?.let { (30000 / it).coerceIn(150, 750) } ?: 600
    val live = bpm != null
    val pulse = rememberInfiniteTransition(label = "hr").animateFloat(
        1f, if (live) 1.35f else 1f, infiniteRepeatable(tween(beatMs), RepeatMode.Reverse), label = "hrPulse",
    )
    Row(
        Modifier.height(46.dp).clip(CircleShape).background(Volt.surface)
            .drawBehind { if (live) drawRect(zoneColor.copy(alpha = 0.14f)) }
            .clickable(onClick = onClick).padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Filled.Favorite, "Pulsómetro", tint = if (live) zoneColor else Volt.textDim,
            modifier = Modifier.size(18.dp).graphicsLayer { scaleX = pulse.value; scaleY = pulse.value },
        )
        Spacer(Modifier.width(8.dp))
        when {
            bpm != null && state == "connected" -> Column {
                Text("$bpm", color = Volt.text, style = VoltType.numberS.copy(fontSize = 16.sp, lineHeight = 18.sp))
                if (zoneName.isNotEmpty()) Text(zoneName, color = zoneColor, fontSize = 12.sp, lineHeight = 10.sp)
            }
            state == "scanning" -> Text("Buscando…", color = Volt.textDim, fontSize = 12.sp)
            state == "connecting" -> Text("Conectando…", color = Volt.textDim, fontSize = 12.sp)
            state == "error" -> Text("Error", color = Volt.danger, fontSize = 12.sp)
            state == "connected" -> Text("--", color = Volt.text, style = VoltType.numberS)
            else -> Text("Pulso", color = Volt.textDim, fontSize = 12.sp)
        }
    }
}
