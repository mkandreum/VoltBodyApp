package com.voltbody.app.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import androidx.health.connect.client.PermissionController

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

    val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
    val healthPermissionLauncher = rememberLauncherForActivityResult(requestPermissionActivityContract) { _ ->
        viewModel.refresh()
    }

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
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    "Hola, ${state.userName}",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Icon(
                                    Icons.Default.Bolt,
                                    contentDescription = null,
                                    tint = LocalVoltBodyColors.current.accent,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
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

            // ── Health Connect card ───────────────────────────────────────────────
            if (state.healthConnectAvailable) {
                item {
                    StaggeredEntrance(2) {
                        HealthConnectCard(
                            granted = state.healthPermissionsGranted,
                            heartRate = state.heartRate,
                            steps = state.stepsToday,
                            onConnect = {
                                healthPermissionLauncher.launch(viewModel.healthPermissions)
                            }
                        )
                    }
                }
            }

            // ── BLE Heart Rate card ───────────────────────────────────────────────
            item {
                StaggeredEntrance(3) {
                    BleHeartRateCard(
                        state = state.bleState,
                        heartRate = state.bleHeartRate,
                        deviceName = state.bleDeviceName,
                        onToggle = { viewModel.toggleBle() }
                    )
                }
            }

            // ── Recovery Score card ───────────────────────────────────────────────
            item {
                StaggeredEntrance(4) {
                    RecoveryScoreCard(
                        score = state.recoveryScore,
                        sleepHours = state.sleepHours,
                        hrv = state.hrv,
                        onLogRecovery = { viewModel.openRecoveryCheckin() }
                    )
                }
            }

            // ── Motivation card ───────────────────────────────────────────────────
            if (state.motivationPhrase.isNotEmpty() || state.motivationPhotoUrl != null) {
                item {
                    StaggeredEntrance(5) {
                        MotivationCard(
                            phrase = state.motivationPhrase,
                            photoUrl = state.motivationPhotoUrl
                        )
                    }
                }
            }

            // ── Weekly progress widget ────────────────────────────────────────────
            item {
                StaggeredEntrance(6) {
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
                    StaggeredEntrance(7) {
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
                    StaggeredEntrance(8) {
                        FatigueIndexCard(entries = state.fatigueEntries)
                    }
                }
            }

            // ── Progress Report (AI) ──────────────────────────────────────────────
            item {
                StaggeredEntrance(9) {
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
                    StaggeredEntrance(10) {
                        DayTimelineCard(items = state.timelineItems)
                    }
                }
            }

            // ── Quick Actions ─────────────────────────────────────────────────────
            item {
                StaggeredEntrance(11) {
                    QuickActionsCard(
                        onQuickLog = { viewModel.quickLogSet() },
                        onNavigateToAiCoach = onNavigateToAiCoach
                    )
                }
            }

            // ── Volume chart ──────────────────────────────────────────────────────
            if (state.dailyVolumeKg.isNotEmpty()) {
                item {
                    StaggeredEntrance(12) {
                        VolumeChartCard(dailyVolume = state.dailyVolumeKg)
                    }
                }
            }

            // ── AI Coach card ─────────────────────────────────────────────────────
            item {
                StaggeredEntrance(13) {
                    AiCoachCard(onNavigate = onNavigateToAiCoach)
                }
            }

            // ── Recent achievements ───────────────────────────────────────────────
            if (state.recentAchievements.isNotEmpty()) {
                item {
                    StaggeredEntrance(14) {
                        RecentAchievementsCard(achievements = state.recentAchievements)
                    }
                }
            }

            // Bottom spacer for nav bar
            item { Spacer(Modifier.height(100.dp)) }
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
                // FIX: replaced emoji "⚡ Nivel X" with vector icon + text
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        Icons.Default.Bolt,
                        contentDescription = null,
                        tint = vb.accent,
                        modifier = Modifier.size(18.dp)
                    )
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
// FIX: Replaced hardcoded Color(0xFF...) with semantic tokens from Color.kt

@Composable
fun RecoveryScoreCard(
    score: Int?,
    sleepHours: Float?,
    hrv: Int?,
    onLogRecovery: () -> Unit
) {
    // Use semantic color tokens — respects all 3 app themes
    val scoreColor = when {
        score == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        score >= 80   -> ColorSuccess
        score >= 50   -> ColorWarning
        else          -> ColorError
    }
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Recovery Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                if (score != null) {
                    Text(
                        when {
                            score >= 80 -> "Listo para rendir al máximo"
                            score >= 50 -> "Recuperación moderada"
                            else        -> "Necesitas más descanso"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        sleepHours?.let {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Bedtime,
                                    contentDescription = "Horas de sueño",
                                    modifier = Modifier.size(14.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "${"%.1f".format(it)}h",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                        hrv?.takeIf { it > 0 }?.let {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    Icons.Default.Favorite,
                                    contentDescription = "HRV",
                                    modifier = Modifier.size(14.dp),
                                    tint = ColorError
                                )
                                Text(
                                    "HRV $it",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        "Registra tu recuperación",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (score != null) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(scoreColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "$score",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Black,
                        color = scoreColor
                    )
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
                    contentDescription = "Imagen motivacional",
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(20.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(vb.accentDim, vb.bg)
                            )
                        )
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f),
                                Color.Black.copy(alpha = 0.85f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        Icons.Default.Psychology,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.7f),
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "MODO MENTAL",
                        style = UppercaseLabel.copy(letterSpacing = 1.5.sp),
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
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
// FIX: Removed icon-in-colored-circle antipattern; replaced with clean vector icon

@Composable
fun AiCoachCard(onNavigate: () -> Unit) {
    val vb = LocalVoltBodyColors.current
    AppCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onNavigate
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Psychology,
                    contentDescription = "AI Coach",
                    tint = vb.accent,
                    modifier = Modifier.size(28.dp)
                )
                Column {
                    Text(
                        "AI Coach",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Pregúntame lo que necesites",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Ir a AI Coach",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

// ── Recent achievements ───────────────────────────────────────────────────────
// FIX: Replaced emoji "🏆" in title with EmojiEvents vector icon

@Composable
fun RecentAchievementsCard(achievements: List<Achievement>) {
    val vb = LocalVoltBodyColors.current
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = vb.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Logros recientes",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
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
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = vb.accent,
                        modifier = Modifier.size(22.dp)
                    )
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
// FIX: Replaced free-text TextFields with Sliders for better mobile UX

@Composable
fun RecoveryCheckinDialog(
    onConfirm: (Float, Int) -> Unit,
    onDismiss: () -> Unit
) {
    var sleepHours by remember { mutableStateOf(7.5f) }
    var hrv by remember { mutableStateOf(50f) }
    var hasHrv by remember { mutableStateOf(false) }
    val vb = LocalVoltBodyColors.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("¿Cómo te has recuperado?") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                // Sleep hours slider
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                Icons.Default.Bedtime,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text("Horas de sueño", style = MaterialTheme.typography.bodyMedium)
                        }
                        Text(
                            "${"%.1f".format(sleepHours)}h",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = vb.accent
                        )
                    }
                    Slider(
                        value = sleepHours,
                        onValueChange = { sleepHours = it },
                        valueRange = 3f..12f,
                        steps = 17,
                        colors = SliderDefaults.colors(thumbColor = vb.accent, activeTrackColor = vb.accent)
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("3h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text("12h", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // HRV optional toggle
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = ColorError
                        )
                        Text("Registrar HRV (opcional)", style = MaterialTheme.typography.bodyMedium)
                    }
                    Switch(
                        checked = hasHrv,
                        onCheckedChange = { hasHrv = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = ColorBlack, checkedTrackColor = vb.accent)
                    )
                }

                AnimatedVisibility(visible = hasHrv) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("HRV", style = MaterialTheme.typography.bodyMedium)
                            Text(
                                "${hrv.toInt()} ms",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = vb.accent
                            )
                        }
                        Slider(
                            value = hrv,
                            onValueChange = { hrv = it },
                            valueRange = 20f..120f,
                            steps = 99,
                            colors = SliderDefaults.colors(thumbColor = vb.accent, activeTrackColor = vb.accent)
                        )
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("20", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("120 ms", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm(sleepHours, if (hasHrv) hrv.toInt() else 0)
            }) { Text("Guardar") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

// ── Streak badge ──────────────────────────────────────────────────────────────
// FIX: Spring animation was always targeting 1f → never triggered.
//      Now uses appeared flag so it bounces in on first render.

@Composable
fun StreakBadge(days: Int) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
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
// FIX: key(workoutsThisWeek) so spring re-triggers when weekly data updates

@Composable
fun WeeklyProgressCard(
    workoutsThisWeek: Int,
    targetWorkouts: Int,
    totalVolumeKg: Float,
    dailyVolume: List<Float>
) {
    // FIX: key on workoutsThisWeek so animation fires again when data changes,
    // not just on initial composition
    var appeared by remember(workoutsThisWeek) { mutableStateOf(false) }
    LaunchedEffect(workoutsThisWeek) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "week_card_scale"
    )
    AppCard(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale }
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Esta semana",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatChip(label = "Entrenos", value = "$workoutsThisWeek/$targetWorkouts")
                StatChip(label = "Volumen", value = "${"%.0f".format(totalVolumeKg)} kg")
                StatChip(
                    label = "Progreso",
                    value = "${((workoutsThisWeek.toFloat() / targetWorkouts.coerceAtLeast(1)) * 100).toInt()}%"
                )
            }
            if (dailyVolume.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = {
                        (workoutsThisWeek.toFloat() / targetWorkouts.coerceAtLeast(1)).coerceIn(0f, 1f)
                    },
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
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
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
    val haptic = LocalHapticFeedback.current
    AppCard(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "Entreno de hoy",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    workoutName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$exerciseCount ejercicios · ~$estimatedMinutes min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            FilledIconButton(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStart()
                },
                modifier = Modifier.size(52.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = "Empezar entreno")
            }
        }
    }
}

// ── Volume chart (Canvas) ─────────────────────────────────────────────────────
// FIX: Replaced straight lineTo with cubic bezier curves for premium feel

@Composable
fun VolumeChartCard(dailyVolume: List<Float>) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val vb = LocalVoltBodyColors.current
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "Volumen diario (kg)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                drawVolumeChartBezier(dailyVolume, primaryColor, vb.chartFill)
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("L", "M", "X", "J", "V", "S", "D").take(dailyVolume.size).forEach { day ->
                    Text(
                        day,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// FIX: Bezier smooth chart replacing straight-line version
fun DrawScope.drawVolumeChartBezier(volumes: List<Float>, lineColor: Color, fillColor: Color) {
    if (volumes.size < 2) return
    val maxVol = volumes.max().coerceAtLeast(1f)
    val w = size.width
    val h = size.height
    val step = w / (volumes.size - 1).coerceAtLeast(1)

    fun xOf(i: Int) = i * step
    fun yOf(v: Float) = h - (v / maxVol) * h * 0.9f

    val path = Path().apply {
        volumes.forEachIndexed { i, v ->
            val x = xOf(i)
            val y = yOf(v)
            if (i == 0) {
                moveTo(x, y)
            } else {
                val prevX = xOf(i - 1)
                val prevY = yOf(volumes[i - 1])
                val cpX = (prevX + x) / 2f
                cubicTo(cpX, prevY, cpX, y, x, y)
            }
        }
    }

    val fillPath = Path().apply {
        addPath(path)
        lineTo(xOf(volumes.lastIndex), h)
        lineTo(0f, h)
        close()
    }

    drawPath(
        fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(fillColor.copy(alpha = 0.35f), fillColor.copy(alpha = 0.0f))
        )
    )

    drawPath(
        path,
        color = lineColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    volumes.forEachIndexed { i, v ->
        val x = xOf(i)
        val y = yOf(v)
        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
        drawCircle(color = lineColor.copy(alpha = 0.2f), radius = 7.dp.toPx(), center = Offset(x, y))
    }
}

// ── Fatigue Index card ────────────────────────────────────────────────────────
// FIX: Bars upgraded from 6dp to 10dp with horizontal gradient + vector icon title

@Composable
fun FatigueIndexCard(entries: List<FatigueEntry>) {
    val vb = LocalVoltBodyColors.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp)) {
            // FIX: vector icon instead of "⚡" emoji
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = vb.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Índice de Fatiga Semanal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Volumen vs MRV por grupo muscular",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))
            entries.forEach { entry ->
                val barColor = when (entry.status) {
                    FatigueStatus.FRESH       -> ColorSuccess
                    FatigueStatus.MODERATE    -> ColorWarning
                    FatigueStatus.HIGH        -> ColorOrange
                    FatigueStatus.OVERREACHED -> ColorError
                }
                val animatedProgress by animateFloatAsState(
                    targetValue = (entry.percent / 100f).coerceIn(0f, 1f),
                    animationSpec = tween(600, easing = FastOutSlowInEasing),
                    label = "fatigue_${entry.muscleGroup}"
                )
                Column(modifier = Modifier.padding(vertical = 6.dp)) {
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
                    Spacer(Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .clip(RoundedCornerShape(5.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(RoundedCornerShape(5.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            barColor.copy(alpha = 0.6f),
                                            barColor
                                        )
                                    )
                                )
                        )
                    }
                }
            }
        }
    }
}

// ── Progress Report card (AI) ─────────────────────────────────────────────────
// FIX: Replaced emoji "🤖" in title with AutoAwesome vector icon

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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = vb.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Informe IA de progreso",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
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

            AnimatedVisibility(visible = isLoading) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Analizando datos con IA…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "$progress%",
                            style = MaterialTheme.typography.labelSmall,
                            color = vb.accent
                        )
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

            if (report != null) {
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportStatBox("Score total", "${report.overallScore}%", Modifier.weight(1f))
                    ReportStatBox("Progreso", "${report.progressPercent}%", Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ReportStatBox("Consistencia", "${report.consistencyPercent}%", Modifier.weight(1f))
                    ReportStatBox("Te falta", "${report.weeksToVisibleChange} sem", Modifier.weight(1f))
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    report.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (report.improvements.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Qué puedes mejorar",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    report.improvements.forEach { item ->
                        Text("• $item", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 2.dp))
                    }
                }
                if (report.nextActions.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "Siguientes pasos",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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
// FIX: Replaced emoji "📅" in title with CalendarToday vector icon

@Composable
fun DayTimelineCard(items: List<TimelineItem>) {
    val vb = LocalVoltBodyColors.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    tint = vb.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Timeline del día",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(12.dp))
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            if (item.done) vb.accent.copy(alpha = 0.06f) else vb.surface
                        )
                        .then(
                            if (item.done)
                                Modifier.border(1.dp, vb.accent.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            else
                                Modifier.border(1.dp, vb.border, RoundedCornerShape(12.dp))
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(
                                if (item.done) vb.accent
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                    )
                    Text(
                        item.time,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (item.done) "Hecho" else "Pendiente",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (item.done) vb.accent
                                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    if (item.done) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Completado",
                            tint = vb.accent,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                if (index < items.lastIndex) Spacer(Modifier.height(6.dp))
            }
        }
    }
}

// ── Quick Actions card ────────────────────────────────────────────────────────
// FIX: Replaced emoji icon strings with ImageVector icons + press-scale spring

@Composable
fun QuickActionsCard(
    onQuickLog: () -> Unit,
    onNavigateToAiCoach: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    val haptic = LocalHapticFeedback.current
    Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Bolt,
                    contentDescription = null,
                    tint = vb.accent,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    "Acciones rápidas",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Un toque y listo",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                QuickActionButton(
                    icon = Icons.Default.FitnessCenter,
                    contentDesc = "Registrar serie rápida",
                    label = "Registrar serie",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQuickLog()
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.Psychology,
                    contentDesc = "Abrir AI Coach",
                    label = "AI Coach",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToAiCoach()
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = Icons.Default.CameraAlt,
                    contentDesc = "Subir foto de progreso",
                    label = "Subir foto",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// FIX: ImageVector parameter replaces emoji String; press-scale spring animation added
@Composable
private fun QuickActionButton(
    icon: ImageVector,
    contentDesc: String,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
        label = "quick_action_scale"
    )
    Column(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(14.dp))
            .background(vb.surface)
            .border(1.dp, vb.border, RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDesc,
            tint = vb.accent,
            modifier = Modifier.size(26.dp)
        )
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

// ── Health Connect card ───────────────────────────────────────────────────────

@Composable
fun HealthConnectCard(
    granted: Boolean,
    heartRate: Int?,
    steps: Long,
    onConnect: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        if (granted) Icons.Default.Favorite else Icons.Default.HealthAndSafety,
                        contentDescription = if (granted) "Google Health conectado" else "Google Health desconectado",
                        tint = if (granted) ColorError else vb.accent
                    )
                    Text(
                        "Google Health",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (granted) NeonBadge("Sincronizado")
            }
            Spacer(Modifier.height(16.dp))
            if (granted) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Ritmo Cardíaco",
                            style = MaterialTheme.typography.labelSmall,
                            color = vb.textMuted
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                heartRate?.toString() ?: "--",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = ColorWhite
                            )
                            Text(
                                "BPM",
                                style = MaterialTheme.typography.labelSmall,
                                color = vb.textMuted
                            )
                        }
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Pasos Hoy",
                            style = MaterialTheme.typography.labelSmall,
                            color = vb.textMuted
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                steps.toString(),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Black,
                                color = ColorWhite
                            )
                            Icon(
                                Icons.Default.DirectionsRun,
                                contentDescription = "Pasos",
                                modifier = Modifier.size(16.dp),
                                tint = vb.accent
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Conecta con Health Connect para sincronizar tus pasos y ritmo cardíaco automáticamente.",
                    style = MaterialTheme.typography.bodySmall,
                    color = vb.textMuted
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onConnect,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Conectar Google Health")
                }
            }
        }
    }
}

