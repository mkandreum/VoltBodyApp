# Arranca el emulador usando el adb del SDK (en C:\adb hay un adb antiguo v32 que choca con el del SDK y tumba el emulador).
$sdk = "$env:LOCALAPPDATA\Android\Sdk"
$env:Path = "$sdk\platform-tools;$sdk\emulator;" + (($env:Path -split ';' | Where-Object { $_ -notlike 'C:\adb*' }) -join ';')
Get-Process adb -ErrorAction SilentlyContinue | Where-Object { $_.Path -like 'C:\adb*' } | Stop-Process -Force -ErrorAction SilentlyContinue
& "$sdk\emulator\emulator.exe" -avd ev_pixel -no-boot-anim -no-snapshot-save -memory 4096
