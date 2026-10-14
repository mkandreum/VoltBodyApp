package com.voltbody.app.ui.v3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.rememberPulse
import com.voltbody.app.ui.theme.Volt
import kotlinx.coroutines.launch

/**
 * Pantalla mientras llega el perfil del servidor. Evita mandar al onboarding a un usuario que ya tiene plan
 * cuando la red va lenta o falla (y que lo sobrescriba sin querer).
 */
@Composable
fun LoadingProfile(repo: Repository, error: String?) {
    val scope = rememberCoroutineScope()
    var retrying by remember { mutableStateOf(false) }
    val pulse = rememberPulse(0.92f, 1.08f, 900)
    Column(
        Modifier.fillMaxSize().background(Volt.bg).statusBarsPadding().navigationBarsPadding().padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(84.dp).graphicsLayer { scaleX = pulse.value; scaleY = pulse.value }.clip(CircleShape).background(Volt.accent),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Filled.Bolt, null, tint = Color(0xFF0A0A0A), modifier = Modifier.size(44.dp)) }
        Spacer(Modifier.height(24.dp))
        if (error == null) {
            Text("Cargando tu plan…", style = T.section, color = Volt.text)
            Spacer(Modifier.height(16.dp))
            VoltPulse(Modifier.fillMaxWidth(0.6f).height(28.dp))
        } else {
            Text("No pudimos conectar", style = T.screen, color = Volt.text, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(error, style = T.body, color = Volt.textDim, textAlign = TextAlign.Center)
            Spacer(Modifier.height(24.dp))
            LimeButton("Reintentar", {
                retrying = true
                scope.launch { repo.update { it.copy(loadError = null) }; repo.refreshAll(); retrying = false }
            }, loading = retrying)
            Spacer(Modifier.height(10.dp))
            Text("Cerrar sesión", style = T.body, color = Volt.textDim, modifier = Modifier.tap(0.95f) { repo.logout() }.padding(10.dp))
        }
    }
}
