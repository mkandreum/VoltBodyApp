package com.voltbody.app.ui.theme

import androidx.compose.ui.graphics.Color

// ── Shared dark neutrals ──────────────────────────────────────────────────────
val ColorBlack = Color(0xFF000000)
val ColorNearBlack = Color(0xFF09090C)      // app-bg default
val ColorSurface = Color(0xFF131317)        // app-surface
val ColorSurfaceElevated = Color(0xFF1A1A20)
val ColorSurfaceContrast = Color(0xFF202028)
val ColorBorder = Color(0x14FFFFFF)         // rgba(255,255,255,0.08)
val ColorTextMuted = Color(0xFFB8B8C0)
val ColorWhite = Color(0xFFFFFFFF)

// ── Theme: Verde-Negro ────────────────────────────────────────────────────────
val NeonGreen = Color(0xFF39FF14)           // #39ff14
val NeonGreenDim = Color(0x4D39FF14)        // 30% opacity
val BgVerdeNegro = Color(0xFF09090C)
val SurfaceVerdeNegro = Color(0xFF131317)

// ── Theme: Aguamarina-Negro ───────────────────────────────────────────────────
val NeonAquamarine = Color(0xFF3FF5D0)      // #3ff5d0
val NeonAquamarineDim = Color(0x333FF5D0)   // 20% opacity
val BgAguamarinaNegro = Color(0xFF03110E)
val SurfaceAguamarinaNegro = Color(0xFF091C17)

// ── Theme: Ocaso-Negro ────────────────────────────────────────────────────────
val NeonOcaso = Color(0xFFFF8A3D)           // warm amber/orange — matches web #ff8a3d
val NeonOcasoDim = Color(0x40FF8A3D)        // 25% opacity — matches web
val BgOcasoNegro = Color(0xFF120905)
val SurfaceOcasoNegro = Color(0xFF1E0F0A)

// ── Semantic colors ───────────────────────────────────────────────────────────
val ColorSuccess = Color(0xFF34D399)        // emerald-400
val ColorWarning = Color(0xFFFBBF24)        // amber-400
val ColorError = Color(0xFFF87171)          // red-400
val ColorInfo = Color(0xFF60A5FA)           // blue-400
val ColorOrange = Color(0xFFFB923C)         // orange-400

val ColorProtein = Color(0xFFF87171)        // Same as Error/Red
val ColorCarb = Color(0xFF34D399)           // Same as Success/Green
val ColorFat = Color(0xFFFBBF24)            // Same as Warning/Amber

// ── Glass / neuro surfaces ────────────────────────────────────────────────────
val GlassSurface = Color(0x9913131A)        // 60% surface
val GlassBorder = Color(0x14FFFFFF)         // rgba(255,255,255,0.08)
val NeuroSurface = Color(0x80141414) // translúcido
val NeuroShadowDark = Color(0xCC000000)
val NeuroShadowLight = Color(0x0FFFFFFF)

// ── iOS 26 Liquid Glass tokens ───────────────────────────────────────────────
// Specular highlights, depth shadows, and glass translucency values
// inspired by Apple's Liquid Glass design language.
val LiquidSpecularHighlight = Color(0x12FFFFFF)   // Top-edge specular (7% white)
val LiquidSpecularStrong = Color(0x1AFFFFFF)      // Stronger highlight for focus
val LiquidDepthShadow = Color(0x66000000)         // Depth shadow 40%
val LiquidGlassBorder = Color(0x1AFFFFFF)         // Glass border 10% white
val LiquidGlassBorderFocus = Color(0x30FFFFFF)    // Focus state 19% white

// Glass alphas for different elevation levels
const val LiquidAlphaLevel0 = 0.50f               // Lowest: navbars, overlays
const val LiquidAlphaLevel1 = 0.60f               // Standard cards
const val LiquidAlphaLevel2 = 0.70f               // Elevated panels
const val LiquidAlphaLevel3 = 0.80f               // Prominent surfaces

// ── Chart colors ─────────────────────────────────────────────────────────────
val ChartLine1 = NeonGreen
val ChartLine2 = NeonAquamarine
val ChartLine3 = NeonOcaso
val ChartFill1 = Color(0x2239FF14)          // 13% neon green
val ChartFill2 = Color(0x223FF5D0)
val ChartFill3 = Color(0x22FF8A3D)
