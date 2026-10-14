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

/** Titulares con la grotesca de la v3 (Bricolage) y cifras con la display (Unbounded), igual que [com.voltbody.app.ui.v3.T]. */
private fun hd(size: Int, weight: Int, lh: Int, ls: Float = -0.02f) =
    TextStyle(fontFamily = com.voltbody.app.ui.v3.TitleFont, fontSize = size.sp, fontWeight = FontWeight(weight), lineHeight = lh.sp, letterSpacing = ls.em)
private fun num(size: Int, lh: Int, ls: Float = -0.02f) =
    TextStyle(fontFamily = com.voltbody.app.ui.v3.Display, fontSize = size.sp, fontWeight = FontWeight(700), lineHeight = lh.sp, letterSpacing = ls.em)

/**
 * Estilos del sistema de diseno (sin color: heredan o se pasan explicitos).
 * Alineados con la v3 para que las tarjetas antiguas no desentonen: titulares en Bricolage,
 * cifras en Unbounded y cuerpo en Jakarta.
 */
object VoltType {
    val display by lazy { hd(48, 800, 50) }
    val h1 by lazy { hd(32, 800, 35) }
    val h2 by lazy { hd(22, 700, 26, -0.01f) }
    val title = st(16, 600, 21)
    val body = st(14, 500, 20)
    val label = st(11, 700, 14, 0.06.em)
    /** Numeros: fuente display de la v3 (ancha), tamanos algo menores que antes para compensar. */
    val numberXL by lazy { num(52, 58, -0.03f) }
    val numberL by lazy { num(34, 40) }
    val numberM by lazy { num(24, 30) }
    val numberS by lazy { num(17, 22, 0f) }
}

val VoltTypography by lazy { Typography(
    displayLarge = VoltType.display, displayMedium = hd(44, 800, 48), displaySmall = hd(36, 800, 40),
    headlineLarge = VoltType.h1, headlineMedium = hd(26, 800, 30, -0.015f), headlineSmall = VoltType.h2,
    titleLarge = st(20, 700, 26), titleMedium = VoltType.title, titleSmall = st(14, 700, 18),
    bodyLarge = st(15, 500, 22), bodyMedium = VoltType.body, bodySmall = st(12, 500, 16),
    labelLarge = st(14, 700, 18), labelMedium = st(12, 700, 16), labelSmall = VoltType.label,
) }
