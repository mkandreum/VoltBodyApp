package com.voltbody.app.ui.screens.workout

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Picture
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.net.Uri
import android.provider.MediaStore
import android.text.TextPaint
import android.text.TextUtils
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.voltbody.app.ui.MeshBackground
import com.voltbody.app.ui.PrimaryButton
import com.voltbody.app.ui.SkeletonBox
import com.voltbody.app.ui.pressScale
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.rememberHaptics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Datos de la tarjeta final (WorkoutSummaryData de la web + extras de la sesión). */
data class WorkoutSummaryData(
    val focus: String,
    val exerciseCount: Int,
    val completedSets: Int,
    val plannedSets: Int,
    val streak: Int,
    val level: Int,
    val totalXP: Int,
    val xpGained: Int,
    val userName: String? = null,
    val exercises: List<Triple<String, Int, String>> = emptyList(), // nombre, series, reps
    val durationMin: Int? = null,
    val volumeKg: Int? = null,
    val avgBpm: Int? = null,
    val maxBpm: Int? = null,
)

private const val CARD_W = 380f
private const val SCALE = 3f

/**
 * Renderiza la tarjeta compartible a un Bitmap (equivale a html2canvas sobre WorkoutSummaryCard).
 * Se dibuja con android.graphics para no depender de una vista adjunta a la ventana.
 */
