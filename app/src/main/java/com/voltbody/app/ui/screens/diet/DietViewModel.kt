package com.voltbody.app.ui.screens.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voltbody.app.data.remote.ApiService
import com.voltbody.app.data.remote.dto.GenerateAlternativeMealRequest
import com.voltbody.app.domain.model.*
import com.voltbody.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

data class DietUiState(
    val diet: DietPlan? = null,
    val eatenMealIds: Set<String> = emptySet(),
    // --- calories ---
    val eatenCalories: Int = 0,
    val remainingCalories: Int = 0,
    // --- macros eaten (from checked meals) ---
    val eatenProtein: Int = 0,
    val eatenCarbs: Int = 0,
    val eatenFat: Int = 0,
    // --- other ---
    val swappingMealId: String? = null,
    val waterGlasses: Int = 0,
    val foodPreferences: FoodPreferences? = null,
    // --- date navigation ---
    val selectedDate: LocalDate = LocalDate.now(),
    val isToday: Boolean = true,
    val isMacroQuickMode: Boolean = false
)


@HiltViewModel
class DietViewModel @Inject constructor(
    private val api: ApiService,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _waterGlasses = MutableStateFlow(0)
    private val _selectedDate = MutableStateFlow(LocalDate.now())
    private val _uiState = MutableStateFlow(DietUiState())
    val uiState: StateFlow<DietUiState> = _uiState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                appRepository.diet,
                appRepository.mealEatenRecord,
                appRepository.profile,
                _waterGlasses,
                _selectedDate
            ) { diet, mealEatenRecord, profile, water, date ->
                val dateStr = date.toString()
                val eatenIds = mealEatenRecord[dateStr]?.toSet() ?: emptySet()
                val eatenMeals = diet?.meals?.filter { eatenIds.contains(it.id) } ?: emptyList()

                val eatenCalories = eatenMeals.sumOf { it.calories }
                val eatenProtein = eatenMeals.sumOf { it.protein }
                val eatenCarbs = eatenMeals.sumOf { it.carbs }
                val eatenFat = eatenMeals.sumOf { it.fat }
                val target = diet?.dailyCalories ?: 0
                val remaining = target - eatenCalories

                DietUiState(
                    diet = diet,
                    eatenMealIds = eatenIds,
                    eatenCalories = eatenCalories,
                    remainingCalories = remaining,
                    eatenProtein = eatenProtein,
                    eatenCarbs = eatenCarbs,
                    eatenFat = eatenFat,
                    swappingMealId = _uiState.value.swappingMealId,
                    waterGlasses = water,
                    foodPreferences = profile?.foodPreferences,
                    selectedDate = date,
                    isToday = date == LocalDate.now()
                )
            }.collect { _uiState.value = it }
        }
    }

    fun toggleMealEaten(mealId: String) {
        appRepository.toggleMealEaten(mealId, _selectedDate.value.toString())
    }

    fun goToPreviousDay() {
        _selectedDate.value = _selectedDate.value.minusDays(1)
    }

    fun goToNextDay() {
        val next = _selectedDate.value.plusDays(1)
        if (!next.isAfter(LocalDate.now())) {
            _selectedDate.value = next
        }
    }

    fun goToToday() {
        _selectedDate.value = LocalDate.now()
    }

    fun swapMeal(meal: Meal) {
        val profile = appRepository.profile.value ?: return
        val token = appRepository.authToken.value ?: return
        _uiState.value = _uiState.value.copy(swappingMealId = meal.id)

        viewModelScope.launch {
            runCatching {
                api.generateAlternativeMeal(
                    "Bearer $token",
                    GenerateAlternativeMealRequest(meal, profile)
                ).body()!!
            }.onSuccess { newMeal ->
                val currentDiet = appRepository.diet.value ?: return@onSuccess
                val updated = currentDiet.copy(meals = currentDiet.meals.map { if (it.id == meal.id) newMeal else it })
                appRepository.setDiet(updated)
                _uiState.value = _uiState.value.copy(swappingMealId = null)
            }.onFailure {
                _uiState.value = _uiState.value.copy(swappingMealId = null)
            }
        }
    }

    fun addWaterGlass() { if (_waterGlasses.value < 16) _waterGlasses.value++ }
    fun removeWaterGlass() { if (_waterGlasses.value > 0) _waterGlasses.value-- }

    fun toggleMacroQuickMode() {
        _uiState.update { it.copy(isMacroQuickMode = !it.isMacroQuickMode) }
    }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            // Toasts and sync logic removed or moved to repository
            _isRefreshing.value = false
        }
    }
}
