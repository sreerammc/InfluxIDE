# InfluxDB IDE Distribution Package Creator
# PowerShell Script

Write-Host "Creating InfluxDB IDE Distribution Package..." -ForegroundColor Green
Write-Host ""

# Set variables
$PROJECT_NAME = "InfluxDB-IDE"
$VERSION = "2.0.0-Beta"
$DIST_DIR = "$PROJECT_NAME-v$VERSION"
$ZIP_FILE = "$PROJECT_NAME-v$VERSION-Standalone.zip"

# Clean up previous distribution
if (Test-Path $DIST_DIR) {
    Write-Host "Removing previous distribution directory..." -ForegroundColor Yellow
    Remove-Item -Path $DIST_DIR -Recurse -Force
}
if (Test-Path $ZIP_FILE) {
    Write-Host "Removing previous ZIP file..." -ForegroundColor Yellow
    Remove-Item -Path $ZIP_FILE -Force
}

Write-Host "Building project with Maven..." -ForegroundColor Cyan
try {
    & mvn clean package
    if ($LASTEXITCODE -ne 0) {
        throw "Maven build failed with exit code $LASTEXITCODE"
    }
} catch {
    Write-Host "Error: Maven build failed!" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

Write-Host ""
Write-Host "Creating distribution directory..." -ForegroundColor Cyan
New-Item -ItemType Directory -Path $DIST_DIR | Out-Null

Write-Host "Copying application files..." -ForegroundColor Cyan
Copy-Item "target\influx-simple-2.0.0.jar" -Destination "$DIST_DIR\" -Force

Write-Host "Copying JavaFX SDK..." -ForegroundColor Cyan
Copy-Item "javafx-sdk" -Destination "$DIST_DIR\" -Recurse -Force

Write-Host "Copying documentation..." -ForegroundColor Cyan
Copy-Item "README.md" -Destination "$DIST_DIR\" -Force
Copy-Item "LICENSE" -Destination "$DIST_DIR\" -Force

Write-Host "Creating launcher scripts..." -ForegroundColor Cyan

# Main launcher script
@"
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
if exist "javafx-sdk\bin\javafx.graphics.dll" (
    echo Found local JavaFX SDK
    set JAVAFX_PATH=%~dp0javafx-sdk\lib
    echo Using local JavaFX SDK directory
) else (
    echo WARNING: JavaFX SDK not found locally
    echo Make sure JavaFX is installed or in PATH
    set JAVAFX_PATH=
)
echo.

echo Starting InfluxDB IDE with JavaFX...
if defined JAVAFX_PATH (
    java --module-path "%JAVAFX_PATH%" --add-modules javafx.controls,javafx.fxml,javafx.web -jar influx-simple-2.0.0.jar
) else (
    java -jar influx-simple-2.0.0.jar
)

if %errorlevel% neq 0 (
    echo.
    echo Application exited with error code: %errorlevel%
    pause
)
"@ | Out-File -FilePath "$DIST_DIR\InfluxDB-IDE.bat" -Encoding ASCII

# Simple launcher
@"
@echo off
echo Starting InfluxDB IDE...
echo.
call "InfluxDB-IDE.bat"
"@ | Out-File -FilePath "$DIST_DIR\Run-IDE.bat" -Encoding ASCII

# Admin launcher
@"
@echo off
echo Starting InfluxDB IDE as Administrator...
echo.
powershell -Command "Start-Process 'InfluxDB-IDE.bat' -Verb RunAs"
"@ | Out-File -FilePath "$DIST_DIR\Run-IDE-Admin.bat" -Encoding ASCII

Write-Host "Creating README for distribution..." -ForegroundColor Cyan

# Create comprehensive README
@"
# InfluxDB IDE v$VERSION - Standalone Package

## Quick Start

1. **Extract** this ZIP file to any folder
2. **Double-click** `Run-IDE.bat` to start the application
3. **No installation required** - runs directly from the extracted folder

## System Requirements

- **Windows 10/11** (64-bit)
- **Java 11 or higher** (JRE or JDK)
- **No Maven required**
- **No JavaFX SDK required** (included in package)

## Files Included

- `InfluxDB-IDE.bat` - Main launcher script
- `Run-IDE.bat` - Simple launcher (recommended)
- `Run-IDE-Admin.bat` - Launcher with admin privileges
- `influx-simple-2.0.0.jar` - Application JAR file
- `javafx-sdk/` - JavaFX runtime (included)
- `README.md` - Full documentation
- `LICENSE` - MIT License

## Features

- **InfluxDB Query IDE v2.0.0 Beta** with modern JavaFX interface
- **Connection Management** for multiple databases with startup configuration
- **SQL Query Editor** with syntax highlighting and drag & drop support
- **Advanced Results Display** with Excel-like filtering and sorting
- **Text-based Filtering** with options: Contains, Starts With, Ends With, Equals, etc.
- **Drag & Drop** functionality from table cells to query area
- **CSV Export** functionality (moved to menu)
- **Professional UI** with maximized window startup
- **Application Icon** support
- **Beta Version** identification and warnings
- **Enhanced Error Handling** and user feedback
- **Memory Management** optimizations

## Troubleshooting

If you get "Java not found" error:
1. Install Java 11+ from https://adoptium.net/
2. Make sure Java is in your PATH

If the application doesn't start:
1. Try running `Run-IDE-Admin.bat` as administrator
2. Check that Java is properly installed
3. Ensure antivirus isn't blocking the application

## Support

**Author:** Sreeram C Machavaram  
**Repository:** https://github.com/sreerammc/InfluxIDE  
**License:** MIT License

## Version History

**v2.0.0-Beta** - Enhanced Beta Release
- Excel-like filtering with text-based options (Contains, Starts With, Ends With, Equals, etc.)
- Drag and drop functionality from table cells to query area
- Enhanced UI with professional styling and maximized window startup
- Application icon support and beta version identification
- Improved connection management and query execution
- CSV export moved to menu bar
- Fixed duplicate status messages and improved user experience
- Memory management optimizations
- Comprehensive text filtering with dialog interface

**v1.0.0** - Initial release
- Complete InfluxDB IDE with JavaFX interface
- Standalone execution package
- No external dependencies required
"@ | Out-File -FilePath "$DIST_DIR\README-STANDALONE.md" -Encoding UTF8

Write-Host "Creating ZIP archive..." -ForegroundColor Cyan
try {
    Compress-Archive -Path $DIST_DIR -DestinationPath $ZIP_FILE -Force
    $zipSize = (Get-Item $ZIP_FILE).Length
    $zipSizeMB = [math]::Round($zipSize / 1MB, 2)
    
    Write-Host ""
    Write-Host "========================================" -ForegroundColor Green
    Write-Host "Distribution package created successfully!" -ForegroundColor Green
    Write-Host "========================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "Package: $ZIP_FILE" -ForegroundColor White
    Write-Host "Size: $zipSizeMB MB ($zipSize bytes)" -ForegroundColor White
    Write-Host ""
    Write-Host "Contents:" -ForegroundColor Cyan
    Get-ChildItem $DIST_DIR | ForEach-Object { Write-Host "  $($_.Name)" -ForegroundColor White }
    Write-Host ""
    Write-Host "Ready for distribution!" -ForegroundColor Green
    
} catch {
    Write-Host "Error: Failed to create ZIP file!" -ForegroundColor Red
    Write-Host $_.Exception.Message -ForegroundColor Red
}

Write-Host ""
Read-Host "Press Enter to exit" 