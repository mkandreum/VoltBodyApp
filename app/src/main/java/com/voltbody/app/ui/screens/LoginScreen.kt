package com.voltbody.app.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onSizeChanged
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.KineticTitle
import com.voltbody.app.ui.MeshBackground
import com.voltbody.app.ui.PrimaryButton
import com.voltbody.app.ui.SwipeToStart
import com.voltbody.app.ui.gradientBorder
import com.voltbody.app.ui.screens.auth.BoltLogo
import com.voltbody.app.ui.screens.auth.OrbitFigure
import com.voltbody.app.ui.screens.auth.SplashOverlay
import com.voltbody.app.ui.shake
import com.voltbody.app.ui.staggeredEntry
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltType
import com.voltbody.app.ui.voltFieldColors
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val EMAIL_RE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

/** Portada + login/registro en hoja inferior. El resultado (token + perfil) lo recoge MainActivity observando repo.state. */
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
    var showSheet by rememberSaveable { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var errorTick by remember { mutableIntStateOf(0) }
    var showServer by remember { mutableStateOf(false) }

    fun fail(msg: String) {
        error = msg; errorTick++
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
    }

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
        if (v != null) { fail(v); return }
        error = ""
        loading = true
        scope.launch {
            try {
                if (isLogin) repo.login(email, password) else repo.register(email, password, name.trim())
            } catch (e: Exception) {
                fail(e.message?.takeIf { it.isNotBlank() } ?: "Ha ocurrido un error al conectar")
            } finally {
                loading = false
            }
        }
    }

    BackHandler(enabled = showSheet) { focus.clearFocus(); showSheet = false }

    Box(Modifier.fillMaxSize()) {
        MeshBackground(Modifier.fillMaxSize())

        // ───────── Portada ─────────
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.padding(top = 16.dp).staggeredEntry(0), verticalAlignment = Alignment.CenterVertically) {
                BoltLogo(36.dp, pulsing = false)
                Spacer(Modifier.width(10.dp))
                Text("VOLTBODY", color = Volt.text, style = VoltType.label.copy(fontSize = 13.sp))
            }
            Box(Modifier.weight(1f).fillMaxWidth().staggeredEntry(1), contentAlignment = Alignment.Center) {
                OrbitFigure(Modifier.fillMaxWidth().aspectRatio(1f))
            }
            KineticTitle(
                listOf("Más fuerte", "cada día.", "Más sano", "cada paso."), dimFromLine = 2,
                style = VoltType.display.copy(fontSize = 46.sp, lineHeight = 48.sp),
            )
            Spacer(Modifier.height(28.dp))
            Box(Modifier.staggeredEntry(5)) {
                SwipeToStart("Empezar", onComplete = { isLogin = false; showSheet = true })
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Ya tengo cuenta · Iniciar sesión", color = Volt.textDim, style = VoltType.body, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().clip(CircleShape)
                    .clickable { isLogin = true; showSheet = true }.padding(vertical = 16.dp),
            )
        }

        IconButton(onClick = { showServer = true }, modifier = Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(4.dp).graphicsLayer { alpha = 0.5f }) {
            Icon(Icons.Filled.Settings, "Servidor", tint = Volt.textDim)
        }

        // ───────── Hoja inferior ─────────
        AnimatedVisibility(showSheet, enter = fadeIn(tween(250)), exit = fadeOut(tween(200))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { focus.clearFocus(); showSheet = false })
        }
        AnimatedVisibility(
            showSheet, modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(0.85f, 330f)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(200)),
        ) {
            val shape = RoundedCornerShape(topStart = 36.dp, topEnd = 36.dp)
            Column(
                Modifier.fillMaxWidth().clip(shape).background(Volt.surface).gradientBorder(1.dp, shape)
                    .imePadding().navigationBarsPadding().verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp).padding(top = 12.dp, bottom = 24.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).width(40.dp).height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)))
                Spacer(Modifier.height(20.dp))
                Text(if (isLogin) "Bienvenido de vuelta" else "Crea tu cuenta", color = Volt.text, style = VoltType.h1)
                Text(
                    if (isLogin) "Entra y sigue donde lo dejaste." else "Un minuto y tu plan estará listo.",
                    color = Volt.textDim, style = VoltType.body, modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(20.dp))
                AuthTabs(isLogin) { isLogin = it; error = "" }
                Spacer(Modifier.height(18.dp))

                Column(Modifier.shake(errorTick)) {
                    AnimatedVisibility(
                        visible = !isLogin,
                        enter = expandVertically(spring(0.85f, 400f)) + fadeIn(tween(250)),
                        exit = shrinkVertically(tween(200)) + fadeOut(tween(150)),
                    ) {
                        Column {
                            AuthField(name, { name = it }, "Nombre completo", Icons.Filled.Person, imeAction = ImeAction.Next, onNext = { focus.moveFocus(FocusDirection.Down) })
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                    AuthField(
                        email, { email = it.take(120) }, "Correo electrónico", Icons.Filled.Email,
                        keyboardType = KeyboardType.Email, imeAction = ImeAction.Next, onNext = { focus.moveFocus(FocusDirection.Down) },
                    )
                    Spacer(Modifier.height(12.dp))
                    AuthField(
                        password, { password = it.take(128) }, "Contraseña", Icons.Filled.Lock,
                        keyboardType = KeyboardType.Password, imeAction = ImeAction.Done, onNext = { submit() },
                        password = true, showPassword = showPassword, onToggleShow = { showPassword = !showPassword },
                    )
                }

                AnimatedVisibility(visible = error.isNotEmpty(), enter = expandVertically() + fadeIn(), exit = shrinkVertically() + fadeOut()) {
                    Row(
                        Modifier.padding(top = 14.dp).fillMaxWidth().shake(errorTick).clip(RoundedCornerShape(18.dp))
                            .background(Volt.danger.copy(alpha = 0.12f)).border(BorderStroke(1.dp, Volt.danger.copy(alpha = 0.3f)), RoundedCornerShape(18.dp))
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.ErrorOutline, null, tint = Volt.danger, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(error, color = Volt.danger, style = VoltType.body)
                    }
                }

                Spacer(Modifier.height(22.dp))
                PrimaryButton(if (isLogin) "Iniciar sesión" else "Crear mi cuenta", onClick = { submit() }, enabled = !loading)
            }
        }

        AnimatedVisibility(visible = loading, enter = fadeIn(tween(250)), exit = fadeOut(tween(250))) { SplashOverlay() }
    }

    if (showServer) ServerDialog(repo) { showServer = false }
}

