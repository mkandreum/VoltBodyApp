package com.voltbody.app.ui.screens.auth

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.rememberPulse
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt
import kotlinx.coroutines.delay

/** Resplandor ambiental (equivale a los blur-[100px] de la web). */
fun Modifier.ambientGlow(color: Color, center: Offset, radius: Float): Modifier = this.drawBehind {
    drawRect(Brush.radialGradient(listOf(color, Color.Transparent), center = center, radius = radius))
}

/** Icono de marca: cuadrado "squircle" con rayo y halo pulsante. */
@Composable
fun BoltLogo(size: Dp = 96.dp, pulsing: Boolean = true, modifier: Modifier = Modifier) {
    val p by rememberPulse(1f, 1.12f, 1100)
    val halo = Volt.accent.copy(alpha = 0.22f)
    val shape = RoundedCornerShape(size * 0.27f)
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        Box(Modifier.matchParentSize().scale(if (pulsing) p else 1f).clip(shape).background(halo))
        Box(
            Modifier.matchParentSize().clip(shape)
                .background(Brush.verticalGradient(listOf(Color.White.copy(.15f), Color.White.copy(.05f))))
                .border(1.dp, Color.White.copy(.2f), shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Bolt, null, tint = Volt.accent, modifier = Modifier.size(size * 0.55f))
        }
    }
}

@Composable
private fun BreathingDots() {
    val t = rememberInfiniteTransition(label = "dots")
    val color = Volt.accent
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(3) { i ->
            val v by t.animateFloat(
                0.25f, 1f,
                infiniteRepeatable(tween(850, delayMillis = i * 160, easing = FastOutSlowInEasing), RepeatMode.Reverse),
                label = "dot$i",
            )
            Box(Modifier.size(9.dp).scale(0.8f + 0.45f * v).alpha(v).clip(CircleShape).background(color))
        }
    }
}

/** Splash animado con rayo (portado de SplashScreen.tsx). Se usa mientras se hace login/registro. */
@Composable
fun SplashOverlay(modifier: Modifier = Modifier) {
    val enter = remember { Animatable(0f) }
    LaunchedEffect(Unit) { enter.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) }
    val glow = Volt.accent.copy(alpha = 0.15f)
    Box(
        modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.92f))
            .drawBehind { drawRect(Brush.radialGradient(listOf(glow, Color.Transparent), center = Offset(size.width / 2, size.height / 2), radius = size.minDimension * 0.7f)) },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            Modifier.alpha(enter.value).scale(0.85f + 0.15f * enter.value),
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(22.dp),
        ) {
            BoltLogo(96.dp)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val accent = Volt.accent
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White)) { append("Volt") }
                        withStyle(SpanStyle(color = accent)) { append("Body") }
                    },
                    fontSize = 26.sp, fontWeight = FontWeight.Black,
                )
                Text("AI FITNESS SYSTEM", color = Volt.textDim, fontSize = 10.sp, letterSpacing = 2.5.sp)
            }
            BreathingDots()
        }
    }
}

private val PLAN_STEPS = listOf(
    "Analizando tu perfil", "Calculando tus macronutrientes", "Diseñando tu rutina semanal",
    "Estructurando la progresión", "Buscando ejercicios y animaciones", "Puliendo los últimos detalles",
)
private val PLAN_PHRASES = listOf(
    "Nuestra IA está calculando tus macronutrientes y estructurando tu progresión biomecánica.",
    "Esto puede tardar hasta un minuto. No cierres la app.",
    "Adaptando series, repeticiones y cargas a tu nivel.",
    "Ajustando las comidas a tus horarios y preferencias.",
)

/** Pantalla de carga mientras la IA genera el plan (rayo con anillo giratorio, pasos que se van marcando). */
@Composable
fun GeneratingPlanScreen(modifier: Modifier = Modifier) {
    val t = rememberInfiniteTransition(label = "gen")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(2600, easing = LinearEasing)), label = "rot")
    val tilt by t.animateFloat(-5f, 5f, infiniteRepeatable(tween(1500, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "tilt")
    var stage by remember { mutableIntStateOf(0) }
    var phrase by remember { mutableIntStateOf(0) }
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) { progress.animateTo(0.92f, tween(40000, easing = LinearOutSlowInEasing)) }
    LaunchedEffect(Unit) { while (true) { delay(3200); if (stage < PLAN_STEPS.size - 1) stage++ } }
    LaunchedEffect(Unit) { while (true) { delay(4500); phrase = (phrase + 1) % PLAN_PHRASES.size } }
    val accent = Volt.accent
    val track = Volt.surface2
    val glow = Volt.accent.copy(alpha = 0.15f)

    Box(
        modifier.fillMaxSize().drawBehind { drawRect(Brush.radialGradient(listOf(glow, Color.Transparent), center = Offset(size.width / 2, size.height * 0.35f), radius = size.minDimension * 0.8f)) },
        contentAlignment = Alignment.Center,
    ) {
        Column(Modifier.padding(horizontal = 28.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(150.dp), contentAlignment = Alignment.Center) {
                Canvas(Modifier.matchParentSize().rotate(rot)) {
                    val sw = 5.dp.toPx()
                    val d = size.minDimension - sw
                    val tl = Offset((size.width - d) / 2, (size.height - d) / 2)
                    drawArc(track, 0f, 360f, false, tl, Size(d, d), style = Stroke(sw))
                    drawArc(Brush.sweepGradient(listOf(Color.Transparent, accent)), 0f, 300f, false, tl, Size(d, d), style = Stroke(sw, cap = StrokeCap.Round))
                }
                BoltLogo(92.dp, modifier = Modifier.rotate(tilt))
            }
            Spacer(Modifier.height(26.dp))
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = Color.White)) { append("Creando tu ") }
                    withStyle(SpanStyle(color = accent)) { append("Plan Óptimo") }
                },
                fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            AnimatedContent(
                phrase,
                transitionSpec = { (slideInVertically { it / 2 } + fadeIn(tween(400))) togetherWith (slideOutVertically { -it / 2 } + fadeOut(tween(250))) },
                label = "phrase",
            ) { i -> Text(PLAN_PHRASES[i], color = Volt.textDim, fontSize = 13.sp, textAlign = TextAlign.Center, lineHeight = 18.sp, modifier = Modifier.heightIn(min = 54.dp)) }
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(6.dp).clip(CircleShape).background(Volt.surface2)) {
                Box(Modifier.fillMaxWidth(progress.value).fillMaxHeight().clip(CircleShape).background(Brush.horizontalGradient(listOf(Color(0xFF2DD4BF), accent))))
            }
            Spacer(Modifier.height(20.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                PLAN_STEPS.forEachIndexed { i, label ->
                    if (i <= stage) {
                        Row(Modifier.staggeredEntry(0), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                Modifier.size(20.dp).clip(CircleShape).background(if (i < stage) accent else Volt.surface2),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (i < stage) Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(13.dp))
                                else Box(Modifier.size(8.dp).clip(CircleShape).background(accent))
                            }
                            Spacer(Modifier.width(12.dp))
                            Text(label, color = if (i < stage) Volt.textDim else Volt.text, fontSize = 13.sp, fontWeight = if (i == stage) FontWeight.SemiBold else FontWeight.Normal)
                        }
                    }
                }
            }
        }
    }
}
