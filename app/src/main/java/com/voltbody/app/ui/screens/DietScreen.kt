package com.voltbody.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.Meal
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.*
import com.voltbody.app.ui.screens.diet.*
import com.voltbody.app.ui.theme.Volt
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DietScreen(repo: Repository) {
    val state by repo.state.collectAsState()
    val diet = state.diet
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val today = remember { LocalDate.now().toString() }
    var swappingId by remember { mutableStateOf<String?>(null) }
    var generating by remember { mutableStateOf(false) }

    if (diet == null || diet.meals.isEmpty()) {
        EmptyDiet(generating) {
            scope.launch { generating = true; regenerateDiet(repo); generating = false }
        }
        return
    }

    val eatenToday = state.mealEaten[today].orEmpty()
    val eatenMeals = diet.meals.filter { it.id in eatenToday }
    val consumedKcal = eatenMeals.sumOf { it.calories }
    val consumedP = eatenMeals.sumOf { it.protein }
    val consumedC = eatenMeals.sumOf { it.carbs }
    val consumedF = eatenMeals.sumOf { it.fat }
    val totalMeals = diet.meals.size
    val eatenCount = eatenMeals.size
    val compliance = if (totalMeals > 0) (eatenCount * 100f / totalMeals).toInt().coerceAtMost(100) else 0
    val completed = totalMeals > 0 && eatenCount == totalMeals

    fun toggle(id: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            val cur = repo.state.value.mealEaten[today].orEmpty()
            repo.setMealEaten(today, id, id !in cur)
        }
    }

    fun swap(meal: Meal) {
        if (swappingId != null) return
        scope.launch {
            swappingId = meal.id
            try {
                val alt = repo.alternativeMeal(meal)
                val newMeal = alt.copy(id = alt.id.ifBlank { meal.id }, time = alt.time.ifBlank { meal.time })
                repo.saveProfile(withPlan = true) { st ->
                    st.copy(diet = st.diet?.copy(meals = st.diet.meals.map { if (it.id == meal.id) newMeal else it }))
                }
                // si la comida estaba marcada y cambia de id, se traslada la marca
                if (newMeal.id != meal.id) {
                    if (meal.id in repo.state.value.mealEaten[today].orEmpty()) {
                        repo.setMealEaten(today, meal.id, false)
                        repo.setMealEaten(today, newMeal.id, true)
                    }
                }
                repo.toast("success", "Comida actualizada", "${meal.name} fue reemplazada por una alternativa equivalente.")
            } catch (e: Exception) {
                repo.toast("error", "No se pudo cambiar la comida", "Prueba nuevamente en un momento.")
            } finally {
                swappingId = null
            }
        }
    }

    val dateText = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("es", "ES")))
            .replaceFirstChar { it.uppercase() }
    }

    LazyColumn(
        Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 120.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Row(Modifier.staggeredEntry(0).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(dateText, color = Volt.textDim, fontSize = 12.sp)
                    Text("Tu Plan Nutricional", color = Volt.text, fontWeight = FontWeight.Black, fontSize = 26.sp)
                }
                Row(
                    Modifier.clip(RoundedCornerShape(16.dp)).background(Volt.surface).padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Filled.LocalFireDepartment, null, tint = Volt.orange, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("${diet.dailyCalories}", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text(" kcal meta", color = Volt.textDim, fontSize = 11.sp)
                }
            }
        }

        item {
            VoltCard(Modifier.staggeredEntry(1)) {
                Text("BALANCE NUTRICIONAL", color = Volt.textDim, fontSize = 10.sp, letterSpacing = 1.sp)
                Text("Progreso de hoy", color = Volt.text, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Spacer(Modifier.height(12.dp))
                NutriRings(
                    consumedKcal, diet.dailyCalories, consumedP, diet.macros.protein,
                    consumedC, diet.macros.carbs, consumedF, diet.macros.fat,
                )
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatPill("Comidas", "$eatenCount/$totalMeals", Modifier.weight(1f))
                    StatPill("Completado", "$compliance%", Modifier.weight(1f))
                    StatPill("Kcal meta", "${diet.dailyCalories}", Modifier.weight(1f), color = Volt.orange)
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White.copy(alpha = 0.04f)).padding(10.dp),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(Icons.Filled.Bolt, null, tint = Volt.accent, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        if (completed) "¡Excelente trabajo! Has completado todas las comidas planificadas para el día de hoy."
                        else "Si entrenas hoy, asegúrate de registrar tu comida post-entreno con suficiente proteína y carbohidratos.",
                        color = Volt.textDim, fontSize = 11.sp, lineHeight = 16.sp,
                    )
                }
            }
        }

        item {
            Row(Modifier.staggeredEntry(2).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MacroCard("Proteína", consumedP, diet.macros.protein, ProteinColor, Modifier.weight(1f))
                MacroCard("Carbos", consumedC, diet.macros.carbs, CarbsColor, Modifier.weight(1f))
                MacroCard("Grasas", consumedF, diet.macros.fat, FatColor, Modifier.weight(1f))
            }
        }

        item {
            AnimatedVisibility(
                completed,
                enter = fadeIn() + scaleIn(initialScale = 0.92f) + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) { CompletedBanner(totalMeals) }
        }

        item {
            Column(Modifier.staggeredEntry(3)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Comidas del día", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "$eatenCount de $totalMeals", color = Volt.textDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(Volt.surface2).padding(horizontal = 10.dp, vertical = 3.dp),
                    )
                }
                Text(
                    "Marca cada comida al consumirla o cámbiala con IA por una alternativa equivalente.",
                    color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp),
                )
            }
        }

        itemsIndexed(diet.meals, key = { i, m -> "$i-${m.id}" }) { index, meal ->
            MealCard(
                meal = meal, index = index, eaten = meal.id in eatenToday, swapping = swappingId == meal.id,
                onToggle = { toggle(meal.id) }, onSwap = { swap(meal) },
            )
        }

        item { WaterCard(repo, Modifier.staggeredEntry(4)) }
        item { SpecialDishCard(repo, diet, state.profile.specialDish, Modifier.staggeredEntry(5)) }
        item { FoodPreferencesCard(repo, state.profile.foodPreferences ?: com.voltbody.app.data.FoodPreferences(), Modifier.staggeredEntry(6)) }
        item { RegenerateCard(repo, Modifier.staggeredEntry(7)) }
    }
}

