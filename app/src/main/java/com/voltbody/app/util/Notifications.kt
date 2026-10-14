package com.voltbody.app.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.voltbody.app.MainActivity
import com.voltbody.app.data.AppState
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

/**
 * Port de notificationService.ts + sw-notifications.js con WorkManager.
 *  - Comidas: aviso 5 min antes de cada hora de profile.mealTimes (se repite a diario).
 *  - Racha en peligro: 3+ dias sin entrenar (8 s tras abrir la app y revision diaria 10:00).
 *  - Pesaje semanal: sin peso registrado esta semana (15 s tras abrir y revision diaria 10:00).
 *  - Tocar la notificacion abre la app (equivale a notificationclick del service worker).
 */
object Notifications {
    const val CH_TRAINING = "voltbody_training"
    const val CH_MEALS = "voltbody_meals"
    const val CH_WEIGHT = "voltbody_weight"

    private const val PREFS = "voltbody_prefs"
    private const val K_LAST_LOG = "notif_last_log_date"   // YYYY-MM-DD
    private const val K_LAST_WEIGHT = "notif_last_weight_date" // YYYY-MM-DD
    private const val K_MEALS = "notif_meal_times"          // "breakfast=08:00;lunch=14:00"
    private const val K_ENABLED = "notif_enabled"

    private const val MEAL_PREFIX = "voltbody_meal_"
    private const val DAILY_CHECK = "voltbody_daily_check"
    private const val IMMEDIATE_STREAK = "voltbody_streak_now"
    private const val IMMEDIATE_WEIGHT = "voltbody_weight_now"

    private const val STREAK_DELAY_MS = 8_000L
    private const val WEIGHT_DELAY_MS = 15_000L
    private const val MEAL_LEAD_MIN = 5L
    private const val DAILY_CHECK_HOUR = 10

    private val mealLabels = mapOf(
        "breakfast" to "🌅 Desayuno",
        "brunch" to "🥐 Almuerzo matutino",
        "lunch" to "🍽️ Comida",
        "snack" to "🍎 Merienda",
        "dinner" to "🌙 Cena",
    )

