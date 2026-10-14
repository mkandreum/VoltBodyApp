package com.voltbody.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.Female
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Male
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.*
import com.voltbody.app.ui.BigNumberPicker
import com.voltbody.app.ui.EaseOutExpo
import com.voltbody.app.ui.GradientText
import com.voltbody.app.ui.MeshBackground
import com.voltbody.app.ui.PrimaryButton
import com.voltbody.app.ui.RollingNumber
import com.voltbody.app.ui.SegmentedProgress
import com.voltbody.app.ui.VoltChip
import com.voltbody.app.ui.gradientBorder
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.screens.auth.GeneratingPlanScreen
import com.voltbody.app.ui.shake
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.ui.theme.shiftHue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

private enum class Pg { Name, Age, Gender, Weight, Height, Experience, Goal, Direction, Target, Months, Days, Minutes, Work, Meals, Foods, Special, Dish }

private class PageInfo(val pg: Pg, val kicker: String, val title: String, val sub: String)

private val PAGES = listOf(
    PageInfo(Pg.Name, "TUS DATOS", "¿Cómo te llamas?", "Para hablarte por tu nombre."),
    PageInfo(Pg.Age, "TUS DATOS", "¿Qué edad tienes?", "Desliza para elegir."),
    PageInfo(Pg.Gender, "TUS DATOS", "Tu género biológico", "Ajusta calorías y cargas."),
    PageInfo(Pg.Weight, "TU CUERPO", "¿Cuánto pesas?", "Tu punto de partida."),
    PageInfo(Pg.Height, "TU CUERPO", "¿Cuánto mides?", "En centímetros."),
    PageInfo(Pg.Experience, "TU CUERPO", "¿Cuánta experiencia tienes?", "Sé sincero, adaptamos el plan."),
    PageInfo(Pg.Goal, "TU OBJETIVO", "¿Qué quieres conseguir?", "Tu meta principal."),
    PageInfo(Pg.Direction, "TU OBJETIVO", "¿Subir o bajar de peso?", "Define la dirección."),
    PageInfo(Pg.Target, "TU OBJETIVO", "¿Cuántos kilos?", "Cantidad a modificar."),
    PageInfo(Pg.Months, "TU OBJETIVO", "¿En cuánto tiempo?", "Plazo estimado en meses."),
    PageInfo(Pg.Days, "ENTRENAMIENTO", "¿Cuántos días entrenas?", "A la semana."),
    PageInfo(Pg.Minutes, "ENTRENAMIENTO", "¿Cuánto dura cada sesión?", "Sesiones compactas y efectivas."),
    PageInfo(Pg.Work, "HORARIOS", "¿Cuál es tu jornada?", "Trabajo o estudio."),
    PageInfo(Pg.Meals, "HORARIOS", "¿A qué hora comes?", "Toca una comida para cambiar la hora."),
    PageInfo(Pg.Foods, "NUTRICIÓN", "¿Qué te gusta comer?", "Separa con comas."),
    PageInfo(Pg.Special, "NUTRICIÓN", "¿Una clase especial?", "Pádel, fútbol, zumba, natación…"),
    PageInfo(Pg.Dish, "NUTRICIÓN", "¿Un plato especial?", "Opcional. Lo incluimos en tu dieta."),
)

private data class Experience(val title: String, val desc: String, val emoji: String)
private val EXPERIENCES = listOf(
    Experience("Principiante", "Nunca he entrenado o tengo poca experiencia", "🌱"),
    Experience("Intermedio", "Entreno a veces y conozco los ejercicios básicos", "⚡"),
    Experience("Avanzado", "Entreno regularmente con técnica depurada", "🔥"),
)

private data class GoalOption(val title: String, val sub: String, val icon: ImageVector)
private val GOALS = listOf(
    GoalOption("Ganar masa muscular", "Hipertrofia y volumen", Icons.Filled.FitnessCenter),
    GoalOption("Perder grasa", "Definición y corte", Icons.Filled.LocalFireDepartment),
    GoalOption("Mantenimiento y salud", "Vitalidad y longevidad", Icons.Filled.MonitorHeart),
    GoalOption("Fuerza bruta", "Potencia y cargas máximas", Icons.Filled.Bolt),
)

