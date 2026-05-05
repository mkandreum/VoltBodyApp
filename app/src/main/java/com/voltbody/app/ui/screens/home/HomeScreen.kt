package com.voltbody.app.ui.screens.home

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FormatQuote
import androidx.compose.ui.draw.shadow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.theme.*
import com.voltbody.app.util.HapticType
import com.voltbody.app.util.rememberHaptic
import dev.chrisbanes.haze.HazeState

@Composable
fun HomeScreen(
    onNavigateToWorkout: (String) -> Unit,
    onNavigateToDiet: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val vb = LocalVoltBodyColors.current
    val state by viewModel.state.collectAsState()
    val haptic = rememberHaptic()

    LiquidGlassScaffold { hazeState ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 60.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Header (Greeting + Streak)
            item {
                HomeHeader(
                    name = state.userName,
                    progress = state.reportProgress, // Using reportProgress as a placeholder for session % if needed
                    onProfileClick = onNavigateToProfile
                )
            }

            // 2. Daily Quote (Matching Web)
            if (state.motivationPhrase.isNotEmpty()) {
                item {
                    StaggeredEntrance(1) {
                        LiquidGlassCard(hazeState = hazeState, accentGlow = true) {
                            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                Icon(Icons.Outlined.FormatQuote, null, tint = vb.accent, modifier = Modifier.size(20.dp))
                                Text(
                                    state.motivationPhrase,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                                        fontWeight = FontWeight.Medium
                                    ),
                                    color = vb.accent
                                )
                            }
                        }
                    }
                }
            }

            // 3. Hero Card (Today's Conquering)
            item {
                StaggeredEntrance(2) {
                    HeroCard(
                        workout = state.todayWorkout,
                        streak = state.streakDays,
                        level = state.xpLevel,
                        xpInLevel = state.xpCurrent,
                        xpPerLevel = state.xpToNext,
                        progress = state.todayLogs.toFloat() / (state.todayWorkout?.exerciseCount?.coerceAtLeast(1) ?: 1),
                        onStart = {
                            haptic.perform(HapticType.TICK)
                            state.todayWorkout?.let { onNavigateToWorkout(it.id) }
                        },
                        onOptimize = onNavigateToDiet,
                        hazeState = hazeState
                    )
                }
            }

            // 4. XP Progress Bar (Matching Web style)
            item {
                StaggeredEntrance(3) {
                    XpProgressCard(
                        level = state.xpLevel,
                        currentXP = state.xpCurrent,
                        targetXP = state.xpToNext,
                        totalXP = state.totalXP,
                        streak = state.streakDays,
                        logsCount = state.todayLogs, // Using todayLogs as an example
                        hazeState = hazeState
                    )
                }
            }

            // 5. Bento Grid (Matching Web exactly)
            item {
                StaggeredEntrance(4) {
                    BentoGrid(state = state, hazeState = hazeState)
                }
            }

            // 6. Timeline (Coming from web's timelineItems)
            item {
                StaggeredEntrance(5) {
                    TimelineSection(state = state, hazeState = hazeState)
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun HomeHeader(name: String, progress: Int, onProfileClick: () -> Unit) {
    val vb = LocalVoltBodyColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("⚡ VOLTBODY OS", style = UppercaseLabel.copy(fontSize = 11.sp, letterSpacing = 2.sp), color = vb.textMuted)
            Text(
                if (name.isNotEmpty()) "👋 HOLA, ${name.uppercase()}" else "🦁 MODO BESTIA",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                color = ColorWhite
            )
            Text(
                "HOY · $progress% SESIÓN",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = vb.textMuted
            )
        }
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(vb.surfaceElevated.copy(0.3f))
                .border(1.dp, vb.border.copy(0.5f), RoundedCornerShape(16.dp))
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Person, null, tint = vb.accent, modifier = Modifier.size(24.dp))
        }
    }
}

