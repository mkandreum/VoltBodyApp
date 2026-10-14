package com.voltbody.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.voltbody.app.data.AvatarConfig
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.*
import com.voltbody.app.ui.screens.profile.*
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.Notifications
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private data class Rank(val min: Int, val max: Int, val name: String, val emoji: String, val range: String, val color: Color)

private val RANKS = listOf(
    Rank(0, 30, "Recluta de Bronce", "🛡️", "< 30%", Color(0xFFF59E0B)),
    Rank(30, 60, "Guerrero de Acero", "⚔️", "30-59%", Color(0xFFCBD5E1)),
    Rank(60, 85, "Campeón del Neón", "⚡", "60-84%", Color(0xFF22D3EE)),
    Rank(85, 101, "Cyborg Supremo", "🤖", "≥ 85%", Color(0xFFC6F135)),
)

private data class ThemeOpt(val id: String, val label: String, val color: Color)

private val THEMES = listOf(
    ThemeOpt("aguamarina-negro", "Aguamarina", Color(0xFF3FF5D0)),
    ThemeOpt("verde-negro", "Neón Verde", Color(0xFFC6F135)),
    ThemeOpt("ocaso-negro", "Ocaso", Color(0xFFFF8A3D)),
)

@Composable
private fun GroupLabel(text: String, index: Int) {
    Text(
        text.uppercase(), color = Volt.textDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp,
        modifier = Modifier.staggeredEntry(index).padding(start = 6.dp, top = 8.dp),
    )
}

@Composable
private fun IconBadge(icon: androidx.compose.ui.graphics.vector.ImageVector, tint: Color = Volt.accent) {
    Box(Modifier.size(34.dp).clip(RoundedCornerShape(12.dp)).background(Volt.surface2), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun VoltSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked, onCheckedChange = onChange,
        colors = SwitchDefaults.colors(checkedTrackColor = Volt.accent, checkedThumbColor = Color.Black, uncheckedTrackColor = Volt.surface2, uncheckedBorderColor = Volt.border, uncheckedThumbColor = Volt.textDim),
    )
}

