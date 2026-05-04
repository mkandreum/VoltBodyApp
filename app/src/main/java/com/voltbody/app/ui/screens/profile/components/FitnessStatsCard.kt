package com.voltbody.app.ui.screens.profile.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voltbody.app.ui.components.AppCard
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors
import com.voltbody.app.ui.theme.MonoMetric

@Composable
fun FitnessStatsCard(
    totalLogs: Int,
    weeklyGoalProgress: Float,
    weightLogsCount: Int,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current
    
    // Fuerza: min(100, totalLogs * 4)
    val force = minOf(100f, totalLogs * 4f)
    // Consistencia: weeklyGoalProgress
    val consistency = weeklyGoalProgress
    // Energía: min(100, max(30, weightLogsCount * 18))
    val energy = minOf(100f, maxOf(30f, weightLogsCount * 18f))

    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Estadísticas de Forma", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ColorWhite)
            
            StatBar(label = "Fuerza", value = force, color = Color(0xFFF87171), icon = "💪") // Red
            StatBar(label = "Consistencia", value = consistency, color = Color(0xFF34D399), icon = "🔥") // Green
            StatBar(label = "Energía", value = energy, color = Color(0xFF60A5FA), icon = "⚡") // Blue
        }
    }
}

@Composable
private fun StatBar(label: String, value: Float, color: Color, icon: String) {
    val vb = LocalVoltBodyColors.current
    var startAnim by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { startAnim = true }
    
    val animatedProgress by animateFloatAsState(
        targetValue = if (startAnim) value / 100f else 0f,
        animationSpec = tween(1000),
        label = "stat_bar_$label"
    )

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(icon, style = MaterialTheme.typography.bodyMedium)
                Text(label, style = MaterialTheme.typography.labelMedium, color = vb.textMuted)
            }
            Text("${value.toInt()}/100", style = MonoMetric, color = ColorWhite)
        }
        
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(vb.surfaceElevated)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}
