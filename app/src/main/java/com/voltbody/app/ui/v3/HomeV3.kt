package com.voltbody.app.ui.v3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.DailyMetric
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.screens.home.DataImage
import com.voltbody.app.ui.screens.home.ExerciseImage
import com.voltbody.app.ui.screens.home.HomeUi
import com.voltbody.app.ui.screens.home.buildHomeUi
import com.voltbody.app.ui.screens.home.rememberGifLoader
import com.voltbody.app.ui.screens.home.rememberHealthState
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.Haptics
import com.voltbody.app.util.ensureWeeklyGoals
import com.voltbody.app.util.mondayFirstIndex
import com.voltbody.app.util.unlockNewAchievements
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

private val ES = Locale("es", "ES")
fun fmt(n: Number): String = NumberFormat.getIntegerInstance(ES).format(n)
fun fmt1(d: Double): String = String.format(ES, "%.2f", d)

/** Estado derivado de la Home (fuera del hilo principal). */
@Composable
fun rememberHomeUi(repo: Repository): HomeUi? {
    val today = remember { LocalDate.now() }
    val weekday = remember { today.dayOfWeek.getDisplayName(TextStyle.FULL, ES).replaceFirstChar { it.uppercase() } }
    val flow = remember(repo) { repo.state.map { buildHomeUi(it, today, weekday) }.distinctUntilChanged().flowOn(Dispatchers.Default) }
    val ui by flow.collectAsStateWithLifecycle(initialValue = null)
    return ui
}

