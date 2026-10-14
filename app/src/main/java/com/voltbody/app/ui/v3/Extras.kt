package com.voltbody.app.ui.v3

import android.content.Context
import android.content.Intent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.Haptics
import com.voltbody.app.util.ShoppingItem
import com.voltbody.app.util.buildShoppingList
import com.voltbody.app.util.mondayOf
import com.voltbody.app.util.shoppingListText
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private val ES = Locale("es", "ES")

// ─────────────────────────────── Lista de la compra ───────────────────────────────

/** Marcas de la lista: por semana y solo en este dispositivo (es una ayuda para ir al super, no un dato de la cuenta). */
private class ShoppingChecks(ctx: Context, week: LocalDate) {
    private val prefs = ctx.applicationContext.getSharedPreferences("voltbody_shopping", Context.MODE_PRIVATE)
    private val key = "done_$week"
    var done by mutableStateOf(prefs.getStringSet(key, emptySet())!!.toSet())
        private set

    fun toggle(id: String) {
        done = if (id in done) done - id else done + id
        prefs.edit().putStringSet(key, done).apply()
    }

    fun clear() { done = emptySet(); prefs.edit().remove(key).apply() }
}

/** Hoja "Lista de la compra": ingredientes de los 7 dias del menu, sumados y por pasillo, con check y compartir. */
@Composable
fun ShoppingSheet(repo: Repository) {
    val dietFlow = remember(repo) { repo.state.map { it.diet }.distinctUntilChanged() }
    val diet by dietFlow.collectAsStateWithLifecycle(initialValue = repo.state.value.diet)
    val ctx = LocalContext.current
    val week = remember { mondayOf(LocalDate.now()) }
    val checks = remember(week) { ShoppingChecks(ctx, week) }
    val aisles = remember(diet) { diet?.let { buildShoppingList(it) }.orEmpty() }
    val total = aisles.sumOf { it.items.size }
    val doneCount = aisles.sumOf { a -> a.items.count { it.key in checks.done } }
    val range = remember(week) {
        val f = DateTimeFormatter.ofPattern("d MMM", ES)
        "${week.format(f)} – ${week.plusDays(6).format(f)}".replace(".", "")
    }

    SheetScaffold("Lista de la compra", eyebrow = "Semana · $range") {
        if (aisles.isEmpty()) {
            Card(Modifier.fillMaxWidth()) {
                Text("Aún no hay menú", style = T.cardTitle, color = Volt.text)
                Text("Genera tu plan de comidas y aquí aparecerá todo lo que necesitas para la semana.", style = T.body, color = Volt.textDim)
            }
            return@SheetScaffold
        }

        // Progreso + acciones.
        Card(Modifier.fillMaxWidth().enter(1)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    ValueUnit("$doneCount", "de $total en el carro", style = T.hero)
                }
                MiniDonut(if (total > 0) doneCount / total.toFloat() else 0f, Modifier.size(48.dp), color = Volt.accent)
            }
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DarkButton("Compartir", {
                    val text = shoppingListText(aisles, checks.done)
                    val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, text)
                    runCatching { ctx.startActivity(Intent.createChooser(send, "Compartir lista").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }, Modifier.weight(1f), icon = Icons.Outlined.Share, height = 48.dp)
                if (doneCount > 0) DarkButton("Vaciar marcas", { Haptics.light(ctx); checks.clear() }, Modifier.weight(1f), height = 48.dp, tint = Volt.textDim)
            }
            if (diet?.week.isNullOrEmpty()) {
                Spacer(Modifier.height(10.dp))
                Text("Tu plan repite el mismo menú cada día: las cantidades son ×7.", style = T.caption, color = Volt.textDim)
            }
        }

        aisles.forEachIndexed { ai, aisle ->
            Text(
                "${aisle.emoji}  ${aisle.name.uppercase()}", style = T.label, color = Volt.textDim,
                modifier = Modifier.padding(start = 6.dp, top = 6.dp).enter(2 + ai),
            )
            Card(Modifier.fillMaxWidth().enter(2 + ai), padding = 4.dp) {
                aisle.items.forEach { item ->
                    ShoppingRow(item, item.key in checks.done) { Haptics.selection(ctx); checks.toggle(item.key) }
                }
            }
        }
    }
}

