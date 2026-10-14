package com.voltbody.app.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.ImageLoader
import com.voltbody.app.data.DailyMetric
import com.voltbody.app.data.Exercise
import com.voltbody.app.data.Repository
import com.voltbody.app.data.WorkoutLog
import com.voltbody.app.ui.AnimatedCheck
import com.voltbody.app.ui.BentoCard
import com.voltbody.app.ui.CountUpText
import com.voltbody.app.ui.GlowRing
import com.voltbody.app.ui.GradientBanner
import com.voltbody.app.ui.GradientText
import com.voltbody.app.ui.KineticTitle
import com.voltbody.app.ui.MeshBackground
import com.voltbody.app.ui.PrimaryButton
import com.voltbody.app.ui.SectionTitle
import com.voltbody.app.ui.StatPill
import com.voltbody.app.ui.gradientBorder
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.ui.screens.home.AchievementsCard
import com.voltbody.app.ui.screens.home.AreaChart
import com.voltbody.app.ui.screens.home.CaloriesTile
import com.voltbody.app.ui.screens.home.ConfettiBurst
import com.voltbody.app.ui.screens.home.ConnectHealthCard
import com.voltbody.app.ui.screens.home.DataImage
import com.voltbody.app.ui.screens.home.ExerciseImage
import com.voltbody.app.ui.screens.home.HabitsCard
import com.voltbody.app.ui.screens.home.HealthState
import com.voltbody.app.ui.screens.home.HeartRateCard
import com.voltbody.app.ui.screens.home.HmGhostPill
import com.voltbody.app.ui.screens.home.HmGradientBar
import com.voltbody.app.ui.screens.home.HmHomeSkeleton
import com.voltbody.app.ui.screens.home.HmTileHeader
import com.voltbody.app.ui.screens.home.HM_XP_PER_LEVEL
import com.voltbody.app.ui.screens.home.HomeUi
import com.voltbody.app.ui.screens.home.MotivationCard
import com.voltbody.app.ui.screens.home.PhotosCard
import com.voltbody.app.ui.screens.home.QuickActionsCard
import com.voltbody.app.ui.screens.home.RecoveryCard
import com.voltbody.app.ui.screens.home.RecoveryTile
import com.voltbody.app.ui.screens.home.ReportCard
import com.voltbody.app.ui.screens.home.StepsCard
import com.voltbody.app.ui.screens.home.TimelineCard
import com.voltbody.app.ui.screens.home.WeightCard
import com.voltbody.app.ui.screens.home.WeightTile
import com.voltbody.app.ui.screens.home.WaterTile
import com.voltbody.app.ui.screens.home.buildHomeUi
import com.voltbody.app.ui.screens.home.rememberGifLoader
import com.voltbody.app.ui.screens.home.rememberHealthState
import com.voltbody.app.util.ensureWeeklyGoals
import com.voltbody.app.util.mondayFirstIndex
import com.voltbody.app.util.unlockNewAchievements
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.util.Locale

private val DAY_INITIALS = listOf("L", "M", "X", "J", "V", "S", "D")

/** Ultima foto derivada: evita el parpadeo de skeleton al volver a la pestana. */
@Volatile
private var lastHomeUi: HomeUi? = null

/**
 * Pantalla de inicio (diseno v2, bento). [onNavigate] recibe "workout" | "diet" | "profile".
 * El estado se deriva fuera del hilo principal en un [HomeUi] inmutable: solo se recompone lo que cambia.
 */
