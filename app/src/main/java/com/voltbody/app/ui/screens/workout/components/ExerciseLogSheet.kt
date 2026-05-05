package com.voltbody.app.ui.screens.workout.components

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.domain.model.Exercise
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.theme.*
import com.voltbody.app.util.HapticType
import com.voltbody.app.util.rememberHaptic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseLogSheet(
    exercise: Exercise,
    setsDone: Int,
    onDismiss: () -> Unit,
    onLogSet: (weight: Float, reps: Int) -> Unit
) {
    val vb = LocalVoltBodyColors.current
    val haptic = rememberHaptic()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var weightInput by remember { mutableStateOf("") }
    var repsInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = vb.surfaceElevated.copy(alpha = 0.95f),
        contentColor = Color.White,
        tonalElevation = 0.dp,
        dragHandle = { BottomSheetDefaults.DragHandle(color = vb.textMuted.copy(0.3f)) },
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 48.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Header Info
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    exercise.name.uppercase(),
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    textAlign = TextAlign.Center
                )
                Text(
                    "${exercise.sets} SERIES × ${exercise.reps}",
                    style = MaterialTheme.typography.labelSmall,
                    color = vb.textMuted
                )
            }

            // 2. Set Tracker Dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val targetSets = exercise.sets?.toIntOrNull() ?: 3
                repeat(targetSets) { i ->
                    val isDone = i < setsDone
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isDone) vb.accent else vb.surfaceElevated)
                            .border(1.dp, if (isDone) vb.accent else vb.border.copy(0.3f), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isDone) {
                            Icon(Icons.Default.Check, null, tint = Color.Black, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                "${i + 1}",
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                color = vb.textMuted
                            )
                        }
                    }
                    if (i < targetSets - 1) Spacer(Modifier.width(8.dp))
                }
            }

            // 3. Progressive Overload Suggestion (Matching Web)
            LiquidGlassCard(accentGlow = true) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(Icons.Default.TrendingUp, null, tint = vb.accent)
                    Column(modifier = Modifier.weight(1f)) {
                        Text("OBJETIVO RECOMENDADO", style = UppercaseLabel.copy(fontSize = 8.sp), color = vb.textMuted)
                        Text("Sube a 15kg para progresar", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                    }
                }
            }

            // 4. Inputs
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                // Weight
                Column(modifier = Modifier.weight(1f)) {
                    Text("PESO (KG)", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, textAlign = TextAlign.Center),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            unfocusedBorderColor = vb.border.copy(0.3f),
                            focusedBorderColor = vb.accent
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
                // Reps
                Column(modifier = Modifier.weight(1f)) {
                    Text("REPETICIONES", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted, modifier = Modifier.padding(bottom = 8.dp))
                    OutlinedTextField(
                        value = repsInput,
                        onValueChange = { repsInput = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black, textAlign = TextAlign.Center),
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            unfocusedBorderColor = vb.border.copy(0.3f),
                            focusedBorderColor = vb.accent
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            // 5. Action Button
            LiquidGlassButton(
                text = "GUARDAR SERIE 💪",
                onClick = {
                    haptic.perform(HapticType.TICK)
                    onLogSet(weightInput.toFloatOrNull() ?: 0f, repsInput.toIntOrNull() ?: 0)
                    weightInput = ""
                    repsInput = ""
                },
                modifier = Modifier.fillMaxWidth(),
                style = LiquidButtonStyle.Primary
            )
        }
    }
}
