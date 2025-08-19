# InfluxDB Query IDE

A JavaFX-based IDE for querying InfluxDB databases with a modern, user-friendly interface.

## Features

- **Connection Management**: Easy setup for InfluxDB host, database, and API token
- **Query Interface**: Execute InfluxDB queries with syntax highlighting
- **Results Display**: View results in both table format and raw JSON
- **Export Functionality**: Export results to CSV format
- **Drag & Drop**: Drag table names from results into query text
- **Excel-like Features**: Sort and filter results by columns
- **Modern UI**: Clean, responsive interface with application icon

## Prerequisites

- **Java 11 or higher** (Java 24.0.2 recommended)
- **Maven** (for building the project)

## Quick Start

### 1. Build the Project
```bash
mvn clean package
```

### 2. Run the Application

#### Option A: Using the Batch Launcher (Recommended)
```bash
# Windows - Double click or run:
InfluxDB-IDE.bat
```

#### Option B: Using Maven
```bash
mvn javafx:run
```

#### Option C: Direct Java Execution
```bash
java --module-path "path/to/javafx-sdk/lib" --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED -cp "target/influx-simple-1.0.0.jar" com.influxdata.demo.InfluxDBJavaFXIDE
```

## How the Launcher Works

The `InfluxDB-IDE.bat` launcher automatically:

1. **Checks Java installation** and displays version
2. **Detects Maven availability** - if found, uses `mvn javafx:run`
3. **Falls back to standalone execution** using JavaFX modules from Maven repository
4. **Provides clear error messages** and guidance if anything is missing

## Connection Details

- **Host**: Enter your InfluxDB host (e.g., `172.187.233.15:8181`)
- **Database**: Enter your database name (e.g., `test`)
- **API Token**: Enter your InfluxDB API token

## Usage

1. **Connect**: Enter your InfluxDB connection details
2. **Query**: Write your InfluxDB query (e.g., `SHOW MEASUREMENTS`)
3. **Execute**: Click "Execute Query" or press Enter
4. **View Results**: Results appear in both table and raw JSON formats
5. **Export**: Click "Export to CSV" to save results
6. **Filter & Sort**: Use column headers for sorting and filtering

## Menu Options

- **Database → Show Tables**: Automatically executes `SHOW MEASUREMENTS`
- **Help → About**: Shows application information and author details

## Troubleshooting

### "JavaFX runtime components are missing"
- **Solution**: Use `InfluxDB-IDE.bat` - it handles this automatically
- **Alternative**: Use `mvn javafx:run` if Maven is available

### "Java is not installed"
- **Solution**: Install Java 11 or higher from https://adoptium.net/

### "Application JAR not found"
- **Solution**: Build the project first with `mvn clean package`

## Building from Source

```bash
# Clone and build
git clone <repository-url>
cd influxSimple
mvn clean package
```

## Dependencies

- **JavaFX 17.0.2**: UI framework
- **InfluxDB Java Client**: Database connectivity
- **JSON Library**: Data processing
- **Maven**: Build automation

## Author

**Sreeram C Machavaram**

## License

This project is provided as-is for educational and development purposes. 