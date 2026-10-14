package com.voltbody.app.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.health.connect.client.HealthConnectClient
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.DailyMetric
import com.voltbody.app.data.RecoveryLog
import com.voltbody.app.data.Repository
import com.voltbody.app.health.BleHeartRateClient
import com.voltbody.app.health.HealthSnapshot
import com.voltbody.app.health.healthManager
import com.voltbody.app.ui.BentoCard
import com.voltbody.app.ui.CountUpText
import com.voltbody.app.ui.DonutProgress
import com.voltbody.app.ui.EaseOutExpo
import com.voltbody.app.ui.GlowRing
import com.voltbody.app.ui.PrimaryButton
import com.voltbody.app.ui.RollingNumber
import com.voltbody.app.ui.Sparkline
import com.voltbody.app.ui.StatPill
import com.voltbody.app.ui.glowBackdrop
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.rememberPulse
import com.voltbody.app.ui.rememberReduceMotion
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.ui.voltFieldColors
import com.voltbody.app.util.RecoveryAdvice
import com.voltbody.app.util.saveRecoveryCheckin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/** Estado de Health Connect para la UI (pasos, kcal, sueno, HRV...). */
@Stable
class HealthState(private val ctx: Context) {
    /** loading | unavailable | needs | connected */
    var status by mutableStateOf("loading")
    var snap by mutableStateOf<HealthSnapshot?>(null)
    var requestPermissions: () -> Unit = {}
    val manager get() = ctx.healthManager

    /** Lee Health Connect y, si hay [repo], sincroniza las métricas de hoy con el servidor (PUT /data/metrics/:date). */
    suspend fun refresh(repo: Repository? = null) {
        val m = manager
        val avail = runCatching { m.availability() }.getOrDefault(-1)
        if (avail != HealthConnectClient.SDK_AVAILABLE) { status = "unavailable"; return }
        val ok = runCatching { m.hasAnyPermission() }.getOrDefault(false)
        if (!ok) { status = "needs"; return }
        VoltPrefs.setHealthEnabled(ctx, true)
        val s = runCatching { m.readToday() }.getOrNull()
        snap = s
        status = "connected"
        if (s != null && repo != null && repo.state.value.isAuthenticated) {
            val date = LocalDate.now().toString()
            // Solo valores reales (> 0): si Health Connect aun no tiene datos no se pisa lo guardado en el servidor.
            fun pos(d: Double?) = d?.takeIf { it > 0.0 }
            val dm = DailyMetric(
                date, steps = s.steps.toInt().takeIf { it > 0 }, distanceKm = pos(s.distanceKm), activeKcal = pos(s.activeKcal),
                totalKcal = pos(s.totalKcal), sleepHours = pos(s.sleepHours), hrv = pos(s.hrvMs),
                restingHr = s.restingHr?.takeIf { it > 0 }, avgHr = s.avgHr?.takeIf { it > 0 },
            )
            val old = repo.state.value.metrics.firstOrNull { it.date == date }
            val changed = (dm.steps != null && dm.steps != old?.steps) || (dm.distanceKm != null && dm.distanceKm != old?.distanceKm) ||
                (dm.activeKcal != null && dm.activeKcal != old?.activeKcal) || (dm.totalKcal != null && dm.totalKcal != old?.totalKcal) ||
                (dm.sleepHours != null && dm.sleepHours != old?.sleepHours) || (dm.hrv != null && dm.hrv != old?.hrv) ||
                (dm.restingHr != null && dm.restingHr != old?.restingHr) || (dm.avgHr != null && dm.avgHr != old?.avgHr)
            if (changed) repo.saveMetrics(date, dm)
        }
    }

    suspend fun sleepLastNight(): Double? =
        snap?.sleepHours ?: runCatching { manager.readSleepLastNight() }.getOrNull()

