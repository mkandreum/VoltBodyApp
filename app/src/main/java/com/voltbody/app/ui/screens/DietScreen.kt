package com.voltbody.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.FoodPreferences
import com.voltbody.app.data.Meal
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.*
import com.voltbody.app.ui.screens.diet.*
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DietScreen(repo: Repository) {
    val scope = rememberCoroutineScope()
    val haptic = LocalHapticFeedback.current
    val today = remember { LocalDate.now().toString() }

    // Flujos derivados: solo se recompone cuando cambia la parte que interesa.
    val diet by remember(repo) { repo.state.map { it.diet }.distinctUntilChanged() }.collectAsStateWithLifecycle(repo.state.value.diet)
    val eatenToday by remember(repo) { repo.state.map { it.mealEaten[today].orEmpty() }.distinctUntilChanged() }
        .collectAsStateWithLifecycle(repo.state.value.mealEaten[today].orEmpty())
    val specialDish by remember(repo) { repo.state.map { it.profile.specialDish }.distinctUntilChanged() }.collectAsStateWithLifecycle(repo.state.value.profile.specialDish)
    val foodPrefs by remember(repo) { repo.state.map { it.profile.foodPreferences }.distinctUntilChanged() }.collectAsStateWithLifecycle(repo.state.value.profile.foodPreferences)

    var swapMeal by remember { mutableStateOf<Meal?>(null) }
    var swapAlt by remember { mutableStateOf<Meal?>(null) }
    var swapLoading by remember { mutableStateOf(false) }
    var swapApplying by remember { mutableStateOf(false) }
    var generating by remember { mutableStateOf(false) }

    val plan = diet
    if (plan == null || plan.meals.isEmpty()) {
        EmptyDiet(generating) {
            scope.launch { generating = true; regenerateDiet(repo); generating = false }
        }
        return
    }

    // Agregados del dia (solo se recalculan si cambian plan o marcas).
    val totals = remember(plan, eatenToday) {
        val eaten = plan.meals.filter { it.id in eatenToday }
        intArrayOf(eaten.size, eaten.sumOf { it.calories }, eaten.sumOf { it.protein }, eaten.sumOf { it.carbs }, eaten.sumOf { it.fat })
    }
    val eatenCount = totals[0]
    val consumedKcal = totals[1]
    val totalMeals = plan.meals.size
    val completed = totalMeals > 0 && eatenCount == totalMeals

    fun toggle(id: String) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            val cur = repo.state.value.mealEaten[today].orEmpty()
            repo.setMealEaten(today, id, id !in cur)
        }
    }

    fun loadAlternative(meal: Meal) {
        scope.launch {
            swapLoading = true
            swapAlt = null
            try {
                val alt = repo.alternativeMeal(meal)
                swapAlt = alt.copy(id = alt.id.ifBlank { meal.id }, time = alt.time.ifBlank { meal.time })
            } catch (e: Exception) {
                swapMeal = null
                repo.toast("error", "No se pudo cambiar la comida", "Prueba nuevamente en un momento.")
            } finally {
                swapLoading = false
            }
        }
    }

    fun applySwap(meal: Meal, newMeal: Meal) {
        if (swapApplying) return
        scope.launch {
            swapApplying = true
            try {
                repo.saveProfile(withPlan = true) { st ->
                    st.copy(diet = st.diet?.copy(meals = st.diet.meals.map { if (it.id == meal.id) newMeal else it }))
                }
                // si la comida estaba marcada y cambia de id, se traslada la marca
                if (newMeal.id != meal.id && meal.id in repo.state.value.mealEaten[today].orEmpty()) {
                    repo.setMealEaten(today, meal.id, false)
                    repo.setMealEaten(today, newMeal.id, true)
                }
                repo.toast("success", "Comida actualizada", "${meal.name} fue reemplazada por una alternativa equivalente.")
                swapMeal = null
                swapAlt = null
            } catch (e: Exception) {
                repo.toast("error", "No se pudo cambiar la comida", "Prueba nuevamente en un momento.")
            } finally {
                swapApplying = false
            }
        }
    }

    val dateText = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, d MMMM", Locale("es", "ES")))
            .replaceFirstChar { it.uppercase() }
    }

    Box(Modifier.fillMaxSize().statusBarsPadding()) {
        MeshBackground(Modifier.fillMaxSize(), intensity = 0.45f)
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item(key = "hero", contentType = "hero") {
                BentoCard(Modifier.staggeredEntry(0), hero = true, padding = 24.dp) {
                    Text(dateText.uppercase(), color = Volt.textDim, style = VoltType.label)
                    Text("Tu plan de hoy", color = Volt.text, style = VoltType.h2, modifier = Modifier.padding(top = 2.dp))
                    Spacer(Modifier.height(12.dp))
                    Box(Modifier.glowBackdrop(alpha = 0.18f)) {
                        RollingNumber(plan.dailyCalories, style = VoltType.numberXL, suffix = "kcal al día", brush = Volt.gradient)
                    }
                    Text(
                        "$consumedKcal consumidas · $eatenCount de $totalMeals comidas",
                        color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(top = 4.dp, bottom = 20.dp),
                    )
                    NutriRings(
                        consumedKcal, plan.dailyCalories, totals[2], plan.macros.protein,
                        totals[3], plan.macros.carbs, totals[4], plan.macros.fat,
                    )
                }
            }

            item(key = "macros", contentType = "macros") {
                Row(Modifier.staggeredEntry(1).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    MacroCard("Proteína", totals[2], plan.macros.protein, ProteinColor, Modifier.weight(1f))
                    MacroCard("Carbos", totals[3], plan.macros.carbs, CarbsColor, Modifier.weight(1f))
                    MacroCard("Grasas", totals[4], plan.macros.fat, FatColor, Modifier.weight(1f))
                }
            }

            item(key = "done", contentType = "done") {
                AnimatedVisibility(
                    completed,
                    enter = fadeIn() + scaleIn(initialScale = 0.92f) + expandVertically(),
                    exit = fadeOut() + shrinkVertically(),
                ) { CompletedBanner(totalMeals) }
            }

            item(key = "mealsTitle", contentType = "title") {
                Column(Modifier.staggeredEntry(2)) {
                    SectionTitle("Comidas del día")
                    Text(
                        "Marca cada comida al consumirla o cámbiala con IA por una equivalente.",
                        color = Volt.textDim, style = VoltType.body,
                    )
                }
            }

            itemsIndexed(plan.meals, key = { i, m -> "$i-${m.id}" }, contentType = { _, _ -> "meal" }) { index, meal ->
                MealCard(
                    meal = meal, index = 3 + index, eaten = meal.id in eatenToday,
                    onToggle = { toggle(meal.id) },
                    onSwap = {
                        if (!swapLoading && swapMeal == null) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            swapMeal = meal
                            loadAlternative(meal)
                        }
                    },
                )
            }

            item(key = "water", contentType = "water") { WaterCard(repo, Modifier.staggeredEntry(2)) }
            item(key = "special", contentType = "special") { SpecialDishCard(repo, plan, specialDish, Modifier.staggeredEntry(2)) }
            item(key = "prefs", contentType = "prefs") { FoodPreferencesCard(repo, foodPrefs ?: FoodPreferences(), Modifier.staggeredEntry(2)) }
            item(key = "regen", contentType = "regen") { RegenerateCard(repo, Modifier.staggeredEntry(2)) }
        }
    }

    swapMeal?.let { m ->
        MealSwapSheet(
            old = m, alt = swapAlt, loading = swapLoading, applying = swapApplying,
            onApply = { swapAlt?.let { applySwap(m, it) } },
            onRetry = { loadAlternative(m) },
            onDismiss = { if (!swapApplying) { swapMeal = null; swapAlt = null } },
        )
    }
}

