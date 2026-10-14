package com.voltbody.app.ui.v3

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.voltbody.app.util.Haptics
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import kotlinx.coroutines.launch

/**
 * Hoja inferior de cristal esmerilado real: no es un ModalBottomSheet (que se dibuja en otra
 * ventana y no puede desenfocar el fondo), sino un overlay dentro de la misma composicion.
 * El fondo de la app lleva [dev.chrisbanes.haze.hazeSource] y aqui se aplica
 * [dev.chrisbanes.haze.hazeEffect], de modo que se ve la pantalla desenfocada detras.
 *
 * Entra con muelle (spring) y el desenfoque se "enfoca" al aparecer; haptico al abrir y cerrar.
 */
@Composable
fun GlassSheet(haze: HazeState, onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val a = remember { Animatable(0f) }
    var closing by remember { mutableStateOf(false) }
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.9f).dp

    LaunchedEffect(Unit) {
        Haptics.light(ctx)
        a.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 380f))
    }

    fun close() {
        if (closing) return
        closing = true
        Haptics.selection(ctx)
        scope.launch {
            a.animateTo(0f, tween(180))
            onDismiss()
        }
    }

    BackHandler { close() }

    val shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
    // El desenfoque (y el tinte) se animan: el cristal se "esmerila" al entrar.
    val glass = HazeStyle(
        backgroundColor = Color(0xE00E0E0E),
        tint = HazeTint(Color.White.copy(alpha = 0.03f + 0.05f * a.value)),
        blurRadius = (4f + 40f * a.value).dp,
        noiseFactor = 0.12f,
        fallbackTint = HazeTint(Color(0xF50E0E0E)),
    )

    Box(Modifier.fillMaxSize()) {
        // Telon: oscurece y capta el toque para cerrar.
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer { alpha = 0.55f * a.value }
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
        )

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .graphicsLayer {
                    translationY = (1f - a.value) * 90.dp.toPx()
                    val s = 0.975f + 0.025f * a.value
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .clip(shape)
                .hazeEffect(haze, glass)
                .heightIn(max = maxH),
        ) {
            // Brillo superior del cristal (sin bordes de color).
            Box(
                Modifier.fillMaxWidth().height(1.dp)
                    .background(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x1FFFFFFF), Color(0x00FFFFFF)))),
            )
            // Asa del cristal.
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.size(width = 44.dp, height = 5.dp).background(Color(0x33FFFFFF), CircleShape))
            }
            content()
        }
    }
}
