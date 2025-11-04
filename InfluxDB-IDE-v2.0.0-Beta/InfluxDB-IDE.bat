@echo off
title InfluxDB Query IDE v2.0.0 Beta - Launcher
echo ========================================
echo InfluxDB Query IDE v2.0.0 Beta - Launcher
echo ========================================
echo.

echo Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 11 or higher from https://adoptium.net/
    pause
    exit /b 1
)
echo Java is available
echo.

echo Checking if JAR exists...
if not exist "influx-simple-2.0.0.jar" (
    echo ERROR: JAR file not found: influx-simple-2.0.0.jar
    pause
    exit /b 1
)
echo JAR file found
echo.

echo Looking for JavaFX runtime...
if exist "javafx-sdk\lib\javafx.controls.jar" (
    echo Found local JavaFX SDK
    set JAVAFX_PATH=%~dp0javafx-sdk\lib
    echo Using local JavaFX SDK directory: %JAVAFX_PATH%
) else (
    echo WARNING: JavaFX SDK not found locally
    echo Make sure JavaFX is installed or in PATH
    set JAVAFX_PATH=
)
echo.

echo Starting InfluxDB IDE with JavaFX and Flight SQL support...
if defined JAVAFX_PATH (
    echo Running with JavaFX module path: %JAVAFX_PATH%
    java --module-path "%JAVAFX_PATH%" ^
         --add-modules javafx.controls,javafx.fxml ^
         --add-opens=java.base/java.nio=org.apache.arrow.memory.core,ALL-UNNAMED ^
         --add-opens=java.base/java.nio=ALL-UNNAMED ^
         --add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED ^
         -Xmx2g ^
         -XX:+UseG1GC ^
         -jar influx-simple-2.0.0.jar
) else (
    echo Running without JavaFX module path
    java --add-opens=java.base/java.nio=org.apache.arrow.memory.core,ALL-UNNAMED ^
         --add-opens=java.base/java.nio=ALL-UNNAMED ^
         --add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED ^
         -Xmx2g ^
         -XX:+UseG1GC ^
         -jar influx-simple-2.0.0.jar
)

if %errorlevel% neq 0 (
    echo.
    echo Application exited with error code: %errorlevel%
    pause
)