fun renderSummaryBitmap(data: WorkoutSummaryData, accent: Int, accent2: Int = 0xFF2DD4BF.toInt()): Bitmap {
    val neon = accent
    val orange = 0xFFF97316.toInt()
    val completionPct = if (data.plannedSets > 0) Math.round(data.completedSets * 100f / data.plannedSets) else 100
    val today = LocalDate.now().format(DateTimeFormatter.ofPattern("d 'de' MMMM, yyyy", Locale("es", "ES")))

    fun paint(color: Int, size: Float, bold: Boolean = false, mono: Boolean = false, spacing: Float = 0f, align: Paint.Align = Paint.Align.LEFT) =
        TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = color; textSize = size; letterSpacing = spacing; textAlign = align
            typeface = when { mono -> Typeface.create(Typeface.MONOSPACE, if (bold) Typeface.BOLD else Typeface.NORMAL); bold -> Typeface.DEFAULT_BOLD; else -> Typeface.DEFAULT }
        }
    fun fill(color: Int) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.FILL }
    fun stroke(color: Int, w: Float = 1f) = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color; style = Paint.Style.STROKE; strokeWidth = w }
    fun ell(p: TextPaint, s: String, maxW: Float) = TextUtils.ellipsize(s, p, maxW, TextUtils.TruncateAt.END).toString()
    fun alpha(color: Int, a: Float) = (color and 0x00FFFFFF) or ((a * 255).toInt().coerceIn(0, 255) shl 24)

    // 1) Contenido grabado en un Picture para conocer la altura final
    val picture = Picture()
    val pc = picture.beginRecording(CARD_W.toInt(), 2400)
    val padX = 24f
    val innerW = CARD_W - padX * 2
    var y = 32f

    // Cabecera de marca
    pc.drawText("⚡ VOLTBODY", padX, y + 14f, paint(neon, 16f, bold = true, spacing = 0.12f))
    pc.drawText(today, CARD_W - padX, y + 13f, paint(0xFF9CA3AF.toInt(), 11f, mono = true, align = Paint.Align.RIGHT))
    y += 20f + 20f

    // Panel trofeo + titulo
    val panelH = if (data.userName.isNullOrBlank()) 80f else 96f
    val panel = RectF(padX, y, CARD_W - padX, y + panelH)
    pc.drawRoundRect(panel, 20f, 20f, fill(0x0AFFFFFF))
    pc.drawRoundRect(panel, 20f, 20f, stroke(alpha(neon, 0.25f)))
    pc.drawText("🏆", panel.left + 16f, panel.top + 52f, paint(0xFFFFFFFF.toInt(), 36f))
    val tx = panel.left + 16f + 52f
    val tw = panel.right - tx - 12f
    pc.drawText("SESIÓN COMPLETADA", tx, panel.top + 28f, paint(neon, 10f, bold = true, spacing = 0.22f))
    val titleP = paint(0xFFFFFFFF.toInt(), 21f, bold = true)
    pc.drawText(ell(titleP, data.focus, tw), tx, panel.top + 54f, titleP)
    if (!data.userName.isNullOrBlank()) {
        val nameP = paint(0xFF9CA3AF.toInt(), 12f)
        pc.drawText(ell(nameP, "por ${data.userName}", tw), tx, panel.top + 76f, nameP)
    }
    y += panelH + 16f

    // 3 estadisticas
    val stats = listOf(Triple("💪", data.exerciseCount.toString(), "EJERCICIOS"), Triple("✅", "${data.completedSets}/${data.plannedSets}", "SERIES"), Triple("📈", "$completionPct%", "PROGRESO"))
    val gap = 8f
    val bw = (innerW - gap * 2) / 3f
    stats.forEachIndexed { i, (icon, value, label) ->
        val r = RectF(padX + i * (bw + gap), y, padX + i * (bw + gap) + bw, y + 76f)
        pc.drawRoundRect(r, 16f, 16f, fill(0x08FFFFFF))
        pc.drawRoundRect(r, 16f, 16f, stroke(0x14FFFFFF))
        pc.drawText(icon, r.centerX(), r.top + 26f, paint(0xFFFFFFFF.toInt(), 17f, align = Paint.Align.CENTER))
        pc.drawText(value, r.centerX(), r.top + 48f, paint(0xFFFFFFFF.toInt(), 15f, bold = true, mono = true, align = Paint.Align.CENTER))
        pc.drawText(label, r.centerX(), r.top + 64f, paint(0xFF6B7280.toInt(), 8.5f, bold = true, spacing = 0.12f, align = Paint.Align.CENTER))
    }
    y += 76f + 16f

    // Lista de ejercicios
    if (data.exercises.isNotEmpty()) {
        pc.drawText("EJERCICIOS REALIZADOS", padX, y + 10f, paint(0xFF6B7280.toInt(), 10f, bold = true, spacing = 0.16f))
        y += 22f
        val shown = data.exercises.take(6)
        shown.forEachIndexed { i, (name, sets, reps) ->
            pc.drawText("${i + 1}.", padX + 4f, y + 14f, paint(0xFF4B5563.toInt(), 10f, bold = true, mono = true))
            val repsP = paint(neon, 11f, bold = true, mono = true, align = Paint.Align.RIGHT)
            val label = "${sets}×$reps"
            pc.drawText(label, CARD_W - padX - 4f, y + 14f, repsP)
            val nameP = paint(0xFFF3F4F6.toInt(), 12f, bold = true)
            pc.drawText(ell(nameP, name, innerW - 36f - repsP.measureText(label) - 12f), padX + 24f, y + 14f, nameP)
            if (i < shown.size - 1) pc.drawLine(padX, y + 26f, CARD_W - padX, y + 26f, stroke(0x0FFFFFFF))
            y += 28f
        }
        if (data.exercises.size > 6) {
            pc.drawText("+${data.exercises.size - 6} ejercicios más", CARD_W / 2f, y + 14f, paint(0xFF6B7280.toInt(), 11f, mono = true, align = Paint.Align.CENTER))
            y += 24f
        }
        y += 14f
    }

    // Racha + XP
    val bw2 = (innerW - gap) / 2f
    val streakR = RectF(padX, y, padX + bw2, y + 56f)
    pc.drawRoundRect(streakR, 16f, 16f, fill(0x08FFFFFF)); pc.drawRoundRect(streakR, 16f, 16f, stroke(alpha(orange, 0.25f)))
    pc.drawText("🔥", streakR.left + 14f, streakR.top + 35f, paint(0xFFFFFFFF.toInt(), 22f))
    pc.drawText("${data.streak} ${if (data.streak == 1) "día" else "días"}", streakR.left + 50f, streakR.top + 28f, paint(orange, 14f, bold = true, mono = true))
    pc.drawText("RACHA ACTIVA", streakR.left + 50f, streakR.top + 44f, paint(0xFF6B7280.toInt(), 8.5f, bold = true, spacing = 0.12f))
    val xpR = RectF(padX + bw2 + gap, y, CARD_W - padX, y + 56f)
    pc.drawRoundRect(xpR, 16f, 16f, fill(0x08FFFFFF)); pc.drawRoundRect(xpR, 16f, 16f, stroke(alpha(neon, 0.25f)))
    pc.drawText("⚡", xpR.left + 14f, xpR.top + 35f, paint(0xFFFFFFFF.toInt(), 22f))
    pc.drawText("Nv. ${data.level}", xpR.left + 50f, xpR.top + 28f, paint(neon, 14f, bold = true, mono = true))
    pc.drawText("+${data.xpGained} XP", xpR.left + 50f, xpR.top + 44f, paint(0xFF6B7280.toInt(), 8.5f, bold = true, spacing = 0.12f))
    y += 56f + 16f

    // Barra de cumplimiento
    pc.drawText("CUMPLIMIENTO TOTAL", padX, y + 9f, paint(0xFF6B7280.toInt(), 9f, bold = true, spacing = 0.14f))
    pc.drawText("$completionPct%", CARD_W - padX, y + 10f, paint(neon, 10f, bold = true, mono = true, align = Paint.Align.RIGHT))
    y += 16f
    val track = RectF(padX, y, CARD_W - padX, y + 6f)
    pc.drawRoundRect(track, 3f, 3f, fill(0x14FFFFFF))
    val fillW = innerW * (completionPct.coerceIn(0, 100) / 100f)
    if (fillW > 0f) {
        val bar = RectF(padX, y, padX + fillW, y + 6f)
        val gp = Paint(Paint.ANTI_ALIAS_FLAG).apply { shader = LinearGradient(padX, 0f, CARD_W - padX, 0f, neon, accent2, Shader.TileMode.CLAMP) }
        pc.drawRoundRect(bar, 3f, 3f, gp)
    }
    y += 6f + 14f

    // Extras de la sesion (duracion / volumen / pulso)
    val extras = buildList {
        data.durationMin?.let { add("⏱ $it min") }
        data.volumeKg?.takeIf { it > 0 }?.let { add("🏋 ${"%,d".format(Locale.US, it)} kg") }
        data.avgBpm?.let { add("❤ $it bpm" + (data.maxBpm?.let { m -> " (máx $m)" } ?: "")) }
    }
    if (extras.isNotEmpty()) {
        pc.drawText(extras.joinToString("   "), CARD_W / 2f, y + 12f, paint(0xFFD1D5DB.toInt(), 11.5f, bold = true, align = Paint.Align.CENTER))
        y += 28f
    }

    // Pie
    pc.drawLine(padX, y, CARD_W - padX, y, stroke(0x14FFFFFF))
    y += 24f
    pc.drawText("voltbody.app", padX, y, paint(0xFF4B5563.toInt(), 10f, bold = true, spacing = 0.04f))
    pc.drawText("💪 Constancia & Disciplina", CARD_W - padX, y, paint(0xFF4B5563.toInt(), 10f, bold = true, align = Paint.Align.RIGHT))
    y += 24f
    picture.endRecording()

    // 2) Bitmap final: fondo degradado + brillos + contenido
    val totalH = y
    val bmp = Bitmap.createBitmap((CARD_W * SCALE).toInt(), (totalH * SCALE).toInt(), Bitmap.Config.ARGB_8888)
    val c = Canvas(bmp)
    c.scale(SCALE, SCALE)
    val shape = RectF(0f, 0f, CARD_W, totalH)
    val clip = Path().apply { addRoundRect(shape, 28f, 28f, Path.Direction.CW) }
    c.save()
    c.clipPath(clip)
    c.drawRect(shape, Paint().apply { shader = LinearGradient(0f, 0f, CARD_W, totalH, intArrayOf(0xFF1A1A22.toInt(), 0xFF0B0B0F.toInt(), 0xFF0B0B0F.toInt()), floatArrayOf(0f, 0.6f, 1f), Shader.TileMode.CLAMP) })
    c.drawCircle(60f, 20f, 160f, Paint().apply { shader = RadialGradient(60f, 20f, 160f, alpha(neon, 0.14f), 0x00000000, Shader.TileMode.CLAMP) })
    c.drawCircle(CARD_W + 10f, 60f, 140f, Paint().apply { shader = RadialGradient(CARD_W + 10f, 60f, 140f, alpha(neon, 0.22f), 0x00000000, Shader.TileMode.CLAMP) })
    c.drawCircle(-10f, totalH + 10f, 130f, Paint().apply { shader = RadialGradient(-10f, totalH + 10f, 130f, alpha(neon, 0.1f), 0x00000000, Shader.TileMode.CLAMP) })
    c.drawPicture(picture)
    c.restore()
    c.drawRoundRect(RectF(0.5f, 0.5f, CARD_W - 0.5f, totalH - 0.5f), 28f, 28f, stroke(0x1FFFFFFF))
    return bmp
}

