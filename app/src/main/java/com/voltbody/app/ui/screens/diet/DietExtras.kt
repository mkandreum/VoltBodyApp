package com.voltbody.app.ui.screens.diet

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
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
import com.voltbody.app.data.DailyMetric
import com.voltbody.app.data.DietPlan
import com.voltbody.app.data.FoodPreferences
import com.voltbody.app.data.Repository
import com.voltbody.app.data.SpecialDish
import com.voltbody.app.ui.*
import com.voltbody.app.ui.theme.Volt
import kotlinx.coroutines.launch
import java.time.LocalDate

const val WATER_TARGET_ML = 2500

/** Contador de agua del día: se lee/escribe en DailyMetric.waterMl (servidor). */
@Composable
fun WaterCard(repo: Repository, modifier: Modifier = Modifier) {
    val st by repo.state.collectAsState()
    val scope = rememberCoroutineScope()
    val today = remember { LocalDate.now().toString() }
    val ml = st.metrics.firstOrNull { it.date == today }?.waterMl ?: 0
    fun add(delta: Int) {
        val next = (ml + delta).coerceAtLeast(0)
        scope.launch { repo.saveMetrics(today, DailyMetric(today, waterMl = next)) }
    }
    val pct = ((ml * 100f) / WATER_TARGET_ML).coerceIn(0f, 100f)
    val bar by animateFloatAsState(pct / 100f, tween(700, easing = FastOutSlowInEasing), label = "waterBar")
    val cups = 10
    val activeCups = (ml / 250).coerceAtMost(cups)

    VoltCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("HIDRATACIÓN", color = Volt.textDim, fontSize = 10.sp, letterSpacing = 1.sp)
                Text("Agua de hoy", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            }
            Row(
                Modifier.clip(RoundedCornerShape(50)).background(Volt.water.copy(alpha = 0.12f)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CountUpText(ml.toFloat(), style = androidx.compose.ui.text.TextStyle(color = Volt.water, fontSize = 13.sp, fontWeight = FontWeight.Bold))
                Text(" / ${WATER_TARGET_ML}ml", color = Volt.water, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            Text("${pct.toInt()}% completado", color = Volt.water, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
            Text("${(WATER_TARGET_ML - ml).coerceAtLeast(0)}ml restantes", color = Volt.textDim, fontSize = 11.sp)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(5.dp)).background(Volt.surface2)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(bar).clip(RoundedCornerShape(5.dp)).background(Volt.water))
        }
        Spacer(Modifier.height(14.dp))
        for (row in 0 until 2) {
            Row(Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 5) {
                    val idx = row * 5 + col
                    val filled = idx < activeCups
                    val fill by animateFloatAsState(if (filled) 1f else 0f, tween(450, easing = FastOutSlowInEasing), label = "cup")
                    Box(
                        Modifier.weight(1f).height(46.dp).clip(RoundedCornerShape(12.dp))
                            .background(Volt.surface2)
                            .pressScale { if (filled) add(-250) else add(250) },
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(Modifier.fillMaxWidth().fillMaxHeight(fill).background(Volt.water.copy(alpha = 0.75f)))
                        Text(if (filled) "💧" else "250", color = if (filled) Color.White else Volt.textDim, fontSize = 10.sp, modifier = Modifier.padding(bottom = 4.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            WaterButton("+ 250ml", Volt.water, Modifier.weight(1f)) { add(250) }
            WaterButton("+ 500ml", Volt.water, Modifier.weight(1f)) { add(500) }
            WaterButton("- 250ml", Volt.textDim, Modifier.weight(1f), enabled = ml > 0) { add(-250) }
        }
    }
}

@Composable
private fun WaterButton(text: String, color: Color, modifier: Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    Box(
        modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(color.copy(alpha = if (enabled) 0.12f else 0.05f))
            .let { if (enabled) it.pressScale(onClick) else it },
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (enabled) color else color.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

private val baseSpecialDish = listOf("arroz" to 130, "lentejas" to 116, "tomate" to 18, "queso feta" to 265)

/** Plato especial ajustable: escala los gramos (base 100 g de cada ingrediente) al objetivo calorico. */
@Composable
fun SpecialDishCard(repo: Repository, diet: DietPlan, saved: SpecialDish?, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var target by remember(saved?.targetCalories) { mutableStateOf((saved?.targetCalories?.takeIf { it in 200..900 } ?: 390).toString()) }
    var quick by remember { mutableStateOf(false) }
    val targetKcal = (target.toIntOrNull() ?: 390).coerceIn(200, 900)
    val baseCalories = baseSpecialDish.sumOf { it.second }
    val scale = targetKcal.toDouble() / baseCalories
    val totalMacros = (diet.macros.protein + diet.macros.carbs + diet.macros.fat).coerceAtLeast(1)

    VoltCard(modifier.fillMaxWidth()) {
        SectionTitle("🍲 Plato Especial Ajustable")
        Text("Base estándar: arroz + lentejas + tomate + queso feta", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Calorías objetivo", color = Volt.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text("$targetKcal kcal", color = Volt.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(6.dp))
        Slider(
            value = targetKcal.toFloat(), onValueChange = { target = (it / 10f).toInt().times(10).toString() },
            valueRange = 200f..900f, steps = 69,
            colors = SliderDefaults.colors(thumbColor = Volt.accent, activeTrackColor = Volt.accent, inactiveTrackColor = Volt.surface2),
        )
        VoltTextField(target, { v -> target = v.filter { it.isDigit() }.take(3) }, "Kcal (200-900)", keyboardType = KeyboardType.Number)
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            baseSpecialDish.forEach { (name, _) ->
                Column(Modifier.weight(1f).clip(RoundedCornerShape(14.dp)).background(Volt.surface2).padding(8.dp)) {
                    Text(name, color = Volt.textDim, fontSize = 9.sp, maxLines = 1)
                    CountUpText((100 * scale).toFloat(), suffix = "g", style = androidx.compose.ui.text.TextStyle(color = Volt.text, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Volt.surface2.copy(alpha = 0.6f)).padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Equivalencias rápidas por macros", color = Volt.text, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Switch(
                    checked = quick, onCheckedChange = { quick = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = Volt.accent, checkedThumbColor = Color.Black, uncheckedTrackColor = Volt.surface, uncheckedBorderColor = Volt.border),
                )
            }
            AnimatedVisibility(quick, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                Column(Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    QuickRow("+25g proteína:", ProteinColor, "+120g pollo o +1 scoop whey")
                    QuickRow("+30g carbos:", CarbsColor, "+45g avena o +130g arroz")
                    QuickRow("+10g grasas:", FatColor, "+15g frutos secos o +12g aceite")
                    Text(
                        "P ${Math.round(diet.macros.protein * 100f / totalMacros)}% · C ${Math.round(diet.macros.carbs * 100f / totalMacros)}% · G ${Math.round(diet.macros.fat * 100f / totalMacros)}%",
                        color = Volt.text, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Volt.accent.copy(alpha = 0.12f)).padding(8.dp),
                    )
                }
            }
            if (!quick) Text("Activa el switch para ver reemplazos rápidos según macronutrientes.", color = Volt.textDim, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
        }
        Spacer(Modifier.height(12.dp))
        PrimaryButton("Guardar como mi plato especial", onClick = {
            scope.launch {
                repo.saveProfile { it.copy(profile = it.profile.copy(specialDish = SpecialDish("arroz + lentejas + tomate + queso feta", targetKcal))) }
                repo.toast("success", "Plato especial guardado", "$targetKcal kcal")
            }
        })
    }
}

@Composable
private fun QuickRow(label: String, color: Color, text: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(Color.White.copy(alpha = 0.04f)).padding(8.dp)) {
        Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(6.dp))
        Text(text, color = Volt.text, fontSize = 11.sp)
    }
}

private fun parseList(s: String): List<String> = s.split(',', ';', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(30)

/** Preferencias de comida: vista y edicion (verduras, carbos, proteinas). */
@Composable
fun FoodPreferencesCard(repo: Repository, prefs: FoodPreferences, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var editing by remember { mutableStateOf(false) }
    var veg by remember(prefs) { mutableStateOf(prefs.vegetables.joinToString(", ")) }
    var carbs by remember(prefs) { mutableStateOf(prefs.carbs.joinToString(", ")) }
    var prot by remember(prefs) { mutableStateOf(prefs.proteins.joinToString(", ")) }

    VoltCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🥘 Preferencias Nutricionales", color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, modifier = Modifier.weight(1f))
            Text(
                if (editing) "Guardar" else "Editar", color = Volt.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.clip(RoundedCornerShape(50)).pressScale {
                    if (editing) {
                        scope.launch {
                            repo.saveProfile { it.copy(profile = it.profile.copy(foodPreferences = FoodPreferences(parseList(veg), parseList(carbs), parseList(prot)))) }
                            repo.toast("success", "Preferencias guardadas", "Se usarán al cambiar o regenerar comidas.")
                        }
                    }
                    editing = !editing
                }.padding(horizontal = 10.dp, vertical = 6.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        AnimatedVisibility(!editing) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PrefRow("🥦 Verduras:", prefs.vegetables.joinToString(", ").ifBlank { "Variadas" })
                PrefRow("🍚 Carbos:", prefs.carbs.joinToString(", ").ifBlank { "Complejos" })
                PrefRow("🥩 Proteínas:", prefs.proteins.joinToString(", ").ifBlank { "Magras" })
            }
        }
        AnimatedVisibility(editing, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Separa los alimentos con comas.", color = Volt.textDim, fontSize = 11.sp)
                VoltTextField(veg, { veg = it }, "Verduras", singleLine = false)
                VoltTextField(carbs, { carbs = it }, "Carbohidratos", singleLine = false)
                VoltTextField(prot, { prot = it }, "Proteínas", singleLine = false)
            }
        }
    }
}

@Composable
private fun PrefRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Volt.surface2.copy(alpha = 0.6f)).padding(10.dp)) {
        Text(label, color = Volt.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.width(8.dp))
        Text(value, color = Volt.text, fontSize = 12.sp, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.End)
    }
}

/** Regenerar plan de dieta con IA (con confirmacion). */
@Composable
fun RegenerateCard(repo: Repository, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var confirm by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    VoltCard(modifier.fillMaxWidth()) {
        SectionTitle("✨ Regenerar plan nutricional")
        Text(
            "La IA creará una dieta nueva con tus datos y preferencias actuales. Tu rutina de entrenamiento no cambia.",
            color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        PrimaryButton("Regenerar dieta", onClick = { confirm = true }, loading = loading)
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            containerColor = Volt.surface,
            title = { Text("¿Regenerar tu dieta?", color = Volt.text, fontWeight = FontWeight.Bold) },
            text = { Text("Se sustituirán todas las comidas actuales por un plan nuevo y se reiniciarán las comidas marcadas de hoy.", color = Volt.textDim) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = false
                    scope.launch { loading = true; regenerateDiet(repo); loading = false }
                }) { Text("Regenerar", color = Volt.accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text("Cancelar", color = Volt.textDim) } },
        )
    }
}

