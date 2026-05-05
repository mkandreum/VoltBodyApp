package com.voltbody.app.data.repository

import com.voltbody.app.data.local.dao.*
import com.voltbody.app.data.local.entities.*
import com.voltbody.app.data.preferences.AppPreferences
import com.voltbody.app.data.remote.ApiService
import com.voltbody.app.data.remote.dto.*
import com.voltbody.app.domain.model.*
import com.voltbody.app.domain.usecase.checkNewAchievements
import com.voltbody.app.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppRepository @Inject constructor(
    private val api: ApiService,
    private val prefs: AppPreferences,
    private val workoutLogDao: WorkoutLogDao,
    private val weightLogDao: WeightLogDao,
    private val recoveryLogDao: RecoveryLogDao,
    private val progressPhotoDao: ProgressPhotoDao,
    @ApplicationScope private val scope: CoroutineScope
) {

    // ── Auth state ────────────────────────────────────────────────────────────

    private val _authToken = MutableStateFlow<String?>(null)
    val authToken: StateFlow<String?> = _authToken.asStateFlow()

    private val _user = MutableStateFlow<User?>(null)
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _isAuthenticated = MutableStateFlow(false)
    val isAuthenticated: StateFlow<Boolean> = _isAuthenticated.asStateFlow()

    private val _isOnboarded = MutableStateFlow(false)
    val isOnboarded: StateFlow<Boolean> = _isOnboarded.asStateFlow()

    private val _hasHydrated = MutableStateFlow(false)
    val hasHydrated: StateFlow<Boolean> = _hasHydrated.asStateFlow()

    // ── Profile data ──────────────────────────────────────────────────────────

    private val _profile = MutableStateFlow<UserProfile?>(null)
    val profile: StateFlow<UserProfile?> = _profile.asStateFlow()

    private val _routine = MutableStateFlow<List<WorkoutDay>>(emptyList())
    val routine: StateFlow<List<WorkoutDay>> = _routine.asStateFlow()

    private val _diet = MutableStateFlow<DietPlan?>(null)
    val diet: StateFlow<DietPlan?> = _diet.asStateFlow()

    private val _insights = MutableStateFlow<Insights?>(null)
    val insights: StateFlow<Insights?> = _insights.asStateFlow()

    private val _theme = MutableStateFlow(AppTheme.VERDE_NEGRO)
    val theme: StateFlow<AppTheme> = _theme.asStateFlow()

    private val _motivationPhrase = MutableStateFlow("Cada serie te acerca más a tu meta. ¡No pares!")
    val motivationPhrase: StateFlow<String> = _motivationPhrase.asStateFlow()

    private val _motivationPhoto = MutableStateFlow<String?>(null)
    val motivationPhoto: StateFlow<String?> = _motivationPhoto.asStateFlow()

    private val _profilePhoto = MutableStateFlow<String?>(null)
    val profilePhoto: StateFlow<String?> = _profilePhoto.asStateFlow()

    private val _notificationsEnabled = MutableStateFlow(false)
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    // ── Logs (from Room DB) ───────────────────────────────────────────────────

    val workoutLogs: StateFlow<List<WorkoutLog>> =
        workoutLogDao.getAllFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val weightLogs: StateFlow<List<WeightLog>> =
        weightLogDao.getAllFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val recoveryLogs: StateFlow<List<RecoveryLog>> =
        recoveryLogDao.getAllFlow()
            .map { entities -> entities.map { it.toDomain() } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    val progressPhotos: StateFlow<List<ProgressPhoto>> =
        progressPhotoDao.getAllFlow()
            .map { entities -> entities.map { ProgressPhoto(it.date, it.url) } }
            .stateIn(scope, SharingStarted.Eagerly, emptyList())

    // ── UI state ──────────────────────────────────────────────────────────────

    private val _achievements = MutableStateFlow<List<Achievement>>(emptyList())
    val achievements: StateFlow<List<Achievement>> = _achievements.asStateFlow()

    private val _mealEatenRecord = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val mealEatenRecord: StateFlow<Map<String, List<String>>> = _mealEatenRecord.asStateFlow()

    private val _weeklyGoals = MutableStateFlow<List<WeeklyGoal>>(defaultWeeklyGoals())
    val weeklyGoals: StateFlow<List<WeeklyGoal>> = _weeklyGoals.asStateFlow()

    init {
        scope.launch {
            rehydrate()
        }
    }

    private suspend fun rehydrate() {
        prefs.authToken.first()?.let { token ->
            _authToken.value = token
            _isAuthenticated.value = true
        }
        prefs.getUser().first()?.let { _user.value = it }
        prefs.isOnboarded.first().let { _isOnboarded.value = it }
        prefs.theme.first().let { _theme.value = AppTheme.fromKey(it) }
        prefs.getProfile().first()?.let { _profile.value = it }
        prefs.getRoutine().first().let { _routine.value = it }
        prefs.getDiet().first()?.let { _diet.value = it }
        prefs.getInsights().first()?.let { _insights.value = it }
        prefs.motivationPhrase.first().let { _motivationPhrase.value = it }
        prefs.motivationPhoto.first()?.let { _motivationPhoto.value = it }
        prefs.profilePhoto.first()?.let { _profilePhoto.value = it }
        prefs.notificationsEnabled.first().let { _notificationsEnabled.value = it }
        prefs.getAchievements().first().let { _achievements.value = it }
        prefs.getMealEatenRecord().first().let { _mealEatenRecord.value = it }
        _hasHydrated.value = true
    }

    // ── Actions (Auth, Profile, Logs) ──────────────────────────────────────────

    fun setAuthToken(token: String?) {
        _authToken.value = token
        _isAuthenticated.value = token != null
        scope.launch { prefs.saveAuthToken(token) }
    }

    fun setUser(user: User?) {
        _user.value = user
        scope.launch { prefs.saveUser(user) }
    }

    fun completeOnboarding() {
        _isOnboarded.value = true
        scope.launch { prefs.setOnboarded(true) }
    }

    fun logout() {
        _authToken.value = null
        _isAuthenticated.value = false
        _isOnboarded.value = false
        _user.value = null
        _profile.value = null
        _routine.value = emptyList()
        _diet.value = null
        _insights.value = null
        _motivationPhrase.value = "Cada serie te acerca más a tu meta. ¡No pares!"
        _motivationPhoto.value = null
        _profilePhoto.value = null
        _achievements.value = emptyList()
        _mealEatenRecord.value = emptyMap()
        _weeklyGoals.value = defaultWeeklyGoals()
        scope.launch {
            prefs.clearAll()
            workoutLogDao.deleteAll()
            weightLogDao.deleteAll()
            recoveryLogDao.deleteAll()
            progressPhotoDao.deleteAll()
        }
    }

    fun setProfile(profile: UserProfile) {
        _profile.value = profile
        scope.launch { prefs.saveProfile(profile) }
    }

    fun setRoutine(routine: List<WorkoutDay>) {
        _routine.value = routine
        scope.launch { prefs.saveRoutine(routine) }
        scope.launch { enrichRoutineIfNeeded(routine) }
    }

    private suspend fun enrichRoutineIfNeeded(routine: List<WorkoutDay>) {
        val token = _authToken.value ?: return
        runCatching {
            val response = api.enrichRoutine("Bearer $token", EnrichRoutineRequest(routine))
            if (response.isSuccessful) {
                response.body()?.routine?.let { enriched ->
                    _routine.value = enriched
                    prefs.saveRoutine(enriched)
                }
            }
        }
    }

    fun setDiet(diet: DietPlan) {
        _diet.value = diet
        scope.launch { prefs.saveDiet(diet) }
    }

    fun setInsights(insights: Insights) {
        _insights.value = insights
        scope.launch { prefs.saveInsights(insights) }
    }

    fun setTheme(theme: AppTheme) {
        _theme.value = theme
        scope.launch { prefs.setTheme(theme.key) }
    }

    fun setMotivationPhrase(phrase: String) {
        _motivationPhrase.value = phrase
        scope.launch { prefs.setMotivationPhrase(phrase) }
    }

    fun setMotivationPhoto(url: String?) {
        _motivationPhoto.value = url
        scope.launch { prefs.setMotivationPhoto(url) }
    }

    fun setProfilePhoto(url: String?) {
        _profilePhoto.value = url
        scope.launch { prefs.setProfilePhoto(url) }
    }

    fun setNotificationsEnabled(v: Boolean) {
        _notificationsEnabled.value = v
        scope.launch { prefs.setNotificationsEnabled(v) }
    }

    fun addWorkoutLog(log: WorkoutLog) {
        scope.launch {
            workoutLogDao.insert(log.toEntity())
            val currentLogs = workoutLogs.value + log
            val newAchievements = checkNewAchievements(
                currentLogs, _achievements.value.map { it.id }, log.exerciseId, log.weight
            )
            if (newAchievements.isNotEmpty()) {
                val now = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                val unlocked = newAchievements.map { it.copy(unlockedAt = now) }
                _achievements.value = _achievements.value + unlocked
                prefs.saveAchievements(_achievements.value)
            }
            _authToken.value?.let { token ->
                runCatching {
                    api.syncWorkoutLogs("Bearer $token", SyncLogsRequest(listOf(log.toDto())))
                }
            }
        }
    }

    fun addWeightLog(log: WeightLog) {
        scope.launch {
            weightLogDao.insert(log.toEntity())
            _authToken.value?.let { token ->
                runCatching {
                    api.syncWeightLogs("Bearer $token", SyncWeightLogsRequest(listOf(WeightLogDto(log.date, log.weight))))
                }
            }
        }
    }

    fun addProgressPhoto(photo: ProgressPhoto) {
        scope.launch {
            progressPhotoDao.insert(ProgressPhotoEntity(date = photo.date, url = photo.url))
            _authToken.value?.let { token ->
                runCatching {
                    api.addProgressPhoto("Bearer $token", ProgressPhotoDto(photo.date, photo.url))
                }
            }
        }
    }

    fun toggleMealEaten(mealId: String, date: String) {
        val current = _mealEatenRecord.value.toMutableMap()
        val eaten = current[date]?.toMutableList() ?: mutableListOf()
        if (eaten.contains(mealId)) eaten.remove(mealId) else eaten.add(mealId)
        current[date] = eaten
        _mealEatenRecord.value = current
        scope.launch { prefs.saveMealEatenRecord(current) }
    }

    fun toggleWeeklyGoal(id: String) {
        _weeklyGoals.value = _weeklyGoals.value.map {
            if (it.id == id) it.copy(done = !it.done) else it
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun WorkoutLog.toEntity() = WorkoutLogEntity(
        date = date, exerciseId = exerciseId, weight = weight, reps = reps,
        duration = duration, rpe = rpe, rir = rir
    )

    private fun WorkoutLog.toDto() = WorkoutLogDto(
        date = date, exerciseId = exerciseId, weight = weight, reps = reps,
        duration = duration, rpe = rpe, rir = rir
    )

    private fun WorkoutLogEntity.toDomain() = WorkoutLog(
        date = date, exerciseId = exerciseId, weight = weight, reps = reps,
        duration = duration, rpe = rpe, rir = rir
    )

    private fun WeightLog.toEntity() = WeightLogEntity(date = date, weight = weight)

    private fun WeightLogEntity.toDomain() = WeightLog(date = date, weight = weight)

    private fun RecoveryLogEntity.toDomain() = RecoveryLog(
        date = date, sleepHours = sleepHours, hrv = hrv, score = score
    )

    private fun defaultWeeklyGoals() = listOf(
        WeeklyGoal("sleep", "Dormir 8h cada noche"),
        WeeklyGoal("water", "Beber 2L de agua al día"),
        WeeklyGoal("steps", "10.000 pasos diarios"),
        WeeklyGoal("protein", "Llegar al objetivo proteico"),
        WeeklyGoal("workout", "Completar todos los entrenos")
    )
}