@Composable
private fun HeroCard(
    workout: TodayWorkoutInfo?,
    streak: Int,
    level: Int,
    xpInLevel: Int,
    xpPerLevel: Int,
    progress: Float,
    onStart: () -> Unit,
    onOptimize: () -> Unit,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), accentGlow = true, hazeState = hazeState) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("🏆 HOY CONQUISTAS", style = UppercaseLabel.copy(fontSize = 10.sp, letterSpacing = 1.5.sp), color = vb.textMuted)
                Spacer(Modifier.height(4.dp))
                HeadlineGradient(
                    text = (workout?.name ?: "RECUPERACIÓN ACTIVA").uppercase(),
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (workout != null) "⚡ ${workout.estimatedMinutes} MIN · ${workout.exerciseCount} EJERCICIOS" else "ACTIVA UNA SESIÓN RÁPIDA",
                    style = MaterialTheme.typography.labelSmall,
                    color = vb.textMuted
                )
            }
            VoltBodyCircularProgress(value = progress * 100, size = 68.dp)
        }
        
        Spacer(Modifier.height(20.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(modifier = Modifier.weight(1f).neuroRaised(12.dp).padding(10.dp)) {
                Column {
                    Text("$streak🔥", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                    Text("RACHA DÍAS", style = UppercaseLabel.copy(fontSize = 9.sp), color = vb.textMuted)
                }
            }
            Box(modifier = Modifier.weight(1f).neuroRaised(12.dp).padding(10.dp)) {
                Column {
                    Text("NV. $level⚡", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                    Text("$xpInLevel/$xpPerLevel XP", style = UppercaseLabel.copy(fontSize = 9.sp), color = vb.textMuted)
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            LiquidGlassButton(
                text = "EMPEZAR SESIÓN",
                onClick = onStart,
                modifier = Modifier.weight(1.2f),
                style = LiquidButtonStyle.Primary,
                leadingIcon = { Icon(Icons.Default.Bolt, null, size = 16.dp) }
            )
            LiquidGlassButton(
                text = "DIETA 🍽️",
                onClick = onOptimize,
                modifier = Modifier.weight(0.8f),
                style = LiquidButtonStyle.Secondary
            )
        }
    }
}

@Composable
private fun XpProgressCard(
    level: Int,
    currentXP: Int,
    targetXP: Int,
    totalXP: Int,
    streak: Int,
    logsCount: Int,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Default.Bolt, null, tint = vb.accent, modifier = Modifier.size(14.dp))
                Text("NIVEL $level", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                Text("· $totalXP XP TOTAL", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = vb.textMuted)
            }
            Text("$currentXP/$targetXP XP", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = vb.textMuted)
        }
        Spacer(modifier = Modifier.height(10.dp))
        LiquidProgressBar(progress = currentXP.toFloat() / targetXP.coerceAtLeast(1), height = 6.dp)
        Spacer(modifier = Modifier.height(12.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("🔥 $streak DÍAS RACHA", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = vb.textMuted)
            Text("💪 $logsCount SERIES HOY", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = vb.textMuted)
        }
    }
}

@Composable
private fun BentoGrid(state: HomeState, hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Large Consistency Card
        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("📊 CONSISTENCIA SEMANAL", style = UppercaseLabel.copy(fontSize = 9.sp), color = vb.textMuted)
                    Text("85%", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                    Text("🔥 ${state.streakDays} DÍAS EN RACHA", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                }
                Icon(Icons.Default.QueryStats, null, tint = vb.accent)
            }
            Spacer(Modifier.height(24.dp))
            // High-fidelity area chart matching web
            VoltBodyAreaChart(
                data = listOf(0.4f, 0.7f, 0.5f, 0.8f, 0.9f, 0.6f, 0.85f),
                modifier = Modifier.fillMaxWidth().height(80.dp)
            )
        }

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            BentoSmallCard(
                title = "OBJETIVO CALÓRICO",
                value = "${state.weeklyTarget * 500}", // Example
                subtitle = "🍽️ ${state.weeklyTarget} COMIDAS",
                icon = Icons.Default.Whatshot,
                modifier = Modifier.weight(1f),
                hazeState = hazeState
            )
            BentoSmallCard(
                title = "RECUPERACIÓN",
                value = "92%",
                subtitle = "🌙 ESTADO DE DESCANSO",
                icon = Icons.Default.NightsStay,
                modifier = Modifier.weight(1f),
                hazeState = hazeState
            )
        }
    }
}

@Composable
private fun BentoSmallCard(
    title: String,
    value: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = modifier, hazeState = hazeState) {
        Icon(icon, null, tint = vb.accent, modifier = Modifier.size(18.dp))
        Spacer(Modifier.height(12.dp))
        Text(value, style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = ColorWhite)
        Text(title, style = UppercaseLabel.copy(fontSize = 8.sp), color = vb.textMuted)
        Text(subtitle, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = vb.textMuted)
    }
}

@Composable
private fun TimelineSection(state: HomeState, hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Text("📅 LÍNEA DE TIEMPO", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
        Spacer(Modifier.height(20.dp))
        
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            TimelineRow(time = "07:30", title = "PROTEÍNA + AVENA", done = true)
            TimelineRow(time = "10:30", title = "ENTRENO FUERZA", done = true)
            TimelineRow(time = "14:00", title = "POLLO + ARROZ", done = false)
            TimelineRow(time = "18:00", title = "BATIDO + FRUTA", done = false)
        }
    }
}

@Composable
private fun TimelineRow(time: String, title: String, done: Boolean) {
    val vb = LocalVoltBodyColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(60.dp)) {
            Text(time, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = if (done) vb.accent else vb.textMuted)
        }
        
        // Glowing dot matching web
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(if (done) vb.accent else vb.surfaceElevated.copy(0.8f))
                .then(if (done) Modifier.shadow(8.dp, CircleShape, spotColor = vb.accent) else Modifier)
                .border(1.dp, if (done) vb.accent.copy(0.5f) else vb.border.copy(0.2f), CircleShape)
        )
        
        Text(
            title,
            style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (done) FontWeight.Black else FontWeight.Medium),
            color = if (done) ColorWhite else vb.textMuted
        )
    }
}
