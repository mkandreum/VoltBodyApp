package com.voltbody.app.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.voltbody.app.data.Achievement
import com.voltbody.app.data.ProgressPhoto
import com.voltbody.app.data.ProgressReport
import com.voltbody.app.data.Repository
import com.voltbody.app.data.WeeklyGoal
import com.voltbody.app.data.WeightLog
import com.voltbody.app.health.healthManager
import com.voltbody.app.ui.AnimatedCheck
import com.voltbody.app.ui.BentoCard
import com.voltbody.app.ui.GlowRing
import com.voltbody.app.ui.GradientText
import com.voltbody.app.ui.SkeletonBox
import com.voltbody.app.ui.StatPill
import com.voltbody.app.ui.gradientBorder
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.PrimaryButton
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.ui.voltFieldColors
import com.voltbody.app.util.ALL_ACHIEVEMENTS
import com.voltbody.app.util.toggleWeeklyGoal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

// ───────────────────────── Habitos semanales ─────────────────────────
@Composable
fun HabitsCard(repo: Repository, goals: List<WeeklyGoal>, scope: CoroutineScope) {
    val done = goals.count { it.done }
    val pct = if (goals.isEmpty()) 0f else done.toFloat() / goals.size
    val haptic = LocalHapticFeedback.current
    BentoCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                HmTileHeader(Icons.Filled.Flag, Volt.accent, "Hábitos")
                Spacer(Modifier.height(8.dp))
                Text("Tu semana, en marcha", color = Volt.text, style = VoltType.h2)
                Text("Se reinician cada lunes", color = Volt.textDim, style = VoltType.body)
            }
            GlowRing(pct, Modifier.size(76.dp), stroke = 6.dp) {
                Text("$done/${goals.size}", color = Volt.text, style = VoltType.numberS.copy(fontSize = 15.sp))
            }
        }
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            goals.forEach { g ->
                androidx.compose.runtime.key(g.id) {
                    HabitRow(g) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); scope.launch { toggleWeeklyGoal(repo, g.id) } }
                }
            }
        }
    }
}

@Composable
private fun HabitRow(g: WeeklyGoal, onToggle: () -> Unit) {
    val a = animateFloatAsState(if (g.done) 1f else 0f, tween(300), label = "habit")
    val tint = Volt.accent
    val base = Volt.surface2
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(20.dp))
            .drawBehind { drawRect(base); drawRect(tint.copy(alpha = 0.14f * a.value)) }
            .pressScale(onToggle).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(28.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.fillMaxSize().border(2.dp, Volt.textDim.copy(alpha = 0.55f), CircleShape))
            AnimatedCheck(g.done, Modifier.size(28.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(g.label, color = if (g.done) Volt.text else Volt.textDim, style = VoltType.body.copy(fontWeight = FontWeight.SemiBold))
    }
}

// ───────────────────────── Logros ─────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AchievementsCard(achievements: List<Achievement>) {
    val unlocked = remember(achievements) { achievements.associateBy { it.id } }
    BentoCard(Modifier.fillMaxWidth()) {
        HmTileHeader(Icons.Filled.EmojiEvents, Volt.accent, "Logros")
        Spacer(Modifier.height(10.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            GradientText("${unlocked.size}", style = VoltType.numberL)
            Text(" de ${ALL_ACHIEVEMENTS.size} desbloqueados", color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(bottom = 6.dp))
        }
        Spacer(Modifier.height(10.dp))
        HmGradientBar(if (ALL_ACHIEVEMENTS.isEmpty()) 0f else unlocked.size.toFloat() / ALL_ACHIEVEMENTS.size, height = 6.dp)
        Spacer(Modifier.height(14.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ALL_ACHIEVEMENTS.forEach { a ->
                val on = unlocked.containsKey(a.id)
                Row(
                    Modifier.clip(CircleShape)
                        .background(if (on) Volt.accent.copy(alpha = 0.14f) else Volt.surface2)
                        .let { if (on) it.gradientBorder(1.dp, CircleShape) else it.border(1.dp, Volt.border, CircleShape) }
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (on) a.icon else "🔒", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(a.label, color = if (on) Volt.text else Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold))
                }
            }
        }
        val last = achievements.lastOrNull()
        if (last != null) {
            Spacer(Modifier.height(12.dp))
            Text("Último: ${last.icon} ${last.description}", color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp))
        }
    }
}

