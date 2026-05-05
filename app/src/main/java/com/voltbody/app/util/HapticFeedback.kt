package com.voltbody.app.util

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

enum class HapticType { TICK, CONFIRM, ERROR, HEAVY }

class HapticFeedback(private val context: Context) {
    private val vibrator: Vibrator by lazy {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vm.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    fun perform(type: HapticType) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val effect = when (type) {
            HapticType.TICK    -> VibrationEffect.createOneShot(10, 40)
            HapticType.CONFIRM -> VibrationEffect.createOneShot(30, 80)
            HapticType.ERROR   -> VibrationEffect.createWaveform(longArrayOf(0, 40, 60, 40), -1)
            HapticType.HEAVY   -> VibrationEffect.createOneShot(60, VibrationEffect.DEFAULT_AMPLITUDE)
        }
        vibrator.vibrate(effect)
    }
}

@Composable
fun rememberHaptic(): HapticFeedback {
    val context = LocalContext.current
    return remember { HapticFeedback(context) }
}
