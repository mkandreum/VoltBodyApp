package com.voltbody.app.ui.v3

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.FoodPreferences
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.screens.diet.FoodPreferencesCard
import com.voltbody.app.ui.screens.diet.RegenerateCard
import com.voltbody.app.ui.screens.diet.SpecialDishCard
import com.voltbody.app.ui.screens.home.AchievementsCard
import com.voltbody.app.ui.screens.home.DataImage
import com.voltbody.app.ui.screens.home.HabitsCard
import com.voltbody.app.ui.screens.home.HeartRateCard
import com.voltbody.app.ui.screens.home.PhotosCard
import com.voltbody.app.ui.screens.home.RecoveryCard
import com.voltbody.app.ui.screens.home.ReportCard
import com.voltbody.app.ui.screens.home.WeightCard
import com.voltbody.app.ui.screens.home.rememberHealthState
import com.voltbody.app.ui.screens.profile.DataCard
import com.voltbody.app.ui.screens.profile.HealthCard
import com.voltbody.app.ui.screens.profile.PersonalDataCard
import com.voltbody.app.ui.screens.profile.ServerCard
import com.voltbody.app.ui.screens.profile.rememberPhotoPicker
import com.voltbody.app.ui.screens.workout.defaultExerciseLibrary
import com.voltbody.app.ui.screens.workout.toExercise
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.themeLabel
import com.voltbody.app.ui.theme.voltColors
import com.voltbody.app.util.Haptics
import com.voltbody.app.util.Notifications
import com.voltbody.app.util.computeFatigueIndex
import com.voltbody.app.util.computeSmartStreak
import com.voltbody.app.util.fatigueStatusColor
import com.voltbody.app.util.fatigueStatusLabel
import kotlinx.coroutines.launch

