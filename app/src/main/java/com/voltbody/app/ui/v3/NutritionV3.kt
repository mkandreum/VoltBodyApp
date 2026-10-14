package com.voltbody.app.ui.v3

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.DailyMetric
import com.voltbody.app.data.Meal
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.screens.diet.MealSwapSheet
import com.voltbody.app.ui.screens.workout.ConfettiBurst
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.Haptics
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val WATER_GOAL_ML = 2500
private val PROTEIN = Color(0xFFFF6B8B)
private val CARBS = Color(0xFFFFB547)
private val FAT = Color(0xFF8B7CFF)

/** Emoji del plato segun sus ingredientes (y si no, segun la hora). */
fun foodEmoji(meal: Meal): String {
    val t = Normalizer.normalize((meal.name + " " + meal.description).lowercase(), Normalizer.Form.NFD).replace(Regex("\\p{M}+"), "")
    val map = listOf(
        listOf("tortita", "pancake", "crepe") to "🥞", listOf("huevo", "tortilla", "revuelto") to "🍳", listOf("avena", "porridge", "cereal", "granola") to "🥣",
        listOf("batido", "smoothie", "shake") to "🥤", listOf("yogur", "kefir", "skyr") to "🥛", listOf("tostada", "pan ", "sandwich", "bocadillo") to "🥪",
        listOf("wrap", "burrito", "fajita") to "🌯", listOf("pizza") to "🍕", listOf("hamburguesa", "burger") to "🍔",
        listOf("salmon", "atun", "merluza", "pescado", "bacalao", "dorada", "sardina") to "🐟", listOf("gamba", "langostino", "marisco") to "🍤",
        listOf("pollo", "pavo") to "🍗", listOf("ternera", "carne", "solomillo", "lomo", "filete") to "🥩",
        listOf("pasta", "espagueti", "macarron") to "🍝", listOf("arroz", "risotto", "poke") to "🍚", listOf("sopa", "crema", "caldo", "guiso", "lenteja", "garbanzo") to "🍲",
        listOf("ensalada", "quinoa", "bowl") to "🥗", listOf("boniato", "patata") to "🍠", listOf("aguacate") to "🥑",
        listOf("frutos secos", "nueces", "almendra", "cacahuete") to "🥜", listOf("platano", "banana") to "🍌", listOf("frutos rojos", "arandano", "fresa") to "🫐",
        listOf("manzana", "fruta") to "🍎", listOf("queso", "requeson") to "🧀", listOf("verdura", "brocoli", "espinaca") to "🥦",
    )
    map.firstOrNull { (keys, _) -> keys.any { t.contains(it) } }?.let { return it.second }
    val h = meal.time.take(2).toIntOrNull() ?: 12
    return when { h < 11 -> "🥣"; h < 13 -> "🍎"; h < 16 -> "🍽️"; h < 19 -> "🥜"; else -> "🌙" }
}

private fun mealMoment(time: String): String {
    val h = time.take(2).toIntOrNull() ?: 12
    return when { h < 11 -> "Desayuno"; h < 13 -> "Media mañana"; h < 17 -> "Comida"; h < 20 -> "Merienda"; else -> "Cena" }
}

