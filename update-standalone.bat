@echo off
REM Script to update standalone distribution with latest fixes

echo ========================================
echo Updating Standalone Distribution
echo ========================================
echo.

REM Build the project with all latest fixes
echo [1/3] Building project with latest fixes...
call mvn clean package -DskipTests
if %errorlevel% neq 0 (
    echo ERROR: Build failed!
    pause
    exit /b 1
)

echo [2/3] Copying JAR to standalone distribution...
if not exist "InfluxDB-IDE-v2.0.0-Beta" (
    mkdir "InfluxDB-IDE-v2.0.0-Beta"
)

copy /Y target\influx-simple-2.0.0.jar InfluxDB-IDE-v2.0.0-Beta\influx-simple-2.0.0.jar
if %errorlevel% neq 0 (
    echo ERROR: Failed to copy JAR file!
    pause
    exit /b 1
)

echo [3/3] Standalone distribution updated!
echo.
echo The standalone distribution includes:
echo   - SSL validation skip for all API types (Flight SQL, InfluxDB 3 API, REST)
echo   - Strict API isolation (no fallbacks)
echo   - Fixed Flight SQL with correct JDBC URL format
echo   - Updated InfluxDB 3 Java API implementation
echo   - Proper JVM arguments for Flight SQL support
echo.
echo To run the standalone version:
echo   cd InfluxDB-IDE-v2.0.0-Beta
echo   InfluxDB-IDE.bat
echo.
pause
