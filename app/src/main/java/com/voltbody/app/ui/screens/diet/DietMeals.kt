package com.voltbody.app.ui.screens.diet

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.Meal
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt

private val EatenGreen = Color(0xFF10B981)

fun mealEmoji(meal: Meal): String {
    val time = meal.time.lowercase()
    val name = meal.name.lowercase()
    return when {
        listOf("07", "08", "09").any { time.contains(it) } || name.contains("desay") -> "🥣"
        time.contains("11") || name.contains("almuer") -> "🍎"
        listOf("13", "14", "15").any { time.contains(it) } || name.contains("comida") -> "🍽️"
        time.contains("17") || time.contains("18") || name.contains("meri") -> "🥜"
        listOf("20", "21", "22").any { time.contains(it) } || name.contains("cena") -> "🌙"
        else -> "🍴"
    }
}

/** Antepone un emoji al nombre salvo que ya empiece por uno (misma logica que la web). */
fun mealTitle(meal: Meal): String {
    val base = meal.name.trim()
    val cp = if (base.isNotEmpty()) base.codePointAt(0) else 0
    val startsWithPictograph = cp in 0x2600..0x27BF || cp in 0x1F000..0x1FAFF
    return if (startsWithPictograph) base else "${mealEmoji(meal)} $base"
}

@Composable
fun MealCard(
    meal: Meal, index: Int, eaten: Boolean, swapping: Boolean,
    onToggle: () -> Unit, onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val borderColor by animateColorAsState(if (eaten) EatenGreen.copy(alpha = 0.45f) else Volt.border, tween(300), label = "mealBorder")
    val bg by animateColorAsState(if (eaten) EatenGreen.copy(alpha = 0.08f) else Volt.surface, tween(300), label = "mealBg")
    val shape = RoundedCornerShape(24.dp)

    Column(
        modifier.staggeredEntry(index).fillMaxWidth().clip(shape).background(bg).border(BorderStroke(1.dp, borderColor), shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            CheckToggle(eaten, onToggle)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    mealTitle(meal), fontWeight = FontWeight.Bold, fontSize = 16.sp,
                    color = if (eaten) Volt.textDim else Volt.text,
                    textDecoration = if (eaten) TextDecoration.LineThrough else null,
                )
                AnimatedVisibility(eaten, enter = fadeIn() + scaleIn(), exit = fadeOut() + scaleOut()) {
                    Text(
                        "✓ Registrada", color = EatenGreen, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 4.dp).clip(RoundedCornerShape(6.dp)).background(EatenGreen.copy(alpha = 0.18f)).padding(horizontal = 8.dp, vertical = 2.dp),
                    )
                }
                if (meal.description.isNotBlank()) {
                    Text(
                        meal.description, color = if (eaten) Volt.textDim.copy(alpha = 0.7f) else Volt.textDim, fontSize = 12.sp,
                        lineHeight = 17.sp, maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            SwapButton(swapping, onSwap)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Color.Black.copy(alpha = 0.35f)).padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Schedule, null, tint = Volt.textDim, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(4.dp))
                Text(meal.time, color = Volt.text, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("P ${meal.protein}g", color = ProteinColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("C ${meal.carbs}g", color = CarbsColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                Text("G ${meal.fat}g", color = FatColor, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
            }
            Row(
                Modifier.clip(RoundedCornerShape(10.dp)).background(Volt.accent.copy(alpha = 0.12f)).padding(horizontal = 8.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = Volt.accent, modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(3.dp))
                Text("${meal.calories} kcal", color = Volt.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun CheckToggle(eaten: Boolean, onToggle: () -> Unit) {
    val fill by animateColorAsState(if (eaten) EatenGreen else Color.Transparent, tween(250), label = "checkFill")
    val ring by animateColorAsState(if (eaten) EatenGreen else Volt.textDim.copy(alpha = 0.6f), tween(250), label = "checkRing")
    val scale by animateFloatAsState(if (eaten) 1f else 0.92f, spring(dampingRatio = 0.35f, stiffness = 500f), label = "checkScale")
    Box(
        Modifier.size(34.dp).clip(CircleShape).clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier.size((28f * scale).dp).clip(CircleShape).background(fill).border(2.dp, ring, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedVisibility(eaten, enter = scaleIn(spring(dampingRatio = 0.3f, stiffness = 600f)) + fadeIn(), exit = scaleOut() + fadeOut()) {
                Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SwapButton(swapping: Boolean, onSwap: () -> Unit) {
    val t = rememberInfiniteTransition(label = "swapSpin")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing)), label = "swapAngle")
    Box(
        Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(Volt.surface2)
            .clickable(enabled = !swapping, onClick = onSwap),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            Icons.Filled.Refresh, "Cambiar comida con IA",
            tint = if (swapping) Volt.accent else Volt.textDim,
            modifier = Modifier.size(20.dp).rotate(if (swapping) angle else 0f),
        )
    }
}
