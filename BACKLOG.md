# VoltBody Android — Backlog de Sprints

> Actualizado automáticamente tras cada sprint. Referencia única de estado del proyecto.

---

## ✅ Sprint 1 — Fundamentos
- Arquitectura base: Hilt DI, Retrofit, Moshi, Navigation Compose
- Auth: Login + registro con JWT
- Screens base: Home, Workout, Diet, Calendar, Profile, Onboarding
- NavGraph + BottomNav inicial
- Theme M3 básico

---

## ✅ Sprint 2 — M3 Expressive + Nav Physics
- `MaterialExpressiveTheme` + `MotionScheme.expressive()`
- `VoltBodyShapes` con radios squircle (6/10/16/24/32 dp)
- BottomNav con spring física: `dampingRatio=0.45, stiffness=380`
- Icono activo sube -2dp con spring separado
- Dot indicator 4dp bajo icono activo (spring)
- Botón central alterna ⚡ HOME ↔ 🤖 AI_COACH
- Glow del botón central animado

---

## ✅ Sprint 3 — AI Coach Screen
- `AiChatDto.kt`: DTOs `AiChatRequest / AiChatResponse / AiChatContext` (Moshi)
- `AiCoachViewModel.kt`: contexto real (perfil + rutina de hoy + logs 7d), historial 20 turnos
- `AiCoachScreen.kt`:
  - Header avatar animado + badge "Online · Gemini Pro"
  - Bubbles usuario (degradado acento) / asistente (surface)
  - Thinking indicator 3 dots pulsantes desfasados 150ms
  - Quick-action chips (desaparecen tras 2º mensaje)
  - Input bar con `imePadding()` + botón send animado
- `ApiService.kt`: endpoint `POST api/ai/chat`
- `Models.kt`: `AppTab.AI_COACH`
- `NavGraph.kt`: ruta `ai_coach`
- `MainActivity.kt`: routing a AI_COACH

---

## ✅ Sprint 4 — Polish M3 Expressive + Migración Web→Android

### Polish
- `VoltBodyComponents.kt`:
  - `SetsButtonGroup`: M3 `ButtonGroup` segmentado para selección sets/reps
  - `WorkoutFloatingToolbar`: `HorizontalFloatingAppBar` con Pausar / Saltar descanso / Finalizar
  - `VoltBodyLoadingIndicator`: shimmer skeleton con `Brush.linearGradient` animado
- `HapticFeedback.kt`: wrapper `VibrationEffect` para TICK / CONFIRM / ERROR / HEAVY
- Spring `dampingRatio=0.55, stiffness=400` en entrada de cards (WorkoutSession, Home, Profile)

### Migración Web→Android

**Workout activo (nuevo):**
- `WorkoutSessionViewModel.kt`:
  - Timer elapsed (coroutine, pausa soportada)
  - Rest timer countdown con `skipRest()`
  - `logSet()` → auto-avance ejercicio/serie
  - POST sesión al backend al finalizar
- `WorkoutSessionScreen.kt`:
  - `RestTimerCard`: countdown visual + `LinearProgressIndicator`
  - `SetsButtonGroup` para reps (6/8/10/12/15/20)
  - `OutlinedTextField` para peso
  - Historial de series (`SetHistoryRow`) con spring entrance
  - `WorkoutFloatingToolbar` anclado al centro

**Home (ampliado):**
- `StreakBadge`: badge circular con días de racha, spring al aparecer
- `WeeklyProgressCard`: entrenos/semana, volumen total, % progreso, `LinearProgressIndicator`
- `VolumeChartCard`: gráfica Canvas (línea + fill + dots) 7 días
- `HomeViewModel`: `streakDays`, `weeklyWorkouts`, `weeklyTarget`, `weeklyVolumeKg`, `dailyVolumeKg`
- `TodayWorkoutCard`: card con `FilledIconButton` de play → navega a sesión activa

**Profile (ampliado):**
- `EditPhysicalDataSheet`: `ModalBottomSheet` con peso/altura/edad/objetivo
- `ChangePasswordDialog`: `AlertDialog` con visibilidad toggle + validación de match
- Units toggle: Switch métrico/imperial con conversión en tiempo real (kg↔lb, cm↔ft)
- `ProfileViewModel`: `updatePhysicalData()`, `changePassword()`, `toggleUnits()`

---

## 🔲 Sprint 5 — Diet completo
- Registro de comidas (búsqueda + añadir)
- Macros del día (proteína / carbos / grasas) con chart de anillo Canvas
- Historial semanal de calorías
- Scanner de código de barras (ML Kit BarcodeScanner)
- Sincronización con API `/diet/log`

---

## 🔲 Sprint 6 — Calendar + Progreso
- Vista mensual con días completados (dot markers)
- Detalle de día: resumen entreno + macros
- Gráficas de progreso: peso corporal, fuerza por ejercicio (1RM estimado)
- Export PDF del historial (iText o PdfDocument)

---

## 🔲 Sprint 7 — Notificaciones + Widgets
- Push notifications: recordatorio de entreno (FCM)
- Notificación de descanso durante sesión activa
- Widget Android (Glance API): próximo entreno + racha
- Deeplink desde widget → sesión activa

---

## 🔲 Sprint 8 — Release
- Onboarding revisado con animaciones Lottie
- Splash screen con `SplashScreen API`
- Firma APK / AAB para Play Store
- ProGuard rules definitivas
- Testes de instrumentación (Espresso) en flujo crítico: login → workout → finish
- Crash reporting (Firebase Crashlytics)
