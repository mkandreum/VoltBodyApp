package com.voltbody.app.ui.screens.profile

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.voltbody.app.domain.model.*
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.theme.*
import com.voltbody.app.ui.screens.profile.components.PersonalRecordsCard
import com.voltbody.app.ui.screens.profile.components.WeeklyGoalsCard
import dev.chrisbanes.haze.HazeState

@Composable
fun ProfileScreen(
    onLogout: () -> Unit = {},
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val vb = LocalVoltBodyColors.current
    val state by viewModel.state.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    LiquidGlassScaffold(
        background = {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.size(400.dp).align(Alignment.TopCenter).background(vb.accent.copy(0.08f), CircleShape).offset(y = (-100).dp))
                Box(modifier = Modifier.size(300.dp).align(Alignment.BottomEnd).background(ColorInfo.copy(0.05f), CircleShape).offset(50.dp, 50.dp))
            }
        }
    ) { hazeState ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = { viewModel.refresh() },
            modifier = Modifier.fillMaxSize()
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 70.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    StaggeredEntrance(0) {
                        ProfileHeader(
                            name = state.name,
                            photoUrl = state.profilePhoto,
                            level = (state.totalWorkoutLogs / 5) + 1
                        )
                    }
                }

                item {
                    StaggeredEntrance(1) {
                        ProfileStatsRow(
                            workouts = state.totalWorkoutLogs,
                            streak = 5,
                            xp = state.totalWorkoutLogs * 50
                        )
                    }
                }

                item {
                    StaggeredEntrance(2) {
                        WeightChartCard(
                            currentWeight = state.weightKg,
                            logs = state.weightLogs,
                            onLogWeight = { viewModel.addWeightLog(it) },
                            hazeState = hazeState
                        )
                    }
                }

                item {
                    StaggeredEntrance(3) {
                        PersonalRecordsCard(records = state.personalRecords, hazeState = hazeState)
                    }
                }

                item {
                    StaggeredEntrance(4) {
                        MotivationCard(
                            phrase = state.motivationPhrase,
                            photoUrl = state.motivationPhoto,
                            hazeState = hazeState
                        )
                    }
                }

                item {
                    StaggeredEntrance(5) {
                        // Using set of labels for WeeklyGoalsCard compatibility
                        WeeklyGoalsCard(
                            goals = state.completedWeeklyGoals,
                            onToggleGoal = { viewModel.toggleWeeklyGoal(it) },
                            hazeState = hazeState
                        )
                    }
                }

                item {
                    StaggeredEntrance(6) {
                        SettingsCard(
                            theme = state.theme,
                            onThemeChange = { viewModel.setTheme(it) },
                            notifsEnabled = state.notificationsEnabled,
                            onNotifsToggle = { viewModel.toggleNotifications() },
                            onLogout = { 
                                viewModel.logout()
                                onLogout()
                            },
                            hazeState = hazeState
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(100.dp)) }
            }
        }
    }
}

@Composable
private fun ProfileHeader(name: String, photoUrl: String?, level: Int) {
    val vb = LocalVoltBodyColors.current
    val infiniteTransition = rememberInfiniteTransition(label = "avatar_glow")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glow_alpha"
    )

    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(2.dp, vb.accent.copy(glowAlpha), CircleShape)
                    .padding(4.dp)
                    .border(1.dp, vb.accent.copy(0.2f), CircleShape)
            )
            
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(vb.surfaceElevated),
                contentAlignment = Alignment.Center
            ) {
                if (!photoUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = photoUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(Icons.Default.Person, null, tint = vb.accent, modifier = Modifier.size(48.dp))
                }
            }
            
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(vb.accent)
                    .border(2.dp, vb.bg, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text("$level", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, fontSize = 10.sp), color = vb.bg)
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        HeadlineGradient(name.uppercase().ifEmpty { "USUARIO" }, style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black))
        Text("GUERRERO VOLTBODY", style = UppercaseLabel.copy(fontSize = 10.sp, letterSpacing = 2.sp), color = vb.textMuted)
    }
}

