@echo off
set "PROJECT_ROOT=%~dp0"
set "ORIGINAL_JAVA_HOME=%JAVA_HOME%"
set "JAVA_HOME=%PROJECT_ROOT%.gradle\local-jdks\jdk-17.0.2"

if not exist "%JAVA_HOME%\bin\java.exe" (
  set "JAVA_HOME=%PROJECT_ROOT%.gradle\local-jdks\microsoft-jdk-17\jdk-17.0.12+7"
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  set "JAVA_HOME=%PROJECT_ROOT%..\mapsociety-template-26.1\.gradle\local-jdks\microsoft-jdk-17\jdk-17.0.12+7"
)

if not exist "%JAVA_HOME%\bin\java.exe" if exist "F:\Dev\Java\jdk-17.0.12\bin\java.exe" (
  set "JAVA_HOME=F:\Dev\Java\jdk-17.0.12"
)

if not exist "%JAVA_HOME%\bin\java.exe" if exist "%ORIGINAL_JAVA_HOME%\bin\java.exe" (
  set "JAVA_HOME=%ORIGINAL_JAVA_HOME%"
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Java 17 was not found. Install Java 17 or copy .gradle\local-jdks from a working sibling project.
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

if "%~1"=="" (
  call "%PROJECT_ROOT%gradlew.bat" -Dorg.gradle.java.installations.auto-download=false -Dorg.gradle.java.installations.paths="%JAVA_HOME%" build
) else (
  call "%PROJECT_ROOT%gradlew.bat" -Dorg.gradle.java.installations.auto-download=false -Dorg.gradle.java.installations.paths="%JAVA_HOME%" %*
)

exit /b %ERRORLEVEL%
