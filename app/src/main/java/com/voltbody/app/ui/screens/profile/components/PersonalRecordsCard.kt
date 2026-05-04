package com.voltbody.app.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.components.AppCard
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors
import com.voltbody.app.ui.theme.MonoMetric
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

data class PersonalRecord(
    val exerciseName: String,
    val weight: Float,
    val reps: Int,
    val date: String
)

@Composable
fun PersonalRecordsCard(
    records: List<PersonalRecord>,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current
    
    if (records.isEmpty()) return

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
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = vb.accent)
                }
                Column {
                    Text("Récords Personales", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ColorWhite)
                    Text("Top 10 levantamientos históricos", style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                records.take(10).forEachIndexed { index, pr ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = "${index + 1}",
                                style = MonoMetric.copy(fontSize = 14.sp),
                                color = if (index < 3) vb.accent else vb.textMuted,
                                modifier = Modifier.width(20.dp)
                            )
                            Column {
                                Text(pr.exerciseName, style = MaterialTheme.typography.bodyMedium, color = ColorWhite, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                val dateStr = runCatching {
                                    LocalDate.parse(pr.date.take(10)).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale("es")))
                                }.getOrDefault(pr.date.take(10))
                                Text(dateStr, style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${if (pr.weight % 1 == 0f) pr.weight.toInt() else pr.weight} kg",
                                style = MonoMetric.copy(fontSize = 16.sp),
                                color = vb.accent,
                                fontWeight = FontWeight.Bold
                            )
                            Text("${pr.reps} reps", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                        }
                    }
                }
            }
        }
    }
}
