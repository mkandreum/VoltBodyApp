package com.voltbody.app.ui.v3

import androidx.activity.BackEventCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.animate
import androidx.compose.foundation.shape.RoundedCornerShape
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.Haptics

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Inicio", Icons.Outlined.Home),
    Activity("Actividad", Icons.Outlined.BarChart),
    Train("Entrenar", Icons.Filled.FitnessCenter),
    Food("Nutrición", Icons.Outlined.Restaurant),
    Profile("Perfil", Icons.Outlined.Person),
}

/** Pantallas secundarias (se apilan encima de las pestanas). */
sealed interface Route {
    /** Detalle de un dia de la rutina (0 = lunes). */
    data class Day(val weekday: Int) : Route
    /** Reproductor de entreno: ejercicios del dia [weekday] o de la rutina personal si [custom]. */
    data class Session(val weekday: Int, val start: Int = 0, val custom: Boolean = false) : Route
    /** Pantalla de seccion (perfil, progreso, nutricion extra...). */
    data class Section(val id: String) : Route
    data class Pose(val exercise: String) : Route
}

class Navigator(private val stack: SnapshotStateList<Route>, private val setTab: (Tab) -> Unit) {
    val top: Route? get() = stack.lastOrNull()
    val depth: Int get() = stack.size
    fun push(r: Route) { stack.add(r) }
    fun pop() { if (stack.isNotEmpty()) stack.removeAt(stack.lastIndex) }
    fun replace(r: Route) { pop(); push(r) }
    fun tab(t: Tab) { stack.clear(); setTab(t) }
}

val LocalNav = staticCompositionLocalOf<Navigator> { error("Navigator no disponible") }

@Composable
fun AppShell(repo: Repository) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    val stack = remember { mutableStateListOf<Route>() }
    val nav = remember { Navigator(stack, { tab = it }) }
    val ctx = LocalContext.current

    BackHandler(enabled = stack.isNotEmpty() || tab != Tab.Home) {
        if (stack.isNotEmpty()) nav.pop() else tab = Tab.Home
    }

    // Atras predictivo (Android 14+) en las paginas apiladas: la pagina se encoge y se redondea siguiendo
    // el dedo, dejando ver la pestana de debajo; al soltar se cierra y si se cancela vuelve con muelle.
    // Las hojas de seccion tienen su propio gesto (GlassSheet). El progreso se lee solo en fase draw.
    var backProgress by remember { mutableFloatStateOf(0f) }
    var backFromLeft by remember { mutableStateOf(true) }
    var backRoute by remember { mutableStateOf<Route?>(null) }
    val topIsPage = stack.isNotEmpty() && stack.last() !is Route.Section
    PredictiveBackHandler(enabled = topIsPage) { events ->
        try {
            events.collect { e ->
                if (backRoute == null) backRoute = stack.lastOrNull()
                backProgress = e.progress
                backFromLeft = e.swipeEdge == BackEventCompat.EDGE_LEFT
            }
            nav.pop()
        } catch (e: CancellationException) {
            withContext(NonCancellable) { animate(backProgress, 0f, animationSpec = spring(0.8f, 600f)) { v, _ -> backProgress = v } }
            backRoute = null
            throw e
        }
    }
    // Tras cerrar con el gesto, se mantiene la pagina encogida mientras sale y luego se limpia.
    LaunchedEffect(stack.size) {
        if (backRoute != null && backRoute !in stack) { delay(260); backRoute = null; backProgress = 0f }
    }

    CompositionLocalProvider(LocalNav provides nav) {
        Box(Modifier.fillMaxSize().background(Volt.bg)) {
            // Todo lo que queda detras de la hoja se captura como origen del cristal esmerilado.
            Box(Modifier.fillMaxSize()) {
                // Pestanas: cambio rapido (fade + leve escala), sin deslizar toda la pantalla.
                AnimatedContent(
                    targetState = tab, modifier = Modifier.fillMaxSize(),
                    transitionSpec = { (fadeIn(tween(160)) + scaleIn(tween(220), initialScale = 0.985f)) togetherWith fadeOut(tween(90)) },
                    label = "tab",
                ) { t ->
                    when (t) {
                        Tab.Home -> HomeV3(repo)
                        Tab.Activity -> ActivityV3(repo)
                        Tab.Train -> TrainV3(repo)
                        Tab.Food -> NutritionV3(repo)
                        Tab.Profile -> ProfileV3(repo)
                    }
                }

                // Pila de pantallas secundarias: entran deslizando desde la derecha.
                // Paginas (detalle de dia, sesion, pose) a pantalla completa; las secciones van en hoja inferior.
                val pages = stack.filter { it !is Route.Section }
                val depth = pages.size
                AnimatedContent(
                    targetState = pages.lastOrNull() to depth, modifier = Modifier.fillMaxSize(),
                    transitionSpec = {
                        val forward = targetState.second >= initialState.second
                        if (forward) (slideInHorizontally(spring(0.9f, 700f)) { it / 3 } + fadeIn(tween(160))) togetherWith fadeOut(tween(120))
                        // Cerrada con el gesto predictivo: la pagina ya esta encogida, solo se desvanece.
                        else if (backRoute != null) fadeIn(tween(120)) togetherWith fadeOut(tween(180))
                        else fadeIn(tween(140)) togetherWith (slideOutHorizontally(spring(0.9f, 700f)) { it / 3 } + fadeOut(tween(160)))
                    },
                    label = "stack",
                ) { (route, _) ->
                    if (route != null) Box(
                        Modifier.fillMaxSize()
                            .graphicsLayer {
                                val p = if (route == backRoute) backProgress else 0f
                                if (p > 0f) {
                                    val s = 1f - 0.1f * p
                                    scaleX = s; scaleY = s
                                    translationX = (if (backFromLeft) 1f else -1f) * p * 24.dp.toPx()
                                    shape = RoundedCornerShape((32f * p).dp)
                                    clip = true
                                }
                            }
                            .background(Volt.bg),
                    ) { RouteHost(repo, route) }
                    else Box(Modifier.fillMaxSize())
                }

                // Barra flotante: se oculta (desliza abajo) dentro de pantallas secundarias.
                androidx.compose.animation.AnimatedVisibility(
                    visible = pages.isEmpty(),
                    modifier = Modifier.align(Alignment.BottomCenter),
                    enter = slideInVertically(spring(0.85f, 700f)) { it } + fadeIn(),
                    exit = slideOutVertically(tween(160)) { it } + fadeOut(tween(120)),
                ) {
                    FloatingNav(tab) { t -> if (t != tab) { Haptics.selection(ctx); tab = t } }
                }
            }

            // Hoja inferior de cristal esmerilado (desenfoca de verdad lo que hay detras).
            val section = stack.lastOrNull() as? Route.Section
            if (section != null) {
                key(section.id) {
                    GlassSheet(onDismiss = { nav.pop() }) { SectionHost(repo, section.id) }
                }
            }
        }
    }
}

