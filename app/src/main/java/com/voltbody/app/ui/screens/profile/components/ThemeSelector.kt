package com.voltbody.app.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.domain.model.AppTheme
import com.voltbody.app.ui.components.LiquidGlassCard
import com.voltbody.app.ui.components.neuroRaised
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors
import dev.chrisbanes.haze.HazeState

@Composable
fun ThemeSelector(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    val themes = listOf(
        AppTheme.AGUAMARINA_NEGRO to "AGUAMARINA",
        AppTheme.VERDE_NEGRO to "VERDE VOLT",
        AppTheme.OCASO_NEGRO to "OCASO"
    )

    LiquidGlassCard(modifier = modifier, hazeState = hazeState) {
        Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).neuroRaised(cornerRadius = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.Palette, null, tint = vb.accent, modifier = Modifier.size(20.dp))
                }
                Text(
                    "TEMA VISUAL", 
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    ), 
                    color = ColorWhite
                )
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                themes.forEach { (theme, label) ->
                    val isSelected = theme == selectedTheme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(if (isSelected) vb.accent.copy(0.15f) else vb.surfaceElevated.copy(alpha = 0.3f))
                            .clickable { onThemeSelected(theme) }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(if (isSelected) vb.accent else Color.Transparent)
                                .border(1.dp, if (isSelected) vb.accent else vb.border, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isSelected) {
                                Box(Modifier.size(10.dp).clip(CircleShape).background(vb.bg))
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Text(
                            label, 
                            color = if (isSelected) vb.accent else ColorWhite, 
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = if (isSelected) FontWeight.Black else FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        )
                    }
                }
            }
        }
    }
}
