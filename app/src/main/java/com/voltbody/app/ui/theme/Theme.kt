package com.voltbody.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

/**
 * Paleta por tema. Los ids vienen del servidor (no cambian) pero los colores/etiquetas si:
 *  verde-negro = Lima neón (defecto), aguamarina-negro = Aguamarina, ocaso-negro = Ocaso.
 * Gradiente de marca = accent -> accent2 (solo para el banner). Kcal siempre naranja.
 */
data class VoltColors(
    val accent: Color, val accentDim: Color, val bg: Color, val surface: Color, val surface2: Color,
    val border: Color, val text: Color, val textDim: Color, val orange: Color, val water: Color, val danger: Color,
    val accent2: Color = accent,
    val onAccent: Color = Color(0xFF0B0B0F),
) {
    /** Gradiente de marca horizontal (accent -> accent2). */
    // Sin degradados de color (decision de diseno): la "marca" es el acento solido.
    val gradient: Brush get() = androidx.compose.ui.graphics.SolidColor(accent)
    val gradientV: Brush get() = androidx.compose.ui.graphics.SolidColor(accent)
}

private val BG = Color(0xFF0A0A0A)
private val SURFACE = Color(0xFF161616)
private val SURFACE2 = Color(0xFF212121)
private val TEXT = Color(0xFFF5F5F2)
private val DIM = Color(0xFF9A9A9A)
private val BORDER = Color(0x14FFFFFF) // blanco 8 %
private val DANGER = Color(0xFFFF4D6A)
private val WATER = Color(0xFF38BDF8)

// accent2 = accent: cualquier degradado acento->acento2 heredado queda en color solido (sin degradados de color).
private fun palette(accent: Color, @Suppress("UNUSED_PARAMETER") accent2: Color, warm: Color) =
    VoltColors(accent, accent.copy(alpha = 0.22f), BG, SURFACE, SURFACE2, BORDER, TEXT, DIM, warm, WATER, DANGER, accent)

// Verde neon #27F58B sobre negro (tema por defecto); kcal siempre naranja.
private val LIMA = palette(Color(0xFF27F58B), Color(0xFF22D3EE), Color(0xFFFF8A00))
private val AGUA = palette(Color(0xFF3FF5D0), Color(0xFF38BDF8), Color(0xFFFF8A00))
private val OCASO = palette(Color(0xFFFF8A3D), Color(0xFFFFC857), Color(0xFFFF8A00))

fun voltColors(theme: String): VoltColors = when (theme) {
    "aguamarina-negro" -> AGUA
    "ocaso-negro" -> OCASO
    else -> LIMA
}

/** Etiqueta legible de cada id de tema del servidor. */
fun themeLabel(theme: String): String = when (theme) {
    "aguamarina-negro" -> "Aguamarina"
    "ocaso-negro" -> "Ocaso"
    else -> "Neón"
}

val LocalVolt = staticCompositionLocalOf { LIMA }

/** Acceso corto: Volt.accent, Volt.accent2, Volt.gradient... */
val Volt: VoltColors @Composable get() = LocalVolt.current

/** Desplaza el matiz de un color (grados). Usado para fondos que cambian de tono por paso. */
fun Color.shiftHue(degrees: Float): Color {
    val hsv = FloatArray(3)
    android.graphics.Color.colorToHSV(toArgb(), hsv)
    hsv[0] = ((hsv[0] + degrees) % 360f + 360f) % 360f
    return Color(android.graphics.Color.HSVToColor(hsv))
}

@Composable
fun VoltTheme(theme: String, content: @Composable () -> Unit) {
    val c = remember(theme) { voltColors(theme) }
    val scheme = remember(c) {
        darkColorScheme(
            primary = c.accent, onPrimary = c.onAccent, secondary = c.accent2, background = c.bg, onBackground = c.text,
            surface = c.surface, onSurface = c.text, surfaceVariant = c.surface2, onSurfaceVariant = c.textDim,
            outline = c.border, error = c.danger,
        )
    }
    CompositionLocalProvider(LocalVolt provides c) {
        MaterialTheme(colorScheme = scheme, typography = VoltTypography, content = content)
    }
}
