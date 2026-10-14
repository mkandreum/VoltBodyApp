package com.voltbody.app.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.Pose
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.voltbody.app.ui.theme.Volt
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.acos
import kotlin.math.sqrt

// -------------------------------------------------------------------------------------------
// Modelo / reglas (port de PoseCoach.tsx)
// -------------------------------------------------------------------------------------------

/** Landmark normalizado (0..1 sobre la imagen ya girada) con visibilidad. */
private data class Lm(val x: Float, val y: Float, val v: Float)

private enum class Severity { OK, WARNING, ERROR }
private data class PoseRule(val id: String, val message: String, val severity: Severity)

private enum class PoseExercise { SQUAT, PLANK, DEADLIFT, LUNGE, PUSHUP, GENERAL }

// Indices MediaPipe/ML Kit (33 puntos)
private const val NOSE = 0
private const val L_SHOULDER = 11; private const val R_SHOULDER = 12
private const val L_ELBOW = 13; private const val R_ELBOW = 14
private const val L_WRIST = 15; private const val R_WRIST = 16
private const val L_HIP = 23; private const val R_HIP = 24
private const val L_KNEE = 25; private const val R_KNEE = 26
private const val L_ANKLE = 27; private const val R_ANKLE = 28

private val CONNECTIONS = listOf(
    11 to 12, 11 to 13, 13 to 15, 12 to 14, 14 to 16, 11 to 23, 12 to 24, 23 to 24,
    23 to 25, 25 to 27, 24 to 26, 26 to 28, 27 to 29, 29 to 31, 27 to 31, 28 to 30, 30 to 32, 28 to 32,
    15 to 17, 15 to 19, 15 to 21, 16 to 18, 16 to 20, 16 to 22,
    0 to 1, 1 to 2, 2 to 3, 3 to 7, 0 to 4, 4 to 5, 5 to 6, 6 to 8, 9 to 10,
)
private val BODY_POINTS = listOf(0, 11, 12, 13, 14, 15, 16, 23, 24, 25, 26, 27, 28)

/** Detecta el tipo de ejercicio por el nombre (ES/EN). Desconocido -> GENERAL (usa reglas de sentadilla, como la web). */
private fun detectExercise(name: String): PoseExercise {
    val n = name.lowercase()
    return when {
        listOf("sentadilla", "squat", "prensa", "goblet").any { n.contains(it) } -> PoseExercise.SQUAT
        listOf("plancha", "plank").any { n.contains(it) } -> PoseExercise.PLANK
        listOf("peso muerto", "deadlift", "buenos d", "good morning", "rumano").any { n.contains(it) } -> PoseExercise.DEADLIFT
        listOf("zancada", "estocada", "lunge", "desplante", "split squat", "búlgara", "bulgar").any { n.contains(it) } -> PoseExercise.LUNGE
        listOf("flexi", "push-up", "pushup", "push up", "lagartija", "fondos", "dip").any { n.contains(it) } -> PoseExercise.PUSHUP
        else -> PoseExercise.GENERAL
    }
}

/** Angulo en el vertice B formado por A-B-C (0-180). `aspect` = ancho/alto para no deformar angulos. */
private fun angleDeg(a: Lm, b: Lm, c: Lm, aspect: Float): Float {
    val ax = (a.x - b.x) * aspect; val ay = a.y - b.y
    val cx = (c.x - b.x) * aspect; val cy = c.y - b.y
    val magA = sqrt(ax * ax + ay * ay); val magC = sqrt(cx * cx + cy * cy)
    if (magA == 0f || magC == 0f) return 0f
    val cos = ((ax * cx + ay * cy) / (magA * magC)).coerceIn(-1f, 1f)
    return (acos(cos.toDouble()) * 180.0 / Math.PI).toFloat()
}

private fun Array<Lm?>.vis(i: Int, th: Float = 0.5f): Lm? = getOrNull(i)?.takeIf { it.v >= th }

/** Lado con mejor visibilidad (cadera+rodilla+tobillo+hombro). Devuelve true si es el izquierdo. */
private fun Array<Lm?>.leftSide(): Boolean {
    fun score(ids: List<Int>) = ids.sumOf { (getOrNull(it)?.v ?: 0f).toDouble() }
    return score(listOf(L_SHOULDER, L_HIP, L_KNEE, L_ANKLE)) >= score(listOf(R_SHOULDER, R_HIP, R_KNEE, R_ANKLE))
}

