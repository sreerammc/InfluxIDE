@echo off
echo Creating InfluxDB IDE Distribution Package...
echo.

REM Set variables
set "PROJECT_NAME=InfluxDB-IDE"
set "VERSION=1.0.0"
set "DIST_DIR=%PROJECT_NAME%-v%VERSION%"
set "ZIP_FILE=%PROJECT_NAME%-v%VERSION%-Standalone.zip"

REM Clean up previous distribution
if exist "%DIST_DIR%" rmdir /s /q "%DIST_DIR%"
if exist "%ZIP_FILE%" del "%ZIP_FILE%"

echo Building project...
call mvn clean package
if errorlevel 1 (
    echo Error: Maven build failed!
    pause
    exit /b 1
)

echo.
echo Creating distribution directory...
mkdir "%DIST_DIR%"

echo Copying application files...
copy "target\influx-simple-1.0.0.jar" "%DIST_DIR%\"
copy "InfluxDB-IDE-Standalone\InfluxDB-IDE.bat" "%DIST_DIR%\"

echo Copying JavaFX SDK...
xcopy "javafx-sdk" "%DIST_DIR%\javafx-sdk\" /E /I /Y

echo Copying documentation...
copy "README.md" "%DIST_DIR%\"
copy "LICENSE" "%DIST_DIR%\"

echo Creating launcher scripts...
echo @echo off > "%DIST_DIR%\Run-IDE.bat"
echo echo Starting InfluxDB IDE... >> "%DIST_DIR%\Run-IDE.bat"
echo echo. >> "%DIST_DIR%\Run-IDE.bat"
echo call "InfluxDB-IDE.bat" >> "%DIST_DIR%\Run-IDE.bat"

echo @echo off > "%DIST_DIR%\Run-IDE-Admin.bat"
echo echo Starting InfluxDB IDE as Administrator... >> "%DIST_DIR%\Run-IDE-Admin.bat"
echo echo. >> "%DIST_DIR%\Run-IDE-Admin.bat"
echo powershell -Command "Start-Process 'InfluxDB-IDE.bat' -Verb RunAs" >> "%DIST_DIR%\Run-IDE-Admin.bat"

echo Creating README for distribution...
echo # InfluxDB IDE v%VERSION% - Standalone Package > "%DIST_DIR%\README-STANDALONE.md"
echo. >> "%DIST_DIR%\README-STANDALONE.md"
echo ## Quick Start >> "%DIST_DIR%\README-STANDALONE.md"
echo. >> "%DIST_DIR%\README-STANDALONE.md"
echo 1. **Extract** this ZIP file to any folder >> "%DIST_DIR%\README-STANDALONE.md"
echo 2. **Double-click** `Run-IDE.bat` to start the application >> "%DIST_DIR%\README-STANDALONE.md"
echo 3. **No installation required** - runs directly from the extracted folder >> "%DIST_DIR%\README-STANDALONE.md"
echo. >> "%DIST_DIR%\README-STANDALONE.md"
echo ## System Requirements >> "%DIST_DIR%\README-STANDALONE.md"
echo. >> "%DIST_DIR%\README-STANDALONE.md"
echo - **Windows 10/11** (64-bit) >> "%DIST_DIR%\README-STANDALONE.md"
echo - **Java 11 or higher** (JRE or JDK) >> "%DIST_DIR%\README-STANDALONE.md"
echo - **No Maven required** >> "%DIST_DIR%\README-STANDALONE.md"
echo - **No JavaFX SDK required** (included in package) >> "%DIST_DIR%\README-STANDALONE.md"
echo. >> "%DIST_DIR%\README-STANDALONE.md"
echo ## Files Included >> "%DIST_DIR%\README-STANDALONE.md"
echo. >> "%DIST_DIR%\README-STANDALONE.md"
echo - `InfluxDB-IDE.bat` - Main launcher script >> "%DIST_DIR%\README-STANDALONE.md"
echo - `Run-IDE.bat` - Simple launcher (recommended) >> "%DIST_DIR%\README-STANDALONE.md"
echo - `Run-IDE-Admin.bat` - Launcher with admin privileges >> "%DIST_DIR%\README-STANDALONE.md"
echo - `influx-simple-1.0.0.jar` - Application JAR file >> "%DIST_DIR%\README-STANDALONE.md"
echo - `javafx-sdk/` - JavaFX runtime (included) >> "%DIST_DIR%\README-STANDALONE.md"
echo - `README.md` - Full documentation >> "%DIST_DIR%\README-STANDALONE.md"
echo - `LICENSE` - MIT License >> "%DIST_DIR%\README-STANDALONE.md"
echo. >> "%DIST_DIR%\README-STANDALONE.md"
echo ## Troubleshooting >> "%DIST_DIR%\README-STANDALONE.md"
echo. >> "%DIST_DIR%\README-STANDALONE.md"
echo If you get "Java not found" error: >> "%DIST_DIR%\README-STANDALONE.md"
echo 1. Install Java 11+ from https://adoptium.net/ >> "%DIST_DIR%\README-STANDALONE.md"
echo 2. Make sure Java is in your PATH >> "%DIST_DIR%\README-STANDALONE.md"
echo. >> "%DIST_DIR%\README-STANDALONE.md"
echo ## Support >> "%DIST_DIR%\README-STANDALONE.md"
echo. >> "%DIST_DIR%\README-STANDALONE.md"
echo Author: Sreeram C Machavaram >> "%DIST_DIR%\README-STANDALONE.md"
echo Repository: https://github.com/sreerammc/InfluxIDE >> "%DIST_DIR%\README-STANDALONE.md"

echo Creating ZIP archive...
powershell -Command "Compress-Archive -Path '%DIST_DIR%' -DestinationPath '%ZIP_FILE%' -Force"

if exist "%ZIP_FILE%" (
    echo.
    echo ========================================
    echo Distribution package created successfully!
    echo ========================================
    echo.
    echo Package: %ZIP_FILE%
    echo Size: 
    for %%A in ("%ZIP_FILE%") do echo        %%~zA bytes
    echo.
    echo Contents:
    dir "%DIST_DIR%" /B
    echo.
    echo Ready for distribution!
) else (
    echo Error: Failed to create ZIP file!
)

echo.
pause 