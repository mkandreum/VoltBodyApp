package com.voltbody.app.ui.screens.calendar

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.voltbody.app.domain.model.WorkoutDay
import com.voltbody.app.domain.model.WorkoutLog
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.theme.*
import com.voltbody.app.util.HapticType
import com.voltbody.app.util.rememberHaptic
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = hiltViewModel()
) {
    val vb = LocalVoltBodyColors.current
    val uiState by viewModel.uiState.collectAsState()
    val haptic = rememberHaptic()

    val selectedDate = uiState.selectedDay ?: LocalDate.now()
    val weekDates = remember(uiState.displayMonth, selectedDate) {
        val startOfWeek = selectedDate.with(java.time.DayOfWeek.MONDAY)
        (0..6).map { startOfWeek.plusDays(it.toLong()) }
    }

    LiquidGlassScaffold { hazeState ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 60.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Header (Matching Web)
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.CalendarMonth, null, tint = vb.accent, modifier = Modifier.size(32.dp))
                    Column {
                        Text("📅 CALENDARIO", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                        Text("PLANIFICACIÓN Y REGISTRO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontMono = true), color = vb.textMuted)
                    }
                }
            }

            // 2. Weekly Selector Card
            item {
                StaggeredEntrance(1) {
                    LiquidGlassCard(hazeState = hazeState) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { viewModel.prevMonth() }) {
                                Icon(Icons.Default.ChevronLeft, null, tint = vb.textMuted)
                            }
                            Text(
                                uiState.displayMonth.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale("es"))).uppercase(),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                color = ColorWhite
                            )
                            IconButton(onClick = { viewModel.nextMonth() }) {
                                Icon(Icons.Default.ChevronRight, null, tint = vb.textMuted)
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            val days = listOf("L", "M", "X", "J", "V", "S", "D")
                            weekDates.forEachIndexed { index, date ->
                                val isSelected = date == selectedDate
                                val isToday = date == LocalDate.now()
                                val hasLogs = uiState.completedDaysInMonth.contains(date)

                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(0.8f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) vb.accent.copy(0.2f) else vb.surfaceElevated.copy(0.3f))
                                        .border(
                                            width = 1.dp,
                                            color = if (isSelected) vb.accent else if (isToday) vb.textMuted else vb.border.copy(0.3f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable { haptic.perform(HapticType.TICK); viewModel.selectDay(date) },
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(days[index], style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = vb.textMuted)
                                    Text(
                                        date.dayOfMonth.toString(),
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
                                        color = if (isSelected) vb.accent else ColorWhite
                                    )
                                    if (hasLogs) {
                                        Box(modifier = Modifier.size(4.dp).background(vb.accent, CircleShape))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 3. Selected Day Title
            item {
                StaggeredEntrance(2) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            selectedDate.format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("es"))).uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
                            color = ColorWhite
                        )
                        if (selectedDate == LocalDate.now()) {
                            Text(
                                "HOY",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontMono = true),
                                color = vb.accent,
                                modifier = Modifier.background(vb.accent.copy(0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }

            // 4. Planned Routine Card
            item {
                StaggeredEntrance(3) {
                    val routine = uiState.selectedDayWorkout
                    if (routine != null) {
                        PlannedRoutineCard(routine = routine, progress = (uiState.dayProgress * 100).toInt(), hazeState = hazeState)
                    } else {
                        LiquidGlassCard(hazeState = hazeState) {
                            Text("🏋️ RUTINA PLANIFICADA", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                            Spacer(Modifier.height(12.dp))
                            Text("Este día no tienes entrenamiento programado.", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                        }
                    }
                }
            }

            // 5. Training Logs Section
            item {
                StaggeredEntrance(4) {
                    LiquidGlassCard(hazeState = hazeState) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.CheckCircle, null, tint = vb.accent, modifier = Modifier.size(16.dp))
                            Text("✅ REGISTRO DE ENTRENAMIENTO", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                        }
                        Spacer(Modifier.height(16.dp))
                        val logs = uiState.selectedDayLogs
                        if (logs.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                logs.forEach { log ->
                                    TrainingLogItem(log)
                                }
                            }
                        } else {
                            Text("No hay registros para este día.", style = MaterialTheme.typography.labelSmall, color = vb.textMuted, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            }

            // 6. Diet Summary Card
            item {
                StaggeredEntrance(5) {
                    uiState.diet?.let { diet ->
                        LiquidGlassCard(hazeState = hazeState) {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Icon(Icons.Default.LocalFireDepartment, null, tint = vb.accent, modifier = Modifier.size(16.dp))
                                Text("🔥 OBJETIVO NUTRICIONAL", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                            }
                            Spacer(Modifier.height(16.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    "${diet.dailyCalories} KCAL",
                                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
                                    color = ColorWhite
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    MacroPill("P: ${diet.macros.protein}G")
                                    MacroPill("C: ${diet.macros.carbs}G")
                                    MacroPill("G: ${diet.macros.fat}G")
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun PlannedRoutineCard(routine: WorkoutDay, progress: Int, hazeState: dev.chrisbanes.haze.HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Text("🏋️ RUTINA PLANIFICADA", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(modifier = Modifier.size(48.dp).clip(CircleShape).background(Color.Black).border(1.dp, vb.accent.copy(0.3f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.FitnessCenter, null, tint = vb.accent, modifier = Modifier.size(24.dp))
            }
            Column {
                Text(routine.focus.uppercase(), style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                Text("${routine.exercises.size} EJERCICIOS", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
            }
        }
        Spacer(Modifier.height(20.dp))
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("PROGRESO POR SERIES", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = vb.textMuted)
                Text("$progress%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = vb.accent)
            }
            Spacer(Modifier.height(8.dp))
            LiquidProgressBar(progress = progress / 100f, height = 6.dp)
        }
    }
}

@Composable
private fun TrainingLogItem(log: WorkoutLog) {
    val vb = LocalVoltBodyColors.current
    Row(
        modifier = Modifier.fillMaxWidth().background(vb.surfaceElevated.copy(0.2f), RoundedCornerShape(12.dp)).border(1.dp, vb.border.copy(0.1f), RoundedCornerShape(12.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("EJERCICIO", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ColorWhite)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("${log.weight}KG", style = MaterialTheme.typography.labelSmall.copy(fontMono = true), color = vb.textMuted)
            Text("X${log.reps}", style = MaterialTheme.typography.labelSmall.copy(fontMono = true, fontWeight = FontWeight.Black), color = vb.accent)
        }
    }
}

@Composable
private fun MacroPill(text: String) {
    val vb = LocalVoltBodyColors.current
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, fontMono = true),
        color = vb.textMuted,
        modifier = Modifier.background(vb.surfaceElevated.copy(0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
    )
}
