package com.voltbody.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Feedback haptico semantico (equivale a haptics.ts). Patrones: [vibrar, pausa, vibrar...] en ms.
 * Requiere permiso VIBRATE (ya en el manifest). Nunca lanza excepciones.
 */
class Haptics(context: Context) {
    private val vibrator: Vibrator? = runCatching {
        if (Build.VERSION.SDK_INT >= 31) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
    }.getOrNull()

    private fun vibrate(vararg pattern: Long) {
        val v = vibrator ?: return
        runCatching {
            if (!v.hasVibrator()) return
            val effect = if (pattern.size == 1) VibrationEffect.createOneShot(pattern[0], VibrationEffect.DEFAULT_AMPLITUDE)
            else VibrationEffect.createWaveform(longArrayOf(0L) + pattern, -1)
            v.vibrate(effect)
        }
    }

    /** Toque ultra ligero: navegacion, cambios de pestana (~6ms) */
    fun selection() = vibrate(6)
    /** Impacto ligero: botones y toggles (~10ms) */
    fun light() = vibrate(10)
    /** Impacto medio: series completadas, anadir agua (~20ms) */
    fun medium() = vibrate(20)
    /** Impacto fuerte: terminar entreno, desbloquear logro (~35ms) */
    fun heavy() = vibrate(35)
    /** Exito: doble toque rapido */
    fun success() = vibrate(10, 30, 20)
    fun warning() = vibrate(20, 50, 20)
    fun error() = vibrate(30, 60, 30, 60, 30)

    companion object {
        fun of(context: Context) = Haptics(context.applicationContext)
        fun selection(ctx: Context) = of(ctx).selection()
        fun light(ctx: Context) = of(ctx).light()
        fun medium(ctx: Context) = of(ctx).medium()
        fun heavy(ctx: Context) = of(ctx).heavy()
        fun success(ctx: Context) = of(ctx).success()
        fun warning(ctx: Context) = of(ctx).warning()
        fun error(ctx: Context) = of(ctx).error()
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val ctx = LocalContext.current
    return remember(ctx) { Haptics.of(ctx) }
}
