$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
Set-Location $PSScriptRoot
cmd /c ".\gradlew.bat assembleDebug --console=plain > build.log 2>&1"
Select-String -Path build.log -Pattern 'BUILD (SUCCESSFUL|FAILED)'
Write-Host 'DONE'
