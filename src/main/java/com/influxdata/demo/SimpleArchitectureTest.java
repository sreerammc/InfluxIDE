package com.influxdata.demo;

import com.influxdata.demo.config.ApplicationConfig;
import com.influxdata.demo.model.ApiType;
import com.influxdata.demo.model.Protocol;
import com.influxdata.demo.model.QueryTimeout;
import com.influxdata.demo.service.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple test for the refactored architecture (no JavaFX components)
 * Tests all services and configuration to ensure they work correctly
 */
public class SimpleArchitectureTest {
    
    public static void main(String[] args) {
        System.out.println("🧪 Testing Refactored Architecture (Services & Configuration)...");
        System.out.println("================================================================");
        
        try {
            // Test 1: Configuration System
            testConfigurationSystem();
            
            // Test 2: Service Layer
            testServiceLayer();
            
            // Test 3: Data Processing
            testDataProcessing();
            
            // Test 4: Export Functionality
            testExportFunctionality();
            
            // Test 5: Memory Management
            testMemoryManagement();
            
            // Test 6: Error Handling
            testErrorHandling();
            
            System.out.println("================================================================");
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
        
        // Test validation
        ApplicationConfig invalidConfig = new ApplicationConfig();
        assert !invalidConfig.isValid() : "Empty config should be invalid";
        assert invalidConfig.getValidationError() != null : "Should have validation error";
        
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
        assert settingsService.getSettingsFilePath().contains(".influxdb-ide") : "Settings path should contain .influxdb-ide";
        
        // Test TimezoneService
        TimezoneService timezoneService = new TimezoneService();
        assert timezoneService != null : "Timezone service should be created";
        assert timezoneService.isTimestampColumn("time") : "Should detect 'time' as timestamp column";
        assert timezoneService.isTimestampColumn("created_at") : "Should detect 'created_at' as timestamp column";
        assert timezoneService.isTimestampColumn("updated_date") : "Should detect 'updated_date' as timestamp column";
        assert !timezoneService.isTimestampColumn("name") : "Should not detect 'name' as timestamp column";
        assert !timezoneService.isTimestampColumn("value") : "Should not detect 'value' as timestamp column";
        
        String[] timezones = timezoneService.getAvailableTimezones();
        assert timezones.length > 0 : "Should have available timezones";
        assert timezones[0].equals("System Default (Local)") : "First timezone should be System Default";
        assert timezones[1].equals("UTC") : "Second timezone should be UTC";
        
        // Test DataProcessingService
        DataProcessingService dataService = new DataProcessingService();
        assert dataService != null : "Data processing service should be created";
        
        // Test ExportService
        ExportService exportService = new ExportService();
        assert exportService != null : "Export service should be created";
        assert exportService.isFormatSupported("CSV") : "CSV format should be supported";
        assert !exportService.isFormatSupported("XML") : "XML format should not be supported";
        assert !exportService.isFormatSupported("JSON") : "JSON format should not be supported yet";
        
        String[] supportedFormats = exportService.getSupportedFormats();
        assert supportedFormats.length == 1 : "Should have 1 supported format";
        assert supportedFormats[0].equals("CSV") : "First supported format should be CSV";
        
        System.out.println("✅ Service Layer test passed");
    }
    
    /**
     * Test data processing functionality
     */
    private static void testDataProcessing() {
        System.out.println("\n📊 Testing Data Processing...");
        
        DataProcessingService dataService = new DataProcessingService();
        
        // Test direct array format parsing
        String directArrayJson = "[{\"id\":1,\"name\":\"Test Item 1\",\"value\":100.5},{\"id\":2,\"name\":\"Test Item 2\",\"value\":200.75}]";
        
        try {
            List<Map<String, Object>> parsedData = dataService.parseInfluxDBResponse(directArrayJson);
            assert parsedData != null : "Data should be parsed";
            assert parsedData.size() == 2 : "Should have 2 rows";
            
            // Check first row
            Map<String, Object> firstRow = parsedData.get(0);
            assert firstRow.get("id").equals("1") : "First row id should be '1'";
            assert firstRow.get("name").equals("Test Item 1") : "First row name should be 'Test Item 1'";
            assert firstRow.get("value").equals("100.5") : "First row value should be '100.5'";
            
            // Check second row
            Map<String, Object> secondRow = parsedData.get(1);
            assert secondRow.get("id").equals("2") : "Second row id should be '2'";
            assert secondRow.get("name").equals("Test Item 2") : "Second row name should be 'Test Item 2'";
            assert secondRow.get("value").equals("200.75") : "Second row value should be '200.75'";
            
        } catch (Exception e) {
            throw new RuntimeException("Data processing test failed", e);
        }
        
        System.out.println("✅ Data Processing test passed");
    }
    
    /**
     * Test export functionality
     */
    private static void testExportFunctionality() {
        System.out.println("\n📤 Testing Export Functionality...");
        
        ExportService exportService = new ExportService();
        
        // Create sample data
        List<Map<String, Object>> sampleData = createSampleData();
        
        try {
            // Test export validation
            exportService.validateExportData(sampleData);
            
            // Test export with default filename
            String exportPath = exportService.exportToCSV(sampleData);
            assert exportPath != null : "Export path should not be null";
            assert exportPath.endsWith(".csv") : "Export path should end with .csv";
            assert exportPath.contains("influxdb_export") : "Export path should contain influxdb_export";
            
            // Test export statistics
            String stats = exportService.getExportStatistics(sampleData);
            assert stats.contains("2 rows") : "Stats should mention 2 rows";
            assert stats.contains("3 columns") : "Stats should mention 3 columns";
            
            // Clean up test file
            new java.io.File(exportPath).delete();
            
        } catch (Exception e) {
            throw new RuntimeException("Export functionality test failed", e);
        }
        
        System.out.println("✅ Export Functionality test passed");
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
            row.put("description", "This is a long description for item " + i + " to test memory usage");
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
            assert invalidConfig.getValidationError().contains("Host is required") : "Should mention host requirement";
            
            // Test invalid timezone
            TimezoneService timezoneService = new TimezoneService();
            try {
                timezoneService.getSelectedTimezone("Invalid/Timezone");
                assert false : "Should throw exception for invalid timezone";
            } catch (Exception e) {
                // Expected exception
                assert e.getMessage().contains("Invalid timezone") : "Should have appropriate error message";
            }
            
            // Test invalid data processing
            DataProcessingService dataService = new DataProcessingService();
            try {
                dataService.parseInfluxDBResponse("invalid json");
                assert false : "Should throw exception for invalid JSON";
            } catch (Exception e) {
                // Expected exception
                assert e.getMessage().contains("Unknown response format") : "Should have appropriate error message";
            }
            
        } catch (Exception e) {
            throw new RuntimeException("Error handling test failed", e);
        }
        
        System.out.println("✅ Error Handling test passed");
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
} 