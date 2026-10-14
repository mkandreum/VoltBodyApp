package com.voltbody.app.ui.screens.profile

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.AppJson
import com.voltbody.app.data.AppState
import com.voltbody.app.data.Repository
import com.voltbody.app.health.HealthSnapshot
import com.voltbody.app.health.healthManager
import com.voltbody.app.ui.*
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.Notifications
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.util.concurrent.TimeUnit

private const val PREFS = "voltbody_prefs"
private const val HC_SDK_AVAILABLE = 3 // HealthConnectClient.SDK_AVAILABLE

@Composable
private fun StatusDot(ok: Boolean) {
    Box(Modifier.size(9.dp).clip(RoundedCornerShape(50)).background(if (ok) Volt.accent else Volt.textDim))
}

@Composable
private fun SmallButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, danger: Boolean = false, onClick: () -> Unit) {
    val c = if (danger) Volt.danger else Volt.accent
    Box(
        modifier.height(44.dp).clip(RoundedCornerShape(14.dp)).background(c.copy(alpha = if (enabled) 0.14f else 0.05f))
            .let { if (enabled) it.pressScale(onClick) else it },
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (enabled) c else c.copy(alpha = 0.4f), fontSize = 12.sp, fontWeight = FontWeight.Bold) }
}

/** Seccion "Salud": conexion con Health Connect (Samsung Health se sincroniza a traves de el). */
@Composable
fun HealthCard(repo: Repository, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { ctx.healthManager }
    val prefs = remember { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var enabled by remember { mutableStateOf(prefs.getBoolean("healthConnectEnabled", false)) }
    var availability by remember { mutableIntStateOf(runCatching { manager.availability() }.getOrDefault(1)) }
    var hasPerms by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<HealthSnapshot?>(null) }
    var sleep by remember { mutableStateOf<Double?>(null) }
    var busy by remember { mutableStateOf(false) }

    suspend fun refresh() {
        hasPerms = runCatching { manager.hasAllPermissions() }.getOrDefault(false)
        if (hasPerms) {
            snapshot = runCatching { manager.readToday() }.getOrNull()
            sleep = runCatching { manager.readSleepLastNight() }.getOrNull()
        }
    }
    LaunchedEffect(Unit) { availability = runCatching { manager.availability() }.getOrDefault(1); refresh() }

    val contract = remember(manager) { manager.requestContract() }
    val launcher = rememberLauncherForActivityResult(contract) { granted: Set<String> ->
        scope.launch {
            val all = granted.containsAll(manager.permissions)
            enabled = granted.isNotEmpty()
            prefs.edit().putBoolean("healthConnectEnabled", enabled).apply()
            refresh()
            if (all) repo.toast("success", "Health Connect conectado", "Pasos, sueño y peso listos para sincronizar.")
            else if (granted.isNotEmpty()) repo.toast("info", "Permisos parciales", "Algunos datos no estarán disponibles.")
            else repo.toast("error", "Permiso denegado", "Puedes concederlo desde los ajustes de Health Connect.")
        }
    }

    val available = availability == HC_SDK_AVAILABLE
    val connected = enabled && hasPerms
    val today = remember { LocalDate.now().toString() }

    VoltCard(modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Health Connect", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Pasos, sueño, HRV, calorías y peso", color = Volt.textDim, fontSize = 11.sp)
            }
            StatusDot(connected)
            Spacer(Modifier.width(6.dp))
            Text(if (connected) "Conectado" else "No conectado", color = if (connected) Volt.accent else Volt.textDim, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            "Samsung Health se sincroniza con VoltBody a través de Health Connect: activa la sincronización con Health Connect en Samsung Health (Ajustes > Health Connect) y concede aquí los permisos.",
            color = Volt.textDim, fontSize = 11.sp, lineHeight = 16.sp,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Volt.surface2.copy(alpha = 0.6f)).padding(10.dp),
        )
        Spacer(Modifier.height(12.dp))

        if (!available) {
            Text(
                if (availability == 2) "Health Connect necesita una actualización para funcionar." else "Health Connect no está instalado o no es compatible con este dispositivo.",
                color = Volt.orange, fontSize = 12.sp,
            )
            Spacer(Modifier.height(10.dp))
            SmallButton(if (availability == 2) "Actualizar Health Connect" else "Instalar Health Connect", Modifier.fillMaxWidth()) {
                val pkg = "com.google.android.apps.healthdata"
                val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                runCatching { ctx.startActivity(market) }.onFailure {
                    runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                }
            }
            return@VoltCard
        }

        if (!connected) {
            PrimaryButton("Conectar Health Connect", onClick = { launcher.launch(manager.permissions) })
        } else {
            val s = snapshot
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatPill("Pasos hoy", s?.steps?.toString() ?: "--", Modifier.weight(1f))
                StatPill("Sueño", sleep?.let { "%.1f h".format(it) } ?: "--", Modifier.weight(1f), color = Volt.water)
                StatPill("Peso HC", s?.weightKg?.let { "%.1f kg".format(it) } ?: "--", Modifier.weight(1f), color = Volt.orange)
            }
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("Sincronizar peso", Modifier.weight(1f), enabled = !busy) {
                    scope.launch {
                        busy = true
                        val snap = runCatching { manager.readToday() }.getOrNull().also { snapshot = it }
                        val kg = snap?.weightKg
                        if (kg != null && kg in 20.0..400.0) {
                            repo.addWeight(today, Math.round(kg * 10) / 10.0)
                            repo.toast("success", "Peso sincronizado", "%.1f kg desde Health Connect".format(kg))
                        } else repo.toast("info", "Sin peso reciente", "No hay un registro de peso válido en Health Connect.")
                        busy = false
                    }
                }
                SmallButton("Sincronizar sueño", Modifier.weight(1f), enabled = !busy) {
                    scope.launch {
                        busy = true
                        sleep = runCatching { manager.readSleepLastNight() }.getOrNull()
                        val h = sleep
                        if (h != null) repo.toast("success", "Sueño sincronizado", "%.1f h anoche".format(h))
                        else repo.toast("info", "Sin datos de sueño", "No hay sueño registrado para anoche.")
                        busy = false
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallButton("Sincronizar pasos", Modifier.weight(1f), enabled = !busy) {
                    scope.launch {
                        busy = true
                        snapshot = runCatching { manager.readToday() }.getOrNull()
                        val st = snapshot
                        if (st != null) repo.toast("success", "Pasos sincronizados", "${st.steps} pasos · ${"%.1f".format(st.distanceKm)} km")
                        else repo.toast("error", "No se pudo leer Health Connect")
                        busy = false
                    }
                }
                SmallButton("Enviar mi peso", Modifier.weight(1f), enabled = !busy) {
                    scope.launch {
                        busy = true
                        val kg = repo.state.value.profile.weight
                        if (kg != null) {
                            runCatching { manager.writeWeight(kg) }
                                .onSuccess { repo.toast("success", "Peso enviado", "%.1f kg escrito en Health Connect".format(kg)) }
                                .onFailure { repo.toast("error", "No se pudo escribir el peso", it.message) }
                        }
                        busy = false
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SmallButton("Desconectar de VoltBody", Modifier.fillMaxWidth(), danger = true) {
                enabled = false
                prefs.edit().putBoolean("healthConnectEnabled", false).apply()
                repo.toast("info", "Health Connect desconectado", "Puedes revocar los permisos desde los ajustes de Health Connect.")
            }
        }
    }
}

private fun normalizeUrl(raw: String): String {
    var u = raw.trim()
    if (u.isEmpty()) return u
    if (!u.startsWith("http://") && !u.startsWith("https://")) u = "http://$u"
    u = u.trimEnd('/')
    if (u.endsWith("/api")) u = u.removeSuffix("/api")
    return u
}

/** Seccion "Servidor": URL del backend + prueba /api/ping. */
@Composable
fun ServerCard(repo: Repository, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    var url by remember { mutableStateOf(repo.serverUrl()) }
    var result by remember { mutableStateOf<Pair<Boolean, String>?>(null) }
    var testing by remember { mutableStateOf(false) }

    VoltCard(modifier.fillMaxWidth()) {
        Text("Servidor", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text("Dirección del backend de VoltBody (el mismo de la web)", color = Volt.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        VoltTextField(url, { url = it; result = null }, "URL del servidor", keyboardType = KeyboardType.Uri)
        Spacer(Modifier.height(10.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SmallButton("Probar conexión", Modifier.weight(1f), enabled = !testing && url.isNotBlank()) {
                scope.launch {
                    testing = true
                    result = pingServer(normalizeUrl(url))
                    testing = false
                }
            }
            SmallButton("Guardar", Modifier.weight(1f), enabled = url.isNotBlank()) {
                val n = normalizeUrl(url)
                url = n
                repo.setServerUrl(n)
                repo.toast("success", "Servidor guardado", n)
            }
        }
        AnimatedVisibility(testing || result != null) {
            Row(Modifier.padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (testing) {
                    Text("Comprobando...", color = Volt.textDim, fontSize = 12.sp)
                } else {
                    val r = result
                    if (r != null) {
                        StatusDot(r.first)
                        Spacer(Modifier.width(8.dp))
                        Text(r.second, color = if (r.first) Volt.accent else Volt.danger, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

private suspend fun pingServer(base: String): Pair<Boolean, String> = withContext(Dispatchers.IO) {
    if (base.isBlank()) return@withContext false to "Introduce una URL."
    val client = OkHttpClient.Builder().connectTimeout(8, TimeUnit.SECONDS).readTimeout(8, TimeUnit.SECONDS).build()
    try {
        val t0 = System.currentTimeMillis()
        client.newCall(Request.Builder().url("$base/api/ping").get().build()).execute().use { resp ->
            val ms = System.currentTimeMillis() - t0
            if (resp.isSuccessful) true to "Conectado correctamente (${ms} ms)" else false to "El servidor respondió con el código ${resp.code}"
        }
    } catch (e: IllegalArgumentException) {
        false to "URL no válida."
    } catch (e: Exception) {
        false to "No se pudo conectar: ${e.message ?: "error de red"}"
    }
}

/** Datos: exportar, resetear datos locales y cerrar sesion. */
@Composable
fun DataCard(repo: Repository, state: AppState, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    var confirmReset by remember { mutableStateOf(false) }
    var confirmLogout by remember { mutableStateOf(false) }

    val exporter = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val s = repo.state.value.copy(authToken = null) // nunca exportar el token de sesion
                    ctx.contentResolver.openOutputStream(uri)?.use { it.write(AppJson.encodeToString(s).toByteArray()) } ?: error("sin salida")
                }.isSuccess
            }
            if (ok) repo.toast("success", "Datos exportados", "Archivo JSON guardado.") else repo.toast("error", "No se pudo exportar")
        }
    }

    VoltCard(modifier.fillMaxWidth()) {
        Text("Datos y sesión", color = Volt.text, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(state.user?.email ?: "Sesión local", color = Volt.textDim, fontSize = 11.sp)
        Spacer(Modifier.height(12.dp))
        SmallButton("Exportar mis datos (JSON)", Modifier.fillMaxWidth()) {
            exporter.launch("voltbody-datos-${LocalDate.now()}.json")
        }
        Spacer(Modifier.height(8.dp))
        SmallButton("Resetear datos locales de la app", Modifier.fillMaxWidth(), danger = true) { confirmReset = true }
        Spacer(Modifier.height(8.dp))
        SmallButton("Cerrar sesión", Modifier.fillMaxWidth(), danger = true) { confirmLogout = true }
    }

    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Volt.surface,
            title = { Text("¿Resetear datos locales?", color = Volt.text, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Se vaciará la memoria de la app y las preferencias del dispositivo (Health Connect). Mantienes la sesión y tu plan, y todo se volverá a descargar del servidor.",
                    color = Volt.textDim,
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReset = false
                    scope.launch {
                        Notifications.cancelAll(ctx)
                        ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
                        repo.update {
                            it.copy(logs = emptyList(), weightLogs = emptyList(), progressPhotos = emptyList(), metrics = emptyList())
                        }
                        if (repo.state.value.notificationsEnabled) repo.setNotificationsEnabled(false)
                        runCatching { repo.refreshAll() }
                        repo.toast("success", "Datos locales reseteados", "Se han vuelto a descargar del servidor.")
                    }
                }) { Text("Resetear", color = Volt.danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar", color = Volt.textDim) } },
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = Volt.surface,
            title = { Text("¿Cerrar sesión?", color = Volt.text, fontWeight = FontWeight.Bold) },
            text = { Text("Se borrarán los datos de este dispositivo. Tu cuenta y tus datos siguen en el servidor.", color = Volt.textDim) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    Notifications.cancelAll(ctx)
                    repo.logout()
                }) { Text("Cerrar sesión", color = Volt.danger, fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancelar", color = Volt.textDim) } },
        )
    }
}
