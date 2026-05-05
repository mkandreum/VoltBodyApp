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
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 70.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StaggeredEntrance(0) {
                        HeaderSection()
                    }
                }

                item {
                    StaggeredEntrance(1) {
                        WeekGridSelector(
                            selectedDay = uiState.selectedDayIndex,
                            completedDays = uiState.completedDays,
                            routinesByDay = routinesByDay,
                            onDaySelected = viewModel::selectDay
                        )
                    }
                }

                item {
                    StaggeredEntrance(2) {
                        PrioritySessionCard(
                            uiState = uiState,
                            onStart = viewModel::startSession,
                            onFinish = viewModel::finishSession,
                            hazeState = hazeState
                        )
                    }
                }

                if (uiState.sessionRunning) {
                    item {
                        StaggeredEntrance(3) {
                            SessionProgressCard(progress = uiState.dayProgress, hazeState = hazeState)
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
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
private fun HeaderSection() {
    val vb = LocalVoltBodyColors.current
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.FitnessCenter, contentDescription = null, tint = vb.accent, modifier = Modifier.size(32.dp))
            HeadlineGradient("RUTINA DE HOY", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black))
        }
        Text("💪 Prepárate para el máximo rendimiento", style = MaterialTheme.typography.labelSmall, color = vb.textMuted, modifier = Modifier.padding(start = 44.dp))
    }
}

@Composable
private fun WeekGridSelector(
    selectedDay: Int,
    completedDays: Set<Int>,
    routinesByDay: Array<WorkoutDay?>,
    onDaySelected: (Int) -> Unit
) {
    val vb = LocalVoltBodyColors.current
    val days = listOf("LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM")
    
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        days.forEachIndexed { index, day ->
            val isSelected = selectedDay == index
            val isCompleted = completedDays.contains(index)
            val hasRoutine = routinesByDay[index] != null
            
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(0.7f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        when {
                            isSelected -> vb.accent.copy(0.15f)
                            hasRoutine -> vb.surfaceElevated.copy(0.3f)
                            else -> Color.Transparent
                        }
                    )
                    .border(
                        1.dp,
                        if (isSelected) vb.accent.copy(0.5f) else if (hasRoutine) vb.border else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onDaySelected(index) },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        day,
                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black),
                        color = if (isSelected) vb.accent else if (hasRoutine) ColorWhite else vb.textMuted
                    )
                    if (isCompleted) {
                        Icon(Icons.Filled.CheckCircle, null, tint = vb.accent, modifier = Modifier.size(12.dp).padding(top = 4.dp))
                    } else if (hasRoutine) {
                        Box(modifier = Modifier.padding(top = 6.dp).size(4.dp).clip(CircleShape).background(vb.accent.copy(0.5f)))
                    }
                }
            }
        }
    }
}

@Composable
private fun PrioritySessionCard(
    uiState: WorkoutUiState,
    onStart: () -> Unit,
    onFinish: () -> Unit,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        accentGlow = uiState.sessionRunning,
        hazeState = hazeState
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text("🎯 SESIÓN PRIORITARIA", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.accent)
                    Text(
                        uiState.currentWorkoutDay?.focus ?: "DÍA DE DESCANSO",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = ColorWhite
                    )
                }
                if (uiState.sessionRunning) {
                    GlowText(formatDuration(uiState.sessionElapsed), style = MonoMetric.copy(fontSize = 28.sp, fontWeight = FontWeight.Black))
                } else {
                    Icon(Icons.Default.Star, null, tint = vb.accent, modifier = Modifier.size(24.dp))
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPillSmall("ESTADO", if (uiState.currentWorkoutDay != null) "ACTIVO" else "LIBRE", color = if (uiState.currentWorkoutDay != null) vb.accent else vb.textMuted)
                StatPillSmall("SERIES", "${uiState.currentWorkoutDay?.exercises?.sumOf { it.sets } ?: 0}")
                StatPillSmall("ETA", "45 MIN")
            }
            
            Spacer(modifier = Modifier.height(20.dp))
            
            if (uiState.sessionRunning) {
                LiquidGlassButton(
                    text = "TERMINAR SESIÓN",
                    onClick = onFinish,
                    modifier = Modifier.fillMaxWidth(),
                    style = LiquidButtonStyle.Secondary,
                    hazeState = hazeState
                )
            } else if (uiState.currentWorkoutDay != null) {
                LiquidGlassButton(
                    text = "EMPEZAR SESIÓN 🚀",
                    onClick = onStart,
                    modifier = Modifier.fillMaxWidth(),
                    style = LiquidButtonStyle.Primary,
                    hazeState = hazeState
                )
            }
        }
    }
}

