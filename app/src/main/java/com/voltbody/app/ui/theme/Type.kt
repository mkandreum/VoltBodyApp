package com.voltbody.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.sp

// ── Google Fonts Provider ─────────────────────────────────────────────────────
// Downloads Inter, Barlow Condensed, and JetBrains Mono at runtime.
// Falls back to system defaults if the download fails.

private val GoogleFontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = com.voltbody.app.R.array.com_google_android_gms_fonts_certs
)

private val InterFont = GoogleFont("Inter")
private val BarlowCondensedFont = GoogleFont("Barlow Condensed")
private val JetBrainsMonoFont = GoogleFont("JetBrains Mono")

// ── Font Families ─────────────────────────────────────────────────────────────

val InterFontFamily = FontFamily(
    Font(googleFont = InterFont, fontProvider = GoogleFontProvider, weight = FontWeight.Normal),
    Font(googleFont = InterFont, fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = InterFont, fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = InterFont, fontProvider = GoogleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = InterFont, fontProvider = GoogleFontProvider, weight = FontWeight.ExtraBold),
    Font(googleFont = InterFont, fontProvider = GoogleFontProvider, weight = FontWeight.Black),
)

// Custom display font (Barlow Condensed — bold, condensed, uppercase)
val DisplayFontFamily = FontFamily(
    Font(googleFont = BarlowCondensedFont, fontProvider = GoogleFontProvider, weight = FontWeight.SemiBold),
    Font(googleFont = BarlowCondensedFont, fontProvider = GoogleFontProvider, weight = FontWeight.Bold),
    Font(googleFont = BarlowCondensedFont, fontProvider = GoogleFontProvider, weight = FontWeight.ExtraBold),
)

// Mono font (JetBrains Mono — metrics, code, stat values)
val MonoFontFamily = FontFamily(
    Font(googleFont = JetBrainsMonoFont, fontProvider = GoogleFontProvider, weight = FontWeight.Medium),
    Font(googleFont = JetBrainsMonoFont, fontProvider = GoogleFontProvider, weight = FontWeight.Bold),
)

val VoltBodyTypography = Typography(
    // Display / Hero headings — Barlow Condensed style
    displayLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.02).sp
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.Black,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.01).sp
    ),
    displaySmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // Headlines — section headers, card titles
    headlineLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.012).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 24.sp,
        letterSpacing = (-0.012).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.012).sp
    ),

    // Titles — list items, bottom nav labels
    titleLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.01).sp
    ),
    titleMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.006).sp
    ),
    titleSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),

    // Body text
    bodyLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.012).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.008).sp
    ),
    bodySmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),

    // Labels — chips, badges, micro UI
    labelLarge = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.02.sp
    ),
    labelMedium = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.05.sp
    ),
    labelSmall = TextStyle(
        fontFamily = InterFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 9.sp,
        lineHeight = 12.sp,
        letterSpacing = 0.06.sp
    )
)

// ── Uppercase tracking style for section labels ────────────────────────────────
val UppercaseLabel = TextStyle(
    fontFamily = InterFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 10.sp,
    lineHeight = 14.sp,
    letterSpacing = 0.1.sp
)

// ── Mono metric style (JetBrains Mono analog) ────────────────────────────────
val MonoMetric = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 22.sp,
    lineHeight = 26.sp,
    letterSpacing = (-0.01).sp
)
