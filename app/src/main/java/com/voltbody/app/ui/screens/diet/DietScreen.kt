package com.voltbody.app.ui.screens.diet

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voltbody.app.domain.model.*
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.screens.diet.components.FoodPreferencesCard
import com.voltbody.app.ui.screens.diet.components.MacroTipsCard
import com.voltbody.app.ui.screens.diet.components.SpecialDishCalculator
import com.voltbody.app.ui.theme.*
import com.voltbody.app.util.HapticType
import com.voltbody.app.util.perform
import com.voltbody.app.util.rememberHaptic
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

// Colour helpers for remaining calories
private fun remainingColor(remaining: Int, target: Int): Color = when {
    target <= 0 -> ColorInfo
    remaining < 0 -> ColorError           // over target
    remaining.toFloat() / target < 0.1f -> Color(0xFFFBBF24)  // amber — <10% left
    else -> ColorSuccess                   // on track
}

@Composable
fun DietScreen(
    viewModel: DietViewModel = hiltViewModel()
) {
    val vb = LocalVoltBodyColors.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 60.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                StaggeredEntrance(0) {
                    Text(
                        "Plan Nutricional",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
                        color = ColorWhite
                    )
                }
            }

            // ── Date navigation ───────────────────────────────────────────
            item {
                StaggeredEntrance(1) {
                    DateNavigationBar(
                        selectedDate = uiState.selectedDate,
                        isToday = uiState.isToday,
                        onPrevious = viewModel::goToPreviousDay,
                        onNext = viewModel::goToNextDay,
                        onToday = viewModel::goToToday
                    )
                }
            }

            // ── Macros summary ────────────────────────────────────────────
            uiState.diet?.let { diet ->
                item {
                    StaggeredEntrance(2) {
                        MacrosSummaryCard(
                            diet = diet,
                            eatenCalories = uiState.eatenCalories,
                            remainingCalories = uiState.remainingCalories,
                            totalMeals = diet.meals.size,
                            eatenMeals = uiState.eatenMealIds.size,
                            eatenProtein = uiState.eatenProtein,
                            eatenCarbs = uiState.eatenCarbs,
                            eatenFat = uiState.eatenFat
                        )
                    }
                }

                item {
                    StaggeredEntrance(3) {
                        MacroTipsCard()
                    }
                }

                // ── Macro Quick Mode toggle ──────────────────────────────
                item {
                    StaggeredEntrance(4) {
                        MacroQuickModeToggle(
                            enabled = uiState.isMacroQuickMode,
                            onToggle = viewModel::toggleMacroQuickMode
                        )
                    }
                }

                if (uiState.isMacroQuickMode) {
                    item {
                        StaggeredEntrance(5) {
                            MacroQuickTable()
                        }
                    }
                }

                // ── Meals ────────────────────────────────────────────────
                itemsIndexed(diet.meals, key = { _, it -> it.id }) { index, meal ->

                    val isEaten = uiState.eatenMealIds.contains(meal.id)
                    StaggeredEntrance(index + 4) {
                        MealCard(
                            meal = meal,
                            isEaten = isEaten,
                            isSwapping = uiState.swappingMealId == meal.id,
                            onToggleEaten = { viewModel.toggleMealEaten(meal.id) },
                            onSwap = { viewModel.swapMeal(meal) }
                        )
                    }
                }

                item {
                    StaggeredEntrance(diet.meals.size + 4) {
                        SpecialDishCalculator()
                    }
                }
                
                uiState.foodPreferences?.let { prefs ->
                    item {
                        StaggeredEntrance(diet.meals.size + 5) {
                            FoodPreferencesCard(preferences = prefs)
                        }
                    }
                }
            } ?: item {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🥗", fontSize = 40.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No tienes plan nutricional", style = MaterialTheme.typography.titleMedium, color = vb.textMuted, textAlign = TextAlign.Center)
                    }
                }
            }

            // ── Hydration tracker ──────────────────────────────────────────
            item {
                StaggeredEntrance(10) { // arbitrary higher index to ensure it follows
                    HydrationCard(
                        glassCount = uiState.waterGlasses,
                        onAddGlass = viewModel::addWaterGlass,
                        onRemoveGlass = viewModel::removeWaterGlass
                    )
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────
// Components
// ──────────────────────────────────────────────────────────────────────────

/** Prev/Next date navigator, blocks future dates. */
@Composable
private fun DateNavigationBar(
    selectedDate: LocalDate,
    isToday: Boolean,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    val formatter = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale("es")) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(vb.surface)
            .border(1.dp, vb.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious, modifier = Modifier.size(36.dp)) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = "Día anterior", tint = vb.textMuted)
        }
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable(enabled = !isToday) { onToday() }
        ) {
            Text(
                text = if (isToday) "Hoy" else selectedDate.format(formatter).replaceFirstChar { it.uppercase() },
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (isToday) vb.accent else ColorWhite
            )
            if (!isToday) {
                Text("Toca para volver a hoy", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
            }
        }
        IconButton(
            onClick = onNext,
            enabled = !isToday,
            modifier = Modifier.size(36.dp)
        ) {
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = "Día siguiente",
                tint = if (isToday) vb.border else vb.textMuted
            )
        }
    }
}

