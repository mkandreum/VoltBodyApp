package com.voltbody.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.AppState
import com.voltbody.app.data.DailyMetric
import com.voltbody.app.data.Repository
import com.voltbody.app.data.WorkoutLog
import com.voltbody.app.ui.*
import com.voltbody.app.ui.screens.home.*
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

private const val XP_PER_LOG = 12
private const val XP_PER_STREAK_DAY = 8
private const val XP_PER_LEVEL = 250
private const val MINUTES_PER_EXERCISE = 5

private fun parseMealHour(time: String): Int = Regex("^(\\d{1,2}):").find(time)?.groupValues?.get(1)?.toIntOrNull() ?: -1
private val DAY_INITIALS = listOf("L", "M", "X", "J", "V", "S", "D")

/**
 * Pantalla de inicio. [onNavigate] recibe "workout" | "diet" | "profile" para cambiar de pestana
 * (opcional: la barra inferior de MainActivity puede conectarlo).
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(repo: Repository, onNavigate: (String) -> Unit = {}) {
    val st by repo.state.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val health = rememberHealthState(repo)
    val gif = rememberGifLoader()
    val today = remember { LocalDate.now() }
    val todayKey = today.toString()

    var syncState by remember { mutableStateOf("idle") }
    var confetti by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }
    val water = st.waterOn(today)

    // ── Datos derivados ──
    val routineByDay = remember(st.routine) { mapRoutineByWeekday(st.routine) }
    val todayRoutine = routineByDay[mondayFirstIndex(today)]
    val logDates = remember(st.logs) { st.logs.mapNotNull { l -> logLocalDate(l.date)?.let { it to l } } }
    val monday = mondayOf(today)
    val completedDays = remember(logDates) { logDates.map { it.first }.filter { !it.isBefore(monday) && !it.isAfter(monday.plusDays(6)) }.toSet().size }
    val streak = remember(st.logs, st.routine) { computeSmartStreak(st.logs, st.routine) }
    val todayExerciseIds = remember(logDates) { logDates.filter { it.first == today }.map { it.second.exerciseId }.toSet() }
    val routineCompletion = remember(todayRoutine, todayExerciseIds) {
        val ex = todayRoutine?.exercises.orEmpty()
        if (ex.isEmpty()) 0 else Math.round(ex.count { it.id in todayExerciseIds } * 100f / ex.size)
    }
    val weeklyConsistency = minOf(Math.round(completedDays / 7f * 100), 100)
    val diet = st.diet
    val caloriesTarget = diet?.dailyCalories ?: 0
    val meals = diet?.meals.orEmpty()
    val eatenToday = st.mealEaten[todayKey].orEmpty()
    val consumedKcal = meals.filter { it.id in eatenToday }.sumOf { it.calories }
    val nutritionAdherence = if (meals.isEmpty()) 0 else Math.round(meals.count { it.id in eatenToday } * 100f / meals.size)
    val totalXP = st.logs.size * XP_PER_LOG + streak * XP_PER_STREAK_DAY
    val level = totalXP / XP_PER_LEVEL + 1
    val xpInLevel = totalXP % XP_PER_LEVEL
    val estimatedMinutes = (todayRoutine?.exercises?.size ?: 0) * MINUTES_PER_EXERCISE

    val sleepScore = remember(st.insights?.sleepRecommendation) {
        val rec = st.insights?.sleepRecommendation.orEmpty()
        if (rec.isBlank()) 72 else {
            val h = Regex("(\\d+(?:[.,]\\d+)?)\\s*hor", RegexOption.IGNORE_CASE).find(rec)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull() ?: 7.0
            Math.round((h / 8 * 100).coerceIn(40.0, 100.0)).toInt()
        }
    }
    val todayRecovery = st.recoveryLogs.firstOrNull { it.date == todayKey }
    val recoveryScore = todayRecovery?.score ?: sleepScore
    val advice = remember(recoveryScore, todayRoutine?.focus) { getRecoveryAdvice(recoveryScore, todayRoutine?.focus) }
    val coach = remember(recoveryScore, routineCompletion, todayRoutine) {
        when {
            recoveryScore < 65 -> Triple("${advice.emoji} ${advice.title}", advice.subtitle, advice.intensityLabel)
            routineCompletion < 45 && todayRoutine != null -> Triple("🔥 Hoy toca. Sin excusas.",
                "Completa al menos ${maxOf(1, Math.ceil(todayRoutine.exercises.size * 0.6).toInt())} ejercicios para cerrar el día en verde.", "🚀 Empezar ahora")
            recoveryScore >= 85 -> Triple("${advice.emoji} ${advice.title}", advice.subtitle, advice.intensityLabel)
            else -> Triple("🎯 Ritmo perfecto", "Mantén intensidad y cuida la ejecución. +1% mejor hoy.", "📋 Seguir plan")
        }
    }

    // Pasos de la semana (hoy en vivo desde Health Connect; resto desde DailyMetric del servidor)
    val stepsToday = health.snap?.steps ?: st.stepsOn(today)
    val weekSteps = remember(health.snap, st.metrics, today) {
        (6 downTo 0).map { i -> val d = today.minusDays(i.toLong()); if (i == 0) stepsToday.toFloat() else st.stepsOn(d).toFloat() }
    }
    val weekLabels = remember(today) { (6 downTo 0).map { DAY_INITIALS[mondayFirstIndex(today.minusDays(it.toLong()))] } }
    val km = health.snap?.distanceKm?.takeIf { it > 0 } ?: (stepsToday * 0.00075)

    // ── Efectos ──
    LaunchedEffect(st.hydrated, st.isAuthenticated, st.weeklyGoalsWeek, st.weeklyGoals.isEmpty()) {
        if (st.hydrated && st.isAuthenticated) ensureWeeklyGoals(repo)
    }
    LaunchedEffect(st.logs.size, st.hydrated) {
        if (!st.hydrated) return@LaunchedEffect
        val fresh = unlockNewAchievements(repo)
        if (fresh.isNotEmpty()) {
            confetti++
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            fresh.forEach { repo.toast("success", "🏅 Logro desbloqueado", "${it.icon} ${it.label}") }
        }
    }

    fun quickLogSet() {
        val tr = todayRoutine
        if (tr == null || tr.exercises.isEmpty()) { onNavigate("workout"); return }
        val pending = tr.exercises.firstOrNull { it.id !in todayExerciseIds } ?: tr.exercises.first()
        val reps = Regex("\\d+").find(pending.reps)?.value?.toIntOrNull() ?: 10
        val log = WorkoutLog(Instant.now().toString(), pending.id, pending.weight.coerceAtLeast(0.0), reps)
        scope.launch {
            syncState = "syncing"
            repo.addLog(log)
            val ok = repo.state.value.logs.any { it.date == log.date && it.exerciseId == log.exerciseId }
            syncState = if (ok) "synced" else "error"
            if (ok) repo.toast("success", "Serie guardada", "${pending.name}: ${log.weight}kg x ${log.reps}")
        }
    }

    // ── Timeline ──
    val timeline = run {
        fun eaten(pred: (Int, String) -> Boolean) = meals.any { m -> pred(parseMealHour(m.time), m.name.lowercase()) && m.id in eatenToday }
        listOf(
            TimelineItem(st.profile.mealTimes?.breakfast ?: "08:00", "🥣 Desayuno de arranque", eaten { h, n -> (h in 6..10) || n.contains("desay") }),
            TimelineItem(st.profile.mealTimes?.lunch ?: "14:00", "🍗 Comida principal", eaten { h, n -> (h in 12..16) || n.contains("comida") }),
            TimelineItem("Entreno", "💪 ${todayRoutine?.focus ?: "Sesión rápida"}", routineCompletion >= 100),
            TimelineItem("Progreso", "📸 Subir foto del día", st.progressPhotos.any { it.date.take(10) == todayKey }),
        )
    }

    val weightHistory = st.weightLogs.takeLast(10).map { it.weight.toFloat() }
    val weightDelta = if (st.weightLogs.size >= 2) st.weightLogs.last().weight - st.weightLogs[st.weightLogs.size - 2].weight else null
    val weekdayName = remember { today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale("es", "ES")).replaceFirstChar { it.uppercase() } }
    val featured = (todayRoutine ?: st.routine.firstOrNull())?.exercises.orEmpty()

    Box(Modifier.fillMaxSize()) {
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; runCatching { repo.refreshAll(); health.refresh(repo) }; refreshing = false } },
            modifier = Modifier.fillMaxSize(),
        ) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Cabecera
            item {
                Row(Modifier.fillMaxWidth().staggeredEntry(0), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Keep Moving Today!", color = Volt.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            if (!st.profile.name.isNullOrBlank()) "Hola, ${st.profile.name}" else "Modo Bestia 🦁",
                            color = Volt.text, fontSize = 28.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                        Text("$weekdayName · $routineCompletion% sesión", color = Volt.textDim, fontSize = 12.sp)
                    }
                    Box(
                        Modifier.size(54.dp).clip(CircleShape).background(Volt.surface2).border(2.dp, Volt.accent, CircleShape)
                            .clickable { onNavigate("profile") },
                        contentAlignment = Alignment.Center,
                    ) {
                        if (st.profile.profilePhoto.isNullOrBlank()) Icon(Icons.Filled.Person, null, tint = Volt.textDim)
                        else DataImage(st.profile.profilePhoto, Modifier.fillMaxSize())
                    }
                }
            }
            // Banner + frase del dia
            item {
                Column(Modifier.staggeredEntry(1), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GradientBanner("Stay Active", st.motivationPhrase.ifBlank { "Hoy toca. Sin excusas." }, Icons.Filled.Bolt)
                    st.insights?.dailyQuote?.takeIf { it.isNotBlank() }?.let { q ->
                        VoltCard(Modifier.fillMaxWidth()) {
                            Row(verticalAlignment = Alignment.Top) {
                                Icon(Icons.Filled.FormatQuote, null, tint = Volt.accent)
                                Spacer(Modifier.width(10.dp))
                                Text(q, color = Volt.accent, fontSize = 13.sp, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }
            // Conectar salud
            if (health.status == "needs" || health.status == "unavailable") item { ConnectHealthCard(health, Modifier.staggeredEntry(2)) }

            // Estadisticas
            item { SectionTitle("Tus estadísticas recientes", Modifier.staggeredEntry(2)) }
            item { StepsCard(stepsToday, km, weekSteps, weekLabels, Modifier.fillMaxWidth().staggeredEntry(3)) }
            item {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).staggeredEntry(4), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CaloriesTile(consumedKcal, caloriesTarget, health.snap?.activeKcal, Modifier.weight(1f).fillMaxHeight())
                    WaterTile(water, { d -> scope.launch { repo.saveMetrics(todayKey, DailyMetric(todayKey, waterMl = (water + d).coerceAtLeast(0))) } }, Modifier.weight(1f).fillMaxHeight())
                }
            }
            item {
                Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).staggeredEntry(5), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecoveryTile(recoveryScore, advice, st.recoveryLogs.takeLast(7).map { it.score.toFloat() }, todayRecovery != null, Modifier.weight(1f).fillMaxHeight())
                    WeightTile(st.weightLogs.lastOrNull()?.weight, weightDelta, weightHistory, Modifier.weight(1f).fillMaxHeight())
                }
            }

            // Hoy conquistas
            item {
                VoltCard(Modifier.fillMaxWidth().staggeredEntry(6)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("🏆 HOY CONQUISTAS", color = Volt.textDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.height(4.dp))
                            Text((todayRoutine?.focus ?: "Recuperación activa") + if (todayRoutine != null) " 💀🔥" else "", color = Volt.accent, fontSize = 24.sp, fontWeight = FontWeight.Black, lineHeight = 28.sp)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (todayRoutine != null) "⚡ $estimatedMinutes min · 🔥 $caloriesTarget kcal · ${todayRoutine.exercises.size} ejercicios"
                                else "Sin rutina cargada. Activa una sesión rápida en menos de 2 min",
                                color = Volt.textDim, fontSize = 12.sp,
                            )
                        }
                        DonutProgress(routineCompletion / 100f, Modifier.size(72.dp), stroke = 8.dp, color = Volt.accent) {
                            CountUpText(routineCompletion.toFloat(), suffix = "%", style = TextStyle(color = Volt.text, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        FlipMetric("$streak🔥", "Racha días", Modifier.weight(1f))
                        FlipMetric("Nv. $level ⚡", "$xpInLevel/$XP_PER_LEVEL XP", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        PrimaryButton("⚡ Iniciar Sesión", { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigate("workout") }, Modifier.weight(1f))
                        Box(
                            Modifier.weight(1f).height(54.dp).clip(RoundedCornerShape(27.dp)).background(Volt.surface2).border(1.dp, Volt.border, RoundedCornerShape(27.dp))
                                .pressScale { onNavigate("diet") },
                            contentAlignment = Alignment.Center,
                        ) { Text("🍽 Plan Nutricional", color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp) }
                    }
                }
            }

            // Entrenos destacados
            if (featured.isNotEmpty()) {
                item { SectionTitle("Entrenos destacados", Modifier.staggeredEntry(7), "Ver rutina") { onNavigate("workout") } }
                item {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.staggeredEntry(8)) {
                        items(featured) { ex ->
                            val done = ex.id in todayExerciseIds
                            Box(Modifier.size(width = 190.dp, height = 240.dp).clip(RoundedCornerShape(24.dp)).background(Volt.surface).clickable { onNavigate("workout") }) {
                                ExerciseImage(ex.gifUrl, gif, Modifier.fillMaxSize())
                                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.88f)))))
                                if (done) Box(Modifier.align(Alignment.TopEnd).padding(10.dp).size(26.dp).clip(CircleShape).background(Volt.accent), contentAlignment = Alignment.Center) {
                                    Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                                }
                                Column(Modifier.align(Alignment.BottomStart).padding(14.dp)) {
                                    Text(ex.name, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                    Text("${ex.sets} series · ${ex.reps} reps · ~${ex.sets * 2} min", color = Color.White.copy(.7f), fontSize = 11.sp)
                                    Spacer(Modifier.height(8.dp))
                                    Box(Modifier.clip(RoundedCornerShape(50)).background(Volt.accent).padding(horizontal = 14.dp, vertical = 6.dp)) {
                                        Text(if (done) "Hecho" else "Empezar", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // XP
            item {
                VoltCard(Modifier.fillMaxWidth().staggeredEntry(9)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Bolt, null, tint = Volt.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Nivel $level", color = Volt.text, fontWeight = FontWeight.Black)
                        Text("  · $totalXP XP total", color = Volt.textDim, fontSize = 11.sp, modifier = Modifier.weight(1f))
                        Text("$xpInLevel/$XP_PER_LEVEL XP", color = Volt.text, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                    Spacer(Modifier.height(10.dp))
                    val xp by androidx.compose.animation.core.animateFloatAsState(xpInLevel / XP_PER_LEVEL.toFloat(), androidx.compose.animation.core.tween(900), label = "xp")
                    Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Volt.surface2)) {
                        Box(Modifier.fillMaxHeight().fillMaxWidth(xp).clip(RoundedCornerShape(5.dp)).background(Brush.horizontalGradient(listOf(Color(0xFF2DD4BF), Volt.accent))))
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPill("días racha", "$streak🔥", Modifier.weight(1f))
                        StatPill("series totales", "${st.logs.size}", Modifier.weight(1f))
                        StatPill("semana", "$weeklyConsistency%", Modifier.weight(1f))
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPill("Objetivo calórico", if (caloriesTarget > 0) "$caloriesTarget kcal" else "—", Modifier.weight(1f), Volt.orange)
                        StatPill("Adherencia nutricional", "$nutritionAdherence%", Modifier.weight(1f), Volt.water)
                    }
                }
            }

            // Grafica de peso levantado (max por dia, 7 dias)
            item {
                val chart = (6 downTo 0).map { i ->
                    val d = today.minusDays(i.toLong())
                    (logDates.filter { it.first == d }.maxOfOrNull { it.second.weight } ?: 0.0).toFloat()
                }
                VoltCard(Modifier.fillMaxWidth().staggeredEntry(10)) {
                    Text("📊 Consistencia semanal", color = Volt.text, fontWeight = FontWeight.Bold)
                    Row(verticalAlignment = Alignment.Bottom) {
                        CountUpText(weeklyConsistency.toFloat(), suffix = "%", style = TextStyle(color = Volt.accent, fontSize = 32.sp, fontWeight = FontWeight.Black))
                        Text(if (streak > 0) "   🔥 $streak días en racha" else "   🚀 Inicia tu racha hoy", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 6.dp))
                    }
                    Spacer(Modifier.height(8.dp))
                    Text("Peso máximo levantado · últimos 7 días", color = Volt.textDim, fontSize = 11.sp)
                    AreaChart(chart, weekLabels)
                }
            }

            if (st.achievements.isNotEmpty() || st.logs.isNotEmpty()) item { Box(Modifier.staggeredEntry(11)) { AchievementsCard(st) } }

            item { Box(Modifier.staggeredEntry(12)) { RecoveryCard(repo, health, todayRecovery, advice, recoveryScore, scope) } }
            item { Box(Modifier.staggeredEntry(13)) { HeartRateCard() } }

            // Coach IA
            item {
                AnimatedContent(coach, transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) }, label = "coach", modifier = Modifier.staggeredEntry(14)) { c ->
                    GradientBanner(c.first, c.second + "  ·  " + c.third, Icons.Filled.AutoAwesome)
                }
            }

            item { Box(Modifier.staggeredEntry(15)) { HabitsCard(repo, st, scope) } }
            item { Box(Modifier.staggeredEntry(16)) { WeightCard(repo, st, scope) } }
            item { Box(Modifier.staggeredEntry(17)) { PhotosCard(repo, st, scope) } }
            item { Box(Modifier.staggeredEntry(18)) { ReportCard(repo) } }
            item { Box(Modifier.staggeredEntry(19)) { TimelineCard(timeline) } }
            item { Box(Modifier.staggeredEntry(20)) { QuickActionsCard(syncState, ::quickLogSet, { onNavigate("diet") }, { onNavigate("profile") }) } }
            item { Box(Modifier.staggeredEntry(21)) { MotivationCard(st.profile.motivationPhoto, st.motivationPhrase) } }
        }
        }
        ConfettiBurst(confetti, Modifier.fillMaxSize())
    }
}

/** Metrica que "voltea" (slide+fade) cuando cambia el valor. */
@Composable
private fun FlipMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Volt.surface2).padding(horizontal = 14.dp, vertical = 12.dp)) {
        AnimatedContent(value, transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) }, label = "flip") { v ->
            Text(v, color = Volt.text, fontSize = 22.sp, fontWeight = FontWeight.Black)
        }
        Text(label.uppercase(), color = Volt.textDim, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}
