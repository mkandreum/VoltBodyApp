package com.voltbody.app.util

import com.voltbody.app.data.Repository
import com.voltbody.app.data.WeeklyGoal
import com.voltbody.app.data.WorkoutDay
import com.voltbody.app.data.WorkoutLog
import java.text.Normalizer
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

/** Port de routineWeek.ts: semana ISO (lunes primero), mapeo de la rutina a dias reales y racha inteligente. */
data class WeekdayLabel(val key: String, val short: String, val full: String)

val WEEKDAY_LABELS = listOf(
    WeekdayLabel("lun", "Lun", "Lunes"),
    WeekdayLabel("mar", "Mar", "Martes"),
    WeekdayLabel("mie", "Mie", "Miercoles"),
    WeekdayLabel("jue", "Jue", "Jueves"),
    WeekdayLabel("vie", "Vie", "Viernes"),
    WeekdayLabel("sab", "Sab", "Sabado"),
    WeekdayLabel("dom", "Dom", "Domingo"),
)

private const val MAX_STREAK_DAYS = 365

/** Habitos semanales por defecto (los mismos que la web). */
val DEFAULT_WEEKLY_GOALS = listOf(
    WeeklyGoal("habit-1", "Completar 3 sesiones de fuerza", false),
    WeeklyGoal("habit-2", "Dormir 7h al menos 5 dias", false),
    WeeklyGoal("habit-3", "Cumplir proteina diaria 5 dias", false),
)

private fun normalizeText(v: String): String =
    Normalizer.normalize(v.lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "").trim()

/** 0 = lunes ... 6 = domingo a partir del nombre del dia ("Lunes", "miércoles"...); -1 si no se reconoce. */
fun weekdayIndexFromName(dayName: String): Int {
    val n = normalizeText(dayName)
    return when {
        n.contains("lun") -> 0
        n.contains("mar") -> 1
        n.contains("mie") -> 2
        n.contains("jue") -> 3
        n.contains("vie") -> 4
        n.contains("sab") -> 5
        n.contains("dom") -> 6
        else -> -1
    }
}

/** 0 = lunes ... 6 = domingo. */
fun mondayFirstIndex(date: LocalDate): Int = date.dayOfWeek.value - 1

/** Lunes de la semana de [date]. */
fun mondayOf(date: LocalDate): LocalDate = date.minusDays((date.dayOfWeek.value - 1).toLong())

/** Fecha local de un log (ISO instant del servidor o YYYY-MM-DD). */
fun logLocalDate(iso: String): LocalDate? =
    runCatching { Instant.parse(iso).atZone(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
        ?: runCatching { OffsetDateTime.parse(iso).atZoneSameInstant(ZoneId.systemDefault()).toLocalDate() }.getOrNull()
        ?: runCatching { LocalDate.parse(iso.take(10)) }.getOrNull()

/** Rutina repartida en 7 huecos (lun..dom); los dias sin nombre reconocible ocupan huecos libres. */
fun mapRoutineByWeekday(routine: List<WorkoutDay>): List<WorkoutDay?> {
    val mapped = arrayOfNulls<WorkoutDay>(7)
    val unresolved = mutableListOf<WorkoutDay>()
    routine.forEach { day ->
        val idx = weekdayIndexFromName(day.day)
        if (idx >= 0 && mapped[idx] == null) mapped[idx] = day else unresolved.add(day)
    }
    unresolved.forEach { day ->
        val free = mapped.indexOfFirst { it == null }
        if (free >= 0) mapped[free] = day
    }
    return mapped.toList()
}

/**
 * Racha actual saltando dias de descanso: solo los dias de entreno planificados sin logs la rompen;
 * el entreno de hoy aun no hecho no penaliza.
 */
fun computeSmartStreak(logs: List<WorkoutLog>, routine: List<WorkoutDay>, today: LocalDate = LocalDate.now()): Int {
    if (logs.isEmpty()) return 0
    val workoutWeekdays = mapRoutineByWeekday(routine).mapIndexedNotNull { i, d -> if (d != null) i else null }.toSet()
    val dateSet = logs.mapNotNull { logLocalDate(it.date) }.toSet()
    var streak = 0
    var cursor = today
    for (i in 0 until MAX_STREAK_DAYS) {
        val isWorkoutDay = workoutWeekdays.isEmpty() || workoutWeekdays.contains(mondayFirstIndex(cursor))
        if (isWorkoutDay) {
            if (dateSet.contains(cursor)) streak += 1
            else if (cursor == today) { /* hoy aun no entrenado: no penaliza */ }
            else break
        }
        cursor = cursor.minusDays(1)
    }
    return streak
}

/**
 * Reinicio semanal de habitos: si weeklyGoalsWeek no es el lunes actual (o no hay habitos),
 * restablece la lista por defecto sin marcar y guarda la semana. Devuelve true si reinicio.
 */
suspend fun ensureWeeklyGoals(repo: Repository, today: LocalDate = LocalDate.now()): Boolean {
    val monday = mondayOf(today).toString()
    val st = repo.state.value
    if (st.weeklyGoalsWeek == monday && st.weeklyGoals.isNotEmpty()) return false
    repo.saveGoals(monday, DEFAULT_WEEKLY_GOALS)
    return true
}

/** Alterna un habito semanal y lo persiste. */
suspend fun toggleWeeklyGoal(repo: Repository, id: String) {
    val st = repo.state.value
    val monday = st.weeklyGoalsWeek ?: mondayOf(LocalDate.now()).toString()
    repo.saveGoals(monday, st.weeklyGoals.map { if (it.id == id) it.copy(done = !it.done) else it })
}
