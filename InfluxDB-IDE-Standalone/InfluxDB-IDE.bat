@echo off
echo ========================================
echo    InfluxDB Query IDE - Launcher
echo ========================================
echo.

echo Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 11 or higher and try again
    pause
    exit /b 1
)

echo Java is available
echo.

echo Checking if JAR exists...
if not exist "influx-simple-1.0.0.jar" (
    echo ERROR: Application JAR not found!
    echo Please make sure influx-simple-1.0.0.jar is in the same folder
    pause
    exit /b 1
)

echo JAR file found
echo.

echo Looking for JavaFX runtime...

REM Check for local JavaFX SDK
if exist "javafx-sdk\lib" (
    echo Found local JavaFX SDK
    set "modulePath=javafx-sdk\lib"
    echo Using local JavaFX SDK directory
    goto :launch
)

echo ERROR: JavaFX runtime not found!
echo Please make sure javafx-sdk folder is in the same directory
pause
exit /b 1

:launch
echo Starting InfluxDB IDE with JavaFX...
echo.

java --module-path "%modulePath%" --add-modules javafx.controls,javafx.fxml,javafx.graphics --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED -cp "influx-simple-1.0.0.jar" com.influxdata.demo.InfluxDBJavaFXIDE

if %errorlevel% equ 0 (
    echo.
    echo Application exited successfully
) else (
    echo.
    echo Application exited with error code: %errorlevel%
)

echo.
pause 