@Composable
private fun SessionProgressCard(progress: Int, hazeState: HazeState? = null) {
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("PROGRESO DE LA SESIÓN", style = UppercaseLabel.copy(fontSize = 10.sp), color = ColorWhite)
            Text("$progress%", style = MonoMetric.copy(fontSize = 14.sp, fontWeight = FontWeight.Black), color = LocalVoltBodyColors.current.accent)
        }
        Spacer(Modifier.height(12.dp))
        LiquidProgressBar(progress = progress / 100f, height = 8.dp)
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
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.size(56.dp).clip(RoundedCornerShape(14.dp)).background(vb.surfaceElevated.copy(0.4f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isComplete) {
                        Icon(Icons.Filled.CheckCircle, null, tint = vb.accent, modifier = Modifier.size(28.dp))
                    } else {
                        Icon(Icons.Default.Dumbbell, null, tint = if (completedSets > 0) vb.accent else vb.textMuted.copy(0.5f))
                    }
                }
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(exercise.muscleGroup?.uppercase() ?: "GENERAL", style = UppercaseLabel.copy(fontSize = 8.sp), color = vb.accent)
                    Text(exercise.name.uppercase(), style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = if (isComplete) vb.accent else ColorWhite)
                    Text("${exercise.sets} series × ${exercise.reps}" + if (exercise.weight > 0) " · ${exercise.weight}kg" else "", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                }
                
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.MoreVert, null, tint = vb.textMuted)
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
            
            Spacer(modifier = Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                LiquidProgressBar(progress = completedSets.toFloat() / exercise.sets.coerceAtLeast(1), modifier = Modifier.weight(1f), height = 4.dp)
                Text("$completedSets/${exercise.sets}", style = MonoMetric.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = if (isComplete) vb.accent else vb.textMuted)
            }
        }
    }
}

@Composable
private fun StatPillSmall(label: String, value: String, color: Color = ColorWhite) {
    val vb = LocalVoltBodyColors.current
    Column(
        modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(vb.surfaceElevated.copy(0.3f)).padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, style = UppercaseLabel.copy(fontSize = 7.sp), color = vb.textMuted)
        Text(value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp), color = color)
    }
}

@Composable
private fun RestDayCard(hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("😴", fontSize = 64.sp)
            Text("DÍA DE RECUPERACIÓN", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = ColorWhite)
            Text("El músculo crece cuando descansas. Aprovecha para hidratarte y dormir bien.", style = MaterialTheme.typography.bodyMedium, color = vb.textMuted, textAlign = TextAlign.Center)
        }
    }
}

@Composable
private fun RestTimerCard(secondsLeft: Int, onSkip: () -> Unit, hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(modifier = Modifier.size(40.dp).neuroRaised(cornerRadius = 20.dp), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Timer, null, tint = vb.accent, modifier = Modifier.size(18.dp))
                }
                Column {
                    Text("DESCANSO", style = UppercaseLabel.copy(fontSize = 8.sp), color = vb.accent)
                    GlowText("${secondsLeft}S", style = MonoMetric.copy(fontSize = 22.sp, fontWeight = FontWeight.Black))
                }
            }
            LiquidGlassButton(text = "SALTAR", onClick = onSkip, style = LiquidButtonStyle.Secondary, hazeState = hazeState)
        }
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
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("✨ ¡ENTRENO COMPLETADO!", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
            Text(day.focus.uppercase(), style = UppercaseLabel.copy(fontSize = 10.sp), color = LocalVoltBodyColors.current.accent)
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
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
        Text(value, style = MonoMetric.copy(fontSize = 20.sp, fontWeight = FontWeight.Black), color = ColorWhite)
        Text(label, style = UppercaseLabel.copy(fontSize = 8.sp), color = LocalVoltBodyColors.current.textMuted)
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
            modifier = Modifier.fillMaxWidth(0.92f).wrapContentHeight(),
            hazeState = null
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                Text("REGISTRAR ${exercise.name.uppercase()}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = weight,
                        onValueChange = { weight = it },
                        label = { Text("PESO (KG)", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = vb.accent, unfocusedBorderColor = vb.border, cursorColor = vb.accent)
                    )
                    OutlinedTextField(
                        value = reps,
                        onValueChange = { reps = it },
                        label = { Text("REPS", style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = vb.accent, unfocusedBorderColor = vb.border, cursorColor = vb.accent)
                    )
                }
                
                OutlinedTextField(
                    value = sets,
                    onValueChange = { sets = it },
                    label = { Text("Nº SERIES", style = MaterialTheme.typography.labelSmall) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = vb.accent, unfocusedBorderColor = vb.border, cursorColor = vb.accent)
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
    val vb = LocalVoltBodyColors.current
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        LiquidGlassCard(
            modifier = Modifier.fillMaxWidth(0.92f).fillMaxHeight(0.85f),
            hazeState = null
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("BIBLIOTECA DE EJERCICIOS", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                    itemsIndexed(library) { _, item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(vb.surfaceElevated.copy(0.4f))
                                .clickable { onAdd(item) }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(item.name.uppercase(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                                Text(item.muscleGroup.uppercase(), style = UppercaseLabel.copy(fontSize = 8.sp), color = vb.accent)
                            }
                            Icon(Icons.Default.AddCircle, null, tint = vb.accent, modifier = Modifier.size(24.dp))
                        }
                    }
                }
                LiquidGlassButton(text = "CERRAR", onClick = onDismiss, style = LiquidButtonStyle.Secondary, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun formatDuration(seconds: Int): String {
    val m = seconds / 60
    val s = seconds % 60
    return "${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
}