@Composable
fun HomeScreen(repo: Repository, onNavigate: (String) -> Unit = {}) {
    val health = rememberHealthState(repo)
    val gif = rememberGifLoader()
    val today = remember { LocalDate.now() }
    val weekdayName = remember { today.dayOfWeek.getDisplayName(java.time.format.TextStyle.FULL, Locale("es", "ES")).replaceFirstChar { it.uppercase() } }
    val uiFlow = remember(repo) {
        repo.state.map { buildHomeUi(it, today, weekdayName) }.distinctUntilChanged().onEach { lastHomeUi = it }.flowOn(Dispatchers.Default)
    }
    val ui by uiFlow.collectAsStateWithLifecycle(initialValue = lastHomeUi)

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        MeshBackground(Modifier.fillMaxSize(), intensity = 0.5f)
        val u = ui
        if (u == null) HmHomeSkeleton()
        else HomeContent(u, repo, health, gif, today, onNavigate)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeContent(u: HomeUi, repo: Repository, health: HealthState, gif: ImageLoader, today: LocalDate, onNavigate: (String) -> Unit) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val todayKey = remember(today) { today.toString() }
    var syncState by remember { mutableStateOf("idle") }
    var confetti by remember { mutableIntStateOf(0) }
    var refreshing by remember { mutableStateOf(false) }

    // Pasos: hoy en vivo desde Health Connect; el resto, de DailyMetric del servidor.
    val stepsToday = health.snap?.steps ?: u.todayStepsMetric
    val weekSteps = remember(u.pastSteps, stepsToday) { u.pastSteps + stepsToday.toFloat() }
    val weekLabels = remember(today) { (6 downTo 0).map { DAY_INITIALS[mondayFirstIndex(today.minusDays(it.toLong()))] } }
    val km = health.snap?.distanceKm?.takeIf { it > 0 } ?: (stepsToday * 0.00075)

    // ── Efectos ──
    LaunchedEffect(u.hydrated, u.authed, u.weeklyGoalsWeek, u.goals.isEmpty()) {
        if (u.hydrated && u.authed) ensureWeeklyGoals(repo)
    }
    LaunchedEffect(u.logCount, u.hydrated) {
        if (!u.hydrated) return@LaunchedEffect
        val fresh = unlockNewAchievements(repo)
        if (fresh.isNotEmpty()) {
            confetti++
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            fresh.forEach { repo.toast("success", "🏅 Logro desbloqueado", "${it.icon} ${it.label}") }
        }
    }

    fun quickLogSet() {
        if (u.todayExercises.isEmpty()) { onNavigate("workout"); return }
        val pending = u.todayExercises.firstOrNull { it.id !in u.todayDoneIds } ?: u.todayExercises.first()
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

    val ptrState = rememberPullToRefreshState()
    Box(Modifier.fillMaxSize()) {
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = { scope.launch { refreshing = true; runCatching { repo.refreshAll(); health.refresh(repo) }; refreshing = false } },
            modifier = Modifier.fillMaxSize(),
            state = ptrState,
            indicator = {
                PullToRefreshDefaults.Indicator(
                    state = ptrState, isRefreshing = refreshing, modifier = Modifier.align(Alignment.TopCenter),
                    containerColor = Volt.surface2, color = Volt.accent,
                )
            },
        ) {
            LazyColumn(
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 120.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Cabecera con titular enorme
                item(key = "header", contentType = "header") { HomeHeader(u, onNavigate, Modifier.staggeredEntry(0)) }

                // Banner de marca + frase del dia
                item(key = "banner", contentType = "banner") {
                    Column(Modifier.staggeredEntry(1), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        GradientBanner("Stay Active", u.phrase.ifBlank { "Hoy toca. Sin excusas." }, Icons.Filled.Bolt)
                        u.dailyQuote?.takeIf { it.isNotBlank() }?.let { q ->
                            BentoCard(Modifier.fillMaxWidth(), padding = 16.dp) {
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(Icons.Filled.FormatQuote, null, tint = Volt.accent)
                                    Spacer(Modifier.width(12.dp))
                                    Text(q, color = Volt.text, style = VoltType.body.copy(fontStyle = FontStyle.Italic))
                                }
                            }
                        }
                    }
                }

                if (health.status == "needs" || health.status == "unavailable") {
                    item(key = "health", contentType = "health") { ConnectHealthCard(health, Modifier.staggeredEntry(2)) }
                }

                // Pasos (heroe)
                item(key = "steps", contentType = "steps") {
                    StepsCard(stepsToday, km, weekSteps, weekLabels, Modifier.fillMaxWidth().staggeredEntry(2))
                }
                // Calorias + Agua
                item(key = "kcalWater", contentType = "tiles") {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).staggeredEntry(3), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        CaloriesTile(u.consumedKcal, u.caloriesTarget, health.snap?.activeKcal, Modifier.weight(1f).fillMaxHeight())
                        WaterTile(
                            u.waterMl,
                            { d -> scope.launch { repo.saveMetrics(todayKey, DailyMetric(todayKey, waterMl = (u.waterMl + d).coerceAtLeast(0))) } },
                            Modifier.weight(1f).fillMaxHeight(),
                        )
                    }
                }
                // Recuperacion + Peso
                item(key = "recWeight", contentType = "tiles") {
                    Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).staggeredEntry(4), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RecoveryTile(u.recoveryScore, u.advice, u.recoveryHistory, u.todayRecovery != null, Modifier.weight(1f).fillMaxHeight())
                        WeightTile(u.weightLast, u.weightDelta, u.weightHistory, Modifier.weight(1f).fillMaxHeight())
                    }
                }

                // Hoy / conquistas
                item(key = "today", contentType = "today") { TodayCard(u, onNavigate, Modifier.staggeredEntry(5)) }

                // Entrenos destacados
                if (u.featured.isNotEmpty()) {
                    item(key = "featuredTitle", contentType = "title") {
                        SectionTitle("Entrenos destacados", Modifier.staggeredEntry(6), "Ver rutina") { onNavigate("workout") }
                    }
                    item(key = "featured", contentType = "featured") {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.staggeredEntry(6)) {
                            itemsIndexed(u.featured, key = { i, e -> "${i}_${e.id}" }, contentType = { _, _ -> "exercise" }) { _, ex ->
                                FeaturedCard(ex, ex.id in u.todayDoneIds, gif) { onNavigate("workout") }
                            }
                        }
                    }
                }

                item(key = "habits", contentType = "habits") { HabitsCard(repo, u.goals, scope) }

                if (u.achievements.isNotEmpty() || u.logCount > 0) {
                    item(key = "achievements", contentType = "achievements") { AchievementsCard(u.achievements) }
                }

                item(key = "recovery", contentType = "recovery") { RecoveryCard(repo, health, u.todayRecovery, u.advice, u.recoveryScore, scope) }

                item(key = "coach", contentType = "coach") {
                    AnimatedContent(
                        u.coach, label = "coach",
                        transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) },
                    ) { c ->
                        BentoCard(Modifier.fillMaxWidth()) {
                            HmTileHeader(Icons.Filled.AutoAwesome, Volt.accent, "Coach IA")
                            Spacer(Modifier.height(10.dp))
                            Text(c.title, color = Volt.text, style = VoltType.h2)
                            Spacer(Modifier.height(4.dp))
                            Text(c.subtitle, color = Volt.textDim, style = VoltType.body)
                            Spacer(Modifier.height(12.dp))
                            Box(Modifier.clip(CircleShape).background(Volt.accent.copy(alpha = 0.16f)).padding(horizontal = 14.dp, vertical = 8.dp)) {
                                Text(c.label, color = Volt.accent, style = VoltType.body.copy(fontWeight = FontWeight.Bold, fontSize = 12.sp))
                            }
                        }
                    }
                }

                item(key = "photos", contentType = "photos") { PhotosCard(repo, u.photos, scope) }
                item(key = "weight", contentType = "weight") { WeightCard(repo, u.weightLogs, scope) }
                item(key = "report", contentType = "report") { ReportCard(repo) }

                item(key = "consistency", contentType = "consistency") {
                    BentoCard(Modifier.fillMaxWidth()) {
                        HmTileHeader(Icons.Filled.BarChart, Volt.accent, "Consistencia semanal")
                        Spacer(Modifier.height(10.dp))
                        Row(verticalAlignment = Alignment.Bottom) {
                            CountUpText(u.weeklyConsistency.toFloat(), suffix = "%", style = VoltType.numberXL.copy(brush = Volt.gradient))
                            Text(
                                if (u.streak > 0) "  ${u.streak} días en racha" else "  Inicia tu racha hoy",
                                color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(bottom = 10.dp),
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text("Peso máximo levantado · últimos 7 días", color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp))
                        Spacer(Modifier.height(8.dp))
                        AreaChart(u.liftChart, weekLabels)
                    }
                }

                item(key = "xp", contentType = "xp") { XpCard(u) }

                item(key = "heart", contentType = "heart") { HeartRateCard() }
                item(key = "timeline", contentType = "timeline") { TimelineCard(u.timeline) }
                item(key = "quick", contentType = "quick") {
                    QuickActionsCard(syncState, ::quickLogSet, { onNavigate("diet") }, { onNavigate("profile") })
                }
                item(key = "motivation", contentType = "motivation") { MotivationCard(u.motivationPhoto, u.phrase) }
            }
        }
        ConfettiBurst(confetti, Modifier.fillMaxSize())
    }
}

