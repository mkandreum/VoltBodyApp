package com.voltbody.app.data

import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Reproduce login / registro / refreshAll contra un servidor falso con las MISMAS respuestas que
 * devuelve el backend real (capturadas de https://voltbody.xyoncloud.win). Garantiza que el parseo
 * de la sesión y del perfil no lanza excepciones sin mensaje ("Error al conectar").
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RepositoryAuthTest {

    private lateinit var server: MockWebServer
    private lateinit var repo: Repository

    private val registerBody = """
        {"token":"tok-123","user":{"id":"u1","email":"a@b.co","name":"Test VB"},
         "profile":{"id":"p1","userId":"u1","age":30,"weight":80.5,"height":180,"gender":"male",
          "goal":"ganar musculo","goalDirection":"Perder","goalTargetKg":5,"goalTimelineMonths":3,
          "currentState":"normal","schedule":"noche","trainingDaysPerWeek":4,"sessionMinutes":45,
          "workHours":"9-18","theme":"verde-negro","motivationPhrase":"Dale duro","motivationPhoto":null,
          "mealTimes":{"lunch":"14:00","snack":"17:00","brunch":"11:00","dinner":"21:00","breakfast":"08:00"},
          "foodPreferences":{"carbs":["b"],"proteins":["c"],"vegetables":["a"]},
          "weeklySpecialSession":{"day":"Sabado","enabled":true,"activity":"Zumba","durationMinutes":60},
          "specialDish":{"ingredients":"pollo","targetCalories":500},
          "avatarConfig":{"bodyFat":20,"muscleMass":50},
          "routine":[{"day":"Lunes","focus":"Pecho","exercises":[{"id":"e1","name":"Press","reps":"10","sets":3,"gifUrl":"","weight":40,"muscleGroup":"Pecho"}]}],
          "diet":{"meals":[{"id":"m1","fat":15,"name":"Desayuno","time":"08:00","carbs":50,"protein":30,"calories":500,"description":"avena"}],"macros":{"fat":60,"carbs":200,"protein":150},"dailyCalories":2000},
          "insights":{"dailyQuote":"quote","estimatedResults":"bien","sleepRecommendation":"dormir 8h"},
          "profilePhoto":null,"notificationsEnabled":false}}
    """.trimIndent()

    private val profileBody = """
        {"id":"p1","userId":"u1","age":30,"weight":80.5,"height":180,"gender":"male","goal":"ganar musculo",
         "goalDirection":"Perder","goalTargetKg":5,"goalTimelineMonths":3,"currentState":"normal","schedule":"noche",
         "trainingDaysPerWeek":4,"sessionMinutes":45,"workHours":"9-18","theme":"verde-negro","motivationPhrase":"Dale duro",
         "motivationPhoto":null,"mealTimes":{"lunch":"14:00","snack":"17:00","brunch":"11:00","dinner":"21:00","breakfast":"08:00"},
         "foodPreferences":{"carbs":["b"],"proteins":["c"],"vegetables":["a"]},
         "weeklySpecialSession":{"day":"Sabado","enabled":true,"activity":"Zumba","durationMinutes":60},
         "specialDish":{"ingredients":"pollo","targetCalories":500},"avatarConfig":{"bodyFat":20,"muscleMass":50},
         "routine":[{"day":"Lunes","focus":"Pecho","exercises":[{"id":"e1","name":"Press","reps":"10","sets":3,"gifUrl":"","weight":40,"muscleGroup":"Pecho"}]}],
         "diet":{"meals":[{"id":"m1","fat":15,"name":"Desayuno","time":"08:00","carbs":50,"protein":30,"calories":500,"description":"avena"}],"macros":{"fat":60,"carbs":200,"protein":150},"dailyCalories":2000},
         "insights":{"dailyQuote":"quote","estimatedResults":"bien","sleepRecommendation":"dormir 8h"},"profilePhoto":null,"notificationsEnabled":false}
    """.trimIndent()

    private val dataAllBody = """{"metrics":[],"mealLogs":[],"goals":[],"achievements":[],"customWorkout":[],"recovery":[]}"""

    /** Login con campos numéricos como TEXTO (Prisma/IA a veces serializan así) y anidados válidos. */
    private val numericStringsBody = """
        {"token":"tok-num","user":{"id":"u1","email":"a@b.co","name":"Num"},
         "profile":{"id":"p1","userId":"u1","age":"30","weight":"80.5","height":"180","gender":"male",
          "goal":"ganar musculo","goalDirection":"Perder","goalTargetKg":"5","goalTimelineMonths":"3",
          "currentState":"normal","schedule":"noche","trainingDaysPerWeek":"4","sessionMinutes":"45","workHours":"9-18",
          "theme":"verde-negro","motivationPhrase":"Dale duro","motivationPhoto":null,
          "mealTimes":{"lunch":"14:00","snack":"17:00","brunch":"11:00","dinner":"21:00","breakfast":"08:00"},
          "foodPreferences":{"carbs":["b"],"proteins":["c"],"vegetables":["a"]},
          "weeklySpecialSession":{"day":"Sabado","enabled":true,"activity":"Zumba","durationMinutes":60},
          "specialDish":{"ingredients":"pollo","targetCalories":500},"avatarConfig":{"bodyFat":20,"muscleMass":50},
          "routine":[{"day":"Lunes","focus":"Pecho","exercises":[{"id":"e1","name":"Press","reps":"10","sets":3,"gifUrl":"","weight":40,"muscleGroup":"Pecho"}]}],
          "diet":{"meals":[{"id":"m1","fat":15,"name":"Desayuno","time":"08:00","carbs":50,"protein":30,"calories":500,"description":"avena"}],"macros":{"fat":60,"carbs":200,"protein":150},"dailyCalories":2000},
          "insights":{"dailyQuote":"quote","estimatedResults":"bien","sleepRecommendation":"dormir"},"profilePhoto":null,"notificationsEnabled":false}}
    """.trimIndent()

    /** Perfil con objetos anidados de tipo inesperado: no debe lanzar ni perder el plan válido. */
    private val malformedNestedProfile = """
        {"id":"p2","userId":"u2","age":30,"weight":80.5,"height":180,
         "mealTimes":"08:00-21:00","avatarConfig":[1,2,3],
         "specialDish":{"ingredients":"pollo","targetCalories":"500"},
         "weeklySpecialSession":{"enabled":"maybe","activity":"Zumba","day":"Sabado","durationMinutes":"60"},
         "routine":[{"day":"Lunes","focus":"Pecho","exercises":[{"id":"e1","name":"Press","reps":"10","sets":3,"gifUrl":"","weight":40,"muscleGroup":"Pecho"}]}],
         "diet":{"meals":[{"id":"m1","fat":15,"name":"Desayuno","time":"08:00","carbs":50,"protein":30,"calories":500,"description":"avena"}],"macros":{"fat":60,"carbs":200,"protein":150},"dailyCalories":2000}}
    """.trimIndent()

    private val malformedNestedBody =
        """{"token":"tok-mal","user":{"id":"u2","email":"a@b.co","name":"Mal"},"profile":$malformedNestedProfile}"""

    private val partialLoginBody = """{"token":"tok-par","user":{"id":"u3","email":"a@b.co","name":"Parcial"}}"""

    private fun json(code: Int, body: String) = MockResponse().setResponseCode(code)
        .addHeader("Content-Type", "application/json").setBody(body)

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        repo = Repository(ApplicationProvider.getApplicationContext())
        repo.setServerUrl(server.url("/").toString())
    }

    @After
    fun tearDown() {
        runCatching { server.shutdown() }
    }

    private fun defaultDispatcher(register: String = registerBody, profile: String = profileBody) = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
            "/api/auth/register", "/api/auth/login" -> json(200, register)
            "/api/profile" -> json(200, profile)
            "/api/workout/logs", "/api/workout/photos", "/api/workout/weight-logs" -> json(200, "[]")
            "/api/data/all" -> json(200, dataAllBody)
            else -> json(404, """{"error":"not found"}""")
        }
    }

    /** Fotos/pesos con filas malformadas mezcladas con válidas: antes hacían `!!` → NPE sin mensaje. */
    private fun malformedRowsDispatcher() = object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse = when (request.path) {
            "/api/auth/login", "/api/auth/register" -> json(200, registerBody)
            "/api/profile" -> json(200, profileBody)
            "/api/workout/photos" -> json(200,
                """[{},{"url":"solo-url"},{"date":"solo-fecha"},{"date":"2026-01-02T10:00:00.000Z","url":"data:image/png;base64,AAA"}]""")
            "/api/workout/weight-logs" -> json(200,
                """[{},{"date":"2026-01-02T00:00:00.000Z"},{"weight":90},{"date":"2026-01-02T00:00:00.000Z","weight":"80.5"}]""")
            "/api/workout/logs" -> json(200, "[]")
            "/api/data/all" -> json(200, dataAllBody)
            else -> json(404, """{"error":"not found"}""")
        }
    }

    @Test
    fun register_hydrates_session_and_profile() = runBlocking {
        server.dispatcher = defaultDispatcher()
        repo.register("a@b.co", "testpass123", "Test VB")

        val st = repo.state.value
        assertTrue(st.isAuthenticated)
        assertEquals("tok-123", st.authToken)
        assertEquals("u1", st.user?.id)
        assertEquals("Test VB", st.profile.name)
        assertEquals(30, st.profile.age)
        assertEquals("verde-negro", st.theme)
        assertTrue(st.isOnboarded) // hay rutina + dieta
        assertEquals(1, st.routine.size)
        assertEquals(2000, st.diet?.dailyCalories)
    }

    @Test
    fun login_with_bad_credentials_surfaces_server_message() {
        server.enqueue(json(401, """{"error":"Invalid credentials"}"""))
        val e = runCatching { runBlocking { repo.login("a@b.co", "bad") } }.exceptionOrNull()
        assertTrue(e is ApiException)
        assertEquals("Invalid credentials", e?.message)
        assertEquals(401, (e as ApiException).code)
        assertFalse(repo.state.value.isAuthenticated)
    }

    @Test
    fun login_with_body_missing_token_shows_clear_message() {
        server.enqueue(json(200, """{"user":{"id":"u1","email":"a@b.co"}}"""))
        val e = runCatching { runBlocking { repo.login("a@b.co", "ok") } }.exceptionOrNull()
        assertTrue(e is ApiException)
        assertTrue("mensaje no debe quedar vacío", !e!!.message.isNullOrBlank())
        assertFalse(repo.state.value.isAuthenticated)
    }

    @Test
    fun refreshAll_parses_full_profile_without_crashing() = runBlocking {
        server.dispatcher = defaultDispatcher()
        repo.login("a@b.co", "testpass123")
        val st = repo.state.value
        assertTrue(st.isAuthenticated)
        assertEquals("Dale duro", st.motivationPhrase)
        assertEquals(4, st.profile.trainingDaysPerWeek)
        assertEquals(50.0, st.profile.avatarConfig?.muscleMass ?: -1.0, 0.001)
        assertTrue(st.metrics.isEmpty())
    }

    @Test
    fun login_accepts_numeric_fields_serialized_as_strings() = runBlocking {
        server.dispatcher = defaultDispatcher(register = numericStringsBody)
        repo.login("a@b.co", "testpass123")
        val st = repo.state.value
        assertTrue(st.isAuthenticated)
        assertEquals(30, st.profile.age)
        assertEquals(80.5, st.profile.weight ?: -1.0, 0.001)
        assertEquals(4, st.profile.trainingDaysPerWeek)
        assertEquals(45, st.profile.sessionMinutes)
        assertEquals(5.0, st.profile.goalTargetKg ?: -1.0, 0.001)
        assertTrue(st.isOnboarded)
    }

    @Test
    fun login_tolerates_malformed_nested_profile_objects() = runBlocking {
        server.dispatcher = defaultDispatcher(register = malformedNestedBody, profile = malformedNestedProfile)
        repo.login("a@b.co", "testpass123")
        val st = repo.state.value
        assertTrue(st.isAuthenticated)
        assertEquals(30, st.profile.age)
        assertEquals(MealTimes(), st.profile.mealTimes)
        assertEquals(AvatarConfig(0.5, 0.5), st.profile.avatarConfig)
        // targetCalories viene como texto, pero AppJson es lenient: si se decodifica bien, se conserva.
        assertEquals(SpecialDish(ingredients = "pollo", targetCalories = 500), st.profile.specialDish)
        assertEquals(WeeklySpecialSession(false, "Zumba", "Sábado", 60), st.profile.weeklySpecialSession)
        assertTrue(st.isOnboarded) // la rutina y la dieta válidas se conservan
    }

    @Test
    fun refreshAll_skips_malformed_photo_and_weight_rows() = runBlocking {
        server.dispatcher = malformedRowsDispatcher()
        repo.login("a@b.co", "testpass123")
        val st = repo.state.value
        assertTrue(st.isAuthenticated)
        assertEquals(1, st.progressPhotos.size)
        assertEquals("data:image/png;base64,AAA", st.progressPhotos[0].url)
        assertEquals(1, st.weightLogs.size)
        assertEquals("2026-01-02", st.weightLogs[0].date)
        assertEquals(80.5, st.weightLogs[0].weight, 0.001)
    }

    @Test
    fun login_without_profile_field_is_tolerated() = runBlocking {
        server.dispatcher = defaultDispatcher(register = partialLoginBody, profile = "{}")
        repo.login("a@b.co", "testpass123")
        val st = repo.state.value
        assertTrue(st.isAuthenticated)
        assertFalse(st.isOnboarded)
        assertEquals("Parcial", st.profile.name)
        assertEquals(3, st.profile.trainingDaysPerWeek)
    }

    @Test
    fun login_network_failure_returns_clear_message() {
        server.shutdown()
        val e = runCatching { runBlocking { repo.login("a@b.co", "testpass123") } }.exceptionOrNull()
        assertTrue(e is ApiException)
        assertTrue("mensaje no debe quedar vacío", !e!!.message.isNullOrBlank())
        assertFalse(repo.state.value.isAuthenticated)
    }
}
