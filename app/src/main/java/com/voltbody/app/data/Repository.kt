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

/** type: success|error|info. Con [actionLabel]/[onAction] se muestra un boton (p. ej. "Deshacer"). */
data class Toast(val type: String, val title: String, val message: String? = null, val actionLabel: String? = null, val onAction: (() -> Unit)? = null)

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

    /** Aviso con accion (normalmente "Deshacer") para cambios que el usuario puede revertir. */
    fun toastWithAction(type: String, title: String, message: String?, actionLabel: String, onAction: () -> Unit) {
        _toasts.tryEmit(Toast(type, title, message, actionLabel, onAction))
    }

    // ───────────────────────── Auth ─────────────────────────
    suspend fun login(email: String, password: String) = authenticate("/auth/login", email, password, null)
    suspend fun register(email: String, password: String, name: String) = authenticate("/auth/register", email, password, name)

    private suspend fun authenticate(path: String, email: String, password: String, name: String?) {
        val body = buildJsonObject { put("email", email.trim()); put("password", password); name?.let { put("name", it) } }
        val res = api.request("POST", path, body, auth = false) as? JsonObject
            ?: throw ApiException("El servidor devolvió una respuesta no válida al iniciar sesión.")
        val token = (res["token"] as? JsonPrimitive)?.takeIf { it !is JsonNull }?.contentOrNull
        val userEl = res["user"]?.takeIf { it !is JsonNull }
        // Nunca usar `!!`: un cuerpo inesperado lanzaría una NPE sin mensaje ("Error al conectar" genérico).
        if (token.isNullOrBlank() || userEl == null) {
            throw ApiException("El servidor respondió sin sesión válida. Revisa la URL o inténtalo de nuevo.")
        }
        val user = runCatching { AppJson.decodeFromJsonElement<AuthUser>(userEl) }
            .getOrElse { throw ApiException("Respuesta de usuario no reconocida por la app.") }
        update { it.copy(authToken = token, user = user) }
        applyServerProfile(res["profile"] as? JsonObject, user.name)
        refreshAll()
    }

    fun logout() = update { AppState(hydrated = true, serverUrl = it.serverUrl) }

    fun setServerUrl(url: String) = update { it.copy(serverUrl = url.trim()) }

    // ───────────────────── Perfil / plan ─────────────────────
    /** Decodifica un elemento a [T] tolerando tipos inesperados; devuelve null en lugar de lanzar. */
    private inline fun <reified T> decodeSoft(el: JsonElement?): T? =
        el?.let { runCatching { AppJson.decodeFromJsonElement<T>(it) }.getOrNull() }

    /**
     * Vuelca el perfil del servidor (UserProfile) al estado local. onboarded = tiene rutina y dieta (igual que la web).
     * Cada campo se lee de forma tolerante: números que llegan como texto se aceptan y un valor con tipo inesperado
     * cae a su valor por defecto en vez de perder todo el perfil o lanzar una excepción sin mensaje.
     */
    private fun applyServerProfile(p: JsonObject?, userName: String?) {
        if (p == null) return
        val prefs = (p["insights"] as? JsonObject)?.get("appPreferences") as? JsonObject
        fun el(key: String): JsonElement? = p[key]?.takeIf { it !is JsonNull } ?: prefs?.get(key)?.takeIf { it !is JsonNull }
        fun str(key: String): String? = runCatching { el(key)?.jsonPrimitive?.contentOrNull }.getOrNull()
        fun int(key: String): Int? = runCatching { el(key)?.jsonPrimitive?.intOrNull }.getOrNull()
        fun dbl(key: String): Double? = runCatching { el(key)?.jsonPrimitive?.doubleOrNull }.getOrNull()
        val profile = Profile(
            name = userName ?: str("name") ?: _state.value.profile.name,
            age = int("age"),
            weight = dbl("weight"),
            height = dbl("height"),
            gender = str("gender"),
            goal = str("goal"),
            goalDirection = str("goalDirection") ?: "Perder",
            goalTargetKg = dbl("goalTargetKg") ?: 5.0,
            goalTimelineMonths = int("goalTimelineMonths") ?: 3,
            currentState = str("currentState"),
            schedule = str("schedule"),
            workHours = str("workHours"),
            trainingDaysPerWeek = int("trainingDaysPerWeek") ?: 3,
            sessionMinutes = int("sessionMinutes") ?: 45,
            theme = str("theme"),
            motivationPhrase = str("motivationPhrase"),
            motivationPhoto = str("motivationPhoto"),
            mealTimes = decodeSoft<MealTimes>(p["mealTimes"] as? JsonObject) ?: MealTimes(),
            foodPreferences = decodeSoft<FoodPreferences>(el("foodPreferences") as? JsonObject) ?: FoodPreferences(),
            weeklySpecialSession = decodeSoft<WeeklySpecialSession>(el("weeklySpecialSession") as? JsonObject)
                ?: WeeklySpecialSession(false, "Zumba", "Sábado", 60),
            specialDish = decodeSoft<SpecialDish>(el("specialDish") as? JsonObject),
            avatarConfig = decodeSoft<AvatarConfig>(p["avatarConfig"] as? JsonObject) ?: AvatarConfig(0.5, 0.5),
            profilePhoto = str("profilePhoto"),
        )
        val routine = runCatching { (p["routine"] as? JsonArray)?.let { AppJson.decodeFromJsonElement<List<WorkoutDay>>(it) }.orEmpty() }.getOrDefault(emptyList())
        val diet = runCatching { (p["diet"] as? JsonObject)?.let { AppJson.decodeFromJsonElement<DietPlan>(it) } }.getOrNull()
        val insights = runCatching { (p["insights"] as? JsonObject)?.let { AppJson.decodeFromJsonElement<Insights>(it) } }.getOrNull()
        update {
            it.copy(
                profile = profile, routine = routine, diet = diet, insights = insights,
                isOnboarded = routine.isNotEmpty() && diet != null,
                profileLoaded = true, loadError = null,
                theme = str("theme") ?: it.theme,
                motivationPhrase = str("motivationPhrase") ?: it.motivationPhrase,
                notificationsEnabled = (p["notificationsEnabled"] as? JsonPrimitive)?.booleanOrNull ?: it.notificationsEnabled,
            )
        }
    }

    private fun mondayOf(d: LocalDate): LocalDate = d.minusDays((d.dayOfWeek.value - 1).toLong())

    /** Descarga perfil, logs, fotos, pesos y /data/all. Seguro de llamar al abrir la app o en pull-to-refresh. */
    suspend fun refreshAll() = coroutineScope {
        if (!_state.value.isAuthenticated) return@coroutineScope
        val profRes = async { runCatching { api.request("GET", "/profile").jsonObject } }
        val logs = async { runCatching { api.request("GET", "/workout/logs").jsonArray }.getOrNull() }
        val photos = async { runCatching { api.request("GET", "/workout/photos").jsonArray }.getOrNull() }
        val weights = async { runCatching { api.request("GET", "/workout/weight-logs").jsonArray }.getOrNull() }
        val data = async { runCatching { AppJson.decodeFromJsonElement<DataAll>(api.request("GET", "/data/all")) }.getOrNull() }
        val pr = profRes.await()
        pr.getOrNull()?.let { applyServerProfile(it, _state.value.user?.name) }
        if (pr.isFailure && !_state.value.profileLoaded) update { it.copy(loadError = pr.exceptionOrNull()?.message ?: "No se pudo cargar tu perfil") }
        logs.await()?.let { arr -> applyLogs(arr) }
        photos.await()?.let { arr -> applyPhotos(arr) }
        weights.await()?.let { arr -> applyWeights(arr) }
        data.await()?.let { d -> runCatching { applyData(d) } }
    }

    private fun applyLogs(arr: JsonArray) {
        val logs = arr.mapNotNull { runCatching { AppJson.decodeFromJsonElement<WorkoutLog>(it) }.getOrNull() }
        update { it.copy(logs = logs) }
    }

    /** Mapea fotos del servidor ignorando filas malformadas (antes usaba `!!` → NPE sin mensaje). */
    private fun applyPhotos(arr: JsonArray) {
        val photos = arr.mapNotNull { e ->
            runCatching {
                val o = e.jsonObject
                val date = o["date"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
                val url = o["url"]?.jsonPrimitive?.contentOrNull ?: return@runCatching null
                ProgressPhoto(date, url)
            }.getOrNull()
        }
        update { it.copy(progressPhotos = photos) }
    }

    /** Mapea pesos del servidor aceptando `weight` numérico o en texto e ignorando filas malformadas. */
    private fun applyWeights(arr: JsonArray) {
        val rows = arr.mapNotNull { e ->
            runCatching {
                val o = e.jsonObject
                val date = o["date"]?.jsonPrimitive?.contentOrNull?.take(10) ?: return@runCatching null
                val kg = o["weight"]?.jsonPrimitive?.doubleOrNull ?: return@runCatching null
                WeightLog(date, kg)
            }.getOrNull()
        }
        update { s -> s.copy(weightLogs = rows.sortedBy { it.date }) }
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

    suspend fun addWeight(date: String, kg: Double): Boolean {
        val w = WeightLog(date, kg)
        update { it.copy(weightLogs = (it.weightLogs.filter { x -> x.date != date } + w).sortedBy { x -> x.date },
            profile = if (date == LocalDate.now().toString()) it.profile.copy(weight = kg) else it.profile) }
        return write("peso", reload = { refreshAll() }) {
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

    private val _celebrations = MutableSharedFlow<Achievement>(extraBufferCapacity = 8)
    /** Logros recien desbloqueados: la app los celebra a pantalla completa (ver CelebrationHost). */
    val celebrations: SharedFlow<Achievement> = _celebrations.asSharedFlow()

    suspend fun unlockAchievement(a: Achievement) {
        val at = a.unlockedAt ?: Instant.now().toString()
        val isNew = _state.value.achievements.none { it.id == a.id }
        update { s -> if (s.achievements.any { it.id == a.id }) s else s.copy(achievements = s.achievements + a.copy(unlockedAt = at)) }
        if (isNew) _celebrations.tryEmit(a.copy(unlockedAt = at))
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

    /** [dayMeals] = resto de comidas de ese dia: el servidor las usa para no repetir ingredientes. */
    suspend fun alternativeMeal(old: Meal, dayMeals: List<Meal> = emptyList()): Meal = api.post("/ai/generate-alternative-meal", buildJsonObject {
        put("oldMeal", AppJson.encodeToJsonElement(old)); put("profile", AppJson.encodeToJsonElement(_state.value.profile))
        _state.value.diet?.let { d -> put("targets", buildJsonObject { put("dailyCalories", d.dailyCalories); put("macros", AppJson.encodeToJsonElement(d.macros)) }) }
        val others = dayMeals.filter { it.id != old.id }
        if (others.isNotEmpty()) put("dayMeals", JsonArray(others.map { m -> buildJsonObject { put("name", m.name); put("time", m.time); put("calories", m.calories) } }))
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
    suspend fun enrichRoutine(routine: List<WorkoutDay>, force: Boolean = false): List<WorkoutDay> = runCatching {
        val res = api.request("POST", "/ai/enrich-routine", buildJsonObject {
            put("routine", AppJson.encodeToJsonElement(routine)); if (force) put("force", true)
        }).jsonObject
        AppJson.decodeFromJsonElement<List<WorkoutDay>>(res["routine"]!!)
    }.getOrDefault(routine)

    private var gifsChecked = false

    /**
     * Re-empareja los GIF de la rutina y de la rutina personal con la tabla curada del servidor (una vez por arranque).
     * Si alguno cambia, guarda la version corregida en el servidor (la web tambien la vera bien).
     */
    suspend fun fixExerciseGifs() {
        if (gifsChecked || !_state.value.isAuthenticated) return
        gifsChecked = true
        val s = _state.value
        if (s.routine.isNotEmpty()) {
            val fixed = enrichRoutine(s.routine, force = true)
            val changed = fixed.flatMap { it.exercises }.map { it.gifUrl } != s.routine.flatMap { it.exercises }.map { it.gifUrl }
            if (changed && fixed.size == s.routine.size) saveProfile(withPlan = true) { it.copy(routine = fixed) }
        }
        if (s.customWorkout.isNotEmpty()) {
            val fixed = enrichRoutine(listOf(WorkoutDay("custom", "custom", s.customWorkout)), force = true).firstOrNull()?.exercises
            if (fixed != null && fixed.size == s.customWorkout.size && fixed.map { it.gifUrl } != s.customWorkout.map { it.gifUrl }) saveCustomWorkout(fixed)
        }
    }
}
