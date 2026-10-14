package com.voltbody.app.util

import com.voltbody.app.data.AppState
import com.voltbody.app.data.Achievement
import com.voltbody.app.data.Repository
import java.time.Instant
import java.time.LocalDate

/** Port de achievements.ts. */
val ALL_ACHIEVEMENTS: List<Achievement> = listOf(
    Achievement("first-series", "Primera serie", "🏋️", "Registraste tu primera serie. ¡El camino empieza aquí!"),
    Achievement("series-10", "10 series", "💪", "Has acumulado 10 series registradas."),
    Achievement("series-50", "50 series", "⚡", "50 series completadas. ¡Eso es dedicación!"),
    Achievement("series-100", "100 series", "🔥", "100 series. Eres imparable."),
    Achievement("series-500", "500 series", "💥", "500 series. Nivel élite."),
    Achievement("streak-3", "Racha de 3 días", "🔗", "Entrenaste 3 días seguidos."),
    Achievement("streak-7", "Racha semanal", "📅", "7 días de racha. ¡Un hábito de hierro!"),
    Achievement("streak-14", "Dos semanas seguidas", "🗓️", "14 días de racha. Consistencia brutal."),
    Achievement("streak-30", "Racha mensual", "🏆", "30 días en racha. Eres una bestia."),
    Achievement("pr-beaten", "Nuevo récord personal", "🥇", "Superaste tu mejor marca en un ejercicio."),
    Achievement("first-100kg", "100 kg en la barra", "🦾", "Registraste 100 kg o más en un ejercicio."),
    Achievement("consistency-week", "Semana perfecta", "✅", "Completaste todos los días de entreno en una semana."),
)

/**
 * Logros recien desbloqueados segun el estado actual (los que aun no estan en s.achievements),
 * con unlockedAt = ahora. Idempotente: llamarlo tras cada log es seguro.
 */
fun evaluateAchievements(s: AppState): List<Achievement> {
    val earned = s.achievements.map { it.id }.toSet()
    val logs = s.logs
    val total = logs.size
    val streak = computeSmartStreak(logs, s.routine)
    val ids = linkedSetOf<String>()
    if (total >= 1) ids += "first-series"
    if (total >= 10) ids += "series-10"
    if (total >= 50) ids += "series-50"
    if (total >= 100) ids += "series-100"
    if (total >= 500) ids += "series-500"
    if (streak >= 3) ids += "streak-3"
    if (streak >= 7) ids += "streak-7"
    if (streak >= 14) ids += "streak-14"
    if (streak >= 30) ids += "streak-30"
    if (logs.any { it.weight >= 100 }) ids += "first-100kg"

    // Récord: el último log de un ejercicio supera el mejor anterior (> 0)
    val prBeaten = logs.groupBy { it.exerciseId }.values.any { list ->
        if (list.size < 2) false else {
            val last = list.last().weight
            val prevBest = list.dropLast(1).maxOf { it.weight }
            prevBest > 0 && last > prevBest
        }
    }
    if (prBeaten) ids += "pr-beaten"

    // Semana perfecta: todos los dias de entreno planificados de la semana actual tienen logs
    val plannedIdx = mapRoutineByWeekday(s.routine).mapIndexedNotNull { i, d -> if (d != null) i else null }
    if (plannedIdx.isNotEmpty()) {
        val monday = mondayOf(LocalDate.now())
        val dates = logs.mapNotNull { logLocalDate(it.date) }.toSet()
        if (plannedIdx.all { dates.contains(monday.plusDays(it.toLong())) }) ids += "consistency-week"
    }

    val now = Instant.now().toString()
    return ids.filter { it !in earned }.mapNotNull { id -> ALL_ACHIEVEMENTS.find { it.id == id }?.copy(unlockedAt = now) }
}

/** Evalua y persiste (POST /data/achievements). Devuelve los nuevos para animar/avisar. */
suspend fun unlockNewAchievements(repo: Repository): List<Achievement> {
    val fresh = evaluateAchievements(repo.state.value)
    if (fresh.isEmpty()) return fresh
    fresh.forEach { repo.unlockAchievement(it) }
    return fresh
}
