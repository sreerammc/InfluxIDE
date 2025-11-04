========================================
InfluxDB IDE v2.0.0 - Standalone Distribution
========================================

Quick Start:
-----------
1. Double-click InfluxDB-IDE.bat to start the application
2. Configure your InfluxDB connection in the Connection Settings dialog
3. Select your API type (Flight SQL, InfluxDB 3 Java API, or REST API)
4. Start querying!

Files Included:
--------------
- influx-simple-2.0.0.jar - Main application JAR (includes all dependencies)
- InfluxDB-IDE.bat - Launcher script with proper JVM arguments
- javafx-sdk/ - JavaFX runtime (required for UI)
- CHANGELOG.txt - List of changes and improvements

System Requirements:
-------------------
- Java 11 or higher (check with: java -version)
- Windows operating system

Connection Configuration:
------------------------
Host: your-server:port (e.g., 20.90.176.154:8181)
Database: your database/bucket name
Token: your API token
Protocol: HTTP or HTTPS
API Type: Choose one:
  - Flight SQL (best performance, requires JVM args - already included)
  - InfluxDB 3 Java API (uses /api/v3/query_sql endpoint)
  - REST API (traditional /query endpoint)
Skip SSL validation: Check this for self-signed certificates

Troubleshooting:
---------------
- If Flight SQL fails: The launcher already includes required JVM arguments
- If SSL errors: Check "Skip SSL validation" in connection settings
- Check logs folder for detailed error messages
- Ensure your InfluxDB server is accessible and Flight SQL is enabled

For more information, see CHANGELOG.txt
