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

    /** Usa los efectos predefinidos del motor haptico (API 29+), que si se notan; si no, one-shot a amplitud maxima. */
    private fun fire(effectId: Int, fallbackMs: Long) {
        val v = vibrator ?: return
        runCatching {
            if (!v.hasVibrator()) return
            val effect = if (Build.VERSION.SDK_INT >= 29) VibrationEffect.createPredefined(effectId)
            else VibrationEffect.createOneShot(fallbackMs, 255)
            v.vibrate(effect)
        }
    }

    private fun wave(vararg pattern: Long) {
        val v = vibrator ?: return
        runCatching {
            if (!v.hasVibrator()) return
            val amps = IntArray(pattern.size) { if (it % 2 == 0) 0 else 255 }
            v.vibrate(VibrationEffect.createWaveform(pattern, amps, -1))
        }
    }

    /** Tic de seleccion: navegacion, ruedas, chips */
    fun selection() = fire(VibrationEffect.EFFECT_TICK, 12)
    /** Pulsacion de boton */
    fun light() = fire(VibrationEffect.EFFECT_CLICK, 18)
    /** Serie completada, anadir agua */
    fun medium() = fire(VibrationEffect.EFFECT_CLICK, 28)
    /** Terminar entreno, logro */
    fun heavy() = fire(VibrationEffect.EFFECT_HEAVY_CLICK, 45)
    /** Exito: doble toque */
    fun success() = fire(VibrationEffect.EFFECT_DOUBLE_CLICK, 30)
    fun warning() = wave(0, 25, 60, 25)
    /** Celebracion (logro, entreno completo): golpe fuerte + redoble que crece. */
    fun celebrate() = wave(0, 45, 70, 20, 50, 20, 50, 70)
    fun error() = wave(0, 35, 70, 35, 70, 35)

    companion object {
        fun of(context: Context) = Haptics(context.applicationContext)
        fun selection(ctx: Context) = of(ctx).selection()
        fun light(ctx: Context) = of(ctx).light()
        fun medium(ctx: Context) = of(ctx).medium()
        fun heavy(ctx: Context) = of(ctx).heavy()
        fun success(ctx: Context) = of(ctx).success()
        fun warning(ctx: Context) = of(ctx).warning()
        fun error(ctx: Context) = of(ctx).error()
        fun celebrate(ctx: Context) = of(ctx).celebrate()
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val ctx = LocalContext.current
    return remember(ctx) { Haptics.of(ctx) }
}
