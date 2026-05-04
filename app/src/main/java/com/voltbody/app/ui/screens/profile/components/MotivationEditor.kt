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
import coil.compose.AsyncImage
import com.voltbody.app.ui.components.AppCard
import com.voltbody.app.ui.theme.ColorWhite
import com.voltbody.app.ui.theme.LocalVoltBodyColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MotivationEditor(
    phrase: String,
    photoUrl: String?,
    onSave: (String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val vb = LocalVoltBodyColors.current
    var currentPhrase by remember { mutableStateOf(phrase) }
    var currentPhoto by remember { mutableStateOf(photoUrl) }
    var isEditing by remember { mutableStateOf(false) }

    AppCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Filled.FormatQuote, null, tint = vb.accent)
                    Text("Tu Motivación", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), color = ColorWhite)
                }
                TextButton(onClick = { 
                    if (isEditing) onSave(currentPhrase, currentPhoto)
                    isEditing = !isEditing 
                }) {
                    Text(if (isEditing) "Guardar" else "Editar", color = vb.accent)
                }
            }

            if (isEditing) {
                OutlinedTextField(
                    value = currentPhrase,
                    onValueChange = { currentPhrase = it },
                    label = { Text("Frase motivacional") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = vb.accent,
                        unfocusedBorderColor = vb.border
                    )
                )
                OutlinedTextField(
                    value = currentPhoto ?: "",
                    onValueChange = { currentPhoto = it.ifBlank { null } },
                    label = { Text("URL de foto (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = vb.accent,
                        unfocusedBorderColor = vb.border
                    )
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(vb.surfaceElevated)
                        .border(1.dp, vb.border, RoundedCornerShape(12.dp))
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
                                .background(androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.4f))
                        )
                    }
                    
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Filled.FormatQuote, null, tint = vb.accent.copy(0.6f), modifier = Modifier.size(32.dp))
                        Spacer(Modifier.height(8.dp))
                        Text(
                            phrase,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                            color = ColorWhite,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