@Composable
private fun CompletedBanner(totalMeals: Int) {
    val pulse by rememberPulse(1f, 1.1f, 700)
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp))
            .background(Brush.horizontalGradient(listOf(Color(0xFF10B981).copy(alpha = 0.25f), Volt.accent.copy(alpha = 0.22f))))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(48.dp).scale(pulse).clip(RoundedCornerShape(16.dp)).background(Color(0xFF10B981).copy(alpha = 0.25f)),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.EmojiEvents, null, tint = Color(0xFF34D399), modifier = Modifier.size(26.dp)) }
        Spacer(Modifier.width(14.dp))
        Column {
            Text("¡Día 100% completado! 🎉", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(
                "Has alcanzado tus $totalMeals comidas planificadas hoy. ¡Excelente consistencia y disciplina nutricional!",
                color = Color(0xFF6EE7B7), fontSize = 12.sp, lineHeight = 17.sp,
            )
        }
    }
}

@Composable
private fun EmptyDiet(generating: Boolean, onGenerate: () -> Unit) {
    val t = rememberInfiniteTransition(label = "emptyFloat")
    val y by t.animateFloat(0f, -10f, infiniteRepeatable(tween(1300), RepeatMode.Reverse), label = "floatY")
    Column(
        Modifier.fillMaxSize().padding(horizontal = 28.dp).padding(bottom = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.offset(y = y.dp).size(72.dp).clip(RoundedCornerShape(22.dp)).background(Volt.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) { Text("🥗", fontSize = 34.sp) }
        Spacer(Modifier.height(18.dp))
        Text("Sin plan nutricional", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        Spacer(Modifier.height(8.dp))
        Text(
            "Completa tus datos en el perfil o genera ahora tu dieta hiper-personalizada con IA.",
            color = Volt.textDim, fontSize = 14.sp, lineHeight = 20.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton("Generar mi dieta", onClick = onGenerate, loading = generating)
    }
}
