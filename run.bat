@echo off
REM Simple launcher for InfluxDB IDE with Flight SQL support
REM Sets JAVA_TOOL_OPTIONS to ensure JVM arguments are applied

echo Starting InfluxDB IDE with Flight SQL support...
echo.

REM Set JAVA_TOOL_OPTIONS - Java will automatically pick these up
REM This is the most reliable way to ensure JVM arguments are applied
set JAVA_TOOL_OPTIONS=--add-opens=java.base/java.nio=org.apache.arrow.memory.core,ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=javafx.graphics/javafx.scene=ALL-UNNAMED

REM Run via JavaFX Maven plugin
call mvn javafx:run

REM Clear the environment variable
set JAVA_TOOL_OPTIONS=