@echo off
echo Starting InfluxDB IDE...
java --module-path javafx-sdk\lib --add-modules javafx.controls,javafx.fxml,javafx.graphics,javafx.base --add-opens javafx.graphics/javafx.scene=ALL-UNNAMED -cp influx-simple-1.0.0.jar com.influxdata.demo.InfluxDBJavaFXIDE
pause
