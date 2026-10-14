package com.voltbody.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.EaseOutExpo
import com.voltbody.app.ui.gradientBorder
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.pressScaleIf
import com.voltbody.app.ui.rememberPulse
import com.voltbody.app.ui.screens.*
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltTheme
import com.voltbody.app.ui.theme.VoltType
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.abs
import kotlin.math.min

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Inicio", Icons.Filled.Home), Workout("Rutina", Icons.Filled.FitnessCenter), Diet("Dieta", Icons.Filled.Restaurant),
    Calendar("Calendario", Icons.Filled.CalendarMonth), Profile("Perfil", Icons.Filled.Person)
}

/** Orden visual en la barra: Rutina queda en el centro, destacada. */
private val NAV_ORDER = listOf(Tab.Home, Tab.Diet, Tab.Workout, Tab.Calendar, Tab.Profile)

/** Solo lo que necesita la raiz: evita recomponer toda la app con cada cambio de AppState. */
private data class Root(val auth: Boolean, val onboarded: Boolean, val hydrated: Boolean, val theme: String)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        if (Build.VERSION.SDK_INT >= 29) { window.isNavigationBarContrastEnforced = false; window.isStatusBarContrastEnforced = false }
        requestHighRefreshRate()
        val repo = (application as VoltBodyApp).repo
        setContent {
            val rootFlow = remember { repo.state.map { Root(it.isAuthenticated, it.isOnboarded, it.hydrated, it.theme) }.distinctUntilChanged() }
            val initial = remember { repo.state.value.let { Root(it.isAuthenticated, it.isOnboarded, it.hydrated, it.theme) } }
            val root by rootFlow.collectAsStateWithLifecycle(initialValue = initial)
            VoltTheme(root.theme) {
                Surface(Modifier.fillMaxSize(), color = Volt.bg) {
                    LaunchedEffect(root.auth) { if (root.auth) { repo.refreshAll(); repo.fixExerciseGifs() } }
                    val phase = when {
                        !root.hydrated -> 0
                        !root.auth -> 1
                        !root.onboarded -> 2
                        else -> 3
                    }
                    Box(Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = phase, modifier = Modifier.fillMaxSize(),
                            transitionSpec = { fadeIn(tween(280)) togetherWith fadeOut(tween(160)) },
                            label = "phase",
                        ) { p ->
                            when (p) {
                                0 -> Box(Modifier.fillMaxSize())
                                1 -> com.voltbody.app.ui.v3.LoginV3(repo)
                                2 -> OnboardingScreen(repo)
                                else -> com.voltbody.app.ui.v3.AppShell(repo)
                            }
                        }
                        ToastHost(repo)
                    }
                }
            }
        }
    }

    /** Fija el modo de pantalla con mayor frecuencia de refresco (120 Hz) para la resolucion actual. */
    private fun requestHighRefreshRate() {
        try {
            @Suppress("DEPRECATION")
            val d = if (Build.VERSION.SDK_INT >= 30) display else windowManager.defaultDisplay
            if (d == null) return
            val cur = d.mode
            val best = d.supportedModes
                .filter { it.physicalWidth == cur.physicalWidth && it.physicalHeight == cur.physicalHeight }
                .maxByOrNull { it.refreshRate } ?: return
            window.attributes = window.attributes.also {
                it.preferredDisplayModeId = best.modeId
                it.preferredRefreshRate = best.refreshRate
            }
        } catch (_: Throwable) {
        }
    }
}

// ───────────────────────── Toasts ─────────────────────────

@Composable
private fun BoxScope.ToastHost(repo: Repository) {
    var shown by remember { mutableStateOf<com.voltbody.app.data.Toast?>(null) }
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        repo.toasts.collectLatest { shown = it; visible = true; delay(3200); visible = false }
    }
    AnimatedVisibility(
        visible, modifier = Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(16.dp),
        enter = slideInVertically(spring(dampingRatio = 0.8f, stiffness = 800f)) { -it * 2 } + fadeIn(tween(120)) + scaleIn(tween(160, easing = EaseOutExpo), initialScale = 0.95f),
        exit = slideOutVertically(tween(120)) { -it * 2 } + fadeOut(tween(80)),
    ) {
        shown?.let { ToastPill(it) }
    }
}

