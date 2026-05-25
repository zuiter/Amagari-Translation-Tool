@echo off
set "PROJECT_ROOT=%~dp0"
set "ORIGINAL_JAVA_HOME=%JAVA_HOME%"
set "JAVA_HOME=%PROJECT_ROOT%.gradle\local-jdks\microsoft-jdk-21\jdk-21.0.5+11"

if not exist "%JAVA_HOME%\bin\java.exe" (
  set "JAVA_HOME=%PROJECT_ROOT%..\mapsociety-template-26.1\.gradle\local-jdks\microsoft-jdk-21\jdk-21.0.5+11"
)

if not exist "%JAVA_HOME%\bin\java.exe" if exist "F:\Dev\Java\jdk-21.0.9\bin\java.exe" (
  set "JAVA_HOME=F:\Dev\Java\jdk-21.0.9"
)

if not exist "%JAVA_HOME%\bin\java.exe" if exist "C:\Program Files\Microsoft\jdk-21.0.6.7-hotspot\bin\java.exe" (
  set "JAVA_HOME=C:\Program Files\Microsoft\jdk-21.0.6.7-hotspot"
)

if not exist "%JAVA_HOME%\bin\java.exe" if exist "%ORIGINAL_JAVA_HOME%\bin\java.exe" (
  set "JAVA_HOME=%ORIGINAL_JAVA_HOME%"
)

if not exist "%JAVA_HOME%\bin\java.exe" (
  echo Java 21 was not found. Install Java 21 or copy .gradle\local-jdks from a working sibling project.
  exit /b 1
)

set "PATH=%JAVA_HOME%\bin;%PATH%"

if "%~1"=="" (
  call "%PROJECT_ROOT%gradlew.bat" build
) else (
  call "%PROJECT_ROOT%gradlew.bat" %*
)

exit /b %ERRORLEVEL%
