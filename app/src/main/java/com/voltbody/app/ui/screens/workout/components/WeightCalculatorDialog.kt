package com.voltbody.app.ui.screens.workout.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voltbody.app.ui.theme.LocalVoltBodyColors
import kotlin.math.floor

// ── Plates & Bars ─────────────────────────────────────────────────────────────

val availablePlates = listOf(25f, 20f, 15f, 10f, 5f, 2.5f, 1.25f)
val plateColors = mapOf(
    25f to Color(0xFFEF4444), // Red
    20f to Color(0xFF3B82F6), // Blue
    15f to Color(0xFFEAB308), // Yellow
    10f to Color(0xFF22C55E), // Green
    5f to Color(0xFFF97316),  // Orange
    2.5f to Color(0xFF64748B), // Gray
    1.25f to Color(0xFF94A3B8) // Light Gray
)

enum class BarType(val label: String, val weight: Float) {
    OLYMPIC("Olímpica (20kg)", 20f),
    WOMENS("Olímpica Mujer (15kg)", 15f),
    EZ_CURL("Barra EZ (10kg)", 10f),
    MULTIPOWER("Multipower (~10kg)", 10f)
}

// ── Calculator State ─────────────────────────────────────────────────────────

data class CalculatorState(
    val bar: BarType = BarType.OLYMPIC,
    val platesPerSide: List<Float> = emptyList(),
    val targetWeight: Float = 60f,
    val mode: CalcMode = CalcMode.TARGET_TO_PLATES
)

