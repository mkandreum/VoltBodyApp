package com.voltbody.app.util

import com.voltbody.app.data.AppState
import com.voltbody.app.data.Exercise
import com.voltbody.app.data.WorkoutDay
import com.voltbody.app.data.WorkoutLog
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.ZoneId

class AchievementsTest {
    private val zone = ZoneId.systemDefault()
    private fun at(d: LocalDate, h: Int) = d.atTime(h, 0).atZone(zone).toInstant().toString()

    private fun state(logs: List<WorkoutLog>, monday: LocalDate) = AppState(
        routine = listOf(
            WorkoutDay("Lunes", "Torso", listOf(Exercise(id = "press", sets = 2), Exercise(id = "plank", sets = 1, exerciseType = "isometric"))),
        ),
        logs = logs,
    )

    @Test fun exerciseAndWorkoutCompletion() {
        val monday = mondayOf(LocalDate.now())
        val oneSet = listOf(WorkoutLog(at(monday, 10), "press", 50.0, 10))
        val ids1 = evaluateAchievements(state(oneSet, monday)).map { it.id }
        assertFalse("exercise-done" in ids1)

        val full = oneSet + WorkoutLog(at(monday, 10), "press", 50.0, 10) + WorkoutLog(at(monday, 10), "plank", 0.0, 1, duration = 65)
        val ids2 = evaluateAchievements(state(full, monday)).map { it.id }
        assertTrue("exercise-done" in ids2)
        assertTrue("workout-1" in ids2)
        assertTrue("plank-60" in ids2)
        assertTrue("volume-1t" in ids2) // 2 x 50 kg x 10 = 1000 kg
    }

    @Test fun scheduleAndWeekend() {
        val sat = mondayOf(LocalDate.now()).minusDays(2)
        val logs = listOf(WorkoutLog(at(sat, 7), "press", 20.0, 5), WorkoutLog(at(sat.plusDays(1), 23), "press", 20.0, 5))
        val ids = evaluateAchievements(state(logs, sat)).map { it.id }
        assertTrue("early-bird" in ids)
        assertTrue("night-owl" in ids)
        assertTrue("weekend-warrior" in ids)
    }
}
