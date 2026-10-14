package com.voltbody.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.voltbody.app.data.Repository
import com.voltbody.app.ui.screens.*
import com.voltbody.app.ui.theme.Volt
import com.voltbody.app.ui.theme.VoltTheme
import kotlinx.coroutines.delay

enum class Tab(val label: String, val icon: ImageVector) {
    Home("Inicio", Icons.Filled.Home), Workout("Rutina", Icons.Filled.FitnessCenter), Diet("Dieta", Icons.Filled.Restaurant),
    Calendar("Calendario", Icons.Filled.CalendarMonth), Profile("Perfil", Icons.Filled.Person)
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val repo = (application as VoltBodyApp).repo
        setContent {
            val st by repo.state.collectAsStateWithLifecycle()
            VoltTheme(st.theme) {
                Surface(Modifier.fillMaxSize(), color = Volt.bg) {
                    var toast by remember { mutableStateOf<com.voltbody.app.data.Toast?>(null) }
                    LaunchedEffect(Unit) { repo.toasts.collect { toast = it; delay(3500); toast = null } }
                    LaunchedEffect(st.isAuthenticated) { if (st.isAuthenticated) repo.refreshAll() }
                    Box(Modifier.fillMaxSize().statusBarsPadding()) {
                        when {
                            !st.hydrated -> Box(Modifier.fillMaxSize())
                            !st.isAuthenticated -> LoginScreen(repo)
                            !st.isOnboarded -> OnboardingScreen(repo)
                            else -> MainShell(repo)
                        }
                        toast?.let { t ->
                            Box(
                                Modifier.align(Alignment.TopCenter).padding(16.dp).clip(RoundedCornerShape(16.dp))
                                    .background(if (t.type == "error") Volt.danger else if (t.type == "success") Volt.accent else Volt.surface2)
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                            ) {
                                Column {
                                    Text(t.title, color = if (t.type == "info") Volt.text else Color.Black, fontWeight = FontWeight.Bold)
                                    t.message?.let { Text(it, color = if (t.type == "info") Volt.textDim else Color.Black.copy(.8f), fontSize = 12.sp) }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/** Contenido de cada pestana debe dejar ~96dp libres abajo (barra flotante). */
@Composable
fun MainShell(repo: Repository) {
    var tab by rememberSaveable { mutableStateOf(Tab.Home) }
    Box(Modifier.fillMaxSize()) {
        when (tab) {
            Tab.Home -> HomeScreen(repo, onNavigate = { tab = navTab(it) })
            Tab.Workout -> WorkoutScreen(repo)
            Tab.Diet -> DietScreen(repo)
            Tab.Calendar -> CalendarScreen(repo, onNavigate = { tab = navTab(it) })
            Tab.Profile -> ProfileScreen(repo)
        }
        Row(
            Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp)
                .fillMaxWidth().clip(RoundedCornerShape(32.dp)).background(Volt.surface).border(1.dp, Volt.border, RoundedCornerShape(32.dp))
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically,
        ) {
            Tab.entries.forEach { t ->
                val sel = t == tab
                Box(
                    Modifier.size(48.dp).clip(CircleShape).background(if (sel) Volt.accent else Color.Transparent).clickable { tab = t },
                    contentAlignment = Alignment.Center,
                ) { Icon(t.icon, t.label, tint = if (sel) Color.Black else Volt.textDim) }
            }
        }
    }
}

private fun navTab(key: String): Tab = when (key) {
    "workout" -> Tab.Workout; "diet" -> Tab.Diet; "calendar" -> Tab.Calendar; "profile" -> Tab.Profile; else -> Tab.Home
}
