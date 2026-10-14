package com.voltbody.app.ui.screens.workout

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voltbody.app.data.WorkoutDay
import com.voltbody.app.ui.SectionTitle
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.rememberHaptics

/**
 * Detalle del entreno del día (diseño 3 del mockup): imagen casi a pantalla completa, pill de duración,
 * descripción, lista "Programa" con miniaturas y botón lima "Empezar" fijo abajo.
 */
@Composable
fun DayDetailDialog(
    day: WorkoutDay, setsByExercise: Map<String, Int>, estimatedMinutes: Int, onClose: () -> Unit, onStart: (startIndex: Int) -> Unit,
) {
    val haptics = rememberHaptics()
    val exercises = day.exercises
    val planned = exercises.sumOf { it.targetSets() }
    val completed = exercises.sumOf { minOf(setsByExercise[it.id] ?: 0, it.targetSets()) }
    val pct = if (planned > 0) completed * 100 / planned else 0
    val allDone = exercises.isNotEmpty() && exercises.all { (setsByExercise[it.id] ?: 0) >= it.targetSets() }
    val firstPending = exercises.indexOfFirst { (setsByExercise[it.id] ?: 0) < it.targetSets() }.let { if (it < 0) 0 else it }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(700, easing = FastOutSlowInEasing)) }

    Dialog(onDismissRequest = onClose, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Volt.bg)) {
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // ── Cabecera: imagen casi a pantalla completa ──
                Box(Modifier.fillMaxWidth().height(400.dp)) {
                    ExerciseImage(exercises.firstOrNull()?.gifUrl, day.focus, Modifier.fillMaxSize().scale(1.15f - 0.15f * appear.value))
                    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.45f), Color.Transparent, Volt.bg))))
                    Column(Modifier.align(Alignment.BottomStart).padding(horizontal = 20.dp, vertical = 8.dp)) {
                        Row(
                            Modifier.clip(RoundedCornerShape(50)).background(Volt.accent).padding(horizontal = 14.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(Icons.Filled.Schedule, null, tint = Color.Black, modifier = Modifier.size(15.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("$estimatedMinutes min", color = Color.Black, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(day.day.uppercase(), color = Volt.accent, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                        Text(day.focus.ifBlank { "Entrenamiento" }, color = Volt.text, fontSize = 32.sp, fontWeight = FontWeight.Black, lineHeight = 36.sp)
                    }
                }

                Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        "${exercises.size} ejercicios listos para ejecutar. Enfócate en ${day.focus.ifBlank { "tu sesión" }}: técnica limpia, constancia y cero excusas.",
                        color = Volt.textDim, fontSize = 14.sp, lineHeight = 20.sp, modifier = Modifier.staggeredEntry(0),
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.staggeredEntry(1)) {
                        DetailStat("Ejercicios", "${exercises.size}", Modifier.weight(1f))
                        DetailStat("Series", "$completed/$planned", Modifier.weight(1f))
                        DetailStat("Progreso", "$pct%", Modifier.weight(1f))
                    }
                    SectionTitle("Programa", Modifier.staggeredEntry(2))
                    exercises.forEachIndexed { i, e ->
                        val done = setsByExercise[e.id] ?: 0
                        val complete = done >= e.targetSets()
                        Row(
                            Modifier.staggeredEntry(3 + i).fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(Volt.surface)
                                .border(1.dp, if (complete) Volt.accent.copy(alpha = 0.6f) else Volt.border, RoundedCornerShape(24.dp))
                                .pressScale { haptics.light(); onStart(i) }.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            ExerciseImage(e.gifUrl, e.name, Modifier.size(64.dp).clip(RoundedCornerShape(18.dp)))
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(e.name, color = Volt.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("${e.sets} series × ${e.reps}" + if (e.muscleGroup.isNotBlank()) " · ${e.muscleGroup}" else "", color = Volt.textDim, fontSize = 12.sp)
                                if (done > 0) Text(if (complete) "✅ Completado" else "$done/${e.targetSets()} series", color = Volt.accent, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                            }
                            Box(
                                Modifier.size(38.dp).clip(CircleShape).background(if (complete) Color.Transparent else Volt.accent.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(if (complete) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow, null, tint = Volt.accent)
                            }
                        }
                    }
                    Spacer(Modifier.height(120.dp))
                }
            }

            // Botón atrás
            Box(
                Modifier.statusBarsPadding().padding(16.dp).size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.55f)).pressScale(onClose),
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White) }

            // Botón fijo "Empezar"
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Transparent, Volt.bg, Volt.bg)))
                    .navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            ) {
                val label = when { allDone -> "Repetir entreno"; completed > 0 -> "Continuar"; else -> "Empezar" }
                val pulse by com.voltbody.app.ui.rememberPulse(1f, 1.03f, 900)
                Row(
                    Modifier.fillMaxWidth().height(60.dp).scale(if (completed == 0) pulse else 1f).clip(RoundedCornerShape(30.dp)).background(Volt.accent)
                        .pressScale { haptics.medium(); onStart(if (allDone) 0 else firstPending) },
                    verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(Icons.Filled.PlayArrow, null, tint = Color.Black)
                    Spacer(Modifier.width(8.dp))
                    Text(label, color = Color.Black, fontWeight = FontWeight.Black, fontSize = 18.sp)
                }
            }
        }
    }
}

@Composable
private fun DetailStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier.clip(RoundedCornerShape(20.dp)).background(Volt.surface).padding(vertical = 14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = Volt.accent, fontWeight = FontWeight.Black, fontSize = 20.sp)
        Text(label, color = Volt.textDim, fontSize = 11.sp)
    }
}