/** Guarda el PNG en la galería (MediaStore) y abre el selector de compartir. */
fun shareSummaryBitmap(ctx: Context, bmp: Bitmap, text: String): Boolean {
    return try {
        val url = MediaStore.Images.Media.insertImage(ctx.contentResolver, bmp, "voltbody-sesion", "VoltBody - Sesión completada")
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_TEXT, text)
            putExtra(Intent.EXTRA_SUBJECT, "VoltBody – Sesión completada")
            if (url != null) putExtra(Intent.EXTRA_STREAM, Uri.parse(url))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(send, "Compartir logro").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        ctx.startActivity(chooser)
        true
    } catch (_: Throwable) {
        false
    }
}

/** Diálogo final: tarjeta renderizada (vista previa de lo que se comparte) + botón Compartir. */
@Composable
fun SummaryDialog(data: WorkoutSummaryData, onDismiss: () -> Unit, onToast: (String, String) -> Unit) {
    val ctx = LocalContext.current
    val accent = Volt.accent.toArgb()
    val accent2 = Volt.accent2.toArgb()
    var bmp by remember { mutableStateOf<Bitmap?>(null) }
    var sharing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val haptics = rememberHaptics()
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { haptics.heavy(); appear.animateTo(1f, tween(600, easing = FastOutSlowInEasing)) }
    LaunchedEffect(data) { bmp = withContext(Dispatchers.Default) { runCatching { renderSummaryBitmap(data, accent, accent2) }.getOrNull() } }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)) {
        Box(Modifier.fillMaxSize().background(Volt.bg).systemBarsPadding()) {
            MeshBackground(Modifier.fillMaxSize(), colorA = Volt.accent, colorB = Volt.accent2, intensity = 0.55f)
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text("¡Entreno completado!", color = Volt.text, style = VoltType.h1, modifier = Modifier.weight(1f))
                    Box(Modifier.size(46.dp).clip(CircleShape).background(Volt.surface).pressScale(onDismiss), contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.Close, "Cerrar", tint = Volt.text)
                    }
                }
                Spacer(Modifier.height(16.dp))
                val b = bmp
                if (b == null) {
                    SkeletonBox(Modifier.fillMaxWidth().height(420.dp), RoundedCornerShape(36.dp))
                } else {
                    Image(
                        b.asImageBitmap(), "Tarjeta de sesión completada", contentScale = ContentScale.FillWidth,
                        modifier = Modifier.fillMaxWidth().graphicsLayer { val s = 0.9f + 0.1f * appear.value; scaleX = s; scaleY = s; alpha = appear.value },
                    )
                }
                Spacer(Modifier.height(20.dp))
                PrimaryButton(
                    if (sharing) "Generando tarjeta..." else "Compartir logro",
                    onClick = {
                        val bm = bmp ?: return@PrimaryButton
                        if (sharing) return@PrimaryButton
                        sharing = true
                        scope.launch {
                            val ok = withContext(Dispatchers.IO) { shareSummaryBitmap(ctx, bm, "Acabo de completar \"${data.focus}\" (${data.exerciseCount} ejercicios) en VoltBody 🔥") }
                            if (!ok) onToast("Error al compartir", "No se pudo generar la imagen.")
                            sharing = false
                        }
                    },
                    enabled = b != null, loading = sharing,
                )
                Spacer(Modifier.height(10.dp))
                Text("Cerrar", color = Volt.textDim, style = VoltType.title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().pressScale(onDismiss).padding(14.dp))
            }
        }
    }
}