/** Cabecera: etiqueta del dia, "Hola," + nombre gigante que se revela y avatar con anillo de marca. */
@Composable
private fun HomeHeader(u: HomeUi, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val first = u.name?.trim()?.split(" ")?.firstOrNull().orEmpty().ifBlank { "Bestia" }
    val size = when {
        first.length <= 7 -> 52
        first.length <= 9 -> 44
        first.length <= 12 -> 36
        else -> 30
    }
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text("HOY · ${u.weekdayName.uppercase()}", color = Volt.accent, style = VoltType.label, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Text("Hola,", color = Volt.textDim, style = VoltType.h1, maxLines = 1)
            KineticTitle(listOf(first), style = VoltType.display.copy(fontSize = size.sp, lineHeight = (size + 4).sp))
            Spacer(Modifier.height(4.dp))
            Text("${u.routineCompletion}% de la sesión de hoy", color = Volt.textDim, style = VoltType.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.size(60.dp).pressScale { onNavigate("profile") }.clip(CircleShape).background(Volt.surface2)
                .gradientBorder(2.dp, CircleShape, Volt.gradient),
            contentAlignment = Alignment.Center,
        ) {
            if (u.profilePhoto.isNullOrBlank()) Icon(Icons.Filled.Person, null, tint = Volt.textDim)
            else DataImage(u.profilePhoto, Modifier.fillMaxSize(), maxPx = 256)
        }
    }
}

