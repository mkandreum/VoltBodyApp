package com.voltbody.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voltbody.app.data.remote.ApiService
import com.voltbody.app.domain.model.*
import com.voltbody.app.ui.screens.profile.components.PersonalRecord
import com.voltbody.app.ui.viewmodel.AppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileState(
    val name: String = "",
    val email: String = "",
    val weightKg: Float = 70f,
    val heightCm: Int = 175,
    val age: Int = 25,
    val goal: String = "Ganar músculo",
    val useMetric: Boolean = true,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val weightLogs: List<WeightLog> = emptyList(),
    val totalWorkoutLogs: Int = 0,
    val completedWeeklyGoals: Set<String> = emptySet(),
    val notificationsEnabled: Boolean = false,
    val personalRecords: List<PersonalRecord> = emptyList()
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: ApiService,
    private val appViewModel: AppViewModel
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()
    
    private val _completedGoals = MutableStateFlow<Set<String>>(emptySet())

    init {
        viewModelScope.launch {
            combine(
                appViewModel.user,
                appViewModel.profile,
                appViewModel.workoutLogs,
                appViewModel.weightLogs,
                appViewModel.routine,
                appViewModel.notificationsEnabled,
                _completedGoals
            ) { user, profile, workoutLogs, weightLogs, routine, notifs, goals ->
                
                val prs = workoutLogs
                    .groupBy { it.exerciseId }
                    .mapNotNull { (exId, logs) ->
                        val maxLog = logs.maxByOrNull { it.weight } ?: return@mapNotNull null
                        // find exercise name
                        val exName = routine.flatMap { it.exercises }.find { it.id == exId }?.name ?: "Ejercicio"
                        PersonalRecord(exName, maxLog.weight, maxLog.reps, maxLog.date)
                    }
                    .sortedByDescending { it.weight }
                    .take(10)

                ProfileState(
                    name = user?.name ?: "",
                    email = user?.email ?: "",
                    weightKg = profile?.weight ?: 70f,
                    heightCm = profile?.height?.toInt() ?: 175,
                    age = 25, // not in profile? keep 25
                    goal = profile?.goal ?: "",
                    useMetric = true,
                    isLoading = false,
                    isSaving = _state.value.isSaving,
                    error = _state.value.error,
                    successMessage = _state.value.successMessage,
                    weightLogs = weightLogs,
                    totalWorkoutLogs = workoutLogs.size,
                    completedWeeklyGoals = goals,
                    notificationsEnabled = notifs,
                    personalRecords = prs
                )
            }.collect { _state.value = it }
        }
    }

    fun toggleWeeklyGoal(goal: String) {
        val current = _completedGoals.value.toMutableSet()
        if (current.contains(goal)) current.remove(goal) else current.add(goal)
        _completedGoals.value = current
    }
    
    fun toggleNotifications() {
        appViewModel.setNotificationsEnabled(!_state.value.notificationsEnabled)
    }

    fun addWeightLog(weight: Float) {
        val log = WeightLog(
            date = java.time.Instant.now().toString(),
            weight = weight
        )
        appViewModel.addWeightLog(log)
    }

    fun updatePhysicalData(weightKg: Float, heightCm: Int, age: Int, goal: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                api.updateProfile(weightKg = weightKg, heightCm = heightCm, age = age, goal = goal)
                _state.update {
                    it.copy(
                        isSaving = false,
                        weightKg = weightKg,
                        heightCm = heightCm,
                        age = age,
                        goal = goal,
                        successMessage = "Datos actualizados"
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun changePassword(currentPassword: String, newPassword: String) {
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true) }
            try {
                api.changePassword(currentPassword, newPassword)
                _state.update { it.copy(isSaving = false, successMessage = "Contraseña cambiada") }
            } catch (e: Exception) {
                _state.update { it.copy(isSaving = false, error = e.message) }
            }
        }
    }

    fun toggleUnits() {
        val newValue = !_state.value.useMetric
        _state.update { it.copy(useMetric = newValue) }
        viewModelScope.launch {
            try { api.updateProfile(useMetric = newValue) } catch (_: Exception) {}
        }
    }

    fun clearMessages() = _state.update { it.copy(error = null, successMessage = null) }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            appViewModel.syncData()
            kotlinx.coroutines.delay(1000)
            _isRefreshing.value = false
        }
    }
}
