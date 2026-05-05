package com.voltbody.app.ui.screens.profile

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.*
import androidx.compose.ui.unit.*
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.voltbody.app.domain.model.*
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.theme.*
import com.voltbody.app.util.*
import com.voltbody.app.ui.screens.profile.components.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.draw.shadow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel(),
    onLogout: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val haptic = rememberHaptic()
    var showEditSheet by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    LiquidGlassScaffold(
        background = {
            // Elegant "Profile" background with blue/indigo splashes
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.size(400.dp).align(Alignment.TopCenter).background(vb.accentDim.copy(0.1f), CircleShape).offset(0.dp, (-150).dp))
                Box(modifier = Modifier.size(250.dp).align(Alignment.BottomEnd).background(vb.accent.copy(0.08f), CircleShape).offset(50.dp, 50.dp))
            }
        }
    ) { hazeState ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 60.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
            // ── Avatar + name ─────────────────────────────────────────────────────
            StaggeredEntrance(0) {
                LiquidGlassCard(
                    modifier = Modifier.fillMaxWidth(),
                    glassAlpha = 0.4f,
                    hazeState = hazeState
                ) {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(110.dp)
                                .clip(CircleShape)
                                .background(vb.surfaceElevated.copy(alpha = 0.5f))
                                .border(2.dp, vb.accent, CircleShape)
                                .shadow(24.dp, CircleShape, ambientColor = vb.accent.copy(alpha = 0.4f))
                                .clickable { /* Picker logic */ },
                            contentAlignment = Center
                        ) {
                            if (state.profilePhoto != null) {
                                AsyncImage(
                                    model = state.profilePhoto,
                                    contentDescription = "Foto de perfil",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                HeadlineGradient(
                                    state.name.take(1).uppercase(),
                                    style = MaterialTheme.typography.displaySmall
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        HeadlineGradient(
                            state.name,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            state.email.uppercase(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = vb.textMuted
                        )
                    }
                }
            }

            StaggeredEntrance(1) {
                FitnessStatsCard(
                    totalLogs = state.totalWorkoutLogs,
                    weeklyGoalProgress = if (state.completedWeeklyGoals.isNotEmpty()) state.completedWeeklyGoals.size / 5f else 0f,
                    weightLogsCount = state.weightLogs.size,
                    hazeState = hazeState
                )
            }

            // ── Motivation ────────────────────────────────────────────────────────
            StaggeredEntrance(2) {
                MotivationEditor(
                    phrase = state.motivationPhrase,
                    photoUrl = state.motivationPhoto,
                    onSave = viewModel::setMotivation,
                    hazeState = hazeState
                )
            }

            // ── Weekly Goals ──────────────────────────────────────────────────────
            StaggeredEntrance(3) {
                WeeklyGoalsCard(
                    completedGoals = state.completedWeeklyGoals.filter { it.done }.map { it.id }.toSet(),
                    onToggleGoal = viewModel::toggleWeeklyGoal,
                    hazeState = hazeState
                )
            }

            // ── Progress Photos ───────────────────────────────────────────────────
            StaggeredEntrance(4) {
                ProgressPhotosCard(
                    photos = state.progressPhotos,
                    onAddPhoto = { 
                        // Simplified: in a real app we'd open a picker. 
                        // For this demo, let's just add a placeholder or prompt.
                    },
                    hazeState = hazeState
                )
            }

            // ── Weight Tracking ───────────────────────────────────────────────────
            StaggeredEntrance(5) {
                WeightTrackingCard(
                    logs = state.weightLogs,
                    onLogWeight = viewModel::addWeightLog,
                    hazeState = hazeState
                )
            }

            // ── Personal Records ──────────────────────────────────────────────────
            StaggeredEntrance(6) {
                PersonalRecordsCard(records = state.personalRecords, hazeState = hazeState)
            }

            // ── Theme Selector ────────────────────────────────────────────────────
            StaggeredEntrance(7) {
                ThemeSelector(
                    selectedTheme = state.theme,
                    onThemeSelected = viewModel::setTheme,
                    hazeState = hazeState
                )
            }

            // ── Settings ──────────────────────────────────────────────────────────
            StaggeredEntrance(8) {
                LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
                    Column {
                        Text(
                            "AJUSTES DE CUENTA",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 1.sp
                            ),
                            color = ColorWhite
                        )
                        Spacer(Modifier.height(16.dp))

                        SettingsRow(
                            icon = Icons.Default.Person,
                            label = "Editar Datos Físicos",
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
                        
                        Spacer(Modifier.height(12.dp))
                        
                        LiquidGlassButton(
                            text = "CERRAR SESIÓN",
                            onClick = {
                                haptic.perform(HapticType.HEAVY)
                                viewModel.logout()
                                onLogout()
                            },
                            modifier = Modifier.fillMaxWidth(),
                            hazeState = hazeState,
                            style = LiquidButtonStyle.Secondary,
                            leadingIcon = { Icon(Icons.AutoMirrored.Filled.ExitToApp, null, tint = vb.accent) }
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
