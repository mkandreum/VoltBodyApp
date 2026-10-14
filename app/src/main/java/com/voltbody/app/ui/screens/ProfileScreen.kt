package com.voltbody.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.AvatarConfig
import com.voltbody.app.data.Repository
import com.voltbody.app.data.WorkoutLog
import com.voltbody.app.ui.*
import com.voltbody.app.ui.screens.profile.*
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.ui.theme.themeLabel
import com.voltbody.app.ui.theme.voltColors
import com.voltbody.app.util.Notifications
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters

private class Rank(val min: Int, val max: Int, val name: String, val emoji: String, val range: String, val color: Color)

private val RANKS = listOf(
    Rank(0, 30, "Recluta de Bronce", "🛡️", "< 30%", Color(0xFFF59E0B)),
    Rank(30, 60, "Guerrero de Acero", "⚔️", "30-59%", Color(0xFFCBD5E1)),
    Rank(60, 85, "Campeón del Neón", "⚡", "60-84%", Color(0xFF22D3EE)),
    Rank(85, 101, "Cyborg Supremo", "🤖", "≥ 85%", Color(0xFFFFB020)),
)

/** ids del servidor (no cambian): verde-negro = Lima, aguamarina-negro = Aguamarina, ocaso-negro = Ocaso. */
private val THEME_IDS = listOf("verde-negro", "aguamarina-negro", "ocaso-negro")

@Composable
private fun GroupLabel(text: String, index: Int) {
    Text(
        text.uppercase(), color = Volt.textDim, style = VoltType.label,
        modifier = Modifier.staggeredEntry(index).padding(start = 8.dp, top = 12.dp),
    )
}

