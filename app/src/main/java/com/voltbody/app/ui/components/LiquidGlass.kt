package com.voltbody.app.ui.components

import android.os.Build
import androidx.compose.animation.core.*
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
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

// ─── Liquid Glass Design Tokens ──────────────────────────────────────────────
// Authentic iOS 26 Liquid Glass: refracion, inner highlights, depth re-imagined.

private val LiquidGlassShape = RoundedCornerShape(26.dp)
private val LiquidGlassShapeSmall = RoundedCornerShape(18.dp)
private val LiquidSpring = spring<Float>(dampingRatio = 0.65f, stiffness = 350f)

// ─── LiquidGlassScaffold ────────────────────────────────────────────────────
// Global manager for backdrop blur. Wraps the screen and provides hazeState.

@Composable
fun LiquidGlassScaffold(
    modifier: Modifier = Modifier,
    background: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.(HazeState) -> Unit
) {
    val hazeState = remember { HazeState() }
    
    Box(modifier = modifier.fillMaxSize()) {
        // Background layer (where blur is sampled)
        Box(
            modifier = Modifier
                .fillMaxSize()
                .haze(
                    state = hazeState,
                    backgroundColor = LocalVoltBodyColors.current.bg,
                )
        ) {
            background()
        }
        
        // Content layer
        content(hazeState)
    }
}

// ─── LiquidGlassCard ────────────────────────────────────────────────────────
// Real glass card using backdrop blur (Haze), inner pill highlights, 
// and dynamic vertical gradient borders.

@Composable
fun LiquidGlassCard(
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    glassAlpha: Float = 0.12f, // Much lower with real blur
    accentGlow: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val vb = LocalVoltBodyColors.current
    val haptic = rememberHaptic()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) 0.97f else 1f,
        animationSpec = LiquidSpring,
        label = "liquid_card_scale"
    )

    val glowPulse by rememberInfiniteTransition(label = "card_pulse").animateFloat(
        initialValue = 0.1f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "pulse"
    )

    val cardModifier = modifier
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .shadow(
            elevation = if (accentGlow) 24.dp else 12.dp,
            shape = LiquidGlassShape,
            ambientColor = if (accentGlow) vb.accent.copy(alpha = glowPulse) else Color.Black.copy(alpha = 0.15f),
            spotColor = if (accentGlow) vb.accent.copy(alpha = glowPulse * 0.5f) else Color.Black.copy(alpha = 0.1f)
        )
        // Apply Haze child if state is provided
        .then(if (hazeState != null) Modifier.hazeChild(state = hazeState, shape = LiquidGlassShape) else Modifier)
        .clip(LiquidGlassShape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    Color.White.copy(alpha = glassAlpha),
                    Color.White.copy(alpha = glassAlpha * 0.5f)
                )
            )
        )
        .drawWithContent {
            drawContent()
            
            // 1. Inner Highlight Pill (Top Edge) - Characteristic of real glass
            drawRoundRect(
                color = Color.White.copy(alpha = 0.22f),
                topLeft = Offset(24.dp.toPx(), 1.dp.toPx()),
                size = Size(size.width - 48.dp.toPx(), 1.5.dp.toPx()),
                cornerRadius = CornerRadius(1.dp.toPx())
            )
            
            // 2. Inner Shadow / Material Thickness
            drawRect(
                brush = Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.06f),
                    0.1f to Color.Transparent
                )
            )
        }
        // 3. Dynamic Vertical Border (Brighter at top light source)
        .border(
            width = 1.dp,
            brush = Brush.verticalGradient(
                listOf(
                    Color.White.copy(alpha = 0.28f),
                    Color.White.copy(alpha = 0.05f)
                )
            ),
            shape = LiquidGlassShape
        )

    if (onClick != null) {
        Surface(
            onClick = {
                haptic.perform(HapticType.TICK)
                onClick()
            },
            modifier = cardModifier,
            color = Color.Transparent,
            shape = LiquidGlassShape,
            interactionSource = interactionSource
        ) {
            Column(modifier = Modifier.padding(20.dp), content = content)
        }
    } else {
        Column(modifier = cardModifier.padding(20.dp), content = content)
    }
}