private fun rulesSquat(lm: Array<Lm?>, aspect: Float): List<PoseRule> {
    val rules = mutableListOf<PoseRule>()
    val left = lm.leftSide()
    val shoulder = lm.vis(if (left) L_SHOULDER else R_SHOULDER)
    val hip = lm.vis(if (left) L_HIP else R_HIP)
    val knee = lm.vis(if (left) L_KNEE else R_KNEE)
    val ankle = lm.vis(if (left) L_ANKLE else R_ANKLE)

    if (hip != null && knee != null && ankle != null) {
        val kneeAngle = angleDeg(hip, knee, ankle, aspect)
        when {
            kneeAngle > 145 -> rules += PoseRule("depth", "⬇️ Baja más — rodilla a 90°", Severity.WARNING)
            kneeAngle < 60 -> rules += PoseRule("depth", "✅ Buena profundidad", Severity.OK)
            else -> rules += PoseRule("depth", "✅ Profundidad correcta", Severity.OK)
        }
    }
    // Valgo: rodilla debe seguir la linea cadera-tobillo
    val lh = lm.vis(L_HIP); val lk = lm.vis(L_KNEE); val la = lm.vis(L_ANKLE)
    if (lh != null && lk != null && la != null && lk.x > la.x + 0.04f)
        rules += PoseRule("valgus-left", "⚠️ Rodilla izquierda colapsando hacia dentro", Severity.ERROR)
    val rh = lm.vis(R_HIP); val rk = lm.vis(R_KNEE); val ra = lm.vis(R_ANKLE)
    if (rh != null && rk != null && ra != null && rk.x < ra.x - 0.04f)
        rules += PoseRule("valgus-right", "⚠️ Rodilla derecha colapsando hacia dentro", Severity.ERROR)

    if (shoulder != null && hip != null && knee != null) {
        val torso = angleDeg(shoulder, hip, knee, aspect)
        if (torso < 40) rules += PoseRule("lean", "⚠️ Demasiada inclinación hacia adelante", Severity.WARNING)
        else rules += PoseRule("lean", "✅ Torso erguido", Severity.OK)
    }
    return rules
}

private fun rulesPlank(lm: Array<Lm?>): List<PoseRule> {
    val rules = mutableListOf<PoseRule>()
    val left = lm.leftSide()
    val s = lm.vis(if (left) L_SHOULDER else R_SHOULDER)
    val h = lm.vis(if (left) L_HIP else R_HIP)
    val a = lm.vis(if (left) L_ANKLE else R_ANKLE)
    if (s != null && h != null && a != null) {
        val lineY = s.y + (a.y - s.y) * ((h.x - s.x) / (a.x - s.x + 0.0001f))
        val delta = h.y - lineY
        when {
            delta > 0.06f -> rules += PoseRule("hip-drop", "⚠️ Cadera cayendo — activa el core", Severity.ERROR)
            delta < -0.06f -> rules += PoseRule("hip-high", "⚠️ Cadera demasiado alta", Severity.WARNING)
            else -> rules += PoseRule("alignment", "✅ Alineación correcta", Severity.OK)
        }
    }
    return rules
}

private fun rulesDeadlift(lm: Array<Lm?>, aspect: Float): List<PoseRule> {
    val rules = mutableListOf<PoseRule>()
    val left = lm.leftSide()
    val s = lm.vis(if (left) L_SHOULDER else R_SHOULDER)
    val h = lm.vis(if (left) L_HIP else R_HIP)
    val k = lm.vis(if (left) L_KNEE else R_KNEE)
    val a = lm.vis(if (left) L_ANKLE else R_ANKLE)
    if (s != null && h != null && a != null) {
        val back = angleDeg(s, h, a, aspect)
        if (back < 150) rules += PoseRule("back-rounding", "⚠️ Espalda curvada — mantén columna neutra", Severity.ERROR)
        else rules += PoseRule("back-straight", "✅ Espalda recta", Severity.OK)
    }
    if (h != null && k != null && s != null) {
        val hipAngle = angleDeg(s, h, k, aspect)
        if (hipAngle > 160) rules += PoseRule("hinge", "⬇️ Lleva más las caderas hacia atrás", Severity.WARNING)
        else rules += PoseRule("hinge", "✅ Bisagra de cadera correcta", Severity.OK)
    }
    return rules
}

