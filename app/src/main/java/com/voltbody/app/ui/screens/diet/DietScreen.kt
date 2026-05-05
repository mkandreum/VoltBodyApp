package com.voltbody.app.ui.screens.diet

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
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
import com.voltbody.app.ui.theme.*
import com.voltbody.app.util.HapticType
import com.voltbody.app.util.rememberHaptic
import dev.chrisbanes.haze.HazeState
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DietScreen(
    viewModel: DietViewModel = hiltViewModel()
) {
    val vb = LocalVoltBodyColors.current
    val uiState by viewModel.uiState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LiquidGlassScaffold(
        background = {
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
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 70.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                uiState.diet?.let { diet ->
                    item {
                        StaggeredEntrance(0) {
                            DietHeader(targetCalories = diet.dailyCalories)
                        }
                    }

                    item {
                        val eatenCount = uiState.eatenMealIds.size
                        val totalMeals = diet.meals.size
                        val macroBalance = if (diet.dailyCalories > 0) {
                            ((diet.macros.protein * 4 + diet.macros.carbs * 4 + diet.macros.fat * 9).toFloat() / diet.dailyCalories * 100).toInt()
                        } else 100
                        val dailyCompliance = (((eatenCount.toFloat() / totalMeals.coerceAtLeast(1)) * 55) + ((macroBalance.toFloat() / 100) * 45)).toInt().coerceIn(0, 100)

                        StaggeredEntrance(1) {
                            NutritionalSummaryCard(
                                diet = diet,
                                eatenCount = eatenCount,
                                compliance = dailyCompliance,
                                hazeState = hazeState
                            )
                        }
                    }

                    item {
                        StaggeredEntrance(2) {
                            MacrosGrid(diet.macros)
                        }
                    }

                    itemsIndexed(diet.meals, key = { _, m -> m.id }) { index, meal ->
                        StaggeredEntrance(index + 3) {
                            MealTile(
                                meal = meal,
                                isEaten = uiState.eatenMealIds.contains(meal.id),
                                isSwapping = uiState.swappingMealId == meal.id,
                                onToggleEaten = { viewModel.toggleMealEaten(meal.id) },
                                onSwap = { viewModel.swapMeal(meal) },
                                hazeState = hazeState
                            )
                        }
                    }

                    uiState.foodPreferences?.let { prefs ->
                        item {
                            StaggeredEntrance(diet.meals.size + 4) {
                                FoodPreferencesCard(prefs)
                            }
                        }
                    }

                    item {
                        StaggeredEntrance(diet.meals.size + 5) {
                            SpecialDishCard()
                        }
                    }
                } ?: item {
                    NoDietView()
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun DietHeader(targetCalories: Int) {
    val vb = LocalVoltBodyColors.current
    Column(modifier = Modifier.padding(bottom = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Icon(Icons.Default.Restaurant, contentDescription = null, tint = vb.accent, modifier = Modifier.size(32.dp))
            HeadlineGradient("🍽️ TU DIETA", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black))
        }
        Text("Objetivo diario: $targetCalories kcal", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoMetric.fontFamily), color = vb.textMuted, modifier = Modifier.padding(start = 44.dp))
    }
}

@Composable
private fun NutritionalSummaryCard(
    diet: DietPlan,
    eatenCount: Int,
    compliance: Int,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("📊 RESUMEN NUTRICIONAL", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                Text("🍏 HOY COMES PARA RENDIR", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = vb.accent)
                Text("${diet.meals.size} comidas planificadas con foco en energía estable.", style = MaterialTheme.typography.bodySmall, color = vb.textMuted, modifier = Modifier.padding(top = 4.dp))
            }
            Icon(Icons.Default.AutoAwesome, null, tint = vb.accent, modifier = Modifier.size(24.dp))
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatPillSmall("KCAL", "${diet.dailyCalories}")
            StatPillSmall("COMIDAS", "${diet.meals.size}")
            StatPillSmall("COMPLETADAS", "$eatenCount/${diet.meals.size}")
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(vb.surfaceElevated.copy(0.3f)).padding(12.dp)) {
            Column {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cumplimiento diario", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = vb.textMuted)
                    Text("$compliance%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Black), color = vb.accent)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LiquidProgressBar(progress = compliance / 100f, height = 6.dp)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        Text("💡 Tip: si entrenas intenso hoy, prioriza proteína + carbohidrato en la comida post-entreno.", style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp), color = vb.textMuted)
    }
}

@Composable
private fun MacrosGrid(macros: Macros) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MacroCard("PROTEÍNA", "${macros.protein}g", Icons.Default.Restaurant, Color(0xFFF87171), modifier = Modifier.weight(1f))
        MacroCard("CARBOS", "${macros.carbs}g", Icons.Default.WheatDiet, Color(0xFFFBBF24), modifier = Modifier.weight(1f))
        MacroCard("GRASAS", "${macros.fat}g", Icons.Default.WaterDrop, Color(0xFF38BDF8), modifier = Modifier.weight(1f))
    }
}

@Composable
private fun MacroCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier) {
    val vb = LocalVoltBodyColors.current
    AppCard(modifier = modifier) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
            Text(label, style = UppercaseLabel.copy(fontSize = 8.sp), color = vb.textMuted)
        }
    }
}