@Composable
private fun ShoppingRow(item: ShoppingItem, checked: Boolean, onToggle: () -> Unit) {
    val box by animateColorAsState(if (checked) Volt.accent else Color.Transparent, tween(150), label = "chk")
    Row(
        Modifier.fillMaxWidth().heightIn(min = 48.dp).clip(RoundedCornerShape(18.dp))
            .tap(0.98f, haptic = false, onClick = onToggle)
            .semantics(mergeDescendants = true) {
                role = Role.Checkbox
                stateDescription = if (checked) "En el carro" else "Pendiente"
            }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(24.dp).clip(CircleShape).background(box)
                .border(1.5.dp, if (checked) Volt.accent else Volt.textDim.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center,
        ) { if (checked) Icon(Icons.Filled.Check, null, tint = Color(0xFF0A0A0A), modifier = Modifier.size(16.dp)) }
        Spacer(Modifier.width(12.dp))
        Text(
            item.name, style = T.cardTitle, color = if (checked) Volt.textDim else Volt.text, maxLines = 2, overflow = TextOverflow.Ellipsis,
            textDecoration = if (checked) TextDecoration.LineThrough else null, modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(8.dp))
        Text(item.amountLabel(), style = T.caption, color = Volt.textDim, maxLines = 1)
    }
}

// ─────────────────────────────── Tendencias ───────────────────────────────

/** Serie diaria (null = sin dato) de los ultimos [days] dias, de mas antiguo a hoy. */
private fun <T> series(days: Int, today: LocalDate, pick: (String) -> T?): List<T?> =
    (days - 1 downTo 0).map { pick(today.minusDays(it.toLong()).toString()) }

/** Media movil de [window] dias ignorando huecos. */
private fun movingAverage(values: List<Float?>, window: Int = 7): List<Float?> = values.indices.map { i ->
    val w = values.subList((i - window + 1).coerceAtLeast(0), i + 1).filterNotNull()
    if (values[i] == null && w.isEmpty()) null else w.takeIf { it.isNotEmpty() }?.average()?.toFloat()
}

private fun List<Float?>.avg(): Float? = filterNotNull().takeIf { it.isNotEmpty() }?.average()?.toFloat()

/** Variacion entre la media de la segunda mitad y la de la primera (para la flecha de tendencia). */
private fun List<Float?>.halfDelta(): Float? {
    val h = size / 2
    val a = subList(0, h).avg() ?: return null
    val b = subList(h, size).avg() ?: return null
    return b - a
}