@Composable
private fun ToastPill(t: com.voltbody.app.data.Toast) {
    val shape = RoundedCornerShape(28.dp)
    val info = t.type != "error" && t.type != "success"
    val brush = when (t.type) {
        "error" -> Brush.linearGradient(listOf(Volt.danger, Color(0xFFFF8A5B)))
        "success" -> Volt.gradient
        else -> Brush.linearGradient(listOf(Volt.surface2, Volt.surface2))
    }
    val fg = if (info) Volt.text else Volt.onAccent
    val icon = when (t.type) { "error" -> Icons.Filled.ErrorOutline; "success" -> Icons.Filled.CheckCircle; else -> Icons.Filled.Info }
    Row(
        Modifier.widthIn(max = 420.dp).clip(shape).background(brush).then(if (info) Modifier.gradientBorder(1.dp, shape) else Modifier)
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = if (info) Volt.accent else fg, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f, fill = false)) {
            Text(t.title, color = fg, style = VoltType.title, maxLines = 2)
            t.message?.let { Text(it, color = fg.copy(alpha = 0.78f), style = VoltType.body.copy(fontSize = 12.sp, lineHeight = 16.sp), maxLines = 3) }
        }
    }
}

// ───────────────────────── Shell + barra de navegacion ─────────────────────────

/** Contenido de cada pestana debe dejar ~110dp libres abajo (barra flotante). */
@Composable
fun MainShell(repo: Repository) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    BackHandler(enabled = tab != Tab.Home) { tab = Tab.Home }
    val px = with(LocalDensity.current) { 24.dp.roundToPx() }
    val c1 = Volt.accent
    val c2 = Volt.accent2
    Box(
        Modifier.fillMaxSize().drawBehind {
            drawRect(Brush.radialGradient(listOf(c1.copy(alpha = 0.10f), Color.Transparent), Offset(0f, 0f), size.maxDimension * 0.7f))
            drawRect(Brush.radialGradient(listOf(c2.copy(alpha = 0.07f), Color.Transparent), Offset(size.width, size.height), size.maxDimension * 0.7f))
        },
    ) {
        AnimatedContent(
            targetState = tab, modifier = Modifier.fillMaxSize().statusBarsPadding(),
                            transitionSpec = {
                                (fadeIn(tween(120)) + scaleIn(tween(160, easing = EaseOutExpo), initialScale = 0.98f)) togetherWith fadeOut(tween(80))
                            },
            label = "tab",
        ) { t ->
            when (t) {
                Tab.Home -> HomeScreen(repo, onNavigate = { tab = navTab(it) })
                Tab.Workout -> WorkoutScreen(repo)
                Tab.Diet -> DietScreen(repo)
                Tab.Calendar -> CalendarScreen(repo, onNavigate = { tab = navTab(it) })
                Tab.Profile -> ProfileScreen(repo)
            }
        }
        FloatingNav(tab, { tab = it }, Modifier.align(Alignment.BottomCenter))
    }
}

/** Pildora oscura flotante como el mockup: circulo de acento que se desliza bajo el icono activo; Rutina (centro) mas grande. */
@Composable
private fun FloatingNav(selected: Tab, onSelect: (Tab) -> Unit, modifier: Modifier = Modifier) {
    val sel = NAV_ORDER.indexOf(selected)
    val pos = animateFloatAsState(sel.toFloat(), spring(dampingRatio = 0.8f, stiffness = 800f), label = "navPos")
    val accent = Volt.accent
    val surface = Volt.surface
    val border = Volt.border
    Row(
        modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 10.dp).height(68.dp)
            .drawBehind {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(Color.Black.copy(alpha = 0.55f), Offset(0f, 4.dp.toPx()), size, r)
                drawRoundRect(surface, Offset.Zero, size, r)
                drawRoundRect(border, Offset.Zero, size, r, style = Stroke(1.dp.toPx()))
                val slot = size.width / NAV_ORDER.size
                val cx = slot * (pos.value + 0.5f)
                drawCircle(accent, 25.dp.toPx(), Offset(cx, size.height / 2))
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val ctx = androidx.compose.ui.platform.LocalContext.current
        NAV_ORDER.forEach { t ->
            val active = selected == t
            val big = t == Tab.Workout
            val tint = animateColorAsState(if (active) Volt.onAccent else Volt.textDim, tween(100), label = "navTint")
            Box(
                Modifier.weight(1f).fillMaxHeight().pressScaleIf(true, 0.88f) { if (!active) onSelect(t) },
                contentAlignment = Alignment.Center,
            ) { Icon(t.icon, t.label, tint = tint.value, modifier = Modifier.size(if (big) 28.dp else 24.dp)) }
        }
    }
}

private fun navTab(key: String): Tab = when (key) {
    "workout" -> Tab.Workout; "diet" -> Tab.Diet; "calendar" -> Tab.Calendar; "profile" -> Tab.Profile; else -> Tab.Home
}