@Composable
private fun IconBadge(icon: ImageVector, tint: Color = Volt.accent) {
    Box(Modifier.size(40.dp).clip(CircleShape).background(tint.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

@Composable
private fun VoltSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    Switch(
        checked = checked, onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = Volt.accent, checkedThumbColor = Volt.onAccent, checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = Volt.surface2, uncheckedBorderColor = Volt.border, uncheckedThumbColor = Volt.textDim,
        ),
    )
}

@Composable
private fun PillAction(text: String, modifier: Modifier = Modifier, filled: Boolean = false, icon: ImageVector? = null, onClick: () -> Unit) {
    val bg = if (filled) Volt.accent else Volt.surface2
    Row(
        modifier.height(48.dp).clip(CircleShape).background(bg).pressScale(onClick).padding(horizontal = 18.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) { Icon(icon, null, tint = if (filled) Volt.onAccent else Volt.text, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
        Text(text, color = if (filled) Volt.onAccent else Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.Bold), maxLines = 1)
    }
}

@Composable
fun ProfileScreen(repo: Repository) {
    val state by repo.state.collectAsStateWithLifecycle()
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

    // ── calculos (memoizados) ──
    val today = remember { LocalDate.now() }
    val todayKey = today.toString()
    val weekStart = remember { today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) }
    val nextWeekStart = remember { weekStart.plusDays(7) }
    val weightLogs = remember(state.weightLogs) { state.weightLogs.sortedBy { it.date } }
    val alreadyLoggedThisWeek = remember(weightLogs) { weightLogs.any { it.date >= weekStart.toString() && it.date < nextWeekStart.toString() } }
    val lastWeight = weightLogs.lastOrNull()

    val weeklyChart = remember(weightLogs) {
        val byWeek = weightLogs.mapNotNull { l ->
            runCatching { LocalDate.parse(l.date.take(10)).with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)) to l.weight }.getOrNull()
        }.toMap()
        (0 until 8).mapNotNull { i -> byWeek[weekStart.minusWeeks((7 - i).toLong())]?.toFloat() }
    }

    val prs = remember(state.logs, state.routine) {
        val names = state.routine.flatMap { it.exercises }.associate { it.id to it.name }
        val best = HashMap<String, WorkoutLog>()
        state.logs.forEach { l -> val e = best[l.exerciseId]; if (e == null || l.weight > e.weight) best[l.exerciseId] = l }
        best.values.filter { it.weight > 0 }.sortedByDescending { it.weight }.take(10).map { Triple(names[it.exerciseId] ?: it.exerciseId, it, it.date.take(10)) }
    }

    val goals = state.weeklyGoals
    val completedGoals = goals.count { it.done }
    val goalProgress = Math.round(completedGoals * 100f / goals.size.coerceAtLeast(1))
    val strengthScore = (state.logs.size * 4).coerceAtMost(100)
    val energyScore = (weightLogs.size * 18).coerceIn(30, 100)
    val rank = RANKS.firstOrNull { goalProgress >= it.min && goalProgress < it.max } ?: RANKS[0]

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        MeshBackground(Modifier.fillMaxSize(), intensity = 0.4f)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ───── Cabecera heroe ─────
            item(key = "hero", contentType = "hero") {
                BentoCard(Modifier.staggeredEntry(0), hero = true, padding = 24.dp) {
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(Modifier.size(132.dp)) {
                            Box(
                                Modifier.fillMaxSize().clip(CircleShape).background(Volt.gradient).padding(4.dp).clip(CircleShape)
                                    .background(Volt.surface2).clickable { profilePicker.open() },
                                contentAlignment = Alignment.Center,
                            ) {
                                val photo = p.profilePhoto
                                if (!photo.isNullOrBlank()) DataUrlImage(photo, Modifier.fillMaxSize(), reqSize = 400)
                                else Icon(Icons.Filled.Person, null, tint = Volt.accent, modifier = Modifier.size(60.dp))
                            }
                            Box(
                                Modifier.align(Alignment.BottomEnd).size(44.dp).clip(CircleShape).background(Volt.gradient).pressScale { profilePicker.open() },
                                contentAlignment = Alignment.Center,
                            ) { Icon(Icons.Filled.PhotoCamera, "Cambiar foto de perfil", tint = Volt.onAccent, modifier = Modifier.size(20.dp)) }
                        }
                        Spacer(Modifier.height(16.dp))
                        Text(p.name ?: state.user?.name ?: "Usuario Volt", color = Volt.text, style = VoltType.h1, maxLines = 1, textAlign = TextAlign.Center)
                        if (!p.goal.isNullOrBlank()) {
                            Text(p.goal.orEmpty(), color = Volt.accent, style = VoltType.body.copy(fontWeight = FontWeight.Bold), maxLines = 2, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 2.dp))
                        }
                        Text(
                            listOfNotNull(p.gender, p.age?.let { "$it años" }).joinToString(" · "),
                            color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(top = 2.dp),
                        )
                        Spacer(Modifier.height(20.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            HeroStat("Peso", p.weight?.let { trimNum(it) } ?: "--", "kg", Modifier.weight(1f))
                            HeroStat("Estatura", p.height?.let { trimNum(it) } ?: "--", "cm", Modifier.weight(1f))
                            HeroStat("Racha metas", "$goalProgress", "%", Modifier.weight(1f))
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(
                            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Volt.surface2.copy(alpha = 0.7f)).pressScale { showRank = true }.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconBadge(Icons.Filled.Shield, rank.color)
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("RANGO ACTUAL", color = Volt.textDim, style = VoltType.label)
                                Text("${rank.emoji} ${rank.name}", color = rank.color, style = VoltType.title)
                            }
                            Icon(Icons.Filled.ChevronRight, null, tint = Volt.textDim)
                        }
                    }
                }
            }

            // ───── Biometría ─────
            item(key = "gBio", contentType = "label") { GroupLabel("Biometría & rendimiento", 1) }
            item(key = "bio", contentType = "card") {
                BentoCard(Modifier.staggeredEntry(2), padding = 8.dp) {
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
                            Spacer(Modifier.width(14.dp))
                            Text(label, color = Volt.textDim, style = VoltType.body, modifier = Modifier.weight(1f))
                            Text(value, color = Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.ExtraBold))
                        }
                        if (i < rows.lastIndex) HorizontalDivider(color = Volt.border, modifier = Modifier.padding(horizontal = 12.dp))
                    }
                }
            }
            item(key = "personal", contentType = "card") { PersonalDataCard(repo, p, state.notificationsEnabled, Modifier.staggeredEntry(3)) }
            item(key = "score", contentType = "card") {
                BentoCard(Modifier.staggeredEntry(4)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Score de rendimiento", color = Volt.text, style = VoltType.h2, modifier = Modifier.weight(1f))
                        Text("ALGORITMO VOLT", color = Volt.textDim, style = VoltType.label)
                    }
                    Spacer(Modifier.height(10.dp))
                    listOf(Triple("💪 Fuerza", strengthScore, 0), Triple("🔥 Consistencia", goalProgress, 1), Triple("⚡ Energía", energyScore, 2)).forEach { (l, v, i) ->
                        ScoreBar(l, v, i)
                    }
                }
            }

            // ───── Peso ─────
            item(key = "gWeight", contentType = "label") { GroupLabel("Seguimiento de peso", 5) }
            item(key = "weight", contentType = "card") {
                BentoCard(Modifier.staggeredEntry(6)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.TrendingUp)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Evolución semanal", color = Volt.text, style = VoltType.title)
                            Text(
                                if (alreadyLoggedThisWeek) "Registrado: ${lastWeight?.let { trimNum(it.weight) }} kg esta semana" else "Registra tu peso semanal para el cálculo de déficit",
                                color = Volt.textDim, style = VoltType.body,
                            )
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    val v = weightInput.toDoubleOrNull()
                    val valid = v != null && v in 20.0..400.0
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        VoltTextField(weightInput, { weightInput = filterDecimal(it) }, "Ej. ${p.weight?.let { trimNum(it) } ?: "70"} kg", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                        val enabled = valid && !alreadyLoggedThisWeek
                        Box(
                            Modifier.height(56.dp).clip(CircleShape).background(if (enabled) Volt.accent else Volt.surface2)
                                .let {
                                    if (enabled) it.pressScale {
                                        scope.launch {
                                            repo.addWeight(todayKey, v!!)
                                            repo.toast("success", "Peso registrado: ${trimNum(v)} kg")
                                            weightInput = ""
                                        }
                                    } else it
                                }.padding(horizontal = 20.dp),
                            contentAlignment = Alignment.Center,
                        ) { Text("Registrar", color = if (enabled) Volt.onAccent else Volt.textDim, style = VoltType.body.copy(fontWeight = FontWeight.ExtraBold)) }
                    }
                    if (weightInput.isNotEmpty() && !valid) {
                        Text("El peso debe estar entre 20 y 400 kg.", color = Volt.danger, style = VoltType.body, modifier = Modifier.padding(top = 6.dp))
                    }
                    Spacer(Modifier.height(14.dp))
                    if (weeklyChart.size >= 2) {
                        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.3f)).padding(14.dp)) {
                            Sparkline(weeklyChart, Modifier.fillMaxWidth().height(90.dp), color = Volt.accent)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                "Últimas 8 semanas · mín ${"%.1f".format(weeklyChart.min())} kg · máx ${"%.1f".format(weeklyChart.max())} kg",
                                color = Volt.textDim, style = VoltType.body,
                            )
                        }
                    } else {
                        Text("Sin suficientes registros. Anota tu peso cada semana para ver la gráfica.", color = Volt.textDim, style = VoltType.body, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp))
                    }
                    if (weightLogs.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        weightLogs.asReversed().take(4).forEach { l ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(16.dp)).background(Color.Black.copy(alpha = 0.25f)).padding(horizontal = 14.dp, vertical = 10.dp),
                            ) {
                                Text(l.date.take(10), color = Volt.textDim, style = VoltType.body, modifier = Modifier.weight(1f))
                                Text("${trimNum(l.weight)} kg", color = Volt.accent, style = VoltType.body.copy(fontWeight = FontWeight.ExtraBold))
                            }
                        }
                    }
                }
            }

            // ───── Objetivos y récords ─────
            item(key = "gGoals", contentType = "label") { GroupLabel("Objetivos & récords", 7) }
            item(key = "goals", contentType = "card") {
                BentoCard(Modifier.staggeredEntry(8)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.TrackChanges)
                        Spacer(Modifier.width(12.dp))
                        Text("Metas semanales", color = Volt.text, style = VoltType.h2, modifier = Modifier.weight(1f))
                        Text("$completedGoals/${goals.size}", color = Volt.accent, style = VoltType.numberS)
                    }
                    Spacer(Modifier.height(14.dp))
                    GoalBar(goalProgress / 100f)
                    Spacer(Modifier.height(12.dp))
                    if (goals.isEmpty()) {
                        Text("Tus metas de la semana aparecerán aquí.", color = Volt.textDim, style = VoltType.body)
                    }
                    goals.forEach { g ->
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(20.dp))
                                .background(if (g.done) Volt.accent.copy(alpha = 0.12f) else Volt.surface2.copy(alpha = 0.5f))
                                .clickable {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    scope.launch { com.voltbody.app.util.toggleWeeklyGoal(repo, g.id) }
                                }.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
                                Box(Modifier.size(26.dp).border(2.dp, Volt.textDim.copy(alpha = 0.55f), CircleShape))
                                AnimatedCheck(g.done, Modifier.size(28.dp))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(g.label, color = if (g.done) Volt.text else Volt.textDim, style = VoltType.body.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }
            }
            if (prs.isNotEmpty()) {
                item(key = "prs", contentType = "card") {
                    BentoCard(Modifier.staggeredEntry(9)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconBadge(Icons.Filled.EmojiEvents)
                            Spacer(Modifier.width(12.dp))
                            Text("Récords personales", color = Volt.text, style = VoltType.h2)
                        }
                        Spacer(Modifier.height(10.dp))
                        prs.forEach { (name, log, date) ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 3.dp).clip(RoundedCornerShape(18.dp)).background(Color.Black.copy(alpha = 0.3f)).padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(name, color = Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.Bold), maxLines = 1)
                                    Text(date, color = Volt.textDim, style = VoltType.body)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${trimNum(log.weight)} kg", color = Volt.accent, style = VoltType.numberS)
                                    Text("× ${log.reps} reps", color = Volt.textDim, style = VoltType.body)
                                }
                            }
                        }
                    }
                }
            }
            item(key = "gallery", contentType = "card") {
                BentoCard(Modifier.staggeredEntry(10)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.PhotoLibrary)
                        Spacer(Modifier.width(12.dp))
                        Text("Galería de progreso", color = Volt.text, style = VoltType.h2, modifier = Modifier.weight(1f))
                        PillAction("Añadir", icon = Icons.Filled.Add) { progressPicker.open() }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (state.progressPhotos.isEmpty()) {
                        Text("Sube fotografías de control para registrar tus cambios visuales.", color = Volt.textDim, style = VoltType.body, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
                    } else {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            itemsIndexed(state.progressPhotos, key = { i, ph -> "$i-${ph.date}" }) { i, ph ->
                                Box(
                                    Modifier.staggeredEntry(i, 40).size(width = 104.dp, height = 140.dp).clip(RoundedCornerShape(24.dp)).background(Volt.surface2)
                                        .pressScale { activePhoto = ph.url },
                                ) {
                                    DataUrlImage(ph.url, Modifier.fillMaxSize(), reqSize = 300)
                                    Text(
                                        ph.date.take(10), color = Color.White, style = VoltType.label,
                                        modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Color.Black.copy(alpha = 0.55f)).padding(6.dp),
                                        textAlign = TextAlign.Center,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ───── Apariencia ─────
            item(key = "gTheme", contentType = "label") { GroupLabel("Apariencia", 11) }
            item(key = "theme", contentType = "card") {
                BentoCard(Modifier.staggeredEntry(12)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Palette)
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Paleta del sistema", color = Volt.text, style = VoltType.title)
                            Text("Cambia el color de toda la app al instante", color = Volt.textDim, style = VoltType.body)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        THEME_IDS.forEach { id ->
                            ThemeSwatch(id, state.theme == id) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                repo.update { it.copy(theme = id) } // aplica en vivo
                                scope.launch { repo.saveProfile { it } }
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = Volt.border)
                    AvatarSliders(p.avatarConfig ?: AvatarConfig(0.5, 0.5)) { cfg ->
                        scope.launch { repo.saveProfile { it.copy(profile = it.profile.copy(avatarConfig = cfg)) } }
                    }
                }
            }

            // ───── Preferencias ─────
            item(key = "gPrefs", contentType = "label") { GroupLabel("Preferencias de la app", 13) }
            item(key = "prefs", contentType = "card") {
                BentoCard(Modifier.staggeredEntry(14), padding = 8.dp) {
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.Notifications)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Notificaciones", color = Volt.text, style = VoltType.title)
                            Text("Recordatorios de comida y entreno", color = Volt.textDim, style = VoltType.body)
                        }
                        VoltSwitch(state.notificationsEnabled) { toggleNotifications(it) }
                    }
                    HorizontalDivider(color = Volt.border, modifier = Modifier.padding(horizontal = 12.dp))
                    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.CalendarMonth)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text("Disponibilidad", color = Volt.text, style = VoltType.title)
                            Text(
                                buildString {
                                    append(p.schedule.orEmpty())
                                    p.weeklySpecialSession?.takeIf { it.enabled }?.let { append("  ·  ${it.activity} ${it.day} (${it.durationMinutes} min)") }
                                },
                                color = Volt.textDim, style = VoltType.body,
                            )
                        }
                    }
                }
            }

            // ───── Motivación ─────
            item(key = "gMotiv", contentType = "label") { GroupLabel("Motivación", 15) }
            item(key = "motiv", contentType = "card") {
                BentoCard(Modifier.staggeredEntry(16)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconBadge(Icons.Filled.FormatQuote)
                        Spacer(Modifier.width(12.dp))
                        Text("Frase de guerra", color = Volt.text, style = VoltType.h2)
                    }
                    Spacer(Modifier.height(14.dp))
                    VoltTextField(phrase, { phrase = it.take(240) }, "Escribe tu frase motivadora...", singleLine = false)
                    Spacer(Modifier.height(10.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PillAction("Guardar frase", Modifier.weight(1f), filled = phrase != state.motivationPhrase) {
                            if (phrase != state.motivationPhrase) scope.launch {
                                repo.saveProfile { it.copy(motivationPhrase = phrase.trim()) }
                                repo.toast("success", "Frase guardada")
                            }
                        }
                        PillAction("Foto inspiración", Modifier.weight(1f), icon = Icons.Filled.PhotoCamera) { motivationPicker.open() }
                    }
                    val mp = p.motivationPhoto
                    if (!mp.isNullOrBlank()) {
                        Spacer(Modifier.height(14.dp))
                        Box(Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(24.dp))) {
                            DataUrlImage(mp, Modifier.fillMaxSize(), reqSize = 900)
                            Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f)))))
                            Text(
                                "“${state.motivationPhrase}”", color = Color.White, style = VoltType.body.copy(fontStyle = FontStyle.Italic, fontWeight = FontWeight.Bold),
                                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                            )
                        }
                    }
                }
            }

            // ───── Salud / Servidor / Datos ─────
            item(key = "gHealth", contentType = "label") { GroupLabel("Salud", 17) }
            item(key = "health", contentType = "card") { HealthCard(repo, Modifier.staggeredEntry(18)) }
            item(key = "gServer", contentType = "label") { GroupLabel("Servidor", 19) }
            item(key = "server", contentType = "card") { ServerCard(repo, Modifier.staggeredEntry(20)) }
            item(key = "gData", contentType = "label") { GroupLabel("Datos y sesión", 21) }
            item(key = "data", contentType = "card") { DataCard(repo, state, Modifier.staggeredEntry(22)) }
        }
    }

    if (showRank) RankDialog(goalProgress) { showRank = false }
    activePhoto?.let { url ->
        Dialog(onDismissRequest = { activePhoto = null }) {
            Box(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Color.Black)) {
                DataUrlImage(url, Modifier.fillMaxWidth().heightIn(min = 240.dp, max = 560.dp), ContentScale.Fit, reqSize = 1600)
                Box(
                    Modifier.align(Alignment.TopEnd).padding(10.dp).size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.7f)).clickable { activePhoto = null },
                    contentAlignment = Alignment.Center,
                ) { Icon(Icons.Filled.Close, "Cerrar foto", tint = Color.White) }
            }
        }
    }
}