@Composable
fun ProfileV3(repo: Repository) {
    val st by repo.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val p = st.profile
    val streak = remember(st.logs, st.routine) { computeSmartStreak(st.logs, st.routine) }
    val picker = rememberPhotoPicker("Foto de perfil") { url -> repo.saveProfile(withPhotos = true) { it.copy(profile = it.profile.copy(profilePhoto = url)) } }

    Screen {
        Text("Perfil", style = T.screen, color = Volt.text, modifier = Modifier.enter(0))
        // Cabecera
        Card(Modifier.fillMaxWidth().enter(1), padding = 18.dp) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(72.dp).clip(CircleShape).background(Volt.surface2).border(2.dp, Volt.accent, CircleShape).tap(0.92f) { picker.open() }, contentAlignment = Alignment.Center) {
                    if (p.profilePhoto != null) DataImage(p.profilePhoto, Modifier.fillMaxSize().clip(CircleShape), maxPx = 240)
                    else Text((p.name ?: st.user?.name ?: "A").take(1).uppercase(), style = T.screen, color = Volt.accent)
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    FitText(p.name ?: st.user?.name ?: "Atleta", T.section.copy(fontSize = 19.sp), Volt.text, maxLines = 1)
                    FitText(p.goal ?: "Sin objetivo", T.caption, Volt.accent, maxLines = 2, minScale = 0.85f)
                    Text(st.user?.email ?: "", style = T.caption, color = Volt.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconCircle(Icons.Outlined.Edit, size = 40.dp, desc = "Editar datos") { nav.push(Route.Section("personal")) }
            }
            Spacer(Modifier.height(16.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(p.weight?.let { trim(it) } ?: "—", "kg", Modifier.weight(1f))
                Stat(p.height?.let { trim(it) } ?: "—", "cm", Modifier.weight(1f))
                Stat("$streak", "días racha", Modifier.weight(1f))
            }
        }

        Text("TU PROGRESO", style = T.label, color = Volt.textDim, modifier = Modifier.padding(start = 6.dp, top = 4.dp).enter(2))
        Row(Modifier.fillMaxWidth().enter(2), horizontalArrangement = Arrangement.spacedBy(K.gap)) {
            ArtTile("📊", "Peso", st.weightLogs.lastOrNull()?.let { "${trim(it.weight)} kg" } ?: "Registrar", ArtPalette.ice, 1, Modifier.weight(1f)) { nav.push(Route.Section("weight")) }
            ArtTile("🏆", "Logros", "${st.achievements.size} desbloqueados", ArtPalette.ember, 2, Modifier.weight(1f)) { nav.push(Route.Section("achievements")) }
        }
        Row(Modifier.fillMaxWidth().enter(3), horizontalArrangement = Arrangement.spacedBy(K.gap)) {
            ArtTile("📸", "Fotos", "${st.progressPhotos.size} fotos", ArtPalette.violet, 3, Modifier.weight(1f)) { nav.push(Route.Section("photos")) }
            ArtTile("✅", "Hábitos", "${st.weeklyGoals.count { it.done }}/${st.weeklyGoals.size} esta semana", ArtPalette.mint, 4, Modifier.weight(1f)) { nav.push(Route.Section("habits")) }
        }
        Row(Modifier.fillMaxWidth().enter(4), horizontalArrangement = Arrangement.spacedBy(K.gap)) {
            ArtTile("😴", "Recovery", "Sueño y HRV", ArtPalette.ice, 5, Modifier.weight(1f)) { nav.push(Route.Section("recovery")) }
            ArtTile("✨", "Informe IA", "Tu evolución", ArtPalette.mint, 6, Modifier.weight(1f)) { nav.push(Route.Section("report")) }
        }
        MenuGroup("Plan", 5) {
            MenuRow(Icons.Outlined.Person, "Datos personales", "Edad, medidas, horarios") { nav.push(Route.Section("personal")) }
            MenuRow(Icons.Outlined.Restaurant, "Plan de comidas", "Preferencias, plato especial, regenerar") { nav.push(Route.Section("diet")) }
            MenuRow(Icons.Outlined.FitnessCenter, "Rutina personal", "${st.customWorkout.size} ejercicios") { nav.push(Route.Section("library")) }
        }
        MenuGroup("Dispositivos y app", 6) {
            MenuRow(Icons.Outlined.FavoriteBorder, "Salud y sensores", "Health Connect · Samsung Health · pulsómetro") { nav.push(Route.Section("health")) }
            MenuRow(Icons.Outlined.Notifications, "Notificaciones", if (st.notificationsEnabled) "Activadas" else "Desactivadas") { nav.push(Route.Section("notifications")) }
            MenuRow(Icons.Outlined.Palette, "Tema", themeLabel(st.theme)) { nav.push(Route.Section("theme")) }
            MenuRow(Icons.Outlined.Dns, "Servidor", repo.serverUrl().removePrefix("https://")) { nav.push(Route.Section("server")) }
            MenuRow(Icons.Outlined.Storage, "Mis datos", "Exportar o resetear") { nav.push(Route.Section("data")) }
        }
        Card(Modifier.fillMaxWidth().enter(7), padding = 6.dp) {
            MenuRow(Icons.AutoMirrored.Outlined.Logout, "Cerrar sesión", tint = Volt.danger) { Haptics.warning(ctx); repo.logout() }
        }
    }
}

