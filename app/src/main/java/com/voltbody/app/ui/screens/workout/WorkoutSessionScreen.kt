package com.voltbody.app.ui.screens.workout

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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.voltbody.app.ui.components.WorkoutFloatingToolbar
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.screens.workout.components.WeightCalculatorDialog
import com.voltbody.app.util.*

// RPE colour coding — same palette as web
private fun rpeColor(rpe: Int): Color = when (rpe) {
    in 1..4 -> Color(0xFF4ADE80)  // green  — easy
    in 5..6 -> Color(0xFFFBBF24)  // amber  — moderate
    in 7..8 -> Color(0xFFF97316)  // orange — hard
    else    -> Color(0xFFEF4444)  // red    — max effort
}

private val rpeLabels = mapOf(
    1 to "Muy fácil", 2 to "Fácil", 3 to "Ligero", 4 to "Moderado",
    5 to "Algo duro", 6 to "Duro", 7 to "Muy duro", 8 to "Muy muy duro",
    9 to "Casi máximo", 10 to "Máximo esfuerzo"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSessionScreen(
    onFinish: () -> Unit,
    viewModel: WorkoutSessionViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val haptic = rememberHaptic()

    val exercise = state.exercises.getOrNull(state.currentExerciseIndex)

    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(state.currentExerciseIndex) {
        visible = false
        kotlinx.coroutines.delay(80)
        visible = true
    }
    val cardScale by animateFloatAsState(
        targetValue = if (visible) 1f else 0.92f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "card_scale"
    )
    val cardAlpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 500f),
        label = "card_alpha"
    )

    var showWeightCalc by remember { mutableStateOf(false) }

    if (state.isFinished) {
        LaunchedEffect(Unit) {
            haptic.perform(HapticType.HEAVY)
            onFinish()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.workoutName, fontWeight = FontWeight.SemiBold) },
                actions = {
                    val elapsed = state.elapsedSeconds
                    val h = elapsed / 3600
                    val m = (elapsed % 3600) / 60
                    val s = elapsed % 60
                    val timeStr = if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
                    SuggestionChip(
                        onClick = {},
                        label = { Text(timeStr, style = MaterialTheme.typography.labelLarge) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            )
        },
        floatingActionButton = {
            WorkoutFloatingToolbar(
                onPause = {
                    haptic.perform(HapticType.TICK)
                    viewModel.togglePause()
                },
                onSkipRest = {
                    haptic.perform(HapticType.TICK)
                    viewModel.skipRest()
                },
                onFinish = {
                    haptic.perform(HapticType.HEAVY)
                    onFinish()
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(top = 16.dp, bottom = 100.dp)
        ) {
            // ── Rest timer overlay ──────────────────────────────────────────
            if (state.isResting) {
                item {
                    RestTimerCard(
                        remainingSeconds = state.restSeconds,
                        onSkip = {
                            haptic.perform(HapticType.TICK)
                            viewModel.skipRest()
                        }
                    )
                }
            }

            // ── Current exercise card ────────────────────────────────────────
            exercise?.let { ex ->
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                scaleX = cardScale
                                scaleY = cardScale
                                alpha = cardAlpha
                            },
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(Modifier.padding(20.dp)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Ejercicio ${state.currentExerciseIndex + 1}/${state.exercises.size}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                    Text(
                                        text = ex.name,
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${state.currentSetIndex + 1}/${ex.sets}",
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // ── Reps picker ──────────────────────────────────
                            Text(
                                "Repeticiones",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Spacer(Modifier.height(8.dp))
                            SetsButtonGroup(
                                options = listOf("6", "8", "10", "12", "15", "20"),
                                selectedIndex = when (state.selectedReps) {
                                    6 -> 0; 8 -> 1; 10 -> 2; 12 -> 3; 15 -> 4; else -> 5
                                },
                                onSelected = { idx ->
                                    haptic.perform(HapticType.TICK)
                                    val reps = listOf(6, 8, 10, 12, 15, 20)[idx]
                                    viewModel.setReps(reps)
                                }
                            )

                            Spacer(Modifier.height(16.dp))

                            // ── Weight input ─────────────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                OutlinedTextField(
                                    value = if (state.selectedWeight == 0f) "" else state.selectedWeight.toString(),
                                    onValueChange = { v ->
                                        v.toFloatOrNull()?.let { viewModel.setWeight(it) }
                                    },
                                    label = { Text("Peso (kg)") },
                                    modifier = Modifier.weight(1f),
                                    singleLine = true,
                                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal,
                                        imeAction = ImeAction.Next
                                    )
                                )
                                FilledIconButton(
                                    onClick = {
                                        haptic.perform(HapticType.TICK)
                                        showWeightCalc = true
                                    },
                                    modifier = Modifier.size(56.dp).padding(top = 8.dp),
                                    colors = IconButtonDefaults.filledIconButtonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Calculate,
                                        contentDescription = "Calculadora de discos",
                                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }

                            Spacer(Modifier.height(20.dp))

                            // ── RPE selector (1–10) ───────────────────────────
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "RPE — Esfuerzo percibido",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                state.selectedRpe?.let { rpe ->
                                    Text(
                                        rpeLabels[rpe] ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = rpeColor(rpe)
                                    )
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                (1..10).forEach { v ->
                                    val isSelected = state.selectedRpe == v
                                    val color = rpeColor(v)
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isSelected) color.copy(alpha = 0.25f)
                                                else MaterialTheme.colorScheme.surface
                                            )
                                            .border(
                                                width = if (isSelected) 2.dp else 1.dp,
                                                color = if (isSelected) color else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                haptic.perform(HapticType.TICK)
                                                viewModel.setRpe(if (isSelected) null else v)
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            "$v",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isSelected) color
                                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }

                            Spacer(Modifier.height(16.dp))

                            // ── Note field (optional) ─────────────────────────
                            OutlinedTextField(
                                value = state.currentNote,
                                onValueChange = viewModel::setNote,
                                label = { Text("Nota (opcional)") },
                                placeholder = { Text("ej. buena forma, dolor lumbar leve…", style = MaterialTheme.typography.bodySmall) },
                                modifier = Modifier.fillMaxWidth(),
                                maxLines = 2,
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                                    imeAction = ImeAction.Done
                                )
                            )

                            Spacer(Modifier.height(20.dp))

                            // ── Log set button ────────────────────────────────
                            Button(
                                onClick = {
                                    haptic.perform(HapticType.CONFIRM)
                                    viewModel.logSet(state.selectedReps, state.selectedWeight)
                                },
                                modifier = Modifier.fillMaxWidth(),
                                contentPadding = PaddingValues(vertical = 14.dp)
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text("Serie completada", style = MaterialTheme.typography.titleMedium)
                            }
                        }
                    }
                }
            }

            // ── Set history ─────────────────────────────────────────────────
            if (state.setLogs.isNotEmpty()) {
                item {
                    Text(
                        "Historial de series",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(state.setLogs.reversed()) { log ->
                    SetHistoryRow(log)
                }
            }
        }
    }

    if (showWeightCalc) {
        WeightCalculatorDialog(
            initialWeight = if (state.selectedWeight > 0f) state.selectedWeight else null,
            onDismiss = { showWeightCalc = false },
            onApplyWeight = { w ->
                viewModel.setWeight(w)
                showWeightCalc = false
                haptic.perform(HapticType.TICK)
            }
        )
    }
}

