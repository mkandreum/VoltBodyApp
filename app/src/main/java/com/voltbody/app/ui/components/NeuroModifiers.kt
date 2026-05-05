package com.voltbody.app.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.voltbody.app.ui.theme.*

// ─── Neumorphic Modifier Extensions ─────────────────────────────────────────
// Reusable modifiers matching the web's .neuro-raised, .neuro-inset, and
// .pulse-surface CSS classes for consistent neumorphic styling across the app.

/**
 * Neumorphic raised surface — matches web CSS `.neuro-raised`.
 *
 * Creates a "lifted" card-like appearance with dual shadows:
 * - Dark shadow on bottom-right (depth)
 * - Light inner highlight on top-left (surface reflection)
 * - Subtle accent glow ring around the border
 */
fun Modifier.neuroRaised(
    cornerRadius: Dp = 20.dp,
    accentGlow: Boolean = true
): Modifier = composed {
    val vb = LocalVoltBodyColors.current
    val shape = RoundedCornerShape(cornerRadius)

    this
        .shadow(
            elevation = 10.dp,
            shape = shape,
            ambientColor = NeuroShadowDark,
            spotColor = NeuroShadowDark
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    vb.surfaceElevated.copy(alpha = 0.85f),
                    vb.surface.copy(alpha = 0.75f)
                )
            )
        )
        // Inner top-left highlight (simulates light reflection)
        .drawWithContent {
            drawContent()
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.05f),
                        Color.Transparent
                    ),
                    endY = size.height * 0.35f
                )
            )
        }
        .border(
            width = 1.dp,
            color = if (accentGlow)
                Color.White.copy(alpha = 0.08f)
            else
                Color.White.copy(alpha = 0.05f),
            shape = shape
        )
}

/**
 * Neumorphic inset surface — matches web CSS `.neuro-inset`.
 *
 * Creates a "sunken" field appearance with inner shadows,
 * perfect for input fields, stat displays, and progress tracks.
 */
fun Modifier.neuroInset(
    cornerRadius: Dp = 12.dp
): Modifier = composed {
    val vb = LocalVoltBodyColors.current
    val shape = RoundedCornerShape(cornerRadius)

    this
        .clip(shape)
        .background(vb.bg)
        .drawWithContent {
            drawContent()
            // Inner shadow from top-left (sunken effect)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.35f),
                        Color.Transparent
                    ),
                    start = Offset.Zero,
                    end = Offset(size.width * 0.4f, size.height * 0.4f)
                )
            )
            // Inner highlight from bottom-right (subtle lift at edge)
            drawRect(
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.Transparent,
                        Color.White.copy(alpha = 0.04f)
                    ),
                    start = Offset(size.width * 0.6f, size.height * 0.6f),
                    end = Offset(size.width, size.height)
                )
            )
        }
        .border(
            width = 1.dp,
            color = Color.White.copy(alpha = 0.05f),
            shape = shape
        )
}

/**
 * Accent glow border — matches web CSS `0 0 0 1px color-mix(accent 8%)`.
 *
 * Adds a subtle accent-colored glow ring around any composable.
 */
fun Modifier.glowBorder(
    cornerRadius: Dp = 20.dp,
    glowAlpha: Float = 0.15f
): Modifier = composed {
    val vb = LocalVoltBodyColors.current
    val shape = RoundedCornerShape(cornerRadius)

    this
        .shadow(
            elevation = 4.dp,
            shape = shape,
            ambientColor = vb.accent.copy(alpha = glowAlpha),
            spotColor = vb.accent.copy(alpha = glowAlpha * 0.5f),
            clip = false
        )
        .border(
            width = 1.dp,
            color = vb.accent.copy(alpha = glowAlpha),
            shape = shape
        )
}

/**
 * Pulse-on-press modifier — matches web CSS `.pulse-surface`.
 *
 * Applies a radial accent glow that intensifies when pressed,
 * providing premium tactile feedback without material ripple.
 *
 * Note: Use with `clickable(interactionSource, indication = null)` to
 * avoid double-feedback with the default ripple indication.
 */
fun Modifier.pulseOnPress(
    isPressed: Boolean,
    cornerRadius: Dp = 20.dp
): Modifier = composed {
    val vb = LocalVoltBodyColors.current

    val pulseAlpha by animateFloatAsState(
        targetValue = if (isPressed) 0.15f else 0f,
        animationSpec = if (isPressed)
            tween(50, easing = FastOutLinearInEasing) // Fast in
        else
            tween(320, easing = LinearOutSlowInEasing), // Slow out
        label = "pulse"
    )

    this.drawWithContent {
        drawContent()
        if (pulseAlpha > 0f) {
            drawRect(
                brush = Brush.radialGradient(
                    colors = listOf(
                        vb.accent.copy(alpha = pulseAlpha),
                        Color.Transparent
                    ),
                    center = Offset(size.width / 2f, size.height / 2f),
                    radius = size.maxDimension * 0.7f
                )
            )
        }
    }
}

/**
 * Glass panel modifier — matches web CSS `.glass-panel`.
 *
 * Combines the neuro surface with a heavier glass look:
 * translucent background, accent glow shadow, specular highlight.
 */
fun Modifier.glassPanel(
    cornerRadius: Dp = 20.dp
): Modifier = composed {
    val vb = LocalVoltBodyColors.current
    val shape = RoundedCornerShape(cornerRadius)

    this
        .shadow(
            elevation = 16.dp,
            shape = shape,
            ambientColor = vb.accent.copy(alpha = 0.10f),
            spotColor = Color.Black.copy(alpha = 0.4f)
        )
        .clip(shape)
        .background(
            brush = Brush.verticalGradient(
                colors = listOf(
                    vb.surfaceElevated.copy(alpha = 0.70f),
                    vb.surface.copy(alpha = 0.60f)
                )
            )
        )
        .drawWithContent {
            drawContent()
            // Top specular highlight
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.06f),
                        Color.Transparent
                    ),
                    endY = size.height * 0.2f
                )
            )
        }
        .border(1.dp, Color.White.copy(alpha = 0.09f), shape)
}