@Composable
private fun HeroStat(label: String, value: String, unit: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(Volt.surface2.copy(alpha = 0.7f)).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.Bottom) {
            GradientText(value, style = VoltType.numberM)
            Text(unit, color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(start = 2.dp, bottom = 3.dp))
        }
        Text(label.uppercase(), color = Volt.textDim, style = VoltType.label, maxLines = 1)
    }
}

/** Muestra de tema: circulo con el degradado del tema, check animado al elegirlo y nombre debajo. */
@Composable
private fun ThemeSwatch(id: String, selected: Boolean, onClick: () -> Unit) {
    val c = remember(id) { voltColors(id) }
    val sel = animateFloatAsState(if (selected) 1f else 0f, tween(300), label = "swatchSel")
    val textDim = Volt.textDim
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.pressScale(onClick).padding(4.dp)) {
        Box(
            Modifier.size(72.dp).graphicsLayer { val s = 1f + 0.08f * sel.value; scaleX = s; scaleY = s }
                .drawBehind {
                    drawCircle(Brush.linearGradient(listOf(c.accent, c.accent2)))
                    if (sel.value > 0.01f) drawCircle(Color.White.copy(alpha = 0.9f * sel.value), radius = size.minDimension / 2f + 4.dp.toPx(), style = androidx.compose.ui.graphics.drawscope.Stroke(2.dp.toPx()))
                },
            contentAlignment = Alignment.Center,
        ) { AnimatedCheck(selected, Modifier.size(32.dp)) }
        Spacer(Modifier.height(10.dp))
        Text(themeLabel(id), color = if (selected) Volt.text else textDim, style = VoltType.body.copy(fontWeight = FontWeight.Bold))
    }
}

