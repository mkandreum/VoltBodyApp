package com.voltbody.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.theme.*
import com.voltbody.app.util.HapticType
import com.voltbody.app.util.rememberHaptic
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild

// ─── Design Tokens (Neuro System) ──────────────────────────────────────────
private val AppCardShape = RoundedCornerShape(24.dp)
private val AppButtonShape = RoundedCornerShape(14.dp)
private val AppSpring = spring<Float>(dampingRatio = 0.7f, stiffness = 400f)

@Composable
fun LiquidGlassScaffold(
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.(HazeState) -> Unit
) {
    val hazeState = remember { HazeState() }
    Box(modifier = modifier.fillMaxSize().background(LocalVoltBodyColors.current.bg)) {
        Box(modifier = Modifier.fillMaxSize().haze(state = hazeState)) {
            background()
        }
        content(hazeState)
    }
}

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    accentGlow: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val vb = LocalVoltBodyColors.current
    val haptic = rememberHaptic()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.98f else 1f,
        animationSpec = AppSpring,
        label = "card_scale"
    )

    val cardModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = if (accentGlow) 20.dp else 12.dp,
            shape = AppCardShape,
            ambientColor = if (accentGlow) vb.accent.copy(alpha = 0.3f) else Color.Black,
            spotColor = if (accentGlow) vb.accent.copy(alpha = 0.25f) else Color.Black
        )
        .clip(AppCardShape)
        .then(if (hazeState != null) Modifier.hazeChild(state = hazeState) else Modifier)
        .background(vb.surfaceElevated.copy(0.3f))
        .drawWithContent {
            drawContent()
            drawRoundRect(
                color = Color.White.copy(alpha = 0.08f),
                topLeft = Offset(1.dp.toPx(), 1.dp.toPx()),
                size = Size(size.width - 2.dp.toPx(), 1.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
        }
        .border(1.dp, vb.border.copy(0.5f), AppCardShape)

    if (onClick != null) {
        Surface(
            onClick = {
                haptic.perform(HapticType.TICK)
                onClick()
            },
            modifier = cardModifier,
            color = Color.Transparent,
            shape = AppCardShape,
            interactionSource = interactionSource
        ) {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    } else {
        Column(modifier = cardModifier.padding(20.dp), content = content)
    }
}

@Composable
fun LiquidGlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    enabled: Boolean = true,
    style: LiquidButtonStyle = LiquidButtonStyle.Primary,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val vb = LocalVoltBodyColors.current
    val haptic = rememberHaptic()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.96f else 1f,
        animationSpec = AppSpring,
        label = "btn_scale"
    )

    val (bgBrush, textColor) = when (style) {
        LiquidButtonStyle.Primary -> Brush.linearGradient(
            listOf(vb.accent, vb.accent.copy(0.8f))
        ) to Color.Black
        LiquidButtonStyle.Secondary -> Brush.verticalGradient(
            listOf(vb.surfaceElevated, vb.surfaceElevated.copy(0.7f))
        ) to Color.White
    }

    Box(
        modifier = modifier
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .then(
                if (style == LiquidButtonStyle.Primary) {
                    Modifier.shadow(12.dp, AppButtonShape, spotColor = vb.accent.copy(0.4f))
                } else Modifier
            )
            .clip(AppButtonShape)
            .then(if (hazeState != null && style != LiquidButtonStyle.Primary) Modifier.hazeChild(state = hazeState) else Modifier)
            .background(bgBrush)
            .border(
                width = 1.dp,
                brush = if (style == LiquidButtonStyle.Primary) {
                    Brush.verticalGradient(listOf(Color.White.copy(0.2f), Color.Transparent))
                } else {
                    SolidColor(vb.border)
                },
                shape = AppButtonShape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { haptic.perform(HapticType.TICK); onClick() }
            )
            .padding(horizontal = 24.dp, vertical = 14.dp)
            .then(if (!enabled) Modifier.alpha(0.5f) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leadingIcon?.invoke()
            if (leadingIcon != null) Spacer(Modifier.width(8.dp))
            Text(
                text.uppercase(),
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp
                ),
                color = textColor
            )
        }
    }
}

enum class LiquidButtonStyle { Primary, Secondary }

@Composable
fun LiquidProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 8.dp
) {
    val vb = LocalVoltBodyColors.current
    val animProgress by animateFloatAsState(
        progress.coerceIn(0f, 1f),
        tween(1000, easing = FastOutSlowInEasing),
        label = "progress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(50))
            .background(vb.bg.copy(0.5f))
            .border(1.dp, Color.White.copy(0.05f), RoundedCornerShape(50))
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(animProgress)
                .clip(RoundedCornerShape(50))
                .background(Brush.horizontalGradient(listOf(vb.accent, vb.accent.copy(0.7f))))
        )
    }
}

@Composable
fun VoltBodyCircularProgress(
    value: Float,
    modifier: Modifier = Modifier,
    size: Dp = 64.dp,
    strokeWidth: Dp = 4.dp
) {
    val vb = LocalVoltBodyColors.current
    val animatedValue by animateFloatAsState(
        targetValue = value,
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "circular_progress"
    )

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = strokeWidth.toPx()
            drawCircle(
                color = Color.White.copy(alpha = 0.1f),
                style = Stroke(width = stroke)
            )
            drawArc(
                color = vb.accent,
                startAngle = -90f,
                sweepAngle = (animatedValue / 100f) * 360f,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            text = "${value.toInt()}%",
            style = MaterialTheme.typography.labelSmall.copy(
                fontWeight = FontWeight.Black,
                fontSize = 10.sp
            ),
            color = Color.White
        )
    }
}

@Composable
fun HeadlineGradient(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineLarge
) {
    val vb = LocalVoltBodyColors.current
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(
            fontWeight = FontWeight.Black,
            brush = Brush.linearGradient(
                colors = listOf(Color.White, vb.accent.copy(0.7f)),
                start = Offset.Zero,
                end = Offset(1000f, 0f)
            )
        )
    )
}

@Composable
fun GlowText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalVoltBodyColors.current.accent
) {
    Text(
        text = text,
        modifier = modifier,
        style = style.copy(
            shadow = Shadow(color = color.copy(0.5f), blurRadius = 16f)
        ),
        color = color
    )
}

fun Modifier.neuroRaised(cornerRadius: Dp) = this.then(
    Modifier
        .shadow(8.dp, RoundedCornerShape(cornerRadius), ambientColor = Color.Black.copy(0.5f))
        .background(NeuroSurface, RoundedCornerShape(cornerRadius))
        .border(1.dp, ColorBorder, RoundedCornerShape(cornerRadius))
)
