package com.voltbody.app.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.voltbody.app.domain.model.AppTheme
import com.voltbody.app.ui.components.AppCard
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors

@Composable
fun ThemeSelector(
    selectedTheme: AppTheme,
    onThemeSelected: (AppTheme) -> Unit,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current
    val themes = listOf(
        AppTheme.AGUAMARINA_NEGRO to "🌊 Aguamarina",
        AppTheme.VERDE_NEGRO to "💚 Verde Volt",
        AppTheme.OCASO_NEGRO to "🌅 Ocaso"
    )

    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(Icons.Filled.Palette, null, tint = vb.accent)
                Text("Tema Visual", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ColorWhite)
            }
            
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                themes.forEach { (theme, label) ->
                    val isSelected = theme == selectedTheme
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) vb.accent.copy(0.1f) else vb.surface)
                            .border(1.dp, if (isSelected) vb.accent else vb.border, RoundedCornerShape(12.dp))
                            .clickable { onThemeSelected(theme) }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isSelected,
                            onClick = { onThemeSelected(theme) },
                            colors = RadioButtonDefaults.colors(selectedColor = vb.accent)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(label, color = if (isSelected) vb.accent else ColorWhite, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}