/** Tarjeta "Hoy": foco del dia, anillo de progreso, racha/nivel y CTAs. */
@Composable
private fun TodayCard(u: HomeUi, onNavigate: (String) -> Unit, modifier: Modifier = Modifier) {
    val haptic = LocalHapticFeedback.current
    BentoCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                HmTileHeader(Icons.Filled.EmojiEvents, Volt.accent, "Hoy conquistas")
                Spacer(Modifier.height(10.dp))
                GradientText(u.todayFocus ?: "Recuperación activa", style = VoltType.h1, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                Text(
                    if (u.hasTodayRoutine) "${u.estimatedMinutes} min · ${u.caloriesTarget} kcal · ${u.todayExerciseCount} ejercicios"
                    else "Sin rutina cargada. Activa una sesión rápida en menos de 2 min",
                    color = Volt.textDim, style = VoltType.body,
                )
            }
            Spacer(Modifier.width(12.dp))
            GlowRing(u.routineCompletion / 100f, Modifier.size(92.dp), stroke = 7.dp) {
                CountUpText(u.routineCompletion.toFloat(), suffix = "%", style = VoltType.numberS.copy(color = Volt.text, fontSize = 16.sp))
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FlipMetric("${u.streak} 🔥", "Racha días", Modifier.weight(1f))
            FlipMetric("Nv. ${u.level}", "${u.xpInLevel}/$HM_XP_PER_LEVEL XP", Modifier.weight(1f))
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            PrimaryButton("Iniciar sesión", { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onNavigate("workout") }, Modifier.weight(1f))
            HmGhostPill("Nutrición", { onNavigate("diet") }, Modifier.weight(1f), height = 58.dp)
        }
    }
}

