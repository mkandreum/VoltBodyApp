package com.voltbody.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voltbody.app.data.remote.ApiService
import com.voltbody.app.data.remote.dto.*
import com.voltbody.app.domain.model.*
import com.voltbody.app.domain.usecase.*
import com.voltbody.app.ui.viewmodel.AppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ── Gamification constants (matching web values exactly) ──────────────────────
private const val XP_PER_LOG = 12
private const val XP_PER_STREAK_DAY = 8
private const val XP_PER_LEVEL = 250

data class TimelineItem(
    val time: String,
    val title: String,
    val done: Boolean
)

data class HomeState(
    val userName: String = "",
    val greeting: String = "¡Listo para entrenar?",
    val streakDays: Int = 0,
    // ── Gamification ─────────────────────────────────────────────────────────
    val xpLevel: Int = 1,
    val xpCurrent: Int = 0,
    val xpToNext: Int = XP_PER_LEVEL,
    val totalXP: Int = 0,
    val todayXP: Int = 0,
    // ── Recovery ──────────────────────────────────────────────────────────────
    val recoveryScore: Int? = null,
    val sleepHours: Float? = null,
    val hrv: Int? = null,
    val showRecoveryDialog: Boolean = false,
    // ── Motivation ─────────────────────────────────────────────────────────────
    val motivationPhrase: String = "",
    val motivationPhotoUrl: String? = null,
    // ── Weekly ─────────────────────────────────────────────────────────────────
    val weeklyWorkouts: Int = 0,
    val weeklyTarget: Int = 4,
    val weeklyVolumeKg: Float = 0f,
    val dailyVolumeKg: List<Float> = emptyList(),
    // ── Today ───────────────────────────────────────────────────────────────────
    val todayWorkout: TodayWorkoutInfo? = null,
    val todayLogs: Int = 0,
    // ── Achievements ──────────────────────────────────────────────────────────
    val recentAchievements: List<Achievement> = emptyList(),
    // ── Fatigue Index ──────────────────────────────────────────────────────────
    val fatigueEntries: List<FatigueEntry> = emptyList(),
    // ── Timeline ──────────────────────────────────────────────────────────────
    val timelineItems: List<TimelineItem> = emptyList(),
    // ── Progress Report (AI) ──────────────────────────────────────────────────
    val reportLoading: Boolean = false,
    val reportProgress: Int = 0,
    val report: ProgressReportResponse? = null,
    // ── BLE Heart Rate ────────────────────────────────────────────────────────
    val heartRate: Int? = null,
    val bleConnected: Boolean = false,
    // ── Meta ──────────────────────────────────────────────────────────────────
    val isLoading: Boolean = true,
    val error: String? = null
)