@Composable
private fun MealTile(
    meal: Meal,
    isEaten: Boolean,
    isSwapping: Boolean,
    onToggleEaten: () -> Unit,
    onSwap: () -> Unit,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    val haptic = rememberHaptic()
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(if (isEaten) vb.accent.copy(0.05f) else vb.surfaceElevated.copy(0.2f))
            .border(1.dp, if (isEaten) vb.accent.copy(0.3f) else vb.border, RoundedCornerShape(24.dp))
            .padding(16.dp)
    ) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.Top, modifier = Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    IconButton(
                        onClick = { 
                            haptic.perform(HapticType.TICK)
                            onToggleEaten() 
                        },
                        modifier = Modifier.size(24.dp).padding(top = 2.dp)
                    ) {
                        Icon(
                            if (isEaten) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                            contentDescription = null,
                            tint = if (isEaten) vb.accent else vb.textMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            meal.name.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, textDecoration = if (isEaten) androidx.compose.ui.text.style.TextDecoration.LineThrough else null),
                            color = if (isEaten) vb.textMuted else ColorWhite
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(meal.time, style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoMetric.fontFamily, fontSize = 10.sp), color = vb.textMuted, modifier = Modifier.clip(RoundedCornerShape(4.dp)).background(vb.border).padding(horizontal = 6.dp, vertical = 2.dp))
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Icon(Icons.Default.LocalFireDepartment, null, tint = vb.accent, modifier = Modifier.size(14.dp))
                                Text("${meal.calories}", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoMetric.fontFamily, fontWeight = FontWeight.Bold), color = vb.accent)
                            }
                        }
                    }
                }
                
                IconButton(
                    onClick = onSwap,
                    modifier = Modifier.size(36.dp).neuroRaised(cornerRadius = 18.dp)
                ) {
                    if (isSwapping) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = vb.accent, strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = vb.textMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }
            
            if (meal.description.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(meal.description, style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MacroPill("P", "${meal.protein}g")
                MacroPill("C", "${meal.carbs}g")
                MacroPill("G", "${meal.fat}g")
            }
        }
    }
}

@Composable
private fun MacroPill(label: String, value: String) {
    Text("$label: $value", style = MaterialTheme.typography.labelSmall.copy(fontFamily = MonoMetric.fontFamily, fontSize = 10.sp), color = LocalVoltBodyColors.current.textMuted)
}

@Composable
private fun StatPillSmall(label: String, value: String) {
    val vb = LocalVoltBodyColors.current
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(label, style = UppercaseLabel.copy(fontSize = 8.sp), color = vb.textMuted)
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
    }
}

@Composable
private fun FoodPreferencesCard(prefs: FoodPreferences) {
    val vb = LocalVoltBodyColors.current
    AppCard(modifier = Modifier.fillMaxWidth()) {
        Text("🥘 PREFERENCIAS PARA TU DIETA", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
        Spacer(modifier = Modifier.height(12.dp))
        PreferenceRow("Verduras", prefs.vegetables.joinToString(", "))
        PreferenceRow("Carbohidratos", prefs.carbs.joinToString(", "))
        PreferenceRow("Proteínas", prefs.proteins.joinToString(", "))
    }
}

@Composable
private fun PreferenceRow(label: String, value: String) {
    Row(modifier = Modifier.padding(vertical = 2.dp)) {
        Text("$label: ", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = LocalVoltBodyColors.current.textMuted)
        Text(if (value.isBlank()) "No definidas" else value, style = MaterialTheme.typography.labelSmall, color = ColorWhite)
    }
}

@Composable
private fun SpecialDishCard() {
    val vb = LocalVoltBodyColors.current
    AppCard(modifier = Modifier.fillMaxWidth(), accent = true) {
        Text("🍲 PLATO ESPECIAL AJUSTABLE", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Base: arroz + lentejas + tomate + queso feta", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
        Spacer(modifier = Modifier.height(12.dp))
        Text("Calculadora integrada disponible en la versión web para ajustes precisos por gramaje.", style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
    }
}

@Composable
private fun NoDietView() {
    Box(modifier = Modifier.fillMaxWidth().padding(vertical = 80.dp), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("🥗", fontSize = 48.sp)
            Spacer(modifier = Modifier.height(16.dp))
            Text("Sin plan nutricional", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = ColorWhite)
            Text("Completa el onboarding para que la IA genere tu dieta personalizada.", style = MaterialTheme.typography.bodyMedium, color = LocalVoltBodyColors.current.textMuted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
        }
    }
}
