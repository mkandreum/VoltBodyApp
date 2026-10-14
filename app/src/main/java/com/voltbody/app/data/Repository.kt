package com.voltbody.app.data

import android.content.Context
import com.voltbody.app.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.time.Instant
import java.time.LocalDate

data class Toast(val type: String, val title: String, val message: String? = null) // type: success|error|info

/**
 * Estado en memoria alimentado por el servidor de la web (/api). En disco SOLO se guardan serverUrl,
 * authToken y user. Las escrituras son optimistas; si fallan se avisa con un toast y se recarga del servidor.
 */
class Repository(private val ctx: Context) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val file = File(ctx.filesDir, "session.json")
    private val _state = MutableStateFlow(AppState())
    val state: StateFlow<AppState> = _state.asStateFlow()
    private val _toasts = MutableSharedFlow<Toast>(extraBufferCapacity = 16)
    val toasts: SharedFlow<Toast> = _toasts.asSharedFlow()
    private var saveJob: Job? = null

    val api = Api(
        baseUrl = { serverUrl() },
        token = { _state.value.authToken },
        onUnauthorized = { logout(); toast("error", "Sesión caducada", "Vuelve a iniciar sesión") },
    )

    fun serverUrl(): String = _state.value.serverUrl.ifBlank { BuildConfig.DEFAULT_API_URL }

    init {
        val legacy = File(ctx.filesDir, "state.json")
        val loaded = runCatching { AppJson.decodeFromString<PersistedSession>(file.takeIf { it.exists() }?.readText() ?: legacy.readText()) }
            .getOrNull() ?: PersistedSession()
        runCatching { legacy.delete() } // el antiguo estado local con datos de usuario ya no se usa
        _state.value = AppState(hydrated = true, serverUrl = loaded.serverUrl, authToken = loaded.authToken, user = loaded.user)
    }

    /** Cambia el estado en memoria. A disco solo se persiste la sesión (url, token, user). */
    fun update(block: (AppState) -> AppState) {
        _state.update(block)
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(300)
            val st = _state.value
            runCatching { file.writeText(AppJson.encodeToString(PersistedSession(st.serverUrl, st.authToken, st.user))) }
        }
    }

    fun toast(type: String, title: String, message: String? = null) { _toasts.tryEmit(Toast(type, title, message)) }

    // ───────────────────────── Auth ─────────────────────────
    suspend fun login(email: String, password: String) = authenticate("/auth/login", email, password, null)
    suspend fun register(email: String, password: String, name: String) = authenticate("/auth/register", email, password, name)

    private suspend fun authenticate(path: String, email: String, password: String, name: String?) {
        val body = buildJsonObject { put("email", email.trim()); put("password", password); name?.let { put("name", it) } }
        val res = api.request("POST", path, body, auth = false).jsonObject
        val user = AppJson.decodeFromJsonElement<AuthUser>(res["user"]!!)
        update { it.copy(authToken = res["token"]!!.jsonPrimitive.content, user = user) }
        applyServerProfile(res["profile"] as? JsonObject, user.name)
        refreshAll()
    }

    fun logout() = update { AppState(hydrated = true, serverUrl = it.serverUrl) }

    fun setServerUrl(url: String) = update { it.copy(serverUrl = url.trim()) }

    // ───────────────────── Perfil / plan ─────────────────────
    /** Vuelca el perfil del servidor (UserProfile) al estado local. onboarded = tiene rutina y dieta (igual que la web). */
    private fun applyServerProfile(p: JsonObject?, userName: String?) {
        if (p == null) return
        val prefs = (p["insights"] as? JsonObject)?.get("appPreferences") as? JsonObject
        fun <T> pref(key: String, f: (JsonElement) -> T?): T? =
            (p[key]?.takeIf { it !is JsonNull }?.let(f)) ?: prefs?.get(key)?.takeIf { it !is JsonNull }?.let(f)
        val profile = AppJson.decodeFromJsonElement<Profile>(p).copy(
            name = userName ?: _state.value.profile.name,
            trainingDaysPerWeek = pref("trainingDaysPerWeek") { it.jsonPrimitive.intOrNull } ?: 3,
            sessionMinutes = pref("sessionMinutes") { it.jsonPrimitive.intOrNull } ?: 45,
            goalDirection = pref("goalDirection") { it.jsonPrimitive.contentOrNull } ?: "Perder",
            goalTargetKg = pref("goalTargetKg") { it.jsonPrimitive.doubleOrNull } ?: 5.0,
            goalTimelineMonths = pref("goalTimelineMonths") { it.jsonPrimitive.intOrNull } ?: 3,
            foodPreferences = pref("foodPreferences") { AppJson.decodeFromJsonElement<FoodPreferences>(it) } ?: FoodPreferences(),
            weeklySpecialSession = pref("weeklySpecialSession") { AppJson.decodeFromJsonElement<WeeklySpecialSession>(it) }
                ?: WeeklySpecialSession(false, "Zumba", "Sábado", 60),
            specialDish = pref("specialDish") { AppJson.decodeFromJsonElement<SpecialDish>(it) },
            mealTimes = (p["mealTimes"] as? JsonObject)?.let { AppJson.decodeFromJsonElement<MealTimes>(it) } ?: MealTimes(),
            avatarConfig = (p["avatarConfig"] as? JsonObject)?.let { AppJson.decodeFromJsonElement<AvatarConfig>(it) } ?: AvatarConfig(0.5, 0.5),
        )
        val routine = (p["routine"] as? JsonArray)?.let { AppJson.decodeFromJsonElement<List<WorkoutDay>>(it) }.orEmpty()
        val diet = (p["diet"] as? JsonObject)?.let { AppJson.decodeFromJsonElement<DietPlan>(it) }
        val insights = (p["insights"] as? JsonObject)?.let { AppJson.decodeFromJsonElement<Insights>(it) }
        update {
            it.copy(
                profile = profile, routine = routine, diet = diet, insights = insights,
                isOnboarded = routine.isNotEmpty() && diet != null,
                theme = pref("theme") { e -> e.jsonPrimitive.contentOrNull } ?: it.theme,
                motivationPhrase = pref("motivationPhrase") { e -> e.jsonPrimitive.contentOrNull } ?: it.motivationPhrase,
                notificationsEnabled = (p["notificationsEnabled"] as? JsonPrimitive)?.booleanOrNull ?: it.notificationsEnabled,
            )
        }
    }

    private fun mondayOf(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value - 1).toLong())

    /** Descarga perfil, logs, fotos, pesos y /data/all. Seguro de llamar al abrir la app o en pull-to-refresh. */
    suspend fun refreshAll() = coroutineScope {
        if (!_state.value.isAuthenticated) return@coroutineScope
        val prof = async { runCatching { api.request("GET", "/profile").jsonObject }.getOrNull() }
        val logs = async { runCatching { api.request("GET", "/workout/logs").jsonArray }.getOrNull() }
        val photos = async { runCatching { api.request("GET", "/workout/photos").jsonArray }.getOrNull() }
        val weights = async { runCatching { api.request("GET", "/workout/weight-logs").jsonArray }.getOrNull() }
        val data = async { runCatching { AppJson.decodeFromJsonElement<DataAll>(api.request("GET", "/data/all")) }.getOrNull() }
        prof.await()?.let { applyServerProfile(it, _state.value.user?.name) }
        logs.await()?.let { arr -> applyLogs(arr) }
        photos.await()?.let { arr ->
            update { it.copy(progressPhotos = arr.map { e -> ProgressPhoto(e.jsonObject["date"]!!.jsonPrimitive.content, e.jsonObject["url"]!!.jsonPrimitive.content) }) }
        }
        weights.await()?.let { arr ->
            val server = arr.map { e -> WeightLog(e.jsonObject["date"]!!.jsonPrimitive.content.take(10), e.jsonObject["weight"]!!.jsonPrimitive.double) }
            update { s -> s.copy(weightLogs = server.sortedBy { it.date }) }
        }
        data.await()?.let { applyData(it) }
    }

    private fun applyLogs(arr: JsonArray) {
        val logs = arr.mapNotNull { runCatching { AppJson.decodeFromJsonElement<WorkoutLog>(it) }.getOrNull() }
        update { it.copy(logs = logs) }
    }

    private suspend fun reloadLogs() {
        runCatching { api.request("GET", "/workout/logs").jsonArray }.getOrNull()?.let { applyLogs(it) }
    }

    private fun applyData(d: DataAll) {
        val monday = mondayOf(LocalDate.now()).toString()
        val goals = d.goals.filter { it.weekStart == monday }.map { WeeklyGoal(it.goalId, it.label, it.done) }
        val ach = d.achievements.map { r ->
            (com.voltbody.app.util.ALL_ACHIEVEMENTS.find { it.id == r.id } ?: Achievement(r.id)).copy(unlockedAt = r.unlockedAt)
        }
        update {
            it.copy(
                metrics = d.metrics.sortedBy { m -> m.date },
                mealEaten = d.mealLogs.groupBy({ m -> m.date }, { m -> m.mealId }),
                weeklyGoals = goals, weeklyGoalsWeek = monday,
                achievements = ach, customWorkout = d.customWorkout,
                recoveryLogs = d.recovery.sortedBy { r -> r.date },
            )
        }
    }

    private suspend fun reloadData() {
        runCatching { AppJson.decodeFromJsonElement<DataAll>(api.request("GET", "/data/all")) }.getOrNull()?.let { applyData(it) }
    }

    /** Ejecuta una escritura al servidor; si falla: toast de error y recarga de datos (revierte el cambio optimista). */
    private suspend fun write(what: String, reload: suspend () -> Unit = { reloadData() }, block: suspend () -> Unit): Boolean =
        try { block(); true } catch (e: CancellationException) { throw e } catch (e: Exception) {
            toast("error", "No se pudo guardar: $what", e.message)
            reload()
            false
        }

    /** Cuerpo PUT /profile. IMPORTANTE: el servidor reemplaza insights y reconstruye appPreferences, así que
     *  siempre se envían TODAS las preferencias junto a insights para no perderlas. */
    private fun profileBody(s: AppState, withPlan: Boolean = false, withPhotos: Boolean = false): JsonObject {
        val p = s.profile
        return buildJsonObject {
            p.age?.let { put("age", it) }; p.weight?.let { put("weight", it) }; p.height?.let { put("height", it) }
            p.gender?.takeIf { it.isNotBlank() }?.let { put("gender", it) }
            p.goal?.takeIf { it.isNotBlank() }?.let { put("goal", it) }
            p.currentState?.takeIf { it.isNotBlank() }?.let { put("currentState", it) }
            p.schedule?.takeIf { it.isNotBlank() }?.let { put("schedule", it) }
            p.workHours?.takeIf { it.isNotBlank() }?.let { put("workHours", it) }
            p.goalDirection?.let { put("goalDirection", it) }; p.goalTargetKg?.let { put("goalTargetKg", it) }
            p.goalTimelineMonths?.let { put("goalTimelineMonths", it) }
            p.trainingDaysPerWeek?.let { put("trainingDaysPerWeek", it) }; p.sessionMinutes?.let { put("sessionMinutes", it) }
            put("theme", s.theme); put("motivationPhrase", s.motivationPhrase.take(240))
            p.mealTimes?.let { put("mealTimes", AppJson.encodeToJsonElement(it)) }
            p.foodPreferences?.let { put("foodPreferences", AppJson.encodeToJsonElement(it)) }
            p.weeklySpecialSession?.let { put("weeklySpecialSession", AppJson.encodeToJsonElement(it)) }
            p.specialDish?.let { put("specialDish", AppJson.encodeToJsonElement(it)) }
            p.avatarConfig?.let { put("avatarConfig", AppJson.encodeToJsonElement(it)) }
            if (withPlan) {
                put("routine", AppJson.encodeToJsonElement(s.routine))
                s.diet?.let { put("diet", AppJson.encodeToJsonElement(it)) }
            }
            if (withPhotos) {
                p.profilePhoto?.let { put("profilePhoto", it) }
                p.motivationPhoto?.let { put("motivationPhoto", it) }
            }
            put("notificationsEnabled", s.notificationsEnabled)
            put("insights", AppJson.encodeToJsonElement(s.insights ?: Insights()))
        }
    }

    /** Guarda el perfil local y lo sube. */
    suspend fun saveProfile(withPlan: Boolean = false, withPhotos: Boolean = false, transform: (AppState) -> AppState = { it }) {
        update(transform)
        runCatching { api.request("PUT", "/profile", profileBody(_state.value, withPlan, withPhotos)) }
            .onFailure { toast("error", "No se pudo guardar en el servidor", it.message) }
    }

    // ───────────────────── Entreno / peso / fotos ─────────────────────
    /** Registra una serie (weight >= 0; 0 = peso corporal). Optimista; el id real se toma de la respuesta o recargando. */
    suspend fun addLog(log: WorkoutLog) {
        val local = log.copy(id = null, weight = log.weight.coerceAtLeast(0.0), reps = log.reps.coerceIn(1, 200))
        update { it.copy(logs = it.logs + local) }
        write("serie", reload = { reloadLogs() }) {
            val res = api.request("POST", "/workout/logs", buildJsonObject {
                put("date", local.date); put("exerciseId", local.exerciseId)
                put("weight", local.weight); put("reps", local.reps)
                local.duration?.let { put("duration", it.coerceIn(0, 86400)) }
                local.rpe?.let { put("rpe", it.coerceIn(1, 10)) }
                local.rir?.let { put("rir", it.coerceIn(0, 10)) }
            })
            val id = (res as? JsonObject)?.get("id")?.jsonPrimitive?.contentOrNull
            if (id != null) {
                update { s ->
                    val i = s.logs.indexOfLast { it.id == null && it.date == local.date && it.exerciseId == local.exerciseId }
                    if (i < 0) s else s.copy(logs = s.logs.toMutableList().also { l -> l[i] = l[i].copy(id = id) })
                }
            } else reloadLogs()
        }
    }

    /** Edita una serie existente (PUT /workout/logs/:id). */
    suspend fun updateLog(log: WorkoutLog): Boolean {
        val id = log.id ?: run { toast("error", "No se puede editar", "La serie aún no se ha sincronizado"); return false }
        update { s -> s.copy(logs = s.logs.map { if (it.id == id) log else it }) }
        return write("serie", reload = { reloadLogs() }) {
            api.request("PUT", "/workout/logs/$id", buildJsonObject {
                put("weight", log.weight.coerceAtLeast(0.0)); put("reps", log.reps.coerceIn(1, 200))
                log.duration?.let { put("duration", it.coerceIn(0, 86400)) }
                log.rpe?.let { put("rpe", it.coerceIn(1, 10)) }
                log.rir?.let { put("rir", it.coerceIn(0, 10)) }
            })
        }
    }

    suspend fun deleteLog(log: WorkoutLog): Boolean {
        val id = log.id ?: run { toast("error", "No se puede borrar", "La serie aún no se ha sincronizado"); return false }
        update { s -> s.copy(logs = s.logs.filter { it.id != id }) }
        return write("serie", reload = { reloadLogs() }) { api.request("DELETE", "/workout/logs/$id") }
    }

    suspend fun addWeight(date: String, kg: Double) {
        val w = WeightLog(date, kg)
        update { it.copy(weightLogs = (it.weightLogs.filter { x -> x.date != date } + w).sortedBy { x -> x.date },
            profile = if (date == LocalDate.now().toString()) it.profile.copy(weight = kg) else it.profile) }
        write("peso", reload = { refreshAll() }) {
            api.request("POST", "/workout/weight-logs", buildJsonObject { put("date", "${date}T12:00:00.000Z"); put("weight", kg) })
        }
    }

    suspend fun addPhoto(dateIso: String, dataUrl: String) {
        update { it.copy(progressPhotos = listOf(ProgressPhoto(dateIso, dataUrl)) + it.progressPhotos) }
        write("foto", reload = { refreshAll() }) {
            api.request("POST", "/workout/photos", buildJsonObject { put("date", dateIso); put("url", dataUrl) })
        }
    }

    // ───────────────────── /api/data/* ─────────────────────
    suspend fun setMealEaten(date: String, mealId: String, eaten: Boolean) {
        update { s ->
            val cur = s.mealEaten[date].orEmpty().filter { it != mealId }
            s.copy(mealEaten = s.mealEaten + (date to if (eaten) cur + mealId else cur))
        }
        write("comida") {
            api.request("PUT", "/data/meals", buildJsonObject { put("date", date); put("mealId", mealId); put("eaten", eaten) })
        }
    }

    /** Reemplaza los hábitos de la semana (weekStart = lunes YYYY-MM-DD). */
    suspend fun saveGoals(weekStart: String, goals: List<WeeklyGoal>) {
        update { it.copy(weeklyGoals = goals, weeklyGoalsWeek = weekStart) }
        write("hábitos") {
            api.request("PUT", "/data/goals", buildJsonObject {
                put("weekStart", weekStart)
                put("goals", JsonArray(goals.map { g -> buildJsonObject { put("id", g.id); put("label", g.label); put("done", g.done) } }))
            })
        }
    }

    suspend fun unlockAchievement(a: Achievement) {
        val at = a.unlockedAt ?: Instant.now().toString()
        update { s -> if (s.achievements.any { it.id == a.id }) s else s.copy(achievements = s.achievements + a.copy(unlockedAt = at)) }
        write("logro") { api.request("POST", "/data/achievements", buildJsonObject { put("id", a.id); put("unlockedAt", at) }) }
    }

    suspend fun saveCustomWorkout(exercises: List<Exercise>) {
        val list = exercises.take(100)
        update { it.copy(customWorkout = list) }
        write("rutina personal") {
            api.request("PUT", "/data/custom-workout", buildJsonObject { put("exercises", AppJson.encodeToJsonElement(list)) })
        }
    }

    suspend fun saveRecovery(log: RecoveryLog) {
        update { s -> s.copy(recoveryLogs = (s.recoveryLogs.filter { it.date != log.date } + log).sortedBy { it.date }) }
        write("recuperación") {
            api.request("PUT", "/data/recovery/${log.date}", buildJsonObject {
                put("sleepHours", log.sleepHours); log.hrv?.let { put("hrv", it) }; put("score", log.score)
            })
        }
    }

    /** Guarda métricas de un día; solo se envían (y fusionan) los campos no nulos de [m]. */
    suspend fun saveMetrics(date: String, m: DailyMetric) {
        update { s ->
            val old = s.metrics.firstOrNull { it.date == date } ?: DailyMetric(date)
            val merged = DailyMetric(
                date, m.steps ?: old.steps, m.distanceKm ?: old.distanceKm, m.activeKcal ?: old.activeKcal,
                m.totalKcal ?: old.totalKcal, m.sleepHours ?: old.sleepHours, m.hrv ?: old.hrv,
                m.restingHr ?: old.restingHr, m.avgHr ?: old.avgHr, m.waterMl ?: old.waterMl,
            )
            s.copy(metrics = (s.metrics.filter { it.date != date } + merged).sortedBy { it.date })
        }
        write("métricas") {
            api.request("PUT", "/data/metrics/$date", buildJsonObject {
                m.steps?.let { put("steps", it) }; m.distanceKm?.let { put("distanceKm", it) }
                m.activeKcal?.let { put("activeKcal", it) }; m.totalKcal?.let { put("totalKcal", it) }
                m.sleepHours?.let { put("sleepHours", it) }; m.hrv?.let { put("hrv", it) }
                m.restingHr?.let { put("restingHr", it) }; m.avgHr?.let { put("avgHr", it) }
                m.waterMl?.let { put("waterMl", it) }
            })
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        update { it.copy(notificationsEnabled = enabled) }
        write("notificaciones", reload = { refreshAll() }) {
            api.request("PUT", "/profile", profileBody(_state.value))
        }
    }

    // ───────────────────────── IA ─────────────────────────
    /** Cuerpo para generate-plan: perfil + name (la web envía el perfil completo). */
    suspend fun generatePlan(profile: Profile): Plan =
        api.post("/ai/generate-plan", AppJson.encodeToJsonElement(profile))

    suspend fun alternativeMeal(old: Meal): Meal = api.post("/ai/generate-alternative-meal", buildJsonObject {
        put("oldMeal", AppJson.encodeToJsonElement(old)); put("profile", AppJson.encodeToJsonElement(_state.value.profile))
    })

    suspend fun progressReport(): ProgressReport {
        val s = _state.value
        return api.post("/ai/generate-progress-report", buildJsonObject {
            put("profile", AppJson.encodeToJsonElement(s.profile))
            put("logs", AppJson.encodeToJsonElement(s.logs.takeLast(4000)))
            put("routine", AppJson.encodeToJsonElement(s.routine))
            s.diet?.let { put("diet", AppJson.encodeToJsonElement(it)) }
            put("progressPhotos", JsonArray(s.progressPhotos.take(6).map { buildJsonObject { put("date", it.date) } })) // sin base64 para no saturar
        })
    }

    /** Rellena gifUrl vacíos desde la base de ejercicios del servidor. Degrada en silencio. */
    suspend fun enrichRoutine(routine: List<WorkoutDay>): List<WorkoutDay> = runCatching {
        val res = api.request("POST", "/ai/enrich-routine", buildJsonObject { put("routine", AppJson.encodeToJsonElement(routine)) }).jsonObject
        AppJson.decodeFromJsonElement<List<WorkoutDay>>(res["routine"]!!)
    }.getOrDefault(routine)
}
