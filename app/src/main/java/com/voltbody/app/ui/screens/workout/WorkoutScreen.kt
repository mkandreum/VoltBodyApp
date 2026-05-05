package com.voltbody.app.ui.screens.workout

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import com.voltbody.app.domain.model.*
import com.voltbody.app.domain.usecase.*
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.theme.*
import dev.chrisbanes.haze.HazeState

@Composable
fun WorkoutScreen(
    viewModel: WorkoutViewModel = hiltViewModel()
) {
    val vb = LocalVoltBodyColors.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    var showLibraryDialog by remember { mutableStateOf(false) }

    val routinesByDay = remember(uiState.routine) { mapRoutineByWeekday(uiState.routine) }

    LiquidGlassScaffold(
        background = {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.size(400.dp).align(Alignment.TopEnd).background(vb.accent.copy(0.12f), CircleShape).offset(80.dp, (-80).dp))
                Box(modifier = Modifier.size(300.dp).align(Alignment.CenterStart).background(ColorError.copy(0.1f), CircleShape).offset((-100).dp, 150.dp))
                Box(modifier = Modifier.size(350.dp).align(Alignment.BottomEnd).background(vb.accentDim.copy(0.08f), CircleShape).offset(50.dp, 50.dp))
            }
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
                item {
                    StaggeredEntrance(0) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(modifier = Modifier.size(44.dp).neuroRaised(cornerRadius = 22.dp), contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = vb.accent, modifier = Modifier.size(20.dp))
                            }
                            Column {
                                HeadlineGradient("RUTINA DE HOY", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    uiState.currentWorkoutDay?.focus ?: "Activa tu cuerpo hoy",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = vb.textMuted
                                )
                            }
                        }
                    }
                }

                item {
                    StaggeredEntrance(1) {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("PLAN SEMANAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = vb.accent)
                                IconButton(onClick = {}, modifier = Modifier.size(32.dp).neuroRaised(cornerRadius = 16.dp)) {
                                    Icon(Icons.Filled.Settings, contentDescription = null, tint = ColorWhite, modifier = Modifier.size(14.dp))
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            WeekDaySelector(
                                selectedDay = uiState.selectedDayIndex,
                                completedDays = uiState.completedDays,
                                routinesByDay = routinesByDay,
                                onDaySelected = viewModel::selectDay
                            )
                        }
                    }
                }

                item {
                    StaggeredEntrance(2) {
                        LiquidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { if (!uiState.sessionRunning) viewModel.startSession() },
                            accentGlow = uiState.sessionRunning,
                            hazeState = hazeState
                        ) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("🎯 SESIÓN PRIORITARIA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = vb.accent)
                                    Text(
                                        uiState.currentWorkoutDay?.focus ?: "Personaliza tu sesión",
                                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                                        color = ColorWhite
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        StatPillGlass("ESTADO", if (uiState.currentWorkoutDay != null) "ACTIVO" else "LIBRE")
                                        StatPillGlass("EJERCICIOS", "${uiState.currentWorkoutDay?.exercises?.size ?: 0}")
                                    }
                                }
                                if (uiState.sessionRunning) {
                                    Box(modifier = Modifier.size(48.dp).neuroRaised(cornerRadius = 24.dp), contentAlignment = Alignment.Center) {
                                        Icon(Icons.Filled.Timer, contentDescription = null, tint = vb.accent, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                            
                            if (uiState.sessionRunning) {
                                Spacer(modifier = Modifier.height(20.dp))
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    GlowText(formatDuration(uiState.sessionElapsed), style = MonoMetric.copy(fontSize = 32.sp, fontWeight = FontWeight.Black))
                                    Text("TIEMPO TOTAL", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                LiquidGlassButton(
                                    text = "TERMINAR SESIÓN",
                                    onClick = viewModel::finishSession,
                                    modifier = Modifier.fillMaxWidth(),
                                    hazeState = hazeState
                                )
                            } else {
                                Spacer(modifier = Modifier.height(20.dp))
                                LiquidGlassButton(
                                    text = "EMPEZAR SESIÓN 🚀",
                                    onClick = viewModel::startSession,
                                    modifier = Modifier.fillMaxWidth(),
                                    style = LiquidButtonStyle.Primary,
                                    hazeState = hazeState
                                )
                            }
                        }
                    }
                }

                if (uiState.sessionRunning) {
                    item {
                        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("PROGRESO ACTUAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                                Text("${uiState.dayProgress}%", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = vb.accent)
                            }
                            Spacer(Modifier.height(12.dp))
                            LiquidProgressBar(progress = uiState.dayProgress / 100f)
                        }
                    }
                }

                if (uiState.restSecondsLeft > 0) {
                    item {
                        RestTimerCard(secondsLeft = uiState.restSecondsLeft, onSkip = viewModel::skipRest, hazeState = hazeState)
                    }
                }

                uiState.currentWorkoutDay?.let { day ->
                    itemsIndexed(day.exercises, key = { _, it -> it.id }) { index, exercise ->
                        StaggeredEntrance(index + 4) {
                            ExerciseCard(
                                exercise = exercise,
                                completedSets = uiState.completedSets[exercise.id] ?: 0,
                                progressiveSuggestion = uiState.progressiveSuggestions[exercise.id],
                                onLogSet = { viewModel.openLogSheet(exercise) },
                                onDelete = { viewModel.removeExerciseFromDay(uiState.selectedDayIndex, exercise.id) },
                                hazeState = hazeState
                            )
                        }
                    }
                    
                    item {
                        LiquidGlassButton(
                            text = "AÑADIR EJERCICIO",
                            onClick = { showLibraryDialog = true },
                            modifier = Modifier.fillMaxWidth(),
                            style = LiquidButtonStyle.Secondary,
                            leadingIcon = { Icon(Icons.Default.Add, null, tint = vb.accent) },
                            hazeState = hazeState
                        )
                    }
                } ?: item {
                    RestDayCard(hazeState = hazeState)
                }

                if (uiState.workoutComplete) {
                    item {
                        uiState.currentWorkoutDay?.let { day ->
                            WorkoutSummaryShareCard(
                                day = day,
                                setsLogged = uiState.todaySetsLogged,
                                duration = uiState.sessionElapsed,
                                streak = uiState.currentStreak,
                                userName = uiState.userName,
                                hazeState = hazeState
                            )
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }

        if (showLibraryDialog) {
            ExerciseLibraryDialog(
                library = uiState.exerciseLibrary,
                onDismiss = { showLibraryDialog = false },
                onAdd = { viewModel.addExerciseToDay(uiState.selectedDayIndex, it); showLibraryDialog = false }
            )
        }

        uiState.logSheetExercise?.let { exercise ->
            LogSetDialog(
                exercise = exercise,
                history = uiState.logSheetHistory,
                lastWeight = uiState.lastWeightForExercise[exercise.id],
                onDismiss = viewModel::closeLogSheet,
                onLog = { weight, reps, rir, sets, duration, rpe ->
                    viewModel.logSet(exercise, weight, reps, rir, sets, duration, rpe)
                }
            )
        }
    }
}

@Composable
private fun WeekDaySelector(
    selectedDay: Int,
    completedDays: Set<Int>,
    routinesByDay: Array<WorkoutDay?>,
    onDaySelected: (Int) -> Unit
) {
    val vb = LocalVoltBodyColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        listOf("L", "M", "X", "J", "V", "S", "D").forEachIndexed { index, day ->
            val isSelected = selectedDay == index
            val isCompleted = completedDays.contains(index)
            val hasRoutine = routinesByDay[index] != null
            
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { onDaySelected(index) }
                    .background(if (isSelected) vb.accent.copy(0.15f) else Color.Transparent)
                    .padding(8.dp)
            ) {
                Text(
                    day,
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                    color = if (isSelected) vb.accent else vb.textMuted
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                isCompleted -> vb.accent
                                hasRoutine -> vb.textMuted.copy(0.3f)
                                else -> Color.Transparent
                            }
                        )
                )
            }
        }
    }
}

