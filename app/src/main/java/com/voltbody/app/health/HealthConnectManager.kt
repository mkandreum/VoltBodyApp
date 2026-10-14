package com.voltbody.app.health

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.HeartRateRecord
import androidx.health.connect.client.records.HeartRateVariabilityRmssdRecord
import androidx.health.connect.client.records.Record
import androidx.health.connect.client.records.RestingHeartRateRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.reflect.KClass

data class HealthSnapshot(
    val steps: Long = 0,
    val distanceKm: Double = 0.0,
    val activeKcal: Double = 0.0,
    val totalKcal: Double = 0.0,
    val sleepHours: Double? = null,
    val hrvMs: Double? = null,
    val restingHr: Int? = null,
    val avgHr: Int? = null,
    val weightKg: Double? = null,
)

/**
 * Acceso a Google Health Connect.
 *
 * Samsung Health NO ofrece un SDK publico sin ser partner; en cambio Samsung Health sincroniza
 * pasos, sueno, FC, peso, etc. con Health Connect (Samsung Health > Ajustes > Health Connect).
 * Por eso toda la integracion (Samsung Health, Fitbit, Garmin, Google Fit...) se hace
 * unicamente leyendo/escribiendo en Health Connect.
 */
class HealthConnectManager(ctx: Context) {

    private val appCtx: Context = ctx.applicationContext
    private val zone: ZoneId get() = ZoneId.systemDefault()

    /** HealthConnectClient.SDK_AVAILABLE / SDK_UNAVAILABLE / SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED. */
    fun availability(): Int = HealthConnectClient.getSdkStatus(appCtx)

