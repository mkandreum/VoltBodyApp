package com.voltbody.app.ui.screens.home

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.voltbody.app.ui.components.VoltBodyLoadingIndicator

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onStartWorkout: (String) -> Unit = {},
    onNavigateToAiCoach: () -> Unit = {},
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Greeting ──────────────────────────────────────────────────────────
        item {
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

        // ── XP / Level card ───────────────────────────────────────────────────
        item {
            XpLevelCard(
                level = state.xpLevel,
                xpCurrent = state.xpCurrent,
                xpToNext = state.xpToNext
            )
        }

        // ── Recovery Score card ───────────────────────────────────────────────
        item {
            RecoveryScoreCard(
                score = state.recoveryScore,
                sleepHours = state.sleepHours,
                hrv = state.hrv,
                onLogRecovery = { viewModel.openRecoveryCheckin() }
            )
        }

        // ── Motivation card ───────────────────────────────────────────────────
        if (state.motivationPhrase.isNotEmpty()) {
            item {
                MotivationCard(phrase = state.motivationPhrase)
            }
        }

        // ── Weekly progress widget ────────────────────────────────────────────
        item {
            WeeklyProgressCard(
                workoutsThisWeek = state.weeklyWorkouts,
                targetWorkouts = state.weeklyTarget,
                totalVolumeKg = state.weeklyVolumeKg,
                dailyVolume = state.dailyVolumeKg
            )
        }

        // ── Today's workout card ──────────────────────────────────────────────
        state.todayWorkout?.let { workout ->
            item {
                TodayWorkoutCard(
                    workoutName = workout.name,
                    exerciseCount = workout.exerciseCount,
                    estimatedMinutes = workout.estimatedMinutes,
                    onStart = { onStartWorkout(workout.id) }
                )
            }
        } ?: item {
            if (state.isLoading) VoltBodyLoadingIndicator()
        }

        // ── Volume chart ──────────────────────────────────────────────────────
        if (state.dailyVolumeKg.isNotEmpty()) {
            item { VolumeChartCard(dailyVolume = state.dailyVolumeKg) }
        }

        // ── AI Coach card ─────────────────────────────────────────────────────
        item { AiCoachCard(onNavigate = onNavigateToAiCoach) }

        // ── Recent achievements ───────────────────────────────────────────────
        if (state.recentAchievements.isNotEmpty()) {
            item { RecentAchievementsCard(achievements = state.recentAchievements) }
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
fun XpLevelCard(level: Int, xpCurrent: Int, xpToNext: Int) {
    val progress = if (xpToNext > 0) xpCurrent.toFloat() / xpToNext.toFloat() else 1f
    val animProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "xp_progress"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Text(
                        "Nivel $level",
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
                color = MaterialTheme.colorScheme.secondary,
                trackColor = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.15f)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                "${xpToNext - xpCurrent} XP para el siguiente nivel",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
            )
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
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
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
fun MotivationCard(phrase: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("💬", style = MaterialTheme.typography.titleLarge)
            Text(
                phrase,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

// ── AI Coach card ─────────────────────────────────────────────────────────────

@Composable
fun AiCoachCard(onNavigate: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onNavigate),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
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

data class AchievementPreview(val icon: String, val label: String)

@Composable
fun RecentAchievementsCard(achievements: List<AchievementPreview>) {
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp)) {
            Text("Logros recientes", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                achievements.take(4).forEach { a ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(a.icon, style = MaterialTheme.typography.titleMedium)
                        Text(a.label, style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer)
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
    Card(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
        shape = RoundedCornerShape(20.dp)
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
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
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
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