@Composable
fun NutritionV3(repo: Repository) {
    val st by repo.state.collectAsStateWithLifecycle()
    val nav = LocalNav.current
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val todayDate = remember { LocalDate.now() }
    val today = remember { todayDate.toString() }
    // Dia que se esta viendo: hoy por defecto; se puede mirar el menu de dias pasados y futuros.
    var selected by rememberSaveable(stateSaver = LocalDateSaver) { mutableStateOf(todayDate) }
    val selKey = selected.toString()
    val isToday = selected == todayDate
    val isFuture = selected.isAfter(todayDate)
    val dateText = remember(selected) { selected.format(DateTimeFormatter.ofPattern("EEEE d 'de' MMMM", ES)).replaceFirstChar { it.uppercase() } }
    val weekdayText = remember(selected) { selected.format(DateTimeFormatter.ofPattern("EEEE", ES)) }
    val diet = st.diet
    val eaten = st.mealEaten[selKey].orEmpty()
    val water = st.metrics.firstOrNull { it.date == today }?.waterMl ?: 0
    var confetti by remember { mutableIntStateOf(0) }

    var swapMeal by remember { mutableStateOf<Meal?>(null) }
    var swapAlt by remember { mutableStateOf<Meal?>(null) }
    var swapLoading by remember { mutableStateOf(false) }
    var swapApplying by remember { mutableStateOf(false) }

    fun loadAlternative(meal: Meal) {
        scope.launch {
            swapLoading = true; swapAlt = null
            try {
                val alt = repo.alternativeMeal(meal, repo.state.value.diet?.mealsFor(selected).orEmpty())
                swapAlt = alt.copy(id = alt.id.ifBlank { meal.id }, time = alt.time.ifBlank { meal.time })
            } catch (e: Exception) {
                swapMeal = null
                repo.toast("error", "No se pudo cambiar la comida", e.message ?: "Prueba de nuevo en un momento.")
            } finally { swapLoading = false }
        }
    }

    fun applySwap(meal: Meal, newMeal: Meal) {
        if (swapApplying) return
        val day = selected
        scope.launch {
            swapApplying = true
            try {
                // Solo cambia el menu de ese dia de la semana (o el menu base si el plan no es semanal).
                repo.saveProfile(withPlan = true) { s -> s.copy(diet = s.diet?.replaceMeal(day, meal.id, newMeal)) }
                val key = day.toString()
                if (newMeal.id != meal.id && meal.id in repo.state.value.mealEaten[key].orEmpty()) {
                    repo.setMealEaten(key, meal.id, false); repo.setMealEaten(key, newMeal.id, true)
                }
                Haptics.success(ctx)
                repo.toastWithAction("success", "Comida cambiada ${foodEmoji(newMeal)}", newMeal.name, "Deshacer") {
                    scope.launch { repo.saveProfile(withPlan = true) { s -> s.copy(diet = s.diet?.replaceMeal(day, newMeal.id, meal)) } }
                }
                swapMeal = null; swapAlt = null
            } catch (e: Exception) {
                repo.toast("error", "No se pudo cambiar la comida", e.message)
            } finally { swapApplying = false }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Screen {
            Row(Modifier.fillMaxWidth().enter(0), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(dateText, style = T.hello, color = Volt.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    MixTitle("Come bien, rinde más", maxLines = 1)
                }
                IconCircle(Icons.Outlined.ShoppingCart, desc = "Lista de la compra") { nav.push(Route.Section("shopping")) }
                Spacer(Modifier.width(8.dp))
                IconCircle(Icons.Outlined.Tune, desc = "Plan de comidas") { nav.push(Route.Section("diet")) }
            }

            DayStrip(todayDate, selected, Modifier.enter(1)) { if (it != selected) { selected = it; Haptics.selection(ctx) } }

            if (diet == null || (diet.meals.isEmpty() && diet.week.isEmpty())) {
                Card(Modifier.fillMaxWidth().enter(1), padding = 22.dp) {
                    Text("🥗", fontSize = 44.sp)
                    Spacer(Modifier.height(10.dp))
                    Text("Tu plan de comidas te espera", style = T.display, color = Volt.text)
                    Text("Lo creamos con IA según tu objetivo y tus gustos.", style = T.body, color = Volt.textDim)
                    Spacer(Modifier.height(16.dp))
                    LimeButton("Crear mi plan", { nav.push(Route.Section("diet")) }, height = 50.dp)
                }
                return@Screen
            }

            val meals = remember(diet, selected) { diet.mealsFor(selected).sortedBy { it.time } }
            val done = meals.filter { it.id in eaten }
            // En dias futuros se muestra lo planificado (vista previa); en hoy y pasados, lo comido.
            val counted = if (isFuture) meals else done
            val kcal = counted.sumOf { it.calories }
            val left = (diet.dailyCalories - kcal).coerceAtLeast(0)
            val p = counted.sumOf { it.protein }; val c = counted.sumOf { it.carbs }; val f = counted.sumOf { it.fat }
            val now = remember { LocalTime.now().toString().take(5) }
            val nextId = if (!isToday) null else meals.firstOrNull { it.id !in eaten && it.time >= now }?.id ?: meals.firstOrNull { it.id !in eaten }?.id

            // ── Heroe: kcal + mensaje con actitud + macros con emoji
            Card(Modifier.fillMaxWidth().enter(1), padding = 18.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BigDonut(if (isFuture) 0f else if (diet.dailyCalories > 0) kcal / diet.dailyCalories.toFloat() else 0f, Modifier.size(118.dp), stroke = 12.dp) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔥", fontSize = 18.sp)
                            AnimatedContent(kcal, transitionSpec = { fadeIn(tween(160)) togetherWith fadeOut(tween(90)) }, label = "kcal") { k ->
                                Text(fmt(k), style = T.value, color = Volt.text)
                            }
                            Text("kcal", style = T.caption, color = Volt.textDim)
                        }
                    }
                    Spacer(Modifier.width(16.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            when {
                                isFuture -> "Así comerás el $weekdayText 👀"
                                !isToday -> pastCheer(done.size, meals.size, weekdayText)
                                else -> cheer(done.size, meals.size, left)
                            },
                            style = T.display.copy(fontSize = 15.sp, lineHeight = 20.sp), color = Volt.text, maxLines = 3,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            when {
                                isFuture -> "${fmt(kcal)} kcal planificadas en ${meals.size} comidas"
                                left > 0 -> (if (isToday) "Te quedan " else "Faltaron ") + "${fmt(left)} kcal de ${fmt(diet.dailyCalories)}"
                                else -> "Objetivo de ${fmt(diet.dailyCalories)} kcal cumplido"
                            },
                            style = T.caption, color = Volt.textDim,
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    MacroChip("🥩", "Proteína", p, if (isFuture) p else diet.macros.protein, PROTEIN, Modifier.weight(1f))
                    MacroChip("🍚", "Carbos", c, if (isFuture) c else diet.macros.carbs, CARBS, Modifier.weight(1f))
                    MacroChip("🥑", "Grasas", f, if (isFuture) f else diet.macros.fat, FAT, Modifier.weight(1f))
                }
            }

            // ── Agua (un toque = +250 ml): solo se registra en el dia de hoy.
            if (isToday) Row(
                Modifier.fillMaxWidth().enter(2).clip(RoundedCornerShape(22.dp)).background(Volt.surface)
                    .tap(0.98f, haptic = false) {
                        Haptics.medium(ctx)
                        scope.launch { repo.saveMetrics(today, DailyMetric(today, waterMl = water + 250)) }
                    }.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("💧", fontSize = 22.sp)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text("Agua · ${fmt(water)} / ${fmt(WATER_GOAL_ML)} ml", style = T.cardTitle, color = Volt.text)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        val glasses = WATER_GOAL_ML / 250
                        repeat(glasses) { i ->
                            val full = water >= (i + 1) * 250
                            Box(Modifier.weight(1f).height(8.dp).clip(RoundedCornerShape(4.dp)).background(if (full) Volt.water else Volt.surface2))
                        }
                    }
                }
                Spacer(Modifier.width(12.dp))
                Text("+250", style = T.cardTitle, color = Volt.water)
            }

            // Plan antiguo (un solo menu): invita a regenerarlo para tener un menu distinto cada dia.
            if (diet.week.isEmpty()) {
                Row(
                    Modifier.fillMaxWidth().enter(3).clip(RoundedCornerShape(22.dp)).background(Volt.surface)
                        .tap(0.98f) { nav.push(Route.Section("diet")) }.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("🗓️", fontSize = 22.sp)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Menú igual todos los días", style = T.cardTitle, color = Volt.text)
                        Text("Regenera tu plan para tener un menú distinto cada día de la semana.", style = T.caption, color = Volt.textDim)
                    }
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Volt.textDim, modifier = Modifier.size(20.dp))
                }
            }

            SectionLabel(
                when {
                    isToday -> "Tu día · ${done.size}/${meals.size} comidas"
                    isFuture -> "Menú del ${dateText.replaceFirstChar { it.lowercase() }}"
                    else -> "${dateText} · ${done.size}/${meals.size}"
                },
                Modifier.enter(3),
            )
            // key(selected): al cambiar de dia la lista vuelve a entrar escalonada.
            key(selected) { meals.forEachIndexed { i, m ->
                MealTimelineRow(
                    m, m.id in eaten, m.id == nextId, last = i == meals.lastIndex, modifier = Modifier.enter(4 + i),
                    canToggle = !isFuture,
                    onToggle = {
                        val nowEaten = m.id !in eaten
                        if (nowEaten) {
                            val allDone = done.size + 1 == meals.size
                            if (allDone) { Haptics.success(ctx); confetti++; repo.toast("success", "¡Día perfecto! 🎉", "Has cumplido todas tus comidas.") }
                            else Haptics.medium(ctx)
                        } else Haptics.light(ctx)
                        scope.launch { repo.setMealEaten(selKey, m.id, nowEaten) }
                    },
                    onSwap = { swapMeal = m; loadAlternative(m) },
                )
            } }
        }
        ConfettiBurst(confetti, Modifier.fillMaxSize(), big = true)
    }

    swapMeal?.let { meal ->
        MealSwapSheet(
            old = meal, alt = swapAlt, loading = swapLoading, applying = swapApplying,
            onApply = { swapAlt?.let { applySwap(meal, it) } }, onRetry = { loadAlternative(meal) },
            onDismiss = { if (!swapApplying) { swapMeal = null; swapAlt = null } },
        )
    }
}

