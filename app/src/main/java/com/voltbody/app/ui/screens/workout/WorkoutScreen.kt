package com.voltbody.app.ui.screens.workout

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import com.voltbody.app.domain.model.Exercise
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.theme.*
import com.voltbody.app.util.HapticType
import com.voltbody.app.util.rememberHaptic
import dev.chrisbanes.haze.HazeState

@Composable
fun WorkoutScreen(
    onNavigateBack: () -> Unit = {},
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val vb = LocalVoltBodyColors.current
    val uiState by viewModel.uiState.collectAsState()
    val haptic = rememberHaptic()

    val routinesByDay = mapRoutineByWeekday(uiState.routine)
    val currentDay = uiState.currentWorkoutDay
    val plannedSets = currentDay?.exercises?.sumOf { it.sets } ?: 0
    val completedSetsCount = uiState.completedSets.values.sum()
    val etaMinutes = (plannedSets - completedSetsCount).coerceAtLeast(0) * 2

    var selectedExerciseForLog by remember { mutableStateOf<Exercise?>(null) }

    LiquidGlassScaffold(
        background = {
            Box(modifier = Modifier.fillMaxSize()) {
                // Background glows are now handled in the Scaffold itself
            }
        }
    ) { hazeState ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 60.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ... (keep header, weekly selector, hero card, progress)
            // Header
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.FitnessCenter, null, tint = vb.accent, modifier = Modifier.size(32.dp))
                    Column {
                        Text("💪 RUTINA DE HOY", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                        GlowText(
                            (currentDay?.focus ?: "HOY TOCA ACTIVAR EL CUERPO").uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }

            // Weekly Selector
            item {
                StaggeredEntrance(1) {
                    LiquidGlassCard(hazeState = hazeState) {
                        Text("SEMANA DE ENTRENAMIENTO", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                        Spacer(Modifier.height(4.dp))
                        Text("Selecciona un día. Los días sin plan quedan bloqueados.", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = vb.textMuted)
                        Spacer(Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val days = listOf("L", "M", "X", "J", "V", "S", "D")
                            days.forEachIndexed { index, day ->
                                val isSelected = uiState.selectedDayIndex == index
                                val hasRoutine = routinesByDay[index] != null
                                
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) vb.accent.copy(0.15f) else vb.surfaceElevated.copy(0.3f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) vb.accent.copy(0.6f) else vb.border.copy(0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable(enabled = hasRoutine) {
                                            haptic.perform(HapticType.TICK)
                                            viewModel.selectDay(index)
                                        }
                                        .alpha(if (hasRoutine) 1f else 0.4f),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        day,
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                                        color = if (isSelected) vb.accent else ColorWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Hero Card
            item {
                StaggeredEntrance(2) {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), accentGlow = true, hazeState = hazeState) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("🎯 SESIÓN PRIORITARIA", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                                Spacer(Modifier.height(4.dp))
                                HeadlineGradient(
                                    text = (currentDay?.focus ?: "CREA TU SESIÓN").uppercase(),
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${currentDay?.exercises?.size ?: 0} EJERCICIOS LISTOS PARA EJECUTAR.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = vb.textMuted
                                )
                            }
                            Icon(Icons.Default.Flame, null, tint = vb.accent, modifier = Modifier.size(24.dp))
                        }
                        
                        Spacer(Modifier.height(16.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatPill(label = "ESTADO", value = if (currentDay != null) "ACTIVO ✅" else "CUSTOM")
                            StatPill(label = "EJERCICIOS", value = "${currentDay?.exercises?.size ?: 0}")
                        }

                        Spacer(Modifier.height(20.dp))

                        LiquidGlassButton(
                            text = "EMPEZAR SESIÓN 🚀",
                            onClick = { haptic.perform(HapticType.TICK); viewModel.startSession() },
                            modifier = Modifier.fillMaxWidth(),
                            style = LiquidButtonStyle.Primary
                        )
                    }
                }
            }

            // Progress
            item {
                StaggeredEntrance(3) {
                    LiquidGlassCard(hazeState = hazeState) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("CHECKLIST DE SESIÓN", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                            Text("$completedSetsCount/$plannedSets SERIES", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                        }
                        Spacer(Modifier.height(10.dp))
                        LiquidProgressBar(progress = if (plannedSets > 0) completedSetsCount.toFloat() / plannedSets else 0f)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("PROGRESO: ${uiState.dayProgress}%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = vb.textMuted)
                            Text("ETA: $etaMinutes MIN", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = vb.textMuted)
                        }
                    }
                }
            }

            // Exercise List
            itemsIndexed(currentDay?.exercises ?: emptyList()) { index, exercise ->
                StaggeredEntrance(index + 4) {
                    ExerciseItem(
                        exercise = exercise,
                        setsDone = uiState.completedSets[exercise.id] ?: 0,
                        onSelect = { selectedExerciseForLog = exercise },
                        hazeState = hazeState
                    )
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }

    // Modal Log Sheet
    selectedExerciseForLog?.let { exercise ->
        com.voltbody.app.ui.screens.workout.components.ExerciseLogSheet(
            exercise = exercise,
            setsDone = uiState.completedSets[exercise.id] ?: 0,
            onDismiss = { selectedExerciseForLog = null },
            onLogSet = { weight, reps ->
                viewModel.logSet(exercise.id, weight, reps)
                // We keep it open if the user wants to log more series, 
                // just like web often does until closed manually or routine finished.
            }
        )
    }
}

@Composable
private fun ExerciseItem(
    exercise: Exercise,
    setsDone: Int,
    onSelect: () -> Unit,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    val targetSets = exercise.sets?.toIntOrNull() ?: 3
    val isCompleted = setsDone >= targetSets

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        hazeState = hazeState
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // GIF Placeholder (Matching Web's LazyImage)
            VoltBodyImage(
                model = exercise.gifUrl,
                contentDescription = exercise.name,
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, vb.border.copy(0.3f), RoundedCornerShape(12.dp))
            )
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    exercise.name.uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                    color = ColorWhite
                )
                Text(
                    "${exercise.sets} SERIES × ${exercise.reps}",
                    style = MaterialTheme.typography.labelSmall,
                    color = vb.textMuted
                )
            }
            
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    "$setsDone/$targetSets",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                    color = if (isCompleted) vb.accent else ColorWhite
                )
                if (isCompleted) {
                    Icon(Icons.Default.CheckCircle, null, tint = vb.accent, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    val vb = LocalVoltBodyColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(vb.surfaceElevated.copy(0.4f))
            .border(1.dp, vb.border.copy(0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = UppercaseLabel.copy(fontSize = 7.sp), color = vb.textMuted)
            Text(value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = ColorWhite)
        }
    }
}