/**
 * Macros summary card.
 * Shows animated progress bars for each macro (eaten vs target).
 * Shows calories eaten / target + remaining counter.
 */
@Composable
private fun MacrosSummaryCard(
    diet: DietPlan,
    eatenCalories: Int,
    remainingCalories: Int,
    totalMeals: Int,
    eatenMeals: Int,
    eatenProtein: Int,
    eatenCarbs: Int,
    eatenFat: Int
) {
    val vb = LocalVoltBodyColors.current
    val remainColor = remainingColor(remainingCalories, diet.dailyCalories)

    AppCard {
        // ── Calories row ───────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Calorías consumidas", style = UppercaseLabel, color = vb.textMuted)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("$eatenCalories", style = MonoMetric, color = vb.accent)
                    Text("/ ${diet.dailyCalories} kcal", style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
                }
            }
            // Remaining badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (remainingCalories >= 0) "Quedan" else "Exceso",
                    style = UppercaseLabel,
                    color = vb.textMuted
                )
                Text(
                    "${if (remainingCalories < 0) "+" else ""}${kotlin.math.abs(remainingCalories)} kcal",
                    style = MonoMetric.copy(fontSize = 18.sp),
                    color = remainColor
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Calories global bar
        val calProgress by animateFloatAsState(
            targetValue = if (diet.dailyCalories > 0)
                (eatenCalories.toFloat() / diet.dailyCalories).coerceIn(0f, 1f)
            else 0f,
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            label = "cal_progress"
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(CircleShape)
                .background(vb.border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(calProgress)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            listOf(vb.accent, remainColor)
                        )
                    )
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        AccentDivider()
        Spacer(modifier = Modifier.height(12.dp))

        // ── Macro progress bars ───────────────────────────────────────
        Text("Macronutrientes", style = UppercaseLabel, color = vb.textMuted)
        Spacer(modifier = Modifier.height(8.dp))
        listOf(
            MacroRowData("Prot", eatenProtein, diet.macros.protein, ColorInfo),
            MacroRowData("HC", eatenCarbs, diet.macros.carbs, ColorWarning),
            MacroRowData("Grasa", eatenFat, diet.macros.fat, ColorError)
        ).forEach { macro ->
            MacroProgressBar(macro)
            Spacer(modifier = Modifier.height(6.dp))
        }

        Spacer(modifier = Modifier.height(4.dp))
        // Meals summary chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonBadge("$eatenMeals / $totalMeals comidas")
        }
    }
}

private data class MacroRowData(val label: String, val eaten: Int, val target: Int, val color: Color)