@Composable
private fun ExerciseCard(
    exercise: Exercise,
    completedSets: Int,
    progressiveSuggestion: ProgressiveSuggestion?,
    onLogSet: () -> Unit,
    onDelete: () -> Unit,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    val isComplete = completedSets >= exercise.sets
    
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onLogSet,
        accentGlow = !isComplete && completedSets > 0,
        hazeState = hazeState
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(vb.surfaceElevated.copy(0.4f)),
                contentAlignment = Alignment.Center
            ) {
                if (isComplete) {
                    Icon(Icons.Filled.CheckCircle, null, tint = vb.accent, modifier = Modifier.size(32.dp))
                } else {
                    Icon(Icons.Default.FitnessCenter, null, tint = vb.accent.copy(0.4f))
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(exercise.name.uppercase(), style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = if (isComplete) vb.accent else ColorWhite)
                Text("${exercise.sets} SERIES × ${exercise.reps}" + if (exercise.weight > 0) " · ${exercise.weight}KG" else "", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                if (completedSets > 0 && !isComplete) {
                    Text("$completedSets/${exercise.sets} COMPLETADAS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 9.sp), color = vb.accent)
                }
            }
            
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.DeleteOutline, null, tint = vb.textMuted)
            }
        }
        
        if (progressiveSuggestion != null) {
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(vb.accent.copy(0.1f)).padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.AutoMirrored.Filled.TrendingUp, null, tint = vb.accent, modifier = Modifier.size(14.dp))
                Text("SUGERIDO: ${progressiveSuggestion.suggestedWeight}KG", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = vb.accent)
            }
        }
        
        Spacer(Modifier.height(12.dp))
        LiquidProgressBar(progress = completedSets.toFloat() / exercise.sets.coerceAtLeast(1), height = 4.dp)
    }
}

