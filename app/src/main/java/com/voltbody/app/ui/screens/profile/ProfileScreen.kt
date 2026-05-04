package com.voltbody.app.ui.screens.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.screens.profile.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val haptic = rememberHaptic()
    var showEditSheet by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { viewModel.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Avatar + name ─────────────────────────────────────────────────────
            StaggeredEntrance(0) {
                AppCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.padding(24.dp).fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.name.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineLarge,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Black
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            state.name,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = ColorWhite
                        )
                        Text(
                            state.email,
                            style = MaterialTheme.typography.bodyMedium,
                            color = LocalVoltBodyColors.current.textMuted
                        )
                    }
                }
            }

            StaggeredEntrance(1) {
                FitnessStatsCard(
                    totalLogs = state.totalWorkoutLogs,
                    weeklyGoalProgress = if (state.completedWeeklyGoals.isNotEmpty()) state.completedWeeklyGoals.size / 5f else 0f,
                    weightLogsCount = state.weightLogs.size
                )
            }

            StaggeredEntrance(2) {
                WeeklyGoalsCard(
                    completedGoals = state.completedWeeklyGoals,
                    onToggleGoal = viewModel::toggleWeeklyGoal
                )
            }

            StaggeredEntrance(3) {
                WeightTrackingCard(
                    logs = state.weightLogs,
                    onLogWeight = viewModel::addWeightLog
                )
            }

            StaggeredEntrance(4) {
                PersonalRecordsCard(records = state.personalRecords)
            }

            // ── Settings ──────────────────────────────────────────────────────────
            StaggeredEntrance(5) {
                AppCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            "Ajustes",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ColorWhite
                        )
                        Spacer(Modifier.height(12.dp))

                        SettingsRow(
                            icon = Icons.Default.Settings,
                            label = "Editar Perfil",
                            onClick = { 
                                haptic.perform(HapticType.TICK)
                                showEditSheet = true 
                            }
                        )
                        SettingsRow(
                            icon = Icons.Default.Lock,
                            label = "Cambiar Contraseña",
                            onClick = { 
                                haptic.perform(HapticType.TICK)
                                showPasswordDialog = true 
                            }
                        )
                        SettingsRow(
                            icon = Icons.Default.Straighten,
                            label = if (state.useMetric) "Usar Sistema Imperial" else "Usar Sistema Métrico",
                            onClick = {
                                haptic.perform(HapticType.TICK)
                                viewModel.toggleUnits()
                            }
                        )
                        SettingsRow(
                            icon = Icons.Default.Notifications,
                            label = if (state.notificationsEnabled) "Desactivar Notificaciones" else "Activar Notificaciones",
                            onClick = {
                                haptic.perform(HapticType.TICK)
                                viewModel.toggleNotifications()
                            }
                        )
                        SettingsRow(
                            icon = Icons.Default.ExitToApp,
                            label = "Cerrar Sesión",
                            onClick = {
                                haptic.perform(HapticType.IMPACT)
                                viewModel.logout()
                            },
                            isLast = true
                        )
                    }
                }
            }

            Spacer(Modifier.height(80.dp))
        }
    }

    // ── Edit physical data bottom sheet ──────────────────────────────────────
    if (showEditSheet) {
        EditPhysicalDataSheet(
            state = state,
            onSave = { weight, height, age, goal ->
                haptic.perform(HapticType.CONFIRM)
                viewModel.updatePhysicalData(weight, height, age, goal)
                showEditSheet = false
            },
            onDismiss = { showEditSheet = false }
        )
    }

    // ── Change password dialog ────────────────────────────────────────────────
    if (showPasswordDialog) {
        ChangePasswordDialog(
            onConfirm = { current, new ->
                haptic.perform(HapticType.CONFIRM)
                viewModel.changePassword(current, new)
                showPasswordDialog = false
            },
            onDismiss = { showPasswordDialog = false }
        )
    }
}

@Composable
fun PhysicalDataRow(label: String, value: String) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditPhysicalDataSheet(
    state: ProfileState,
    onSave: (Float, Int, Int, String) -> Unit,
    onDismiss: () -> Unit
) {
    var weight by remember { mutableStateOf(state.weightKg.toString()) }
    var height by remember { mutableStateOf(state.heightCm.toString()) }
    var age by remember { mutableStateOf(state.age.toString()) }
    var goal by remember { mutableStateOf(state.goal) }
    val goals = listOf("Perder peso", "Ganar músculo", "Mantener", "Mejorar resistencia")

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Editar datos físicos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = weight, onValueChange = { weight = it },
                label = { Text("Peso (kg)") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
            )
            OutlinedTextField(
                value = height, onValueChange = { height = it },
                label = { Text("Altura (cm)") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            OutlinedTextField(
                value = age, onValueChange = { age = it },
                label = { Text("Edad") }, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )
            // Goal selector
            Text("Objetivo", style = MaterialTheme.typography.labelLarge)
            goals.forEach { g ->
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
                        .background(if (g == goal) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant)
                        .clickable { goal = g }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = g == goal, onClick = { goal = g })
                    Spacer(Modifier.width(8.dp))
                    Text(g, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Button(
                onClick = {
                    onSave(
                        weight.toFloatOrNull() ?: state.weightKg,
                        height.toIntOrNull() ?: state.heightCm,
                        age.toIntOrNull() ?: state.age,
                        goal
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar cambios") }
        }
    }
}

@Composable
fun ChangePasswordDialog(
    onConfirm: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    var current by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var currentVisible by remember { mutableStateOf(false) }
    var newVisible by remember { mutableStateOf(false) }
    val match = new == confirm && new.isNotEmpty()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cambiar contraseña") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = current, onValueChange = { current = it },
                    label = { Text("Contraseña actual") },
                    visualTransformation = if (currentVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { currentVisible = !currentVisible }) {
                            Icon(if (currentVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    }
                )
                OutlinedTextField(
                    value = new, onValueChange = { new = it },
                    label = { Text("Nueva contraseña") },
                    visualTransformation = if (newVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { newVisible = !newVisible }) {
                            Icon(if (newVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility, null)
                        }
                    }
                )
                OutlinedTextField(
                    value = confirm, onValueChange = { confirm = it },
                    label = { Text("Confirmar nueva") },
                    isError = confirm.isNotEmpty() && !match,
                    visualTransformation = PasswordVisualTransformation()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(current, new) }, enabled = match && current.isNotEmpty()) {
                Text("Cambiar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
