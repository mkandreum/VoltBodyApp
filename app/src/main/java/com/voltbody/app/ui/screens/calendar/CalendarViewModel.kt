package com.voltbody.app.ui.screens.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voltbody.app.domain.model.WorkoutDay
import com.voltbody.app.domain.usecase.computeSmartStreak
import com.voltbody.app.domain.usecase.getMondayFirstIndex
import com.voltbody.app.domain.usecase.mapRoutineByWeekday
import com.voltbody.app.ui.viewmodel.AppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

data class CalendarUiState(
    val displayMonth: YearMonth = YearMonth.now(),
    val selectedDay: LocalDate? = LocalDate.now(),
    val selectedDayWorkout: WorkoutDay? = null,
    val workoutDaysInMonth: Set<LocalDate> = emptySet(),
    val completedDaysInMonth: Set<LocalDate> = emptySet(),
    val weekWorkouts: Int = 0,
    val weekSets: Int = 0,
    val weekStreak: Int = 0,
    val isRescheduling: Boolean = false
)


@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val appViewModel: AppViewModel
) : ViewModel() {

    private val _displayMonth = MutableStateFlow(YearMonth.now())
    private val _selectedDay = MutableStateFlow<LocalDate?>(LocalDate.now())

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                appViewModel.routine,
                appViewModel.workoutLogs,
                _displayMonth,
                _selectedDay
            ) { routine, logs, displayMonth, selectedDay ->
                val routineByDay = mapRoutineByWeekday(routine)
                val workoutWeekdays = routineByDay.mapIndexed { i, d -> if (d != null) i else -1 }.filter { it >= 0 }.toSet()

                // Which days in month are workout days
                val workoutDays = (1..displayMonth.lengthOfMonth()).mapNotNull { day ->
                    val date = displayMonth.atDay(day)
                    if (workoutWeekdays.contains(getMondayFirstIndex(date))) date else null
                }.toSet()

                // Which days have workout logs
                val completedDays = logs.mapNotNull { log ->
                    runCatching { LocalDate.parse(log.date.take(10)) }.getOrNull()
                        ?.takeIf { it.year == displayMonth.year && it.monthValue == displayMonth.monthValue }
                }.toSet()

                // Selected day workout
                val selectedDayWorkout = selectedDay?.let { d ->
                    val idx = getMondayFirstIndex(d)
                    routineByDay[idx]
                }

                // This week stats
                val wf = WeekFields.of(Locale("es"))
                val today = LocalDate.now()
                val monday = today.with(wf.dayOfWeek(), 1)
                val sunday = monday.plusDays(6)
                val weekLogs = logs.filter {
                    runCatching { LocalDate.parse(it.date.take(10)) }.getOrNull()?.let { d -> d >= monday && d <= sunday } == true
                }
                val weekWorkouts = weekLogs.map { it.date.take(10) }.distinct().size
                val weekSets = weekLogs.size
                val streak = computeSmartStreak(logs, routine)

                CalendarUiState(
                    displayMonth = displayMonth,
                    selectedDay = selectedDay,
                    selectedDayWorkout = selectedDayWorkout,
                    workoutDaysInMonth = workoutDays,
                    completedDaysInMonth = completedDays,
                    weekWorkouts = weekWorkouts,
                    weekSets = weekSets,
                    weekStreak = streak
                )
            }.collect { _uiState.value = it }
        }
    }

    fun prevMonth() { _displayMonth.value = _displayMonth.value.minusMonths(1) }
    fun nextMonth() { _displayMonth.value = _displayMonth.value.plusMonths(1) }
    fun selectDay(day: LocalDate) { _selectedDay.value = day }

    fun toggleRescheduling() {
        _uiState.update { it.copy(isRescheduling = !it.isRescheduling) }
    }

    fun moveWorkout(fromDay: LocalDate, toDay: LocalDate) {
        val routine = appViewModel.routine.value.toMutableList()
        val fromIdx = getMondayFirstIndex(fromDay)
        val toIdx = getMondayFirstIndex(toDay)
        
        if (fromIdx == toIdx) return
        
        val routineByDay = mapRoutineByWeekday(routine)
        val workoutToMove = routineByDay[fromIdx] ?: return
        
        // Find the index in the original list for the 'from' day
        val originalFromIdx = routine.indexOfFirst { getMondayFirstIndex(LocalDate.now().with(it.dayOfWeek())) == fromIdx }
        
        // Update the day of the workout
        val targetDayOfWeek = toDay.dayOfWeek
        val updatedWorkout = workoutToMove.copy(day = targetDayOfWeek.name.lowercase().capitalize())
        
        // If there's already a workout on the target day, we might want to swap or just move. 
        // Web version seems to swap/reassign.
        val newRoutine = routine.map { 
            if (it.day.lowercase() == fromDay.dayOfWeek.name.lowercase()) {
                // This is the day we are moving FROM. If we move it, what happens to this day? 
                // In the app logic, we usually just change the day field.
                updatedWorkout
            } else if (it.day.lowercase() == toDay.dayOfWeek.name.lowercase()) {
                // If there was a workout here, we swap it to the fromDay
                it.copy(day = fromDay.dayOfWeek.name.lowercase().capitalize())
            } else {
                it
            }
        }
        
        appViewModel.setRoutine(newRoutine)
        _uiState.update { it.copy(isRescheduling = false) }
    }
}