private val DAYS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

/** Estado del formulario; cada campo sobrevive a rotaciones (rememberSaveable). */
private class Form(
    val name: MutableState<String>, val age: MutableState<String>, val gender: MutableState<String>,
    val weight: MutableState<String>, val height: MutableState<String>, val experience: MutableState<String>,
    val goal: MutableState<String>, val direction: MutableState<String>, val target: MutableState<String>, val months: MutableState<String>,
    val days: MutableState<Int>, val minutes: MutableState<Int>, val workHours: MutableState<String>,
    val breakfast: MutableState<String>, val brunch: MutableState<String>, val lunch: MutableState<String>,
    val snack: MutableState<String>, val dinner: MutableState<String>,
    val vegs: MutableState<String>, val carbs: MutableState<String>, val prots: MutableState<String>,
    val specialOn: MutableState<Boolean>, val specialActivity: MutableState<String>, val specialDay: MutableState<String>, val specialMin: MutableState<Int>,
    val dish: MutableState<String>, val dishKcal: MutableState<String>,
)

@Composable
private fun rememberForm(initial: Profile, userName: String?): Form {
    fun num(d: Double?, def: String) = d?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: def
    return Form(
        name = rememberSaveable { mutableStateOf(initial.name?.takeIf { it.isNotBlank() } ?: userName.orEmpty()) },
        age = rememberSaveable { mutableStateOf(initial.age?.toString() ?: "25") },
        gender = rememberSaveable { mutableStateOf(initial.gender?.takeIf { it == "Masculino" || it == "Femenino" } ?: "Masculino") },
        weight = rememberSaveable { mutableStateOf(num(initial.weight, "70")) },
        height = rememberSaveable { mutableStateOf(num(initial.height, "175")) },
        experience = rememberSaveable { mutableStateOf("${EXPERIENCES[0].title} (${EXPERIENCES[0].desc})") },
        goal = rememberSaveable { mutableStateOf("${GOALS[0].title} (${GOALS[0].sub})") },
        direction = rememberSaveable { mutableStateOf("Ganar") },
        target = rememberSaveable { mutableStateOf("5") },
        months = rememberSaveable { mutableStateOf("3") },
        days = rememberSaveable { mutableStateOf(3) },
        minutes = rememberSaveable { mutableStateOf(45) },
        workHours = rememberSaveable { mutableStateOf("09:00 - 17:00") },
        breakfast = rememberSaveable { mutableStateOf("08:00") },
        brunch = rememberSaveable { mutableStateOf("11:30") },
        lunch = rememberSaveable { mutableStateOf("14:00") },
        snack = rememberSaveable { mutableStateOf("18:00") },
        dinner = rememberSaveable { mutableStateOf("21:00") },
        vegs = rememberSaveable { mutableStateOf("brócoli, espinaca, zanahoria") },
        carbs = rememberSaveable { mutableStateOf("arroz, papa, avena") },
        prots = rememberSaveable { mutableStateOf("pollo, huevo, atún") },
        specialOn = rememberSaveable { mutableStateOf(false) },
        specialActivity = rememberSaveable { mutableStateOf("Zumba") },
        specialDay = rememberSaveable { mutableStateOf("Sábado") },
        specialMin = rememberSaveable { mutableStateOf(60) },
        dish = rememberSaveable { mutableStateOf("arroz, lentejas, tomate, queso feta") },
        dishKcal = rememberSaveable { mutableStateOf("390") },
    )
}

private fun parseNum(s: String): Double? = s.trim().replace(',', '.').toDoubleOrNull()
private fun splitList(s: String): List<String> = s.split(',').map { it.trim() }.filter { it.isNotEmpty() }
private fun fmtNum(v: Float): String = if (v % 1f == 0f) v.toInt().toString() else v.toString()

