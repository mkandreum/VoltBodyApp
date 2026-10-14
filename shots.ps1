$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
Set-Location $PSScriptRoot
cmd /c ".\gradlew.bat :app:testDebugUnitTest --console=plain --continue > shots.log 2>&1"
Select-String -Path shots.log -Pattern 'BUILD (SUCCESSFUL|FAILED)'
Get-ChildItem app\build\shots -ErrorAction SilentlyContinue | Select Name,Length
Write-Host 'DONE'
