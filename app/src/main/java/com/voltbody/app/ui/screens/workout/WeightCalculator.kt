package com.voltbody.app.ui.screens.workout

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import kotlin.math.abs
import kotlin.math.round

// ─── Deteccion de categoria (port de WeightCalculator.tsx) ───
enum class ExerciseCategory(val label: String) {
    Barbell("Con barra"), Dumbbell("Mancuernas"), Machine("Máquina / Cable"), Bodyweight("Peso corporal")
}

private val BARBELL_KEYWORDS = listOf(
    "banca", "press de banca", "sentadilla", "peso muerto", "press militar", "remo con barra", "jalón", "dominada",
    "press banca", "rumano", "press francés", "barra", "hip thrust", "curl con barra",
)
private val DUMBBELL_KEYWORDS = listOf(
    "mancuerna", "curl de bíceps", "curl martillo", "press inclinado con mancuernas", "aperturas", "elevaciones laterales",
    "pájaros", "press con mancuernas", "curl de biceps", "press de hombros",
)
private val BODYWEIGHT_KEYWORDS_CALC = listOf(
    "dominadas asistidas", "fondos en paralelas", "fondos", "flexiones", "plancha", "burpees", "sentadilla búlgara", "step",
)
private val MACHINE_KEYWORDS = listOf(
    "polea", "prensa", "cable", "extensión", "face pull", "máquina", "jalón al pecho", "remo en cable", "extensión de tríceps",
)

fun detectExerciseCategory(name: String): ExerciseCategory {
    val lower = name.lowercase()
    if (BODYWEIGHT_KEYWORDS_CALC.any { lower.contains(it) }) return ExerciseCategory.Bodyweight
    if (DUMBBELL_KEYWORDS.any { lower.contains(it) }) return ExerciseCategory.Dumbbell
    if (BARBELL_KEYWORDS.any { lower.contains(it) }) return ExerciseCategory.Barbell
    if (MACHINE_KEYWORDS.any { lower.contains(it) }) return ExerciseCategory.Machine
    return ExerciseCategory.Machine
}

private val PLATE_OPTIONS = listOf(1.25, 2.5, 5.0, 10.0, 15.0, 20.0, 25.0)
private val BARBELL_OPTIONS = listOf(Triple("Olímpica", 20.0, "Barra Olímpica"), Triple("Mujer", 15.0, "Barra Mujer"), Triple("Pesada", 25.0, "Barra Pesada"))

/** Descompone el peso por lado en discos (voraz) para mostrar "20 + 5 + 1.25". */
fun platesBreakdown(perSide: Double): List<Double> {
    var rest = perSide
    val out = mutableListOf<Double>()
    for (p in PLATE_OPTIONS.sortedDescending()) {
        while (rest + 1e-9 >= p) { out.add(p); rest -= p }
    }
    return out
}

private fun prefs(ctx: Context) = ctx.getSharedPreferences("voltbody_prefs", Context.MODE_PRIVATE)

/**
 * Calculadora de carga y discos. Emite el peso total efectivo con [onWeightChange].
 * [weightIsEmpty]: solo precarga el peso en el primer montaje si el campo está vacío.
 */