@Composable
fun HomeV3(repo: Repository) {
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val health = rememberHealthState(repo)
    val u = rememberHomeUi(repo)
    val gif = rememberGifLoader()
    val todayKey = remember { LocalDate.now().toString() }

    LaunchedEffect(u?.hydrated, u?.authed) {
        if (u != null && u.hydrated && u.authed) {
            ensureWeeklyGoals(repo)
            val fresh = unlockNewAchievements(repo)
            if (fresh.isNotEmpty()) { Haptics.success(ctx); repo.toast("success", "Logro desbloqueado", fresh.first().label) }
        }
    }

    Screen {
        if (u == null) { HomeSkeleton(); return@Screen }

        // ── Cabecera
        Row(Modifier.fillMaxWidth().enter(0), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(greeting(u), style = T.hello, color = Volt.textDim, maxLines = 1)
                Text("Hola, ${u.name?.substringBefore(' ') ?: "atleta"}", style = T.screen, color = Volt.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Box(Modifier.size(46.dp).clip(CircleShape).background(Volt.surface2).tap(0.9f) { nav.tab(Tab.Profile) }, contentAlignment = Alignment.Center) {
                if (u.profilePhoto != null) DataImage(u.profilePhoto, Modifier.fillMaxSize(), maxPx = 160)
                else Text((u.name ?: "A").take(1).uppercase(), style = T.section, color = Volt.accent)
            }
        }

        // ── Banner de energia: degradado + linea de voltaje que lo recorre
        Box(
            Modifier.fillMaxWidth().enter(1).clip(RoundedCornerShape(24.dp)).background(bannerBrush).tap(0.98f) { nav.tab(Tab.Train) },
        ) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(46.dp).clip(CircleShape).background(Color(0xFF0A0A0A)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Bolt, null, tint = Volt.accent, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    FitText(if (u.hasTodayRoutine) gymTerm(u.todayFocus ?: "Workout") else "Recarga y muévete", T.display.copy(fontSize = 17.sp), Color(0xFF0A0A0A), maxLines = 2, minScale = 0.8f)
                    Text(u.dailyQuote ?: u.phrase, style = T.caption, color = Color(0xFF0A0A0A).copy(alpha = 0.72f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                if (u.hasTodayRoutine) Icon(Icons.Filled.PlayArrow, null, tint = Color(0xFF0A0A0A), modifier = Modifier.size(26.dp))
            }
        }

        SectionLabel("Tus estadísticas", Modifier.enter(2))

        // ── Pasos (tarjeta grande con barras)
        val steps = maxOf(health.snap?.steps ?: 0L, u.todayStepsMetric)
        val km = health.snap?.distanceKm?.takeIf { it > 0.0 && (health.snap?.steps ?: 0L) >= u.todayStepsMetric } ?: (steps * 0.00075)
        val week = remember(u.pastSteps, steps) { u.pastSteps.takeLast(6) + steps.toFloat() }
        var sel by remember(week.size) { mutableIntStateOf(week.lastIndex) }
        Card(Modifier.fillMaxWidth().enter(3), onClick = { nav.tab(Tab.Activity) }) {
            CardHeader(Icons.AutoMirrored.Filled.DirectionsWalk, Volt.accent, "Pasos", trailing = if (health.status == "needs" || health.status == "unavailable") {
                { SmallPill("Conectar", onClick = { nav.push(Route.Section("health")) }) }
            } else null)
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.Bottom) {
                ValueUnit(fmt(week.getOrElse(sel) { 0f }.toLong()), "Pasos", unitBelow = true)
                Spacer(Modifier.width(22.dp))
                ValueUnit(fmt1(if (sel == week.lastIndex) km else week[sel] * 0.00075), "km", unitBelow = true)
            }
            Spacer(Modifier.height(10.dp))
            MockBars(week, sel, fmt(week.getOrElse(sel) { 0f }.toLong()), onSelect = { sel = it })
        }

        // ── Calorias + Agua
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).enter(4), horizontalArrangement = Arrangement.spacedBy(K.gap)) {
            Card(Modifier.weight(1f).fillMaxHeight(), onClick = { nav.tab(Tab.Food) }) {
                CardHeader(Icons.Filled.LocalFireDepartment, Volt.orange, "Calorías")
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ValueUnit(fmt(u.consumedKcal), "de ${fmt(u.caloriesTarget)} kcal", Modifier.weight(1f), unitBelow = true)
                    MiniDonut(if (u.caloriesTarget > 0) u.consumedKcal / u.caloriesTarget.toFloat() else 0f)
                }
            }
            Card(Modifier.weight(1f).fillMaxHeight(), onClick = {
                Haptics.medium(ctx)
                scope.launch { repo.saveMetrics(todayKey, DailyMetric(todayKey, waterMl = u.waterMl + 250)) }
            }) {
                CardHeader(Icons.Filled.WaterDrop, Volt.water, "Agua", trailing = { Text("+250", style = T.caption, color = Volt.water) })
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ValueUnit(fmt(u.waterMl), "ml", Modifier.weight(1f), unitBelow = true)
                    WaveLine(listOf(0.2f, 0.5f, 0.35f, 0.8f, 0.55f, 0.9f), Modifier.width(56.dp).height(30.dp), color = Volt.water)
                }
            }
        }

        // ── Entreno de hoy
        SectionLabel("Entreno de hoy", Modifier.enter(5), "Ver semana") { nav.tab(Tab.Train) }
        val todayIdx = remember { mondayFirstIndex(LocalDate.now()) }
        Card(Modifier.fillMaxWidth().enter(6), padding = 10.dp, onClick = {
            if (u.hasTodayRoutine) nav.push(Route.Day(todayIdx)) else nav.tab(Tab.Train)
        }) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(width = 92.dp, height = 78.dp).clip(RoundedCornerShape(18.dp)).background(Volt.surface2), contentAlignment = Alignment.Center) {
                    val first = u.todayExercises.firstOrNull()
                    if (first != null && first.gifUrl.isNotBlank()) ExerciseImage(first.gifUrl, gif, Modifier.fillMaxSize(), 280, 240)
                    else Icon(Icons.Outlined.FitnessCenter, null, tint = Volt.textDim, modifier = Modifier.size(30.dp))
                }
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    FitText(if (u.hasTodayRoutine) gymTerm(u.todayFocus ?: "Workout") else "Día de descanso", T.section, Volt.text, maxLines = 2, minScale = 0.8f)
                    Text(
                        if (u.hasTodayRoutine) "${u.todayExerciseCount} ejercicios · ${u.estimatedMinutes} min" else "Recupera o arma una sesión libre",
                        style = T.caption, color = Volt.textDim, maxLines = 1,
                    )
                    if (u.hasTodayRoutine) {
                        Spacer(Modifier.height(8.dp))
                        Box(Modifier.fillMaxWidth(0.9f).height(5.dp).clip(RoundedCornerShape(3.dp)).background(Volt.surface2)) {
                            Box(Modifier.fillMaxWidth(u.routineCompletion / 100f).fillMaxHeight().clip(RoundedCornerShape(3.dp)).background(Volt.accent))
                        }
                    }
                }
                Spacer(Modifier.width(10.dp))
                Box(Modifier.size(44.dp).clip(CircleShape).background(Volt.accent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.PlayArrow, "Empezar", tint = Color(0xFF0A0A0A))
                }
            }
        }

        // ── Accesos rapidos
        Row(Modifier.fillMaxWidth().enter(7), horizontalArrangement = Arrangement.spacedBy(K.gap)) {
            QuickTile(Icons.Outlined.SelfImprovement, "Recovery", if (u.todayRecovery != null) "${u.recoveryScore}" else "Check-in", Volt.water, Modifier.weight(1f)) { nav.push(Route.Section("recovery")) }
            QuickTile(Icons.Outlined.MonitorWeight, "Peso", u.weightLast?.let { "${trim(it)} kg" } ?: "Añadir", Volt.water, Modifier.weight(1f)) { nav.push(Route.Section("weight")) }
            QuickTile(Icons.Outlined.Whatshot, "Racha", if (u.streak == 1) "1 día" else "${u.streak} días", Volt.orange, Modifier.weight(1f)) { nav.push(Route.Section("habits")) }
        }
    }
}

private fun greeting(u: HomeUi): String {
    val h = java.time.LocalTime.now().hour
    val base = when { h < 6 -> "Noche de campeones"; h < 13 -> "Buenos días"; h < 20 -> "Buenas tardes"; else -> "Buenas noches" }
    return when {
        u.streak >= 3 -> "$base · racha de ${u.streak} días, no la rompas"
        u.hasTodayRoutine -> "$base · hoy toca darlo todo"
        else -> "$base · hoy recargas pilas"
    }
}

fun trim(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else String.format(ES, "%.1f", d)

@Composable
private fun QuickTile(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String, tint: Color, modifier: Modifier, onClick: () -> Unit) {
    Card(modifier, onClick = onClick, padding = 12.dp) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(8.dp))
        Text(value, style = T.cardTitle.copy(fontSize = 15.sp), color = Volt.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(label, style = T.caption, color = Volt.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun HomeSkeleton() {
    repeat(4) { i ->
        Box(Modifier.fillMaxWidth().height(if (i == 2) 180.dp else 90.dp).clip(RoundedCornerShape(K.radius)).background(Volt.surface))
    }
}