@Composable
fun RestTimerCard(remainingSeconds: Int, onSkip: () -> Unit) {
    val progress = (remainingSeconds / 60f).coerceIn(0f, 1f)
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Descanso", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "%d:%02d".format(remainingSeconds / 60, remainingSeconds % 60),
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.secondary
            )
            Spacer(Modifier.height(12.dp))
            OutlinedButton(onClick = onSkip) {
                Icon(Icons.Default.SkipNext, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Saltar descanso")
            }
        }
    }
}

@Composable
fun SetHistoryRow(log: SetLog) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(log) { visible = true }
    val offsetY by animateDpAsState(
        targetValue = if (visible) 0.dp else 20.dp,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 380f),
        label = "row_offset"
    )
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.7f, stiffness = 400f),
        label = "row_alpha"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .offset(y = offsetY)
            .graphicsLayer { this.alpha = alpha }
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                log.exerciseName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Serie ${log.setNumber}  ·  ${log.reps} reps  ·  ${log.weightKg}kg",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                // RPE badge
                log.rpe?.let { rpe ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(rpeColor(rpe).copy(alpha = 0.15f))
                            .border(1.dp, rpeColor(rpe).copy(alpha = 0.4f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            "RPE $rpe",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = rpeColor(rpe)
                        )
                    }
                }
            }
        }
        // Note — shown only if present
        log.note?.let { note ->
            Text(
                "📝 $note",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
            )
        }
    }
}