private fun cheer(done: Int, total: Int, left: Int): String = when {
    total == 0 -> "Hoy toca comer bien"
    done == 0 -> "Empieza fuerte: tu primera comida te espera 💪"
    done == total -> "Día perfecto. Así se construye un cuerpo 🏆"
    done * 2 >= total -> "¡Vas genial! Ya queda menos 🔥"
    left < 400 -> "Casi lo tienes, remata el día 🎯"
    else -> "Buen arranque, sigue así ⚡"
}

private fun pastCheer(done: Int, total: Int, weekday: String): String = when {
    total == 0 -> "Sin menú ese día"
    done == total -> "El $weekday lo clavaste 🏆"
    done == 0 -> "El $weekday no marcaste comidas"
    else -> "El $weekday cumpliste $done de $total"
}

private val ES = Locale("es", "ES")

/** Guarda la fecha seleccionada al rotar o volver a la pestaña. */
private val LocalDateSaver = androidx.compose.runtime.saveable.Saver<LocalDate, Long>({ it.toEpochDay() }, { LocalDate.ofEpochDay(it) })

@Composable
private fun MacroChip(emoji: String, label: String, value: Int, target: Int, color: Color, modifier: Modifier) {
    val frac = if (target > 0) (value / target.toFloat()).coerceIn(0f, 1f) else 0f
    val anim by animateFloatAsState(frac, tween(700), label = "macro")
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Volt.surface2).padding(horizontal = 10.dp, vertical = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, fontSize = 15.sp)
            Spacer(Modifier.width(5.dp))
            Text(label, style = T.caption, color = Volt.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text("$value", style = T.value.copy(fontSize = 16.sp), color = Volt.text)
            Text(" /${target}g", style = T.caption, color = Volt.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(6.dp))
        Box(Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)).background(Volt.bg)) {
            Box(Modifier.fillMaxHeight().fillMaxWidth(anim.coerceAtLeast(0.001f)).clip(RoundedCornerShape(3.dp)).background(color))
        }
    }
}

