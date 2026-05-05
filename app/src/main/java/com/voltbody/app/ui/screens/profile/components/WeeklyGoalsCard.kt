package com.voltbody.app.ui.screens.profile.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.components.LiquidGlassCard
import com.voltbody.app.ui.components.LiquidProgressBar
import com.voltbody.app.ui.components.GlowText
import com.voltbody.app.ui.components.neuroRaised
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors
import com.voltbody.app.util.HapticType
import com.voltbody.app.util.rememberHaptic
import dev.chrisbanes.haze.HazeState

@Composable
fun WeeklyGoalsCard(
    completedGoals: Set<String>,
    onToggleGoal: (String) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    val haptic = rememberHaptic()
    
    val predefinedGoals = listOf(
        "ENTRENAR 3 DÍAS",
        "BEBER 2L DE AGUA DIARIOS",
        "CUMPLIR PROTEÍNA 5 DÍAS",
        "DORMIR 7H+ 5 DÍAS",
        "COMPLETAR SESIÓN EXTRA"
    )
    
    val progress = if (predefinedGoals.isNotEmpty()) {
        predefinedGoals.count { completedGoals.contains(it) }.toFloat() / predefinedGoals.size
    } else 0f
    
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "goals_progress")

    LiquidGlassCard(modifier = modifier, hazeState = hazeState) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .neuroRaised(cornerRadius = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = null, tint = vb.accent, modifier = Modifier.size(20.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "METAS SEMANALES", 
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ), 
                        color = ColorWhite
                    )
                    Spacer(Modifier.height(4.dp))
                    LiquidProgressBar(progress = animatedProgress, height = 6.dp)
                }
                GlowText(
                    "${(progress * 100).toInt()}%", 
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                predefinedGoals.forEach { goal ->
                    val isChecked = completedGoals.contains(goal)
                    val checkScale by animateFloatAsState(
                        targetValue = if (isChecked) 1f else 0.8f,
                        animationSpec = spring(dampingRatio = 0.5f, stiffness = 500f),
                        label = "check_bounce_$goal"
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isChecked) vb.accent.copy(alpha = 0.15f) else vb.surfaceElevated.copy(alpha = 0.3f))
                            .clickable {
                                haptic.perform(HapticType.TICK)
                                onToggleGoal(goal)
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            goal,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isChecked) FontWeight.Black else FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            ),
                            color = if (isChecked) vb.accent else vb.textMuted
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .graphicsLayer {
                                    scaleX = checkScale
                                    scaleY = checkScale
                                }
                                .clip(CircleShape)
                                .background(if (isChecked) vb.accent else vb.surfaceElevated)
                                .then(if (!isChecked) Modifier.border(1.dp, vb.border, CircleShape) else Modifier),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = vb.bg, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
