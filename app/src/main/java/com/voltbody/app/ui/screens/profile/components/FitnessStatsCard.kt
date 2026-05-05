package com.voltbody.app.ui.screens.profile.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.components.LiquidGlassCard
import com.voltbody.app.ui.components.LiquidProgressBar
import com.voltbody.app.ui.components.GlowText
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors
import com.voltbody.app.ui.theme.MonoMetric
import dev.chrisbanes.haze.HazeState

@Composable
fun FitnessStatsCard(
    totalLogs: Int,
    weeklyGoalProgress: Float,
    weightLogsCount: Int,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    
    // Fuerza: min(100, totalLogs * 4)
    val force = minOf(100f, totalLogs * 4f)
    // Consistencia: weeklyGoalProgress
    val consistency = weeklyGoalProgress
    // Energía: min(100, max(30, weightLogsCount * 18))
    val energy = minOf(100f, maxOf(30f, weightLogsCount * 18f))

    LiquidGlassCard(modifier = modifier, hazeState = hazeState) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "ESTADÍSTICAS DE FORMA", 
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ), 
                color = ColorWhite
            )
            
            StatBar(label = "FUERZA", value = force, color = Color(0xFFF87171), icon = "💪") // Red
            StatBar(label = "CONSISTENCIA", value = consistency, color = Color(0xFF34D399), icon = "🔥") // Green
            StatBar(label = "ENERGÍA", value = energy, color = Color(0xFF60A5FA), icon = "⚡") // Blue
        }
    }
}

@Composable
private fun StatBar(label: String, value: Float, color: Color, icon: String) {
    val vb = LocalVoltBodyColors.current
    
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(icon, style = MaterialTheme.typography.bodyLarge)
                Text(
                    label, 
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold), 
                    color = vb.textMuted
                )
            }
            GlowText(
                "${value.toInt()}/100", 
                style = MonoMetric.copy(fontSize = 12.sp, fontWeight = FontWeight.Black)
            )
        }
        
        LiquidProgressBar(
            progress = value / 100f,
            height = 10.dp
        )
    }
}