    /**
     * Vuelca a la cuenta el historico de Health Connect del ultimo mes ([days] dias: pasos, distancia,
     * kcal, sueno, HRV, FC en reposo y peso) para que Actividad/Calendario tengan datos de dias pasados.
     * Idempotente: como mucho una vez cada [SYNC_EVERY_MS] por proceso (aunque haya varias pantallas
     * con su propio HealthState), solo sube los dias que cambiaron y, si la lectura falla, se reintenta.
     */
    suspend fun syncLastMonth(repo: Repository?, days: Int = HISTORY_DAYS): Int {
        if (repo == null) return -1
        val now = System.currentTimeMillis()
        if (now - lastMonthSync < SYNC_EVERY_MS || syncing) return -1
        val m = manager
        if (runCatching { m.availability() }.getOrDefault(-1) != HealthConnectClient.SDK_AVAILABLE) return -1
        if (!runCatching { m.hasAnyPermission() }.getOrDefault(false)) return -1
        if (!repo.state.value.isAuthenticated) return -1
        syncing = true
        try {
            // Espera a tener los datos del servidor para comparar y no resubir dias ya guardados.
            withTimeoutOrNull(15_000) { repo.state.first { it.profileLoaded } }
            val today = LocalDate.now()
            val from = today.minusDays((days - 1).toLong())
            val daily = runCatching { m.readDaily(from, today) }.getOrNull() ?: return -1
            val weights = runCatching { m.readDailyWeights(from, today) }.getOrDefault(emptyMap())
            lastMonthSync = now
            var saved = 0
            for ((date, dm) in daily.toSortedMap()) {
                if (date == today.toString()) continue // hoy ya lo maneja refresh()
                if (dm == DailyMetric(date)) continue // dia sin datos utiles
                val old = repo.state.value.metrics.firstOrNull { it.date == date }
                if (!metricsDiffer(dm, old)) continue
                if (runCatching { repo.saveMetrics(date, dm) }.isSuccess) saved++
            }
            // Peso: solo dias sin registro en la app (no pisa lo que el usuario apunto a mano).
            val known = repo.state.value.weightLogs.map { it.date }.toSet()
            var savedWeights = 0
            for ((date, kg) in weights.toSortedMap()) {
                if (date in known) continue
                if (runCatching { repo.addWeight(date, kg) }.getOrDefault(false)) savedWeights++
            }
            if (saved > 0 || savedWeights > 0) {
                val parts = listOfNotNull(
                    saved.takeIf { it > 0 }?.let { "$it días de actividad" },
                    savedWeights.takeIf { it > 0 }?.let { "$it pesos" },
                )
                repo.toast("success", "Historial de salud importado", parts.joinToString(" y ") + " del último mes")
            }
            return saved + savedWeights
        } finally {
            syncing = false
        }
    }

    companion object {
        /** Ventana de historico que se importa (un mes). */
        const val HISTORY_DAYS = 31
        private const val SYNC_EVERY_MS = 6 * 60 * 60 * 1000L
        @Volatile private var lastMonthSync = 0L
        @Volatile private var syncing = false

        /** Fuerza el proximo volcado (p. ej. tras conceder permisos nuevos). */
        fun resetMonthSync() { lastMonthSync = 0L }
    }

    private fun metricsDiffer(m: DailyMetric, old: DailyMetric?): Boolean =
        (m.steps != null && m.steps != old?.steps) || (m.distanceKm != null && m.distanceKm != old?.distanceKm) ||
            (m.activeKcal != null && m.activeKcal != old?.activeKcal) || (m.totalKcal != null && m.totalKcal != old?.totalKcal) ||
            (m.sleepHours != null && m.sleepHours != old?.sleepHours) || (m.hrv != null && m.hrv != old?.hrv) ||
            (m.restingHr != null && m.restingHr != old?.restingHr) || (m.avgHr != null && m.avgHr != old?.avgHr)
}

@Composable
fun rememberHealthState(repo: Repository? = null): HealthState {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val hs = remember { HealthState(ctx.applicationContext) }
    val launcher = rememberLauncherForActivityResult(hs.manager.requestContract()) {
        HealthState.resetMonthSync()
        scope.launch { hs.refresh(repo); hs.syncLastMonth(repo) }
    }
    hs.requestPermissions = { runCatching { launcher.launch(hs.manager.requestedPermissions) } }
    // Solo el flag de autenticacion (no todo el AppState): evita recomponer la pantalla en cada cambio de estado.
    val authedFlow = remember(repo) { (repo?.state?.map { it.isAuthenticated } ?: flowOf(false)).distinctUntilChanged() }
    val authed by authedFlow.collectAsStateWithLifecycle(initialValue = false)
    LaunchedEffect(authed) { hs.refresh(repo); hs.syncLastMonth(repo) }
    return hs
}

