package com.voltbody.app.ui.v3

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voltbody.app.ui.theme.Volt
import kotlin.math.PI
import kotlin.math.sin

/** Paletas de arte (manchas) por "tono" de entreno. Sin degradados planos: son manchas vistas a traves de vidrio. */
object ArtPalette {
    val mint = listOf(Color(0xFF27F58B), Color(0xFF0B6B45), Color(0xFF00E0C7), Color(0xFF0A0A0A))
    val ember = listOf(Color(0xFFFF8A00), Color(0xFF27F58B), Color(0xFF5B1E00), Color(0xFF0A0A0A))
    val ice = listOf(Color(0xFF38BDF8), Color(0xFF27F58B), Color(0xFF0B2B5B), Color(0xFF0A0A0A))
    val violet = listOf(Color(0xFF8B7CFF), Color(0xFF27F58B), Color(0xFF231A5B), Color(0xFF0A0A0A))
    private val all = listOf(mint, ember, ice, violet)
    /** Paleta estable para un texto (mismo entreno = mismo arte). */
    fun forKey(key: String): List<Color> = all[(key.hashCode() and 0x7fffffff) % all.size]
}

/**
 * Arte de "vidrio estriado" (como las tarjetas de la referencia): manchas de color suaves que derivan
 * despacio y se ven a traves de franjas verticales que las desplazan (refraccion) y les dan relieve.
 * Todo en fase draw: no recompone.
 */
@Composable
fun FlutedArt(modifier: Modifier = Modifier, colors: List<Color> = ArtPalette.mint, seed: Int = 0, stripe: Dp = 13.dp, animated: Boolean = true) {
    val t = rememberInfiniteTransition(label = "art")
    val drift = if (animated) t.animateFloat(0f, 1f, infiniteRepeatable(tween(14000, easing = LinearEasing), RepeatMode.Reverse), label = "drift") else null
    val base = colors.last()
    val blobs = remember(seed, colors) {
        val r = java.util.Random(seed.toLong() * 7919 + 17)
        colors.dropLast(1).mapIndexed { i, c -> floatArrayOf(0.15f + r.nextFloat() * 0.7f, 0.15f + r.nextFloat() * 0.7f, 0.45f + r.nextFloat() * 0.35f, i.toFloat()) to c }
    }
    Canvas(modifier) {
        drawRect(base)
        val d = drift?.value ?: 0.5f
        val sw = stripe.toPx()
        val n = (size.width / sw).toInt() + 1
        for (k in 0 until n) {
            val x0 = k * sw
            // desplazamiento de "refraccion" distinto por franja
            val dx = sin(k * 1.7f + d * PI.toFloat() * 2f) * sw * 0.9f
            val dy = sin(k * 0.9f + d * PI.toFloat()) * sw * 0.6f
            clipRect(x0, 0f, x0 + sw, size.height) {
                blobs.forEach { (p, c) ->
                    val cx = size.width * (p[0] + 0.08f * sin(d * 6.28f + p[3])) + dx
                    val cy = size.height * (p[1] + 0.06f * sin(d * 4.2f + p[3] * 2f)) + dy
                    val rad = size.maxDimension * p[2]
                    drawCircle(Brush.radialGradient(listOf(c, c.copy(alpha = 0f)), Offset(cx, cy), rad), rad, Offset(cx, cy))
                }
                // relieve de la estria: brillo a la izquierda, sombra a la derecha
                drawRect(Brush.horizontalGradient(listOf(Color.White.copy(alpha = 0.16f), Color.Transparent, Color.Black.copy(alpha = 0.32f)), startX = x0, endX = x0 + sw), Offset(x0, 0f), Size(sw, size.height))
            }
        }
    }
}

/**
 * CTA "liquido" de la referencia: pildora con texto fusionada con un circulo con flecha
 * (puente concavo entre ambos, efecto metaball).
 */
@Composable
fun GooeyCta(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector = Icons.AutoMirrored.Filled.ArrowForward,
    height: Dp = 60.dp, color: Color = Volt.accent, bg: Color = Volt.bg, loading: Boolean = false, enabled: Boolean = true,
) {
    val fill = if (enabled) color else Volt.surface2
    val fg = if (enabled) Color(0xFF0A0A0A) else Volt.textDim
    Box(
        modifier.fillMaxWidth().height(height)
            .drawBehind {
                val h = size.height
                val r = h / 2
                val circleX = size.width - r
                val gap = h * 0.18f
                val pillW = size.width - h - gap
                drawRoundRect(fill, Offset.Zero, Size(pillW, h), CornerRadius(r))
                drawCircle(fill, r, Offset(circleX, r))
                // puente
                val bx = pillW - r * 0.5f
                val bw = (circleX - r * 0.5f) - bx
                val bh = h * 0.54f
                drawRect(fill, Offset(bx, (h - bh) / 2), Size(bw, bh))
                // muescas concavas (color de fondo) para que parezca liquido
                val nx = pillW + gap / 2
                val nr = gap * 1.15f
                drawCircle(bg, nr, Offset(nx, (h - bh) / 2 - nr * 0.62f))
                drawCircle(bg, nr, Offset(nx, (h + bh) / 2 + nr * 0.62f))
            }
            .then(if (enabled && !loading) Modifier.tap(0.97f, onClick = onClick) else Modifier),
    ) {
        Box(Modifier.fillMaxHeight().fillMaxWidth().padding(end = height + height * 0.18f).padding(horizontal = 24.dp), contentAlignment = Alignment.CenterStart) {
            if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = fg, strokeWidth = 2.dp)
            else Text(text, style = T.button.copy(fontSize = androidx.compose.ui.unit.TextUnit(16f, androidx.compose.ui.unit.TextUnitType.Sp)), color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Box(Modifier.align(Alignment.CenterEnd).size(height), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = fg, modifier = Modifier.size(height * 0.38f))
        }
    }
}

/**
 * Imagen circular con burbuja de dato pegada (como "820m2" de la referencia). La burbuja lleva
 * un anillo del color de fondo para parecer recortada sobre el circulo.
 */
@Composable
fun CircleMedia(
    size: Dp, bubble: String?, modifier: Modifier = Modifier, ring: Color = Volt.bg,
    bubbleColor: Color = Volt.accent, content: @Composable BoxScope.() -> Unit,
) {
    Box(modifier.size(size)) {
        Box(Modifier.matchParentSize().clip(CircleShape).background(Volt.surface2), contentAlignment = Alignment.Center, content = content)
        if (bubble != null) {
            val b = size * 0.36f
            Box(
                Modifier.align(Alignment.TopEnd).offset(x = b * 0.12f, y = -b * 0.05f).size(b).clip(CircleShape).background(ring).padding(5.dp)
                    .clip(CircleShape).background(bubbleColor),
                contentAlignment = Alignment.Center,
            ) { Text(bubble, style = T.cardTitle, color = Volt.onAccent, maxLines = 1, overflow = TextOverflow.Ellipsis) }
        }
    }
}
