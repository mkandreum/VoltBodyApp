package com.voltbody.app.data.remote

import com.voltbody.app.domain.model.ExerciseLibraryEntry
import com.voltbody.app.domain.model.ExerciseType

object ExerciseLibrary {
    val ITEMS = listOf(
        ExerciseLibraryEntry("bench_press", "Press de Banca", "Bench Press", muscleGroup = "Pecho"),
        ExerciseLibraryEntry("squat", "Sentadilla", "Squat", muscleGroup = "Piernas"),
        ExerciseLibraryEntry("deadlift", "Peso Muerto", "Deadlift", muscleGroup = "Espalda/Piernas"),
        ExerciseLibraryEntry("overhead_press", "Press Militar", "Overhead Press", muscleGroup = "Hombros"),
        ExerciseLibraryEntry("pull_ups", "Dominadas", "Pull Ups", muscleGroup = "Espalda", exerciseType = ExerciseType.BODYWEIGHT),
        ExerciseLibraryEntry("barbell_row", "Remo con Barra", "Barbell Row", muscleGroup = "Espalda"),
        ExerciseLibraryEntry("bicep_curl", "Curl de Bíceps", "Bicep Curl", muscleGroup = "Bíceps"),
        ExerciseLibraryEntry("tricep_extension", "Extensión de Tríceps", "Tricep Extension", muscleGroup = "Tríceps"),
        ExerciseLibraryEntry("leg_press", "Prensa de Piernas", "Leg Press", muscleGroup = "Piernas"),
        ExerciseLibraryEntry("lat_pulldown", "Jalón al Pecho", "Lat Pulldown", muscleGroup = "Espalda")
    )
}