/** Color semantico de recuperacion (sin verdes neon). */
fun recoveryColor(score: Int): Color = when {
    score >= 85 -> Color(0xFF2DD4BF)
    score >= 65 -> Color(0xFF4DA3FF)
    score >= 40 -> Color(0xFFFFB84D)
    else -> Color(0xFFFF4D6A)
}

@Composable
fun ConnectHealthCard(health: HealthState, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    BentoCard(modifier, padding = 20.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val pulse = rememberPulse()
            Box(
                Modifier.size(48.dp).graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }.clip(CircleShape).background(Volt.accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Favorite, null, tint = Volt.accent) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Conecta tu salud", color = Volt.text, style = VoltType.title)
                Text(
                    if (health.status == "unavailable") "Health Connect no está disponible en este dispositivo. Instálalo desde Google Play para sincronizar pasos, sueño y HRV (Samsung Health, Garmin...)."
                    else "Autoriza Health Connect para ver pasos, calorías, sueño y HRV (también los de Samsung Health).",
                    color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp, lineHeight = 17.sp),
                )
            }
        }
        if (health.status == "needs") {
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Conectar Health Connect", { haptic.performHapticFeedback(HapticFeedbackType.LongPress); health.requestPermissions() })
        }
    }
}

// ───────────────────────── Tarjetas de estadisticas ─────────────────────────

/** Tarjeta heroe: pasos del dia, progreso hacia la meta y barras semanales con burbuja (toca una barra). */
@Composable
fun StepsCard(steps: Long, km: Double, week: List<Float>, dayLabels: List<String>, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    var selected by remember(week.size) { mutableIntStateOf((week.size - 1).coerceAtLeast(0)) }
    val pct = ((steps * 100f) / STEPS_GOAL).toInt()
    BentoCard(modifier, hero = true, padding = 22.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HmTileHeader(Icons.Filled.DirectionsWalk, Volt.accent, "Pasos", Modifier.weight(1f))
            Box(Modifier.clip(CircleShape).background(Volt.accent.copy(alpha = 0.16f)).padding(horizontal = 12.dp, vertical = 6.dp)) {
                Text("$pct% de la meta", color = Volt.accent, style = VoltType.label, maxLines = 1)
            }
        }
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth().glowBackdrop(alpha = 0.16f, radiusFraction = 0.55f)) {
            Row(verticalAlignment = Alignment.Bottom) {
                CountUpText(steps.toFloat(), style = VoltType.numberXL.copy(brush = Volt.gradient))
                Spacer(Modifier.width(8.dp))
                Text("pasos", color = Volt.textDim, style = VoltType.title, modifier = Modifier.padding(bottom = 10.dp))
            }
        }
        Text("${"%.2f".format(km)} km · meta ${"%,d".format(STEPS_GOAL)}", color = Volt.textDim, style = VoltType.body)
        Spacer(Modifier.height(14.dp))
        HmGradientBar((steps.toFloat() / STEPS_GOAL).coerceIn(0f, 1f), height = 8.dp)
        Spacer(Modifier.height(18.dp))
        WeekBars(week, selected, { i -> if (i != selected) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); selected = i } }, Modifier.fillMaxWidth().height(132.dp))
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth()) {
            dayLabels.forEachIndexed { i, l ->
                Text(
                    l, color = if (i == selected) Volt.accent else Volt.textDim, style = VoltType.label,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f),
                )
            }
        }
    }
}