    private val client: HealthConnectClient? by lazy {
        if (availability() == HealthConnectClient.SDK_AVAILABLE) HealthConnectClient.getOrCreate(appCtx) else null
    }

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
        HealthPermission.getReadPermission(HeartRateVariabilityRmssdRecord::class),
        HealthPermission.getReadPermission(RestingHeartRateRecord::class),
        HealthPermission.getReadPermission(HeartRateRecord::class),
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getWritePermission(WeightRecord::class),
    )

    private suspend fun granted(): Set<String> {
        val c = client ?: return emptySet()
        return try { c.permissionController.getGrantedPermissions() } catch (_: Exception) { emptySet() }
    }

    suspend fun hasAllPermissions(): Boolean = granted().containsAll(permissions)

    /** Con permisos parciales tambien funciona (se leen solo los concedidos). */
    suspend fun hasAnyPermission(): Boolean = granted().any { it in permissions }

    private fun Set<String>.canRead(rec: KClass<out Record>) = contains(HealthPermission.getReadPermission(rec))

    suspend fun readToday(): HealthSnapshot {
        val c = client ?: return HealthSnapshot()
        val now = Instant.now()
        val startOfDay = LocalDate.now(zone).atStartOfDay(zone).toInstant()
        val range = TimeRangeFilter.between(startOfDay, now)
        val g = granted()

        var steps = 0L
        var dist = 0.0
        var active = 0.0
        var total = 0.0
        var avgHr: Int? = null

        // Agregados de HC: deduplican entre fuentes (Samsung Health, Google Fit...)
        val metrics = buildSet {
            if (g.canRead(StepsRecord::class)) add(StepsRecord.COUNT_TOTAL)
            if (g.canRead(DistanceRecord::class)) add(DistanceRecord.DISTANCE_TOTAL)
            if (g.canRead(ActiveCaloriesBurnedRecord::class)) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            if (g.canRead(TotalCaloriesBurnedRecord::class)) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            if (g.canRead(HeartRateRecord::class)) add(HeartRateRecord.BPM_AVG)
        }
        if (metrics.isNotEmpty()) {
            try {
                val r = c.aggregate(AggregateRequest(metrics = metrics, timeRangeFilter = range))
                steps = r[StepsRecord.COUNT_TOTAL] ?: 0L
                dist = r[DistanceRecord.DISTANCE_TOTAL]?.inKilometers ?: 0.0
                active = r[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
                total = r[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
                avgHr = r[HeartRateRecord.BPM_AVG]?.toInt()
            } catch (_: Exception) { }
        }

        return HealthSnapshot(
            steps = steps,
            distanceKm = dist,
            activeKcal = active,
            totalKcal = total,
            sleepHours = if (g.canRead(SleepSessionRecord::class)) readSleepLastNight() else null,
            hrvMs = if (g.canRead(HeartRateVariabilityRmssdRecord::class)) readHrv() else null,
            restingHr = if (g.canRead(RestingHeartRateRecord::class)) readRestingHr() else null,
            avgHr = avgHr,
            weightKg = if (g.canRead(WeightRecord::class)) readLatestWeight() else null,
        )
    }

    /** Horas dormidas la ultima noche (sesiones que terminan hoy; intervalos fusionados). Null si no hay datos. */
    suspend fun readSleepLastNight(): Double? {
        val c = client ?: return null
        if (!granted().canRead(SleepSessionRecord::class)) return null
        return try {
            val today = LocalDate.now(zone)
            val from = ZonedDateTime.of(today.minusDays(1), LocalTime.NOON, zone).toInstant()
            val resp = c.readRecords(
                ReadRecordsRequest(SleepSessionRecord::class, TimeRangeFilter.between(from, Instant.now()))
            )
            val startOfToday = today.atStartOfDay(zone).toInstant()
            val sessions = resp.records.filter { it.endTime.isAfter(startOfToday) }
            if (sessions.isEmpty()) return null
            val intervals = sessions.flatMap { s ->
                if (s.stages.isEmpty()) {
                    listOf(s.startTime to s.endTime)
                } else {
                    s.stages.filter {
                        it.stage != SleepSessionRecord.STAGE_TYPE_AWAKE &&
                            it.stage != SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED &&
                            it.stage != SleepSessionRecord.STAGE_TYPE_OUT_OF_BED
                    }.map { it.startTime to it.endTime }
                }
            }.sortedBy { it.first }
            var totalMs = 0L
            var curS: Instant? = null
            var curE: Instant? = null
            for ((s, e) in intervals) {
                val cs = curS
                val ce = curE
                if (cs == null || ce == null) { curS = s; curE = e }
                else if (!s.isAfter(ce)) { if (e.isAfter(ce)) curE = e }
                else { totalMs += Duration.between(cs, ce).toMillis(); curS = s; curE = e }
            }
            val fs = curS
            val fe = curE
            if (fs != null && fe != null) totalMs += Duration.between(fs, fe).toMillis()
            if (totalMs <= 0) null else Math.round(totalMs / 3_600_000.0 * 10) / 10.0
        } catch (_: Exception) { null }
    }

    /** HRV RMSSD (ms): media de lecturas desde ayer 18:00 (noche + manana). */
    private suspend fun readHrv(): Double? {
        val c = client ?: return null
        return try {
            val from = ZonedDateTime.of(LocalDate.now(zone).minusDays(1), LocalTime.of(18, 0), zone).toInstant()
            val resp = c.readRecords(
                ReadRecordsRequest(HeartRateVariabilityRmssdRecord::class, TimeRangeFilter.between(from, Instant.now()))
            )
            if (resp.records.isEmpty()) null
            else Math.round(resp.records.map { it.heartRateVariabilityMillis }.average() * 10) / 10.0
        } catch (_: Exception) { null }
    }

    private suspend fun readRestingHr(): Int? {
        val c = client ?: return null
        return try {
            val from = Instant.now().minus(Duration.ofDays(3))
            val resp = c.readRecords(
                ReadRecordsRequest(
                    RestingHeartRateRecord::class, TimeRangeFilter.between(from, Instant.now()),
                    ascendingOrder = false, pageSize = 1,
                )
            )
            resp.records.firstOrNull()?.beatsPerMinute?.toInt()
        } catch (_: Exception) { null }
    }

    private suspend fun readLatestWeight(): Double? {
        val c = client ?: return null
        return try {
            val from = Instant.now().minus(Duration.ofDays(365))
            val resp = c.readRecords(
                ReadRecordsRequest(
                    WeightRecord::class, TimeRangeFilter.between(from, Instant.now()),
                    ascendingOrder = false, pageSize = 1,
                )
            )
            resp.records.firstOrNull()?.weight?.inKilograms
        } catch (_: Exception) { null }
    }

    suspend fun writeWeight(kg: Double) {
        val c = client ?: return
        if (kg <= 0.0) return
        val now = Instant.now()
        c.insertRecords(
            listOf(WeightRecord(time = now, zoneOffset = zone.rules.getOffset(now), weight = Mass.kilograms(kg)))
        )
    }

    fun requestContract() = PermissionController.createRequestPermissionResultContract()

    // ---- Preferencia local "healthConnectEnabled" (SharedPreferences "voltbody_prefs") ----
    fun isEnabled(): Boolean = appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_ENABLED, false)
    fun setEnabled(v: Boolean) {
        appCtx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY_ENABLED, v).apply()
    }

    companion object {
        private const val PREFS = "voltbody_prefs"
        private const val KEY_ENABLED = "healthConnectEnabled"
    }
}

val Context.healthManager: HealthConnectManager get() = HealthConnectManager(this)
