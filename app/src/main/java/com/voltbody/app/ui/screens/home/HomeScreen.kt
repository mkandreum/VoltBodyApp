package com.voltbody.app.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.*
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.*
import androidx.compose.material3.pulltorefresh.*
import androidx.activity.compose.rememberLauncherForActivityResult
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
    val vb = LocalVoltBodyColors.current

    val requestPermissionActivityContract = PermissionController.createRequestPermissionResultContract()
    val healthPermissionLauncher = rememberLauncherForActivityResult(requestPermissionActivityContract) { _ ->
        viewModel.refresh()
    }

    LiquidGlassScaffold(
        background = {
            // Dynamic background for blur sampling
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(vb.accent.copy(alpha = 0.15f), vb.bg),
                            center = Offset(0f, 0f),
                            radius = 1000f
                        )
                    )
            )
        }
    ) { hazeState ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 80.dp),
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
                            HeadlineGradient(
                                "Hola, ${state.userName} ⚡",
                                style = MaterialTheme.typography.headlineMedium
                            )
                            Text(
                                state.greeting,
                                style = MaterialTheme.typography.bodyMedium,
                                color = vb.textMuted
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
                        todayXP = state.todayXP,
                        hazeState = hazeState
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
                            hazeState = hazeState,
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
                        hazeState = hazeState,
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
                        hazeState = hazeState,
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
                            photoUrl = state.motivationPhotoUrl,
                            hazeState = hazeState
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
                        dailyVolume = state.dailyVolumeKg,
                        hazeState = hazeState
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
                            hazeState = hazeState,
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
                        FatigueIndexCard(entries = state.fatigueEntries, hazeState = hazeState)
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
                        hazeState = hazeState,
                        onGenerate = { viewModel.generateProgressReport() }
                    )
                }
            }

            // ── Day Timeline ──────────────────────────────────────────────────────
            if (state.timelineItems.isNotEmpty()) {
                item {
                    StaggeredEntrance(10) {
                        DayTimelineCard(items = state.timelineItems, hazeState = hazeState)
                    }
                }
            }

            // ── Quick Actions ─────────────────────────────────────────────────────
            item {
                StaggeredEntrance(11) {
                    QuickActionsCard(
                        onQuickLog = { viewModel.quickLogSet() },
                        onNavigateToAiCoach = onNavigateToAiCoach,
                        hazeState = hazeState
                    )
                }
            }

            // ── Volume chart ──────────────────────────────────────────────────────
            if (state.dailyVolumeKg.isNotEmpty()) {
                item {
                    StaggeredEntrance(12) {
                        VolumeChartCard(dailyVolume = state.dailyVolumeKg, hazeState = hazeState)
                    }
                }
            }

            // ── AI Coach card ─────────────────────────────────────────────────────
            item {
                StaggeredEntrance(13) {
                    AiCoachCard(onNavigate = onNavigateToAiCoach, hazeState = hazeState)
                }
            }

            // ── Recent achievements ───────────────────────────────────────────────
            if (state.recentAchievements.isNotEmpty()) {
                item {
                    StaggeredEntrance(14) {
                        RecentAchievementsCard(achievements = state.recentAchievements, hazeState = hazeState)
                    }
                }
            }

            // Bottom spacer for nav bar
            item { Spacer(Modifier.height(80.dp)) }
        }
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
fun XpLevelCard(level: Int, xpCurrent: Int, xpToNext: Int, todayXP: Int = 0, hazeState: HazeState? = null) {
    val progress = if (xpToNext > 0) xpCurrent.toFloat() / xpToNext.toFloat() else 1f
    val animProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "xp_progress"
    )
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        hazeState = hazeState,
        accentGlow = true
    ) {
        Column {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Nivel actual",
                        tint = vb.accent
                    )
                    Text(
                        "NIVEL $level",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = ColorWhite
                    )
                }
                Text(
                    "$xpCurrent / $xpToNext XP",
                    style = MaterialTheme.typography.bodySmall,
                    color = vb.textMuted
                )
            }
            Spacer(Modifier.height(14.dp))
            LiquidProgressBar(progress = progress)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    "${xpToNext - xpCurrent} XP PARA EL SIGUIENTE NIVEL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    ),
                    color = vb.textMuted
                )
                if (todayXP > 0) {
                    GlowText(
                        "+$todayXP XP HOY",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black)
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
    hazeState: HazeState? = null,
    onLogRecovery: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    // Use semantic color tokens — respects all 3 app themes
    val scoreColor = when {
        score == null -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
        score >= 80   -> ColorSuccess
        score >= 50   -> ColorWarning
        else          -> ColorError
    }
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        hazeState = hazeState,
        onClick = onLogRecovery
    ) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Recovery Score",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (score != null) {
                    Text(
                        when {
                            score >= 80 -> "Listo para rendir al máximo"
                            score >= 50 -> "Recuperación moderada"
                            else        -> "Necesitas más descanso"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = vb.textMuted
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        sleepHours?.let {
                            Text(
                                "😴 ${"%.1f".format(it)}h",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorWhite
                            )
                        }
                        hrv?.takeIf { it > 0 }?.let {
                            Text(
                                "💓 HRV $it",
                                style = MaterialTheme.typography.bodySmall,
                                color = ColorWhite
                            )
                        }
                    }
                } else {
                    Text(
                        "Registra tu recuperación",
                        style = MaterialTheme.typography.bodySmall,
                        color = vb.textMuted
                    )
                }
            }
            if (score != null) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(scoreColor.copy(alpha = 0.15f))
                        .border(1.dp, scoreColor.copy(alpha = 0.3f), CircleShape),
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
                LiquidGlassButton(
                    text = "Registrar",
                    onClick = onLogRecovery,
                    hazeState = hazeState,
                    style = LiquidButtonStyle.Primary
                )
            }
        }
    }
}