@Composable
private fun WeekBars(values: List<Float>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(values) { progress.snapTo(0f); progress.animateTo(1f, tween(1000, easing = EaseOutExpo)) }
    val sel = remember { Animatable(selected.toFloat()) }
    LaunchedEffect(selected) { sel.animateTo(selected.toFloat(), spring(0.7f, 420f)) }
    val measurer = rememberTextMeasurer()
    val c1 = Volt.accent
    val c2 = Volt.accent2
    val dim = c1.copy(alpha = 0.22f)
    val ink = Volt.onAccent
    val bubbleStyle = VoltType.label.copy(fontSize = 12.sp, color = ink, letterSpacing = 0.sp)
    val maxV = remember(values) { (values.maxOrNull() ?: 1f).coerceAtLeast(1f) }
    Canvas(
        modifier.pointerInput(values.size) {
            detectTapGestures { pos ->
                val n = values.size
                if (n > 0) onSelect((pos.x / size.width * n).toInt().coerceIn(0, n - 1))
            }
        },
    ) {
        val n = values.size
        if (n == 0) return@Canvas
        val topPad = 36.dp.toPx()
        val chartH = size.height - topPad
        val gap = 8.dp.toPx()
        val w = (size.width - gap * (n - 1)) / n
        val minH = 6.dp.toPx()
        val p = progress.value
        val s = sel.value.coerceIn(0f, (n - 1).toFloat())
        fun barH(i: Int) = minH + (values[i] / maxV) * (chartH - minH)
        values.forEachIndexed { i, _ ->
            val local = (p * 1.5f - i * 0.5f / n).coerceIn(0f, 1f)
            val h = barH(i) * local
            val x = i * (w + gap)
            val top = size.height - h
            val r = CornerRadius(w / 2, w / 2)
            drawRoundRect(dim, Offset(x, top), Size(w, h), r)
            val k = (1f - abs(i - s)).coerceIn(0f, 1f)
            if (k > 0.01f) drawRoundRect(Brush.verticalGradient(listOf(c2, c1), startY = top, endY = size.height), Offset(x, top), Size(w, h), r, alpha = k)
        }
        // Burbuja con el valor del dia seleccionado (interpola entre barras al cambiar)
        val a = (((p - 0.55f) / 0.45f)).coerceIn(0f, 1f)
        if (a > 0.01f) {
            val i0 = floor(s).toInt().coerceIn(0, n - 1)
            val i1 = min(n - 1, i0 + 1)
            val f = s - i0
            val cx = (i0 + 0.5f) * (w + gap) - gap / 2f + f * (w + gap) * (if (i1 != i0) 1f else 0f)
            val barTop = size.height - (barH(i0) + (barH(i1) - barH(i0)) * f)
            val txt = "%,d".format(values[s.roundToInt().coerceIn(0, n - 1)].toInt())
            val layout = measurer.measure(txt, bubbleStyle)
            val bw = layout.size.width + 22.dp.toPx()
            val bh = 26.dp.toPx()
            val bx = (cx - bw / 2f).coerceIn(0f, size.width - bw)
            val by = (barTop - bh - 8.dp.toPx()).coerceAtLeast(0f)
            drawRoundRect(
                Brush.horizontalGradient(listOf(c1, c2), startX = bx, endX = bx + bw), Offset(bx, by), Size(bw, bh),
                CornerRadius(bh / 2, bh / 2), alpha = a,
            )
            drawText(layout, color = ink.copy(alpha = a), topLeft = Offset(bx + (bw - layout.size.width) / 2f, by + (bh - layout.size.height) / 2f))
        }
    }
}

@Composable
fun CaloriesTile(consumed: Int, target: Int, activeKcal: Double?, modifier: Modifier = Modifier) {
    BentoCard(modifier, padding = 16.dp) {
        HmTileHeader(Icons.Filled.LocalFireDepartment, Volt.accent, "Calorías")
        Spacer(Modifier.height(14.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DonutProgress(if (target > 0) consumed.toFloat() / target else 0f, Modifier.size(116.dp), stroke = 12.dp, color = Volt.accent) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CountUpText(consumed.toFloat(), style = VoltType.numberM.copy(color = Volt.text))
                    Text("kcal", color = Volt.textDim, style = VoltType.label)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            if (target > 0) "de $target kcal" else "Sin plan nutricional", color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp),
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
        )
        if (activeKcal != null && activeKcal > 0) {
            Text(
                "${activeKcal.toInt()} activas", color = Volt.accent, style = VoltType.label,
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp), textAlign = TextAlign.Center,
            )
        }
    }
}