@Composable
fun WeightCalculator(
    exerciseId: String, exerciseName: String, targetWeight: Double, userBodyweight: Double,
    weightIsEmpty: Boolean, onWeightChange: (Double) -> Unit, modifier: Modifier = Modifier,
) {
    val ctx = LocalContext.current
    val category = remember(exerciseName) { detectExerciseCategory(exerciseName) }
    val p = remember(exerciseId) { prefs(ctx) }
    var open by remember { mutableStateOf(false) }
    var barbellKg by remember(exerciseId) { mutableDoubleStateOf(p.getFloat("wc_${exerciseId}_barbell", 20f).toDouble()) }
    var plates by remember(exerciseId) { mutableDoubleStateOf(p.getFloat("wc_${exerciseId}_plates", 0f).toDouble()) }
    var dumbbell by remember(exerciseId) { mutableDoubleStateOf(p.getFloat("wc_${exerciseId}_dumbbell", 0f).toDouble()) }
    var machine by remember(exerciseId) { mutableDoubleStateOf(if (targetWeight > 0) targetWeight else 0.0) }

    val total: Double = when (category) {
        ExerciseCategory.Barbell -> barbellKg + plates * 2
        ExerciseCategory.Dumbbell -> dumbbell * 2
        ExerciseCategory.Machine -> machine
        ExerciseCategory.Bodyweight -> userBodyweight
    }

    var first by remember(exerciseId) { mutableStateOf(true) }
    LaunchedEffect(total) {
        val isFirst = first
        first = false
        if (total > 0 && (!isFirst || weightIsEmpty)) onWeightChange(total)
    }
    LaunchedEffect(barbellKg, plates, dumbbell) {
        p.edit().putFloat("wc_${exerciseId}_barbell", barbellKg.toFloat()).putFloat("wc_${exerciseId}_plates", plates.toFloat())
            .putFloat("wc_${exerciseId}_dumbbell", dumbbell.toFloat()).apply()
    }

    val gap = if (targetWeight > 0) round((targetWeight - total) * 10) / 10 else 0.0
    val barbellOnlyWarning = category == ExerciseCategory.Barbell && targetWeight > 0 && targetWeight < barbellKg

    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Volt.surface2)
                .clickable { open = !open }.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Filled.Build, null, tint = Volt.accent, modifier = Modifier.size(16.dp))
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("Calculadora de carga y discos", color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Text(category.label, color = Volt.textDim, fontSize = 11.sp)
            }
            Text(if (total > 0) "${fmtNum(total)} kg" else "Configurar", color = Volt.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(4.dp))
            Icon(if (open) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown, null, tint = Volt.textDim)
        }

        AnimatedVisibility(open, enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
            Column(
                Modifier.padding(top = 10.dp).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Volt.surface.copy(alpha = 0.9f)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (targetWeight > 0) {
                    Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.04f)).padding(10.dp)) {
                        Text("Meta sugerida por IA:", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Text("${fmtNum(targetWeight)} kg total", color = Volt.accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
                if (barbellOnlyWarning) {
                    Text(
                        "⚠️ La meta (${fmtNum(targetWeight)} kg) es menor que la barra sola (${fmtNum(barbellKg)} kg). Usa una barra de menor peso o la barra sin carga.",
                        color = Color(0xFFFCD34D), fontSize = 12.sp,
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0x1AF59E0B)).padding(10.dp),
                    )
                }

                when (category) {
                    ExerciseCategory.Barbell -> {
                        FieldLabel("Tipo de barra")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            BARBELL_OPTIONS.forEach { (short, kg, _) ->
                                val sel = barbellKg == kg
                                Column(
                                    Modifier.weight(1f).clip(RoundedCornerShape(14.dp))
                                        .background(if (sel) Volt.accent.copy(alpha = 0.18f) else Color.White.copy(alpha = 0.04f))
                                        .border(1.dp, if (sel) Volt.accent else Volt.border, RoundedCornerShape(14.dp))
                                        .clickable { barbellKg = kg }.padding(vertical = 10.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                ) {
                                    Text("${fmtNum(kg)} kg", color = if (sel) Volt.accent else Volt.text, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(short, color = Volt.textDim, fontSize = 10.sp)
                                }
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FieldLabel("Discos por lado", Modifier.weight(1f))
                            if (targetWeight > 0) {
                                MiniPill("Sugerir discos", onClick = {
                                    val needed = (targetWeight - barbellKg) / 2
                                    plates = if (needed <= 0) 0.0 else round(needed / 1.25) * 1.25
                                })
                            }
                        }
                        StepperField(
                            label = "kg por lado", value = if (plates == 0.0) "" else fmtNum(plates),
                            onChange = { plates = it.toD().coerceAtLeast(0.0) },
                            onMinus = { plates = (round((plates - 2.5) * 100) / 100).coerceAtLeast(0.0) },
                            onPlus = { plates = round((plates + 2.5) * 100) / 100 }, decimal = true,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                            PLATE_OPTIONS.forEach { pl ->
                                MiniPill("+${fmtNum(pl)}k", onClick = { plates = pl }, selected = plates == pl)
                            }
                        }
                        if (plates > 0) {
                            val breakdown = platesBreakdown(plates)
                            Text(
                                "${fmtNum(barbellKg)} kg barra + (${fmtNum(plates)} × 2) kg discos = ${fmtNum(total)} kg" +
                                    (if (breakdown.isNotEmpty()) "\nPor lado: " + breakdown.joinToString(" + ") { fmtNum(it) } else ""),
                                color = Volt.textDim, fontSize = 12.sp,
                            )
                        }
                    }
                    ExerciseCategory.Dumbbell -> {
                        StepperField(
                            label = "Peso por mancuerna (kg/mano)", value = if (dumbbell == 0.0) "" else fmtNum(dumbbell),
                            onChange = { dumbbell = it.toD().coerceAtLeast(0.0) },
                            onMinus = { dumbbell = (dumbbell - 2).coerceAtLeast(0.0) }, onPlus = { dumbbell += 2 }, decimal = true,
                        )
                        if (dumbbell > 0) Text("${fmtNum(dumbbell)} kg × 2 brazos = ${fmtNum(total)} kg total", color = Volt.textDim, fontSize = 12.sp)
                    }
                    ExerciseCategory.Machine -> {
                        StepperField(
                            label = "Placas o selector de máquina (kg)", value = if (machine == 0.0) "" else fmtNum(machine),
                            onChange = { machine = it.toD().coerceAtLeast(0.0) },
                            onMinus = { machine = (machine - 5).coerceAtLeast(0.0) }, onPlus = { machine += 5 }, decimal = true,
                        )
                    }
                    ExerciseCategory.Bodyweight -> {
                        Text(
                            if (userBodyweight > 0) "Peso corporal de referencia: ${fmtNum(userBodyweight)} kg" else "Peso corporal estándar",
                            color = Volt.text, fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White.copy(alpha = 0.04f)).padding(12.dp),
                        )
                    }
                }

                if (total > 0) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            FieldLabel("Peso total resultante")
                            AnimatedContent(
                                targetState = total,
                                transitionSpec = { (slideInVertically { it / 2 } + fadeIn()) togetherWith (slideOutVertically { -it / 2 } + fadeOut()) },
                                label = "calcTotal",
                            ) { t ->
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(fmtNum(t), style = VoltType.numberL.copy(brush = Volt.gradient))
                                    Spacer(Modifier.width(4.dp))
                                    Text("kg", color = Volt.textDim, fontSize = 12.sp, modifier = Modifier.padding(bottom = 5.dp))
                                }
                            }
                            if (targetWeight > 0 && !barbellOnlyWarning) {
                                Text(
                                    when {
                                        abs(gap) < 1e-9 -> "✅ Exactamente en la meta"
                                        gap > 0 -> "${fmtNum(gap)} kg por debajo de la meta"
                                        else -> "${fmtNum(abs(gap))} kg por encima de la meta"
                                    },
                                    color = when { abs(gap) < 1e-9 -> Volt.accent2; gap > 0 -> PrGold; else -> Volt.water },
                                    fontSize = 11.sp,
                                )
                            }
                        }
                        Row(
                            Modifier.clip(CircleShape).background(Volt.gradient).clickable { onWeightChange(total) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Usar", color = Volt.onAccent, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.Filled.ArrowForward, null, tint = Volt.onAccent, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    }
}
