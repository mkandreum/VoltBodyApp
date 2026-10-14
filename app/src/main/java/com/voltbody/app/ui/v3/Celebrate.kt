package com.voltbody.app.ui.v3

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.Achievement
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.rememberReduceMotion
import com.voltbody.app.ui.screens.workout.ConfettiBurst
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.Haptics
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Escucha los logros recien desbloqueados ([Repository.celebrations]) y los celebra uno a uno a pantalla
 * completa, este donde este el usuario. Se coloca una vez en el AppShell, por encima de todo.
 */
@Composable
fun CelebrationHost(repo: Repository) {
    val queue = remember { mutableStateListOf<Achievement>() }
    LaunchedEffect(repo) { repo.celebrations.collect { a -> if (queue.none { it.id == a.id }) queue.add(a) } }
    val current = queue.firstOrNull()
    if (current != null) {
        key(current.id) {
            // Rafaga (p. ej. un historial antiguo que desbloquea varios a la vez): una sola celebracion con "+N".
            val shown = remember { queue.size }
            AchievementCelebration(current, more = shown - 1) { repeat(shown.coerceAtMost(queue.size)) { queue.removeAt(0) } }
        }
    }
}

/**
 * Celebracion de logro: telon, rayos de luz girando, insignia que entra con rebote, confeti y redoble haptico.
 * Se cierra sola a los ~3,4 s o al tocar. Con "Quitar animaciones" no hay rayos girando ni rebote.
 */
@Composable
fun AchievementCelebration(a: Achievement, more: Int = 0, onDone: () -> Unit) {
    val ctx = LocalContext.current
    val reduce = rememberReduceMotion()
    val enter = remember { Animatable(0f) }
    val badge = remember { Animatable(if (reduce) 1f else 0.2f) }
    var confetti by remember { mutableIntStateOf(0) }
    var closing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        Haptics.celebrate(ctx)
        confetti++
        enter.animateTo(1f, tween(220))
        if (!reduce) badge.animateTo(1f, spring(dampingRatio = 0.42f, stiffness = 320f))
        delay(2600)
        closing = true
    }
    LaunchedEffect(closing) {
        if (closing) { enter.animateTo(0f, tween(220)); onDone() }
    }

    val rays = if (reduce) null else rememberInfiniteTransition(label = "rays").animateFloat(
        0f, 360f, infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart), label = "raysAngle",
    )
    val accent = Volt.accent

    Box(
        Modifier.fillMaxSize()
            .graphicsLayer { alpha = enter.value }
            .background(Color.Black.copy(alpha = 0.78f))
            .clickable(remember { MutableInteractionSource() }, indication = null) { closing = true }
            .semantics { liveRegion = LiveRegionMode.Polite },
        contentAlignment = Alignment.Center,
    ) {
        // Rayos + halo detras de la insignia (todo en fase draw).
        Canvas(Modifier.size(360.dp)) {
            val c = center
            drawCircle(Brush.radialGradient(listOf(accent.copy(alpha = 0.45f), Color.Transparent), c, size.minDimension / 2), size.minDimension / 2, c)
            rotate(rays?.value ?: 0f, c) {
                val n = 14
                for (i in 0 until n) {
                    val a0 = (i * 2 * PI / n).toFloat()
                    val a1 = a0 + (PI / n * 0.55f).toFloat()
                    val r = size.minDimension / 2
                    val path = androidx.compose.ui.graphics.Path().apply {
                        moveTo(c.x, c.y)
                        lineTo(c.x + r * cos(a0), c.y + r * sin(a0))
                        lineTo(c.x + r * cos(a1), c.y + r * sin(a1))
                        close()
                    }
                    drawPath(path, Brush.radialGradient(listOf(accent.copy(alpha = 0.22f), Color.Transparent), c, r))
                }
            }
        }
        ConfettiBurst(confetti, Modifier.fillMaxSize(), big = true)
        Column(Modifier.padding(horizontal = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                Modifier.size(132.dp).graphicsLayer { scaleX = badge.value; scaleY = badge.value }
                    .clip(CircleShape).background(Volt.surface2).background(accent.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center,
            ) { Text(a.icon.ifBlank { "🏆" }, fontSize = 64.sp) }
            Spacer(Modifier.height(22.dp))
            Text("LOGRO DESBLOQUEADO", style = T.label, color = accent)
            Spacer(Modifier.height(6.dp))
            Text(a.label, style = T.screen.copy(fontSize = 32.sp, lineHeight = 36.sp), color = Volt.text, textAlign = TextAlign.Center)
            if (a.description.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(a.description, style = T.body, color = Volt.textDim, textAlign = TextAlign.Center)
            }
            if (more > 0) {
                Spacer(Modifier.height(14.dp))
                Text(
                    "+$more logro${if (more > 1) "s" else ""} más · míralos en Perfil › Logros", style = T.cardTitle, color = accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = 0.14f)).padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
            Spacer(Modifier.height(26.dp))
            Text("Toca para continuar", style = T.caption, color = Volt.textDim.copy(alpha = 0.7f))
        }
    }
}

/**
 * Aviso flotante de "ejercicio completado": pildora que baja desde arriba con un check que salta.
 * Mostrar con [visible]; el llamador decide cuando ocultarlo.
 */
@Composable
fun ExerciseDoneBanner(visible: Boolean, title: String, subtitle: String, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible, modifier = modifier,
        enter = slideInVertically(spring(0.7f, 500f)) { -it * 2 } + fadeIn(tween(120)) + scaleIn(spring(0.55f, 500f), initialScale = 0.8f),
        exit = slideOutVertically(tween(180)) { -it * 2 } + fadeOut(tween(140)),
    ) {
        val pop = remember { Animatable(0f) }
        LaunchedEffect(Unit) { pop.animateTo(1f, spring(0.35f, 600f)) }
        Row(
            Modifier.clip(RoundedCornerShape(28.dp)).background(Volt.accent).padding(start = 8.dp, end = 18.dp, top = 8.dp, bottom = 8.dp)
                .semantics { liveRegion = LiveRegionMode.Polite },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(36.dp).graphicsLayer { scaleX = pop.value; scaleY = pop.value }.clip(CircleShape).background(Volt.onAccent),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Check, null, tint = Volt.accent, modifier = Modifier.size(22.dp)) }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(title, style = T.cardTitle.copy(fontSize = 15.sp), color = Volt.onAccent, maxLines = 1)
                Text(subtitle, style = T.caption, color = Volt.onAccent.copy(alpha = 0.75f), maxLines = 1)
            }
        }
    }
}