/** Ola de agua: el nivel sube con spring y la superficie ondula (solo fase draw). */
@Composable
private fun WaterWave(level: Float, modifier: Modifier = Modifier) {
    val lvl = remember { Animatable(0f) }
    LaunchedEffect(level) { lvl.animateTo(level.coerceIn(0f, 1f), spring(0.85f, 140f)) }
    val reduce = rememberReduceMotion()
    val phase = rememberInfiniteTransition(label = "wave").animateFloat(
        0f, if (reduce) 0f else (2f * PI.toFloat()), infiniteRepeatable(tween(3800, easing = LinearEasing)), label = "wavePhase",
    )
    val c = Volt.water
    val path = remember { Path() }
    Canvas(modifier.clip(RoundedCornerShape(16.dp))) {
        drawRect(Color.White.copy(alpha = 0.05f))
        val l = lvl.value
        if (l <= 0.003f) return@Canvas
        val amp = 4.dp.toPx() * (if (l < 0.97f) 1f else 0.4f)
        val base = size.height * (1f - 0.8f * l) - amp
        val step = 8f
        for (layer in 0..1) {
            path.reset()
            val ph = phase.value * (if (layer == 0) 1f else -1f) + layer * 1.7f
            val b = base + layer * 3.dp.toPx()
            path.moveTo(0f, size.height)
            var x = 0f
            while (x <= size.width + step) {
                path.lineTo(x, b + amp * sin((x / size.width * 2f * PI.toFloat() * 1.4f + ph).toDouble()).toFloat())
                x += step
            }
            path.lineTo(size.width, size.height)
            path.close()
            drawPath(path, Brush.verticalGradient(listOf(c.copy(alpha = if (layer == 0) 0.85f else 0.40f), c.copy(alpha = 0.22f)), startY = b - amp, endY = size.height))
        }
    }
}

@Composable
fun WaterTile(ml: Int, onAdd: (Int) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    BentoCard(modifier, padding = 16.dp) {
        HmTileHeader(Icons.Filled.WaterDrop, Volt.water, "Agua")
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            CountUpText(ml.toFloat(), style = VoltType.numberM.copy(color = Volt.water))
            Text(" ml", color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(bottom = 4.dp))
        }
        Text("meta $WATER_GOAL_ML ml", color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp))
        Spacer(Modifier.height(10.dp))
        WaterWave((ml.toFloat() / WATER_GOAL_ML), Modifier.fillMaxWidth().weight(1f).heightIn(min = 44.dp))
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(
                Modifier.weight(1f).height(48.dp).clip(CircleShape).background(Volt.surface2)
                    .pressScale { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onAdd(-250) },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Remove, "Quitar 250 ml", tint = Volt.textDim, modifier = Modifier.size(18.dp)) }
            Row(
                Modifier.weight(2f).height(48.dp).clip(CircleShape).background(Volt.water)
                    .pressScale { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAdd(250) },
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
            ) {
                Icon(Icons.Filled.Add, null, tint = Volt.onAccent, modifier = Modifier.size(16.dp))
                Text("250 ml", color = Volt.onAccent, style = VoltType.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp), maxLines = 1)
            }
        }
    }
}

@Composable
fun RecoveryTile(score: Int, advice: RecoveryAdvice, history: List<Float>, checkedIn: Boolean, modifier: Modifier = Modifier) {
    val col = recoveryColor(score)
    BentoCard(modifier, padding = 16.dp) {
        HmTileHeader(Icons.Filled.Bedtime, col, "Recuperación")
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            CountUpText(score.toFloat(), style = VoltType.numberL.copy(color = col))
            Text(if (checkedIn) "/100" else "%", color = Volt.textDim, style = VoltType.title, modifier = Modifier.padding(start = 2.dp, bottom = 6.dp))
        }
        Text(
            if (checkedIn) advice.intensityLabel else "Estimado · sin check-in", color = Volt.textDim,
            style = VoltType.body.copy(fontSize = 12.sp), maxLines = 2,
        )
        Spacer(Modifier.height(10.dp))
        if (history.size >= 2) Sparkline(history, Modifier.fillMaxWidth().height(38.dp), col)
        else Box(Modifier.fillMaxWidth().height(38.dp))
    }
}

