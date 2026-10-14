package com.voltbody.app.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import com.voltbody.app.data.AppState
import com.voltbody.app.data.ProgressReport
import com.voltbody.app.data.Repository
import com.voltbody.app.health.healthManager
import com.voltbody.app.ui.*
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.ALL_ACHIEVEMENTS
import com.voltbody.app.util.toggleWeeklyGoal
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate

// ───────────────────────── Habitos semanales ─────────────────────────
@Composable
fun HabitsCard(repo: Repository, st: AppState, scope: CoroutineScope) {
    val goals = st.weeklyGoals
    val done = goals.count { it.done }
    val pct = if (goals.isEmpty()) 0f else done.toFloat() / goals.size
    val haptic = LocalHapticFeedback.current
    VoltCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("🎯 Hábitos semanales", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Se reinician cada lunes", color = Volt.textDim, fontSize = 11.sp)
            }
            DonutProgress(pct, Modifier.size(54.dp), stroke = 6.dp, color = Volt.accent) {
                Text("$done/${goals.size}", color = Volt.text, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(10.dp))
        goals.forEachIndexed { i, g ->
            val bg by animateColorAsState(if (g.done) Volt.accent.copy(.14f) else Volt.surface2, tween(300), label = "hbg")
            val sc by animateFloatAsState(if (g.done) 1f else 0.92f, spring(0.4f, 500f), label = "hck")
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).staggeredEntry(i).clip(RoundedCornerShape(16.dp)).background(bg)
                    .clickable { haptic.performHapticFeedback(HapticFeedbackType.LongPress); scope.launch { toggleWeeklyGoal(repo, g.id) } }.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(24.dp).scale(sc).clip(CircleShape).background(if (g.done) Volt.accent else Color.Transparent)
                    .border(2.dp, if (g.done) Volt.accent else Volt.textDim, CircleShape), contentAlignment = Alignment.Center) {
                    if (g.done) Icon(Icons.Filled.Check, null, tint = Color.Black, modifier = Modifier.size(16.dp))
                }
                Spacer(Modifier.width(12.dp))
                Text(g.label, color = if (g.done) Volt.text else Volt.textDim, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ───────────────────────── Logros ─────────────────────────
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AchievementsCard(st: AppState) {
    val unlocked = st.achievements.associateBy { it.id }
    VoltCard(Modifier.fillMaxWidth()) {
        Text("🏅 Logros", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("${unlocked.size} de ${ALL_ACHIEVEMENTS.size} desbloqueados", color = Volt.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ALL_ACHIEVEMENTS.forEach { a ->
                val on = unlocked.containsKey(a.id)
                Row(
                    Modifier.clip(RoundedCornerShape(50)).background(if (on) Volt.accent.copy(.16f) else Volt.surface2)
                        .border(1.dp, if (on) Volt.accent.copy(.5f) else Volt.border, RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(if (on) a.icon else "🔒", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(a.label, color = if (on) Volt.text else Volt.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
        val last = st.achievements.lastOrNull()
        if (last != null) {
            Spacer(Modifier.height(10.dp))
            Text("Último: ${last.icon} ${last.description}", color = Volt.textDim, fontSize = 12.sp)
        }
    }
}

// ───────────────────────── Registro de peso ─────────────────────────
@Composable
fun WeightCard(repo: Repository, st: AppState, scope: CoroutineScope) {
    val ctx = LocalContext.current
    var input by remember { mutableStateOf("") }
    val haptic = LocalHapticFeedback.current
    val logs = st.weightLogs
    val recent = logs.takeLast(30)
    VoltCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.MonitorWeight, null, tint = Volt.accent)
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text("⚖️ Registro de peso", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(
                    if (logs.isEmpty()) "Aún sin registros" else "${logs.size} registros · último ${"%.1f".format(logs.last().weight)} kg",
                    color = Volt.textDim, fontSize = 11.sp,
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        if (recent.size >= 2) {
            AreaChart(recent.map { it.weight.toFloat() }, listOf(recent.first().date.takeLast(5), recent.last().date.takeLast(5)))
            Spacer(Modifier.height(10.dp))
        }
        val value = input.replace(',', '.').toDoubleOrNull()
        val valid = value != null && value in 20.0..400.0
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                input, { input = it }, label = { Text("Peso de hoy (kg)") }, singleLine = true, modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(16.dp), colors = voltFieldColors(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            )
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    val kg = value!!
                    scope.launch {
                        repo.addWeight(LocalDate.now().toString(), kg)
                        repo.toast("success", "Peso registrado: $kg kg ⚖️")
                        if (com.voltbody.app.ui.screens.home.VoltPrefs.healthEnabled(ctx)) runCatching { ctx.healthManager.writeWeight(kg) }
                    }
                    input = ""
                },
                enabled = valid, shape = RoundedCornerShape(27.dp), modifier = Modifier.height(54.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Volt.accent, contentColor = Color.Black, disabledContainerColor = Volt.surface2, disabledContentColor = Volt.textDim),
            ) { Text("Guardar", fontWeight = FontWeight.Bold) }
        }
        if (input.isNotBlank() && !valid) Text("Introduce un peso entre 20 y 400 kg", color = Volt.danger, fontSize = 11.sp, modifier = Modifier.padding(top = 6.dp))
    }
}

// ───────────────────────── Fotos de progreso ─────────────────────────
@Composable
fun PhotosCard(repo: Repository, st: AppState, scope: CoroutineScope) {
    val ctx = LocalContext.current
    val haptic = LocalHapticFeedback.current
    var preview by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val photos = remember(st.progressPhotos) { st.progressPhotos.sortedByDescending { it.date } }

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

    VoltCard(Modifier.fillMaxWidth()) {
        Text("📸 Fotos de progreso", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("${photos.size} fotos", color = Volt.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        if (photos.isEmpty()) Text("Sube tu primera foto para comparar tu evolución.", color = Volt.textDim, fontSize = 12.sp)
        else LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(photos.take(30)) { p ->
                Box(Modifier.size(width = 96.dp, height = 128.dp).clip(RoundedCornerShape(16.dp)).clickable { preview = p.url }) {
                    DataImage(p.url, Modifier.fillMaxSize())
                    Text(p.date.take(10).takeLast(5), color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.align(Alignment.BottomStart).background(Color.Black.copy(.55f), RoundedCornerShape(topEnd = 10.dp)).padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) cameraLauncher.launch(null)
                    else camPerm.launch(Manifest.permission.CAMERA)
                },
                enabled = !busy, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Volt.accent, contentColor = Color.Black),
            ) { Icon(Icons.Filled.PhotoCamera, null); Spacer(Modifier.width(6.dp)); Text("Cámara", fontWeight = FontWeight.Bold) }
            OutlinedButton(
                onClick = { galleryLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) },
                enabled = !busy, modifier = Modifier.weight(1f).height(48.dp), shape = RoundedCornerShape(24.dp),
            ) { Icon(Icons.Filled.PhotoLibrary, null, tint = Volt.text); Spacer(Modifier.width(6.dp)); Text("Galería", color = Volt.text) }
        }
        if (busy) { Spacer(Modifier.height(8.dp)); LinearProgressIndicator(Modifier.fillMaxWidth(), color = Volt.accent, trackColor = Volt.surface2) }
    }
    preview?.let { url ->
        Dialog(onDismissRequest = { preview = null }) {
            Box(Modifier.clip(RoundedCornerShape(24.dp)).clickable { preview = null }) { DataImage(url, Modifier.fillMaxWidth().heightIn(max = 560.dp), androidx.compose.ui.layout.ContentScale.Fit) }
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

    VoltCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Insights, null, tint = Volt.accent)
            Spacer(Modifier.width(8.dp))
            Text("🤖 Informe IA de progreso", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(6.dp))
        Text("Analiza tus historiales de entrenamiento, nutrición y evolución para calcular tu porcentaje de avance y proyección estética.", color = Volt.textDim, fontSize = 12.sp)
        Spacer(Modifier.height(12.dp))
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
            Column(Modifier.padding(top = 12.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Analizando datos biométricos…", color = Volt.textDim, fontSize = 11.sp)
                    Text("$progress%", color = Volt.text, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(6.dp))
                val p by animateFloatAsState(progress / 100f, tween(350), label = "rp")
                Box(Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)).background(Volt.surface2)) {
                    Box(Modifier.fillMaxHeight().fillMaxWidth(p).clip(RoundedCornerShape(4.dp)).background(Volt.accent))
                }
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
                    ReportBlock { Text(r.summary, color = Volt.text, fontSize = 13.sp) }
                    ReportList("Qué puedes mejorar", r.improvements)
                    ReportList("Siguientes pasos", r.nextActions)
                }
            }
        }
    }
}

@Composable
private fun ReportBlock(content: @Composable ColumnScope.() -> Unit) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Volt.surface2).padding(14.dp), content = content)
}

