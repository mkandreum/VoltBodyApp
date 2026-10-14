# VoltBody Android

App nativa (Kotlin + Jetpack Compose) de VoltBody Powered. Usa el mismo servidor y base de datos que la web
([VoltBodyPowered](https://github.com/mkandreum/VoltBodyPowered)): no guarda datos de usuario en local
(solo sesión y URL del servidor).

- Salud: Google Health Connect (recibe también los datos de Samsung Health).
- Configura el servidor en `app/build.gradle.kts` (`DEFAULT_API_URL`) o en Login (engranaje) / Perfil.
- Build: `./gradlew assembleDebug` (JDK 17, Android SDK 35).
