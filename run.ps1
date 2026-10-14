$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
Set-Location $PSScriptRoot
$adb = Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe'
cmd /c ".\gradlew.bat assembleRelease --console=plain > build.log 2>&1"
$ok = Select-String -Path build.log -Pattern 'BUILD SUCCESSFUL' -Quiet
if ($ok) {
  & $adb wait-for-device
  & $adb install -r app\build\outputs\apk\release\app-release.apk
  & $adb reverse tcp:3000 tcp:3000
  & $adb shell am start -n com.voltbody.app/.MainActivity
  Write-Host 'INSTALLED'
} else { Select-String -Path build.log -Pattern '^e: |What went wrong' -Context 0,3 | Select -First 20 }
Write-Host 'DONE'