@Composable
private fun ReportList(title: String, items: List<String>) {
    if (items.isEmpty()) return
    ReportBlock {
        Text(title.uppercase(), color = Volt.textDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(6.dp))
        items.forEach { Row(Modifier.padding(vertical = 2.dp)) { Text("• ", color = Volt.accent); Text(it, color = Volt.text, fontSize = 13.sp) } }
    }
}

// ───────────────────────── Timeline del dia ─────────────────────────
data class TimelineItem(val time: String, val title: String, val done: Boolean)

@Composable
fun TimelineCard(items: List<TimelineItem>) {
    VoltCard(Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Schedule, null, tint = Volt.accent)
            Spacer(Modifier.width(8.dp))
            Text("📅 Timeline del día", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
        Spacer(Modifier.height(10.dp))
        items.forEachIndexed { i, it ->
            val sc by animateFloatAsState(if (it.done) 1f else 0.85f, spring(0.4f, 500f), label = "tl")
            Row(
                Modifier.fillMaxWidth().padding(vertical = 4.dp).staggeredEntry(i).clip(RoundedCornerShape(16.dp)).background(Volt.surface2)
                    .then(if (it.done) Modifier.border(1.dp, Volt.accent.copy(.3f), RoundedCornerShape(16.dp)) else Modifier).padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(Modifier.size(12.dp).scale(sc).clip(CircleShape).background(if (it.done) Volt.accent else Volt.textDim.copy(.4f)))
                Spacer(Modifier.width(10.dp))
                Text(it.time, color = Volt.textDim, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(60.dp))
                Text(it.title, color = Volt.text, fontSize = 13.sp, fontWeight = FontWeight.Medium, modifier = Modifier.weight(1f))
                Text(if (it.done) "Hecho ✓" else "Pendiente", color = if (it.done) Color(0xFF34D399) else Volt.textDim, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ───────────────────────── Acciones rapidas ─────────────────────────
@Composable
fun QuickActionsCard(syncState: String, onLogSet: () -> Unit, onSwapMeal: () -> Unit, onProgress: () -> Unit) {
    VoltCard(Modifier.fillMaxWidth()) {
        Text("⚡ Acciones rápidas", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Un toque y listo", color = Volt.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile("Registrar serie 📝", Icons.Filled.FitnessCenter, onLogSet, Modifier.weight(1f))
            QuickTile("Cambiar comida 🔄", Icons.Filled.Restaurant, onSwapMeal, Modifier.weight(1f))
            QuickTile("Subir progreso 📸", Icons.Filled.PhotoCamera, onProgress, Modifier.weight(1f))
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Sync: " + when (syncState) {
                "syncing" -> "⟳ sincronizando..."; "synced" -> "✓ sincronizado"; "error" -> "⚠ error al sincronizar"; else -> "⏸ sin actividad"
            },
            color = Volt.textDim, fontSize = 11.sp,
        )
    }
}

@Composable
private fun QuickTile(label: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit, modifier: Modifier) {
    val haptic = LocalHapticFeedback.current
    Column(
        modifier.height(88.dp).clip(RoundedCornerShape(18.dp)).background(Volt.surface2)
            .pressScale { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onClick() }.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = Volt.accent)
        Spacer(Modifier.height(6.dp))
        Text(label, color = Volt.text, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center, lineHeight = 14.sp)
    }
}

// ───────────────────────── Modo mental ─────────────────────────
@Composable
fun MotivationCard(photo: String?, phrase: String) {
    Box(Modifier.fillMaxWidth().height(190.dp).clip(RoundedCornerShape(24.dp)).background(Brush.linearGradient(listOf(Volt.accent.copy(.25f), Color.Black)))) {
        DataImage(photo, Modifier.fillMaxSize())
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.85f)))))
        Column(Modifier.align(Alignment.BottomStart).padding(18.dp)) {
            Text("🧠 MODO MENTAL", color = Color.White.copy(.75f), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(phrase.ifBlank { "Hoy toca. Sin excusas." }, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, lineHeight = 22.sp)
        }
    }
}
