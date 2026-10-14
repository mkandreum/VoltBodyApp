package com.voltbody.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import com.voltbody.app.data.*
import com.voltbody.app.ui.screens.*
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

/** Renderiza pantallas a PNG en la JVM (sin emulador) para revisar el diseno: build/shots (PNG) */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = "w411dp-h891dp-xxhdpi")
class ScreenShots {
    @get:Rule val rule = createComposeRule()

    private fun demoRepo(): Repository {
        val repo = Repository(ApplicationProvider.getApplicationContext())
        val today = LocalDate.now()
        fun ex(i: Int, n: String, m: String, t: String? = null) = Exercise(id = "e$i", name = n, sets = 4, reps = "8-12", weight = 40.0, muscleGroup = m, exerciseType = t, technique = "1. Colócate.\n2. Baja controlado.\n3. Sube explosivo.")
        val routine = listOf(
            WorkoutDay("Lunes", "Pecho y tríceps", listOf(ex(1, "Press de banca", "Pecho"), ex(2, "Press inclinado con mancuernas", "Pecho"), ex(3, "Fondos en paralelas", "Pecho", "bodyweight"), ex(4, "Extensión de tríceps en polea", "Tríceps"))),
            WorkoutDay("Martes", "Espalda y bíceps", listOf(ex(5, "Remo con barra", "Espalda"), ex(6, "Jalón al pecho", "Espalda"))),
            WorkoutDay("Jueves", "Piernas", listOf(ex(7, "Sentadilla goblet", "Piernas"), ex(8, "Plancha", "Core", "isometric"))),
        )
        val diet = DietPlan(2300, Macros(160, 250, 70), listOf(
            Meal("m1", "Avena con frutos rojos", "08:00", 480, 30, 60, 12, "Avena, yogur y fruta"),
            Meal("m2", "Pollo con arroz", "14:00", 720, 55, 80, 18, "Pechuga, arroz y verduras"),
            Meal("m3", "Salmón con boniato", "21:00", 650, 45, 50, 25, "Salmón al horno"),
        ), week = listOf("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado", "Domingo").mapIndexed { i, d ->
            DayMenu(d, listOf(
                Meal("d$i-1", "🥣 Avena con plátano", "08:00", 480, 30, 60, 12, "60 g copos de avena, 200 ml leche, 1 plátano, 10 g nueces. Mezcla y calienta."),
                Meal("d$i-2", if (i % 2 == 0) "🍗 Pollo con arroz" else "🐟 Merluza con patata", "14:00", 720, 55, 80, 18,
                    if (i % 2 == 0) "150 g pechuga de pollo, 80 g arroz basmati, 150 g brócoli, 10 g AOVE. Plancha." else "180 g merluza, 250 g patata, 100 g espinacas, 10 g AOVE. Al horno."),
                Meal("d$i-3", "🍳 Tortilla de espinacas", "21:00", 650, 45, 50, 25, "3 huevos, 100 g espinacas, 60 g pan integral, 30 g queso fresco. A la sartén."),
            ))
        })
        val metrics = (0..29).map { d ->
            DailyMetric(
                today.minusDays(d.toLong()).toString(), steps = 6000 + ((d * 1237) % 6000), distanceKm = 3.0 + d * 0.1, activeKcal = 300.0 + d * 4,
                waterMl = 1000 + (d % 7) * 150, sleepHours = 6.2 + ((d * 7) % 20) / 10.0, hrv = 48.0 + ((d * 5) % 17) - d * 0.2, restingHr = 58 + (d % 5) + d / 6,
            )
        }
        repo.update {
            it.copy(
                hydrated = true, authToken = "t", user = AuthUser("1", "a@b.co", "Daniel"), isOnboarded = true,
                profile = Profile(name = "Daniel", age = 30, weight = 78.0, height = 178.0, goal = "Ganar músculo", goalDirection = "Ganar", goalTargetKg = 4.0, goalTimelineMonths = 4, trainingDaysPerWeek = 4, sessionMinutes = 60),
                routine = routine, diet = diet, metrics = metrics,
                logs = listOf(WorkoutLog(today.toString() + "T10:00:00.000Z", "e1", 60.0, 10)),
                weightLogs = (0..29).map { d -> WeightLog(today.minusDays((29 - d).toLong()).toString(), 79.6 - d * 0.04 + (if (d % 3 == 0) 0.3 else -0.1)) },
                weeklyGoals = listOf(WeeklyGoal("habit-1", "Completar 3 sesiones de fuerza", true), WeeklyGoal("habit-2", "Dormir 7h al menos 5 días"), WeeklyGoal("habit-3", "Cumplir proteína diaria 5 días")),
                insights = Insights("Duerme 7-8 h", "Verás cambios en 6 semanas", "La disciplina vence al talento."),
            )
        }
        return repo
    }

