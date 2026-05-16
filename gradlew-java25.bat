@echo off
set "PROJECT_ROOT=%~dp0"
set "JAVA_HOME=%PROJECT_ROOT%.gradle\local-jdks\microsoft-jdk-25\jdk-25.0.2+10"

if not exist "%JAVA_HOME%\bin\java.exe" (
  set "JAVA_HOME=%PROJECT_ROOT%..\mapsociety-template-26.1\.gradle\local-jdks\microsoft-jdk-25\jdk-25.0.2+10"
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Java 25 was not found. Install Java 25 or copy .gradle\local-jdks from a working sibling project.
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

if "%~1"=="" (
  call "%PROJECT_ROOT%gradlew.bat" build
) else (
  call "%PROJECT_ROOT%gradlew.bat" %*
)

exit /b %ERRORLEVEL%
