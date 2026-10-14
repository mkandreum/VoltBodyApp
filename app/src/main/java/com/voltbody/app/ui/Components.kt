package com.voltbody.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType

/** Borde degradado fino (accent 35 % -> transparente) para tarjetas destacadas. */
fun Modifier.gradientBorder(width: Dp = 1.dp, shape: Shape = RoundedCornerShape(28.dp), brush: Brush? = null): Modifier = composed {
    val b = brush ?: Brush.linearGradient(listOf(Volt.accent.copy(alpha = 0.35f), Color.White.copy(alpha = 0.04f), Color.Transparent))
    this.border(width, b, shape)
}

/**
 * true dentro de las hojas inferiores de cristal: las tarjetas se dibujan como paneles de vidrio
 * (relleno blanco translucido + filo de luz de 1dp, sin brillos de color) para integrarse con el fondo desenfocado.
 */
val LocalGlass = androidx.compose.runtime.staticCompositionLocalOf { false }

/** Radio comun de las tarjetas dentro de una hoja (coincide con las tarjetas v3). */
val GlassRadius = 24.dp

/** Panel de vidrio: relleno [Volt.surface] (translucido en hojas) + filo superior de luz. */
fun Modifier.glassPanel(shape: Shape = RoundedCornerShape(GlassRadius)): Modifier = composed {
    val fill = Volt.surface
    this.clip(shape).background(fill)
        .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.14f), Color.White.copy(alpha = 0.04f))), shape)
}

/** Tarjeta oscura (radio 28) con borde de 1dp blanco 6 %. */
@Composable
fun VoltCard(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, padding: Dp = 16.dp, content: @Composable ColumnScope.() -> Unit) {
    if (LocalGlass.current) {
        Column(modifier.glassPanel().let { if (onClick != null) it.pressScale(onClick) else it }.padding(padding), content = content)
        return
    }
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier.clip(shape).background(Volt.surface).border(1.dp, Volt.border, shape)
            .let { if (onClick != null) it.pressScale(onClick) else it }.padding(padding),
        content = content,
    )
}

/**
 * Tarjeta "bento": radio 28 (36 si [hero]), brillo superior sutil y borde degradado.
 * [gradient] = relleno con el degradado de marca (solo para una tarjeta heroe por pantalla).
 */
@Composable
fun BentoCard(
    modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, hero: Boolean = false, gradient: Boolean = false,
    padding: Dp = 20.dp, content: @Composable ColumnScope.() -> Unit,
) {
    if (LocalGlass.current && !gradient) {
        Column(modifier.glassPanel().let { if (onClick != null) it.pressScale(onClick) else it }.padding(padding), content = content)
        return
    }
    val shape = RoundedCornerShape(if (hero) 36.dp else 28.dp)
    val fill = Volt.surface
    val grad = Volt.gradient
    val glow = Volt.accent
    Column(
        modifier.clip(shape)
            .drawBehind {
                if (gradient) drawRect(grad)
                else {
                    drawRect(fill)
                    drawRect(Brush.radialGradient(listOf(glow.copy(alpha = if (hero) 0.16f else 0.07f), Color.Transparent), center = Offset(size.width * 0.9f, 0f), radius = size.maxDimension * 0.8f))
                    drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.04f), Color.Transparent), endY = size.height * 0.4f))
                }
            }
            .let { if (gradient) it else it.gradientBorder(1.dp, shape) }
            .let { if (onClick != null) it.pressScale(onClick) else it }
            .padding(padding),
        content = content,
    )
}

/** Banner degradado de marca (titular + subtitulo + icono). */
@Composable
fun GradientBanner(title: String, subtitle: String, icon: ImageVector, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    val shape = RoundedCornerShape(28.dp)
    val ink = Volt.onAccent
    Row(
        modifier.fillMaxWidth().clip(shape).background(Volt.gradient)
            .drawBehind {
                drawCircle(Color.White.copy(alpha = 0.14f), size.height * 0.9f, Offset(size.width * 0.95f, -size.height * 0.2f))
                drawCircle(Color.White.copy(alpha = 0.08f), size.height * 0.6f, Offset(size.width * 0.75f, size.height * 1.1f))
            }
            .let { if (onClick != null) it.pressScale(onClick) else it }.padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(ink.copy(alpha = 0.85f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Volt.accent2)
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, color = ink, style = VoltType.title)
            Text(subtitle, color = ink.copy(alpha = 0.72f), style = VoltType.body)
        }
    }
}

