package com.voltbody.app.ui.v3

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.ui.LocalGlass
import com.voltbody.app.ui.rememberReduceMotion
import com.voltbody.app.ui.theme.LocalVolt
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltColors
import com.voltbody.app.util.Haptics
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.launch
import kotlin.math.max

/** Cierre animado de la hoja actual (la X de la cabecera lo usa en vez de cortar la hoja de golpe). */
val LocalSheetClose = staticCompositionLocalOf<(() -> Unit)?> { null }

/** Paleta dentro de las hojas: superficies translucidas para que las tarjetas sean paneles de vidrio. */
private fun glassPalette(base: VoltColors): VoltColors = base.copy(
    surface = Color.White.copy(alpha = 0.05f),
    surface2 = Color.White.copy(alpha = 0.08f),
    border = Color.White.copy(alpha = 0.10f),
)

private val SheetShape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)

/** Filo de luz superior + asa (comun a todas las hojas). */
@Composable
private fun SheetHandle() {
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier.fillMaxWidth().height(1.dp)
                .background(Brush.horizontalGradient(listOf(Color(0x00FFFFFF), Color(0x29FFFFFF), Color(0x00FFFFFF)))),
        )
        Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 8.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(width = 40.dp, height = 5.dp).background(Color(0x40FFFFFF), CircleShape))
        }
    }
}

/** Superficie de las hojas: casi opaca para que el texto tenga contraste sobre cualquier pantalla. */
private val SheetSurface = Color(0xFA111113)

/**
 * Hoja inferior como overlay dentro de la misma composicion (no ModalBottomSheet, para poder animarla
 * y arrastrarla con total control). Sin desenfoque en vivo: el blur re-renderizaba el fondo en cada
 * fotograma y ralentizaba la app; superficie oscura solida + telon, como pide DESIGN.md.
 *
 * - Entra con muelle; haptico al abrir y cerrar. Todas las animaciones se leen en fase draw.
 * - Se cierra arrastrando hacia abajo (desde la cabecera o desde el contenido cuando esta arriba del todo),
 *   tocando el telon, con atras o con la X; si el arrastre no llega al umbral vuelve a su sitio con muelle.
 * - Sube con el teclado y su contenido usa paneles de vidrio ([LocalGlass]).
 */
@Composable
fun GlassSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val reduce = rememberReduceMotion()
    val a = remember { Animatable(0f) }
    // Desplazamiento por arrastre (px hacia abajo). Estado simple: se lee en fase draw.
    var drag by remember { mutableFloatStateOf(0f) }
    var sheetH by remember { mutableIntStateOf(0) }
    var closing by remember { mutableStateOf(false) }
    val settleJob = remember { mutableStateOf<Job?>(null) }
    val maxH = (LocalConfiguration.current.screenHeightDp * 0.92f).dp

    LaunchedEffect(Unit) {
        Haptics.light(ctx)
        if (reduce) a.snapTo(1f) else a.animateTo(1f, spring(dampingRatio = 0.82f, stiffness = 380f))
    }

    fun close() {
        if (closing) return
        closing = true
        settleJob.value?.cancel()
        Haptics.selection(ctx)
        scope.launch {
            val from = drag
            launch { animate(from, from + sheetH * 0.5f, animationSpec = tween(200)) { v, _ -> drag = v } }
            a.animateTo(0f, tween(200))
            onDismiss()
        }
    }

    fun settle(velocity: Float) {
        if (closing) return
        val threshold = max(sheetH * 0.22f, with(density) { 96.dp.toPx() })
        if (drag > threshold || (velocity > 1600f && drag > with(density) { 12.dp.toPx() })) close()
        else settleJob.value = scope.launch { animate(drag, 0f, animationSpec = spring(0.78f, 520f)) { v, _ -> drag = v } }
    }

    // El contenido desplazable cede el arrastre a la hoja cuando ya esta arriba del todo.
    val nested = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y >= 0f || drag <= 0f) return Offset.Zero
                settleJob.value?.cancel()
                val used = max(available.y, -drag)
                drag += used
                return Offset(0f, used)
            }

            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source != NestedScrollSource.UserInput || available.y <= 0f || closing) return Offset.Zero
                settleJob.value?.cancel()
                drag += available.y
                return Offset(0f, available.y)
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                if (drag <= 0f) return Velocity.Zero
                settle(available.y)
                return available
            }
        }
    }
    val dragState = rememberDraggableState { d ->
        if (!closing) { settleJob.value?.cancel(); drag = (drag + d).coerceAtLeast(0f) }
    }
    val closeRef = remember { { close() } }

    // Atras predictivo: la hoja baja siguiendo el gesto; al soltar se cierra, al cancelar vuelve con muelle.
    PredictiveBackHandler { events ->
        try {
            events.collect { e -> settleJob.value?.cancel(); drag = e.progress * sheetH * 0.3f }
            close()
        } catch (e: CancellationException) {
            withContext(NonCancellable) { animate(drag, 0f, animationSpec = spring(0.8f, 600f)) { v, _ -> drag = v } }
            throw e
        }
    }

    val base = Volt
    val palette = remember(base) { glassPalette(base) }

    Box(Modifier.fillMaxSize()) {
        // Telon: oscurece (menos cuanto mas se arrastra) y capta el toque para cerrar.
        Box(
            Modifier.fillMaxSize()
                .graphicsLayer {
                    val pulled = if (sheetH > 0) (drag / sheetH).coerceIn(0f, 1f) else 0f
                    alpha = 0.62f * a.value * (1f - pulled)
                }
                .background(Color.Black)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { close() },
        )

        Column(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .imePadding()
                .onSizeChanged { sheetH = it.height }
                .graphicsLayer {
                    translationY = (1f - a.value) * 90.dp.toPx() + drag
                    val s = 0.975f + 0.025f * a.value
                    scaleX = s
                    scaleY = s
                    transformOrigin = TransformOrigin(0.5f, 1f)
                }
                .clip(SheetShape)
                .background(SheetSurface)
                .heightIn(max = maxH)
                .nestedScroll(nested)
                .draggable(dragState, Orientation.Vertical, onDragStopped = { v -> settle(v) }),
        ) {
            SheetHandle()
            CompositionLocalProvider(LocalVolt provides palette, LocalGlass provides true, LocalSheetClose provides closeRef) {
                content()
            }
        }
    }
}

