package com.voltbody.app.ui.screens.diet

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voltbody.app.data.remote.ApiService
import com.voltbody.app.data.remote.dto.GenerateAlternativeMealRequest
import com.voltbody.app.domain.model.*
import com.voltbody.app.ui.viewmodel.AppViewModel
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
    val isToday: Boolean = true
)

@HiltViewModel
class DietViewModel @Inject constructor(
    private val api: ApiService,
    private val appViewModel: AppViewModel
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
                appViewModel.diet,
                appViewModel.mealEatenRecord,
                appViewModel.profile,
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
        appViewModel.toggleMealEaten(mealId, _selectedDate.value.toString())
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
        val profile = appViewModel.profile.value ?: return
        val token = appViewModel.authToken.value ?: return
        _uiState.value = _uiState.value.copy(swappingMealId = meal.id)

        viewModelScope.launch {
            runCatching {
                api.generateAlternativeMeal(
                    "Bearer $token",
                    GenerateAlternativeMealRequest(meal, profile)
                ).body()!!
            }.onSuccess { newMeal ->
                appViewModel.swapMeal(meal.id, newMeal)
                _uiState.value = _uiState.value.copy(swappingMealId = null)
                appViewModel.showToast(AppToast(
                    id = "meal_swapped",
                    type = ToastType.SUCCESS,
                    title = "Comida cambiada",
                    message = "${meal.name} \u2192 ${newMeal.name}"
                ))
            }.onFailure {
                _uiState.value = _uiState.value.copy(swappingMealId = null)
                appViewModel.showToast(AppToast(id = "meal_swap_error", type = ToastType.ERROR, title = "Error al generar alternativa"))
            }
        }
    }

    fun addWaterGlass() { if (_waterGlasses.value < 16) _waterGlasses.value++ }
    fun removeWaterGlass() { if (_waterGlasses.value > 0) _waterGlasses.value-- }

    fun refresh() {
        viewModelScope.launch {
            _isRefreshing.value = true
            appViewModel.syncData()
            kotlinx.coroutines.delay(1000)
            _isRefreshing.value = false
        }
    }
}