@Composable
private fun RouteHost(repo: Repository, route: Route) {
    when (route) {
        is Route.Day -> DayDetailV3(repo, route.weekday)
        is Route.Session -> SessionV3(repo, route)
        is Route.Section -> SectionHost(repo, route.id)
        is Route.Pose -> {
            val nav = LocalNav.current
            com.voltbody.app.ui.PoseCoachScreen(route.exercise) { nav.pop() }
        }
    }
}

/** Pildora oscura del mockup: circulo lima bajo el icono activo, que se desliza con spring. */
@Composable
internal fun FloatingNav(selected: Tab, onSelect: (Tab) -> Unit) {
    val tabs = Tab.entries
    val sel = tabs.indexOf(selected)
    val pos = animateFloatAsState(sel.toFloat(), spring(dampingRatio = 0.78f, stiffness = 800f), label = "navPos")
    val accent = Volt.accent
    // Translucida: deja intuir el contenido que pasa por debajo; el filo un poco mas claro mantiene la forma.
    val surface = Color(0xB31A1A1C) // ~70 % opaca
    val border = Color(0x24FFFFFF)
    Row(
        Modifier.navigationBarsPadding().padding(horizontal = 24.dp).padding(bottom = 12.dp).fillMaxWidth().height(66.dp)
            .drawBehind {
                val r = CornerRadius(size.height / 2)
                drawRoundRect(surface, cornerRadius = r)
                drawRoundRect(border, cornerRadius = r, style = Stroke(1.dp.toPx()))
                val slot = size.width / tabs.size
                val c = Offset(slot * (pos.value + 0.5f), size.height / 2)
                drawCircle(androidx.compose.ui.graphics.Brush.radialGradient(listOf(accent.copy(alpha = 0.35f), Color.Transparent), c, 44.dp.toPx()), 44.dp.toPx(), c)
                drawCircle(accent, 25.dp.toPx(), c)
            },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        tabs.forEach { t ->
            val active = t == selected
            val tint by animateColorAsState(if (active) Color(0xFF0A0A0A) else Color(0xFF8A8A8A), tween(140), label = "tint")
            val bounce = remember { androidx.compose.animation.core.Animatable(1f) }
            LaunchedEffect(active) {
                if (active) { bounce.snapTo(0.7f); bounce.animateTo(1f, spring(0.35f, 600f)) }
            }
            Box(Modifier.weight(1f).fillMaxHeight().tap(0.86f, haptic = false) { onSelect(t) }, contentAlignment = Alignment.Center) {
                Icon(t.icon, t.label, tint = tint, modifier = Modifier.size(if (t == Tab.Train) 26.dp else 24.dp).graphicsLayer { scaleX = bounce.value; scaleY = bounce.value })
            }
        }
    }
}