@Composable
private fun GoalBar(target: Float) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(target) { a.animateTo(target.coerceIn(0f, 1f), tween(800, easing = EaseOutExpo)) }
    val g = Volt.gradient
    val track = Volt.surface2
    Box(
        Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).drawBehind {
            drawRect(track)
            drawRoundRect(g, Offset.Zero, Size(size.width * a.value, size.height), CornerRadius(size.height / 2))
        },
    )
}

@Composable
private fun ScoreBar(label: String, value: Int, index: Int) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(value) {
        kotlinx.coroutines.delay(150L + index * 100L)
        a.animateTo(value / 100f, tween(900, easing = EaseOutExpo))
    }
    val g = Volt.gradient
    Column(Modifier.padding(vertical = 6.dp)) {
        Row {
            Text(label, color = Volt.textDim, style = VoltType.body, modifier = Modifier.weight(1f))
            Text("$value%", color = Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.ExtraBold))
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier.fillMaxWidth().height(8.dp).clip(CircleShape).drawBehind {
                drawRect(Color.White.copy(alpha = 0.07f))
                drawRoundRect(g, Offset.Zero, Size(size.width * a.value, size.height), CornerRadius(size.height / 2))
            },
        )
    }
}

@Composable
private fun AvatarSliders(cfg: AvatarConfig, onSave: (AvatarConfig) -> Unit) {
    var muscle by remember(cfg.muscleMass) { mutableFloatStateOf(cfg.muscleMass.toFloat().coerceIn(0f, 1f)) }
    var fat by remember(cfg.bodyFat) { mutableFloatStateOf(cfg.bodyFat.toFloat().coerceIn(0f, 1f)) }
    val colors = SliderDefaults.colors(thumbColor = Volt.accent, activeTrackColor = Volt.accent, inactiveTrackColor = Volt.surface2)
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconBadge(Icons.Filled.Accessibility)
            Spacer(Modifier.width(12.dp))
            Text("Avatar corporal", color = Volt.text, style = VoltType.title)
        }
        Spacer(Modifier.height(8.dp))
        Row { Text("Masa muscular", color = Volt.textDim, style = VoltType.body, modifier = Modifier.weight(1f)); Text("${(muscle * 100).toInt()}%", color = Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.ExtraBold)) }
        Slider(muscle, { muscle = it }, onValueChangeFinished = { onSave(AvatarConfig(muscle.toDouble(), fat.toDouble())) }, colors = colors)
        Row { Text("Grasa corporal", color = Volt.textDim, style = VoltType.body, modifier = Modifier.weight(1f)); Text("${(fat * 100).toInt()}%", color = Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.ExtraBold)) }
        Slider(fat, { fat = it }, onValueChangeFinished = { onSave(AvatarConfig(muscle.toDouble(), fat.toDouble())) }, colors = colors)
    }
}

