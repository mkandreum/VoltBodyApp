package com.voltbody.app.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import com.voltbody.app.ui.*
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.RecoveryAdvice
import com.voltbody.app.util.RecoveryTier
import com.voltbody.app.util.saveRecoveryCheckin
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate

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
        val ok = runCatching { m.hasAllPermissions() }.getOrDefault(false)
        if (!ok) { status = "needs"; return }
        VoltPrefs.setHealthEnabled(ctx, true)
        val s = runCatching { m.readToday() }.getOrNull()
        snap = s
        status = "connected"
        if (s != null && repo != null && repo.state.value.isAuthenticated) {
            val date = LocalDate.now().toString()
            val m = DailyMetric(
                date, steps = s.steps.toInt(), distanceKm = s.distanceKm, activeKcal = s.activeKcal, totalKcal = s.totalKcal,
                sleepHours = s.sleepHours, hrv = s.hrvMs, restingHr = s.restingHr, avgHr = s.avgHr,
            )
            val old = repo.state.value.metrics.firstOrNull { it.date == date }
            if (old == null || old.steps != m.steps || old.distanceKm != m.distanceKm || old.activeKcal != m.activeKcal ||
                old.totalKcal != m.totalKcal || (m.sleepHours != null && old.sleepHours != m.sleepHours)
            ) repo.saveMetrics(date, m)
        }
    }

    suspend fun sleepLastNight(): Double? =
        snap?.sleepHours ?: runCatching { manager.readSleepLastNight() }.getOrNull()
}

@Composable
fun rememberHealthState(repo: Repository? = null): HealthState {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val hs = remember { HealthState(ctx.applicationContext) }
    val launcher = rememberLauncherForActivityResult(hs.manager.requestContract()) { scope.launch { hs.refresh(repo) } }
    hs.requestPermissions = { runCatching { launcher.launch(hs.manager.permissions) } }
    val authed = repo?.state?.collectAsState()?.value?.isAuthenticated ?: false
    LaunchedEffect(authed) { hs.refresh(repo) }
    return hs
}

fun recoveryColor(score: Int): Color = when {
    score >= 85 -> Color(0xFFC6F135)
    score >= 65 -> Color(0xFF34D399)
    score >= 40 -> Color(0xFFFACC15)
    else -> Color(0xFF60A5FA)
}

@Composable
fun ConnectHealthCard(health: HealthState, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    VoltCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val pulse by rememberPulse()
            Box(Modifier.size(46.dp).scale(pulse).clip(CircleShape).background(Volt.accent.copy(.18f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Favorite, null, tint = Volt.accent)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Conectar salud", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    if (health.status == "unavailable") "Health Connect no está disponible en este dispositivo. Instálalo desde Google Play para sincronizar pasos, sueño y HRV (Samsung Health, Garmin...)."
                    else "Autoriza Health Connect para ver pasos, calorías, sueño y HRV (también los de Samsung Health).",
                    color = Volt.textDim, fontSize = 12.sp,
                )
            }
        }
        if (health.status == "needs") {
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Conectar Health Connect", { haptic.performHapticFeedback(HapticFeedbackType.LongPress); health.requestPermissions() })
        }
    }
}

// ───────────────────────── Tarjetas de estadisticas ─────────────────────────