/**
 * Estructura comun del contenido de una hoja: cabecera fija (antetitulo opcional, titulo y X)
 * y cuerpo desplazable con un filo que aparece al hacer scroll.
 */
@Composable
fun SheetScaffold(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    mixTitle: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val nav = LocalNav.current
    val close = LocalSheetClose.current ?: { nav.pop() }
    val scroll = rememberScrollState()
    Column(modifier.fillMaxWidth()) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = K.pad).padding(top = 2.dp, bottom = 12.dp).enter(0),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                if (eyebrow != null) {
                    Text(eyebrow.uppercase(), style = T.label, color = Volt.accent, maxLines = 1)
                    androidx.compose.foundation.layout.Spacer(Modifier.height(2.dp))
                }
                val style = T.screen.copy(fontSize = 28.sp, lineHeight = 32.sp)
                if (mixTitle) MixTitle(title, style = style, maxLines = 1)
                else FitText(title, style, Volt.text, maxLines = 1)
            }
            androidx.compose.foundation.layout.Spacer(Modifier.width(12.dp))
            IconCircle(Icons.Filled.Close, size = 40.dp, desc = "Cerrar", onClick = close)
        }
        // Filo bajo la cabecera: solo visible cuando el contenido ya se ha desplazado (lectura en draw).
        Box(
            Modifier.fillMaxWidth().height(1.dp)
                .graphicsLayer { alpha = (scroll.value / 24f).coerceIn(0f, 1f) }
                .background(Color.White.copy(alpha = 0.08f)),
        )
        Column(
            Modifier.weight(1f, fill = false).verticalScroll(scroll)
                .navigationBarsPadding().padding(horizontal = K.pad).padding(top = 10.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(K.gap),
            content = content,
        )
    }
}

/**
 * Hoja modal con el mismo acabado que [GlassSheet] para los sitios donde no hay [HazeState]
 * (selector de mes, cambio de comida con IA...). Usa el ModalBottomSheet de Material (arrastre incluido).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoltModalSheet(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    val ctx = LocalContext.current
    val base = Volt
    val palette = remember(base) { glassPalette(base) }
    LaunchedEffect(Unit) { Haptics.light(ctx) }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = SheetShape,
        containerColor = SheetSurface,
        contentColor = base.text,
        scrimColor = Color.Black.copy(alpha = 0.55f),
        tonalElevation = 0.dp,
        dragHandle = { SheetHandle() },
    ) {
        CompositionLocalProvider(LocalVolt provides palette, LocalGlass provides true) {
            content()
        }
    }
}