// ───────────────────────── Registro de peso ─────────────────────────
@Composable
fun WeightCard(repo: Repository, weightLogs: List<WeightLog>, scope: CoroutineScope) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val recent = remember(weightLogs) { weightLogs.takeLast(30) }
    val values = remember(recent) { recent.map { it.weight.toFloat() } }
    BentoCard(Modifier.fillMaxWidth()) {
        HmTileHeader(Icons.Filled.MonitorWeight, Volt.accent, "Registro de peso")
        Spacer(Modifier.height(8.dp))
        Text(
            if (weightLogs.isEmpty()) "Aún sin registros" else "${weightLogs.size} registros · último ${"%.1f".format(weightLogs.last().weight)} kg",
            color = Volt.textDim, style = VoltType.body,
        )
        Spacer(Modifier.height(12.dp))
        if (recent.size >= 2) {
            AreaChart(values, listOf(recent.first().date.takeLast(5), recent.last().date.takeLast(5)))
            Spacer(Modifier.height(12.dp))
        }
        val value = input.replace(',', '.').toDoubleOrNull()
        val valid = value != null && value in 20.0..400.0
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                input, { input = it }, label = { Text("Peso de hoy (kg)") }, singleLine = true, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(20.dp), colors = voltFieldColors(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            HmGradientPill("Guardar", {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                val kg = value!!
                scope.launch {
                    repo.addWeight(LocalDate.now().toString(), kg)
                    repo.toast("success", "Peso registrado: $kg kg ⚖️")
                    if (VoltPrefs.healthEnabled(ctx)) runCatching { ctx.healthManager.writeWeight(kg) }
                }
                input = ""
            }, enabled = valid, height = 56.dp)
        }
        if (input.isNotBlank() && !valid) Text("Introduce un peso entre 20 y 400 kg", color = Volt.danger, style = VoltType.body.copy(fontSize = 12.sp), modifier = Modifier.padding(top = 6.dp))
    }
}

// ───────────────────────── Fotos de progreso ─────────────────────────
@Composable
fun PhotosCard(repo: Repository, photos: List<ProgressPhoto>, scope: CoroutineScope) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var preview by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val shown = remember(photos) { photos.take(30) }

    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp: Bitmap? ->
        if (bmp != null) {
            busy = true
            scope.launch {
                repo.addPhoto(Instant.now().toString(), bitmapToDataUrl(bmp))
                repo.toast("success", "Foto de progreso guardada 📸")
                busy = false
            }
        }
    }
    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        if (ok) cameraLauncher.launch(null) else repo.toast("error", "Permiso de cámara denegado", "Usa la galería o concede el permiso en Ajustes.")
    }
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                val url = uriToDataUrl(ctx, uri)
                if (url != null) { repo.addPhoto(Instant.now().toString(), url); repo.toast("success", "Foto de progreso guardada 📸") }
                else repo.toast("error", "No se pudo leer la imagen")
                busy = false
            }
        }
    }

    BentoCard(Modifier.fillMaxWidth()) {
        HmTileHeader(Icons.Filled.PhotoCamera, Volt.accent, "Foto de progreso")
        Spacer(Modifier.height(8.dp))
        Text(if (photos.isEmpty()) "Tu evolución empieza con una foto" else "${photos.size} fotos · toca para ampliar", color = Volt.textDim, style = VoltType.body)
        Spacer(Modifier.height(14.dp))
        if (shown.isEmpty()) {
            Box(
                Modifier.fillMaxWidth().height(96.dp).clip(RoundedCornerShape(24.dp)).background(Volt.surface2),
                contentAlignment = Alignment.Center,
            ) { Text("Sube tu primera foto para comparar tu evolución.", color = Volt.textDim, style = VoltType.body, textAlign = TextAlign.Center, modifier = Modifier.padding(16.dp)) }
        } else LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shown, contentType = { "photo" }) { p ->
                Box(Modifier.size(width = 104.dp, height = 140.dp).clip(RoundedCornerShape(22.dp)).background(Volt.surface2).clickable { preview = p.url }) {
                    DataImage(p.url, Modifier.fillMaxSize(), maxPx = 320)
                    Text(
                        p.date.take(10).takeLast(5), color = Color.White, style = VoltType.label,
                        modifier = Modifier.align(Alignment.BottomStart).background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(topEnd = 12.dp)).padding(horizontal = 8.dp, vertical = 3.dp),
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            HmGradientPill("Cámara", {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraLauncher.launch(null)
                else camPerm.launch(Manifest.permission.CAMERA)
            }, Modifier.weight(1f), icon = Icons.Filled.PhotoCamera, enabled = !busy)
            HmGhostPill("Galería", { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }, Modifier.weight(1f), icon = Icons.Filled.PhotoLibrary, enabled = !busy)
        }
        if (busy) { Spacer(Modifier.height(10.dp)); SkeletonBox(Modifier.fillMaxWidth().height(8.dp), CircleShape) }
    }
    preview?.let { url ->
        Dialog(onDismissRequest = { preview = null }) {
            Box(Modifier.clip(RoundedCornerShape(28.dp)).clickable { preview = null }) { DataImage(url, Modifier.fillMaxWidth().heightIn(max = 560.dp), ContentScale.Fit) }
        }
    }
}

