package com.voltbody.app.ui.screens.diet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.components.LiquidGlassCard
import com.voltbody.app.ui.components.GlowText
import com.voltbody.app.ui.components.neuroRaised
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors
import com.voltbody.app.ui.theme.MonoMetric

private data class Ingredient(
    val name: String,
    val baseGrams: Float,
    val kcalPer100g: Float,
    val icon: String,
    val color: Color
)

private val Ingredients = listOf(
    Ingredient("ARROZ SOS INTEGRAL", 100f, 130f, "🍚", Color(0xFFFEF08A)), // Yellow
    Ingredient("LENTEJAS LUENGO", 100f, 116f, "🫘", Color(0xFFFFEDD5)), // Orange
    Ingredient("TOMATE FRITO", 100f, 18f, "🍅", Color(0xFFFEE2E2)), // Red
    Ingredient("QUESO FETA LIGHT", 100f, 265f, "🧀", Color(0xFFE0F2FE)) // Blue
)

private val BASE_TOTAL_KCAL = Ingredients.sumOf { (it.baseGrams * it.kcalPer100g / 100).toDouble() }.toFloat()

@Composable
fun SpecialDishCalculator(modifier: Modifier = Modifier) {
    val vb = LocalVoltBodyColors.current
    var targetCalories by remember { mutableFloatStateOf(600f) }

    val factor = targetCalories / BASE_TOTAL_KCAL

    LiquidGlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).neuroRaised(cornerRadius = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.RestaurantMenu, contentDescription = null, tint = vb.accent, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        "PLATO ADAPTATIVO", 
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp), 
                        color = ColorWhite
                    )
                    Text("Cantidades ajustadas por IA", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                }
            }

            // Slider Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(vb.surfaceElevated.copy(alpha = 0.3f))
                    .padding(16.dp)
            ) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("CALORÍAS OBJETIVO", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = vb.textMuted)
                    GlowText("${targetCalories.toInt()} KCAL", style = MonoMetric.copy(fontSize = 18.sp, fontWeight = FontWeight.Black))
                }
                Slider(
                    value = targetCalories,
                    onValueChange = { targetCalories = it },
                    valueRange = 200f..1200f,
                    steps = 19,
                    colors = SliderDefaults.colors(
                        thumbColor = vb.accent,
                        activeTrackColor = vb.accent,
                        inactiveTrackColor = vb.border.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Ingredients List
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Ingredients.forEach { ingredient ->
                    val grams = ingredient.baseGrams * factor
                    val cals = grams * ingredient.kcalPer100g / 100f
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(vb.surfaceElevated.copy(alpha = 0.2f))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(ingredient.color.copy(alpha = 0.15f))
                                    .border(1.dp, ingredient.color.copy(alpha = 0.3f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(ingredient.icon, fontSize = 16.sp)
                            }
                            Column {
                                Text(ingredient.name, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                                Text("${cals.toInt()} KCAL", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = vb.textMuted)
                            }
                        }
                        GlowText(
                            "${grams.toInt()} G", 
                            style = MonoMetric.copy(fontSize = 15.sp, fontWeight = FontWeight.Black)
                        )
                    }
                }
            }
        }
    }
}
