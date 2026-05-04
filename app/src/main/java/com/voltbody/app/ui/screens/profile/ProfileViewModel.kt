package com.voltbody.app.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voltbody.app.data.ApiService
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
    val successMessage: String? = null
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val api: ApiService
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init { loadProfile() }

    fun loadProfile() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            try {
                val p = api.getProfile()
                _state.update {
                    it.copy(
                        isLoading = false,
                        name = p.name,
                        email = p.email,
                        weightKg = p.weightKg,
                        heightCm = p.heightCm,
                        age = p.age,
                        goal = p.goal,
                        useMetric = p.useMetric
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
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
}
