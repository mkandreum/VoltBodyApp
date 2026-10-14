package com.voltbody.app.util

import com.voltbody.app.data.WorkoutLog
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.math.roundToLong

/** Clave de dia local (yyyy-MM-dd) de un log. Acepta ISO instant/offset o "yyyy-MM-dd". */
fun logDayKey(date: String): String {
    runCatching { return Instant.parse(date).atZone(ZoneId.systemDefault()).toLocalDate().toString() }
    runCatching { return OffsetDateTime.parse(date).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate().toString() }
    return date.take(10)
}

data class ProgressiveSuggestion(val suggestedWeight: Double, val currentWeight: Double, val sessionsAnalyzed: Int, val increment: Double)

/**
 * Analiza las ultimas [minSessions] sesiones distintas de un ejercicio. Si el pico de peso es el mismo
 * en todas, sugiere subir 2.5 kg (redondeado a 0.5 kg).
 */
fun getProgressiveSuggestion(exerciseId: String, logs: List<WorkoutLog>, minSessions: Int = 3): ProgressiveSuggestion? {
    val sessions = logs.filter { it.exerciseId == exerciseId }
        .groupBy { logDayKey(it.date) }
        .toList()
        .sortedByDescending { it.first }
        .take(minSessions)
    if (sessions.size < minSessions) return null
    val weights = sessions.map { (_, s) -> s.maxOf { it.weight } }
    if (!weights.all { it == weights[0] }) return null
    val current = weights[0]
    if (current <= 0) return null
    val increment = 2.5
    val suggested = ((current + increment) * 2).roundToLong() / 2.0
    return ProgressiveSuggestion(suggested, current, sessions.size, increment)
}

data class ExerciseSession(
    val date: String, // yyyy-MM-dd
    val maxWeight: Double,
    val totalVolume: Double, // suma peso x reps
    val sets: Int,
    val maxReps: Int,
    val maxDuration: Int = 0, // segundos (isometricos / cardio)
)

/** Agregados por sesion de un ejercicio, del mas antiguo al mas nuevo (para graficas). */
fun getExerciseHistory(exerciseId: String, logs: List<WorkoutLog>, limit: Int = 15): List<ExerciseSession> =
    logs.filter { it.exerciseId == exerciseId }
        .groupBy { logDayKey(it.date) }
        .toList()
        .sortedBy { it.first }
        .takeLast(limit)
        .map { (date, s) ->
            ExerciseSession(
                date = date,
                maxWeight = s.maxOf { it.weight },
                totalVolume = s.sumOf { it.weight * it.reps },
                sets = s.size,
                maxReps = s.maxOf { it.reps },
                maxDuration = s.maxOf { it.duration ?: 0 },
            )
        }
