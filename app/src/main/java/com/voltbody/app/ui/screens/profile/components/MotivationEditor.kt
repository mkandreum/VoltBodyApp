package com.voltbody.app.ui.screens.profile.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.FormatQuote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.voltbody.app.ui.components.LiquidGlassCard
import com.voltbody.app.ui.components.LiquidGlassButton
import com.voltbody.app.ui.components.LiquidButtonStyle
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors
import dev.chrisbanes.haze.HazeState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotivationEditor(
    phrase: String,
    photoUrl: String?,
    onSave: (String, String?) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null
) {
    val vb = LocalVoltBodyColors.current
    var currentPhrase by remember { mutableStateOf(phrase) }
    var currentPhoto by remember { mutableStateOf(photoUrl) }
    var isEditing by remember { mutableStateOf(false) }

    LiquidGlassCard(modifier = modifier, hazeState = hazeState) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.FormatQuote, null, tint = vb.accent)
                    Text(
                        "TU MOTIVACIÓN", 
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        ), 
                        color = ColorWhite
                    )
                }
                TextButton(onClick = { 
                    if (isEditing) onSave(currentPhrase, currentPhoto)
                    isEditing = !isEditing 
                }) {
                    Text(if (isEditing) "GUARDAR" else "EDITAR", color = vb.accent, style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Black))
                }
            }

            if (isEditing) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = currentPhrase,
                        onValueChange = { currentPhrase = it },
                        label = { Text("Frase motivacional") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = vb.accent,
                            unfocusedBorderColor = vb.border
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    OutlinedTextField(
                        value = currentPhoto ?: "",
                        onValueChange = { currentPhoto = it.ifBlank { null } },
                        label = { Text("URL de foto (opcional)") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = vb.accent,
                            unfocusedBorderColor = vb.border
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(vb.surfaceElevated.copy(alpha = 0.3f))
                        .border(1.dp, ColorWhite.copy(alpha = 0.1f), RoundedCornerShape(18.dp))
                ) {
                    if (photoUrl != null) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f))
                        )
                    }
                    
                    Column(
                        modifier = Modifier.fillMaxSize().padding(24.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.FormatQuote, null, tint = vb.accent.copy(0.8f), modifier = Modifier.size(40.dp))
                        Spacer(Modifier.height(12.dp))
                        Text(
                            phrase.uppercase(),
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Black,
                                letterSpacing = 0.5.sp
                            ),
                            color = ColorWhite,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