// ───────────────────────── Informe IA ─────────────────────────
@Composable
fun ReportCard(repo: Repository) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(false) }
    var progress by remember { mutableIntStateOf(0) }
    var report by remember { mutableStateOf<ProgressReport?>(null) }
    val haptic = LocalHapticFeedback.current

    LaunchedEffect(loading) {
        if (loading) {
            progress = 0
            var cur = 0.0
            while (progress < 85) { delay(400); cur = minOf(85.0, cur + Math.random() * 6 + 2); progress = cur.toInt() }
        } else if (progress > 0) { progress = 100; delay(600); progress = 0 }
    }

    BentoCard(Modifier.fillMaxWidth(), gradient = false) {
        HmTileHeader(Icons.Filled.Insights, Volt.accent, "Informe IA")
        Spacer(Modifier.height(8.dp))
        Text("Tu progreso, analizado", color = Volt.text, style = VoltType.h2)
        Spacer(Modifier.height(4.dp))
        Text("Analiza tus historiales de entrenamiento, nutrición y evolución para calcular tu porcentaje de avance y proyección estética.", color = Volt.textDim, style = VoltType.body)
        Spacer(Modifier.height(14.dp))
        PrimaryButton(if (loading) "Generando informe..." else "Generar informe con IA", {
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            loading = true
            scope.launch {
                runCatching { repo.progressReport() }
                    .onSuccess { report = it; repo.toast("success", "Informe generado", "Revisa tu estado actual y los siguientes pasos recomendados.") }
                    .onFailure { repo.toast("error", "No se pudo generar el informe", it.message ?: "Inténtalo de nuevo en unos segundos.") }
                loading = false
            }
        }, loading = loading)
        AnimatedVisibility(loading, enter = fadeIn() + expandVertically(), exit = shrinkVertically()) {
            Column(Modifier.padding(top = 14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Analizando datos biométricos…", color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp))
                    Text("$progress%", color = Volt.text, style = VoltType.label)
                }
                HmGradientBar(progress / 100f, height = 8.dp)
                SkeletonBox(Modifier.fillMaxWidth().height(56.dp), RoundedCornerShape(20.dp))
                SkeletonBox(Modifier.fillMaxWidth().height(84.dp), RoundedCornerShape(20.dp))
            }
        }
        AnimatedVisibility(report != null, enter = fadeIn() + expandVertically()) {
            report?.let { r ->
                Column(Modifier.padding(top = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPill("Score total", "${r.overallScore}%", Modifier.weight(1f))
                        StatPill("Progreso", "${r.progressPercent}%", Modifier.weight(1f))
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPill("Consistencia", "${r.consistencyPercent}%", Modifier.weight(1f))
                        StatPill("Te falta", "${r.weeksToVisibleChange} sem", Modifier.weight(1f))
                    }
                    if (r.nutritionPercent > 0 || r.trainingExecutionPercent > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        StatPill("Nutrición", "${r.nutritionPercent}%", Modifier.weight(1f), Volt.orange)
                        StatPill("Ejecución", "${r.trainingExecutionPercent}%", Modifier.weight(1f), Volt.water)
                    }
                    ReportBlock { Text(r.summary, color = Volt.text, style = VoltType.body) }
                    ReportList("Qué puedes mejorar", r.improvements)
                    ReportList("Siguientes pasos", r.nextActions)
                }
            }
        }
    }
}

