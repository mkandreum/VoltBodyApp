package com.voltbody.app.ui.screens.calendar

import com.voltbody.app.data.AppState
import com.voltbody.app.data.Meal
import com.voltbody.app.data.WorkoutDay
import com.voltbody.app.data.WorkoutLog
import com.voltbody.app.ui.screens.home.WATER_GOAL_ML
import com.voltbody.app.util.logLocalDate
import com.voltbody.app.util.mapRoutineByWeekday
import com.voltbody.app.util.mondayFirstIndex
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

/** Resumen de un dia concreto (entreno, comidas, agua, pasos, peso) para el calendario. */
data class DayStats(
    val date: LocalDate,
    val planned: WorkoutDay?,
    val dayLogs: List<WorkoutLog>,
    val setsByExercise: Map<String, Int>,
    val totalSets: Int,
    val completedSets: Int,
    val sessionProgress: Int,
    val meals: List<Meal>,
    val eatenIds: Set<String>,
    val consumedKcal: Int,
    val water: Int,
    val steps: Long,
    val weight: Double?,
    val objectivesPct: Int,
)

fun dayStats(st: AppState, date: LocalDate, water: Int, steps: Long): DayStats {
    val planned = mapRoutineByWeekday(st.routine)[mondayFirstIndex(date)]
    val dayLogs = st.logs.filter { logLocalDate(it.date) == date }
    val sets = dayLogs.groupingBy { it.exerciseId }.eachCount()
    val total = planned?.exercises?.sumOf { max(1, it.sets) } ?: 0
    val done = planned?.exercises?.sumOf { min(max(1, it.sets), sets[it.id] ?: 0) } ?: 0
    val progress = if (total > 0) Math.round(done * 100f / total) else 0
    val meals = st.diet?.meals.orEmpty()
    val eaten = st.mealEaten[date.toString()].orEmpty().toSet()
    val kcal = meals.filter { it.id in eaten }.sumOf { it.calories }
    val parts = buildList {
        if (planned != null) add(progress / 100f)
        if (meals.isNotEmpty()) add(meals.count { it.id in eaten }.toFloat() / meals.size)
        add(min(1f, water.toFloat() / WATER_GOAL_ML))
    }
    val pct = Math.round(parts.average().toFloat() * 100)
    return DayStats(date, planned, dayLogs, sets, total, done, progress, meals, eaten, kcal, water, steps, st.weightLogs.lastOrNull { it.date == date.toString() }?.weight, pct)
}
