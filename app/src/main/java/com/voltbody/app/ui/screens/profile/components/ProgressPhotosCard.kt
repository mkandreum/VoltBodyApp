package com.voltbody.app.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.voltbody.app.domain.model.ProgressPhoto
import com.voltbody.app.ui.components.AppCard
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors
import java.time.LocalDate

@Composable
fun ProgressPhotosCard(
    photos: List<ProgressPhoto>,
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current

    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.CameraAlt, null, tint = vb.accent)
                    Text("Fotos de Progreso", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ColorWhite)
                }
                IconButton(onClick = onAddPhoto, modifier = Modifier.size(32.dp).clip(RoundedCornerShape(8.dp)).background(vb.accent.copy(0.1f))) {
                    Icon(Icons.Filled.Add, null, tint = vb.accent, modifier = Modifier.size(20.dp))
                }
            }

            if (photos.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(vb.surface)
                        .clickable { onAddPhoto() },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.CameraAlt, null, tint = vb.textMuted, modifier = Modifier.size(32.dp))
                        Text("Sube tu primera foto", style = MaterialTheme.typography.bodySmall, color = vb.textMuted)
                    }
                }
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 4.dp)
                ) {
                    items(photos.sortedByDescending { it.date }) { photo ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(width = 110.dp, height = 150.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(vb.surface)
                                    .border(1.dp, vb.border, RoundedCornerShape(12.dp))
                            ) {
                                AsyncImage(
                                    model = photo.url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                runCatching { LocalDate.parse(photo.date.take(10)).toString() }.getOrDefault(""),
                                style = MaterialTheme.typography.labelSmall,
                                color = vb.textMuted,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
