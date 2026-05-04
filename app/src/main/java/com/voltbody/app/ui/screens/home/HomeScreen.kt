package com.voltbody.app.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.voltbody.app.data.remote.dto.ProgressReportResponse
import com.voltbody.app.domain.model.Achievement
import com.voltbody.app.domain.usecase.FatigueEntry
import com.voltbody.app.domain.usecase.FatigueStatus
import com.voltbody.app.domain.usecase.fatigueStatusLabel
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartWorkout: (String) -> Unit = {},
    onNavigateToAiCoach: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    val isRefreshing by viewModel.isRefreshing.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Greeting ──────────────────────────────────────────────────────────
            item {
                StaggeredEntrance(0) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Hola, ${state.userName} ⚡",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                state.greeting,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        StreakBadge(days = state.streakDays)
                    }
                }
            }

            // ── XP / Level card ───────────────────────────────────────────────────
            item {
                StaggeredEntrance(1) {
                    XpLevelCard(
                        level = state.xpLevel,
                        xpCurrent = state.xpCurrent,
                        xpToNext = state.xpToNext,
                        todayXP = state.todayXP
                    )
                }
            }

            // ── Recovery Score card ───────────────────────────────────────────────
            item {
                StaggeredEntrance(2) {
                    RecoveryScoreCard(
                        score = state.recoveryScore,
                        sleepHours = state.sleepHours,
                        hrv = state.hrv,
                        onLogRecovery = { viewModel.openRecoveryCheckin() }
                    )
                }
            }

            // ── Motivation card (enhanced with photo) ─────────────────────────────
            if (state.motivationPhrase.isNotEmpty() || state.motivationPhotoUrl != null) {
                item {
                    StaggeredEntrance(3) {
                        MotivationCard(
                            phrase = state.motivationPhrase,
                            photoUrl = state.motivationPhotoUrl
                        )
                    }
                }
            }

            // ── Weekly progress widget ────────────────────────────────────────────
            item {
                StaggeredEntrance(4) {
                    WeeklyProgressCard(
                        workoutsThisWeek = state.weeklyWorkouts,
                        targetWorkouts = state.weeklyTarget,
                        totalVolumeKg = state.weeklyVolumeKg,
                        dailyVolume = state.dailyVolumeKg
                    )
                }
            }

            // ── Today's workout card ──────────────────────────────────────────────
            state.todayWorkout?.let { workout ->
                item {
                    StaggeredEntrance(5) {
                        TodayWorkoutCard(
                            workoutName = workout.name,
                            exerciseCount = workout.exerciseCount,
                            estimatedMinutes = workout.estimatedMinutes,
                            onStart = { onStartWorkout(workout.id) }
                        )
                    }
                }
            } ?: item {
                if (state.isLoading) VoltBodyLoadingIndicator()
            }

            // ── Fatigue Index ─────────────────────────────────────────────────────
            if (state.fatigueEntries.isNotEmpty()) {
                item {
                    StaggeredEntrance(6) {
                        FatigueIndexCard(entries = state.fatigueEntries)
                    }
                }
            }

            // ── Progress Report (AI) ──────────────────────────────────────────────
            item {
                StaggeredEntrance(7) {
                    ProgressReportCard(
                        report = state.report,
                        isLoading = state.reportLoading,
                        progress = state.reportProgress,
                        onGenerate = { viewModel.generateProgressReport() }
                    )
                }
            }

            // ── Day Timeline ──────────────────────────────────────────────────────
            if (state.timelineItems.isNotEmpty()) {
                item {
                    StaggeredEntrance(8) {
                        DayTimelineCard(items = state.timelineItems)
                    }
                }
            }

            // ── Quick Actions ─────────────────────────────────────────────────────
            item {
                StaggeredEntrance(9) {
                    QuickActionsCard(
                        onQuickLog = { viewModel.quickLogSet() },
                        onNavigateToAiCoach = onNavigateToAiCoach
                    )
                }
            }

            // ── Volume chart ──────────────────────────────────────────────────────
            if (state.dailyVolumeKg.isNotEmpty()) {
                item {
                    StaggeredEntrance(10) {
                        VolumeChartCard(dailyVolume = state.dailyVolumeKg)
                    }
                }
            }

            // ── AI Coach card ─────────────────────────────────────────────────────
            item {
                StaggeredEntrance(11) {
                    AiCoachCard(onNavigate = onNavigateToAiCoach)
                }
            }

            // ── Recent achievements (enhanced with labels & descriptions) ─────────
            if (state.recentAchievements.isNotEmpty()) {
                item {
                    StaggeredEntrance(12) {
                        RecentAchievementsCard(achievements = state.recentAchievements)
                    }
                }
            }

            // Bottom spacer for nav bar
            item { Spacer(Modifier.height(80.dp)) }
        }
    }

    // ── Recovery check-in dialog ──────────────────────────────────────────────
    if (state.showRecoveryDialog) {
        RecoveryCheckinDialog(
            onConfirm = { sleep, hrv -> viewModel.logRecovery(sleep, hrv) },
            onDismiss = { viewModel.dismissRecoveryDialog() }
        )
    }
}

