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
    val weeklyWorkouts: Int = 0,
    val weeklyTarget: Int = 4,
    val weeklyVolumeKg: Float = 0f,
    val dailyVolumeKg: List<Float> = emptyList(),
    val todayWorkout: TodayWorkoutInfo? = null,
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
                        }
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }
}
