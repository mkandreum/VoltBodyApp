# Herramientas de desarrollo

## Servidor de pruebas local (`mock-server.mjs`)
Imita la API de la web en memoria para probar la app en el emulador sin tocar el servidor real.

```bash
node devtools/mock-server.mjs
adb reverse tcp:3000 tcp:3000
```
En la app: engranaje del login (o Perfil > Servidor) → `http://localhost:3000`.

Cuenta de prueba (solo existe en este servidor local): `demo@voltbody.test` / `Volt-demo-2026`.

## Capturas sin emulador
`& .\shots.ps1` → `app/build/shots/*.png` (Robolectric + Roborazzi).

## Compilar, instalar y abrir en el emulador
`& .\run.ps1`