/** Genera un plan nuevo y reemplaza SOLO la dieta (y la frase/insights del plan), y conservando el resto de datos. */
suspend fun regenerateDiet(repo: Repository): Boolean {
    val s = repo.state.value
    val p = s.profile
    if ((p.age ?: 0) !in 12..100 || (p.weight ?: 0.0) !in 30.0..350.0 || (p.height ?: 0.0) !in 120.0..250.0) {
        repo.toast("error", "Datos incompletos", "Revisa edad (12-100), peso (30-350) y estatura (120-250) en tu perfil.")
        return false
    }
    return try {
        val plan = repo.generatePlan(p.copy(name = p.name ?: s.user?.name ?: "Usuario"))
        if (plan.diet.meals.isEmpty()) {
            repo.toast("error", "No se pudo generar la dieta", plan.details ?: "Inténtalo de nuevo en un momento.")
            return false
        }
        repo.saveProfile(withPlan = true) { st ->
            st.copy(diet = plan.diet, insights = plan.insights)
        }
        val today = LocalDate.now().toString()
        s.mealEaten[today].orEmpty().forEach { repo.setMealEaten(today, it, false) }
        if (plan.fallback) repo.toast("info", "Plan básico generado", "La IA no estuvo disponible; se usó un plan de respaldo.")
        else repo.toast("success", "Nueva dieta lista", "${plan.diet.dailyCalories} kcal diarias")
        true
    } catch (e: Exception) {
        repo.toast("error", "No se pudo generar la dieta", e.message)
        false
    }
}
