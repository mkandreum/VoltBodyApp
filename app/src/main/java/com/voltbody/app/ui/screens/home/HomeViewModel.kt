package com.voltbody.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voltbody.app.data.ApiService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class TodayWorkoutInfo(
    val id: String,
    val name: String,
    val exerciseCount: Int,
    val estimatedMinutes: Int
)

data class HomeState(
    val userName: String = "",
    val greeting: String = "¡Listo para entrenar?",
    val streakDays: Int = 0,
    // ── Gamification ─────────────────────────────────────────────────────────────────
    val xpLevel: Int = 1,
    val xpCurrent: Int = 0,
    val xpToNext: Int = 1000,
    // ── Recovery ──────────────────────────────────────────────────────────────────────
    val recoveryScore: Int? = null,
    val sleepHours: Float? = null,
    val hrv: Int? = null,
    val showRecoveryDialog: Boolean = false,
    // ── Motivation ─────────────────────────────────────────────────────────────────
    val motivationPhrase: String = "",
    val motivationPhotoUrl: String? = null,
    // ── Weekly ─────────────────────────────────────────────────────────────────────
    val weeklyWorkouts: Int = 0,
    val weeklyTarget: Int = 4,
    val weeklyVolumeKg: Float = 0f,
    val dailyVolumeKg: List<Float> = emptyList(),
    // ── Today ───────────────────────────────────────────────────────────────────────
    val todayWorkout: TodayWorkoutInfo? = null,
    // ── Achievements ──────────────────────────────────────────────────────────────
    val recentAchievements: List<AchievementPreview> = emptyList(),
    // ── Meta ──────────────────────────────────────────────────────────────────────────
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    init { loadHome() }

    fun loadHome() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            try {
                val dashboard = api.getHomeDashboard()
                _state.update {
                    it.copy(
                        isLoading = false,
                        userName = dashboard.userName,
                        greeting = dashboard.greeting,
                        streakDays = dashboard.streakDays,
                        xpLevel = dashboard.xpLevel ?: 1,
                        xpCurrent = dashboard.xpCurrent ?: 0,
                        xpToNext = dashboard.xpToNext ?: 1000,
                        recoveryScore = dashboard.recoveryScore,
                        sleepHours = dashboard.sleepHours,
                        hrv = dashboard.hrv,
                        motivationPhrase = dashboard.motivationPhrase ?: "",
                        motivationPhotoUrl = dashboard.motivationPhotoUrl,
                        weeklyWorkouts = dashboard.weeklyWorkouts,
                        weeklyTarget = dashboard.weeklyTarget,
                        weeklyVolumeKg = dashboard.weeklyVolumeKg,
                        dailyVolumeKg = dashboard.dailyVolumeKg,
                        todayWorkout = dashboard.todayWorkout?.let { w ->
                            TodayWorkoutInfo(
                                id = w.id,
                                name = w.name,
                                exerciseCount = w.exerciseCount,
                                estimatedMinutes = w.estimatedMinutes
                            )
                        },
                        recentAchievements = dashboard.recentAchievements?.map {
                            AchievementPreview(icon = it.icon, label = it.label)
                        } ?: emptyList()
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun openRecoveryCheckin() = _state.update { it.copy(showRecoveryDialog = true) }

    fun dismissRecoveryDialog() = _state.update { it.copy(showRecoveryDialog = false) }

    fun logRecovery(sleepHours: Float, hrv: Int) {
        viewModelScope.launch {
            _state.update { it.copy(showRecoveryDialog = false) }
            val score = calculateRecoveryScore(sleepHours, hrv)
            _state.update {
                it.copy(
                    recoveryScore = score,
                    sleepHours = sleepHours,
                    hrv = hrv.takeIf { it > 0 }
                )
            }
            runCatching { api.logRecovery(sleepHours = sleepHours, hrv = hrv) }
        }
    }

    private fun calculateRecoveryScore(sleep: Float, hrv: Int): Int {
        val sleepScore = (sleep / 8f).coerceIn(0f, 1f) * 60f
        val hrvScore = if (hrv > 0) (hrv.toFloat() / 100f).coerceIn(0f, 1f) * 40f else 40f
        return (sleepScore + hrvScore).toInt().coerceIn(0, 100)
    }
}
