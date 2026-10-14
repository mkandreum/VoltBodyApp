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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.CloudDownload
import androidx.compose.material.icons.outlined.MonitorWeight
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.Icon
import com.voltbody.app.ui.v3.Card
import com.voltbody.app.ui.v3.LimeButton
import com.voltbody.app.ui.v3.MenuRow
import com.voltbody.app.ui.v3.T
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
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

/** Boton secundario sobrio (como DarkButton de la v3): neutro; solo las acciones destructivas van en rojo. */
@Composable
private fun SmallButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, danger: Boolean = false, onClick: () -> Unit) {
    val fg = if (danger) Volt.danger else Volt.text
    Box(
        modifier.height(48.dp).clip(androidx.compose.foundation.shape.CircleShape)
            .background(if (danger) Volt.danger.copy(alpha = 0.10f) else Volt.surface2)
            .let { if (enabled) it.pressScale(onClick) else it },
        contentAlignment = Alignment.Center,
    ) { Text(text, color = if (enabled) fg else Volt.textDim, style = VoltType.body.copy(fontWeight = FontWeight.Bold), maxLines = 1) }
}

/**
 * Seccion "Salud": conexion con Health Connect (Samsung Health se sincroniza a traves de el).
 * Una accion principal (importar el ultimo mes) y el resto como filas discretas; cifras en una linea.
 */
