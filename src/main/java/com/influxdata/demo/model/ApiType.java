package com.influxdata.demo.model;

/**
 * Enum representing different API types for InfluxDB connections
 * Provides type safety and consistent naming
 */
public enum ApiType {
    
    FLIGHT_SQL("Flight SQL", "Best performance with JDBC driver and Apache Arrow (Default) - Note: Requires InfluxDB 3.x with Flight SQL enabled. The app will try multiple endpoints: /flight, /arrow-flight, or direct host. If Flight SQL fails, it automatically falls back to REST API."),
    INFLUXDB_3_API("InfluxDB 3 Java API", "Legacy Java client for InfluxDB 3.x"),
    REST_API("REST API", "Traditional HTTP queries with InfluxDB 1.x compatibility");
    
    private final String displayName;
    private final String description;
    
    ApiType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public String getDescription() {
        return description;
    }
    
    /**
     * Get API type from display name
     */
    public static ApiType fromDisplayName(String displayName) {
        for (ApiType apiType : values()) {
            if (apiType.displayName.equals(displayName)) {
                return apiType;
            }
        }
        throw new IllegalArgumentException("Unknown API type: " + displayName);
    }
    
    /**
     * Check if this API type supports Flight SQL
     */
    public boolean isFlightSQL() {
        return this == FLIGHT_SQL || this == INFLUXDB_3_API;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 