@Composable
fun WeightTile(last: Double?, delta: Double?, history: List<Float>, modifier: Modifier = Modifier) {
    BentoCard(modifier, padding = 16.dp) {
        HmTileHeader(Icons.Filled.MonitorWeight, Volt.accent, "Peso")
        Spacer(Modifier.height(12.dp))
        if (last != null) {
            Row(verticalAlignment = Alignment.Bottom) {
                CountUpText(last.toFloat(), decimals = 1, style = VoltType.numberL.copy(brush = Volt.gradient))
                Text(" kg", color = Volt.textDim, style = VoltType.title, modifier = Modifier.padding(bottom = 6.dp))
            }
        } else Text("—", color = Volt.textDim, style = VoltType.numberL)
        Text(
            when { delta == null -> "Registra tu peso"; delta > 0 -> "▲ +${"%.1f".format(delta)} kg"; delta < 0 -> "▼ ${"%.1f".format(delta)} kg"; else -> "= sin cambios" },
            color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp),
        )
        Spacer(Modifier.height(10.dp))
        if (history.size >= 2) Sparkline(history, Modifier.fillMaxWidth().height(38.dp), Volt.accent)
        else Box(Modifier.fillMaxWidth().height(38.dp))
    }
}

// ───────────────────────── Recovery check-in ─────────────────────────

@Composable
fun RecoveryCard(repo: Repository, health: HealthState, todayLog: RecoveryLog?, advice: RecoveryAdvice, score: Int, scope: CoroutineScope) {
    var showForm by remember { mutableStateOf(false) }
    var sleep by remember { mutableStateOf("") }
    var hrv by remember { mutableStateOf("") }
    var prefilled by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    // Prellena desde Health Connect (sueno de anoche y HRV) al abrir el formulario
    LaunchedEffect(showForm, health.status) {
        if (showForm && health.status == "connected" && sleep.isBlank() && hrv.isBlank()) {
            val s = health.sleepLastNight()
            val h = health.snap?.hrvMs
            if (s != null && s > 0) { sleep = "%.1f".format(s).replace(',', '.'); prefilled = true }
            if (h != null && h > 0) { hrv = h.toInt().toString(); prefilled = true }
        }
    }

    BentoCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                HmTileHeader(Icons.Filled.Bedtime, recoveryColor(score), "Recovery Score")
                Spacer(Modifier.height(8.dp))
                Text(if (todayLog != null) advice.title else "Check-in matutino", color = Volt.text, style = VoltType.h2)
                Text(if (todayLog != null) advice.intensityLabel else "Calibra la intensidad de hoy", color = Volt.textDim, style = VoltType.body)
            }
            if (todayLog != null) {
                GlowRing(score / 100f, Modifier.size(84.dp), stroke = 7.dp) {
                    CountUpText(score.toFloat(), style = VoltType.numberM.copy(color = Volt.text, fontSize = 22.sp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        if (todayLog != null && !showForm) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("Sueño", "${todayLog.sleepHours}h", Modifier.weight(1f))
                todayLog.hrv?.let { StatPill("HRV", "${it.toInt()} ms", Modifier.weight(1f), Volt.danger) }
                StatPill("Score", "$score/100", Modifier.weight(1f), recoveryColor(score))
            }
            Spacer(Modifier.height(12.dp))
            Text(advice.subtitle, color = Volt.text, style = VoltType.body)
            Spacer(Modifier.height(8.dp))
            Text(
                "Actualizar check-in", color = Volt.accent, style = VoltType.label,
                modifier = Modifier.clip(CircleShape).pressScale {
                    sleep = todayLog.sleepHours.toString(); hrv = todayLog.hrv?.toInt()?.toString() ?: ""; showForm = true
                }.padding(vertical = 14.dp, horizontal = 4.dp),
            )
        } else if (!showForm) {
            Text("Registra tus horas de sueño y HRV matutino para calcular tu Recovery Score y calibrar la intensidad del entrenamiento.", color = Volt.textDim, style = VoltType.body)
            Spacer(Modifier.height(14.dp))
            PrimaryButton("Check-in matutino", { showForm = true })
        }

        AnimatedVisibility(showForm, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        sleep, { sleep = it }, label = { Text("Horas de sueño") }, singleLine = true, placeholder = { Text("7.5") },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), colors = voltFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        hrv, { hrv = it }, label = { Text("HRV (ms)") }, singleLine = true, placeholder = { Text("60") },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(20.dp), colors = voltFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Text(
                    if (prefilled) "Datos rellenados desde Health Connect (puedes editarlos). HRV opcional."
                    else "HRV: variabilidad cardíaca de tu smartwatch o banda. Déjalo en blanco si no dispones del dato.",
                    color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp),
                )
                val sleepVal = sleep.replace(',', '.').toDoubleOrNull()
                val valid = sleepVal != null && sleepVal > 0 && sleepVal <= 14
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    PrimaryButton("Calcular Recovery", {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        saving = true
                        scope.launch {
                            val (sc, adv) = saveRecoveryCheckin(repo, sleepVal!!, hrv.replace(',', '.').toDoubleOrNull())
                            repo.toast("success", "Recovery Score: $sc/100 💤", adv.intensityLabel)
                            saving = false; showForm = false; sleep = ""; hrv = ""; prefilled = false
                        }
                    }, Modifier.weight(1f), enabled = valid, loading = saving)
                    HmGhostPill("Cancelar", { showForm = false; sleep = ""; hrv = ""; prefilled = false }, height = 58.dp)
                }
            }
        }
    }
}