@Composable
fun StepsCard(steps: Long, km: Double, week: List<Float>, dayLabels: List<String>, modifier: Modifier = Modifier) {
    VoltCard(modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.DirectionsWalk, null, tint = Volt.accent)
            Spacer(Modifier.width(8.dp))
            Text("Pasos", color = Volt.text, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Box(Modifier.clip(RoundedCornerShape(50)).background(Volt.accent).padding(horizontal = 10.dp, vertical = 4.dp)) {
                Text("${"%,d".format(steps)} hoy", color = Color.Black, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            CountUpText(steps.toFloat(), style = TextStyle(color = Volt.text, fontSize = 34.sp, fontWeight = FontWeight.Black))
            Spacer(Modifier.width(8.dp))
            Text("de $STEPS_GOAL · ${"%.2f".format(km)} km", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(12.dp))
        AnimatedBars(week, Modifier.fillMaxWidth().height(80.dp), highlight = week.lastIndex)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            dayLabels.forEachIndexed { i, l ->
                Text(l, color = if (i == dayLabels.lastIndex) Volt.accent else Volt.textDim, fontSize = 11.sp, fontWeight = if (i == dayLabels.lastIndex) FontWeight.Bold else FontWeight.Normal, textAlign = TextAlign.Center, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun CaloriesTile(consumed: Int, target: Int, activeKcal: Double?, modifier: Modifier = Modifier) {
    VoltCard(modifier, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.LocalFireDepartment, null, tint = Volt.orange, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Calorías", color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            DonutProgress(if (target > 0) consumed.toFloat() / target else 0f, Modifier.size(92.dp), stroke = 10.dp, color = Volt.orange) {
                CountUpText(consumed.toFloat(), style = TextStyle(color = Volt.text, fontSize = 18.sp, fontWeight = FontWeight.Bold))
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(if (target > 0) "de $target kcal" else "Sin plan nutricional", color = Volt.textDim, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        if (activeKcal != null && activeKcal > 0) Text("🔥 ${activeKcal.toInt()} activas", color = Volt.orange, fontSize = 11.sp, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
    }
}

@Composable
fun WaterTile(ml: Int, onAdd: (Int) -> Unit, modifier: Modifier = Modifier) {
    VoltCard(modifier, padding = 14.dp) {
        val haptic = LocalHapticFeedback.current
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.WaterDrop, null, tint = Volt.water, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Agua", color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(10.dp))
        CountUpText(ml.toFloat(), suffix = " ml", style = TextStyle(color = Volt.water, fontSize = 24.sp, fontWeight = FontWeight.Black))
        Text("meta $WATER_GOAL_ML ml", color = Volt.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        val p by animateFloatAsStateCompat((ml.toFloat() / WATER_GOAL_ML).coerceIn(0f, 1f))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Volt.surface2)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape(4.dp)).background(Volt.water))
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(12.dp)).background(Volt.surface2)
                .pressScale { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onAdd(-250) }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Remove, "Quitar 250 ml", tint = Volt.textDim, modifier = Modifier.size(18.dp))
            }
            Box(Modifier.weight(2f).height(36.dp).clip(RoundedCornerShape(12.dp)).background(Volt.water)
                .pressScale { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onAdd(250) }, contentAlignment = Alignment.Center) {
                Text("+250 ml", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun animateFloatAsStateCompat(target: Float): State<Float> =
    androidx.compose.animation.core.animateFloatAsState(target, androidx.compose.animation.core.tween(700), label = "bar")

@Composable
fun RecoveryTile(score: Int, advice: RecoveryAdvice, history: List<Float>, checkedIn: Boolean, modifier: Modifier = Modifier) {
    VoltCard(modifier, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Bedtime, null, tint = recoveryColor(score), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Recuperación", color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        CountUpText(score.toFloat(), suffix = if (checkedIn) "/100" else "%", style = TextStyle(color = recoveryColor(score), fontSize = 26.sp, fontWeight = FontWeight.Black))
        Text(if (checkedIn) advice.intensityLabel else "🌙 Estimado (sin check-in)", color = Volt.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(8.dp))
        if (history.size >= 2) Sparkline(history, Modifier.fillMaxWidth().height(36.dp), recoveryColor(score))
        else Box(Modifier.fillMaxWidth().height(36.dp))
    }
}

@Composable
fun WeightTile(last: Double?, delta: Double?, history: List<Float>, modifier: Modifier = Modifier) {
    VoltCard(modifier, padding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.MonitorWeight, null, tint = Volt.accent, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("Peso", color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
        }
        Spacer(Modifier.height(8.dp))
        if (last != null) CountUpText(last.toFloat(), decimals = 1, suffix = " kg", style = TextStyle(color = Volt.text, fontSize = 24.sp, fontWeight = FontWeight.Black))
        else Text("—", color = Volt.textDim, fontSize = 24.sp, fontWeight = FontWeight.Black)
        Text(
            when { delta == null -> "Registra tu peso"; delta > 0 -> "▲ +${"%.1f".format(delta)} kg"; delta < 0 -> "▼ ${"%.1f".format(delta)} kg"; else -> "= sin cambios" },
            color = Volt.textDim, fontSize = 11.sp,
        )
        Spacer(Modifier.height(8.dp))
        if (history.size >= 2) Sparkline(history, Modifier.fillMaxWidth().height(36.dp), Volt.accent)
        else Box(Modifier.fillMaxWidth().height(36.dp))
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

    VoltCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Bedtime, null, tint = Volt.accent)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("💤 Recovery Score", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Check-in matutino de recuperación", color = Volt.textDim, fontSize = 11.sp)
            }
            if (todayLog != null) Column(horizontalAlignment = Alignment.End) {
                Row(verticalAlignment = Alignment.Bottom) {
                    CountUpText(score.toFloat(), style = TextStyle(color = recoveryColor(score), fontSize = 28.sp, fontWeight = FontWeight.Black))
                    Text("/100", color = Volt.textDim, fontSize = 11.sp, modifier = Modifier.padding(bottom = 4.dp))
                }
                Text(advice.intensityLabel, color = Volt.text, fontSize = 11.sp)
            }
        }
        Spacer(Modifier.height(12.dp))

        if (todayLog != null && !showForm) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("😴 Sueño", "${todayLog.sleepHours}h", Modifier.weight(1f))
                todayLog.hrv?.let { StatPill("❤️ HRV", "${it.toInt()} ms", Modifier.weight(1f), Color(0xFFFF6B81)) }
                StatPill("🎯 Score", "$score/100", Modifier.weight(1f), recoveryColor(score))
            }
            Spacer(Modifier.height(10.dp))
            Text(advice.subtitle, color = Volt.text, fontSize = 13.sp)
            Spacer(Modifier.height(8.dp))
            Text("Actualizar check-in", color = Volt.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.pressScale {
                sleep = todayLog.sleepHours.toString(); hrv = todayLog.hrv?.toInt()?.toString() ?: ""; showForm = true
            })
        } else if (!showForm) {
            Text("Registra tus horas de sueño y HRV matutino para calcular tu Recovery Score y calibrar la intensidad del entrenamiento.", color = Volt.textDim, fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            PrimaryButton("Check-in matutino", { showForm = true })
        }

        AnimatedVisibility(showForm, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        sleep, { sleep = it }, label = { Text("Horas de sueño") }, singleLine = true, placeholder = { Text("7.5") },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = voltFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    )
                    OutlinedTextField(
                        hrv, { hrv = it }, label = { Text("HRV (ms)") }, singleLine = true, placeholder = { Text("60") },
                        modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp), colors = voltFieldColors(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    )
                }
                Text(
                    if (prefilled) "Datos rellenados desde Health Connect (puedes editarlos). HRV opcional."
                    else "HRV: variabilidad cardíaca de tu smartwatch o banda. Déjalo en blanco si no dispones del dato.",
                    color = Volt.textDim, fontSize = 11.sp,
                )
                val sleepVal = sleep.replace(',', '.').toDoubleOrNull()
                val valid = sleepVal != null && sleepVal > 0 && sleepVal <= 14
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PrimaryButton("Calcular Recovery", {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        saving = true
                        scope.launch {
                            val (sc, adv) = saveRecoveryCheckin(repo, sleepVal!!, hrv.replace(',', '.').toDoubleOrNull())
                            repo.toast("success", "Recovery Score: $sc/100 💤", adv.intensityLabel)
                            saving = false; showForm = false; sleep = ""; hrv = ""; prefilled = false
                        }
                    }, Modifier.weight(1f), enabled = valid, loading = saving)
                    OutlinedButton({ showForm = false; sleep = ""; hrv = ""; prefilled = false }, Modifier.height(54.dp), shape = RoundedCornerShape(27.dp)) {
                        Text("Cancelar", color = Volt.text)
                    }
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

    VoltCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Favorite, null, tint = Color(0xFFFF6B81))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("❤️ Monitor de FC", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Sensor Bluetooth (Polar, Garmin…)", color = Volt.textDim, fontSize = 11.sp)
            }
            val label = when (state) { "connected" -> "live"; "connecting", "scanning" -> "…"; "error" -> "err"; else -> "off" }
            StatPill("sensor", label, color = if (state == "connected") Volt.accent else Volt.textDim)
        }
        Spacer(Modifier.height(12.dp))
        AnimatedVisibility(state == "connected" && bpm != null) {
            val pulse by rememberPulse(1f, 1.2f, 600)
            Row(
                Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(RoundedCornerShape(20.dp)).background(Volt.surface2).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Favorite, null, tint = Color(0xFFFF6B81), modifier = Modifier.size(34.dp).scale(pulse))
                Spacer(Modifier.width(14.dp))
                Column {
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text("${bpm ?: 0}", color = Volt.text, fontSize = 38.sp, fontWeight = FontWeight.Black)
                        Text(" bpm", color = Volt.textDim, fontSize = 13.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    device?.let { Text(it, color = Volt.textDim, fontSize = 11.sp) }
                }
            }
        }
        Text(
            when (state) {
                "scanning" -> "Buscando sensores cercanos…"
                "connecting" -> "Conectando…"
                "error" -> "⚠️ No se pudo conectar. Comprueba que el sensor esté encendido y el Bluetooth activo."
                else -> "Conecta un pulsómetro BLE estándar para monitorizar tu frecuencia cardíaca en tiempo real."
            },
            color = Volt.textDim, fontSize = 12.sp,
        )
        Spacer(Modifier.height(12.dp))
        if (state == "connected") {
            OutlinedButton({ ble.stop() }, Modifier.fillMaxWidth().height(48.dp), shape = RoundedCornerShape(24.dp)) {
                Icon(Icons.Filled.BluetoothDisabled, null, tint = Volt.text); Spacer(Modifier.width(8.dp)); Text("Desconectar", color = Volt.text)
            }
        } else {
            PrimaryButton(if (state == "connecting" || state == "scanning") "Conectando…" else "Conectar sensor", { connect() }, enabled = state != "connecting" && state != "scanning")
        }
    }
}
