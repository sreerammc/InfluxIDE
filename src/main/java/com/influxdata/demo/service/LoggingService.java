package com.influxdata.demo.service;

import com.influxdata.demo.config.LoggingConfig;
import com.influxdata.demo.exception.ApplicationException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LoggingService {
    private static final Logger LOGGER = Logger.getLogger(LoggingService.class.getName());
    private static final DateTimeFormatter TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    
    private final Logger applicationLogger;
    private final Logger connectionLogger;
    private final Logger queryLogger;
    private final Logger uiLogger;
    private final Logger exportLogger;
    private final Logger performanceLogger;
    
    private final ScheduledExecutorService rotationScheduler;
    private final FileHandler applicationFileHandler;
    private final FileHandler queryFileHandler;
    private final FileHandler errorFileHandler;
    private final FileHandler performanceFileHandler;
    
    private static LoggingService instance;
    
    private LoggingService() throws ApplicationException {
        try {
            // Create log directory first
            createLogDirectory();
            
            // Create loggers
            this.applicationLogger = createLogger(LoggingConfig.APPLICATION_LOGGER, LoggingConfig.DEFAULT_LOG_LEVEL);
            this.connectionLogger = createLogger(LoggingConfig.CONNECTION_LOGGER, LoggingConfig.DEFAULT_LOG_LEVEL);
            this.queryLogger = createLogger(LoggingConfig.QUERY_LOGGER, LoggingConfig.QUERY_LOG_LEVEL);
            this.uiLogger = createLogger(LoggingConfig.UI_LOGGER, LoggingConfig.DEFAULT_LOG_LEVEL);
            this.exportLogger = createLogger(LoggingConfig.EXPORT_LOGGER, LoggingConfig.DEFAULT_LOG_LEVEL);
            this.performanceLogger = createLogger(LoggingConfig.PERFORMANCE_LOGGER, LoggingConfig.PERFORMANCE_LOG_LEVEL);
            
            // Create file handlers
            this.applicationFileHandler = createFileHandler(LoggingConfig.APPLICATION_LOG_FILE, LoggingConfig.MAX_LOG_FILE_SIZE_MB);
            this.queryFileHandler = createFileHandler(LoggingConfig.QUERY_LOG_FILE, LoggingConfig.MAX_LOG_FILE_SIZE_MB);
            this.errorFileHandler = createFileHandler(LoggingConfig.ERROR_LOG_FILE, LoggingConfig.MAX_LOG_FILE_SIZE_MB);
            this.performanceFileHandler = createFileHandler(LoggingConfig.PERFORMANCE_LOG_FILE, LoggingConfig.MAX_LOG_FILE_SIZE_MB);
            
            // Add handlers to loggers
            setupLoggerHandlers();
            
            // Setup rotation scheduler
            this.rotationScheduler = Executors.newScheduledThreadPool(1);
            scheduleLogRotation();
            
            LOGGER.info("LoggingService initialized successfully");
            
        } catch (Exception e) {
            throw new ApplicationException("Failed to initialize LoggingService", e);
        }
    }
    
    public static synchronized LoggingService getInstance() {
        if (instance == null) {
            try {
                instance = new LoggingService();
            } catch (ApplicationException e) {
                LOGGER.severe("Failed to create LoggingService instance: " + e.getMessage());
                throw new RuntimeException("LoggingService initialization failed", e);
            }
        }
        return instance;
    }
    
    private Logger createLogger(String name, Level level) {
        Logger logger = Logger.getLogger(name);
        logger.setLevel(level);
        logger.setUseParentHandlers(false); // Don't use parent handlers
        return logger;
    }
    
    private FileHandler createFileHandler(String logFile, int maxSizeMB) throws IOException {
        FileHandler handler = new FileHandler(logFile, maxSizeMB * 1024 * 1024, LoggingConfig.MAX_LOG_FILES, true);
        handler.setFormatter(new SimpleFormatter());
        return handler;
    }
    
    private void setupLoggerHandlers() {
        // Application logger gets all handlers
        applicationLogger.addHandler(applicationFileHandler);
        applicationLogger.addHandler(errorFileHandler);
        
        // Query logger gets query and error handlers
        queryLogger.addHandler(queryFileHandler);
        queryLogger.addHandler(errorFileHandler);
        
        // Performance logger gets performance and error handlers
        performanceLogger.addHandler(performanceFileHandler);
        performanceLogger.addHandler(errorFileHandler);
        
        // Other loggers get error handler
        connectionLogger.addHandler(errorFileHandler);
        uiLogger.addHandler(errorFileHandler);
        exportLogger.addHandler(errorFileHandler);
    }
    
    private void scheduleLogRotation() {
        rotationScheduler.scheduleAtFixedRate(this::rotateLogs, 1, LoggingConfig.LOG_ROTATION_INTERVAL_HOURS, TimeUnit.HOURS);
    }
    
    private void rotateLogs() {
        try {
            rotateLogFile(LoggingConfig.APPLICATION_LOG_FILE);
            rotateLogFile(LoggingConfig.QUERY_LOG_FILE);
            rotateLogFile(LoggingConfig.ERROR_LOG_FILE);
            rotateLogFile(LoggingConfig.PERFORMANCE_LOG_FILE);
            LOGGER.info("Log rotation completed successfully");
        } catch (Exception e) {
            LOGGER.warning("Log rotation failed: " + e.getMessage());
        }
    }
    
    private void rotateLogFile(String logFile) throws IOException {
        File file = new File(logFile);
        if (file.exists() && file.length() > 0) {
            String timestamp = LocalDateTime.now().format(TIMESTAMP_FORMATTER);
            String rotatedFileName = logFile.replace(".log", "_" + timestamp + ".log");
            
            Path source = Paths.get(logFile);
            Path target = Paths.get(rotatedFileName);
            
            Files.move(source, target, StandardCopyOption.ATOMIC_MOVE);
            
            // Clean up old rotated files
            cleanupOldRotatedFiles(logFile);
        }
    }
    
    private void cleanupOldRotatedFiles(String baseLogFile) {
        try {
            File logDir = new File(LoggingConfig.LOG_DIR);
            String baseName = new File(baseLogFile).getName().replace(".log", "");
            
            File[] oldFiles = logDir.listFiles((dir, name) -> 
                name.startsWith(baseName + "_") && name.endsWith(".log"));
            
            if (oldFiles != null && oldFiles.length > LoggingConfig.MAX_LOG_FILES) {
                // Sort by modification time and delete oldest
                java.util.Arrays.sort(oldFiles, (f1, f2) -> 
                    Long.compare(f1.lastModified(), f2.lastModified()));
                
                for (int i = 0; i < oldFiles.length - LoggingConfig.MAX_LOG_FILES; i++) {
                    if (oldFiles[i].delete()) {
                        LOGGER.info("Deleted old log file: " + oldFiles[i].getName());
                    }
                }
            }
        } catch (Exception e) {
            LOGGER.warning("Failed to cleanup old rotated files: " + e.getMessage());
        }
    }
    
    private static void createLogDirectory() {
        File logDir = new File(LoggingConfig.LOG_DIR);
        if (!logDir.exists()) {
            if (logDir.mkdirs()) {
                LOGGER.info("Log directory created: " + LoggingConfig.LOG_DIR);
            } else {
                LOGGER.warning("Failed to create log directory: " + LoggingConfig.LOG_DIR);
            }
        }
    }

    // Public logging methods
    public void logApplication(Level level, String message) {
        applicationLogger.log(level, message);
    }
    
    public void logConnection(Level level, String message) {
        connectionLogger.log(level, message);
    }
    
    public void logQuery(Level level, String message) {
        queryLogger.log(level, message);
    }
    
    public void logUI(Level level, String message) {
        uiLogger.log(level, message);
    }
    
    public void logExport(Level level, String message) {
        exportLogger.log(level, message);
    }
    
    public void logPerformance(Level level, String message) {
        performanceLogger.log(level, message);
    }
    
    // Convenience methods for common log levels
    public void info(String category, String message) {
        switch (category.toLowerCase()) {
            case "application": logApplication(Level.INFO, message); break;
            case "connection": logConnection(Level.INFO, message); break;
            case "query": logQuery(Level.INFO, message); break;
            case "ui": logUI(Level.INFO, message); break;
            case "export": logExport(Level.INFO, message); break;
            case "performance": logPerformance(Level.INFO, message); break;
            default: logApplication(Level.INFO, message);
        }
    }
    
    public void warning(String category, String message) {
        switch (category.toLowerCase()) {
            case "application": logApplication(Level.WARNING, message); break;
            case "connection": logConnection(Level.WARNING, message); break;
            case "query": logQuery(Level.WARNING, message); break;
            case "ui": logUI(Level.WARNING, message); break;
            case "export": logExport(Level.WARNING, message); break;
            case "performance": logPerformance(Level.WARNING, message); break;
            default: logApplication(Level.WARNING, message);
        }
    }
    
    public void error(String category, String message) {
        switch (category.toLowerCase()) {
            case "application": logApplication(Level.SEVERE, message); break;
            case "connection": logConnection(Level.SEVERE, message); break;
            case "query": logQuery(Level.SEVERE, message); break;
            case "ui": logUI(Level.SEVERE, message); break;
            case "export": logExport(Level.SEVERE, message); break;
            case "performance": logPerformance(Level.SEVERE, message); break;
            default: logApplication(Level.SEVERE, message);
        }
    }
    
    public void debug(String category, String message) {
        switch (category.toLowerCase()) {
            case "application": logApplication(Level.FINE, message); break;
            case "connection": logConnection(Level.FINE, message); break;
            case "query": logQuery(Level.FINE, message); break;
            case "ui": logUI(Level.FINE, message); break;
            case "export": logExport(Level.FINE, message); break;
            case "performance": logPerformance(Level.FINE, message); break;
            default: logApplication(Level.FINE, message);
        }
    }
    
    // Performance logging with timing
    public void logQueryExecution(String query, long executionTimeMs, int resultCount) {
        String message = String.format("Query executed in %dms, returned %d results: %s", 
            executionTimeMs, resultCount, truncateQuery(query));
        logPerformance(Level.INFO, message);
    }
    
    public void logConnectionAttempt(String host, String database, boolean success, long durationMs) {
        String status = success ? "SUCCESS" : "FAILED";
        String message = String.format("Connection %s to %s/%s in %dms", 
            status, host, database, durationMs);
        logConnection(success ? Level.INFO : Level.WARNING, message);
    }
    
    public void logExportOperation(String format, int recordCount, long durationMs, String filename) {
        String message = String.format("Export to %s completed in %dms: %d records -> %s", 
            format, durationMs, recordCount, filename);
        logExport(Level.INFO, message);
    }
    
    private String truncateQuery(String query) {
        if (query == null) return "null";
        return query.length() > 100 ? query.substring(0, 97) + "..." : query;
    }
    
    // Cleanup method
    public void shutdown() {
        try {
            if (rotationScheduler != null) {
                rotationScheduler.shutdown();
                if (!rotationScheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    rotationScheduler.shutdownNow();
                }
            }
            
            if (applicationFileHandler != null) applicationFileHandler.close();
            if (queryFileHandler != null) queryFileHandler.close();
            if (errorFileHandler != null) errorFileHandler.close();
            if (performanceFileHandler != null) performanceFileHandler.close();
            
            LOGGER.info("LoggingService shutdown completed");
        } catch (Exception e) {
            LOGGER.warning("Error during LoggingService shutdown: " + e.getMessage());
        }
    }
    
    // Get log file paths for UI display
    public String getApplicationLogPath() { return LoggingConfig.APPLICATION_LOG_FILE; }
    public String getQueryLogPath() { return LoggingConfig.QUERY_LOG_FILE; }
    public String getErrorLogPath() { return LoggingConfig.ERROR_LOG_FILE; }
    public String getPerformanceLogPath() { return LoggingConfig.PERFORMANCE_LOG_FILE; }
    
    public String getLogDirectory() { return LoggingConfig.LOG_DIR; }
} 