/** Onboarding estilo "stories": una pregunta por pantalla + generación del plan con IA. Validaciones idénticas a las del servidor. */
@Composable
fun OnboardingScreen(repo: Repository) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val f = rememberForm(repo.state.value.profile, repo.state.value.user?.name)
    var page by rememberSaveable { mutableIntStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    var errTick by remember { mutableIntStateOf(0) }
    val errors = remember { mutableStateMapOf<String, String>() }
    var timeVisible by remember { mutableStateOf(false) }
    var timeTarget by remember { mutableIntStateOf(0) }
    val meals = listOf(f.breakfast, f.brunch, f.lunch, f.snack, f.dinner)

    fun validate(pg: Pg): Boolean {
        errors.clear()
        when (pg) {
            Pg.Name -> {
                val n = f.name.value.trim()
                if (n.isEmpty()) errors["name"] = "Introduce tu nombre para continuar"
                else if (n.length > 80) errors["name"] = "El nombre no puede superar 80 caracteres"
            }
            Pg.Age -> {
                val a = f.age.value.trim().toIntOrNull()
                if (a == null || a < 12 || a > 100) errors["age"] = "La edad debe estar entre 12 y 100"
            }
            Pg.Weight -> {
                val w = parseNum(f.weight.value)
                if (w == null || w < 30 || w > 350) errors["weight"] = "Entre 30 y 350 kg"
            }
            Pg.Height -> {
                val h = parseNum(f.height.value)
                if (h == null || h < 120 || h > 250) errors["height"] = "Entre 120 y 250 cm"
            }
            Pg.Target -> {
                val t = parseNum(f.target.value)
                if (t == null || t < 1 || t > 50) errors["target"] = "Entre 1 y 50 kg"
            }
            Pg.Months -> {
                val m = f.months.value.trim().toIntOrNull()
                if (m == null || m < 1 || m > 24) errors["months"] = "Entre 1 y 24 meses"
            }
            Pg.Work -> if (f.workHours.value.length > 120) errors["workHours"] = "Máximo 120 caracteres"
            Pg.Dish -> {
                val k = f.dishKcal.value.trim()
                if (k.isNotEmpty() && (k.toIntOrNull()?.let { it < 50 || it > 3000 } != false)) errors["dishKcal"] = "Entre 50 y 3000 kcal"
            }
            else -> Unit
        }
        if (errors.isNotEmpty()) { errTick++; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
        return errors.isEmpty()
    }

    fun buildProfile(): Profile {
        val goal = f.goal.value
        val dish = f.dish.value.trim()
        return repo.state.value.profile.copy(
            name = f.name.value.trim(),
            age = f.age.value.trim().toIntOrNull() ?: 25,
            weight = parseNum(f.weight.value) ?: 70.0,
            height = parseNum(f.height.value) ?: 175.0,
            gender = f.gender.value,
            goal = goal,
            currentState = f.experience.value,
            schedule = "${f.days.value} días a la semana, ${f.minutes.value} min",
            workHours = f.workHours.value.trim(),
            trainingDaysPerWeek = f.days.value,
            sessionMinutes = f.minutes.value,
            goalDirection = f.direction.value,
            goalTargetKg = parseNum(f.target.value) ?: 5.0,
            goalTimelineMonths = f.months.value.trim().toIntOrNull() ?: 3,
            mealTimes = MealTimes(f.breakfast.value, f.brunch.value, f.lunch.value, f.snack.value, f.dinner.value),
            foodPreferences = FoodPreferences(splitList(f.vegs.value), splitList(f.carbs.value), splitList(f.prots.value)),
            weeklySpecialSession = WeeklySpecialSession(f.specialOn.value, f.specialActivity.value.trim(), f.specialDay.value, f.specialMin.value),
            specialDish = if (dish.isEmpty()) null else SpecialDish(dish, f.dishKcal.value.trim().toIntOrNull() ?: 0),
            avatarConfig = AvatarConfig(
                muscleMass = if (goal.contains("masa", ignoreCase = true)) 0.6 else 0.4,
                bodyFat = if (goal.contains("perder", ignoreCase = true)) 0.3 else 0.5,
            ),
        )
    }

    fun generate() {
        if (loading) return
        val profile = buildProfile()
        loading = true
        scope.launch {
            try {
                // Sin fotos base64 en la petición de IA.
                val plan = repo.generatePlan(profile.copy(profilePhoto = null, motivationPhoto = null))
                if (plan.routine.isEmpty()) throw IllegalStateException("El plan generado no contiene entrenamientos")
                val routine = repo.enrichRoutine(plan.routine)
                if (plan.fallback) {
                    repo.toast("info", "Plan de respaldo", plan.details ?: "La IA está saturada; se ha generado un plan base. Podrás regenerarlo más tarde.")
                }
                // No cancelable: al marcar isOnboarded esta pantalla sale de composición y no debe abortar la subida.
                withContext(NonCancellable) {
                    repo.saveProfile(withPlan = true) {
                        it.copy(profile = profile, routine = routine, diet = plan.diet, insights = plan.insights, isOnboarded = true)
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                repo.toast("error", "No se pudo generar tu plan", e.message ?: "Inténtalo de nuevo en unos segundos.")
                loading = false
            }
        }
    }

    fun next() {
        if (loading) return
        if (!validate(PAGES[page].pg)) return
        if (page < PAGES.lastIndex) page++ else generate()
    }

    fun back() { if (page > 0 && !loading) { errors.clear(); page-- } }

    BackHandler(enabled = loading || page > 0 || timeVisible) { if (timeVisible) timeVisible = false else if (!loading) back() }

    val shift = page * 9f
    val ca = animateColorAsState(Volt.accent.shiftHue(shift), tween(800), label = "hueA")
    val cb = animateColorAsState(Volt.accent2.shiftHue(shift), tween(800), label = "hueB")

    Box(Modifier.fillMaxSize()) {
        MeshBackground(Modifier.fillMaxSize(), colorA = { ca.value }, colorB = { cb.value }, intensity = 0.85f)
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn(tween(400)) togetherWith fadeOut(tween(250)) },
            label = "onbLoading",
            modifier = Modifier.fillMaxSize(),
        ) { isLoading ->
            if (isLoading) GeneratingPlanScreen()
            else Column(Modifier.fillMaxSize().statusBarsPadding().imePadding().navigationBarsPadding().padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val backA = animateFloatAsState(if (page == 0) 0f else 1f, tween(250), label = "backA")
                    Box(
                        Modifier.size(44.dp).graphicsLayer { alpha = backA.value }.clip(CircleShape).background(Color.White.copy(alpha = 0.08f))
                            .clickable(enabled = page > 0) { back() },
                        contentAlignment = Alignment.Center,
                    ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Paso anterior", tint = Volt.text, modifier = Modifier.size(22.dp)) }
                    Spacer(Modifier.width(12.dp))
                    SegmentedProgress(PAGES.size, page, Modifier.weight(1f), onJump = { if (it < page) { errors.clear(); page = it } })
                }
                Spacer(Modifier.height(16.dp))
                AnimatedContent(
                    targetState = page,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    transitionSpec = {
                        val dir = if (targetState > initialState) 1 else -1
                        (slideInHorizontally(tween(480, easing = EaseOutExpo)) { (dir * it * 0.6f).toInt() } + fadeIn(tween(320))) togetherWith
                            (slideOutHorizontally(tween(380, easing = EaseOutExpo)) { (-dir * it * 0.25f).toInt() } + fadeOut(tween(160)))
                    },
                    label = "onbPage",
                ) { p ->
                    val info = PAGES[p]
                    Column(Modifier.fillMaxSize()) {
                        Column(Modifier.staggeredEntry(0)) {
                            Text(info.kicker, color = Volt.accent, style = VoltType.label)
                            Spacer(Modifier.height(8.dp))
                            Text(info.title, color = Volt.text, style = VoltType.display.copy(fontSize = 38.sp, lineHeight = 40.sp))
                            Spacer(Modifier.height(6.dp))
                            Text(info.sub, color = Volt.textDim, style = VoltType.body)
                        }
                        Spacer(Modifier.height(20.dp))
                        Box(Modifier.weight(1f).fillMaxWidth().staggeredEntry(1).shake(errTick)) {
                            PageBody(info.pg, f, errors, meals, onOpenTime = { timeTarget = it; timeVisible = true }, onDone = { next() })
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                PrimaryButton(if (page == PAGES.lastIndex) "Generar mi plan" else "Continuar", onClick = { next() })
            }
        }

        AnimatedVisibility(
            timeVisible && !loading, enter = fadeIn(tween(200)), exit = fadeOut(tween(160)), modifier = Modifier.fillMaxSize(),
        ) {
            TimeSheet(
                title = listOf("Desayuno", "Media mañana", "Comida", "Merienda", "Cena")[timeTarget],
                initial = meals[timeTarget].value,
                onDone = { meals[timeTarget].value = it; timeVisible = false },
                onDismiss = { timeVisible = false },
            )
        }
    }
}

// ───────────────────────── Contenido de cada pregunta ─────────────────────────

@Composable
private fun BoxScope.PageBody(
    pg: Pg, f: Form, errors: MutableMap<String, String>, meals: List<MutableState<String>>,
    onOpenTime: (Int) -> Unit, onDone: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    when (pg) {
        Pg.Name -> BigTextField(f.name.value, { f.name.value = it.take(80); errors.remove("name") }, "Tu nombre o apodo", errors["name"], imeAction = ImeAction.Done, onDone = onDone)

        Pg.Age -> BigNumberPicker(
            (f.age.value.toIntOrNull() ?: 25).toFloat().coerceIn(12f, 100f),
            { f.age.value = it.roundToInt().toString(); errors.remove("age") }, 12f..100f, Modifier.align(Alignment.Center), unit = "años",
        )

        Pg.Gender -> Row(Modifier.fillMaxWidth().height(230.dp).align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf("Masculino" to Icons.Filled.Male, "Femenino" to Icons.Filled.Female).forEach { (v, icon) ->
                Tile(f.gender.value == v, { f.gender.value = v }, Modifier.weight(1f).fillMaxHeight()) { fg, _ ->
                    Spacer(Modifier.weight(1f))
                    Icon(icon, null, tint = fg, modifier = Modifier.size(64.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(v, color = fg, style = VoltType.title)
                }
            }
        }

        Pg.Weight -> BigNumberPicker(
            (parseNum(f.weight.value) ?: 70.0).toFloat().coerceIn(30f, 350f),
            { f.weight.value = fmtNum(it); errors.remove("weight") }, 30f..350f, Modifier.align(Alignment.Center), step = 0.5f, unit = "kg",
        )

        Pg.Height -> BigNumberPicker(
            (parseNum(f.height.value) ?: 175.0).toFloat().coerceIn(120f, 250f),
            { f.height.value = fmtNum(it); errors.remove("height") }, 120f..250f, Modifier.align(Alignment.Center), unit = "cm",
        )

        Pg.Experience -> Column(Modifier.fillMaxWidth().align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            EXPERIENCES.forEach { e ->
                Tile(f.experience.value.startsWith(e.title), { f.experience.value = "${e.title} (${e.desc})" }, Modifier.fillMaxWidth()) { fg, dim ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(e.emoji, fontSize = 34.sp)
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.title, color = fg, style = VoltType.h2)
                            Text(e.desc, color = dim, style = VoltType.body)
                        }
                    }
                }
            }
        }

        Pg.Goal -> Column(Modifier.fillMaxWidth().align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            GOALS.chunked(2).forEach { pair ->
                Row(Modifier.fillMaxWidth().height(170.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    pair.forEach { g ->
                        Tile(f.goal.value.contains(g.title), {
                            f.goal.value = "${g.title} (${g.sub})"
                            // sugerencia coherente de dirección (editable después)
                            f.direction.value = if (g.title.contains("Perder")) "Perder" else "Ganar"
                        }, Modifier.weight(1f).fillMaxHeight()) { fg, dim ->
                            Icon(g.icon, null, tint = fg, modifier = Modifier.size(38.dp))
                            Spacer(Modifier.weight(1f))
                            Text(g.title, color = fg, style = VoltType.title)
                            Text(g.sub, color = dim, style = VoltType.body.copy(fontSize = 12.sp))
                        }
                    }
                }
            }
        }

        Pg.Direction -> Row(Modifier.fillMaxWidth().height(230.dp).align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(Triple("Perder", "Bajar de peso", Icons.Filled.TrendingDown), Triple("Ganar", "Subir de peso", Icons.Filled.TrendingUp)).forEach { (v, label, icon) ->
                Tile(f.direction.value == v, { f.direction.value = v }, Modifier.weight(1f).fillMaxHeight()) { fg, _ ->
                    Spacer(Modifier.weight(1f))
                    Icon(icon, null, tint = fg, modifier = Modifier.size(56.dp))
                    Spacer(Modifier.height(10.dp))
                    Text(label, color = fg, style = VoltType.title)
                }
            }
        }

        Pg.Target -> GiantSlider(
            (parseNum(f.target.value) ?: 5.0).toFloat().coerceIn(1f, 50f), { f.target.value = it.roundToInt().toString(); errors.remove("target") },
            1f..50f, "kg", errors["target"], Modifier.align(Alignment.Center),
        )

        Pg.Months -> GiantSlider(
            (f.months.value.toIntOrNull() ?: 3).toFloat().coerceIn(1f, 24f), { f.months.value = it.roundToInt().toString(); errors.remove("months") },
            1f..24f, "meses", errors["months"], Modifier.align(Alignment.Center),
        )

        Pg.Days -> Column(Modifier.fillMaxWidth().align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(listOf(2, 3, 4), listOf(5, 6)).forEach { rowDays ->
                Row(Modifier.fillMaxWidth().height(130.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowDays.forEach { d ->
                        Tile(f.days.value == d, { f.days.value = d }, Modifier.weight(1f).fillMaxHeight()) { fg, dim ->
                            Spacer(Modifier.weight(1f))
                            Text("$d", color = fg, style = VoltType.numberXL.copy(fontSize = 52.sp, lineHeight = 54.sp))
                            Text("días", color = dim, style = VoltType.body)
                        }
                    }
                    if (rowDays.size < 3) Spacer(Modifier.weight(1f))
                }
            }
        }

        Pg.Minutes -> Column(Modifier.fillMaxWidth().align(Alignment.Center), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            listOf(listOf(30, 45), listOf(60, 90)).forEach { rowMin ->
                Row(Modifier.fillMaxWidth().height(150.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    rowMin.forEach { m ->
                        Tile(f.minutes.value == m, { f.minutes.value = m }, Modifier.weight(1f).fillMaxHeight()) { fg, dim ->
                            Spacer(Modifier.weight(1f))
                            Text("$m", color = fg, style = VoltType.numberXL.copy(fontSize = 52.sp, lineHeight = 54.sp))
                            Text("minutos", color = dim, style = VoltType.body)
                        }
                    }
                }
            }
        }

        Pg.Work -> BigTextField(f.workHours.value, { f.workHours.value = it.take(120); errors.remove("workHours") }, "09:00 - 18:00", errors["workHours"], imeAction = ImeAction.Done, onDone = onDone)

        Pg.Meals -> Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            val rows = listOf(
                "Desayuno" to Icons.Filled.Coffee, "Media mañana" to Icons.Filled.Fastfood, "Comida" to Icons.Filled.WbSunny,
                "Merienda" to Icons.Filled.Restaurant, "Cena" to Icons.Filled.NightsStay,
            )
            rows.forEachIndexed { i, (label, icon) ->
                Row(
                    Modifier.fillMaxWidth().staggeredEntry(i).clip(RoundedCornerShape(24.dp)).background(Volt.surface)
                        .gradientBorder(1.dp, RoundedCornerShape(24.dp)).pressScale { onOpenTime(i) }.padding(horizontal = 18.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(icon, null, tint = Volt.textDim, modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(14.dp))
                    Text(label, color = Volt.text, style = VoltType.title, modifier = Modifier.weight(1f))
                    GradientText(meals[i].value, style = VoltType.numberM)
                }
            }
        }

        Pg.Foods -> Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            LabeledField("Verduras", f.vegs.value, { f.vegs.value = it }, "brócoli, espinaca, tomate")
            LabeledField("Carbohidratos", f.carbs.value, { f.carbs.value = it }, "arroz, papa, avena")
            LabeledField("Proteínas", f.prots.value, { f.prots.value = it }, "pollo, huevo, atún")
        }

        Pg.Special -> Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Tile(f.specialOn.value, { f.specialOn.value = true }, Modifier.weight(1f).fillMaxHeight()) { fg, _ ->
                    Spacer(Modifier.weight(1f)); Text("Sí, tengo", color = fg, style = VoltType.title)
                }
                Tile(!f.specialOn.value, { f.specialOn.value = false }, Modifier.weight(1f).fillMaxHeight()) { fg, _ ->
                    Spacer(Modifier.weight(1f)); Text("No por ahora", color = fg, style = VoltType.title)
                }
            }
            AnimatedVisibility(
                visible = f.specialOn.value,
                enter = expandVertically(spring(0.85f, 400f)) + fadeIn(tween(250)),
                exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    LabeledField("Actividad", f.specialActivity.value, { f.specialActivity.value = it.take(60) }, "Pádel")
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DAYS.forEach { d -> VoltChip(d, f.specialDay.value.equals(d, ignoreCase = true), { f.specialDay.value = d }) }
                    }
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 45, 60, 90).forEach { m -> VoltChip("$m min", f.specialMin.value == m, { f.specialMin.value = m }) }
                    }
                }
            }
        }

        Pg.Dish -> Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            LabeledField("Ingredientes", f.dish.value, { f.dish.value = it.take(200) }, "arroz, lentejas, tomate, queso feta")
            LabeledField(
                "Calorías objetivo", f.dishKcal.value, { f.dishKcal.value = it.filter(Char::isDigit).take(4); errors.remove("dishKcal") }, "390",
                error = errors["dishKcal"], keyboardType = KeyboardType.Number, suffix = "kcal",
            )
        }
    }
}