@Composable
fun ProfileScreen(repo: Repository) {
    val state by repo.state.collectAsState()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val p = state.profile

    var showRank by remember { mutableStateOf(false) }
    var activePhoto by remember { mutableStateOf<String?>(null) }
    var weightInput by remember { mutableStateOf("") }
    var phrase by remember(state.motivationPhrase) { mutableStateOf(state.motivationPhrase) }

    // ── fotos ──
    val profilePicker = rememberPhotoPicker("Foto de perfil") { url ->
        repo.saveProfile(withPhotos = true) { it.copy(profile = it.profile.copy(profilePhoto = url)) }
        repo.toast("success", "Foto de perfil actualizada")
    }
    val motivationPicker = rememberPhotoPicker("Foto de inspiración") { url ->
        repo.saveProfile(withPhotos = true) { it.copy(profile = it.profile.copy(motivationPhoto = url)) }
        repo.toast("success", "Foto de inspiración guardada")
    }
    val progressPicker = rememberPhotoPicker("Foto de progreso") { url ->
        repo.addPhoto(Instant.now().toString(), url)
        repo.toast("success", "Foto añadida a tu galería")
    }

    // ── notificaciones ──
    fun enableNotifications() {
        scope.launch {
            repo.setNotificationsEnabled(true)
            Notifications.ensureChannels(ctx)
            Notifications.scheduleAll(ctx, repo.state.value)
            repo.toast("success", "Notificaciones activadas", "Recordatorios de comida, entreno y peso.")
        }
    }
    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) enableNotifications() else repo.toast("error", "Permiso denegado", "Actívalo en los ajustes del sistema para recibir recordatorios.")
    }
    fun toggleNotifications(on: Boolean) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        if (on) {
            val needs = Build.VERSION.SDK_INT >= 33 &&
                ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
            if (needs) notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS) else enableNotifications()
        } else {
            Notifications.cancelAll(ctx)
            scope.launch { repo.setNotificationsEnabled(false) }
        }
    }

    // ── calculos ──
    val today = LocalDate.now()
    val todayKey = today.toString()
    val weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val nextWeekStart = weekStart.plusDays(7)
    val weightLogs = state.weightLogs.sortedBy { it.date }
    val alreadyLoggedThisWeek = weightLogs.any { it.date >= weekStart.toString() && it.date < nextWeekStart.toString() }
    val lastWeight = weightLogs.lastOrNull()

    val weeklyChart = remember(weightLogs) {
        val byWeek = weightLogs.mapNotNull { l ->
            runCatching { LocalDate.parse(l.date.take(10)).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to l.weight }.getOrNull()
        }.toMap()
        (0 until 8).mapNotNull { i -> byWeek[weekStart.minusWeeks((7 - i).toLong())]?.toFloat() }
    }

    val prs = remember(state.logs, state.routine) {
        val names = state.routine.flatMap { it.exercises }.associate { it.id to it.name }
        val best = HashMap<String, com.voltbody.app.data.WorkoutLog>()
        state.logs.forEach { l -> val e = best[l.exerciseId]; if (e == null || l.weight > e.weight) best[l.exerciseId] = l }
        best.values.filter { it.weight > 0 }.sortedByDescending { it.weight }.take(10).map { Triple(names[it.exerciseId] ?: it.exerciseId, it, it.date.take(10)) }
    }

    val goals = state.weeklyGoals
    val completedGoals = goals.count { it.done }
    val goalProgress = Math.round(completedGoals * 100f / goals.size.coerceAtLeast(1))
    val strengthScore = (state.logs.size * 4).coerceAtMost(100)
    val energyScore = (weightLogs.size * 18).coerceIn(30, 100)
    val rank = RANKS.firstOrNull { goalProgress >= it.min && goalProgress < it.max } ?: RANKS[0]

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // ───── Cabecera ─────
        item {
            Column(Modifier.staggeredEntry(0)) {
                Text("CONFIGURACIÓN & BIOMETRÍA", color = Volt.textDim, fontSize = 11.sp, letterSpacing = 1.6.sp, fontWeight = FontWeight.SemiBold)
                Text("Perfil", color = Volt.text, fontSize = 30.sp, fontWeight = FontWeight.Black)
            }
        }

        // ───── Identidad ─────
        item {
            VoltCard(Modifier.staggeredEntry(1)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(84.dp)) {
                        Box(
                            Modifier.size(80.dp).clip(RoundedCornerShape(24.dp)).background(Volt.surface2).border(2.dp, Volt.accent.copy(alpha = 0.5f), RoundedCornerShape(24.dp))
                                .clickable { profilePicker.open() },
                            contentAlignment = Alignment.Center,
                        ) {
                            val photo = p.profilePhoto
                            if (!photo.isNullOrBlank()) DataUrlImage(photo, Modifier.fillMaxSize(), reqSize = 400)
                            else Icon(Icons.Filled.Person, null, tint = Volt.accent, modifier = Modifier.size(38.dp))
                        }
                        Box(
                            Modifier.align(Alignment.BottomEnd).size(28.dp).clip(CircleShape).background(Volt.accent).pressScale { profilePicker.open() },
                            contentAlignment = Alignment.Center,
                        ) { Icon(Icons.Filled.PhotoCamera, "Cambiar foto de perfil", tint = Color.Black, modifier = Modifier.size(15.dp)) }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(p.name ?: state.user?.name ?: "Usuario Volt", color = Volt.text, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                        Text(p.goal.orEmpty(), color = Volt.accent, fontSize = 12.sp, fontWeight = FontWeight.Medium, maxLines = 2)
                        Text(listOfNotNull(p.gender, p.age?.let { "$it años" }).joinToString(" · "), color = Volt.textDim, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Volt.surface2.copy(alpha = 0.6f)).clickable { showRank = true }.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconBadge(Icons.Filled.Shield, rank.color)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("RANGO ACTUAL", color = Volt.textDim, fontSize = 10.sp, letterSpacing = 1.sp)
                        Text("${rank.emoji} ${rank.name}", color = rank.color, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Text("$goalProgress%", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Icon(Icons.Filled.ChevronRight, null, tint = Volt.textDim)
                }
            }
        }

        // ───── Biometría ─────
        item { GroupLabel("Biometría & rendimiento", 2) }
        item {
            VoltCard(Modifier.staggeredEntry(3), padding = 4.dp) {
                val rows = listOf(
                    Triple(Icons.Filled.MonitorWeight, "Peso corporal", p.weight?.let { "${trimNum(it)} kg" } ?: "--"),
                    Triple(Icons.Filled.Height, "Estatura", p.height?.let { "${trimNum(it)} cm" } ?: "--"),
                    Triple(Icons.Filled.FitnessCenter, "Estado físico", p.currentState?.substringBefore(' ') ?: "--"),
                    Triple(Icons.Filled.Cake, "Edad", p.age?.let { "$it años" } ?: "--"),
                    Triple(Icons.Filled.Flag, "Objetivo", if (p.goalTargetKg != null) "${p.goalDirection ?: ""} ${trimNum(p.goalTargetKg)} kg · ${p.goalTimelineMonths ?: "-"} m" else "--"),
                )
                rows.forEachIndexed { i, (icon, label, value) ->
                    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(icon)
                        Spacer(Modifier.width(12.dp))
                        Text(label, color = Volt.text, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        Text(value, color = Volt.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    if (i < rows.lastIndex) HorizontalDivider(color = Volt.border, modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
        }
        item { PersonalDataCard(repo, p, state.notificationsEnabled, Modifier.staggeredEntry(4)) }
        item {
            VoltCard(Modifier.staggeredEntry(5)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.AutoAwesome, null, tint = Volt.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Score de rendimiento", color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text("Algoritmo Volt", color = Volt.textDim, fontSize = 10.sp)
                }
                Spacer(Modifier.height(10.dp))
                listOf(Triple("💪 Fuerza", strengthScore, 0), Triple("🔥 Consistencia", goalProgress, 1), Triple("⚡ Energía", energyScore, 2)).forEach { (l, v, i) ->
                    ScoreBar(l, v, i)
                }
            }
        }

        // ───── Peso ─────
        item { GroupLabel("Seguimiento de peso", 6) }
        item {
            VoltCard(Modifier.staggeredEntry(7)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TrendingUp, null, tint = Volt.accent)
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text("Evolución semanal", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            if (alreadyLoggedThisWeek) "Registrado: ${lastWeight?.let { trimNum(it.weight) }} kg esta semana" else "Registra tu peso semanal para el cálculo de déficit",
                            color = Volt.textDim, fontSize = 10.sp,
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoltTextField(weightInput, { weightInput = filterDecimal(it) }, "Ej. ${p.weight?.let { trimNum(it) } ?: "70"} kg", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                    val v = weightInput.toDoubleOrNull()
                    val valid = v != null && v in 20.0..400.0
                    Box(
                        Modifier.height(56.dp).clip(RoundedCornerShape(18.dp)).background(if (valid && !alreadyLoggedThisWeek) Volt.accent else Volt.surface2)
                            .let { if (valid && !alreadyLoggedThisWeek) it.pressScale {
                                scope.launch {
                                    repo.addWeight(todayKey, v!!)
                                    repo.toast("success", "Peso registrado: ${trimNum(v)} kg")
                                    weightInput = ""
                                }
                            } else it }.padding(horizontal = 18.dp),
                        contentAlignment = Alignment.Center,
                    ) { Text("Registrar", color = if (valid && !alreadyLoggedThisWeek) Color.Black else Volt.textDim, fontWeight = FontWeight.Bold, fontSize = 13.sp) }
                }
                if (weightInput.isNotEmpty() && weightInput.toDoubleOrNull().let { it == null || it !in 20.0..400.0 }) {
                    Text("El peso debe estar entre 20 y 400 kg.", color = Volt.danger, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
                }
                Spacer(Modifier.height(12.dp))
                if (weeklyChart.size >= 2) {
                    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.3f)).padding(12.dp)) {
                        Sparkline(weeklyChart, Modifier.fillMaxWidth().height(90.dp), color = Volt.accent)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Últimas 8 semanas · mín ${"%.1f".format(weeklyChart.min())} kg · máx ${"%.1f".format(weeklyChart.max())} kg",
                            color = Volt.textDim, fontSize = 10.sp,
                        )
                    }
                } else {
                    Text("Sin suficientes registros. Anota tu peso cada semana para ver la gráfica.", color = Volt.textDim, fontSize = 11.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                }
                if (weightLogs.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    weightLogs.reversed().take(4).forEach { l ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(12.dp)).background(Color.Black.copy(alpha = 0.25f)).padding(horizontal = 12.dp, vertical = 8.dp),
                        ) {
                            Text(l.date.take(10), color = Volt.textDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
                            Text("${trimNum(l.weight)} kg", color = Volt.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ───── Objetivos y récords ─────
        item { GroupLabel("Objetivos & récords", 8) }
        item {
            VoltCard(Modifier.staggeredEntry(9)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.TrackChanges, null, tint = Volt.accent)
                    Spacer(Modifier.width(8.dp))
                    Text("Metas semanales", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Text("$completedGoals/${goals.size}", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                val bar by animateFloatAsState(goalProgress / 100f, tween(800, easing = FastOutSlowInEasing), label = "goalBar")
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Volt.surface2)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(bar).clip(RoundedCornerShape(4.dp)).background(Volt.accent))
                }
                Spacer(Modifier.height(10.dp))
                if (goals.isEmpty()) {
                    Text("Tus metas de la semana aparecerán aquí.", color = Volt.textDim, fontSize = 12.sp)
                }
                goals.forEach { g ->
                    val bg by animateColorAsState(if (g.done) Volt.accent.copy(alpha = 0.12f) else Volt.surface2.copy(alpha = 0.5f), tween(250), label = "goalBg")
                    Row(
                        Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(16.dp)).background(bg)
                            .clickable {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                scope.launch { com.voltbody.app.util.toggleWeeklyGoal(repo, g.id) }
                            }.padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val fill by animateColorAsState(if (g.done) Volt.accent else Color.Transparent, tween(200), label = "goalChk")
                        Box(Modifier.size(22.dp).clip(RoundedCornerShape(7.dp)).background(fill).border(1.5.dp, if (g.done) Volt.accent else Volt.textDim, RoundedCornerShape(7.dp)), contentAlignment = Alignment.Center) {
                            if (g.done) Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(g.label, color = if (g.done) Volt.text else Volt.textDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
        if (prs.isNotEmpty()) {
            item {
                VoltCard(Modifier.staggeredEntry(10)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.EmojiEvents, null, tint = Volt.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Récords personales (PRs)", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Spacer(Modifier.height(8.dp))
                    prs.forEach { (name, log, date) ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.3f)).padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(name, color = Volt.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                                Text(date, color = Volt.textDim, fontSize = 10.sp)
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("${trimNum(log.weight)} kg", color = Volt.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("× ${log.reps} reps", color = Volt.textDim, fontSize = 10.sp)
                            }
                        }
                    }
                }
            }
        }
        item {
            VoltCard(Modifier.staggeredEntry(11)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.PhotoLibrary, null, tint = Volt.accent)
                    Spacer(Modifier.width(8.dp))
                    Text("Galería de progreso", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.weight(1f))
                    Row(
                        Modifier.clip(RoundedCornerShape(50)).background(Volt.surface2).pressScale { progressPicker.open() }.padding(horizontal = 12.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Add, null, tint = Volt.text, modifier = Modifier.size(15.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Añadir", color = Volt.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
                Spacer(Modifier.height(10.dp))
                if (state.progressPhotos.isEmpty()) {
                    Text("Sube fotografías de control para registrar tus cambios visuales.", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                } else {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        itemsIndexed(state.progressPhotos) { i, ph ->
                            Box(
                                Modifier.staggeredEntry(i, 40).size(width = 96.dp, height = 128.dp).clip(RoundedCornerShape(18.dp)).background(Volt.surface2)
                                    .pressScale { activePhoto = ph.url },
                            ) {
                                DataUrlImage(ph.url, Modifier.fillMaxSize(), reqSize = 300)
                                Text(
                                    ph.date.take(10), color = Color.White, fontSize = 9.sp,
                                    modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).padding(4.dp),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                )
                            }
                        }
                    }
                }
            }
        }

        // ───── Preferencias ─────
        item { GroupLabel("Preferencias de la app", 12) }
        item {
            VoltCard(Modifier.staggeredEntry(13), padding = 0.dp) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.Notifications)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Notificaciones", color = Volt.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text("Recordatorios de comida y entreno", color = Volt.textDim, fontSize = 10.sp)
                    }
                    VoltSwitch(state.notificationsEnabled) { toggleNotifications(it) }
                }
                HorizontalDivider(color = Volt.border)
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Palette)
                        Spacer(Modifier.width(12.dp))
                        Text("Paleta del sistema", color = Volt.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        THEMES.forEach { t ->
                            val sel = state.theme == t.id
                            val border by animateColorAsState(if (sel) t.color else Volt.border, tween(250), label = "themeBorder")
                            Row(
                                Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(16.dp)).background(if (sel) t.color.copy(alpha = 0.14f) else Volt.surface2)
                                    .border(BorderStroke(1.dp, border), RoundedCornerShape(16.dp))
                                    .pressScale {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        repo.update { it.copy(theme = t.id) } // aplica en vivo
                                        scope.launch { repo.saveProfile { it } }
                                    },
                                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(t.color))
                                Spacer(Modifier.width(6.dp))
                                Text(t.label, color = if (sel) Volt.text else Volt.textDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }
                        }
                    }
                }
                HorizontalDivider(color = Volt.border)
                AvatarSliders(p.avatarConfig ?: AvatarConfig(0.5, 0.5)) { cfg ->
                    scope.launch { repo.saveProfile { it.copy(profile = it.profile.copy(avatarConfig = cfg)) } }
                }
                HorizontalDivider(color = Volt.border)
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconBadge(Icons.Filled.CalendarMonth)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Disponibilidad", color = Volt.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        Text(
                            buildString {
                                append(p.schedule.orEmpty())
                                p.weeklySpecialSession?.takeIf { it.enabled }?.let { append("  ·  ${it.activity} ${it.day} (${it.durationMinutes} min)") }
                            },
                            color = Volt.textDim, fontSize = 10.sp,
                        )
                    }
                }
            }
        }

        // ───── Motivación ─────
        item { GroupLabel("Motivación", 14) }
        item {
            VoltCard(Modifier.staggeredEntry(15)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.FormatQuote, null, tint = Volt.accent, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Frase de guerra", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
                Spacer(Modifier.height(10.dp))
                VoltTextField(phrase, { phrase = it.take(240) }, "Escribe tu frase motivadora...", singleLine = false)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Box(
                        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(16.dp)).background(Volt.accent.copy(alpha = if (phrase != state.motivationPhrase) 0.18f else 0.06f))
                            .pressScale {
                                if (phrase != state.motivationPhrase) scope.launch {
                                    repo.saveProfile { it.copy(motivationPhrase = phrase.trim()) }
                                    repo.toast("success", "Frase guardada")
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) { Text("Guardar frase", color = Volt.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                    Row(
                        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(16.dp)).background(Volt.surface2).pressScale { motivationPicker.open() },
                        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.PhotoCamera, null, tint = Volt.text, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Foto inspiración", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                val mp = p.motivationPhoto
                if (!mp.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(20.dp))) {
                        DataUrlImage(mp, Modifier.fillMaxSize(), reqSize = 900)
                        Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                        Text(
                            "“${state.motivationPhrase}”", color = Color.White, fontSize = 12.sp, fontStyle = FontStyle.Italic, fontWeight = FontWeight.Medium,
                            modifier = Modifier.align(Alignment.BottomStart).padding(14.dp),
                        )
                    }
                }
            }
        }

        // ───── Salud / Servidor / Datos ─────
        item { GroupLabel("Salud", 16) }
        item { HealthCard(repo, Modifier.staggeredEntry(17)) }
        item { GroupLabel("Servidor", 18) }
        item { ServerCard(repo, Modifier.staggeredEntry(19)) }
        item { GroupLabel("Sesión", 20) }
        item { DataCard(repo, state, Modifier.staggeredEntry(21)) }
    }

    if (showRank) RankDialog(goalProgress) { showRank = false }
    activePhoto?.let { url ->
        Dialog(onDismissRequest = { activePhoto = null }) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Color.Black)) {
                DataUrlImage(url, Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 560.dp), ContentScale.Fit, reqSize = 1600)
                Box(
                    Modifier.align(Alignment.TopEnd).padding(10.dp).size(38.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.7f)).clickable { activePhoto = null },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Close, "Cerrar foto", tint = Color.White) }
            }
        }
    }
}