@Composable
private fun MacroProgressBar(data: MacroRowData) {
    val vb = LocalVoltBodyColors.current
    val fraction = if (data.target > 0)
        (data.eaten.toFloat() / data.target).coerceIn(0f, 1f)
    else 0f
    val animFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "macro_${data.label}"
    )
    val isOver = data.eaten > data.target

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // label
        Text(
            data.label,
            style = UppercaseLabel,
            color = vb.textMuted,
            modifier = Modifier.width(40.dp)
        )
        // progress track
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(CircleShape)
                .background(vb.border)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animFraction)
                    .clip(CircleShape)
                    .background(if (isOver) ColorError else data.color)
            )
        }
        // eaten / target
        Text(
            "${data.eaten}/${data.target}g",
            style = UppercaseLabel,
            color = if (isOver) ColorError else data.color,
            modifier = Modifier.width(72.dp),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun MealCard(
    meal: Meal,
    isEaten: Boolean,
    isSwapping: Boolean,
    onToggleEaten: () -> Unit,
    onSwap: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    val haptic = rememberHaptic()
    var expanded by remember { mutableStateOf(false) }

    val checkScale by animateFloatAsState(
        targetValue = if (isEaten) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "meal_check"
    )

    AppCard(onClick = { expanded = !expanded }) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            // Eaten toggle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .graphicsLayer { scaleX = checkScale; scaleY = checkScale }
                    .clip(CircleShape)
                    .background(if (isEaten) ColorSuccess.copy(0.15f) else vb.surface)
                    .border(1.dp, if (isEaten) ColorSuccess.copy(0.5f) else vb.border, CircleShape)
                    .clickable { 
                        haptic.perform(HapticType.TICK)
                        onToggleEaten() 
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isEaten) Icons.Filled.Check else Icons.Outlined.RestaurantMenu,
                    contentDescription = null,
                    tint = if (isEaten) ColorSuccess else vb.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            meal.name,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isEaten) vb.textMuted else ColorWhite
                        )
                        Text(meal.time, style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("${meal.calories} kcal", style = MaterialTheme.typography.labelLarge, color = vb.accent)
                        Text(
                            "P:${meal.protein}g · HC:${meal.carbs}g · G:${meal.fat}g",
                            style = MaterialTheme.typography.labelSmall,
                            color = vb.textMuted
                        )
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (meal.description.isNotBlank()) {
                            Text(meal.description, style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
                        }
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            if (isSwapping) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = vb.accent, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generando alternativa...", style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
                            } else {
                                OutlinedButton(
                                    onClick = onSwap,
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, vb.accent.copy(0.4f)),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Icon(Icons.Outlined.AutoAwesome, contentDescription = null, modifier = Modifier.size(14.dp), tint = vb.accent)
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Alternativa IA", style = MaterialTheme.typography.labelMedium, color = vb.accent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HydrationCard(glassCount: Int, onAddGlass: () -> Unit, onRemoveGlass: () -> Unit) {
    val vb = LocalVoltBodyColors.current
    val targetGlasses = 8
    AppCard {
        SectionHeader(title = "💧 Hidratación")
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("$glassCount / $targetGlasses vasos", style = MaterialTheme.typography.titleSmall, color = ColorWhite)
                Text("${glassCount * 250} ml de 2000 ml", style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onRemoveGlass,
                    enabled = glassCount > 0,
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(vb.surface)
                ) { Text("−", color = vb.textMuted) }
                IconButton(
                    onClick = onAddGlass,
                    enabled = glassCount < 16,
                    modifier = Modifier.size(36.dp).clip(CircleShape)
                        .background(vb.accent.copy(0.15f))
                        .border(1.dp, vb.accent.copy(0.4f), CircleShape)
                ) { Text("+", color = vb.accent, fontWeight = FontWeight.Black) }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        // Hydration progress bar
        val hydrationProgress by animateFloatAsState(
            targetValue = (glassCount.toFloat() / targetGlasses).coerceIn(0f, 1f),
            animationSpec = tween(400),
            label = "hydration"
        )
        Box(
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(vb.border)
        ) {
            Box(
                modifier = Modifier.fillMaxHeight().fillMaxWidth(hydrationProgress).clip(CircleShape).background(ColorInfo)
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            repeat(targetGlasses) { i ->
                val filled = i < glassCount
                val scale by animateFloatAsState(
                    targetValue = if (filled) 1f else 0.85f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
                    label = "glass_$i"
                )
                Box(
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (filled) ColorInfo else vb.border)
                )
            }
        }
    }
}
    }
}

@Composable
private fun MacroQuickModeToggle(enabled: Boolean, onToggle: () -> Unit) {
    val vb = LocalVoltBodyColors.current
    AppCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier.size(36.dp).clip(CircleShape).background(vb.accent.copy(0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Bolt, null, tint = vb.accent, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text("Modo Rápido de Macros", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    Text("Equivalencias de alimentos", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                }
            }
            Switch(
                checked = enabled,
                onCheckedChange = { onToggle() },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = vb.accent,
                    checkedTrackColor = vb.accent.copy(0.3f)
                )
            )
        }
    }
}

@Composable
private fun MacroQuickTable() {
    val vb = LocalVoltBodyColors.current
    AppCard {
        SectionHeader(title = "⚡ Equivalencias Rápidas")
        Spacer(modifier = Modifier.height(12.dp))
        
        QuickMacroItem("Proteína (25g)", "🍗 120g Pollo/Pavo\n🐟 140g Atún/Pescado\n🍳 150g Claras\n🥤 1 scoop Proteína", ColorInfo)
        Spacer(modifier = Modifier.height(12.dp))
        QuickMacroItem("Hidratos (50g)", "🍚 200g Arroz cocido\n🍝 250g Pasta cocida\n🥔 300g Patata\n🥣 60g Avena", ColorWarning)
        Spacer(modifier = Modifier.height(12.dp))
        QuickMacroItem("Grasas (15g)", "🥜 30g Nueces\n🥑 1/2 Aguacate\n🫒 1.5 cdas Aceite Oliva", ColorError)
    }
}

@Composable
private fun QuickMacroItem(title: String, details: String, color: Color) {
    val vb = LocalVoltBodyColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.05f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(12.dp)
    ) {
        Text(title, style = UppercaseLabel, color = color)
        Spacer(modifier = Modifier.height(4.dp))
        Text(details, style = MaterialTheme.typography.bodySmall, color = ColorWhite, lineHeight = 20.sp)
    }
}
