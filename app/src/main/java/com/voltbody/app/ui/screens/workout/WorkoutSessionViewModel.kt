package com.voltbody.app.ui.screens.workout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.voltbody.app.data.remote.ApiService
import com.voltbody.app.data.remote.dto.*
import com.voltbody.app.domain.model.*
import com.voltbody.app.data.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.time.Instant
import javax.inject.Inject

data class SetLog(
    val exerciseId: String,
    val exerciseName: String,
    val setNumber: Int,
    val reps: Int,
    val weightKg: Float,
    val rpe: Int? = null,
    val note: String? = null,
    val completedAt: Long = System.currentTimeMillis()
)

data class WorkoutSessionState(
    val workoutId: String = "",
    val workoutName: String = "",
    val exercises: List<WorkoutExercise> = emptyList(),
    val currentExerciseIndex: Int = 0,
    val currentSetIndex: Int = 0,
    val setLogs: List<SetLog> = emptyList(),
    val elapsedSeconds: Int = 0,
    val restSeconds: Int = 0,
    val isResting: Boolean = false,
    val isPaused: Boolean = false,
    val isFinished: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedReps: Int = 10,
    val selectedWeight: Float = 0f,
    val selectedRpe: Int? = null,
    val currentNote: String = "",
    val availableReps: List<Int> = (1..20).toList(),
)

data class WorkoutExercise(
    val id: String,
    val name: String,
    val sets: Int,
    val targetReps: Int,
    val restSeconds: Int = 60,
    val imageUrl: String? = null
)

@HiltViewModel
class WorkoutSessionViewModel @Inject constructor(
    private val api: ApiService,
    private val appRepository: AppRepository
) : ViewModel() {

    private val _state = MutableStateFlow(WorkoutSessionState())
    val state: StateFlow<WorkoutSessionState> = _state.asStateFlow()

    private var elapsedJob: Job? = null
    private var restJob: Job? = null

    fun startSession(workoutId: String, workoutName: String, exercises: List<WorkoutExercise>) {
        _state.update {
            it.copy(
                workoutId = workoutId,
                workoutName = workoutName,
                exercises = exercises,
                currentExerciseIndex = 0,
                currentSetIndex = 0,
                elapsedSeconds = 0,
                selectedReps = exercises.firstOrNull()?.targetReps ?: 10,
                selectedRpe = null,
                currentNote = ""
            )
        }
        startElapsedTimer()
    }

    private fun startElapsedTimer() {
        elapsedJob?.cancel()
        elapsedJob = viewModelScope.launch {
            while (true) {
                delay(1000)
                if (!_state.value.isPaused && !_state.value.isResting) {
                    _state.update { it.copy(elapsedSeconds = it.elapsedSeconds + 1) }
                }
            }
        }
    }

    fun logSet(reps: Int, weightKg: Float) {
        val s = _state.value
        val exercise = s.exercises.getOrNull(s.currentExerciseIndex) ?: return
        val log = SetLog(
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            setNumber = s.currentSetIndex + 1,
            reps = reps,
            weightKg = weightKg,
            rpe = s.selectedRpe,
            note = s.currentNote.trim().takeIf { it.isNotEmpty() }
        )
        val newLogs = s.setLogs + log
        val nextSet = s.currentSetIndex + 1
        if (nextSet >= exercise.sets) {
            val nextExercise = s.currentExerciseIndex + 1
            if (nextExercise >= s.exercises.size) {
                _state.update { it.copy(setLogs = newLogs, isFinished = true) }
                finishSession(newLogs)
            } else {
                _state.update {
                    it.copy(
                        setLogs = newLogs,
                        currentExerciseIndex = nextExercise,
                        currentSetIndex = 0,
                        selectedReps = s.exercises[nextExercise].targetReps,
                        selectedRpe = null,
                        currentNote = ""
                    )
                }
                startRest(exercise.restSeconds)
            }
        } else {
            _state.update {
                it.copy(
                    setLogs = newLogs,
                    currentSetIndex = nextSet,
                    selectedRpe = null,
                    currentNote = ""
                )
            }
            startRest(exercise.restSeconds)
        }
    }

    fun startRest(seconds: Int) {
        restJob?.cancel()
        _state.update { it.copy(isResting = true, restSeconds = seconds) }
        restJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                if (!_state.value.isPaused) {
                    delay(1000)
                    remaining--
                    _state.update { it.copy(restSeconds = remaining) }
                } else {
                    delay(200)
                }
            }
            _state.update { it.copy(isResting = false, restSeconds = 0) }
        }
    }

    fun skipRest() {
        restJob?.cancel()
        _state.update { it.copy(isResting = false, restSeconds = 0) }
    }

    fun togglePause() {
        _state.update { it.copy(isPaused = !it.isPaused) }
    }

    fun setReps(reps: Int) = _state.update { it.copy(selectedReps = reps) }
    fun setWeight(weight: Float) = _state.update { it.copy(selectedWeight = weight) }
    fun setRpe(rpe: Int?) = _state.update { it.copy(selectedRpe = rpe) }
    fun setNote(note: String) = _state.update { it.copy(currentNote = note) }

    private fun finishSession(logs: List<SetLog>) {
        val token = appRepository.authToken.value ?: return
        viewModelScope.launch {
            elapsedJob?.cancel()
            restJob?.cancel()
            try {
                val dtoList = logs.map {
                    WorkoutLogDto(
                        date = Instant.ofEpochMilli(it.completedAt).toString(),
                        exerciseId = it.exerciseId,
                        weight = it.weightKg,
                        reps = it.reps,
                        rpe = it.rpe
                    )
                }
                api.logWorkoutSession(
                    token = "Bearer $token",
                    request = LogSessionRequest(
                        workoutId = _state.value.workoutId,
                        durationSeconds = _state.value.elapsedSeconds,
                        logs = dtoList
                    )
                )
            } catch (_: Exception) { /* best-effort */ }
        }
    }

    override fun onCleared() {
        elapsedJob?.cancel()
        restJob?.cancel()
        super.onCleared()
    }
}
