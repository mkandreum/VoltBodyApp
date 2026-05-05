package com.voltbody.app.ui.screens.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voltbody.app.domain.model.*
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val vb = LocalVoltBodyColors.current
    val uiState by viewModel.uiState.collectAsState()

    LiquidGlassScaffold(
        background = {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.size(400.dp).align(Alignment.TopEnd).background(vb.accent.copy(0.1f), CircleShape).offset(80.dp, (-80).dp))
                Box(modifier = Modifier.size(300.dp).align(Alignment.BottomStart).background(ColorInfo.copy(0.08f), CircleShape).offset((-80).dp, 80.dp))
            }
        }
    ) { hazeState ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 70.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StaggeredEntrance(0) {
                    CalendarHeader(
                        month = uiState.displayMonth,
                        onPrev = viewModel::prevMonth,
                        onNext = viewModel::nextMonth
                    )
                }
            }

            item {
                StaggeredEntrance(1) {
                    WeeklyStatsRow(
                        workouts = uiState.weekWorkouts,
                        sets = uiState.weekSets,
                        streak = uiState.weekStreak
                    )
                }
            }

            item {
                StaggeredEntrance(2) {
                    CalendarGrid(
                        displayMonth = uiState.displayMonth,
                        selectedDay = uiState.selectedDay,
                        workoutDays = uiState.workoutDaysInMonth,
                        completedDays = uiState.completedDaysInMonth,
                        onDaySelected = { day ->
                            if (uiState.isRescheduling && uiState.selectedDay != null && uiState.selectedDayWorkout != null) {
                                viewModel.moveWorkout(uiState.selectedDay!!, day)
                            } else {
                                viewModel.selectDay(day)
                            }
                        },
                        hazeState = hazeState
                    )
                }
            }

            item {
                StaggeredEntrance(3) {
                    DayDetailCard(
                        selectedDay = uiState.selectedDay ?: LocalDate.now(),
                        workout = uiState.selectedDayWorkout,
                        progress = uiState.dayProgress,
                        isRescheduling = uiState.isRescheduling,
                        onToggleReschedule = viewModel::toggleRescheduling,
                        hazeState = hazeState
                    )
                }
            }

            if (uiState.selectedDayWorkout != null) {
                itemsIndexed(uiState.selectedDayWorkout!!.exercises) { index, exercise ->
                    StaggeredEntrance(index + 4) {
                        ExerciseSmallItem(exercise = exercise, hazeState = hazeState)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun CalendarHeader(
    month: YearMonth,
    onPrev: () -> Unit,
    onNext: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = vb.accent, modifier = Modifier.size(32.dp))
                HeadlineGradient("🗓️ CALENDARIO", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black))
            }
            Text(
                month.month.getDisplayName(TextStyle.FULL, Locale("es")).uppercase() + " " + month.year,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoMetric.fontFamily),
                color = vb.textMuted,
                modifier = Modifier.padding(start = 44.dp)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            IconButton(onClick = onPrev, modifier = Modifier.size(36.dp).neuroRaised(cornerRadius = 18.dp)) {
                Icon(Icons.Default.ChevronLeft, null, tint = ColorWhite, modifier = Modifier.size(20.dp))
            }
            IconButton(onClick = onNext, modifier = Modifier.size(36.dp).neuroRaised(cornerRadius = 18.dp)) {
                Icon(Icons.Default.ChevronRight, null, tint = ColorWhite, modifier = Modifier.size(20.dp))
            }
        }
    }
}