// ───────────────────────── Componentes ─────────────────────────

@Composable
private fun LabeledField(
    label: String, value: String, onChange: (String) -> Unit, placeholder: String, error: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text, suffix: String? = null,
) {
    Column {
        Text(label.uppercase(), color = Volt.textDim, style = VoltType.label)
        BigTextField(value, onChange, placeholder, error, keyboardType = keyboardType, suffix = suffix, size = 22)
    }
}

/** Campo de texto enorme con subrayado degradado (sin caja). */
@Composable
private fun BigTextField(
    value: String, onChange: (String) -> Unit, placeholder: String, error: String?, modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text, imeAction: ImeAction = ImeAction.Next, onDone: () -> Unit = {},
    suffix: String? = null, size: Int = 30,
) {
    val style = VoltType.h1.copy(fontSize = size.sp, lineHeight = (size * 1.25f).sp, color = Volt.text)
    val grad = Volt.gradient
    Column(modifier.fillMaxWidth()) {
        BasicTextField(
            value = value, onValueChange = onChange, singleLine = true, textStyle = style, cursorBrush = SolidColor(Volt.accent),
            keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
            keyboardActions = KeyboardActions(onDone = { onDone() }, onNext = { }),
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { inner ->
                Row(Modifier.padding(vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f)) {
                        if (value.isEmpty()) Text(placeholder, style = style, color = Volt.textDim.copy(alpha = 0.45f), maxLines = 1)
                        inner()
                    }
                    if (suffix != null) Text(suffix, color = Volt.textDim, style = VoltType.title)
                }
            },
        )
        Box(Modifier.fillMaxWidth().height(3.dp).clip(CircleShape).drawBehind {
            if (error != null) drawRect(Color(0xFFFF4D6A)) else drawRect(grad)
        })
        if (error != null) Text(error, color = Volt.danger, style = VoltType.body, modifier = Modifier.padding(top = 8.dp))
    }
}