@Composable
private fun AuthTabs(isLogin: Boolean, onSelect: (Boolean) -> Unit) {
    var widthPx by remember { mutableIntStateOf(0) }
    val pos = remember { Animatable(if (isLogin) 0f else 1f) }
    LaunchedEffect(isLogin) { pos.animateTo(if (isLogin) 0f else 1f, spring(0.72f, 420f)) }
    val grad = Volt.gradient
    Box(Modifier.fillMaxWidth().height(52.dp).clip(CircleShape).background(Volt.surface2).onSizeChanged { widthPx = it.width }) {
        Box(
            Modifier.offset { IntOffset(((widthPx / 2f) * pos.value).roundToInt() + 4, 4) }
                .width(with(androidx.compose.ui.platform.LocalDensity.current) { (widthPx / 2).toDp() } - 8.dp)
                .height(44.dp).clip(CircleShape).background(grad),
        )
        Row(Modifier.fillMaxSize()) {
            listOf(true to "Iniciar sesión", false to "Crear cuenta").forEach { (login, label) ->
                val active = login == isLogin
                Box(Modifier.weight(1f).fillMaxHeight().clip(CircleShape).clickable { onSelect(login) }, contentAlignment = Alignment.Center) {
                    Text(label, color = if (active) Volt.onAccent else Volt.textDim, style = VoltType.title.copy(fontSize = 14.sp))
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
        label = { Text(label) }, shape = RoundedCornerShape(22.dp), colors = voltFieldColors(), textStyle = VoltType.title,
        leadingIcon = { Icon(icon, null, tint = Volt.textDim, modifier = Modifier.size(22.dp)) },
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
        title = { Text("Servidor", color = Volt.text, style = VoltType.h2) },
        text = {
            Column {
                Text("Dirección del servidor VoltBody (el mismo backend que la web).", color = Volt.textDim, style = VoltType.body)
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = url, onValueChange = { url = it; err = null }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    label = { Text("URL") }, placeholder = { Text("http://192.168.1.20:3000") },
                    isError = err != null, supportingText = if (err != null) { { Text(err ?: "") } } else null,
                    shape = RoundedCornerShape(18.dp), colors = voltFieldColors(),
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
            }) { Text("Guardar", color = Volt.accent, style = VoltType.title) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar", color = Volt.textDim) } },
    )
}