/** Hoja "Tendencias": pasos, sueno, HRV, FC en reposo y peso de los ultimos 7 o 30 dias (Health Connect + registros). */
@Composable
fun TrendsSheet(repo: Repository) {
    val st by repo.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val ctx = LocalContext.current
    var days by rememberSaveable { mutableIntStateOf(30) }
    val today = remember { LocalDate.now() }
    val byDate = remember(st.metrics) { st.metrics.associateBy { it.date } }
    val weights = remember(st.weightLogs) { st.weightLogs.associate { it.date to it.weight.toFloat() } }

    val steps = remember(byDate, days) { series(days, today) { byDate[it]?.steps?.toFloat() } }
    val sleep = remember(byDate, days) { series(days, today) { byDate[it]?.sleepHours?.toFloat() } }
    val hrv = remember(byDate, days) { series(days, today) { byDate[it]?.hrv?.toFloat() } }
    val rhr = remember(byDate, days) { series(days, today) { byDate[it]?.restingHr?.toFloat() } }
    val weight = remember(weights, days) { series(days, today) { weights[it] } }
    var selected by remember(days) { mutableIntStateOf(days - 1) }

    SheetScaffold("Tendencias", eyebrow = "Últimos $days días") {
        Row(Modifier.enter(1), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 30).forEach { d ->
                SmallPill("$d días", selected = days == d) { if (days != d) { Haptics.selection(ctx); days = d } }
            }
        }

        val hasHealth = listOf(steps, sleep, hrv, rhr).any { s -> s.any { it != null } }
        if (!hasHealth) {
            Card(Modifier.fillMaxWidth().enter(2)) {
                Text("Sin datos de salud todavía", style = T.cardTitle, color = Volt.text)
                Text("Conecta Health Connect (o Samsung Health a través de él) e importa el último mes para ver tu evolución.", style = T.body, color = Volt.textDim)
                Spacer(Modifier.height(12.dp))
                LimeButton("Conectar salud", { nav.replace(Route.Section("health")) }, height = 48.dp)
            }
        }

        // Pasos: barras por dia, toca una para ver el valor.
        if (steps.any { it != null }) {
            val values = steps.map { it ?: 0f }
            val sel = selected.coerceIn(0, values.lastIndex)
            Card(Modifier.fillMaxWidth().enter(2)) {
                CardHeader(Icons.AutoMirrored.Filled.DirectionsWalk, Volt.accent, "Pasos", chevron = false)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    ValueUnit(fmt(steps.avg()?.toInt() ?: 0), "de media al día", Modifier.weight(1f))
                    TrendBadge(steps.halfDelta(), higherIsBetter = true, unit = "")
                }
                Spacer(Modifier.height(12.dp))
                MockBars(values, sel, fmt(values[sel].toInt()), Modifier.fillMaxWidth().height(110.dp)) { selected = it }
                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(dayLabel(today.minusDays((days - 1).toLong())), style = T.caption, color = Volt.textDim)
                    if (sel != values.lastIndex) Text(dayLabel(today.minusDays((days - 1 - sel).toLong())), style = T.caption, color = Volt.accent)
                    Text("Hoy", style = T.caption, color = Volt.textDim)
                }
            }
        }

        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).enter(3), horizontalArrangement = Arrangement.spacedBy(K.gap)) {
            TrendTile("Sueño", sleep, "h", Volt.water, higherIsBetter = true, decimals = 1, modifier = Modifier.weight(1f).fillMaxHeight())
            TrendTile("HRV", hrv, "ms", Volt.accent, higherIsBetter = true, modifier = Modifier.weight(1f).fillMaxHeight())
        }
        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).enter(4), horizontalArrangement = Arrangement.spacedBy(K.gap)) {
            TrendTile("FC reposo", rhr, "lpm", Volt.danger, higherIsBetter = false, modifier = Modifier.weight(1f).fillMaxHeight())
            TrendTile("Peso", weight, "kg", Volt.orange, higherIsBetter = null, decimals = 1, smooth = true, modifier = Modifier.weight(1f).fillMaxHeight())
        }
        Text(
            "La línea gruesa es la media de 7 días (filtra el ruido de un mal día); la fina, el dato diario. Las flechas comparan la segunda mitad del periodo con la primera.",
            style = T.caption, color = Volt.textDim, modifier = Modifier.padding(horizontal = 6.dp).enter(5),
        )
    }
}

private fun dayLabel(d: LocalDate): String = d.format(DateTimeFormatter.ofPattern("d MMM", ES)).replace(".", "")

@Composable
private fun TrendTile(
    title: String, values: List<Float?>, unit: String, color: Color, higherIsBetter: Boolean?,
    modifier: Modifier, decimals: Int = 0, smooth: Boolean = false,
) {
    val avg = values.avg()
    val last = values.lastOrNull { it != null }
    Card(modifier) {
        Text(title, style = T.cardTitle, color = Volt.text)
        Spacer(Modifier.height(6.dp))
        val shown = if (smooth) last else avg
        ValueUnit(
            shown?.let { if (decimals == 0) fmt(it.toInt()) else String.format(ES, "%.${decimals}f", it) } ?: "—",
            if (shown == null) null else if (smooth) unit else "$unit · media",
            style = T.value,
        )
        Spacer(Modifier.height(4.dp))
        TrendBadge(values.halfDelta(), higherIsBetter, unit, decimals)
        Spacer(Modifier.weight(1f))
        Spacer(Modifier.height(10.dp))
        TrendLine(values, movingAverage(values), color, Modifier.fillMaxWidth().height(44.dp))
    }
}

