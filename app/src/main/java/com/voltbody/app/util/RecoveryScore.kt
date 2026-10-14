package com.voltbody.app.util

import com.voltbody.app.data.RecoveryLog
import com.voltbody.app.data.Repository
import java.time.LocalDate
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Port de recoveryScore.ts: score 0-100 a partir de sueno y HRV matutino. */
enum class RecoveryTier { LOW, MODERATE, GOOD, OPTIMAL }

data class RecoveryAdvice(val emoji: String, val title: String, val subtitle: String, val intensityLabel: String, val tier: RecoveryTier)

private fun sleepSubScore(hours: Double): Int {
    if (hours <= 0) return 0
    if (hours in 5.0..9.5) return max(0.0, 50 - abs(hours - 8) * 11).roundToInt()
    if (hours < 5) return ((hours / 5) * 18).roundToInt()
    return max(0.0, 50 - (hours - 9.5) * 10).roundToInt()
}

private fun hrvSubScore(hrv: Double?, recent: List<RecoveryLog>): Int {
    if (hrv == null || hrv <= 0) return 25
    val past = recent.takeLast(7).mapNotNull { it.hrv }.filter { it > 0 }
    if (past.size >= 3) {
        val baseline = past.average()
        val diff = hrv - baseline
        val raw = 25 + (if (diff > 0) diff else diff * 1.5)
        return max(0.0, min(50.0, raw)).roundToInt()
    }
    return (10 + ((min(90.0, max(20.0, hrv)) - 20) / 70) * 40).roundToInt()
}

/** @param recentLogs registros anteriores (sin hoy) para la linea base de HRV. */
fun computeRecoveryScore(sleepHours: Double, hrv: Double?, recentLogs: List<RecoveryLog> = emptyList()): Int =
    min(100, max(0, sleepSubScore(sleepHours) + hrvSubScore(hrv, recentLogs)))

fun getRecoveryTier(score: Int): RecoveryTier = when {
    score < 40 -> RecoveryTier.LOW
    score < 65 -> RecoveryTier.MODERATE
    score < 85 -> RecoveryTier.GOOD
    else -> RecoveryTier.OPTIMAL
}

fun getRecoveryAdvice(score: Int, todayFocus: String? = null): RecoveryAdvice {
    val focus = todayFocus ?: "sesión de hoy"
    return when (getRecoveryTier(score)) {
        RecoveryTier.LOW -> RecoveryAdvice("🛌", "Recuperación prioritaria",
            "Recovery Score $score/100. Convierte $focus en movilidad activa y estiramiento. No fuerces intensidad.", "🔵 Activación / movilidad", RecoveryTier.LOW)
        RecoveryTier.MODERATE -> RecoveryAdvice("⚠️", "Baja el volumen hoy",
            "Recovery Score $score/100. Reduce el número de series un 20% y prioriza técnica limpia.", "🟡 Volumen reducido", RecoveryTier.MODERATE)
        RecoveryTier.GOOD -> RecoveryAdvice("✅", "Sesión normal",
            "Recovery Score $score/100. Estás en condiciones para tu $focus habitual.", "🟢 Intensidad normal", RecoveryTier.GOOD)
        RecoveryTier.OPTIMAL -> RecoveryAdvice("🚀", "¡Día de sobrecarga!",
            "Recovery Score $score/100. Condición óptima — añade peso o sube el volumen en $focus.", "🔥 Máxima intensidad", RecoveryTier.OPTIMAL)
    }
}

/**
 * Check-in de recuperación: calcula el score con el historial previo, lo guarda (upsert por fecha)
 * en recoveryLogs y lo sube (PUT /data/recovery). Devuelve (score, consejo).
 */
suspend fun saveRecoveryCheckin(repo: Repository, sleepHours: Double, hrv: Double?, date: LocalDate = LocalDate.now()): Pair<Int, RecoveryAdvice> {
    val d = date.toString()
    val past = repo.state.value.recoveryLogs.filter { it.date < d }.sortedBy { it.date }
    val hrvClean = hrv?.takeIf { it > 0 }
    val score = computeRecoveryScore(sleepHours, hrvClean, past)
    val log = RecoveryLog(d, sleepHours, hrvClean, score)
    repo.saveRecovery(log)
    return score to getRecoveryAdvice(score)
}
