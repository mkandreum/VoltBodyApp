package com.voltbody.app.data

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject

/** Json compartido: tolerante con el servidor (campos extra/nulos). */
val AppJson = Json { ignoreUnknownKeys = true; coerceInputValues = true; explicitNulls = false; isLenient = true; encodeDefaults = true }

@Serializable data class AuthUser(val id: String = "", val email: String = "", val name: String? = null)

@Serializable data class MealTimes(val breakfast: String = "08:00", val brunch: String = "11:00", val lunch: String = "14:00", val snack: String = "17:00", val dinner: String = "21:00")
@Serializable data class FoodPreferences(val vegetables: List<String> = emptyList(), val carbs: List<String> = emptyList(), val proteins: List<String> = emptyList())
@Serializable data class WeeklySpecialSession(val enabled: Boolean = false, val activity: String = "", val day: String = "", val durationMinutes: Int = 0)
@Serializable data class AvatarConfig(val muscleMass: Double = 50.0, val bodyFat: Double = 20.0)
@Serializable data class SpecialDish(val ingredients: String = "", val targetCalories: Int = 0)

/** Equivale a UserProfile de la web (+ name que viene de user). Todos opcionales como en el servidor. */
@Serializable data class Profile(
    val name: String? = null,
    val age: Int? = null, val weight: Double? = null, val height: Double? = null,
    val gender: String? = null, val goal: String? = null,
    val goalDirection: String? = null, val goalTargetKg: Double? = null, val goalTimelineMonths: Int? = null,
    val currentState: String? = null, val schedule: String? = null, val workHours: String? = null,
    val trainingDaysPerWeek: Int? = null, val sessionMinutes: Int? = null,
    val theme: String? = null, val motivationPhrase: String? = null, val motivationPhoto: String? = null,
    val mealTimes: MealTimes? = null, val foodPreferences: FoodPreferences? = null,
    val weeklySpecialSession: WeeklySpecialSession? = null, val specialDish: SpecialDish? = null,
    val avatarConfig: AvatarConfig? = null, val profilePhoto: String? = null,
)

@Serializable data class Exercise(
    val id: String = "", val name: String = "", val nameEn: String? = null,
    val sets: Int = 3, val reps: String = "10", val weight: Double = 0.0,
    val gifUrl: String = "", val muscleGroup: String = "", val technique: String? = null,
    val exerciseType: String? = null, // weighted | isometric | bodyweight | cardio
    val durationTarget: Int? = null,
)
@Serializable data class WorkoutDay(val day: String = "", val focus: String = "", val exercises: List<Exercise> = emptyList())

@Serializable data class Meal(
    val id: String = "", val name: String = "", val time: String = "",
    val calories: Int = 0, val protein: Int = 0, val carbs: Int = 0, val fat: Int = 0, val description: String = "",
)
@Serializable data class Macros(val protein: Int = 0, val carbs: Int = 0, val fat: Int = 0)
/** Menu de un dia de la semana ([day] = "Lunes".."Domingo"). */
@Serializable data class DayMenu(val day: String = "", val meals: List<Meal> = emptyList())

/**
 * Plan de comidas. [meals] es el menu base (lo que usan la web y los planes antiguos);
 * [week] trae un menu distinto por dia cuando el servidor lo genera (7 entradas, lunes primero).
 */
@Serializable data class DietPlan(
    val dailyCalories: Int = 0, val macros: Macros = Macros(), val meals: List<Meal> = emptyList(),
    val week: List<DayMenu> = emptyList(),
) {
    /** Indice 0 = lunes ... 6 = domingo del menu de [week] para ese dia, o -1 si no hay. */
    fun weekIndexFor(date: java.time.LocalDate): Int {
        val wd = date.dayOfWeek.value - 1
        val i = week.indexOfFirst { com.voltbody.app.util.weekdayIndexFromName(it.day) == wd && it.meals.isNotEmpty() }
        return if (i >= 0) i else if (week.size == 7 && week[wd].meals.isNotEmpty()) wd else -1
    }

    /** Comidas de [date]: las del dia de la semana si hay menu semanal, si no el menu base. */
    fun mealsFor(date: java.time.LocalDate): List<Meal> = weekIndexFor(date).let { if (it >= 0) week[it].meals else meals }

    /** Sustituye la comida [oldId] por [newMeal] solo en el dia [date] (o en el menu base si no hay semana). */
    fun replaceMeal(date: java.time.LocalDate, oldId: String, newMeal: Meal): DietPlan {
        val i = weekIndexFor(date)
        return if (i < 0) copy(meals = meals.map { if (it.id == oldId) newMeal else it })
        else copy(week = week.mapIndexed { j, d -> if (j == i) d.copy(meals = d.meals.map { if (it.id == oldId) newMeal else it }) else d })
    }
}

