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
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voltbody.app.domain.model.Meal
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.theme.*
import com.voltbody.app.util.HapticType
import com.voltbody.app.util.rememberHaptic
import dev.chrisbanes.haze.HazeState

@Composable
fun DietScreen(
    viewModel: DietViewModel = hiltViewModel()
) {
    val vb = LocalVoltBodyColors.current
    val state by viewModel.uiState.collectAsState()
    val haptic = rememberHaptic()

    val eatenCount = state.eatenMealIds.size
    val totalMeals = state.diet?.meals?.size ?: 1
    val dailyCompliance = (eatenCount * 100) / totalMeals

    LiquidGlassScaffold { hazeState ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 60.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // 1. Header (Matching Web)
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.Restaurant, null, tint = vb.accent, modifier = Modifier.size(32.dp))
                    Column {
                        Text("🍽️ TU DIETA", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                        Text(
                            "OBJETIVO DIARIO: ${state.diet?.dailyCalories ?: 0} KCAL",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontMono = true),
                            color = vb.textMuted
                        )
                    }
                }
            }

            // 2. Resumen Nutricional
            item {
                StaggeredEntrance(1) {
                    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), accentGlow = true, hazeState = hazeState) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("📊 RESUMEN NUTRICIONAL", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                                Spacer(Modifier.height(4.dp))
                                HeadlineGradient(
                                    text = "🍏 HOY COMES PARA RENDIR",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${state.diet?.meals?.size ?: 0} COMIDAS PLANIFICADAS CON FOCO EN ENERGÍA.",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = vb.textMuted
                                )
                            }
                            Icon(Icons.Default.AutoAwesome, null, tint = vb.accent, modifier = Modifier.size(24.dp))
                        }
                        
                        Spacer(Modifier.height(20.dp))
                        
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            StatPill(label = "KCAL", value = "${state.diet?.dailyCalories ?: 0}")
                            StatPill(label = "COMIDAS", value = "${state.diet?.meals?.size ?: 0}")
                            StatPill(label = "CUMPLIDAS", value = "$eatenCount/${state.diet?.meals?.size ?: 0}")
                        }

                        Spacer(Modifier.height(20.dp))

                        Box(modifier = Modifier.fillMaxWidth().neuroRaised(12.dp).padding(12.dp)) {
                            Column {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text("CUMPLIMIENTO DIARIO", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = vb.textMuted)
                                    Text("$dailyCompliance%", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = vb.accent)
                                }
                                Spacer(Modifier.height(8.dp))
                                LiquidProgressBar(progress = dailyCompliance / 100f, height = 6.dp)
                            }
                        }
                        
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "💡 Tip: si entrenas intenso hoy, prioriza proteína + carbohidrato en la comida post-entreno.",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = vb.textMuted
                        )
                    }
                }
            }

            // 3. Macros Grid
            item {
                StaggeredEntrance(2) {
                    MacrosGrid(
                        protein = state.diet?.macros?.protein ?: 0,
                        carbs = state.diet?.macros?.carbs ?: 0,
                        fat = state.diet?.macros?.fat ?: 0,
                        hazeState = hazeState
                    )
                }
            }

            // 4. Meal List
            itemsIndexed(state.diet?.meals ?: emptyList()) { index, meal ->
                StaggeredEntrance(index + 3) {
                    val isEaten = state.eatenMealIds.contains(meal.id)
                    MealItem(
                        meal = meal,
                        isEaten = isEaten,
                        onToggle = { viewModel.toggleMealEaten(meal.id) },
                        onSwap = { viewModel.swapMeal(meal) },
                        hazeState = hazeState
                    )
                }
            }

            // 5. Special Dish
            item {
                StaggeredEntrance(state.diet?.meals?.size?.plus(3) ?: 5) {
                    SpecialDishCard(hazeState = hazeState)
                }
            }

            item { Spacer(Modifier.height(100.dp)) }
        }
    }
}

