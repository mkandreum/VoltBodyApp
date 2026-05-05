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
import com.voltbody.app.ui.components.LiquidGlassCard
import com.voltbody.app.ui.components.GlowText
import com.voltbody.app.ui.components.neuroRaised
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors
import com.voltbody.app.ui.theme.MonoMetric
import dev.chrisbanes.haze.HazeState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun PersonalRecordsCard(
    records: List<PersonalRecord>,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    
    if (records.isEmpty()) return

    LiquidGlassCard(modifier = modifier, hazeState = hazeState) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Box(
                    modifier = Modifier.size(44.dp).neuroRaised(cornerRadius = 22.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.EmojiEvents, contentDescription = null, tint = vb.accent, modifier = Modifier.size(20.dp))
                }
                Column {
                    Text(
                        "RÉCORDS PERSONALES", 
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ), 
                        color = ColorWhite
                    )
                    Text("Tus hitos históricos más relevantes", style = MaterialTheme.typography.labelSmall, color = vb.textMuted)
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                records.take(10).forEachIndexed { index, pr ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(if (index < 3) vb.accent.copy(0.1f) else vb.surfaceElevated.copy(0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MonoMetric.copy(fontSize = 12.sp, fontWeight = FontWeight.Black),
                                    color = if (index < 3) vb.accent else vb.textMuted
                                )
                            }
                            Column {
                                Text(
                                    pr.exerciseName.uppercase(), 
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black, letterSpacing = 0.5.sp), 
                                    color = ColorWhite, 
                                    maxLines = 1, 
                                    overflow = TextOverflow.Ellipsis
                                )
                                val dateStr = runCatching {
                                    LocalDate.parse(pr.date.take(10)).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale("es")))
                                }.getOrDefault(pr.date.take(10))
                                Text(dateStr.uppercase(), style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp), color = vb.textMuted)
                            }
                        }
                        
                        Column(horizontalAlignment = Alignment.End) {
                            GlowText(
                                "${if (pr.weight % 1 == 0f) pr.weight.toInt() else pr.weight} KG",
                                style = MonoMetric.copy(fontSize = 16.sp, fontWeight = FontWeight.Black)
                            )
                            Text("${pr.reps} REPS", style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp, fontWeight = FontWeight.Bold), color = vb.textMuted)
                        }
                    }
                }
            }
        }
    }
}
