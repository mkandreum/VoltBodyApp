package com.voltbody.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voltbody.app.data.repository.AppRepository
import com.voltbody.app.domain.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val repository: AppRepository
) : ViewModel() {

    // Delegate flows to repository
    val authToken: StateFlow<String?> = repository.authToken
    val user: StateFlow<User?> = repository.user
    val isAuthenticated: StateFlow<Boolean> = repository.isAuthenticated
    val isOnboarded: StateFlow<Boolean> = repository.isOnboarded
    val hasHydrated: StateFlow<Boolean> = repository.hasHydrated
    
    val profile: StateFlow<UserProfile?> = repository.profile
    val routine: StateFlow<List<WorkoutDay>> = repository.routine
    val diet: StateFlow<DietPlan?> = repository.diet
    val insights: StateFlow<Insights?> = repository.insights
    val theme: StateFlow<AppTheme> = repository.theme
    
    val motivationPhrase: StateFlow<String> = repository.motivationPhrase
    val motivationPhoto: StateFlow<String?> = repository.motivationPhoto
    val profilePhoto: StateFlow<String?> = repository.profilePhoto
    val notificationsEnabled: StateFlow<Boolean> = repository.notificationsEnabled

    val workoutLogs: StateFlow<List<WorkoutLog>> = repository.workoutLogs
    val weightLogs: StateFlow<List<WeightLog>> = repository.weightLogs
    val recoveryLogs: StateFlow<List<RecoveryLog>> = repository.recoveryLogs
    val progressPhotos: StateFlow<List<ProgressPhoto>> = repository.progressPhotos

    val weeklyGoals: StateFlow<List<WeeklyGoal>> = repository.weeklyGoals
    val achievements: StateFlow<List<Achievement>> = repository.achievements
    val mealEatenRecord: StateFlow<Map<String, List<String>>> = repository.mealEatenRecord

    // UI state local to ViewModel (tabs/toasts)
    private val _currentTab = MutableStateFlow(AppTab.HOME)
    val currentTab: StateFlow<AppTab> = _currentTab.asStateFlow()

    private val _toasts = MutableStateFlow<List<AppToast>>(emptyList())
    val toasts: StateFlow<List<AppToast>> = _toasts.asStateFlow()

    // Methods delegating to repository
    fun setAuthToken(token: String?) = repository.setAuthToken(token)
    fun setUser(user: User?) = repository.setUser(user)
    fun completeOnboarding() = repository.completeOnboarding()
    fun logout() = repository.logout()

    fun setProfile(profile: UserProfile) = repository.setProfile(profile)
    fun updateProfile(updates: UserProfile) = repository.setProfile(updates)
    fun setRoutine(routine: List<WorkoutDay>) = repository.setRoutine(routine)
    fun setDiet(diet: DietPlan) = repository.setDiet(diet)
    fun setTheme(theme: AppTheme) = repository.setTheme(theme)
    fun setMotivationPhrase(phrase: String) = repository.setMotivationPhrase(phrase)
    fun setMotivationPhoto(url: String?) = repository.setMotivationPhoto(url)
    fun setProfilePhoto(url: String?) = repository.setProfilePhoto(url)
    fun setNotificationsEnabled(v: Boolean) = repository.setNotificationsEnabled(v)

    fun addWorkoutLog(log: WorkoutLog) = repository.addWorkoutLog(log)
    fun addWeightLog(log: WeightLog) = repository.addWeightLog(log)
    fun toggleMealEaten(mealId: String, date: String) = repository.toggleMealEaten(mealId, date)
    fun toggleWeeklyGoal(id: String) = repository.toggleWeeklyGoal(id)

    // Local UI methods
    fun setTab(tab: AppTab) { _currentTab.value = tab }
    fun showToast(toast: AppToast) {
        _toasts.value = listOf(toast) + _toasts.value.take(2)
    }
    fun dismissToast(id: String) {
        _toasts.value = _toasts.value.filter { it.id != id }
    }
}
