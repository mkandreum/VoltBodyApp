package com.voltbody.app.ui.screens.profile

import android.app.TimePickerDialog
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.MealTimes
import com.voltbody.app.data.Profile
import com.voltbody.app.data.Repository
import com.voltbody.app.data.WeeklySpecialSession
import com.voltbody.app.ui.*
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.util.Notifications
import kotlinx.coroutines.launch

val LEVELS = listOf(
    "Principiante" to "Nunca he entrenado o tengo poca experiencia",
    "Intermedio" to "Entreno a veces y conozco los ejercicios básicos",
    "Avanzado" to "Entreno regularmente con técnica depurada",
)
val GOALS = listOf(
    "Ganar masa muscular" to "Hipertrofia y volumen",
    "Perder grasa" to "Definición y corte",
    "Mantenimiento y salud" to "Vitalidad y longevidad",
    "Fuerza bruta" to "Potencia y cargas máximas",
)
val WEEK_DAYS = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfChips(content: @Composable () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
}

@Composable
fun ProfLabel(text: String) {
    Text(text.uppercase(), color = Volt.textDim, style = VoltType.label, modifier = Modifier.padding(top = 18.dp, bottom = 8.dp))
}

/**
 * Edicion de todos los datos del perfil con las MISMAS validaciones que el servidor
 * (edad 12-100, peso 30-350, estatura 120-250, textos <= 140, workHours <= 120).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PersonalDataCard(repo: Repository, p: Profile, notificationsEnabled: Boolean, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }

    var age by remember(p) { mutableStateOf(p.age?.toString().orEmpty()) }
    var weight by remember(p) { mutableStateOf(p.weight?.let { trimNum(it) }.orEmpty()) }
    var height by remember(p) { mutableStateOf(p.height?.let { trimNum(it) }.orEmpty()) }
    var gender by remember(p) { mutableStateOf(p.gender ?: "Masculino") }
    var level by remember(p) { mutableStateOf(LEVELS.firstOrNull { p.currentState?.startsWith(it.first) == true }?.first ?: LEVELS[0].first) }
    var goal by remember(p) { mutableStateOf(GOALS.firstOrNull { p.goal?.startsWith(it.first) == true }?.first ?: GOALS[0].first) }
    var direction by remember(p) { mutableStateOf(p.goalDirection ?: "Perder") }
    var targetKg by remember(p) { mutableStateOf(p.goalTargetKg?.let { trimNum(it) }.orEmpty()) }
    var months by remember(p) { mutableStateOf(p.goalTimelineMonths?.toString().orEmpty()) }
    var days by remember(p) { mutableStateOf(p.trainingDaysPerWeek ?: 3) }
    var minutes by remember(p) { mutableStateOf((p.sessionMinutes ?: 45).toString()) }
    var workHours by remember(p) { mutableStateOf(p.workHours.orEmpty()) }
    var meals by remember(p) { mutableStateOf(p.mealTimes ?: MealTimes()) }
    var special by remember(p) { mutableStateOf(p.weeklySpecialSession ?: WeeklySpecialSession(false, "Zumba", "Sábado", 60)) }
    var specialDuration by remember(p) { mutableStateOf(special.durationMinutes.toString()) }

    fun validate(): String? {
        val a = age.toIntOrNull(); val w = weight.replace(',', '.').toDoubleOrNull(); val h = height.replace(',', '.').toDoubleOrNull()
        if (a == null || a !in 12..100) return "La edad debe estar entre 12 y 100 años."
        if (w == null || w < 30 || w > 350) return "El peso debe estar entre 30 y 350 kg."
        if (h == null || h < 120 || h > 250) return "La estatura debe estar entre 120 y 250 cm."
        val t = targetKg.replace(',', '.').toDoubleOrNull()
        if (t == null || t <= 0 || t > 200) return "Los kg objetivo deben estar entre 0,1 y 200."
        val m = months.toIntOrNull()
        if (m == null || m !in 1..36) return "El plazo debe estar entre 1 y 36 meses."
        val mi = minutes.toIntOrNull()
        if (mi == null || mi !in 15..180) return "La duración de sesión debe estar entre 15 y 180 min."
        if (workHours.length > 120) return "Horario laboral demasiado largo (máx. 120 caracteres)."
        if (special.enabled) {
            if (special.activity.isBlank() || special.activity.length > 60) return "Indica la actividad de la sesión especial (máx. 60 caracteres)."
            val d = specialDuration.toIntOrNull()
            if (d == null || d !in 10..300) return "La duración de la sesión especial debe estar entre 10 y 300 min."
        }
        return null
    }

    fun save() {
        val err = validate()
        error = err
        if (err != null) return
        val levelFull = LEVELS.first { it.first == level }.let { "${it.first} (${it.second})" }
        val goalFull = GOALS.first { it.first == goal }.let { "${it.first} (${it.second})" }
        val sp = special.copy(durationMinutes = specialDuration.toIntOrNull() ?: special.durationMinutes, activity = special.activity.trim())
        saving = true
        scope.launch {
            repo.saveProfile {
                it.copy(profile = it.profile.copy(
                    age = age.toInt(), weight = weight.replace(',', '.').toDouble(), height = height.replace(',', '.').toDouble(),
                    gender = gender, currentState = levelFull, goal = goalFull, goalDirection = direction,
                    goalTargetKg = targetKg.replace(',', '.').toDouble(), goalTimelineMonths = months.toInt(),
                    trainingDaysPerWeek = days, sessionMinutes = minutes.toInt(),
                    schedule = "$days días a la semana, $minutes min",
                    workHours = workHours.trim().ifBlank { null }, mealTimes = meals, weeklySpecialSession = sp,
                ))
            }
            if (notificationsEnabled) Notifications.scheduleAll(ctx, repo.state.value)
            repo.toast("success", "Cambios guardados", "Tus datos se han actualizado.")
            saving = false
            editing = false
        }
    }

    BentoCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Mis datos", color = Volt.text, style = VoltType.h2)
                Text("Biometría, objetivos, horarios y sesión especial", color = Volt.textDim, style = VoltType.body)
            }
            Text(
                if (editing) "Cancelar" else "Editar", color = if (editing) Volt.textDim else Volt.accent, style = VoltType.label,
                modifier = Modifier.clip(androidx.compose.foundation.shape.CircleShape).background(Volt.surface2).pressScale { editing = !editing; error = null }.padding(horizontal = 16.dp, vertical = 14.dp),
            )
        }
        AnimatedVisibility(editing, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column {
                ProfLabel("Biometría")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoltTextField(age, { age = it.filter(Char::isDigit).take(3) }, "Edad", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                    VoltTextField(weight, { weight = filterDecimal(it) }, "Peso (kg)", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                    VoltTextField(height, { height = filterDecimal(it) }, "Estatura (cm)", Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                }
                ProfLabel("Género biológico")
                ProfChips { listOf("Masculino", "Femenino").forEach { VoltChip(it, gender == it, { gender = it }) } }
                ProfLabel("Nivel de experiencia")
                ProfChips { LEVELS.forEach { VoltChip(it.first, level == it.first, { level = it.first }) } }
                ProfLabel("Meta principal")
                ProfChips { GOALS.forEach { VoltChip(it.first, goal == it.first, { goal = it.first }) } }
                ProfLabel("Objetivo de peso")
                ProfChips { listOf("Perder", "Ganar").forEach { VoltChip(it, direction == it, { direction = it }) } }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    VoltTextField(targetKg, { targetKg = filterDecimal(it) }, "Kg a $direction".lowercase().replaceFirstChar { c -> c.uppercase() }, Modifier.weight(1f), keyboardType = KeyboardType.Decimal)
                    VoltTextField(months, { months = it.filter(Char::isDigit).take(2) }, "Plazo (meses)", Modifier.weight(1f), keyboardType = KeyboardType.Number)
                }
                ProfLabel("Días de entreno por semana")
                ProfChips { (1..7).forEach { VoltChip("$it", days == it, { days = it }) } }
                Spacer(Modifier.height(8.dp))
                VoltTextField(minutes, { minutes = it.filter(Char::isDigit).take(3) }, "Minutos por sesión (15-180)", keyboardType = KeyboardType.Number)
                Spacer(Modifier.height(8.dp))
                VoltTextField(workHours, { workHours = it.take(120) }, "Horario laboral (ej. 9:00-17:00)")

                ProfLabel("Horarios de comida")
                ProfChips {
                    TimeChip("Desayuno", meals.breakfast) { meals = meals.copy(breakfast = it) }
                    TimeChip("Brunch", meals.brunch) { meals = meals.copy(brunch = it) }
                    TimeChip("Comida", meals.lunch) { meals = meals.copy(lunch = it) }
                    TimeChip("Merienda", meals.snack) { meals = meals.copy(snack = it) }
                    TimeChip("Cena", meals.dinner) { meals = meals.copy(dinner = it) }
                }

                ProfLabel("Sesión semanal especial")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Añadir una sesión extra (ej. Zumba, running)", color = Volt.text, style = VoltType.body, modifier = Modifier.weight(1f))
                    Switch(
                        checked = special.enabled, onCheckedChange = { special = special.copy(enabled = it) },
                        colors = SwitchDefaults.colors(checkedTrackColor = Volt.accent, checkedThumbColor = Volt.onAccent, uncheckedTrackColor = Volt.surface2, uncheckedBorderColor = Volt.border),
                    )
                }
                AnimatedVisibility(special.enabled, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Column(Modifier.padding(top = 8.dp)) {
                        VoltTextField(special.activity, { special = special.copy(activity = it.take(60)) }, "Actividad")
                        ProfLabel("Día")
                        ProfChips { WEEK_DAYS.forEach { d -> VoltChip(d, special.day == d, { special = special.copy(day = d) }) } }
                        Spacer(Modifier.height(8.dp))
                        VoltTextField(specialDuration, { specialDuration = it.filter(Char::isDigit).take(3) }, "Duración (min)", keyboardType = KeyboardType.Number)
                    }
                }

                AnimatedVisibility(error != null) {
                    Text(error.orEmpty(), color = Volt.danger, style = VoltType.body, modifier = Modifier.padding(top = 12.dp))
                }
                Spacer(Modifier.height(16.dp))
                PrimaryButton("Guardar cambios", onClick = ::save, loading = saving)
            }
        }
    }
}

@Composable
private fun TimeChip(label: String, value: String, onChange: (String) -> Unit) {
    val ctx = LocalContext.current
    Column(
        Modifier.clip(RoundedCornerShape(20.dp)).background(Volt.surface2).clickable {
            val h = value.substringBefore(':').toIntOrNull() ?: 12
            val m = value.substringAfter(':', "0").toIntOrNull() ?: 0
            TimePickerDialog(ctx, { _, hh, mm -> onChange("%02d:%02d".format(hh, mm)) }, h, m, true).show()
        }.padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(label.uppercase(), color = Volt.textDim, style = VoltType.label)
        Text(value, color = Volt.text, style = VoltType.numberS)
    }
}

fun filterDecimal(s: String): String {
    val t = s.replace(',', '.').filter { it.isDigit() || it == '.' }
    val i = t.indexOf('.')
    return (if (i >= 0) t.substring(0, i + 1) + t.substring(i + 1).replace(".", "") else t).take(6)
}

fun trimNum(d: Double): String = if (d % 1.0 == 0.0) d.toInt().toString() else "%.1f".format(d).replace(',', '.')