@Serializable data class WorkoutLog(
    val date: String, val exerciseId: String, val weight: Double, val reps: Int,
    val duration: Int? = null, val rpe: Int? = null, val rir: Int? = null,
    val id: String? = null, // id del servidor (necesario para PUT/DELETE)
)
@Serializable data class WeightLog(val date: String, val weight: Double) // date = YYYY-MM-DD
@Serializable data class ProgressPhoto(val date: String, val url: String)

@Serializable data class Achievement(val id: String, val label: String = "", val icon: String = "", val description: String = "", val unlockedAt: String? = null)
@Serializable data class WeeklyGoal(val id: String, val label: String, val done: Boolean = false)
@Serializable data class RecoveryLog(val date: String, val sleepHours: Double, val hrv: Double? = null, val score: Int)

/** Métricas diarias (Health Connect + agua). Todos los campos opcionales; date = YYYY-MM-DD. */
@Serializable data class DailyMetric(
    val date: String = "",
    val steps: Int? = null, val distanceKm: Double? = null, val activeKcal: Double? = null, val totalKcal: Double? = null,
    val sleepHours: Double? = null, val hrv: Double? = null, val restingHr: Int? = null, val avgHr: Int? = null,
    val waterMl: Int? = null,
)
@Serializable data class MealLog(val date: String, val mealId: String)
@Serializable data class GoalRow(val weekStart: String = "", val goalId: String = "", val label: String = "", val done: Boolean = false)
@Serializable data class AchievementRow(val id: String = "", val unlockedAt: String? = null)
@Serializable data class DataAll(
    val metrics: List<DailyMetric> = emptyList(),
    val mealLogs: List<MealLog> = emptyList(),
    val goals: List<GoalRow> = emptyList(),
    val achievements: List<AchievementRow> = emptyList(),
    val customWorkout: List<Exercise> = emptyList(),
    val recovery: List<RecoveryLog> = emptyList(),
)

@Serializable data class Insights(
    val sleepRecommendation: String = "", val estimatedResults: String = "", val dailyQuote: String = "",
    val appPreferences: JsonObject? = null,
)

@Serializable data class ProgressReport(
    val overallScore: Int = 0, val progressPercent: Int = 0, val consistencyPercent: Int = 0,
    val nutritionPercent: Int = 0, val trainingExecutionPercent: Int = 0, val weeksToVisibleChange: Int = 0,
    val summary: String = "", val improvements: List<String> = emptyList(), val nextActions: List<String> = emptyList(),
)

@Serializable data class Plan(val routine: List<WorkoutDay> = emptyList(), val diet: DietPlan = DietPlan(), val insights: Insights = Insights(), val fallback: Boolean = false, val details: String? = null)

/** Estado en memoria (todo viene del servidor salvo serverUrl/authToken/user, que se persisten en disco). */
@Serializable data class AppState(
    val hydrated: Boolean = false,
    val serverUrl: String = "",
    val authToken: String? = null,
    val user: AuthUser? = null,
    val isOnboarded: Boolean = false,
    /** true cuando el perfil ya llego del servidor (hasta entonces no se decide si mostrar onboarding). */
    val profileLoaded: Boolean = false,
    val loadError: String? = null,
    val profile: Profile = Profile(),
    val routine: List<WorkoutDay> = emptyList(),
    val diet: DietPlan? = null,
    val insights: Insights? = null,
    val logs: List<WorkoutLog> = emptyList(),
    val weightLogs: List<WeightLog> = emptyList(),
    val progressPhotos: List<ProgressPhoto> = emptyList(),
    val theme: String = "verde-negro", // verde-negro (Lima) | aguamarina-negro (Aguamarina) | ocaso-negro (Ocaso)
    val motivationPhrase: String = "Cada repetición te acerca a tu mejor versión.",
    val metrics: List<DailyMetric> = emptyList(),
    val mealEaten: Map<String, List<String>> = emptyMap(), // fecha -> ids de comidas hechas
    val weeklyGoals: List<WeeklyGoal> = emptyList(),
    val weeklyGoalsWeek: String? = null, // lunes (YYYY-MM-DD) de weeklyGoals
    val achievements: List<Achievement> = emptyList(),
    val customWorkout: List<Exercise> = emptyList(),
    val recoveryLogs: List<RecoveryLog> = emptyList(),
    val notificationsEnabled: Boolean = false,
) { val isAuthenticated get() = authToken != null }

/** Lo único que se guarda en disco. */
@Serializable data class PersistedSession(val serverUrl: String = "", val authToken: String? = null, val user: AuthUser? = null)
