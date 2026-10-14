package com.voltbody.app.util

import androidx.compose.ui.graphics.Color
import com.voltbody.app.data.Exercise
import com.voltbody.app.data.WorkoutDay
import com.voltbody.app.data.WorkoutLog
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlin.math.roundToInt

/** MRV = volumen maximo recuperable (series/semana por grupo muscular). Medianas conservadoras (Israetel et al.). */
val MRV_DEFAULTS: Map<String, Int> = mapOf(
    "Pecho" to 20, "Espalda" to 25, "Hombros" to 20, "Piernas" to 22, "Bíceps" to 20,
    "Tríceps" to 20, "Core" to 25, "Calistenia" to 18, "Glúteos" to 20, "Cardio" to 5,
)
private const val DEFAULT_MRV = 18

/** status: fresh | moderate | high | overreached */
data class FatigueEntry(val muscleGroup: String, val weeklyVolume: Int, val mrv: Int, val percent: Int, val status: String)

/**
 * Fatiga por grupo muscular en la semana ISO (lunes-domingo) de [weekRef].
 * [extraExercises] permite incluir ejercicios fuera de la rutina (entreno custom) para mapear id -> musculo.
 */
fun computeFatigueIndex(
    logs: List<WorkoutLog>, routine: List<WorkoutDay>, weekRef: LocalDate = LocalDate.now(), extraExercises: List<Exercise> = emptyList(),
): List<FatigueEntry> {
    val monday = weekRef.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val fromKey = monday.toString()
    val toKey = monday.plusDays(6).toString()
    val muscleMap = HashMap<String, String>()
    (routine.flatMap { it.exercises } + extraExercises).forEach { if (it.id.isNotEmpty() && it.muscleGroup.isNotEmpty()) muscleMap[it.id] = it.muscleGroup }
    val sets = HashMap<String, Int>()
    for (log in logs) {
        val key = logDayKey(log.date)
        if (key < fromKey || key > toKey) continue
        val muscle = muscleMap[log.exerciseId] ?: continue
        sets[muscle] = (sets[muscle] ?: 0) + 1
    }
    return sets.map { (muscle, n) ->
        val mrv = MRV_DEFAULTS[muscle] ?: DEFAULT_MRV
        val percent = (n * 100.0 / mrv).roundToInt()
        val status = when { percent < 50 -> "fresh"; percent < 75 -> "moderate"; percent < 100 -> "high"; else -> "overreached" }
        FatigueEntry(muscle, n, mrv, percent, status)
    }.sortedByDescending { it.percent }
}

fun fatigueStatusLabel(status: String): String = when (status) {
    "fresh" -> "Fresco"
    "moderate" -> "Moderado"
    "high" -> "Alto"
    else -> "Sobreentrenado"
}

fun fatigueStatusColor(status: String): Color = when (status) {
    "fresh" -> Color(0xFF34D399)
    "moderate" -> Color(0xFFFACC15)
    "high" -> Color(0xFFFB923C)
    else -> Color(0xFFF87171)
}
