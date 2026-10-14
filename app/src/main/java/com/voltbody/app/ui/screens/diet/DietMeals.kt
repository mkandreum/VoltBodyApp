package com.voltbody.app.ui.screens.diet

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.voltbody.app.data.Meal
import com.voltbody.app.ui.AnimatedCheck
import com.voltbody.app.ui.PrimaryButton
import com.voltbody.app.ui.SkeletonBox
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType

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

/** Tarjeta de comida: hora destacada, check que se dibuja y boton de cambio con IA. */
@Composable
fun MealCard(
    meal: Meal, index: Int, eaten: Boolean,
    onToggle: () -> Unit, onSwap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(28.dp)
    val e = animateFloatAsState(if (eaten) 1f else 0f, tween(320), label = "mealEaten")
    val surface = Volt.surface
    val accent = Volt.accent
    val border = Volt.border

    Column(
        modifier.staggeredEntry(index).fillMaxWidth().clip(shape)
            .drawBehind {
                drawRect(surface)
                drawRect(accent.copy(alpha = 0.10f * e.value))
            }
            .drawBehind {
                val a = 0.06f + 0.40f * e.value
                drawRoundRect(
                    if (e.value > 0.01f) accent.copy(alpha = a) else border, cornerRadius = CornerRadius(28.dp.toPx()),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(1.dp.toPx()),
                )
            }
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Box(Modifier.size(48.dp).pressScale(onClick = onToggle), contentAlignment = Alignment.Center) {
                Box(Modifier.size(30.dp).border(2.dp, Volt.textDim.copy(alpha = 0.55f), CircleShape))
                AnimatedCheck(eaten, Modifier.size(30.dp))
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f).padding(top = 2.dp)) {
                Text(meal.time.uppercase(), color = Volt.accent, style = VoltType.label)
                Text(
                    mealTitle(meal), style = VoltType.title,
                    color = if (eaten) Volt.textDim else Volt.text,
                    textDecoration = if (eaten) TextDecoration.LineThrough else null,
                    modifier = Modifier.padding(top = 2.dp),
                )
                if (meal.description.isNotBlank()) {
                    Text(
                        meal.description, color = Volt.textDim, style = VoltType.body,
                        maxLines = 3, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
            Spacer(Modifier.width(8.dp))
            Box(
                Modifier.size(48.dp).clip(CircleShape).background(Volt.surface2).pressScale(onClick = onSwap),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.AutoAwesome, "Cambiar comida con IA", tint = Volt.accent, modifier = Modifier.size(22.dp)) }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("P ${meal.protein}g", color = ProteinColor, style = VoltType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                Text("C ${meal.carbs}g", color = CarbsColor, style = VoltType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
                Text("G ${meal.fat}g", color = FatColor, style = VoltType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold))
            }
            Row(
                Modifier.clip(CircleShape).background(Volt.accent.copy(alpha = 0.14f)).padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.LocalFireDepartment, null, tint = Volt.accent, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(4.dp))
                Text("${meal.calories} kcal", color = Volt.accent, style = VoltType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold))
            }
        }
    }
}

@Composable
private fun SheetSpinner() {
    val t = rememberInfiniteTransition(label = "sheetSpin")
    val angle = t.animateFloat(0f, 360f, infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart), label = "sheetAngle")
    Icon(Icons.Filled.Refresh, null, tint = Volt.accent, modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = angle.value })
}

@Composable
private fun MacroLine(m: Meal) {
    val bold = androidx.compose.ui.text.font.FontWeight.Bold
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("P ${m.protein}g", color = ProteinColor, style = VoltType.body.copy(fontWeight = bold))
        Text("C ${m.carbs}g", color = CarbsColor, style = VoltType.body.copy(fontWeight = bold))
        Text("G ${m.fat}g", color = FatColor, style = VoltType.body.copy(fontWeight = bold))
        Text("${m.calories} kcal", color = Volt.accent, style = VoltType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold))
    }
}

/** Hoja inferior: propone una alternativa equivalente (shimmer mientras la IA responde). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealSwapSheet(
    old: Meal, alt: Meal?, loading: Boolean, applying: Boolean,
    onApply: () -> Unit, onRetry: () -> Unit, onDismiss: () -> Unit,
) {
    com.voltbody.app.ui.v3.VoltModalSheet(onDismiss = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("CAMBIO CON IA", color = Volt.accent, style = VoltType.label)
            Text("Una alternativa para ${old.name}", color = Volt.text, style = VoltType.h2)
            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Volt.surface2).padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ACTUAL", color = Volt.textDim, style = VoltType.label)
                Text(mealTitle(old), color = Volt.textDim, style = VoltType.title, maxLines = 2)
                MacroLine(old)
            }
            AnimatedContent(
                targetState = loading, label = "swapContent",
                transitionSpec = { fadeIn(tween(250)) togetherWith fadeOut(tween(120)) },
            ) { isLoading ->
                if (isLoading || alt == null) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            SheetSpinner()
                            Text("Buscando algo equivalente...", color = Volt.textDim, style = VoltType.body)
                        }
                        SkeletonBox(Modifier.fillMaxWidth(0.6f).height(22.dp), RoundedCornerShape(11.dp))
                        SkeletonBox(Modifier.fillMaxWidth().height(14.dp), RoundedCornerShape(7.dp))
                        SkeletonBox(Modifier.fillMaxWidth(0.85f).height(14.dp), RoundedCornerShape(7.dp))
                        SkeletonBox(Modifier.fillMaxWidth(0.5f).height(18.dp), RoundedCornerShape(9.dp))
                    }
                } else {
                    val diff = alt.calories - old.calories
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Volt.accent.copy(alpha = 0.10f))
                            .border(1.dp, Volt.accent.copy(alpha = 0.35f), RoundedCornerShape(24.dp)).padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text("NUEVA PROPUESTA", color = Volt.accent, style = VoltType.label)
                        Text(mealTitle(alt), color = Volt.text, style = VoltType.title)
                        if (alt.description.isNotBlank()) Text(alt.description, color = Volt.textDim, style = VoltType.body, maxLines = 5, overflow = TextOverflow.Ellipsis)
                        MacroLine(alt)
                        Text(
                            if (diff == 0) "Mismas calorías" else if (diff > 0) "+$diff kcal respecto a la actual" else "$diff kcal respecto a la actual",
                            color = Volt.textDim, style = VoltType.body,
                        )
                    }
                }
            }
            PrimaryButton("Usar esta comida", onClick = onApply, enabled = !loading && alt != null, loading = applying)
            Text(
                "Probar otra opción", color = Volt.accent, style = VoltType.title,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(CircleShape).pressScaleIfAvailable(!loading && !applying, onRetry).padding(vertical = 14.dp),
            )
        }
    }
}

private fun Modifier.pressScaleIfAvailable(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) this.pressScale(onClick) else this.graphicsLayer { alpha = 0.4f }
