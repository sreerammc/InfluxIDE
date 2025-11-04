package com.influxdata.demo;

import com.influxdata.demo.controller.MainApplicationController;
import com.influxdata.demo.ui.ConnectionDialog;
import com.influxdata.demo.config.ApplicationConfig;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.image.Image;
import javafx.stage.Stage;

/**
 * Refactored InfluxDB IDE Application
 * Uses the new modular architecture with proper separation of concerns
 */
public class InfluxDBIDERefactored extends Application {
    
    private MainApplicationController mainController;
    private ApplicationConfig connectionConfig;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Set application icon
            setApplicationIcon(primaryStage);
            
            // Show connection dialog first
            if (!showConnectionDialog(primaryStage)) {
                System.exit(0);
            }
            
            // Create main application controller with connection config
            mainController = new MainApplicationController(primaryStage, connectionConfig);
            
            // Show main window
            primaryStage.show();
            
            // Set JVM timezone based on configuration
            setJVMTimezone();
            
        } catch (Exception e) {
            showError("Application Error", "Failed to start application: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
            System.exit(1);
        }
    }
    
    /**
     * Show connection dialog
     * @param parentStage Parent stage for the dialog
     * @return true if connection successful, false if cancelled
     */
    private boolean showConnectionDialog(Stage parentStage) {
        try {
            ConnectionDialog dialog = new ConnectionDialog(parentStage);
            boolean success = dialog.showDialog();
            if (success) {
                connectionConfig = dialog.getConfig();
            }
            return success;
        } catch (Exception e) {
            showError("Connection Error", "Failed to show connection dialog: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Set application icon
     */
    private void setApplicationIcon(Stage stage) {
        try {
            stage.getIcons().add(new Image(getClass().getResourceAsStream("/icons/app_icon.png")));
        } catch (Exception e) {
            System.err.println("Failed to set application icon: " + e.getMessage());
        }
    }
    
    /**
     * Set JVM timezone
     */
    private void setJVMTimezone() {
        try {
            // Timezone will be set by the main controller after configuration is loaded
        } catch (Exception e) {
            System.err.println("Failed to set JVM timezone: " + e.getMessage());
        }
    }
    
    /**
     * Show error dialog
     */
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Main method
     */
    public static void main(String[] args) {
        try {
            // Check for Apache Arrow JVM arguments
            checkApacheArrowJVMArgs();
            
            // Launch JavaFX application
            launch(args);
            
        } catch (Exception e) {
            System.err.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Check if Apache Arrow JVM arguments are set
     * According to Apache Arrow documentation: https://arrow.apache.org/docs/java/flight_sql_jdbc_driver.html
     */
    private static void checkApacheArrowJVMArgs() {
        // Check if the required JVM argument is set
        // This check may not be 100% reliable when run via Maven, but serves as a reminder
        String vmArgs = System.getProperty("sun.java.command", "");
        String javaToolOptions = System.getenv("JAVA_TOOL_OPTIONS");
        String javaOptions = javaToolOptions != null ? javaToolOptions : "";
        String allArgs = vmArgs + " " + javaOptions;
        
        if (!allArgs.contains("--add-opens=java.base/java.nio=org.apache.arrow.memory.core,ALL-UNNAMED") &&
            !allArgs.contains("--add-opens java.base/java.nio=org.apache.arrow.memory.core,ALL-UNNAMED")) {
            System.err.println("WARNING: Apache Arrow JVM arguments not detected.");
            System.err.println("For Flight SQL support, ensure the following JVM arguments are set:");
            System.err.println("--add-opens=java.base/java.nio=org.apache.arrow.memory.core,ALL-UNNAMED");
            System.err.println("--add-opens=java.base/java.nio=ALL-UNNAMED");
            System.err.println("See: https://arrow.apache.org/docs/java/flight_sql_jdbc_driver.html");
        }
    }
} 