@Composable
private fun MacrosGrid(protein: Int, carbs: Int, fat: Int, hazeState: HazeState? = null) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        MacroCard("PROTEÍNA", "${protein}G", Icons.Default.Restaurant, Color(0xFFF87171), modifier = Modifier.weight(1f), hazeState = hazeState)
        MacroCard("CARBOS", "${carbs}G", Icons.Default.BakeryDining, Color(0xFFFBBF24), modifier = Modifier.weight(1f), hazeState = hazeState)
        MacroCard("GRASAS", "${fat}G", Icons.Default.WaterDrop, Color(0xFF38BDF8), modifier = Modifier.weight(1f), hazeState = hazeState)
    }
}

@Composable
private fun MacroCard(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color, modifier: Modifier = Modifier, hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = modifier, hazeState = hazeState) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(icon, null, tint = color, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
            Text(label, style = UppercaseLabel.copy(fontSize = 8.sp), color = vb.textMuted)
        }
    }
}

@Composable
private fun MealItem(
    meal: Meal,
    isEaten: Boolean,
    onToggle: () -> Unit,
    onSwap: () -> Unit,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    val haptic = rememberHaptic()

    LiquidGlassCard(
        modifier = Modifier.fillMaxWidth(),
        hazeState = hazeState,
        accentGlow = isEaten
    ) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            // Circle Check
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(if (isEaten) vb.accent else vb.surfaceElevated.copy(0.5f))
                    .border(1.dp, if (isEaten) vb.accent else vb.border.copy(0.3f), CircleShape)
                    .clickable { haptic.perform(HapticType.TICK); onToggle() },
                contentAlignment = Alignment.Center
            ) {
                if (isEaten) {
                    Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(14.dp))
                }
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        meal.name.uppercase(),
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Black,
                            textDecoration = if (isEaten) androidx.compose.ui.text.style.TextDecoration.LineThrough else null
                        ),
                        color = if (isEaten) vb.textMuted else ColorWhite
                    )
                    IconButton(onClick = onSwap, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Outlined.Refresh, null, tint = vb.textMuted, modifier = Modifier.size(16.dp))
                    }
                }
                
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        meal.time,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontMono = true),
                        color = vb.accent,
                        modifier = Modifier.background(vb.accent.copy(0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                    Text("🔥 ${meal.calories} KCAL", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = vb.textMuted)
                }
                
                Spacer(Modifier.height(8.dp))
                Text(meal.description, style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacroPill("P: ${meal.protein}G")
                    MacroPill("C: ${meal.carbs}G")
                    MacroPill("G: ${meal.fat}G")
                }
            }
        }
    }
}

@Composable
private fun MacroPill(text: String) {
    val vb = LocalVoltBodyColors.current
    Text(
        text,
        style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold, fontMono = true),
        color = vb.textMuted,
        modifier = Modifier.background(vb.surfaceElevated.copy(0.3f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
private fun SpecialDishCard(hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Text("🍲 PLATO ESPECIAL AJUSTABLE", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
        Spacer(Modifier.height(8.dp))
        Text("Base: arroz + lentejas + tomate + queso feta", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
        
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Calorías:", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
            Box(modifier = Modifier.weight(1f).neuroRaised(8.dp).padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text("390", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
            }
        }
        
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SpecialIngredientItem("Arroz", "100g")
            SpecialIngredientItem("Lentejas", "100g")
            SpecialIngredientItem("Tomate", "100g")
            SpecialIngredientItem("Queso Feta", "100g")
        }
    }
}

@Composable
private fun SpecialIngredientItem(name: String, amount: String) {
    val vb = LocalVoltBodyColors.current
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(name, style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
        Text(amount, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = ColorWhite)
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    val vb = LocalVoltBodyColors.current
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(vb.surfaceElevated.copy(0.4f))
            .border(1.dp, vb.border.copy(0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = UppercaseLabel.copy(fontSize = 7.sp), color = vb.textMuted)
            Text(value, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = ColorWhite)
        }
    }
}
