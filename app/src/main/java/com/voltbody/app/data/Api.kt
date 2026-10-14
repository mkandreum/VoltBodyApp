package com.voltbody.app.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.contentOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.TimeUnit

class ApiException(message: String, val code: Int = 0) : Exception(message)

/** Cliente HTTP mínimo contra el servidor de la web (/api). */
class Api(private val baseUrl: () -> String, private val token: () -> String?, private val onUnauthorized: () -> Unit) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS) // la IA puede tardar
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    private val json = "application/json".toMediaType()

    /** Devuelve el cuerpo ya parseado. Lanza ApiException con el mensaje del servidor (details/error). */
    suspend fun request(method: String, path: String, body: JsonElement? = null, auth: Boolean = true): JsonElement =
        withContext(Dispatchers.IO) {
            val url = baseUrl().trimEnd('/') + "/api" + path
            val rb = Request.Builder().url(url)
            if (auth) token()?.let { rb.header("Authorization", "Bearer $it") }
            val payload = (body?.toString() ?: "{}").toRequestBody(json)
            when (method) {
                "GET" -> rb.get()
                "POST" -> rb.post(payload)
                "PUT" -> rb.put(payload)
                "DELETE" -> rb.delete()
            }
            try {
                client.newCall(rb.build()).execute().use { resp ->
                    val text = resp.body?.string().orEmpty()
                    if (!resp.isSuccessful) {
                        if (resp.code == 401 && auth && !path.startsWith("/auth")) onUnauthorized()
                        val msg = runCatching {
                            val o = AppJson.parseToJsonElement(text).jsonObject
                            (o["details"] as? kotlinx.serialization.json.JsonPrimitive)?.contentOrNull
                                ?: o["error"]?.jsonPrimitive?.contentOrNull
                        }.getOrNull() ?: "Error HTTP ${resp.code}"
                        throw ApiException(msg, resp.code)
                    }
                    if (text.isBlank()) JsonObject(emptyMap()) else AppJson.parseToJsonElement(text)
                }
            } catch (e: ApiException) {
                throw e
            } catch (e: Exception) {
                throw ApiException("Sin conexión con el servidor (${e.message ?: "error de red"})")
            }
        }

    suspend inline fun <reified T> get(path: String): T = AppJson.decodeFromJsonElement<T>(request("GET", path))
    suspend inline fun <reified T> post(path: String, body: JsonElement?, auth: Boolean = true): T =
        AppJson.decodeFromJsonElement<T>(request("POST", path, body, auth))
}