@Composable
private fun ReportBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Volt.surface2).padding(16.dp), content = content)
}

@Composable
private fun ReportList(title: String, items: List<String>) {
    if (items.isEmpty()) return
    ReportBlock {
        Text(title.uppercase(), color = Volt.textDim, style = VoltType.label)
        Spacer(Modifier.height(8.dp))
        items.forEach { Row(Modifier.padding(vertical = 2.dp)) { Text("• ", color = Volt.accent, style = VoltType.body); Text(it, color = Volt.text, style = VoltType.body) } }
    }
}

// ───────────────────────── Timeline del dia ─────────────────────────
data class TimelineItem(val time: String, val title: String, val done: Boolean)

@Composable
fun TimelineCard(items: List<TimelineItem>) {
    BentoCard(Modifier.fillMaxWidth()) {
        HmTileHeader(Icons.Filled.Schedule, Volt.accent, "Timeline del día")
        Spacer(Modifier.height(12.dp))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items.forEach { it ->
                Row(
                    Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(RoundedCornerShape(20.dp)).background(Volt.surface2)
                        .let { m -> if (it.done) m.gradientBorder(1.dp, RoundedCornerShape(20.dp)) else m }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.size(26.dp), contentAlignment = Alignment.Center) {
                        Box(Modifier.fillMaxSize().border(2.dp, Volt.textDim.copy(alpha = 0.45f), CircleShape))
                        AnimatedCheck(it.done, Modifier.size(26.dp))
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(it.time, color = Volt.textDim, style = VoltType.label, modifier = Modifier.width(64.dp), maxLines = 1)
                    Text(it.title, color = Volt.text, style = VoltType.body.copy(fontWeight = FontWeight.SemiBold), modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

// ───────────────────────── Acciones rapidas ─────────────────────────
@Composable
fun QuickActionsCard(syncState: String, onLogSet: () -> Unit, onSwapMeal: () -> Unit, onProgress: () -> Unit) {
    BentoCard(Modifier.fillMaxWidth()) {
        Text("Acciones rápidas", color = Volt.text, style = VoltType.h2)
        Text("Un toque y listo", color = Volt.textDim, style = VoltType.body)
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile("Registrar serie", Icons.Filled.FitnessCenter, onLogSet, Modifier.weight(1f))
            QuickTile("Cambiar comida", Icons.Filled.Restaurant, onSwapMeal, Modifier.weight(1f))
            QuickTile("Subir progreso", Icons.Filled.PhotoCamera, onProgress, Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Text(
            "Sync: " + when (syncState) {
                "syncing" -> "⟳ sincronizando..."; "synced" -> "✓ sincronizado"; "error" -> "⚠ error al sincronizar"; else -> "⏸ sin actividad"
            },
            color = Volt.textDim, style = VoltType.body.copy(fontSize = 12.sp),
        )
    }
}

@Composable
private fun QuickTile(label: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier) {
    val haptic = LocalHapticFeedback.current
    val grad = Volt.gradient
    Column(
        modifier.heightIn(min = 104.dp).clip(RoundedCornerShape(24.dp)).background(Volt.surface2)
            .pressScale { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }.padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(40.dp).clip(CircleShape).drawBehind { drawRect(grad) }, contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Volt.onAccent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.height(8.dp))
        Text(label, color = Volt.text, style = VoltType.body.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp), textAlign = TextAlign.Center)
    }
}

// ───────────────────────── Modo mental ─────────────────────────
@Composable
fun MotivationCard(photo: String?, phrase: String) {
    val shape = RoundedCornerShape(36.dp)
    Box(
        Modifier.fillMaxWidth().height(200.dp).clip(shape)
            .background(Brush.linearGradient(listOf(Volt.accent.copy(alpha = 0.30f), Volt.accent2.copy(alpha = 0.12f), Color.Black)))
            .gradientBorder(1.dp, shape),
    ) {
        DataImage(photo, Modifier.fillMaxSize(), maxPx = 1024)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.85f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(22.dp)) {
            Text("MODO MENTAL", color = Color.White.copy(alpha = 0.75f), style = VoltType.label)
            Spacer(Modifier.height(6.dp))
            Text(phrase.ifBlank { "Hoy toca. Sin excusas." }, color = Color.White, style = VoltType.h2)
        }
    }
}
