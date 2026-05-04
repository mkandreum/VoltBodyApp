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
import com.voltbody.app.domain.model.FoodPreferences
import com.voltbody.app.ui.components.AppCard
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors

@Composable
fun FoodPreferencesCard(preferences: FoodPreferences, modifier: Modifier = Modifier) {
    val vb = LocalVoltBodyColors.current
    
    if (preferences.vegetables.isEmpty() && preferences.carbs.isEmpty() && preferences.proteins.isEmpty()) {
        return
    }

    AppCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Tus Preferencias", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ColorWhite)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PreferenceCol(title = "Verduras \uD83E\uDD66", items = preferences.vegetables, modifier = Modifier.weight(1f))
                PreferenceCol(title = "Carbos \uD83C\uDF5A", items = preferences.carbs, modifier = Modifier.weight(1f))
                PreferenceCol(title = "Proteína \uD83C\uDF57", items = preferences.proteins, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun PreferenceCol(title: String, items: List<String>, modifier: Modifier = Modifier) {
    val vb = LocalVoltBodyColors.current
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items.take(3).forEach { item ->
                Text(
                    item,
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                    color = ColorWhite,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(vb.surfaceElevated)
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                )
            }
            if (items.size > 3) {
                Text("+${items.size - 3} más", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
            }
            if (items.isEmpty()) {
                Text("-", style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
            }
        }
    }
}
