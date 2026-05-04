package com.voltbody.app.ui.screens.profile.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voltbody.app.ui.components.AppCard
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors
import androidx.compose.animation.core.spring
import androidx.compose.ui.graphics.graphicsLayer
import com.voltbody.app.util.HapticType
import com.voltbody.app.util.perform
import com.voltbody.app.util.rememberHaptic

@Composable
fun WeeklyGoalsCard(
    completedGoals: Set<String>,
    onToggleGoal: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current
    val haptic = rememberHaptic()
    
    val predefinedGoals = listOf(
        "Entrenar 3 días",
        "Beber 2L de agua diarios",
        "Cumplir proteína 5 días",
        "Dormir 7h+ 5 días",
        "Completar sesión extra"
    )
    
    val progress = if (predefinedGoals.isNotEmpty()) {
        predefinedGoals.count { completedGoals.contains(it) }.toFloat() / predefinedGoals.size
    } else 0f
    
    val animatedProgress by animateFloatAsState(targetValue = progress, label = "goals_progress")

    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(vb.accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Flag, contentDescription = null, tint = vb.accent)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text("Metas Semanales", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ColorWhite)
                    
                    // Progress Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .padding(top = 6.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(vb.surfaceElevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animatedProgress)
                                .clip(RoundedCornerShape(2.dp))
                                .background(vb.accent)
                        )
                    }
                }
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, color = vb.accent, fontWeight = FontWeight.Bold)
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isChecked) vb.surfaceElevated else vb.surface)
                            .clickable {
                                haptic.perform(HapticType.TICK)
                                onToggleGoal(goal)
                            }
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            goal,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (isChecked) ColorWhite else vb.textMuted
                        )
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .graphicsLayer {
                                    scaleX = checkScale
                                    scaleY = checkScale
                                }
                                .clip(CircleShape)
                                .background(if (isChecked) vb.accent else vb.surfaceElevated),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(Icons.Filled.Check, contentDescription = null, tint = vb.bg, modifier = Modifier.size(16.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