private fun rulesLunge(lm: Array<Lm?>, aspect: Float): List<PoseRule> {
    val rules = mutableListOf<PoseRule>()
    val left = lm.leftSide()
    val h = lm.vis(if (left) L_HIP else R_HIP)
    val k = lm.vis(if (left) L_KNEE else R_KNEE)
    val a = lm.vis(if (left) L_ANKLE else R_ANKLE)
    if (h != null && k != null && a != null) {
        val kneeAngle = angleDeg(h, k, a, aspect)
        // La web solo comprueba la pierna izquierda; con la derecha el sentido del eje se invierte.
        val forward = if (left) k.x > a.x + 0.05f else k.x < a.x - 0.05f
        if (forward) rules += PoseRule("knee-forward", "⚠️ Rodilla adelantada al pie — retrocede más", Severity.WARNING)
        else rules += PoseRule("knee-ok", "✅ Posición de rodilla correcta", Severity.OK)
        if (kneeAngle > 130) rules += PoseRule("depth", "⬇️ Baja más la rodilla trasera", Severity.WARNING)
    }
    return rules
}

private fun rulesPushup(lm: Array<Lm?>, aspect: Float): List<PoseRule> {
    val rules = mutableListOf<PoseRule>()
    val left = lm.leftSide()
    val s = lm.vis(if (left) L_SHOULDER else R_SHOULDER)
    val e = lm.vis(if (left) L_ELBOW else R_ELBOW)
    val h = lm.vis(if (left) L_HIP else R_HIP)
    val a = lm.vis(if (left) L_ANKLE else R_ANKLE)
    if (s != null && e != null) {
        if (abs(e.x - s.x) > 0.15f) rules += PoseRule("elbow-flare", "⚠️ Codos muy abiertos — mantén a 45°", Severity.WARNING)
        else rules += PoseRule("elbow-ok", "✅ Posición de codos correcta", Severity.OK)
    }
    if (s != null && h != null && a != null) {
        val body = angleDeg(s, h, a, aspect)
        if (body < 155) rules += PoseRule("body-line", "⚠️ Cuerpo no alineado — activa el core", Severity.ERROR)
        else rules += PoseRule("body-line", "✅ Línea corporal correcta", Severity.OK)
    }
    return rules
}

private fun getRules(ex: PoseExercise, lm: Array<Lm?>, aspect: Float): List<PoseRule> = when (ex) {
    PoseExercise.SQUAT -> rulesSquat(lm, aspect)
    PoseExercise.PLANK -> rulesPlank(lm)
    PoseExercise.DEADLIFT -> rulesDeadlift(lm, aspect)
    PoseExercise.LUNGE -> rulesLunge(lm, aspect)
    PoseExercise.PUSHUP -> rulesPushup(lm, aspect)
    PoseExercise.GENERAL -> rulesSquat(lm, aspect)
}

// -------------------------------------------------------------------------------------------
// Contador de repeticiones (maquina de estados con histeresis sobre el angulo principal)
// -------------------------------------------------------------------------------------------

private class RepCounter(private val ex: PoseExercise) {
    var reps = 0; private set
    var holdSeconds = 0f; private set
    private var down = false
    private var smooth: Float? = null
    private var lastTs = 0L

    val isHold get() = ex == PoseExercise.PLANK

    /** Devuelve true si se acaba de completar una repeticion. */
    fun update(lm: Array<Lm?>, aspect: Float, rules: List<PoseRule>, nowMs: Long): Boolean {
        val dt = if (lastTs == 0L) 0f else ((nowMs - lastTs).coerceAtMost(500)) / 1000f
        lastTs = nowMs
        if (ex == PoseExercise.PLANK) {
            // Isometrico: cuenta el tiempo mientras la alineacion sea correcta
            if (rules.isNotEmpty() && rules.none { it.severity == Severity.ERROR }) holdSeconds += dt
            return false
        }
        val left = lm.leftSide()
        val raw: Float = when (ex) {
            PoseExercise.PUSHUP -> {
                val s = lm.vis(if (left) L_SHOULDER else R_SHOULDER); val e = lm.vis(if (left) L_ELBOW else R_ELBOW)
                val w = lm.vis(if (left) L_WRIST else R_WRIST)
                if (s != null && e != null && w != null) angleDeg(s, e, w, aspect) else null
            }
            PoseExercise.DEADLIFT -> {
                val s = lm.vis(if (left) L_SHOULDER else R_SHOULDER); val h = lm.vis(if (left) L_HIP else R_HIP)
                val k = lm.vis(if (left) L_KNEE else R_KNEE)
                if (s != null && h != null && k != null) angleDeg(s, h, k, aspect) else null
            }
            else -> { // sentadilla / zancada / general: angulo de rodilla
                val h = lm.vis(if (left) L_HIP else R_HIP); val k = lm.vis(if (left) L_KNEE else R_KNEE)
                val a = lm.vis(if (left) L_ANKLE else R_ANKLE)
                if (h != null && k != null && a != null) angleDeg(h, k, a, aspect) else null
            }
        } ?: return false
        val prev = smooth
        val sm = if (prev == null) raw else prev + (raw - prev) * 0.4f
        smooth = sm
        val (downTh, upTh) = when (ex) {
            PoseExercise.PUSHUP -> 95f to 155f
            PoseExercise.DEADLIFT -> 115f to 160f
            PoseExercise.LUNGE -> 110f to 160f
            else -> 105f to 160f
        }
        if (!down && sm < downTh) down = true
        else if (down && sm > upTh) { down = false; reps++; return true }
        return false
    }