@Composable
private fun CompletedBanner(totalMeals: Int) {
    val pulse = rememberPulse(1f, 1.1f, 700)
    GradientBanner(
        title = "¡Día 100% completado!",
        subtitle = "Has cumplido tus $totalMeals comidas de hoy. Constancia de campeón.",
        icon = Icons.Filled.EmojiEvents,
        modifier = Modifier.graphicsLayer { scaleX = 1f + (pulse.value - 1f) * 0.1f; scaleY = scaleX },
    )
}

@Composable
private fun EmptyDiet(generating: Boolean, onGenerate: () -> Unit) {
    val pulse = rememberPulse(1f, 1.08f, 1200)
    val draw = remember { Animatable(0f) }
    LaunchedEffect(Unit) { draw.animateTo(1f, tween(900)) }
    Box(Modifier.fillMaxSize()) {
        MeshBackground(Modifier.fillMaxSize(), intensity = 0.55f)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 28.dp).padding(bottom = 100.dp),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
        ) {
            Box(
                Modifier.graphicsLayer { scaleX = pulse.value; scaleY = pulse.value; alpha = draw.value }
                    .size(88.dp).clip(CircleShape).background(Volt.gradient),
                contentAlignment = Alignment.Center,
            ) { Text("🥗", style = VoltType.h1) }
            Spacer(Modifier.height(24.dp))
            Text("Sin plan nutricional", color = Volt.text, style = VoltType.h1, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                "Completa tus datos en el perfil o genera ahora tu dieta hiper-personalizada con IA.",
                color = Volt.textDim, style = VoltType.body, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))
            PrimaryButton("Generar mi dieta", onClick = onGenerate, loading = generating)
        }
    }
}