@Composable
fun SectionTitle(text: String, modifier: Modifier = Modifier, action: String? = null, onAction: (() -> Unit)? = null) {
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text, color = Volt.text, style = VoltType.h2, modifier = Modifier.weight(1f))
        if (action != null) {
            Text(
                action, color = Volt.accent, style = VoltType.label,
                modifier = Modifier.clip(CircleShape).clickable { onAction?.invoke() }.padding(horizontal = 12.dp, vertical = 12.dp),
            )
        }
    }
}

@Composable
fun StatPill(label: String, value: String, modifier: Modifier = Modifier, color: Color = Volt.accent) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Volt.surface2).padding(horizontal = 16.dp, vertical = 12.dp)) {
        Text(value, color = color, style = VoltType.numberS)
        Text(label.uppercase(), color = Volt.textDim, style = VoltType.label)
    }
}

/** CTA principal (mismo aspecto que el boton de la v3): pildora de acento solido, spring al pulsar y haptica. */
@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, loading: Boolean = false) {
    val accent = Volt.accent
    val off = Volt.surface2
    val on = enabled && !loading
    val sel = animateFloatAsState(if (on) 1f else 0f, tween(200), label = "btnOn")
    Box(
        modifier.fillMaxWidth().height(54.dp).pressScaleIf(on, 0.96f, onClick).clip(CircleShape)
            .drawBehind {
                drawRect(off)
                drawRect(accent, alpha = sel.value)
            },
        contentAlignment = Alignment.Center,
    ) {
        if (loading) CircularProgressIndicator(Modifier.size(22.dp), color = Volt.onAccent, strokeWidth = 2.5.dp)
        else Text(text, color = if (on) Volt.onAccent else Volt.textDim, style = VoltType.title.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), maxLines = 1)
    }
}

@Composable
fun voltFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Volt.accent, unfocusedBorderColor = Color.Transparent, focusedLabelColor = Volt.accent,
    unfocusedLabelColor = Volt.textDim, cursorColor = Volt.accent, focusedTextColor = Volt.text, unfocusedTextColor = Volt.text,
    focusedContainerColor = Volt.surface2, unfocusedContainerColor = Volt.surface2,
    errorBorderColor = Volt.danger, errorContainerColor = Volt.surface2, errorLabelColor = Volt.danger,
)

@Composable
fun VoltTextField(
    value: String, onChange: (String) -> Unit, label: String, modifier: Modifier = Modifier, singleLine: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text, password: Boolean = false,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = singleLine, modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp), colors = voltFieldColors(), textStyle = VoltType.title,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
    )
}

/** Chip seleccionable: degradado de marca al seleccionar y "salto" con spring. */
@Composable
fun VoltChip(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val sel = animateFloatAsState(if (selected) 1f else 0f, spring(0.6f, 500f), label = "chip")
    val grad = Volt.gradient
    val base = Volt.surface2
    val txt = animateColorAsState(if (selected) Volt.onAccent else Volt.text, tween(180), label = "chipT")
    Box(
        modifier.graphicsLayer { val s = 1f + 0.06f * sel.value; scaleX = s; scaleY = s }
            .clip(CircleShape).drawBehind { drawRect(base); drawRect(grad, alpha = sel.value.coerceIn(0f, 1f)) }
            .clickable(onClick = onClick).padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = txt.value, style = VoltType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold)) }
}

@Composable
fun EmptyState(text: String, modifier: Modifier = Modifier) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(Unit) { a.animateTo(1f, tween(1800)) }
    val c = Volt.accent
    Column(modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(56.dp)) {
            drawCircle(c.copy(alpha = 0.10f), size.minDimension / 2f)
            drawArc(c, -90f, 360f * a.value, false, Offset(size.width * 0.12f, size.height * 0.12f), size * 0.76f, style = androidx.compose.ui.graphics.drawscope.Stroke(3.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
        }
        Spacer(Modifier.height(16.dp))
        Text(text, color = Volt.textDim, style = VoltType.body, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
