package com.voltbody.app.ui.screens.profile

import android.Manifest
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.Image
import coil.compose.AsyncImage
import com.voltbody.app.ui.theme.Volt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.max

object PhotoUtil {
    /** Tamano maximo del JPEG comprimido (bytes, antes de base64). */
    const val MAX_BYTES = 1_000_000
    private const val MAX_DIM = 1280

    /** Decodifica un Uri (galeria/camara) reduciendo a MAX_DIM y lo devuelve como data-URL JPEG <= ~1MB. */
    suspend fun uriToDataUrl(ctx: Context, uri: Uri): String? = withContext(Dispatchers.Default) {
        runCatching {
            val bmp = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ctx.contentResolver, uri)) { decoder, info, _ ->
                val m = max(info.size.width, info.size.height)
                if (m > MAX_DIM) {
                    val s = MAX_DIM.toFloat() / m
                    decoder.setTargetSize((info.size.width * s).toInt().coerceAtLeast(1), (info.size.height * s).toInt().coerceAtLeast(1))
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
            bitmapToDataUrl(bmp)
        }.getOrNull()
    }

    /** Comprime a JPEG bajando calidad y, si hace falta, resolucion hasta caber en MAX_BYTES. */
    suspend fun bitmapToDataUrl(source: Bitmap): String = withContext(Dispatchers.Default) {
        var bmp = source
        var bytes: ByteArray
        while (true) {
            var q = 88
            var out: ByteArray
            do {
                val bos = ByteArrayOutputStream()
                bmp.compress(Bitmap.CompressFormat.JPEG, q, bos)
                out = bos.toByteArray()
                q -= 12
            } while (out.size > MAX_BYTES && q >= 40)
            bytes = out
            if (bytes.size <= MAX_BYTES || max(bmp.width, bmp.height) < 320) break
            bmp = Bitmap.createScaledBitmap(bmp, (bmp.width * 0.75f).toInt(), (bmp.height * 0.75f).toInt(), true)
        }
        "data:image/jpeg;base64," + Base64.encodeToString(bytes, Base64.NO_WRAP)
    }

    /** Decodifica un data-URL base64 a Bitmap reducido (para mostrar). */
    fun decodeDataUrl(url: String, reqSize: Int = 800): Bitmap? = runCatching {
        val b64 = url.substringAfter("base64,", "")
        if (b64.isEmpty()) return null
        val data = Base64.decode(b64, Base64.DEFAULT)
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(data, 0, data.size, bounds)
        var sample = 1
        while (bounds.outWidth / (sample * 2) >= reqSize && bounds.outHeight / (sample * 2) >= reqSize) sample *= 2
        BitmapFactory.decodeByteArray(data, 0, data.size, BitmapFactory.Options().apply { inSampleSize = sample })
    }.getOrNull()
}

/** Muestra una imagen que puede ser data-URL base64 o URL http(s). */
@Composable
fun DataUrlImage(url: String, modifier: Modifier = Modifier, contentScale: ContentScale = ContentScale.Crop, reqSize: Int = 800) {
    if (url.startsWith("data:")) {
        val bmp by produceState<Bitmap?>(null, url) { value = withContext(Dispatchers.Default) { PhotoUtil.decodeDataUrl(url, reqSize) } }
        val b = bmp
        if (b != null) Image(b.asImageBitmap(), null, modifier, contentScale = contentScale) else Box(modifier)
    } else {
        AsyncImage(url, null, modifier, contentScale = contentScale)
    }
}

@Stable
class PhotoPicker internal constructor(private val show: MutableState<Boolean>) {
    fun open() { show.value = true }
}

/**
 * Selector de foto (galeria o camara). Devuelve el data-URL JPEG comprimido (<= ~1MB) en [onPhoto].
 * Incluye el dialogo de eleccion; basta llamar a picker.open().
 */
@Composable
fun rememberPhotoPicker(title: String, onPhoto: suspend (String) -> Unit): PhotoPicker {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val show = remember { mutableStateOf(false) }
    val callback by rememberUpdatedState(onPhoto)
    var cameraUri by remember { mutableStateOf<Uri?>(null) }

    fun deliver(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            val url = PhotoUtil.uriToDataUrl(ctx, uri)
            if (url == null) com.voltbody.app.ui.screens.profile.toastError(ctx) else callback(url)
        }
    }

    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { deliver(it) }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val uri = cameraUri
        if (ok && uri != null) {
            scope.launch {
                val url = PhotoUtil.uriToDataUrl(ctx, uri)
                runCatching { ctx.contentResolver.delete(uri, null, null) } // no dejar copia en la galeria
                if (url == null) toastError(ctx) else callback(url)
            }
        } else if (uri != null) runCatching { ctx.contentResolver.delete(uri, null, null) }
        cameraUri = null
    }
    val preview = rememberLauncherForActivityResult(ActivityResultContracts.TakePicturePreview()) { bmp ->
        if (bmp != null) scope.launch { callback(PhotoUtil.bitmapToDataUrl(bmp)) }
    }

    fun launchCamera() {
        if (Build.VERSION.SDK_INT >= 29) {
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, "voltbody_${System.currentTimeMillis()}.jpg")
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            }
            val uri = runCatching { ctx.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) }.getOrNull()
            if (uri != null) { cameraUri = uri; takePicture.launch(uri); return }
        }
        preview.launch(null)
    }

    val camPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera()
    }

    if (show.value) {
        AlertDialog(
            onDismissRequest = { show.value = false },
            containerColor = Volt.dialogSurface,
            title = { Text(title, color = Volt.text, fontWeight = FontWeight.Bold) },
            text = { Text("Elige de dónde quieres obtener la foto. Se comprime automáticamente.", color = Volt.textDim) },
            confirmButton = {
                TextButton(onClick = {
                    show.value = false
                    camPerm.launch(Manifest.permission.CAMERA)
                }) { Text("Cámara", color = Volt.accent, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { show.value = false; gallery.launch("image/*") }) { Text("Galería", color = Volt.accent, fontWeight = FontWeight.Bold) }
            },
        )
    }
    return remember { PhotoPicker(show) }
}

internal fun toastError(ctx: Context) {
    android.widget.Toast.makeText(ctx, "No se pudo procesar la imagen", android.widget.Toast.LENGTH_SHORT).show()
}
