package com.voltbody.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.voltbody.app.R

/** Plus Jakarta Sans (fuente variable en res/font/jakarta.ttf) con los pesos usados por la app. */
@OptIn(ExperimentalTextApi::class)
val Jakarta: FontFamily = FontFamily(
    listOf(400, 500, 600, 700, 800).map { w ->
        Font(R.font.jakarta, FontWeight(w), variationSettings = FontVariation.Settings(FontVariation.weight(w)))
    }
)

private fun st(size: Int, weight: Int, lh: Int = (size * 1.3f).toInt(), ls: TextUnit = 0.sp) =
    TextStyle(fontFamily = Jakarta, fontSize = size.sp, fontWeight = FontWeight(weight), lineHeight = lh.sp, letterSpacing = ls)

/** Estilos del sistema de diseno (sin color: heredan o se pasan explicitos). */
object VoltType {
    val display = st(52, 800, 54, (-0.02f).em)
    val h1 = st(32, 800, 36, (-0.02f).em)
    val h2 = st(22, 700, 27, (-0.01f).em)
    val title = st(17, 700, 22)
    val body = st(14, 500, 20)
    val label = st(11, 700, 14, 0.08.em)
    /** Numeros grandes: siempre 800. */
    val numberXL = st(64, 800, 66, (-0.03f).em)
    val numberL = st(40, 800, 44, (-0.02f).em)
    val numberM = st(28, 800, 32, (-0.01f).em)
    val numberS = st(18, 800, 22)
}

val VoltTypography = Typography(
    displayLarge = VoltType.display, displayMedium = st(44, 800, 48, (-0.02f).em), displaySmall = st(36, 800, 40, (-0.02f).em),
    headlineLarge = VoltType.h1, headlineMedium = st(26, 800, 30, (-0.015f).em), headlineSmall = VoltType.h2,
    titleLarge = st(20, 700, 26), titleMedium = VoltType.title, titleSmall = st(14, 700, 18),
    bodyLarge = st(15, 500, 22), bodyMedium = VoltType.body, bodySmall = st(12, 500, 16),
    labelLarge = st(14, 700, 18), labelMedium = st(12, 700, 16), labelSmall = VoltType.label,
)
