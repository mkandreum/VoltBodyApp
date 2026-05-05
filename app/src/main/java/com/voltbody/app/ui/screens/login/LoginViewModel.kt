package com.voltbody.app.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voltbody.app.data.preferences.AppPreferences
import com.voltbody.app.data.remote.ApiService
import com.voltbody.app.data.remote.dto.LoginRequest
import com.voltbody.app.data.remote.dto.RegisterRequest
import com.voltbody.app.domain.model.AppTheme
import com.voltbody.app.domain.model.User
import com.voltbody.app.data.repository.AppRepository
import com.voltbody.app.util.NetworkErrorMapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AuthResult(val needsOnboarding: Boolean)

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val authResult: AuthResult? = null
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val api: ApiService,
    private val prefs: AppPreferences,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun login(request: LoginRequest) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            runCatching {
                val response = api.login(request)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    throw Exception(errorBody)
                }
                response.body()!!
            }.onSuccess { dto ->
                appRepository.setAuthToken(dto.token)
                appRepository.setUser(User(dto.user.id, dto.user.email, dto.user.name))

                // Restore saved plan if available
                dto.profile?.let { appRepository.setProfile(it) }
                dto.routine?.let { appRepository.setRoutine(it) }
                dto.diet?.let { appRepository.setDiet(it) }
                dto.insights?.let { appRepository.setInsights(it) }
                dto.profilePhoto?.let { appRepository.setProfilePhoto(it) }
                dto.motivationPhrase?.let { appRepository.setMotivationPhrase(it) }
                dto.motivationPhoto?.let { appRepository.setMotivationPhoto(it) }

                val hasProfile = dto.profile != null
                val hasRoutine = !dto.routine.isNullOrEmpty()

                if (hasProfile && hasRoutine) {
                    appRepository.completeOnboarding()
                    _uiState.value = LoginUiState(authResult = AuthResult(needsOnboarding = false))
                } else {
                    _uiState.value = LoginUiState(authResult = AuthResult(needsOnboarding = true))
                }
            }.onFailure { e ->
                _uiState.value = LoginUiState(error = NetworkErrorMapper.parse(e))
            }
        }
    }

    fun register(request: RegisterRequest) {
        viewModelScope.launch {
            _uiState.value = LoginUiState(isLoading = true)
            runCatching {
                val response = api.register(request)
                if (!response.isSuccessful) {
                    val errorBody = response.errorBody()?.string() ?: "Error desconocido"
                    throw Exception(errorBody)
                }
                response.body()!!
            }.onSuccess { dto ->
                appRepository.setAuthToken(dto.token)
                appRepository.setUser(User(dto.user.id, dto.user.email, dto.user.name))
                _uiState.value = LoginUiState(authResult = AuthResult(needsOnboarding = true))
            }.onFailure { e ->
                _uiState.value = LoginUiState(error = NetworkErrorMapper.parse(e))
            }
        }
    }
}
