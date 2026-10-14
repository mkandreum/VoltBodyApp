package com.voltbody.app.ui.screens

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cake
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Coffee
import androidx.compose.material.icons.filled.Fastfood
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.*
import com.voltbody.app.ui.VoltChip
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.rememberPulse
import com.voltbody.app.ui.screens.auth.GeneratingPlanScreen
import com.voltbody.app.ui.screens.auth.ambientGlow
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.voltFieldColors
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class StepInfo(val title: String, val subtitle: String)

private val STEPS = listOf(
    StepInfo("Tus Datos", "Información básica para personalizar tu perfil"),
    StepInfo("Tu Cuerpo", "Medidas corporales y experiencia actual"),
    StepInfo("Tu Objetivo", "Define tu meta física y horizonte temporal"),
    StepInfo("Entrenamiento", "Frecuencia y duración de tus sesiones"),
    StepInfo("Horarios", "Organización de tus comidas y jornadas"),
    StepInfo("Nutrición", "Alimentos favoritos y platos especiales"),
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

/** Wizard de alta: 6 pasos + generación del plan con IA. Validaciones idénticas a las del servidor. */
@Composable
fun OnboardingScreen(repo: Repository) {
    val st by repo.state.collectAsState()
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val f = rememberForm(repo.state.value.profile, repo.state.value.user?.name)
    var step by rememberSaveable { mutableStateOf(0) }
    var loading by remember { mutableStateOf(false) }
    val errors = remember { mutableStateMapOf<String, String>() }

    fun validate(s: Int): Boolean {
        errors.clear()
        when (s) {
            0 -> {
                val n = f.name.value.trim()
                if (n.isEmpty()) errors["name"] = "Introduce tu nombre para continuar"
                else if (n.length > 80) errors["name"] = "El nombre no puede superar 80 caracteres"
                val a = f.age.value.trim().toIntOrNull()
                if (a == null || a < 12 || a > 100) errors["age"] = "La edad debe estar entre 12 y 100"
            }
            1 -> {
                val w = parseNum(f.weight.value)
                if (w == null || w < 30 || w > 350) errors["weight"] = "Entre 30 y 350 kg"
                val h = parseNum(f.height.value)
                if (h == null || h < 120 || h > 250) errors["height"] = "Entre 120 y 250 cm"
            }
            2 -> {
                val t = parseNum(f.target.value)
                if (t == null || t < 1 || t > 50) errors["target"] = "Entre 1 y 50 kg"
                val m = f.months.value.trim().toIntOrNull()
                if (m == null || m < 1 || m > 24) errors["months"] = "Entre 1 y 24 meses"
            }
            4 -> if (f.workHours.value.length > 120) errors["workHours"] = "Máximo 120 caracteres"
            5 -> {
                val k = f.dishKcal.value.trim()
                if (k.isNotEmpty() && (k.toIntOrNull()?.let { it < 50 || it > 3000 } != false)) errors["dishKcal"] = "Entre 50 y 3000 kcal"
            }
        }
        if (errors.isNotEmpty()) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        return errors.isEmpty()
    }

    fun buildProfile(): Profile {
        val goal = f.goal.value
        val dish = f.dish.value.trim()
        return st.profile.copy(
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
        if (!validate(step)) return
        if (step < STEPS.lastIndex) step++ else generate()
    }

    fun back() { if (step > 0 && !loading) { errors.clear(); step-- } }

    BackHandler(enabled = loading || step > 0) { if (!loading) back() }

    val glow = Volt.accent.copy(alpha = 0.10f)
    Box(Modifier.fillMaxSize().ambientGlow(glow, Offset(1000f, 300f), 700f)) {
        AnimatedContent(
            targetState = loading,
            transitionSpec = { fadeIn(tween(350)) togetherWith fadeOut(tween(250)) },
            label = "onbLoading",
        ) { isLoading ->
            if (isLoading) GeneratingPlanScreen()
            else Column(Modifier.fillMaxSize().imePadding().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)) {
                StepHeader(step, onBack = { back() }, onJump = { if (it < step) { errors.clear(); step = it } })
                Spacer(Modifier.height(14.dp))
                Box(
                    Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Volt.surface)
                        .border(1.dp, Volt.border, RoundedCornerShape(28.dp)),
                ) {
                    AnimatedContent(
                        targetState = step,
                        transitionSpec = {
                            val fwd = targetState > initialState
                            (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { if (fwd) it / 3 else -it / 3 } + fadeIn(tween(320))) togetherWith
                                (slideOutHorizontally(tween(260, easing = FastOutSlowInEasing)) { if (fwd) -it / 3 else it / 3 } + fadeOut(tween(180)))
                        },
                        label = "onbStep",
                    ) { s ->
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp)) {
                            Text("PASO ${s + 1} DE ${STEPS.size}", color = Volt.accent, fontSize = 10.sp, letterSpacing = 2.sp, fontWeight = FontWeight.SemiBold)
                            Text(STEPS[s].title, color = Volt.text, fontSize = 26.sp, fontWeight = FontWeight.Black, modifier = Modifier.padding(top = 2.dp))
                            Text(STEPS[s].subtitle, color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp, bottom = 20.dp))
                            when (s) {
                                0 -> StepBasics(f, errors)
                                1 -> StepBody(f, errors)
                                2 -> StepGoal(f, errors)
                                3 -> StepTraining(f)
                                4 -> StepSchedule(f, errors)
                                else -> StepNutrition(f, errors)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
                val last = step == STEPS.lastIndex
                val pulse by rememberPulse(1f, 1.025f, 900)
                Button(
                    onClick = { next() },
                    modifier = Modifier.fillMaxWidth().height(54.dp).scale(if (last) pulse else 1f),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Volt.accent, contentColor = Color.Black),
                ) {
                    Text(if (last) "Generar mi plan con IA" else "Continuar", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(if (last) Icons.Filled.AutoAwesome else Icons.AutoMirrored.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
private fun StepHeader(step: Int, onBack: () -> Unit, onJump: (Int) -> Unit) {
    val backAlpha by animateFloatAsState(if (step == 0) 0f else 1f, tween(250), label = "backA")
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Box(
            Modifier.size(44.dp).alpha(backAlpha).clip(RoundedCornerShape(16.dp)).background(Color.White.copy(alpha = 0.06f))
                .border(1.dp, Volt.border, RoundedCornerShape(16.dp)).clickable(enabled = step > 0, onClick = onBack),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Paso anterior", tint = Volt.text, modifier = Modifier.size(20.dp)) }

        Row(
            Modifier.clip(CircleShape).background(Color.Black.copy(alpha = 0.4f)).border(1.dp, Volt.border, CircleShape).padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(STEPS.size) { i ->
                val w by animateDpAsState(if (i == step) 24.dp else 8.dp, tween(300, easing = FastOutSlowInEasing), label = "dotW")
                val c by animateColorAsState(
                    when { i == step -> Volt.accent; i < step -> Volt.accent.copy(alpha = 0.4f); else -> Color.White.copy(alpha = 0.2f) },
                    tween(300), label = "dotC",
                )
                Box(Modifier.height(8.dp).width(w).clip(CircleShape).background(c).clickable(enabled = i < step) { onJump(i) })
            }
        }
        Text("${step + 1}/${STEPS.size}", color = Volt.textDim, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(44.dp), textAlign = TextAlign.End)
    }
}

// ───────────────────────── Pasos ─────────────────────────

@Composable
private fun StepBasics(f: Form, errors: MutableMap<String, String>) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.staggeredEntry(0)) {
            ObField(f.name.value, { f.name.value = it.take(80); errors.remove("name") }, "¿Cómo te llamas?", Icons.Filled.Person, errors["name"], placeholder = "Tu nombre o apodo")
        }
        Box(Modifier.staggeredEntry(1)) {
            ObField(f.age.value, { f.age.value = it.filter(Char::isDigit).take(3); errors.remove("age") }, "Edad (años)", Icons.Filled.Cake, errors["age"], KeyboardType.Number)
        }
        Column(Modifier.staggeredEntry(2)) {
            Text("Género biológico", color = Volt.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                listOf("Masculino" to "♂️ Masculino", "Femenino" to "♀️ Femenino").forEach { (v, label) ->
                    Choice(f.gender.value == v, { f.gender.value = v; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, Modifier.weight(1f)) {
                        Box(Modifier.fillMaxWidth().heightIn(min = 36.dp), contentAlignment = Alignment.Center) {
                            Text(label, color = Volt.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepBody(f: Form, errors: MutableMap<String, String>) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(Modifier.staggeredEntry(0), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.weight(1f)) {
                ObField(f.weight.value, { f.weight.value = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6); errors.remove("weight") }, "Peso (kg)", Icons.Filled.Scale, errors["weight"], KeyboardType.Decimal)
            }
            Box(Modifier.weight(1f)) {
                ObField(f.height.value, { f.height.value = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(6); errors.remove("height") }, "Altura (cm)", Icons.Filled.Straighten, errors["height"], KeyboardType.Decimal)
            }
        }
        Text("Nivel de experiencia", color = Volt.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp).staggeredEntry(1))
        EXPERIENCES.forEachIndexed { i, e ->
            Choice(f.experience.value.startsWith(e.title), { f.experience.value = "${e.title} (${e.desc})"; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, Modifier.staggeredEntry(i + 2)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(e.emoji, fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(e.title, color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(e.desc, color = Volt.textDim, fontSize = 12.sp)
                    }
                    CheckDot(f.experience.value.startsWith(e.title))
                }
            }
        }
    }
}

@Composable
private fun StepGoal(f: Form, errors: MutableMap<String, String>) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text("Meta principal", color = Volt.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
        GOALS.chunked(2).forEachIndexed { r, pair ->
            Row(Modifier.height(IntrinsicSize.Min).staggeredEntry(r), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                pair.forEach { g ->
                    val sel = f.goal.value.contains(g.title)
                    Choice(sel, {
                        f.goal.value = "${g.title} (${g.sub})"
                        // sugerencia coherente de dirección (editable después)
                        f.direction.value = if (g.title.contains("Perder")) "Perder" else "Ganar"
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }, Modifier.weight(1f).fillMaxHeight()) {
                        Column {
                            Box(
                                Modifier.size(36.dp).clip(RoundedCornerShape(12.dp)).background(if (sel) Volt.accent else Color.White.copy(alpha = 0.06f)),
                                contentAlignment = Alignment.Center,
                            ) { Icon(g.icon, null, tint = if (sel) Color.Black else Volt.textDim, modifier = Modifier.size(20.dp)) }
                            Spacer(Modifier.height(10.dp))
                            Text(g.title, color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Text(g.sub, color = Volt.textDim, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
        Column(
            Modifier.staggeredEntry(3).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Volt.border, RoundedCornerShape(20.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Dirección del peso", color = Volt.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                listOf("Perder", "Ganar").forEach { d ->
                    VoltChip(d, f.direction.value == d, { f.direction.value = d; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, Modifier.padding(start = 6.dp))
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.weight(1f)) {
                    ObField(f.target.value, { f.target.value = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(5); errors.remove("target") }, "Cantidad a modificar", null, errors["target"], KeyboardType.Decimal, suffix = "kg")
                }
                Box(Modifier.weight(1f)) {
                    ObField(f.months.value, { f.months.value = it.filter(Char::isDigit).take(2); errors.remove("months") }, "Plazo estimado", null, errors["months"], KeyboardType.Number, suffix = "meses")
                }
            }
        }
    }
}

@Composable
private fun StepTraining(f: Form) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Column(Modifier.staggeredEntry(0)) {
            Row(Modifier.padding(horizontal = 4.dp, vertical = 0.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Días de entrenamiento / semana", color = Volt.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${f.days.value} días", color = Volt.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(2, 3, 4, 5, 6).forEach { d ->
                    NumberBox("${d}d", f.days.value == d, { f.days.value = d; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, Modifier.weight(1f))
                }
            }
        }
        Column(Modifier.staggeredEntry(1)) {
            Row(Modifier.padding(horizontal = 4.dp).padding(bottom = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Duración por sesión", color = Volt.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                Text("${f.minutes.value} min", color = Volt.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(30, 45, 60, 90).forEach { m ->
                    NumberBox("${m}m", f.minutes.value == m, { f.minutes.value = m; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) }, Modifier.weight(1f))
                }
            }
        }
        Row(
            Modifier.staggeredEntry(2).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Volt.accent.copy(alpha = 0.10f))
                .border(1.dp, Volt.accent.copy(alpha = 0.25f), RoundedCornerShape(20.dp)).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.AutoAwesome, null, tint = Volt.accent, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                androidx.compose.ui.text.buildAnnotatedString {
                    append("Diseñaremos un microciclo de ")
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Volt.accent, fontWeight = FontWeight.Bold)); append("${f.days.value} días"); pop()
                    append(" con sesiones compactas de ")
                    pushStyle(androidx.compose.ui.text.SpanStyle(color = Volt.accent, fontWeight = FontWeight.Bold)); append("${f.minutes.value} minutos"); pop()
                    append(".")
                },
                color = Volt.text, fontSize = 12.sp, lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun StepSchedule(f: Form, errors: MutableMap<String, String>) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Box(Modifier.staggeredEntry(0)) {
            ObField(f.workHours.value, { f.workHours.value = it.take(120); errors.remove("workHours") }, "Horario laboral / estudio", Icons.Filled.Schedule, errors["workHours"], placeholder = "Ej. 09:00 - 18:00")
        }
        Column(
            Modifier.staggeredEntry(1).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Volt.border, RoundedCornerShape(20.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("HORARIOS DE INGESTA", color = Volt.textDim, fontSize = 11.sp, letterSpacing = 1.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp, bottom = 2.dp))
            MealTimeRow("Desayuno", Icons.Filled.Coffee, f.breakfast.value) { f.breakfast.value = it }
            MealTimeRow("Media mañana", Icons.Filled.Fastfood, f.brunch.value) { f.brunch.value = it }
            MealTimeRow("Comida", Icons.Filled.WbSunny, f.lunch.value) { f.lunch.value = it }
            MealTimeRow("Merienda", Icons.Filled.Restaurant, f.snack.value) { f.snack.value = it }
            MealTimeRow("Cena", Icons.Filled.NightsStay, f.dinner.value) { f.dinner.value = it }
        }
    }
}

@Composable
private fun StepNutrition(f: Form, errors: MutableMap<String, String>) {
    val haptic = LocalHapticFeedback.current
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.staggeredEntry(0)) { ObField(f.vegs.value, { f.vegs.value = it }, "Verduras y vegetales preferidos", null, null, placeholder = "Ej: brócoli, espinaca, tomate") }
        Box(Modifier.staggeredEntry(1)) { ObField(f.carbs.value, { f.carbs.value = it }, "Carbohidratos", null, null, placeholder = "Arroz, papa, avena") }
        Box(Modifier.staggeredEntry(2)) { ObField(f.prots.value, { f.prots.value = it }, "Proteínas", null, null, placeholder = "Pollo, huevo, atún") }

        Column(
            Modifier.staggeredEntry(3).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.35f))
                .border(1.dp, Volt.border, RoundedCornerShape(20.dp)).padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Clase especial semanal", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("Pádel, fútbol, zumba, natación...", color = Volt.textDim, fontSize = 11.sp)
                }
                Switch(
                    checked = f.specialOn.value,
                    onCheckedChange = { f.specialOn.value = it; haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove) },
                    colors = SwitchDefaults.colors(checkedThumbColor = Color.Black, checkedTrackColor = Volt.accent, uncheckedThumbColor = Color.White, uncheckedTrackColor = Volt.surface2, uncheckedBorderColor = Volt.border),
                )
            }
            AnimatedVisibility(
                visible = f.specialOn.value,
                enter = expandVertically(tween(280)) + fadeIn(tween(280)),
                exit = shrinkVertically(tween(220)) + fadeOut(tween(160)),
            ) {
                Column(Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ObField(f.specialActivity.value, { f.specialActivity.value = it.take(60) }, "Actividad (ej. Pádel)", null, null)
                    Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DAYS.forEach { d -> VoltChip(d, f.specialDay.value.equals(d, ignoreCase = true), { f.specialDay.value = d }) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(30, 45, 60, 90).forEach { m -> NumberBox("${m}m", f.specialMin.value == m, { f.specialMin.value = m }, Modifier.weight(1f)) }
                    }
                }
            }
        }

        Column(Modifier.staggeredEntry(4), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Plato especial (opcional)", color = Volt.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(start = 4.dp))
            ObField(f.dish.value, { f.dish.value = it.take(200) }, "Ingredientes", null, null, placeholder = "arroz, lentejas, tomate, queso feta")
            ObField(f.dishKcal.value, { f.dishKcal.value = it.filter(Char::isDigit).take(4); errors.remove("dishKcal") }, "Calorías objetivo", null, errors["dishKcal"], KeyboardType.Number, suffix = "kcal")
        }
    }
}

// ───────────────────────── Componentes ─────────────────────────

@Composable
private fun ObField(
    value: String, onChange: (String) -> Unit, label: String, icon: ImageVector?, error: String?,
    keyboardType: KeyboardType = KeyboardType.Text, placeholder: String? = null, suffix: String? = null,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        placeholder = if (placeholder != null) { { Text(placeholder, color = Volt.textDim.copy(alpha = 0.6f)) } } else null,
        leadingIcon = if (icon != null) { { Icon(icon, null, tint = Volt.textDim, modifier = Modifier.size(20.dp)) } } else null,
        suffix = if (suffix != null) { { Text(suffix, color = Volt.textDim, fontSize = 12.sp) } } else null,
        isError = error != null,
        supportingText = if (error != null) { { Text(error) } } else null,
        shape = RoundedCornerShape(18.dp), colors = voltFieldColors(),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
    )
}

/** Tarjeta seleccionable con borde/fondo animados y rebote al pulsar. */
@Composable
private fun Choice(selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val border by animateColorAsState(if (selected) Volt.accent else Volt.border, tween(220), label = "choiceB")
    val bg by animateColorAsState(if (selected) Volt.accent.copy(alpha = 0.12f) else Color.White.copy(alpha = 0.03f), tween(220), label = "choiceBg")
    Box(
        modifier.pressScale(onClick).clip(RoundedCornerShape(20.dp)).background(bg)
            .border(if (selected) 1.5.dp else 1.dp, border, RoundedCornerShape(20.dp)).padding(14.dp),
    ) { content() }
}

@Composable
private fun CheckDot(visible: Boolean) {
    AnimatedVisibility(visible, enter = scaleIn(tween(220)) + fadeIn(), exit = scaleOut(tween(150)) + fadeOut()) {
        Box(Modifier.size(22.dp).clip(CircleShape).background(Volt.accent), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp))
        }
    }
}

@Composable
private fun NumberBox(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val border by animateColorAsState(if (selected) Volt.accent else Volt.border, tween(200), label = "nbB")
    val bg by animateColorAsState(if (selected) Volt.accent.copy(alpha = 0.15f) else Color.White.copy(alpha = 0.03f), tween(200), label = "nbBg")
    val tc by animateColorAsState(if (selected) Volt.accent else Volt.textDim, tween(200), label = "nbT")
    Box(
        modifier.height(48.dp).pressScale(onClick).clip(RoundedCornerShape(16.dp)).background(bg).border(1.dp, border, RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = tc, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
}

@Composable
private fun MealTimeRow(label: String, icon: ImageVector, time: String, onPick: (String) -> Unit) {
    val ctx = LocalContext.current
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.Black.copy(alpha = 0.3f)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = Volt.textDim, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(10.dp))
        Text(label, color = Volt.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
        Box(
            Modifier.clip(RoundedCornerShape(10.dp)).background(Volt.surface2).clickable {
                val (h, m) = runCatching { time.split(":").let { it[0].toInt() to it[1].toInt() } }.getOrDefault(12 to 0)
                TimePickerDialog(ctx, { _, hh, mm -> onPick("%02d:%02d".format(hh, mm)) }, h, m, true).show()
            }.padding(horizontal = 14.dp, vertical = 8.dp),
        ) { Text(time, color = Volt.accent, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
    }
}