// ── Motivation card ───────────────────────────────────────────────────────────

@Composable
fun MotivationCard(phrase: String, photoUrl: String? = null, hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        hazeState = hazeState,
        glassAlpha = 0.3f // Muestre más del fondo
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
                                Color.Black.copy(alpha = 0.5f),
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )
            Column(
                modifier = Modifier.align(Alignment.BottomStart).padding(16.dp)
            ) {
                Text(
                    "🧠 MODO MENTAL",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp
                    ),
                    color = vb.accent
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    phrase.ifEmpty { "Hoy toca. Sin excusas." },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = ColorWhite
                )
            }
        }
    }
}

// ── AI Coach card ─────────────────────────────────────────────────────────────
// FIX: Removed icon-in-colored-circle antipattern; replaced with clean vector icon

@Composable
fun AiCoachCard(onNavigate: () -> Unit, hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onNavigate,
        hazeState = hazeState
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

@Composable
fun RecentAchievementsCard(achievements: List<Achievement>, hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Column(Modifier.padding(20.dp)) {
            Text(
                "🏆 Logros recientes",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
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
                        Text(
                            "😴 Horas de sueño",
                            style = MaterialTheme.typography.bodyMedium
                        )
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
                        steps = 17, // 0.5h increments
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
                    Text("💓 Registrar HRV (opcional)", style = MaterialTheme.typography.bodyMedium)
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
// FIX: Added key(workoutsThisWeek) so animation re-triggers when data changes

@Composable
fun WeeklyProgressCard(
    workoutsThisWeek: Int,
    targetWorkouts: Int,
    totalVolumeKg: Float,
    dailyVolume: List<Float>,
    hazeState: HazeState? = null
) {
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val scale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.9f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "week_card_scale"
    )
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth().graphicsLayer { scaleX = scale; scaleY = scale },
        hazeState = hazeState,
        accentGlow = workoutsThisWeek >= targetWorkouts
    ) {
        Column {
            Text(
                "ESTA SEMANA",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = ColorWhite
            )
            Spacer(Modifier.height(14.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                StatChip(label = "ENTRENOS", value = "$workoutsThisWeek/$targetWorkouts")
                StatChip(label = "VOLUMEN", value = "${"%.0f".format(totalVolumeKg)} KG")
                StatChip(
                    label = "PROGRESO",
                    value = "${((workoutsThisWeek.toFloat() / targetWorkouts.coerceAtLeast(1)) * 100).toInt()}%"
                )
            }
            Spacer(Modifier.height(16.dp))
            LiquidProgressBar(
                progress = (workoutsThisWeek.toFloat() / targetWorkouts.coerceAtLeast(1)).coerceIn(0f, 1f)
            )
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
    hazeState: HazeState? = null,
    onStart: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    val haptic = LocalHapticFeedback.current
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        hazeState = hazeState,
        accentGlow = true
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "ENTRENO DE HOY",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ),
                    color = vb.accent
                )
                Text(
                    workoutName,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = ColorWhite
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "$exerciseCount ejercicios · ~$estimatedMinutes min",
                    style = MaterialTheme.typography.bodyMedium,
                    color = vb.textMuted
                )
            }
            
            LiquidGlassButton(
                text = "START",
                onClick = onStart,
                hazeState = hazeState,
                style = LiquidButtonStyle.Primary,
                leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = ColorBlack) }
            )
        }
    }
}

// ── Volume chart (Canvas) ─────────────────────────────────────────────────────
// FIX: Replaced straight lineTo with cubic bezier curves for premium feel