data class TodayWorkoutInfo(
    val id: String,
    val name: String,
    val exerciseCount: Int,
    val estimatedMinutes: Int
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: ApiService,
    private val appViewModel: AppViewModel
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                appViewModel.user,
                appViewModel.profile,
                appViewModel.routine,
                appViewModel.workoutLogs,
                appViewModel.diet
            ) { user, profile, routine, logs, diet ->
                computeHomeState(user, profile, routine, logs, diet)
            }.collect { newState ->
                _state.update { old ->
                    // Preserve report/BLE/recovery dialog state from old
                    newState.copy(
                        report = old.report,
                        reportLoading = old.reportLoading,
                        reportProgress = old.reportProgress,
                        showRecoveryDialog = old.showRecoveryDialog,
                        heartRate = old.heartRate,
                        bleConnected = old.bleConnected
                    )
                }
            }
        }

        // Also observe achievements, motivation, and recovery
        viewModelScope.launch {
            combine(
                appViewModel.achievements,
                appViewModel.motivationPhrase,
                appViewModel.motivationPhoto,
                appViewModel.recoveryLogs,
                appViewModel.mealEatenRecord
            ) { achievements, phrase, photo, recoveryLogs, mealEaten ->
                val today = LocalDate.now().toString()
                val todayRecovery = recoveryLogs.firstOrNull { it.date == today }
                val todayEatenMeals = mealEaten[today] ?: emptyList()

                HomeSecondaryData(achievements, phrase, photo, todayRecovery, todayEatenMeals)
            }.collect { data ->
                _state.update { old ->
                    old.copy(
                        recentAchievements = data.achievements.sortedByDescending { it.unlockedAt }.take(5),
                        motivationPhrase = data.phrase,
                        motivationPhotoUrl = data.photo,
                        recoveryScore = data.todayRecovery?.score,
                        sleepHours = data.todayRecovery?.sleepHours,
                        hrv = data.todayRecovery?.hrv?.toInt(),
                        timelineItems = buildTimeline(
                            old, data.todayEatenMeals, data.todayRecovery != null
                        )
                    )
                }
            }
        }
    }

    private data class HomeSecondaryData(
        val achievements: List<Achievement>,
        val phrase: String,
        val photo: String?,
        val todayRecovery: RecoveryLog?,
        val todayEatenMeals: List<String>
    )

    private fun computeHomeState(
        user: User?,
        profile: UserProfile?,
        routine: List<WorkoutDay>,
        logs: List<WorkoutLog>,
        diet: DietPlan?
    ): HomeState {
        val today = LocalDate.now()
        val todayStr = today.toString()
        val todayDayIndex = getMondayFirstIndex(today)
        val routineByDay = mapRoutineByWeekday(routine)
        val currentDay = routineByDay[todayDayIndex]

        // Streak
        val streak = computeSmartStreak(logs, routine)

        // XP & Level (matching web exactly)
        val totalXP = logs.size * XP_PER_LOG + streak * XP_PER_STREAK_DAY
        val level = totalXP / XP_PER_LEVEL + 1
        val xpInCurrentLevel = totalXP % XP_PER_LEVEL

        // Today's logs & XP
        val todayLogs = logs.filter { it.date.take(10) == todayStr }
        val todayXP = todayLogs.size * XP_PER_LOG

        // Weekly stats
        val weekStart = today.with(java.time.DayOfWeek.MONDAY)
        val weekStartStr = weekStart.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val weekLogs = logs.filter { it.date.take(10) >= weekStartStr }
        val weeklyWorkouts = weekLogs.map { it.date.take(10) }.distinct().size
        val weeklyVolumeKg = weekLogs.sumOf { (it.weight * it.reps).toDouble() }.toFloat()

        // Daily volume for chart (7 days Mon-Sun)
        val dailyVolume = (0..6).map { dayOffset ->
            val dayStr = weekStart.plusDays(dayOffset.toLong()).toString()
            logs.filter { it.date.take(10) == dayStr }
                .sumOf { (it.weight * it.reps).toDouble() }.toFloat()
        }

        // Fatigue index
        val fatigueEntries = computeFatigueIndex(logs, routine)

        // Today workout info
        val todayWorkout = currentDay?.let {
            TodayWorkoutInfo(
                id = it.day,
                name = "${it.focus}",
                exerciseCount = it.exercises.size,
                estimatedMinutes = it.exercises.size * 7  // ~7 min per exercise estimate
            )
        }

        // Greeting based on time of day
        val hour = java.time.LocalTime.now().hour
        val greeting = when {
            hour < 6  -> "Madrugador, ${user?.name?.split(" ")?.firstOrNull() ?: "crac"}. ¡A darle!"
            hour < 12 -> "Buenos días, ${user?.name?.split(" ")?.firstOrNull() ?: "crac"} 💪"
            hour < 18 -> "Buenas tardes, ${user?.name?.split(" ")?.firstOrNull() ?: "crac"} 🔥"
            else      -> "Buenas noches, ${user?.name?.split(" ")?.firstOrNull() ?: "crac"} 🌙"
        }

        return HomeState(
            userName = user?.name ?: "",
            greeting = greeting,
            streakDays = streak,
            xpLevel = level,
            xpCurrent = xpInCurrentLevel,
            xpToNext = XP_PER_LEVEL,
            totalXP = totalXP,
            todayXP = todayXP,
            weeklyWorkouts = weeklyWorkouts,
            weeklyTarget = profile?.trainingDaysPerWeek ?: 4,
            weeklyVolumeKg = weeklyVolumeKg,
            dailyVolumeKg = dailyVolume,
            todayWorkout = todayWorkout,
            todayLogs = todayLogs.size,
            fatigueEntries = fatigueEntries,
            isLoading = false
        )
    }

    private fun buildTimeline(
        state: HomeState,
        todayEatenMeals: List<String>,
        hasRecoveryLog: Boolean
    ): List<TimelineItem> {
        val items = mutableListOf<TimelineItem>()

        // Recovery check-in
        items += TimelineItem("07:00", "🩺 Check-in de recuperación", hasRecoveryLog)

        // Breakfast
        items += TimelineItem("08:00", "🍳 Desayuno", todayEatenMeals.isNotEmpty())

        // Training
        val workoutDone = state.todayLogs > 0
        items += TimelineItem(
            "10:00",
            "🏋️ ${state.todayWorkout?.name ?: "Entrenamiento"}",
            workoutDone
        )

        // Lunch
        items += TimelineItem("14:00", "🥗 Comida principal", todayEatenMeals.size >= 2)

        // Snack
        items += TimelineItem("17:00", "🍌 Merienda", todayEatenMeals.size >= 3)

        // Dinner
        items += TimelineItem("21:00", "🍽️ Cena", todayEatenMeals.size >= 4)

        return items
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    fun openRecoveryCheckin() = _state.update { it.copy(showRecoveryDialog = true) }

    fun dismissRecoveryDialog() = _state.update { it.copy(showRecoveryDialog = false) }

    fun logRecovery(sleepHours: Float, hrv: Int) {
        viewModelScope.launch {
            _state.update { it.copy(showRecoveryDialog = false) }
            val score = computeRecoveryScore(sleepHours, if (hrv > 0) hrv.toFloat() else null, emptyList())
            appViewModel.addRecoveryLog(
                RecoveryLog(
                    date = LocalDate.now().toString(),
                    sleepHours = sleepHours,
                    hrv = if (hrv > 0) hrv.toFloat() else null,
                    score = score
                )
            )
            _state.update {
                it.copy(
                    recoveryScore = score,
                    sleepHours = sleepHours,
                    hrv = hrv.takeIf { it > 0 }
                )
            }
        }
    }

    fun generateProgressReport() {
        val profile = appViewModel.profile.value ?: return
        val token = appViewModel.authToken.value ?: return
        val routine = appViewModel.routine.value
        val diet = appViewModel.diet.value
        val logs = appViewModel.workoutLogs.value
        val photos = appViewModel.progressPhotos.value

        viewModelScope.launch {
            _state.update { it.copy(reportLoading = true, reportProgress = 0, report = null) }

            // Simulate progress bar (like web does)
            val progressJob = launch {
                for (p in 1..85) {
                    delay(80)
                    _state.update { it.copy(reportProgress = p) }
                }
            }

            runCatching {
                api.generateProgressReport(
                    "Bearer $token",
                    ProgressReportRequest(
                        profile = profile,
                        logs = logs.map { WorkoutLogDto(it.date, it.exerciseId, it.weight, it.reps, it.duration, it.rpe, it.rir) },
                        routine = routine,
                        diet = diet,
                        progressPhotos = photos.map { ProgressPhotoDto(it.date, it.url) }
                    )
                )
            }.onSuccess { response ->
                progressJob.cancel()
                _state.update { it.copy(reportProgress = 100) }
                delay(300)
                _state.update {
                    it.copy(
                        reportLoading = false,
                        report = response.body(),
                        reportProgress = 0
                    )
                }
            }.onFailure { e ->
                progressJob.cancel()
                _state.update { it.copy(reportLoading = false, reportProgress = 0) }
                appViewModel.showToast(
                    AppToast(id = "report_err", type = ToastType.ERROR, title = "Error generando informe", message = e.message)
                )
            }
        }
    }

    fun quickLogSet() {
        val routine = appViewModel.routine.value
        val today = LocalDate.now()
        val todayDayIndex = getMondayFirstIndex(today)
        val routineByDay = mapRoutineByWeekday(routine)
        val currentDay = routineByDay[todayDayIndex] ?: return
        val firstExercise = currentDay.exercises.firstOrNull() ?: return

        appViewModel.addWorkoutLog(
            WorkoutLog(
                date = Instant.now().toString(),
                exerciseId = firstExercise.id,
                weight = firstExercise.weight,
                reps = 10
            )
        )
        appViewModel.showToast(
            AppToast(id = "quick_log", type = ToastType.SUCCESS, title = "Serie rápida registrada 💪", message = firstExercise.name)
        )
    }

    fun updateHeartRate(bpm: Int?) {
        _state.update { it.copy(heartRate = bpm) }
    }

    fun setBleConnected(connected: Boolean) {
        _state.update { it.copy(bleConnected = connected) }
    }
}

