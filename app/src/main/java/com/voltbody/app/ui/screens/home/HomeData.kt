package com.voltbody.app.ui.screens.home

import androidx.compose.runtime.Immutable
import com.voltbody.app.data.Achievement
import com.voltbody.app.data.AppState
import com.voltbody.app.data.Exercise
import com.voltbody.app.data.ProgressPhoto
import com.voltbody.app.data.RecoveryLog
import com.voltbody.app.data.WeeklyGoal
import com.voltbody.app.data.WeightLog
import com.voltbody.app.util.RecoveryAdvice
import com.voltbody.app.util.computeSmartStreak
import com.voltbody.app.util.getRecoveryAdvice
import com.voltbody.app.util.logLocalDate
import com.voltbody.app.util.mapRoutineByWeekday
import com.voltbody.app.util.mondayFirstIndex
import com.voltbody.app.util.mondayOf
import java.time.LocalDate

internal const val HM_XP_PER_LOG = 12
internal const val HM_XP_PER_STREAK_DAY = 8
internal const val HM_XP_PER_LEVEL = 250
private const val MINUTES_PER_EXERCISE = 5

private fun parseMealHour(time: String): Int = Regex("^(\\d{1,2}):").find(time)?.groupValues?.get(1)?.toIntOrNull() ?: -1

/** Mensaje del coach (titulo, subtitulo, etiqueta de intensidad). */
@Immutable
data class HomeCoach(val title: String, val subtitle: String, val label: String)

/**
 * Foto fija de TODO lo que la pantalla Home necesita, derivada de [AppState] fuera del hilo principal
 * (Dispatchers.Default) y con igualdad estructural: la UI solo se recompone cuando algo cambia de verdad.
 */
@Immutable
data class HomeUi(
    val hydrated: Boolean,
    val authed: Boolean,
    val name: String?,
    val profilePhoto: String?,
    val motivationPhoto: String?,
    val phrase: String,
    val dailyQuote: String?,
    val todayFocus: String?,
    val hasTodayRoutine: Boolean,
    val todayExerciseCount: Int,
    val todayExercises: List<Exercise>,
    val featured: List<Exercise>,
    val todayDoneIds: Set<String>,
    val routineCompletion: Int,
    val weeklyConsistency: Int,
    val streak: Int,
    val totalXp: Int,
    val level: Int,
    val xpInLevel: Int,
    val logCount: Int,
    val caloriesTarget: Int,
    val consumedKcal: Int,
    val nutritionAdherence: Int,
    val estimatedMinutes: Int,
    val waterMl: Int,
    val todayStepsMetric: Long,
    /** Pasos de los 6 dias anteriores a hoy (de mas antiguo a mas reciente). */
    val pastSteps: List<Float>,
    val recoveryScore: Int,
    val advice: RecoveryAdvice,
    val recoveryHistory: List<Float>,
    val todayRecovery: RecoveryLog?,
    val coach: HomeCoach,
    val weightLast: Double?,
    val weightDelta: Double?,
    val weightHistory: List<Float>,
    val weightLogs: List<WeightLog>,
    val achievements: List<Achievement>,
    val goals: List<WeeklyGoal>,
    val weeklyGoalsWeek: String?,
    val photos: List<ProgressPhoto>,
    val timeline: List<TimelineItem>,
    val liftChart: List<Float>,
    val weekdayName: String,
)

