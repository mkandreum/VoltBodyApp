package com.voltbody.app.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
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
fun ProfileScreen(
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val vb = LocalVoltBodyColors.current
    val state by viewModel.state.collectAsState()
    val haptic = rememberHaptic()

    val weeklyGoalProgress = if (state.completedWeeklyGoals.isNotEmpty()) {
        (state.completedWeeklyGoals.count { it.completed } * 100) / state.completedWeeklyGoals.size
    } else 0

    LiquidGlassScaffold { hazeState ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 60.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Header (Matching Web)
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Person, null, tint = vb.accent, modifier = Modifier.size(32.dp))
                        Text("👤 PERFIL", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                    }
                    IconButton(onClick = { /* Edit logic */ }, modifier = Modifier.neuroRaised(12.dp)) {
                        Icon(Icons.Default.Edit, null, tint = vb.textMuted, modifier = Modifier.size(20.dp))
                    }
                }
            }

            // 2. Profile Summary (Avatar + Goal)
            item {
                StaggeredEntrance(1) {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Box {
                                Box(
                                    modifier = Modifier
                                        .size(80.dp)
                                        .clip(CircleShape)
                                        .background(vb.surfaceElevated.copy(0.3f))
                                        .border(2.dp, vb.accent, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, null, tint = vb.accent, modifier = Modifier.size(40.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .align(Alignment.BottomEnd)
                                        .clip(CircleShape)
                                        .background(vb.accent)
                                        .clickable { haptic.perform(HapticType.TICK) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.CameraAlt, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                                }
                            }
                            Column {
                                HeadlineGradient(
                                    text = (state.name.ifEmpty { "USUARIO VOLT" }).uppercase(),
                                    style = MaterialTheme.typography.titleLarge
                                )
                                GlowText(
                                    state.goal.uppercase(),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                                )
                            }
                        }
                    }
                }
            }

            // 3. Stats Grid (2x2)
            item {
                StaggeredEntrance(2) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfileStatItem("PESO", "${state.weightKg} KG", Icons.Default.Scale, Modifier.weight(1f), hazeState)
                            ProfileStatItem("ALTURA", "${state.heightCm} CM", Icons.Default.Straighten, Modifier.weight(1f), hazeState)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            ProfileStatItem("NIVEL", "BRONCE", Icons.Default.Bolt, Modifier.weight(1f), hazeState)
                            ProfileStatItem("EDAD", "${state.age} AÑOS", Icons.Default.Schedule, Modifier.weight(1f), hazeState)
                        }
                    }
                }
            }

            // 4. Fitness Indicators
            item {
                StaggeredEntrance(3) {
                    LiquidGlassCard(hazeState = hazeState) {
                        Text("📊 INDICADORES DE FORMA", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                        Spacer(Modifier.height(20.dp))
                        FitnessIndicator("💪 FUERZA", 75)
                        Spacer(Modifier.height(16.dp))
                        FitnessIndicator("🔥 CONSISTENCIA", weeklyGoalProgress)
                        Spacer(Modifier.height(16.dp))
                        FitnessIndicator("⚡ ENERGÍA", 60)
                    }
                }
            }

            // 5. Progress Photos (Placeholder)
            item {
                StaggeredEntrance(4) {
                    LiquidGlassCard(hazeState = hazeState) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("📸 FOTOS DE PROGRESO", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                            IconButton(onClick = { /* Add photo */ }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Add, null, tint = vb.accent)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            if (state.progressPhotos.isEmpty()) {
                                repeat(4) {
                                    Box(
                                        modifier = Modifier
                                            .size(width = 120.dp, height = 160.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .background(vb.surfaceElevated.copy(0.3f))
                                            .border(1.dp, vb.border.copy(0.2f), RoundedCornerShape(12.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Image, null, tint = vb.textMuted.copy(0.3f))
                                    }
                                }
                            } else {
                                state.progressPhotos.forEach { photo ->
                                    VoltBodyImage(
                                        model = photo.url,
                                        contentDescription = "Progreso ${photo.date}",
                                        modifier = Modifier
                                            .size(width = 120.dp, height = 160.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                            .border(1.dp, vb.border.copy(0.2f), RoundedCornerShape(12.dp))
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 6. Weight Chart Placeholder
            item {
                StaggeredEntrance(5) {
                    LiquidGlassCard(hazeState = hazeState) {
                        Text("⚖️ REGISTRO DE PESO SEMANAL", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                        Spacer(Modifier.height(16.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(100.dp).background(vb.accent.copy(0.05f), RoundedCornerShape(12.dp)))
                    }
                }
            }

            // 7. Personal Records (Trophy Section)
            item {
                StaggeredEntrance(6) {
                    LiquidGlassCard(hazeState = hazeState) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.EmojiEvents, null, tint = vb.accent, modifier = Modifier.size(18.dp))
                            Text("🏆 RÉCORDS PERSONALES", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                        }
                        Spacer(Modifier.height(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            if (state.personalRecords.isEmpty()) {
                                Text("Aún no tienes récords registrados.", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                            } else {
                                state.personalRecords.forEach { pr ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth().neuroRaised(12.dp).padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column {
                                            Text(pr.exerciseName.uppercase(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                                            Text(pr.date, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = vb.textMuted)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("${pr.weight}KG", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black), color = vb.accent)
                                            Text("× ${pr.reps} REPS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = vb.textMuted)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 8. Weekly Goals (Matching Web)
            item {
                StaggeredEntrance(7) {
                    LiquidGlassCard(hazeState = hazeState) {
                        Text("TARGET METAS SEMANALES", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                        Spacer(Modifier.height(12.dp))
                        LiquidProgressBar(progress = weeklyGoalProgress / 100f, height = 6.dp)
                        Spacer(Modifier.height(20.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.completedWeeklyGoals.forEach { goal ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (goal.completed) vb.accent.copy(0.1f) else vb.surfaceElevated.copy(0.3f))
                                        .border(1.dp, if (goal.completed) vb.accent.copy(0.5f) else vb.border.copy(0.3f), RoundedCornerShape(12.dp))
                                        .clickable { haptic.perform(HapticType.TICK); viewModel.toggleWeeklyGoal(goal.id) }
                                        .padding(12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Text(if (goal.completed) "✅" else "⬜")
                                        Text(
                                            goal.label.uppercase(),
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (goal.completed) FontWeight.Black else FontWeight.Bold),
                                            color = if (goal.completed) ColorWhite else vb.textMuted
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 9. Logout Button
            item {
                LiquidGlassButton(
                    text = "🚪 CERRAR SESIÓN",
                    onClick = { haptic.perform(HapticType.TICK); viewModel.logout(); onLogout() },
                    modifier = Modifier.fillMaxWidth(),
                    style = LiquidButtonStyle.Secondary
                )
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun ProfileStatItem(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    Box(
        modifier = modifier
            .neuroRaised(20.dp)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(icon, null, tint = vb.accent, modifier = Modifier.size(24.dp))
            Column {
                Text(label, style = UppercaseLabel.copy(fontSize = 8.sp), color = vb.textMuted)
                Text(value, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
            }
        }
    }
}

@Composable
private fun FitnessIndicator(label: String, progress: Int) {
    val vb = LocalVoltBodyColors.current
    Column {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
            Text("$progress%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = ColorWhite)
        }
        Spacer(Modifier.height(8.dp))
        LiquidProgressBar(progress = progress / 100f, height = 6.dp)
    }
}