    fun ensureChannels(ctx: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CH_TRAINING, "Entreno y racha", NotificationManager.IMPORTANCE_HIGH)
            .apply { description = "Avisos de entrenamiento y racha en peligro" })
        nm.createNotificationChannel(NotificationChannel(CH_MEALS, "Comidas", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Recordatorios de tus comidas" })
        nm.createNotificationChannel(NotificationChannel(CH_WEIGHT, "Pesaje semanal", NotificationManager.IMPORTANCE_DEFAULT)
            .apply { description = "Recordatorio semanal de registrar tu peso" })
    }

    fun hasPermission(ctx: Context): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        else true
        return granted && NotificationManagerCompat.from(ctx).areNotificationsEnabled()
    }

    /** Programa todo segun el estado actual (llamar al abrir la app y al cambiar perfil/logs). */
    fun scheduleAll(ctx: Context, s: AppState) {
        val app = ctx.applicationContext
        ensureChannels(app)
        cancelAll(app)
        val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val enabled = s.notificationsEnabled
        prefs.edit().putBoolean(K_ENABLED, enabled).apply()
        if (!enabled) return

        val lastLog = s.logs.map { it.date.take(10) }.maxOrNull()
        val lastWeight = s.weightLogs.map { it.date.take(10) }.maxOrNull()
        val mt = s.profile.mealTimes
        val meals = if (mt == null) emptyMap() else linkedMapOf(
            "breakfast" to mt.breakfast, "brunch" to mt.brunch, "lunch" to mt.lunch, "snack" to mt.snack, "dinner" to mt.dinner,
        ).filterValues { it.isNotBlank() }
        prefs.edit()
            .putString(K_LAST_LOG, lastLog ?: "")
            .putString(K_LAST_WEIGHT, lastWeight ?: "")
            .putString(K_MEALS, meals.entries.joinToString(";") { "${it.key}=${it.value}" })
            .apply()

        val wm = WorkManager.getInstance(app)
        for ((key, time) in meals) scheduleMeal(app, key, time)

        // Comprobaciones inmediatas (8 s / 15 s tras abrir, como la web) + revision diaria
        wm.enqueueUniqueWork(IMMEDIATE_STREAK, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CheckWorker>().setInitialDelay(STREAK_DELAY_MS, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(CheckWorker.KIND to "streak", CheckWorker.REPEAT to false)).build())
        wm.enqueueUniqueWork(IMMEDIATE_WEIGHT, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<CheckWorker>().setInitialDelay(WEIGHT_DELAY_MS, TimeUnit.MILLISECONDS)
                .setInputData(workDataOf(CheckWorker.KIND to "weight", CheckWorker.REPEAT to false)).build())
        scheduleDailyCheck(app)
    }

    fun cancelAll(ctx: Context) {
        val wm = WorkManager.getInstance(ctx.applicationContext)
        listOf("breakfast", "brunch", "lunch", "snack", "dinner").forEach { wm.cancelUniqueWork(MEAL_PREFIX + it) }
        wm.cancelUniqueWork(DAILY_CHECK)
        wm.cancelUniqueWork(IMMEDIATE_STREAK)
        wm.cancelUniqueWork(IMMEDIATE_WEIGHT)
    }

    // ---- Internos ----

    private fun parseTime(t: String): LocalTime? = try {
        val (h, m) = t.trim().split(":").map { it.toInt() }
        if (h !in 0..23 || m !in 0..59) null else LocalTime.of(h, m)
    } catch (_: Exception) { null }

    /** Siguiente instante (hoy si falta mas de 1 min, si no manana) en ms de retardo. */
    private fun delayUntil(time: LocalTime, leadMinutes: Long = 0): Long {
        val now = LocalDateTime.now()
        var target = LocalDateTime.of(LocalDate.now(), time).minusMinutes(leadMinutes)
        if (Duration.between(now, target).toMillis() <= 60_000L) target = target.plusDays(1)
        return Duration.between(now, target).toMillis()
    }

    internal fun scheduleMeal(ctx: Context, key: String, time: String) {
        val t = parseTime(time) ?: return
        val req = OneTimeWorkRequestBuilder<MealWorker>()
            .setInitialDelay(delayUntil(t, MEAL_LEAD_MIN), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(MealWorker.KEY to key, MealWorker.TIME to time)).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(MEAL_PREFIX + key, ExistingWorkPolicy.REPLACE, req)
    }

    internal fun scheduleDailyCheck(ctx: Context) {
        val req = OneTimeWorkRequestBuilder<CheckWorker>()
            .setInitialDelay(delayUntil(LocalTime.of(DAILY_CHECK_HOUR, 0)), TimeUnit.MILLISECONDS)
            .setInputData(workDataOf(CheckWorker.KIND to "both", CheckWorker.REPEAT to true)).build()
        WorkManager.getInstance(ctx).enqueueUniqueWork(DAILY_CHECK, ExistingWorkPolicy.REPLACE, req)
    }

    internal fun show(ctx: Context, id: Int, channel: String, title: String, body: String) {
        if (!hasPermission(ctx)) return
        ensureChannels(ctx)
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(ctx, channel)
            .setSmallIcon(ctx.applicationInfo.icon)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(open)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try { NotificationManagerCompat.from(ctx).notify(id, n) } catch (_: SecurityException) { }
    }

    internal fun streakDays(ctx: Context): Int? {
        val last = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(K_LAST_LOG, "") ?: ""
        if (last.isBlank()) return null
        return try { ChronoUnit.DAYS.between(LocalDate.parse(last), LocalDate.now()).toInt() } catch (_: Exception) { null }
    }

    internal fun hasWeightThisWeek(ctx: Context): Boolean {
        val last = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(K_LAST_WEIGHT, "") ?: ""
        if (last.isBlank()) return false
        return try {
            val monday = LocalDate.now().with(DayOfWeek.MONDAY)
            !LocalDate.parse(last).isBefore(monday)
        } catch (_: Exception) { false }
    }

    internal fun isEnabled(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(K_ENABLED, false)

    internal fun mealLabel(key: String) = mealLabels[key] ?: "Comida"

    internal fun notifyStreak(ctx: Context) {
        val days = streakDays(ctx) ?: return // sin logs no hay racha que perder (como la web)
        if (days >= 3) show(ctx, 7001, CH_TRAINING, "¡Tu racha está en peligro! 🔥", "Llevas $days días sin entrenar. ¡Hoy es el día!")
    }

    internal fun notifyWeight(ctx: Context) {
        if (!hasWeightThisWeek(ctx))
            show(ctx, 7002, CH_WEIGHT, "⚖️ Recordatorio semanal", "Aún no has registrado tu peso esta semana. ¡Solo tarda 5 segundos!")
    }
}

/** Recordatorio de comida 5 min antes; se reprograma para el dia siguiente. */
class MealWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    companion object { const val KEY = "key"; const val TIME = "time" }

    override suspend fun doWork(): Result {
        val key = inputData.getString(KEY) ?: return Result.success()
        val time = inputData.getString(TIME) ?: return Result.success()
        if (!Notifications.isEnabled(applicationContext)) return Result.success()
        val label = Notifications.mealLabel(key)
        val plain = label.substringAfter(' ').lowercase()
        Notifications.show(
            applicationContext, 6000 + key.hashCode().and(0xFF), Notifications.CH_MEALS,
            "VoltBody – $label en 5 min", "Tu $plain es a las $time. ¡Prepárate!",
        )
        Notifications.scheduleMeal(applicationContext, key, time)
        return Result.success()
    }
}

/** Racha en peligro / pesaje semanal. Si REPEAT, se reprograma cada dia a las 10:00. */
class CheckWorker(ctx: Context, params: WorkerParameters) : CoroutineWorker(ctx, params) {
    companion object { const val KIND = "kind"; const val REPEAT = "repeat" }

    override suspend fun doWork(): Result {
        val c = applicationContext
        if (Notifications.isEnabled(c)) {
            val kind = inputData.getString(KIND) ?: "both"
            if (kind == "streak" || kind == "both") Notifications.notifyStreak(c)
            if (kind == "weight" || kind == "both") Notifications.notifyWeight(c)
            if (inputData.getBoolean(REPEAT, false)) Notifications.scheduleDailyCheck(c)
        }
        return Result.success()
    }
}