// ─── LiquidGlassButton ──────────────────────────────────────────────────────
// High-fidelity glass button with backdrop blur, shimmer, and pulsing glow.

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
        animationSpec = LiquidSpring
    )

    val shimmerTransition = rememberInfiniteTransition(label = "btn_anim")
    val shimmerOffset by shimmerTransition.animateFloat(
        initialValue = -1.5f,
        targetValue = 2.5f,
        animationSpec = infiniteRepeatable(tween(3500, easing = LinearEasing)),
        label = "shimmer"
    )

    val glowPulse by shimmerTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Reverse),
        label = "glow"
    )

    val (bgBrush, textColor) = when (style) {
        LiquidButtonStyle.Primary -> Brush.linearGradient(listOf(vb.accent, vb.accent.copy(0.85f))) to ColorBlack
        LiquidButtonStyle.Secondary -> Brush.verticalGradient(listOf(Color.White.copy(0.12f), Color.White.copy(0.06f))) to ColorWhite
        LiquidButtonStyle.Danger -> Brush.linearGradient(listOf(ColorError.copy(0.2f), ColorError.copy(0.1f))) to Color(0xFFFECDD3)
    }

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (style == LiquidButtonStyle.Primary) {
                    Modifier.shadow(16.dp, LiquidGlassShapeSmall, spotColor = vb.accent.copy(glowPulse))
                } else Modifier
            )
            .then(if (hazeState != null && style != LiquidButtonStyle.Primary) Modifier.hazeChild(hazeState, LiquidGlassShapeSmall) else Modifier)
            .clip(LiquidGlassShapeSmall)
            .background(bgBrush)
            .drawWithContent {
                drawContent()
                // Inner highlight
                drawRoundRect(
                    color = Color.White.copy(alpha = if (style == LiquidButtonStyle.Primary) 0.3f else 0.15f),
                    topLeft = Offset(18.dp.toPx(), 1.dp.toPx()),
                    size = Size(size.width - 36.dp.toPx(), 1.5.dp.toPx()),
                    cornerRadius = CornerRadius(1.dp.toPx())
                )
                // Shimmer
                if (enabled) {
                    val startX = size.width * shimmerOffset
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(0.15f), Color.Transparent),
                            start = Offset(startX, 0f),
                            end = Offset(startX + size.width * 0.4f, size.height)
                        )
                    )
                }
            }
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(
                    listOf(Color.White.copy(0.25f), Color.White.copy(0.05f))
                ),
                shape = LiquidGlassShapeSmall
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = { haptic.perform(HapticType.TICK); onClick() }
            )
            .padding(horizontal = 24.dp, vertical = 16.dp)
            .then(if (!enabled) Modifier.alpha(0.5f) else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            leadingIcon?.invoke()
            if (leadingIcon != null) Spacer(Modifier.width(8.dp))
            Text(text.uppercase(), style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Black, letterSpacing = 1.sp), color = textColor)
        }
    }
}

enum class LiquidButtonStyle { Primary, Secondary, Danger }

// ─── LiquidProgressBar ──────────────────────────────────────────────────────
// XP bar with internal shimmer and external glow pulse.

@Composable
fun LiquidProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    height: Dp = 10.dp
) {
    val vb = LocalVoltBodyColors.current
    val animProgress by animateFloatAsState(progress.coerceIn(0f, 1f), tween(1000, easing = FastOutSlowInEasing))
    
    val infiniteTransition = rememberInfiniteTransition(label = "pb")
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(tween(2500, easing = LinearEasing)),
        label = "shimmer"
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
                .drawWithContent {
                    drawContent()
                    // Internal shimmer
                    val startX = size.width * shimmerOffset
                    drawRect(
                        brush = Brush.linearGradient(
                            listOf(Color.Transparent, Color.White.copy(0.25f), Color.Transparent),
                            start = Offset(startX, 0f),
                            end = Offset(startX + size.width * 0.3f, size.height)
                        )
                    )
                }
        )
    }
}

// ─── HeadlineGradient ───────────────────────────────────────────────────────

@Composable
fun HeadlineGradient(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineLarge
) {
    val vb = LocalVoltBodyColors.current
    Text(
        text = text.uppercase(),
        modifier = modifier,
        style = style.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp,
            brush = Brush.linearGradient(
                colors = listOf(ColorWhite, vb.accent.copy(0.8f)),
                start = Offset.Zero,
                end = Offset(1000f, 0f)
            )
        )
    )
}

// ─── GlowText ───────────────────────────────────────────────────────────────

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
            shadow = Shadow(color = color.copy(0.6f), blurRadius = 24f)
        ),
        color = color
    )
}
