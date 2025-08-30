package com.influxdata.demo.config;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class LoggingConfig {
    private static final Logger LOGGER = Logger.getLogger(LoggingConfig.class.getName());
    
    // Log file settings
    public static final String LOG_DIR = UIConstants.SETTINGS_DIR + File.separator + "logs";
    public static final String APPLICATION_LOG_FILE = LOG_DIR + File.separator + "influxdb-ide.log";
    public static final String QUERY_LOG_FILE = LOG_DIR + File.separator + "queries.log";
    public static final String ERROR_LOG_FILE = LOG_DIR + File.separator + "errors.log";
    public static final String PERFORMANCE_LOG_FILE = LOG_DIR + File.separator + "performance.log";
    
    // Rotation settings
    public static final int MAX_LOG_FILE_SIZE_MB = 10;
    public static final int MAX_LOG_FILES = 5;
    public static final int LOG_ROTATION_INTERVAL_HOURS = 24;
    
    // Log levels
    public static final Level DEFAULT_LOG_LEVEL = Level.INFO;
    public static final Level QUERY_LOG_LEVEL = Level.INFO;
    public static final Level ERROR_LOG_LEVEL = Level.WARNING;
    public static final Level PERFORMANCE_LOG_LEVEL = Level.INFO;
    
    // Logger names for different components
    public static final String APPLICATION_LOGGER = "com.influxdata.demo.application";
    public static final String CONNECTION_LOGGER = "com.influxdata.demo.connection";
    public static final String QUERY_LOGGER = "com.influxdata.demo.query";
    public static final String UI_LOGGER = "com.influxdata.demo.ui";
    public static final String EXPORT_LOGGER = "com.influxdata.demo.export";
    public static final String PERFORMANCE_LOGGER = "com.influxdata.demo.performance";
    
    private LoggingConfig() {}
    
    public static void initializeLogging() {
        try {
            createLogDirectory();
            setupLogging();
            LOGGER.info("Logging system initialized successfully");
        } catch (Exception e) {
            System.err.println("Failed to initialize logging: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void createLogDirectory() {
        File logDir = new File(LOG_DIR);
        if (!logDir.exists()) {
            if (logDir.mkdirs()) {
                LOGGER.info("Created log directory: " + LOG_DIR);
            } else {
                LOGGER.warning("Failed to create log directory: " + LOG_DIR);
            }
        }
    }
    
    private static void setupLogging() {
        // This will be implemented in LoggingService
        // For now, just ensure the directory exists
    }
    
    public static String getLogDirectory() {
        return LOG_DIR;
    }
    
    public static boolean isLoggingEnabled() {
        return new File(LOG_DIR).exists();
    }
} 