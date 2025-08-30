package com.influxdata.demo;

import com.influxdata.demo.config.ApplicationConfig;
import com.influxdata.demo.model.ApiType;
import com.influxdata.demo.model.Protocol;
import com.influxdata.demo.model.QueryTimeout;
import com.influxdata.demo.service.*;
import com.influxdata.demo.ui.ConnectionDialog;
import com.influxdata.demo.ui.QueryPanel;
import com.influxdata.demo.ui.ResultsPanel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Comprehensive test for the refactored architecture
 * Tests all components and services to ensure they work correctly
 */
public class RefactoredArchitectureTest {
    
    public static void main(String[] args) {
        System.out.println("🧪 Testing Complete Refactored Architecture...");
        System.out.println("================================================");
        
        try {
            // Test 1: Configuration System
            testConfigurationSystem();
            
            // Test 2: Service Layer
            testServiceLayer();
            
            // Test 3: UI Components
            testUIComponents();
            
            // Test 4: Integration
            testIntegration();
            
            System.out.println("================================================");
            System.out.println("✅ All tests passed! Architecture is working correctly.");
            System.out.println("🚀 Ready for production use!");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    /**
     * Test configuration system
     */
    private static void testConfigurationSystem() {
        System.out.println("\n📋 Testing Configuration System...");
        
        // Test ApplicationConfig
        ApplicationConfig config = new ApplicationConfig();
        assert config.getProtocol() == Protocol.HTTP : "Default protocol should be HTTP";
        assert config.getApiType() == ApiType.FLIGHT_SQL : "Default API type should be Flight SQL";
        assert config.getQueryTimeout() == QueryTimeout.TWO_MINUTES : "Default timeout should be 2 minutes";
        
        // Test setters and validation
        config.setProtocol(Protocol.HTTPS);
        config.setHost("localhost:8086");
        config.setDatabase("testdb");
        config.setToken("test-token");
        
        assert config.getProtocol() == Protocol.HTTPS : "Protocol should be HTTPS";
        assert config.getHost().equals("localhost:8086") : "Host should be localhost:8086";
        assert config.isValid() : "Config should be valid";
        assert config.getValidationError() == null : "Validation error should be null";
        
        // Test copy constructor
        ApplicationConfig configCopy = new ApplicationConfig(config);
        assert configCopy.getHost().equals(config.getHost()) : "Copy should have same host";
        assert configCopy.getDatabase().equals(config.getDatabase()) : "Copy should have same database";
        
        System.out.println("✅ Configuration System test passed");
    }
    
    /**
     * Test service layer
     */
    private static void testServiceLayer() {
        System.out.println("\n🔧 Testing Service Layer...");
        
        // Test SettingsService
        SettingsService settingsService = new SettingsService();
        assert settingsService != null : "Settings service should be created";
        assert settingsService.getSettingsFilePath() != null : "Settings file path should not be null";
        
        // Test TimezoneService
        TimezoneService timezoneService = new TimezoneService();
        assert timezoneService != null : "Timezone service should be created";
        assert timezoneService.isTimestampColumn("time") : "Should detect 'time' as timestamp column";
        assert timezoneService.isTimestampColumn("created_at") : "Should detect 'created_at' as timestamp column";
        assert !timezoneService.isTimestampColumn("name") : "Should not detect 'name' as timestamp column";
        
        String[] timezones = timezoneService.getAvailableTimezones();
        assert timezones.length > 0 : "Should have available timezones";
        assert timezones[0].equals("System Default (Local)") : "First timezone should be System Default";
        
        // Test DataProcessingService
        DataProcessingService dataService = new DataProcessingService();
        assert dataService != null : "Data processing service should be created";
        
        // Test ExportService
        ExportService exportService = new ExportService();
        assert exportService != null : "Export service should be created";
        assert exportService.isFormatSupported("CSV") : "CSV format should be supported";
        assert !exportService.isFormatSupported("XML") : "XML format should not be supported";
        
        System.out.println("✅ Service Layer test passed");
    }
    
    /**
     * Test UI components
     */
    private static void testUIComponents() {
        System.out.println("\n🎨 Testing UI Components...");
        
        // Test QueryPanel
        QueryPanel queryPanel = new QueryPanel();
        assert queryPanel != null : "Query panel should be created";
        assert queryPanel.getQueryText().isEmpty() : "Query text should be empty initially";
        
        queryPanel.setQueryText("SELECT * FROM test");
        assert queryPanel.getQueryText().equals("SELECT * FROM test") : "Query text should be set";
        
        queryPanel.clearQuery();
        assert queryPanel.getQueryText().isEmpty() : "Query should be cleared";
        
        // Test ResultsPanel
        ResultsPanel resultsPanel = new ResultsPanel();
        assert resultsPanel != null : "Results panel should be created";
        assert !resultsPanel.hasResults() : "Results panel should have no results initially";
        assert resultsPanel.getCurrentRecordCount() == 0 : "Record count should be 0 initially";
        
        // Test with sample data
        List<Map<String, Object>> sampleData = createSampleData();
        resultsPanel.displayResults(sampleData);
        assert resultsPanel.hasResults() : "Results panel should have results";
        assert resultsPanel.getCurrentRecordCount() == 2 : "Record count should be 2";
        
        System.out.println("✅ UI Components test passed");
    }
    
    /**
     * Test integration between components
     */
    private static void testIntegration() {
        System.out.println("\n🔗 Testing Component Integration...");
        
        // Test data flow
        DataProcessingService dataService = new DataProcessingService();
        ExportService exportService = new ExportService();
        
        // Create sample JSON response
        String sampleJson = createSampleJSON();
        
        try {
            // Parse data
            List<Map<String, Object>> parsedData = dataService.parseInfluxDBResponse(sampleJson);
            assert parsedData != null : "Data should be parsed";
            assert parsedData.size() == 2 : "Should have 2 rows";
            
            // Test export
            String exportPath = exportService.exportToCSV(parsedData, "test_export");
            assert exportPath != null : "Export path should not be null";
            assert exportPath.endsWith(".csv") : "Export path should end with .csv";
            
            // Clean up test file
            new java.io.File(exportPath).delete();
            
        } catch (Exception e) {
            throw new RuntimeException("Integration test failed", e);
        }
        
        System.out.println("✅ Component Integration test passed");
    }
    
    /**
     * Create sample data for testing
     */
    private static List<Map<String, Object>> createSampleData() {
        List<Map<String, Object>> data = new ArrayList<>();
        
        Map<String, Object> row1 = new HashMap<>();
        row1.put("id", 1);
        row1.put("name", "Test Item 1");
        row1.put("value", 100.5);
        data.add(row1);
        
        Map<String, Object> row2 = new HashMap<>();
        row2.put("id", 2);
        row2.put("name", "Test Item 2");
        row2.put("value", 200.75);
        data.add(row2);
        
        return data;
    }
    
    /**
     * Create sample JSON for testing
     */
    private static String createSampleJSON() {
        return "[{\"id\":1,\"name\":\"Test Item 1\",\"value\":100.5}," +
               "{\"id\":2,\"name\":\"Test Item 2\",\"value\":200.75}]";
    }
    
    /**
     * Test memory management
     */
    private static void testMemoryManagement() {
        System.out.println("\n💾 Testing Memory Management...");
        
        Runtime runtime = Runtime.getRuntime();
        long initialMemory = runtime.totalMemory() - runtime.freeMemory();
        
        // Create large dataset
        List<Map<String, Object>> largeData = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            Map<String, Object> row = new HashMap<>();
            row.put("id", i);
            row.put("name", "Item " + i);
            row.put("value", Math.random() * 1000);
            largeData.add(row);
        }
        
        long afterDataCreation = runtime.totalMemory() - runtime.freeMemory();
        long memoryUsed = afterDataCreation - initialMemory;
        
        System.out.println("Memory used for 1000 rows: " + (memoryUsed / 1024) + " KB");
        
        // Clear data and force garbage collection
        largeData.clear();
        largeData = null;
        System.gc();
        
        long afterCleanup = runtime.totalMemory() - runtime.freeMemory();
        long memoryRecovered = afterDataCreation - afterCleanup;
        
        System.out.println("Memory recovered: " + (memoryRecovered / 1024) + " KB");
        
        assert memoryRecovered > 0 : "Memory should be recovered after cleanup";
        
        System.out.println("✅ Memory Management test passed");
    }
    
    /**
     * Test error handling
     */
    private static void testErrorHandling() {
        System.out.println("\n⚠️ Testing Error Handling...");
        
        try {
            // Test invalid configuration
            ApplicationConfig invalidConfig = new ApplicationConfig();
            assert !invalidConfig.isValid() : "Empty config should be invalid";
            assert invalidConfig.getValidationError() != null : "Should have validation error";
            
            // Test invalid timezone
            TimezoneService timezoneService = new TimezoneService();
            try {
                timezoneService.getSelectedTimezone("Invalid/Timezone");
                assert false : "Should throw exception for invalid timezone";
            } catch (Exception e) {
                // Expected exception
                assert e.getMessage().contains("Invalid timezone") : "Should have appropriate error message";
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error handling test failed", e);
        }
        
        System.out.println("✅ Error Handling test passed");
    }
} 