enum class CalcMode(val label: String) {
    TARGET_TO_PLATES("Peso total -> Discos"),
    PLATES_TO_TARGET("Discos -> Peso total")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeightCalculatorDialog(
    initialWeight: Float? = null,
    onDismiss: () -> Unit,
    onApplyWeight: (Float) -> Unit
) {
    val vb = LocalVoltBodyColors.current
    var state by remember {
        mutableStateOf(
            CalculatorState(
                targetWeight = initialWeight ?: 60f
            )
        )
    }

    // Auto calculate plates if in TARGET_TO_PLATES mode
    val calculatedPlates = remember(state.targetWeight, state.bar) {
        if (state.mode == CalcMode.TARGET_TO_PLATES) {
            calculatePlates(state.targetWeight, state.bar.weight)
        } else state.platesPerSide
    }

    // Auto calculate total if in PLATES_TO_TARGET mode
    val totalWeight = remember(state.platesPerSide, state.bar) {
        if (state.mode == CalcMode.PLATES_TO_TARGET) {
            state.bar.weight + (state.platesPerSide.sum() * 2)
        } else state.targetWeight
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(Modifier.padding(24.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Calculadora de Peso",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Close, "Cerrar")
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Mode switch
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = state.mode == CalcMode.TARGET_TO_PLATES,
                        onClick = {
                            if (state.mode != CalcMode.TARGET_TO_PLATES) {
                                state = state.copy(mode = CalcMode.TARGET_TO_PLATES, targetWeight = totalWeight)
                            }
                        },
                        shape = RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)
                    ) { Text("Calculadora") }
                    SegmentedButton(
                        selected = state.mode == CalcMode.PLATES_TO_TARGET,
                        onClick = {
                            if (state.mode != CalcMode.PLATES_TO_TARGET) {
                                state = state.copy(mode = CalcMode.PLATES_TO_TARGET, platesPerSide = calculatedPlates)
                            }
                        },
                        shape = RoundedCornerShape(topEnd = 12.dp, bottomEnd = 12.dp)
                    ) { Text("Sumar Discos") }
                }
                Spacer(Modifier.height(20.dp))

                // Bar selector
                Text("Barra", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(BarType.values()) { bar ->
                        val selected = state.bar == bar
                        FilterChip(
                            selected = selected,
                            onClick = { state = state.copy(bar = bar) },
                            label = { Text(bar.label) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = vb.accent.copy(alpha = 0.2f),
                                selectedLabelColor = vb.accent
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                borderColor = if (selected) vb.accent else vb.border,
                                enabled = true,
                                selected = selected
                            )
                        )
                    }
                }
                Spacer(Modifier.height(24.dp))

                // Mode specific UI
                if (state.mode == CalcMode.TARGET_TO_PLATES) {
                    TargetWeightInput(
                        weight = state.targetWeight,
                        onWeightChange = { state = state.copy(targetWeight = it) }
                    )
                } else {
                    PlatesInput(
                        plates = state.platesPerSide,
                        onAddPlate = { state = state.copy(platesPerSide = state.platesPerSide + it) },
                        onRemovePlate = { p ->
                            val list = state.platesPerSide.toMutableList()
                            list.remove(p)
                            state = state.copy(platesPerSide = list)
                        },
                        onClear = { state = state.copy(platesPerSide = emptyList()) }
                    )
                }

                Spacer(Modifier.height(24.dp))

                // Visual Representation
                val activePlates = if (state.mode == CalcMode.TARGET_TO_PLATES) calculatedPlates else state.platesPerSide
                val activeTotal = if (state.mode == CalcMode.TARGET_TO_PLATES) state.targetWeight else totalWeight
                
                PlateVisualization(plates = activePlates)
                
                Spacer(Modifier.height(16.dp))

                if (state.mode == CalcMode.TARGET_TO_PLATES && calculatePlates(state.targetWeight, state.bar.weight) != calculateExactPlates(state.targetWeight, state.bar.weight)) {
                    val actual = state.bar.weight + calculatePlates(state.targetWeight, state.bar.weight).sum() * 2
                    Text(
                        "Aproximado a ${actual}kg (por falta de discos menores)",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                    Spacer(Modifier.height(8.dp))
                }
                
                // Result & Apply
                Button(
                    onClick = { onApplyWeight(activeTotal) },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = vb.accent)
                ) {
                    Text("Aplicar ${activeTotal} kg", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}

@Composable
private fun TargetWeightInput(weight: Float, onWeightChange: (Float) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("Peso Objetivo", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(8.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            FilledIconButton(
                onClick = { onWeightChange((weight - 2.5f).coerceAtLeast(0f)) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) { Text("-", style = MaterialTheme.typography.titleLarge) }
            
            Text(
                "${if (weight % 1 == 0f) weight.toInt() else weight} kg",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Black
            )
            
            FilledIconButton(
                onClick = { onWeightChange(weight + 2.5f) },
                colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) { Text("+", style = MaterialTheme.typography.titleLarge) }
        }
    }
}

@Composable
private fun PlatesInput(
    plates: List<Float>,
    onAddPlate: (Float) -> Unit,
    onRemovePlate: (Float) -> Unit,
    onClear: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Añadir discos (por lado)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            TextButton(onClick = onClear) { Text("Limpiar") }
        }
        Spacer(Modifier.height(8.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(availablePlates) { plate ->
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(plateColors[plate] ?: Color.Gray)
                        .clickable { onAddPlate(plate) },
                    contentAlignment = Alignment.Center
                ) {
                    Text("${if (plate % 1 == 0f) plate.toInt() else plate}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
        
        AnimatedVisibility(visible = plates.isNotEmpty()) {
            Column {
                Spacer(Modifier.height(16.dp))
                Text("En la barra (clic para quitar)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(plates.sortedDescending()) { plate ->
                        Box(
                            modifier = Modifier
                                .height(48.dp)
                                .width(24.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(plateColors[plate] ?: Color.Gray)
                                .clickable { onRemovePlate(plate) },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("${if (plate % 1 == 0f) plate.toInt() else plate}", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(2.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PlateVisualization(plates: List<Float>) {
    val vb = LocalVoltBodyColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(120.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Sleeve
            Box(modifier = Modifier.width(40.dp).height(20.dp).background(Color.DarkGray))
            // Stop
            Box(modifier = Modifier.width(10.dp).height(40.dp).background(Color(0xFF1E293B)))
            // Plates
            if (plates.isEmpty()) {
                Text("Barra sola", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp))
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    plates.sortedDescending().forEach { plate ->
                        val height = when (plate) {
                            25f, 20f, 15f -> 90.dp
                            10f -> 70.dp
                            5f -> 50.dp
                            2.5f -> 40.dp
                            else -> 30.dp
                        }
                        val width = when (plate) {
                            25f -> 24.dp
                            20f -> 20.dp
                            15f -> 18.dp
                            else -> 14.dp
                        }
                        Box(
                            modifier = Modifier
                                .height(height)
                                .width(width)
                                .clip(RoundedCornerShape(3.dp))
                                .background(plateColors[plate] ?: Color.Gray)
                        )
                    }
                }
            }
        }
        Text(
            "Visualización por lado",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp)
        )
    }
}

private fun calculateExactPlates(targetWeight: Float, barWeight: Float): List<Float> {
    var perSide = (targetWeight - barWeight) / 2
    if (perSide <= 0) return emptyList()
    val plates = mutableListOf<Float>()
    for (plate in availablePlates) {
        while (perSide >= plate) {
            plates.add(plate)
            perSide -= plate
        }
    }
    return plates
}

private fun calculatePlates(targetWeight: Float, barWeight: Float): List<Float> {
    var perSide = (targetWeight - barWeight) / 2
    if (perSide <= 0) return emptyList()
    val plates = mutableListOf<Float>()
    for (plate in availablePlates) {
        // Only use the plates that fit
        while (perSide >= plate - 0.01f) {
            plates.add(plate)
            perSide -= plate
        }
    }
    return plates
}
