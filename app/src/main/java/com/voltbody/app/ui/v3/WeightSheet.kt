package com.voltbody.app.ui.v3

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.Repository
import com.voltbody.app.data.WeightLog
import com.voltbody.app.health.healthManager
import com.voltbody.app.ui.screens.home.VoltPrefs
import com.voltbody.app.ui.theme.Volt
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.util.Locale

/** Paso del stepper de peso (kg). */
private const val WEIGHT_STEP = 0.1

/**
 * Hoja de Peso rediseñada de cero para el cristal esmerilado: cabecera grande, cifra enorme,
 * chip de variación, mini gráfica de evolución y registro con botones ± grandes.
 * Sin degradados ni bordes: solo color solido y superficies translucidas.
 */
@Composable
fun WeightSheet(repo: Repository, logs: List<WeightLog>) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()

    val last = logs.lastOrNull()?.weight
    val prev = if (logs.size >= 2) logs[logs.size - 2].weight else null
    var value by remember(last) { mutableDoubleStateOf(last ?: 70.0) }

    val delta = prev?.let { value - it }
    val bias = when {
        delta == null -> 0
        delta < -0.05 -> -1
        delta > 0.05 -> 1
        else -> 0
    }
    val chipTint = when (bias) {
        -1 -> Volt.accent
        1 -> Volt.orange
        else -> Volt.textDim
    }
    val recent = remember(logs) { logs.takeLast(30) } // un mes (incluye lo importado de Health Connect)

    SheetScaffold("Tu peso, hoy", eyebrow = "Tu progreso", mixTitle = true) {

        // Cifra enorme.
        Column(Modifier.fillMaxWidth().enter(1), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.Bottom) {
                RollingText(trim(value), style = T.heroXL.copy(fontSize = 68.sp, lineHeight = 70.sp), color = Volt.text)
                Spacer(Modifier.width(6.dp))
                Text("kg", style = T.section, color = Volt.textDim, modifier = Modifier.padding(bottom = 12.dp))
            }
            Spacer(Modifier.height(10.dp))
            // Chip de variación (solido translucido, sin borde).
            Row(
                Modifier.clip(CircleShape).background(chipTint.copy(alpha = 0.14f)).padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    when (bias) {
                        -1 -> Icons.AutoMirrored.Outlined.TrendingDown
                        1 -> Icons.AutoMirrored.Outlined.TrendingUp
                        else -> Icons.AutoMirrored.Outlined.TrendingFlat
                    },
                    contentDescription = null, tint = chipTint, modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    delta?.let { "${String.format(ES, "%+.1f", it)} kg" } ?: "Sin registro previo",
                    style = T.cardTitle, color = chipTint,
                )
            }
        }

        // Mini gráfica de evolución.
        if (recent.size >= 2) {
            Column(Modifier.fillMaxWidth().enter(2)) {
                Text("EVOLUCIÓN", style = T.label, color = Volt.textDim, modifier = Modifier.padding(start = 6.dp))
                Spacer(Modifier.height(8.dp))
                MockBars(
                    values = recent.map { it.weight.toFloat() },
                    highlight = recent.lastIndex,
                    bubble = trim(recent.last().weight),
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    onSelect = { i -> recent.getOrNull(i)?.let { value = it.weight } },
                )
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(recent.first().date.takeLast(5), style = T.caption, color = Volt.textDim)
                    Text(recent.last().date.takeLast(5), style = T.caption, color = Volt.textDim)
                }
            }
        } else {
            Card(Modifier.fillMaxWidth().enter(2)) {
                Text("Registra peso varios días para ver tu evolución.", style = T.body, color = Volt.textDim)
            }
        }

        // Registro con botones ± grandes.
        Column(Modifier.fillMaxWidth().enter(3), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("REGISTRO DE HOY", style = T.label, color = Volt.textDim)
            Spacer(Modifier.height(10.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StepButton(Icons.Filled.Remove, "Bajar ${trim(WEIGHT_STEP)} kg", Modifier.weight(1f)) {
                    value = ((value - WEIGHT_STEP) * 10).round1().coerceAtLeast(20.0)
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(trim(value), style = T.hero.copy(fontSize = 30.sp), color = Volt.text, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("kg", style = T.caption, color = Volt.textDim)
                }
                StepButton(Icons.Filled.Add, "Subir ${trim(WEIGHT_STEP)} kg", Modifier.weight(1f)) {
                    value = ((value + WEIGHT_STEP) * 10).round1().coerceAtMost(400.0)
                }
            }
            Spacer(Modifier.height(12.dp))
            LimeButton("Registrar ${trim(value)} kg", onClick = {
                val kg = value.round1()
                scope.launch {
                    repo.addWeight(LocalDate.now().toString(), kg)
                    repo.toast("success", "Peso registrado: ${trim(kg)} kg ⚖️")
                    if (VoltPrefs.healthEnabled(ctx)) runCatching { ctx.healthManager.writeWeight(kg) }
                }
            })
        }
    }
}

@Composable
private fun StepButton(icon: ImageVector, desc: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier.height(84.dp).clip(RoundedCornerShape(26.dp)).background(Volt.surface2).tap(0.92f, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, desc, tint = Volt.text, modifier = Modifier.size(32.dp))
    }
}

private val ES = Locale("es", "ES")

private fun Double.round1(): Double = kotlin.math.round(this * 10.0) / 10.0
