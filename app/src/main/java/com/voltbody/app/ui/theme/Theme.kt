package com.voltbody.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.voltbody.app.domain.model.AppTheme

// ── Per-theme tokens ──────────────────────────────────────────────────────────

data class VoltBodyColors(
    val accent: Color,
    val accentDim: Color,
    val bg: Color,
    val surface: Color,
    val surfaceElevated: Color = ColorSurfaceElevated,
    val surfaceContrast: Color = ColorSurfaceContrast,
    val border: Color = ColorBorder,
    val textPrimary: Color = ColorWhite,
    val textMuted: Color = ColorTextMuted,
    val chartFill: Color,
    val chartLine: Color
)

fun voltBodyColorsForTheme(theme: AppTheme): VoltBodyColors = when (theme) {
    AppTheme.VERDE_NEGRO -> VoltBodyColors(
        accent = NeonGreen,
        accentDim = NeonGreenDim,
        bg = BgVerdeNegro,
        surface = SurfaceVerdeNegro,
        chartFill = ChartFill1,
        chartLine = ChartLine1
    )
    AppTheme.AGUAMARINA_NEGRO -> VoltBodyColors(
        accent = NeonAquamarine,
        accentDim = NeonAquamarineDim,
        bg = BgAguamarinaNegro,
        surface = SurfaceAguamarinaNegro,
        chartFill = ChartFill2,
        chartLine = ChartLine2
    )
    AppTheme.OCASO_NEGRO -> VoltBodyColors(
        accent = NeonOcaso,
        accentDim = NeonOcasoDim,
        bg = BgOcasoNegro,
        surface = SurfaceOcasoNegro,
        chartFill = ChartFill3,
        chartLine = ChartLine3
    )
}

// ── CompositionLocal ─────────────────────────────────────────────────────────

val LocalVoltBodyColors = staticCompositionLocalOf {
    voltBodyColorsForTheme(AppTheme.VERDE_NEGRO)
}

// ── M3 Expressive shapes ──────────────────────────────────────────────────────
// Squircle-biased radii matching M3 Expressive spec
private val VoltBodyShapes = Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(6.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(10.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(24.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
)

// ── Material 3 dark color scheme (mapped to VoltBody tokens) ──────────────────

private fun buildColorScheme(c: VoltBodyColors) = darkColorScheme(
    primary = c.accent,
    onPrimary = ColorBlack,
    primaryContainer = c.accentDim,
    onPrimaryContainer = c.accent,
    secondary = c.surfaceElevated,
    onSecondary = c.textPrimary,
    secondaryContainer = c.surfaceContrast,
    onSecondaryContainer = c.textPrimary,
    tertiary = ColorInfo,
    background = c.bg,
    onBackground = c.textPrimary,
    surface = c.surface,
    onSurface = c.textPrimary,
    surfaceVariant = c.surfaceElevated,
    onSurfaceVariant = c.textMuted,
    outline = c.border,
    error = ColorError,
    onError = ColorBlack
)

// ── Root composable ───────────────────────────────────────────────────────────
// FIX: Removed full-app Brush.verticalGradient which forced a gradient repaint
// on every frame across all composables — expensive on OLED & non-OLED alike.
// Solid bg color is more battery-friendly on OLED (true black = 0 pixels lit)
// and avoids the generic "AI app" gradient-background aesthetic.
// Section-level gradients (hero, motivation card, etc.) are preserved as-is.

@Composable
fun VoltBodyTheme(
    appTheme: AppTheme = AppTheme.VERDE_NEGRO,
    content: @Composable () -> Unit
) {
    val vbColors = remember(appTheme) { voltBodyColorsForTheme(appTheme) }
    val colorScheme = remember(appTheme) { buildColorScheme(vbColors) }

    CompositionLocalProvider(LocalVoltBodyColors provides vbColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = VoltBodyTypography,
            shapes = VoltBodyShapes,
            content = {
                // FIX: Solid background instead of vertical gradient.
                // The bg token is already a near-black, so OLED screens
                // benefit from maximum pixel-off savings.
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = vbColors.bg
                ) {
                    content()
                }
            }
        )
    }
}

// ── Convenience extension ─────────────────────────────────────────────────────

val androidx.compose.material3.MaterialTheme.vbColors: VoltBodyColors
    @Composable get() = LocalVoltBodyColors.current
