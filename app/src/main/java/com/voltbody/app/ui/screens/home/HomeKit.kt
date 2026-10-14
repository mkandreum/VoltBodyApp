package com.voltbody.app.ui.screens.home

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.EaseOutExpo
import com.voltbody.app.ui.SkeletonBox
import com.voltbody.app.ui.pressScaleIf
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType

/** Barra de progreso con degradado de marca; el relleno se anima en fase draw. */
@Composable
internal fun HmGradientBar(progress: Float, modifier: Modifier = Modifier, height: Dp = 8.dp, brush: Brush? = null) {
    val a = remember { Animatable(0f) }
    LaunchedEffect(progress) { a.animateTo(progress.coerceIn(0f, 1f), tween(900, easing = EaseOutExpo)) }
    val b = brush ?: Volt.gradient
    Box(
        modifier.fillMaxWidth().height(height).clip(CircleShape).drawBehind {
            drawRect(Color.White.copy(alpha = 0.08f))
            drawRect(b, size = Size(size.width * a.value, size.height))
        },
    )
}

/** Cabecera de tarjeta (como la v3): icono en cuadro redondeado tintado + titulo en tono normal. */
@Composable
internal fun HmTileHeader(icon: ImageVector, tint: Color, title: String, modifier: Modifier = Modifier) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(30.dp).clip(RoundedCornerShape(10.dp)).background(tint.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Text(title, color = Volt.text, style = VoltType.title, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
    }
}

/** Titulo + subtitulo de tarjeta (Title 17/700 + Body dim). */
@Composable
internal fun HmCardTitle(title: String, subtitle: String? = null, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(title, color = Volt.text, style = VoltType.title)
        if (subtitle != null) Text(subtitle, color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp))
    }
}

/** Pill con degradado de marca (CTA secundario), spring + haptica al pulsar. */
@Composable
internal fun HmGradientPill(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, height: Dp = 48.dp,
) {
    val grad = Volt.gradient
    val off = Volt.surface2
    val ink = Volt.onAccent
    Row(
        modifier.height(height).pressScaleIf(enabled, 0.95f, onClick).clip(CircleShape)
            .drawBehind {
                if (enabled) {
                    drawRect(grad)
                    drawRect(Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.22f), Color.Transparent), endY = size.height * 0.55f))
                } else drawRect(off)
            }
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = if (enabled) ink else Volt.textDim, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (enabled) ink else Volt.textDim, style = VoltType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), maxLines = 1)
    }
}

/** Pill secundario (superficie + borde fino). */
@Composable
internal fun HmGhostPill(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null, enabled: Boolean = true, height: Dp = 48.dp,
) {
    Row(
        modifier.height(height).pressScaleIf(enabled, 0.95f, onClick).clip(CircleShape).background(Volt.surface2)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, tint = Volt.text, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (enabled) Volt.text else Volt.textDim, style = VoltType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.Bold), maxLines = 1)
    }
}

/** Esqueleto con shimmer de toda la pantalla (mientras se derivan los datos). */
@Composable
internal fun HmHomeSkeleton(modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SkeletonBox(Modifier.width(110.dp).height(12.dp), CircleShape)
                SkeletonBox(Modifier.width(220.dp).height(44.dp), RoundedCornerShape(14.dp))
            }
            SkeletonBox(Modifier.size(56.dp), CircleShape)
        }
        SkeletonBox(Modifier.fillMaxWidth().height(88.dp), RoundedCornerShape(28.dp))
        SkeletonBox(Modifier.fillMaxWidth().height(250.dp), RoundedCornerShape(36.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(Modifier.weight(1f).height(220.dp), RoundedCornerShape(28.dp))
            SkeletonBox(Modifier.weight(1f).height(220.dp), RoundedCornerShape(28.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SkeletonBox(Modifier.weight(1f).height(140.dp), RoundedCornerShape(28.dp))
            SkeletonBox(Modifier.weight(1f).height(140.dp), RoundedCornerShape(28.dp))
        }
    }
}
