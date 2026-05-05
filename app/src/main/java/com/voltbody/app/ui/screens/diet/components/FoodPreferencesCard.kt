package com.voltbody.app.ui.screens.diet.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.domain.model.FoodPreferences
import com.voltbody.app.ui.components.LiquidGlassCard
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors

@Composable
fun FoodPreferencesCard(preferences: FoodPreferences, modifier: Modifier = Modifier) {
    val vb = LocalVoltBodyColors.current
    
    if (preferences.vegetables.isEmpty() && preferences.carbs.isEmpty() && preferences.proteins.isEmpty()) {
        return
    }

    LiquidGlassCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(
                "TUS PREFERENCIAS", 
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp), 
                color = ColorWhite
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                PreferenceCol(title = "VERDURAS \uD83E\uDD66", items = preferences.vegetables, modifier = Modifier.weight(1f))
                PreferenceCol(title = "CARBOS \uD83C\uDF5A", items = preferences.carbs, modifier = Modifier.weight(1f))
                PreferenceCol(title = "PROTS \uD83C\uDF57", items = preferences.proteins, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PreferenceCol(title: String, items: List<String>, modifier: Modifier = Modifier) {
    val vb = LocalVoltBodyColors.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            title, 
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black), 
            color = vb.textMuted
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items.take(3).forEach { item ->
                Text(
                    item.uppercase(),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, fontSize = 9.sp),
                    color = ColorWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(vb.surfaceElevated.copy(alpha = 0.4f))
                        .padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }
            if (items.size > 3) {
                Text("+${items.size - 3} MÁS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Black), color = vb.textMuted)
            }
            if (items.isEmpty()) {
                Text("-", style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
            }
        }
    }
}
