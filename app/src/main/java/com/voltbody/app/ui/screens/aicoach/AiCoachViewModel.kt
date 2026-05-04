package com.voltbody.app.ui.screens.aicoach

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voltbody.app.data.remote.ApiService
import com.voltbody.app.data.remote.dto.AiChatContext
import com.voltbody.app.data.remote.dto.AiChatMessage
import com.voltbody.app.data.remote.dto.AiChatRequest
import com.voltbody.app.domain.model.WorkoutLog
import com.voltbody.app.ui.viewmodel.AppViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

data class ChatMessage(
    val id: String,
    val role: String,   // "user" | "assistant"
    val content: String,
    val isThinking: Boolean = false
)

data class AiCoachUiState(
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)

// Quick-action suggestions shown above the input
val QUICK_ACTIONS = listOf(
    "¿Cómo va mi progreso?",
    "¿Qué ejercicio debo hacer hoy?",
    "Consejos de recuperación",
    "Ajusta mi rutina",
    "¿Cuántas calorías necesito?",
    "Técnica de sentadilla"
)

@HiltViewModel
class AiCoachViewModel @Inject constructor(
    private val api: ApiService,
    private val appViewModel: AppViewModel
) : ViewModel() {

    private val _uiState = MutableStateFlow(AiCoachUiState())
    val uiState: StateFlow<AiCoachUiState> = _uiState.asStateFlow()

    init {
        // Greeting message on first open
        val profile = appViewModel.profile.value
        val name = profile?.name?.takeIf { it.isNotBlank() } ?: "atleta"
        _uiState.value = _uiState.value.copy(
            messages = listOf(
                ChatMessage(
                    id = "greeting",
                    role = "assistant",
                    content = "¡Hola, $name! ⚡ Soy tu coach IA de VoltBody. " +
                        "Tengo acceso a tu rutina, historial de entrenos y perfil. " +
                        "Pregúntame cualquier cosa sobre entrenamiento, nutrición o recuperación."
                )
            )
        )
    }

    fun onInputChange(text: String) {
        _uiState.value = _uiState.value.copy(inputText = text, error = null)
    }

    fun sendMessage(text: String = _uiState.value.inputText.trim()) {
        if (text.isBlank() || _uiState.value.isLoading) return

        val userMsg = ChatMessage(
            id = System.currentTimeMillis().toString(),
            role = "user",
            content = text
        )
        val thinkingMsg = ChatMessage(
            id = "thinking",
            role = "assistant",
            content = "",
            isThinking = true
        )

        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages + userMsg + thinkingMsg,
            inputText = "",
            isLoading = true,
            error = null
        )

        viewModelScope.launch {
            val token = appViewModel.authToken.value
            if (token == null) {
                replaceThinkingWithError("No autenticado")
                return@launch
            }

            // Build context: profile + today workout + last 7 days of logs
            val profile = appViewModel.profile.value
            val routine = appViewModel.routine.value
            val allLogs = appViewModel.workoutLogs.value
            val today = LocalDate.now()
            val sevenDaysAgo = today.minusDays(7).format(DateTimeFormatter.ISO_LOCAL_DATE)
            val recentLogs: List<WorkoutLog> = allLogs.filter { it.date.take(10) >= sevenDaysAgo }

            // Map today's workout from routine
            val dayOfWeek = today.dayOfWeek.value - 1  // 0=Mon
            val todayWorkout = routine.getOrNull(dayOfWeek)

            // Build history for API (exclude thinking bubble, max 20 turns)
            val history = _uiState.value.messages
                .filter { !it.isThinking }
                .takeLast(20)
                .map { AiChatMessage(role = it.role, content = it.content) }

            runCatching {
                api.aiChat(
                    "Bearer $token",
                    AiChatRequest(
                        messages = history,
                        context = AiChatContext(
                            profile = profile,
                            todayWorkout = todayWorkout,
                            recentLogs = recentLogs
                        )
                    )
                ).body()!!
            }.onSuccess { response ->
                val assistantMsg = ChatMessage(
                    id = System.currentTimeMillis().toString() + "_ai",
                    role = "assistant",
                    content = response.reply
                )
                val updated = _uiState.value.messages
                    .filter { !it.isThinking } + assistantMsg
                _uiState.value = _uiState.value.copy(
                    messages = updated,
                    isLoading = false
                )
            }.onFailure { e ->
                replaceThinkingWithError(e.message ?: "Error desconocido")
            }
        }
    }

    fun clearChat() {
        _uiState.value = AiCoachUiState()
        // Re-add greeting
        val name = appViewModel.profile.value?.name?.takeIf { it.isNotBlank() } ?: "atleta"
        _uiState.value = _uiState.value.copy(
            messages = listOf(
                ChatMessage(
                    id = "greeting",
                    role = "assistant",
                    content = "¡Hola de nuevo, $name! ⚡ ¿En qué puedo ayudarte?"
                )
            )
        )
    }

    private fun replaceThinkingWithError(msg: String) {
        val errorMsg = ChatMessage(
            id = "error_${System.currentTimeMillis()}",
            role = "assistant",
            content = "⚠️ $msg. Por favor, inténtalo de nuevo."
        )
        _uiState.value = _uiState.value.copy(
            messages = _uiState.value.messages.filter { !it.isThinking } + errorMsg,
            isLoading = false,
            error = msg
        )
    }
}