/** Fila de linea de tiempo: hora + punto, emoji grande, nombre, macros, check y cambio IA. La siguiente comida va destacada. */
@Composable
private fun MealTimelineRow(m: Meal, eaten: Boolean, next: Boolean, last: Boolean, modifier: Modifier, canToggle: Boolean = true, onToggle: () -> Unit, onSwap: () -> Unit) {
    val checkBg by animateColorAsState(if (eaten) Volt.accent else Color.Transparent, tween(180), label = "chk")
    val pop by animateFloatAsState(if (eaten) 1f else 0f, spring(0.45f, 600f), label = "pop")
    Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
        // Linea de tiempo
        Column(Modifier.width(44.dp).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(m.time, style = T.caption, color = if (next) Volt.accent else Volt.textDim)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.size(10.dp).clip(CircleShape).background(if (eaten || next) Volt.accent else Volt.surface2))
            if (!last) Box(Modifier.width(2.dp).weight(1f).background(Volt.surface2))
        }
        Spacer(Modifier.width(6.dp))
        Row(
            Modifier.weight(1f).padding(bottom = 10.dp).clip(RoundedCornerShape(22.dp)).background(Volt.surface)
                .then(if (next) Modifier.border(1.5.dp, Volt.accent, RoundedCornerShape(22.dp)) else Modifier)
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.size(54.dp).clip(RoundedCornerShape(16.dp)).background(Volt.surface2), contentAlignment = Alignment.Center) {
                Text(foodEmoji(m), fontSize = 28.sp, modifier = Modifier.graphicsLayer { alpha = if (eaten) 0.45f else 1f; val s = 1f + 0.12f * pop; scaleX = s; scaleY = s })
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(mealMoment(m.time).uppercase(), style = T.label, color = if (next) Volt.accent else Volt.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (next) {
                        Spacer(Modifier.width(6.dp))
                        Text("AHORA", style = T.label, color = Volt.onAccent, modifier = Modifier.clip(RoundedCornerShape(6.dp)).background(Volt.accent).padding(horizontal = 5.dp, vertical = 1.dp))
                    }
                }
                FitText(m.name, T.cardTitle.copy(fontSize = 14.sp), if (eaten) Volt.textDim else Volt.text, maxLines = 2)
                Text("${m.calories} kcal · P${m.protein} C${m.carbs} G${m.fat}", style = T.caption, color = Volt.textDim, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Spacer(Modifier.width(6.dp))
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(0.dp)) {
                // Area tactil de 48dp (minimo de accesibilidad) con el circulo visual de 36dp dentro.
                if (canToggle) Box(
                    Modifier.size(48.dp).clip(CircleShape).tap(0.85f, haptic = false, onClick = onToggle)
                        .semantics { role = Role.Checkbox; stateDescription = if (eaten) "Comida hecha" else "Pendiente" },
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        Modifier.size(36.dp).graphicsLayer { val s = 1f + 0.18f * pop * (1f - pop) * 4f; scaleX = s; scaleY = s }
                            .clip(CircleShape).background(checkBg).border(1.5.dp, if (eaten) Volt.accent else Volt.textDim.copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { if (eaten) Icon(Icons.Filled.Check, null, tint = Volt.onAccent, modifier = Modifier.size(20.dp)) }
                }
                Box(Modifier.size(48.dp).clip(CircleShape).tap(0.8f, onClick = onSwap), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.AutoAwesome, "Cambiar con IA", tint = Volt.accent, modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}
