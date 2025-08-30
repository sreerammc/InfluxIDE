package com.influxdata.demo;

import com.influxdata.demo.config.ApplicationConfig;
import com.influxdata.demo.model.ApiType;
import com.influxdata.demo.model.Protocol;
import com.influxdata.demo.model.QueryTimeout;
import com.influxdata.demo.service.SettingsService;
import com.influxdata.demo.service.TimezoneService;

/**
 * Simple test class to verify our refactored architecture
 * This is temporary and will be removed after refactoring is complete
 */
public class RefactoringTest {
    
    public static void main(String[] args) {
        System.out.println("🧪 Testing Refactored Architecture...");
        
        try {
            // Test 1: Configuration
            testConfiguration();
            
            // Test 2: Settings Service
            testSettingsService();
            
            // Test 3: Timezone Service
            testTimezoneService();
            
            System.out.println("✅ All tests passed! Architecture is working correctly.");
            
        } catch (Exception e) {
            System.err.println("❌ Test failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testConfiguration() {
        System.out.println("Testing Configuration...");
        
        ApplicationConfig config = new ApplicationConfig();
        
        // Test default values
        assert config.getProtocol() == Protocol.HTTP : "Default protocol should be HTTP";
        assert config.getApiType() == ApiType.FLIGHT_SQL : "Default API type should be Flight SQL";
        assert config.getQueryTimeout() == QueryTimeout.TWO_MINUTES : "Default timeout should be 2 minutes";
        
        // Test setters
        config.setProtocol(Protocol.HTTPS);
        config.setHost("localhost:8086");
        config.setDatabase("testdb");
        config.setToken("test-token");
        
        assert config.getProtocol() == Protocol.HTTPS : "Protocol should be HTTPS";
        assert config.getHost().equals("localhost:8086") : "Host should be localhost:8086";
        assert config.isValid() : "Config should be valid";
        
        System.out.println("✅ Configuration test passed");
    }
    
    private static void testSettingsService() {
        System.out.println("Testing Settings Service...");
        
        SettingsService service = new SettingsService();
        
        // Test service creation
        assert service != null : "Settings service should be created";
        assert service.getSettingsFilePath() != null : "Settings file path should not be null";
        
        System.out.println("✅ Settings Service test passed");
    }
    
    private static void testTimezoneService() {
        System.out.println("Testing Timezone Service...");
        
        TimezoneService service = new TimezoneService();
        
        // Test service creation
        assert service != null : "Timezone service should be created";
        
        // Test timezone detection
        assert service.isTimestampColumn("time") : "Should detect 'time' as timestamp column";
        assert service.isTimestampColumn("created_at") : "Should detect 'created_at' as timestamp column";
        assert !service.isTimestampColumn("name") : "Should not detect 'name' as timestamp column";
        
        // Test available timezones
        String[] timezones = service.getAvailableTimezones();
        assert timezones.length > 0 : "Should have available timezones";
        assert timezones[0].equals("System Default (Local)") : "First timezone should be System Default";
        
        System.out.println("✅ Timezone Service test passed");
    }
} 