package com.voltbody.app.ui.screens.home

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
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.voltbody.app.ui.components.*
import com.voltbody.app.ui.theme.*
import dev.chrisbanes.haze.HazeState

@Composable
fun HomeScreen(
    onNavigateToWorkout: (String) -> Unit,
    onNavigateToDiet: () -> Unit,
    onNavigateToProfile: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val vb = LocalVoltBodyColors.current
    val state by viewModel.state.collectAsState()

    LiquidGlassScaffold(
        background = {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.size(500.dp).align(Alignment.TopEnd).background(vb.accent.copy(0.05f), CircleShape).offset(100.dp, (-150).dp))
                Box(modifier = Modifier.size(300.dp).align(Alignment.CenterStart).background(ColorInfo.copy(0.03f), CircleShape).offset((-100).dp, 50.dp))
            }
        }
    ) { hazeState ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 70.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item {
                HomeHeader(name = state.userName, onProfileClick = onNavigateToProfile)
            }

            item {
                HeroMotivationCard(
                    phrase = state.motivationPhrase,
                    workout = state.todayWorkout,
                    progress = 0f, // From state if available, or calc
                    onStart = { state.todayWorkout?.let { onNavigateToWorkout(it.id) } },
                    hazeState = hazeState
                )
            }

            item {
                BentoGrid(state = state, hazeState = hazeState, onNavigateToDiet = onNavigateToDiet)
            }

            item {
                LevelProgressCard(
                    current = state.xpCurrent,
                    target = state.xpToNext,
                    level = state.xpLevel,
                    hazeState = hazeState
                )
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

@Composable
private fun HomeHeader(name: String, onProfileClick: () -> Unit) {
    val vb = LocalVoltBodyColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("HOLA,", style = UppercaseLabel.copy(fontSize = 12.sp), color = vb.textMuted)
            Text(name.uppercase().ifEmpty { "GUERRERO" }, style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black), color = ColorWhite)
        }
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(vb.surfaceElevated.copy(0.3f))
                .border(1.dp, vb.border.copy(0.5f), CircleShape)
                .clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.Notifications, null, tint = ColorWhite, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun HeroMotivationCard(
    phrase: String,
    workout: TodayWorkoutInfo?,
    progress: Float,
    onStart: () -> Unit,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), accentGlow = workout != null, hazeState = hazeState) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.weight(1f)) {
                Text("🔥 MODO GUERRERO", style = UppercaseLabel.copy(fontSize = 10.sp), color = vb.accent)
                Text(
                    if (workout != null) "HOY: ${workout.name.uppercase()}" else "HOY: DESCANSO",
                    style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Black),
                    color = ColorWhite
                )
            }
            VoltBodyCircularProgress(value = progress * 100, size = 56.dp)
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            phrase.ifEmpty { "La disciplina es el puente entre tus metas y tus logros." },
            style = MaterialTheme.typography.bodyMedium.copy(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic),
            color = vb.textMuted
        )
        
        if (workout != null) {
            Spacer(modifier = Modifier.height(24.dp))
            LiquidGlassButton(
                text = "EMPEZAR ENTRENAMIENTO",
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                hazeState = hazeState
            )
        }
    }
}

@Composable
private fun BentoGrid(
    state: HomeState,
    hazeState: HazeState? = null,
    onNavigateToDiet: () -> Unit
) {
    val vb = LocalVoltBodyColors.current
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth().height(160.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LiquidGlassCard(modifier = Modifier.weight(0.4f).fillMaxHeight(), hazeState = hazeState) {
                Icon(Icons.Default.LocalDrink, null, tint = ColorInfo, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.weight(1f))
                Text("2.4L", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                Text("HIDRATACIÓN", style = UppercaseLabel.copy(fontSize = 8.sp), color = vb.textMuted)
            }
            LiquidGlassCard(modifier = Modifier.weight(0.6f).fillMaxHeight(), hazeState = hazeState) {
                Text("CONSISTENCIA", style = UppercaseLabel.copy(fontSize = 8.sp), color = vb.textMuted)
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    val maxVol = state.dailyVolumeKg.maxOrNull()?.coerceAtLeast(1f) ?: 1f
                    state.dailyVolumeKg.takeLast(7).forEach { vol ->
                        val h = (vol / maxVol).coerceAtLeast(0.1f)
                        Box(modifier = Modifier.weight(1f).fillMaxHeight(h).clip(RoundedCornerShape(4.dp)).background(if (vol > 0) vb.accent else vb.textMuted.copy(0.2f)))
                    }
                }
            }
        }
        
        LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState, onClick = onNavigateToDiet) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(modifier = Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(vb.accent.copy(0.1f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Restaurant, null, tint = vb.accent)
                }
                Column {
                    Text("PRÓXIMA COMIDA", style = UppercaseLabel.copy(fontSize = 8.sp), color = vb.textMuted)
                    Text("POLLO CON ARROZ", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold), color = ColorWhite)
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, null, tint = vb.textMuted)
            }
        }
        
        Row(modifier = Modifier.fillMaxWidth().height(120.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LiquidGlassCard(modifier = Modifier.weight(1f).fillMaxHeight(), hazeState = hazeState) {
                Text("PESO", style = UppercaseLabel.copy(fontSize = 8.sp), color = vb.textMuted)
                Spacer(modifier = Modifier.weight(1f))
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("78.4", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                    Text("KG", style = MaterialTheme.typography.labelSmall, color = vb.textMuted, modifier = Modifier.padding(bottom = 4.dp))
                }
            }
            LiquidGlassCard(modifier = Modifier.weight(1f).fillMaxHeight(), hazeState = hazeState, accentGlow = true) {
                Icon(Icons.Default.AutoAwesome, null, tint = vb.accent, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.weight(1f))
                Text("COACH IA", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = ColorWhite)
                Text("ANALIZANDO...", style = UppercaseLabel.copy(fontSize = 7.sp), color = vb.accent)
            }
        }
    }
}

@Composable
private fun LevelProgressCard(current: Int, target: Int, level: Int, hazeState: HazeState? = null) {
    val vb = LocalVoltBodyColors.current
    LiquidGlassCard(modifier = Modifier.fillMaxWidth(), hazeState = hazeState) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("NIVEL $level", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black), color = ColorWhite)
            Text("$current / $target XP", style = MonoMetric.copy(fontSize = 11.sp), color = vb.accent)
        }
        Spacer(modifier = Modifier.height(12.dp))
        LiquidProgressBar(progress = if (target > 0) current.toFloat() / target else 0f)
        Spacer(modifier = Modifier.height(8.dp))
        Text("Te faltan ${target - current} XP para el nivel ${level + 1}", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
    }
}
