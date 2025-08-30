package com.influxdata.demo.model;

/**
 * Enum representing query timeout options
 */
public enum QueryTimeout {
    
    THIRTY_SECONDS("30 seconds", 30),
    ONE_MINUTE("1 minute", 60),
    TWO_MINUTES("2 minutes", 120),
    FIVE_MINUTES("5 minutes", 300),
    TEN_MINUTES("10 minutes", 600);
    
    private final String displayName;
    private final int seconds;
    
    QueryTimeout(String displayName, int seconds) {
        this.displayName = displayName;
        this.seconds = seconds;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    public int getSeconds() {
        return seconds;
    }
    
    public int getMilliseconds() {
        return seconds * 1000;
    }
    
    /**
     * Get timeout from display name
     */
    public static QueryTimeout fromDisplayName(String displayName) {
        for (QueryTimeout timeout : values()) {
            if (timeout.displayName.equals(displayName)) {
                return timeout;
            }
        }
        throw new IllegalArgumentException("Unknown timeout: " + displayName);
    }
    
    /**
     * Get default timeout
     */
    public static QueryTimeout getDefault() {
        return TWO_MINUTES;
    }
    
    @Override
    public String toString() {
        return displayName;
    }
} 