    private fun shot(name: String, content: @Composable (Repository) -> Unit) {
        val repo = demoRepo()
        rule.mainClock.autoAdvance = false
        rule.setContent {
            val stack = androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateListOf<com.voltbody.app.ui.v3.Route>() }
            val nav = androidx.compose.runtime.remember { com.voltbody.app.ui.v3.Navigator(stack) {} }
            VoltTheme("verde-negro") {
                androidx.compose.runtime.CompositionLocalProvider(com.voltbody.app.ui.v3.LocalNav provides nav) {
                    Box(Modifier.fillMaxSize().background(Volt.bg)) { content(repo) }
                }
            }
        }
        rule.mainClock.advanceTimeBy(2500)
        rule.onRoot().captureRoboImage("build/shots/$name.png")
    }

    /** Hoja de cristal sobre una pantalla real (con desenfoque), como en la app. */
    private fun sheetShot(name: String, behind: @Composable (Repository) -> Unit, sheet: @Composable (Repository) -> Unit) = shot(name) { repo ->
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.fillMaxSize()) { behind(repo) }
            com.voltbody.app.ui.v3.GlassSheet(onDismiss = {}) { sheet(repo) }
        }
    }

    @Test fun shopping() = sheetShot("sheet_shopping", { com.voltbody.app.ui.v3.NutritionV3(it) }) { com.voltbody.app.ui.v3.ShoppingSheet(it) }
    @Test fun trends() = sheetShot("sheet_trends", { com.voltbody.app.ui.v3.ActivityV3(it) }) { com.voltbody.app.ui.v3.TrendsSheet(it) }
    @Test fun health() = sheetShot("sheet_health", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "health") }
    @Test fun s_personal() = sheetShot("sec_personal", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "personal") }
    @Test fun s_photos() = sheetShot("sec_photos", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "photos") }
    @Test fun s_achievements() = sheetShot("sec_achievements", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "achievements") }
    @Test fun s_habits() = sheetShot("sec_habits", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "habits") }
    @Test fun s_recovery() = sheetShot("sec_recovery", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "recovery") }
    @Test fun s_report() = sheetShot("sec_report", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "report") }
    @Test fun s_notifications() = sheetShot("sec_notifications", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "notifications") }
    @Test fun s_theme() = sheetShot("sec_theme", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "theme") }
    @Test fun s_server() = sheetShot("sec_server", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "server") }
    @Test fun s_data() = sheetShot("sec_data", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "data") }
    @Test fun s_diet() = sheetShot("sec_diet", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "diet") }
    @Test fun s_fatigue() = sheetShot("sec_fatigue", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "fatigue") }
    @Test fun s_library() = sheetShot("sec_library", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "library") }
    @Test fun weight() = sheetShot("sheet_weight", { com.voltbody.app.ui.v3.ProfileV3(it) }) { com.voltbody.app.ui.v3.SectionHost(it, "weight") }

    @Test fun login() = shot("login") { com.voltbody.app.ui.v3.LoginV3(it) }
    @Test fun home() = shot("home") { com.voltbody.app.ui.v3.HomeV3(it) }
    @Test fun activity() = shot("activity") { com.voltbody.app.ui.v3.ActivityV3(it) }
    @Test fun train() = shot("train") { com.voltbody.app.ui.v3.TrainV3(it) }
    @Test fun day() = shot("day") { com.voltbody.app.ui.v3.DayDetailV3(it, 0) }
    @Test fun session() = shot("session") { com.voltbody.app.ui.v3.SessionV3(it, com.voltbody.app.ui.v3.Route.Session(0)) }
    @Test fun nutrition() = shot("nutrition") { com.voltbody.app.ui.v3.NutritionV3(it) }
    @Test fun profile() = shot("profile") { com.voltbody.app.ui.v3.ProfileV3(it) }
}
