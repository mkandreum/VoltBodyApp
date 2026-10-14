package com.voltbody.app.ui.screens.home

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Base64
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.State
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.GifDecoder
import coil.decode.ImageDecoderDecoder
import com.voltbody.app.ui.theme.Volt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

private fun decodeSampled(bytes: ByteArray, target: Int = 1024): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
    var sample = 1
    while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= target) sample *= 2
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
}

/** Imagen desde data-URL base64 (BitmapFactory) o URL remota (Coil). */
@Composable
fun DataImage(src: String?, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop) {
    if (src.isNullOrBlank()) return
    if (src.startsWith("data:")) {
        val bmp: State<ImageBitmap?> = produceState<ImageBitmap?>(null, src) {
            value = withContext(Dispatchers.Default) {
                runCatching {
                    val bytes = Base64.decode(src.substringAfter("base64,"), Base64.DEFAULT)
                    decodeSampled(bytes)?.asImageBitmap()
                }.getOrNull()
            }
        }
        Box(modifier) { bmp.value?.let { Image(it, null, Modifier.fillMaxSize(), contentScale = contentScale) } }
    } else {
        AsyncImage(model = src, contentDescription = null, modifier = modifier, contentScale = contentScale)
    }
}

/** ImageLoader con soporte GIF (los ejercicios traen GIF). */
@Composable
fun rememberGifLoader(): ImageLoader {
    val ctx = LocalContext.current
    return remember {
        ImageLoader.Builder(ctx).components {
            if (Build.VERSION.SDK_INT >= 28) add(ImageDecoderDecoder.Factory()) else add(GifDecoder.Factory())
        }.crossfade(true).build()
    }
}

/** Imagen/GIF de un ejercicio con marcador si no hay url. */
@Composable
fun ExerciseImage(url: String?, loader: ImageLoader, modifier: Modifier = Modifier) {
    if (url.isNullOrBlank()) {
        Box(modifier.background(Volt.surface2), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.FitnessCenter, null, tint = Volt.textDim)
        }
    } else {
        AsyncImage(model = url, imageLoader = loader, contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    }
}

/** Reduce a <=1024px y comprime a JPEG; devuelve data-URL base64. */
fun bitmapToDataUrl(src: Bitmap, maxSide: Int = 1024, quality: Int = 82): String {
    val scale = min(1f, maxSide.toFloat() / max(src.width, src.height))
    val bmp = if (scale < 1f) Bitmap.createScaledBitmap(src, (src.width * scale).roundToInt().coerceAtLeast(1), (src.height * scale).roundToInt().coerceAtLeast(1), true) else src
    val out = ByteArrayOutputStream()
    bmp.compress(Bitmap.CompressFormat.JPEG, quality, out)
    return "data:image/jpeg;base64," + Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
}

/** Lee una imagen de la galeria respetando la rotacion EXIF y la devuelve como data-URL JPEG <=1024px. */
suspend fun uriToDataUrl(ctx: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
    runCatching {
        val bytes = ctx.contentResolver.openInputStream(uri)!!.use { it.readBytes() }
        var bmp = decodeSampled(bytes, 2048) ?: return@runCatching null
        val rotation = runCatching {
            ctx.contentResolver.openInputStream(uri)!!.use { s ->
                when (ExifInterface(s).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                    else -> 0f
                }
            }
        }.getOrDefault(0f)
        if (rotation != 0f) bmp = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, Matrix().apply { postRotate(rotation) }, true)
        bitmapToDataUrl(bmp)
    }.getOrNull()
}

/** Confeti breve: cada vez que [trigger] cambia (y es > 0) lanza una rafaga. */
@Composable
fun ConfettiBurst(trigger: Int, modifier: Modifier = Modifier) {
    val progress = remember { Animatable(1f) }
    val particles = remember(trigger) {
        List(48) {
            val a = Random.nextFloat() * 6.283f
            val speed = 0.25f + Random.nextFloat() * 0.6f
            Triple(Offset(kotlin.math.cos(a) * speed, kotlin.math.sin(a) * speed - 0.25f), Random.nextInt(4), 4f + Random.nextFloat() * 6f)
        }
    }
    LaunchedEffect(trigger) {
        if (trigger > 0) { progress.snapTo(0f); progress.animateTo(1f, tween(1400, easing = LinearEasing)) }
    }
    val colors = listOf(Volt.accent, Volt.orange, Volt.water, Color.White)
    Canvas(modifier) {
        val t = progress.value
        if (t >= 1f) return@Canvas
        val cx = size.width / 2; val cy = size.height * 0.35f
        particles.forEach { (v, c, s) ->
            val x = cx + v.x * size.width * t
            val y = cy + v.y * size.height * t + 0.6f * size.height * t * t
            drawRect(colors[c].copy(alpha = 1f - t), Offset(x, y), Size(s * 2, s))
        }
    }
}