@Composable
fun HealthCard(repo: Repository, modifier: Modifier = Modifier) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val manager = remember { ctx.healthManager }
    val prefs = remember { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var availability by remember { mutableIntStateOf(runCatching { manager.availability() }.getOrDefault(1)) }
    var hasPerms by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<HealthSnapshot?>(null) }
    var sleep by remember { mutableStateOf<Double?>(null) }
    var updatedAt by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var importing by remember { mutableStateOf(false) }
    val historyState = remember { com.voltbody.app.ui.screens.home.HealthState(ctx.applicationContext) }

    suspend fun refresh() {
        hasPerms = runCatching { manager.hasAnyPermission() }.getOrDefault(false)
        if (hasPerms) {
            snapshot = runCatching { manager.readToday() }.getOrNull()
            sleep = snapshot?.sleepHours ?: runCatching { manager.readSleepLastNight() }.getOrNull()
            updatedAt = java.time.LocalTime.now().toString().take(5)
        }
    }
    LaunchedEffect(Unit) { availability = runCatching { manager.availability() }.getOrDefault(1); refresh() }

    val contract = remember(manager) { manager.requestContract() }
    val launcher = rememberLauncherForActivityResult(contract) { granted: Set<String> ->
        scope.launch {
            val all = granted.containsAll(manager.permissions)
            prefs.edit().putBoolean("healthConnectEnabled", granted.isNotEmpty()).apply()
            refresh()
            if (granted.isNotEmpty()) {
                com.voltbody.app.ui.screens.home.HealthState.resetMonthSync()
                historyState.syncLastMonth(repo)
            }
            if (all) repo.toast("success", "Health Connect conectado", "Pasos, sueño y peso listos para sincronizar.")
            else if (granted.isNotEmpty()) repo.toast("info", "Permisos parciales", "Algunos datos no estarán disponibles.")
            else repo.toast("error", "Permiso denegado", "Puedes concederlo desde los ajustes de Health Connect.")
        }
    }

    val available = availability == HC_SDK_AVAILABLE
    val connected = available && hasPerms
    val today = remember { LocalDate.now().toString() }
    val s = snapshot

    Card(modifier.fillMaxWidth(), padding = 18.dp) {
        // Cabecera: icono, nombre y estado.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp)).background(Volt.danger.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Favorite, null, tint = Volt.danger, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Health Connect", style = T.cardTitle.copy(fontSize = 16.sp), color = Volt.text)
                Text("Pasos, sueño, HRV y peso", style = T.caption, color = Volt.textDim, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
            StatusChip(connected)
        }
        Spacer(Modifier.height(16.dp))

        when {
            !available -> {
                Text(
                    if (availability == 2) "Health Connect necesita una actualización para funcionar."
                    else "Health Connect no está instalado o no es compatible con este dispositivo.",
                    style = T.body, color = Volt.text,
                )
                Spacer(Modifier.height(14.dp))
                LimeButton(if (availability == 2) "Actualizar Health Connect" else "Instalar Health Connect", {
                    val pkg = "com.google.android.apps.healthdata"
                    val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    runCatching { ctx.startActivity(market) }.onFailure {
                        runCatching { ctx.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$pkg")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                    }
                }, height = 50.dp)
            }
            !connected -> {
                Text("Conecta para ver tus pasos, sueño y peso en VoltBody e importar tu último mes.", style = T.body, color = Volt.text)
                Spacer(Modifier.height(14.dp))
                LimeButton("Conectar Health Connect", { launcher.launch(manager.requestedPermissions) }, height = 50.dp)
            }
            else -> {
                // Cifras de hoy: valor + unidad en una sola linea (sin cortes).
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HcStat(s?.steps?.let { com.voltbody.app.ui.v3.fmt(it) } ?: "—", null, "Pasos hoy", Modifier.weight(1f))
                    HcStat(sleep?.let { String.format(java.util.Locale("es", "ES"), "%.1f", it) } ?: "—", if (sleep != null) "h" else null, "Sueño", Modifier.weight(1f))
                    HcStat(s?.weightKg?.let { String.format(java.util.Locale("es", "ES"), "%.1f", it) } ?: "—", if (s?.weightKg != null) "kg" else null, "Peso", Modifier.weight(1f))
                }
                updatedAt?.let {
                    Spacer(Modifier.height(8.dp))
                    Text("Actualizado a las $it", style = T.caption, color = Volt.textDim)
                }
                Spacer(Modifier.height(14.dp))
                LimeButton(
                    "Importar último mes", {
                        scope.launch {
                            importing = true
                            com.voltbody.app.ui.screens.home.HealthState.resetMonthSync()
                            when (historyState.syncLastMonth(repo)) {
                                0 -> repo.toast("info", "Historial al día", "No hay datos nuevos en los últimos ${com.voltbody.app.ui.screens.home.HealthState.HISTORY_DAYS} días.")
                                -1 -> repo.toast("error", "No se pudo leer Health Connect", "Revisa los permisos y vuelve a intentarlo.")
                            }
                            importing = false
                        }
                    }, height = 50.dp, loading = importing, icon = Icons.Outlined.CloudDownload,
                )
                Spacer(Modifier.height(6.dp))
                MenuRow(Icons.Outlined.Refresh, "Actualizar ahora", "Pasos, sueño y peso de hoy") {
                    if (!busy) scope.launch { busy = true; refresh(); repo.toast("success", "Datos actualizados", s?.let { "${it.steps} pasos" }); busy = false }
                }
                val hcKg = s?.weightKg
                if (hcKg != null && hcKg in 20.0..400.0) {
                    MenuRow(Icons.Outlined.MonitorWeight, "Usar peso de Health Connect", "Guardar ${trimNum(Math.round(hcKg * 10) / 10.0)} kg como peso de hoy") {
                        if (!busy) scope.launch {
                            busy = true
                            if (repo.addWeight(today, Math.round(hcKg * 10) / 10.0)) repo.toast("success", "Peso guardado", "%.1f kg desde Health Connect".format(hcKg))
                            busy = false
                        }
                    }
                }
                repo.state.value.profile.weight?.let { kg ->
                    MenuRow(Icons.Outlined.Upload, "Enviar mi peso", "Escribir ${trimNum(kg)} kg en Health Connect") {
                        if (!busy) scope.launch {
                            busy = true
                            runCatching { manager.writeWeight(kg) }
                                .onSuccess { repo.toast("success", "Peso enviado", "%.1f kg escrito en Health Connect".format(kg)) }
                                .onFailure { repo.toast("error", "No se pudo escribir el peso", it.message) }
                            busy = false
                        }
                    }
                }
            }
        }
    }

    // Ayuda de Samsung Health: texto discreto fuera de la tarjeta (antes era una caja gris ilegible).
    Text(
        "¿Usas Samsung Health? Activa la sincronización en Samsung Health › Ajustes › Health Connect y los datos llegarán aquí.",
        style = T.caption, color = Volt.textDim, modifier = Modifier.padding(horizontal = 6.dp),
    )
    if (connected) {
        Text(
            "Desconectar de VoltBody", style = T.cardTitle, color = Volt.danger,
            modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable {
                prefs.edit().putBoolean("healthConnectEnabled", false).apply()
                repo.toast("info", "Health Connect desconectado", "Puedes revocar los permisos desde los ajustes de Health Connect.")
            }.padding(horizontal = 6.dp, vertical = 14.dp),
        )
    }
}

