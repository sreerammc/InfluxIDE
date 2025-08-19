# InfluxDB IDE Distribution Package Creator
# PowerShell Script

Write-Host "Creating InfluxDB IDE Distribution Package..." -ForegroundColor Green
Write-Host ""

# Set variables
$PROJECT_NAME = "InfluxDB-IDE"
$VERSION = "1.0.0"
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
Copy-Item "target\influx-simple-1.0.0.jar" -Destination "$DIST_DIR\" -Force
Copy-Item "InfluxDB-IDE-Standalone\InfluxDB-IDE.bat" -Destination "$DIST_DIR\" -Force

Write-Host "Copying JavaFX SDK..." -ForegroundColor Cyan
Copy-Item "javafx-sdk" -Destination "$DIST_DIR\" -Recurse -Force

Write-Host "Copying documentation..." -ForegroundColor Cyan
Copy-Item "README.md" -Destination "$DIST_DIR\" -Force
Copy-Item "LICENSE" -Destination "$DIST_DIR\" -Force

Write-Host "Creating launcher scripts..." -ForegroundColor Cyan

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
- `influx-simple-1.0.0.jar` - Application JAR file
- `javafx-sdk/` - JavaFX runtime (included)
- `README.md` - Full documentation
- `LICENSE` - MIT License

## Features

- **InfluxDB Query IDE** with modern JavaFX interface
- **Connection Management** for multiple databases
- **SQL Query Editor** with syntax highlighting
- **Results Display** in table format with filtering and sorting
- **CSV Export** functionality
- **Drag & Drop** support for table names
- **Excel-like Features** including column filtering and sorting

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

**v$VERSION** - Initial release
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