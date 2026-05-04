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
import com.voltbody.app.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val haptic = rememberHaptic()
    var showEditSheet by remember { mutableStateOf(false) }
    var showPasswordDialog by remember { mutableStateOf(false) }

    // Card spring entrance
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { appeared = true }
    val cardScale by animateFloatAsState(
        targetValue = if (appeared) 1f else 0.93f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 400f),
        label = "profile_card"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // ── Avatar + name ─────────────────────────────────────────────────────
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer { scaleX = cardScale; scaleY = cardScale },
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                Modifier.padding(24.dp),
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
                Text(state.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer)
                Text(state.email, style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
            }
        }

        // ── Physical data card ────────────────────────────────────────────────
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically) {
                    Text("Datos físicos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    IconButton(onClick = { showEditSheet = true }) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar")
                    }
                }
                Divider(Modifier.padding(vertical = 8.dp))
                PhysicalDataRow("Peso", if (state.useMetric) "${state.weightKg} kg" else "${"%.1f".format(state.weightKg * 2.20462f)} lb")
                PhysicalDataRow("Altura", if (state.useMetric) "${state.heightCm} cm" else "${state.heightCm / 2.54f |> { "%.0f".format(it / 12) }}'${"%.0f".format(it % 12)}\"")
                PhysicalDataRow("Edad", "${state.age} años")
                PhysicalDataRow("Objetivo", state.goal)
            }
        }

        // ── Units toggle ──────────────────────────────────────────────────────
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Row(
                Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Unidades", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
                    Text(
                        if (state.useMetric) "kg / cm" else "lb / ft",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Métrico", style = MaterialTheme.typography.labelMedium)
                    Switch(
                        checked = !state.useMetric,
                        onCheckedChange = {
                            haptic.perform(HapticType.TICK)
                            viewModel.toggleUnits()
                        }
                    )
                    Text("Imperial", style = MaterialTheme.typography.labelMedium)
                }
            }
        }

        // ── Security ──────────────────────────────────────────────────────────
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            ListItem(
                headlineContent = { Text("Cambiar contraseña") },
                leadingContent = { Icon(Icons.Default.Lock, contentDescription = null) },
                trailingContent = { Icon(Icons.Default.ChevronRight, contentDescription = null) },
                modifier = Modifier.clickable {
                    haptic.perform(HapticType.TICK)
                    showPasswordDialog = true
                }
            )
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