/** Tarjeta bento con arte de vidrio estriado (referencia del usuario): emoji, titulo grande y dato. */
@Composable
private fun ArtTile(emoji: String, title: String, value: String, palette: List<Color>, seed: Int, modifier: Modifier, onClick: () -> Unit) {
    Box(modifier.height(132.dp).clip(RoundedCornerShape(26.dp)).tap(0.97f, onClick = onClick)) {
        FlutedArt(Modifier.matchParentSize(), palette, seed = seed, stripe = 11.dp)
        Text(emoji, fontSize = 22.sp, modifier = Modifier.padding(14.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
            FitText(title, T.display.copy(fontSize = 18.sp), Color.White, maxLines = 1)
            Text(value, style = T.caption, color = Color.White.copy(alpha = 0.85f), maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(Volt.surface2).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = T.value.copy(fontSize = 18.sp), color = Volt.text, maxLines = 1)
        Text(label, style = T.caption, color = Volt.textDim, maxLines = 1)
    }
}

@Composable
private fun MenuGroup(title: String, index: Int, content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().enter(index)) {
        Text(title.uppercase(), style = T.label, color = Volt.textDim, modifier = Modifier.padding(start = 6.dp, bottom = 6.dp, top = 4.dp))
        Card(Modifier.fillMaxWidth(), padding = 6.dp, content = content)
    }
}

// ─────────────────────────────── Pantallas de seccion ───────────────────────────────

@Composable
private fun SectionScreen(title: String, content: @Composable ColumnScope.() -> Unit) {
    val nav = LocalNav.current
    Column(
        Modifier.fillMaxSize().background(Volt.bg).verticalScroll(rememberScrollState())
            .statusBarsPadding().navigationBarsPadding().padding(horizontal = K.pad).padding(top = 8.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(K.gap),
    ) {
        TopBar(title, { nav.pop() })
        content()
    }
}

@Composable
fun SectionHost(repo: Repository, id: String) {
    val st by repo.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val nav = LocalNav.current
    val ctx = LocalContext.current
    when (id) {
        "personal" -> SectionScreen("Datos personales") { PersonalDataCard(repo, st.profile, st.notificationsEnabled) }
        "weight" -> SectionScreen("Peso") { WeightCard(repo, st.weightLogs, scope) }
        "photos" -> SectionScreen("Fotos de progreso") { PhotosCard(repo, st.progressPhotos, scope) }
        "achievements" -> SectionScreen("Logros") { AchievementsCard(st.achievements) }
        "habits" -> SectionScreen("Hábitos de la semana") {
            val streak = remember(st.logs, st.routine) { computeSmartStreak(st.logs, st.routine) }
            Card(Modifier.fillMaxWidth()) { ValueUnit("$streak", "días de racha", style = T.hero) }
            HabitsCard(repo, st.weeklyGoals, scope)
        }
        "recovery" -> SectionScreen("Recuperación") {
            val health = rememberHealthState(repo)
            val u = rememberHomeUi(repo)
            if (u != null) RecoveryCard(repo, health, u.todayRecovery, u.advice, u.recoveryScore, scope)
        }
        "report" -> SectionScreen("Informe IA") { ReportCard(repo) }
        "health" -> SectionScreen("Salud y sensores") {
            HealthCard(repo)
            HeartRateCard()
        }
        "notifications" -> SectionScreen("Notificaciones") { NotificationsSection(repo) }
        "theme" -> SectionScreen("Tema") {
            listOf("verde-negro", "aguamarina-negro", "ocaso-negro").forEach { t ->
                val c = voltColors(t)
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(K.radius)).background(Volt.surface)
                        .tap(0.98f) { scope.launch { repo.saveProfile { it.copy(theme = t) } } }.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(36.dp).clip(CircleShape).background(c.accent))
                    Spacer(Modifier.width(14.dp))
                    Text(themeLabel(t), style = T.section, color = Volt.text, modifier = Modifier.weight(1f))
                    if (st.theme == t) Icon(Icons.Filled.Check, "Activo", tint = Volt.accent)
                }
            }
        }
        "server" -> SectionScreen("Servidor") { ServerCard(repo) }
        "data" -> SectionScreen("Mis datos") { DataCard(repo, st) }
        "diet" -> SectionScreen("Plan de comidas") {
            st.diet?.let { SpecialDishCard(repo, it, st.profile.specialDish) }
            FoodPreferencesCard(repo, st.profile.foodPreferences ?: FoodPreferences())
            RegenerateCard(repo)
        }
        "fatigue" -> SectionScreen("Fatiga muscular") {
            val entries = remember(st.logs, st.routine, st.customWorkout) { computeFatigueIndex(st.logs, st.routine, extraExercises = st.customWorkout) }
            if (entries.isEmpty()) Card(Modifier.fillMaxWidth()) { Text("Registra series esta semana para ver tu volumen por grupo muscular.", style = T.body, color = Volt.textDim) }
            entries.forEach { e ->
                Card(Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(e.muscleGroup, style = T.cardTitle, color = Volt.text, modifier = Modifier.weight(1f))
                        Text(fatigueStatusLabel(e.status), style = T.caption, color = fatigueStatusColor(e.status))
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Volt.surface2)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth((e.percent / 100f).coerceIn(0.02f, 1f)).clip(RoundedCornerShape(4.dp)).background(fatigueStatusColor(e.status)))
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("${e.weeklyVolume} de ${e.mrv} series semanales (${e.percent}%)", style = T.caption, color = Volt.textDim)
                }
            }
        }
        "library" -> SectionScreen("Rutina personal") {
            val custom = st.customWorkout
            if (custom.isNotEmpty()) LimeButton("Entrenar rutina personal", { nav.replace(Route.Session(-1, 0, custom = true)) }, icon = Icons.Filled.PlayArrow)
            custom.forEach { ex ->
                Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Volt.surface).padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(ex.name, style = T.cardTitle, color = Volt.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(setsLabel(ex) + " · " + ex.muscleGroup, style = T.caption, color = Volt.textDim, maxLines = 1)
                    }
                    IconCircle(Icons.Filled.Close, size = 34.dp, desc = "Quitar") { scope.launch { repo.saveCustomWorkout(custom.filter { it.id != ex.id }) } }
                }
            }
            SectionLabel("Biblioteca")
            defaultExerciseLibrary.groupBy { it.muscleGroup }.forEach { (group, items) ->
                Text(group.uppercase(), style = T.label, color = Volt.textDim, modifier = Modifier.padding(start = 4.dp, top = 4.dp))
                items.forEach { e ->
                    val added = custom.any { it.id == e.id }
                    Row(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Volt.surface)
                            .tap(0.98f) {
                                if (!added) scope.launch {
                                    Haptics.medium(ctx)
                                    val withGif = repo.enrichRoutine(listOf(com.voltbody.app.data.WorkoutDay("custom", "custom", listOf(e.toExercise())))).firstOrNull()?.exercises?.firstOrNull() ?: e.toExercise()
                                    repo.saveCustomWorkout(custom + withGif)
                                }
                            }.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(e.name, style = T.cardTitle, color = Volt.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text("${e.defaultSets} × ${e.defaultReps}", style = T.caption, color = Volt.textDim)
                        }
                        Icon(if (added) Icons.Filled.Check else Icons.Filled.Add, null, tint = Volt.accent)
                    }
                }
            }
        }
        else -> SectionScreen("") { Text("Sección no disponible", style = T.body, color = Volt.textDim) }
    }
}

@Composable
private fun NotificationsSection(repo: Repository) {
    val st by repo.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    fun enable() = scope.launch {
        repo.setNotificationsEnabled(true)
        Notifications.ensureChannels(ctx)
        Notifications.scheduleAll(ctx, repo.state.value)
        repo.toast("success", "Notificaciones activadas", "Recordatorios de comidas, entreno y peso.")
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) enable() else repo.toast("error", "Permiso denegado", "Actívalo en los ajustes de Android.")
    }
    Card(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Recordatorios", style = T.section, color = Volt.text)
                Text("Comidas según tu horario, entreno del día y pesaje semanal.", style = T.caption, color = Volt.textDim)
            }
            Switch(
                checked = st.notificationsEnabled,
                onCheckedChange = { on ->
                    Haptics.light(ctx)
                    if (on) {
                        val needs = Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        if (needs) launcher.launch(Manifest.permission.POST_NOTIFICATIONS) else enable()
                    } else {
                        Notifications.cancelAll(ctx)
                        scope.launch { repo.setNotificationsEnabled(false) }
                    }
                },
                colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF0A0A0A), checkedTrackColor = Volt.accent, uncheckedTrackColor = Volt.surface2),
            )
        }
    }
}
