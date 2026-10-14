package com.voltbody.app.ui.v3

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.voltbody.app.BuildConfig
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.screens.home.ExerciseImage
import com.voltbody.app.ui.screens.home.rememberGifLoader
import com.voltbody.app.ui.shake
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.util.Haptics
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Traduce los errores (en ingles) que devuelve el servidor. */
private fun humanError(msg: String?): String = when {
    msg.isNullOrBlank() -> "No se pudo conectar con el servidor"
    msg.contains("Invalid credentials", true) -> "Correo o contraseña incorrectos"
    msg.contains("already exists", true) -> "Ya existe una cuenta con ese correo"
    msg.contains("at least 6", true) -> "La contraseña necesita al menos 6 caracteres"
    msg.contains("Invalid email", true) -> "El correo no es válido"
    msg.contains("Too many", true) -> "Demasiados intentos. Espera unos minutos"
    else -> msg
}

private val EMAIL_RE = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
private const val RUNNER_GIF = "https://raw.githubusercontent.com/hasaneyldrm/exercises-dataset/main/videos/0685-oLrKqDH.gif"

/** Portada (concepto de la imagen del corredor, en oscuro) + login/registro en hoja inferior. */
@Composable
fun LoginV3(repo: Repository) {
    val ctx = LocalContext.current
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    var sheet by rememberSaveable { mutableStateOf(false) }
    var isLogin by rememberSaveable { mutableStateOf(true) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var showPass by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var shakeTick by remember { mutableIntStateOf(0) }
    var showServer by remember { mutableStateOf(false) }
    val gif = rememberGifLoader()

    fun fail(msg: String) { error = msg; shakeTick++; Haptics.error(ctx) }
    fun submit() {
        if (loading) return
        focus.clearFocus()
        val e = email.trim()
        val v = when {
            !isLogin && name.isBlank() -> "Escribe tu nombre"
            !EMAIL_RE.matches(e) || e.length > 120 -> "El correo no es válido"
            password.length < 6 -> "La contraseña necesita al menos 6 caracteres"
            password.length > 128 -> "La contraseña es demasiado larga"
            else -> null
        }
        if (v != null) { fail(v); return }
        error = ""; loading = true
        scope.launch {
            try {
                if (isLogin) repo.login(e, password) else repo.register(e, password, name.trim())
                Haptics.success(ctx)
            } catch (ex: Exception) {
                fail(humanError(ex.message))
            } finally { loading = false }
        }
    }

    BackHandler(enabled = sheet) { focus.clearFocus(); sheet = false }

    Box(Modifier.fillMaxSize().background(Volt.bg)) {
        // ── Portada
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 24.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(Volt.accent), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Bolt, null, tint = Volt.onAccent, modifier = Modifier.size(20.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text("VoltBody", style = T.section, color = Volt.text, modifier = Modifier.weight(1f))
                IconCircle(Icons.Outlined.Settings, size = 38.dp, tint = Volt.textDim, desc = "Servidor") { showServer = true }
            }

            // Figura: corredor dentro de un anillo con orbita (concepto de las imagenes)
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) { RunnerOrb(gif) }

            KineticLines(
                listOf("Más fuerte" to true, "cada día." to true, "Más sano" to false, "cada paso." to false),
            )
            Spacer(Modifier.height(28.dp))
            GooeyCta("Empezar", { isLogin = false; sheet = true })
            Spacer(Modifier.height(10.dp))
            Text(
                "Ya tengo cuenta · Iniciar sesión", style = T.body, color = Volt.textDim,
                modifier = Modifier.align(Alignment.CenterHorizontally).clip(RoundedCornerShape(12.dp)).tap(0.96f) { isLogin = true; sheet = true }.padding(10.dp),
            )
            Spacer(Modifier.height(8.dp))
        }

        // ── Hoja de login/registro
        AnimatedVisibility(sheet, enter = fadeIn(tween(150)), exit = fadeOut(tween(150))) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)).tap(1f, haptic = false) { focus.clearFocus(); sheet = false })
        }
        AnimatedVisibility(
            sheet, modifier = Modifier.align(Alignment.BottomCenter),
            enter = slideInVertically(spring(0.86f, 600f)) { it }, exit = slideOutVertically(tween(200)) { it },
        ) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)).background(Volt.surface)
                    .navigationBarsPadding().imePadding().padding(horizontal = 22.dp).padding(top = 12.dp, bottom = 20.dp)
                    .shake(shakeTick),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Box(Modifier.align(Alignment.CenterHorizontally).size(width = 40.dp, height = 4.dp).clip(RoundedCornerShape(2.dp)).background(Volt.surface2))
                // Tabs
                Row(Modifier.fillMaxWidth().height(46.dp).clip(RoundedCornerShape(23.dp)).background(Volt.surface2).padding(4.dp)) {
                    listOf(true to "Iniciar sesión", false to "Crear cuenta").forEach { (login, label) ->
                        val sel = isLogin == login
                        Box(
                            Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(20.dp)).background(if (sel) Volt.accent else Color.Transparent)
                                .tap(0.96f) { isLogin = login; error = "" },
                            contentAlignment = Alignment.Center,
                        ) { Text(label, style = T.cardTitle, color = if (sel) Color(0xFF0A0A0A) else Volt.textDim) }
                    }
                }
                AnimatedContent(isLogin, transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(90)) }, label = "authTitle") { l ->
                    Text(if (l) "Bienvenido de nuevo" else "Empieza tu cambio", style = T.screen.copy(fontSize = 22.sp), color = Volt.text)
                }
                AnimatedVisibility(!isLogin) { Field(name, { name = it; error = "" }, "Nombre", KeyboardType.Text) }
                Field(email, { email = it; error = "" }, "Correo electrónico", KeyboardType.Email)
                Field(password, { password = it; error = "" }, "Contraseña", KeyboardType.Password, password = !showPass, done = { submit() },
                    trailing = { Icon(if (showPass) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility, "Mostrar", tint = Volt.textDim, modifier = Modifier.tap(0.9f) { showPass = !showPass }) })
                AnimatedVisibility(error.isNotEmpty()) {
                    Text(error, style = T.body, color = Volt.danger, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Volt.danger.copy(alpha = 0.12f)).padding(12.dp))
                }
                LimeButton(if (isLogin) "Entrar" else "Crear cuenta", { submit() }, loading = loading)
            }
        }
    }

    if (showServer) ServerDialog(repo) { showServer = false }
}