/** Flecha + variacion. [higherIsBetter] = null: neutra (el color no juzga, p. ej. el peso depende del objetivo). */
@Composable
private fun TrendBadge(delta: Float?, higherIsBetter: Boolean?, unit: String, decimals: Int = 0) {
    if (delta == null) { Text("Sin datos suficientes", style = T.caption, color = Volt.textDim); return }
    val flat = kotlin.math.abs(delta) < (if (decimals == 0) 1f else 0.1f)
    val up = delta > 0
    val good = when { flat || higherIsBetter == null -> null; else -> up == higherIsBetter }
    val tint = when (good) { true -> Volt.accent; false -> Volt.orange; null -> Volt.textDim }
    val icon: ImageVector = when { flat -> Icons.AutoMirrored.Outlined.TrendingFlat; up -> Icons.AutoMirrored.Outlined.TrendingUp; else -> Icons.AutoMirrored.Outlined.TrendingDown }
    val txt = if (flat) "Estable" else (if (up) "+" else "") + (if (decimals == 0) fmt(delta.toInt()) else String.format(ES, "%.${decimals}f", delta)) + (if (unit.isNotEmpty()) " $unit" else "")
    Row(
        Modifier.clip(CircleShape).background(tint.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp)
            .semantics(mergeDescendants = true) { contentDescription = "Tendencia: $txt" },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = tint, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(4.dp))
        Text(txt, style = T.caption.copy(fontSize = 11.sp), color = tint, maxLines = 1)
    }
}

/**
 * Dato diario (fino y atenuado) + media movil (gruesa) que se dibujan al aparecer.
 * Ambas comparten escala; los dias sin dato se saltan uniendo los puntos existentes.
 */
@Composable
private fun TrendLine(raw: List<Float?>, values: List<Float?>, color: Color, modifier: Modifier, strokeWidth: Dp = 2.5.dp) {
    val p = remember { Animatable(0f) }
    LaunchedEffect(values) { p.snapTo(0f); p.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }
    val track = Volt.surface2
    Canvas(modifier) {
        val pts = values.mapIndexedNotNull { i, v -> v?.let { i to it } }
        val rawPts = raw.mapIndexedNotNull { i, v -> v?.let { i to it } }
        drawLine(track, Offset(0f, size.height - 1f), Offset(size.width, size.height - 1f), 1.dp.toPx())
        if (pts.size < 2) return@Canvas
        val all = pts.map { it.second } + rawPts.map { it.second }
        val mn = all.min(); val mx = all.max(); val r = (mx - mn).coerceAtLeast(0.0001f)
        val n = (values.size - 1).coerceAtLeast(1)
        fun pos(i: Int, v: Float) = Offset(i * size.width / n, size.height * 0.9f - ((v - mn) / r) * size.height * 0.8f)
        fun smoothPath(o: List<Offset>) = Path().apply {
            moveTo(o[0].x, o[0].y)
            for (i in 1 until o.size) { val m = (o[i - 1].x + o[i].x) / 2; cubicTo(m, o[i - 1].y, m, o[i].y, o[i].x, o[i].y) }
        }
        if (rawPts.size >= 2) {
            val rp = PathMeasure().apply { setPath(smoothPath(rawPts.map { (i, v) -> pos(i, v) }), false) }
            val rOut = Path(); rp.getSegment(0f, rp.length * p.value, rOut, true)
            drawPath(rOut, color.copy(alpha = 0.28f), style = Stroke(1.2.dp.toPx(), cap = StrokeCap.Round))
        }
        val o = pts.map { (i, v) -> pos(i, v) }
        val path = smoothPath(o)
        val pm = PathMeasure().apply { setPath(path, false) }
        val out = Path(); pm.getSegment(0f, pm.length * p.value, out, true)
        drawPath(out, color, style = Stroke(strokeWidth.toPx(), cap = StrokeCap.Round))
        if (p.value > 0.98f) drawCircle(color, 3.5.dp.toPx(), o.last())
    }
}
