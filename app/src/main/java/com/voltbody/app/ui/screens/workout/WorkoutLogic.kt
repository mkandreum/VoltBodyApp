package com.voltbody.app.ui.screens.workout

import com.voltbody.app.data.Exercise
import com.voltbody.app.data.WorkoutDay
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Locale

const val FALLBACK_IMG = "https://images.unsplash.com/photo-1534438327276-14e5300c3a48?q=80&w=400&auto=format&fit=crop"

const val T_WEIGHTED = "weighted"
const val T_ISOMETRIC = "isometric"
const val T_BODYWEIGHT = "bodyweight"
const val T_CARDIO = "cardio"

private val ISOMETRIC_KEYWORDS = listOf("plancha", "frog stand", "isométrica", "wall sit", "estática", "side plank")
private val BODYWEIGHT_KEYWORDS = listOf("dominadas", "fondos", "flexiones", "burpees", "pull-up", "dip")
private val CARDIO_KEYWORDS = listOf("cardio", "cuerda", "correr", "bicicleta", "remo ergómetro", "caminar", "elíptica")

/** Detector por palabras clave cuando el ejercicio no trae exerciseType. */
fun detectExerciseType(name: String): String {
    val lower = name.lowercase()
    if (ISOMETRIC_KEYWORDS.any { lower.contains(it) }) return T_ISOMETRIC
    if (BODYWEIGHT_KEYWORDS.any { lower.contains(it) }) return T_BODYWEIGHT
    if (CARDIO_KEYWORDS.any { lower.contains(it) }) return T_CARDIO
    return T_WEIGHTED
}

/** Tipo efectivo: el del ejercicio, si no el de la biblioteca, si no por palabras clave. */
fun exerciseTypeOf(ex: Exercise): String =
    ex.exerciseType?.takeIf { it.isNotBlank() } ?: libraryEntryById(ex.id)?.exerciseType ?: detectExerciseType(ex.name)

fun techniqueOf(ex: Exercise): String? =
    (libraryEntryById(ex.id)?.technique ?: ex.technique)?.takeIf { it.isNotBlank() }

fun isMockOrEmptyGifUrl(url: String?): Boolean {
    if (url.isNullOrBlank()) return true
    val s = url.lowercase()
    return s.contains("exercisedb.dev") || s.contains("unsplash.com") || s.contains("placeholder") || s.contains("mock")
}

fun routineNeedsEnrichment(routine: List<WorkoutDay>): Boolean =
    routine.any { d -> d.exercises.any { isMockOrEmptyGifUrl(it.gifUrl) } }

/** mm:ss */
fun formatDuration(seconds: Int): String = "%02d:%02d".format(Locale.US, seconds / 60, seconds % 60)

/** Numero sin decimales superfluos: 20.0 -> "20", 22.5 -> "22.5" */
fun fmtNum(d: Double): String = if (d % 1.0 == 0.0) d.toLong().toString() else "%.1f".format(Locale.US, d).trimEnd('0').trimEnd('.')

fun String.toD(): Double = replace(',', '.').toDoubleOrNull() ?: 0.0

fun nowIso(offsetMs: Long = 0): String = Instant.now().truncatedTo(ChronoUnit.MILLIS).plusMillis(offsetMs).toString()

const val XP_PER_LOG = 12
const val XP_PER_STREAK_DAY = 8
const val XP_PER_LEVEL = 250

/** Sesion en curso: titulo + ejercicios + posicion inicial. */
data class SessionSpec(val title: String, val exercises: List<Exercise>, val startIndex: Int = 0)

/** Series objetivo de un ejercicio (al menos 1). */
fun Exercise.targetSets(): Int = sets.coerceAtLeast(1)