@Composable
private fun Field(
    value: String, onChange: (String) -> Unit, label: String, type: KeyboardType,
    password: Boolean = false, done: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        keyboardOptions = KeyboardOptions(keyboardType = type, imeAction = if (done != null) ImeAction.Done else ImeAction.Next),
        keyboardActions = KeyboardActions(onDone = { done?.invoke() }),
        visualTransformation = if (password) PasswordVisualTransformation() else VisualTransformation.None,
        trailingIcon = trailing,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Volt.accent, unfocusedBorderColor = Color.Transparent, focusedLabelColor = Volt.accent,
            unfocusedLabelColor = Volt.textDim, cursorColor = Volt.accent, focusedTextColor = Volt.text, unfocusedTextColor = Volt.text,
            focusedContainerColor = Volt.surface2, unfocusedContainerColor = Volt.surface2,
        ),
    )
}

/** Titular gigante bicolor que entra linea a linea (rapido). */
@Composable
private fun KineticLines(lines: List<Pair<String, Boolean>>) {
    Column {
        lines.forEachIndexed { i, (text, strong) ->
            val a = remember { Animatable(0f) }
            LaunchedEffect(Unit) { delay(120L + i * 70L); a.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
            Text(
                text, style = T.heroXL.copy(fontSize = 44.sp, lineHeight = 46.sp), color = if (strong) Volt.text else Volt.textDim.copy(alpha = 0.45f),
                maxLines = 1, modifier = Modifier.graphicsLayer { alpha = a.value; translationY = (1f - a.value) * 28.dp.toPx() },
            )
        }
    }
}

/** Anillo lima con orbita punteada que gira y el corredor animado dentro. */
@Composable
private fun RunnerOrb(gif: coil.ImageLoader) {
    val t = rememberInfiniteTransition(label = "orb")
    val rot = t.animateFloat(0f, 360f, infiniteRepeatable(tween(14000, easing = LinearEasing)), label = "rot")
    val breathe = t.animateFloat(0.97f, 1.03f, infiniteRepeatable(tween(1800), RepeatMode.Reverse), label = "breathe")
    val accent = Volt.accent
    Box(Modifier.size(270.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.matchParentSize().graphicsLayer { rotationZ = rot.value }) {
            val r = size.minDimension / 2
            drawCircle(Color.White.copy(alpha = 0.12f), r - 2.dp.toPx(), style = Stroke(1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 8.dp.toPx()))))
            drawArc(accent, -90f, 110f, false, Offset(14.dp.toPx(), 14.dp.toPx()), androidx.compose.ui.geometry.Size(size.width - 28.dp.toPx(), size.height - 28.dp.toPx()), style = Stroke(6.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round))
            drawCircle(accent, 6.dp.toPx(), Offset(center.x + (r - 2.dp.toPx()), center.y))
        }
        Box(Modifier.size(206.dp).graphicsLayer { scaleX = breathe.value; scaleY = breathe.value }.clip(CircleShape).background(Color.White), contentAlignment = Alignment.Center) {
            ExerciseImage(RUNNER_GIF, gif, Modifier.fillMaxSize(), 520, 520)
        }
    }
}

@Composable
private fun ServerDialog(repo: Repository, onDismiss: () -> Unit) {
    var url by remember { mutableStateOf(repo.serverUrl()) }
    AlertDialog(
        onDismissRequest = onDismiss, containerColor = Volt.surface,
        title = { Text("Servidor", style = T.section, color = Volt.text) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dirección del servidor de VoltBody.", style = T.body, color = Volt.textDim)
                Field(url, { url = it }, "URL", KeyboardType.Uri)
            }
        },
        confirmButton = { TextButton({ repo.setServerUrl(url); onDismiss() }) { Text("Guardar", color = Volt.accent) } },
        dismissButton = { TextButton({ repo.setServerUrl(""); url = BuildConfig.DEFAULT_API_URL; onDismiss() }) { Text("Restablecer", color = Volt.textDim) } },
    )
}
