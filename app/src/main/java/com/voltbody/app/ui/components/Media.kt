package com.voltbody.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.voltbody.app.ui.theme.LocalVoltBodyColors

@Composable
fun VoltBodyImage(
    model: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val vb = LocalVoltBodyColors.current
    var isLoading by remember { mutableStateOf(true) }
    var isError by remember { mutableStateOf(false) }

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        if (model != null) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(model)
                    .crossfade(true)
                    .build(),
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
                contentScale = contentScale,
                onLoading = { isLoading = true; isError = false },
                onSuccess = { isLoading = false; isError = false },
                onError = { isLoading = false; isError = true }
            )
        } else {
            isError = true
        }

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(vb.surfaceElevated.copy(0.3f))
            )
        }

        if (isError) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(vb.surfaceElevated.copy(0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Image,
                    null,
                    tint = vb.textMuted.copy(0.3f),
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
