package com.voltbody.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.rememberPulse
import com.voltbody.app.ui.screens.auth.BoltLogo
import com.voltbody.app.ui.screens.auth.SplashOverlay
import com.voltbody.app.ui.screens.auth.ambientGlow
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.voltFieldColors
import kotlinx.coroutines.launch

private val EMAIL_RE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** Login / registro. El resultado (token + perfil hidratado) lo recoge MainActivity observando repo.state. */
@Composable
fun LoginScreen(repo: Repository) {
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val haptic = LocalHapticFeedback.current
    var isLogin by rememberSaveable { mutableStateOf(true) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var showServer by remember { mutableStateOf(false) }

    fun validate(): String? {
        val e = email.trim()
        if (!isLogin && name.isBlank()) return "Introduce tu nombre completo"
        if (!EMAIL_RE.matches(e)) return "El correo electrónico no es válido"
        if (e.length > 120) return "El correo es demasiado largo"
        if (password.length < 6) return "La contraseña debe tener al menos 6 caracteres"
        if (password.length > 128) return "La contraseña es demasiado larga"
        return null
    }

    fun submit() {
        if (loading) return
        focus.clearFocus()
        val v = validate()
        if (v != null) { error = v; haptic.performHapticFeedback(HapticFeedbackType.LongPress); return }
        error = ""
        loading = true
        scope.launch {
            try {
                if (isLogin) repo.login(email, password) else repo.register(email, password, name.trim())
            } catch (e: Exception) {
                error = e.message?.takeIf { it.isNotBlank() } ?: "Ha ocurrido un error al conectar"
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
            } finally {
                loading = false
            }
        }
    }

    val glowA = Volt.accent.copy(alpha = 0.16f)
    val glowB = Volt.accent.copy(alpha = 0.10f)

    Box(
        Modifier.fillMaxSize()
            .ambientGlow(glowA, Offset(0f, 0f), 700f)
            .ambientGlow(glowB, Offset(1200f, 2400f), 800f),
    ) {
        Column(
            Modifier.fillMaxSize().imePadding().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(40.dp))
            Column(Modifier.staggeredEntry(0), horizontalAlignment = Alignment.CenterHorizontally) {
                BoltLogo(84.dp)
                Spacer(Modifier.height(16.dp))
                Text("VOLTBODY ECOSYSTEM", color = Volt.textDim, fontSize = 11.sp, letterSpacing = 2.5.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(4.dp))
                val accent = Volt.accent
                Text(
                    buildAnnotatedString {
                        withStyle(SpanStyle(color = Color.White)) { append("MODO ") }
                        withStyle(SpanStyle(color = accent)) { append("BESTIA") }
                    },
                    fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "Entrenamiento de alta precisión y nutrición guiada por IA.",
                    color = Volt.textDim, fontSize = 12.sp, textAlign = TextAlign.Center, modifier = Modifier.widthIn(max = 280.dp),
                )
            }
            Spacer(Modifier.height(28.dp))

            Column(
                Modifier.staggeredEntry(1).widthIn(max = 420.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(28.dp)).background(Volt.surface)
                    .border(1.dp, Volt.border, RoundedCornerShape(28.dp)).padding(22.dp),
            ) {
                AuthTabs(isLogin) { isLogin = it; error = "" }
                Spacer(Modifier.height(18.dp))

                AnimatedVisibility(
                    visible = !isLogin,
                    enter = expandVertically(tween(280, easing = FastOutSlowInEasing)) + fadeIn(tween(280)),
                    exit = shrinkVertically(tween(220, easing = FastOutSlowInEasing)) + fadeOut(tween(180)),
                ) {
                    Column {
                        AuthField(name, { name = it }, "Nombre completo", Icons.Filled.Person, imeAction = ImeAction.Next, onNext = { focus.moveFocus(FocusDirection.Down) })
                        Spacer(Modifier.height(14.dp))
                    }
                }
                AuthField(
                    email, { email = it.take(120) }, "Correo electrónico", Icons.Filled.Email,
                    keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, onNext = { focus.moveFocus(FocusDirection.Down) },
                )
                Spacer(Modifier.height(14.dp))
                AuthField(
                    password, { password = it.take(128) }, "Contraseña", Icons.Filled.Lock,
                    keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, onNext = { submit() },
                    password = true, showPassword = showPassword, onToggleShow = { showPassword = !showPassword },
                )

                AnimatedVisibility(
                    visible = error.isNotEmpty(),
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut(),
                ) {
                    Row(
                        Modifier.padding(top = 14.dp).fillMaxWidth().clip(RoundedCornerShape(16.dp))
                            .background(Volt.danger.copy(alpha = 0.12f)).border(1.dp, Volt.danger.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = Volt.danger, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(error, color = Volt.danger, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }

                Spacer(Modifier.height(20.dp))
                val pulse by rememberPulse(1f, 1.015f, 1200)
                Button(
                    onClick = { submit() }, enabled = !loading,
                    modifier = Modifier.fillMaxWidth().height(54.dp).scale(pulse),
                    shape = RoundedCornerShape(27.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Volt.accent, contentColor = Color.Black),
                ) {
                    Text(if (isLogin) "Iniciar sesión" else "Comenzar registro", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.width(8.dp))
                    Icon(Icons.Filled.ArrowForward, null, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(20.dp))
            Text(
                "Protegido con encriptación de extremo a extremo · VoltBody AI",
                color = Volt.textDim.copy(alpha = 0.7f), fontSize = 11.sp, textAlign = TextAlign.Center,
                modifier = Modifier.staggeredEntry(2),
            )
            Spacer(Modifier.height(24.dp))
        }

        // Engranaje discreto: URL del servidor.
        IconButton(onClick = { showServer = true }, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(4.dp).alpha(0.55f)) {
            Icon(Icons.Filled.Settings, "Servidor", tint = Volt.textDim)
        }

        AnimatedVisibility(visible = loading, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) { SplashOverlay() }
    }

    if (showServer) ServerDialog(repo) { showServer = false }
}

@Composable
private fun AuthTabs(isLogin: Boolean, onSelect: (Boolean) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.Black.copy(alpha = 0.4f)).padding(4.dp)) {
        val half = maxWidth / 2
        val off by animateDpAsState(if (isLogin) 0.dp else half, spring(dampingRatio = 0.75f, stiffness = Spring.StiffnessMedium), label = "tab")
        Box(Modifier.offset(x = off).width(half).height(44.dp).clip(RoundedCornerShape(16.dp)).background(Volt.accent))
        Row {
            listOf(true to "Iniciar sesión", false to "Crear cuenta").forEach { (login, label) ->
                val active = login == isLogin
                val c by animateColorAsState(if (active) Color.Black else Volt.textDim, tween(200), label = "tabText")
                Box(Modifier.width(half).height(44.dp).clip(RoundedCornerShape(16.dp)).clickable { onSelect(login) }, contentAlignment = Alignment.Center) {
                    Text(label, color = c, fontSize = 13.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun AuthField(
    value: String, onChange: (String) -> Unit, label: String, icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text, imeAction: ImeAction = ImeAction.Next, onNext: () -> Unit = {},
    password: Boolean = false, showPassword: Boolean = false, onToggleShow: () -> Unit = {},
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, singleLine = true, modifier = Modifier.fillMaxWidth(),
        label = { Text(label) }, shape = RoundedCornerShape(18.dp), colors = voltFieldColors(),
        leadingIcon = { Icon(icon, null, tint = Volt.textDim, modifier = Modifier.size(20.dp)) },
        trailingIcon = if (password) {
            { IconButton(onClick = onToggleShow) { Icon(if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility, if (showPassword) "Ocultar contraseña" else "Ver contraseña", tint = Volt.textDim) } }
        } else null,
        visualTransformation = if (password && !showPassword) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onNext() }, onDone = { onNext() }),
    )
}

@Composable
private fun ServerDialog(repo: Repository, onDismiss: () -> Unit) {
    val current = remember { repo.state.value.serverUrl }
    var url by remember { mutableStateOf(current.ifBlank { repo.serverUrl() }) }
    var err by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Volt.surface,
        shape = RoundedCornerShape(28.dp),
        title = { Text("Servidor", color = Volt.text, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("Dirección del servidor VoltBody (el mismo backend que la web).", color = Volt.textDim, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url, onValueChange = { url = it; err = null }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL") }, placeholder = { Text("http://192.168.1.20:3000") },
                    isError = err != null, supportingText = if (err != null) { { Text(err ?: "") } } else null,
                    shape = RoundedCornerShape(16.dp), colors = voltFieldColors(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Done),
                )
                if (current.isNotBlank()) {
                    TextButton(onClick = { repo.setServerUrl(""); repo.toast("info", "Servidor restablecido", "Se usará la dirección por defecto."); onDismiss() }) {
                        Text("Restablecer valor por defecto", color = Volt.textDim, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val u = url.trim().trimEnd('/')
                if (!(u.startsWith("http://") || u.startsWith("https://")) || u.length < 10) {
                    err = "Debe empezar por http:// o https://"
                } else {
                    repo.setServerUrl(u)
                    repo.toast("success", "Servidor guardado", u)
                    onDismiss()
                }
            }) { Text("Guardar", color = Volt.accent, fontWeight = FontWeight.Bold) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Volt.textDim) } },
    )
}