/** Mosaico seleccionable: relleno degradado al elegir, borde degradado fino si no, rebote al pulsar. */
@Composable
private fun Tile(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable ColumnScope.(Color, Color) -> Unit) {
    val sel = animateFloatAsState(if (selected) 1f else 0f, spring(0.7f, 450f), label = "tile")
    val shape = RoundedCornerShape(28.dp)
    val grad = Volt.gradient
    val base = Volt.surface
    val fg = animateColorAsState(if (selected) Volt.onAccent else Volt.text, tween(180), label = "tileFg").value
    val dim = animateColorAsState(if (selected) Volt.onAccent.copy(alpha = 0.7f) else Volt.textDim, tween(180), label = "tileDim").value
    Column(
        modifier.graphicsLayer { val s = 1f + 0.025f * sel.value; scaleX = s; scaleY = s }
            .pressScale(onClick).clip(shape)
            .drawBehind { drawRect(base); drawRect(grad, alpha = sel.value.coerceIn(0f, 1f)) }
            .then(if (selected) Modifier else Modifier.gradientBorder(1.dp, shape))
            .padding(20.dp),
    ) { this.content(fg, dim) }
}

/** Valor gigante con digitos rodantes + slider; tic haptico en cada valor entero. */
@Composable
private fun GiantSlider(
    value: Float, onChange: (Float) -> Unit, range: ClosedFloatingPointRange<Float>, unit: String, error: String?, modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val grad = Volt.gradient
    Column(modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        RollingNumber(
            value.roundToInt(), style = VoltType.numberXL.copy(fontSize = 120.sp, lineHeight = 124.sp), suffix = unit, brush = grad,
        )
        Spacer(Modifier.height(24.dp))
        Slider(
            value = value,
            onValueChange = { v ->
                val r = v.roundToInt().toFloat()
                if (r != value) { haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onChange(r) }
            },
            valueRange = range,
            colors = SliderDefaults.colors(
                thumbColor = Volt.accent, activeTrackColor = Volt.accent, inactiveTrackColor = Color.White.copy(alpha = 0.12f),
            ),
        )
        Row(Modifier.fillMaxWidth()) {
            Text("${range.start.roundToInt()}", color = Volt.textDim, style = VoltType.body, modifier = Modifier.weight(1f))
            Text("${range.endInclusive.roundToInt()}", color = Volt.textDim, style = VoltType.body)
        }
        if (error != null) Text(error, color = Volt.danger, style = VoltType.body, modifier = Modifier.padding(top = 12.dp))
    }
}

