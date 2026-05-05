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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import dev.chrisbanes.haze.HazeState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
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

    LiquidGlassScaffold(
        background = {
            // "Healthy" background with green/teal splashes for diet context
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.size(350.dp).align(Alignment.TopStart).background(ColorSuccess.copy(0.12f), CircleShape).offset((-60).dp, (-60).dp))
                Box(modifier = Modifier.size(300.dp).align(Alignment.CenterEnd).background(ColorInfo.copy(0.08f), CircleShape).offset(80.dp, 100.dp))
            }
        }
    ) { hazeState ->
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
                    HeadlineGradient(
                        "Plan Nutricional",
                        style = MaterialTheme.typography.headlineMedium
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
                        onToday = viewModel::goToToday,
                        hazeState = hazeState
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
                            eatenFat = uiState.eatenFat,
                            hazeState = hazeState
                        )
                    }
                }

                item {
                    StaggeredEntrance(3) {
                        MacroTipsCard() // TODO: Update internal LiquidGlassCard to use hazeState if needed
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
                itemsIndexed(diet.meals, key = { _, it -> it.id }) { index: Int, meal: com.voltbody.app.domain.model.Meal ->

                    val isEaten = uiState.eatenMealIds.contains(meal.id)
                    StaggeredEntrance(index + 4) {
                        MealCard(
                            meal = meal,
                            isEaten = isEaten,
                            isSwapping = uiState.swappingMealId == meal.id,
                            onToggleEaten = { viewModel.toggleMealEaten(meal.id) },
                            onSwap = { viewModel.swapMeal(meal) },
                            hazeState = hazeState
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
                        onRemoveGlass = viewModel::removeWaterGlass,
                        hazeState = hazeState
                    )
                }
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
    onToday: () -> Unit,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    val formatter = remember { DateTimeFormatter.ofPattern("EEE d MMM", Locale("es")) }
    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        hazeState = hazeState,
        glassAlpha = 0.2f
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(
                onClick = onPrevious, 
                modifier = Modifier.size(36.dp).neuroRaised(cornerRadius = 18.dp)
            ) {
                Icon(Icons.Filled.ChevronLeft, contentDescription = "Día anterior", tint = ColorWhite, modifier = Modifier.size(20.dp))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.clickable(enabled = !isToday) { onToday() }
            ) {
                Text(
                    text = if (isToday) "HOY" else selectedDate.format(formatter).uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                    color = if (isToday) vb.accent else ColorWhite
                )
                if (!isToday) {
                    Text("VOLVER A HOY", style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold), color = vb.textMuted)
                }
            }
            IconButton(
                onClick = onNext,
                enabled = !isToday,
                modifier = Modifier.size(36.dp).neuroRaised(cornerRadius = 18.dp)
            ) {
                Icon(
                    Icons.Filled.ChevronRight,
                    contentDescription = "Día siguiente",
                    tint = if (isToday) vb.textMuted.copy(alpha = 0.3f) else ColorWhite,
                    modifier = Modifier.size(20.dp)
                )
            }
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
    eatenFat: Int,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    val remainColor = remainingColor(remainingCalories, diet.dailyCalories)

    LiquidGlassCard(hazeState = hazeState) {
        // ── Calories row ───────────────────────────────────────────
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "CALORÍAS CONSUMIDAS", 
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp), 
                    color = vb.textMuted
                )
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowText("$eatenCalories", style = MonoMetric.copy(fontSize = 24.sp, fontWeight = FontWeight.Black))
                    Text("/ ${diet.dailyCalories} KCAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = vb.textMuted, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            // Remaining badge
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    if (remainingCalories >= 0) "RESTANTES" else "EXCESO",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp),
                    color = vb.textMuted
                )
                Text(
                    "${if (remainingCalories < 0) "+" else ""}${kotlin.math.abs(remainingCalories)}",
                    style = MonoMetric.copy(fontSize = 20.sp, fontWeight = FontWeight.Black),
                    color = remainColor
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Calories global bar
        val calProgress by animateFloatAsState(
            targetValue = if (diet.dailyCalories > 0)
                (eatenCalories.toFloat() / diet.dailyCalories).coerceIn(0f, 1f)
            else 0f,
            animationSpec = tween(600, easing = FastOutSlowInEasing),
            label = "cal_progress"
        )
        LiquidProgressBar(progress = calProgress, height = 10.dp)

        Spacer(modifier = Modifier.height(24.dp))
        AccentDivider()
        Spacer(modifier = Modifier.height(16.dp))

        // ── Macro progress bars ───────────────────────────────────────
        Text(
            "MACRONUTRIENTES", 
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp), 
            color = ColorWhite
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            listOf(
                MacroRowData("PROTEÍNA", eatenProtein, diet.macros.protein, ColorInfo),
                MacroRowData("HIDRATOS", eatenCarbs, diet.macros.carbs, ColorWarning),
                MacroRowData("GRASAS", eatenFat, diet.macros.fat, ColorError)
            ).forEach { macro ->
                MacroProgressBar(macro)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        // Meals summary chips
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NeonBadge("$eatenMeals / $totalMeals COMIDAS")
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
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // label
        Text(
            data.label,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = vb.textMuted,
            modifier = Modifier.width(60.dp)
        )
        // progress track
        Box(modifier = Modifier.weight(1f)) {
            LiquidProgressBar(
                progress = animFraction,
                height = 6.dp
            )
        }
        // eaten / target
        Text(
            "${data.eaten}/${data.target}G",
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black),
            color = if (isOver) ColorError else ColorWhite,
            modifier = Modifier.width(80.dp),
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
    onSwap: () -> Unit,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    val haptic = rememberHaptic()
    var expanded by remember { mutableStateOf(false) }

    val checkScale by animateFloatAsState(
        targetValue = if (isEaten) 1.1f else 1f,
        animationSpec = spring(dampingRatio = 0.4f, stiffness = 500f),
        label = "meal_check"
    )

    LiquidGlassCard(
        onClick = { expanded = !expanded },
        accentGlow = isEaten,
        hazeState = hazeState
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Eaten toggle
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .graphicsLayer { scaleX = checkScale; scaleY = checkScale }
                    .neuroRaised(cornerRadius = 24.dp)
                    .clickable { 
                        haptic.perform(HapticType.TICK)
                        onToggleEaten() 
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    if (isEaten) Icons.Filled.Check else Icons.Outlined.RestaurantMenu,
                    contentDescription = null,
                    tint = if (isEaten) vb.accent else vb.textMuted,
                    modifier = Modifier.size(24.dp)
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
                            meal.name.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp),
                            color = if (isEaten) vb.accent else ColorWhite
                        )
                        Text(meal.time.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = vb.textMuted)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        GlowText("${meal.calories} KCAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
                        Text(
                            "P:${meal.protein}G · H:${meal.carbs}G · G:${meal.fat}G",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold),
                            color = vb.textMuted
                        )
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        if (meal.description.isNotBlank()) {
                            Text(meal.description, style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
                        }
                        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                            if (isSwapping) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = vb.accent, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("GENERANDO ALTERNATIVA...", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = vb.textMuted)
                            } else {
                                LiquidGlassButton(
                                    text = "ALTERNATIVA IA",
                                    onClick = onSwap,
                                    style = LiquidButtonStyle.Secondary,
                                    hazeState = hazeState,
                                    leadingIcon = { Icon(Icons.Outlined.AutoAwesome, null, tint = vb.accent, modifier = Modifier.size(16.dp)) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HydrationCard(glassCount: Int, onAddGlass: () -> Unit, onRemoveGlass: () -> Unit, hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    val targetGlasses = 8
    LiquidGlassCard(hazeState = hazeState) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(
                modifier = Modifier.size(44.dp).neuroRaised(cornerRadius = 22.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.WaterDrop, null, tint = ColorInfo, modifier = Modifier.size(20.dp))
            }
            Text(
                "HIDRATACIÓN DIARIA", 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp), 
                color = ColorWhite
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    GlowText("$glassCount", style = MonoMetric.copy(fontSize = 24.sp, fontWeight = FontWeight.Black))
                    Text("/ $targetGlasses VASOS", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = vb.textMuted, modifier = Modifier.padding(bottom = 4.dp))
                }
                Text("${glassCount * 250} ML DE 2000 ML", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = vb.textMuted)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                IconButton(
                    onClick = onRemoveGlass,
                    enabled = glassCount > 0,
                    modifier = Modifier.size(40.dp).neuroRaised(cornerRadius = 20.dp)
                ) { Icon(Icons.Default.Remove, null, tint = if (glassCount > 0) ColorWhite else vb.textMuted.copy(0.3f), modifier = Modifier.size(20.dp)) }
                IconButton(
                    onClick = onAddGlass,
                    enabled = glassCount < 16,
                    modifier = Modifier.size(40.dp).neuroRaised(cornerRadius = 20.dp)
                ) { Icon(Icons.Default.Add, null, tint = vb.accent, modifier = Modifier.size(20.dp)) }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        val hydrationProgress by animateFloatAsState(
            targetValue = (glassCount.toFloat() / targetGlasses).coerceIn(0f, 1f),
            animationSpec = tween(400),
            label = "hydration"
        )
        LiquidProgressBar(progress = hydrationProgress, height = 8.dp)
        
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
            repeat(targetGlasses) { i ->
                val filled = i < glassCount
                val scale by animateFloatAsState(
                    targetValue = if (filled) 1f else 0.85f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
                    label = "glass_$i"
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(20.dp)
                        .graphicsLayer { scaleX = scale; scaleY = scale }
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (filled) ColorInfo else vb.surfaceElevated.copy(0.4f))
                        .then(if (!filled) Modifier.border(1.dp, vb.border, RoundedCornerShape(6.dp)) else Modifier)
                )
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
