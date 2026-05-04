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
    viewModel: HomeViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Greeting ─────────────────────────────────────────────────────────
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
                // Streak badge
                StreakBadge(days = state.streakDays)
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
            item {
                VolumeChartCard(dailyVolume = state.dailyVolumeKg)
            }
        }
    }
}

// ── Streak badge ─────────────────────────────────────────────────────────────

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
            Text(
                "$days",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onPrimary
            )
            Text(
                "días",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
            )
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
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { scaleX = scale; scaleY = scale },
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
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("Volumen diario (kg)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                drawVolumeChart(dailyVolume, primaryColor, surfaceColor)
            }
            Spacer(Modifier.height(8.dp))
            // Day labels
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
    // Fill area
    val fillPath = Path().apply {
        addPath(path)
        lineTo(w, h)
        lineTo(0f, h)
        close()
    }
    drawPath(fillPath, color = lineColor.copy(alpha = 0.15f))
    drawPath(path, color = lineColor, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    // Dots
    volumes.forEachIndexed { i, v ->
        val x = i * step
        val y = h - (v / maxVol) * h * 0.9f
        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
        drawCircle(color = fillColor, radius = 2.dp.toPx(), center = Offset(x, y))
    }
}
