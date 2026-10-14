package com.voltbody.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color

/** Paleta por tema (mismos 3 temas que la web) con inspiracion visual en el mockup: negro, tarjetas oscuras, acento neon. */
data class VoltColors(
    val accent: Color, val accentDim: Color, val bg: Color, val surface: Color, val surface2: Color,
    val border: Color, val text: Color, val textDim: Color, val orange: Color, val water: Color, val danger: Color,
)

fun voltColors(theme: String): VoltColors = when (theme) {
    "aguamarina-negro" -> VoltColors(Color(0xFF3FF5D0), Color(0x333FF5D0), Color(0xFF03110E), Color(0xFF0A1A16), Color(0xFF10251F), Color(0xFF1F3A34), Color.White, Color(0xFF8FA8A2), Color(0xFFFF8A00), Color(0xFF38BDF8), Color(0xFFFF5470))
    "ocaso-negro" -> VoltColors(Color(0xFFFF8A3D), Color(0x40FF8A3D), Color(0xFF120905), Color(0xFF1A100A), Color(0xFF24170F), Color(0xFF3A271C), Color.White, Color(0xFFB59A8A), Color(0xFFFFC857), Color(0xFF38BDF8), Color(0xFFFF5470))
    else -> VoltColors(Color(0xFFC6F135), Color(0x33C6F135), Color(0xFF0A0A0A), Color(0xFF161616), Color(0xFF1E1E1E), Color(0x14FFFFFF), Color.White, Color(0xFF9A9A9A), Color(0xFFFF8A00), Color(0xFF38BDF8), Color(0xFFFF5470))
}

val LocalVolt = staticCompositionLocalOf { voltColors("verde-negro") }

/** Acceso corto: Volt.accent, Volt.surface... */
val Volt: VoltColors @Composable get() = LocalVolt.current

@Composable
fun VoltTheme(theme: String, content: @Composable () -> Unit) {
    val c = voltColors(theme)
    val scheme = darkColorScheme(
        primary = c.accent, onPrimary = Color.Black, secondary = c.orange, background = c.bg, onBackground = c.text,
        surface = c.surface, onSurface = c.text, surfaceVariant = c.surface2, onSurfaceVariant = c.textDim,
        outline = c.border, error = c.danger,
    )
    CompositionLocalProvider(LocalVolt provides c) {
        MaterialTheme(colorScheme = scheme, typography = Typography(), content = content)
    }
}
