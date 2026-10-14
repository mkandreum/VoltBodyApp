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
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.health.connect.client.units.Mass
import com.voltbody.app.data.DailyMetric
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Period
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

    /**
     * Permiso opcional para leer mas de 30 dias hacia atras (Health Connect por defecto solo deja
     * leer los 30 dias previos a la primera concesion). Se pide junto al resto; si el dispositivo
     * no lo soporta simplemente no se concede.
     */
    val historyPermission = "android.permission.health.READ_HEALTH_DATA_HISTORY"

    /** Lo que se pide al usuario: los permisos base + el de historico. */
    val requestedPermissions: Set<String> get() = permissions + historyPermission

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

    /**
     * Metricas diarias de [start]..[end] (ambos inclusive) desde Health Connect, lista para volcar al servidor.
     * Solo incluye los campos con permiso concedido y con datos (> 0). 4 lecturas en total (agregado diario + sueno + HRV + FC reposo).
     */
    suspend fun readDaily(start: LocalDate, end: LocalDate): Map<String, DailyMetric> {
        val c = client ?: return emptyMap()
        val g = granted()
        val from = start.atStartOfDay(zone).toInstant()
        val to = end.plusDays(1).atStartOfDay(zone).toInstant()
        val range = TimeRangeFilter.between(from, to)
        // aggregateGroupByPeriod (Period = dias de calendario) EXIGE un rango en hora local:
        // con Instant lanza IllegalArgumentException y no se importaba ningun dia de pasos/kcal.
        val localRange = TimeRangeFilter.between(start.atStartOfDay(), end.plusDays(1).atStartOfDay())
        val out = LinkedHashMap<String, DailyMetric>()
        fun at(date: String) = out[date] ?: DailyMetric(date)

        // 1) Agregados por dia (deduplican entre fuentes: Samsung Health, Google Fit...).
        val metrics = buildSet {
            if (g.canRead(StepsRecord::class)) add(StepsRecord.COUNT_TOTAL)
            if (g.canRead(DistanceRecord::class)) add(DistanceRecord.DISTANCE_TOTAL)
            if (g.canRead(ActiveCaloriesBurnedRecord::class)) add(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL)
            if (g.canRead(TotalCaloriesBurnedRecord::class)) add(TotalCaloriesBurnedRecord.ENERGY_TOTAL)
            if (g.canRead(HeartRateRecord::class)) add(HeartRateRecord.BPM_AVG)
        }
        if (metrics.isNotEmpty()) {
            runCatching {
                c.aggregateGroupByPeriod(AggregateGroupByPeriodRequest(metrics, localRange, Period.ofDays(1)))
            }.getOrNull()?.forEach { row ->
                val date = row.startTime.toLocalDate().toString()
                val steps = row.result[StepsRecord.COUNT_TOTAL] ?: 0L
                val dist = row.result[DistanceRecord.DISTANCE_TOTAL]?.inKilometers ?: 0.0
                val active = row.result[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories ?: 0.0
                val total = row.result[TotalCaloriesBurnedRecord.ENERGY_TOTAL]?.inKilocalories ?: 0.0
                val avgHr = row.result[HeartRateRecord.BPM_AVG]?.toInt()
                out[date] = at(date).copy(
                    steps = steps.toInt().takeIf { it > 0 },
                    distanceKm = dist.takeIf { it > 0.0 },
                    activeKcal = active.takeIf { it > 0.0 },
                    totalKcal = total.takeIf { it > 0.0 },
                    avgHr = avgHr?.takeIf { it > 0 },
                )
            }
        }

        // 2) Sueno por dia: sesiones que terminan ese dia, intervalos fusionados (excluye "despierto").
        if (g.canRead(SleepSessionRecord::class)) {
            runCatching {
                // Las sesiones que terminan el primer dia empiezan la noche anterior.
                readAll(SleepSessionRecord::class, TimeRangeFilter.between(from.minus(Duration.ofHours(14)), to))
            }.getOrNull()?.filter { !it.endTime.isBefore(from) }?.let { sessions ->
                val byDay = HashMap<String, MutableList<Pair<Instant, Instant>>>()
                for (s in sessions) {
                    val intervals = if (s.stages.isEmpty()) {
                        listOf(s.startTime to s.endTime)
                    } else {
                        s.stages.filter {
                            it.stage != SleepSessionRecord.STAGE_TYPE_AWAKE &&
                                it.stage != SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED &&
                                it.stage != SleepSessionRecord.STAGE_TYPE_OUT_OF_BED
                        }.map { it.startTime to it.endTime }
                    }
                    val day = s.endTime.atZone(zone).toLocalDate().toString()
                    byDay.getOrPut(day) { mutableListOf() }.addAll(intervals)
                }
                for ((date, intervals) in byDay) mergedHours(intervals)?.let { out[date] = at(date).copy(sleepHours = it) }
            }
        }

        // 3) HRV por dia (media de las lecturas de ese dia).
        if (g.canRead(HeartRateVariabilityRmssdRecord::class)) {
            runCatching {
                readAll(HeartRateVariabilityRmssdRecord::class, range)
            }.getOrNull()?.groupBy { it.time.atZone(zone).toLocalDate().toString() }?.forEach { (date, recs) ->
                val avg = recs.map { it.heartRateVariabilityMillis }.average()
                if (avg > 0) out[date] = at(date).copy(hrv = Math.round(avg * 10) / 10.0)
            }
        }

        // 4) FC en reposo por dia (ultima lectura del dia).
        if (g.canRead(RestingHeartRateRecord::class)) {
            runCatching {
                readAll(RestingHeartRateRecord::class, range)
            }.getOrNull()?.groupBy { it.time.atZone(zone).toLocalDate().toString() }?.forEach { (date, recs) ->
                val bpm = recs.last().beatsPerMinute.toInt()
                if (bpm > 0) out[date] = at(date).copy(restingHr = bpm)
            }
        }

        return out
    }

    /** Lee todas las paginas de un tipo de registro (readRecords devuelve como mucho 1000 por pagina). */
    private suspend fun <T : Record> readAll(type: KClass<T>, range: TimeRangeFilter, maxPages: Int = 20): List<T> {
        val c = client ?: return emptyList()
        val out = ArrayList<T>()
        var token: String? = null
        var pages = 0
        do {
            val resp = c.readRecords(ReadRecordsRequest(type, range, ascendingOrder = true, pageSize = 1000, pageToken = token))
            out += resp.records
            token = resp.pageToken
            pages++
        } while (token != null && pages < maxPages)
        return out
    }

    /** Peso por dia (ultima lectura de cada dia) en [start]..[end], en kg con 1 decimal. */
    suspend fun readDailyWeights(start: LocalDate, end: LocalDate): Map<String, Double> {
        if (client == null || !granted().canRead(WeightRecord::class)) return emptyMap()
        val range = TimeRangeFilter.between(start.atStartOfDay(zone).toInstant(), end.plusDays(1).atStartOfDay(zone).toInstant())
        return runCatching { readAll(WeightRecord::class, range) }.getOrDefault(emptyList())
            .groupBy { it.time.atZone(zone).toLocalDate().toString() }
            .mapValues { (_, recs) -> Math.round(recs.last().weight.inKilograms * 10) / 10.0 }
            .filterValues { it > 0 }
    }

    /** Fusiona intervalos solapados y devuelve las horas totales (1 decimal) o null si <= 0. */
    private fun mergedHours(intervals: List<Pair<Instant, Instant>>): Double? {
        if (intervals.isEmpty()) return null
        var totalMs = 0L
        var cs: Instant? = null
        var ce: Instant? = null
        for ((s, e) in intervals.sortedBy { it.first }) {
            val a = cs
            val b = ce
            if (a == null || b == null) { cs = s; ce = e }
            else if (!s.isAfter(b)) { if (e.isAfter(b)) ce = e }
            else { totalMs += Duration.between(a, b).toMillis(); cs = s; ce = e }
        }
        val a = cs
        val b = ce
        if (a != null && b != null) totalMs += Duration.between(a, b).toMillis()
        return if (totalMs <= 0) null else Math.round(totalMs / 3_600_000.0 * 10) / 10.0
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