// ───────────────────────── Monitor de FC (BLE) ─────────────────────────

@Composable
fun HeartRateCard() {
    val ctx = LocalContext.current
    val ble = remember { BleHeartRateClient(ctx.applicationContext) }
    DisposableEffect(Unit) { onDispose { ble.stop() } }
    val bpm by ble.bpm.collectAsStateWithLifecycle()
    val state by ble.state.collectAsStateWithLifecycle()
    val device by ble.deviceName.collectAsStateWithLifecycle()
    val perms = if (Build.VERSION.SDK_INT >= 31) arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { res ->
        if (res.values.all { it }) ble.start()
    }
    val haptic = LocalHapticFeedback.current
    fun connect() {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (perms.all { ContextCompat.checkSelfPermission(ctx, it) == PackageManager.PERMISSION_GRANTED }) ble.start() else launcher.launch(perms)
    }

    BentoCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                HmTileHeader(Icons.Filled.Favorite, Volt.danger, "Monitor de FC")
                Spacer(Modifier.height(6.dp))
                Text("Sensor Bluetooth (Polar, Garmin…)", color = Volt.textDim, style = VoltType.body)
            }
            val label = when (state) { "connected" -> "En vivo"; "connecting", "scanning" -> "Buscando…"; "error" -> "Error"; else -> "Apagado" }
            val c = when (state) { "connected" -> Volt.accent; "error" -> Volt.danger; else -> Volt.textDim }
            Row(
                Modifier.clip(CircleShape).background(c.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(7.dp).clip(CircleShape).background(c))
                Spacer(Modifier.width(6.dp))
                Text(label, color = c, style = VoltType.body.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold), maxLines = 1)
            }
        }
        Spacer(Modifier.height(14.dp))
        AnimatedVisibility(state == "connected" && bpm != null) {
            val pulse = rememberPulse(1f, 1.2f, 600)
            Row(
                Modifier.fillMaxWidth().padding(bottom = 14.dp).clip(RoundedCornerShape(24.dp)).background(Volt.surface2).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Favorite, null, tint = Volt.danger, modifier = Modifier.size(36.dp).graphicsLayer { scaleX = pulse.value; scaleY = pulse.value })
                Spacer(Modifier.width(16.dp))
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        RollingNumber(bpm ?: 0, style = VoltType.numberL.copy(color = Volt.text))
                        Text(" bpm", color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    device?.let { Text(it, color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp)) }
                }
            }
        }
        Text(
            when (state) {
                "scanning" -> "Buscando sensores cercanos…"
                "connecting" -> "Conectando…"
                "error" -> "No se pudo conectar. Comprueba que el sensor esté encendido y el Bluetooth activo."
                else -> "Conecta un pulsómetro BLE estándar para monitorizar tu frecuencia cardíaca en tiempo real."
            },
            color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp),
        )
        Spacer(Modifier.height(14.dp))
        if (state == "connected") {
            HmGhostPill("Desconectar", { ble.stop() }, Modifier.fillMaxWidth(), icon = Icons.Filled.BluetoothDisabled)
        } else {
            PrimaryButton(if (state == "connecting" || state == "scanning") "Conectando…" else "Conectar sensor", { connect() }, enabled = state != "connecting" && state != "scanning")
        }
    }
}
