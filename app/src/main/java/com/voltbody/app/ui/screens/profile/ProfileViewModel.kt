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
    val completedWeeklyGoals: List<WeeklyGoal> = emptyList(),
    val notificationsEnabled: Boolean = false,
    val personalRecords: List<PersonalRecord> = emptyList(),
    val progressPhotos: List<ProgressPhoto> = emptyList(),
    val motivationPhrase: String = "",
    val motivationPhoto: String? = null,
    val profilePhoto: String? = null,
    val theme: AppTheme = AppTheme.VERDE_NEGRO
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
                appViewModel.weeklyGoals,
                appViewModel.progressPhotos,
                appViewModel.motivationPhrase,
                appViewModel.motivationPhoto,
                appViewModel.profilePhoto,
                appViewModel.theme
            ) { args ->
                val user = args[0] as User?
                val profile = args[1] as UserProfile?
                val workoutLogs = args[2] as List<WorkoutLog>
                val weightLogs = args[3] as List<WeightLog>
                val routine = args[4] as List<WorkoutDay>
                val notifs = args[5] as Boolean
                val weeklyGoals = args[6] as List<WeeklyGoal>
                val photos = args[7] as List<ProgressPhoto>
                val mPhrase = args[8] as String
                val mPhoto = args[9] as String?
                val pPhoto = args[10] as String?
                val theme = args[11] as AppTheme
                
                val prs = workoutLogs
                    .groupBy { it.exerciseId }
                    .mapNotNull { (exId, logs) ->
                        val maxLog = logs.maxByOrNull { it.weight } ?: return@mapNotNull null
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
                    age = profile?.age ?: 25,
                    goal = profile?.goal ?: "",
                    useMetric = true,
                    isLoading = false,
                    isSaving = _state.value.isSaving,
                    error = _state.value.error,
                    successMessage = _state.value.successMessage,
                    weightLogs = weightLogs,
                    totalWorkoutLogs = workoutLogs.size,
                    completedWeeklyGoals = weeklyGoals,
                    notificationsEnabled = notifs,
                    personalRecords = prs,
                    progressPhotos = photos,
                    motivationPhrase = mPhrase,
                    motivationPhoto = mPhoto,
                    profilePhoto = pPhoto,
                    theme = theme
                )
            }.collect { _state.value = it }
        }
    }

    fun toggleWeeklyGoal(goalId: String) {
        appViewModel.toggleWeeklyGoal(goalId)
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

    fun addProgressPhoto(url: String) {
        val photo = ProgressPhoto(java.time.Instant.now().toString(), url)
        appViewModel.addProgressPhoto(photo)
    }

    fun setMotivation(phrase: String, photoUrl: String?) {
        appViewModel.setMotivationPhrase(phrase)
        photoUrl?.let { appViewModel.setMotivationPhoto(it) }
    }

    fun setTheme(theme: AppTheme) {
        appViewModel.setTheme(theme)
    }

    fun setProfilePhoto(url: String) {
        appViewModel.setProfilePhoto(url)
    }
}