@Composable
private fun ProfileStatsRow(workouts: Int, streak: Int, xp: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ProfileStatPill("ENTRENOS", "$workouts", modifier = Modifier.weight(1f))
        ProfileStatPill("RACHA", "$streak", modifier = Modifier.weight(1f))
        ProfileStatPill("XP", "$xp", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun ProfileStatPill(label: String, value: String, modifier: Modifier = Modifier) {
    val vb = LocalVoltBodyColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(vb.surfaceElevated.copy(0.3f))
            .padding(12.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
            Text(label, style = UppercaseLabel.copy(fontSize = 7.sp), color = vb.textMuted)
        }
    }
}

@Composable
private fun WeightChartCard(
    currentWeight: Float,
    logs: List<WeightLog>,
    onLogWeight: (Float) -> Unit,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    var showDialog by remember { mutableStateOf(false) }

    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("📈 SEGUIMIENTO DE PESO", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$currentWeight", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                    Text("KG", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), color = vb.textMuted, modifier = Modifier.padding(bottom = 6.dp))
                }
            }
            LiquidGlassButton(
                text = "REGISTRAR",
                onClick = { showDialog = true },
                style = LiquidButtonStyle.Secondary,
                hazeState = hazeState
            )
        }
        
        Spacer(modifier = Modifier.height(20.dp))
        
        Box(modifier = Modifier.fillMaxWidth().height(100.dp).clip(RoundedCornerShape(12.dp)).background(vb.surfaceElevated.copy(0.2f)).padding(12.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val path = Path()
                if (logs.size > 1) {
                    val width = size.width
                    val height = size.height
                    val minW = logs.minOf { it.weight } - 2
                    val maxW = logs.maxOf { it.weight } + 2
                    val diffW = (maxW - minW).coerceAtLeast(1f)
                    
                    logs.takeLast(10).forEachIndexed { i, log ->
                        val x = i * (width / (logs.takeLast(10).size - 1).coerceAtLeast(1))
                        val y = height - ((log.weight - minW) / diffW * height)
                        if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                    drawPath(path, color = vb.accent, style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                } else {
                    drawLine(vb.textMuted.copy(0.2f), Offset(0f, size.height/2), Offset(size.width, size.height/2), strokeWidth = 2.dp.toPx())
                }
            }
            if (logs.isEmpty()) {
                Text("No hay datos suficientes", style = MaterialTheme.typography.labelSmall, color = vb.textMuted, modifier = Modifier.align(Alignment.Center))
            }
        }
    }
    
    if (showDialog) {
        WeightLogDialog(onDismiss = { showDialog = false }, onSave = { onLogWeight(it); showDialog = false })
    }
}

@Composable
private fun WeightLogDialog(onDismiss: () -> Unit, onSave: (Float) -> Unit) {
    var weight by remember { mutableStateOf("") }
    val vb = LocalVoltBodyColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Registrar peso actual") },
        text = {
            OutlinedTextField(
                value = weight,
                onValueChange = { weight = it },
                label = { Text("Peso en KG") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = vb.accent)
            )
        },
        confirmButton = {
            TextButton(onClick = { weight.toFloatOrNull()?.let { onSave(it) } }) {
                Text("GUARDAR", color = vb.accent, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("CANCELAR", color = vb.textMuted) }
        },
        containerColor = vb.surfaceElevated,
        titleContentColor = ColorWhite,
        textContentColor = ColorWhite
    )
}

@Composable
private fun MotivationCard(phrase: String, photoUrl: String?, hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Box(modifier = Modifier.fillMaxWidth().height(140.dp).clip(RoundedCornerShape(16.dp))) {
            if (!photoUrl.isNullOrEmpty()) {
                AsyncImage(model = photoUrl, contentDescription = null, modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop, alpha = 0.4f)
            } else {
                Box(modifier = Modifier.fillMaxSize().background(vb.accent.copy(0.1f)))
            }
            Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Default.FormatQuote, null, tint = vb.accent, modifier = Modifier.size(32.dp))
                Text(
                    phrase.ifBlank { "LA DISCIPLINA ES EL PUENTE ENTRE LAS METAS Y EL LOGRO." }.uppercase(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold, fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
                    color = ColorWhite,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SettingsCard(
    theme: AppTheme,
    onThemeChange: (AppTheme) -> Unit,
    notifsEnabled: Boolean,
    onNotifsToggle: () -> Unit,
    onLogout: () -> Unit,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Text("⚙️ CONFIGURACIÓN", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.textMuted)
        Spacer(modifier = Modifier.height(20.dp))
        
        Text("Personalización de color", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
        Spacer(modifier = Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            ThemeCircle(AppTheme.VERDE_NEGRO, NeonGreen, theme == AppTheme.VERDE_NEGRO, onThemeChange)
            ThemeCircle(AppTheme.AGUAMARINA_NEGRO, NeonAquamarine, theme == AppTheme.AGUAMARINA_NEGRO, onThemeChange)
            ThemeCircle(AppTheme.OCASO_NEGRO, NeonOcaso, theme == AppTheme.OCASO_NEGRO, onThemeChange)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Notificaciones de entreno", style = MaterialTheme.typography.bodyMedium, color = ColorWhite)
            Switch(checked = notifsEnabled, onCheckedChange = { onNotifsToggle() }, colors = SwitchDefaults.colors(checkedThumbColor = vb.accent))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        LiquidGlassButton(
            text = "CERRAR SESIÓN",
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth(),
            style = LiquidButtonStyle.Secondary,
            hazeState = hazeState,
            leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, null, tint = ColorError, modifier = Modifier.size(18.dp)) }
        )
    }
}

@Composable
private fun ThemeCircle(theme: AppTheme, color: Color, isSelected: Boolean, onClick: (AppTheme) -> Unit) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(color)
            .border(3.dp, if (isSelected) ColorWhite else Color.Transparent, CircleShape)
            .clickable { onClick(theme) }
    )
}