    fun reset() { reps = 0; holdSeconds = 0f; down = false; smooth = null; lastTs = 0L }
}

// -------------------------------------------------------------------------------------------
// UI
// -------------------------------------------------------------------------------------------

@Composable
fun PoseCoachScreen(exerciseName: String, onClose: () -> Unit) {
    val ctx = LocalContext.current
    var hasCamera by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    var denied by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        hasCamera = ok; denied = !ok
    }
    LaunchedEffect(Unit) { if (!hasCamera) launcher.launch(Manifest.permission.CAMERA) }

    val exercise = remember(exerciseName) { detectExercise(exerciseName) }
    var status by remember { mutableStateOf("loading") } // loading | running | error | nocamera
    var errorMsg by remember { mutableStateOf("") }
    var rules by remember { mutableStateOf<List<PoseRule>>(emptyList()) }
    var points by remember { mutableStateOf<List<Offset?>>(emptyList()) } // normalizados, ya espejados
    var reps by remember { mutableIntStateOf(0) }
    var hold by remember { mutableStateOf(0f) }
    var frames by remember { mutableIntStateOf(0) }
    val counter = remember(exercise) { RepCounter(exercise) }
    val haptic = LocalHapticFeedback.current
    var repFlash by remember { mutableIntStateOf(0) }

    LaunchedEffect(hasCamera, denied) {
        if (denied && !hasCamera) {
            status = "nocamera"
            errorMsg = "No se pudo acceder a la cámara. Permite el permiso en los ajustes de Android e inténtalo de nuevo."
        }
    }

    val errors = rules.filter { it.severity == Severity.ERROR }
    val warnings = rules.filter { it.severity == Severity.WARNING }
    val oks = rules.filter { it.severity == Severity.OK }
    val hasIssues = errors.isNotEmpty() || warnings.isNotEmpty()
    val stateColor by animateColorAsState(if (hasIssues) Volt.danger else Volt.accent, tween(300), label = "poseState")

    Column(Modifier.fillMaxSize().background(Color.Black).statusBarsPadding().navigationBarsPadding()) {
        // Cabecera
        Row(
            Modifier.fillMaxWidth().background(Color.Black.copy(alpha = .8f)).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(Modifier.weight(1f)) {
                Text("POSECOACH IA", color = Volt.textDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.5.sp)
                Text(exerciseName.ifBlank { "Análisis de postura" }, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold,
                    maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            AnimatedVisibility(status == "running", enter = fadeIn() + scaleIn(), exit = fadeOut()) {
                val pulse = rememberInfiniteTransition(label = "dot")
                val a by pulse.animateFloat(0.3f, 1f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "dotA")
                Row(
                    Modifier.padding(end = 10.dp).clip(RoundedCornerShape(50)).background(stateColor.copy(alpha = .18f))
                        .border(1.dp, stateColor.copy(alpha = .5f), RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(8.dp).clip(CircleShape).background(stateColor.copy(alpha = a)))
                    Spacer(Modifier.width(6.dp))
                    Text(if (hasIssues) "Atención" else "Postura correcta", color = stateColor, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Box(
                Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = .1f))
                    .pressScale { onClose() },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Filled.Close, contentDescription = "Cerrar PoseCoach", tint = Color.White) }
        }

        // Camara + overlay
        Box(Modifier.weight(1f).fillMaxWidth().background(Color.Black), contentAlignment = Alignment.Center) {
            if (hasCamera) {
                CameraWithPose(
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f / 4f),
                    onReady = { status = "running" },
                    onError = { status = "error"; errorMsg = it },
                    onResult = { lm, aspect, imgPoints ->
                        points = imgPoints
                        val r = getRules(exercise, lm, aspect)
                        rules = r
                        frames++
                        val rep = counter.update(lm, aspect, r, System.currentTimeMillis())
                        if (rep) { reps = counter.reps; repFlash++; haptic.performHapticFeedback(HapticFeedbackType.LongPress) }
                        hold = counter.holdSeconds
                    },
                    onNoPerson = { points = emptyList() },
                    overlay = { SkeletonOverlay(points, stateColor, Modifier.fillMaxSize()) },
                )
            }

            // Contador de repeticiones / tiempo
            if (status == "running") {
                Column(
                    Modifier.align(Alignment.TopStart).padding(14.dp).clip(RoundedCornerShape(20.dp))
                        .background(Color.Black.copy(alpha = .55f)).padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val scale = remember { Animatable(1f) }
                    LaunchedEffect(repFlash) {
                        if (repFlash > 0) { scale.snapTo(1.4f); scale.animateTo(1f, spring(0.4f, Spring.StiffnessMedium)) }
                    }
                    Text(if (counter.isHold) "TIEMPO" else "REPS", color = Volt.textDim, fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    if (counter.isHold) {
                        Text("${hold.toInt()}s", color = Volt.accent, fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
                    } else {
                        AnimatedContent(
                            reps,
                            transitionSpec = { (slideInVertically { -it } + fadeIn()) togetherWith (slideOutVertically { it } + fadeOut()) },
                            label = "reps",
                        ) { n ->
                            Text("$n", color = Volt.accent, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, modifier = Modifier.scale(scale.value))
                        }
                    }
                }
            }

            // Estados
            if (status == "loading" && hasCamera) {
                Column(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = .9f)).padding(24.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    CircularProgressIndicator(color = Volt.accent)
                    Spacer(Modifier.height(14.dp))
                    Text("Iniciando PoseCoach…", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text("Cargando modelo de postura en tu dispositivo", color = Volt.textDim, fontSize = 12.sp)
                }
            }
            if (status == "error" || status == "nocamera") {
                Column(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = .95f)).padding(32.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Icon(if (status == "nocamera") Icons.Filled.CameraAlt else Icons.Filled.Warning, null,
                        tint = if (status == "nocamera") Volt.textDim else Volt.danger, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(12.dp))
                    Text(errorMsg, color = Color.White.copy(alpha = .85f), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
                }
            }
        }

        // Panel de feedback (bottom sheet)
        Column(
            Modifier.fillMaxWidth().heightIn(max = 260.dp).background(Color.Black.copy(alpha = .9f))
                .verticalScroll(rememberScrollState()).padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(RoundedCornerShape(50)).background(Color.White.copy(alpha = .25f)))
            if (status == "running" && rules.isEmpty()) {
                Text("Posiciónate de cuerpo entero frente a la cámara…", color = Volt.textDim, fontSize = 12.sp,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp))
            }
            errors.forEach { FeedbackRow(it, Color(0xFFEF4444), Icons.Filled.Warning) }
            warnings.forEach { FeedbackRow(it, Color(0xFFF59E0B), Icons.Filled.Warning) }
            oks.forEach { FeedbackRow(it, Color(0xFF10B981), Icons.Filled.CheckCircle) }
            if (status == "running" && frames > 0) {
                Text("ML Kit Pose · frame #$frames", color = Color(0xFF6B7280), fontSize = 10.sp, textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun FeedbackRow(rule: PoseRule, tint: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    val state = remember(rule.id) { MutableTransitionState(false).apply { targetState = true } }
    AnimatedVisibility(state, enter = fadeIn() + slideInVertically { it / 3 }, exit = fadeOut()) {
        Row(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(tint.copy(alpha = .14f))
                .border(1.dp, tint.copy(alpha = .3f), RoundedCornerShape(16.dp)).padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(icon, null, tint = tint, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(10.dp))
            Text(rule.message, color = Color.White.copy(alpha = .92f), fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/** Esqueleto animado: conexiones + articulaciones con pulso; los puntos ya vienen suavizados. */
@Composable
private fun SkeletonOverlay(points: List<Offset?>, color: Color, modifier: Modifier) {
    val pulse = rememberInfiniteTransition(label = "joint")
    val r by pulse.animateFloat(5f, 8f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "jointR")
    Canvas(modifier) {
        if (points.isEmpty()) return@Canvas
        fun p(i: Int): Offset? = points.getOrNull(i)?.let { Offset(it.x * size.width, it.y * size.height) }
        for ((a, b) in CONNECTIONS) {
            val pa = p(a); val pb = p(b)
            if (pa != null && pb != null) drawLine(color.copy(alpha = .9f), pa, pb, strokeWidth = 5f, cap = StrokeCap.Round)
        }
        for (i in BODY_POINTS) {
            val pt = p(i) ?: continue
            drawCircle(color.copy(alpha = .25f), r * 2.2f, pt)
            drawCircle(Color.White, r * .6f, pt)
            drawCircle(color, r, pt, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3f))
        }
    }
}

/**
 * CameraX (camara frontal) + ML Kit Pose Detection. Entrega landmarks normalizados al hilo principal.
 * `onResult(lm, aspect, puntosEspejados)`: lm sin espejar (para las reglas), puntos espejados para dibujar sobre la preview frontal.
 */
@OptIn(ExperimentalGetImage::class)
@Composable
private fun CameraWithPose(
    modifier: Modifier,
    onReady: () -> Unit,
    onError: (String) -> Unit,
    onResult: (Array<Lm?>, Float, List<Offset?>) -> Unit,
    onNoPerson: () -> Unit,
    overlay: @Composable () -> Unit,
) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FILL_CENTER; implementationMode = PreviewView.ImplementationMode.COMPATIBLE }
    }
    val executor = remember { Executors.newSingleThreadExecutor() }
    val detector: PoseDetector = remember {
        PoseDetection.getClient(PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build())
    }
    val mainExec = remember { ContextCompat.getMainExecutor(ctx) }

    DisposableEffect(lifecycleOwner) {
        var provider: ProcessCameraProvider? = null
        var closed = false
        val smoothed = arrayOfNulls<Offset>(33)
        var firstFrame = true
        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            if (closed) return@addListener
            try {
                val p = future.get()
                provider = p
                val rs = ResolutionSelector.Builder().setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY).build()
                val preview = Preview.Builder().setResolutionSelector(rs).build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val analysis = ImageAnalysis.Builder()
                    .setResolutionSelector(rs)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()
                analysis.setAnalyzer(executor) { proxy: ImageProxy ->
                    val media = proxy.image
                    if (media == null || closed) { proxy.close(); return@setAnalyzer }
                    val rot = proxy.imageInfo.rotationDegrees
                    val w = (if (rot == 90 || rot == 270) proxy.height else proxy.width).toFloat()
                    val h = (if (rot == 90 || rot == 270) proxy.width else proxy.height).toFloat()
                    detector.process(InputImage.fromMediaImage(media, rot))
                        .addOnSuccessListener(mainExec) { pose: Pose ->
                            if (closed) return@addOnSuccessListener
                            val all = pose.allPoseLandmarks
                            if (all.isEmpty()) { onNoPerson(); return@addOnSuccessListener }
                            val lm = arrayOfNulls<Lm>(33)
                            for (l in all) {
                                val t = l.landmarkType
                                if (t in 0..32) lm[t] = Lm((l.position.x / w).coerceIn(0f, 1f), (l.position.y / h).coerceIn(0f, 1f), l.inFrameLikelihood)
                            }
                            // Suavizado exponencial (animacion fluida) y espejo para camara frontal
                            val pts = ArrayList<Offset?>(33)
                            for (i in 0 until 33) {
                                val cur = lm[i]
                                if (cur == null || cur.v < 0.3f) { smoothed[i] = null; pts.add(null); continue }
                                val target = Offset(1f - cur.x, cur.y)
                                val prev = smoothed[i]
                                val s = if (prev == null) target else Offset(prev.x + (target.x - prev.x) * 0.55f, prev.y + (target.y - prev.y) * 0.55f)
                                smoothed[i] = s
                                pts.add(s)
                            }
                            onResult(lm, w / h, pts)
                        }
                        .addOnCompleteListener(mainExec) { proxy.close() }
                }
                p.unbindAll()
                p.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, analysis)
                if (firstFrame) { firstFrame = false; onReady() }
            } catch (e: Exception) {
                onError("No se pudo iniciar la cámara: ${e.message ?: "error desconocido"}")
            }
        }, mainExec)
        onDispose {
            closed = true
            try { provider?.unbindAll() } catch (_: Exception) { }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            try { detector.close() } catch (_: Exception) { }
            executor.shutdown()
        }
    }

    Box(modifier) {
        AndroidView({ previewView }, Modifier.fillMaxSize())
        overlay()
    }
}
