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
import com.voltbody.app.ui.components.AppCard
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
    Ingredient("Arroz SOS Integral", 100f, 130f, "🍚", Color(0xFFFEF08A)), // Yellow
    Ingredient("Lentejas Luengo (bote)", 100f, 116f, "🫘", Color(0xFFFFEDD5)), // Orange
    Ingredient("Tomate Frito", 100f, 18f, "🍅", Color(0xFFFEE2E2)), // Red
    Ingredient("Queso Feta Light", 100f, 265f, "🧀", Color(0xFFE0F2FE)) // Blue
)

private val BASE_TOTAL_KCAL = Ingredients.sumOf { (it.baseGrams * it.kcalPer100g / 100).toDouble() }.toFloat()

@Composable
fun SpecialDishCalculator(modifier: Modifier = Modifier) {
    val vb = LocalVoltBodyColors.current
    var targetCalories by remember { mutableFloatStateOf(600f) }

    val factor = targetCalories / BASE_TOTAL_KCAL

    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(vb.accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.RestaurantMenu, contentDescription = null, tint = vb.accent)
                }
                Column {
                    Text("Plato Especial Adaptativo", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ColorWhite)
                    Text("Ajusta las cantidades según tus calorías", style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
                }
            }

            // Slider
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text("Calorías Objetivo", style = MaterialTheme.typography.labelMedium, color = vb.textMuted)
                    Text(
                        "${targetCalories.toInt()} kcal",
                        style = MonoMetric.copy(fontSize = 20.sp),
                        color = vb.accent,
                        fontWeight = FontWeight.Bold
                    )
                }
                Slider(
                    value = targetCalories,
                    onValueChange = { targetCalories = it },
                    valueRange = 200f..1200f,
                    steps = 19, // step of 50
                    colors = SliderDefaults.colors(
                        thumbColor = vb.accent,
                        activeTrackColor = vb.accent,
                        inactiveTrackColor = vb.surfaceElevated
                    )
                )
            }

            // Ingredients List
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(vb.surfaceElevated)
                    .border(1.dp, vb.border, RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Ingredients.forEach { ingredient ->
                    val grams = ingredient.baseGrams * factor
                    val cals = grams * ingredient.kcalPer100g / 100f
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
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
                                Text(ingredient.name, style = MaterialTheme.typography.bodyMedium, color = ColorWhite)
                                Text("${cals.toInt()} kcal", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                            }
                        }
                        Text(
                            "${grams.toInt()} g",
                            style = MonoMetric.copy(fontSize = 16.sp),
                            color = vb.accent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
