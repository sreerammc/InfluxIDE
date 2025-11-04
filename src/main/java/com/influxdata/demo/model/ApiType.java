package com.influxdata.demo.model;

/**
 * Enum representing different API types for InfluxDB connections
 * Provides type safety and consistent naming
 */
public enum ApiType {
    
    FLIGHT_SQL("Flight SQL", "Best performance with JDBC driver and Apache Arrow. Works with InfluxDB 3.0 Core, Enterprise, and Clustered. Uses jdbc:arrow-flight-sql:// protocol."),
    INFLUXDB_3_API("InfluxDB 3 Java API", "Official InfluxDB 3 Java client library. Works with InfluxDB 3.0 Core, Enterprise, and Clustered. Uses com.influxdb.v3.client.InfluxDBClient."),
    REST_API("REST API", "HTTP REST API for querying. Works with InfluxDB 3.0 Core, Enterprise, and Clustered using /api/v3/query_sql endpoint.");
    
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