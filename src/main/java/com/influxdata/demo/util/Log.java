package com.influxdata.demo.util;

import com.influxdata.demo.service.LoggingService;
import java.util.logging.Level;

/**
 * Simple logging utility class for easy access to logging functionality.
 * Provides static methods for logging at different levels and categories.
 */
public final class Log {
    
    private Log() {} // Utility class, prevent instantiation
    
    // Application logging
    public static void appInfo(String message) {
        getLoggingService().info("application", message);
    }
    
    public static void appWarning(String message) {
        getLoggingService().warning("application", message);
    }
    
    public static void appError(String message) {
        getLoggingService().error("application", message);
    }
    
    public static void appDebug(String message) {
        getLoggingService().debug("application", message);
    }
    
    // Connection logging
    public static void connectionInfo(String message) {
        getLoggingService().info("connection", message);
    }
    
    public static void connectionWarning(String message) {
        getLoggingService().warning("connection", message);
    }
    
    public static void connectionError(String message) {
        getLoggingService().error("connection", message);
    }
    
    public static void connectionDebug(String message) {
        getLoggingService().debug("connection", message);
    }
    
    // Query logging
    public static void queryInfo(String message) {
        getLoggingService().info("query", message);
    }
    
    public static void queryWarning(String message) {
        getLoggingService().warning("query", message);
    }
    
    public static void queryError(String message) {
        getLoggingService().error("query", message);
    }
    
    public static void queryDebug(String message) {
        getLoggingService().debug("query", message);
    }
    
    // UI logging
    public static void uiInfo(String message) {
        getLoggingService().info("ui", message);
    }
    
    public static void uiWarning(String message) {
        getLoggingService().warning("ui", message);
    }
    
    public static void uiError(String message) {
        getLoggingService().error("ui", message);
    }
    
    public static void uiDebug(String message) {
        getLoggingService().debug("ui", message);
    }
    
    // Export logging
    public static void exportInfo(String message) {
        getLoggingService().info("export", message);
    }
    
    public static void exportWarning(String message) {
        getLoggingService().warning("export", message);
    }
    
    public static void exportError(String message) {
        getLoggingService().error("export", message);
    }
    
    public static void exportDebug(String message) {
        getLoggingService().debug("export", message);
    }
    
    // Performance logging
    public static void performanceInfo(String message) {
        getLoggingService().info("performance", message);
    }
    
    public static void performanceWarning(String message) {
        getLoggingService().warning("performance", message);
    }
    
    public static void performanceError(String message) {
        getLoggingService().error("performance", message);
    }
    
    public static void performanceDebug(String message) {
        getLoggingService().debug("performance", message);
    }
    
    // Specialized logging methods
    public static void logQueryExecution(String query, long executionTimeMs, int resultCount) {
        getLoggingService().logQueryExecution(query, executionTimeMs, resultCount);
    }
    
    public static void logConnectionAttempt(String host, String database, boolean success, long durationMs) {
        getLoggingService().logConnectionAttempt(host, database, success, durationMs);
    }
    
    public static void logExportOperation(String format, int recordCount, long durationMs, String filename) {
        getLoggingService().logExportOperation(format, recordCount, durationMs, filename);
    }
    
    // Generic logging with category
    public static void info(String category, String message) {
        getLoggingService().info(category, message);
    }
    
    public static void warning(String category, String message) {
        getLoggingService().warning(category, message);
    }
    
    public static void error(String category, String message) {
        getLoggingService().error(category, message);
    }
    
    public static void debug(String category, String message) {
        getLoggingService().debug(category, message);
    }
    
    // Exception logging
    public static void logException(String category, String message, Throwable exception) {
        String fullMessage = message + " - Exception: " + exception.getMessage();
        getLoggingService().error(category, fullMessage);
        if (exception.getCause() != null) {
            getLoggingService().error(category, "Caused by: " + exception.getCause().getMessage());
        }
    }
    
    public static void logException(String message, Throwable exception) {
        logException("application", message, exception);
    }
    
    // Shutdown logging service
    public static void shutdown() {
        getLoggingService().shutdown();
    }
    
    // Get log file paths
    public static String getApplicationLogPath() {
        return getLoggingService().getApplicationLogPath();
    }
    
    public static String getQueryLogPath() {
        return getLoggingService().getQueryLogPath();
    }
    
    public static String getErrorLogPath() {
        return getLoggingService().getErrorLogPath();
    }
    
    public static String getPerformanceLogPath() {
        return getLoggingService().getPerformanceLogPath();
    }
    
    public static String getLogDirectory() {
        return getLoggingService().getLogDirectory();
    }
    
    // Private helper method
    private static LoggingService getLoggingService() {
        return LoggingService.getInstance();
    }
} 