@Composable
private fun StatusChip(ok: Boolean) {
    val c = if (ok) Volt.accent else Volt.textDim
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(c.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(RoundedCornerShape(50)).background(c))
        Spacer(Modifier.width(6.dp))
        Text(if (ok) "Conectado" else "Sin conectar", style = T.caption, color = c, maxLines = 1)
    }
}

/** Cifra compacta: valor grande + unidad pequena en la misma linea y etiqueta debajo. */
@Composable
private fun HcStat(value: String, unit: String?, label: String, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(18.dp)).background(Volt.surface2).padding(horizontal = 12.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value, style = T.value.copy(fontSize = 19.sp), color = Volt.text, maxLines = 1, softWrap = false, modifier = Modifier.alignByBaseline())
            if (unit != null) {
                Spacer(Modifier.width(3.dp))
                Text(unit, style = T.caption, color = Volt.textDim, maxLines = 1, modifier = Modifier.alignByBaseline())
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(label, style = T.caption, color = Volt.textDim, maxLines = 1)
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

    BentoCard(modifier.fillMaxWidth()) {
        Text("Servidor", color = Volt.text, style = VoltType.h2)
        Text("Dirección del backend de VoltBody (el mismo de la web)", color = Volt.textDim, style = VoltType.body)
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
                    Text("Comprobando...", color = Volt.textDim, style = VoltType.body)
                } else {
                    val r = result
                    if (r != null) {
                        StatusDot(r.first)
                        Spacer(Modifier.width(8.dp))
                        Text(r.second, color = if (r.first) Volt.accent else Volt.danger, style = VoltType.body)
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

    BentoCard(modifier.fillMaxWidth()) {
        Text("Tu cuenta", color = Volt.text, style = VoltType.h2)
        Text(state.user?.email ?: "Sesión local", color = Volt.textDim, style = VoltType.body)
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
            containerColor = Volt.dialogSurface,
            shape = RoundedCornerShape(28.dp),
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
                }) { Text("Resetear", color = Volt.danger, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { confirmReset = false }) { Text("Cancelar", color = Volt.textDim) } },
        )
    }
    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            containerColor = Volt.dialogSurface,
            shape = RoundedCornerShape(28.dp),
            title = { Text("¿Cerrar sesión?", color = Volt.text, fontWeight = FontWeight.Bold) },
            text = { Text("Se borrarán los datos de este dispositivo. Tu cuenta y tus datos siguen en el servidor.", color = Volt.textDim) },
            confirmButton = {
                TextButton(onClick = {
                    confirmLogout = false
                    Notifications.cancelAll(ctx)
                    repo.logout()
                }) { Text("Cerrar sesión", color = Volt.danger, fontWeight = FontWeight.ExtraBold) }
            },
            dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("Cancelar", color = Volt.textDim) } },
        )
    }
}
