package com.voltbody.app.ui.screens.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voltbody.app.data.WorkoutDay
import com.voltbody.app.ui.AnimatedCheck
import com.voltbody.app.ui.EaseOutExpo
import com.voltbody.app.ui.SectionTitle
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.rememberPulse
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.util.rememberHaptics

/**
 * Detalle del día estilo "Full Body burn": cabecera grande con imagen/GIF y parallax, píldora de duración,
 * lista "Programa" con miniaturas y botón gigante "Empezar" fijo.
 */
@Composable
fun DayDetailDialog(
    day: WorkoutDay, setsByExercise: Map<String, Int>, estimatedMinutes: Int, onClose: () -> Unit, onStart: (startIndex: Int) -> Unit,
) {
    val haptics = rememberHaptics()
    val exercises = day.exercises
    val planned = remember(exercises) { exercises.sumOf { it.targetSets() } }
    val completed = exercises.sumOf { minOf(setsByExercise[it.id] ?: 0, it.targetSets()) }
    val pct = if (planned > 0) completed * 100 / planned else 0
    val allDone = exercises.isNotEmpty() && exercises.all { (setsByExercise[it.id] ?: 0) >= it.targetSets() }
    val firstPending = exercises.indexOfFirst { (setsByExercise[it.id] ?: 0) < it.targetSets() }.let { if (it < 0) 0 else it }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(800, easing = EaseOutExpo)) }
    val listState = rememberLazyListState()
    val pulse = rememberPulse(1f, 1.025f, 1000)

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Volt.bg)) {
            LazyColumn(Modifier.fillMaxSize(), state = listState, contentPadding = PaddingValues(bottom = 140.dp)) {
                // ── Cabecera: imagen/GIF grande con parallax ──
                item(key = "hero", contentType = "hero") {
                    val accentC = Volt.accent
                    val bgC = Volt.bg
                    Box(Modifier.fillMaxWidth().height(460.dp)) {
                        ExerciseImage(
                            exercises.firstOrNull()?.gifUrl, day.focus, maxPx = 900,
                            modifier = Modifier.fillMaxSize().graphicsLayer {
                                val s = 1.18f - 0.18f * appear.value
                                scaleX = s; scaleY = s
                                translationY = if (listState.firstVisibleItemIndex == 0) listState.firstVisibleItemScrollOffset * 0.5f else 0f
                            },
                        )
                        Box(
                            Modifier.fillMaxSize().drawBehind {
                                drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.50f), Color.Transparent, bgC), endY = size.height))
                                drawRect(Brush.linearGradient(listOf(accentC.copy(alpha = 0.30f), Color.Transparent), end = androidx.compose.ui.geometry.Offset(size.width, size.height)))
                            },
                        )
                        Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp)) {
                            Row(
                                Modifier.clip(CircleShape).background(Volt.gradient).padding(horizontal = 16.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Schedule, null, tint = Volt.onAccent, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("$estimatedMinutes min", color = Volt.onAccent, style = VoltType.title.copy(fontSize = 14.sp))
                            }
                            Spacer(Modifier.height(14.dp))
                            Text(day.day.uppercase(), color = Volt.accent, style = VoltType.label)
                            Text(
                                day.focus.ifBlank { "Entrenamiento" }, color = Volt.text,
                                style = VoltType.display.copy(fontSize = 48.sp, lineHeight = 50.sp), maxLines = 3,
                                modifier = Modifier.graphicsLayer { alpha = appear.value; translationY = (1f - appear.value) * 30.dp.toPx() },
                            )
                        }
                    }
                }

                item(key = "intro", contentType = "intro") {
                    Column(Modifier.padding(horizontal = 20.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        Text(
                            "${exercises.size} ejercicios listos. Técnica limpia, constancia y cero excusas.",
                            color = Volt.textDim, style = VoltType.body.copy(fontSize = 15.sp, lineHeight = 22.sp), modifier = Modifier.staggeredEntry(0),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.staggeredEntry(1)) {
                            DetailStat("Ejercicios", "${exercises.size}", Modifier.weight(1f))
                            DetailStat("Series", "$completed/$planned", Modifier.weight(1f))
                            DetailStat("Progreso", "$pct%", Modifier.weight(1f))
                        }
                        SectionTitle("Programa", Modifier.staggeredEntry(2))
                    }
                }

                items(exercises.size, key = { exercises[it].id.ifEmpty { "ex$it" } + "_$it" }, contentType = { "exercise" }) { i ->
                    val e = exercises[i]
                    val done = setsByExercise[e.id] ?: 0
                    val complete = done >= e.targetSets()
                    Row(
                        Modifier.padding(horizontal = 20.dp, vertical = 5.dp).then(if (i < 6) Modifier.staggeredEntry(3 + i) else Modifier)
                            .fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Volt.surface)
                            .pressScale { haptics.light(); onStart(i) }.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ExerciseImage(e.gifUrl, e.name, Modifier.size(76.dp).clip(RoundedCornerShape(22.dp)), maxPx = 220)
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(e.name, color = Volt.text, style = VoltType.title, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                "${e.sets} series × ${e.reps}" + if (e.muscleGroup.isNotBlank()) " · ${e.muscleGroup}" else "",
                                color = Volt.textDim, style = VoltType.body, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                            if (done > 0 && !complete) Text("$done/${e.targetSets()} series", color = Volt.accent, style = VoltType.label)
                        }
                        if (complete) AnimatedCheck(true, Modifier.size(32.dp))
                        else Box(Modifier.size(40.dp).clip(CircleShape).background(Volt.accent.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Filled.PlayArrow, null, tint = Volt.accent)
                        }
                    }
                }
            }

            // Botón atrás
            Box(
                Modifier.statusBarsPadding().padding(16.dp).size(48.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).pressScale(onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) }

            // Botón gigante fijo "Empezar"
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Volt.bg, Volt.bg)))
                    .navigationBarsPadding().padding(start = 20.dp, end = 20.dp, top = 28.dp, bottom = 16.dp),
            ) {
                val label = when { allDone -> "Repetir entreno"; completed > 0 -> "Continuar"; else -> "Empezar" }
                Row(
                    Modifier.fillMaxWidth().height(72.dp)
                        .graphicsLayer { if (completed == 0) { val s = pulse.value; scaleX = s; scaleY = s } }
                        .clip(CircleShape).background(Volt.gradient)
                        .pressScale { haptics.medium(); onStart(if (allDone) 0 else firstPending) },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Volt.onAccent, modifier = Modifier.size(30.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = Volt.onAccent, style = VoltType.h2.copy(fontSize = 22.sp))
                }
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(24.dp)).background(Volt.surface).padding(vertical = 16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Volt.accent, style = VoltType.numberM)
        Text(label.uppercase(), color = Volt.textDim, style = VoltType.label)
    }
}
