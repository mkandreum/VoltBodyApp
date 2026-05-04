package com.voltbody.app.ui.theme

import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.*
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

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun VoltBodyTheme(
    appTheme: AppTheme = AppTheme.VERDE_NEGRO,
    content: @Composable () -> Unit
) {
    val vbColors = remember(appTheme) { voltBodyColorsForTheme(appTheme) }
    val colorScheme = remember(appTheme) { buildColorScheme(vbColors) }

    CompositionLocalProvider(LocalVoltBodyColors provides vbColors) {
        MaterialExpressiveTheme(
            colorScheme = colorScheme,
            typography = VoltBodyTypography,
            shapes = VoltBodyShapes,
            motionScheme = MotionScheme.expressive(),
            content = {
                Surface(
                    modifier = androidx.compose.ui.Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    Box(
                        modifier = androidx.compose.ui.Modifier
                            .fillMaxSize()
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        vbColors.bg,
                                        Color(0xFF000000)
                                    )
                                )
                            )
                    ) {
                        content()
                    }
                }
            }
        )
    }
}

// ── Convenience extension ─────────────────────────────────────────────────────

val androidx.compose.material3.MaterialTheme.vbColors: VoltBodyColors
    @Composable get() = LocalVoltBodyColors.current