/** Selector de hora propio: dos ruedas (horas / minutos) en una hoja inferior. */
@Composable
private fun TimeSheet(title: String, initial: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    val (h0, m0) = remember(initial) { runCatching { initial.split(":").let { it[0].toInt() to it[1].toInt() } }.getOrDefault(12 to 0) }
    var h by remember { mutableIntStateOf(h0.coerceIn(0, 23)) }
    var m by remember { mutableIntStateOf(m0.coerceIn(0, 59)) }
    val none = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
    Box(
        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.6f)).clickable(interactionSource = none, indication = null) { onDismiss() },
        contentAlignment = Alignment.BottomCenter,
    ) {
        Column(
            Modifier.fillMaxWidth().clip(shape).background(Volt.surface).gradientBorder(1.dp, shape)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                .navigationBarsPadding().padding(horizontal = 24.dp).padding(top = 20.dp, bottom = 20.dp),
        ) {
            Text(title.uppercase(), color = Volt.accent, style = VoltType.label)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                BigNumberPicker(h.toFloat(), { h = it.roundToInt() }, 0f..23f, Modifier.weight(1f), itemHeight = 52.dp)
                Text(":", color = Volt.text, style = VoltType.numberL, modifier = Modifier.padding(horizontal = 8.dp))
                BigNumberPicker(m.toFloat(), { m = it.roundToInt() }, 0f..59f, Modifier.weight(1f), itemHeight = 52.dp)
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton("Listo", onClick = { onDone("%02d:%02d".format(h, m)) })
        }
    }
}