// ── BLE Heart Rate card ───────────────────────────────────────────────────────

@Composable
fun BleHeartRateCard(
    state: String,
    heartRate: Int?,
    deviceName: String?,
    onToggle: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    val haptic = LocalHapticFeedback.current
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (state == "connected") 1.2f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )

    AppCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(20.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(
                            if (state == "connected") vb.accent.copy(alpha = 0.15f)
                            else vb.surfaceElevated
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = "Monitor cardíaco ${if (state == "connected") "conectado" else "desconectado"}",
                        tint = if (state == "connected") vb.accent else vb.textMuted,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                    )
                }
                Column {
                    Text(
                        if (state == "connected") "Sensor en vivo" else "Sensor de pulso",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        when (state) {
                            "connected"  -> deviceName ?: "Conectado"
                            "connecting" -> "Buscando..."
                            "error"      -> "Error de conexión"
                            else         -> "Desconectado"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = vb.textMuted
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state == "connected" && heartRate != null) {
                    Text(
                        "$heartRate",
                        style = MonoMetric.copy(fontSize = 24.sp, fontWeight = FontWeight.Black),
                        color = vb.accent
                    )
                    Text(
                        "BPM",
                        style = UppercaseLabel.copy(fontSize = 10.sp),
                        color = vb.textMuted
                    )
                }
                IconButton(
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onToggle()
                    },
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(
                            if (state == "connected") vb.accent else vb.surfaceElevated
                        )
                ) {
                    Icon(
                        imageVector = if (state == "connected")
                            Icons.Default.BluetoothDisabled
                        else
                            Icons.Default.Bluetooth,
                        contentDescription = if (state == "connected") "Desconectar sensor" else "Conectar sensor",
                        tint = if (state == "connected") ColorBlack else vb.accent
                    )
                }
            }
        }
    }
}