@Composable
fun VolumeChartCard(dailyVolume: List<Float>, hazeState: HazeState? = null) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Column {
            Text(
                "VOLUMEN DIARIO (KG)",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = ColorWhite
            )
            Spacer(Modifier.height(12.dp))
            androidx.compose.foundation.Canvas(
                modifier = Modifier.fillMaxWidth().height(120.dp)
            ) {
                drawVolumeChartBezier(dailyVolume, vb.accent, vb.accent.copy(alpha = 0.2f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("L", "M", "X", "J", "V", "S", "D").take(dailyVolume.size).forEach { day ->
                    Text(
                        day,
                        style = MaterialTheme.typography.labelSmall,
                        color = vb.textMuted
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

    // Build smooth bezier path
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

    // Build fill path by extending curve to bottom
    val fillPath = Path().apply {
        addPath(path)
        lineTo(xOf(volumes.lastIndex), h)
        lineTo(0f, h)
        close()
    }

    // Gradient fill under curve
    drawPath(
        fillPath,
        brush = Brush.verticalGradient(
            colors = listOf(fillColor.copy(alpha = 0.35f), fillColor.copy(alpha = 0.0f))
        )
    )

    // Main line
    drawPath(
        path,
        color = lineColor,
        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
    )

    // Data point dots
    volumes.forEachIndexed { i, v ->
        val x = xOf(i)
        val y = yOf(v)
        drawCircle(color = lineColor, radius = 4.dp.toPx(), center = Offset(x, y))
        drawCircle(color = lineColor.copy(alpha = 0.2f), radius = 7.dp.toPx(), center = Offset(x, y))
    }
}

// ── Fatigue Index card ────────────────────────────────────────────────────────
// FIX: Bars upgraded from 6dp to 10dp with horizontal gradient for premium look

@Composable
fun FatigueIndexCard(entries: List<FatigueEntry>, hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Column {
            Text(
                "⚡ ÍNDICE DE FATIGA SEMANAL",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = ColorWhite
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Volumen vs MRV por grupo muscular",
                style = MaterialTheme.typography.bodySmall,
                color = vb.textMuted
            )
            Spacer(Modifier.height(20.dp))
            entries.forEach { entry ->
                val barColor = when (entry.status) {
                    FatigueStatus.FRESH       -> ColorSuccess
                    FatigueStatus.MODERATE    -> ColorWarning
                    FatigueStatus.HIGH        -> ColorOrange
                    FatigueStatus.OVERREACHED -> ColorError
                }
                Column(modifier = Modifier.padding(vertical = 8.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            entry.muscleGroup.uppercase(),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = ColorWhite
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                fatigueStatusLabel(entry.status),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = barColor
                            )
                            Text(
                                "${entry.weeklyVolume}/${entry.mrv}",
                                style = MaterialTheme.typography.labelSmall,
                                color = vb.textMuted
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    
                    LiquidProgressBar(
                        progress = (entry.percent / 100f).coerceIn(0f, 1f),
                        height = 10.dp
                    )
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
    hazeState: HazeState? = null,
    onGenerate: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Column {
            Text(
                "🤖 INFORME IA DE PROGRESO",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = ColorWhite
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Analiza tus entrenos, rutina, dieta y fotos para ver cómo vas.",
                style = MaterialTheme.typography.bodySmall,
                color = vb.textMuted
            )
            Spacer(Modifier.height(16.dp))

            LiquidGlassButton(
                text = if (isLoading) "Generando informe..." else "Generar informe con IA",
                onClick = onGenerate,
                enabled = !isLoading,
                hazeState = hazeState,
                modifier = Modifier.fillMaxWidth(),
                style = LiquidButtonStyle.Primary,
                leadingIcon = if (isLoading) {
                    { CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = ColorBlack) }
                } else null
            )

            AnimatedVisibility(visible = isLoading) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Analizando datos con IA…",
                            style = MaterialTheme.typography.labelSmall,
                            color = vb.textMuted
                        )
                        Text(
                            "$progress%",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = vb.accent
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    LiquidProgressBar(progress = progress / 100f)
                }
            }

            if (report != null) {
                Spacer(Modifier.height(20.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportStatBox("SCORE TOTAL", "${report.overallScore}%", Modifier.weight(1f))
                    ReportStatBox("PROGRESO", "${report.progressPercent}%", Modifier.weight(1f))
                }
                Spacer(Modifier.height(10.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    ReportStatBox("CONSISTENCIA", "${report.consistencyPercent}%", Modifier.weight(1f))
                    ReportStatBox("TE FALTA", "${report.weeksToVisibleChange} SEM", Modifier.weight(1f))
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    report.summary,
                    style = MaterialTheme.typography.bodyMedium,
                    color = ColorWhite
                )
                if (report.improvements.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "QUÉ PUEDES MEJORAR",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Black),
                        color = vb.accent
                    )
                    report.improvements.forEach { item ->
                        Text("• $item", style = MaterialTheme.typography.bodySmall, color = vb.textMuted, modifier = Modifier.padding(top = 4.dp))
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
fun DayTimelineCard(items: List<TimelineItem>, hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Column {
            Text(
                "📅 TIMELINE DEL DÍA",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = ColorWhite
            )
            Spacer(Modifier.height(16.dp))
            items.forEachIndexed { index, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(
                            if (item.done) vb.accent.copy(alpha = 0.08f) else vb.surfaceElevated.copy(alpha = 0.4f)
                        )
                        .border(
                            1.dp, 
                            if (item.done) vb.accent.copy(alpha = 0.3f) else Color.White.copy(alpha = 0.05f), 
                            RoundedCornerShape(14.dp)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (item.done) vb.accent else vb.textMuted.copy(alpha = 0.3f))
                            .then(if (item.done) Modifier.shadow(8.dp, CircleShape, ambientColor = vb.accent) else Modifier)
                    )
                    Text(
                        item.time,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = vb.textMuted
                    )
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        color = ColorWhite,
                        modifier = Modifier.weight(1f)
                    )
                    if (item.done) {
                        Icon(Icons.Default.CheckCircle, null, tint = vb.accent, modifier = Modifier.size(18.dp))
                    }
                }
                if (index < items.lastIndex) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

// ── Quick Actions card ────────────────────────────────────────────────────────

@Composable
fun QuickActionsCard(
    onQuickLog: () -> Unit,
    onNavigateToAiCoach: () -> Unit,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    val haptic = LocalHapticFeedback.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Column {
            Text(
                "⚡ ACCIONES RÁPIDAS",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = ColorWhite
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                QuickActionButton(
                    icon = "🏋️",
                    label = "REGISTRAR",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onQuickLog()
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = "🤖",
                    label = "COACH IA",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onNavigateToAiCoach()
                    },
                    modifier = Modifier.weight(1f)
                )
                QuickActionButton(
                    icon = "📸",
                    label = "FOTO",
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    },
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
            .neuroRaised(cornerRadius = 18.dp)
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(icon, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                letterSpacing = 0.5.sp,
                fontSize = 10.sp
            ),
            color = ColorWhite,
            maxLines = 1
        )
    }
}

// ── Health Connect card ───────────────────────────────────────────────────────

@Composable
fun HealthConnectCard(
    granted: Boolean,
    heartRate: Int?,
    steps: Long,
    hazeState: HazeState? = null,
    onConnect: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Column {
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
                        contentDescription = null,
                        tint = if (granted) ColorError else vb.accent
                    )
                    Text(
                        "GOOGLE HEALTH",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = ColorWhite
                    )
                }
                if (granted) NeonBadge("CONECTADO")
            }
            Spacer(Modifier.height(20.dp))
            if (granted) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            "RITMO CARDÍACO",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = vb.textMuted
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                heartRate?.toString() ?: "--",
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
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
                            "PASOS HOY",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = vb.textMuted
                        )
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                steps.toString(),
                                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                color = ColorWhite
                            )
                            Icon(
                                Icons.AutoMirrored.Filled.DirectionsRun,
                                contentDescription = null,
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
                Spacer(Modifier.height(16.dp))
                LiquidGlassButton(
                    text = "CONECTAR GOOGLE HEALTH",
                    onClick = onConnect,
                    hazeState = hazeState,
                    modifier = Modifier.fillMaxWidth()
                )
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
    hazeState: HazeState? = null,
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

    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Row(
            modifier = Modifier.fillMaxWidth(),
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
                            else vb.surfaceElevated.copy(alpha = 0.5f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = if (state == "connected") vb.accent else vb.textMuted,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer { scaleX = scale; scaleY = scale }
                    )
                }
                Column {
                    Text(
                        if (state == "connected") "SENSOR EN VIVO" else "SENSOR DE PULSO",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ),
                        color = ColorWhite
                    )
                    Text(
                        when (state) {
                            "connected"  -> deviceName ?: "CONECTADO"
                            "connecting" -> "BUSCANDO..."
                            "error"      -> "ERROR DE CONEXIÓN"
                            else         -> "DESCONECTADO"
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
                    GlowText(
                        "$heartRate",
                        style = MonoMetric.copy(fontSize = 24.sp, fontWeight = FontWeight.Black)
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
                            if (state == "connected") vb.accent else vb.surfaceElevated.copy(alpha = 0.5f)
                        )
                ) {
                    Icon(
                        imageVector = if (state == "connected")
                            Icons.Default.BluetoothDisabled
                        else
                            Icons.Default.Bluetooth,
                        contentDescription = null,
                        tint = if (state == "connected") ColorBlack else vb.accent
                    )
                }
            }
        }
    }
}
