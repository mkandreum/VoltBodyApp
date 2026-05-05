package com.voltbody.app.ui.screens.diet.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.components.LiquidGlassCard
import com.voltbody.app.ui.components.neuroRaised
import com.voltbody.app.ui.theme.*

@Composable
fun MacroTipsCard(modifier: Modifier = Modifier) {
    val vb = LocalVoltBodyColors.current
    var expanded by remember { mutableStateOf(false) }

    LiquidGlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier.size(36.dp).neuroRaised(cornerRadius = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Filled.Info, contentDescription = null, tint = ColorInfo, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        "TIPS DE INTERCAMBIO", 
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp), 
                        color = ColorWhite
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = "Expandir/Colapsar",
                    tint = vb.accent,
                    modifier = Modifier.size(20.dp)
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    MacroTipRow(
                        title = "+25G PROTEÍNA",
                        equivalences = listOf("120g Pollo/Pavo", "1 Scoop Whey", "100g Atún"),
                        color = ColorProtein
                    )
                    MacroTipRow(
                        title = "+30G CARBS",
                        equivalences = listOf("45g Avena", "130g Arroz cocido", "150g Patata"),
                        color = ColorCarb
                    )
                    MacroTipRow(
                        title = "+10G GRASAS",
                        equivalences = listOf("15g Frutos secos", "12g Aceite de oliva", "70g Aguacate"),
                        color = ColorFat
                    )
                }
            }
        }
    }
}

@Composable
private fun MacroTipRow(title: String, equivalences: List<String>, color: androidx.compose.ui.graphics.Color) {
    val vb = LocalVoltBodyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(vb.surfaceElevated.copy(alpha = 0.3f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(14.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), color = color)
        Column(horizontalAlignment = Alignment.End) {
            equivalences.forEach { eq ->
                Text("O $eq".uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = vb.textMuted)
            }
        }
    }
}
