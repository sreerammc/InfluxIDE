package com.influxdata.demo;

import com.influxdata.demo.util.Log;

/**
 * Simple test class to verify logging functionality
 */
public class LoggingTest {
    
    public static void main(String[] args) {
        System.out.println("🧪 Testing Logging System...");
        System.out.println("=============================");
        
        try {
            // Test basic logging
            Log.appInfo("Application started");
            Log.appWarning("This is a test warning");
            Log.appError("This is a test error");
            
            // Test category logging
            Log.info("connection", "Testing connection to localhost");
            Log.warning("query", "Query timeout approaching");
            Log.error("export", "Export failed due to disk space");
            Log.debug("ui", "User clicked export button");
            
            // Test specialized logging
            Log.logQueryExecution("SELECT * FROM measurements", 1500, 1000);
            Log.logConnectionAttempt("localhost", "testdb", true, 250);
            Log.logExportOperation("CSV", 500, 3000, "export.csv");
            
            // Test exception logging
            try {
                throw new RuntimeException("Test exception for logging");
            } catch (Exception e) {
                Log.logException("test", "Testing exception logging", e);
            }
            
            System.out.println("✅ All logging tests completed successfully!");
            System.out.println("📁 Check log files in: " + Log.getLogDirectory());
            
        } catch (Exception e) {
            System.err.println("❌ Logging test failed: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Shutdown logging
            Log.shutdown();
        }
    }
} 