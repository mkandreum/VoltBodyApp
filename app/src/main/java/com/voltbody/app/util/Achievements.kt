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
    Achievement("exercise-done", "Primer ejercicio completo", "🎯", "Hiciste todas las series de un ejercicio. ¡Así se empieza!"),
    Achievement("workout-1", "Primer entreno completo", "🎉", "Completaste todos los ejercicios de una sesión."),
    Achievement("workout-10", "10 entrenos completos", "🏅", "Diez sesiones completas. Ya es un hábito."),
    Achievement("workout-25", "25 entrenos completos", "👑", "Veinticinco sesiones completas. Nivel leyenda."),
    Achievement("volume-1t", "Una tonelada", "🏗️", "Moviste 1.000 kg de volumen en un solo día."),
    Achievement("volume-10t", "10 toneladas", "🐘", "10.000 kg de volumen acumulado."),
    Achievement("early-bird", "Madrugador", "🌅", "Entrenaste antes de las 8:00."),
    Achievement("night-owl", "Búho nocturno", "🦉", "Entrenaste después de las 22:00."),
    Achievement("plank-60", "Minuto de hierro", "🧱", "Aguantaste 60 segundos o más en un isométrico."),
    Achievement("weekend-warrior", "Guerrero de finde", "⚔️", "Entrenaste sábado y domingo del mismo fin de semana."),
    Achievement("meals-perfect", "Día perfecto de comidas", "🥗", "Cumpliste todas las comidas del plan en un día."),
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

    // Ejercicios completos: todas las series planificadas de un ejercicio en un mismo dia.
    val zone = java.time.ZoneId.systemDefault()
    val plannedSets = (s.routine.flatMap { it.exercises } + s.customWorkout).associate { it.id to it.sets.coerceAtLeast(1) }
    val byDay = logs.groupBy { logLocalDate(it.date) }.filterKeys { it != null }
    if (byDay.values.any { day -> day.groupingBy { it.exerciseId }.eachCount().any { (id, n) -> n >= (plannedSets[id] ?: Int.MAX_VALUE) } }) ids += "exercise-done"

    // Entrenos completos: dias en los que se hicieron todas las series de todos los ejercicios del dia planificado.
    val routineByDay = mapRoutineByWeekday(s.routine)
    val fullDays = byDay.count { (date, day) ->
        val plan = routineByDay.getOrNull(mondayFirstIndex(date!!)) ?: return@count false
        val counts = day.groupingBy { it.exerciseId }.eachCount()
        plan.exercises.isNotEmpty() && plan.exercises.all { (counts[it.id] ?: 0) >= it.sets.coerceAtLeast(1) }
    }
    if (fullDays >= 1) ids += "workout-1"
    if (fullDays >= 10) ids += "workout-10"
    if (fullDays >= 25) ids += "workout-25"

    // Volumen (kg x reps).
    if (byDay.values.any { day -> day.sumOf { it.weight * it.reps } >= 1000 }) ids += "volume-1t"
    if (logs.sumOf { it.weight * it.reps } >= 10_000) ids += "volume-10t"

    // Horario.
    val hours = logs.mapNotNull { runCatching { Instant.parse(it.date).atZone(zone).hour }.getOrNull() }
    if (hours.any { it < 8 }) ids += "early-bird"
    if (hours.any { it >= 22 }) ids += "night-owl"

    // Isometrico de 60 s o mas (ejercicio marcado como isometrico en la rutina o rutina personal).
    val iso = (s.routine.flatMap { it.exercises } + s.customWorkout).filter { it.exerciseType == "isometric" }.map { it.id }.toSet()
    if (logs.any { it.exerciseId in iso && (it.duration ?: 0) >= 60 }) ids += "plank-60"

    // Fin de semana completo.
    val dates = byDay.keys.filterNotNull().toSet()
    if (dates.any { it.dayOfWeek == java.time.DayOfWeek.SATURDAY && it.plusDays(1) in dates }) ids += "weekend-warrior"

    // Dia perfecto de comidas.
    val diet = s.diet
    if (diet != null && s.mealEaten.any { (date, eaten) ->
            val meals = runCatching { diet.mealsFor(LocalDate.parse(date)) }.getOrDefault(emptyList())
            meals.isNotEmpty() && meals.all { it.id in eaten }
        }) ids += "meals-perfect"

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