/** Nivel + XP + resumen numerico. */
@Composable
private fun XpCard(u: HomeUi) {
    BentoCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            HmTileHeader(Icons.Filled.Bolt, Volt.accent, "Nivel ${u.level}", Modifier.weight(1f))
            Text("${u.xpInLevel}/$HM_XP_PER_LEVEL XP", color = Volt.text, style = VoltType.label)
        }
        Spacer(Modifier.height(4.dp))
        Text("${u.totalXp} XP total", color = Volt.textDim, style = VoltType.body)
        Spacer(Modifier.height(12.dp))
        HmGradientBar(u.xpInLevel / HM_XP_PER_LEVEL.toFloat(), height = 10.dp)
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("días racha", "${u.streak}", Modifier.weight(1f))
            StatPill("series totales", "${u.logCount}", Modifier.weight(1f))
            StatPill("semana", "${u.weeklyConsistency}%", Modifier.weight(1f))
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPill("Objetivo calórico", if (u.caloriesTarget > 0) "${u.caloriesTarget} kcal" else "—", Modifier.weight(1f), Volt.orange)
            StatPill("Adherencia nutricional", "${u.nutritionAdherence}%", Modifier.weight(1f), Volt.water)
        }
    }
}

/** Tarjeta de entreno destacado con GIF (decodificado a tamano acotado), check animado y CTA pill. */
@Composable
private fun FeaturedCard(ex: Exercise, done: Boolean, gif: ImageLoader, onClick: () -> Unit) {
    val shade = remember { Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.88f))) }
    val grad = Volt.gradient
    Box(
        Modifier.size(width = 200.dp, height = 264.dp).pressScale(onClick).clip(RoundedCornerShape(32.dp)).background(Volt.surface),
    ) {
        ExerciseImage(ex.gifUrl, gif, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(shade))
        if (done) AnimatedCheck(true, Modifier.align(Alignment.TopEnd).padding(12.dp).size(30.dp))
        Column(Modifier.align(Alignment.BottomStart).padding(16.dp)) {
            Text(ex.name, color = Color.White, style = VoltType.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("${ex.sets} series · ${ex.reps} reps · ~${ex.sets * 2} min", color = Color.White.copy(alpha = 0.72f), style = VoltType.body.copy(fontSize = 12.sp))
            Spacer(Modifier.height(10.dp))
            Box(
                Modifier.clip(CircleShape).then(if (done) Modifier.background(Color.White.copy(alpha = 0.18f)) else Modifier.background(grad))
                    .padding(horizontal = 18.dp, vertical = 9.dp),
            ) {
                Text(if (done) "Hecho" else "Empezar", color = if (done) Color.White else Volt.onAccent, style = VoltType.body.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp))
            }
        }
    }
}

/** Metrica que "voltea" (slide+fade) cuando cambia el valor. */
@Composable
private fun FlipMetric(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(Volt.surface2).padding(horizontal = 16.dp, vertical = 14.dp)) {
        AnimatedContent(value, label = "flip", transitionSpec = { (slideInVertically { it } + fadeIn()) togetherWith (slideOutVertically { -it } + fadeOut()) }) { v ->
            Text(v, color = Volt.text, style = VoltType.numberM, maxLines = 1)
        }
        Text(label.uppercase(), color = Volt.textDim, style = VoltType.label, maxLines = 1)
    }
}
