@echo off
setlocal
set GRADLE_VERSION=8.13
set BASE=%USERPROFILE%\.gradle\gymmanagement-bootstrap
set DIST=%BASE%\gradle-%GRADLE_VERSION%
if exist "%DIST%\bin\gradle.bat" goto run_gradle
where gradle >nul 2>nul
if %ERRORLEVEL%==0 (
  gradle %*
  exit /b %ERRORLEVEL%
)
if not exist "%BASE%" mkdir "%BASE%"
echo Downloading Gradle %GRADLE_VERSION% for this project...
powershell -NoProfile -ExecutionPolicy Bypass -Command "$ErrorActionPreference='Stop'; $zip=Join-Path '%BASE%' 'gradle-%GRADLE_VERSION%-bin.zip'; if (!(Test-Path $zip)) { Invoke-WebRequest 'https://services.gradle.org/distributions/gradle-%GRADLE_VERSION%-bin.zip' -OutFile $zip }; Expand-Archive -Path $zip -DestinationPath '%BASE%' -Force"
if errorlevel 1 (
  echo Could not download Gradle. Connect to the internet or configure Gradle in Android Studio.
  exit /b 1
)
:run_gradle
call "%DIST%\bin\gradle.bat" %*
exit /b %ERRORLEVEL%