@Composable
private fun RankDialog(consistency: Int, onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(36.dp)).background(Volt.surface).border(1.dp, Volt.border, RoundedCornerShape(36.dp)).padding(24.dp)) {
            Text("RANGOS DE CONDICIÓN", color = Volt.accent, style = VoltType.label)
            Text("Tu nivel", color = Volt.text, style = VoltType.h1)
            Text("Se calcula según el cumplimiento de tus metas semanales.", color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(top = 4.dp, bottom = 16.dp))
            RANKS.forEachIndexed { i, r ->
                val current = consistency >= r.min && consistency < r.max
                Row(
                    Modifier.fillMaxWidth().padding(vertical = 4.dp).staggeredEntry(i, 50).clip(RoundedCornerShape(20.dp))
                        .background(if (current) Volt.accent.copy(alpha = 0.14f) else Volt.surface2.copy(alpha = 0.5f))
                        .border(1.dp, if (current) Volt.accent else Color.Transparent, RoundedCornerShape(20.dp)).padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("${r.emoji} ${r.name}", color = if (current) Volt.text else Volt.textDim, style = VoltType.body.copy(fontWeight = FontWeight.Bold), modifier = Modifier.weight(1f))
                    Text(r.range, color = Volt.accent, style = VoltType.label)
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Cerrar", onClick = onClose)
        }
    }
}