@Composable
private fun LogSetDialog(
    exercise: Exercise,
    history: List<ExerciseSession>,
    lastWeight: Float?,
    onDismiss: () -> Unit,
    onLog: (Float, Int, Int?, Int, Int?, Int?) -> Unit
) {
    var weight by remember { mutableStateOf(lastWeight?.toString() ?: "") }
    var reps by remember { mutableStateOf("") }
    var sets by remember { mutableStateOf("1") }
    val vb = LocalVoltBodyColors.current

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(0.9f).wrapContentHeight(),
            hazeState = null // Dialog handles its own surface
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("REGISTRAR ${exercise.name.uppercase()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("PESO (KG)") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = vb.accent, unfocusedBorderColor = vb.border)
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("REPS") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = vb.accent, unfocusedBorderColor = vb.border)
                    )
                }
                
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Nº SERIES") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = vb.accent, unfocusedBorderColor = vb.border)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LiquidGlassButton(text = "CANCELAR", onClick = onDismiss, style = LiquidButtonStyle.Secondary, modifier = Modifier.weight(1f))
                    LiquidGlassButton(
                        text = "GUARDAR",
                        onClick = {
                            val w = weight.toFloatOrNull() ?: 0f
                            val r = reps.toIntOrNull() ?: 0
                            val s = sets.toIntOrNull() ?: 1
                            onLog(w, r, null, s, null, null)
                        },
                        style = LiquidButtonStyle.Primary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ExerciseLibraryDialog(
    library: List<ExerciseLibraryEntry>,
    onDismiss: () -> Unit,
    onAdd: (ExerciseLibraryEntry) -> Unit
) {
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(0.9f).fillMaxHeight(0.8f),
            hazeState = null
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("BIBLIOTECA DE EJERCICIOS", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                    itemsIndexed(library) { _, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(LocalVoltBodyColors.current.surface)
                                .clickable { onAdd(item) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ColorWhite)
                                Text(item.muscleGroup, style = MaterialTheme.typography.labelSmall, color = LocalVoltBodyColors.current.textMuted)
                            }
                            Icon(Icons.Default.Add, null, tint = LocalVoltBodyColors.current.accent)
                        }
                    }
                }
                LiquidGlassButton(text = "CERRAR", onClick = onDismiss, style = LiquidButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun RestDayCard(hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("😴", fontSize = 64.sp)
            Text("DÍA DE RECUPERACIÓN", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = ColorWhite)
            Text("El músculo crece cuando descansas.", style = MaterialTheme.typography.bodyMedium, color = vb.textMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RestTimerCard(secondsLeft: Int, onSkip: () -> Unit, hazeState: HazeState? = null) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Column {
                Text("DESCANSO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = LocalVoltBodyColors.current.accent)
                GlowText("${secondsLeft}S", style = MonoMetric.copy(fontSize = 24.sp, fontWeight = FontWeight.Black))
            }
            LiquidGlassButton(text = "SALTAR", onClick = onSkip, style = LiquidButtonStyle.Secondary, hazeState = hazeState)
        }
    }
}

@Composable
private fun StatPillGlass(label: String, value: String) {
    val vb = LocalVoltBodyColors.current
    Column(
        modifier = Modifier.clip(RoundedCornerShape(10.dp)).background(vb.surfaceElevated.copy(0.3f)).padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Black), color = vb.textMuted)
        Text(value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = ColorWhite)
    }
}

@Composable
private fun WorkoutSummaryShareCard(
    day: WorkoutDay,
    setsLogged: Int,
    duration: Int,
    streak: Int,
    userName: String?,
    hazeState: HazeState? = null
) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("¡ENTRENO COMPLETADO!", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
            Text(day.focus, style = MaterialTheme.typography.bodySmall, color = LocalVoltBodyColors.current.accent)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                SummaryStat("SERIES", "$setsLogged")
                SummaryStat("TIEMPO", formatDuration(duration))
                SummaryStat("RACHA", "$streak")
            }
        }
    }
}

@Composable
private fun SummaryStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MonoMetric.copy(fontSize = 18.sp, fontWeight = FontWeight.Black), color = ColorWhite)
        Text(label, style = MaterialTheme.typography.labelSmall, color = LocalVoltBodyColors.current.textMuted)
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