// ── XP / Level card ───────────────────────────────────────────────────────────

@Composable
fun XpLevelCard(level: Int, xpCurrent: Int, xpToNext: Int, todayXP: Int = 0) {
    val progress = if (xpToNext > 0) xpCurrent.toFloat() / xpToNext.toFloat() else 1f
    val animProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "xp_progress"
    )
    val vb = LocalVoltBodyColors.current
    AppCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = vb.accent)
                    Text(
                        "⚡ Nivel $level",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Text(
                    "$xpCurrent / $xpToNext XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { animProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
                color = vb.accent,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f)
            )
            Spacer(Modifier.height(6.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${xpToNext - xpCurrent} XP para el siguiente nivel",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
                if (todayXP > 0) {
                    Text(
                        "+$todayXP XP hoy",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = vb.accent
                    )
                }
            }
        }
    }
}

// ── Recovery Score card ───────────────────────────────────────────────────────

@Composable
fun RecoveryScoreCard(
    score: Int?,
    sleepHours: Float?,
    hrv: Int?,
    onLogRecovery: () -> Unit
) {
    val scoreColor = when {
        score == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        score >= 80 -> Color(0xFF4CAF50)
        score >= 50 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Recovery Score", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (score != null) {
                    Text(
                        when {
                            score >= 80 -> "Listo para rendir al máximo"
                            score >= 50 -> "Recuperación moderada"
                            else -> "Necesitas más descanso"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        sleepHours?.let { Text("😴 ${"%.1f".format(it)}h", style = MaterialTheme.typography.bodySmall) }
                        hrv?.takeIf { it > 0 }?.let { Text("💓 HRV $it", style = MaterialTheme.typography.bodySmall) }
                    }
                } else {
                    Text("Registra tu recuperación", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (score != null) {
                Box(
                    modifier = Modifier.size(60.dp).clip(CircleShape)
                        .background(scoreColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("$score", style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black, color = scoreColor)
                }
            } else {
                FilledTonalButton(onClick = onLogRecovery) { Text("Registrar") }
            }
        }
    }
}

// ── Motivation card ───────────────────────────────────────────────────────────

@Composable
fun MotivationCard(phrase: String, photoUrl: String? = null) {
    val vb = LocalVoltBodyColors.current
    AppCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(170.dp)) {
            if (photoUrl != null) {
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Motivación",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(vb.accentDim, vb.bg)
                            )
                        )
                )
            }
            // Gradient overlay
            Box(
                modifier = Modifier.fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f), Color.Black.copy(alpha = 0.85f))
                        )
                    )
            )
            // Text on top
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                Text(
                    "🧠 MODO MENTAL",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.7f),
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    phrase.ifEmpty { "Hoy toca. Sin excusas." },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}

// ── AI Coach card ─────────────────────────────────────────────────────────────