@Composable
private fun ScoreBar(label: String, value: Int, index: Int) {
    var started by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { started = true }
    val frac by animateFloatAsState(if (started) value / 100f else 0f, tween(900, delayMillis = 150 + index * 100, easing = FastOutSlowInEasing), label = "score")
    Column(Modifier.padding(vertical = 5.dp)) {
        Row {
            Text(label, color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
            Text("$value%", color = Volt.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(5.dp))
        Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Color.Black.copy(alpha = 0.4f))) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(frac).clip(RoundedCornerShape(4.dp)).background(Volt.accent))
        }
    }
}

@Composable
private fun AvatarSliders(cfg: AvatarConfig, onSave: (AvatarConfig) -> Unit) {
    var muscle by remember(cfg.muscleMass) { mutableFloatStateOf(cfg.muscleMass.toFloat().coerceIn(0f, 1f)) }
    var fat by remember(cfg.bodyFat) { mutableFloatStateOf(cfg.bodyFat.toFloat().coerceIn(0f, 1f)) }
    val colors = SliderDefaults.colors(thumbColor = Volt.accent, activeTrackColor = Volt.accent, inactiveTrackColor = Volt.surface2)
    Column(Modifier.fillMaxWidth().padding(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Accessibility)
            Spacer(Modifier.width(12.dp))
            Text("Avatar corporal", color = Volt.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(8.dp))
        Row { Text("Masa muscular", color = Volt.textDim, fontSize = 11.sp, modifier = Modifier.weight(1f)); Text("${(muscle * 100).toInt()}%", color = Volt.text, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        Slider(muscle, { muscle = it }, onValueChangeFinished = { onSave(AvatarConfig(muscle.toDouble(), fat.toDouble())) }, colors = colors)
        Row { Text("Grasa corporal", color = Volt.textDim, fontSize = 11.sp, modifier = Modifier.weight(1f)); Text("${(fat * 100).toInt()}%", color = Volt.text, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
        Slider(fat, { fat = it }, onValueChangeFinished = { onSave(AvatarConfig(muscle.toDouble(), fat.toDouble())) }, colors = colors)
    }
}

@Composable
private fun RankDialog(consistency: Int, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Volt.surface).border(1.dp, Volt.border, RoundedCornerShape(28.dp)).padding(22.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Shield, null, tint = Volt.accent)
                Spacer(Modifier.width(8.dp))
                Text("Rangos de condición", color = Volt.text, fontWeight = FontWeight.ExtraBold, fontSize = 18.sp)
            }
            Text("Se calcula según el cumplimiento de tus metas semanales.", color = Volt.textDim, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp, bottom = 14.dp))
            RANKS.forEachIndexed { i, r ->
                val current = consistency >= r.min && consistency < r.max
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).staggeredEntry(i, 50).clip(RoundedCornerShape(16.dp))
                        .background(if (current) Volt.accent.copy(alpha = 0.14f) else Volt.surface2.copy(alpha = 0.5f))
                        .border(1.dp, if (current) Volt.accent else Color.Transparent, RoundedCornerShape(16.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${r.emoji} ${r.name}", color = if (current) Volt.text else Volt.textDim, fontWeight = FontWeight.Bold, fontSize = 13.sp, modifier = Modifier.weight(1f))
                    Text(r.range, color = Volt.accent, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(Modifier.height(14.dp))
            PrimaryButton("Cerrar", onClick = onClose)
        }
    }
}