fun buildHomeUi(st: AppState, today: LocalDate, weekdayName: String): HomeUi {
    val todayKey = today.toString()
    val routineByDay = mapRoutineByWeekday(st.routine)
    val todayRoutine = routineByDay[mondayFirstIndex(today)]
    val logDates = st.logs.mapNotNull { l -> logLocalDate(l.date)?.let { it to l } }
    val monday = mondayOf(today)
    val completedDays = logDates.map { it.first }.filter { !it.isBefore(monday) && !it.isAfter(monday.plusDays(6)) }.toSet().size
    val streak = computeSmartStreak(st.logs, st.routine)
    val todayDoneIds = logDates.filter { it.first == today }.map { it.second.exerciseId }.toSet()
    val todayEx = todayRoutine?.exercises.orEmpty()
    val routineCompletion = if (todayEx.isEmpty()) 0 else Math.round(todayEx.count { it.id in todayDoneIds } * 100f / todayEx.size)
    val weeklyConsistency = minOf(Math.round(completedDays / 7f * 100), 100)

    val diet = st.diet
    val meals = diet?.mealsFor(today).orEmpty()
    val eatenToday = st.mealEaten[todayKey].orEmpty()
    val consumedKcal = meals.filter { it.id in eatenToday }.sumOf { it.calories }
    val nutritionAdherence = if (meals.isEmpty()) 0 else Math.round(meals.count { it.id in eatenToday } * 100f / meals.size)
    val totalXp = st.logs.size * HM_XP_PER_LOG + streak * HM_XP_PER_STREAK_DAY

    val rec = st.insights?.sleepRecommendation.orEmpty()
    val sleepScore = if (rec.isBlank()) 72 else {
        val h = Regex("(\\d+(?:[.,]\\d+)?)\\s*hor", RegexOption.IGNORE_CASE).find(rec)?.groupValues?.get(1)?.replace(',', '.')?.toDoubleOrNull() ?: 7.0
        Math.round((h / 8 * 100).coerceIn(40.0, 100.0)).toInt()
    }
    val todayRecovery = st.recoveryLogs.firstOrNull { it.date == todayKey }
    val recoveryScore = todayRecovery?.score ?: sleepScore
    val advice = getRecoveryAdvice(recoveryScore, todayRoutine?.focus)
    val coach = when {
        recoveryScore < 65 -> HomeCoach("${advice.emoji} ${advice.title}", advice.subtitle, advice.intensityLabel)
        routineCompletion < 45 && todayRoutine != null -> HomeCoach(
            "🔥 Hoy toca. Sin excusas.",
            "Completa al menos ${maxOf(1, Math.ceil(todayRoutine.exercises.size * 0.6).toInt())} ejercicios para cerrar el día en verde.",
            "🚀 Empezar ahora",
        )
        recoveryScore >= 85 -> HomeCoach("${advice.emoji} ${advice.title}", advice.subtitle, advice.intensityLabel)
        else -> HomeCoach("🎯 Ritmo perfecto", "Mantén intensidad y cuida la ejecución. +1% mejor hoy.", "📋 Seguir plan")
    }

    fun eaten(pred: (Int, String) -> Boolean) = meals.any { m -> pred(parseMealHour(m.time), m.name.lowercase()) && m.id in eatenToday }
    val timeline = listOf(
        TimelineItem(st.profile.mealTimes?.breakfast ?: "08:00", "🥣 Desayuno de arranque", eaten { h, n -> (h in 6..10) || n.contains("desay") }),
        TimelineItem(st.profile.mealTimes?.lunch ?: "14:00", "🍗 Comida principal", eaten { h, n -> (h in 12..16) || n.contains("comida") }),
        TimelineItem("Entreno", "💪 ${todayRoutine?.focus ?: "Sesión rápida"}", routineCompletion >= 100),
        TimelineItem("Progreso", "📸 Subir foto del día", st.progressPhotos.any { it.date.take(10) == todayKey }),
    )

    val wl = st.weightLogs
    val liftChart = (6 downTo 0).map { i ->
        val d = today.minusDays(i.toLong())
        (logDates.filter { it.first == d }.maxOfOrNull { it.second.weight } ?: 0.0).toFloat()
    }

    return HomeUi(
        hydrated = st.hydrated,
        authed = st.isAuthenticated,
        name = st.profile.name,
        profilePhoto = st.profile.profilePhoto,
        motivationPhoto = st.profile.motivationPhoto,
        phrase = st.motivationPhrase,
        dailyQuote = st.insights?.dailyQuote,
        todayFocus = todayRoutine?.focus,
        hasTodayRoutine = todayRoutine != null,
        todayExerciseCount = todayEx.size,
        todayExercises = todayEx,
        featured = (todayRoutine ?: st.routine.firstOrNull())?.exercises.orEmpty(),
        todayDoneIds = todayDoneIds,
        routineCompletion = routineCompletion,
        weeklyConsistency = weeklyConsistency,
        streak = streak,
        totalXp = totalXp,
        level = totalXp / HM_XP_PER_LEVEL + 1,
        xpInLevel = totalXp % HM_XP_PER_LEVEL,
        logCount = st.logs.size,
        caloriesTarget = diet?.dailyCalories ?: 0,
        consumedKcal = consumedKcal,
        nutritionAdherence = nutritionAdherence,
        estimatedMinutes = todayEx.size * MINUTES_PER_EXERCISE,
        waterMl = st.waterOn(today),
        todayStepsMetric = st.stepsOn(today),
        pastSteps = (6 downTo 1).map { i -> st.stepsOn(today.minusDays(i.toLong())).toFloat() },
        recoveryScore = recoveryScore,
        advice = advice,
        recoveryHistory = st.recoveryLogs.takeLast(7).map { it.score.toFloat() },
        todayRecovery = todayRecovery,
        coach = coach,
        weightLast = wl.lastOrNull()?.weight,
        weightDelta = if (wl.size >= 2) wl.last().weight - wl[wl.size - 2].weight else null,
        weightHistory = wl.takeLast(10).map { it.weight.toFloat() },
        weightLogs = wl,
        achievements = st.achievements,
        goals = st.weeklyGoals,
        weeklyGoalsWeek = st.weeklyGoalsWeek,
        photos = st.progressPhotos.sortedByDescending { it.date },
        timeline = timeline,
        liftChart = liftChart,
        weekdayName = weekdayName,
    )
}