@Composable
private fun WeeklyStatsRow(workouts: Int, sets: Int, streak: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        StatPillFlat("SESIONES", "$workouts", Icons.Default.FitnessCenter, modifier = Modifier.weight(1f))
        StatPillFlat("SERIES", "$sets", Icons.Default.StackedLineChart, modifier = Modifier.weight(1f))
        StatPillFlat("RACHA", "$streak", Icons.Default.Whatshot, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun StatPillFlat(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier) {
    val vb = LocalVoltBodyColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(vb.surfaceElevated.copy(0.3f))
            .padding(12.dp)
    ) {
        Column {
            Icon(icon, null, tint = vb.accent, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
            Text(label, style = UppercaseLabel.copy(fontSize = 7.sp), color = vb.textMuted)
        }
    }
}

@Composable
private fun CalendarGrid(
    displayMonth: YearMonth,
    selectedDay: LocalDate?,
    workoutDays: Set<LocalDate>,
    completedDays: Set<LocalDate>,
    onDaySelected: (LocalDate) -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    val daysOfWeek = listOf("L", "M", "X", "J", "V", "S", "D")
    val firstDayOfMonth = displayMonth.atDay(1)
    val dayOfWeekOffset = (firstDayOfMonth.dayOfWeek.value - 1)
    val daysInMonth = displayMonth.lengthOfMonth()

    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Column {
            Row(modifier = Modifier.fillMaxWidth()) {
                daysOfWeek.forEach { day ->
                    Text(
                        day,
                        modifier = Modifier.weight(1f),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
                        color = vb.textMuted
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            var currentDay = 1
            for (week in 0..5) {
                if (currentDay > daysInMonth) break
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dayInWeek in 0..6) {
                        val isOffset = (week == 0 && dayInWeek < dayOfWeekOffset) || currentDay > daysInMonth
                        if (isOffset) {
                            Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val date = displayMonth.atDay(currentDay)
                            val isSelected = date == selectedDay
                            val isWorkout = workoutDays.contains(date)
                            val isCompleted = completedDays.contains(date)
                            val isToday = date == LocalDate.now()
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) vb.accent.copy(0.2f) else Color.Transparent)
                                    .border(1.dp, if (isSelected) vb.accent else if (isToday) vb.accent.copy(0.3f) else Color.Transparent, CircleShape)
                                    .clickable { onDaySelected(date) },
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        "$currentDay",
                                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal),
                                        color = if (isSelected || isToday) ColorWhite else vb.textMuted
                                    )
                                    if (isWorkout) {
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp)
                                                .size(4.dp)
                                                .clip(CircleShape)
                                                .background(if (isCompleted) vb.accent else vb.textMuted.copy(0.4f))
                                        )
                                    }
                                }
                            }
                            currentDay++
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DayDetailCard(
    selectedDay: LocalDate,
    workout: WorkoutDay?,
    progress: Float,
    isRescheduling: Boolean,
    onToggleReschedule: () -> Unit,
    hazeState: dev.chrisbanes.haze.HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    val formatter = java.time.format.DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", Locale("es"))
    
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        accentGlow = isRescheduling,
        hazeState = hazeState
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(selectedDay.format(formatter).uppercase(), style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.accent)
                    Text(
                        workout?.focus ?: "DÍA DE DESCANSO",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                        color = ColorWhite
                    )
                }
                if (workout != null) {
                    IconButton(
                        onClick = onToggleReschedule,
                        modifier = Modifier.size(44.dp).neuroRaised(cornerRadius = 22.dp)
                    ) {
                        Icon(
                            if (isRescheduling) Icons.Default.Close else Icons.Default.EventRepeat,
                            null,
                            tint = if (isRescheduling) ColorError else vb.accent
                        )
                    }
                }
            }
            
            if (workout != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    LiquidProgressBar(progress = progress, modifier = Modifier.weight(1f), height = 6.dp)
                    Text("${(progress * 100).toInt()}%", style = MonoMetric.copy(fontSize = 12.sp, fontWeight = FontWeight.Black), color = vb.accent)
                }
                if (isRescheduling) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(vb.accent.copy(0.1f)).padding(12.dp)) {
                        Text("📍 Selecciona un nuevo día en el calendario para mover esta sesión.", style = MaterialTheme.typography.labelSmall, color = vb.accent)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Text("No hay sesiones programadas para este día.", style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
            }
        }
    }
}

@Composable
private fun ExerciseSmallItem(exercise: Exercise, hazeState: dev.chrisbanes.haze.HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(vb.surfaceElevated.copy(0.3f))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)).background(vb.surfaceElevated.copy(0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Dumbbell, null, tint = vb.accent, modifier = Modifier.size(20.dp))
            }
            Column {
                Text(exercise.name.uppercase(), style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                Text("${exercise.sets} series × ${exercise.reps}", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
            }
        }
    }
}