@Composable
fun AiCoachCard(onNavigate: () -> Unit) {
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onNavigate
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🤖", style = MaterialTheme.typography.titleMedium)
                }
                Column {
                    Text("AI Coach", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Pregúntame lo que necesites", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ── Recent achievements ───────────────────────────────────────────────────────

@Composable
fun RecentAchievementsCard(achievements: List<Achievement>) {
    val vb = LocalVoltBodyColors.current
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("🏆 Logros recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            achievements.take(5).forEach { a ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(vb.surface)
                        .border(1.dp, vb.border, RoundedCornerShape(12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(a.icon, style = MaterialTheme.typography.titleLarge)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            a.label,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = vb.accent
                        )
                        Text(
                            a.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// ── Recovery check-in dialog ──────────────────────────────────────────────────

@Composable
fun RecoveryCheckinDialog(
    onConfirm: (Float, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sleep by remember { mutableStateOf("8.0") }
    var hrv by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cómo te has recuperado?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = sleep, onValueChange = { sleep = it },
                    label = { Text("Horas de sueño") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = hrv, onValueChange = { hrv = it },
                    label = { Text("HRV (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(sleep.toFloatOrNull() ?: 8f, hrv.toIntOrNull() ?: 0)
            }) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

// ── Streak badge ──────────────────────────────────────────────────────────────

@Composable
fun StreakBadge(days: Int) {
    val scale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 600f),
        label = "streak_scale"
    )
    Box(
        modifier = Modifier
            .size(64.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(CircleShape)
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primaryContainer
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$days", style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onPrimary)
            Text("días", style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f))
        }
    }
}

// ── Weekly progress card ──────────────────────────────────────────────────────

@Composable
fun WeeklyProgressCard(
    workoutsThisWeek: Int,
    targetWorkouts: Int,
    totalVolumeKg: Float,
    dailyVolume: List<Float>
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "week_card_scale"
    )
    AppCard(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Esta semana", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatChip(label = "Entrenos", value = "$workoutsThisWeek/$targetWorkouts")
                StatChip(label = "Volumen", value = "${"%.0f".format(totalVolumeKg)} kg")
                StatChip(label = "Progreso", value = "${((workoutsThisWeek.toFloat() / targetWorkouts.coerceAtLeast(1)) * 100).toInt()}%")
            }
            if (dailyVolume.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = { (workoutsThisWeek.toFloat() / targetWorkouts.coerceAtLeast(1)).coerceIn(0f, 1f) },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── Today workout card ────────────────────────────────────────────────────────

@Composable
fun TodayWorkoutCard(
    workoutName: String,
    exerciseCount: Int,
    estimatedMinutes: Int,
    onStart: () -> Unit
) {
    AppCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Entreno de hoy", style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                Text(workoutName, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Spacer(Modifier.height(4.dp))
                Text("$exerciseCount ejercicios · ~$estimatedMinutes min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
            FilledIconButton(onClick = onStart, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Empezar")
            }
        }
    }
}

// ── Volume chart (Canvas) ─────────────────────────────────────────────────────

@Composable
fun VolumeChartCard(dailyVolume: List<Float>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surfaceVariant
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text("Volumen diario (kg)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                drawVolumeChart(dailyVolume, primaryColor, surfaceColor)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("L", "M", "X", "J", "V", "S", "D").take(dailyVolume.size).forEach { day ->
                    Text(day, style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

fun DrawScope.drawVolumeChart(volumes: List<Float>, lineColor: Color, fillColor: Color) {
    if (volumes.isEmpty()) return
    val maxVol = volumes.max().coerceAtLeast(1f)
    val w = size.width
    val h = size.height
    val step = w / (volumes.size - 1).coerceAtLeast(1)
    val path = Path().apply {
        volumes.forEachIndexed { i, v ->
            val x = i * step
            val y = h - (v / maxVol) * h * 0.9f
            if (i == 0) moveTo(x, y) else lineTo(x, y)
        }
    }
    val fillPath = Path().apply {
        addPath(path); lineTo(w, h); lineTo(0f, h); close()
    }
    drawPath(fillPath, color = lineColor.copy(alpha = 0.15f))
    drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    volumes.forEachIndexed { i, v ->
        val x = i * step
        val y = h - (v / maxVol) * h * 0.9f
        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
        drawCircle(color = fillColor, radius = 2.dp.toPx(), center = Offset(x, y))
    }
}

// ── Fatigue Index card ────────────────────────────────────────────────────────

@Composable
fun FatigueIndexCard(entries: List<FatigueEntry>) {
    val vb = LocalVoltBodyColors.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "⚡ Índice de Fatiga Semanal",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Volumen vs MRV por grupo muscular",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            entries.forEach { entry ->
                val barColor = when (entry.status) {
                    FatigueStatus.FRESH -> ColorSuccess
                    FatigueStatus.MODERATE -> ColorWarning
                    FatigueStatus.HIGH -> ColorOrange
                    FatigueStatus.OVERREACHED -> ColorError
                }
                val animatedProgress by animateFloatAsState(
                    targetValue = (entry.percent / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                    label = "fatigue_${entry.muscleGroup}"
                )
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            entry.muscleGroup,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                fatigueStatusLabel(entry.status),
                                style = MaterialTheme.typography.labelSmall,
                                color = barColor
                            )
                            Text(
                                "${entry.weeklyVolume}/${entry.mrv}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.1f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(RoundedCornerShape(3.dp))
                                .background(barColor)
                        )
                    }
                }
            }
        }
    }
}

// ── Progress Report card (AI) ─────────────────────────────────────────────────

@Composable
fun ProgressReportCard(
    report: ProgressReportResponse?,
    isLoading: Boolean,
    progress: Int,
    onGenerate: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "🤖 Informe IA de progreso",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Analiza tus entrenos, rutina, dieta y fotos para ver cómo vas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onGenerate,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                    Spacer(Modifier.width(8.dp))
                }
                Text(if (isLoading) "Generando informe..." else "Generar informe con IA")
            }

            // Loading progress bar
            AnimatedVisibility(visible = isLoading) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Analizando datos con IA…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("$progress%", style = MaterialTheme.typography.labelSmall, color = vb.accent)
                    }
                    Spacer(Modifier.height(4.dp))
                    val animProgress by animateFloatAsState(
                        targetValue = progress / 100f,
                        animationSpec = tween(350, easing = FastOutSlowInEasing),
                        label = "report_progress"
                    )
                    LinearProgressIndicator(
                        progress = { animProgress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = vb.accent
                    )
                }
            }

            // Report results
            if (report != null) {
                Spacer(Modifier.height(16.dp))
                // Stats grid (2x2)
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportStatBox("Score total", "${report.overallScore}%", Modifier.weight(1f))
                    ReportStatBox("Progreso", "${report.progressPercent}%", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportStatBox("Consistencia", "${report.consistencyPercent}%", Modifier.weight(1f))
                    ReportStatBox("Te falta", "${report.weeksToVisibleChange} sem", Modifier.weight(1f))
                }
                // Summary
                Spacer(Modifier.height(12.dp))
                Text(
                    report.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                // Improvements
                if (report.improvements.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Qué puedes mejorar", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    report.improvements.forEach { item ->
                        Text("• $item", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                // Next actions
                if (report.nextActions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text("Siguientes pasos", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    report.nextActions.forEach { item ->
                        Text("• $item", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ReportStatBox(label: String, value: String, modifier: Modifier = Modifier) {
    val vb = LocalVoltBodyColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(vb.surface)
            .border(1.dp, vb.border, RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(
            label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
    }
}

// ── Day Timeline card ─────────────────────────────────────────────────────────

@Composable
fun DayTimelineCard(items: List<TimelineItem>) {
    val vb = LocalVoltBodyColors.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "📅 Timeline del día",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (item.done) vb.accent.copy(alpha = 0.06f)
                            else vb.surface
                        )
                        .then(
                            if (item.done) Modifier.border(1.dp, vb.accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            else Modifier.border(1.dp, vb.border, RoundedCornerShape(12.dp))
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (item.done) vb.accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                    )
                    // Time
                    Text(
                        item.time,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Title
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    // Status label
                    Text(
                        if (item.done) "Hecho ✓" else "Pendiente",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.done) vb.accent else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
                if (index < items.lastIndex) Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ── Quick Actions card ────────────────────────────────────────────────────────

@Composable
fun QuickActionsCard(
    onQuickLog: () -> Unit,
    onNavigateToAiCoach: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "⚡ Acciones rápidas",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(4.dp))
            Text("Un toque y listo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = "🏋️",
                    label = "Registrar serie",
                    onClick = onQuickLog,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = "🤖",
                    label = "AI Coach",
                    onClick = onNavigateToAiCoach,
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = "📸",
                    label = "Subir foto",
                    onClick = { /* navigate to profile */ },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(vb.surface)
            .border(1.dp, vb.border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

